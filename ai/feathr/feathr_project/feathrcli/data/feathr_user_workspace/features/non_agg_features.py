from feathr.anchor import FeatureAnchor
from feathr.feature import Feature
from feathr.dtype import BOOLEAN, INT32, ValueType
from feathr.typed_key import TypedKey
from feathr.source import HdfsSource

batch_source = HdfsSource(name="nycTaxiBatchSource",
                          path="wasbs://public@azurefeathrstorage.blob.core.windows.net/sample_data/green_tripdata_2020-04.csv",
                          event_timestamp_column="lpep_dropoff_datetime",
                          timestamp_format="yyyy-MM-dd HH:mm:ss")

location_id = TypedKey(key_column="DOLocationID",
                       key_column_type=ValueType.INT32,
                       description="location id in NYC",
                       full_name="nyc_taxi.location_id")
features = [
    Feature(name="f_loc_is_long_trip_distance",
            feature_type=BOOLEAN,
            transform="cast_float(trip_distance)>30", key=location_id),
    Feature(name="f_loc_day_of_week",
            feature_type=INT32,
            transform="dayofweek(lpep_dropoff_datetime)", key=location_id)
]

anchor = FeatureAnchor(name="nonAggFeatures",
                       source=batch_source,
                       features=features)