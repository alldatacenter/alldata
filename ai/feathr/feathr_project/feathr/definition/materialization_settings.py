from datetime import datetime, timedelta
from typing import List, Optional
from feathr.definition.sink import HdfsSink, RedisSink, Sink
import math


class BackfillTime:
    """Time range to materialize/backfill feature data. Please refer to https://feathr-ai.github.io/feathr/concepts/materializing-features.html#feature-backfill for a more detailed explanation.

    Attributes:
        start: start time of the backfill, inclusive.
        end: end time of the backfill, inclusive.
        step: duration of each backfill step. e.g. if you want to materialize features on daily basis, use timedelta(days=1)
    """
    def __init__(self, start: datetime, end: datetime, step: timedelta):
        self.start = start
        self.end = end
        self.step = step


class MaterializationSettings:
    """Settings about materialization features.

    Attributes:
        name: The materialization job name
        sinks: sinks where the materialized features should be written to
        feature_names: list of feature names to be materialized
        backfill_time: time range and frequency for the materialization. Default to now().
        resolution: time interval for output directories. Only support 'DAILY' and 'HOURLY' for now (DAILY by default).
                    If 'DAILY', output paths should be: yyyy/MM/dd;
                    Otherwise would be: yyyy/MM/dd/HH
    """
    def __init__(self, name: str, sinks: List[Sink], feature_names: List[str], backfill_time: Optional[BackfillTime] = None, resolution: str = "DAILY"):
        if resolution not in ["DAILY", "HOURLY"]:
            raise RuntimeError(
                f'{resolution} is not supported. Only \'DAILY\' and \'HOURLY\' are currently supported.')
        self.resolution = resolution
        self.name = name
        now = datetime.now()
        self.backfill_time = backfill_time if backfill_time else BackfillTime(start=now, end=now, step=timedelta(days=1))
        for sink in sinks:
            if isinstance(sink, HdfsSink):
                self.has_hdfs_sink = True
                sink.aggregation_features = feature_names
            elif isinstance(sink, RedisSink):
                sink.aggregation_features = feature_names
        self.sinks = sinks
        self.feature_names = feature_names

    def get_backfill_cutoff_time(self) -> List[datetime]:
        """Get the backfill cutoff time points for materialization. 
        E.g. for `BackfillTime(start=datetime(2022, 3, 1), end=datetime(2022, 3, 5), step=timedelta(days=1))`, 
        it returns cutoff time list as `[2022-3-1, 2022-3-2, 2022-3-3, 2022-3-4, 2022-3-5]`, 
        for `BackfillTime(start=datetime(2022, 3, 1, 1), end=datetime(2022, 3, 1, 5), step=timedelta(hours=1))`, 
        it returns cutoff time list as `[2022-3-1 01:00:00, 2022-3-1 02:00:00, 2022-3-1 03:00:00, 2022-3-1 04:00:00, 2022-3-1 05:00:00]`
        """        

        start_time = self.backfill_time.start
        end_time = self.backfill_time.end
        step_in_seconds = self.backfill_time.step.total_seconds()
        assert start_time <= end_time, "Start time {} must be earlier or equal to end time {}".format(start_time, end_time)
        assert step_in_seconds > 0, "Step in time range should be greater than 0, but got {}".format(step_in_seconds)
        num_delta = (self.backfill_time.end - self.backfill_time.start).total_seconds() / step_in_seconds
        num_delta = math.floor(num_delta) + 1
        return [end_time - timedelta(seconds=n*step_in_seconds) for n in reversed(range(num_delta))]