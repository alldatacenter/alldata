import re
from copy import deepcopy
from typing import List, Optional, Union, Dict

from jinja2 import Template

from feathr.definition.dtype import FeatureType
from feathr.definition.transformation import ExpressionTransformation, Transformation, WindowAggTransformation
from feathr.definition.typed_key import DUMMY_KEY, TypedKey
from feathr.definition.feathrconfig import HoconConvertible


class FeatureBase(HoconConvertible):
    """The base class for features
    It has a feature name, feature type, and a convenient transformation used to produce its feature value.

    Attributes:
        name: Unique name of the feature. Only alphabet, numbers, and '_' are allowed in the name.
                It can not start with numbers. Note that '.' is NOT ALLOWED!
        feature_type: the feature value type. e.g. INT32, FLOAT, etc. feathr.dtype
        key: The key of this feature. e.g. user_id.
        transform: A transformation used to produce its feature value. e.g. amount * 10
        registry_tags: A dict of (str, str) that you can pass to feature registry for better organization. For example, you can use {"deprecated": "true"} to indicate this feature is deprecated, etc.
    """
    def __init__(self,
                 name: str,
                 feature_type: FeatureType,
                 transform: Optional[Union[str, Transformation]] = None,
                 key: Optional[Union[TypedKey, List[TypedKey]]] = [DUMMY_KEY],
                 registry_tags: Optional[Dict[str, str]] = None,
                 ):
        FeatureBase.validate_feature_name(name)

        # Validate the feature type
        if not isinstance(feature_type, FeatureType):
            raise KeyError(f'Feature type must be a FeatureType class, like INT32, but got {feature_type}')

        self.name = name
        self.feature_type = feature_type
        self.registry_tags=registry_tags
        self.key = key if isinstance(key, List) else [key]
        # feature_alias: Rename the derived feature to `feature_alias`. Default to feature name.
        self.feature_alias = name
        # If no transformation is specified, default to referencing the a field with the same name
        if transform is None:
            self.transform = ExpressionTransformation(name)
        elif isinstance(transform, str):
            self.transform = ExpressionTransformation(transform)
        else:
            self.transform = transform
        # An alias for the key in this feature. Default to its key column alias. Useful in derived features.
        # self.key could be null, when getting features from registry.
        self.key_alias = [k.key_column_alias for k in self.key if k]

    @classmethod
    def validate_feature_name(cls, feature_name: str) -> bool:
        """
        Only alphabet, numbers, and '_' are allowed in the name.
        It can not start with numbers. Note that '.' is NOT ALLOWED!
        This is because some compute engines, such as Spark, will consider them as operators in feature name.
        """
        if not feature_name:
            raise Exception('Feature name rule violation: empty feature name detected')

        feature_validator = re.compile(r"""^              # from the start of the string
                                           [a-zA-Z_]{1}   # first character can only be a letter or underscore 
                                           [a-zA-Z0-9_]+  # as many letters, numbers, or underscores as you like  
                                           $""",          # to the end of the string
                                       re.X)

        if not feature_validator.match(feature_name):
            raise Exception(
                'Feature name rule violation: only letters, numbers, and underscores are allowed in the name, ' +
                f'and the name cannot start with a number. name={feature_name}')

        return True

    def with_key(self, key_alias: Union[str, List[str]]):
        """Rename the feature key with the alias. This is useful in derived features that depends on
        the same feature with different keys."""
        cleaned_key_alias = [key_alias] if isinstance(key_alias, str) else key_alias
        assert(len(cleaned_key_alias) == len(self.key))
        new_key = []
        for i in range(0, len(cleaned_key_alias)):
            typed_key = deepcopy(self.key[i])
            typed_key.key_column_alias = cleaned_key_alias[i]
            new_key.append(typed_key)

        res = deepcopy(self)
        res.key = new_key
        res.key_alias = cleaned_key_alias
        return res

    def as_feature(self, feature_alias):
        """Provide the feature a different alias, which can be used to reference the feature in transformation
        expression. This is useful in derived features that depends on the same feature with different keys."""
        new_feature = deepcopy(self)
        new_feature.feature_alias = feature_alias
        return new_feature


class Feature(FeatureBase):
    """A feature is an individual measurable property or characteristic of an entity.
    It has a feature name, feature type, and a convenient row transformation used to produce its feature value.

    Attributes:
        name: Unique name of the feature. Only alphabet, numbers, and '_' are allowed in the name.
                It can not start with numbers. Note that '.' is NOT ALLOWED!
        feature_type: the feature value type. e.g. INT32, FLOAT, etc. Should be part of `feathr.dtype`
        key: The key of this feature. e.g. user_id.
        transform: A row transformation used to produce its feature value. e.g. amount * 10
        registry_tags: A dict of (str, str) that you can pass to feature registry for better organization. For example, you can use {"deprecated": "true"} to indicate this feature is deprecated, etc.
    """
    def __init__(self,
                name: str,
                feature_type: FeatureType,
                key: Optional[Union[TypedKey, List[TypedKey]]] = [DUMMY_KEY],
                transform: Optional[Union[str, Transformation]] = None,
                registry_tags: Optional[Dict[str, str]] = None,
                ):
        super(Feature, self).__init__(name, feature_type, transform, key, registry_tags)


    def to_feature_config(self) -> str:
        tm = Template("""
            {{feature.name}}: {
                {{feature.transform.to_feature_config()}}
                {{feature.feature_type.to_feature_config()}} 
            }
        """)
        return tm.render(feature=self)

