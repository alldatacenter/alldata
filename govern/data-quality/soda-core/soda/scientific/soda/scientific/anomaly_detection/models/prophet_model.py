"""Prophet predictor model class."""

import multiprocessing
import os
import sys
import warnings
from dataclasses import dataclass
from typing import Any, Dict, Optional

warnings.filterwarnings(
    "ignore",
    ".*The frame.append method is deprecated and will be removed from pandas in a future version. Use pandas.concat instead.*",
)
import numpy as np
import pandas as pd
from soda.common.logs import Logs

from soda.scientific.anomaly_detection.models.base import BaseDetector


class SuppressStdoutStderr:
    """
    Contex manager to do deep log suppression.

    Suppresses stdout and stderr in
    Python, i.e. will suppress all print, even if the print originates in a
    compiled C/Fortran sub-function.
       This will not suppress raised exceptions, since exceptions are printed
    to stderr just before a script exits, and after the context manager has
    exited (at least, I think that is why it lets exceptions through).
    """

    def __init__(self):  # noqa: D105
        # Open a pair of null files
        self.null_fds = [os.open(os.devnull, os.O_RDWR) for x in range(2)]
        # Save the actual stdout (1) and stderr (2) file descriptors.
        self.save_fds = [os.dup(1), os.dup(2)]

    def __enter__(self):  # noqa: D105
        # Assign the null pointers to stdout and stderr.
        os.dup2(self.null_fds[0], 1)
        os.dup2(self.null_fds[1], 2)

    def __exit__(self, *_):  # noqa: D105
        # Re-assign the real stdout/stderr back to (1) and (2)
        os.dup2(self.save_fds[0], 1)
        os.dup2(self.save_fds[1], 2)
        # Close the null files
        for fd in self.null_fds + self.save_fds:
            os.close(fd)


with SuppressStdoutStderr():
    from prophet import Prophet


@dataclass
class DetectorMessageComponent:
    """Defines the error code data object from freq detection."""

    log_message: str
    severity: str
    error_code_int: int
    error_code_str: str


ERROR_CODE_LEVEL_CUTTOFF = 99

DETECTOR_MESSAGES: Dict[str, DetectorMessageComponent] = {
    "native_freq": DetectorMessageComponent(
        log_message="native frequency detected",
        severity="info",
        error_code_int=0,
        error_code_str="Native frequency is detected successfully",
    ),
    "converted_daily_no_dupes": DetectorMessageComponent(
        log_message="converted to daily frequency no dupes with time info removed",
        severity="info",
        error_code_int=1,
        error_code_str="",
    ),
    "coerced_daily": DetectorMessageComponent(
        log_message="Coerced to daily frequency with last daily time point kept",
        severity="warn",
        error_code_int=2,
        error_code_str="made_daily_keeping_last_point_only",
    ),
    "last_four": DetectorMessageComponent(
        log_message="Frequency inferred from the last 4 stable data points",
        severity="warn",
        error_code_int=3,
        error_code_str="frequency_from_last_4_points",
    ),
    "not_enough_measurements": DetectorMessageComponent(
        log_message="Data frame must have at least 4 measurements",
        severity="error",
        error_code_int=100,
        error_code_str="not_enough_measurements",
    ),
    "bailing_out": DetectorMessageComponent(
        log_message="All attempts to detect the datset frequency failed. Process terminated.",
        severity="error",
        error_code_int=ERROR_CODE_LEVEL_CUTTOFF,
        error_code_str="all_freq_detection_attempts_failed",
    ),
}


@dataclass
class FreqDetectionResult:
    """Frequency Detection Result data model."""

    inferred_frequency: Optional[str]
    df: pd.DataFrame
    freq_detection_strategy: str
    error_code_int: int
    error_code: str
    error_severity: str
    error_message: str


class PreprocessError(Exception):
    """Thrown in case of pre-processing.

    To be raised and passed as a result error message down the line.
    """


class ProphetDetector(BaseDetector):
    """ProphetDetector."""

    # TODO: [CLOUD-2823] Deprecate use of class variable to indicate integer type metrics
    integer_type_metrics = ["row_count", "duplicate_count", "missing_count", "invalid_count"]

    def __init__(
        self,
        logs: Logs,
        params: Dict[str, Any],
        time_series_data: pd.DataFrame,
        metric_name: str,
        has_exegonenous_regressor: bool = False,
    ) -> None:
        """Constructor for ProphetDetector

        Args:
            params (Dict[str, Any]): config class parsed from detector_config.yml.
            request (Request): Request class containing parsed and validated request data.

        Returns:
            None
        """
        super().__init__(params, time_series_data)  # runs the measurement elimination that is contained in the base

        try:
            if "pytest" not in sys.argv[0]:
                multiprocessing.set_start_method("fork")
        except:
            pass

        self._logs = logs
        self._params = params
        self._prophet_detector_params = self._params["prophet_detector"]
        self._preprocess_params = self._prophet_detector_params["preprocess_params"]
        self._detector_params = self._prophet_detector_params["prophet_setup"]
        self._anomaly_detection_params = self._prophet_detector_params["anomaly_detection"]
        self._request_params = self._params["request_params"]
        self._n_points = self._anomaly_detection_params["n_points"]
        self._criticality_threshold = self._anomaly_detection_params["criticality_threshold"]
        self._suppress_stan = self._prophet_detector_params["suppress_stan"]
        self._is_trained: bool = False
        self._has_exogenous_regressor = has_exegonenous_regressor
        self.time_series_data = time_series_data  # this gets potentially rewritten when runnin skip measurements
        self.uncertainty_bounds_require_integer_rounding: bool = metric_name in self.integer_type_metrics

        # public attrs
        self.model: Prophet
        self.predictions: pd.DataFrame
        self.anomalies: pd.DataFrame
        self.time_series: pd.DataFrame

    @property
    def time_series_df(self):
        assert (
            self.time_series
        ), "ProphetDetector does not have a time_series attribute. Make sure you run preprocess first"
        return self.time_series_dd
        # return self.time_series.pd_dataframe()

    def detect_frequency_better(self) -> FreqDetectionResult:
        # check if pandas detected a frequency by itself.
        assert not self.time_series_data.empty, "df cannot be empty"
        not_enough_measurements_freq_result = FreqDetectionResult(
            None,
            pd.DataFrame(),
            freq_detection_strategy="not_enough_measurements",
            error_code_int=DETECTOR_MESSAGES["not_enough_measurements"].error_code_int,
            error_code=DETECTOR_MESSAGES["not_enough_measurements"].error_code_str,
            error_severity=DETECTOR_MESSAGES["not_enough_measurements"].severity,
            error_message=DETECTOR_MESSAGES["not_enough_measurements"].log_message,
        )

        if not len(self.time_series_data) >= 4:
            return not_enough_measurements_freq_result

        _df = self.time_series_data.copy()
        _df["ds"] = _df["ds"].dt.tz_localize(None)
        _df = _df.set_index("ds")
        _df = _df.sort_index()
        inferred_frequency = pd.infer_freq(_df.index)
        if inferred_frequency and isinstance(_df, pd.DataFrame):
            self._logs.info(DETECTOR_MESSAGES["native_freq"].log_message)
            return FreqDetectionResult(
                inferred_frequency,
                _df.tz_localize(None).reset_index(),
                freq_detection_strategy="native_freq",
                error_code_int=DETECTOR_MESSAGES["native_freq"].error_code_int,
                error_code=DETECTOR_MESSAGES["native_freq"].error_code_str,
                error_severity=DETECTOR_MESSAGES["native_freq"].severity,
                error_message=DETECTOR_MESSAGES["native_freq"].log_message,
            )
        #   # if FAILED:
        #       # is it in fact a "daily dataset"?
        #           # chuck time info
        #           # get unique dates, if dupes it's not a daily if not "it is daily".
        #           # impute/fill missing dates + values via interpolation
        #           # make sure we can have a count of the number of the we're about to impute.
        #           # if below rejection threshold, make it a TS and run with it.
        #           # capture a warning and push it into the results.
        _df = _df.reset_index()
        _df["ds"] = _df["ds"].dt.normalize()
        has_dupe_dates = _df.duplicated(subset=["ds"]).any()
        if not has_dupe_dates:
            self._logs.info("Anomaly Detection Frequency Warning: Converted into daily dataset with no data dropping")
            return FreqDetectionResult(
                inferred_frequency="D",
                df=_df,
                freq_detection_strategy="converted_daily_no_dupes",
                error_code_int=DETECTOR_MESSAGES["native_freq"].error_code_int,
                error_code=DETECTOR_MESSAGES["converted_daily_no_dupes"].error_code_str,
                error_severity=DETECTOR_MESSAGES["converted_daily_no_dupes"].severity,
                error_message=DETECTOR_MESSAGES["converted_daily_no_dupes"].log_message,
            )

        #       # if not a near daily, then it's more frequent and we cannot chuck the time
        #       # since we did not get a freq before we know we're still stuffed.
        #           # we either make it be daily (this is the current solution --but I really don't like it)
        if self._preprocess_params.get("assume_daily", False):
            _df = _df.drop_duplicates("ds", keep="last")
            if isinstance(_df, pd.DataFrame):
                self._logs.warning(
                    "Anomaly Detection Frequency Warning: Coerced into daily dataset with last daily time point kept"
                )
                if len(_df) >= 4:
                    return FreqDetectionResult(
                        inferred_frequency="D",
                        df=_df,
                        freq_detection_strategy="coerced_daily",
                        error_code_int=DETECTOR_MESSAGES["native_freq"].error_code_int,
                        error_code=DETECTOR_MESSAGES["coerced_daily"].error_code_str,
                        error_severity=DETECTOR_MESSAGES["coerced_daily"].severity,
                        error_message=DETECTOR_MESSAGES["coerced_daily"].log_message,
                    )
                else:
                    return not_enough_measurements_freq_result
        #           # we take the last 4 data points. Try to get a freq on that.
        _df = _df.set_index("ds")
        _df = _df.sort_index()
        inferred_frequency = pd.infer_freq(_df[-4:])
        _df = _df.reset_index()
        if inferred_frequency and isinstance(_df, pd.DataFrame):
            self._logs.warning(
                "Anomaly Detection Frequency Warning: Using inferred frequency from the last 4 data points."
            )
            return FreqDetectionResult(
                inferred_frequency=inferred_frequency,
                df=_df,
                freq_detection_strategy="last_four",
                error_code_int=DETECTOR_MESSAGES["native_freq"].error_code_int,
                error_code=DETECTOR_MESSAGES["last_four"].error_code_str,
                error_severity=DETECTOR_MESSAGES["last_four"].severity,
                error_message=DETECTOR_MESSAGES["last_four"].log_message,
            )
        #           # if we get it:
        #               # make it be the freq of the df, fill missing dates and values and run with it.
        #               # do we want then to run ADS only from those measurements? How do we keep track of that?
        #               # how do we communcate this to our users? Is it even a good idea to do that at all?
        raise PreprocessError(DETECTOR_MESSAGES["bailing_out"].log_message)

    def preprocess(self):
        missing_values = self.time_series_data.isnull().sum().sum()
        if self._preprocess_params["warn_if_missing_values"] and missing_values:
            self._logs.debug(f"dataframe has {missing_values} missing values.")

        try:
            self.freq_detection_result = self.detect_frequency_better()
            if self.freq_detection_result.error_severity == "error":
                self._logs.warning(DETECTOR_MESSAGES[self.freq_detection_result.freq_detection_strategy].log_message)
                return

        except Exception as e:
            if isinstance(e, PreprocessError):
                self.freq_detection_result = FreqDetectionResult(
                    None,
                    pd.DataFrame(),
                    freq_detection_strategy="bailing_out",
                    error_code_int=DETECTOR_MESSAGES["bailing_out"].error_code_int,
                    error_code=DETECTOR_MESSAGES["bailing_out"].error_code_str,
                    error_severity=DETECTOR_MESSAGES["bailing_out"].severity,
                    error_message=DETECTOR_MESSAGES["bailing_out"].log_message,
                )
            self._logs.error(e)
            return

        if isinstance(self.time_series_data, pd.DataFrame):
            if self._has_exogenous_regressor:
                self.time_series = self.time_series_data[["ds", "y", "external_regressor"]]
            else:
                self.time_series = self.time_series_data[["ds", "y"]]

        if not self._preprocess_params.get("interpolation_kwargs").get("method"):
            self._preprocess_params["interpolation_kwargs"]["method"] = "linear"

        self.time_series = self.time_series.set_index("ds")
        self.time_series = self.time_series.resample(self.freq_detection_result.inferred_frequency).mean()
        self.time_series = self.time_series.reset_index()
        self.time_series["y"] = self.time_series["y"].interpolate(**self._preprocess_params["interpolation_kwargs"])
        if self._has_exogenous_regressor:
            self.time_series["external_regressor"] = self.time_series["external_regressor"].fillna(0)

    def setup_fit_predict(self):
        """Sets up Prophet model and fits it on the self.time_series_data."""

        self._logs.debug(
            f"Anomaly Detection: Fitting prophet model with the following parameters:\n{self._detector_params}"
        )
        if "external_regressor" in self.time_series:
            self._logs.info(
                "Anomaly Detection: Found a custom external_regressor derived from user feedback and adding it to Prophet model"
            )
            self.model = Prophet(**self._detector_params).add_regressor("external_regressor", mode="multiplicative")
        else:
            self._logs.debug("Anomaly Detection: No external_regressor/user feedback found")
            self.model = Prophet(**self._detector_params)
        if self._suppress_stan:
            with SuppressStdoutStderr():
                self.model.fit(self.time_series)
        else:
            self.model.fit(self.time_series)
        self.predictions = self.model.predict(self.time_series)
        self._is_trained = True

    def detect_anomalies(self):
        assert (
            self._is_trained
        ), "ProphetDetector has not been trained yet. Make sure you run `setup_and_train_ts_model` first"

        # pick out n-last points to detect anomalies for
        if self._n_points:
            self.predictions = self.predictions.iloc[-self._n_points :]  # noqa: E203

        self.predictions["real_data"] = self.time_series["y"].reset_index(drop=True)

        self._logs.debug(f"Anomaly Detection: detecting anomalies for the last {self._n_points} points.")
        # round/trucate because floats are shit and cause precision errors
        latest_measurement = self.predictions.iloc[-1]
        no_integer_between_bounds = (
            np.abs(np.ceil(latest_measurement["yhat_upper"]) - np.floor(latest_measurement["yhat_lower"])) < 2
        )
        if no_integer_between_bounds and self.uncertainty_bounds_require_integer_rounding:
            self.predictions["yhat_lower"] = np.floor(self.predictions["yhat_lower"])
            self.predictions["yhat_upper"] = np.ceil(self.predictions["yhat_upper"])
        else:
            self.predictions["real_data"] = self.predictions["real_data"].round(10)
            self.predictions["yhat_lower"] = self.predictions["yhat_lower"].round(10)
            self.predictions["yhat_upper"] = self.predictions["yhat_upper"].round(10)

        # flag data points that fall out of confidence bounds
        self.predictions["is_anomaly"] = 0
        self.predictions.loc[self.predictions["real_data"] > self.predictions["yhat_upper"], "is_anomaly"] = 1
        self.predictions.loc[self.predictions["real_data"] < self.predictions["yhat_lower"], "is_anomaly"] = -1

        self.anomalies = self.predictions

    def generate_severity_zones(self):
        assert (
            not self.anomalies.empty
        ), "Anomalies have not been detected yet. Make sure you run `detect_anomalies` first."
        # See criticality_threshold_calc method, the critical zone will always take over and
        # "extend" or replace the extreme to inf points of the warning zone.
        self.anomalies.loc[:, "critical_greater_than_or_equal"] = self.anomalies.apply(
            lambda x: self._criticality_threshold_calc(
                x, threshold=self._criticality_threshold, directionality="upper"
            ),
            axis=1,
        )
        self.anomalies.loc[:, "critical_lower_than_or_equal"] = self.anomalies.apply(
            lambda x: self._criticality_threshold_calc(
                x, threshold=self._criticality_threshold, directionality="lower"
            ),
            axis=1,
        )

        # The bounds for warning are in fact anything that is outside of the model's
        # confidence bounds so we simply reassign them to another column.
        self.anomalies.loc[:, "warning_greater_than_or_equal"] = self.anomalies["yhat_upper"]
        self.anomalies.loc[:, "warning_lower_than_or_equal"] = self.anomalies["yhat_lower"]

    @staticmethod
    def _criticality_threshold_calc(row, threshold: float, directionality: str) -> float:
        if directionality == "upper":
            adder_arg = row["yhat_upper"]
        elif directionality == "lower":
            adder_arg = row["yhat_lower"]
        else:
            raise NotImplementedError(f"directionality can only be 'upper' or 'lower' not {directionality}.")
        confidence_spread = row["yhat_upper"] - (row["yhat_lower"])
        adder = confidence_spread * threshold
        if directionality == "upper":
            return adder_arg + adder
        return adder_arg - adder

    def compute_alert_level(self) -> None:
        assert (
            not self.anomalies.empty
        ), "Anomalies have not been detected yet. Make suere you run `detect_anomalies` first."
        self.anomalies.loc[:, "level"] = "pass"
        self.anomalies.loc[
            (self.anomalies["real_data"] <= self.anomalies["warning_lower_than_or_equal"])
            & (self.anomalies["real_data"] > self.anomalies["critical_lower_than_or_equal"])
            & self.anomalies["is_anomaly"]
            != 0,
            "level",
        ] = "warn"
        self.anomalies.loc[
            (self.anomalies["real_data"] >= self.anomalies["warning_greater_than_or_equal"])
            & (self.anomalies["real_data"] < self.anomalies["critical_greater_than_or_equal"])
            & self.anomalies["is_anomaly"]
            != 0,
            "level",
        ] = "warn"
        self.anomalies.loc[
            (self.anomalies["real_data"] >= self.anomalies["critical_greater_than_or_equal"])
            & self.anomalies["is_anomaly"]
            != 0,
            "level",
        ] = "fail"
        self.anomalies.loc[
            (self.anomalies["real_data"] <= self.anomalies["critical_lower_than_or_equal"])
            & self.anomalies["is_anomaly"]
            != 0,
            "level",
        ] = "fail"

    def run(self) -> pd.DataFrame:
        """Convenience orchestrator that outputs last anomalies as a pd.DataFrame."""
        try:
            if self._suppress_stan:
                pd.set_option("mode.chained_assignment", None)
            self.skip_measurements()
            self.preprocess()
            if self.freq_detection_result.error_code_int < ERROR_CODE_LEVEL_CUTTOFF:
                self.setup_fit_predict()
                self.detect_anomalies()
                self.generate_severity_zones()
                self.compute_alert_level()
                return self.anomalies
            else:
                self.anomalies = pd.DataFrame()
                return self.anomalies
        except Exception as e:
            self._logs.error(e, exception=e)
            raise e
