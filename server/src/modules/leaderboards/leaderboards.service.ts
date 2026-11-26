import { BadRequestException, HttpException, HttpStatus, Injectable } from "@nestjs/common";
import type { Player } from "../../generated/prisma/client";
import { LeaderboardBoard } from "../../generated/prisma/enums";
import { PrismaService } from "../../prisma/prisma.service";
import { boardScoreCap } from "./leaderboard.util";
import type { SubmitScoreDto } from "./dto";

function entry(row: {
  bestScore: number;
  updatedAt: Date;
  player: { id: string; nickname: string };
}, rank: number) {
  return {
    rank,
    playerId: row.player.id,
    nickname: row.player.nickname,
    score: row.bestScore,
    updatedAt: row.updatedAt,
  };
}

@Injectable()
export class LeaderboardsService {
  constructor(private readonly prisma: PrismaService) {}

  async top(board: LeaderboardBoard, limit = 100) {
    const take = Math.min(Math.max(limit, 1), 100);
    const rows = await this.prisma.bestScore.findMany({
      where: { board, player: { bannedAt: null } },
      orderBy: [{ bestScore: "desc" }, { updatedAt: "asc" }],
      take,
      include: { player: true },
    });
    return {
      board: board.toLowerCase(),
      entries: rows.map((row, index) => entry(row, index + 1)),
    };
  }

  async aroundMe(board: LeaderboardBoard, player: Player, range = 10) {
    const mine = await this.prisma.bestScore.findUnique({
      where: { playerId_board: { playerId: player.id, board } },
    });
    if (!mine) {
      const top = await this.top(board, Math.min(100, range * 2 + 1));
      return { ...top, me: null };
    }

    const better = await this.prisma.bestScore.count({
      where: {
        board,
        bestScore: { gt: mine.bestScore },
        player: { bannedAt: null },
      },
    });
    const rank = better + 1;
    const radius = Math.min(Math.max(range, 1), 25);
    const skip = Math.max(0, rank - 1 - radius);
    const rows = await this.prisma.bestScore.findMany({
      where: { board, player: { bannedAt: null } },
      orderBy: [{ bestScore: "desc" }, { updatedAt: "asc" }],
      skip,
      take: radius * 2 + 1,
      include: { player: true },
    });
    return {
      board: board.toLowerCase(),
      me: {
        rank,
        playerId: player.id,
        nickname: player.nickname,
        score: mine.bestScore,
        updatedAt: mine.updatedAt,
      },
      entries: rows.map((row, index) => ({
        ...entry(row, skip + index + 1),
        isMe: row.playerId === player.id,
      })),
    };
  }

  async submit(board: LeaderboardBoard, player: Player, dto: SubmitScoreDto) {
    const cap = boardScoreCap(board);
    if (dto.score > cap) {
      throw new BadRequestException(`score is above the accepted cap for ${board.toLowerCase()}`);
    }

    const since = new Date(Date.now() - 60_000);
    const recentCount = await this.prisma.scoreHistory.count({
      where: { playerId: player.id, createdAt: { gte: since } },
    });
    if (recentCount >= 30) {
      throw new HttpException("Too many score submissions", HttpStatus.TOO_MANY_REQUESTS);
    }

    const debug = dto.debug === true;
    const excludeDebugScore = debug && process.env.ALLOW_DEBUG_SCORES !== "true";
    const result = await this.prisma.$transaction(async (tx) => {
      await tx.scoreHistory.create({
        data: {
          playerId: player.id,
          board,
          score: dto.score,
          configVersion: dto.configVersion,
          appVersion: dto.appVersion,
          debug,
          clientMeta: dto.clientMeta as never,
        },
      });

      if (excludeDebugScore) {
        return { accepted: false as const, reason: "debug scores are excluded", bestScore: null as number | null, rank: null as number | null };
      }

      const key = { playerId_board: { playerId: player.id, board } };
      const existing = await tx.bestScore.findUnique({ where: key });
      const bestScore = Math.max(existing?.bestScore ?? 0, dto.score);
      await tx.bestScore.upsert({
        where: key,
        create: {
          playerId: player.id,
          board,
          bestScore,
          lastScore: dto.score,
          configVersion: dto.configVersion,
        },
        update: {
          bestScore,
          lastScore: dto.score,
          configVersion: dto.configVersion,
        },
      });
      const better = await tx.bestScore.count({
        where: {
          board,
          bestScore: { gt: bestScore },
          player: { bannedAt: null },
        },
      });
      return { accepted: true as const, reason: null as string | null, bestScore, rank: better + 1 };
    });

    return {
      board: board.toLowerCase(),
      submittedScore: dto.score,
      ...result,
    };
  }
}
