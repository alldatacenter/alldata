from registry.data-models.common.models import *
from typing import Optional


class SlidingWindowAggregationType(Enum):
    """
    Represents supported types of aggregation.
    """
    SUM = "sum"
    COUNT = "count"
    MAX = "maximum"
    MIN = "minium"
    AVG = "average"


class SlidingWindowEmbeddingAggregationType(Enum):
    """
    Represents supported types for embedding aggregation.
    Pooling is a sample-based discretization process. The objective is to down-sample an input
    representation and reduce its dimensionality.
    """
    MAX_POOLING = "max_pooling"  # Max pooling is done by applying a max filter to (usually) non-overlapping subregions of the initial representation
    MIN_POOLING = "min_pooling"  # Min pooling is done by applying a min filter to (usually) non-overlapping subregions of the initial representation.
    AVG_POOLING = "avg_pooling"  # Average pooling is done by applying a average filter to (usually) non-overlapping subregions of the initial representation


class WindowTimeUnit(Enum):
    """
    Represents a unit of time.
    """
    DAY = "day"
    HOUR = "hour"
    MINUTE = "minute"
    SECOND = "second"


class Window(BaseModel):
    """
    Represents a time window used in sliding window algorithms.
    """
    size: int  # Represents the duration of the window.
    unit: WindowTimeUnit


class SlidingWindowAggregation(Function):
    """
    Sliding window aggregation produces feature data by aggregating a collection of data within a given time
    interval into an aggregate value. It ensures point-in-time correctness, when joining with label data,
    it looks back the configurable time window from each entry's timestamp and compute the aggregate value.
    This class can be extended to support LateralView in aggregation.
    """
    aggregationType: SlidingWindowAggregationType  # Represents supported types of aggregation.
    window: Window  # Represents the time window to look back from label data's timestamp.
    targetColumn: SparkSqlExpression  # The target column to perform aggregation against.
    filter: Optional[SparkSqlExpression]  # Represents the filter statement before the aggregation.
    groupBy: Optional[SparkSqlExpression]  # Represents the target to be grouped by before aggregation.
    limit: Optional[int]  # Represents the max number of groups (with aggregation results) to return.


class SlidingWindowEmbeddingAggregation(Function):
    """
    Sliding window embedding aggregation produces a single embedding by performing element-wise operations or
    discretion on a collection of embeddings within a given time interval. It ensures point-in-time correctness,
    when joining with label data, feathr looks back the configurable time window from each entry's timestamp and produce
    the aggregated embedding.
    """
    aggregationType: SlidingWindowEmbeddingAggregationType  # Represents supported types for embedding aggregation.
    window: Window  # Represents the time window to look back from label data's timestamp.
    targetColumn: SparkSqlExpression  # The target column to perform aggregation against.
    filter: Optional[SparkSqlExpression]  # Represents the filter statement before the aggregation.
    groupBy: Optional[SparkSqlExpression]  # Represents the target to be grouped by before aggregation.


class SlidingWindowLatestAvailable(Function):
    """
    This sliding window algorithm picks the latest available feature data from the source data.
    Note the latest here means event time instead of processing time.
    This class can be extended to support LateralView in aggregation.
    """
    window: Optional[Window]  # Represents the time window to look back from label data's timestamp.
    targetColumn: SparkSqlExpression  # The target column to perform aggregation against.
    filter: Optional[SparkSqlExpression]  # Represents the filter statement before the aggregation.
    groupBy: Optional[SparkSqlExpression]  # Represents the target to be grouped by before aggregation.
    limit: Optional[int]  # Represents the max number of groups (with aggregation results) to return.
