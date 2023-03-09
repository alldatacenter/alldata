from feathr import AvroJsonSchema
from feathr import KafKaSource
from feathr import KafkaConfig
from typing import List
import os
import random
from datetime import datetime, timedelta

from feathr import (BOOLEAN, FLOAT, INPUT_CONTEXT, INT32, STRING,
                    DerivedFeature, Feature, FeatureAnchor, HdfsSource,
                    TypedKey, ValueType, WindowAggTransformation)
from feathr import FeathrClient
from feathr.definition.transformation import ExpressionTransformation

import dsl_generator

batch_source = HdfsSource(name="nycTaxiBatchSource",
                            path="wasbs://public@azurefeathrstorage.blob.core.windows.net/sample_data/green_tripdata_2020-04.csv",
                            event_timestamp_column="lpep_dropoff_datetime",
                            timestamp_format="yyyy-MM-dd HH:mm:ss")

f_trip_distance = Feature(name="f_trip_distance",
                            feature_type=FLOAT, transform="trip_distance")
f_trip_time_duration = Feature(name="f_trip_time_duration",
                                feature_type=INT32,
                                transform="(to_unix_timestamp(lpep_dropoff_datetime) - to_unix_timestamp(lpep_pickup_datetime))/60")
f_is_long_trip_distance =   Feature(name="f_is_long_trip_distance",
            feature_type=BOOLEAN,
            transform="cast_float(trip_distance)>30"),
f_day_of_week = Feature(name="f_day_of_week",
            feature_type=INT32,
            transform="some_fancy_func(lpep_dropoff_datetime)")
features = [
    f_trip_distance,
    f_trip_time_duration,
    f_is_long_trip_distance,
    f_day_of_week,
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
f_location_avg_fare = Feature(name="f_location_avg_fare",
                        key=location_id,
                        feature_type=FLOAT,
                        transform=WindowAggTransformation(agg_expr="cast_float(fare_amount)",
                                                            agg_func="AVG",
                                                            window="90d",
                                                            filter="fare_amount > 0"
                                                            ))
agg_features = [f_location_avg_fare,
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


# This should work
dsl = dsl_generator.gen_dsl("test_pipeline", [f_trip_time_rounded, f_trip_time_distance])
print(dsl)

# This will cause NotImplementedError, because it uses `some_fancy_func` function, which is unsupported
try:
    dsl_generator.gen_dsl("test_pipeline", [f_day_of_week])
    raise Exception("Should have failed")
except NotImplementedError as e:
    pass
    

# This will also fail because we don't support WindowAggTransformation
try:
    dsl_generator.gen_dsl("test_pipeline", [f_location_avg_fare])
    raise Exception("Should have failed")
except:
    pass