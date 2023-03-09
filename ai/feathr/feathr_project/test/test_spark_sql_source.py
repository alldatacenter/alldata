import os
from datetime import datetime, timedelta
from pathlib import Path
from feathr import (BOOLEAN, FLOAT, INPUT_CONTEXT, INT32,
                    DerivedFeature, Feature, FeatureAnchor,
                    TypedKey, ValueType, WindowAggTransformation)

import pytest
from feathr import FeathrClient
from feathr import FeatureQuery
from feathr import ObservationSettings
from feathr import TypedKey
from feathr import ValueType
from feathr.definition.materialization_settings import BackfillTime, MaterializationSettings
from feathr.definition.sink import HdfsSink
from feathr.utils.job_utils import get_result_df
from test_utils.constants import Constants
from feathr.definition.source import SparkSqlSource


def test_feathr_spark_sql_query_source():
    test_workspace_dir = Path(
        __file__).parent.resolve() / "test_user_workspace"
    config_path = os.path.join(test_workspace_dir, "feathr_config.yaml")

    _get_offline_features(config_path, _sql_query_source())
    _get_offline_features(config_path, _sql_table_source())
    _materialize_to_offline(config_path, _sql_query_source())


def _get_offline_features(config_path: str, sql_source: SparkSqlSource):
    client: FeathrClient = _spark_sql_test_setup(config_path, sql_source)
    location_id = TypedKey(key_column="DOLocationID",
                           key_column_type=ValueType.INT32,
                           description="location id in NYC",
                           full_name="nyc_taxi.location_id")

    feature_query = FeatureQuery(
        feature_list=["f_location_avg_fare"], key=location_id)
    settings = ObservationSettings(
        observation_path="wasbs://public@azurefeathrstorage.blob.core.windows.net/sample_data/green_tripdata_2020-04.csv",
        event_timestamp_column="lpep_dropoff_datetime",
        timestamp_format="yyyy-MM-dd HH:mm:ss")

    now = datetime.now()
    if client.spark_runtime == 'databricks':
        output_path = ''.join(['dbfs:/feathrazure_cijob_materialize_offline_','_', str(now.minute), '_', str(now.second), ""])
    else:
        output_path = ''.join(['abfss://feathrazuretest3fs@feathrazuretest3storage.dfs.core.windows.net/demo_data/feathrazure_cijob_materialize_offline_','_', str(now.minute), '_', str(now.second), ""])
    client.get_offline_features(observation_settings=settings,
                                feature_query=feature_query,
                                output_path=output_path)

    # assuming the job can successfully run; otherwise it will throw exception
    client.wait_job_to_finish(timeout_sec=Constants.SPARK_JOB_TIMEOUT_SECONDS)
    return


def _materialize_to_offline(config_path: str, sql_source: SparkSqlSource):
    client: FeathrClient = _spark_sql_test_setup(config_path, sql_source)
    backfill_time = BackfillTime(start=datetime(
        2020, 5, 20), end=datetime(2020, 5, 20), step=timedelta(days=1))

    now = datetime.now()
    if client.spark_runtime == 'databricks':
        output_path = ''.join(['dbfs:/feathrazure_cijob_materialize_offline_sparksql',
                              '_', str(now.minute), '_', str(now.second), ""])
    else:
        output_path = ''.join(['abfss://feathrazuretest3fs@feathrazuretest3storage.dfs.core.windows.net/demo_data/feathrazure_cijob_materialize_offline_sparksql',
                              '_', str(now.minute), '_', str(now.second), ""])
    offline_sink = HdfsSink(output_path=output_path)
    settings = MaterializationSettings("nycTaxiTable",
                                       sinks=[offline_sink],
                                       feature_names=[
                                           "f_location_avg_fare", "f_location_max_fare"],
                                       backfill_time=backfill_time)
    client.materialize_features(settings)
    # assuming the job can successfully run; otherwise it will throw exception
    client.wait_job_to_finish(timeout_sec=Constants.SPARK_JOB_TIMEOUT_SECONDS)

    # download result and just assert the returned result is not empty
    # by default, it will write to a folder appended with date
    res_df = get_result_df(
        client, "avro", output_path + "/df0/daily/2020/05/20")
    assert res_df.shape[0] > 0


def _spark_sql_test_setup(config_path: str, sql_source: SparkSqlSource):
    client = FeathrClient(config_path=config_path)

    f_trip_distance = Feature(name="f_trip_distance",
                              feature_type=FLOAT, transform="trip_distance")
    f_trip_time_duration = Feature(name="f_trip_time_duration",
                                   feature_type=INT32,
                                   transform="(to_unix_timestamp(lpep_dropoff_datetime) - to_unix_timestamp(lpep_pickup_datetime))/60")

    features = [
        f_trip_distance,
        f_trip_time_duration,
        Feature(name="f_is_long_trip_distance",
                feature_type=BOOLEAN,
                transform="cast_float(trip_distance)>30"),
        Feature(name="f_day_of_week",
                feature_type=INT32,
                transform="dayofweek(lpep_dropoff_datetime)"),
    ]

    request_anchor = FeatureAnchor(name="request_features",
                                   source=INPUT_CONTEXT,
                                   features=features)

    f_trip_time_distance = DerivedFeature(name="f_trip_time_distance",
                                          feature_type=FLOAT,
                                          input_features=[
                                              f_trip_distance, f_trip_time_duration],
                                          transform="f_trip_distance * f_trip_time_duration")

    f_trip_time_rounded = DerivedFeature(name="f_trip_time_rounded",
                                         feature_type=INT32,
                                         input_features=[f_trip_time_duration],
                                         transform="f_trip_time_duration % 10")

    location_id = TypedKey(key_column="DOLocationID",
                           key_column_type=ValueType.INT32,
                           description="location id in NYC",
                           full_name="nyc_taxi.location_id")
    agg_features = [Feature(name="f_location_avg_fare",
                            key=location_id,
                            feature_type=FLOAT,
                            transform=WindowAggTransformation(agg_expr="cast_float(fare_amount)",
                                                              agg_func="AVG",
                                                              window="90d",
                                                              filter="fare_amount > 0"
                                                              )),
                    Feature(name="f_location_max_fare",
                            key=location_id,
                            feature_type=FLOAT,
                            transform=WindowAggTransformation(agg_expr="cast_float(fare_amount)",
                                                              agg_func="MAX",
                                                              window="90d"))
                    ]

    agg_anchor = FeatureAnchor(name="aggregationFeatures",
                               source=sql_source,
                               features=agg_features)

    client.build_features(anchor_list=[agg_anchor, request_anchor], derived_feature_list=[
        f_trip_time_distance, f_trip_time_rounded])

    return client


def _sql_query_source():
    return SparkSqlSource(name="sparkSqlQuerySource", sql="SELECT * FROM green_tripdata_2020_04_with_index", event_timestamp_column="lpep_dropoff_datetime", timestamp_format="yyyy-MM-dd HH:mm:ss")


def _sql_table_source():
    return SparkSqlSource(name="sparkSqlTableSource", table="green_tripdata_2020_04_with_index", event_timestamp_column="lpep_dropoff_datetime", timestamp_format="yyyy-MM-dd HH:mm:ss")
