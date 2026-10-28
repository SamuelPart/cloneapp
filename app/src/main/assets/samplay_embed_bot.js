/* SamPlay's local, rule-based web-player bot. No native bridge or external AI service. */
(function () {
    'use strict';
    if (window.__samplayBot && window.__samplayBot.version === 1) return true;

    var state = {
        masked: false,
        selected: null,
        savedStyles: [],
        attempts: 0,
        lastAttempt: 0,
        hasPlayed: false,
        userPaused: false,
        needsGesture: false,
        resumeAfterBackground: false
    };
    var closedButtons = typeof WeakSet === 'function' ? new WeakSet() : null;
    var adToken = /(?:^|[\s_-])(?:ad|ads|advert|advertisement|adslot|sponsor|popunder)(?:$|[\s_-])/i;
    var adSelectors = [
        '.ad-container', '.ads-container', '.ad-overlay', '.advertisement',
        '.popup-ad', '.popunder', '#ad-container', '#ad-overlay', '#ads-container',
        '[id^="google_ads_iframe"]', 'iframe[src*="doubleclick.net/"]',
        'iframe[src*="googlesyndication.com/"]'
    ].join(',');
    var playSelectors = [
        '.vjs-big-play-button', '.jw-icon-playback', '.plyr__control--overlaid',
        '.mejs-overlay-button', '.ytp-large-play-button',
        'button[aria-label]', '[role="button"][aria-label]', 'button[title]',
        '.player button', '#player button', '.player [role="button"]', '#player [role="button"]'
    ].join(',');
    var closeSelectors = [
        'button', '[role="button"]', 'a[href="#"]', '.close', '.close-button', '.btn-close'
    ].join(',');

    function documents() {
        var result = [];
        function visit(doc, frames, depth) {
            if (!doc || depth > 3 || result.length >= 16) return;
            result.push({ doc: doc, frames: frames });
            var children;
            try { children = doc.querySelectorAll('iframe'); } catch (_) { return; }
            for (var i = 0; i < children.length && i < 16; i++) {
                try {
                    var child = children[i].contentDocument;
                    if (child && child.documentElement) visit(child, frames.concat([children[i]]), depth + 1);
                } catch (_) { /* Cross-origin frames remain in the provider's own controls. */ }
            }
        }
        visit(document, [], 0);
        return result;
    }

    function isAdElement(element) {
        for (var node = element, count = 0; node && count < 7; node = node.parentElement, count++) {
            var marker = '';
            try { marker = (node.getAttribute('id') || '') + ' ' + (node.getAttribute('class') || ''); } catch (_) {}
            if (adToken.test(marker)) return true;
        }
        return false;
    }

    function visible(element) {
        try {
            var rect = element.getBoundingClientRect();
            var win = element.ownerDocument.defaultView;
            var style = win.getComputedStyle(element);
            var inViewport = (typeof rect.right !== 'number' || rect.right > 0) &&
                (typeof rect.bottom !== 'number' || rect.bottom > 0) &&
                (typeof rect.left !== 'number' || rect.left < win.innerWidth) &&
                (typeof rect.top !== 'number' || rect.top < win.innerHeight);
            return rect.width > 8 && rect.height > 8 && inViewport && style.display !== 'none' &&
                style.visibility !== 'hidden' && style.opacity !== '0';
        } catch (_) { return false; }
    }

    function closeAds(docs) {
        var total = 0;
        docs.forEach(function (entry) {
            var containers;
            try { containers = entry.doc.querySelectorAll(adSelectors); } catch (_) { return; }
            for (var i = 0; i < containers.length && i < 40 && total < 8; i++) {
                var container = containers[i];
                // Never remove a container holding a video: it could be the actual player.
                // A child iframe may also be the player, even when cross-origin and unreadable.
                var hasChildFrame = false;
                try {
                    if (container.querySelector('video')) continue;
                    hasChildFrame = !!container.querySelector('iframe');
                } catch (_) { continue; }
                var buttons = container.querySelectorAll(closeSelectors);
                var closed = false;
                for (var j = 0; j < buttons.length && j < 20; j++) {
                    var button = buttons[j];
                    var labels = [button.getAttribute('aria-label'), button.getAttribute('title'), button.textContent];
                    var className = (button.getAttribute('class') || '').toLowerCase();
                    var isClose = labels.some(function (label) {
                        return /^(?:close|cerrar|skip|omitir|x|×|✕|✖)$/.test((label || '').trim().toLowerCase());
                    }) || /(?:^|[\s_-])(?:close|cerrar)(?:$|[\s_-])/.test(className);
                    var tag = (button.tagName || '').toLowerCase();
                    if (!isClose || !visible(button) || (tag === 'a' && button.getAttribute('href') !== '#') ||
                        (closedButtons && closedButtons.has(button))) continue;
                    try {
                        if (closedButtons) closedButtons.add(button);
                        button.click();
                        closed = true;
                        total++;
                        break;
                    } catch (_) {}
                }
                // Hiding a known ad layer is safer than clicking an unrecognized link.
                if (!closed && !hasChildFrame) {
                    try { container.style.setProperty('display', 'none', 'important'); total++; } catch (_) {}
                }
            }
        });
    }

    function findVideo(docs) {
        var best = null;
        docs.forEach(function (entry) {
            var videos;
            try { videos = entry.doc.querySelectorAll('video'); } catch (_) { return; }
            for (var i = 0; i < videos.length && i < 20; i++) {
                var video = videos[i];
                try {
                    if (isAdElement(video) || !visible(video)) continue;
                    if (!(video.currentSrc || video.src || video.srcObject || video.readyState > 0)) continue;
                    var rect = video.getBoundingClientRect();
                    var score = Math.min(rect.width * rect.height, 10000000) +
                        (video.readyState > 0 ? 1000000 : 0) + (!video.paused ? 2000000 : 0);
                    if (!best || score > best.score) best = { video: video, frames: entry.frames, score: score };
                } catch (_) {}
            }
        });
        return best;
    }

    function restoreStyles() {
        for (var i = state.savedStyles.length - 1; i >= 0; i--) {
            var saved = state.savedStyles[i];
            try {
                if (saved.style === null) saved.element.removeAttribute('style');
                else saved.element.setAttribute('style', saved.style);
                if (saved.wasVideo) saved.element.controls = saved.controls;
            } catch (_) {}
        }
        state.savedStyles = [];
    }

    function maskElement(element, isVideo) {
        state.savedStyles.push({ element: element, style: element.getAttribute('style'),
            wasVideo: isVideo, controls: isVideo ? element.controls : false });
        var style = element.style;
        style.setProperty('position', 'fixed', 'important');
        style.setProperty('top', '0', 'important');
        style.setProperty('left', '0', 'important');
        style.setProperty('width', '100vw', 'important');
        style.setProperty('height', '100vh', 'important');
        style.setProperty('max-width', 'none', 'important');
        style.setProperty('max-height', 'none', 'important');
        style.setProperty('margin', '0', 'important');
        style.setProperty('background', '#000', 'important');
        style.setProperty('z-index', '2147483646', 'important');
        if (isVideo) {
            style.setProperty('object-fit', 'contain', 'important');
            element.controls = false;
        } else {
            style.setProperty('border', '0', 'important');
        }
    }

    function applySelection(selection) {
        var changed = !state.selected || !selection || state.selected.video !== selection.video;
        if (changed) {
            restoreStyles();
            state.selected = selection;
        }
        if (selection && state.masked && state.savedStyles.length === 0) {
            try {
                selection.frames.forEach(function (frame) { maskElement(frame, false); });
                maskElement(selection.video, true);
            } catch (_) { restoreStyles(); }
        }
    }

    function safePlay(video) {
        function rejected(error) {
            if (error && error.name === 'NotAllowedError') state.needsGesture = true;
        }
        try {
            var result = video.play();
            if (result && typeof result.then === 'function') {
                result.then(function () { state.needsGesture = false; }, rejected);
            }
        } catch (error) { rejected(error); }
    }

    function playerContainer(element) {
        try {
            return !!element.closest('.video-js, .jwplayer, .plyr, .mejs-container, .html5-video-player, .player, #player');
        } catch (_) { return false; }
    }

    function clickFirstPlay(docs) {
        for (var i = 0; i < docs.length; i++) {
            var buttons;
            try { buttons = docs[i].doc.querySelectorAll(playSelectors); } catch (_) { continue; }
            for (var j = 0; j < buttons.length && j < 50; j++) {
                var button = buttons[j];
                var tag = (button.tagName || '').toLowerCase();
                if (tag === 'a' || isAdElement(button) || !visible(button)) continue;
                var classes = (button.getAttribute('class') || '').toLowerCase();
                var known = /(?:vjs-big-play-button|jw-icon-playback|plyr__control--overlaid|mejs-overlay-button|ytp-large-play-button)/.test(classes);
                var label = (button.getAttribute('aria-label') || button.getAttribute('title') || button.textContent || '').trim().toLowerCase();
                if (!known && !(playerContainer(button) && /^(?:play|play video|reproducir|reproducir vídeo|reproducir video|iniciar)$/.test(label))) continue;
                try { button.click(); return true; } catch (_) {}
            }
        }
        return false;
    }

    function snapshot(selection) {
        var video = selection && selection.video;
        if (!video) return { controllable: false, playing: false, ended: false, time: 0, duration: 0, seekable: false, needsGesture: false, attempts: state.attempts };
        try {
            var duration = Number(video.duration);
            var time = Number(video.currentTime);
            if (!isFinite(duration) || duration <= 0) duration = 0;
            if (!isFinite(time) || time < 0) time = 0;
            var seekable = duration > 0 && video.seekable && video.seekable.length > 0;
            if (!video.paused && !video.ended) {
                state.hasPlayed = true;
                state.needsGesture = false;
            }
            return { controllable: true, playing: !video.paused && !video.ended, ended: !!video.ended,
                time: time, duration: duration, seekable: !!seekable, needsGesture: state.needsGesture, attempts: state.attempts };
        } catch (_) {
            return { controllable: false, playing: false, ended: false, time: 0, duration: 0, seekable: false, needsGesture: false, attempts: state.attempts };
        }
    }

    function tick() {
        var docs = documents();
        closeAds(docs);
        var selection = findVideo(docs);
        applySelection(selection);
        var result = snapshot(selection);
        var now = Date.now();
        if (!state.hasPlayed && !state.userPaused && state.attempts < 3 && now - state.lastAttempt >= 2000) {
            if (selection) {
                state.attempts++;
                state.lastAttempt = now;
                safePlay(selection.video);
            } else if (clickFirstPlay(docs)) {
                state.attempts++;
                state.lastAttempt = now;
            }
        }
        return result;
    }

    function command(action, value) {
        var selection = findVideo(documents());
        applySelection(selection);
        var video = selection && selection.video;
        if (action === 'play') {
            state.userPaused = false;
            if (video) safePlay(video); else clickFirstPlay(documents());
        } else if (action === 'pause') {
            state.userPaused = true;
            if (video) { try { video.pause(); } catch (_) {} }
        } else if (action === 'seek' && video) {
            try {
                var target = Number(value);
                if (isFinite(target) && video.seekable && video.seekable.length > 0) {
                    var first = video.seekable.start(0);
                    var last = video.seekable.end(video.seekable.length - 1);
                    video.currentTime = Math.max(first, Math.min(last, target));
                }
            } catch (_) {}
        }
        return snapshot(selection);
    }

    window.__samplayBot = {
        version: 1,
        tick: tick,
        command: command,
        setMasked: function (enabled) {
            state.masked = !!enabled;
            if (!state.masked) restoreStyles();
            else applySelection(findVideo(documents()));
            return snapshot(state.selected);
        },
        background: function () {
            var video = state.selected && state.selected.video;
            state.resumeAfterBackground = !!(video && !video.paused && !video.ended);
            if (video) { try { video.pause(); } catch (_) {} }
        },
        foreground: function () {
            if (state.resumeAfterBackground && !state.userPaused && state.selected) safePlay(state.selected.video);
            state.resumeAfterBackground = false;
        },
        dispose: function () {
            var video = state.selected && state.selected.video;
            if (video) { try { video.pause(); } catch (_) {} }
            restoreStyles();
            delete window.__samplayBot;
        }
    };
    return true;
})();
