/**
 * Reputation points awarded per action.
 * Used by GamificationService.awardPoints(userId, POINTS.ACTION).
 */
export const POINTS = {
  CREATE_POST: 10,
  RECEIVE_LIKE: 2,
  LEAVE_COMMENT: 5,
  RECEIVE_COMMENT: 3,
  FOLLOW_SOMEONE: 1,
  GAIN_FOLLOWER: 5,
  RSVP_TO_EVENT: 8,
  ATTEND_EVENT: 15,
  JOIN_CLUB: 5,
  CREATE_LISTING: 8,
  SELL_ITEM: 20,
  LEAVE_REVIEW: 12,
  DAILY_LOGIN: 3,
} as const;
