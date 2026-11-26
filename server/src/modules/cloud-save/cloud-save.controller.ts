import { Body, Controller, Get, Put, UseGuards } from "@nestjs/common";
import type { Player } from "../../generated/prisma/client";
import { CurrentPlayer } from "../../common/current-player.decorator";
import { PlayerAuthGuard } from "../../common/player-auth.guard";
import { CloudSaveService } from "./cloud-save.service";
import { PutCloudSaveDto } from "./dto";

@Controller("api/v1/saves/me")
@UseGuards(PlayerAuthGuard)
export class CloudSaveController {
  constructor(private readonly saves: CloudSaveService) {}

  @Get()
  getMine(@CurrentPlayer() player: Player) {
    return this.saves.getMine(player);
  }

  @Put()
  putMine(@CurrentPlayer() player: Player, @Body() dto: PutCloudSaveDto) {
    return this.saves.putMine(player, dto);
  }
}
