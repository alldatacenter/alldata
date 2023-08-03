import os
from datetime import datetime, timedelta
from pathlib import Path
from feathr.client import FeathrClient

from pyspark.sql import DataFrame
from pyspark.sql.functions import col

from feathr import (BackfillTime, MaterializationSettings)
from feathr import Feature
from feathr import FeatureAnchor
from feathr import FeatureQuery
from feathr import HdfsSource, SnowflakeSource
from feathr import ObservationSettings
from feathr import RedisSink
from feathr import STRING, FLOAT, INT32, ValueType
from feathr import TypedKey
from feathr import WindowAggTransformation
from feathr.utils.job_utils import get_result_df
from test_fixture import (snowflake_test_setup, get_online_test_table_name, basic_test_setup)
from test_utils.constants import Constants


def trip_distance_preprocessing(df: DataFrame):
    df = df.withColumn("trip_distance", df.trip_distance.cast('double') - 90000)
    df = df.withColumn("fare_amount", df.fare_amount.cast('double') - 90000)

    return df

def add_new_dropoff_and_fare_amount_column(df: DataFrame):
    df = df.withColumn("new_lpep_dropoff_datetime", col("lpep_dropoff_datetime"))
    df = df.withColumn("new_fare_amount", col("fare_amount") + 1000000)
    return df

def add_new_fare_amount(df: DataFrame) -> DataFrame:
    df = df.withColumn("fare_amount_new", col("fare_amount") + 8000000)

    return df

def add_new_surcharge_amount_and_pickup_column(df: DataFrame) -> DataFrame:
    df = df.withColumn("new_improvement_surcharge", col("improvement_surcharge") + 1000000)
    df = df.withColumn("new_tip_amount", col("tip_amount") + 1000000)
    df = df.withColumn("new_lpep_pickup_datetime", col("lpep_pickup_datetime"))

    return df

def add_old_lpep_dropoff_datetime(df: DataFrame) -> DataFrame:
    df = df.withColumn("old_lpep_dropoff_datetime", col("lpep_dropoff_datetime"))

    return df

def feathr_udf_day_calc(df: DataFrame) -> DataFrame:
    df = df.withColumn("f_day_of_week", dayofweek("lpep_dropoff_datetime"))
    df = df.withColumn("f_day_of_year", dayofyear("lpep_dropoff_datetime"))
    return df

def test_non_swa_feature_gen_with_offline_preprocessing():
    """
    Test non-SWA feature gen with preprocessing
    """
    test_workspace_dir = Path(__file__).parent.resolve() / "test_user_workspace"

    client:FeathrClient = basic_test_setup(os.path.join(test_workspace_dir, "feathr_config.yaml"))

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

    online_test_table = get_online_test_table_name('nycTaxiCITableOfflineProcessing')

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
    # just assume the job is successful without validating the actual result in Redis. Might need to consolidate
    # this part with the test_feathr_online_store test case
    client.wait_job_to_finish(timeout_sec=Constants.SPARK_JOB_TIMEOUT_SECONDS)

    res = client.get_online_features(online_test_table, '2020-04-01 07:21:51', [
        'f_is_long_trip_distance', 'f_day_of_week'])
    assert res == [8000006.0, 4]
    
    client._clean_test_data(online_test_table)


def test_feature_swa_feature_gen_with_preprocessing():
    """
    Test SWA feature gen with preprocessing.
    """
    test_workspace_dir = Path(__file__).parent.resolve() / "test_user_workspace"
    client = basic_test_setup(os.path.join(test_workspace_dir, "feathr_config.yaml"))

    batch_source = HdfsSource(name="nycTaxiBatchSource",
                              path="wasbs://public@azurefeathrstorage.blob.core.windows.net/sample_data/green_tripdata_2020-04.csv",
                              preprocessing=add_new_dropoff_and_fare_amount_column,
                              event_timestamp_column="new_lpep_dropoff_datetime",
                              timestamp_format="yyyy-MM-dd HH:mm:ss")

    location_id = TypedKey(key_column="DOLocationID",
                           key_column_type=ValueType.INT32,
                           description="location id in NYC",
                           full_name="nyc_taxi.location_id")


    agg_features = [Feature(name="f_location_avg_fare",
                            key=location_id,
                            feature_type=FLOAT,
                            transform=WindowAggTransformation(agg_expr="new_fare_amount",
                                                              agg_func="AVG",
                                                              window="90d")),
                    Feature(name="f_location_max_fare",
                            key=location_id,
                            feature_type=FLOAT,
                            transform=WindowAggTransformation(agg_expr="new_fare_amount",
                                                              agg_func="MAX",
                                                              window="90d"))
                    ]

    agg_anchor = FeatureAnchor(name="aggregationFeatures",
                               source=batch_source,
                               features=agg_features)

    client.build_features(anchor_list=[agg_anchor])

    online_test_table = get_online_test_table_name('nycTaxiCITableSWAFeatureMaterialization')

    backfill_time = BackfillTime(start=datetime(
        2020, 5, 20), end=datetime(2020, 5, 20), step=timedelta(days=1))
    redisSink = RedisSink(table_name=online_test_table)
    settings = MaterializationSettings(name="py_udf",
                                       sinks=[redisSink],
                                       feature_names=[
                                           "f_location_avg_fare",
                                           "f_location_max_fare",
                                       ],
                                       backfill_time=backfill_time)
    client.materialize_features(settings)
    # just assume the job is successful without validating the actual result in Redis. Might need to consolidate
    # this part with the test_feathr_online_store test case
    client.wait_job_to_finish(timeout_sec=Constants.SPARK_JOB_TIMEOUT_SECONDS)

    res = client.get_online_features(online_test_table, '265', ['f_location_avg_fare', 'f_location_max_fare'])
    assert res == [1000041.625, 1000100.0]
    
    client._clean_test_data(online_test_table)


def test_feathr_get_offline_features_hdfs_source():
    """
    Test get offline features for blob storage
    """
    test_workspace_dir = Path(__file__).parent.resolve() / "test_user_workspace"

    client = basic_test_setup(os.path.join(test_workspace_dir, "feathr_config.yaml"))

    batch_source1 = HdfsSource(name="nycTaxiBatchSource_add_new_dropoff_and_fare_amount_column",
                              path="wasbs://public@azurefeathrstorage.blob.core.windows.net/sample_data/green_tripdata_2020-04.csv",
                              preprocessing=add_new_dropoff_and_fare_amount_column,
                              event_timestamp_column="new_lpep_dropoff_datetime",
                              # event_timestamp_column="lpep_dropoff_datetime",
                              timestamp_format="yyyy-MM-dd HH:mm:ss")

    batch_source2 = HdfsSource(name="nycTaxiBatchSource_add_new_fare_amount",
                              path="wasbs://public@azurefeathrstorage.blob.core.windows.net/sample_data/green_tripdata_2020-04.csv",
                              preprocessing=add_new_fare_amount,
                              event_timestamp_column="lpep_dropoff_datetime",
                              timestamp_format="yyyy-MM-dd HH:mm:ss")


    location_id = TypedKey(key_column="DOLocationID",
                           key_column_type=ValueType.INT32,
                           description="location id in NYC",
                           full_name="nyc_taxi.location_id")


    agg_features = [Feature(name="f_location_avg_fare",
                            key=location_id,
                            feature_type=FLOAT,
                            transform=WindowAggTransformation(agg_expr="new_fare_amount",
                                                              agg_func="SUM",
                                                              window="90d")),
                    Feature(name="f_location_max_fare",
                            key=location_id,
                            feature_type=FLOAT,
                            transform=WindowAggTransformation(agg_expr="new_fare_amount",
                                                              agg_func="MAX",
                                                              window="90d"))
                    ]

    agg_anchor = FeatureAnchor(name="aggregationFeatures",
                               source=batch_source1,
                               features=agg_features,
                               )

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

    regular_anchor = FeatureAnchor(name="regular_anchor",
                                   source=batch_source2,
                                   features=features,
                                   )

    client.build_features(anchor_list=[agg_anchor, regular_anchor])

    feature_query = [FeatureQuery(
        feature_list=["f_is_long_trip_distance", "f_day_of_week"], key=pickup_time_as_id),
        FeatureQuery(
            feature_list=["f_location_avg_fare", "f_location_max_fare"], key=location_id)
    ]

    settings = ObservationSettings(
        observation_path="wasbs://public@azurefeathrstorage.blob.core.windows.net/sample_data/green_tripdata_2020-04.csv",
        event_timestamp_column="lpep_dropoff_datetime",
        timestamp_format="yyyy-MM-dd HH:mm:ss")

    now = datetime.now()
    # set output folder based on different runtime
    if client.spark_runtime == 'databricks':
        output_path = ''.join(['dbfs:/feathrazure_cijob','_', str(now.minute), '_', str(now.second), ".avro"])
    else:
        output_path = ''.join(['abfss://feathrazuretest3fs@feathrazuretest3storage.dfs.core.windows.net/demo_data/output','_', str(now.minute), '_', str(now.second), ".avro"])


    client.get_offline_features(observation_settings=settings,
                                feature_query=feature_query,
                                output_path=output_path)

    # assuming the job can successfully run; otherwise it will throw exception
    client.wait_job_to_finish(timeout_sec=Constants.SPARK_JOB_TIMEOUT_SECONDS)

    # download result and just assert the returned result is not empty
    res_df = get_result_df(client)
    assert res_df.shape[0] > 0


def test_get_offline_feature_two_swa_with_diff_preprocessing():
    """
    Test get offline features for two SWA anchors with different preprocessing and different sources.
    """
    test_workspace_dir = Path(__file__).parent.resolve() / "test_user_workspace"

    client = basic_test_setup(os.path.join(test_workspace_dir, "feathr_config.yaml"))

    swa_source_1 = HdfsSource(name="nycTaxiBatchSource1",
                               path="wasbs://public@azurefeathrstorage.blob.core.windows.net/sample_data/green_tripdata_2020-04.csv",
                               preprocessing=add_new_dropoff_and_fare_amount_column,
                               event_timestamp_column="new_lpep_dropoff_datetime",
                               timestamp_format="yyyy-MM-dd HH:mm:ss")

    location_id = TypedKey(key_column="DOLocationID",
                           key_column_type=ValueType.INT32,
                           description="location id in NYC",
                           full_name="nyc_taxi.location_id")


    agg_features1 = [Feature(name="f_location_avg_fare",
                            key=location_id,
                            feature_type=FLOAT,
                            transform=WindowAggTransformation(agg_expr="new_fare_amount",
                                                              agg_func="SUM",
                                                              window="90d")),
                    Feature(name="f_location_max_fare",
                            key=location_id,
                            feature_type=FLOAT,
                            transform=WindowAggTransformation(agg_expr="new_fare_amount",
                                                              agg_func="MAX",
                                                              window="90d"))
                    ]

    agg_anchor1 = FeatureAnchor(name="aggregationFeatures1",
                               source=swa_source_1,
                               features=agg_features1,
                               )


    swa_source_2 = HdfsSource(name="nycTaxiBatchSource2",
                              path="wasbs://public@azurefeathrstorage.blob.core.windows.net/sample_data/green_tripdata_2020-04.csv",
                              preprocessing=add_new_surcharge_amount_and_pickup_column,
                              event_timestamp_column="new_lpep_pickup_datetime",
                              timestamp_format="yyyy-MM-dd HH:mm:ss")

    agg_features2 = [Feature(name="f_location_new_tip_amount",
                             key=location_id,
                             feature_type=FLOAT,
                             transform=WindowAggTransformation(agg_expr="new_tip_amount",
                                                               agg_func="SUM",
                                                               window="90d")),
                     Feature(name="f_location_max_improvement_surcharge",
                             key=location_id,
                             feature_type=FLOAT,
                             transform=WindowAggTransformation(agg_expr="new_improvement_surcharge",
                                                               agg_func="SUM",
                                                               window="90d"))
                     ]
    agg_anchor2 = FeatureAnchor(name="aggregationFeatures2",
                               source=swa_source_2,
                               features=agg_features2,
                               )

    swa_source_3 = HdfsSource(name="nycTaxiBatchSource3",
                              path="wasbs://public@azurefeathrstorage.blob.core.windows.net/sample_data/green_tripdata_2020-04_old.csv",
                              preprocessing=add_old_lpep_dropoff_datetime,
                              event_timestamp_column="old_lpep_dropoff_datetime",
                              timestamp_format="yyyy-MM-dd HH:mm:ss")

    agg_features3 = [Feature(name="f_location_old_tip_amount",
                             key=location_id,
                             feature_type=FLOAT,
                             transform=WindowAggTransformation(agg_expr="cast_double(old_tip_amount)",
                                                               agg_func="SUM",
                                                               window="90d"))
                     ]
    agg_anchor3 = FeatureAnchor(name="aggregationFeatures3",
                                source=swa_source_3,
                                features=agg_features3,
                                )

    client.build_features(anchor_list=[agg_anchor1, agg_anchor2, agg_anchor3])

    feature_query = [
        FeatureQuery(feature_list=["f_location_new_tip_amount", "f_location_max_improvement_surcharge"], key=location_id),
        FeatureQuery(
            feature_list=["f_location_avg_fare", "f_location_max_fare"], key=location_id),
        FeatureQuery(
            feature_list=["f_location_old_tip_amount"], key=location_id)
    ]

    settings = ObservationSettings(
        observation_path="wasbs://public@azurefeathrstorage.blob.core.windows.net/sample_data/green_tripdata_2020-04.csv",
        event_timestamp_column="lpep_dropoff_datetime",
        timestamp_format="yyyy-MM-dd HH:mm:ss")

    now = datetime.now()
    # set output folder based on different runtime
    if client.spark_runtime == 'databricks':
        output_path = ''.join(['dbfs:/feathrazure_cijob','_', str(now.minute), '_', str(now.second), ".avro"])
    else:
        output_path = ''.join(['abfss://feathrazuretest3fs@feathrazuretest3storage.dfs.core.windows.net/demo_data/output','_', str(now.minute), '_', str(now.second), ".avro"])

    client.get_offline_features(observation_settings=settings,
                                feature_query=feature_query,
                                output_path=output_path)

    # assuming the job can successfully run; otherwise it will throw exception
    client.wait_job_to_finish(timeout_sec=Constants.SPARK_JOB_TIMEOUT_SECONDS)
    res_df = get_result_df(client)

    # download result and just assert the returned result is not empty
    assert res_df.shape[0] > 0


def snowflake_preprocessing(df: DataFrame) -> DataFrame:
    df = df.withColumn("NEW_CC_DIVISION_NAME", concat(col("CC_DIVISION_NAME"), lit("0000"), col("CC_DIVISION_NAME")))
    df = df.withColumn("NEW_CC_ZIP", concat(col("CC_ZIP"), lit("____"), col("CC_ZIP")))
    return df


def test_feathr_get_offline_features_from_snowflake():
    """
    Test get_offline_features() can get feature data from Snowflake source correctly.
    """
    test_workspace_dir = Path(__file__).parent.resolve() / "test_user_workspace"
    client = snowflake_test_setup(os.path.join(test_workspace_dir, "feathr_config.yaml"))
    batch_source = SnowflakeSource(name="nycTaxiBatchSource",
                                    database="SNOWFLAKE_SAMPLE_DATA",
                                    schema="TPCDS_SF10TCL",
                                    dbtable="CALL_CENTER",
                                    preprocessing=snowflake_preprocessing,
                                                            event_timestamp_column="lpep_dropoff_datetime",
                                                            timestamp_format="yyyy-MM-dd HH:mm:ss")
    call_sk_id = TypedKey(key_column="CC_CALL_CENTER_SK",
                          key_column_type=ValueType.STRING,
                          description="call center sk",
                          full_name="snowflake.CC_CALL_CENTER_SK")

    features = [
        Feature(name="f_snowflake_call_center_division_name_with_preprocessing",
                key=call_sk_id,
                feature_type=STRING,
                transform="NEW_CC_DIVISION_NAME"),
        Feature(name="f_snowflake_call_center_zipcode_with_preprocessing",
                key=call_sk_id,
                feature_type=STRING,
                transform="NEW_CC_ZIP"),
    ]

    feature_anchor = FeatureAnchor(name="snowflake_features",
                                   source=batch_source,
                                   features=features,
                                   )
    client.build_features(anchor_list=[feature_anchor])

    feature_query = FeatureQuery(
        feature_list=['f_snowflake_call_center_division_name_with_preprocessing', 'f_snowflake_call_center_zipcode_with_preprocessing'],
        key=call_sk_id)

    observation_path = client.get_snowflake_path(database="SNOWFLAKE_SAMPLE_DATA", schema="TPCDS_SF10TCL", dbtable="CALL_CENTER")
    settings = ObservationSettings(
        observation_path=observation_path)

    now = datetime.now()
    # set output folder based on different runtime
    if client.spark_runtime == 'databricks':
        output_path = ''.join(['dbfs:/feathrazure_cijob_snowflake', '_', str(now.minute), '_', str(now.second), ".avro"])
    else:
        output_path = ''.join(['abfss://feathrazuretest3fs@feathrazuretest3storage.dfs.core.windows.net/demo_data/snowflake_output','_', str(now.minute), '_', str(now.second), ".avro"])

    client.get_offline_features(observation_settings=settings,
                                feature_query=feature_query,
                                output_path=output_path)

    # assuming the job can successfully run; otherwise it will throw exception
    client.wait_job_to_finish(timeout_sec=Constants.SPARK_JOB_TIMEOUT_SECONDS)

    res = get_result_df(client)
    # just assume there are results.
    assert res.shape[0] > 1
