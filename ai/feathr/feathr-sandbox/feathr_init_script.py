# Goal of this file is to run a basic Feathr script within spark so that Maven packages can be downloaded into the docker container to save time during actual run.
# This can also serve as a sanity check

import os
import tempfile
from datetime import datetime
import pandas as pd
from feathr import FeathrClient
from feathr import BOOLEAN, FLOAT, INT32, ValueType
from feathr import Feature, DerivedFeature, FeatureAnchor
from feathr import FeatureQuery, ObservationSettings
from feathr import INPUT_CONTEXT, HdfsSource
from feathr import WindowAggTransformation
from feathr import TypedKey
from pyspark.sql import DataFrame
import feathr
from pathlib import Path

print(feathr.__version__)

os.environ['SPARK_LOCAL_IP'] = "127.0.0.1"
os.environ['REDIS_PASSWORD'] = "foobared"  # default password for Redis

# Make sure we get the Feathr jar name, assuming we just have one jar file.

import glob
jar_name = glob.glob("./*.jar")[0]
print(f"Found jar file at {jar_name}")

yaml_config = f"""
api_version: 1
project_config:
  project_name: 'local_spark'

spark_config:
  # choice for spark runtime. Currently support: azure_synapse, databricks, local
  spark_cluster: 'local'
  spark_result_output_parts: '1'
  local:
    master: 'local[*]'
    feathr_runtime_location: "{jar_name}"

online_store:
  redis:
    # Redis configs to access Redis cluster
    host: '127.0.0.1'
    port: 6379
    ssl_enabled: False

feature_registry:
  # The API endpoint of the registry service
  api_endpoint: "http://127.0.0.1:8000/api/v1"
"""
feathr_workspace_folder = Path("./feathr_config.yaml")
feathr_workspace_folder.parent.mkdir(exist_ok=True, parents=True)
feathr_workspace_folder.write_text(yaml_config)


client = FeathrClient(str(feathr_workspace_folder))
DATA_FILE_PATH = "/tmp/green_tripdata_2020-04_with_index.csv"
from feathr.datasets.utils import maybe_download
from feathr.datasets.constants import NYC_TAXI_SMALL_URL

maybe_download(src_url=NYC_TAXI_SMALL_URL, dst_filepath=DATA_FILE_PATH)


TIMESTAMP_COL = "lpep_dropoff_datetime"
TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss"


def preprocessing(df: DataFrame) -> DataFrame:
    import pyspark.sql.functions as F
    df = df.withColumn("fare_amount_cents",
                       (F.col("fare_amount") * 100.0).cast("float"))
    return df


batch_source = HdfsSource(
    name="nycTaxiBatchSource",
    path=DATA_FILE_PATH,
    event_timestamp_column=TIMESTAMP_COL,
    preprocessing=preprocessing,
    timestamp_format=TIMESTAMP_FORMAT,
)

# We define f_trip_distance and f_trip_time_duration features separately
# so that we can reuse them later for the derived features.
f_trip_distance = Feature(
    name="f_trip_distance",
    feature_type=FLOAT,
    transform="trip_distance",
)
f_trip_time_duration = Feature(
    name="f_trip_time_duration",
    feature_type=FLOAT,
    transform="cast_float((to_unix_timestamp(lpep_dropoff_datetime) - to_unix_timestamp(lpep_pickup_datetime)) / 60)",
)


features = [
    f_trip_distance,
    f_trip_time_duration,
    Feature(
        name="f_is_long_trip_distance",
        feature_type=BOOLEAN,
        transform="trip_distance > 30.0",
    ),
    Feature(
        name="f_day_of_week",
        feature_type=INT32,
        transform="dayofweek(lpep_dropoff_datetime)",
    ),
    Feature(
        name="f_day_of_month",
        feature_type=INT32,
        transform="dayofmonth(lpep_dropoff_datetime)",
    ),
    Feature(
        name="f_hour_of_day",
        feature_type=INT32,
        transform="hour(lpep_dropoff_datetime)",
    ),
]

# After you have defined features, bring them together to build the anchor to the source.
feature_anchor = FeatureAnchor(
    name="feature_anchor",
    source=INPUT_CONTEXT,  # Pass through source, i.e. observation data.
    features=features,
)

agg_key = TypedKey(
    key_column="DOLocationID",
    key_column_type=ValueType.INT32,
    description="location id in NYC",
    full_name="nyc_taxi.location_id",
)

agg_window = "90d"

# Anchored features with aggregations
agg_features = [
    Feature(
        name="f_location_avg_fare",
        key=agg_key,
        feature_type=FLOAT,
        transform=WindowAggTransformation(
            agg_expr="fare_amount_cents",
            agg_func="AVG",
            window=agg_window,
        ),
    ),
    Feature(
        name="f_location_max_fare",
        key=agg_key,
        feature_type=FLOAT,
        transform=WindowAggTransformation(
            agg_expr="fare_amount_cents",
            agg_func="MAX",
            window=agg_window,
        ),
    ),
]

agg_feature_anchor = FeatureAnchor(
    name="agg_feature_anchor",
    # External data source for feature. Typically a data table.
    source=batch_source,
    features=agg_features,
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

derived_feature = [f_trip_time_distance, f_trip_time_rounded]


client.build_features(
    anchor_list=[feature_anchor, agg_feature_anchor],
    derived_feature_list=derived_feature,
)


feature_names = [feature.name for feature in features + agg_features]
feature_names


# Try to register the service after the spark run (so that the Feathr API can start with sufficient time)
try:
    client.register_features()
except Exception as e:
    print(e)
print(client.list_registered_features(project_name=client.project_name))


now = datetime.now().strftime("%Y%m%d%H%M%S")
offline_features_path = os.path.join("debug", f"test_output_{now}")

# Features that we want to request. Can use a subset of features
query = FeatureQuery(
    feature_list=feature_names,
    key=agg_key,
)
settings = ObservationSettings(
    observation_path=DATA_FILE_PATH,
    event_timestamp_column=TIMESTAMP_COL,
    timestamp_format=TIMESTAMP_FORMAT,
)
client.get_offline_features(
    observation_settings=settings,
    feature_query=query,
    output_path=offline_features_path,
)

client.wait_job_to_finish(timeout_sec=5000)

from feathr.utils.job_utils import get_result_df
res_df = get_result_df(client)
print(res_df.head())