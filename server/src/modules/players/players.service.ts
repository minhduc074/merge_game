import { Injectable } from "@nestjs/common";
import type { Player } from "../../generated/prisma/client";
import { playerTokenHash, randomToken } from "../../common/crypto";
import { PrismaService } from "../../prisma/prisma.service";

export function publicPlayer(player: Player) {
  return {
    id: player.id,
    nickname: player.nickname,
    createdAt: player.createdAt,
    lastSeenAt: player.lastSeenAt,
    banned: player.bannedAt != null,
  };
}

@Injectable()
export class PlayersService {
  constructor(private readonly prisma: PrismaService) {}

  async bootstrap(installId: string, nickname?: string) {
    const fallbackName = `Bep #${installId.replace(/[^A-Za-z0-9]/g, "").slice(-4).toUpperCase()}`;
    const player = await this.prisma.player.upsert({
      where: { installId },
      update: {
        lastSeenAt: new Date(),
        ...(nickname ? { nickname } : {}),
      },
      create: {
        installId,
        nickname: nickname || fallbackName,
        lastSeenAt: new Date(),
      },
    });

    const token = randomToken();
    await this.prisma.playerToken.create({
      data: {
        playerId: player.id,
        tokenHash: playerTokenHash(token),
      },
    });

    return {
      player: publicPlayer(player),
      token,
    };
  }

  async updateNickname(player: Player, nickname: string) {
    const updated = await this.prisma.player.update({
      where: { id: player.id },
      data: { nickname },
    });
    return { player: publicPlayer(updated) };
  }
}
