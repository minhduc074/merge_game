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

    /** Z-order inside the room Box: paint < rug < furniture < wall/surface
     *  decor, then back-to-front by iso depth (gx+gy grows toward the
     *  viewer in the corner room). */
    fun sortKey(p: PlacedDecor): Int {
        val item = DecorCatalog[p.itemId]
        return item.layer.ordinal * 100_000 + (p.gx + p.gy) * 100 + p.gx
    }

    fun roomOf(decor: DecorState, roomId: String): RoomSave =
        decor.rooms.getValue(roomId)

    /** Saved paint variant, or the room's default when unpainted. Variant
     *  ids saved by older versions resolve through the legacy catalog
     *  items; unknown ids fall back to the default instead of crashing. */
    fun paintVariant(saved: String?, defaultId: String): DecorVariant =
        saved?.let { DecorCatalog.variantOrNull(it) } ?: DecorCatalog.variant(defaultId)

    fun wallVariant(room: RoomSave, def: RoomDef): DecorVariant =
        paintVariant(room.wallPaint, def.defaultWall)

    fun floorVariant(room: RoomSave, def: RoomDef): DecorVariant =
        paintVariant(room.floorPaint, def.defaultFloor)

    fun skyVariant(room: RoomSave, def: RoomDef): DecorVariant =
        paintVariant(room.skyPaint, def.defaultSky)

    fun wallTint(decor: DecorState, roomId: String): Long {
        val def = DecorCatalog.room(roomId)
        return wallVariant(roomOf(decor, roomId), def).tint
    }

    fun floorTint(decor: DecorState, roomId: String): Long {
        val def = DecorCatalog.room(roomId)
        return floorVariant(roomOf(decor, roomId), def).tint
    }

    // ---------- validation ----------

    fun canPlace(
        decor: DecorState,
        roomId: String,
        itemId: String,
        gx: Int,
        gy: Int,
        rot: Int,
        ignoreUid: Long = 0,
    ): Boolean {
        val room = decor.rooms[roomId] ?: return false
        val def = DecorCatalog.room(roomId)
        val item = DecorCatalog[itemId]
        if (item.placement == Placement.PAINT) return false
        val (w, h) = footprint(item, rot)
        if (gx < 0 || gy < 0 || gx + w > def.cols || gy + h > def.rows) return false
        when (item.placement) {
            Placement.WALL -> if (gy + h > def.wallRows) return false
            Placement.FLOOR -> if (gy < def.wallRows) return false
            else -> {}
        }
        // Same layer = same collision group; rugs fit under furniture and
        // floor items never clash with things hanging on the wall band.
        // Wall items: rot encodes the wall side (0 = right, 90 = left) —
        // items on different walls never overlap even at the same (gx, gy).
        return room.placed.asSequence()
            .filter { it.uid != ignoreUid }
            .filter { DecorCatalog[it.itemId].layer == item.layer }
            .filter { item.placement != Placement.WALL || it.rot == rot }
            .none { overlaps(it, gx, gy, w, h) }
    }

    private fun overlaps(p: PlacedDecor, gx: Int, gy: Int, w: Int, h: Int): Boolean {
        val (pw, ph) = footprint(DecorCatalog[p.itemId], p.rot)
        return gx < p.gx + pw && p.gx < gx + w && gy < p.gy + ph && p.gy < gy + h
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
        if (!canPlace(state.decor, roomId, itemId, gx, gy, rot, 0)) {
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
        if (!canPlace(state.decor, roomId, p.itemId, gx, gy, rot, ignoreUid = uid)) {
            return state to listOf(GameEvent.InvalidDrop)
        }
        val nextRoom = room.copy(
            placed = room.placed.map {
                if (it.uid == uid) it.copy(gx = gx, gy = gy, rot = rot) else it
            },
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
            if (!canPlace(state.decor, roomId, p.itemId, p.gx, p.gy, newRot, ignoreUid = uid)) {
                return state to listOf(GameEvent.InvalidDrop)
            }
            room.copy(
                placed = room.placed.map { if (it.uid == uid) it.copy(rot = newRot) else it },
            )
        } else {
            room.copy(
                placed = room.placed.map {
                    if (it.uid == uid) it.copy(flip = !it.flip, rot = 0) else it
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

    /** Paint items cover a whole surface — buying one instantly applies it;
     *  owned paints can be re-applied freely. */
    fun setPaint(
        state: GameState,
        roomId: String,
        variantId: String,
    ): Pair<GameState, List<GameEvent>> {
        val item = DecorCatalog.itemOfVariant(variantId)
        if (item.placement != Placement.PAINT) return state to emptyList()
        val v = item.variantOf(variantId)
        failReason(state, v)?.let { return state to listOf(it) }
        val events = mutableListOf<GameEvent>()
        var next = acquire(state, v, events)
        val room = roomOf(next.decor, roomId)
        val nextRoom = when (item.layer) {
            DecorLayer.WALL_PAINT -> room.copy(wallPaint = variantId)
            DecorLayer.SKY_PAINT -> room.copy(skyPaint = variantId)
            else -> room.copy(floorPaint = variantId)
        }
        next = withRoom(next, roomId, nextRoom)
        events += GameEvent.PaintApplied(roomId)
        return next to events
    }
}
