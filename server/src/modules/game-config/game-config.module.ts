import { Module } from "@nestjs/common";
import { AdminConfigController } from "./admin-config.controller";
import { GameConfigController } from "./game-config.controller";
import { GameConfigService } from "./game-config.service";

@Module({
  controllers: [GameConfigController, AdminConfigController],
  providers: [GameConfigService],
  exports: [GameConfigService],
})
export class GameConfigModule {}
