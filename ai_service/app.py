from __future__ import annotations

import hashlib
from functools import lru_cache
import re
from collections import Counter, OrderedDict
from threading import Lock
from typing import Literal

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer


MODEL_NAME = "sentence-transformers/all-MiniLM-L6-v2"
app = FastAPI(title="Hirely AI Service", version="1.0.0")

CAREER_ANCHORS = {
    "Internship": "internship opportunity",
    "Job Offer": "job opening hiring",
    "Interview": "interview preparation",
    "Resume": "resume review cv feedback",
    "Salary": "salary negotiation compensation",
    "Career Advice": "career advice and growth",
    "Applications": "application tips for jobs",
}

OFF_TOPIC_ANCHORS = {
    "Gaming": "gaming and video games",
    "Memes": "internet memes and jokes",
    "Politics": "political debate and elections",
    "Dating": "dating and relationships",
    "Random Chat": "random casual chat",
}

LINK_PATTERN = re.compile(r"(https?://\S+|www\.\S+)", re.IGNORECASE)
REPEATED_CHAR_PATTERN = re.compile(r"(.)\1{3,}")
REPEATED_PUNCT_PATTERN = re.compile(r"[!?.,]{4,}")
NON_ALNUM_PATTERN = re.compile(r"[^a-z0-9]")
MAX_POST_CACHE = 200
MAX_COMMENT_CACHE = 300

DUPLICATE_CACHE_LOCK = Lock()
RECENT_POST_EMBEDDINGS: OrderedDict[str, list[float]] = OrderedDict()
RECENT_COMMENT_EMBEDDINGS: OrderedDict[str, list[float]] = OrderedDict()


class ScoreRequest(BaseModel):
    text: str
    type: Literal["post", "comment"] = "post"
    content_key: str | None = None


class ReasonsResponse(BaseModel):
    relevance: list[str]
    quality: list[str]
    duplicate: list[str]


class ScoreResponse(BaseModel):
    relevance: float
    category: str
    quality: float
    duplicate_similarity: float
    reasons: ReasonsResponse


@lru_cache(maxsize=1)
def get_model() -> SentenceTransformer:
    return SentenceTransformer(MODEL_NAME)


@lru_cache(maxsize=1)
def get_anchor_embeddings() -> tuple[dict[str, list[float]], dict[str, list[float]]]:
    model = get_model()
    career_labels = list(CAREER_ANCHORS.keys())
    career_texts = [CAREER_ANCHORS[label] for label in career_labels]
    career_vectors = model.encode(career_texts, normalize_embeddings=True).tolist()

    off_labels = list(OFF_TOPIC_ANCHORS.keys())
    off_texts = [OFF_TOPIC_ANCHORS[label] for label in off_labels]
    off_vectors = model.encode(off_texts, normalize_embeddings=True).tolist()

    career = {label: [float(v) for v in vector] for label, vector in zip(career_labels, career_vectors)}
    off_topic = {label: [float(v) for v in vector] for label, vector in zip(off_labels, off_vectors)}
    return career, off_topic


@app.get("/health")
def health() -> dict:
    return {"ok": True, "model": MODEL_NAME}


@app.post("/score", response_model=ScoreResponse)
def score(req: ScoreRequest) -> ScoreResponse:
    text = (req.text or "").strip()
    if not text:
        raise HTTPException(status_code=400, detail="text is required")

    model = get_model()
    text_vector = [float(v) for v in model.encode(text, normalize_embeddings=True).tolist()]
    relevance, category, relevance_reasons = score_relevance(text_vector, req.type)
    quality, quality_reasons = score_quality(text, req.type)
    duplicate_similarity, duplicate_reasons = score_duplicate_and_remember(
        text_vector, req.type, req.content_key, text
    )

    return ScoreResponse(
        relevance=relevance,
        category=category,
        quality=quality,
        duplicate_similarity=duplicate_similarity,
        reasons=ReasonsResponse(
            relevance=relevance_reasons,
            quality=quality_reasons,
            duplicate=duplicate_reasons,
        ),
    )


def score_relevance(embedding: list[float], content_type: str) -> tuple[float, str, list[str]]:
    career_anchor_vectors, off_topic_anchor_vectors = get_anchor_embeddings()
    best_career_similarity = -1.0
    best_category = "General"

    for label, anchor_vector in career_anchor_vectors.items():
        similarity = cosine(embedding, anchor_vector)
        if similarity > best_career_similarity:
            best_career_similarity = similarity
            best_category = label

    best_off_topic_similarity = -1.0
    off_topic_category = ""
    for label, anchor_vector in off_topic_anchor_vectors.items():
        similarity = cosine(embedding, anchor_vector)
        if similarity > best_off_topic_similarity:
            best_off_topic_similarity = similarity
            off_topic_category = label

    relevance = clamp01(best_career_similarity)
    reasons: list[str] = [f"Matched: {best_category}"]

    if best_off_topic_similarity > best_career_similarity:
        penalty = min(0.35, (best_off_topic_similarity - best_career_similarity) * 0.60)
        relevance = clamp01(relevance - penalty)
        reasons.append(f"Off-topic signal ({off_topic_category}) reduced relevance")

    if content_type == "comment" and relevance < 0.35 and best_career_similarity > 0.35:
        relevance = clamp01(relevance + 0.08)
        reasons.append("Comment context adjustment applied")

    return relevance, best_category, reasons


def score_quality(text: str, content_type: str) -> tuple[float, list[str]]:
    score = 1.0
    reasons: list[str] = []

    min_length = 12 if content_type == "comment" else 30
    ideal_length = 80 if content_type == "comment" else 180
    text_length = len(text)
    if text_length < min_length:
        score -= 0.40
        reasons.append("Too short")
    elif text_length < ideal_length:
        score -= 0.15

    link_count = len(LINK_PATTERN.findall(text))
    if link_count >= 3:
        score -= 0.25
        reasons.append("Too many links")
    elif link_count == 2:
        score -= 0.15
        reasons.append("High link density")

    caps = caps_ratio(text)
    if caps > 0.70:
        score -= 0.25
        reasons.append("Excessive all-caps")
    elif caps > 0.45:
        score -= 0.10

    if REPEATED_CHAR_PATTERN.search(text):
        score -= 0.12
        reasons.append("Repeated characters detected")

    if REPEATED_PUNCT_PATTERN.search(text):
        score -= 0.12
        reasons.append("Repeated punctuation detected")

    diversity = token_diversity(text)
    if diversity < 0.35:
        score -= 0.20
        reasons.append("Low word diversity")
    elif diversity < 0.50:
        score -= 0.08

    final_score = clamp01(score)
    if final_score >= 0.80:
        reasons.append("Strong content quality")

    return final_score, reasons


def score_duplicate_and_remember(
    embedding: list[float], content_type: str, content_key: str | None, text: str
) -> tuple[float, list[str]]:
    cache = RECENT_COMMENT_EMBEDDINGS if content_type == "comment" else RECENT_POST_EMBEDDINGS
    max_size = MAX_COMMENT_CACHE if content_type == "comment" else MAX_POST_CACHE
    max_similarity = 0.0
    key = (content_key or "").strip()
    if not key:
        stable_hash = hashlib.sha256(text.encode("utf-8")).hexdigest()[:16]
        key = f"{content_type}:{stable_hash}"

    with DUPLICATE_CACHE_LOCK:
        for existing_key, existing in cache.items():
            # Avoid self-comparison when the caller provides a stable entity key.
            if existing_key == key:
                continue
            similarity = cosine(embedding, existing)
            if similarity > max_similarity:
                max_similarity = similarity

        reasons: list[str] = []
        if max_similarity >= 0.93:
            reasons.append(f"Near-duplicate content detected ({max_similarity:.2f} similarity)")
        elif max_similarity >= 0.88:
            reasons.append(f"Possible duplicate detected ({max_similarity:.2f} similarity)")

        if key in cache:
            cache.move_to_end(key)
        cache[key] = embedding

        while len(cache) > max_size:
            cache.popitem(last=False)

    return max_similarity, reasons


def cosine(a: list[float], b: list[float]) -> float:
    if not a or not b or len(a) != len(b):
        return 0.0
    return sum(x * y for x, y in zip(a, b))


def clamp01(value: float) -> float:
    if value < 0.0:
        return 0.0
    if value > 1.0:
        return 1.0
    return value


def caps_ratio(text: str) -> float:
    letters = [ch for ch in text if ch.isalpha()]
    if not letters:
        return 0.0
    uppercase_count = sum(1 for ch in letters if ch.isupper())
    return uppercase_count / len(letters)


def token_diversity(text: str) -> float:
    raw_tokens = text.lower().split()
    cleaned = [NON_ALNUM_PATTERN.sub("", token) for token in raw_tokens]
    valid_tokens = [token for token in cleaned if token]
    if not valid_tokens:
        return 0.0
    unique_count = len(Counter(valid_tokens))
    return unique_count / len(valid_tokens)
