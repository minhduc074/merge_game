package com.bepngon.merge.game

/** Where a decor item is allowed to sit. PAINT items are not placed on the
 *  grid — buying one instantly covers the whole matching surface. */
enum class Placement { FLOOR, WALL, PAINT }

/** Draw order + collision group: items collide only inside their own layer,
 *  so a rug slides under furniture and wall art never blocks the sofa. */
enum class DecorLayer { FLOOR_PAINT, WALL_PAINT, RUG, FURNITURE, WALL_DECOR, SURFACE_DECOR }

enum class DecorStyle { AM_CUNG, HIEN_DAI, CO_DIEN }

enum class DecorRarity { COMMON, RARE }

/** One purchasable look of a [DecorItem]. MVP renders [tint] as the item's
 *  tile backdrop + paint color; [artName] is reserved for sliced sprites. */
data class DecorVariant(
    val id: String,
    val name: String,
    val tint: Long,
    val style: DecorStyle,
    val rarity: DecorRarity = DecorRarity.COMMON,
    val priceCoins: Int = 0,
    val unlockLevel: Int = 1,
    val artName: String? = null,
)

data class DecorItem(
    val id: String,
    val name: String,
    val emoji: String,
    val category: String,
    val placement: Placement,
    val layer: DecorLayer,
    val w: Int,
    val h: Int,
    val canRotate: Boolean = true,
    val canFlip: Boolean = false,
    val variants: List<DecorVariant>,
) {
    fun variantOf(variantId: String): DecorVariant = variants.first { it.id == variantId }
}

data class PlacedDecor(
    val uid: Long,
    val itemId: String,
    val variantId: String,
    val gx: Int,
    val gy: Int,
    val rot: Int = 0,
    val flip: Boolean = false,
)

data class RoomDef(
    val id: String,
    val name: String,
    val cols: Int,
    val rows: Int,
    /** Top grid rows reserved for WALL items; floor area starts at [wallRows]. */
    val wallRows: Int,
    val defaultWall: Long,
    val defaultFloor: Long,
    val priceCoins: Int = 0,
    val unlockLevel: Int = 1,
) {
    val floorRows: Int get() = rows - wallRows
}

data class RoomSave(
    val version: Int = 1,
    val placed: List<PlacedDecor> = emptyList(),
    val floorPaint: String? = null,
    val wallPaint: String? = null,
)

data class DecorState(
    val rooms: Map<String, RoomSave> = mapOf(DecorEngine.DEFAULT_ROOM to RoomSave()),
    val ownedVariants: Set<String> = emptySet(),
    val nextPlacedUid: Long = 1,
)
