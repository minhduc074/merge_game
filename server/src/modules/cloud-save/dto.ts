import { Type } from "class-transformer";
import { IsInt, IsString, Length, Matches, Max, Min } from "class-validator";

export class PutCloudSaveDto {
  @Type(() => Number)
  @IsInt()
  @Min(0)
  @Max(1_000_000)
  baseRevision!: number;

  @IsString()
  @Length(2, 250_000)
  saveJson!: string;

  @IsString()
  @Matches(/^[a-f0-9]{64}$/i)
  checksum!: string;

  @Type(() => Number)
  @IsInt()
  @Min(0)
  clientUpdatedAt!: number;
}
