package com.bepngon.merge.game

/** Mutable remote-tunable balance. Defaults mirror the bundled v1.1.x tuning;
 *  server config replaces these at runtime. Game code should read this object
 *  instead of hardcoded constants where remote balancing matters. */
object GameBalance {
    @Volatile var energyCap = 100
    @Volatile var energyPerSpawn = 1
    @Volatile var energyRegenMs = 20_000L
    @Volatile var genCooldownMs = 30_000L
    @Volatile var genRegenMs = 30_000L
    @Volatile var maxActiveOrders = 5
    @Volatile var orderIntervalMinMs = 6_000L
    @Volatile var orderIntervalMaxMs = 28_000L
    @Volatile var orderTierWindow = 3
    @Volatile var orderTierLevelSpan = 30
    @Volatile var rareOrderWeight = 1
    @Volatile var orderCountWeightOne = 40
    @Volatile var orderCountWeightTwo = 35
    @Volatile var orderCountWeightThree = 25
    @Volatile var xpBase = 60
    @Volatile var xpLinear = 15
    @Volatile var xpQuadratic = 5
    @Volatile var comboWindowMs = 30_000L
    @Volatile var comboMin = 3
    @Volatile var qteEnergy = 15
    @Volatile var qteCoins = 30
    @Volatile var miniEnergyBrackets: List<Pair<Int, Int>> = listOf(0 to 10, 100 to 25, 300 to 40, 700 to 60)
    @Volatile var blockEnergyBrackets: List<Pair<Int, Int>> = listOf(0 to 40, 800 to 65, 2500 to 100, 7000 to 160)

    fun reset() {
        energyCap = 100; energyPerSpawn = 1; energyRegenMs = 20_000L
        genCooldownMs = 30_000L; genRegenMs = 30_000L
        maxActiveOrders = 5; orderIntervalMinMs = 6_000L; orderIntervalMaxMs = 28_000L
        orderTierWindow = 3; orderTierLevelSpan = 30; rareOrderWeight = 1
        orderCountWeightOne = 40; orderCountWeightTwo = 35; orderCountWeightThree = 25
        xpBase = 60; xpLinear = 15; xpQuadratic = 5
        comboWindowMs = 30_000L; comboMin = 3; qteEnergy = 15; qteCoins = 30
        miniEnergyBrackets = listOf(0 to 10, 100 to 25, 300 to 40, 700 to 60)
        blockEnergyBrackets = listOf(0 to 40, 800 to 65, 2500 to 100, 7000 to 160)
    }
}
