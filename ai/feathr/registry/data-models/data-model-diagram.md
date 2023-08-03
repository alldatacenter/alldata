
# Feathr Abstract backend Data Model Diagram

This file defines abstract backend data models diagram for feature registry.
[Python Code](./models.py)

```mermaid
classDiagram
   Project "1" --> "n" FeatureName: contains
    Project "1" --> "n" Anchor: contains
    FeatureName "1" --> "n" Feature: contains
    FeatureName --> "Optional" SemanticVersion: contains
    FeatureName --> "Optional" FeatureType: contains
    Anchor "1" --> "n" AnchorFeature: contains
    Anchor -->  DataSource: contains    
    Feature <|-- AnchorFeature: extends
    Feature <|-- DerivedFeature: extends
    Feature --> Transformation: contains
    Feature --> Source: contains    
    Transformation --> Function: contains
    Source <|-- DataSource: extends
    DataSource --> "Optional" Clazz: contains
    DataSource --> "Optional" Function: contains
    Source <|-- MultiFeatureSource: extends
    MultiFeatureSource "1" --> "1..n" FeatureSource: contains
    AnchorFeature  -->  DataSource: contains
    DerivedFeature -->  MultiFeatureSource: contains
    FeathrModel <|-- Project: extends
    FeathrModel <|-- FeatureName: extends
    FeathrModel <|-- Anchor: extends
    FeathrModel <|-- Feature: extends
    FeathrModel <|-- Source: extends
    Dimension --> DimensionType: contains
    TensorFeatureFormat --> TensorCategory: contains
    TensorFeatureFormat --> ValueType: contains
    TensorFeatureFormat "1" --> "1..n" Dimension: contains
    FeatureType --> FeatureValueType: contains
    FeatureType --> "Optional" TensorFeatureFormat: contains
    Window --> WindowTimeUnit: contains
    Function <|-- MvelExpression: extends
    Function <|-- UserDefinedFunction: extends
    Function <|-- SparkSqlExpression: extends
    SlidingWindowAggregation --> SparkSqlExpression: contains
    SlidingWindowAggregation --> SlidingWindowAggregationType: contains
    SlidingWindowAggregation --> Window: contains
    SlidingWindowEmbeddingAggregation --> SparkSqlExpression: contains
    SlidingWindowEmbeddingAggregation --> SlidingWindowEmbeddingAggregationType: contains
    SlidingWindowEmbeddingAggregation --> Window: contains
    SlidingWindowLatestAvailable --> SparkSqlExpression: contains
    SlidingWindowLatestAvailable --> Window: contains
    Function <|-- SlidingWindowAggregation: extends
    Function <|-- SlidingWindowEmbeddingAggregation: extends
    Function <|-- SlidingWindowLatestAvailable: extends
    
    class ValueType{
        <<enumeration>>
        INT
        LONG
        FLOAT
        DOUBLE
        STRING
        BOOLEAN
        BYTES
    }
    class DimensionType{
        <<enumeration>>
        INT
        LONG
        STRING
        BOOLEAN
        BYTES
    }
    class TensorCategory{
        <<enumeration>>
        DENSE
        SPARSE
        RAGGED
    }
    class FeatureValueType{
        <<enumeration>>
        BOOLEAN
        NUMERIC
        CATEGORICAL
        CATEGORICAL_SET
        DENSE_VECTOR
        TERM_VECTOR
        TENSOR
        UNSPECIFIED
    }
    class Dimension{
        +DimensionType type
        +Optional[str] shape
    }
    class TensorFeatureFormat{
        +TensorCategory tensorCategory
        +ValueType valueType
        +List[Dimension] dimensions
    }
    class FeatureType{
        +FeatureValueType type
        +Optional[TensorFeatureFormat] format
        +Union[bool, int, float, str, types] defaultValue
    }
    class Clazz{
        +str fullyQualifiedName
    }
    class Function{
        +str expression
    }
    class MvelExpression{
        +str mvel
    }
    class UserDefinedFunction{
        +str sql
    }
    class SemanticVersion{
        +int majorVersion
        +int minorVersion
        +int patchVersion
        +Optional[str] metadata
    }
    class FeathrModel{
        +str displayName
        +str typeName
    }
    class SlidingWindowAggregationType{
        <<enumeration>>
        SUM
        COUNT
        MAX
        MIN
        AVG
    }
    class SlidingWindowEmbeddingAggregationType{
        <<enumeration>>
        MAX_POOLING
        MIN_POOLING
        AVG_POOLING
    }
    class WindowTimeUnit{
        <<enumeration>>
        DAY
        HOUR
        MINUTE
        SECOND
    }
    class Window{
        +int size
        +WindowTimeUnit unit     
    }
    class SlidingWindowAggregation{
        +SlidingWindowAggregationType aggregationType
        +Window window
        +SparkSqlExpression targetColumn
        +Optional[SparkSqlExpression] filter
        +Optional[SparkSqlExpression] groupBy
        +Optional[int] limit        
    }
    class SlidingWindowEmbeddingAggregation{
        +SlidingWindowEmbeddingAggregationType aggregationType
        +Window window
        +SparkSqlExpression targetColumn
        +Optional[SparkSqlExpression] filter
        +Optional[SparkSqlExpression] groupBy
    }
    class SlidingWindowLatestAvailable{
        +Optional[Window] window
        +SparkSqlExpression targetColumn
        +Optional[SparkSqlExpression] filter
        +Optional[SparkSqlExpression] groupBy
        +Optional[int] limit        
    }
    class Source{
    }
    class DataSource{
        +Optional[Clazz] clazz
        +Optional[Function] keyFunction
    }
    class FeatureSource{
        +FeatureNameId input_feature_name_id
        +Optional[str] alias
    }
    class MultiFeatureSource{
        +List[FeatureSource] sources
    }
    class Transformation{
        +Function transformationFunction
    }
    class Feature{
        +FeatureId id
        +FeatureNameId feature_namme_id
        +Source source
        +Transformation transformation
    }
    class AnchorFeature{
        +AnchorId  anchor_id
        +DataSource source
    }
    class DerivedFeature{
        +MultiFeatureSource source
    }
    class FeatureName{
        +FeatureNameId id
        +ProjectId project_id
        +List[FeatureId] feature_ids
        +Optional[SemanticVersion] semanticVersion
        +Optional[FeatureType] featureType
    }
    class Project{
        +ProjectId id
        +List[FeatureNameId] feature_name_ids
        +List[AnchorId] anchor_ids
    }
    class Anchor{
        +AnchorId id
        +ProjectId project_id
        +DataSource source
        +List[FeatureId] anchor_feature_ids
    }
```