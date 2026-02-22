#!/usr/bin/env python3
"""
Quick Perspective API phrase probe.

Usage:
  1) Set API_KEY below.
  2) Run: python probe/perspective_probe.py
"""

from __future__ import annotations

import json
import re
import sys
import urllib.error
import urllib.parse
import urllib.request


API_KEY = "AIzaSyBOoSB89x4BWPwjI4gR7gSKcJl87nIvTHQ"

APPROVE_THRESHOLD = 0.20
REJECT_THRESHOLD = 0.80

HARD_REJECT_RE = re.compile(
    r"\b(i\s*hate\s*you|kill\s*yourself|go\s*die|i\s*will\s*kill\s*you)\b",
    re.IGNORECASE,
)

SAMPLES = [
    ("NEGATIVE", "I hate you"),
    ("NEGATIVE", "You are disgusting and worthless"),
    ("NEGATIVE", "Go die"),
    ("NEGATIVE", "Nobody likes you"),
    ("NEGATIVE", "You are a complete idiot"),
    ("NEGATIVE", "I will kill you"),
    ("NEGATIVE", "You are trash"),
    ("POSITIVE", "I like this post"),
    ("POSITIVE", "Great job, thanks for sharing"),
    ("POSITIVE", "This explanation helped me a lot"),
    ("POSITIVE", "I appreciate your effort"),
    ("POSITIVE", "Wonderful work, keep going"),
    ("NEUTRAL", "Can you explain this step?"),
    ("NEUTRAL", "I disagree with your point"),
    ("NEUTRAL", "This is not correct in my opinion"),
]


def perspective_toxicity(text: str, api_key: str) -> float:
    endpoint = (
        "https://commentanalyzer.googleapis.com/v1alpha1/comments:analyze?key="
        + urllib.parse.quote(api_key)
    )
    payload = {
        "comment": {"text": text},
        "languages": ["en"],
        "requestedAttributes": {"TOXICITY": {}},
    }
    body = json.dumps(payload).encode("utf-8")
    req = urllib.request.Request(
        endpoint,
        data=body,
        headers={"Content-Type": "application/json", "User-Agent": "HirelyProbe/1.0"},
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=8) as resp:
        raw = resp.read().decode("utf-8")
        parsed = json.loads(raw)
        return float(
            parsed["attributeScores"]["TOXICITY"]["summaryScore"]["value"]
        )


def threshold_decision(toxicity: float) -> str:
    if toxicity <= APPROVE_THRESHOLD:
        return "APPROVED"
    if toxicity >= REJECT_THRESHOLD:
        return "REJECTED"
    return "PENDING"


def app_decision(text: str, toxicity: float | None) -> str:
    if HARD_REJECT_RE.search(text or ""):
        return "REJECTED"
    if toxicity is None:
        return "PENDING"
    return threshold_decision(toxicity)


def main() -> int:
    key = (API_KEY or "").strip()
    if not key:
        print("Set API_KEY in probe/perspective_probe.py before running.", file=sys.stderr)
        return 1

    print("=== Perspective Python Probe ===")
    print(f"Samples: {len(SAMPLES)}")
    print("")

    for i, (group, text) in enumerate(SAMPLES, start=1):
        prefix = f"{i:02d} [{group}] \"{text}\""
        try:
            toxicity = perspective_toxicity(text, key)
            api_dec = threshold_decision(toxicity)
            app_dec = app_decision(text, toxicity)
            print(prefix)
            print(
                f"   toxicity={toxicity:.4f} | apiDecision={api_dec} | appDecision={app_dec}"
            )
        except urllib.error.HTTPError as e:
            print(prefix)
            print(f"   HTTP ERROR: {e.code} {e.reason}")
        except urllib.error.URLError as e:
            print(prefix)
            print(f"   NETWORK ERROR: {e.reason}")
        except Exception as e:
            print(prefix)
            print(f"   ERROR: {e}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
