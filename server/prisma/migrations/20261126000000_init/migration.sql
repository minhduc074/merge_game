-- CreateSchema
CREATE SCHEMA IF NOT EXISTS "public";

-- CreateEnum
CREATE TYPE "LeaderboardBoard" AS ENUM ('MAIN', 'MINI', 'BLOCK');

-- CreateEnum
CREATE TYPE "ConfigStatus" AS ENUM ('DRAFT', 'PUBLISHED', 'ARCHIVED');

-- CreateTable
CREATE TABLE "Player" (
    "id" TEXT NOT NULL,
    "installId" TEXT NOT NULL,
    "nickname" TEXT NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "lastSeenAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "bannedAt" TIMESTAMP(3),

    CONSTRAINT "Player_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "PlayerToken" (
    "id" TEXT NOT NULL,
    "playerId" TEXT NOT NULL,
    "tokenHash" TEXT NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "revokedAt" TIMESTAMP(3),

    CONSTRAINT "PlayerToken_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "BestScore" (
    "id" TEXT NOT NULL,
    "playerId" TEXT NOT NULL,
    "board" "LeaderboardBoard" NOT NULL,
    "bestScore" INTEGER NOT NULL,
    "lastScore" INTEGER NOT NULL,
    "configVersion" INTEGER,
    "updatedAt" TIMESTAMP(3) NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "BestScore_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "ScoreHistory" (
    "id" TEXT NOT NULL,
    "playerId" TEXT NOT NULL,
    "board" "LeaderboardBoard" NOT NULL,
    "score" INTEGER NOT NULL,
    "configVersion" INTEGER,
    "appVersion" TEXT,
    "debug" BOOLEAN NOT NULL DEFAULT false,
    "clientMeta" JSONB,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "ScoreHistory_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "GameConfig" (
    "id" TEXT NOT NULL,
    "version" INTEGER NOT NULL,
    "status" "ConfigStatus" NOT NULL DEFAULT 'DRAFT',
    "payload" JSONB NOT NULL,
    "note" TEXT,
    "createdBy" TEXT,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "publishedAt" TIMESTAMP(3),

    CONSTRAINT "GameConfig_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "CloudSave" (
    "id" TEXT NOT NULL,
    "playerId" TEXT NOT NULL,
    "revision" INTEGER NOT NULL DEFAULT 1,
    "saveJson" JSONB NOT NULL,
    "checksum" TEXT NOT NULL,
    "clientUpdatedAt" BIGINT NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "CloudSave_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "AnalyticsEvent" (
    "id" TEXT NOT NULL,
    "playerId" TEXT,
    "sessionId" TEXT,
    "eventName" TEXT NOT NULL,
    "payload" JSONB,
    "appVersion" TEXT,
    "configVersion" INTEGER,
    "clientAt" BIGINT,
    "receivedAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "AnalyticsEvent_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "AdminAuditLog" (
    "id" TEXT NOT NULL,
    "actor" TEXT NOT NULL,
    "action" TEXT NOT NULL,
    "targetType" TEXT,
    "targetId" TEXT,
    "details" JSONB,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "AdminAuditLog_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE UNIQUE INDEX "Player_installId_key" ON "Player"("installId");

-- CreateIndex
CREATE INDEX "Player_createdAt_idx" ON "Player"("createdAt");

-- CreateIndex
CREATE UNIQUE INDEX "PlayerToken_tokenHash_key" ON "PlayerToken"("tokenHash");

-- CreateIndex
CREATE INDEX "PlayerToken_playerId_revokedAt_idx" ON "PlayerToken"("playerId", "revokedAt");

-- CreateIndex
CREATE INDEX "BestScore_board_bestScore_idx" ON "BestScore"("board", "bestScore");

-- CreateIndex
CREATE INDEX "BestScore_board_updatedAt_idx" ON "BestScore"("board", "updatedAt");

-- CreateIndex
CREATE UNIQUE INDEX "BestScore_playerId_board_key" ON "BestScore"("playerId", "board");

-- CreateIndex
CREATE INDEX "ScoreHistory_playerId_board_createdAt_idx" ON "ScoreHistory"("playerId", "board", "createdAt");

-- CreateIndex
CREATE INDEX "ScoreHistory_board_createdAt_idx" ON "ScoreHistory"("board", "createdAt");

-- CreateIndex
CREATE UNIQUE INDEX "GameConfig_version_key" ON "GameConfig"("version");

-- CreateIndex
CREATE INDEX "GameConfig_status_publishedAt_idx" ON "GameConfig"("status", "publishedAt");

-- CreateIndex
CREATE UNIQUE INDEX "CloudSave_playerId_key" ON "CloudSave"("playerId");

-- CreateIndex
CREATE INDEX "AnalyticsEvent_eventName_receivedAt_idx" ON "AnalyticsEvent"("eventName", "receivedAt");

-- CreateIndex
CREATE INDEX "AnalyticsEvent_playerId_receivedAt_idx" ON "AnalyticsEvent"("playerId", "receivedAt");

-- CreateIndex
CREATE INDEX "AnalyticsEvent_receivedAt_idx" ON "AnalyticsEvent"("receivedAt");

-- CreateIndex
CREATE INDEX "AdminAuditLog_action_createdAt_idx" ON "AdminAuditLog"("action", "createdAt");

-- CreateIndex
CREATE INDEX "AdminAuditLog_createdAt_idx" ON "AdminAuditLog"("createdAt");

-- AddForeignKey
ALTER TABLE "PlayerToken" ADD CONSTRAINT "PlayerToken_playerId_fkey" FOREIGN KEY ("playerId") REFERENCES "Player"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "BestScore" ADD CONSTRAINT "BestScore_playerId_fkey" FOREIGN KEY ("playerId") REFERENCES "Player"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "ScoreHistory" ADD CONSTRAINT "ScoreHistory_playerId_fkey" FOREIGN KEY ("playerId") REFERENCES "Player"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "CloudSave" ADD CONSTRAINT "CloudSave_playerId_fkey" FOREIGN KEY ("playerId") REFERENCES "Player"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "AnalyticsEvent" ADD CONSTRAINT "AnalyticsEvent_playerId_fkey" FOREIGN KEY ("playerId") REFERENCES "Player"("id") ON DELETE SET NULL ON UPDATE CASCADE;
