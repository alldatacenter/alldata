from typing import Dict
from unittest.mock import MagicMock

import pytest
from pytest_mock import MockerFixture

from feathr.constants import OUTPUT_PATH_TAG
from feathr.spark_provider._localspark_submission import _FeathrLocalSparkJobLauncher


@pytest.fixture(scope="function")
def local_spark_job_launcher(tmp_path) -> _FeathrLocalSparkJobLauncher:
    return _FeathrLocalSparkJobLauncher(
        workspace_path=str(tmp_path),
        debug_folder=str(tmp_path),
    )


@pytest.mark.parametrize(
    "job_tags,expected_result_uri", [
        (None, None),
        ({OUTPUT_PATH_TAG: "output"}, "output"),
    ]
)
def test__local_spark_job_launcher__submit_feathr_job(
    mocker: MockerFixture,
    local_spark_job_launcher: _FeathrLocalSparkJobLauncher,
    job_tags: Dict[str, str],
    expected_result_uri: str,
):
    # Mock necessary components
    local_spark_job_launcher._init_args = MagicMock(return_value=[])
    mocked_proc = MagicMock()
    mocked_proc.args = []
    mocked_proc.pid = 0

    mocked_spark_proc = mocker.patch("feathr.spark_provider._localspark_submission.Popen", return_value=mocked_proc)

    local_spark_job_launcher.submit_feathr_job(
        job_name="unit-test",
        main_jar_path="",
        main_class_name="",
        job_tags=job_tags,
    )

    # Assert if the mocked spark process has called once
    mocked_spark_proc.assert_called_once()

    # Assert job tags
    assert local_spark_job_launcher.get_job_tags() == job_tags
    assert local_spark_job_launcher.get_job_result_uri() == expected_result_uri


@pytest.mark.parametrize(
    "confs", [{}, {"spark.feathr.outputFormat": "parquet"}]
)
def test__local_spark_job_launcher__init_args(
    local_spark_job_launcher: _FeathrLocalSparkJobLauncher,
    confs: Dict[str, str],
):
    spark_args = local_spark_job_launcher._init_args(job_name=None, confs=confs)

    # Assert if spark_args contains confs at the end
    for k, v in confs.items():
        assert spark_args[-1] == f"{k}={v}"
