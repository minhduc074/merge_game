import {
  CanActivate,
  ExecutionContext,
  Injectable,
  UnauthorizedException,
  ForbiddenException,
} from "@nestjs/common";
import type { Request } from "express";
import type { Player } from "../generated/prisma/client";
import { playerTokenHash } from "./crypto";
import { PrismaService } from "../prisma/prisma.service";

export type AuthedRequest = Request & { player: Player };

@Injectable()
export class PlayerAuthGuard implements CanActivate {
  constructor(private readonly prisma: PrismaService) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const req = context.switchToHttp().getRequest<AuthedRequest>();
    const header = req.headers.authorization;
    const token = header?.startsWith("Bearer ") ? header.slice("Bearer ".length).trim() : null;
    if (!token) throw new UnauthorizedException("Missing Bearer token");

    const tokenHash = playerTokenHash(token);
    const playerToken = await this.prisma.playerToken.findUnique({
      where: { tokenHash },
      include: { player: true },
    });
    if (!playerToken || playerToken.revokedAt) {
      throw new UnauthorizedException("Invalid player token");
    }
    if (playerToken.player.bannedAt) {
      throw new ForbiddenException("Player is banned");
    }
    req.player = playerToken.player;
    void this.prisma.player
      .update({ where: { id: playerToken.playerId }, data: { lastSeenAt: new Date() } })
      .catch(() => undefined);
    return true;
  }
}
