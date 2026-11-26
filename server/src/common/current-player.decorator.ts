import { createParamDecorator, ExecutionContext } from "@nestjs/common";
import type { Player } from "../generated/prisma/client";
import type { AuthedRequest } from "./player-auth.guard";

export const CurrentPlayer = createParamDecorator((_: unknown, ctx: ExecutionContext): Player => {
  return ctx.switchToHttp().getRequest<AuthedRequest>().player;
});
