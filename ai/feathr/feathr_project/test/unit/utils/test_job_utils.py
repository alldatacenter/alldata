# TODO with, without optional args
# TODO test with no data files exception and unsupported format exception
from pathlib import Path
from typing import Type
from unittest.mock import MagicMock

import pandas as pd
import pytest
from pytest_mock import MockerFixture
from pyspark.sql import DataFrame, SparkSession

from feathr import FeathrClient
from feathr.constants import OUTPUT_FORMAT, OUTPUT_PATH_TAG
from feathr.utils.job_utils import (
    get_result_df,
    get_result_pandas_df,
    get_result_spark_df,
)


def test__get_result_pandas_df(mocker: MockerFixture):
    """Test if the base function, get_result_df, called w/ proper args"""
    mocked_get_result_df = mocker.patch("feathr.utils.job_utils.get_result_df")
    client = MagicMock()
    data_format = "some_data_format"
    res_url = "some_res_url"
    local_cache_path = "some_local_cache_path"
    get_result_pandas_df(client, data_format, res_url, local_cache_path)
    mocked_get_result_df.assert_called_once_with(
        client=client,
        data_format=data_format,
        res_url=res_url,
        local_cache_path=local_cache_path,
    )


def test__get_result_spark_df(mocker: MockerFixture):
    """Test if the base function, get_result_df, called w/ proper args"""
    mocked_get_result_df = mocker.patch("feathr.utils.job_utils.get_result_df")
    client = MagicMock()
    spark = MagicMock()
    data_format = "some_data_format"
    res_url = "some_res_url"
    local_cache_path = "some_local_cache_path"
    get_result_spark_df(spark, client, data_format, res_url, local_cache_path)
    mocked_get_result_df.assert_called_once_with(
        client=client,
        data_format=data_format,
        res_url=res_url,
        local_cache_path=local_cache_path,
        spark=spark,
    )


@pytest.mark.parametrize(
    "is_databricks,spark_runtime,res_url,local_cache_path,expected_local_cache_path", [
        # For local spark results, res_url must be a local path and local_cache_path will be ignored.
        (False, "local", "some_res_url", None, "some_res_url"),
        (False, "local", "some_res_url", "some_local_cache_path", "some_res_url"),
        # For databricks results, res_url must be a dbfs path.
        # If the function is called in databricks, local_cache_path will be ignored.
        (True, "databricks", "dbfs:/some_res_url", None, "/dbfs/some_res_url"),
        (True, "databricks", "dbfs:/some_res_url", "some_local_cache_path", "/dbfs/some_res_url"),
        (False, "databricks", "dbfs:/some_res_url", None, "mocked_temp_path"),
        (False, "databricks", "dbfs:/some_res_url", "some_local_cache_path", "some_local_cache_path"),
    ]
)
def test__get_result_df__with_local_cache_path(
    mocker: MockerFixture,
    is_databricks: bool,
    spark_runtime: str,
    res_url: str,
    local_cache_path: str,
    expected_local_cache_path: str,
):
    """Test local_cache_path is used if provided"""
    # Mock client
    client = MagicMock()
    client.spark_runtime = spark_runtime
    client.feathr_spark_launcher.download_result = MagicMock()
    mocked_load_files_to_pandas_df = mocker.patch("feathr.utils.job_utils._load_files_to_pandas_df")

    # Mock is_databricks
    mocker.patch("feathr.utils.job_utils.is_databricks", return_value=is_databricks)

    # Mock temporary file module
    mocked_named_temporary_dir = MagicMock()
    mocked_named_temporary_dir.name = expected_local_cache_path
    mocker.patch("feathr.utils.job_utils.TemporaryDirectory", return_value=mocked_named_temporary_dir)

    data_format = "csv"
    get_result_df(client, data_format=data_format, res_url=res_url, local_cache_path=local_cache_path)

    mocked_load_files_to_pandas_df.assert_called_once_with(
        dir_path=expected_local_cache_path,
        data_format=data_format,
    )


@pytest.mark.parametrize(
    "is_databricks,spark_runtime,res_url,data_format,expected_error", [
        # Test RuntimeError when the function is running at Databricks but client.spark_runtime is not databricks
        (True, "local", "some_url", "some_format", RuntimeError),
        (True, "azure_synapse", "some_url", "some_format", RuntimeError),
        (True, "databricks", "some_url", "some_format", None),
        (False, "local", "some_url", "some_format", None),
        (False, "azure_synapse", "some_url", "some_format", None),
        (False, "databricks", "some_url", "some_format", None),
        # Test ValueError when res_url is None
        (True, "databricks", None, "some_format", ValueError),
        (False, "local", None, "some_format", ValueError),
        (False, "azure_synapse", None, "some_format", ValueError),
        (False, "databricks", None, "some_format", ValueError),
        # Test ValueError when data_format is None
        (True, "databricks", "some_url", None, ValueError),
        (False, "local", "some_url", None, ValueError),
        (False, "azure_synapse", "some_url", None, ValueError),
        (False, "databricks", "some_url", None, ValueError),
    ]
)
def test__get_result_df__exceptions(
    mocker: MockerFixture,
    is_databricks: bool,
    spark_runtime: str,
    res_url: str,
    data_format: str,
    expected_error: Type[Exception],
):
    """Test exceptions"""

    # Mock is_data_bricks
    mocker.patch("feathr.utils.job_utils.is_databricks", return_value=is_databricks)

    # Mock _load_files_to_pandas_df
    mocker.patch("feathr.utils.job_utils._load_files_to_pandas_df")

    # Either job tags or argument should yield the same result
    for job_tag in [None, {OUTPUT_FORMAT: data_format, OUTPUT_PATH_TAG: res_url}]:
        # Mock client
        client = MagicMock()
        client.get_job_result_uri = MagicMock(return_value=res_url)
        client.get_job_tags = MagicMock(return_value=job_tag)
        client.spark_runtime = spark_runtime

        if expected_error is None:
            get_result_df(
                client=client,
                res_url=None if job_tag else res_url,
                data_format=None if job_tag else data_format,
            )
        else:
            with pytest.raises(expected_error):
                get_result_df(
                    client=client,
                    res_url=None if job_tag else res_url,
                    data_format=None if job_tag else data_format,
                )


@pytest.mark.parametrize(
    "data_format,output_filename,expected_count", [
        ("csv", "output.csv", 5),
        ("csv", "output_dir.csv", 4),  # TODO add a header to the csv file and change expected_count to 5 after fixing the bug https://github.com/feathr-ai/feathr/issues/811
        ("parquet", "output.parquet", 5),
        ("avro", "output.avro", 5),
        ("delta", "output-delta", 5),
    ]
)
def test__get_result_df(
    workspace_dir: str,
    data_format: str,
    output_filename: str,
    expected_count: int,
):
    """Test get_result_df returns pandas DataFrame"""
    for spark_runtime in ["local", "databricks", "azure_synapse"]:
        # Note: make sure the output file exists in the test_user_workspace
        res_url = str(Path(workspace_dir, "mock_results", output_filename))
        local_cache_path = res_url

        # Mock client
        client = MagicMock()
        client.spark_runtime = spark_runtime

        # Mock feathr_spark_launcher.download_result
        if client.spark_runtime == "databricks":
            res_url = f"dbfs:/{res_url}"
        if client.spark_runtime == "azure_synapse" and data_format == "delta":
            # TODO currently pass the delta table test on Synapse result due to the delta table package bug.
            continue

        df = get_result_df(
            client=client,
            data_format=data_format,
            res_url=res_url,
            local_cache_path=local_cache_path,
        )
        assert isinstance(df, pd.DataFrame)
        assert len(df) == expected_count


@pytest.mark.parametrize(
    "data_format,output_filename,expected_count", [
        ("csv", "output.csv", 5),
        ("csv", "output_dir.csv", 4),  # TODO add a header to the csv file and change expected_count = 5 after fixing the bug https://github.com/feathr-ai/feathr/issues/811
        ("parquet", "output.parquet", 5),
        ("avro", "output.avro", 5),
        ("delta", "output-delta", 5),
    ]
)
def test__get_result_df__with_spark_session(
    workspace_dir: str,
    spark: SparkSession,
    data_format: str,
    output_filename: str,
    expected_count: int,
):
    """Test get_result_df returns spark DataFrame"""
    for spark_runtime in ["local", "databricks", "azure_synapse"]:
        # Note: make sure the output file exists in the test_user_workspace
        res_url = str(Path(workspace_dir, "mock_results", output_filename))
        local_cache_path = res_url

        # Mock client
        client = MagicMock()
        client.spark_runtime = spark_runtime

        if client.spark_runtime == "databricks":
            res_url = f"dbfs:/{res_url}"

        df = get_result_df(
            client=client,
            data_format=data_format,
            res_url=res_url,
            spark=spark,
            local_cache_path=local_cache_path,
        )
        assert isinstance(df, DataFrame)
        assert df.count() == expected_count


@pytest.mark.parametrize(
    "format,output_filename,expected_count", [
        ("csv", "output.csv", 5),
    ]
)
def test__get_result_df__arg_alias(
    workspace_dir: str,
    format: str,
    output_filename: str,
    expected_count: int,
):
    """Test get_result_df returns pandas DataFrame with the argument alias `format` instead of using `data_format`"""
    for spark_runtime in ["local", "databricks", "azure_synapse"]:
        # Note: make sure the output file exists in the test_user_workspace
        res_url = str(Path(workspace_dir, "mock_results", output_filename))
        local_cache_path = res_url

        # Mock client
        client = MagicMock()
        client.spark_runtime = spark_runtime

        # Mock feathr_spark_launcher.download_result
        if client.spark_runtime == "databricks":
            res_url = f"dbfs:/{res_url}"
        if client.spark_runtime == "azure_synapse" and format == "delta":
            # TODO currently pass the delta table test on Synapse result due to the delta table package bug.
            continue

        df = get_result_df(
            client=client,
            format=format,
            res_url=res_url,
            local_cache_path=local_cache_path,
        )
        assert isinstance(df, pd.DataFrame)
        assert len(df) == expected_count
