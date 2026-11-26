import { BadRequestException } from "@nestjs/common";
import { LeaderboardBoard } from "../../generated/prisma/enums";

export function parseBoard(raw: string): LeaderboardBoard {
  const normalized = raw.toUpperCase();
  if (normalized in LeaderboardBoard) return normalized as LeaderboardBoard;
  throw new BadRequestException("board must be one of: main, mini, block");
}

export function boardScoreCap(board: LeaderboardBoard): number {
  switch (board) {
    case LeaderboardBoard.MAIN:
      return 1_000_000;
    case LeaderboardBoard.MINI:
      return 100_000;
    case LeaderboardBoard.BLOCK:
      return 200_000;
  }
}
