from .client import FeathrClient
from .spark_provider.feathr_configurations import SparkExecutionConfiguration
from .definition.feature_derivations import *
from .definition.anchor import *
from .definition.feature import *
from .definition.feathrconfig import *
from .definition.transformation import *
from .definition.dtype import *
from .definition.source import *
from .definition.typed_key import *
from .definition.materialization_settings import *
from .definition.monitoring_settings import *
from .definition.sink import *
from .definition.query_feature_list import *
from .definition.lookup_feature import *
from .definition.aggregation import *
from .definition.settings import *
from .utils.job_utils import *
from .utils.feature_printer import *
from .version import __version__

# skipped class as they are internal methods:
# RepoDefinitions, HoconConvertible,
# expose the modules so docs can build
# referencee： https://stackoverflow.com/questions/15115514/how-do-i-document-classes-without-the-module-name/31594545#31594545

# __all__ = []
# for v in dir():
#     if not v.startswith('__') and v != 'mypackage':
#         __all__.append(v)


__all__ = [
    'FeatureJoinJobParams',
    'FeatureGenerationJobParams',
    'FeathrClient',
    'DerivedFeature',
    'FeatureAnchor',
    'Feature',
    'ValueType',
    'WindowAggTransformation',
    'TypedKey',
    'DUMMYKEY',
    'BackfillTime',
    'MaterializationSettings',
    'MonitoringSettings',
    'RedisSink',
    'HdfsSink',
    'MonitoringSqlSink',
    'AerospikeSink',
    'FeatureQuery',
    'LookupFeature',
    'Aggregation',
    'get_result_df',
    'AvroJsonSchema',
    'Source',
    'InputContext',
    'HdfsSource',
    'SnowflakeSource',
    'KafkaConfig',
    'KafKaSource',
    'ValueType',
    'BooleanFeatureType',
    'Int32FeatureType',
    'Int64FeatureType',
    'FloatFeatureType',
    'DoubleFeatureType',
    'StringFeatureType',
    'BytesFeatureType',
    'FloatVectorFeatureType',
    'Int32VectorFeatureType',
    'Int64VectorFeatureType',
    'DoubleVectorFeatureType',
    'FeatureNameValidationError',
    'ObservationSettings',
    'FeaturePrinter',
    'SparkExecutionConfiguration',
    __version__,
 ]
