from pathlib import Path
from tempfile import TemporaryDirectory
from threading import local
from urllib.parse import urlparse

import pandas as pd
from pyspark.sql import DataFrame, SparkSession

from feathr.datasets import NYC_TAXI_SMALL_URL
from feathr.datasets.utils import maybe_download
from feathr.utils.platform import is_databricks


def get_pandas_df(
    local_cache_path: str = None,
) -> pd.DataFrame:
    """Get NYC taxi fare prediction data samples as a pandas DataFrame.

    Refs:
        https://www1.nyc.gov/site/tlc/about/tlc-trip-record-data.page

    Args:
        local_cache_path (optional): Local cache file path to download the data set.
            If local_cache_path is a directory, the source file name will be added.

    Returns:
        pandas DataFrame
    """
    # if local_cache_path params is not provided then create a temporary folder
    if local_cache_path is None:
        local_cache_path = TemporaryDirectory().name

    # If local_cache_path is a directory, add the source file name.
    src_filepath = Path(urlparse(NYC_TAXI_SMALL_URL).path)
    dst_path = Path(local_cache_path)
    if dst_path.suffix != src_filepath.suffix:
        local_cache_path = str(dst_path.joinpath(src_filepath.name))

    maybe_download(src_url=NYC_TAXI_SMALL_URL, dst_filepath=local_cache_path)

    pdf = pd.read_csv(local_cache_path)

    return pdf


def get_spark_df(
    spark: SparkSession,
    local_cache_path: str,
) -> DataFrame:
    """Get NYC taxi fare prediction data samples as a spark DataFrame.

    Refs:
        https://www1.nyc.gov/site/tlc/about/tlc-trip-record-data.page

    Args:
        spark: Spark session.
        local_cache_path: Local cache file path to download the data set.
            If local_cache_path is a directory, the source file name will be added.

    Returns:
        Spark DataFrame
    """
    # In spark, local_cache_path should be a persist directory or file path
    if local_cache_path is None:
        raise ValueError("In spark, `local_cache_path` should be a persist directory or file path.")

    # If local_cache_path is a directory, add the source file name.
    src_filepath = Path(urlparse(NYC_TAXI_SMALL_URL).path)
    dst_path = Path(local_cache_path)
    if dst_path.suffix != src_filepath.suffix:
        local_cache_path = str(dst_path.joinpath(src_filepath.name))

    # Databricks uses "dbfs:/" prefix for spark paths and "/dbfs/" prefix for python paths
    if is_databricks():
        # If local_cache_path is a python path, convert it to a spark path.
        if local_cache_path.startswith("/dbfs/"):
            local_cache_path = local_cache_path.replace("/dbfs", "dbfs:", 1)
        # If local_cache_path is not a spark path, add the spark path prefix.
        elif not local_cache_path.startswith("dbfs:"):
            local_cache_path = f"dbfs:/{local_cache_path.lstrip('/')}"

        python_local_cache_path = local_cache_path.replace("dbfs:", "/dbfs", 1)
    # TODO add "if is_synapse()"
    else:
        python_local_cache_path = local_cache_path

    maybe_download(src_url=NYC_TAXI_SMALL_URL, dst_filepath=python_local_cache_path)

    df = spark.read.option("header", True).csv(local_cache_path)

    return df
