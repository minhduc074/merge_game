import {
  Body,
  Controller,
  Delete,
  Get,
  Param,
  Post,
  Query,
  Req,
  Res,
  UnauthorizedException,
  UseGuards,
} from "@nestjs/common";
import type { Request, Response } from "express";
import { AdminAuthGuard, type AdminRequest } from "../../common/admin-auth.guard";
import { createAdminSession } from "../../common/admin-session";
import { safeEqual } from "../../common/crypto";
import { parseBoard } from "../leaderboards/leaderboard.util";
import { AdminService } from "./admin.service";
import { AdminLoginDto } from "./dto";

function adminCredential(name: "ADMIN_USERNAME" | "ADMIN_PASSWORD", devFallback: string): string {
  const value = process.env[name];
  if (value) return value;
  if (process.env.NODE_ENV === "production" || process.env.VERCEL) {
    throw new Error(`${name} must be set in production`);
  }
  return devFallback;
}

@Controller("api/admin")
export class AdminController {
  constructor(private readonly admin: AdminService) {}

  @Post("login")
  login(@Body() dto: AdminLoginDto, @Res({ passthrough: true }) res: Response) {
    const expectedUser = adminCredential("ADMIN_USERNAME", "admin");
    const expectedPassword = adminCredential("ADMIN_PASSWORD", "admin");
    if (!safeEqual(dto.username, expectedUser) || !safeEqual(dto.password, expectedPassword)) {
      throw new UnauthorizedException("Invalid admin credentials");
    }
    const token = createAdminSession(dto.username);
    res.cookie("bepngon_admin", token, {
      httpOnly: true,
      sameSite: "lax",
      secure: process.env.NODE_ENV === "production" || Boolean(process.env.VERCEL),
      maxAge: 12 * 3_600_000,
      path: "/",
    });
    return { ok: true, username: dto.username };
  }

  @Post("logout")
  logout(@Res({ passthrough: true }) res: Response) {
    res.clearCookie("bepngon_admin", { path: "/" });
    return { ok: true };
  }

  @Get("me")
  @UseGuards(AdminAuthGuard)
  me(@Req() req: Request) {
    return { username: (req as AdminRequest).adminUser };
  }

  @Get("dashboard/summary")
  @UseGuards(AdminAuthGuard)
  dashboard() {
    return this.admin.dashboard();
  }

  @Get("leaderboards/:board")
  @UseGuards(AdminAuthGuard)
  leaderboard(@Param("board") board: string, @Query("limit") limit?: string) {
    return this.admin.leaderboard(parseBoard(board), limit ? Number(limit) : 100);
  }

  @Delete("scores/:id")
  @UseGuards(AdminAuthGuard)
  deleteScore(@Req() req: Request, @Param("id") id: string) {
    return this.admin.deleteBestScore((req as AdminRequest).adminUser, id);
  }

  @Get("players")
  @UseGuards(AdminAuthGuard)
  players(@Query("search") search?: string, @Query("limit") limit?: string) {
    return this.admin.players(search, limit ? Number(limit) : 100);
  }

  @Post("players/:id/ban")
  @UseGuards(AdminAuthGuard)
  ban(@Req() req: Request, @Param("id") id: string) {
    return this.admin.setBanned((req as AdminRequest).adminUser, id, true);
  }

  @Post("players/:id/unban")
  @UseGuards(AdminAuthGuard)
  unban(@Req() req: Request, @Param("id") id: string) {
    return this.admin.setBanned((req as AdminRequest).adminUser, id, false);
  }

  @Get("cloud-saves/:playerId/metadata")
  @UseGuards(AdminAuthGuard)
  cloudSave(@Param("playerId") playerId: string) {
    return this.admin.cloudSaveMetadata(playerId);
  }

  @Get("analytics/summary")
  @UseGuards(AdminAuthGuard)
  analytics() {
    return this.admin.analyticsSummary();
  }
}
