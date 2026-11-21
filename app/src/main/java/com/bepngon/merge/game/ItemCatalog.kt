package com.bepngon.merge.game

enum class Family(val tint: Long) {
    FRUIT(0xFFFFE0E6),
    BAKERY(0xFFFFE9C9),
    DRINK(0xFFD7F0FF),
    VIET(0xFFE3F2D9),
    GENERATOR(0xFFEFE3FF),
}

data class SpawnEntry(val itemId: String, val weight: Int)

data class ItemDef(
    val id: String,
    val name: String,
    val emoji: String,
    val family: Family,
    val tier: Int,
    val mergeResult: String?,
    val sellPrice: Int,
    val score: Int,
    val isGenerator: Boolean = false,
    val spawnTable: List<SpawnEntry> = emptyList(),
    val maxCharges: Int = 0,
)

object ItemCatalog {

    private fun MutableMap<String, ItemDef>.chain(
        prefix: String,
        family: Family,
        vararg items: Triple<String, String, Pair<Int, Int>>,
    ) {
        items.forEachIndexed { i, (emoji, name, nums) ->
            val id = "${prefix}_${i + 1}"
            val next = if (i < items.lastIndex) "${prefix}_${i + 2}" else null
            put(
                id, ItemDef(
                    id = id, name = name, emoji = emoji, family = family,
                    tier = i + 1, mergeResult = next,
                    sellPrice = nums.first, score = nums.second,
                )
            )
        }
    }

    private fun MutableMap<String, ItemDef>.generator(
        id: String, tier: Int, emoji: String, name: String,
        next: String?, sellPrice: Int, vararg table: Pair<String, Int>,
    ) {
        put(
            id, ItemDef(
                id = id, name = name, emoji = emoji, family = Family.GENERATOR,
                tier = tier, mergeResult = next, sellPrice = sellPrice, score = 0,
                isGenerator = true,
                spawnTable = table.map { SpawnEntry(it.first, it.second) },
                maxCharges = 8 * tier,
            )
        )
    }

    private val defs: Map<String, ItemDef> = buildMap {
        chain(
            "fruit", Family.FRUIT,
            Triple("🍒", "Cherry", 1 to 1),
            Triple("🍓", "Dâu tây", 2 to 3),
            Triple("🍊", "Cam", 5 to 6),
            Triple("🍎", "Táo đỏ", 10 to 12),
            Triple("🍇", "Nho", 20 to 25),
            Triple("🍉", "Dưa hấu", 40 to 50),
            Triple("🍍", "Dứa", 80 to 100),
            Triple("🥗", "Đĩa trái cây", 160 to 200),
            Triple("🍹", "Sinh tố", 320 to 400),
            Triple("🍰", "Bánh trái cây", 650 to 800),
            Triple("🎂", "Bánh VIP", 1300 to 1600),
        )
        chain(
            "bakery", Family.BAKERY,
            Triple("🌾", "Lúa mì", 1 to 1),
            Triple("🍞", "Bánh mì", 2 to 3),
            Triple("🥐", "Croissant", 5 to 6),
            Triple("🍪", "Cookie", 10 to 12),
            Triple("🍩", "Donut", 20 to 25),
            Triple("🧁", "Cupcake", 40 to 50),
            Triple("🥞", "Pancake", 80 to 100),
            Triple("🍰", "Bánh kem", 160 to 200),
            Triple("🎂", "Bánh sinh nhật", 320 to 400),
            Triple("👑", "Bánh hoàng gia", 650 to 800),
        )
        chain(
            "drink", Family.DRINK,
            Triple("💧", "Nước lọc", 1 to 1),
            Triple("🍵", "Trà nóng", 2 to 3),
            Triple("🥛", "Sữa tươi", 5 to 6),
            Triple("☕", "Cà phê", 10 to 12),
            Triple("🧋", "Trà sữa", 20 to 25),
            Triple("🥤", "Nước ép", 40 to 50),
            Triple("🍹", "Mocktail", 80 to 100),
            Triple("🍸", "Party drink", 160 to 200),
        )
        chain(
            "viet", Family.VIET,
            Triple("🍚", "Cơm trắng", 1 to 1),
            Triple("🥖", "Bánh mì kẹp", 3 to 4),
            Triple("🍜", "Phở bò", 8 to 10),
            Triple("🌯", "Gỏi cuốn", 15 to 20),
            Triple("🍲", "Bún chả", 30 to 40),
            Triple("🍱", "Mâm cơm", 60 to 80),
            Triple("🏆", "Tiệc VIP", 120 to 160),
        )

        generator(
            "gen_fruit_1", 1, "🧺", "Giỏ trái cây", "gen_fruit_2", 25,
            "fruit_1" to 75, "fruit_2" to 20, "fruit_3" to 5,
        )
        generator(
            "gen_fruit_2", 2, "🛒", "Xe trái cây", "gen_fruit_3", 60,
            "fruit_1" to 40, "fruit_2" to 35, "fruit_3" to 20, "fruit_4" to 5,
        )
        generator(
            "gen_fruit_3", 3, "🚚", "Xe tải trái cây", null, 150,
            "fruit_2" to 40, "fruit_3" to 35, "fruit_4" to 20, "fruit_5" to 5,
        )
        generator(
            "gen_bakery_1", 1, "📦", "Hộp bánh", "gen_bakery_2", 25,
            "bakery_1" to 75, "bakery_2" to 20, "bakery_3" to 5,
        )
        generator(
            "gen_bakery_2", 2, "🏪", "Tiệm bánh", "gen_bakery_3", 60,
            "bakery_1" to 40, "bakery_2" to 35, "bakery_3" to 20, "bakery_4" to 5,
        )
        generator(
            "gen_bakery_3", 3, "🏭", "Nhà máy bánh", null, 150,
            "bakery_2" to 40, "bakery_3" to 35, "bakery_4" to 20, "bakery_5" to 5,
        )
        generator(
            "gen_drink_1", 1, "🧃", "Máy pha chế", "gen_drink_2", 30,
            "drink_1" to 70, "drink_2" to 25, "drink_3" to 5,
        )
        generator(
            "gen_drink_2", 2, "🧊", "Tủ lạnh mini", "gen_drink_3", 70,
            "drink_1" to 35, "drink_2" to 40, "drink_3" to 20, "drink_4" to 5,
        )
        generator(
            "gen_drink_3", 3, "🍶", "Quầy bar", null, 160,
            "drink_2" to 40, "drink_3" to 35, "drink_4" to 20, "drink_5" to 5,
        )
        generator(
            "gen_viet_1", 1, "🍳", "Bếp nhỏ", "gen_viet_2", 40,
            "viet_1" to 70, "viet_2" to 25, "viet_3" to 5,
        )
        generator(
            "gen_viet_2", 2, "🔥", "Bếp lớn", "gen_viet_3", 90,
            "viet_1" to 35, "viet_2" to 40, "viet_3" to 20, "viet_4" to 5,
        )
        generator(
            "gen_viet_3", 3, "🏮", "Nhà hàng", null, 200,
            "viet_2" to 40, "viet_3" to 35, "viet_4" to 20, "viet_5" to 5,
        )
    }

    operator fun get(id: String): ItemDef =
        defs[id] ?: error("Unknown item id: $id")

    fun mergeResultOf(item: BoardItem): ItemDef? =
        get(item.defId).mergeResult?.let { get(it) }

    fun allInFamilies(families: Set<Family>): List<ItemDef> =
        defs.values.filter { it.family in families && !it.isGenerator }

    fun generatorsFor(itemDefId: String): List<ItemDef> =
        defs.values.filter { it.isGenerator && it.spawnTable.any { s -> s.itemId == itemDefId } }

    /** The lower-tier def that merges into [defId], null for tier-1 items. */
    fun previousOf(defId: String): ItemDef? =
        defs.values.firstOrNull { it.mergeResult == defId }

    /** Full merge chain containing [defId], ordered tier-1 -> max. */
    fun chainOf(defId: String): List<ItemDef> {
        var first = get(defId)
        while (true) {
            first = previousOf(first.id) ?: break
        }
        val out = mutableListOf<ItemDef>()
        var cur: ItemDef? = first
        while (cur != null) {
            out += cur
            cur = cur.mergeResult?.let { get(it) }
        }
        return out
    }
}
