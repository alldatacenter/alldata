from __future__ import annotations

import re


def string_matches_simple_pattern(input: str, pattern: str) -> bool:
    if "*" in pattern:
        pattern = pattern.replace("*", ".*")
    result = re.fullmatch(pattern, input, re.IGNORECASE)

    return bool(result)
