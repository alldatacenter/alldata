from pathlib import Path
from unittest.mock import MagicMock

from pyspark.sql import SparkSession
import pytest
from pytest_mock import MockerFixture

from feathr.datasets import nyc_taxi


TEST_DATASET_DIR = Path(__file__).parent.parent.parent.joinpath("test_user_workspace")
NYC_TAXI_FILE_PATH = str(TEST_DATASET_DIR.joinpath("green_tripdata_2020-04_with_index.csv").resolve())


@pytest.mark.parametrize(
    "local_cache_path",
    [
        None,  # default temporary directory
        NYC_TAXI_FILE_PATH,  # full filepath
        str(Path(NYC_TAXI_FILE_PATH).parent),  # directory
    ],
)
def test__nyc_taxi__get_pandas_df(
    mocker: MockerFixture,
    local_cache_path: str,
):
    """Test if nyc_taxi.get_pandas_df returns pd.DataFrame. Also check if the proper modules are being called."""
    # Mock maybe_download and TempDirectory
    mocked_maybe_download = mocker.patch("feathr.datasets.nyc_taxi.maybe_download")
    mocked_tmpdir = MagicMock()
    mocked_tmpdir.name = NYC_TAXI_FILE_PATH
    mocked_TemporaryDirectory = mocker.patch("feathr.datasets.nyc_taxi.TemporaryDirectory", return_value=mocked_tmpdir)

    pdf = nyc_taxi.get_pandas_df(local_cache_path=local_cache_path)
    assert len(pdf) == 35612

    # Assert mock called
    if local_cache_path:
        mocked_TemporaryDirectory.assert_not_called()
    else:
        mocked_TemporaryDirectory.assert_called_once()

    # TODO check this is called w/ file extension added
    mocked_maybe_download.assert_called_once_with(src_url=nyc_taxi.NYC_TAXI_SMALL_URL, dst_filepath=NYC_TAXI_FILE_PATH)


@pytest.mark.parametrize(
    "local_cache_path", [
        NYC_TAXI_FILE_PATH,  # full filepath
        str(Path(NYC_TAXI_FILE_PATH).parent),  # directory
    ],
)
def test__nyc_taxi__get_spark_df(
    spark,
    mocker: MockerFixture,
    local_cache_path: str,
):
    """Test if nyc_taxi.get_spark_df returns spark.sql.DataFrame."""
    # Mock maybe_download
    mocked_maybe_download = mocker.patch("feathr.datasets.nyc_taxi.maybe_download")

    df = nyc_taxi.get_spark_df(spark=spark, local_cache_path=local_cache_path)
    assert df.count() == 35612

    mocked_maybe_download.assert_called_once_with(
        src_url=nyc_taxi.NYC_TAXI_SMALL_URL, dst_filepath=NYC_TAXI_FILE_PATH
    )


@pytest.mark.parametrize(
    "local_cache_path, expected_python_cache_path, expected_spark_cache_path", [
        # With file path
        ("test_dir/test.csv", "/dbfs/test_dir/test.csv", "dbfs:/test_dir/test.csv"),
        # With directory path
        ("test_dir", "/dbfs/test_dir/green_tripdata_2020-04_with_index.csv", "dbfs:/test_dir/green_tripdata_2020-04_with_index.csv"),
        # With databricks python file path
        ("/dbfs/test_dir/test.csv", "/dbfs/test_dir/test.csv", "dbfs:/test_dir/test.csv"),
        # With databricks python directory path
        ("/dbfs/test_dir", "/dbfs/test_dir/green_tripdata_2020-04_with_index.csv", "dbfs:/test_dir/green_tripdata_2020-04_with_index.csv"),
        # With databricks spark file path
        ("dbfs:/test_dir/test.csv", "/dbfs/test_dir/test.csv", "dbfs:/test_dir/test.csv"),
        # With databricks spark directory path
        ("dbfs:/test_dir", "/dbfs/test_dir/green_tripdata_2020-04_with_index.csv", "dbfs:/test_dir/green_tripdata_2020-04_with_index.csv"),
    ],
)
def test__nyc_taxi__get_spark_df__with_databricks(
    mocker: MockerFixture,
    local_cache_path: str,
    expected_python_cache_path: str,
    expected_spark_cache_path: str,
):
    # Mock maybe_download and spark session
    mocked_maybe_download = mocker.patch("feathr.datasets.nyc_taxi.maybe_download")
    mocked_is_databricks = mocker.patch("feathr.datasets.nyc_taxi.is_databricks", return_value=True)
    mocked_spark = MagicMock(spec=SparkSession)

    nyc_taxi.get_spark_df(spark=mocked_spark, local_cache_path=local_cache_path)

    # Assert mock called with databricks paths
    mocked_is_databricks.assert_called_once()

    mocked_maybe_download.assert_called_once_with(
        src_url=nyc_taxi.NYC_TAXI_SMALL_URL, dst_filepath=expected_python_cache_path
    )

    mocked_spark.read.option.return_value.csv.assert_called_once_with(expected_spark_cache_path)
