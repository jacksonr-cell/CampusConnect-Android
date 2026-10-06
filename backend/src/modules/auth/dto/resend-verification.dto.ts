import { ApiProperty } from '@nestjs/swagger';
import { IsEmail } from 'class-validator';

export class ResendVerificationDto {
  @ApiProperty({ example: 'jane@kabarak.ac.ke' })
  @IsEmail()
  email: string;
}
