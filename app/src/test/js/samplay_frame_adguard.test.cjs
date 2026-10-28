const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const { JSDOM } = require('jsdom');

const script = fs.readFileSync('app/src/main/assets/samplay_frame_adguard.js', 'utf8');
const settle = () => new Promise(resolve => setTimeout(resolve, 0));
function page(html, url = 'https://player.example/watch/1') {
  const dom = new JSDOM(`<!doctype html><html><body>${html}</body></html>`, { url, runScripts: 'outside-only' });
  dom.window.eval(script);
  return dom;
}

test('hides identified ad overlays and known ad frames without clicking their links', () => {
  const dom = page(`
    <div class="ad-overlay"><a href="https://ads.example/click">Click</a></div>
    <div data-ad-slot="above-video">publicidad</div>
    <iframe src="https://a.doubleclick.net/banner"></iframe>
    <a id="clickthrough" href="https://popads.net/click">advertising link</a>
    <a id="provider-link" href="https://player.example/watch/2">provider link</a>
    <div class="adventure">ordinary content</div>
    <div class="player"><video src="movie.mp4"></video></div>
  `);
  const doc = dom.window.document;
  let clicks = 0;
  doc.querySelector('a').addEventListener('click', () => clicks++);
  assert.equal(doc.querySelector('.ad-overlay').style.getPropertyValue('display'), 'none');
  assert.equal(doc.querySelector('[data-ad-slot]').style.getPropertyValue('display'), 'none');
  assert.equal(doc.querySelector('iframe').style.getPropertyValue('display'), 'none');
  assert.equal(doc.querySelector('#clickthrough').style.display, 'none');
  assert.notEqual(doc.querySelector('#provider-link').style.display, 'none');
  assert.notEqual(doc.querySelector('.adventure').style.display, 'none');
  assert.notEqual(doc.querySelector('.player').style.display, 'none');
  assert.equal(clicks, 0);
  dom.window.close();
});

test('protects playback media, unknown iframe players, canvas and control bars', () => {
  const dom = page(`
    <div class="ad-container" id="video-wrap"><video src="movie.mp4"></video></div>
    <div class="ad-container" id="iframe-wrap"><iframe src="https://other.example/embed/1"></iframe></div>
    <div class="ad-container" id="canvas-wrap"><canvas></canvas></div>
    <div class="vjs-control-bar"><span class="ad-marker"></span></div>
  `);
  for (const selector of ['#video-wrap', '#iframe-wrap', '#canvas-wrap', '.ad-marker']) {
    assert.notEqual(dom.window.document.querySelector(selector).style.display, 'none', selector);
  }
  dom.window.close();
});

test('restores an ad-like wrapper when a player is inserted later', async () => {
  const dom = page('<div class="ad-container" style="display: block"></div>');
  const wrapper = dom.window.document.querySelector('.ad-container');
  assert.equal(wrapper.style.display, 'none');
  wrapper.appendChild(dom.window.document.createElement('video'));
  await settle();
  assert.equal(wrapper.style.display, 'block');
  dom.window.close();
});

test('detects dynamically inserted overlays and ads identified after insertion', async () => {
  const dom = page('<div class="player"><video></video></div>');
  const doc = dom.window.document;
  const overlay = doc.createElement('div');
  overlay.className = 'ad-overlay';
  doc.querySelector('.player').appendChild(overlay);
  const later = doc.createElement('div');
  doc.body.appendChild(later);
  later.setAttribute('data-ad-unit', 'overlay');
  await settle();
  assert.equal(overlay.style.display, 'none');
  assert.equal(later.style.display, 'none');
  later.removeAttribute('data-ad-unit');
  await settle();
  assert.equal(later.style.display, '', 'a node that stops being an ad is restored');
  dom.window.close();
});

test('same-origin iframes get a fallback guard without hiding the iframe player', async () => {
  const dom = page('<iframe id="player"></iframe>');
  const frame = dom.window.document.querySelector('#player');
  frame.contentDocument.body.innerHTML = '<video></video><div class="ad-overlay">overlay</div>';
  await settle();
  assert.equal(frame.contentDocument.querySelector('.ad-overlay').style.display, 'none');
  assert.notEqual(frame.style.display, 'none');
  dom.window.close();
});

test('a document in a different origin can run the same standalone guard', () => {
  // Simulates the separately injected script; jsdom is not Android WebView.
  const external = page('<video></video><div class="advertisement">overlay</div>', 'https://another-provider.example/embed');
  assert.equal(external.window.document.querySelector('.advertisement').style.display, 'none');
  assert.notEqual(external.window.document.querySelector('video').style.display, 'none');
  external.window.eval(script); // registration is idempotent
  assert.equal(external.window.document.querySelector('.advertisement').style.display, 'none');
  external.window.close();
});

test('an ad-domain substring in an unrelated host is never treated as a match', () => {
  const dom = page('<iframe src="https://doubleclick.net.safe.example/embed"></iframe>');
  assert.notEqual(dom.window.document.querySelector('iframe').style.display, 'none');
  dom.window.close();
});

test('large documents are scanned in batches without missing a later ad', async () => {
  const ordinary = Array.from({ length: 450 }, (_, index) => `<div class="entry" id="n${index}"></div>`).join('');
  const dom = page(`${ordinary}<div class="ad-overlay" id="last-overlay">ad</div>`);
  await settle();
  assert.equal(dom.window.document.querySelector('#last-overlay').style.display, 'none');
  assert.notEqual(dom.window.document.querySelector('#n449').style.display, 'none');
  dom.window.close();
});
