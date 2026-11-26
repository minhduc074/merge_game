import { Injectable, NotFoundException } from "@nestjs/common";
import { ConfigStatus, LeaderboardBoard } from "../../generated/prisma/enums";
import { PrismaService } from "../../prisma/prisma.service";

@Injectable()
export class AdminService {
  constructor(private readonly prisma: PrismaService) {}

  async dashboard() {
    const now = Date.now();
    const dayAgo = new Date(now - 24 * 3_600_000);
    const weekAgo = new Date(now - 7 * 24 * 3_600_000);
    const [
      players,
      newPlayers7d,
      activePlayers7d,
      scoreSubmissions24h,
      analyticsEvents24h,
      cloudSaves,
      publishedConfig,
      bannedPlayers,
    ] = await Promise.all([
      this.prisma.player.count(),
      this.prisma.player.count({ where: { createdAt: { gte: weekAgo } } }),
      this.prisma.player.count({ where: { lastSeenAt: { gte: weekAgo } } }),
      this.prisma.scoreHistory.count({ where: { createdAt: { gte: dayAgo } } }),
      this.prisma.analyticsEvent.count({ where: { receivedAt: { gte: dayAgo } } }),
      this.prisma.cloudSave.count(),
      this.prisma.gameConfig.findFirst({
        where: { status: ConfigStatus.PUBLISHED },
        orderBy: { version: "desc" },
      }),
      this.prisma.player.count({ where: { bannedAt: { not: null } } }),
    ]);
    return {
      players,
      newPlayers7d,
      activePlayers7d,
      scoreSubmissions24h,
      analyticsEvents24h,
      cloudSaves,
      bannedPlayers,
      activeConfigVersion: publishedConfig?.version ?? 0,
    };
  }

  async leaderboard(board: LeaderboardBoard, limit = 100) {
    const rows = await this.prisma.bestScore.findMany({
      where: { board },
      orderBy: [{ bestScore: "desc" }, { updatedAt: "asc" }],
      take: Math.min(Math.max(limit, 1), 200),
      include: { player: true },
    });
    return rows.map((row, index) => ({
      id: row.id,
      rank: index + 1,
      playerId: row.playerId,
      nickname: row.player.nickname,
      banned: row.player.bannedAt != null,
      score: row.bestScore,
      lastScore: row.lastScore,
      configVersion: row.configVersion,
      updatedAt: row.updatedAt,
    }));
  }

  async deleteBestScore(actor: string, id: string) {
    const row = await this.prisma.bestScore.findUnique({ where: { id }, include: { player: true } });
    if (!row) throw new NotFoundException("score not found");
    await this.prisma.bestScore.delete({ where: { id } });
    await this.audit(actor, "score.delete", "bestScore", id, {
      playerId: row.playerId,
      nickname: row.player.nickname,
      board: row.board,
      score: row.bestScore,
    });
    return { ok: true };
  }

  async players(search?: string, limit = 100) {
    return this.prisma.player.findMany({
      where: search ? { nickname: { contains: search, mode: "insensitive" } } : undefined,
      orderBy: { lastSeenAt: "desc" },
      take: Math.min(Math.max(limit, 1), 200),
      select: {
        id: true,
        nickname: true,
        createdAt: true,
        lastSeenAt: true,
        bannedAt: true,
        bestScores: { select: { board: true, bestScore: true } },
        cloudSave: { select: { revision: true, updatedAt: true } },
      },
    });
  }

  async setBanned(actor: string, playerId: string, banned: boolean) {
    const player = await this.prisma.player.findUnique({ where: { id: playerId } });
    if (!player) throw new NotFoundException("player not found");
    const updated = await this.prisma.player.update({
      where: { id: playerId },
      data: { bannedAt: banned ? new Date() : null },
    });
    await this.audit(actor, banned ? "player.ban" : "player.unban", "player", playerId, {
      nickname: player.nickname,
    });
    return { ok: true, banned: updated.bannedAt != null };
  }

  async cloudSaveMetadata(playerId: string) {
    const row = await this.prisma.cloudSave.findUnique({ where: { playerId } });
    if (!row) throw new NotFoundException("cloud save not found");
    return {
      playerId,
      revision: row.revision,
      checksum: row.checksum,
      clientUpdatedAt: Number(row.clientUpdatedAt),
      updatedAt: row.updatedAt,
      sizeApprox: JSON.stringify(row.saveJson).length,
    };
  }

  async analyticsSummary() {
    const weekAgo = new Date(Date.now() - 7 * 24 * 3_600_000);
    const grouped = await this.prisma.analyticsEvent.groupBy({
      by: ["eventName"],
      where: { receivedAt: { gte: weekAgo } },
      _count: { eventName: true },
      orderBy: { _count: { eventName: "desc" } },
      take: 30,
    });
    return {
      since: weekAgo,
      events: grouped.map((row) => ({ eventName: row.eventName, count: row._count.eventName })),
    };
  }

  private async audit(actor: string, action: string, targetType: string, targetId: string, details: Record<string, unknown>) {
    await this.prisma.adminAuditLog.create({
      data: { actor, action, targetType, targetId, details: details as never },
    });
  }
}
