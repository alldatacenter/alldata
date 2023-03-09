from typing import List, Optional, Union

from jinja2 import Template

from feathr.definition.typed_key import TypedKey
from feathr.definition.feathrconfig import HoconConvertible

class FeatureQuery(HoconConvertible):
    """A FeatureQuery contains a list of features

    Attributes:
        feature_list: a list of feature names
        key: key of `feature_list`, all features must share the same key
        """
    def __init__(self, feature_list: List[str], key: Optional[Union[TypedKey, List[TypedKey]]] = None) -> None:
        self.key = key
        if isinstance(key, TypedKey):
            self.key = [key]
        self.feature_list = feature_list

    def to_feature_config(self) -> str:
        tm = Template("""
            {
                key: [{{key_columns}}]
                featureList: [{{feature_names}}]
            }
        """)
        key_columns = ", ".join(k.key_column for k in self.key) if self.key else "NOT_NEEDED"
        feature_list = ", ".join(f for f in self.feature_list)
        return tm.render(key_columns = key_columns, feature_names = feature_list)
