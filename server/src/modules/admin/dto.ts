import { IsString, Length } from "class-validator";

export class AdminLoginDto {
  @IsString()
  @Length(1, 80)
  username!: string;

  @IsString()
  @Length(1, 200)
  password!: string;
}
