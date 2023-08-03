import os
from datetime import datetime, timedelta
from pathlib import Path
from feathr.client import FeathrClient
from feathr.utils.job_utils import get_result_df

from pyspark.sql import DataFrame
from pyspark.sql.functions import col

from feathr import BOOLEAN, FLOAT, INT32, ValueType
from feathr import (BackfillTime, MaterializationSettings, FeatureQuery,
                    ObservationSettings, SparkExecutionConfiguration, ConflictsAutoCorrection)
from feathr import Feature
from feathr import FeatureAnchor
from feathr import INPUT_CONTEXT, HdfsSource
from feathr import RedisSink, HdfsSink, AerospikeSink
from feathr import TypedKey
from feathr.definition._materialization_utils import _to_materialization_config
from test_fixture import basic_test_setup, conflicts_auto_correction_setup
from test_fixture import get_online_test_table_name
from test_utils.constants import Constants
from logging import raiseExceptions
import pytest

def test_feature_materialization_config():
    backfill_time = BackfillTime(start=datetime(2020, 5, 20), end=datetime(2020, 5,20), step=timedelta(days=1))
    redisSink = RedisSink(table_name="nycTaxiDemoFeature")
    settings = MaterializationSettings("nycTaxiTable",
                                        sinks=[redisSink],
                                        feature_names=["f_location_avg_fare", "f_location_max_fare"],
                                        backfill_time=backfill_time)
    config = _to_materialization_config(settings)
    expected_config = """ 
        operational: {
            name: nycTaxiTable
            endTime: "2020-05-20 00:00:00"
            endTimeFormat: "yyyy-MM-dd HH:mm:ss"
            resolution: DAILY
            output:[
                {
                    name: REDIS
                    params: {
                        table_name: "nycTaxiDemoFeature"
                        features: [f_location_avg_fare,f_location_max_fare]
                    }
                }
            ]
        }
        features: [f_location_avg_fare, f_location_max_fare]
        """
    assert ''.join(config.split()) == ''.join(expected_config.split())

def test_feature_materialization_offline_config():
    backfill_time = BackfillTime(start=datetime(2020, 5, 20), end=datetime(2020, 5,20), step=timedelta(days=1))
    offlineSink = HdfsSink(output_path="abfss://feathrazuretest3fs@feathrazuretest3storage.dfs.core.windows.net/demo_data/output/hdfs_test.avro")
    settings = MaterializationSettings("nycTaxiTable",
                                       sinks=[offlineSink],
                                       feature_names=["f_location_avg_fare", "f_location_max_fare"],
                                       backfill_time=backfill_time)
    config = _to_materialization_config(settings)
    expected_config = """ 
        operational: {
            name: nycTaxiTable
            endTime: "2020-05-20 00:00:00"
            endTimeFormat: "yyyy-MM-dd HH:mm:ss"
            resolution: DAILY
            enableIncremental = true
            output:[
                {
                    name: HDFS
                    outputFormat: RAW_DATA                   
                    params: {
                        path: "abfss://feathrazuretest3fs@feathrazuretest3storage.dfs.core.windows.net/demo_data/output/hdfs_test.avro"
                        features: [f_location_avg_fare,f_location_max_fare]
                        storeName: "df0"
                    }

                }
            ]
        }
        features: [f_location_avg_fare, f_location_max_fare]
        """
    assert ''.join(config.split()) == ''.join(expected_config.split())

def test_feature_materialization_aerospike_sink_config():
    as_sink = AerospikeSink(name="aerospike",seedhost="20.57.186.153", port=3000, namespace="test", setname="test")
    backfill_time = BackfillTime(start=datetime(
        2020, 5, 20), end=datetime(2020, 5, 20), step=timedelta(days=1))
    settings = MaterializationSettings("nycTaxiTable",
                                       sinks=[as_sink],
                                       feature_names=[
                                           "avgfare", "maxfare"],
                                       backfill_time=backfill_time)
    os.environ[f"aerospike_USER"] = "feathruser"
    os.environ[f"aerospike_PASSWORD"] = "feathrpwd"
    expected_config = """
    operational: {
            name: nycTaxiTable
            endTime: "2020-05-20 00:00:00"
            endTimeFormat: "yyyy-MM-dd HH:mm:ss"
            resolution: DAILY
            output:[
                {
                    "name": "HDFS",                 
                    "params": {
                        "aerospike__seedhost": "20.57.186.153",
                        "aerospike__port": "3000",
                        "aerospike__namespace": "test",
                        "aerospike__user": "${AEROSPIKE_USER}",
                        "aerospike__password": "${AEROSPIKE_PASSWORD}",
                        "aerospike__set": "test",
                        "type": "generic",
                        "format": "aerospike",
                        "mode": "APPEND"
                    }

                }
            ]
        }
        features: [avgfare, maxfare]
    """
    config = _to_materialization_config(settings)
    assert ''.join(config.split()) == ''.join(expected_config.split())
    
def test_feature_materialization_daily_schedule():
    """Test back fill cutoff time for a daily range"""
    backfill_time = BackfillTime(start=datetime(2022, 3, 1), end=datetime(2022, 3, 5), step=timedelta(days=1))
    settings = MaterializationSettings("", [], [], backfill_time)
    expected = [datetime(2022, 3, day) for day in range(1, 6)]
    assert settings.get_backfill_cutoff_time() == expected
    
    

def test_feature_materialization_hourly_schedule():
    """Test back fill cutoff time for a hourly range"""
    backfill_time = BackfillTime(start=datetime(2022, 3, 1, 1), end=datetime(2022, 3, 1, 5), step=timedelta(hours=1))
    settings = MaterializationSettings("", [], [], backfill_time)
    expected = [datetime(2022,3, 1, hour) for hour in range(1, 6)]
    assert settings.get_backfill_cutoff_time() == expected


def test_feature_materialization_now_schedule():
    """Test back fill cutoff time without backfill."""
    settings = MaterializationSettings("", [], [])
    date = settings.get_backfill_cutoff_time()[0]
    expected = datetime.now()
    assert expected.year == date.year
    assert expected.month == date.month
    assert expected.day == date.day

def test_build_feature_verbose():
    """
    Test verbose for pretty printing features
    """
    test_workspace_dir = Path(__file__).parent.resolve() / "test_user_workspace"

    client = basic_test_setup(os.path.join(test_workspace_dir, "feathr_config.yaml"))

    # An anchor feature
    features = [
        Feature(name="trip_distance", feature_type=FLOAT),
        Feature(name="f_is_long_trip_distance",
                feature_type=BOOLEAN,
                transform="cast_float(trip_distance)>30"),
        Feature(name="f_day_of_week",
                feature_type=INT32,
                transform="dayofweek(lpep_dropoff_datetime)")
    ]

    anchor = FeatureAnchor(name="request_features",
                           source=INPUT_CONTEXT,
                           features=features)

    # Check pretty print
    client.build_features(anchor_list=[anchor], verbose=True)

def test_get_offline_features_verbose():
    """
    Test verbose for pretty printing feature query
    """

    test_workspace_dir = Path(__file__).parent.resolve() / "test_user_workspace"

    client = basic_test_setup(os.path.join(test_workspace_dir, "feathr_config.yaml"))

    location_id = TypedKey(key_column="DOLocationID",
                            key_column_type=ValueType.INT32)

    feature_query = FeatureQuery(feature_list=["f_location_avg_fare"], key=location_id)

    settings = ObservationSettings(
        observation_path="wasbs://public@azurefeathrstorage.blob.core.windows.net/sample_data/green_tripdata_2020-04",
        event_timestamp_column="lpep_dropoff_datetime",
        timestamp_format="yyyy-MM-dd HH:mm:ss"
    )

    now = datetime.now()

    # set output folder based on different runtime
    if client.spark_runtime == 'databricks':
        output_path = ''.join(['dbfs:/feathrazure_cijob','_', str(now.minute), '_', str(now.second), ".parquet"])
    else:
        output_path = ''.join(['abfss://feathrazuretest3fs@feathrazuretest3storage.dfs.core.windows.net/demo_data/output','_', str(now.minute), '_', str(now.second), ".parquet"])

    # Check pretty print
    client.get_offline_features(
                                observation_settings=settings,
                                feature_query=feature_query,
                                output_path=output_path,
                                execution_configurations=SparkExecutionConfiguration({"spark.feathr.inputFormat": "parquet", "spark.feathr.outputFormat": "parquet"}),
                                verbose=True
                        )
    
def test_get_offline_features_auto_correct_dataset():
    test_workspace_dir = Path(__file__).parent.resolve() / "test_user_workspace"

    client = conflicts_auto_correction_setup(os.path.join(test_workspace_dir, "feathr_config.yaml"))
    
    now = datetime.now()
    location_id = TypedKey(key_column="DOLocationID",
                       key_column_type=ValueType.INT32,
                       description="location id in NYC",
                       full_name="nyc_taxi.location_id")
    feature_query = FeatureQuery(
    feature_list=["tip_amount", "total_amount"], key=location_id)
    settings = ObservationSettings(
        observation_path="wasbs://public@azurefeathrstorage.blob.core.windows.net/sample_data/green_tripdata_2020-04_with_index.csv",
        event_timestamp_column="lpep_dropoff_datetime",
        timestamp_format="yyyy-MM-dd HH:mm:ss",
        conflicts_auto_correction=ConflictsAutoCorrection(rename_features=False, suffix="test"))
     # set output folder based on different runtime
    if client.spark_runtime == 'databricks':
        output_path = ''.join(['dbfs:/feathrazure_cijob','_', str(now.minute), '_', str(now.second), ".avro"])
    else:
        output_path = ''.join(['abfss://feathrazuretest3fs@feathrazuretest3storage.dfs.core.windows.net/demo_data/output','_', str(now.minute), '_', str(now.second), ".avro"])
    client.get_offline_features(observation_settings=settings,
                            feature_query=feature_query,
                            output_path=output_path
                           )
    client.wait_job_to_finish(timeout_sec=500)

    res_df = get_result_df(client, data_format="avro", res_url = output_path)
    assert res_df.shape[0] > 0
    
def test_get_offline_features_auto_correct_features():
    test_workspace_dir = Path(__file__).parent.resolve() / "test_user_workspace"

    client = conflicts_auto_correction_setup(os.path.join(test_workspace_dir, "feathr_config.yaml"))
    
    now = datetime.now()
    location_id = TypedKey(key_column="DOLocationID",
                       key_column_type=ValueType.INT32,
                       description="location id in NYC",
                       full_name="nyc_taxi.location_id")
    feature_query = FeatureQuery(
    feature_list=["tip_amount", "total_amount"], key=location_id)
    settings = ObservationSettings(
        observation_path="wasbs://public@azurefeathrstorage.blob.core.windows.net/sample_data/green_tripdata_2020-04_with_index.csv",
        event_timestamp_column="lpep_dropoff_datetime",
        timestamp_format="yyyy-MM-dd HH:mm:ss",
        conflicts_auto_correction=ConflictsAutoCorrection(rename_features=True, suffix="test"))
    # set output folder based on different runtime
    if client.spark_runtime == 'databricks':
        output_path = ''.join(['dbfs:/feathrazure_cijob','_', str(now.minute), '_', str(now.second), ".avro"])
    else:
        output_path = ''.join(['abfss://feathrazuretest3fs@feathrazuretest3storage.dfs.core.windows.net/demo_data/output','_', str(now.minute), '_', str(now.second), ".avro"])
   
    client.get_offline_features(observation_settings=settings,
                            feature_query=feature_query,
                            output_path=output_path
                           )
    client.wait_job_to_finish(timeout_sec=500)

    res_df = get_result_df(client, data_format="avro", res_url = output_path)
    assert res_df.shape[0] > 0

def test_materialize_features_verbose():
    online_test_table = get_online_test_table_name("nycTaxiCITableMaterializeVerbose")
    test_workspace_dir = Path(__file__).parent.resolve() / "test_user_workspace"

    client: FeathrClient = basic_test_setup(os.path.join(test_workspace_dir, "feathr_config.yaml"))
    backfill_time = BackfillTime(start=datetime(2020, 5, 20), end=datetime(2020, 5, 20), step=timedelta(days=1))
    redisSink = RedisSink(table_name=online_test_table)
    settings = MaterializationSettings("nycTaxiTable",
                                       sinks=[redisSink],
                                       feature_names=[
                                           "f_location_avg_fare", "f_location_max_fare"],
                                       backfill_time=backfill_time)
    client.materialize_features(settings, verbose=True)
    client._clean_test_data(online_test_table)

def add_new_fare_amount(df: DataFrame) -> DataFrame:
    df = df.withColumn("fare_amount_new", col("fare_amount") + 8000000)

    return df

def test_delete_feature_from_redis():
    """
    Test FeathrClient() delete_feature_from_redis to remove feature from Redis.
    """

    test_workspace_dir = Path(__file__).parent.resolve() / "test_user_workspace"

    client: FeathrClient = basic_test_setup(os.path.join(test_workspace_dir, "feathr_config.yaml"))

    batch_source = HdfsSource(name="nycTaxiBatchSource_add_new_fare_amount",
                              path="wasbs://public@azurefeathrstorage.blob.core.windows.net/sample_data/green_tripdata_2020-04.csv",
                              preprocessing=add_new_fare_amount,
                              event_timestamp_column="lpep_dropoff_datetime",
                              timestamp_format="yyyy-MM-dd HH:mm:ss")

    pickup_time_as_id = TypedKey(key_column="lpep_pickup_datetime",
                                 key_column_type=ValueType.INT32,
                                 description="location id in NYC",
                                 full_name="nyc_taxi.location_id")

    features = [
        Feature(name="f_is_long_trip_distance",
                key=pickup_time_as_id,
                feature_type=FLOAT,
                transform="fare_amount_new"),
        Feature(name="f_day_of_week",
                key=pickup_time_as_id,
                feature_type=INT32,
                transform="dayofweek(lpep_dropoff_datetime)"),
    ]

    regular_anchor = FeatureAnchor(name="request_features_add_new_fare_amount",
                                   source=batch_source,
                                   features=features,
                                   )

    client.build_features(anchor_list=[regular_anchor])

    online_test_table = get_online_test_table_name('nycTaxiCITableDeletion')

    backfill_time = BackfillTime(start=datetime(
        2020, 5, 20), end=datetime(2020, 5, 20), step=timedelta(days=1))
    redisSink = RedisSink(table_name=online_test_table)
    settings = MaterializationSettings(name="py_udf",
                                       sinks=[redisSink],
                                       feature_names=[
                                           "f_is_long_trip_distance",
                                           "f_day_of_week"
                                       ],
                                       backfill_time=backfill_time)
    client.materialize_features(settings, allow_materialize_non_agg_feature=True)
    
    client.wait_job_to_finish(timeout_sec=Constants.SPARK_JOB_TIMEOUT_SECONDS)
    
    res = client.get_online_features(online_test_table, '2020-04-01 07:21:51', [
        'f_is_long_trip_distance', 'f_day_of_week'])

    assert len(res) == 2

    assert res[0] != None
    assert res[1] != None

    # Delete online feature stored in Redis
    client.delete_feature_from_redis(online_test_table, '2020-04-01 07:21:51', 'f_is_long_trip_distance')
    
    # Check if the online feature is deleted successfully
    res = client.get_online_features(online_test_table, '265', ['f_location_avg_fare'])

    assert len(res) == 1
    assert res[0] == None
    
    client._clean_test_data(online_test_table)

def test_feature_list_on_input_context():
    with pytest.raises(RuntimeError) as e_info:
        test_workspace_dir = Path(__file__).parent.resolve() / "test_user_workspace"

        client: FeathrClient = basic_test_setup(os.path.join(test_workspace_dir, "feathr_config.yaml"))
        online_test_table = get_online_test_table_name('nycTaxiCITableDeletion')
        redisSink = RedisSink(table_name=online_test_table)
        settings = MaterializationSettings(name="py_udf",
                                       sinks=[redisSink],
                                       feature_names=[
                                           "f_location_avg_fare",
                                           "f_day_of_week"
                                       ])
        client.materialize_features(settings, allow_materialize_non_agg_feature=True)
    assert e_info is not None
    assert e_info.value.args[0] == "Materializing features that are defined on INPUT_CONTEXT is not supported. f_day_of_week is defined on INPUT_CONTEXT so you should remove it from the feature list in MaterializationSettings."  