/* SamPlay: conservatively hide identified advertising layers, without clicking links. */
(function () {
    'use strict';
    var FLAG = '__samplayAdGuardV1';
    if (window[FLAG]) return;
    var installed = typeof WeakSet === 'function' ? new WeakSet() : null;
    var hidden = typeof WeakMap === 'function' ? new WeakMap() : null;
    var watchedFrames = typeof WeakSet === 'function' ? new WeakSet() : null;
    var observedCount = 0;
    var adMarker = /(?:^|[\s_-])(?:ad|ads|adsbygoogle|advert|advertisement|advertising|adslot|adcontainer|adbox|adlayer|adsbox|sponsor|sponsored|anuncio|anuncios|publicidad|popunder)(?:$|[\s_-])/i;
    var adHosts = ['doubleclick.net', 'googlesyndication.com', 'googleadservices.com', 'adservice.google.com',
        'adnxs.com', 'adsafeprotected.com', 'taboola.com', 'outbrain.com', 'popads.net', 'popcash.net',
        'propellerads.com', 'clickadu.com', 'adsterra.com', 'exoclick.com', 'trafficjunky.net', 'mgid.com', 'adskeeper.com'];
    var candidates = [
        '[id]', '[class]',
        '[data-ad]', '[data-ad-slot]', '[data-ad-unit]', '[data-adunit]', '[data-ad-client]', '[data-google-query-id]',
        '[aria-label]', 'iframe', 'a[href]'
    ].join(',');
    var controls = '.vjs-control-bar, .jw-controlbar, .plyr__controls, .mejs-controls, [role="slider"]';

    function knownAdDestination(element) {
        if (!element) return false;
        var tag = String(element.tagName).toLowerCase();
        if (tag !== 'iframe' && tag !== 'a') return false;
        try {
            var source = element.getAttribute(tag === 'iframe' ? 'src' : 'href');
            if (!source) return false;
            var host = new URL(source, element.ownerDocument.baseURI).hostname.toLowerCase().replace(/\.$/, '');
            return adHosts.some(function (domain) { return host === domain || host.endsWith('.' + domain); });
        } catch (_) { return false; }
    }

    function isAdvertising(element) {
        if (knownAdDestination(element)) return true;
        try {
            var marker = (element.getAttribute('id') || '') + ' ' + (element.getAttribute('class') || '');
            var label = (element.getAttribute('aria-label') || '').trim();
            return adMarker.test(marker) || /^(advertisement|advert|publicidad|anuncio|ad)$/i.test(label) ||
                element.hasAttribute('data-ad') || element.hasAttribute('data-ad-slot') ||
                element.hasAttribute('data-ad-unit') || element.hasAttribute('data-adunit') || element.hasAttribute('data-ad-client') ||
                element.hasAttribute('data-google-query-id');
        } catch (_) { return false; }
    }

    function hasProtectedContent(element) {
        try {
            if (element.matches('video, audio, canvas, object, embed')) return true;
            if (element.matches('iframe')) return !knownAdDestination(element);
            if (element.querySelector('video, audio, canvas, object, embed')) return true;
            var frames = element.querySelectorAll('iframe');
            for (var i = 0; i < frames.length; i++) if (!knownAdDestination(frames[i])) return true;
            return false;
        } catch (_) { return true; }
    }

    function restore(element) {
        if (!hidden || !hidden.has(element)) return;
        var original = hidden.get(element);
        try {
            if (original.value) element.style.setProperty('display', original.value, original.priority);
            else element.style.removeProperty('display');
        } catch (_) {}
        hidden.delete(element);
    }

    function inspect(element) {
        if (!element || element.nodeType !== 1 || !element.style) return;
        try {
            var ad = isAdvertising(element);
            if (!ad || hasProtectedContent(element) || element.closest(controls)) {
                restore(element);
                return;
            }
            if (!hidden || !hidden.has(element)) {
                if (hidden) hidden.set(element, { value: element.style.getPropertyValue('display'), priority: element.style.getPropertyPriority('display') });
            }
            if (element.style.getPropertyValue('display') !== 'none' || element.style.getPropertyPriority('display') !== 'important') {
                element.style.setProperty('display', 'none', 'important');
            }
        } catch (_) { /* An unfamiliar provider must not break the player. */ }
    }

    function watchFrame(frame, depth) {
        if (!frame || (watchedFrames && watchedFrames.has(frame))) return;
        if (watchedFrames) watchedFrames.add(frame);
        function visit() {
            try { if (frame.contentDocument) install(frame.contentDocument, depth + 1); }
            catch (_) { /* Cross-origin documents get their own document-start script when supported. */ }
        }
        try { frame.addEventListener('load', visit); visit(); } catch (_) {}
    }

    function scan(root, depth) {
        if (!root) return;
        if (root.nodeType === 1) {
            inspect(root);
            if (String(root.tagName).toLowerCase() === 'iframe') watchFrame(root, depth);
        }
        var elements;
        try { elements = root.querySelectorAll(candidates); } catch (_) { return; }
        var index = 0;
        function batch() {
            var end = Math.min(index + 400, elements.length);
            for (; index < end; index++) {
                inspect(elements[index]);
                if (String(elements[index].tagName).toLowerCase() === 'iframe') watchFrame(elements[index], depth);
            }
            if (index < elements.length) window.setTimeout(batch, 0);
        }
        batch();
    }

    function install(doc, depth) {
        if (!doc || depth > 3 || observedCount >= 16 || (installed && installed.has(doc))) return;
        try {
            if (doc.defaultView && doc.defaultView !== window && doc.defaultView[FLAG]) return;
            if (doc.defaultView) doc.defaultView[FLAG] = true;
        } catch (_) { return; }
        if (installed) installed.add(doc);
        observedCount++;
        scan(doc, depth);
        try {
            var observer = new MutationObserver(function (records) {
                records.forEach(function (record) {
                    if (record.type === 'attributes') inspect(record.target);
                    else {
                        for (var i = 0; i < record.addedNodes.length; i++) scan(record.addedNodes[i], depth);
                        // A player frame/video may appear inside an initially empty ad-like wrapper.
                        for (var parent = record.target, n = 0; parent && n < 12; parent = parent.parentElement, n++) inspect(parent);
                    }
                });
            });
            observer.observe(doc, { subtree: true, childList: true, attributes: true,
                attributeFilter: ['id', 'class', 'style', 'src', 'href', 'aria-label', 'data-ad', 'data-ad-slot', 'data-ad-unit', 'data-adunit', 'data-ad-client', 'data-google-query-id'] });
        } catch (_) {}
    }
    install(document, 0);
}());
