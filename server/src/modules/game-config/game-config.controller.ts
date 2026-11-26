import { Controller, Get, Query } from "@nestjs/common";
import { GameConfigService } from "./game-config.service";

@Controller("api/v1/config")
export class GameConfigController {
  constructor(private readonly configs: GameConfigService) {}

  @Get("current")
  async current(@Query("sinceVersion") sinceVersion?: string) {
    const current = await this.configs.current();
    if (sinceVersion !== undefined && Number(sinceVersion) === current.version) {
      return { notModified: true, version: current.version };
    }
    return { notModified: false, ...current };
  }
}
