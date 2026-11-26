export type SpawnEntryConfig = { itemId: string; weight: number };
export type GeneratorConfig = { maxCharges: number; spawnTable: SpawnEntryConfig[] };
export type GameConfigPayload = {
  schemaVersion: number;
  minAppVersionCode?: number;
  generators: Record<string, GeneratorConfig>;
  energy: { cap: number; perSpawn: number; regenMs: number };
  generatorTiming: { cooldownMs: number; regenMs: number };
  orders: {
    intervalMinMs: number;
    intervalMaxMs: number;
    maxActive: number;
    tierWindow: number;
    tierLevelSpan: number;
    rareWeight: number;
    countWeights: { one: number; two: number; three: number };
  };
  xp: { base: number; linear: number; quadratic: number };
  rewards: {
    comboWindowMs: number;
    comboMin: number;
    qteEnergy: number;
    qteCoins: number;
    miniEnergyBrackets: Array<[number, number]>;
    blockEnergyBrackets: Array<[number, number]>;
  };
};

const chainIds = (prefix: string, count: number): string[] =>
  Array.from({ length: count }, (_, i) => `${prefix}_${i + 1}`);

export const VALID_ITEM_IDS = new Set([
  ...chainIds("fruit", 11),
  ...chainIds("bakery", 10),
  ...chainIds("drink", 8),
  ...chainIds("viet", 7),
  ...chainIds("rare_fruit", 3),
  ...chainIds("rare_bakery", 3),
  ...chainIds("rare_drink", 3),
  ...chainIds("rare_viet", 3),
  "magic_wild",
]);

export const GENERATOR_IDS = new Set([
  "gen_fruit_1", "gen_fruit_2", "gen_fruit_3",
  "gen_bakery_1", "gen_bakery_2", "gen_bakery_3",
  "gen_drink_1", "gen_drink_2", "gen_drink_3",
  "gen_viet_1", "gen_viet_2", "gen_viet_3",
]);

const gen = (maxCharges: number, table: Array<[string, number]>): GeneratorConfig => ({
  maxCharges,
  spawnTable: table.map(([itemId, weight]) => ({ itemId, weight })),
});

export const DEFAULT_GAME_CONFIG: GameConfigPayload = {
  schemaVersion: 2,
  minAppVersionCode: 5,
  generators: {
    gen_fruit_1: gen(6, [["fruit_1", 76], ["fruit_2", 22], ["rare_fruit_1", 2]]),
    gen_fruit_2: gen(12, [["fruit_1", 58], ["fruit_2", 27], ["fruit_3", 13], ["rare_fruit_1", 2]]),
    gen_fruit_3: gen(18, [["fruit_1", 50], ["fruit_2", 26], ["fruit_3", 14], ["fruit_4", 8], ["rare_fruit_1", 2]]),
    gen_bakery_1: gen(6, [["bakery_1", 76], ["bakery_2", 22], ["rare_bakery_1", 2]]),
    gen_bakery_2: gen(12, [["bakery_1", 58], ["bakery_2", 27], ["bakery_3", 13], ["rare_bakery_1", 2]]),
    gen_bakery_3: gen(18, [["bakery_1", 50], ["bakery_2", 26], ["bakery_3", 14], ["bakery_4", 8], ["rare_bakery_1", 2]]),
    gen_drink_1: gen(6, [["drink_1", 76], ["drink_2", 22], ["rare_drink_1", 2]]),
    gen_drink_2: gen(12, [["drink_1", 64], ["drink_2", 34], ["rare_drink_1", 2]]),
    gen_drink_3: gen(18, [["drink_1", 52], ["drink_2", 30], ["drink_3", 16], ["rare_drink_1", 2]]),
    gen_viet_1: gen(6, [["viet_1", 76], ["viet_2", 22], ["rare_viet_1", 2]]),
    gen_viet_2: gen(12, [["viet_1", 64], ["viet_2", 34], ["rare_viet_1", 2]]),
    gen_viet_3: gen(18, [["viet_1", 52], ["viet_2", 30], ["viet_3", 16], ["rare_viet_1", 2]]),
  },
  energy: { cap: 100, perSpawn: 1, regenMs: 60_000 },
  generatorTiming: { cooldownMs: 30_000, regenMs: 60_000 },
  orders: {
    intervalMinMs: 6_000,
    intervalMaxMs: 28_000,
    maxActive: 5,
    tierWindow: 3,
    tierLevelSpan: 30,
    rareWeight: 1,
    countWeights: { one: 50, two: 35, three: 15 },
  },
  xp: { base: 60, linear: 15, quadratic: 5 },
  rewards: {
    comboWindowMs: 30_000,
    comboMin: 3,
    qteEnergy: 15,
    qteCoins: 30,
    miniEnergyBrackets: [[0, 10], [100, 25], [300, 40], [700, 60]],
    blockEnergyBrackets: [[0, 40], [800, 65], [2500, 100], [7000, 160]],
  },
};

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function intIn(value: unknown, min: number, max: number): boolean {
  return Number.isInteger(value) && (value as number) >= min && (value as number) <= max;
}

function validateBrackets(value: unknown, path: string, errors: string[]) {
  if (!Array.isArray(value) || value.length === 0) {
    errors.push(`${path} must be a non-empty array`);
    return;
  }
  let previousFrom = -1;
  value.forEach((row, index) => {
    if (!Array.isArray(row) || row.length !== 2) {
      errors.push(`${path}[${index}] must be [fromPoints, rate]`);
      return;
    }
    const [from, rate] = row;
    if (!intIn(from, 0, 1_000_000) || !intIn(rate, 1, 10_000)) {
      errors.push(`${path}[${index}] has invalid numeric range`);
    }
    if ((from as number) <= previousFrom) {
      errors.push(`${path}[${index}] starting points must strictly increase`);
    }
    previousFrom = from as number;
  });
}

export function validateGameConfigPayload(payload: unknown): string[] {
  const errors: string[] = [];
  if (!isRecord(payload)) return ["payload must be an object"];
  if (!intIn(payload.schemaVersion, 1, 100)) errors.push("schemaVersion must be an integer 1..100");
  if (payload.minAppVersionCode !== undefined && !intIn(payload.minAppVersionCode, 1, 100_000)) {
    errors.push("minAppVersionCode must be an integer 1..100000");
  }

  if (!isRecord(payload.generators)) {
    errors.push("generators must be an object");
  } else {
    for (const [genId, genValue] of Object.entries(payload.generators)) {
      if (!GENERATOR_IDS.has(genId)) {
        errors.push(`generators.${genId}: unknown generator id`);
        continue;
      }
      if (!isRecord(genValue)) {
        errors.push(`generators.${genId} must be an object`);
        continue;
      }
      if (!intIn(genValue.maxCharges, 1, 60)) {
        errors.push(`generators.${genId}.maxCharges must be 1..60`);
      }
      if (!Array.isArray(genValue.spawnTable) || genValue.spawnTable.length === 0) {
        errors.push(`generators.${genId}.spawnTable must be a non-empty array`);
        continue;
      }
      let sum = 0;
      genValue.spawnTable.forEach((entryValue, index) => {
        if (!isRecord(entryValue)) {
          errors.push(`generators.${genId}.spawnTable[${index}] must be an object`);
          return;
        }
        if (typeof entryValue.itemId !== "string" || !VALID_ITEM_IDS.has(entryValue.itemId) || entryValue.itemId === "magic_wild") {
          errors.push(`generators.${genId}.spawnTable[${index}].itemId is unknown or not spawnable`);
        }
        if (!intIn(entryValue.weight, 0, 100)) {
          errors.push(`generators.${genId}.spawnTable[${index}].weight must be 0..100`);
        } else {
          sum += entryValue.weight as number;
        }
      });
      if (sum !== 100) errors.push(`generators.${genId}.spawnTable weights must sum to 100 (got ${sum})`);
    }
  }

  if (!isRecord(payload.energy)) {
    errors.push("energy must be an object");
  } else {
    if (!intIn(payload.energy.cap, 10, 1_000)) errors.push("energy.cap must be 10..1000");
    if (!intIn(payload.energy.perSpawn, 0, 20)) errors.push("energy.perSpawn must be 0..20");
    if (!intIn(payload.energy.regenMs, 5_000, 3_600_000)) errors.push("energy.regenMs must be 5s..1h");
  }

  if (!isRecord(payload.generatorTiming)) {
    errors.push("generatorTiming must be an object");
  } else {
    if (!intIn(payload.generatorTiming.cooldownMs, 5_000, 3_600_000)) errors.push("generatorTiming.cooldownMs must be 5s..1h");
    if (!intIn(payload.generatorTiming.regenMs, 5_000, 3_600_000)) errors.push("generatorTiming.regenMs must be 5s..1h");
  }

  if (!isRecord(payload.orders)) {
    errors.push("orders must be an object");
  } else {
    const orders = payload.orders;
    if (!intIn(orders.intervalMinMs, 2_000, 300_000)) errors.push("orders.intervalMinMs must be 2s..5m");
    if (!intIn(orders.intervalMaxMs, 2_000, 600_000)) errors.push("orders.intervalMaxMs must be 2s..10m");
    if (intIn(orders.intervalMinMs, 2_000, 300_000) && intIn(orders.intervalMaxMs, 2_000, 600_000) && (orders.intervalMinMs as number) > (orders.intervalMaxMs as number)) {
      errors.push("orders.intervalMinMs must be <= intervalMaxMs");
    }
    if (!intIn(orders.maxActive, 1, 10)) errors.push("orders.maxActive must be 1..10");
    if (!intIn(orders.tierWindow, 1, 10)) errors.push("orders.tierWindow must be 1..10");
    if (!intIn(orders.tierLevelSpan, 5, 100)) errors.push("orders.tierLevelSpan must be 5..100");
    if (!intIn(orders.rareWeight, 0, 100)) errors.push("orders.rareWeight must be 0..100");
    if (!isRecord(orders.countWeights)) {
      errors.push("orders.countWeights must be an object");
    } else {
      const cw = orders.countWeights;
      const allValid = intIn(cw.one, 0, 100) && intIn(cw.two, 0, 100) && intIn(cw.three, 0, 100);
      if (!allValid) errors.push("orders.countWeights.one/two/three must be 0..100");
      if (allValid && (cw.one as number) + (cw.two as number) + (cw.three as number) !== 100) {
        errors.push("orders.countWeights must sum to 100");
      }
    }
  }

  if (!isRecord(payload.xp)) {
    errors.push("xp must be an object");
  } else {
    if (!intIn(payload.xp.base, 1, 10_000)) errors.push("xp.base must be 1..10000");
    if (!intIn(payload.xp.linear, 0, 10_000)) errors.push("xp.linear must be 0..10000");
    if (!intIn(payload.xp.quadratic, 0, 10_000)) errors.push("xp.quadratic must be 0..10000");
  }

  if (!isRecord(payload.rewards)) {
    errors.push("rewards must be an object");
  } else {
    const rewards = payload.rewards;
    if (!intIn(rewards.comboWindowMs, 5_000, 600_000)) errors.push("rewards.comboWindowMs must be 5s..10m");
    if (!intIn(rewards.comboMin, 2, 20)) errors.push("rewards.comboMin must be 2..20");
    if (!intIn(rewards.qteEnergy, 0, 500)) errors.push("rewards.qteEnergy must be 0..500");
    if (!intIn(rewards.qteCoins, 0, 5_000)) errors.push("rewards.qteCoins must be 0..5000");
    validateBrackets(rewards.miniEnergyBrackets, "rewards.miniEnergyBrackets", errors);
    validateBrackets(rewards.blockEnergyBrackets, "rewards.blockEnergyBrackets", errors);
  }

  return errors;
}
