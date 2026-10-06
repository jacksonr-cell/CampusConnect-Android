const BRAND_COLOR  = '#7C3AED';
const ACCENT_COLOR = '#A855F7';
const BG_COLOR     = '#0A0A0A';
const SURFACE      = '#121212';
const TEXT_COLOR   = '#FFFFFF';
const MUTED_COLOR  = '#B3B3B3';

function baseTemplate(content: string, preheader = ''): string {
  return `
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>CampusConnect</title>
  <style>
    * { margin:0; padding:0; box-sizing:border-box; }
    body { background:${BG_COLOR}; font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif; color:${TEXT_COLOR}; }
    .container { max-width:600px; margin:0 auto; padding:40px 24px; }
    .logo-wrap { text-align:center; margin-bottom:36px; }
    .logo-box { display:inline-block; background:linear-gradient(135deg,${BRAND_COLOR},${ACCENT_COLOR}); border-radius:16px; padding:12px 20px; font-size:22px; font-weight:900; color:#fff; letter-spacing:1px; }
    .card { background:${SURFACE}; border-radius:16px; padding:36px; border:1px solid rgba(255,255,255,0.08); }
    h1 { font-size:24px; font-weight:700; margin-bottom:16px; }
    p  { font-size:15px; line-height:1.7; color:${MUTED_COLOR}; margin-bottom:16px; }
    p.highlight { color:${TEXT_COLOR}; }
    .btn { display:block; background:linear-gradient(135deg,${BRAND_COLOR},${ACCENT_COLOR}); color:#fff; text-align:center; text-decoration:none; padding:14px 28px; border-radius:50px; font-size:16px; font-weight:700; margin:28px auto; width:fit-content; }
    .otp  { font-size:42px; font-weight:900; letter-spacing:12px; color:${TEXT_COLOR}; text-align:center; padding:24px; background:rgba(124,58,237,0.12); border-radius:12px; margin:24px 0; border:1px solid rgba(124,58,237,0.3); }
    .footer { text-align:center; margin-top:32px; font-size:12px; color:#555; }
    .divider { border:none; border-top:1px solid rgba(255,255,255,0.06); margin:24px 0; }
  </style>
</head>
<body>
  ${preheader ? `<div style="display:none;max-height:0;overflow:hidden;">${preheader}</div>` : ''}
  <div class="container">
    <div class="logo-wrap"><div class="logo-box">CC</div></div>
    <div class="card">${content}</div>
    <div class="footer">
      <p>© ${new Date().getFullYear()} CampusConnect. All rights reserved.</p>
      <p style="margin-top:8px;">Kabarak University Student Platform</p>
    </div>
  </div>
</body>
</html>`;
}

export function verificationEmailTemplate(firstName: string, token: string, appUrl: string): string {
  const link = `${appUrl}/auth/verify-email?token=${token}`;
  return baseTemplate(`
    <h1>Verify your email ✉️</h1>
    <p class="highlight">Hi ${firstName},</p>
    <p>Thanks for signing up to CampusConnect! To activate your account, please verify your email address.</p>
    <a href="${link}" class="btn">Verify Email Address</a>
    <hr class="divider" />
    <p>Or use this OTP code:</p>
    <div class="otp">${token.slice(0, 6).toUpperCase()}</div>
    <p>This link expires in <strong style="color:${TEXT_COLOR}">24 hours</strong>. If you didn't sign up, you can safely ignore this email.</p>
  `, 'Verify your CampusConnect email address');
}

export function passwordResetTemplate(firstName: string, token: string, appUrl: string): string {
  const link = `${appUrl}/auth/reset-password?token=${token}`;
  return baseTemplate(`
    <h1>Reset your password 🔒</h1>
    <p class="highlight">Hi ${firstName},</p>
    <p>We received a request to reset your CampusConnect password. Click the button below to set a new one.</p>
    <a href="${link}" class="btn">Reset Password</a>
    <hr class="divider" />
    <p>This link expires in <strong style="color:${TEXT_COLOR}">1 hour</strong>.</p>
    <p>If you didn't request a password reset, please change your password immediately and contact support.</p>
  `, 'Reset your CampusConnect password');
}

export function welcomeEmailTemplate(firstName: string, appUrl: string): string {
  return baseTemplate(`
    <h1>Welcome to CampusConnect 🎉</h1>
    <p class="highlight">Hi ${firstName}!</p>
    <p>You're officially part of the CampusConnect community. Here's what you can do:</p>
    <ul style="margin:16px 0;padding-left:20px;color:${MUTED_COLOR};line-height:2;">
      <li>🏠 Browse accommodation on <strong style="color:${TEXT_COLOR}">Kabarak Nest</strong></li>
      <li>📱 Share posts on the <strong style="color:${TEXT_COLOR}">social feed</strong></li>
      <li>🎭 Discover and join <strong style="color:${TEXT_COLOR}">campus events</strong></li>
      <li>🤝 Connect with <strong style="color:${TEXT_COLOR}">clubs & societies</strong></li>
      <li>🛍️ Buy and sell on the <strong style="color:${TEXT_COLOR}">marketplace</strong></li>
    </ul>
    <a href="${appUrl}" class="btn">Explore CampusConnect</a>
  `, 'Your CampusConnect account is ready!');
}

export function bookingUpdateTemplate(
  firstName: string,
  propertyTitle: string,
  status: string,
  message: string | undefined,
  appUrl: string,
): string {
  const statusLabels: Record<string, string> = {
    accepted : '✅ Accepted',
    rejected : '❌ Declined',
    pending  : '⏳ Under Review',
    completed: '🏠 Move-In Confirmed',
    cancelled: '🚫 Cancelled',
  };
  const statusLabel = statusLabels[status] || status;
  return baseTemplate(`
    <h1>Booking Update</h1>
    <p class="highlight">Hi ${firstName},</p>
    <p>Your booking request for <strong style="color:${TEXT_COLOR}">${propertyTitle}</strong> has been updated.</p>
    <div style="background:rgba(124,58,237,0.1);border:1px solid rgba(124,58,237,0.3);border-radius:12px;padding:20px;margin:20px 0;text-align:center;">
      <p style="font-size:20px;font-weight:700;color:${TEXT_COLOR};margin:0;">${statusLabel}</p>
    </div>
    ${message ? `<p><strong style="color:${TEXT_COLOR}">Landlord message:</strong><br/>${message}</p>` : ''}
    <a href="${appUrl}/accommodation" class="btn">View Booking Details</a>
  `, `Your accommodation request has been ${status}`);
}

export function eventReminderTemplate(
  firstName: string,
  eventTitle: string,
  startTime: Date,
  location: string | undefined,
  appUrl: string,
): string {
  const formatted = new Intl.DateTimeFormat('en-KE', {
    dateStyle: 'full',
    timeStyle: 'short',
    timeZone : 'Africa/Nairobi',
  }).format(new Date(startTime));
  return baseTemplate(`
    <h1>Event Reminder 📅</h1>
    <p class="highlight">Hi ${firstName},</p>
    <p>This is a reminder for an upcoming event you've RSVP'd to:</p>
    <div style="background:rgba(124,58,237,0.1);border:1px solid rgba(124,58,237,0.3);border-radius:12px;padding:24px;margin:20px 0;">
      <p style="font-size:18px;font-weight:700;color:${TEXT_COLOR};margin-bottom:8px;">${eventTitle}</p>
      <p style="margin:0;">🗓️ ${formatted}</p>
      ${location ? `<p style="margin:4px 0 0;">📍 ${location}</p>` : ''}
    </div>
    <a href="${appUrl}/events" class="btn">View Event Details</a>
  `, `Reminder: ${eventTitle} is coming up!`);
}
