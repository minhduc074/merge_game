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
        topDown: Boolean = false,
        tex: String? = null,
        panel: Boolean = false,
    ) {
        put(
            id, DecorItem(
                id = id, name = name, emoji = emoji, category = category,
                placement = placement, layer = layer, w = w, h = h,
                canRotate = canRotate, canFlip = canFlip, topDown = topDown,
                tex = tex, panel = panel,
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
            "sofa_mint", "Sofa mây", "🛋️", "seating", Placement.FLOOR, DecorLayer.FURNITURE, 3, 2,
            V("mint", "Mint", 0xFFB2DFDB, 500, 3, DecorStyle.HIEN_DAI),
            V("xanh", "Xanh", 0xFF90CAF9, 650, 4),
            V("kem", "Kem", 0xFFFFF1D6, 800, 6, DecorStyle.CO_DIEN, rare = true),
        )
        item(
            "sofa_cat", "Sofa mèo", "🛋️", "seating", Placement.FLOOR, DecorLayer.FURNITURE, 3, 2,
            V("hong", "Hồng", 0xFFF8BBD0, 550, 3),
            V("kem", "Kem", 0xFFFFF1D6, 700, 5),
            V("xanh", "Xanh", 0xFF90CAF9, 850, 7, DecorStyle.CO_DIEN, rare = true),
        )
        item(
            "sofa_wood", "Sofa gỗ", "🛋️", "seating", Placement.FLOOR, DecorLayer.FURNITURE, 3, 2,
            V("go", "Gỗ", 0xFFD7A899, 450, 2),
            V("hong", "Hồng", 0xFFF8BBD0, 600, 4),
            V("xanh", "Xanh", 0xFF80CBC4, 750, 6, DecorStyle.HIEN_DAI),
        )
        item(
            "armchair_pink", "Ghế bành hồng", "🪑", "seating", Placement.FLOOR, DecorLayer.FURNITURE, 2, 1,
            V("hong", "Hồng", 0xFFF8BBD0, 200, 2),
            V("kem", "Kem", 0xFFFFF1D6, 280, 3),
            V("tim", "Tím", 0xFFCE93D8, 400, 5, DecorStyle.CO_DIEN),
        )
        item(
            "armchair_mauve", "Ghế bành tím", "🪑", "seating", Placement.FLOOR, DecorLayer.FURNITURE, 2, 1,
            V("tim", "Tím", 0xFFCE93D8, 220, 3),
            V("hong", "Hồng", 0xFFF8BBD0, 300, 4),
            V("xanh", "Xanh", 0xFF90CAF9, 420, 6),
        )
        item(
            "armchair_blue", "Ghế bành xanh", "🪑", "seating", Placement.FLOOR, DecorLayer.FURNITURE, 2, 1,
            V("xanh", "Xanh", 0xFF90CAF9, 250, 4, DecorStyle.HIEN_DAI),
            V("trang", "Trắng", 0xFFF5F0E6, 320, 5),
            V("vang", "Vàng", 0xFFFFD54F, 450, 7, DecorStyle.CO_DIEN),
        )
        item(
            "armchair_shell", "Ghế vỏ sò", "🪑", "seating", Placement.FLOOR, DecorLayer.FURNITURE, 2, 1,
            V("mint", "Mint", 0xFFB2DFDB, 280, 4),
            V("hong", "Hồng", 0xFFF8BBD0, 360, 6),
            V("tim", "Tím", 0xFFCE93D8, 500, 8, rare = true),
        )
        item(
            "chair_plaid", "Ghế caro", "🪑", "seating", Placement.FLOOR, DecorLayer.FURNITURE, 1, 1,
            V("go", "Gỗ", 0xFFBC8A5F, 0),
            V("xanh", "Xanh", 0xFF90CAF9, 100, 2),
            V("hong", "Hồng", 0xFFF8BBD0, 150, 4),
        )
        item(
            "chair_green", "Ghế xanh lá", "🪑", "seating", Placement.FLOOR, DecorLayer.FURNITURE, 1, 1,
            V("xanh", "Xanh", 0xFFA5D6A7, 60),
            V("go", "Gỗ", 0xFFBC8A5F, 90),
            V("vang", "Vàng", 0xFFFFD54F, 140, 3),
        )
        item(
            "ottoman_mint", "Đôn mây", "🪑", "seating", Placement.FLOOR, DecorLayer.FURNITURE, 1, 1,
            V("mint", "Mint", 0xFFB2DFDB, 80),
            V("hong", "Hồng", 0xFFF8BBD0, 120, 2),
            V("kem", "Kem", 0xFFFFF1D6, 160, 3),
        )
        item(
            "stool_cloud", "Đôn tròn", "🪑", "seating", Placement.FLOOR, DecorLayer.FURNITURE, 1, 1,
            V("dao", "Đào", 0xFFFFAB91, 70),
            V("mint", "Mint", 0xFFB2DFDB, 100),
            V("tim", "Tím", 0xFFCE93D8, 150, 3),
        )
        // ---- beds ----
        item(
            "bed_pink", "Giường hồng", "🛏️", "bed", Placement.FLOOR, DecorLayer.FURNITURE, 2, 3,
            V("hong", "Hồng", 0xFFF8BBD0, 500, 3),
            V("xanh", "Xanh", 0xFF90CAF9, 650, 5),
            V("kem", "Kem", 0xFFFFF1D6, 800, 7, DecorStyle.CO_DIEN),
        )
        item(
            "bed_iron", "Giường sắt", "🛏️", "bed", Placement.FLOOR, DecorLayer.FURNITURE, 2, 3,
            V("xanh", "Xanh", 0xFF90CAF9, 550, 4, DecorStyle.HIEN_DAI),
            V("trang", "Trắng", 0xFFF5F0E6, 680, 6),
            V("den", "Đen", 0xFF616161, 850, 8),
        )
        item(
            "bed_blue", "Giường xanh", "🛏️", "bed", Placement.FLOOR, DecorLayer.FURNITURE, 3, 2,
            V("xanh", "Xanh", 0xFF90CAF9, 600, 4),
            V("hong", "Hồng", 0xFFF8BBD0, 750, 6),
            V("vang", "Vàng", 0xFFFFD54F, 950, 8, DecorStyle.CO_DIEN, rare = true),
        )
        item(
            "loft_bed", "Giường tầng", "🛏️", "bed", Placement.FLOOR, DecorLayer.FURNITURE, 2, 3,
            V("go", "Gỗ", 0xFFBC8A5F, 900, 6),
            V("trang", "Trắng", 0xFFF5F0E6, 1100, 8, DecorStyle.HIEN_DAI),
            V("hong", "Hồng", 0xFFF8BBD0, 1400, 10, rare = true),
        )
        item(
            "bed_mint", "Giường mint", "🛏️", "bed", Placement.FLOOR, DecorLayer.FURNITURE, 3, 2,
            V("mint", "Mint", 0xFFB2DFDB, 650, 5),
            V("hong", "Hồng", 0xFFF8BBD0, 800, 7),
            V("tim", "Tím", 0xFFCE93D8, 1000, 9),
        )
        item(
            "bed_green", "Giường lá", "🛏️", "bed", Placement.FLOOR, DecorLayer.FURNITURE, 3, 2,
            V("xanh", "Xanh", 0xFFA5D6A7, 700, 6),
            V("do", "Đỏ", 0xFFE57373, 850, 8),
            V("tim", "Tím", 0xFFCE93D8, 1100, 10, DecorStyle.CO_DIEN, rare = true),
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
            "table_peach", "Bàn tròn đào", "🍽️", "table", Placement.FLOOR, DecorLayer.FURNITURE, 2, 2,
            V("dao", "Đào", 0xFFFFAB91, 160),
            V("mint", "Mint", 0xFFB2DFDB, 220, 2),
            V("go", "Gỗ", 0xFFBC8A5F, 300, 4),
            canRotate = false,
        )
        item(
            "table_cream", "Bàn tròn kem", "🍽️", "table", Placement.FLOOR, DecorLayer.FURNITURE, 2, 2,
            V("kem", "Kem", 0xFFFFF1D6, 200, 2),
            V("hong", "Hồng", 0xFFF8BBD0, 260, 4),
            V("go", "Gỗ", 0xFFBC8A5F, 340, 5),
            canRotate = false,
        )
        item(
            "table_mint", "Bàn vuông mint", "🍽️", "table", Placement.FLOOR, DecorLayer.FURNITURE, 2, 2,
            V("mint", "Mint", 0xFFB2DFDB, 180, 2),
            V("hong", "Hồng", 0xFFF8BBD0, 240, 4),
            V("xanh", "Xanh", 0xFF90CAF9, 320, 6),
            canRotate = false,
        )
        item(
            "bench_stripe", "Ghế dài sọc", "🪑", "table", Placement.FLOOR, DecorLayer.FURNITURE, 3, 1,
            V("go", "Gỗ", 0xFFBC8A5F, 150),
            V("xanh", "Xanh", 0xFF80CBC4, 200, 3),
            V("hong", "Hồng", 0xFFF8BBD0, 280, 5),
        )
        item(
            "side_table", "Tủ đầu giường", "🗄️", "table", Placement.FLOOR, DecorLayer.FURNITURE, 1, 1,
            V("go", "Gỗ", 0xFFBC8A5F, 90),
            V("trang", "Trắng", 0xFFF5F0E6, 130, 2),
            V("hong", "Hồng", 0xFFF8BBD0, 180, 4),
        )
        item(
            "vanity", "Bàn trang điểm", "🪞", "table", Placement.FLOOR, DecorLayer.FURNITURE, 2, 1,
            V("go", "Gỗ", 0xFFBC8A5F, 300, 3),
            V("trang", "Trắng", 0xFFF5F0E6, 380, 5, DecorStyle.HIEN_DAI),
            V("hong", "Hồng", 0xFFF8BBD0, 480, 7),
            canFlip = true,
        )
        item(
            "desk", "Bàn học", "📖", "table", Placement.FLOOR, DecorLayer.FURNITURE, 2, 1,
            V("go", "Gỗ", 0xFFBC8A5F, 240),
            V("xanh", "Xanh", 0xFF90CAF9, 320, 3),
            V("hong", "Hồng", 0xFFF8BBD0, 420, 5),
        )
        item(
            "cabinet_orange", "Tủ cam", "🗄️", "table", Placement.FLOOR, DecorLayer.FURNITURE, 1, 1,
            V("cam", "Cam", 0xFFFFAB91, 120),
            V("mint", "Mint", 0xFFB2DFDB, 160, 2),
            V("trang", "Trắng", 0xFFF5F0E6, 220, 4),
        )
        item(
            "cabinet_blue", "Tủ tivi xanh", "🗄️", "table", Placement.FLOOR, DecorLayer.FURNITURE, 2, 1,
            V("xanh", "Xanh", 0xFF90CAF9, 260, 3),
            V("trang", "Trắng", 0xFFF5F0E6, 340, 5),
            V("go", "Gỗ", 0xFFBC8A5F, 450, 7),
        )
        item(
            "dresser_cream", "Tủ ngăn kéo", "🗄️", "table", Placement.FLOOR, DecorLayer.FURNITURE, 2, 1,
            V("kem", "Kem", 0xFFFFF1D6, 280, 3),
            V("hong", "Hồng", 0xFFF8BBD0, 360, 5),
            V("go", "Gỗ", 0xFFBC8A5F, 460, 7),
        )
        item(
            "wardrobe_wood", "Tủ áo gỗ", "🚪", "table", Placement.FLOOR, DecorLayer.FURNITURE, 2, 1,
            V("go", "Gỗ", 0xFFBC8A5F, 400, 4),
            V("trang", "Trắng", 0xFFF5F0E6, 520, 6, DecorStyle.HIEN_DAI),
            V("nau", "Nâu đậm", 0xFF8D6E63, 680, 8, DecorStyle.CO_DIEN),
        )
        item(
            "cabinet_green", "Tủ xanh lá", "🗄️", "table", Placement.FLOOR, DecorLayer.FURNITURE, 1, 1,
            V("xanh", "Xanh", 0xFFA5D6A7, 150),
            V("trang", "Trắng", 0xFFF5F0E6, 200, 3),
            V("vang", "Vàng", 0xFFFFD54F, 280, 5),
        )
        item(
            "wardrobe_mint", "Tủ áo mint", "🚪", "table", Placement.FLOOR, DecorLayer.FURNITURE, 2, 1,
            V("mint", "Mint", 0xFFB2DFDB, 450, 5),
            V("xanh", "Xanh", 0xFF90CAF9, 580, 7),
            V("trang", "Trắng", 0xFFF5F0E6, 750, 9, DecorStyle.HIEN_DAI),
        )
        item(
            "shelf_mauve", "Kệ tím", "🗄️", "table", Placement.FLOOR, DecorLayer.FURNITURE, 2, 1,
            V("tim", "Tím", 0xFFCE93D8, 240, 3),
            V("hong", "Hồng", 0xFFF8BBD0, 320, 5),
            V("trang", "Trắng", 0xFFF5F0E6, 420, 7),
        )
        item(
            "wardrobe_ornate", "Tủ cổ điển", "🚪", "table", Placement.FLOOR, DecorLayer.FURNITURE, 2, 1,
            V("kem", "Kem", 0xFFFFF1D6, 700, 7, DecorStyle.CO_DIEN),
            V("go", "Gỗ", 0xFFBC8A5F, 850, 9),
            V("xanh", "Xanh", 0xFF90CAF9, 1100, 11, rare = true),
        )
        item(
            "shelf_wood", "Kệ gỗ cao", "🗄️", "table", Placement.FLOOR, DecorLayer.FURNITURE, 2, 1,
            V("go", "Gỗ", 0xFFBC8A5F, 260, 2),
            V("trang", "Trắng", 0xFFF5F0E6, 340, 4),
            V("nau", "Nâu", 0xFF8D6E63, 450, 6),
        )
        item(
            "bookshelf", "Tủ sách", "📚", "table", Placement.FLOOR, DecorLayer.FURNITURE, 2, 1,
            V("go", "Gỗ", 0xFFBC8A5F, 350, 4),
            V("trang", "Trắng", 0xFFF5F0E6, 460, 6),
            V("xanh", "Xanh", 0xFF80CBC4, 600, 8, DecorStyle.HIEN_DAI),
        )
        item(
            "shelf_flower", "Kệ hoa", "🌸", "table", Placement.FLOOR, DecorLayer.FURNITURE, 2, 1,
            V("kem", "Kem", 0xFFFFF1D6, 300, 4),
            V("hong", "Hồng", 0xFFF8BBD0, 400, 6),
            V("go", "Gỗ", 0xFFBC8A5F, 520, 8),
        )
        item(
            "ladder_shelf", "Kệ thang", "🗄️", "table", Placement.FLOOR, DecorLayer.FURNITURE, 1, 1,
            V("go", "Gỗ", 0xFFBC8A5F, 180),
            V("trang", "Trắng", 0xFFF5F0E6, 240, 3),
            V("xanh", "Xanh", 0xFFA5D6A7, 320, 5),
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
            topDown = true,
        )
        item(
            "rug_big", "Thảm lớn", "🟧", "decor", Placement.FLOOR, DecorLayer.RUG, 4, 3,
            V("cam", "Cam", 0xFFFFCC80, 260, 2),
            V("xam", "Xám", 0xFFB0BEC5, 320, 3, DecorStyle.HIEN_DAI),
            V("hoatiet", "Họa tiết", 0xFFCE93D8, 450, 5, DecorStyle.CO_DIEN, rare = true),
            topDown = true,
        )
        item(
            "plant_big", "Cây lớn", "🪴", "decor", Placement.FLOOR, DecorLayer.FURNITURE, 1, 1,
            V("xanh", "Xanh", 0xFF66BB6A, 200),
            V("lon", "Tán rộng", 0xFF388E3C, 280, 3),
            V("hoa", "Nở hoa", 0xFFF48FB1, 420, 6, rare = true),
        )
        item(
            "plant_basket", "Cây giỏ đan", "🪴", "decor", Placement.FLOOR, DecorLayer.FURNITURE, 1, 1,
            V("xanh", "Xanh", 0xFF66BB6A, 180),
            V("lon", "Tán lớn", 0xFF388E3C, 250, 3),
            V("vang", "Lá vàng", 0xFFFFD54F, 350, 5),
        )
        item(
            "plant_hang", "Cây treo", "🪴", "decor", Placement.FLOOR, DecorLayer.FURNITURE, 1, 1,
            V("xanh", "Xanh", 0xFF66BB6A, 120),
            V("tim", "Tím", 0xFFCE93D8, 180, 3),
            V("hong", "Hoa hồng", 0xFFF8BBD0, 260, 5),
        )
        item(
            "plant_stool", "Cây trên đôn", "🪴", "decor", Placement.FLOOR, DecorLayer.FURNITURE, 1, 1,
            V("xanh", "Xanh", 0xFF66BB6A, 150),
            V("hong", "Đôn hồng", 0xFFF8BBD0, 220, 3),
            V("go", "Đôn gỗ", 0xFFBC8A5F, 300, 5),
        )
        item(
            "plant_rack", "Giá cây", "🪴", "decor", Placement.FLOOR, DecorLayer.FURNITURE, 1, 1,
            V("go", "Gỗ", 0xFFBC8A5F, 250, 3),
            V("trang", "Trắng", 0xFFF5F0E6, 340, 5, DecorStyle.HIEN_DAI),
            V("xanh", "Xanh", 0xFF80CBC4, 450, 7),
        )
        item(
            "lamp_arc", "Đèn cong", "💡", "decor", Placement.FLOOR, DecorLayer.FURNITURE, 1, 1,
            V("mint", "Mint", 0xFFB2DFDB, 220, 3),
            V("hong", "Hồng", 0xFFF8BBD0, 300, 5),
            V("vang", "Vàng", 0xFFFFD54F, 400, 7, DecorStyle.CO_DIEN),
        )
        item(
            "lamp_cat", "Đèn mèo", "💡", "decor", Placement.FLOOR, DecorLayer.FURNITURE, 1, 1,
            V("hong", "Hồng", 0xFFF8BBD0, 260, 4),
            V("kem", "Kem", 0xFFFFF1D6, 340, 6),
            V("tim", "Tím", 0xFFCE93D8, 460, 8, rare = true),
        )
        item(
            "rug_oval", "Thảm oval", "🧶", "decor", Placement.FLOOR, DecorLayer.RUG, 3, 2,
            V("kem", "Kem", 0xFFFFF1D6, 160),
            V("hong", "Hồng", 0xFFF8BBD0, 220, 3),
            V("xanh", "Xanh", 0xFF90CAF9, 300, 5),
            topDown = true,
        )
        item(
            "rug_geo", "Thảm họa tiết", "🟧", "decor", Placement.FLOOR, DecorLayer.RUG, 4, 3,
            V("hong", "Hồng", 0xFFF8BBD0, 380, 4),
            V("xanh", "Xanh", 0xFF80CBC4, 480, 6, DecorStyle.HIEN_DAI),
            V("cam", "Cam", 0xFFFFCC80, 600, 8, DecorStyle.CO_DIEN),
            topDown = true,
        )
        item(
            "rug_cloud", "Thảm mây", "🧶", "decor", Placement.FLOOR, DecorLayer.RUG, 3, 2,
            V("hong", "Hồng", 0xFFF8BBD0, 200, 2),
            V("tim", "Tím", 0xFFCE93D8, 280, 4),
            V("kem", "Kem", 0xFFFFF1D6, 360, 6),
            topDown = true,
        )
        item(
            "easel", "Giá vẽ", "🎨", "decor", Placement.FLOOR, DecorLayer.FURNITURE, 1, 1,
            V("go", "Gỗ", 0xFFBC8A5F, 200, 3),
            V("trang", "Trắng", 0xFFF5F0E6, 280, 5),
            V("den", "Đen", 0xFF616161, 380, 7),
        )
        item(
            "guitar", "Đàn guitar", "🎸", "decor", Placement.FLOOR, DecorLayer.FURNITURE, 1, 1,
            V("go", "Gỗ", 0xFFBC8A5F, 350, 5),
            V("do", "Đỏ", 0xFFE57373, 460, 7),
            V("xanh", "Xanh", 0xFF90CAF9, 600, 9, DecorStyle.HIEN_DAI, rare = true),
        )
        item(
            "radio", "Radio cổ", "📻", "decor", Placement.FLOOR, DecorLayer.FURNITURE, 1, 1,
            V("hong", "Hồng", 0xFFF8BBD0, 280, 4),
            V("kem", "Kem", 0xFFFFF1D6, 360, 6),
            V("xanh", "Xanh", 0xFF80CBC4, 480, 8, DecorStyle.CO_DIEN),
        )
        item(
            "snow_globe", "Cầu tuyết", "🔮", "decor", Placement.FLOOR, DecorLayer.FURNITURE, 1, 1,
            V("xanh", "Xanh", 0xFF90CAF9, 320, 5),
            V("hong", "Hồng", 0xFFF8BBD0, 420, 7),
            V("vang", "Vàng", 0xFFFFD54F, 550, 9, rare = true),
        )
        item(
            "terrarium", "Tiểu cảnh", "🌿", "decor", Placement.FLOOR, DecorLayer.FURNITURE, 1, 1,
            V("xanh", "Xanh", 0xFF66BB6A, 240, 4),
            V("tim", "Tím", 0xFFCE93D8, 320, 6),
            V("hong", "Hồng", 0xFFF8BBD0, 420, 8),
        )
        // ---- fun ----
        item(
            "cat_tower", "Nhà mèo", "🐱", "fun", Placement.FLOOR, DecorLayer.FURNITURE, 1, 1,
            V("kem", "Kem", 0xFFFFF1D6, 300, 4),
            V("hong", "Hồng", 0xFFF8BBD0, 400, 6),
            V("xanh", "Xanh", 0xFF90CAF9, 520, 8),
        )
        item(
            "teepee", "Lều nhỏ", "⛺", "fun", Placement.FLOOR, DecorLayer.FURNITURE, 2, 2,
            V("kem", "Kem", 0xFFFFF1D6, 500, 6),
            V("hong", "Hồng", 0xFFF8BBD0, 650, 8),
            V("xanh", "Xanh", 0xFF80CBC4, 850, 10, rare = true),
        )
        item(
            "cat_sit", "Mèo ngồi", "🐱", "fun", Placement.FLOOR, DecorLayer.FURNITURE, 1, 1,
            V("cam", "Cam", 0xFFFFCC80, 250, 3),
            V("xam", "Xám", 0xFFB0BEC5, 350, 5),
            V("trang", "Trắng", 0xFFF5F0E6, 500, 7, rare = true),
        )
        item(
            "cat_sleep", "Mèo ngủ", "🐱", "fun", Placement.FLOOR, DecorLayer.FURNITURE, 1, 1,
            V("cam", "Cam", 0xFFFFCC80, 250, 3),
            V("hong", "Hồng", 0xFFF8BBD0, 350, 5),
            V("tim", "Tím", 0xFFCE93D8, 500, 7, rare = true),
        )
        // ---- wall ----
        item(
            "door_wood", "Cửa gỗ", "🚪", "wall", Placement.WALL, DecorLayer.WALL_DECOR, 2, 3,
            V("go", "Gỗ", 0xFFBC8A5F, 350, 3),
            V("trang", "Trắng", 0xFFF5F0E6, 480, 5, DecorStyle.HIEN_DAI),
            V("nau", "Nâu", 0xFF8D6E63, 650, 7, DecorStyle.CO_DIEN),
            canRotate = false,
        )
        item(
            "door_arch", "Cửa vòm", "🚪", "wall", Placement.WALL, DecorLayer.WALL_DECOR, 2, 3,
            V("kem", "Kem", 0xFFFFF1D6, 400, 4),
            V("hong", "Hồng", 0xFFF8BBD0, 540, 6),
            V("xanh", "Xanh", 0xFF80CBC4, 720, 8, DecorStyle.HIEN_DAI),
            canRotate = false,
        )
        item(
            "window_mint", "Cửa sổ mint", "🪟", "wall", Placement.WALL, DecorLayer.WALL_DECOR, 2, 2,
            V("mint", "Mint", 0xFFB2DFDB, 250, 3),
            V("trang", "Trắng", 0xFFF5F0E6, 340, 5),
            V("hong", "Hồng", 0xFFF8BBD0, 450, 7),
            canRotate = false,
        )
        item(
            "window_curtain", "Cửa sổ rèm", "🪟", "wall", Placement.WALL, DecorLayer.WALL_DECOR, 2, 3,
            V("xanh", "Xanh", 0xFF90CAF9, 380, 5),
            V("hong", "Hồng", 0xFFF8BBD0, 500, 7),
            V("kem", "Kem", 0xFFFFF1D6, 680, 9, DecorStyle.CO_DIEN),
            canRotate = false,
        )
        item(
            "wall_shelf_wood", "Kệ tường gỗ", "📚", "wall", Placement.WALL, DecorLayer.WALL_DECOR, 2, 1,
            V("go", "Gỗ", 0xFFBC8A5F, 180),
            V("trang", "Trắng", 0xFFF5F0E6, 250, 3),
            V("nau", "Nâu", 0xFF8D6E63, 340, 5),
            canRotate = false,
        )
        item(
            "wall_shelf_cream", "Kệ tường kem", "📚", "wall", Placement.WALL, DecorLayer.WALL_DECOR, 2, 1,
            V("kem", "Kem", 0xFFFFF1D6, 200, 2),
            V("hong", "Hồng", 0xFFF8BBD0, 280, 4),
            V("xanh", "Xanh", 0xFF80CBC4, 380, 6),
            canRotate = false,
        )
        item(
            "frame_hearts", "Khung ảnh tim", "🖼️", "wall", Placement.WALL, DecorLayer.WALL_DECOR, 2, 1,
            V("go", "Gỗ", 0xFFBC8A5F, 140),
            V("hong", "Hồng", 0xFFF8BBD0, 200, 3),
            V("vang", "Vàng", 0xFFFFD54F, 300, 5, DecorStyle.CO_DIEN),
            canRotate = false, canFlip = true,
        )
        item(
            "sign_dreams", "Bảng Sweet Dreams", "💤", "wall", Placement.WALL, DecorLayer.WALL_DECOR, 2, 1,
            V("kem", "Kem", 0xFFFFF1D6, 220, 4),
            V("hong", "Hồng", 0xFFF8BBD0, 300, 6),
            V("xanh", "Xanh", 0xFF90CAF9, 420, 8, rare = true),
            canRotate = false,
        )
        item(
            "banner_moon", "Cờ trăng", "🌙", "wall", Placement.WALL, DecorLayer.WALL_DECOR, 1, 2,
            V("kem", "Kem", 0xFFFFF1D6, 120),
            V("vang", "Vàng", 0xFFFFD54F, 180, 3),
            V("tim", "Tím", 0xFFCE93D8, 260, 5),
            canRotate = false,
        )
        item(
            "mirror_oval", "Gương oval", "🪞", "wall", Placement.WALL, DecorLayer.WALL_DECOR, 1, 2,
            V("vang", "Vàng", 0xFFFFD54F, 260, 4, DecorStyle.CO_DIEN),
            V("trang", "Trắng", 0xFFF5F0E6, 340, 6),
            V("hong", "Hồng", 0xFFF8BBD0, 460, 8),
            canRotate = false,
        )
        item(
            "mirror_ornate", "Gương cổ điển", "🪞", "wall", Placement.WALL, DecorLayer.WALL_DECOR, 2, 2,
            V("kem", "Kem", 0xFFFFF1D6, 420, 6, DecorStyle.CO_DIEN),
            V("vang", "Vàng", 0xFFFFD54F, 560, 8),
            V("hong", "Hồng", 0xFFF8BBD0, 750, 10, rare = true),
            canRotate = false,
        )
        item(
            "wall_cubby", "Hộc tường", "📦", "wall", Placement.WALL, DecorLayer.WALL_DECOR, 2, 1,
            V("go", "Gỗ", 0xFFBC8A5F, 160),
            V("kem", "Kem", 0xFFFFF1D6, 220, 3),
            V("hong", "Hồng", 0xFFF8BBD0, 300, 5),
            canRotate = false,
        )
        item(
            "plant_wall", "Cây tường", "🌿", "wall", Placement.WALL, DecorLayer.WALL_DECOR, 1, 1,
            V("xanh", "Xanh", 0xFF66BB6A, 100),
            V("lon", "Tán lớn", 0xFF388E3C, 150, 2),
            V("hong", "Hoa hồng", 0xFFF8BBD0, 220, 4),
            canRotate = false,
        )
        item(
            "garland", "Dây trang trí", "✨", "wall", Placement.WALL, DecorLayer.WALL_DECOR, 2, 1,
            V("vang", "Vàng", 0xFFFFD54F, 150),
            V("hong", "Hồng", 0xFFF8BBD0, 220, 3),
            V("xanh", "Xanh", 0xFF90CAF9, 300, 5),
            canRotate = false,
        )
        item(
            "poster", "Tranh nhỏ", "🖼️", "wall", Placement.WALL, DecorLayer.WALL_DECOR, 1, 2,
            V("kem", "Kem", 0xFFFFF1D6, 80),
            V("xanh", "Xanh", 0xFF90CAF9, 120, 2),
            V("hong", "Hồng", 0xFFF8BBD0, 180, 4),
            canRotate = false, canFlip = true,
        )
        item(
            "macrame", "Trang trí len", "🧶", "wall", Placement.WALL, DecorLayer.WALL_DECOR, 1, 2,
            V("kem", "Kem", 0xFFFFF1D6, 140, 2),
            V("be", "Be", 0xFFE8D0A9, 200, 4),
            V("xanh", "Xanh", 0xFF80CBC4, 280, 6),
            canRotate = false,
        )
        item(
            "wall_lamp", "Đèn tường", "💡", "wall", Placement.WALL, DecorLayer.WALL_DECOR, 1, 1,
            V("hong", "Hồng", 0xFFF8BBD0, 180, 3),
            V("vang", "Vàng", 0xFFFFD54F, 250, 5),
            V("xanh", "Xanh", 0xFF90CAF9, 340, 7),
            canRotate = false,
        )
        item(
            "cabinet_wall", "Tủ tường", "🗄️", "wall", Placement.WALL, DecorLayer.WALL_DECOR, 2, 1,
            V("mint", "Mint", 0xFFB2DFDB, 240, 4),
            V("kem", "Kem", 0xFFFFF1D6, 320, 6),
            V("xanh", "Xanh", 0xFF90CAF9, 440, 8),
            canRotate = false,
        )
        // ---- room shell: pattern + color tiles, applied whole-surface ----
        // Walls: shared pastel palette; `tex` = seamless line-art tile,
        // `panel` adds the wainscot band (giống ảnh tham chiếu của user).
        item(
            "wall_floral", "Hoa nhỏ", "🌸", "room", Placement.PAINT, DecorLayer.WALL_PAINT, 0, 0,
            V("tim", "Tím", 0xFFE6DBF0, 0),
            V("hong", "Hồng", 0xFFF9D8E0, 80),
            V("kem", "Kem", 0xFFFBEFD8, 80),
            V("mint", "Mint", 0xFFD8EEE3, 100),
            V("xanh", "Xanh", 0xFFD3E2F3, 100),
            V("la", "Lá", 0xFFDCE9CF, 100),
            canRotate = false, tex = "wtex_floral", panel = true,
        )
        item(
            "wall_gingham", "Caro", "🏁", "room", Placement.PAINT, DecorLayer.WALL_PAINT, 0, 0,
            V("hong", "Hồng", 0xFFF5CBD4, 300),
            V("tim", "Tím", 0xFFDFD1EE, 100),
            V("kem", "Kem", 0xFFF9E7C8, 100),
            V("mint", "Mint", 0xFFCBE8DB, 100),
            V("xanh", "Xanh", 0xFFC9DCF1, 100),
            canRotate = false, tex = "wtex_gingham",
        )
        item(
            "wall_dots", "Chấm bi", "🔵", "room", Placement.PAINT, DecorLayer.WALL_PAINT, 0, 0,
            V("mint", "Mint", 0xFFD8EEE3, 300),
            V("tim", "Tím", 0xFFE2D6F0, 100),
            V("hong", "Hồng", 0xFFF9D8E0, 100),
            V("kem", "Kem", 0xFFFBEFD8, 100),
            V("xanh", "Xanh", 0xFFD3E2F3, 100),
            canRotate = false, tex = "wtex_dots",
        )
        item(
            "wall_stripes", "Sọc dọc", "🎗️", "room", Placement.PAINT, DecorLayer.WALL_PAINT, 0, 0,
            V("hong", "Hồng", 0xFFF6D4D9, 250),
            V("kem", "Kem", 0xFFFBEFD8, 100),
            V("tim", "Tím", 0xFFE2D6F0, 100),
            V("mint", "Mint", 0xFFD8EEE3, 100),
            V("xanh", "Xanh", 0xFFD3E2F3, 100),
            canRotate = false, tex = "wtex_stripes",
        )
        // Floors: same idea — plank/checker/parquet over the tint base.
        item(
            "floor_wood", "Ván gỗ", "🪵", "room", Placement.PAINT, DecorLayer.FLOOR_PAINT, 0, 0,
            V("sang", "Gỗ sáng", 0xFFEFCDA3, 0),
            V("nau", "Gỗ nâu", 0xFFDFAF82, 100),
            V("kem", "Kem", 0xFFF4E3C6, 100),
            V("xam", "Xám", 0xFFDAD2C8, 120),
            V("hong", "Hồng", 0xFFF1D3D3, 120),
            canRotate = false, tex = "ftex_wood",
        )
        item(
            "floor_dots", "Chấm bi", "🔵", "room", Placement.PAINT, DecorLayer.FLOOR_PAINT, 0, 0,
            V("tim", "Tím", 0xFFE0D3EE, 300),
            V("mint", "Mint", 0xFFD3E9DC, 100),
            V("hong", "Hồng", 0xFFF4D5DA, 100),
            V("kem", "Kem", 0xFFF4E6CC, 100),
            V("xanh", "Xanh", 0xFFCEDDEF, 100),
            canRotate = false, tex = "ftex_dots",
        )
        item(
            "floor_checker", "Ô caro", "🏁", "room", Placement.PAINT, DecorLayer.FLOOR_PAINT, 0, 0,
            V("hong", "Hồng", 0xFFF4D5DA, 350),
            V("kem", "Kem", 0xFFF4E6CC, 120),
            V("tim", "Tím", 0xFFE0D3EE, 120),
            V("xanh", "Xanh", 0xFFCEDDEF, 120),
            canRotate = false, tex = "ftex_checker",
        )
        item(
            "floor_parquet", "Xương cá", "🪵", "room", Placement.PAINT, DecorLayer.FLOOR_PAINT, 0, 0,
            V("kem", "Kem", 0xFFF1E0C2, 400),
            V("nau", "Gỗ nâu", 0xFFDFAF82, 140),
            V("sang", "Gỗ sáng", 0xFFEFCDA3, 140),
            V("xam", "Xám", 0xFFDAD2C8, 140),
            canRotate = false, tex = "ftex_parquet",
        )
        // ---- legacy paint ids kept so old saves still resolve ----
        item(
            "paint_wall", "Sơn tường", "🎨", "legacy", Placement.PAINT, DecorLayer.WALL_PAINT, 0, 0,
            V("kem", "Kem", 0xFFFFF8E7, 0),
            V("mint", "Xanh mint", 0xFFE0F2F1, 200),
            V("hongdao", "Hồng đào", 0xFFFCE4EC, 300, 3),
            V("navy", "Xanh navy", 0xFFCFD8DC, 500, 6, DecorStyle.HIEN_DAI),
            canRotate = false,
        )
        item(
            "paint_floor", "Lát sàn", "🟫", "legacy", Placement.PAINT, DecorLayer.FLOOR_PAINT, 0, 0,
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
            defaultWall = "wall_floral_tim",
            defaultFloor = "floor_wood_sang",
        ),
    )

    val categories: List<Pair<String, String>> = listOf(
        "seating" to "Ghế",
        "bed" to "Giường",
        "table" to "Bàn & Kệ",
        "decor" to "Trang trí",
        "fun" to "Thú vui",
        "wall" to "Tường",
        "room" to "Phòng",
    )

    operator fun get(itemId: String): DecorItem =
        items[itemId] ?: error("Unknown decor id: $itemId")

    operator fun contains(itemId: String): Boolean = itemId in items

    fun room(roomId: String): RoomDef =
        rooms[roomId] ?: error("Unknown room id: $roomId")

    fun variant(variantId: String): DecorVariant =
        items.values.firstNotNullOf { item -> item.variants.firstOrNull { it.id == variantId } }

    fun variantOrNull(variantId: String): DecorVariant? =
        items.values.firstNotNullOfOrNull { item -> item.variants.firstOrNull { it.id == variantId } }

    fun itemOfVariant(variantId: String): DecorItem =
        items.values.first { item -> item.variants.any { it.id == variantId } }

    fun itemOfVariantOrNull(variantId: String): DecorItem? =
        items.values.firstOrNull { item -> item.variants.any { it.id == variantId } }

    fun inCategory(category: String): List<DecorItem> =
        items.values.filter { it.category == category }

    /** Price-0 variants are the starter kit — always usable without owning. */
    fun isOwned(decor: DecorState, variantId: String): Boolean =
        variant(variantId).priceCoins == 0 || variantId in decor.ownedVariants
}
