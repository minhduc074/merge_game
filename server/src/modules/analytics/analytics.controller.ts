import { Body, Controller, Post, UseGuards } from "@nestjs/common";
import type { Player } from "../../generated/prisma/client";
import { CurrentPlayer } from "../../common/current-player.decorator";
import { PlayerAuthGuard } from "../../common/player-auth.guard";
import { AnalyticsService } from "./analytics.service";
import { AnalyticsBatchDto } from "./dto";

@Controller("api/v1/analytics")
export class AnalyticsController {
  constructor(private readonly analytics: AnalyticsService) {}

  @Post("events")
  @UseGuards(PlayerAuthGuard)
  ingest(@CurrentPlayer() player: Player, @Body() dto: AnalyticsBatchDto) {
    return this.analytics.ingest(player, dto);
  }
}
