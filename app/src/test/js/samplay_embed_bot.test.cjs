const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { JSDOM } = require('jsdom');

const script = fs.readFileSync(path.join(__dirname, '../../main/assets/samplay_embed_bot.js'), 'utf8');

function page(html = '') {
  const dom = new JSDOM(`<!doctype html><html><body>${html}</body></html>`, {
    url: 'https://provider.example/watch/1', runScripts: 'outside-only'
  });
  dom.window.Element.prototype.getBoundingClientRect = function () {
    return { width: 640, height: 360, top: 0, left: 0, right: 640, bottom: 360 };
  };
  dom.window.eval(script);
  return dom;
}

function mockVideo(video, { failPlay = false } = {}) {
  let paused = true;
  let ended = false;
  let time = 12;
  let plays = 0;
  const seekable = { length: 1, start: () => 0, end: () => 120 };
  Object.defineProperties(video, {
    paused: { configurable: true, get: () => paused },
    ended: { configurable: true, get: () => ended },
    duration: { configurable: true, get: () => 120 },
    currentTime: { configurable: true, get: () => time, set: value => { time = value; } },
    readyState: { configurable: true, get: () => 4 },
    currentSrc: { configurable: true, get: () => 'https://media.example/movie.mp4' },
    seekable: { configurable: true, get: () => seekable }
  });
  video.play = () => { plays++; if (!failPlay) { paused = false; ended = false; } return Promise.resolve(); };
  video.pause = () => { paused = true; };
  return { get plays() { return plays; }, get paused() { return paused; }, get time() { return time; } };
}

test('autoplay is attempted once, then play/pause and seek operate on the real video', () => {
  const dom = page('<div class="player"><video controls src="movie.mp4"></video></div>');
  const video = dom.window.document.querySelector('video');
  const media = mockVideo(video);
  const bot = dom.window.__samplayBot;
  assert.equal(bot.tick().controllable, true);
  assert.equal(media.plays, 1);
  assert.equal(bot.tick().playing, true);
  bot.command('pause', 0);
  assert.equal(media.paused, true);
  bot.tick();
  assert.equal(media.plays, 1, 'the bot must not undo a user pause');
  bot.command('seek', 999);
  assert.equal(media.time, 120);
  bot.command('seek', -15);
  assert.equal(media.time, 0);
  bot.command('play', 0);
  assert.equal(media.paused, false);
  assert.equal(media.plays, 2);
  dom.window.close();
});

test('SamPlay mask can be removed and preserves the provider controls and inline style', () => {
  const dom = page('<video controls style="width: 60%" src="movie.mp4"></video>');
  const video = dom.window.document.querySelector('video');
  mockVideo(video);
  const original = video.getAttribute('style');
  const bot = dom.window.__samplayBot;
  bot.tick();
  bot.setMasked(true);
  assert.equal(video.controls, false);
  assert.equal(video.style.position, 'fixed');
  bot.setMasked(false);
  assert.equal(video.controls, true);
  assert.equal(video.getAttribute('style'), original);
  dom.window.close();
});

test('same-origin iframe videos are controlled and cross-origin frames stay in provider mode', () => {
  const dom = page('<iframe id="local"></iframe><iframe id="external"></iframe>');
  const local = dom.window.document.querySelector('#local');
  const external = dom.window.document.querySelector('#external');
  Object.defineProperty(external, 'contentDocument', { get() { throw new Error('cross origin'); } });
  const innerDoc = local.contentDocument;
  innerDoc.body.innerHTML = '<video controls src="movie.mp4"></video>';
  innerDoc.defaultView.Element.prototype.getBoundingClientRect = function () { return { width: 500, height: 280 }; };
  const video = innerDoc.querySelector('video');
  const media = mockVideo(video);
  const bot = dom.window.__samplayBot;
  assert.equal(bot.tick().controllable, true);
  assert.equal(media.plays, 1);
  bot.setMasked(true);
  assert.equal(local.style.position, 'fixed');
  bot.setMasked(false);
  assert.equal(local.getAttribute('style'), null);
  local.remove();
  assert.equal(bot.tick().controllable, false);
  dom.window.close();
});

test('bot closes or hides known ad layers without clicking ordinary links', () => {
  const dom = page(`
    <div class="ad-overlay"><button aria-label="Cerrar">×</button></div>
    <div class="ad-container"><a href="https://ads.example/click">Advert</a></div>
    <div class="adventure"><button aria-label="Cerrar">Cerrar</button></div>
    <div class="player"><video src="movie.mp4"></video></div>
  `);
  const doc = dom.window.document;
  let closes = 0;
  let adClicks = 0;
  let ordinaryClicks = 0;
  doc.querySelector('.ad-overlay button').addEventListener('click', () => { closes++; });
  doc.querySelector('.ad-container a').addEventListener('click', event => { event.preventDefault(); adClicks++; });
  doc.querySelector('.adventure button').addEventListener('click', () => { ordinaryClicks++; });
  mockVideo(doc.querySelector('video'));
  const bot = dom.window.__samplayBot;
  assert.equal(bot.tick().controllable, true);
  assert.equal(closes, 1);
  assert.equal(adClicks, 0);
  assert.equal(ordinaryClicks, 0);
  assert.equal(doc.querySelector('.ad-container').style.display, 'none');
  bot.tick();
  assert.equal(closes, 1, 'the same close button is not repeatedly clicked');
  dom.window.close();
});

test('ad-like wrappers containing a video are not removed', () => {
  const dom = page('<div class="ad-container"><video src="movie.mp4"></video></div>');
  const container = dom.window.document.querySelector('.ad-container');
  mockVideo(container.querySelector('video'));
  dom.window.__samplayBot.tick();
  assert.notEqual(container.style.display, 'none');
  dom.window.close();
});

test('ad-like wrappers do not hide an uninspectable iframe player', () => {
  const dom = page('<div class="ad-container"><iframe src="https://player.example/embed/1"></iframe></div>');
  const container = dom.window.document.querySelector('.ad-container');
  const frame = container.querySelector('iframe');
  Object.defineProperty(frame, 'contentDocument', { get() { throw new Error('cross origin'); } });
  dom.window.__samplayBot.tick();
  assert.notEqual(container.style.display, 'none');
  dom.window.close();
});

test('dispose restores the original page and pauses the accessible video', () => {
  const dom = page('<video controls style="width: 60%" src="movie.mp4"></video>');
  const video = dom.window.document.querySelector('video');
  const media = mockVideo(video);
  const original = video.getAttribute('style');
  const bot = dom.window.__samplayBot;
  bot.tick();
  bot.setMasked(true);
  bot.dispose();
  assert.equal(media.paused, true);
  assert.equal(video.controls, true);
  assert.equal(video.getAttribute('style'), original);
  assert.equal(dom.window.__samplayBot, undefined);
  dom.window.close();
});

test('only a clearly identified player play button is clicked automatically', () => {
  const dom = page(`
    <div class="ad-overlay"><button class="vjs-big-play-button">ad</button></div>
    <button aria-label="Play">unrelated</button>
    <div class="player"><button aria-label="Reproducir">play</button></div>
  `);
  const doc = dom.window.document;
  let adClicks = 0;
  let unrelatedClicks = 0;
  let playerClicks = 0;
  doc.querySelector('.ad-overlay button').addEventListener('click', () => { adClicks++; });
  doc.querySelector('body > button').addEventListener('click', () => { unrelatedClicks++; });
  doc.querySelector('.player button').addEventListener('click', () => { playerClicks++; });
  const state = dom.window.__samplayBot.tick();
  assert.equal(state.controllable, false);
  assert.equal(playerClicks, 1);
  assert.equal(adClicks, 0);
  assert.equal(unrelatedClicks, 0);
  dom.window.close();
});

test('a text-only play button inside a player can be started', () => {
  const dom = page('<div id="player"><button>Play</button></div>');
  let clicks = 0;
  dom.window.document.querySelector('button').addEventListener('click', () => { clicks++; });
  dom.window.__samplayBot.tick();
  assert.equal(clicks, 1);
  dom.window.close();
});

test('an off-screen preview is not mistaken for the main player', () => {
  const dom = page('<video src="preview.mp4"></video>');
  const video = dom.window.document.querySelector('video');
  const media = mockVideo(video);
  video.getBoundingClientRect = () => ({ width: 320, height: 180, left: -500, right: -180, top: 0, bottom: 180 });
  assert.equal(dom.window.__samplayBot.tick().controllable, false);
  assert.equal(media.plays, 0);
  dom.window.close();
});

test('autoplay retries are bounded when the provider refuses playback', () => {
  const dom = page('<video src="movie.mp4"></video>');
  const media = mockVideo(dom.window.document.querySelector('video'), { failPlay: true });
  let now = 10000;
  dom.window.Date.now = () => now;
  for (let i = 0; i < 8; i++) { dom.window.__samplayBot.tick(); now += 3000; }
  assert.equal(media.plays, 3);
  dom.window.close();
});

test('autoplay gesture rejection exposes the provider controls until playback starts', async () => {
  const dom = page('<video controls src="movie.mp4"></video>');
  const video = dom.window.document.querySelector('video');
  mockVideo(video);
  const realPlay = video.play;
  const denied = new Error('user gesture required');
  denied.name = 'NotAllowedError';
  video.play = () => Promise.reject(denied);
  const bot = dom.window.__samplayBot;
  bot.tick();
  await new Promise(resolve => setImmediate(resolve));
  assert.equal(bot.tick().needsGesture, true);
  video.play = realPlay;
  await video.play(); // Simulate starting playback through the provider's own controls.
  assert.equal(bot.tick().needsGesture, false);
  dom.window.close();
});

test('background restoration does not restart a user-paused video', () => {
  const dom = page('<video src="movie.mp4"></video>');
  const media = mockVideo(dom.window.document.querySelector('video'));
  const bot = dom.window.__samplayBot;
  bot.tick();
  bot.tick();
  bot.background();
  assert.equal(media.paused, true);
  bot.foreground();
  assert.equal(media.paused, false);
  bot.command('pause', 0);
  bot.background();
  bot.foreground();
  assert.equal(media.paused, true);
  dom.window.close();
});
