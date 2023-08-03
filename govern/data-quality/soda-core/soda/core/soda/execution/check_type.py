from enum import Enum


class CheckType(Enum):
    CLOUD = 1
    LOCAL = 2

    def __str__(self):
        return self.name
