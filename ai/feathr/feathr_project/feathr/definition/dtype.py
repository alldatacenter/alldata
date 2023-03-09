import enum
from typing import List
from feathr.definition.feathrconfig import HoconConvertible

class ValueType(enum.Enum):
    """Data type to describe feature keys or observation keys.

    Attributes:
        UNSPECIFIED: key data type is unspecified.
        BOOL: key data type is boolean, either true or false
        INT32: key data type is 32-bit integer, for example, an invoice id, 93231.
        INT64: key data type is 64-bit integer, for example, an invoice id, 93231.
        FLOAT: key data type is float, for example, 123.4f.
        DOUBLE: key data type is double, for example, 123.4d.
        STRING: key data type is string, for example, a user name, 'user_joe'
        BYTES: key data type is bytes.
    """
    UNSPECIFIED = 0
    BOOL = 1
    INT32 = 2
    INT64 = 3
    FLOAT = 4
    DOUBLE = 5
    STRING = 6
    BYTES = 7

def value_type_to_str(v: ValueType) -> str:
    return {
        ValueType.UNSPECIFIED: "UNSPECIFIED",
        ValueType.BOOL: "BOOLEAN",
        ValueType.INT32: "INT",
        ValueType.INT64: "LONG",
        ValueType.FLOAT: "FLOAT",
        ValueType.DOUBLE: "DOUBLE",
        ValueType.STRING: "STRING",
        ValueType.BYTES: "BYTES",
    }[v]


def str_to_value_type(v: str) -> ValueType:
    """
    Additional keys are for backward compatibility
    """
    return {
        "UNSPECIFIED": ValueType.UNSPECIFIED,
        "0": ValueType.UNSPECIFIED,
        "BOOLEAN": ValueType.BOOL,
        "BOOL": ValueType.BOOL,
        "1": ValueType.BOOL,
        "INT": ValueType.INT32,
        "INT32": ValueType.INT32,
        "2": ValueType.INT32,
        "LONG": ValueType.INT64,
        "INT64": ValueType.INT64,
        "3": ValueType.INT64,
        "FLOAT": ValueType.FLOAT,
        "4": ValueType.FLOAT,
        "DOUBLE": ValueType.DOUBLE,
        "5": ValueType.DOUBLE,
        "STRING": ValueType.STRING,
        "6": ValueType.STRING,
        "BYTES": ValueType.BYTES,
        "7": ValueType.BYTES,
    }[v.upper()]

class FeatureType(HoconConvertible):
    """Base class for all feature types"""
    def __init__(self, val_type: ValueType, dimension_type: List[ValueType] = [], tensor_category: str = "DENSE", type: str = "TENSOR"):
        self.val_type = val_type
        self.dimension_type = dimension_type
        self.tensor_category = tensor_category
        self.type = type

    def __eq__(self, o) -> bool:
        return self.val_type == o.val_type \
            and self.dimension_type == o.dimension_type \
            and self.tensor_category == o.tensor_category \
            and self.type == o.type

    def to_feature_config(self) -> str:
        return fr"""
           type: {{
                type: {self.type}
                tensorCategory: {self.tensor_category}
                dimensionType: [{",".join([value_type_to_str(t) for t in self.dimension_type])}]
                valType: {value_type_to_str(self.val_type)}
            }}
        """

class BooleanFeatureType(FeatureType):
    """Boolean feature value, either true or false.
    """
    def __init__(self):
        FeatureType.__init__(self, val_type = ValueType.BOOL)

class Int32FeatureType(FeatureType):
    """32-bit integer feature value, for example, 123, 98765.
    """
    def __init__(self):
        FeatureType.__init__(self, val_type = ValueType.INT32)

class Int64FeatureType(FeatureType):
    """64-bit integer(a.k.a. Long in some system) feature value, for example, 123, 98765 but stored in 64-bit integer.
    """
    def __init__(self):
        FeatureType.__init__(self, val_type = ValueType.INT64)

class FloatFeatureType(FeatureType):
    """Float feature value, for example, 1.3f, 2.4f.
    """
    def __init__(self):
        FeatureType.__init__(self, val_type = ValueType.FLOAT)

class DoubleFeatureType(FeatureType):
    """Double feature value, for example, 1.3d, 2.4d. Double has better precision than float.
    """
    def __init__(self):
        FeatureType.__init__(self, val_type = ValueType.DOUBLE)

class StringFeatureType(FeatureType):
    """String feature value, for example, 'apple', 'orange'.
    """
    def __init__(self):
        FeatureType.__init__(self, val_type = ValueType.STRING)

class BytesFeatureType(FeatureType):
    """Bytes feature value.
    """
    def __init__(self):
        FeatureType.__init__(self, val_type = ValueType.BYTES)

class FloatVectorFeatureType(FeatureType):
    """Float vector feature value, for example, [1,3f, 2.4f, 3.9f]
    """
    def __init__(self):
        FeatureType.__init__(self, val_type = ValueType.FLOAT, dimension_type = [ValueType.INT32])


class Int32VectorFeatureType(FeatureType):
    """32-bit integer vector feature value, for example, [1, 3, 9]
    """
    def __init__(self):
        FeatureType.__init__(self, val_type = ValueType.INT32, dimension_type = [ValueType.INT32])


class Int64VectorFeatureType(FeatureType):
    """64-bit integer vector feature value, for example, [1, 3, 9]
    """
    def __init__(self):
        FeatureType.__init__(self, val_type = ValueType.INT64, dimension_type = [ValueType.INT32])


class DoubleVectorFeatureType(FeatureType):
    """Double vector feature value, for example, [1.3d, 3.3d, 9.3d]
    """
    def __init__(self):
        FeatureType.__init__(self, val_type = ValueType.DOUBLE, dimension_type = [ValueType.INT32])


# tensor dimension/axis
class Dimension:
    def __init__(self, shape: int, dType: ValueType = ValueType.INT32):
        self.shape = shape
        self.dType = dType


BOOLEAN = BooleanFeatureType()
INT32 = Int32FeatureType()
INT64 = Int64FeatureType()
FLOAT = FloatFeatureType()
DOUBLE = DoubleFeatureType()
STRING = StringFeatureType()
BYTES = BytesFeatureType()
FLOAT_VECTOR = FloatVectorFeatureType()
INT32_VECTOR = Int32VectorFeatureType()
INT64_VECTOR = Int64VectorFeatureType()
DOUBLE_VECTOR = DoubleVectorFeatureType()