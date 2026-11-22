package com.bepngon.merge.game

/** Decor catalog — data-driven item list, same convention as [ItemCatalog]:
 *  adding furniture = one [item] call + its variants, no engine changes.
 *  A variant priced 0 is free and always usable (starter kit). */
object DecorCatalog {

    private data class V(
        val suffix: String,
        val name: String,
        val tint: Long,
        val price: Int,
        val level: Int = 1,
        val style: DecorStyle = DecorStyle.AM_CUNG,
        val rare: Boolean = false,
    )

    private fun MutableMap<String, DecorItem>.item(
        id: String,
        name: String,
        emoji: String,
        category: String,
        placement: Placement,
        layer: DecorLayer,
        w: Int,
        h: Int,
        vararg vars: V,
        canRotate: Boolean = true,
        canFlip: Boolean = false,
    ) {
        put(
            id, DecorItem(
                id = id, name = name, emoji = emoji, category = category,
                placement = placement, layer = layer, w = w, h = h,
                canRotate = canRotate, canFlip = canFlip,
                variants = vars.map { v ->
                    DecorVariant(
                        id = "${id}_${v.suffix}",
                        name = v.name,
                        tint = v.tint,
                        style = v.style,
                        rarity = if (v.rare) DecorRarity.RARE else DecorRarity.COMMON,
                        priceCoins = v.price,
                        unlockLevel = v.level,
                    )
                },
            ),
        )
    }

    private val items: Map<String, DecorItem> = buildMap {
        // ---- seating ----
        item(
            "chair_wood", "Ghế gỗ", "🪑", "seating", Placement.FLOOR, DecorLayer.FURNITURE, 1, 1,
            V("nau", "Nâu", 0xFFBC8A5F, 0),
            V("trang", "Trắng", 0xFFF5F0E6, 80, style = DecorStyle.HIEN_DAI),
            V("do", "Đỏ", 0xFFE57373, 120, 2),
        )
        item(
            "armchair", "Ghế bành", "🛋️", "seating", Placement.FLOOR, DecorLayer.FURNITURE, 2, 1,
            V("be", "Be", 0xFFE8D0A9, 150),
            V("mint", "Xanh mint", 0xFFA5D6A7, 200, 2, DecorStyle.HIEN_DAI),
            V("hong", "Hồng", 0xFFF8BBD0, 250, 3),
        )
        item(
            "sofa", "Sofa dài", "🛋️", "seating", Placement.FLOOR, DecorLayer.FURNITURE, 3, 2,
            V("kem", "Kem", 0xFFFFF1D6, 400, 2),
            V("xanh", "Xanh dương", 0xFF90CAF9, 550, 3, DecorStyle.HIEN_DAI),
            V("vang", "Vàng royal", 0xFFFFD54F, 900, 5, DecorStyle.CO_DIEN, rare = true),
        )
        // ---- table ----
        item(
            "table_cafe", "Bàn cà phê", "🍽️", "table", Placement.FLOOR, DecorLayer.FURNITURE, 2, 2,
            V("go", "Gỗ", 0xFFBC8A5F, 0),
            V("trang", "Trắng", 0xFFF5F0E6, 180, style = DecorStyle.HIEN_DAI),
            V("den", "Đen", 0xFF616161, 260, 4, DecorStyle.HIEN_DAI),
            canRotate = false,
        )
        item(
            "counter", "Quầy bánh", "🧁", "table", Placement.FLOOR, DecorLayer.FURNITURE, 4, 1,
            V("go", "Gỗ", 0xFFA9825A, 350, 2),
            V("dahoa", "Đá hoa", 0xFFCFD8DC, 500, 4, DecorStyle.HIEN_DAI),
            V("hong", "Hồng kem", 0xFFF8BBD0, 650, 6),
        )
        item(
            "shelf", "Kệ đồ", "🗄️", "table", Placement.FLOOR, DecorLayer.FURNITURE, 1, 2,
            V("go", "Gỗ", 0xFFBC8A5F, 220),
            V("trang", "Trắng", 0xFFF5F0E6, 300, style = DecorStyle.HIEN_DAI),
            V("xanh", "Xanh", 0xFF80CBC4, 380, 3),
        )
        // ---- decor ----
        item(
            "plant", "Chậu cây", "🪴", "decor", Placement.FLOOR, DecorLayer.FURNITURE, 1, 1,
            V("xanh", "Xanh", 0xFFA5D6A7, 0),
            V("lon", "Cây lớn", 0xFF66BB6A, 150),
            V("hoa", "Nở hoa", 0xFFF48FB1, 300, 3, rare = true),
        )
        item(
            "lamp", "Đèn sàn", "💡", "decor", Placement.FLOOR, DecorLayer.FURNITURE, 1, 1,
            V("vang", "Vàng ấm", 0xFFFFE082, 120),
            V("trang", "Trắng", 0xFFF5F0E6, 180, style = DecorStyle.HIEN_DAI),
            V("neon", "Neon", 0xFFB39DDB, 400, 5, DecorStyle.HIEN_DAI, rare = true),
        )
        item(
            "rug_round", "Thảm tròn", "🧶", "decor", Placement.FLOOR, DecorLayer.RUG, 3, 2,
            V("do", "Đỏ", 0xFFEF9A9A, 0),
            V("kem", "Kem", 0xFFFFF1D6, 140),
            V("xanh", "Xanh", 0xFF90CAF9, 200, 3, DecorStyle.HIEN_DAI),
        )
        item(
            "rug_big", "Thảm lớn", "🟧", "decor", Placement.FLOOR, DecorLayer.RUG, 4, 3,
            V("cam", "Cam", 0xFFFFCC80, 260, 2),
            V("xam", "Xám", 0xFFB0BEC5, 320, 3, DecorStyle.HIEN_DAI),
            V("hoatiet", "Họa tiết", 0xFFCE93D8, 450, 5, DecorStyle.CO_DIEN, rare = true),
        )
        // ---- wall ----
        item(
            "wall_art", "Tranh treo", "🖼️", "wall", Placement.WALL, DecorLayer.WALL_DECOR, 2, 1,
            V("hoa", "Hoa", 0xFFF8BBD0, 100),
            V("canh", "Phong cảnh", 0xFFA5D6A7, 160),
            V("meo", "Mèo", 0xFFFFCC80, 280, 4, rare = true),
            canRotate = false, canFlip = true,
        )
        item(
            "clock", "Đồng hồ", "🕰️", "wall", Placement.WALL, DecorLayer.WALL_DECOR, 1, 1,
            V("go", "Gỗ", 0xFFBC8A5F, 90),
            V("vang", "Vàng", 0xFFFFD54F, 150, 2, DecorStyle.CO_DIEN),
            V("neon", "Neon", 0xFF80DEEA, 320, 5, DecorStyle.HIEN_DAI, rare = true),
            canRotate = false,
        )
        item(
            "shelf_wall", "Kệ treo", "📚", "wall", Placement.WALL, DecorLayer.WALL_DECOR, 2, 1,
            V("go", "Gỗ", 0xFFBC8A5F, 140),
            V("trang", "Trắng", 0xFFF5F0E6, 220, style = DecorStyle.HIEN_DAI),
            V("cauvong", "Cầu vồng", 0xFFFFAB91, 350, 6),
            canRotate = false,
        )
        item(
            "window", "Rèm cửa sổ", "🪟", "wall", Placement.WALL, DecorLayer.WALL_DECOR, 2, 2,
            V("xanh", "Xanh", 0xFF90CAF9, 200, 2),
            V("hong", "Hồng", 0xFFF8BBD0, 260, 3),
            V("vang", "Vàng", 0xFFFFE082, 340, 4),
            canRotate = false,
        )
        // ---- paint: buying covers the whole surface, nothing is placed ----
        item(
            "paint_wall", "Sơn tường", "🎨", "paint", Placement.PAINT, DecorLayer.WALL_PAINT, 0, 0,
            V("kem", "Kem", 0xFFFFF8E7, 0),
            V("mint", "Xanh mint", 0xFFE0F2F1, 200),
            V("hongdao", "Hồng đào", 0xFFFCE4EC, 300, 3),
            V("navy", "Xanh navy", 0xFFCFD8DC, 500, 6, DecorStyle.HIEN_DAI),
            canRotate = false,
        )
        item(
            "paint_floor", "Lát sàn", "🟫", "paint", Placement.PAINT, DecorLayer.FLOOR_PAINT, 0, 0,
            V("gonau", "Gỗ nâu", 0xFFC08A5E, 0),
            V("gosang", "Gỗ sáng", 0xFFE0B88A, 180),
            V("caro", "Caro", 0xFFBCAAA4, 350, 4, DecorStyle.CO_DIEN),
            V("da", "Đá", 0xFFB0BEC5, 550, 6, DecorStyle.HIEN_DAI),
            canRotate = false,
        )
    }

    val rooms: Map<String, RoomDef> = mapOf(
        DecorEngine.DEFAULT_ROOM to RoomDef(
            id = DecorEngine.DEFAULT_ROOM,
            name = "Tiệm cà phê",
            cols = 9,
            rows = 12,
            wallRows = 3,
            defaultWall = 0xFFFFF8E7,
            defaultFloor = 0xFFC08A5E,
        ),
    )

    val categories: List<Pair<String, String>> = listOf(
        "seating" to "Ghế",
        "table" to "Bàn & Kệ",
        "decor" to "Trang trí",
        "wall" to "Tường",
        "paint" to "Sơn",
    )

    operator fun get(itemId: String): DecorItem =
        items[itemId] ?: error("Unknown decor id: $itemId")

    fun room(roomId: String): RoomDef =
        rooms[roomId] ?: error("Unknown room id: $roomId")

    fun variant(variantId: String): DecorVariant =
        items.values.firstNotNullOf { item -> item.variants.firstOrNull { it.id == variantId } }

    fun itemOfVariant(variantId: String): DecorItem =
        items.values.first { item -> item.variants.any { it.id == variantId } }

    fun inCategory(category: String): List<DecorItem> =
        items.values.filter { it.category == category }

    /** Price-0 variants are the starter kit — always usable without owning. */
    fun isOwned(decor: DecorState, variantId: String): Boolean =
        variant(variantId).priceCoins == 0 || variantId in decor.ownedVariants
}
