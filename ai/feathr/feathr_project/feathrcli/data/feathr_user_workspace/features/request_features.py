from feathr.anchor import FeatureAnchor
from feathr.feature import Feature
from feathr.dtype import BOOLEAN, INT32, FLOAT, ValueType
from feathr.feature_derivations import DerivedFeature
from feathr.source import INPUT_CONTEXT

f_trip_distance = Feature(name="f_trip_distance", feature_type=FLOAT, transform="trip_distance")
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
                input_features=[f_trip_distance, f_trip_time_duration],
                transform="f_trip_distance * f_trip_time_duration")

f_trip_time_rounded = DerivedFeature(name="f_trip_time_rounded",
                feature_type=INT32,
                input_features=[f_trip_time_duration],
                transform="f_trip_time_duration % 10")
