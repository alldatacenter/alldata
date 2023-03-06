from enum import Enum


class CheckOutcome(Enum):
    PASS = "pass"
    WARN = "warn"
    FAIL = "fail"

    def __str__(self):
        return self.name
