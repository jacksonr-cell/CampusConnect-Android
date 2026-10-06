/**
 * Lightweight user-agent parser — no external dependency needed.
 * Extracts device, browser, and OS info for login history / sessions.
 */
export interface ParsedUserAgent {
  device : string;
  browser: string;
  os     : string;
}

export function parseUserAgent(userAgent: string | undefined): ParsedUserAgent {
  if (!userAgent) {
    return { device: 'Unknown', browser: 'Unknown', os: 'Unknown' };
  }

  const ua = userAgent.toLowerCase();

  // OS detection
  let os = 'Unknown';
  if (ua.includes('windows'))        os = 'Windows';
  else if (ua.includes('mac os'))    os = 'macOS';
  else if (ua.includes('android'))   os = 'Android';
  else if (ua.includes('iphone') || ua.includes('ipad')) os = 'iOS';
  else if (ua.includes('linux'))     os = 'Linux';

  // Browser detection
  let browser = 'Unknown';
  if (ua.includes('edg/'))           browser = 'Edge';
  else if (ua.includes('chrome/'))   browser = 'Chrome';
  else if (ua.includes('firefox/'))  browser = 'Firefox';
  else if (ua.includes('safari/') && !ua.includes('chrome')) browser = 'Safari';
  else if (ua.includes('okhttp') || ua.includes('campusconnect')) browser = 'CampusConnect App';

  // Device detection
  let device = 'Desktop';
  if (ua.includes('mobile') || ua.includes('android')) device = 'Mobile';
  else if (ua.includes('tablet') || ua.includes('ipad')) device = 'Tablet';

  return { device, browser, os };
}

/** Builds a human-readable device label, e.g. "Chrome on Windows". */
export function formatDeviceName(parsed: ParsedUserAgent): string {
  return `${parsed.browser} on ${parsed.os}`;
}
