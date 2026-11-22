package com.bepngon.merge.game

/** Decor editor logic — pure functions on [GameState], same contract as
 *  [GameEngine]: `(state, args) -> Pair<GameState, List<GameEvent>>`.
 *  Placement validation lives here; the UI calls [canPlace] directly for
 *  ghost previews and routes every committed change through these ops so
 *  undo snapshots + saving stay consistent. */
object DecorEngine {

    const val DEFAULT_ROOM = "cafe"
    const val DECOR_SAVE_VERSION = 1

    // ---------- geometry ----------

    fun footprint(item: DecorItem, rot: Int): Pair<Int, Int> =
        if (item.topDown && rot % 180 != 0) item.h to item.w else item.w to item.h

    /** Z-order inside the room Box: layer buckets (paint < rug <
     *  furniture < wall/surface decor); within a bucket the list order
     *  wins — a stable sort keeps the most recently placed/moved item on
     *  top, so items stack visibly instead of sinking behind others. */
    fun sortKey(p: PlacedDecor): Int = DecorCatalog[p.itemId].layer.ordinal

    fun roomOf(decor: DecorState, roomId: String): RoomSave {
        val room = decor.rooms.getValue(roomId)
        // items dropped from the catalog (old saves) are filtered out so
        // lookups by id never crash; the next room op persists the clean list
        val kept = room.placed.filter { it.itemId in DecorCatalog }
        return if (kept.size == room.placed.size) room else room.copy(placed = kept)
    }

    /** Saved paint variant, or the room's default when unpainted. Variant
     *  ids saved by older versions resolve through the legacy catalog
     *  items; unknown ids fall back to the default instead of crashing. */
    fun paintVariant(saved: String?, defaultId: String): DecorVariant =
        saved?.let { DecorCatalog.variantOrNull(it) } ?: DecorCatalog.variant(defaultId)

    fun wallVariant(room: RoomSave, def: RoomDef): DecorVariant =
        paintVariant(room.wallPaint, def.defaultWall)

    /** [side]: 0 = right wall, 1 = left wall — same encoding as the IsoMap
     *  and placed-item rot. Per-side paint wins over the shared wallPaint. */
    fun wallVariant(room: RoomSave, def: RoomDef, side: Int): DecorVariant =
        paintVariant(
            (if (side == 0) room.wallPaintR else room.wallPaintL) ?: room.wallPaint,
            def.defaultWall,
        )

    fun floorVariant(room: RoomSave, def: RoomDef): DecorVariant =
        paintVariant(room.floorPaint, def.defaultFloor)

    fun wallTint(decor: DecorState, roomId: String): Long {
        val def = DecorCatalog.room(roomId)
        return wallVariant(roomOf(decor, roomId), def).tint
    }

    fun floorTint(decor: DecorState, roomId: String): Long {
        val def = DecorCatalog.room(roomId)
        return floorVariant(roomOf(decor, roomId), def).tint
    }

    // ---------- validation ----------

    /** Placement rules: bounds + zone only. Items are allowed to overlap
     *  each other freely — stacking is part of the decor fun. */
    fun canPlace(
        decor: DecorState,
        roomId: String,
        itemId: String,
        gx: Int,
        gy: Int,
        rot: Int,
    ): Boolean {
        val room = decor.rooms[roomId] ?: return false
        val def = DecorCatalog.room(roomId)
        val item = DecorCatalog[itemId]
        if (item.placement == Placement.PAINT) return false
        val (w, h) = footprint(item, rot)
        if (gx < 0 || gy < 0 || gx + w > def.cols || gy + h > def.rows) return false
        return when (item.placement) {
            Placement.WALL -> gy + h <= def.wallRows
            Placement.FLOOR -> gy >= def.wallRows
            else -> true
        }
    }

    /** Non-null when [v] cannot be acquired (level-gated or too pricey). */
    private fun failReason(state: GameState, v: DecorVariant): GameEvent? = when {
        DecorCatalog.isOwned(state.decor, v.id) -> null
        v.unlockLevel > state.level -> GameEvent.InvalidDrop
        state.coins < v.priceCoins -> GameEvent.NotEnoughCoins
        else -> null
    }

    /** Applies the purchase of [v] when not yet owned; caller already ran
     *  [failReason]. Returns the new state plus a VariantBought event. */
    private fun acquire(
        state: GameState,
        v: DecorVariant,
        events: MutableList<GameEvent>,
    ): GameState {
        if (DecorCatalog.isOwned(state.decor, v.id)) return state
        events += GameEvent.VariantBought(v.id, v.priceCoins)
        return state.copy(
            coins = state.coins - v.priceCoins,
            decor = state.decor.copy(
                ownedVariants = state.decor.ownedVariants + v.id,
            ),
        )
    }

    private fun withRoom(state: GameState, roomId: String, room: RoomSave): GameState =
        state.copy(decor = state.decor.copy(rooms = state.decor.rooms + (roomId to room)))

    // ---------- ops ----------

    /** Drop from the catalog: validates the spot, buys the variant on the
     *  fly when needed, then places. One atomic step so the ghost price tag
     *  and the final charge can never disagree. */
    fun placeItem(
        state: GameState,
        roomId: String,
        itemId: String,
        variantId: String,
        gx: Int,
        gy: Int,
        rot: Int,
    ): Pair<GameState, List<GameEvent>> {
        val item = DecorCatalog[itemId]
        val v = item.variantOf(variantId)
        if (!canPlace(state.decor, roomId, itemId, gx, gy, rot)) {
            return state to listOf(GameEvent.InvalidDrop)
        }
        failReason(state, v)?.let { return state to listOf(it) }
        val events = mutableListOf<GameEvent>()
        var next = acquire(state, v, events)
        val decor = next.decor
        val room = roomOf(decor, roomId)
        val placed = PlacedDecor(decor.nextPlacedUid, itemId, variantId, gx, gy, rot)
        next = next.copy(
            decor = decor.copy(
                rooms = decor.rooms +
                    (roomId to room.copy(placed = room.placed + placed)),
                nextPlacedUid = decor.nextPlacedUid + 1,
            ),
        )
        events += GameEvent.DecorPlaced(roomId, placed.uid)
        return next to events
    }

    fun movePlaced(
        state: GameState,
        roomId: String,
        uid: Long,
        gx: Int,
        gy: Int,
        rot: Int,
    ): Pair<GameState, List<GameEvent>> {
        val room = roomOf(state.decor, roomId)
        val p = room.placed.firstOrNull { it.uid == uid } ?: return state to emptyList()
        if (!canPlace(state.decor, roomId, p.itemId, gx, gy, rot)) {
            return state to listOf(GameEvent.InvalidDrop)
        }
        // moved item re-appends to the end of the list — the stable render
        // sort keeps it on top of anything it overlaps
        val nextRoom = room.copy(
            placed = room.placed.filterNot { it.uid == uid } +
                p.copy(gx = gx, gy = gy, rot = rot),
        )
        return withRoom(state, roomId, nextRoom) to
            listOf(GameEvent.DecorPlaced(roomId, uid))
    }

    /** Picks a placed item back up — ownership of the variant is kept, so
     *  re-placing later is free. */
    fun removePlaced(
        state: GameState,
        roomId: String,
        uid: Long,
    ): Pair<GameState, List<GameEvent>> {
        val room = roomOf(state.decor, roomId)
        if (room.placed.none { it.uid == uid }) return state to emptyList()
        val nextRoom = room.copy(placed = room.placed.filterNot { it.uid == uid })
        return withRoom(state, roomId, nextRoom) to
            listOf(GameEvent.DecorRemoved(roomId, uid))
    }

    fun rotatePlaced(
        state: GameState,
        roomId: String,
        uid: Long,
    ): Pair<GameState, List<GameEvent>> {
        val room = roomOf(state.decor, roomId)
        val p = room.placed.firstOrNull { it.uid == uid } ?: return state to emptyList()
        val item = DecorCatalog[p.itemId]
        if (!item.canRotate && !item.canFlip) {
            return state to listOf(GameEvent.InvalidDrop)
        }
        // Oblique sprites look wrong turned 90° — for them "rotate" means
        // mirror. Only top-down art (rugs) gets a real bitmap rotation.
        val nextRoom = if (item.topDown && item.placement == Placement.FLOOR) {
            val newRot = (p.rot + 90) % 360
            if (!canPlace(state.decor, roomId, p.itemId, p.gx, p.gy, newRot)) {
                return state to listOf(GameEvent.InvalidDrop)
            }
            room.copy(
                placed = room.placed.map { if (it.uid == uid) it.copy(rot = newRot) else it },
            )
        } else {
            // keep rot — wall items encode their side there, so mirroring
            // must not teleport them to the other wall
            room.copy(
                placed = room.placed.map {
                    if (it.uid == uid) it.copy(flip = !it.flip) else it
                },
            )
        }
        return withRoom(state, roomId, nextRoom) to
            listOf(GameEvent.DecorPlaced(roomId, uid))
    }

    fun flipPlaced(
        state: GameState,
        roomId: String,
        uid: Long,
    ): Pair<GameState, List<GameEvent>> {
        val room = roomOf(state.decor, roomId)
        val p = room.placed.firstOrNull { it.uid == uid } ?: return state to emptyList()
        if (!DecorCatalog[p.itemId].canFlip) return state to listOf(GameEvent.InvalidDrop)
        val nextRoom = room.copy(
            placed = room.placed.map { if (it.uid == uid) it.copy(flip = !it.flip) else it },
        )
        return withRoom(state, roomId, nextRoom) to
            listOf(GameEvent.DecorPlaced(roomId, uid))
    }

    fun setVariant(
        state: GameState,
        roomId: String,
        uid: Long,
        variantId: String,
    ): Pair<GameState, List<GameEvent>> {
        val room = roomOf(state.decor, roomId)
        val p = room.placed.firstOrNull { it.uid == uid } ?: return state to emptyList()
        val v = DecorCatalog[p.itemId].variantOf(variantId)
        failReason(state, v)?.let { return state to listOf(it) }
        val events = mutableListOf<GameEvent>()
        var next = acquire(state, v, events)
        val nextRoom = roomOf(next.decor, roomId).copy(
            placed = room.placed.map { if (it.uid == uid) it.copy(variantId = variantId) else it },
        )
        next = withRoom(next, roomId, nextRoom)
        events += GameEvent.DecorPlaced(roomId, uid)
        return next to events
    }

    /** Re-stack a placed item inside its own layer: [front] = top of the
     *  layer ("Trước"), otherwise bottom ("Sau"). */
    fun reorderPlaced(
        state: GameState,
        roomId: String,
        uid: Long,
        front: Boolean,
    ): Pair<GameState, List<GameEvent>> {
        val room = roomOf(state.decor, roomId)
        val p = room.placed.firstOrNull { it.uid == uid } ?: return state to emptyList()
        val layer = DecorCatalog[p.itemId].layer
        val others = room.placed.filterNot { it.uid == uid }
        val newPlaced = if (front) {
            others + p
        } else {
            val idx = others.indexOfFirst { DecorCatalog[it.itemId].layer == layer }
            others.toMutableList().apply { add(if (idx < 0) size else idx, p) }
        }
        return withRoom(state, roomId, room.copy(placed = newPlaced)) to
            listOf(GameEvent.DecorPlaced(roomId, uid))
    }

    /** Cycle to the next owned variant of the same item ("Đổi mẫu") —
     *  unowned looks are skipped so the free button never charges. */
    fun cycleVariant(
        state: GameState,
        roomId: String,
        uid: Long,
    ): Pair<GameState, List<GameEvent>> {
        val room = roomOf(state.decor, roomId)
        val p = room.placed.firstOrNull { it.uid == uid } ?: return state to emptyList()
        val item = DecorCatalog[p.itemId]
        val owned = item.variants.filter { DecorCatalog.isOwned(state.decor, it.id) }
        if (owned.size < 2) return state to listOf(GameEvent.InvalidDrop)
        val cur = owned.indexOfFirst { it.id == p.variantId }
        val next = owned[(cur + 1) % owned.size]
        return setVariant(state, roomId, uid, next.id)
    }

    /** Tap-to-place: find the free cell nearest the zone center and drop
     *  the item there — same atomic buy+place as a manual drop. */
    fun placeAuto(
        state: GameState,
        roomId: String,
        itemId: String,
        variantId: String,
    ): Pair<GameState, List<GameEvent>> {
        val item = DecorCatalog[itemId]
        if (item.placement == Placement.PAINT) return state to listOf(GameEvent.InvalidDrop)
        val def = DecorCatalog.room(roomId)
        val (w, h) = footprint(item, 0)
        val gxRange = 0..(def.cols - w)
        val gyRange = when (item.placement) {
            Placement.WALL -> 0..(def.wallRows - h)
            else -> def.wallRows..(def.rows - h)
        }
        val cx = gxRange.last / 2f
        val cy = (gyRange.first + gyRange.last) / 2f
        val cell = gxRange.flatMap { gx -> gyRange.map { gy -> gx to gy } }
            .minByOrNull { (gx, gy) ->
                val dx = gx - cx
                val dy = gy - cy
                dx * dx + dy * dy
            }
            ?.takeIf { (gx, gy) ->
                canPlace(state.decor, roomId, itemId, gx, gy, 0)
            }
            ?: return state to listOf(GameEvent.InvalidDrop)
        return placeItem(state, roomId, itemId, variantId, cell.first, cell.second, 0)
    }

    /** Resize a placed item's sprite — visual only, footprint unchanged. */
    fun setScale(
        state: GameState,
        roomId: String,
        uid: Long,
        scale: Float,
    ): Pair<GameState, List<GameEvent>> {
        val room = roomOf(state.decor, roomId)
        if (room.placed.none { it.uid == uid }) return state to emptyList()
        val s = scale.coerceIn(0.6f, 1.5f)
        val nextRoom = room.copy(
            placed = room.placed.map { if (it.uid == uid) it.copy(scale = s) else it },
        )
        return withRoom(state, roomId, nextRoom) to
            listOf(GameEvent.DecorPlaced(roomId, uid))
    }

    /** Paint items cover a whole surface — buying one instantly applies it;
     *  owned paints can be re-applied freely. */
    fun setPaint(
        state: GameState,
        roomId: String,
        variantId: String,
        wallSide: Int? = null,
    ): Pair<GameState, List<GameEvent>> {
        val item = DecorCatalog.itemOfVariant(variantId)
        if (item.placement != Placement.PAINT) return state to emptyList()
        val v = item.variantOf(variantId)
        failReason(state, v)?.let { return state to listOf(it) }
        val events = mutableListOf<GameEvent>()
        var next = acquire(state, v, events)
        val room = roomOf(next.decor, roomId)
        val nextRoom = when (item.layer) {
            // wallSide null = both walls: keep the shared field as the
            // base and clear per-side overrides
            DecorLayer.WALL_PAINT -> when (wallSide) {
                0 -> room.copy(wallPaintR = variantId)
                1 -> room.copy(wallPaintL = variantId)
                else -> room.copy(wallPaint = variantId, wallPaintL = null, wallPaintR = null)
            }
            else -> room.copy(floorPaint = variantId)
        }
        next = withRoom(next, roomId, nextRoom)
        events += GameEvent.PaintApplied(roomId)
        return next to events
    }
}
