#!/usr/bin/env python3
"""Small local API for an authorized EXTINF M3U catalog.

This utility only parses direct HTTP(S) playlist entries and redirects a local
player to the exact URL supplied in the playlist. It does not open webpages,
resolve iframe embeds, harvest tokens/cookies, evade bot checks, or proxy bytes.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from dataclasses import dataclass
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs, urlsplit

_ATTRIBUTE = re.compile(r'([A-Za-z0-9_-]+)="([^"]*)"')


@dataclass(frozen=True)
class Entry:
    id: str
    name: str
    url: str
    group: str = ""
    tvg_id: str = ""
    logo: str = ""


def _unquoted_comma(line: str) -> int:
    """Return the EXTINF separator comma, ignoring commas inside quotes."""
    quoted = False
    escaped = False
    for index, character in enumerate(line):
        if character == '"' and not escaped:
            quoted = not quoted
        elif character == "," and not quoted:
            return index
        escaped = character == "\\" and not escaped
        if character != "\\":
            escaped = False
    return -1


_DIRECT_EXTENSIONS = {".m3u8", ".m3u", ".mpd", ".mp4", ".m4v", ".mov", ".ts", ".m4s", ".aac", ".mp3", ".webm"}


def _is_direct_stream_url(value: str) -> bool:
    try:
        parsed = urlsplit(value.strip())
        hostname = parsed.hostname
    except ValueError:
        return False
    path = parsed.path.lower()
    return (
        parsed.scheme.lower() in {"http", "https"}
        and bool(hostname)
        and any(path.endswith(extension) for extension in _DIRECT_EXTENSIONS)
    )


def parse_m3u(text: str) -> list[Entry]:
    """Parse #EXTINF metadata plus a direct HTTP(S) stream URL."""
    entries: list[Entry] = []
    pending: dict[str, str] | None = None

    for raw_line in text.lstrip("\ufeff").splitlines():
        line = raw_line.strip()
        if not line:
            continue

        if line.upper().startswith("#EXTINF:"):
            comma = _unquoted_comma(line)
            if comma < 0:
                pending = None
                continue
            metadata = line[:comma]
            display_name = line[comma + 1 :].strip()
            attributes = dict(_ATTRIBUTE.findall(metadata))
            name = display_name or attributes.get("tvg-name", "").strip()
            pending = {
                "name": name,
                "group": attributes.get("group-title", "").strip(),
                "tvg_id": attributes.get("tvg-id", "").strip(),
                "logo": attributes.get("tvg-logo", "").strip(),
            } if name else None
            continue

        if line.startswith("#"):
            # Directives such as EXTGRP/EXTVLCOPT are not executed or replayed.
            continue

        if pending is None:
            continue

        if not _is_direct_stream_url(line):
            pending = None
            continue

        identity = f"{pending['tvg_id']}\0{pending['name']}\0{line}".encode("utf-8")
        entry_id = hashlib.sha256(identity).hexdigest()[:20]
        entries.append(
            Entry(
                id=entry_id,
                name=pending["name"],
                url=line,
                group=pending["group"],
                tvg_id=pending["tvg_id"],
                logo=pending["logo"],
            )
        )
        pending = None

    return entries


def make_handler(entries: list[Entry]) -> type[BaseHTTPRequestHandler]:
    by_id = {entry.id: entry for entry in entries}

    class Handler(BaseHTTPRequestHandler):
        server_version = "AuthorizedM3U/1.0"

        def do_GET(self) -> None:  # noqa: N802 - BaseHTTPRequestHandler API
            request = urlsplit(self.path)
            if request.path == "/health":
                self._json(200, {"ok": True, "entries": len(entries)})
                return

            if request.path == "/catalog":
                # Do not expose source URLs in the catalog response; /play issues
                # a no-store redirect only when a local player asks for an ID.
                payload = [
                    {
                        "id": entry.id,
                        "name": entry.name,
                        "group": entry.group,
                        "tvg_id": entry.tvg_id,
                        "logo": entry.logo,
                    }
                    for entry in entries
                ]
                self._json(200, payload)
                return

            if request.path == "/play":
                entry_id = parse_qs(request.query).get("id", [""])[0]
                entry = by_id.get(entry_id)
                if entry is None:
                    self._json(404, {"error": "unknown or unavailable catalog id"})
                    return
                self.send_response(302)
                self.send_header("Location", entry.url)
                self.send_header("Cache-Control", "no-store")
                self.send_header("Content-Length", "0")
                self.end_headers()
                return

            self._json(404, {"error": "not found"})

        def _json(self, status: int, payload: object) -> None:
            body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
            self.send_response(status)
            self.send_header("Content-Type", "application/json; charset=utf-8")
            self.send_header("Cache-Control", "no-store")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)

        def log_message(self, format: str, *args: object) -> None:
            # Avoid logging the stream URL or query values.
            sys.stderr.write(f"[{self.log_date_time_string()}] {self.command} {urlsplit(self.path).path}\n")

    return Handler


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("playlist", type=Path, help="path to an authorized EXTINF M3U/M3U8 file")
    parser.add_argument("--port", type=int, default=8765, help="local API port (default: 8765)")
    args = parser.parse_args()

    if not args.playlist.is_file():
        parser.error(f"playlist not found: {args.playlist}")

    entries = parse_m3u(args.playlist.read_text(encoding="utf-8-sig", errors="replace"))
    if not entries:
        parser.error("no direct HTTP(S) EXTINF entries found")

    host = "127.0.0.1"
    server = ThreadingHTTPServer((host, args.port), make_handler(entries))
    print(f"Loaded {len(entries)} direct entries from {args.playlist}")
    print(f"API listening at http://{host}:{args.port}")
    print("Endpoints: GET /health, GET /catalog, GET /play?id=<entry-id>")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("Stopping local API...")
    finally:
        server.server_close()


if __name__ == "__main__":
    main()
