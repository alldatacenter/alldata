from pydantic import BaseModel
from typing import Dict, Optional, List, Union
import json
from enum import Enum


class ValueType(Enum):
    """
    Type of the feature.
    """
    INT = "int"
    LONG = "long"
    FLOAT = "float"
    DOUBLE = "double"
    STRING = "string"
    BOOLEAN = "boolean"
    BYTES = "bytes"


class DimensionType(Enum):
    """
    Supported dimension types for tensors in Feathr.
    """
    INT = "int"
    LONG = "long"
    STRING = "string"
    BOOLEAN = "boolean"
    BYTES = "bytes"


class TensorCategory(Enum):
    """
    Supported Tensor categories in Feathr.
    """
    DENSE = "dense"  # Dense tensors store values in a contiguous sequential block of memory where all values are represented.
    SPARSE = "sparse"  # Sparse tensor represents a dataset in which most of the entries are zero.
    RAGGED = "ragged"  # Ragged tensors (also known as nested tensors) are similar to dense tensors but have variable-length dimensions.


class FeatureValueType(Enum):
    """
    The high level types associated with a feature.
    This represents the high level semantic types supported by early versions of feathr.
    """
    BOOLEAN = "boolean"  # Boolean valued feature
    NUMERIC = "numeric"  # Numerically valued feature
    CATEGORICAL = "categorical"  # Represent a feature that consists of a single category
    CATEGORICAL_SET = "categorical_set"  # Represent a feature that consists of multiple categories
    DENSE_VECTOR = "dense_vector"  # Represent a feature in vector format where the majority of the elements are non-zero
    TERM_VECTOR = "term_vector"  # Represent features that has string terms and numeric value
    TENSOR = "tensor"  # Represent tensor based features.
    UNSPECIFIED = "unspecified"  # Placeholder for when no types are specified


class Dimension(BaseModel):
    """
    Tensor is used to represent feature data. A tensor is a generalization of vectors and matrices to potentially higher dimensions.
    """
    type: DimensionType  # Type of the dimension in the tensor. Each dimension can have a different type.
    shape: Optional[int]  # Size of the dimension in the tensor. If unset, it means the size is unknown and actual size will be determined at runtime.


class TensorFeatureFormat(BaseModel):
    """
    Defines the format of feature data. Feature data is produced by applying transformation on source, in a Feature.
    Tensor is used to represent feature data. A tensor is a generalization of vectors and matrices to potentially
    higher dimensions.
    """
    tensorCategory: TensorCategory  # Type of the tensor.
    valueType: ValueType  # Type of the value column.
    dimensions: List[Dimension]  # A feature data can have zero or more dimensions (columns that represent keys).


class FeatureType(BaseModel):
    """
   Information about a featureName. It defines the type, format and default value.
   Tensor is the next generation representation of the features, so using
   Tensor type w TensorFeatureFormat would be preferable FeatureType.
    """
    type: FeatureValueType  # Defines the high level semantic type of feature.
    format: Optional[TensorFeatureFormat]  # Defines the format of feature data.
    defaultValue: Union[bool, int, float, str, bytes]


class Clazz(BaseModel):
    """
    Reference to a class by fully-qualified name
    """
    fullyQualifiedName: str  # A fully-qualified class name including paths.


class Function(BaseModel):
    """
    Base model for all functions
    """
    expression: str  # Expression in str format
    functionType: str # Type of function in str format, will be used in UI


class MvelExpression(Function):
    """
    An expression in MVEL language.
    """
    mvel: str  # The MVEL expression


class UserDefinedFunction(Function):
    """
    User defined function that can be used in feature extraction or derivation.
    """
    clazz: Clazz  # Reference to the class that implements the user defined function.
    parameters: Dict[str, json] = {}  # This field defines the custom parameters of the user defined function


class SparkSqlExpression(Function):
    """
    An expression in Spark SQL.
    """
    sql: str  # Spark SQl expression


class SemanticVersion(BaseModel):
    """
    A representation of a semantic version (see https://semver.org/)
    """
    majorVersion: int  # The major version of this version. This is the x in x.y.z.
    minorVersion: int  # The minor version of this version. This is the y in x.y.z
    patchVersion: int  # The patch version of this version. This is the z in x.y.z
    metadata: Optional[str]  # Optional build metadata attached to this version.


class FeathrModel(BaseModel):
    """
    Base model for feathr entity which will be displayed in Feathr UI
    """
    displayName: str  # name of the entity showed on UI
    typeName: str  # type of entity in str format, will be displayed in UI
