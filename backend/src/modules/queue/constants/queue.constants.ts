export const QUEUE_NAMES = {
  EMAIL        : 'campusconnect:email',
  NOTIFICATIONS: 'campusconnect:notifications',
  MEDIA        : 'campusconnect:media',
  CLEANUP      : 'campusconnect:cleanup',
} as const;

export const JOB_NAMES = {
  // Email jobs
  SEND_VERIFICATION_EMAIL  : 'send-verification-email',
  SEND_PASSWORD_RESET      : 'send-password-reset',
  SEND_WELCOME_EMAIL       : 'send-welcome-email',
  SEND_NOTIFICATION_EMAIL  : 'send-notification-email',
  SEND_BOOKING_UPDATE      : 'send-booking-update',
  SEND_EVENT_REMINDER      : 'send-event-reminder',

  // Notification jobs
  PUSH_NOTIFICATION        : 'push-notification',
  IN_APP_NOTIFICATION      : 'in-app-notification',
  BULK_NOTIFICATION        : 'bulk-notification',

  // Media jobs
  PROCESS_AVATAR           : 'process-avatar',
  PROCESS_POST_MEDIA       : 'process-post-media',
  PROCESS_PROPERTY_IMAGE   : 'process-property-image',
  GENERATE_THUMBNAIL       : 'generate-thumbnail',

  // Cleanup jobs
  PURGE_EXPIRED_TOKENS     : 'purge-expired-tokens',
  PURGE_OLD_NOTIFICATIONS  : 'purge-old-notifications',
  DEACTIVATE_EXPIRED_LISTINGS: 'deactivate-expired-listings',
} as const;

export const QUEUE_DEFAULTS = {
  attempts          : 3,
  backoff           : { type: 'exponential' as const, delay: 2000 },
  removeOnComplete  : { count: 100 },
  removeOnFail      : { count: 500 },
};
