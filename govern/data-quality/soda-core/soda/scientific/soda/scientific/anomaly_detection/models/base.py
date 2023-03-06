"""ABC for Detectors."""
from abc import ABC, abstractmethod
from typing import Any, Dict, List, Union

import pandas as pd


class BaseDetector(ABC):
    """BaseDetector."""

    def __init__(self, params: Dict[str, Any], time_series_data: pd.DataFrame) -> None:
        self._params = params
        self._data_df = time_series_data

    @abstractmethod
    def run(self):
        raise NotImplementedError("You must implement a `run()` method to instantiate this class")

    @abstractmethod
    def setup_fit_predict(self):
        raise NotImplementedError("You must implement a `setup_fit_predict` to instantiate this class")

    @abstractmethod
    def detect_anomalies(self):
        raise NotImplementedError("You must implement a `detect_anomalies` method to instantiate this class")

    @staticmethod
    def _eliminate_measurements(
        df: pd.DataFrame,
        is_inclusive: bool,
        index: Union[int, List[int]],
        is_multi_index: bool = False,
    ) -> pd.DataFrame:
        skip_from: Union[int, List[int]]
        if isinstance(index, list) and not is_multi_index:
            skip_from = max(index)
        else:
            skip_from = index

        if is_inclusive and not is_multi_index:
            assert not isinstance(skip_from, list), "skip_from cannot be a list when not multi_index is False"
            _df = df.drop(df.index[: skip_from + 1])
            if isinstance(_df, pd.DataFrame):
                return _df

        if is_inclusive and is_multi_index:
            _df = df.drop(df.index[skip_from])
            if isinstance(_df, pd.DataFrame):
                return _df

        assert not isinstance(skip_from, list), "skip_from cannot be a list when not multi_index is False"
        _df = df.drop(df.index[:skip_from])
        if isinstance(_df, pd.DataFrame):
            return _df
        raise TypeError("Did not get a df from _eliminate_measurements")

    def skip_measurements(self):
        """Eliminates measurements that are labelled as skipped by users."""
        is_inclusive = False
        max_index = []
        if "skipMeasurements" in self._data_df.columns:
            self._data_df = self._data_df.sort_values(by="ds", ascending=True)
            self._data_df = self._data_df.reset_index(drop=True)
            skip_previous_inclusive_indexes = self._data_df.index[
                self._data_df["skipMeasurements"] == "previousAndThis"
            ].tolist()  # type: ignore
            skip_previous_exclusive_indexes = self._data_df.index[
                self._data_df["skipMeasurements"] == "previous"
            ].tolist()  # type: ignore

            if skip_previous_exclusive_indexes or skip_previous_inclusive_indexes:
                max_index = max(
                    max(skip_previous_inclusive_indexes) if skip_previous_inclusive_indexes else 0,
                    max(skip_previous_exclusive_indexes) if skip_previous_exclusive_indexes else 0,
                )
                is_inclusive = max_index in skip_previous_inclusive_indexes
                skip_from = max_index
                self._data_df = self._eliminate_measurements(self._data_df, is_inclusive, skip_from)
                self._data_df = self._data_df.reset_index(drop=True)

            skip_individual_indexes = self._data_df.index[
                self._data_df["skipMeasurements"] == "this"
            ].tolist()  # type: ignore
            if skip_individual_indexes:
                self._data_df = self._eliminate_measurements(
                    self._data_df,
                    is_inclusive=True,
                    index=skip_individual_indexes,
                    is_multi_index=True,
                )
        self.time_series_data = self._data_df
