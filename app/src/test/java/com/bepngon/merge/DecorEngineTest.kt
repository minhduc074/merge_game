package com.bepngon.merge

import com.bepngon.merge.game.DecorCatalog
import com.bepngon.merge.game.DecorEngine
import com.bepngon.merge.game.DecorState
import com.bepngon.merge.game.GameEngine
import com.bepngon.merge.game.GameEvent
import com.bepngon.merge.game.GameState
import com.bepngon.merge.game.PlacedDecor
import com.bepngon.merge.game.RoomSave
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

private fun stateWith(
    coins: Int = 0,
    level: Int = 1,
    owned: Set<String> = emptySet(),
    placed: List<PlacedDecor> = emptyList(),
): GameState {
    val decor = DecorState(
        rooms = mapOf(DecorEngine.DEFAULT_ROOM to RoomSave(placed = placed)),
        ownedVariants = owned,
        nextPlacedUid = (placed.maxOfOrNull { it.uid } ?: 0) + 1,
    )
    return GameEngine.initialState(Random(1)).copy(
        coins = coins,
        level = level,
        decor = decor,
    )
}

private fun placed(
    uid: Long,
    itemId: String,
    gx: Int,
    gy: Int,
    rot: Int = 0,
    variantId: String = DecorCatalog[itemId].variants.first().id,
) = PlacedDecor(uid, itemId, variantId, gx, gy, rot)

class DecorEngineTest {

    private val room = DecorEngine.DEFAULT_ROOM
    private val def = DecorCatalog.room(room)

    @Test
    fun `canPlace rejects out-of-bounds and wrong zones`() {
        val state = stateWith()
        // chair 1x1 — fine on the floor
        assertTrue(DecorEngine.canPlace(state.decor, room, "chair_wood", 0, def.wallRows, 0))
        assertTrue(
            DecorEngine.canPlace(
                state.decor, room, "chair_wood", def.cols - 1, def.rows - 1, 0,
            ),
        )
        // outside the grid
        assertFalse(DecorEngine.canPlace(state.decor, room, "chair_wood", -1, def.wallRows, 0))
        assertFalse(DecorEngine.canPlace(state.decor, room, "chair_wood", def.cols, def.wallRows, 0))
        // floor item inside the wall band
        assertFalse(DecorEngine.canPlace(state.decor, room, "chair_wood", 0, 0, 0))
        // wall item below the wall band
        assertFalse(DecorEngine.canPlace(state.decor, room, "wall_art", 0, def.wallRows, 0))
        assertTrue(DecorEngine.canPlace(state.decor, room, "wall_art", 0, 0, 0))
        // sofa 3x2 sticking out the right edge
        assertFalse(
            DecorEngine.canPlace(
                state.decor, room, "sofa", def.cols - 2, def.wallRows, 0,
            ),
        )
    }

    @Test
    fun `canPlace allows overlapping items`() {
        val state = stateWith(
            placed = listOf(placed(1, "sofa", 0, 4)),
        )
        // stacking onto the sofa's cells -> allowed (any layer)
        assertTrue(DecorEngine.canPlace(state.decor, room, "table_cafe", 0, 4, 0))
        assertTrue(DecorEngine.canPlace(state.decor, room, "chair_wood", 2, 5, 0))
        assertTrue(DecorEngine.canPlace(state.decor, room, "rug_big", 0, 4, 0))
        // bounds + zones still enforced
        assertFalse(DecorEngine.canPlace(state.decor, room, "wall_art", 0, def.wallRows, 0))
        assertFalse(DecorEngine.canPlace(state.decor, room, "chair_wood", 0, 0, 0))
        assertFalse(DecorEngine.canPlace(state.decor, room, "chair_wood", def.cols, 5, 0))
    }

    @Test
    fun `placeItem buys and places atomically`() {
        val v = DecorCatalog["armchair"].variantOf("armchair_mint")
        val state = stateWith(coins = v.priceCoins + 10, level = v.unlockLevel)
        val (next, events) = DecorEngine.placeItem(
            state, room, "armchair", v.id, 0, def.wallRows, 0,
        )
        assertEquals(10, next.coins)
        assertTrue(v.id in next.decor.ownedVariants)
        val roomSave = next.decor.rooms.getValue(room)
        assertEquals(1, roomSave.placed.size)
        assertEquals(v.id, roomSave.placed[0].variantId)
        assertTrue(events.any { it is GameEvent.VariantBought })
        assertTrue(events.any { it is GameEvent.DecorPlaced })
    }

    @Test
    fun `placeItem with free variant costs nothing`() {
        val v = DecorCatalog["chair_wood"].variantOf("chair_wood_nau")
        val state = stateWith(coins = 0)
        val (next, events) = DecorEngine.placeItem(
            state, room, "chair_wood", v.id, 0, def.wallRows, 0,
        )
        assertEquals(0, next.coins)
        assertEquals(1, next.decor.rooms.getValue(room).placed.size)
        assertTrue(events.none { it is GameEvent.VariantBought })
    }

    @Test
    fun `placeItem fails without coins or on invalid spot`() {
        val v = DecorCatalog["armchair"].variantOf("armchair_be")
        var state = stateWith(coins = v.priceCoins - 1)
        var (next, events) = DecorEngine.placeItem(
            state, room, "armchair", v.id, 0, def.wallRows, 0,
        )
        assertEquals(listOf(GameEvent.NotEnoughCoins), events)
        assertEquals(state, next)

        // enough coins but the spot is out of bounds
        state = stateWith(
            coins = 999,
            placed = listOf(placed(1, "sofa", 0, 4)),
        )
        val (next2, events2) = DecorEngine.placeItem(
            state, room, "armchair", v.id, -1, def.wallRows, 0,
        )
        assertEquals(listOf(GameEvent.InvalidDrop), events2)
        assertEquals(state, next2)
    }

    @Test
    fun `movePlaced validates and keeps position on invalid drop`() {
        val p = placed(1, "chair_wood", 0, 4)
        val state = stateWith(placed = listOf(p, placed(2, "table_cafe", 3, 4)))
        // valid move
        val (next, _) = DecorEngine.movePlaced(state, room, 1, 0, 5, 0)
        assertEquals(5, next.decor.rooms.getValue(room).placed[0].gy)
        // moving out of bounds -> rejected, state untouched
        val (next2, events2) = DecorEngine.movePlaced(next, room, 1, def.cols, 5, 0)
        assertEquals(listOf(GameEvent.InvalidDrop), events2)
        assertEquals(5, next2.decor.rooms.getValue(room).placed[0].gy)
    }

    @Test
    fun `rotate swaps footprint for top-down items`() {
        // rug_big 4x3 top-down at (0,4); overlapping another rug is fine
        val p = placed(1, "rug_big", 0, 4)
        val state = stateWith(placed = listOf(p, placed(2, "rug_round", 0, 6)))
        val (rotated, ev) = DecorEngine.rotatePlaced(state, room, 1)
        assertTrue(ev.any { it is GameEvent.DecorPlaced })
        assertEquals(90, rotated.decor.rooms.getValue(room).placed[0].rot)
    }

    @Test
    fun `rotate mirrors oblique items instead of turning the sprite`() {
        // furniture art would look wrong rotated 90° — "Xoay" mirrors it
        val state = stateWith(placed = listOf(placed(1, "armchair", 0, 4)))
        val (next, ev) = DecorEngine.rotatePlaced(state, room, 1)
        assertTrue(ev.any { it is GameEvent.DecorPlaced })
        val p = next.decor.rooms.getValue(room).placed[0]
        assertTrue(p.flip)
        assertEquals(0, p.rot)
    }

    @Test
    fun `removePlaced drops the item`() {
        val state = stateWith(placed = listOf(placed(1, "chair_wood", 0, 4)))
        val (next, events) = DecorEngine.removePlaced(state, room, 1)
        assertTrue(next.decor.rooms.getValue(room).placed.isEmpty())
        assertTrue(events.any { it is GameEvent.DecorRemoved })
    }

    @Test
    fun `setVariant buys and swaps the look`() {
        val v = DecorCatalog["chair_wood"].variantOf("chair_wood_trang")
        val p = placed(1, "chair_wood", 0, 4)
        val state = stateWith(coins = v.priceCoins, placed = listOf(p))
        val (next, events) = DecorEngine.setVariant(state, room, 1, v.id)
        assertEquals(0, next.coins)
        assertEquals(v.id, next.decor.rooms.getValue(room).placed[0].variantId)
        assertTrue(events.any { it is GameEvent.VariantBought })
    }

    @Test
    fun `setScale resizes the sprite and clamps the range`() {
        val p = placed(1, "chair_wood", 0, 4)
        val state = stateWith(placed = listOf(p))
        val (big, ev) = DecorEngine.setScale(state, room, 1, 1.25f)
        assertEquals(1.25f, big.decor.rooms.getValue(room).placed[0].scale)
        assertTrue(ev.any { it is GameEvent.DecorPlaced })
        val (clampHi, _) = DecorEngine.setScale(state, room, 1, 9f)
        assertEquals(1.5f, clampHi.decor.rooms.getValue(room).placed[0].scale)
        val (clampLo, _) = DecorEngine.setScale(state, room, 1, 0.1f)
        assertEquals(0.6f, clampLo.decor.rooms.getValue(room).placed[0].scale)
        // unknown uid -> no-op
        val (same, ev2) = DecorEngine.setScale(state, room, 99, 1.25f)
        assertSame(state, same)
        assertTrue(ev2.isEmpty())
    }

    @Test
    fun `setPaint covers the right surface`() {
        val wall = DecorCatalog["paint_wall"].variantOf("paint_wall_mint")
        val floor = DecorCatalog["paint_floor"].variantOf("paint_floor_da")
        var state = stateWith(coins = wall.priceCoins + floor.priceCoins, level = 6)
        val (s1, e1) = DecorEngine.setPaint(state, room, wall.id)
        assertEquals(wall.id, s1.decor.rooms.getValue(room).wallPaint)
        assertEquals(floor.priceCoins, s1.coins)
        assertTrue(e1.any { it is GameEvent.PaintApplied })
        val (s2, _) = DecorEngine.setPaint(s1, room, floor.id)
        assertEquals(floor.id, s2.decor.rooms.getValue(room).floorPaint)
        assertEquals(0, s2.coins)

        // tint falls back to defaults when unpainted
        state = stateWith()
        assertEquals(
            DecorCatalog.variant(def.defaultWall).tint,
            DecorEngine.wallTint(state.decor, room),
        )
        assertEquals(
            DecorCatalog.variant(def.defaultFloor).tint,
            DecorEngine.floorTint(state.decor, room),
        )
        assertEquals(
            DecorCatalog.variant("paint_floor_da").tint,
            DecorEngine.floorTint(s2.decor, room),
        )
    }

    @Test
    fun `setPaint can target each wall side`() {
        val floral = DecorCatalog["wall_floral"].variants.first()
        val gingham = DecorCatalog["wall_gingham"].variants.first()
        var state = stateWith(coins = floral.priceCoins + gingham.priceCoins, level = 30)

        // paint only the left wall — the right one keeps the default
        val (s1, _) = DecorEngine.setPaint(state, room, floral.id, wallSide = 1)
        val r1 = s1.decor.rooms.getValue(room)
        assertEquals(floral.id, r1.wallPaintL)
        assertEquals(floral.id, DecorEngine.wallVariant(r1, def, 1).id)
        assertEquals(def.defaultWall, DecorEngine.wallVariant(r1, def, 0).id)

        // paint the right wall differently — left is untouched
        val (s2, _) = DecorEngine.setPaint(s1, room, gingham.id, wallSide = 0)
        val r2 = s2.decor.rooms.getValue(room)
        assertEquals(gingham.id, DecorEngine.wallVariant(r2, def, 0).id)
        assertEquals(floral.id, DecorEngine.wallVariant(r2, def, 1).id)

        // painting both clears the per-side overrides
        val (s3, _) = DecorEngine.setPaint(s2, room, floral.id)
        val r3 = s3.decor.rooms.getValue(room)
        assertEquals(floral.id, r3.wallPaint)
        assertEquals(null, r3.wallPaintL)
        assertEquals(null, r3.wallPaintR)
        assertEquals(floral.id, DecorEngine.wallVariant(r3, def, 0).id)
        assertEquals(floral.id, DecorEngine.wallVariant(r3, def, 1).id)
    }

    @Test
    fun `setPaint covers the sky surface too`() {
        val night = DecorCatalog["sky_night"].variants.first()
        var state = stateWith(coins = night.priceCoins + 50)
        val (next, events) = DecorEngine.setPaint(state, room, night.id)
        assertEquals(night.id, next.decor.rooms.getValue(room).skyPaint)
        assertEquals(50, next.coins)
        assertTrue(events.any { it is GameEvent.PaintApplied })
        // unpainted room -> default day sky
        assertEquals(
            DecorCatalog.variant(def.defaultSky).id,
            DecorEngine.skyVariant(stateWith().decor.rooms.getValue(room), def).id,
        )
    }

    @Test
    fun `level-gated variant cannot be bought early`() {
        val v = DecorCatalog["lamp"].variantOf("lamp_neon")
        assertTrue(v.unlockLevel > 1)
        val state = stateWith(coins = 999, level = 1)
        val (next, events) = DecorEngine.placeItem(
            state, room, "lamp", v.id, 0, def.wallRows, 0,
        )
        assertEquals(listOf(GameEvent.InvalidDrop), events)
        assertEquals(state, next)
    }

    @Test
    fun `placed uid counter never collides`() {
        var state = stateWith(coins = 0)
        repeat(3) {
            val (next, _) = DecorEngine.placeItem(
                state, room, "chair_wood", "chair_wood_nau", it, def.wallRows, 0,
            )
            state = next
        }
        val uids = state.decor.rooms.getValue(room).placed.map { it.uid }
        assertEquals(uids.size, uids.distinct().size)
        assertTrue(state.decor.nextPlacedUid > uids.max())
        assertNotEquals(uids[0], uids[1])
    }
}
