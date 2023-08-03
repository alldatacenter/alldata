import logging

import pandas as pd
import pytest
from assets.anomaly_detection_assets import (
    test_anomaly_detector_evaluate_expected_results,
    test_anomaly_detector_evaluate_historic_check_results,
    test_anomaly_detector_evaluate_historic_measurements,
    test_feedback_processor_prophet_model_skip_measurements_expectation,
    test_feedback_processor_seasonality_skip_measurements,
    test_feedback_processor_seasonality_skip_measurements_expectation,
    test_prophet_model_skip_measurements_previousAndThis,
    test_prophet_model_skip_measurements_previousAndThis_expectation,
    test_prophet_model_skip_measurements_this_exclusive_previous,
    test_prophet_model_skip_measurements_this_exclusive_previous_expectation,
)
from soda.common.logs import Logs

from soda.scientific.anomaly_detection.anomaly_detector import AnomalyDetector
from soda.scientific.anomaly_detection.feedback_processor import FeedbackProcessor
from soda.scientific.anomaly_detection.models.prophet_model import ProphetDetector

LOGS = Logs(logging.getLogger(__name__))


@pytest.mark.parametrize(
    "historical_measurements, historical_check_results, expectation",
    [
        pytest.param(
            test_anomaly_detector_evaluate_historic_measurements,
            test_anomaly_detector_evaluate_historic_check_results,
            test_anomaly_detector_evaluate_expected_results,
        )
    ],
)
def test_anomaly_detector_evaluate(historical_measurements, historical_check_results, expectation):
    detector = AnomalyDetector(historical_measurements, historical_check_results, logs=LOGS, metric_name="avg_length")
    _, diagnostic = detector.evaluate()
    assert diagnostic["value"] == expectation["value"]
    assert diagnostic["anomalyPredictedValue"] == pytest.approx(expectation["anomalyPredictedValue"])


PROPHET_MODEL_PARAMS = AnomalyDetector(
    {"results": []}, {"results": []}, logs=LOGS, metric_name="row_count"
)._parse_params()


@pytest.mark.parametrize(
    "time_series_with_skip_measurements, expected_filtered_time_series",
    [
        pytest.param(
            test_prophet_model_skip_measurements_this_exclusive_previous,
            test_prophet_model_skip_measurements_this_exclusive_previous_expectation,
            id="this and exclusive previous",
        ),
        pytest.param(
            test_prophet_model_skip_measurements_previousAndThis,
            test_prophet_model_skip_measurements_previousAndThis_expectation,
            id="previousAndThis",
        ),
    ],
)
def test_prophet_model_skip_measurements(time_series_with_skip_measurements, expected_filtered_time_series):
    time_series_data = pd.DataFrame(time_series_with_skip_measurements)
    time_series_data["ds"] = pd.to_datetime(time_series_data["ds"])
    detector = ProphetDetector(
        logs=LOGS,
        params=PROPHET_MODEL_PARAMS,
        time_series_data=time_series_data,
        metric_name="row_count",
    )
    detector.skip_measurements()
    filtered_time_series_data = detector.time_series_data
    expected_filtered_time_series_data = pd.DataFrame(expected_filtered_time_series)
    expected_filtered_time_series_data["ds"] = pd.to_datetime(expected_filtered_time_series_data["ds"])
    pd.testing.assert_frame_equal(filtered_time_series_data, expected_filtered_time_series_data, check_dtype=False)


@pytest.mark.parametrize(
    "historic_check_results, expected_processed_feedback",
    [
        pytest.param(
            test_feedback_processor_seasonality_skip_measurements,
            test_feedback_processor_seasonality_skip_measurements_expectation,
        )
    ],
)
def test_feedback_processor(historic_check_results, expected_processed_feedback):
    df_historic = pd.DataFrame(historic_check_results)
    df_historic["ds"] = pd.to_datetime(df_historic["ds"])

    expected_processed_feedback = pd.DataFrame(expected_processed_feedback)
    expected_processed_feedback["ds"] = pd.to_datetime(expected_processed_feedback["ds"])

    feedback_processor = FeedbackProcessor(params=PROPHET_MODEL_PARAMS, df_historic=df_historic, logs=LOGS)
    feedback_processor.run()
    pd.testing.assert_frame_equal(
        feedback_processor.df_feedback_processed, expected_processed_feedback, check_dtype=False
    )


@pytest.mark.parametrize(
    "historic_check_results, expected_filtered_time_series",
    [
        pytest.param(
            test_feedback_processor_seasonality_skip_measurements,
            test_feedback_processor_prophet_model_skip_measurements_expectation,
        )
    ],
)
def test_feedback_processor_prophet_model_skip_measurements(historic_check_results, expected_filtered_time_series):
    df_historic = pd.DataFrame(historic_check_results)
    df_historic["ds"] = pd.to_datetime(df_historic["ds"])

    df_historic = pd.DataFrame(df_historic)
    df_historic["ds"] = pd.to_datetime(df_historic["ds"])

    feedback_processor = FeedbackProcessor(params=PROPHET_MODEL_PARAMS, df_historic=df_historic, logs=LOGS)
    feedback_processor.run()

    detector = ProphetDetector(
        logs=LOGS,
        params=PROPHET_MODEL_PARAMS,
        time_series_data=feedback_processor.df_feedback_processed,
        metric_name="row_count",
        has_exegonenous_regressor=feedback_processor.has_exegonenous_regressor,
    )
    detector.skip_measurements()

    filtered_time_series_data = detector.time_series_data
    expected_filtered_time_series_data = pd.DataFrame(expected_filtered_time_series)
    expected_filtered_time_series_data["ds"] = pd.to_datetime(expected_filtered_time_series_data["ds"])
    pd.testing.assert_frame_equal(filtered_time_series_data, expected_filtered_time_series_data, check_dtype=False)
