package com.bepngon.merge.game

import org.jbox2d.callbacks.ContactImpulse
import org.jbox2d.callbacks.ContactListener
import org.jbox2d.collision.Manifold
import org.jbox2d.collision.shapes.CircleShape
import org.jbox2d.collision.shapes.PolygonShape
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.Body
import org.jbox2d.dynamics.BodyDef
import org.jbox2d.dynamics.BodyType
import org.jbox2d.dynamics.World
import org.jbox2d.dynamics.contacts.Contact
import kotlin.math.PI
import kotlin.random.Random

/**
 * Fruit-drop minigame physics on jbox2d.
 *
 * Tuned constants:
 *   gravity 9.86*0.15 m/s² · restitution 0.25 · friction 0.35
 *   linearDamping 0.2 · angularDamping 0.4 · mass 0.1 kg · 8 solver iterations
 * World units are METERS: play field is 0.6 x 0.9 m, +y points DOWN.
 */
object Subak {
    const val GAME_WIDTH = 0.6f          // m
    const val GAME_HEIGHT = 0.9f         // m
    const val GAME_WIDTH_PX = 600f       // reference scale from the web build
    const val WALL_THICKNESS = GAME_WIDTH / 20f
    const val GAME_OVER_HEIGHT = GAME_HEIGHT / 6f   // top of fruit above this = danger

    const val GRAVITY = 9.86f * 0.15f
    const val RESTITUTION = 0.25f
    const val FRICTION = 0.35f
    const val FRUIT_MASS = 0.1f
    const val LINEAR_DAMPING = 0.2f
    const val ANGULAR_DAMPING = 0.4f

    const val STEP_MS = 1000f / 60f      // Rapier default dt = 1/60
    const val VELOCITY_ITERATIONS = 8    // numSolverIterations = 8
    const val POSITION_ITERATIONS = 3
    const val MAX_FRAME_MS = 100f        // clamp catch-up burst like the original
    const val DROP_COOLDOWN_MS = 500L
    const val OUT_OF_BOUNDS_MS = 1_000L  // fruit must sit above the line this long
    const val MERGE_EFFECT_MS = 1_000L
    const val WATERMELON_MERGE_POINTS = 150
    const val DROPPABLE_TIERS = 4        // next fruit = uniform random of first 4

    val FRUIT_NAMES = listOf(
        "blueberry", "grape", "lemon", "orange", "apple", "dragonfruit",
        "pear", "peach", "pineapple", "honeydew", "watermelon",
    )

    // Diameter as % of game width (manual, based on suika — same table as the repo)
    private val FRUIT_SIZES = floatArrayOf(
        6.76f, 8.86f, 11.55f, 14.41f, 19.07f, 23.54f,
        28.38f, 34.93f, 39.34f, 48.57f, 57.81f,
    )

    class FruitData(val name: String, val size: Float, val radius: Float, val points: Int)

    val FRUITS: List<FruitData> = FRUIT_NAMES.mapIndexed { i, name ->
        val size = FRUIT_SIZES[i] / 2f
        FruitData(name, size, GAME_WIDTH * (size / 100f), (i + 1) * (i + 1))
    }
}

/** A fruit + its physics body. */
class SubakFruit(
    val id: Int,
    val fruitIndex: Int,
    val body: Body,
) {
    val radius: Float get() = Subak.FRUITS[fruitIndex].radius
    val points: Int get() = Subak.FRUITS[fruitIndex].points
    // previous-step transform for render interpolation
    var prevX = 0f
    var prevY = 0f
    var prevRot = 0f
    // engine-time ms when the fruit first crossed the danger line
    var outOfBoundsSince: Long? = null
}

data class MergeEffect(val id: Int, val x: Float, val y: Float, val radius: Float, val startMs: Long)

/**
 * The whole fruit-drop game state: physics world, fruits, score, merge queue.
 * Pure JVM — unit-testable without Android. Call [step] every frame with the
 * elapsed ms; call [dropFruit] from input (respecting DROP_COOLDOWN_MS).
 */
class SubakEngine(
    private val random: Random = Random,
    worldHeight: Float = Subak.GAME_HEIGHT,
) {
    private val world = World(Vec2(0f, Subak.GRAVITY))
    val fruits = mutableListOf<SubakFruit>()
    val mergeEffects = mutableListOf<MergeEffect>()

    var worldHeight = worldHeight
        private set
    var gameOverHeight = worldHeight / 6f
        private set

    var score = 0
        private set
    var currentFruitIndex = randomDropIndex()
        private set
    var nextFruitIndex = randomDropIndex()
        private set
    var dropCount = 0
        private set
    var gameOverFruitId: Int? = null
        private set
    var gameOver = false
        private set
    // engine clock (sum of consumed fixed steps) — used for the 1s OOB hold
    var engineMs = 0L
        private set

    private var accumulatorMs = 0f
    private var nextId = 1
    private var nextEffectId = 1
    private val boundBodies = mutableListOf<Body>()
    private val pendingMerges = mutableListOf<Pair<SubakFruit, SubakFruit>>()

    init {
        createBounds()
        // collect same-tier contacts during world.step — bodies may not be
        // created/destroyed inside the callback, so merges run afterwards
        world.setContactListener(object : ContactListener {
            override fun beginContact(contact: Contact) {
                val a = contact.fixtureA.body.userData as? SubakFruit ?: return
                val b = contact.fixtureB.body.userData as? SubakFruit ?: return
                if (a.fruitIndex != b.fruitIndex) return
                if (pendingMerges.any { it.first === a || it.second === a ||
                        it.first === b || it.second === b }
                ) return
                pendingMerges += a to b
            }

            override fun endContact(contact: Contact) {}
            override fun preSolve(contact: Contact, oldManifold: Manifold) {}
            override fun postSolve(contact: Contact, impulse: ContactImpulse) {}
        })
    }

    private fun createBounds() {
        fun wall(x: Float, y: Float, w: Float, h: Float) {
            val bd = BodyDef().apply {
                type = BodyType.STATIC
                position.set(x, y)
            }
            val body = world.createBody(bd)
            val box = PolygonShape().apply { setAsBox(w / 2f, h / 2f) }
            body.createFixture(box, 0f)
            boundBodies += body
        }
        val t = Subak.WALL_THICKNESS
        val h = worldHeight
        wall(-t / 2f, h / 2f, t, h)                                // left
        wall(Subak.GAME_WIDTH + t / 2f, h / 2f, t, h)              // right
        wall(Subak.GAME_WIDTH / 2f, h + t / 2f, Subak.GAME_WIDTH, t) // floor
    }

    /**
     * Re-fit the playfield to the real container aspect (height in meters).
     * Only valid before the first fruit exists — afterwards the bounds are
     * locked so a resize mid-game can't warp settled fruit.
     */
    fun configure(heightM: Float) {
        if (fruits.isNotEmpty() || heightM == worldHeight || heightM <= 0f) return
        worldHeight = heightM
        gameOverHeight = heightM / 6f
        boundBodies.forEach { world.destroyBody(it) }
        boundBodies.clear()
        createBounds()
    }

    fun randomDropIndex(): Int = random.nextInt(Subak.DROPPABLE_TIERS)

    fun addFruit(fruitIndex: Int, x: Float, y: Float): SubakFruit {
        val data = Subak.FRUITS[fruitIndex]
        val bd = BodyDef().apply {
            type = BodyType.DYNAMIC
            position.set(x, y)
            linearDamping = Subak.LINEAR_DAMPING
            angularDamping = Subak.ANGULAR_DAMPING
        }
        val body = world.createBody(bd)
        val shape = CircleShape().apply { m_radius = data.radius }
        body.createFixture(shape, Subak.FRUIT_MASS / (PI.toFloat() * data.radius * data.radius)).apply {
            restitution = Subak.RESTITUTION
            friction = Subak.FRICTION
        }
        val fruit = SubakFruit(nextId++, fruitIndex, body).also {
            it.prevX = x; it.prevY = y; it.prevRot = 0f
        }
        body.userData = fruit
        fruits += fruit
        return fruit
    }

    fun dropFruit(x: Float): SubakFruit {
        val f = addFruit(currentFruitIndex, x, gameOverHeight / 2f)
        currentFruitIndex = nextFruitIndex
        nextFruitIndex = randomDropIndex()
        dropCount += 1
        return f
    }

    /** Advance the simulation by [frameDtMs] of real time (clamped). */
    fun step(frameDtMs: Float) {
        if (gameOver) return
        accumulatorMs += frameDtMs.coerceAtMost(Subak.MAX_FRAME_MS)
        while (accumulatorMs >= Subak.STEP_MS) {
            accumulatorMs -= Subak.STEP_MS
            engineMs += Subak.STEP_MS.toLong()
            fruits.forEach {
                val p = it.body.position
                it.prevX = p.x; it.prevY = p.y; it.prevRot = it.body.angle
            }
            world.step(Subak.STEP_MS / 1000f, Subak.VELOCITY_ITERATIONS, Subak.POSITION_ITERATIONS)
            processMerges()
            checkGameOver()
        }
        mergeEffects.removeAll { engineMs - it.startMs > Subak.MERGE_EFFECT_MS }
    }

    /** Fraction [0,1) of the way through the next fixed step — for interpolation. */
    fun interpolationT(): Float = (accumulatorMs / Subak.STEP_MS).coerceIn(0f, 1f)

    private fun processMerges() {
        if (pendingMerges.isEmpty()) return
        for ((a, b) in pendingMerges) {
            if (!fruits.contains(a) || !fruits.contains(b)) continue
            val pa = a.body.position
            val pb = b.body.position
            val mx = (pa.x + pb.x) / 2f
            val my = (pa.y + pb.y) / 2f
            val bothWatermelon = a.fruitIndex == Subak.FRUITS.lastIndex
            destroyFruit(a)
            destroyFruit(b)
            val nextIndex = a.fruitIndex + 1
            val effectRadius = if (bothWatermelon) a.radius else Subak.FRUITS[nextIndex].radius
            mergeEffects += MergeEffect(nextEffectId++, mx, my, effectRadius, engineMs)
            if (bothWatermelon) {
                score += Subak.WATERMELON_MERGE_POINTS
            } else {
                val nf = addFruit(nextIndex, mx, my)
                score += nf.points
            }
        }
        pendingMerges.clear()
    }

    private fun destroyFruit(f: SubakFruit) {
        fruits.remove(f)
        f.body.userData = null
        world.destroyBody(f.body)
    }

    private fun checkGameOver() {
        for (f in fruits) {
            val top = f.body.position.y - f.radius
            if (top < gameOverHeight) {
                val since = f.outOfBoundsSince
                if (since == null) {
                    f.outOfBoundsSince = engineMs
                } else if (engineMs - since > Subak.OUT_OF_BOUNDS_MS) {
                    gameOverFruitId = f.id
                    gameOver = true
                    return
                }
            } else {
                f.outOfBoundsSince = null
            }
        }
    }
}
