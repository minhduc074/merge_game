import { CanActivate, ExecutionContext, Injectable, UnauthorizedException } from "@nestjs/common";
import type { Request } from "express";
import { readCookie, verifyAdminSession } from "./admin-session";

export type AdminRequest = Request & { adminUser: string };

@Injectable()
export class AdminAuthGuard implements CanActivate {
  canActivate(context: ExecutionContext): boolean {
    const req = context.switchToHttp().getRequest<AdminRequest>();
    const token = readCookie(req.headers.cookie, "bepngon_admin");
    const user = verifyAdminSession(token);
    if (!user) throw new UnauthorizedException("Admin session required");
    req.adminUser = user;
    return true;
  }
}
