from numpy import nan

test_anomaly_detector_evaluate_historic_measurements = {
    "results": [
        {
            "id": "49d198f1-eda7-42ad-bd70-5e1789bdf122",
            "identity": "metric-test-adventureworks-anomaly_detection_test-row_count",
            "value": 21.0,
            "dataTime": "2022-04-20T15:05:30Z",
        },
        {
            "id": "959e5167-39e0-481b-9939-8ff7393391a5",
            "identity": "metric-test-adventureworks-anomaly_detection_test-row_count",
            "value": 21.0,
            "dataTime": "2022-04-19T15:05:10Z",
        },
        {
            "id": "efc8f472-3d74-4a9a-965f-de14dcf4b2a9",
            "identity": "metric-test-adventureworks-anomaly_detection_test-row_count",
            "value": 2.0,
            "dataTime": "2022-04-18T14:49:59Z",
        },
        {
            "id": "42a2b60b-d932-411d-9cab-bf7c33a84c65",
            "identity": "metric-test-adventureworks-anomaly_detection_test-row_count",
            "value": 1.0,
            "dataTime": "2022-04-17T14:49:20Z",
        },
        {
            "id": "3ef53638-04cc-4614-b587-a059a81a4c2f",
            "identity": "metric-test-adventureworks-anomaly_detection_test-row_count",
            "value": 1.0,
            "dataTime": "2022-04-16T14:47:44Z",
        },
        {
            "id": "b7dd6e88-f7a0-42c4-87c1-0662eb0e2ce5",
            "identity": "metric-test-adventureworks-anomaly_detection_test-row_count",
            "value": 21.0,
            "dataTime": "2022-04-15T15:04:42Z",
        },
    ]
}

test_anomaly_detector_evaluate_historic_check_results = {
    "results": [
        {
            "type": "anomalyDetection",
            "testId": "8cedb608-e8be-4147-83be-0f86972788a9",
            "testResultId": "97a8ce8b-d43d-4f5e-932b-b1103c637fe7",
            "measurementId": "49d198f1-eda7-42ad-bd70-5e1789bdf122",
            "outcome": "pass",
            "dataTime": "2022-04-20T15:05:30Z",
            "diagnostics": {
                "value": 21.0,
                "anomalyPredictedValue": 20.870516335508597,
                "anomalyErrorSeverity": "warn",
                "anomalyErrorCode": "made_daily_keeping_last_point_only",
                "fail": {
                    "lessThanOrEqual": 20.15142652696,
                    "greaterThanOrEqual": 22.03561767424,
                },
                "warn": {
                    "lessThanOrEqual": 20.3084424559,
                    "greaterThanOrEqual": 21.8786017453,
                },
            },
        },
        {
            "type": "anomalyDetection",
            "testId": "8cedb608-e8be-4147-83be-0f86972788a9",
            "testResultId": "5ebf531e-83dd-445d-ac91-42cd9af45be2",
            "measurementId": "7fd4f85b-37b6-46f7-b6b8-56af91b3f920",
            "outcome": "pass",
            "dataTime": "2022-04-12T15:00:31Z",
            "diagnostics": {
                "value": 2.0,
                "anomalyPredictedValue": 8.46757918589978,
                "anomalyErrorSeverity": "warn",
                "anomalyErrorCode": "made_daily_keeping_last_point_only",
                "fail": {
                    "lessThanOrEqual": -1.89282665126,
                    "greaterThanOrEqual": 17.89954537186,
                },
                "warn": {
                    "lessThanOrEqual": -0.243462316,
                    "greaterThanOrEqual": 16.2501810366,
                },
            },
        },
        {
            "type": "anomalyDetection",
            "testId": "8cedb608-e8be-4147-83be-0f86972788a9",
            "testResultId": "18b6fc89-fea3-4a5f-8c8f-bde7634d016c",
            "measurementId": "d926b795-cf98-4e96-9eea-bf688c86d773",
            "outcome": "pass",
            "dataTime": "2022-04-12T14:59:28Z",
            "diagnostics": {
                "value": 2.0,
                "anomalyPredictedValue": 8.46757918589978,
                "anomalyErrorSeverity": "warn",
                "anomalyErrorCode": "made_daily_keeping_last_point_only",
                "fail": {
                    "lessThanOrEqual": -2.54660323027,
                    "greaterThanOrEqual": 18.60637946177,
                },
                "warn": {
                    "lessThanOrEqual": -0.7838546726,
                    "greaterThanOrEqual": 16.8436309041,
                },
            },
        },
    ]
}

test_anomaly_detector_evaluate_expected_results = {
    "value": 21.0,
    "fail": {"greaterThanOrEqual": 42.59848061337, "lessThanOrEqual": -10.97031870027},
    "warn": {"greaterThanOrEqual": 38.1344140039, "lessThanOrEqual": -6.5062520908},
    "anomalyPredictedValue": 15.419588986390275,
    "anomalyErrorSeverity": "pass",
    "anomalyErrorCode": "",
    "feedback": {
        "isCorrectlyClassified": "None",
        "isAnomaly": "None",
        "reason": "None",
        "freeTextReason": "None",
        "skipMeasurements": "None",
    },
}

test_prophet_model_skip_measurements_this_exclusive_previous = {
    "y": {
        0: 245.0,
        1: 45.0,
        2: 40.0,
        3: 35.0,
        4: 30.0,
        5: 25.0,
        6: 20.0,
        7: 15.0,
        8: 10.0,
        9: 5.0,
        10: 250.0,
    },
    "ds": {
        0: "2023-02-15 11:00:00",
        1: "2023-02-14 11:00:00",
        2: "2023-02-13 11:00:00",
        3: "2023-02-12 11:00:00",
        4: "2023-02-11 11:00:00",
        5: "2023-02-10 11:00:00",
        6: "2023-02-09 11:00:00",
        7: "2023-02-08 11:00:00",
        8: "2023-02-07 11:00:00",
        9: "2023-02-06 11:00:00",
        10: "2023-02-16 11:00:00",
    },
    "skipMeasurements": {
        0: None,
        1: "this",
        2: None,
        3: None,
        4: None,
        5: "previous",
        6: None,
        7: None,
        8: None,
        9: None,
        10: nan,
    },
}

test_prophet_model_skip_measurements_this_exclusive_previous_expectation = {
    "y": {0: 25.0, 1: 30.0, 2: 35.0, 3: 40.0, 5: 245.0, 6: 250.0},
    "ds": {
        0: "2023-02-10 11:00:00",
        1: "2023-02-11 11:00:00",
        2: "2023-02-12 11:00:00",
        3: "2023-02-13 11:00:00",
        5: "2023-02-15 11:00:00",
        6: "2023-02-16 11:00:00",
    },
    "skipMeasurements": {0: "previous", 1: None, 2: None, 3: None, 5: None, 6: nan},
}


test_prophet_model_skip_measurements_previousAndThis = {
    "y": {
        0: 250.0,
        1: 245.0,
        2: 40.0,
        3: 35.0,
        4: 30.0,
        5: 255.0,
    },
    "ds": {
        0: "2023-02-15 11:00:00",
        1: "2023-02-14 11:00:00",
        2: "2023-02-13 11:00:00",
        3: "2023-02-12 11:00:00",
        4: "2023-02-11 11:00:00",
        5: "2023-02-16 11:00:00",
    },
    "skipMeasurements": {
        0: None,
        1: "previousAndThis",
        2: None,
        3: None,
        4: None,
        5: nan,
    },
}

test_prophet_model_skip_measurements_previousAndThis_expectation = {
    "y": {0: 250.0, 1: 255},
    "ds": {
        0: "2023-02-15 11:00:00",
        1: "2023-02-16 11:00:00",
    },
    "skipMeasurements": {0: None, 1: nan},
}


test_feedback_processor_seasonality_skip_measurements = {
    "y": {
        0: 42.0,
        1: 41.0,
        2: 40.0,
        3: 35.0,
    },
    "ds": {
        0: "2023-03-06 11:00:00",
        1: "2023-03-05 11:00:00",
        2: "2023-03-04 11:00:00",
        3: "2023-03-03 11:00:00",
    },
    "feedback": {
        0: {
            "isCorrectlyClassified": False,
            "isAnomaly": False,
            "reason": "expectedWeeklySeasonality",
            "freeTextReason": None,
            "skipMeasurements": None,
        },
        1: {
            "isCorrectlyClassified": None,
            "isAnomaly": None,
            "reason": None,
            "freeTextReason": None,
            "skipMeasurements": "previousAndThis",
        },
        2: {
            "isCorrectlyClassified": None,
            "isAnomaly": None,
            "reason": None,
            "freeTextReason": None,
            "skipMeasurements": None,
        },
        3: {
            "isCorrectlyClassified": True,
            "isAnomaly": True,
            "reason": None,
            "freeTextReason": None,
            "skipMeasurements": None,
        },
    },
    "anomaly_probability": {
        0: 0.0,
        1: 0.0,
        2: 0.0,
        3: 0.0,
    },
    "anomaly_predicted_value": {
        0: 42.83252400162256,
        1: 40.95227600086607,
        2: 38.65839999718243,
        3: 35.780745001179646,
    },
}

test_feedback_processor_seasonality_skip_measurements_expectation = {
    "y": {0: 42.0, 1: 41.0, 2: 40.0, 3: 35.0},
    "ds": {
        0: "2023-03-06 11:00:00",
        1: "2023-03-05 11:00:00",
        2: "2023-03-04 11:00:00",
        3: "2023-03-03 11:00:00",
    },
    "skipMeasurements": {0: None, 1: "previousAndThis", 2: None, 3: None},
    "external_regressor": {
        0: -0.8325240016225592,
        1: 0,
        2: 0,
        3: 0,
    },
}

test_feedback_processor_prophet_model_skip_measurements_expectation = {
    "y": {
        0: 42.0,
    },
    "ds": {
        0: "2023-03-06 11:00:00",
    },
    "skipMeasurements": {
        0: None,
    },
    "external_regressor": {
        0: -0.8325240016225592,
    },
}
