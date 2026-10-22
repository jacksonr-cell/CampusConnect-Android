import { MigrationInterface, QueryRunner } from 'typeorm';

/**
 * CampusConnect — Round 3: Content Moderation
 * Adds the reports table for user-submitted content/user reports.
 *
 * Run: npm run migration:run
 */
export class ReportsTable1700000200000 implements MigrationInterface {
  name = 'ReportsTable1700000200000';

  public async up(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(`CREATE TYPE "report_reason_enum" AS ENUM(
      'spam','harassment','hate_speech','misinformation','inappropriate',
      'scam','intellectual_property','other'
    )`);
    await queryRunner.query(`CREATE TYPE "report_status_enum" AS ENUM(
      'pending','reviewed','actioned','dismissed'
    )`);
    await queryRunner.query(`CREATE TYPE "report_target_type_enum" AS ENUM(
      'post','comment','user','listing','property','event','message'
    )`);

    await queryRunner.query(`
      CREATE TABLE "reports" (
        "id"             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
        "reporter_id"    UUID NOT NULL REFERENCES "users"("id") ON DELETE CASCADE,
        "target_type"    "report_target_type_enum" NOT NULL,
        "target_id"      UUID NOT NULL,
        "reason"         "report_reason_enum" NOT NULL,
        "description"    TEXT,
        "status"         "report_status_enum" NOT NULL DEFAULT 'pending',
        "reviewed_by"    UUID REFERENCES "users"("id") ON DELETE SET NULL,
        "moderator_note" TEXT,
        "reviewed_at"    TIMESTAMP,
        "created_at"     TIMESTAMP NOT NULL DEFAULT NOW(),
        "updated_at"     TIMESTAMP NOT NULL DEFAULT NOW()
      )
    `);

    await queryRunner.query(`CREATE INDEX "idx_reports_status"      ON "reports" ("status")`);
    await queryRunner.query(`CREATE INDEX "idx_reports_target"      ON "reports" ("target_type", "target_id")`);
    await queryRunner.query(`CREATE INDEX "idx_reports_reporter"    ON "reports" ("reporter_id")`);
  }

  public async down(queryRunner: QueryRunner): Promise<void> {
    await queryRunner.query(`DROP TABLE IF EXISTS "reports" CASCADE`);
    await queryRunner.query(`DROP TYPE IF EXISTS "report_target_type_enum"`);
    await queryRunner.query(`DROP TYPE IF EXISTS "report_status_enum"`);
    await queryRunner.query(`DROP TYPE IF EXISTS "report_reason_enum"`);
  }
}
