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
        DecorDef("decor_rug", "Thảm kẻ", "🟫", 50, 1, x = 0.28f, y = 0.78f, w = 0.44f),
        DecorDef("decor_plant", "Chậu cây", "🪴", 60, 1, x = 0.02f, y = 0.52f, w = 0.14f),
        DecorDef("decor_painting", "Tranh hoa", "🖼️", 80, 1, x = 0.40f, y = 0.08f, w = 0.20f),
        DecorDef("decor_clock", "Đồng hồ", "🕰️", 120, 2,
            genCooldownPct = 5, x = 0.66f, y = 0.08f, w = 0.12f),
        DecorDef("decor_lamp", "Đèn lồng", "🏮", 150, 2, x = 0.84f, y = 0.40f, w = 0.12f),
        DecorDef("decor_curtain", "Rèm cửa", "🪟", 180, 3, x = 0.06f, y = 0.06f, w = 0.26f),
        DecorDef("decor_table", "Bàn ghế gỗ", "🪑", 250, 3,
            orderCoinsPct = 2, x = 0.55f, y = 0.55f, w = 0.30f),
        DecorDef("decor_shelf", "Kệ bánh", "🍰", 300, 4, x = 0.40f, y = 0.30f, w = 0.22f),
        DecorDef("decor_flowers", "Chậu hoa lớn", "🌻", 350, 4,
            energyRegenPct = 5, x = 0.90f, y = 0.62f, w = 0.10f),
        DecorDef("decor_counter", "Quầy bar mới", "☕", 500, 5,
            orderCoinsPct = 3, x = 0.02f, y = 0.30f, w = 0.30f),
        DecorDef("decor_oven", "Lò nướng đá", "🔥", 600, 5,
            genCooldownPct = 5, x = 0.70f, y = 0.30f, w = 0.22f),
        DecorDef("decor_piano", "Piano mini", "🎹", 700, 6, x = 0.02f, y = 0.68f, w = 0.22f),
        DecorDef("decor_aquarium", "Bể cá", "🐠", 800, 6,
            energyRegenPct = 5, x = 0.76f, y = 0.52f, w = 0.16f),
        DecorDef("decor_goldcat", "Mèo thần tài", "🐈", 1000, 7,
            orderCoinsPct = 5, x = 0.30f, y = 0.52f, w = 0.12f),
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
