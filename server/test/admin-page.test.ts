import { Script } from "node:vm";
import { describe, expect, it } from "vitest";
import { ADMIN_PAGE } from "../src/modules/admin/admin-page";

describe("admin page", () => {
  it("serves a structured config editor instead of the raw JSON textarea", () => {
    expect(ADMIN_PAGE).toContain("configSection:'generators'");
    expect(ADMIN_PAGE).toContain("Bảng spawn");
    expect(ADMIN_PAGE).toContain("Validate");
    expect(ADMIN_PAGE).toContain("orders.tierWindow");
    expect(ADMIN_PAGE).toContain("orders.tierLevelSpan");
    expect(ADMIN_PAGE).not.toContain("id=\"json\"");
  });

  it("embeds syntactically valid browser JavaScript", () => {
    const match = ADMIN_PAGE.match(/<script>([\s\S]*)<\/script>/);
    expect(match).not.toBeNull();
    expect(() => new Script(match?.[1] ?? "")).not.toThrow();
  });
});
