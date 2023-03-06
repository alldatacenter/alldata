import datetime
from pathlib import Path
from typing import Any, Dict, List, Mapping, Optional, Tuple

import pandas as pd
import yaml
from pydantic import BaseModel, validator
from soda.common.logs import Logs

from soda.scientific.anomaly_detection.feedback_processor import FeedbackProcessor
from soda.scientific.anomaly_detection.models.prophet_model import (
    FreqDetectionResult,
    ProphetDetector,
)


class UserFeedback(BaseModel):
    """Validation model for user feedback data dict in payload."""

    isCorrectlyClassified: Optional[bool] = None
    isAnomaly: Optional[bool] = None
    reason: Optional[str] = None
    freeTextReason: Optional[str] = None
    skipMeasurements: Optional[str] = None

    @validator("skipMeasurements")
    def check_accepted_values_skip_measurements(cls, v):
        accepted_values = ["this", "previous", "previousAndThis", None]
        assert v in accepted_values, f"skip_measurements must be one of {accepted_values}, but '{v}' was provided."
        return v


class SeverityLevelAreas(BaseModel):
    """Validates severity levels dicts."""

    greaterThanOrEqual: Optional[float] = None
    lessThanOrEqual: Optional[float] = None


class AnomalyDiagnostics(BaseModel):
    value: Optional[float] = None
    fail: Optional[SeverityLevelAreas] = None
    warn: Optional[SeverityLevelAreas] = None
    anomalyProbability: Optional[float] = None
    anomalyPredictedValue: Optional[float] = None
    anomalyErrorSeverity: str = "pass"
    anomalyErrorCode: str = ""
    anomalyErrorMessage: str = ""


class LocationModel(BaseModel):
    filePath: Optional[str] = None
    line: Optional[int] = None
    col: Optional[int] = None


# some of those fields might end up being ignored down the line by ADS
class AnomalyResult(BaseModel):
    identity: Optional[str] = None
    measurementId: Optional[str] = None
    type: Optional[str] = None
    definition: Optional[str] = None
    location: LocationModel = LocationModel()
    metrics: Optional[List[str]] = None
    dataSource: Optional[str] = None
    table: Optional[str] = None
    partition: Optional[str] = None
    column: Optional[str] = None
    outcome: Optional[str] = None
    diagnostics: AnomalyDiagnostics = AnomalyDiagnostics()
    feedback: Optional[UserFeedback] = UserFeedback()


class AnomalyHistoricalCheckResults(BaseModel):
    results: List[AnomalyResult]


class AnomalyHistoricalMeasurement(BaseModel):
    id: str
    identity: str
    value: float
    dataTime: datetime.datetime


class AnomalyHistoricalMeasurements(BaseModel):
    results: List[AnomalyHistoricalMeasurement]


class AnomalyDetector:
    def __init__(self, measurements, check_results, logs: Logs, metric_name: str):
        self._logs = logs
        self.metric_name = metric_name
        self.df_measurements = self._parse_historical_measurements(measurements)
        self.df_check_results = self._parse_historical_check_results(check_results)
        self.params = self._parse_params()

    def evaluate(self) -> Tuple[str, Dict[str, Any]]:
        df_historic = self._convert_to_well_shaped_df()

        feedback = FeedbackProcessor(params=self.params, df_historic=df_historic, logs=self._logs)
        feedback.run()

        detector = ProphetDetector(
            logs=self._logs,
            params=self.params,
            time_series_data=feedback.df_feedback_processed,
            metric_name=self.metric_name,
            has_exegonenous_regressor=feedback.has_exegonenous_regressor,
        )
        df_anomalies = detector.run()

        level, diagnostics = self._parse_output(df_anomalies, detector.freq_detection_result)

        return level, diagnostics

    @staticmethod
    def _parse_historical_measurements(measurements: Dict[str, List[Dict[str, Any]]]) -> pd.DataFrame:
        if measurements:
            parsed_measurements = AnomalyHistoricalMeasurements.parse_obj(measurements)
            _df_measurements = pd.DataFrame.from_dict(parsed_measurements.dict()["results"])
            return _df_measurements
        else:
            raise ValueError("No historical measurements found.")

    def _parse_historical_check_results(self, check_results: Dict[str, List[Dict[str, Any]]]) -> pd.DataFrame:
        if check_results.get("results"):
            parsed_check_results = AnomalyHistoricalCheckResults.parse_obj(check_results)
            _df_check_results = pd.DataFrame.from_dict(parsed_check_results.dict()["results"])
            return _df_check_results
        else:
            self._logs.debug(
                "No past check results found. This could be because there are no past runs of "
                "Anomaly Detection for this check yet."
            )
            parsed_check_results = AnomalyHistoricalCheckResults(results=[AnomalyResult()])
            _df_check_results = pd.DataFrame.from_dict(parsed_check_results.dict()["results"])
            return _df_check_results

    def _convert_to_well_shaped_df(self) -> pd.DataFrame:
        if not self.df_check_results.empty:
            self._logs.debug("Got test results from data request. Merging it with the measurements")
            df = self.df_measurements.merge(
                self.df_check_results,
                how="left",
                left_on="id",
                right_on="measurementId",
                suffixes=("", "_tr"),
            )
        else:
            df = self.df_measurements.copy()

        # Flatten diagnostics dictionary
        if "diagnostics" in df.columns:
            df_flattened = self.flatten_df(df.copy(), "diagnostics")

        column_maps = self.params["request_params"]["columns_mapping"]
        df_flattened = df_flattened[df_flattened.columns[df_flattened.columns.isin(list(column_maps.keys()))]]
        df_flattened = df_flattened.rename(columns=column_maps)  # type: ignore
        df_flattened["ds"] = pd.to_datetime(df_flattened["ds"])  # type: ignore
        df_flattened["ds"] = df_flattened["ds"].dt.tz_localize(None)
        return df_flattened

    @staticmethod
    def flatten_df(df: pd.DataFrame, target_col_name: str) -> pd.DataFrame:
        assert isinstance(df, pd.DataFrame)
        assert not df.empty

        df[target_col_name] = df[target_col_name].apply(lambda x: {} if pd.isnull(x) else x)

        target_array_to_flatten = list(df[target_col_name].values)
        df_flattened = pd.DataFrame.from_dict(target_array_to_flatten)  # type: ignore
        df_joined = pd.merge(
            df,
            df_flattened,
            left_index=True,
            right_index=True,
            suffixes=("", "_diag"),
        )
        return df_joined

    def _parse_params(self) -> Dict[str, Any]:
        try:
            this_dir = Path(__file__).parent.resolve()
            config_file = this_dir.joinpath("detector_config.yaml")
            # Read detector configuration
            with open(config_file) as stream:
                loaded_config = yaml.safe_load(stream)

            # Manipulate configuration
            loaded_config["response_params"]["output_columns"] = self._replace_none_values_by_key(
                loaded_config["response_params"]["output_columns"]
            )
            loaded_config["feedback_processor_params"]["output_columns"] = self._replace_none_values_by_key(
                loaded_config["feedback_processor_params"]["output_columns"]
            )
            self._logs.debug(f"Anomaly Detection: config parsed {loaded_config}")

            return loaded_config

        except Exception as e:
            self._logs.error(e)
            raise e

    @staticmethod
    def _replace_none_values_by_key(dct: Dict[str, Any]) -> Mapping[str, Any]:
        result = {}
        for key, value in dct.items():
            if value is None:
                value = key
            result[key] = value
        return result

    @staticmethod
    def _parse_output(
        df_anomalies: pd.DataFrame, freq_detection_result: FreqDetectionResult
    ) -> Tuple[str, Dict[str, Any]]:
        if not df_anomalies.empty:
            results_dict = df_anomalies.to_dict(orient="records")[0]
            level = results_dict["level"]
            diagnostics = {
                "value": results_dict["real_data"],
                "warn": {
                    "greaterThanOrEqual": results_dict["warning_greater_than_or_equal"],
                    "lessThanOrEqual": results_dict["warning_lower_than_or_equal"],
                },
                "fail": {
                    "greaterThanOrEqual": results_dict["critical_greater_than_or_equal"],
                    "lessThanOrEqual": results_dict["critical_lower_than_or_equal"],
                },
                "anomalyProbability": results_dict["anomaly_probability"],
                "anomalyPredictedValue": results_dict["yhat"],
                "anomalyErrorSeverity": freq_detection_result.error_severity,
                "anomalyErrorCode": freq_detection_result.error_code,
                "anomalyErrorMessage": freq_detection_result.error_message,
            }
        else:
            level = "pass"
            diagnostics = {
                "value": None,
                "warn": None,
                "fail": None,
                "anomalyProbability": None,
                "anomalyPredictedValue": None,
                "anomalyErrorSeverity": freq_detection_result.error_severity,
                "anomalyErrorCode": freq_detection_result.error_code,
                "amomalyErrorMessage": freq_detection_result.error_message,
            }

        diagnostics_dict: Dict[str, Any] = AnomalyDiagnostics.parse_obj(diagnostics).dict()
        return level, diagnostics_dict
