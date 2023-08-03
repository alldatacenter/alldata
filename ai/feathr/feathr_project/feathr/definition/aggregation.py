from enum import Enum


class Aggregation(Enum):
    """
    The built-in aggregation functions for LookupFeature
    """
    # No operation
    NOP = 0
    # Average
    AVG = 1
    MAX = 2
    MIN = 3
    SUM = 4
    UNION = 5
    # Element-wise average, typically used in array type value, i.e. 1d dense tensor
    ELEMENTWISE_AVG = 6
    ELEMENTWISE_MIN = 7
    ELEMENTWISE_MAX = 8
    ELEMENTWISE_SUM = 9
    # Pick the latest value according to its timestamp
    LATEST = 10
    # Pick the first value from the looked up values (non-deterministic)
    FIRST = 11