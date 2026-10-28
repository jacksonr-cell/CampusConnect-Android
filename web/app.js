/* CampusConnect web client. Configure API_BASE_URL before deploying a connected build. */
const CONFIG = { API_BASE_URL: window.CAMPUSCONNECT_API_BASE_URL || localStorage.getItem('cc-api-url') || 'http://localhost:3000/api/v1' };
const $ = (selector, root = document) => root.querySelector(selector);
const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];
const state = { token: localStorage.getItem('cc-access-token') || '', refresh: localStorage.getItem('cc-refresh-token') || '', user: null, posts: [], events: [], filter: 'all', authMode: 'login', toastTimer: null };
const demoPosts = [
  { id: 'demo-1', user: { firstName: 'Amina', lastName: 'Muthoni', username: 'aminam', university: 'Kabarak University' }, content: 'There’s something about the campus getting quiet after 6pm that makes everything feel possible. Long study session, good playlist, and the last bit of sunlight through the library windows. ✨', createdAt: new Date(Date.now() - 18 * 60000).toISOString(), community: 'Campus life', likesCount: 24, commentsCount: 6, art: 'campus', following: true, reactions: ['✨', '💜', '🙌'] },
  { id: 'demo-2', user: { firstName: 'Brian', lastName: 'Kiptoo', username: 'briank' }, content: 'The sunset near the old amphitheatre today was unreal. Sometimes the best plan is having no plan at all 🌅', createdAt: new Date(Date.now() - 57 * 60000).toISOString(), community: 'Little things', likesCount: 18, commentsCount: 3, art: 'sunset', following: true, reactions: ['💛', '🌅'] },
  { id: 'demo-3', user: { firstName: 'Njeri', lastName: 'Wanjiku', username: 'njeri.w' }, content: 'A reminder for anyone who needs one: you’re doing better than you think. Keep going, we’re all figuring it out together 💜\n\n#campuslife #littlewins', createdAt: new Date(Date.now() - 3 * 3600000).toISOString(), community: 'Creative minds', likesCount: 42, commentsCount: 11, art: 'study', following: false, reactions: ['💜', '✨', '🥹'] },
];
const demoEvents = [
  { title: 'Open mic under the stars', date: '30', month: 'OCT', time: '6:30 PM', location: 'The Amphitheatre', type: 'Arts & culture' },
  { title: 'Build night: make something', date: '01', month: 'NOV', time: '5:00 PM', location: 'Innovation Hub', type: 'Tech & innovation' },
  { title: 'Sunday morning trail walk', date: '02', month: 'NOV', time: '7:15 AM', location: 'Main gate', type: 'Outdoors' },
];

function initials(user = {}) { return `${(user.firstName || user.username || 'C')[0]}${(user.lastName || '')[0] || ''}`.toUpperCase(); }
function fullName(user = {}) { return [user.firstName, user.lastName].filter(Boolean).join(' ') || user.username || 'Campus friend'; }
function safe(value = '') { const el = document.createElement('span'); el.textContent = String(value); return el.innerHTML; }
function timeAgo(value) { const minutes = Math.max(1, Math.floor((Date.now() - new Date(value).getTime()) / 60000)); if (minutes < 60) return `${minutes}m ago`; const hours = Math.floor(minutes / 60); if (hours < 24) return `${hours}h ago`; return `${Math.floor(hours / 24)}d ago`; }
function unwrap(result) { return result?.data?.data ?? result?.data ?? result; }
function toast(message) { const node = $('#toast'); node.textContent = message; node.classList.add('show'); clearTimeout(state.toastTimer); state.toastTimer = setTimeout(() => node.classList.remove('show'), 2600); }
function avatarStyle(user) { return user.avatarUrl ? `style="background-image:url('${safe(user.avatarUrl)}');background-size:cover;color:transparent"` : ''; }
function apiUrl(path) { return `${CONFIG.API_BASE_URL.replace(/\/$/, '')}${path}`; }
async function api(path, { method = 'GET', body, auth = true } = {}) {
  const headers = { 'Content-Type': 'application/json' };
  if (auth && state.token) headers.Authorization = `Bearer ${state.token}`;
  if (state.refresh) headers['x-refresh-token'] = state.refresh;
  const response = await fetch(apiUrl(path), { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
  const payload = await response.json().catch(() => ({}));
  if (!response.ok) { const message = payload.message; throw new Error(Array.isArray(message) ? message.join('. ') : message || `Request failed (${response.status})`); }
  return unwrap(payload);
}

function normalizePost(post) {
  const user = post.user || post.author || {};
  const reactions = post.reactionsBreakdown || {};
  const total = Object.values(reactions).reduce((sum, n) => sum + (Number(n) || 0), 0);
  return { ...post, user, likesCount: post.likesCount ?? post.reactionsCount ?? total, commentsCount: post.commentsCount ?? post.comments?.length ?? 0, reactions: Object.keys(reactions).length ? Object.keys(reactions).slice(0, 3).map(type => ({ LIKE: '💜', LOVE: '💜', LAUGH: '😂', WOW: '😮', SAD: '🥹', ANGRY: '😤' })[type] || '✨') : ['💜', '✨'], following: true };
}
function postMarkup(post) {
  const author = post.user || {};
  const text = safe(post.content || post.caption || '');
  const highlighted = text.replace(/(^|\s)(#[\p{L}\d_]+)/gu, '$1<span class="hashtag">$2</span>');
  const isDemo = String(post.id).startsWith('demo-');
  const likes = Number(post.likesCount || 0);
  const liked = Boolean(post.myReaction);
  const art = post.art && ['campus', 'sunset', 'study'].includes(post.art) ? `<div class="post-art art-${post.art}">${post.art === 'campus' ? '<span class="art-campus-label">A MOMENT AT KABARAK</span>' : ''}</div>` : (post.mediaUrls || []).filter(url => typeof url === 'string').slice(0, 1).map(url => `<img class="post-image" src="${safe(url)}" alt="Shared by ${safe(fullName(author))}" loading="lazy">`).join('');
  const reactions = post.reactions || ['💜', '✨'];
  return `<article class="post-card" data-post-id="${safe(post.id)}" data-following="${post.following !== false}"><div class="post-top"><span class="avatar post-avatar" ${avatarStyle(author)}>${avatarStyle(author) ? '' : safe(initials(author))}</span><div class="post-identity"><div class="post-name-line"><strong class="post-name">${safe(fullName(author))}</strong>${author.isVerified ? '<span class="verified">✦</span>' : ''}<span class="post-handle">@${safe(author.username || 'student')}</span></div><span class="post-time">${safe(timeAgo(post.createdAt || post.created_at || Date.now()))}</span></div>${post.community ? `<span class="post-circle">${safe(post.community.name || post.community)}</span>` : ''}<button class="post-menu" aria-label="More post options">···</button></div><p class="post-text">${highlighted}</p>${art}<div class="post-reactions"><span class="reaction-bubbles">${reactions.slice(0, 3).map(emoji => `<span>${safe(emoji)}</span>`).join('')}</span><span>${likes || 0} people feel this</span><span>·</span><span>${Number(post.commentsCount || 0)} replies</span></div><div class="post-actions"><button class="post-action ${liked ? 'liked' : ''}" data-action="react"><span>${liked ? '♥' : '♡'}</span> Feel this</button><button class="post-action" data-action="comment"><span>◯</span> Reply</button><button class="post-action" data-action="share"><span>↗</span> Share</button><button class="post-action ${post.saved ? 'saved' : ''}" data-action="save" aria-label="Save post"><span>${post.saved ? '▣' : '♧'}</span></button></div></article>`;
}
function renderPosts() {
  const container = $('#posts-container');
  const query = ($('#search-input').value || '').trim().toLowerCase();
  const posts = state.posts.filter(post => {
    const inFilter = state.filter === 'all' || (state.filter === 'following' ? post.following !== false : Boolean(post.community));
    const text = `${fullName(post.user)} ${post.content || ''} ${post.community?.name || post.community || ''}`.toLowerCase();
    const inSearch = !query || text.includes(query);
    const inSaved = location.hash === '#saved' ? post.saved : true;
    return inFilter && inSearch && inSaved;
  });
  container.innerHTML = posts.length ? posts.map(postMarkup).join('') : `<div class="empty-state"><span>✳</span><strong>Nothing here just yet.</strong><p>Try another search or switch up your feed.</p></div>`;
}
function renderEvents(events = state.events) {
  $('#events-list').innerHTML = events.slice(0, 3).map((event, index) => {
    const date = new Date(event.startsAt || event.startDate || event.date || Date.now() + (index + 2) * 86400000);
    const day = event.dateLabel || String(date.getDate()).padStart(2, '0');
    const month = event.monthLabel || date.toLocaleString('en', { month: 'short' }).toUpperCase();
    return `<div class="event-row"><span class="event-date"><strong>${safe(day)}</strong><small>${safe(month)}</small></span><span class="event-copy"><strong>${safe(event.title || event.name || 'Campus gathering')}</strong><small class="event-type"><i></i>${safe(event.category || event.type || event.location || 'Campus event')}</small></span></div>`;
  }).join('') || '<p class="empty-events">New campus moments are on their way.</p>';
}
async function loadCampus() {
  if (!state.token) { state.posts = demoPosts.map(post => ({ ...post })); state.events = demoEvents; renderPosts(); renderEvents(); return; }
  try {
    const [feed, events] = await Promise.all([api('/posts/feed?page=1&limit=20'), api('/events?page=1&limit=3')]);
    state.posts = (feed.items || feed.data || (Array.isArray(feed) ? feed : [])).map(normalizePost);
    state.events = events.items || events.data || (Array.isArray(events) ? events : []);
    renderPosts(); renderEvents();
  } catch (error) {
    if (/401|unauthori[sz]ed|token/i.test(error.message)) { logout(false); state.posts = demoPosts.map(post => ({ ...post })); state.events = demoEvents; renderPosts(); renderEvents(); toast('Your session ended. Sign in again to reconnect.'); }
    else { state.posts = demoPosts.map(post => ({ ...post })); state.events = demoEvents; renderPosts(); renderEvents(); toast('Showing a campus preview. The API could not be reached.'); }
  }
}
async function refreshUser() {
  if (!state.token) return;
  try { state.user = await api('/auth/me'); const user = state.user; $('.profile-copy strong').textContent = fullName(user); $('.profile-copy small').textContent = [user.course || user.department || 'Student', user.yearOfStudy ? `Year ${user.yearOfStudy}` : ''].filter(Boolean).join(' · ') || 'CampusConnect student'; $('.profile-mini .avatar').textContent = initials(user); $('.top-avatar').textContent = initials(user); }
  catch { state.user = null; }
}
function logout(show = true) { state.token = ''; state.refresh = ''; state.user = null; localStorage.removeItem('cc-access-token'); localStorage.removeItem('cc-refresh-token'); if (show) toast('You’re signed out. See you around campus.'); }

function openAuth(mode = 'login') { state.authMode = mode; updateAuthForm(); $('#auth-dialog').showModal(); }
function updateAuthForm() {
  const register = state.authMode === 'register';
  $('#auth-title').textContent = register ? 'Find your people.' : 'Come on in.';
  $('#auth-description').textContent = register ? 'A campus full of possibility is waiting.' : 'Sign in to make your campus feel a little smaller.';
  $('#auth-switch').textContent = register ? 'Already part of the community? Sign in' : 'New here? Create an account';
  $('#auth-switch').dataset.mode = register ? 'login' : 'register';
  ['username', 'first-name', 'last-name'].forEach(field => $(`#${field}-field`).hidden = !register);
  $('#auth-form [name="password"]').autocomplete = register ? 'new-password' : 'current-password';
  $('.auth-submit').innerHTML = `${register ? 'Create account' : 'Sign in'} <span>↗</span>`;
}
function bind() {
  $('#composer-prompt').addEventListener('click', () => $('#post-dialog').showModal());
  $('#open-composer').addEventListener('click', () => $('#post-dialog').showModal());
  $('#mobile-create').addEventListener('click', () => $('#post-dialog').showModal());
  $('#add-photo-button').addEventListener('click', () => toast('Photo sharing will be available when uploads are connected.'));
  $('#add-event-button').addEventListener('click', () => toast('Event creation is coming soon. You can browse campus events now.'));
  $('#add-story').addEventListener('click', () => toast('Stories are coming soon. In the meantime, share a post with your campus.'));
  $('#all-stories').addEventListener('click', () => toast('You’re all caught up on campus stories.'));
  $('#events-more').addEventListener('click', () => location.hash = '#events');
  $('#see-events').addEventListener('click', () => { location.hash = '#events'; toast('More campus events are on their way.'); });
  ['explore-button', 'campus-pulse-link', 'circles-more', 'see-circles'].forEach(id => $(`#${id}`).addEventListener('click', () => toast('Your campus discovery page is coming soon.')));
  ['profile-button', 'top-profile'].forEach(id => $(`#${id}`).addEventListener('click', () => state.token ? toast('Profile settings are coming soon.') : openAuth('login')));
  $('#notifications-button').addEventListener('click', () => toast('You’re all caught up.'));
  $$('.join-button').forEach(button => button.addEventListener('click', () => { button.classList.toggle('joined'); button.textContent = button.classList.contains('joined') ? '✓' : '+'; toast(button.classList.contains('joined') ? 'Added to your circles.' : 'Removed from your circles.'); }));
  $$('.feed-tab').forEach(button => button.addEventListener('click', () => { state.filter = button.dataset.filter; $$('.feed-tab').forEach(tab => { const selected = tab === button; tab.classList.toggle('selected', selected); tab.setAttribute('aria-selected', String(selected)); }); renderPosts(); }));
  $('#search-input').addEventListener('input', renderPosts);
  document.addEventListener('keydown', event => { if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') { event.preventDefault(); $('#search-input').focus(); } });
  $('#post-content').addEventListener('input', event => $('#char-count').textContent = `${event.target.value.length} / 5000`);
  $('#post-form').addEventListener('submit', async event => {
    event.preventDefault(); const content = $('#post-content').value.trim(); if (!content) return;
    if (state.token) {
      try { await api('/posts', { method: 'POST', body: { content, postType: 'TEXT', visibility: 'PUBLIC' } }); $('#post-dialog').close(); $('#post-form').reset(); $('#char-count').textContent = '0 / 5000'; await loadCampus(); toast('Your post is out there.'); }
      catch (error) { toast(error.message); }
    } else {
      const post = { id: `local-${Date.now()}`, user: { firstName: 'Jordan', lastName: 'Mwangi', username: 'jordanm' }, content, createdAt: new Date().toISOString(), likesCount: 0, commentsCount: 0, reactions: ['✨'], following: true };
      state.posts.unshift(post); $('#post-dialog').close(); $('#post-form').reset(); $('#char-count').textContent = '0 / 5000'; renderPosts(); toast('Preview post added. Sign in to share with your campus.');
    }
  });
  $('#posts-container').addEventListener('click', async event => {
    const button = event.target.closest('[data-action]'); if (!button) return;
    const card = button.closest('[data-post-id]'); const post = state.posts.find(item => String(item.id) === card.dataset.postId); if (!post) return;
    if (button.dataset.action === 'react') {
      if (String(post.id).startsWith('demo-') || String(post.id).startsWith('local-')) { post.myReaction = post.myReaction ? null : 'LOVE'; post.likesCount = Math.max(0, Number(post.likesCount || 0) + (post.myReaction ? 1 : -1)); renderPosts(); return; }
      if (!state.token) return openAuth('login');
      try { await api(`/posts/${encodeURIComponent(post.id)}/react`, { method: 'POST', body: { reactionType: post.myReaction ? 'LOVE' : 'LIKE' } }); post.myReaction = post.myReaction ? null : 'LIKE'; post.likesCount = Math.max(0, Number(post.likesCount || 0) + (post.myReaction ? 1 : -1)); renderPosts(); } catch (error) { toast(error.message); }
    } else if (button.dataset.action === 'save') { post.saved = !post.saved; button.classList.toggle('saved', post.saved); button.querySelector('span').textContent = post.saved ? '▣' : '♧'; toast(post.saved ? 'Saved for later.' : 'Removed from saved.'); }
    else if (button.dataset.action === 'comment') toast(state.token ? 'Replies are coming soon.' : 'Sign in to join the conversation.');
    else if (button.dataset.action === 'share') { try { await navigator.clipboard.writeText(location.href); toast('CampusConnect link copied.'); } catch { toast('Share this moment with your campus.'); } }
  });
  $('#auth-switch').addEventListener('click', () => { state.authMode = $('#auth-switch').dataset.mode; updateAuthForm(); });
  $('#auth-form').addEventListener('submit', async event => {
    event.preventDefault(); const form = new FormData(event.currentTarget); const register = state.authMode === 'register';
    const body = register ? { email: form.get('email'), username: form.get('username'), password: form.get('password'), firstName: form.get('firstName'), lastName: form.get('lastName'), university: 'Kabarak University' } : { email: form.get('email'), password: form.get('password') };
    const button = $('.auth-submit'); button.disabled = true; button.textContent = register ? 'Creating your account…' : 'Signing you in…';
    try {
      const result = await api(`/auth/${register ? 'register' : 'login'}`, { method: 'POST', body, auth: false });
      if (result.mfaRequired) throw new Error('Multi-factor login needs the mobile app. Please sign in with an account that does not require MFA yet.');
      const accessToken = result.accessToken || result.access_token || result.tokens?.accessToken;
      const refreshToken = result.refreshToken || result.refresh_token || result.tokens?.refreshToken;
      if (!accessToken) throw new Error(register ? 'Account created. Check your email to verify it, then sign in.' : 'The API did not return a session token.');
      state.token = accessToken; state.refresh = refreshToken || ''; localStorage.setItem('cc-access-token', state.token); if (state.refresh) localStorage.setItem('cc-refresh-token', state.refresh);
      $('#auth-dialog').close(); event.currentTarget.reset(); await refreshUser(); await loadCampus(); toast(register ? 'Welcome to your campus community.' : `Welcome back${state.user?.firstName ? `, ${state.user.firstName}` : ''}.`);
    } catch (error) { toast(error.message); }
    finally { button.disabled = false; updateAuthForm(); }
  });
  window.addEventListener('hashchange', () => { if (location.hash === '#saved') { state.filter = 'all'; renderPosts(); } });
}

bind(); renderEvents(demoEvents); refreshUser().finally(loadCampus);
