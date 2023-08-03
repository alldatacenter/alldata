from feathr import Aggregation
from feathr import Feature
from feathr import LookupFeature
from feathr import DerivedFeature
from feathr import FLOAT, FLOAT_VECTOR, ValueType, INT32_VECTOR
from feathr import TypedKey

def assert_config_equals(one, another):
    assert one.translate(str.maketrans('', '', ' \n\t\r')) == another.translate(str.maketrans('', '', ' \n\t\r'))
     
def test_single_key_lookup_feature_to_config():
    """Single key lookup feature config generation should work"""
    user_key = TypedKey(full_name="mockdata.user", key_column="user_id", key_column_type=ValueType.INT32, description="An user identifier")
    item_key = TypedKey(full_name="mockdata.item", key_column="item_id", key_column_type=ValueType.INT32, description="An item identifier")
    
    user_item = Feature(name="user_items", feature_type=INT32_VECTOR, key=user_key)
    item_price = Feature(name="item_price", feature_type=FLOAT_VECTOR, key=item_key)

    # A lookup feature
    lookup_feature = LookupFeature(name="user_avg_item_price",
                                    feature_type=FLOAT,
                                    key=user_key,
                                    base_feature=user_item,
                                    expansion_feature=item_price,
                                    aggregation=Aggregation.AVG)

    lookup_feature_config = """
    user_avg_item_price: {
        key: [user_id]
        join: {
            base: {key: [user_id], feature: user_items}
            expansion: {key: [item_id], feature: item_price}
        }
        aggregation: AVG
        type: {
            type: TENSOR
            tensorCategory: DENSE
            dimensionType: []
            valType: FLOAT
        }
    }"""
    assert_config_equals(lookup_feature.to_feature_config(), lookup_feature_config)
    assert(isinstance(lookup_feature, DerivedFeature))
 