import { Module } from "@nestjs/common";
import { PrismaModule } from "./prisma/prisma.module";
import { AdminModule } from "./modules/admin/admin.module";
import { AnalyticsModule } from "./modules/analytics/analytics.module";
import { CloudSaveModule } from "./modules/cloud-save/cloud-save.module";
import { GameConfigModule } from "./modules/game-config/game-config.module";
import { HealthController } from "./modules/health/health.controller";
import { LeaderboardsModule } from "./modules/leaderboards/leaderboards.module";
import { PlayersModule } from "./modules/players/players.module";

@Module({
  imports: [
    PrismaModule,
    PlayersModule,
    LeaderboardsModule,
    GameConfigModule,
    CloudSaveModule,
    AnalyticsModule,
    AdminModule,
  ],
  controllers: [HealthController],
})
export class AppModule {}
