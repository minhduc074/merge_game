package com.bepngon.merge.game

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
