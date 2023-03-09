from feathr.anchor import FeatureAnchor
from feathr.source import HdfsSource
from feathr.feature import Feature
from feathr.dtype import BOOLEAN, FLOAT, ValueType
from feathr.transformation import WindowAggTransformation
from feathr.typed_key import TypedKey

batch_source = HdfsSource(name="nycTaxiBatchSource",
                          path="wasbs://public@azurefeathrstorage.blob.core.windows.net/sample_data/green_tripdata_2020-04.csv",
                          event_timestamp_column="lpep_dropoff_datetime",
                          timestamp_format="yyyy-MM-dd HH:mm:ss")

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
