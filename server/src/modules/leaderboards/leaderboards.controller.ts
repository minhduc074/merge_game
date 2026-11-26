import { Body, Controller, Get, Param, Post, Query, UseGuards } from "@nestjs/common";
import type { Player } from "../../generated/prisma/client";
import { CurrentPlayer } from "../../common/current-player.decorator";
import { PlayerAuthGuard } from "../../common/player-auth.guard";
import { SubmitScoreDto } from "./dto";
import { parseBoard } from "./leaderboard.util";
import { LeaderboardsService } from "./leaderboards.service";

@Controller("api/v1/leaderboards")
export class LeaderboardsController {
  constructor(private readonly leaderboards: LeaderboardsService) {}

  @Get(":board")
  top(@Param("board") board: string, @Query("limit") limit?: string) {
    return this.leaderboards.top(parseBoard(board), limit ? Number(limit) : 100);
  }

  @Get(":board/around-me")
  @UseGuards(PlayerAuthGuard)
  aroundMe(
    @Param("board") board: string,
    @Query("range") range: string | undefined,
    @CurrentPlayer() player: Player,
  ) {
    return this.leaderboards.aroundMe(parseBoard(board), player, range ? Number(range) : 10);
  }

  @Post(":board/submit")
  @UseGuards(PlayerAuthGuard)
  submit(
    @Param("board") board: string,
    @CurrentPlayer() player: Player,
    @Body() dto: SubmitScoreDto,
  ) {
    return this.leaderboards.submit(parseBoard(board), player, dto);
  }
}
