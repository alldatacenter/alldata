from copy import copy, deepcopy
from typing import List, Optional, Union, Dict

from jinja2 import Template

from feathr.definition.dtype import FeatureType
from feathr.definition.feature_derivations import DerivedFeature
from feathr.definition.feature import FeatureBase
from feathr.definition.transformation import RowTransformation
from feathr.definition.typed_key import DUMMY_KEY, TypedKey
from feathr.definition.aggregation import Aggregation

class LookupFeature(DerivedFeature):
    """A lookup feature is a feature defined on top of two other features, i.e. using the feature value of the base feature as key, to lookup the feature value from the expansion feature.
    e.g. a lookup feature user_purchased_item_avg_price could be key-ed by user_id, and computed by:
    base feature is user_purchased_item_ids. For a given user_id, it returns the item ids purchased by the user.
    expansion feature is item_price. For a given item id, it returns the item price.
    aggregation function is average of the item prices.

    Attributes:
        name: Derived feature name
        feature_type: Type of derived feature
        key: Join key of the derived feature
        base_feature: The feature value of this feature will be used as key to lookup from the expansion feature
        expansion_feature: The feature to be looked up
        aggregation: Specify the aggregation for the feature values lookup from the expansion feature, in the
                     case of the base feature value needed to be converted into multiple lookup keys,
                     e.g. feature value is an array and each value in the array is used once as a lookup key.
    """

    def __init__(self,
                name: str,
                feature_type: FeatureType,
                base_feature: FeatureBase,
                expansion_feature: FeatureBase,
                aggregation: Aggregation,
                key: Optional[Union[TypedKey, List[TypedKey]]] = [DUMMY_KEY],
                registry_tags: Optional[Dict[str, str]] = None,
                ):
        super(LookupFeature, self).__init__(name, feature_type, input_features=[base_feature, expansion_feature],
                                            transform="", key=key, registry_tags=registry_tags)
        self.base_feature = base_feature
        self.expansion_feature = expansion_feature
        self.aggregation = aggregation

    def to_feature_config(self) -> str:
        tm = Template("""
            {{lookup_feature.name}}: {
                key: [{{','.join(lookup_feature.key_alias)}}]
                join: {
                    base:{ 
                            key: [{{','.join(lookup_feature.base_feature.key_alias)}}],
                            feature: {{lookup_feature.base_feature.name}} 
                         }
                    expansion: { 
                            key: [{{','.join(lookup_feature.expansion_feature.key_alias)}}],
                            feature: {{lookup_feature.expansion_feature.name}}
                         } 
                }
                aggregation: {{lookup_feature.aggregation.name}}
                {{lookup_feature.feature_type.to_feature_config()}}
            }
        """)
        return tm.render(lookup_feature=self)
