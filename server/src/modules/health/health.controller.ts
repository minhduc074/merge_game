import { Controller, Get } from "@nestjs/common";

@Controller()
export class HealthController {
  @Get()
  root() {
    return {
      name: "Bep Ngon Merge API",
      ok: true,
      admin: "/admin",
      health: "/api/health",
    };
  }

  @Get("api/health")
  health() {
    return { ok: true, time: new Date().toISOString() };
  }
}
