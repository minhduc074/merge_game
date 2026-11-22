package com.bepngon.merge.game

/** A decoration bought with coins and placed in the cafe room.
 *  Passive buff fields are percentages (positive = better). */
data class DecorDef(
    val id: String,
    val name: String,
    val emoji: String,
    val cost: Int,
    val unlockLevel: Int,
    val orderCoinsPct: Int = 0,
    val genCooldownPct: Int = 0,
    val energyRegenPct: Int = 0,
    /** Placement inside the room, as fractions of room size. */
    val x: Float,
    val y: Float,
    val w: Float,
)

object DecorCatalog {
    val ALL = listOf(
        // Positions derived from the AI-furnished room render (art_out/furnished_room.png):
        // the generator placed each item itself, so bought decor lands on a designed spot.
        DecorDef("decor_rug", "Thảm kẻ", "🟫", 50, 1, x = 0.20f, y = 0.65f, w = 0.61f),
        DecorDef("decor_plant", "Chậu cây", "🪴", 60, 1, x = 0.15f, y = 0.56f, w = 0.17f),
        DecorDef("decor_painting", "Tranh hoa", "🖼️", 80, 1, x = 0.34f, y = 0.26f, w = 0.16f),
        DecorDef("decor_clock", "Đồng hồ", "🕰️", 120, 2,
            genCooldownPct = 5, x = 0.61f, y = 0.26f, w = 0.18f),
        DecorDef("decor_lamp", "Đèn lồng", "🏮", 150, 2, x = 0.87f, y = 0.25f, w = 0.11f),
        DecorDef("decor_curtain", "Rèm cửa", "🪟", 180, 3, x = -0.03f, y = 0.28f, w = 0.29f),
        DecorDef("decor_table", "Bàn ghế gỗ", "🪑", 250, 3,
            orderCoinsPct = 2, x = 0.50f, y = 0.60f, w = 0.31f),
        DecorDef("decor_shelf", "Kệ bánh", "🍰", 300, 4, x = 0.46f, y = 0.49f, w = 0.26f),
        DecorDef("decor_flowers", "Chậu hoa lớn", "🌻", 350, 4,
            energyRegenPct = 5, x = 0.80f, y = 0.60f, w = 0.16f),
        DecorDef("decor_counter", "Quầy bar mới", "☕", 500, 5,
            orderCoinsPct = 3, x = 0.00f, y = 0.42f, w = 0.49f),
        DecorDef("decor_oven", "Lò nướng đá", "🔥", 600, 5,
            genCooldownPct = 5, x = 0.73f, y = 0.50f, w = 0.26f),
        DecorDef("decor_piano", "Piano mini", "🎹", 700, 6, x = -0.04f, y = 0.63f, w = 0.24f),
        DecorDef("decor_aquarium", "Bể cá", "🐠", 800, 6,
            energyRegenPct = 5, x = 0.90f, y = 0.64f, w = 0.12f),
        DecorDef("decor_goldcat", "Mèo thần tài", "🐈", 1000, 7,
            orderCoinsPct = 5, x = 0.36f, y = 0.58f, w = 0.15f),
    )

    fun byId(id: String): DecorDef = ALL.first { it.id == id }

    fun orderCoinsPct(owned: Set<String>): Int =
        owned.sumOf { byId(it).orderCoinsPct }

    fun genCooldownPct(owned: Set<String>): Int =
        owned.sumOf { byId(it).genCooldownPct }

    fun energyRegenPct(owned: Set<String>): Int =
        owned.sumOf { byId(it).energyRegenPct }
}

data class PetDef(
    /** Reuses the customer art id (cat, dog, ...). */
    val id: String,
    val cost: Int,
    val buffDesc: String,
    val buff: PetBuff,
) {
    val name: String get() = Customers.byId(id).name
    val emoji: String get() = Customers.byId(id).emoji
}

object PetCatalog {
    val ALL = listOf(
        PetDef("cat", 200, "+5% vàng order", PetBuff.OrderCoinsPct(5)),
        PetDef("dog", 200, "Gen hồi nhanh hơn 10%", PetBuff.GenCooldownPct(10)),
        PetDef("rabbit", 250, "+10% XP", PetBuff.XpPct(10)),
        PetDef("frog", 300, "Năng lượng hồi nhanh hơn 15%", PetBuff.EnergyRegenPct(15)),
        PetDef("pig", 350, "Buff kéo dài thêm 10 phút", PetBuff.FeedBonusMs(10 * 60_000L)),
        PetDef("fox", 400, "+8% vàng order", PetBuff.OrderCoinsPct(8)),
        PetDef("koala", 450, "Combo +10 giây", PetBuff.ComboBonusMs(10_000L)),
        PetDef("panda", 500, "Nhả 🎁 item mỗi ~4 giờ khi no", PetBuff.PeriodicGift),
    )

    fun byId(id: String): PetDef = ALL.first { it.id == id }
}
