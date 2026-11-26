import { Body, Controller, Patch, Post, UseGuards } from "@nestjs/common";
import type { Player } from "../../generated/prisma/client";
import { CurrentPlayer } from "../../common/current-player.decorator";
import { PlayerAuthGuard } from "../../common/player-auth.guard";
import { BootstrapPlayerDto, UpdateNicknameDto } from "./dto";
import { PlayersService } from "./players.service";

@Controller("api/v1/players")
export class PlayersController {
  constructor(private readonly players: PlayersService) {}

  @Post("bootstrap")
  bootstrap(@Body() dto: BootstrapPlayerDto) {
    return this.players.bootstrap(dto.installId, dto.nickname);
  }

  @Patch("me/nickname")
  @UseGuards(PlayerAuthGuard)
  updateNickname(@CurrentPlayer() player: Player, @Body() dto: UpdateNicknameDto) {
    return this.players.updateNickname(player, dto.nickname);
  }
}
