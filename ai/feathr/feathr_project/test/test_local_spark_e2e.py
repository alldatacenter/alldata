from feathr.definition.materialization_settings import BackfillTime, MaterializationSettings
from feathr.definition.sink import RedisSink
import pytest
import glob
import shutil
import pandas as pd
import pandavro as pdx
from pathlib import Path
import os
from datetime import datetime, timedelta
from feathr import FeathrClient, ObservationSettings, FeatureQuery, TypedKey, HdfsSource, Feature, FeatureAnchor, INPUT_CONTEXT, FLOAT, INT32, BOOLEAN, DerivedFeature, WindowAggTransformation, ValueType
from feathr import BOOLEAN, FLOAT, INT32, ValueType

from test_utils.udfs import (add_new_dropoff_and_fare_amount_column, add_new_fare_amount)


def test_local_spark_get_offline_features():
    #This Test is for Local Spark only
    if not _is_local():
        return
    
    test_workspace_dir = Path(
        __file__).parent.resolve() / "test_user_workspace"

    client = _local_client_setup(test_workspace_dir)


    output_path, proc1 = _non_udf_features(client)
    client.wait_job_to_finish()

    df = parse_avro_result(output_path)
    assert df.shape[0] > 0

    shutil.rmtree('debug')
    return 

def test_local_spark_pyudf_get_offline_features():
    #This Test is for Local Spark only
    if not _is_local():
        return
    client = _local_client_setup()
    
    output_path, proc = _udf_features(client)

    client.wait_job_to_finish()
    df = parse_avro_result(output_path)
    assert df.shape[0] > 0
    shutil.rmtree('debug')
    return 

def test_local_spark_materialization():
    #This Test is for Local Spark only
    if not _is_local():
        return

    client: FeathrClient = _local_client_setup()
    results = _feature_gen_test(client)
    # just assume the job is successful without validating the actual result in Redis.
    shutil.rmtree('debug')
    return

def _is_local() -> bool:
    """
    to verify if test is running on local spark mode
    """
    if os.environ.get('SPARK_CONFIG__SPARK_CLUSTER') == 'local':
        return True
    else:
        return False

def _local_client_setup(local_workspace:str = None):
    if not local_workspace:
        local_workspace = Path(
        __file__).parent.resolve() / "test_user_workspace"

    os.chdir(local_workspace)
    client = FeathrClient(os.path.join(local_workspace, "feathr_config_local.yaml"), local_workspace_dir=local_workspace)
    return client

def _non_udf_features(client:FeathrClient = None):
    if not client:
        client = _local_client_setup()
    batch_source = HdfsSource(name="nycTaxiBatchSource",
                              path="./green_tripdata_2020-04_with_index.csv",
                              event_timestamp_column="lpep_dropoff_datetime",
                              timestamp_format="yyyy-MM-dd HH:mm:ss")

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
                                                              )),
                    Feature(name="f_location_max_fare",
                            key=location_id,
                            feature_type=FLOAT,
                            transform=WindowAggTransformation(agg_expr="cast_float(fare_amount)",
                                                              agg_func="MAX",
                                                              window="90d"))
                    ]

    agg_anchor = FeatureAnchor(name="aggregationFeatures",
                               source=batch_source,
                               features=agg_features)

    client.build_features(anchor_list=[agg_anchor, request_anchor], derived_feature_list=[
        f_trip_time_distance, f_trip_time_rounded])

    location_id = TypedKey(key_column="DOLocationID",
                               key_column_type=ValueType.INT32,
                               description="location id in NYC",
                               full_name="nyc_taxi.location_id")

    feature_query = FeatureQuery(
            feature_list=["f_location_avg_fare"], key=location_id)
    
    settings = ObservationSettings(
        observation_path="./green_tripdata_2020-04_with_index.csv",
        #observation_path="wasbs://public@azurefeathrstorage.blob.core.windows.net/sample_data/green_tripdata_2020-04.csv",
        event_timestamp_column="lpep_dropoff_datetime",
        timestamp_format="yyyy-MM-dd HH:mm:ss")

    now = datetime.now().strftime("%Y%m%d%H%M%S")
    output_path = os.path.join("debug", f"test_output_{now}")
    proc = client.get_offline_features(observation_settings=settings,
                            feature_query=feature_query,
                            output_path=output_path,
                            config_file_name = "feature_join_conf/feature_join_local.conf",
                            verbose=False)
    return output_path, proc

def _udf_features(client:FeathrClient = None):
    if not client:
        client = _local_client_setup()
    
    batch_source1 = HdfsSource(name="nycTaxiBatchSource_add_new_dropoff_and_fare_amount_column",
                              path="./green_tripdata_2020-04_with_index.csv",
                              preprocessing=add_new_dropoff_and_fare_amount_column,
                              event_timestamp_column="new_lpep_dropoff_datetime",
                              # event_timestamp_column="lpep_dropoff_datetime",
                              timestamp_format="yyyy-MM-dd HH:mm:ss")

    batch_source2 = HdfsSource(name="nycTaxiBatchSource_add_new_fare_amount",
                              path="./green_tripdata_2020-04_with_index.csv",
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

    now = datetime.now().strftime("%Y%m%d%H%M%S")
    output_path = os.path.join("debug", f"test_output_{now}")
    proc = client.get_offline_features(observation_settings=settings,
                                feature_query=feature_query,
                                output_path=output_path)

    return output_path, proc

def _feature_gen_test(client:FeathrClient = None):
    if not client:
        client = _local_client_setup()
    batch_source = HdfsSource(name="nycTaxiBatchSource",
                              path="./green_tripdata_2020-04_with_index.csv",
                              event_timestamp_column="lpep_dropoff_datetime",
                              timestamp_format="yyyy-MM-dd HH:mm:ss")

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
                                                              )),
                    Feature(name="f_location_max_fare",
                            key=location_id,
                            feature_type=FLOAT,
                            transform=WindowAggTransformation(agg_expr="cast_float(fare_amount)",
                                                              agg_func="MAX",
                                                              window="90d"))
                    ]

    agg_anchor = FeatureAnchor(name="aggregationFeatures",
                               source=batch_source,
                               features=agg_features)

    client.build_features(anchor_list=[agg_anchor, request_anchor], derived_feature_list=[
        f_trip_time_distance, f_trip_time_rounded])
    
    online_test_table = "localSparkTest"

    backfill_time = BackfillTime(start=datetime(
        2020, 4, 1), end=datetime(2020, 4, 2), step=timedelta(days=1))
    redisSink = RedisSink(table_name=online_test_table)
    settings = MaterializationSettings("LocalSparkTest",
                                       sinks=[redisSink],
                                       feature_names=[
                                           "f_location_avg_fare", "f_location_max_fare"],
                                       backfill_time=backfill_time)
    results = client.materialize_features(settings)

    client.wait_job_to_finish()
    res = client.get_online_features(online_test_table, '243', [
                                     'f_location_avg_fare', 'f_location_max_fare'])
    return res

def parse_avro_result(output_path:str):
    dataframe_list = []
    # assuming the result are in avro format
    for file in glob.glob(os.path.join(output_path, '*.avro')):
        dataframe_list.append(pdx.read_avro(file))
    
    vertical_concat_df = pd.concat(dataframe_list, axis=0)
    return vertical_concat_df