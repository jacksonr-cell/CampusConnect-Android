import { MigrationInterface, QueryRunner } from 'typeorm';

/**
 * CampusConnect — Initial Schema Migration
 * Creates all 25 tables for the full platform.
 *
 * Run: npm run migration:run
 */
export class InitialSchema1700000000000 implements MigrationInterface {
  name = 'InitialSchema1700000000000';

  public async up(queryRunner: QueryRunner): Promise<void> {
    // Extensions
    await queryRunner.query(`CREATE EXTENSION IF NOT EXISTS "uuid-ossp"`);
    await queryRunner.query(`CREATE EXTENSION IF NOT EXISTS "pg_trgm"`);

    // ── Enums ─────────────────────────────────────────────────────────────
    await queryRunner.query(`CREATE TYPE "user_role_enum" AS ENUM('student','club_leader','landlord','admin')`);
    await queryRunner.query(`CREATE TYPE "post_type_enum" AS ENUM('text','image','video','poll')`);
    await queryRunner.query(`CREATE TYPE "post_visibility_enum" AS ENUM('public','followers','community')`);
    await queryRunner.query(`CREATE TYPE "reaction_type_enum" AS ENUM('like','love','haha','wow','sad','angry')`);
    await queryRunner.query(`CREATE TYPE "property_type_enum" AS ENUM('hostel','apartment','studio','shared_room','private_room','bedsitter')`);
    await queryRunner.query(`CREATE TYPE "amenity_category_enum" AS ENUM('utilities','security','connectivity','appliances','transport','other')`);
    await queryRunner.query(`CREATE TYPE "booking_status_enum" AS ENUM('inquiry','pending','accepted','rejected','cancelled','completed')`);
    await queryRunner.query(`CREATE TYPE "event_category_enum" AS ENUM('academic','social','sports','culture','career','tech','entertainment','volunteer','other')`);
    await queryRunner.query(`CREATE TYPE "attendee_status_enum" AS ENUM('going','maybe','not_going')`);
    await queryRunner.query(`CREATE TYPE "club_category_enum" AS ENUM('academic','sports','arts','tech','social','religious','career','volunteer','other')`);
    await queryRunner.query(`CREATE TYPE "club_member_role_enum" AS ENUM('member','moderator','leader')`);
    await queryRunner.query(`CREATE TYPE "listing_category_enum" AS ENUM('textbooks','electronics','furniture','clothing','room_takeover','services','food','other')`);
    await queryRunner.query(`CREATE TYPE "listing_condition_enum" AS ENUM('new','like_new','good','fair','poor')`);
    await queryRunner.query(`CREATE TYPE "listing_status_enum" AS ENUM('active','sold','reserved','deleted')`);
    await queryRunner.query(`CREATE TYPE "conversation_type_enum" AS ENUM('direct','group')`);
    await queryRunner.query(`CREATE TYPE "message_type_enum" AS ENUM('text','image','file','audio','system')`);
    await queryRunner.query(`CREATE TYPE "notification_type_enum" AS ENUM('post_like','post_comment','comment_reply','follow','event_reminder','event_invite','booking_update','booking_inquiry','message','club_invite','marketplace_interest','badge_earned','system')`);
    await queryRunner.query(`CREATE TYPE "user_level_enum" AS ENUM('newcomer','student','active_student','campus_regular','campus_star','campus_legend')`);
    await queryRunner.query(`CREATE TYPE "badge_category_enum" AS ENUM('social','academic','accommodation','events','marketplace','special')`);

    // ── users ──────────────────────────────────────────────────────────────
    await queryRunner.query(`
      CREATE TABLE "users" (
        "id"                   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
        "email"                VARCHAR(255) NOT NULL UNIQUE,
        "username"             VARCHAR(50)  NOT NULL UNIQUE,
        "password_hash"        VARCHAR(255) NOT NULL,
        "first_name"           VARCHAR(100) NOT NULL,
        "last_name"            VARCHAR(100) NOT NULL,
        "avatar_url"           TEXT,
        "cover_image_url"      TEXT,
        "bio"                  TEXT,
        "university"           VARCHAR(200),
        "student_id"           VARCHAR(50),
        "course_of_study"      VARCHAR(200),
        "year_of_study"        INTEGER,
        "role"                 "user_role_enum" NOT NULL DEFAULT 'student',
        "is_verified"          BOOLEAN NOT NULL DEFAULT false,
        "is_email_verified"    BOOLEAN NOT NULL DEFAULT false,
        "is_active"            BOOLEAN NOT NULL DEFAULT true,
        "two_factor_enabled"   BOOLEAN NOT NULL DEFAULT false,
        "reputation_points"    INTEGER NOT NULL DEFAULT 0,
        "followers_count"      INTEGER NOT NULL DEFAULT 0,
        "following_count"      INTEGER NOT NULL DEFAULT 0,
        "posts_count"          INTEGER NOT NULL DEFAULT 0,
        "last_seen_at"         TIMESTAMP,
        "google_id"            VARCHAR(255) UNIQUE,
        "created_at"           TIMESTAMP NOT NULL DEFAULT NOW(),
        "updated_at"           TIMESTAMP NOT NULL DEFAULT NOW()
      )
    `);
    await queryRunner.query(`CREATE INDEX "idx_users_email"      ON "users" ("email")`);
    await queryRunner.query(`CREATE INDEX "idx_users_username"   ON "users" ("username")`);
    await queryRunner.query(`CREATE INDEX "idx_users_university" ON "users" ("university")`);
    await queryRunner.query(`CREATE INDEX "idx_users_role"       ON "users" ("role")`);

    // ── refresh_tokens ─────────────────────────────────────────────────────
    await queryRunner.query(`
      CREATE TABLE "refresh_tokens" (
        "id"          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
        "user_id"     UUID NOT NULL REFERENCES "users"("id") ON DELETE CASCADE,
        "token_hash"  VARCHAR(255) NOT NULL,
        "expires_at"  TIMESTAMP NOT NULL,
        "ip_address"  VARCHAR(45),
        "user_agent"  TEXT,
        "created_at"  TIMESTAMP NOT NULL DEFAULT NOW()
      )
    `);
    await queryRunner.query(`CREATE INDEX "idx_refresh_tokens_user" ON "refresh_tokens" ("user_id")`);
    await queryRunner.query(`CREATE INDEX "idx_refresh_tokens_exp"  ON "refresh_tokens" ("expires_at")`);

    // ── user_follows ────────────────────────────────────────────────────────
    await queryRunner.query(`
      CREATE TABLE "user_follows" (
        "id"           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
        "follower_id"  UUID NOT NULL REFERENCES "users"("id") ON DELETE CASCADE,
        "following_id" UUID NOT NULL REFERENCES "users"("id") ON DELETE CASCADE,
        "created_at"   TIMESTAMP NOT NULL DEFAULT NOW(),
        UNIQUE ("follower_id", "following_id")
      )
    `);
    await queryRunner.query(`CREATE INDEX "idx_follows_follower"  ON "user_follows" ("follower_id")`);
    await queryRunner.query(`CREATE INDEX "idx_follows_following" ON "user_follows" ("following_id")`);

    // ── communities ─────────────────────────────────────────────────────────
    await queryRunner.query(`
      CREATE TABLE "communities" (
        "id"           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
        "name"         VARCHAR(100) NOT NULL UNIQUE,
        "display_name" VARCHAR(150) NOT NULL,
        "description"  TEXT,
        "university"   VARCHAR(200),
        "icon_url"     TEXT,
        "banner_url"   TEXT,
        "members_count" INTEGER NOT NULL DEFAULT 0,
        "posts_count"   INTEGER NOT NULL DEFAULT 0,
        "is_public"     BOOLEAN NOT NULL DEFAULT true,
        "is_verified"   BOOLEAN NOT NULL DEFAULT false,
        "created_by"    UUID REFERENCES "users"("id") ON DELETE SET NULL,
        "created_at"    TIMESTAMP NOT NULL DEFAULT NOW(),
        "updated_at"    TIMESTAMP NOT NULL DEFAULT NOW()
      )
    `);

    // ── posts ────────────────────────────────────────────────────────────────
    await queryRunner.query(`
      CREATE TABLE "posts" (
        "id"              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
        "user_id"         UUID NOT NULL REFERENCES "users"("id") ON DELETE CASCADE,
        "content"         TEXT,
        "post_type"       "post_type_enum" NOT NULL DEFAULT 'text',
        "media_urls"      JSONB DEFAULT '[]',
        "community_id"    UUID REFERENCES "communities"("id") ON DELETE SET NULL,
        "visibility"      "post_visibility_enum" NOT NULL DEFAULT 'public',
        "is_pinned"       BOOLEAN NOT NULL DEFAULT false,
        "is_archived"     BOOLEAN NOT NULL DEFAULT false,
        "likes_count"     INTEGER NOT NULL DEFAULT 0,
        "comments_count"  INTEGER NOT NULL DEFAULT 0,
        "shares_count"    INTEGER NOT NULL DEFAULT 0,
        "views_count"     INTEGER NOT NULL DEFAULT 0,
        "created_at"      TIMESTAMP NOT NULL DEFAULT NOW(),
        "updated_at"      TIMESTAMP NOT NULL DEFAULT NOW()
      )
    `);
    await queryRunner.query(`CREATE INDEX "idx_posts_user"      ON "posts" ("user_id")`);
    await queryRunner.query(`CREATE INDEX "idx_posts_community" ON "posts" ("community_id")`);
    await queryRunner.query(`CREATE INDEX "idx_posts_created"   ON "posts" ("created_at" DESC)`);

    // ── comments ────────────────────────────────────────────────────────────
    await queryRunner.query(`
      CREATE TABLE "comments" (
        "id"                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
        "post_id"           UUID NOT NULL REFERENCES "posts"("id") ON DELETE CASCADE,
        "user_id"           UUID NOT NULL REFERENCES "users"("id") ON DELETE CASCADE,
        "parent_comment_id" UUID,
        "content"           TEXT NOT NULL,
        "likes_count"       INTEGER NOT NULL DEFAULT 0,
        "is_edited"         BOOLEAN NOT NULL DEFAULT false,
        "created_at"        TIMESTAMP NOT NULL DEFAULT NOW(),
        "updated_at"        TIMESTAMP NOT NULL DEFAULT NOW()
      )
    `);
    await queryRunner.query(`CREATE INDEX "idx_comments_post" ON "comments" ("post_id")`);
    await queryRunner.query(`CREATE INDEX "idx_comments_user" ON "comments" ("user_id")`);

    // ── reactions ────────────────────────────────────────────────────────────
    await queryRunner.query(`
      CREATE TABLE "reactions" (
        "id"            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
        "post_id"       UUID NOT NULL REFERENCES "posts"("id") ON DELETE CASCADE,
        "user_id"       UUID NOT NULL REFERENCES "users"("id") ON DELETE CASCADE,
        "reaction_type" "reaction_type_enum" NOT NULL DEFAULT 'like',
        "created_at"    TIMESTAMP NOT NULL DEFAULT NOW(),
        UNIQUE ("post_id", "user_id")
      )
    `);

    // ── properties (Kabarak Nest) ────────────────────────────────────────────
    await queryRunner.query(`
      CREATE TABLE "properties" (
        "id"              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
        "landlord_id"     UUID NOT NULL REFERENCES "users"("id") ON DELETE CASCADE,
        "title"           VARCHAR(255) NOT NULL,
        "description"     TEXT NOT NULL,
        "property_type"   "property_type_enum" NOT NULL,
        "address"         VARCHAR(500) NOT NULL,
        "city"            VARCHAR(100) NOT NULL,
        "neighborhood"    VARCHAR(100),
        "county"          VARCHAR(100),
        "latitude"        DECIMAL(10,8),
        "longitude"       DECIMAL(11,8),
        "price_per_month" DECIMAL(10,2) NOT NULL,
        "currency"        VARCHAR(10) NOT NULL DEFAULT 'KES',
        "deposit"         DECIMAL(10,2),
        "available_from"  DATE,
        "available_to"    DATE,
        "is_available"    BOOLEAN NOT NULL DEFAULT true,
        "is_verified"     BOOLEAN NOT NULL DEFAULT false,
        "is_featured"     BOOLEAN NOT NULL DEFAULT false,
        "bedrooms"        INTEGER NOT NULL DEFAULT 1,
        "bathrooms"       INTEGER NOT NULL DEFAULT 1,
        "max_occupants"   INTEGER NOT NULL DEFAULT 1,
        "rating_average"  DECIMAL(3,2) NOT NULL DEFAULT 0,
        "reviews_count"   INTEGER NOT NULL DEFAULT 0,
        "views_count"     INTEGER NOT NULL DEFAULT 0,
        "inquiries_count" INTEGER NOT NULL DEFAULT 0,
        "created_at"      TIMESTAMP NOT NULL DEFAULT NOW(),
        "updated_at"      TIMESTAMP NOT NULL DEFAULT NOW()
      )
    `);
    await queryRunner.query(`CREATE INDEX "idx_props_landlord"  ON "properties" ("landlord_id")`);
    await queryRunner.query(`CREATE INDEX "idx_props_city"      ON "properties" ("city")`);
    await queryRunner.query(`CREATE INDEX "idx_props_available" ON "properties" ("is_available")`);
    await queryRunner.query(`CREATE INDEX "idx_props_type"      ON "properties" ("property_type")`);
    await queryRunner.query(`CREATE INDEX "idx_props_price"     ON "properties" ("price_per_month")`);
    await queryRunner.query(`CREATE INDEX "idx_props_location"  ON "properties" USING GIST (
      point(longitude::float8, latitude::float8) gist_point_ops
    ) WHERE latitude IS NOT NULL AND longitude IS NOT NULL`);

    // ── property_images ──────────────────────────────────────────────────────
    await queryRunner.query(`
      CREATE TABLE "property_images" (
        "id"          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
        "property_id" UUID NOT NULL REFERENCES "properties"("id") ON DELETE CASCADE,
        "image_url"   TEXT NOT NULL,
        "is_primary"  BOOLEAN NOT NULL DEFAULT false,
        "sort_order"  INTEGER NOT NULL DEFAULT 0,
        "caption"     VARCHAR(255),
        "created_at"  TIMESTAMP NOT NULL DEFAULT NOW()
      )
    `);

    // ── property_amenities ───────────────────────────────────────────────────
    await queryRunner.query(`
      CREATE TABLE "property_amenities" (
        "id"           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
        "property_id"  UUID NOT NULL REFERENCES "properties"("id") ON DELETE CASCADE,
        "amenity_name" VARCHAR(100) NOT NULL,
        "category"     "amenity_category_enum" NOT NULL DEFAULT 'other'
      )
    `);

    // ── property_reviews ─────────────────────────────────────────────────────
    await queryRunner.query(`
      CREATE TABLE "property_reviews" (
        "id"                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
        "property_id"       UUID NOT NULL REFERENCES "properties"("id") ON DELETE CASCADE,
        "reviewer_id"       UUID NOT NULL REFERENCES "users"("id") ON DELETE CASCADE,
        "rating"            INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
        "content"           TEXT,
        "rating_cleanliness" INTEGER CHECK (rating_cleanliness BETWEEN 1 AND 5),
        "rating_value"      INTEGER CHECK (rating_value BETWEEN 1 AND 5),
        "rating_location"   INTEGER CHECK (rating_location BETWEEN 1 AND 5),
        "rating_landlord"   INTEGER CHECK (rating_landlord BETWEEN 1 AND 5),
        "created_at"        TIMESTAMP NOT NULL DEFAULT NOW(),
        "updated_at"        TIMESTAMP NOT NULL DEFAULT NOW(),
        UNIQUE ("property_id", "reviewer_id")
      )
    `);

    // ── accommodation_bookings ───────────────────────────────────────────────
    await queryRunner.query(`
      CREATE TABLE "accommodation_bookings" (
        "id"               UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
        "property_id"      UUID NOT NULL REFERENCES "properties"("id") ON DELETE CASCADE,
        "student_id"       UUID NOT NULL REFERENCES "users"("id") ON DELETE CASCADE,
        "status"           "booking_status_enum" NOT NULL DEFAULT 'inquiry',
        "move_in_date"     DATE,
        "move_out_date"    DATE,
        "message"          TEXT,
        "landlord_response" TEXT,
        "responded_at"     TIMESTAMP,
        "created_at"       TIMESTAMP NOT NULL DEFAULT NOW(),
        "updated_at"       TIMESTAMP NOT NULL DEFAULT NOW()
      )
    `);
    await queryRunner.query(`CREATE INDEX "idx_bookings_property" ON "accommodation_bookings" ("property_id")`);
    await queryRunner.query(`CREATE INDEX "idx_bookings_student"  ON "accommodation_bookings" ("student_id")`);

    // ── saved_listings ────────────────────────────────────────────────────────
    await queryRunner.query(`
      CREATE TABLE "saved_listings" (
        "id"          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
        "user_id"     UUID NOT NULL REFERENCES "users"("id") ON DELETE CASCADE,
        "property_id" UUID NOT NULL REFERENCES "properties"("id") ON DELETE CASCADE,
        "saved_at"    TIMESTAMP NOT NULL DEFAULT NOW(),
        UNIQUE ("user_id", "property_id")
      )
    `);

    // ── clubs ─────────────────────────────────────────────────────────────────
    await queryRunner.query(`
      CREATE TABLE "clubs" (
        "id"            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
        "name"          VARCHAR(150) NOT NULL,
        "description"   TEXT,
        "category"      "club_category_enum" NOT NULL DEFAULT 'other',
        "logo_url"      TEXT,
        "banner_url"    TEXT,
        "university"    VARCHAR(200),
        "is_verified"   BOOLEAN NOT NULL DEFAULT false,
        "is_active"     BOOLEAN NOT NULL DEFAULT true,
        "members_count" INTEGER NOT NULL DEFAULT 0,
        "created_by"    UUID REFERENCES "users"("id") ON DELETE SET NULL,
        "created_at"    TIMESTAMP NOT NULL DEFAULT NOW(),
        "updated_at"    TIMESTAMP NOT NULL DEFAULT NOW()
      )
    `);
    await queryRunner.query(`CREATE INDEX "idx_clubs_university" ON "clubs" ("university")`);

    // ── club_members ──────────────────────────────────────────────────────────
    await queryRunner.query(`
      CREATE TABLE "club_members" (
        "id"        UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
        "club_id"   UUID NOT NULL REFERENCES "clubs"("id") ON DELETE CASCADE,
        "user_id"   UUID NOT NULL REFERENCES "users"("id") ON DELETE CASCADE,
        "role"      "club_member_role_enum" NOT NULL DEFAULT 'member',
        "joined_at" TIMESTAMP NOT NULL DEFAULT NOW(),
        UNIQUE ("club_id", "user_id")
      )
    `);

    // ── events ────────────────────────────────────────────────────────────────
    await queryRunner.query(`
      CREATE TABLE "events" (
        "id"              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
        "organizer_id"    UUID NOT NULL REFERENCES "users"("id") ON DELETE CASCADE,
        "club_id"         UUID REFERENCES "clubs"("id") ON DELETE SET NULL,
        "title"           VARCHAR(255) NOT NULL,
        "description"     TEXT NOT NULL,
        "cover_image"     TEXT,
        "location"        VARCHAR(500),
        "venue_latitude"  DECIMAL(10,8),
        "venue_longitude" DECIMAL(11,8),
        "start_time"      TIMESTAMP NOT NULL,
        "end_time"        TIMESTAMP,
        "category"        "event_category_enum" NOT NULL DEFAULT 'other',
        "is_ticketed"     BOOLEAN NOT NULL DEFAULT false,
        "ticket_price"    DECIMAL(10,2),
        "max_attendees"   INTEGER,
        "attendees_count" INTEGER NOT NULL DEFAULT 0,
        "is_cancelled"    BOOLEAN NOT NULL DEFAULT false,
        "is_featured"     BOOLEAN NOT NULL DEFAULT false,
        "university"      VARCHAR(200),
        "created_at"      TIMESTAMP NOT NULL DEFAULT NOW(),
        "updated_at"      TIMESTAMP NOT NULL DEFAULT NOW()
      )
    `);
    await queryRunner.query(`CREATE INDEX "idx_events_organizer"  ON "events" ("organizer_id")`);
    await queryRunner.query(`CREATE INDEX "idx_events_start_time" ON "events" ("start_time")`);
    await queryRunner.query(`CREATE INDEX "idx_events_category"   ON "events" ("category")`);

    // ── event_attendees ───────────────────────────────────────────────────────
    await queryRunner.query(`
      CREATE TABLE "event_attendees" (
        "id"            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
        "event_id"      UUID NOT NULL REFERENCES "events"("id") ON DELETE CASCADE,
        "user_id"       UUID NOT NULL REFERENCES "users"("id") ON DELETE CASCADE,
        "status"        "attendee_status_enum" NOT NULL DEFAULT 'going',
        "checked_in_at" TIMESTAMP,
        "qr_code"       TEXT,
        "created_at"    TIMESTAMP NOT NULL DEFAULT NOW(),
        UNIQUE ("event_id", "user_id")
      )
    `);

    // ── marketplace_listings ──────────────────────────────────────────────────
    await queryRunner.query(`
      CREATE TABLE "marketplace_listings" (
        "id"            UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
        "seller_id"     UUID NOT NULL REFERENCES "users"("id") ON DELETE CASCADE,
        "title"         VARCHAR(255) NOT NULL,
        "description"   TEXT NOT NULL,
        "category"      "listing_category_enum" NOT NULL DEFAULT 'other',
        "condition"     "listing_condition_enum",
        "price"         DECIMAL(10,2) NOT NULL,
        "currency"      VARCHAR(10) NOT NULL DEFAULT 'KES',
        "is_negotiable" BOOLEAN NOT NULL DEFAULT false,
        "image_urls"    JSONB DEFAULT '[]',
        "status"        "listing_status_enum" NOT NULL DEFAULT 'active',
        "university"    VARCHAR(200),
        "views_count"   INTEGER NOT NULL DEFAULT 0,
        "is_featured"   BOOLEAN NOT NULL DEFAULT false,
        "created_at"    TIMESTAMP NOT NULL DEFAULT NOW(),
        "updated_at"    TIMESTAMP NOT NULL DEFAULT NOW()
      )
    `);
    await queryRunner.query(`CREATE INDEX "idx_listings_seller"   ON "marketplace_listings" ("seller_id")`);
    await queryRunner.query(`CREATE INDEX "idx_listings_category" ON "marketplace_listings" ("category")`);
    await queryRunner.query(`CREATE INDEX "idx_listings_status"   ON "marketplace_listings" ("status")`);
    await queryRunner.query(`CREATE INDEX "idx_listings_price"    ON "marketplace_listings" ("price")`);
    await queryRunner.query(`CREATE INDEX "idx_listings_title_trgm" ON "marketplace_listings" USING GIN ("title" gin_trgm_ops)`);

    // ── conversations ─────────────────────────────────────────────────────────
    await queryRunner.query(`
      CREATE TABLE "conversations" (
        "id"              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
        "type"            "conversation_type_enum" NOT NULL DEFAULT 'direct',
        "name"            VARCHAR(150),
        "avatar_url"      TEXT,
        "last_message_at" TIMESTAMP,
        "messages_count"  INTEGER NOT NULL DEFAULT 0,
        "created_at"      TIMESTAMP NOT NULL DEFAULT NOW(),
        "updated_at"      TIMESTAMP NOT NULL DEFAULT NOW()
      )
    `);

    // ── conversation_participants ─────────────────────────────────────────────
    await queryRunner.query(`
      CREATE TABLE "conversation_participants" (
        "id"              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
        "conversation_id" UUID NOT NULL REFERENCES "conversations"("id") ON DELETE CASCADE,
        "user_id"         UUID NOT NULL REFERENCES "users"("id") ON DELETE CASCADE,
        "last_read_at"    TIMESTAMP,
        "is_muted"        BOOLEAN NOT NULL DEFAULT false,
        "is_admin"        BOOLEAN NOT NULL DEFAULT false,
        "joined_at"       TIMESTAMP NOT NULL DEFAULT NOW(),
        UNIQUE ("conversation_id", "user_id")
      )
    `);
    await queryRunner.query(`CREATE INDEX "idx_conv_part_user" ON "conversation_participants" ("user_id")`);

    // ── messages ──────────────────────────────────────────────────────────────
    await queryRunner.query(`
      CREATE TABLE "messages" (
        "id"              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
        "conversation_id" UUID NOT NULL REFERENCES "conversations"("id") ON DELETE CASCADE,
        "sender_id"       UUID NOT NULL REFERENCES "users"("id") ON DELETE CASCADE,
        "content"         TEXT,
        "message_type"    "message_type_enum" NOT NULL DEFAULT 'text',
        "media_url"       TEXT,
        "is_read"         BOOLEAN NOT NULL DEFAULT false,
        "is_deleted"      BOOLEAN NOT NULL DEFAULT false,
        "reply_to_id"     UUID,
        "created_at"      TIMESTAMP NOT NULL DEFAULT NOW(),
        "updated_at"      TIMESTAMP NOT NULL DEFAULT NOW()
      )
    `);
    await queryRunner.query(`CREATE INDEX "idx_messages_conv"    ON "messages" ("conversation_id")`);
    await queryRunner.query(`CREATE INDEX "idx_messages_sender"  ON "messages" ("sender_id")`);
    await queryRunner.query(`CREATE INDEX "idx_messages_created" ON "messages" ("created_at" DESC)`);

    // ── notifications ─────────────────────────────────────────────────────────
    await queryRunner.query(`
      CREATE TABLE "notifications" (
        "id"         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
        "user_id"    UUID NOT NULL REFERENCES "users"("id") ON DELETE CASCADE,
        "actor_id"   UUID REFERENCES "users"("id") ON DELETE SET NULL,
        "type"       "notification_type_enum" NOT NULL,
        "title"      VARCHAR(255) NOT NULL,
        "body"       TEXT NOT NULL,
        "data"       JSONB,
        "is_read"    BOOLEAN NOT NULL DEFAULT false,
        "read_at"    TIMESTAMP,
        "created_at" TIMESTAMP NOT NULL DEFAULT NOW()
      )
    `);
    await queryRunner.query(`CREATE INDEX "idx_notifs_user"    ON "notifications" ("user_id")`);
    await queryRunner.query(`CREATE INDEX "idx_notifs_is_read" ON "notifications" ("is_read")`);
    await queryRunner.query(`CREATE INDEX "idx_notifs_created" ON "notifications" ("created_at" DESC)`);

    // ── user_points ───────────────────────────────────────────────────────────
    await queryRunner.query(`
      CREATE TABLE "user_points" (
        "user_id"        UUID PRIMARY KEY REFERENCES "users"("id") ON DELETE CASCADE,
        "total_points"   INTEGER NOT NULL DEFAULT 0,
        "monthly_points" INTEGER NOT NULL DEFAULT 0,
        "level"          "user_level_enum" NOT NULL DEFAULT 'newcomer',
        "posts_created"  INTEGER NOT NULL DEFAULT 0,
        "comments_made"  INTEGER NOT NULL DEFAULT 0,
        "events_attended" INTEGER NOT NULL DEFAULT 0,
        "helpful_answers" INTEGER NOT NULL DEFAULT 0,
        "updated_at"     TIMESTAMP NOT NULL DEFAULT NOW()
      )
    `);
    await queryRunner.query(`CREATE INDEX "idx_points_total"   ON "user_points" ("total_points" DESC)`);
    await queryRunner.query(`CREATE INDEX "idx_points_monthly" ON "user_points" ("monthly_points" DESC)`);

    // ── badges ────────────────────────────────────────────────────────────────
    await queryRunner.query(`
      CREATE TABLE "badges" (
        "id"              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
        "name"            VARCHAR(100) NOT NULL UNIQUE,
        "description"     VARCHAR(255) NOT NULL,
        "icon_url"        TEXT,
        "category"        "badge_category_enum" NOT NULL DEFAULT 'special',
        "points_reward"   INTEGER NOT NULL DEFAULT 0,
        "criteria_type"   VARCHAR(100) NOT NULL,
        "criteria_value"  INTEGER NOT NULL DEFAULT 1,
        "is_active"       BOOLEAN NOT NULL DEFAULT true,
        "created_at"      TIMESTAMP NOT NULL DEFAULT NOW()
      )
    `);

    // ── user_badges ───────────────────────────────────────────────────────────
    await queryRunner.query(`
      CREATE TABLE "user_badges" (
        "id"        UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
        "user_id"   UUID NOT NULL REFERENCES "users"("id") ON DELETE CASCADE,
        "badge_id"  UUID NOT NULL REFERENCES "badges"("id") ON DELETE CASCADE,
        "earned_at" TIMESTAMP NOT NULL DEFAULT NOW(),
        UNIQUE ("user_id", "badge_id")
      )
    `);

    // ── Full-text search indexes ───────────────────────────────────────────────
    await queryRunner.query(`CREATE INDEX "idx_posts_content_trgm"  ON "posts"  USING GIN (content gin_trgm_ops) WHERE content IS NOT NULL`);
    await queryRunner.query(`CREATE INDEX "idx_users_name_trgm"     ON "users"  USING GIN ((first_name || ' ' || last_name) gin_trgm_ops)`);
    await queryRunner.query(`CREATE INDEX "idx_events_title_trgm"   ON "events" USING GIN (title gin_trgm_ops)`);
    await queryRunner.query(`CREATE INDEX "idx_props_title_trgm"    ON "properties" USING GIN (title gin_trgm_ops)`);
  }

  public async down(queryRunner: QueryRunner): Promise<void> {
    // Drop tables in reverse dependency order
    const tables = [
      'user_badges', 'badges', 'user_points', 'notifications',
      'messages', 'conversation_participants', 'conversations',
      'marketplace_listings', 'event_attendees', 'events',
      'club_members', 'clubs', 'saved_listings', 'accommodation_bookings',
      'property_reviews', 'property_amenities', 'property_images', 'properties',
      'reactions', 'comments', 'posts', 'communities',
      'user_follows', 'refresh_tokens', 'users',
    ];
    for (const table of tables) {
      await queryRunner.query(`DROP TABLE IF EXISTS "${table}" CASCADE`);
    }
    const enums = [
      'user_role_enum', 'post_type_enum', 'post_visibility_enum', 'reaction_type_enum',
      'property_type_enum', 'amenity_category_enum', 'booking_status_enum',
      'event_category_enum', 'attendee_status_enum', 'club_category_enum',
      'club_member_role_enum', 'listing_category_enum', 'listing_condition_enum',
      'listing_status_enum', 'conversation_type_enum', 'message_type_enum',
      'notification_type_enum', 'user_level_enum', 'badge_category_enum',
    ];
    for (const e of enums) {
      await queryRunner.query(`DROP TYPE IF EXISTS "${e}"`);
    }
  }
}
