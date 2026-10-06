import { DataSource, DataSourceOptions } from 'typeorm';
import * as dotenv from 'dotenv';
import { join } from 'path';

dotenv.config();

export const dataSourceOptions: DataSourceOptions = {
  type       : 'postgres',
  host       : process.env.DATABASE_HOST     || 'localhost',
  port       : parseInt(process.env.DATABASE_PORT, 10) || 5432,
  username   : process.env.DATABASE_USERNAME || 'campusconnect',
  password   : process.env.DATABASE_PASSWORD || 'campusconnect_dev',
  database   : process.env.DATABASE_NAME     || 'campusconnect',
  // Entity discovery — compiled JS in production
  entities   : [join(__dirname, '..', '**', '*.entity{.ts,.js}')],
  migrations : [join(__dirname, 'migrations', '*{.ts,.js}')],
  synchronize: false,   // NEVER true in production
  logging    : process.env.NODE_ENV === 'development',
  ssl        : process.env.DATABASE_SSL === 'true' ? { rejectUnauthorized: false } : false,
};

export const AppDataSource = new DataSource(dataSourceOptions);
