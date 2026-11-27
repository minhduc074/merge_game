import { describe, expect, it } from "vitest";
import { DEFAULT_GAME_CONFIG, validateGameConfigPayload } from "../src/modules/game-config/config-schema";

describe("validateGameConfigPayload", () => {
  it("accepts the bundled default config", () => {
    expect(validateGameConfigPayload(DEFAULT_GAME_CONFIG)).toEqual([]);
  });

  it("rejects generator weights that do not sum to 100", () => {
    const payload = structuredClone(DEFAULT_GAME_CONFIG);
    payload.generators.gen_fruit_1.spawnTable[0].weight = 75;
    expect(validateGameConfigPayload(payload)).toContain(
      "generators.gen_fruit_1.spawnTable weights must sum to 100 (got 99)",
    );
  });

  it("rejects unknown generators and items", () => {
    const payload = structuredClone(DEFAULT_GAME_CONFIG) as unknown as Record<string, unknown>;
    payload.generators = {
      gen_unknown: { maxCharges: 6, spawnTable: [{ itemId: "fruit_1", weight: 100 }] },
    };
    expect(validateGameConfigPayload(payload)).toContain("generators.gen_unknown: unknown generator id");

    const badItem = structuredClone(DEFAULT_GAME_CONFIG);
    badItem.generators.gen_fruit_1.spawnTable[0].itemId = "unknown_item";
    expect(validateGameConfigPayload(badItem).join("\n")).toContain("itemId is unknown");
  });

  it("rejects invalid order tier window settings", () => {
    const payload = structuredClone(DEFAULT_GAME_CONFIG);
    payload.orders.tierWindow = 0;
    payload.orders.tierLevelSpan = 4;
    const errors = validateGameConfigPayload(payload);
    expect(errors).toContain("orders.tierWindow must be 1..10");
    expect(errors).toContain("orders.tierLevelSpan must be 5..100");
  });

  it("rejects invalid order count weights and brackets", () => {
    const payload = structuredClone(DEFAULT_GAME_CONFIG);
    payload.orders.countWeights.one = 50;
    payload.rewards.miniEnergyBrackets = [[100, 10], [50, 10]];
    const errors = validateGameConfigPayload(payload);
    expect(errors).toContain("orders.countWeights must sum to 100");
    expect(errors.join("\n")).toContain("starting points must strictly increase");
  });
});
