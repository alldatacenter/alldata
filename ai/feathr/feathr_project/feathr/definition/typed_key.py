from __future__ import annotations
from copy import copy, deepcopy

from typing import List, Optional
from feathr.definition.dtype import ValueType, FeatureType


class TypedKey:
    """The key of a feature. A feature is typically keyed by some id(s). e.g. product id, user id
      Attributes:
        key_column: The id column name of this key. e.g. 'product_id'.
        key_column_type: Types of the key_column
        full_name: Unique name of the key. Recommend using [project_name].[key_name], e.g. ads.user_id
        description: Documentation for the key.
        key_column_alias: Used in some advanced derived features. Default to the key_column.
    """
    def __init__(self,
                 key_column: str,
                 key_column_type: ValueType,
                 full_name: Optional[str] = None,
                 description: Optional[str] = None,
                 key_column_alias: Optional[str] = None) -> None:
        # Validate the key_column type
        if not isinstance(key_column_type, ValueType):
            raise KeyError(f'key_column_type must be a ValueType, like Value.INT32, but got {key_column_type}')
        
        self.key_column = key_column
        self.key_column_type = key_column_type
        self.full_name = full_name
        self.description = description
        self.key_column_alias = key_column_alias if key_column_alias else self.key_column

    def as_key(self, key_column_alias: str) -> TypedKey:
        """Rename the key alias. This is useful in derived features that depends on the same feature
            with different keys.
        """
        new_key = deepcopy(self)
        new_key.key_column_alias = key_column_alias
        return new_key


# passthrough/request feature do not need keys, as they are just a transformation defined on top of the request data
# They do not necessarily describe the value of keyed entity, e.g. dayofweek(timestamp) is transform on a request
# field without key
DUMMY_KEY = TypedKey(key_column="NOT_NEEDED",
                     key_column_type=ValueType.UNSPECIFIED,
                     full_name="feathr.dummy_typedkey",
                     description="A dummy typed key for passthrough/request feature.")