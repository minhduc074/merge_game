import { createHash, createHmac, randomBytes, timingSafeEqual } from "node:crypto";

function requiredSecret(name: string, devFallback: string): string {
  const value = process.env[name];
  if (value && value.length >= 16) return value;
  if (process.env.NODE_ENV === "production" || process.env.VERCEL) {
    throw new Error(`${name} must be set to at least 16 characters`);
  }
  return devFallback;
}

export function randomToken(byteLength = 32): string {
  return randomBytes(byteLength).toString("base64url");
}

export function sha256Hex(value: string): string {
  return createHash("sha256").update(value).digest("hex");
}

export function playerTokenHash(token: string): string {
  const pepper = requiredSecret("PLAYER_TOKEN_PEPPER", "dev-player-token-pepper");
  return createHmac("sha256", pepper).update(token).digest("hex");
}

export function adminSessionSecret(): string {
  return requiredSecret("ADMIN_SESSION_SECRET", "dev-admin-session-secret");
}

export function safeEqual(a: string, b: string): boolean {
  const aa = Buffer.from(a);
  const bb = Buffer.from(b);
  return aa.length === bb.length && timingSafeEqual(aa, bb);
}
