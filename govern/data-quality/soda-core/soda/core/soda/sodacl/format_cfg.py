from __future__ import annotations


class FormatHelper:
    @classmethod
    def is_numeric(cls, format):
        return format in [
            "integer",
            "positive integer",
            "negative integer",
            "decimal",
            "positive decimal",
            "negative decimal",
            "decimal point",
            "positive decimal point",
            "negative decimal point",
            "decimal comma",
            "positive decimal comma",
            "negative decimal comma",
            "percentage",
            "positive percentage",
            "negative percentage",
            "percentage point",
            "positive percentage point",
            "negative percentage point",
            "percentage comma",
            "positive percentage comma",
            "negative percentage comma",
            "money",
            "money point",
            "money comma",
        ]
