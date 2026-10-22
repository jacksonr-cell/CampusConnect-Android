import { MigrationInterface, QueryRunner } from 'typeorm';

/**
 * CampusConnect — Round 2 Security Tables
 * Adds email verification, password reset, login history,
 * device sessions, and MFA support.
 *
 * Run: npm run migration:run
 */
export class SecurityTables1700000100000 implements MigrationInterface {
  name = 'SecurityTables1700000100000';

  public async up(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(`CREATE TYPE "login_status_enum" AS ENUM(
      'success','failed_password','failed_not_found','failed_inactive','failed_mfa'
    )`);

    // ── email_verification_tokens ───────────────────────────────────────────
    await queryRunner.query(`
      CREATE TABLE "email_verification_tokens" (
        "id"         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
        "user_id"    UUID NOT NULL REFERENCES "users"("id") ON DELETE CASCADE,
        "token_hash" VARCHAR(255) NOT NULL UNIQUE,
        "expires_at" TIMESTAMP NOT NULL,
        "used_at"    TIMESTAMP,
        "created_at" TIMESTAMP NOT NULL DEFAULT NOW()
      )
    `);
    await queryRunner.query(`CREATE INDEX "idx_email_tokens_user" ON "email_verification_tokens" ("user_id")`);

    // ── password_reset_tokens ───────────────────────────────────────────────
    await queryRunner.query(`
      CREATE TABLE "password_reset_tokens" (
        "id"           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
        "user_id"      UUID NOT NULL REFERENCES "users"("id") ON DELETE CASCADE,
        "token_hash"   VARCHAR(255) NOT NULL UNIQUE,
        "expires_at"   TIMESTAMP NOT NULL,
        "used_at"      TIMESTAMP,
        "requested_ip" VARCHAR(45),
        "created_at"   TIMESTAMP NOT NULL DEFAULT NOW()
      )
    `);
    await queryRunner.query(`CREATE INDEX "idx_reset_tokens_user" ON "password_reset_tokens" ("user_id")`);

    // ── login_history ────────────────────────────────────────────────────────
    await queryRunner.query(`
      CREATE TABLE "login_history" (
        "id"                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
        "user_id"           UUID REFERENCES "users"("id") ON DELETE CASCADE,
        "email_attempted"   VARCHAR(255),
        "status"            "login_status_enum" NOT NULL,
        "ip_address"        VARCHAR(45),
        "user_agent"        TEXT,
        "device"            VARCHAR(100),
        "browser"           VARCHAR(100),
        "os"                VARCHAR(100),
        "city"              VARCHAR(100),
        "country"           VARCHAR(100),
        "is_suspicious"     BOOLEAN NOT NULL DEFAULT false,
        "suspicious_reason" VARCHAR(255),
        "created_at"        TIMESTAMP NOT NULL DEFAULT NOW()
      )
    `);
    await queryRunner.query(`CREATE INDEX "idx_login_history_user"    ON "login_history" ("user_id")`);
    await queryRunner.query(`CREATE INDEX "idx_login_history_created" ON "login_history" ("created_at" DESC)`);
    await queryRunner.query(`CREATE INDEX "idx_login_history_email"   ON "login_history" ("email_attempted")`);

    // ── user_sessions ─────────────────────────────────────────────────────────
    await queryRunner.query(`
      CREATE TABLE "user_sessions" (
        "id"               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
        "user_id"          UUID NOT NULL REFERENCES "users"("id") ON DELETE CASCADE,
        "refresh_token_id" UUID,
        "device_name"      VARCHAR(150),
        "browser"          VARCHAR(100),
        "os"               VARCHAR(100),
        "ip_address"       VARCHAR(45),
        "city"             VARCHAR(100),
        "country"          VARCHAR(100),
        "is_current"       BOOLEAN NOT NULL DEFAULT false,
        "last_active_at"   TIMESTAMP NOT NULL DEFAULT NOW(),
        "revoked_at"       TIMESTAMP,
        "created_at"       TIMESTAMP NOT NULL DEFAULT NOW(),
        "updated_at"       TIMESTAMP NOT NULL DEFAULT NOW()
      )
    `);
    await queryRunner.query(`CREATE INDEX "idx_sessions_user" ON "user_sessions" ("user_id")`);

    // ── mfa_secrets ───────────────────────────────────────────────────────────
    await queryRunner.query(`
      CREATE TABLE "mfa_secrets" (
        "user_id"      UUID PRIMARY KEY REFERENCES "users"("id") ON DELETE CASCADE,
        "secret"       VARCHAR(255) NOT NULL,
        "is_enabled"   BOOLEAN NOT NULL DEFAULT false,
        "backup_codes" JSONB NOT NULL DEFAULT '[]',
        "verified_at"  TIMESTAMP,
        "created_at"   TIMESTAMP NOT NULL DEFAULT NOW(),
        "updated_at"   TIMESTAMP NOT NULL DEFAULT NOW()
      )
    `);
  }

  public async down(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(`DROP TABLE IF EXISTS "mfa_secrets" CASCADE`);
    await queryRunner.query(`DROP TABLE IF EXISTS "user_sessions" CASCADE`);
    await queryRunner.query(`DROP TABLE IF EXISTS "login_history" CASCADE`);
    await queryRunner.query(`DROP TABLE IF EXISTS "password_reset_tokens" CASCADE`);
    await queryRunner.query(`DROP TABLE IF EXISTS "email_verification_tokens" CASCADE`);
    await queryRunner.query(`DROP TYPE IF EXISTS "login_status_enum"`);
  }
}
