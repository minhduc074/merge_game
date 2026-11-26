import { createHmac } from "node:crypto";
import { adminSessionSecret, safeEqual } from "./crypto";

type SessionPayload = {
  sub: string;
  exp: number;
};

function signPart(value: string): string {
  return createHmac("sha256", adminSessionSecret()).update(value).digest("base64url");
}

export function createAdminSession(username: string, ttlMs = 12 * 3_600_000): string {
  const payload: SessionPayload = { sub: username, exp: Date.now() + ttlMs };
  const body = Buffer.from(JSON.stringify(payload)).toString("base64url");
  return `${body}.${signPart(body)}`;
}

export function verifyAdminSession(token: string | undefined | null): string | null {
  if (!token) return null;
  const [body, sig] = token.split(".");
  if (!body || !sig || !safeEqual(sig, signPart(body))) return null;
  try {
    const payload = JSON.parse(Buffer.from(body, "base64url").toString("utf8")) as SessionPayload;
    if (!payload.sub || payload.exp <= Date.now()) return null;
    return payload.sub;
  } catch {
    return null;
  }
}

export function readCookie(header: string | undefined, name: string): string | null {
  if (!header) return null;
  for (const part of header.split(";")) {
    const [key, ...rest] = part.trim().split("=");
    if (key === name) return decodeURIComponent(rest.join("="));
  }
  return null;
}
