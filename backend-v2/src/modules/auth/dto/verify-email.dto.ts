import { ApiProperty } from '@nestjs/swagger';
import { IsString, MinLength } from 'class-validator';

export class VerifyEmailDto {
  @ApiProperty({ description: 'Email verification token sent to the user\'s inbox' })
  @IsString()
  @MinLength(10)
  token: string;
}
