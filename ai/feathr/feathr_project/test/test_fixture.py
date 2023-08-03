from feathr import AvroJsonSchema
from feathr import KafKaSource
from feathr import KafkaConfig
from typing import List
import os
import random
from datetime import datetime, timedelta

from feathr import (BOOLEAN, FLOAT, INPUT_CONTEXT, INT32, STRING,
                    DerivedFeature, Feature, FeatureAnchor, HdfsSource,
                    TypedKey, ValueType, WindowAggTransformation, SnowflakeSource,
                    FeatureQuery,ObservationSettings)
from feathr import FeathrClient
from pyspark.sql import DataFrame


def basic_test_setup(config_path: str):

    now = datetime.now()
    # set workspace folder by time; make sure we don't have write conflict if there are many CI tests running
    os.environ['SPARK_CONFIG__DATABRICKS__WORK_DIR'] = ''.join(['dbfs:/feathrazure_cijob','_', str(now.minute), '_', str(now.second), '_', str(now.microsecond)]) 
    os.environ['SPARK_CONFIG__AZURE_SYNAPSE__WORKSPACE_DIR'] = ''.join(['abfss://feathrazuretest3fs@feathrazuretest3storage.dfs.core.windows.net/feathr_github_ci','_', str(now.minute), '_', str(now.second) ,'_', str(now.microsecond)]) 
    
    client = FeathrClient(config_path=config_path)
    batch_source = HdfsSource(name="nycTaxiBatchSource",
                              path="wasbs://public@azurefeathrstorage.blob.core.windows.net/sample_data/green_tripdata_2020-04.csv",
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
                               source=batch_source,
                               features=agg_features)

    client.build_features(anchor_list=[agg_anchor, request_anchor], derived_feature_list=[
        f_trip_time_distance, f_trip_time_rounded])

    return client

def conflicts_auto_correction_setup(config_path: str):
    now = datetime.now()
    # set workspace folder by time; make sure we don't have write conflict if there are many CI tests running
    os.environ['SPARK_CONFIG__DATABRICKS__WORK_DIR'] = ''.join(['dbfs:/feathrazure_cijob','_', str(now.minute), '_', str(now.second), '_', str(now.microsecond)]) 
    os.environ['SPARK_CONFIG__AZURE_SYNAPSE__WORKSPACE_DIR'] = ''.join(['abfss://feathrazuretest3fs@feathrazuretest3storage.dfs.core.windows.net/feathr_github_ci','_', str(now.minute), '_', str(now.second) ,'_', str(now.microsecond)]) 
    
    client = FeathrClient(config_path=config_path, local_workspace_dir="conflicts_test")
    batch_source = HdfsSource(name="nycTaxiBatchSource",
                          path="wasbs://public@azurefeathrstorage.blob.core.windows.net/sample_data/green_tripdata_2020-04_with_index.csv",
                          event_timestamp_column="lpep_dropoff_datetime",
                          timestamp_format="yyyy-MM-dd HH:mm:ss")
    location_id = TypedKey(key_column="DOLocationID",
                       key_column_type=ValueType.INT32,
                       description="location id in NYC",
                       full_name="nyc_taxi.location_id")
    pu_location_id = TypedKey(key_column="PULocationID",
                          key_column_type=ValueType.INT32,
                          description="location id in NYC",
                          full_name="nyc_taxi.location_id")

    agg_features = [Feature(name="tip_amount",
                        key=[location_id, pu_location_id],
                        feature_type=FLOAT,
                        transform=WindowAggTransformation(agg_expr="cast_float(fare_amount)",
                                                          agg_func="AVG",
                                                          window="3d")),
                Feature(name="total_amount",
                        key=[location_id, pu_location_id],
                        feature_type=FLOAT,
                        transform=WindowAggTransformation(agg_expr="cast_float(fare_amount)",
                                                          agg_func="MAX",
                                                          window="3d")),
                ]

    agg_anchor = FeatureAnchor(name="aggregationFeatures",
                           source=batch_source,
                           features=agg_features)
    
    client.build_features(anchor_list=[agg_anchor])

    return client

def composite_keys_test_setup(config_path: str):

    now = datetime.now()
    # set workspace folder by time; make sure we don't have write conflict if there are many CI tests running
    os.environ['SPARK_CONFIG__DATABRICKS__WORK_DIR'] = ''.join(['dbfs:/feathrazure_cijob','_', str(now.minute), '_', str(now.second), '_', str(now.microsecond)]) 
    os.environ['SPARK_CONFIG__AZURE_SYNAPSE__WORKSPACE_DIR'] = ''.join(['abfss://feathrazuretest3fs@feathrazuretest3storage.dfs.core.windows.net/feathr_github_ci','_', str(now.minute), '_', str(now.second) ,'_', str(now.microsecond)]) 
    
    client = FeathrClient(config_path=config_path)
    batch_source = HdfsSource(name="nycTaxiBatchSource",
                              path="wasbs://public@azurefeathrstorage.blob.core.windows.net/sample_data/green_tripdata_2020-04.csv",
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
    pu_location_id = TypedKey(key_column="PULocationID",
                          key_column_type=ValueType.INT32,
                          description="location id in NYC",
                          full_name="nyc_taxi.location_id")
    agg_features = [Feature(name="f_location_avg_fare",
                            key=[location_id,pu_location_id],
                            feature_type=FLOAT,
                            transform=WindowAggTransformation(agg_expr="cast_float(fare_amount)",
                                                              agg_func="AVG",
                                                              window="90d",
                                                              filter="fare_amount > 0"
                                                              )),
                    Feature(name="f_location_max_fare",
                            key=[location_id,pu_location_id],
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

    return client


def snowflake_test_setup(config_path: str):
    now = datetime.now()
    # set workspace folder by time; make sure we don't have write conflict if there are many CI tests running
    os.environ['SPARK_CONFIG__DATABRICKS__WORK_DIR'] = ''.join(['dbfs:/feathrazure_cijob_snowflake','_', str(now.minute), '_', str(now.second), '_', str(now.microsecond)]) 
    os.environ['SPARK_CONFIG__AZURE_SYNAPSE__WORKSPACE_DIR'] = ''.join(['abfss://feathrazuretest3fs@feathrazuretest3storage.dfs.core.windows.net/feathr_github_ci_snowflake','_', str(now.minute), '_', str(now.second), '_', str(now.microsecond)]) 

    client = FeathrClient(config_path=config_path)
    batch_source = SnowflakeSource(name="snowflakeSampleBatchSource",
                                   database="SNOWFLAKE_SAMPLE_DATA",
                                   schema="TPCDS_SF10TCL",
                                   dbtable="CALL_CENTER")
 
    call_sk_id = TypedKey(key_column="CC_CALL_CENTER_SK",
                          key_column_type=ValueType.INT32,
                          description="call center sk",
                          full_name="snowflake.CC_CALL_CENTER_SK")

    f_snowflake_call_center_division_name = Feature(name="f_snowflake_call_center_division_name",feature_type=STRING, transform="CC_DIVISION_NAME", key=call_sk_id)

    f_snowflake_call_center_zipcode = Feature(name="f_snowflake_call_center_zipcode",feature_type=STRING, transform="CC_ZIP", key=call_sk_id)



    features = [f_snowflake_call_center_division_name, f_snowflake_call_center_zipcode ]


    snowflakeFeatures = FeatureAnchor(name="snowflakeFeatures",
                                   source=batch_source,
                                   features=features)
    client.build_features(anchor_list=[snowflakeFeatures])
    return client

def kafka_test_setup(config_path: str):
    client = FeathrClient(config_path=config_path)
    schema = AvroJsonSchema(schemaStr="""
    {
        "type": "record",
        "name": "DriverTrips",
        "fields": [
            {"name": "driver_id", "type": "long"},
            {"name": "trips_today", "type": "int"},
            {
                "name": "datetime",
                "type": {"type": "long", "logicalType": "timestamp-micros"}
            }
        ]
    }
    """)
    stream_source = KafKaSource(name="kafkaStreamingSource",
                              kafkaConfig=KafkaConfig(brokers=["feathrazureci.servicebus.windows.net:9093"],
                                                      topics=["feathrcieventhub"],
                                                      schema=schema)
                              )

    driver_id = TypedKey(key_column="driver_id",
                          key_column_type=ValueType.INT64,
                          description="driver id",
                          full_name="nyc driver id")

    kafkaAnchor = FeatureAnchor(name="kafkaAnchor",
                                      source=stream_source,
                                      features=[Feature(name="f_modified_streaming_count",
                                                        feature_type=INT32,
                                                        transform="trips_today + 1",
                                                        key=driver_id),
                                                Feature(name="f_modified_streaming_count2",
                                                        feature_type=INT32,
                                                        transform="trips_today + randn() * cos(trips_today)", # make sure the supported SQL functions are tested
                                                        key=driver_id)]
                                      )
    client.build_features(anchor_list=[kafkaAnchor])
    return client

def registry_test_setup(config_path: str):
    # Use a new project name every time to make sure all features are registered correctly
    # Project name example: feathr_ci_registry_2022_09_24_01_02_30
    now = datetime.now()
    os.environ["project_config__project_name"] = f'feathr_ci_registry_{str(now)[:19].replace(" ", "_").replace(":", "_").replace("-", "_")}'

    client = FeathrClient(config_path=config_path, project_registry_tag={"for_test_purpose":"true"})
    request_anchor, agg_anchor, derived_feature_list = generate_entities()

    client.build_features(anchor_list=[agg_anchor, request_anchor], derived_feature_list=derived_feature_list)
    return client

def registry_test_setup_partially(config_path: str):
    """Register a partial of a project. Will call `generate_entities()` and register only the first anchor feature.
    """
    client = FeathrClient(config_path=config_path, project_registry_tag={"for_test_purpose":"true"})

    request_anchor, agg_anchor, derived_feature_list = generate_entities()
    agg_anchor.features = agg_anchor.features[:1]
    client.build_features(anchor_list=[agg_anchor, request_anchor], derived_feature_list=derived_feature_list)
    return client

def generate_entities():
    def add_new_dropoff_and_fare_amount_column(df: DataFrame):
        from pyspark.sql.functions import col
        df = df.withColumn("new_lpep_dropoff_datetime", col("lpep_dropoff_datetime"))
        df = df.withColumn("new_fare_amount", col("fare_amount") + 1000000)
        return df

    batch_source = HdfsSource(name="nycTaxiBatchSource",
                              path="wasbs://public@azurefeathrstorage.blob.core.windows.net/sample_data/green_tripdata_2020-04.csv",
                              event_timestamp_column="lpep_dropoff_datetime",
                              timestamp_format="yyyy-MM-dd HH:mm:ss",
                              preprocessing=add_new_dropoff_and_fare_amount_column,
                              registry_tags={"for_test_purpose":"true"}
                              )

    f_trip_distance = Feature(name="f_trip_distance",
                              feature_type=FLOAT, transform="trip_distance",
                              registry_tags={"for_test_purpose":"true"}
                              )
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
                                   features=features,
                                   registry_tags={"for_test_purpose":"true"}
                                   )

    f_trip_time_distance = DerivedFeature(name="f_trip_time_distance",
                                          feature_type=FLOAT,
                                          input_features=[
                                              f_trip_distance, f_trip_time_duration],
                                          transform="f_trip_distance * f_trip_time_duration")

    f_trip_time_rounded = DerivedFeature(name="f_trip_time_rounded",
                                         feature_type=INT32,
                                         input_features=[f_trip_time_duration],
                                         transform="f_trip_time_duration % 10")
    f_trip_time_rounded_plus = DerivedFeature(name="f_trip_time_rounded_plus",
                                         feature_type=INT32,
                                         input_features=[f_trip_time_rounded],
                                         transform="f_trip_time_rounded + 100")

    location_id = TypedKey(key_column="DOLocationID",
                           key_column_type=ValueType.INT32,
                           description="location id in NYC",
                           full_name="nyc_taxi.location_id")
    pu_location_id = TypedKey(key_column="PULocationID",
                              key_column_type=ValueType.INT32,
                           full_name="nyc_taxi.pu_location_id"
                       )
    agg_features = [Feature(name="f_location_avg_fare",
                            key=[location_id,pu_location_id],
                            feature_type=FLOAT,
                            transform=WindowAggTransformation(agg_expr="cast_float(fare_amount)",
                                                              agg_func="AVG",
                                                              window="90d"))
                    ]
    
    agg_anchor = FeatureAnchor(name="aggregationFeatures",
                               source=batch_source,
                               features=agg_features)
    
    derived_feature_list = [
                        f_trip_time_distance, f_trip_time_rounded, f_trip_time_rounded_plus]
    
    # shuffle the order to make sure they can be parsed correctly
    # Those input derived features can be in arbitrary order, but in order to parse the right dependencies, we need to reorder them internally in a certain order. 
    # This shuffle is to make sure that each time we have random shuffle for the input and make sure the internal sorting algorithm works (we are using topological sort).
    random.shuffle(derived_feature_list)
    return request_anchor,agg_anchor,derived_feature_list

def registry_test_setup_append(config_path: str):


    # use a new project name every time to make sure all features are registered correctly
    now = datetime.now()
    os.environ["project_config__project_name"] =  ''.join(['feathr_ci_registry','_', str(now.minute), '_', str(now.second), '_', str(now.microsecond)]) 

    client = FeathrClient(config_path=config_path, project_registry_tag={"for_test_purpose":"true"})

    def add_new_dropoff_and_fare_amount_column(df: DataFrame):
        from pyspark.sql.functions import col
        df = df.withColumn("new_lpep_dropoff_datetime", col("lpep_dropoff_datetime"))
        df = df.withColumn("new_fare_amount", col("fare_amount") + 1000000)
        return df

    batch_source = HdfsSource(name="nycTaxiBatchSource",
                              path="wasbs://public@azurefeathrstorage.blob.core.windows.net/sample_data/green_tripdata_2020-04.csv",
                              event_timestamp_column="lpep_dropoff_datetime",
                              timestamp_format="yyyy-MM-dd HH:mm:ss",
                              preprocessing=add_new_dropoff_and_fare_amount_column,
                              registry_tags={"for_test_purpose":"true"}
                              )

    f_trip_distance = Feature(name="f_trip_distance",
                              feature_type=FLOAT, transform="trip_distance",
                              registry_tags={"for_test_purpose":"true"}
                              )
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
                                   features=features,
                                   registry_tags={"for_test_purpose":"true"}
                                   )

    f_trip_time_distance = DerivedFeature(name="f_trip_time_distance",
                                          feature_type=FLOAT,
                                          input_features=[
                                              f_trip_distance, f_trip_time_duration],
                                          transform="f_trip_distance * f_trip_time_duration")

    f_trip_time_rounded = DerivedFeature(name="f_trip_time_rounded",
                                         feature_type=INT32,
                                         input_features=[f_trip_time_duration],
                                         transform="f_trip_time_duration % 10")
    f_trip_time_rounded_plus = DerivedFeature(name="f_trip_time_rounded_plus",
                                         feature_type=INT32,
                                         input_features=[f_trip_time_rounded],
                                         transform="f_trip_time_rounded + 100")

    location_id = TypedKey(key_column="DOLocationID",
                           key_column_type=ValueType.INT32,
                           description="location id in NYC",
                           full_name="nyc_taxi.location_id")
    agg_features = [Feature(name="f_location_avg_fare",
                            key=location_id,
                            feature_type=FLOAT,
                            transform=WindowAggTransformation(agg_expr="cast_float(fare_amount)",
                                                              agg_func="AVG",
                                                              window="90d")),
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
    
    derived_feature_list = [
                        f_trip_time_distance, f_trip_time_rounded, f_trip_time_rounded_plus]
    
    # shuffle the order to make sure they can be parsed correctly
    # Those input derived features can be in arbitrary order, but in order to parse the right dependencies, we need to reorder them internally in a certain order. 
    # This shuffle is to make sure that each time we have random shuffle for the input and make sure the internal sorting algorithm works (we are using topological sort).
    random.shuffle(derived_feature_list)
    client.build_features(anchor_list=[agg_anchor, request_anchor], derived_feature_list=derived_feature_list)
    return client

def registry_test_setup_for_409(config_path: str, project_name: str):
    now = datetime.now()
    os.environ["project_config__project_name"] =  project_name

    client = FeathrClient(config_path=config_path, project_registry_tag={"for_test_purpose":"true"})

    # tranform in other sample is cast_float(trip_distance)>30
    # update this to trigger 409 conflict with the existing one
    features = [
        Feature(name="f_is_long_trip_distance",
                feature_type=BOOLEAN,
                transform="cast_float(trip_distance)>10"),
    ]

    request_anchor = FeatureAnchor(name="request_features",
                                   source=INPUT_CONTEXT,
                                   features=features,
                                   registry_tags={"for_test_purpose":"true"}
                                   )

    client.build_features(anchor_list=[request_anchor])
    return client

def get_online_test_table_name(table_name: str):
    # use different time for testing to avoid write conflicts
    now = datetime.now()
    res_table = '_'.join([table_name, str(now.minute), str(now.second)])
    print("The online Redis table is", res_table)
    return res_table

def time_partition_pattern_feature_gen_test_setup(config_path: str, data_source_path: str, local_workspace_dir: str = None, resolution: str = 'DAILY', postfix_path: str = ""):
    now = datetime.now()
    # set workspace folder by time; make sure we don't have write conflict if there are many CI tests running
    os.environ['SPARK_CONFIG__DATABRICKS__WORK_DIR'] = ''.join(['dbfs:/feathrazure_cijob','_', str(now.minute), '_', str(now.second), '_', str(now.microsecond)]) 
    os.environ['SPARK_CONFIG__AZURE_SYNAPSE__WORKSPACE_DIR'] = ''.join(['abfss://feathrazuretest3fs@feathrazuretest3storage.dfs.core.windows.net/feathr_github_ci','_', str(now.minute), '_', str(now.second) ,'_', str(now.microsecond)]) 
    client = FeathrClient(config_path=config_path, local_workspace_dir=local_workspace_dir)

    if resolution == 'DAILY':
        if postfix_path != "":
            batch_source = HdfsSource(name="testTimePartitionSource",
                          path=data_source_path,
                          time_partition_pattern="yyyy/MM/dd",
                          postfix_path=postfix_path
                        )
        else:
            batch_source = HdfsSource(name="testTimePartitionSource",
                          path=data_source_path,
                          time_partition_pattern="yyyy/MM/dd"
                        )
    else:
        batch_source = HdfsSource(name="testTimePartitionSource",
                          path=data_source_path,
                          time_partition_pattern="yyyy/MM/dd/HH"
                        )
    key = TypedKey(key_column="key0",
               key_column_type=ValueType.INT32)
    agg_features = [
    Feature(name="f_loc_avg_output",
            key=[key],
            feature_type=FLOAT,
            transform=WindowAggTransformation(agg_expr="f_location_avg_fare",
                                              agg_func="AVG",
                                              window="3d")),
    Feature(name="f_loc_max_output",
            feature_type=FLOAT,
            key=[key],
            transform=WindowAggTransformation(agg_expr="f_location_max_fare",
                                              agg_func="MAX",
                                              window="3d")),
    ]

    agg_anchor = FeatureAnchor(name="testTimePartitionFeatures",
                           source=batch_source,
                           features=agg_features)
    client.build_features(anchor_list=[agg_anchor])
    return client

def time_partition_pattern_feature_join_test_setup(config_path: str, data_source_path: str, local_workspace_dir: str = None, resolution: str = 'DAILY', postfix_path: str = ""):
    now = datetime.now()
    # set workspace folder by time; make sure we don't have write conflict if there are many CI tests running
    os.environ['SPARK_CONFIG__DATABRICKS__WORK_DIR'] = ''.join(['dbfs:/feathrazure_cijob','_', str(now.minute), '_', str(now.second), '_', str(now.microsecond)]) 
    os.environ['SPARK_CONFIG__AZURE_SYNAPSE__WORKSPACE_DIR'] = ''.join(['abfss://feathrazuretest3fs@feathrazuretest3storage.dfs.core.windows.net/feathr_github_ci','_', str(now.minute), '_', str(now.second) ,'_', str(now.microsecond)]) 
    client = FeathrClient(config_path=config_path, local_workspace_dir=local_workspace_dir)
    
    if postfix_path == "":
        if resolution == 'DAILY':
            batch_source_tpp = HdfsSource(name="nycTaxiBatchSource",
                          path=data_source_path,
                          time_partition_pattern="yyyy/MM/dd"
                        )
        else:
            batch_source_tpp = HdfsSource(name="nycTaxiBatchSource",
                          path=data_source_path,
                          time_partition_pattern="yyyy/MM/dd/HH"
                        )
    else:
        batch_source_tpp = HdfsSource(name="nycTaxiBatchSource",
                          path=data_source_path,
                          time_partition_pattern="yyyy/MM/dd",
                          postfix_path=postfix_path
                        )
    tpp_key = TypedKey(key_column="f_location_max_fare",
                    key_column_type=ValueType.FLOAT)
    tpp_features = [
    Feature(name="key0",
            key=tpp_key,
            feature_type=FLOAT,
            transform=WindowAggTransformation(agg_expr="key0",
                                              agg_func="LATEST",
                                              window="3d"
                    ))
    ]
    tpp_anchor = FeatureAnchor(name="tppFeatures",
                            source=batch_source_tpp,
                            features=tpp_features)
    client.build_features(anchor_list=[tpp_anchor])
    
    feature_query = FeatureQuery(feature_list=["key0"], key=tpp_key)
    settings = ObservationSettings(
        observation_path='wasbs://public@azurefeathrstorage.blob.core.windows.net/sample_data/tpp_source.csv',
        event_timestamp_column="lpep_dropoff_datetime",
        timestamp_format="yyyy-MM-dd HH:mm:ss")
    return [client, feature_query, settings]