import base64
import copy
import json
import logging
import os
import tempfile
from typing import Any, Dict, List, Tuple, Union, Set

from azure.identity import DefaultAzureCredential
from jinja2 import Template
from loguru import logger
from pyhocon import ConfigFactory
import redis

from feathr.constants import *
from feathr.definition._materialization_utils import _to_materialization_config
from feathr.definition.anchor import FeatureAnchor
from feathr.definition.config_helper import FeathrConfigHelper
from feathr.definition.feature import FeatureBase
from feathr.definition.feature_derivations import DerivedFeature
from feathr.definition.materialization_settings import MaterializationSettings
from feathr.definition.monitoring_settings import MonitoringSettings
from feathr.definition.query_feature_list import FeatureQuery
from feathr.definition.settings import ObservationSettings, ConflictsAutoCorrection
from feathr.definition.sink import HdfsSink, Sink
from feathr.definition.source import InputContext
from feathr.definition.transformation import WindowAggTransformation
from feathr.definition.typed_key import TypedKey
from feathr.protobuf.featureValue_pb2 import FeatureValue
from feathr.registry._feathr_registry_client import _FeatureRegistry, derived_feature_to_def, feature_to_def
from feathr.registry._feature_registry_purview import _PurviewRegistry
from feathr.spark_provider._databricks_submission import _FeathrDatabricksJobLauncher
from feathr.spark_provider._localspark_submission import _FeathrLocalSparkJobLauncher
from feathr.spark_provider._synapse_submission import _FeathrSynapseJobLauncher
from feathr.spark_provider.feathr_configurations import SparkExecutionConfiguration
from feathr.udf._preprocessing_pyudf_manager import _PreprocessingPyudfManager
from feathr.utils._env_config_reader import EnvConfigReader
from feathr.utils._file_utils import write_to_file
from feathr.utils.feature_printer import FeaturePrinter
from feathr.utils.spark_job_params import FeatureGenerationJobParams, FeatureJoinJobParams
from feathr.version import get_version
import importlib.util



class FeathrClient(object):
    """Feathr client.

    The client is used to create training dataset, materialize features, register features, and fetch features from
    the online storage.

    For offline storage and compute engine, Azure ADLS, AWS S3 and Azure Synapse are supported.

    For online storage, currently only Redis is supported.
    The users of this client is responsible for set up all the necessary information needed to start a Redis client via
    environment variable or a Spark cluster. Host address, port and password are needed to start the Redis client.

    Raises:
        RuntimeError: Fail to create the client since necessary environment variables are not set for Redis
            client creation.
    """
    def __init__(
        self,
        config_path:str = "./feathr_config.yaml",
        local_workspace_dir: str = None,
        credential: Any = None,
        project_registry_tag: Dict[str, str] = None,
    ):
        """Initialize Feathr Client.
        Configuration values used by the Feathr are evaluated in the following precedence, with items higher on the list taking priority.
            1. Environment variables
            2. Values in the configuration file
            3. Values in the Azure Key Vault

        Args:
            config_path (optional): Config yaml file path. See [Feathr Config Template](https://github.com/feathr-ai/feathr/blob/main/feathr_project/feathrcli/data/feathr_user_workspace/feathr_config.yaml) for more details.  Defaults to "./feathr_config.yaml".
            local_workspace_dir (optional): Set where is the local work space dir. If not set, Feathr will create a temporary folder to store local workspace related files.
            credential (optional): Azure credential to access cloud resources, most likely to be the returned result of DefaultAzureCredential(). If not set, Feathr will initialize DefaultAzureCredential() inside the __init__ function to get credentials.
            project_registry_tag (optional): Adding tags for project in Feathr registry. This might be useful if you want to tag your project as deprecated, or allow certain customizations on project level. Default is empty
        """
        self.logger = logging.getLogger(__name__)
        # Redis key separator
        self._KEY_SEPARATOR = ':'
        self._COMPOSITE_KEY_SEPARATOR = '#'
        self.env_config = EnvConfigReader(config_path=config_path)
        if local_workspace_dir:
            self.local_workspace_dir = local_workspace_dir
        else:
            # this is required for Windows
            tem_dir_obj = tempfile.TemporaryDirectory()
            self.local_workspace_dir = tem_dir_obj.name

        if not os.path.exists(config_path):
            self.logger.warning('No Configuration file exist at the user provided config_path or the default config_path (./feathr_config.yaml), you need to set the environment variables explicitly. For all the environment variables that you need to set, please refer to https://github.com/feathr-ai/feathr/blob/main/feathr_project/feathrcli/data/feathr_user_workspace/feathr_config.yaml')

        # Load all configs from yaml at initialization
        # DO NOT load any configs from yaml during runtime.
        self.project_name = self.env_config.get(
            'project_config__project_name')

        # Redis configs. This is optional unless users have configured Redis host.
        if self.env_config.get('online_store__redis__host'):
            # For illustrative purposes.
            spec = importlib.util.find_spec("redis")
            if spec is None:
                self.logger.warning('You have configured Redis host, but there is no local Redis client package. Install the package using "pip install redis". ')
            self.redis_host = self.env_config.get(
                'online_store__redis__host')
            self.redis_port = self.env_config.get(
                'online_store__redis__port')
            self.redis_ssl_enabled = self.env_config.get(
                'online_store__redis__ssl_enabled')
            self._construct_redis_client()

        # Offline store enabled configs; false by default
        self.s3_enabled = self.env_config.get(
            'offline_store__s3__s3_enabled')
        self.adls_enabled = self.env_config.get(
            'offline_store__adls__adls_enabled')
        self.wasb_enabled = self.env_config.get(
            'offline_store__wasb__wasb_enabled')
        self.jdbc_enabled = self.env_config.get(
            'offline_store__jdbc__jdbc_enabled')
        self.snowflake_enabled = self.env_config.get(
            'offline_store__snowflake__snowflake_enabled')
        if not (self.s3_enabled or self.adls_enabled or self.wasb_enabled or self.jdbc_enabled or self.snowflake_enabled):
            self.logger.warning("No offline storage enabled.")

        # S3 configs
        if self.s3_enabled:
            self.s3_endpoint = self.env_config.get(
                'offline_store__s3__s3_endpoint')

        # spark configs
        self.output_num_parts = self.env_config.get(
            'spark_config__spark_result_output_parts')
        self.spark_runtime = self.env_config.get(
            'spark_config__spark_cluster')

        self.credential = credential
        if self.spark_runtime not in {'azure_synapse', 'databricks', 'local'}:
            raise RuntimeError(
                f'{self.spark_runtime} is not supported. Only \'azure_synapse\', \'databricks\' and \'local\' are currently supported.')
        elif self.spark_runtime == 'azure_synapse':
            # Feathr is a spark-based application so the feathr jar compiled from source code will be used in the
            # Spark job submission. The feathr jar hosted in cloud saves the time users needed to upload the jar from
            # their local env.
            self._FEATHR_JOB_JAR_PATH = \
                self.env_config.get(
                    'spark_config__azure_synapse__feathr_runtime_location')

            if self.credential is None:
                self.credential = DefaultAzureCredential(exclude_interactive_browser_credential=False)

            self.feathr_spark_launcher = _FeathrSynapseJobLauncher(
                synapse_dev_url=self.env_config.get(
                    'spark_config__azure_synapse__dev_url'),
                pool_name=self.env_config.get(
                    'spark_config__azure_synapse__pool_name'),
                datalake_dir=self.env_config.get(
                    'spark_config__azure_synapse__workspace_dir'),
                executor_size=self.env_config.get(
                    'spark_config__azure_synapse__executor_size'),
                executors=self.env_config.get(
                    'spark_config__azure_synapse__executor_num'),
                credential=self.credential
            )
        elif self.spark_runtime == 'databricks':
            # Feathr is a spark-based application so the feathr jar compiled from source code will be used in the
            # Spark job submission. The feathr jar hosted in cloud saves the time users needed to upload the jar from
            # their local env.
            self._FEATHR_JOB_JAR_PATH = \
                self.env_config.get(
                    'spark_config__databricks__feathr_runtime_location')

            self.feathr_spark_launcher = _FeathrDatabricksJobLauncher(
                workspace_instance_url=self.env_config.get(
                    'spark_config__databricks__workspace_instance_url'),
                token_value=self.env_config.get_from_env_or_akv(
                    'DATABRICKS_WORKSPACE_TOKEN_VALUE'),
                config_template=self.env_config.get(
                    'spark_config__databricks__config_template'),
                databricks_work_dir=self.env_config.get(
                    'spark_config__databricks__work_dir')
            )
        elif self.spark_runtime == 'local':
            self._FEATHR_JOB_JAR_PATH = \
                self.env_config.get(
                    'spark_config__local__feathr_runtime_location')
            self.feathr_spark_launcher = _FeathrLocalSparkJobLauncher(
                workspace_path = self.env_config.get('spark_config__local__workspace'),
                master = self.env_config.get('spark_config__local__master')
                )


        self.secret_names = []

        # initialize config helper
        self.config_helper = FeathrConfigHelper()

        # initialize registry
        self.registry = None
        registry_endpoint = self.env_config.get('feature_registry__api_endpoint')
        azure_purview_name = self.env_config.get('feature_registry__purview__purview_name')
        if registry_endpoint:
            self.registry = _FeatureRegistry(self.project_name, endpoint=registry_endpoint, project_tags=project_registry_tag, credential=credential)
        elif azure_purview_name:
            registry_delimiter = self.env_config.get('feature_registry__purview__delimiter')
            # initialize the registry no matter whether we set purview name or not, given some of the methods are used there.
            self.registry = _PurviewRegistry(self.project_name, azure_purview_name, registry_delimiter, project_registry_tag, config_path = config_path, credential=credential)
            logger.warning("FEATURE_REGISTRY__PURVIEW__PURVIEW_NAME will be deprecated soon. Please use FEATURE_REGISTRY__API_ENDPOINT instead.")
        else:
            # no registry configured
            logger.info("Feathr registry is not configured. Consider setting the Feathr registry component for richer feature store experience.")

        logger.info(f"Feathr client {get_version()} initialized successfully.")

    def _check_required_environment_variables_exist(self):
        """Checks if the required environment variables(form feathr_config.yaml) is set.

        Some required information has to be set via environment variables so the client can work.
        """
        props = self.secret_names
        for required_field in (self.required_fields + props):
            if required_field not in os.environ:
                raise RuntimeError(f'{required_field} is not set in environment variable. All required environment '
                                   f'variables are: {self.required_fields}.')

    def register_features(self, from_context: bool = True):
        """Registers features based on the current workspace

        Args:
            from_context: If from_context is True (default), the features will be generated from the current context, with the previous built features in client.build(). Otherwise, the features will be generated from
            configuration files.
        """

        if from_context:
            # make sure those items are in `self`
            if 'anchor_list' in dir(self) and 'derived_feature_list' in dir(self):
                self.config_helper.save_to_feature_config_from_context(self.anchor_list, self.derived_feature_list, self.local_workspace_dir)
                self.registry.register_features(self.local_workspace_dir, from_context=from_context, anchor_list=self.anchor_list, derived_feature_list=self.derived_feature_list)
            else:
                raise RuntimeError("Please call FeathrClient.build_features() first in order to register features")
        else:
            self.registry.register_features(self.local_workspace_dir, from_context=from_context)

    def build_features(self, anchor_list: List[FeatureAnchor] = [], derived_feature_list: List[DerivedFeature] = [], verbose: bool = False):
        """Build features based on the current workspace. all actions that triggers a spark job will be based on the
        result of this action.
        """
        # Run necessary validations
        # anchor name and source name should be unique
        anchor_names = {}
        source_names = {}
        for anchor in anchor_list:
            if anchor.name in anchor_names:
                raise RuntimeError(f"Anchor name should be unique but there are duplicate anchor names in your anchor "
                                   f"definitions. Anchor name of {anchor} is already defined in {anchor_names[anchor.name]}")
            else:
                anchor_names[anchor.name] = anchor
            if anchor.source.name in source_names and (anchor.source is not source_names[anchor.source.name]):
                raise RuntimeError(f"Source name should be unique but there are duplicate source names in your source "
                                   f"definitions. Source name of {anchor.source} is already defined in {source_names[anchor.source.name]}")
            else:
                source_names[anchor.source.name] = anchor.source

        _PreprocessingPyudfManager.build_anchor_preprocessing_metadata(anchor_list, self.local_workspace_dir)
        self.config_helper.save_to_feature_config_from_context(anchor_list, derived_feature_list, self.local_workspace_dir)
        self.anchor_list = anchor_list
        self.derived_feature_list = derived_feature_list

        # Check if data source used by every anchor requires additional system properties to be set
        props = []
        for anchor in self.anchor_list:
            if hasattr(anchor.source, "get_required_properties"):
                props.extend(anchor.source.get_required_properties())
        # Reset `system_properties`
        self.secret_names = props

        # Pretty print anchor_list
        if verbose and self.anchor_list:
            FeaturePrinter.pretty_print_anchors(self.anchor_list)

    def get_snowflake_path(self, database: str, schema: str, dbtable: str = None, query: str = None) -> str:
        """
        Returns snowflake path given dataset location information.
        Either dbtable or query must be specified but not both.
        """
        if dbtable is not None and query is not None:
            raise RuntimeError("Both dbtable and query are specified. Can only specify one..")
        if dbtable is None and query is None:
            raise RuntimeError("One of dbtable or query must be specified..")
        if dbtable:
            return f"snowflake://snowflake_account/?sfDatabase={database}&sfSchema={schema}&dbtable={dbtable}"
        else:
            return f"snowflake://snowflake_account/?sfDatabase={database}&sfSchema={schema}&query={query}"

    def list_registered_features(self, project_name: str = None) -> List[str]:
        """List all the already registered features under the given project.
        `project_name` must not be None or empty string because it violates the RBAC policy
        """
        return self.registry.list_registered_features(project_name)

    def list_dependent_entities(self, qualified_name: str):
        """
        Lists all dependent/downstream entities for a given entity
        """
        return self.registry.list_dependent_entities(qualified_name)

    def delete_entity(self, qualified_name: str):
        """
        Deletes a single entity if it has no downstream/dependent entities
        """
        return self.registry.delete_entity(qualified_name)

    def _get_registry_client(self):
        """
        Returns registry client in case users want to perform more advanced operations
        """
        return self.registry._get_registry_client()

    def get_online_features(self, feature_table: str, key: Any, feature_names: List[str]):
        """Fetches feature value for a certain key from a online feature table.

        Args:
            feature_table: the name of the feature table.
            key: the key/key list of the entity;
                 for key list, please make sure the order is consistent with the one in feature's definition;
                 the order can be found by 'get_features_from_registry'.
            feature_names: list of feature names to fetch

        Return:
            A list of feature values for this entity. It's ordered by the requested feature names.
            For example, feature_names = ['f_is_medium_trip_distance', 'f_day_of_week', 'f_day_of_month', 'f_hour_of_day']
            then, the returned feature values is: [b'true', b'4.0', b'31.0', b'23.0'].
            If the feature_table or key doesn't exist, then a list of Nones are returned. For example,
            [None, None, None, None].
            If a feature doesn't exist, then a None is returned for that feature. For example:
            [None, b'4.0', b'31.0', b'23.0'].
            """
        redis_key = self._construct_redis_key(feature_table, key)
        res = self.redis_client.hmget(redis_key, *feature_names)
        return self._decode_proto(res)

    def multi_get_online_features(self, feature_table: str, keys: List[Any], feature_names: List[str]):
        """Fetches feature value for a list of keys from a online feature table. This is the batch version of the get API.

        Args:
            feature_table: the name of the feature table.
            keys: list of keys/composite keys for the entities;
                  for composite keys, please make sure each order of them is consistent with the one in feature's definition;
                  the order can be found by 'get_features_from_registry'.
            feature_names: list of feature names to fetch

        Return:
            A list of feature values for the requested entities. It's ordered by the requested feature names. For
            example, keys = [12, 24], feature_names = ['f_is_medium_trip_distance', 'f_day_of_week', 'f_day_of_month',
            'f_hour_of_day'] then, the returned feature values is: {'12': [b'false', b'5.0', b'1.0', b'0.0'],
            '24': [b'true', b'4.0', b'31.0', b'23.0']}. If the feature_table or key doesn't exist, then a list of Nones
            are returned. For example, {'12': [None, None, None, None], '24': [None, None, None, None]} If a feature
            doesn't exist, then a None is returned for that feature. For example: {'12': [None, b'4.0', b'31.0',
            b'23.0'], '24': [b'true', b'4.0', b'31.0', b'23.0']}.
        """
        with self.redis_client.pipeline() as redis_pipeline:
            for key in keys:
                redis_key = self._construct_redis_key(feature_table, key)
                redis_pipeline.hmget(redis_key, *feature_names)
            pipeline_result = redis_pipeline.execute()

        decoded_pipeline_result = []
        for feature_list in pipeline_result:
            decoded_pipeline_result.append(self._decode_proto(feature_list))
        for i in range(len(keys)):
            if isinstance(keys[i], List):
                keys[i] = self._COMPOSITE_KEY_SEPARATOR.join(keys[i])
        return dict(zip(keys, decoded_pipeline_result))

    def _decode_proto(self, feature_list):
        """Decode the bytes(in string form) via base64 decoder. For dense array, it will be returned as Python List.
        For sparse array, it will be returned as tuple of index array and value array. The order of elements in the
        arrays won't be changed.
        """
        typed_result = []
        for raw_feature in feature_list:
            if raw_feature:
                feature_value = FeatureValue()
                decoded = base64.b64decode(raw_feature)
                feature_value.ParseFromString(decoded)
                if feature_value.WhichOneof('FeatureValueOneOf') == 'boolean_value':
                    typed_result.append(feature_value.boolean_value)
                elif feature_value.WhichOneof('FeatureValueOneOf') == 'string_value':
                    typed_result.append(feature_value.string_value)
                elif feature_value.WhichOneof('FeatureValueOneOf') == 'float_value':
                    typed_result.append(feature_value.float_value)
                elif feature_value.WhichOneof('FeatureValueOneOf') == 'double_value':
                    typed_result.append(feature_value.double_value)
                elif feature_value.WhichOneof('FeatureValueOneOf') == 'int_value':
                    typed_result.append(feature_value.int_value)
                elif feature_value.WhichOneof('FeatureValueOneOf') == 'long_value':
                    typed_result.append(feature_value.long_value)
                elif feature_value.WhichOneof('FeatureValueOneOf') == 'int_array':
                    typed_result.append(feature_value.int_array.integers)
                elif feature_value.WhichOneof('FeatureValueOneOf') == 'string_array':
                    typed_result.append(feature_value.string_array.strings)
                elif feature_value.WhichOneof('FeatureValueOneOf') == 'float_array':
                    typed_result.append(feature_value.float_array.floats)
                elif feature_value.WhichOneof('FeatureValueOneOf') == 'double_array':
                    typed_result.append(feature_value.double_array.doubles)
                elif feature_value.WhichOneof('FeatureValueOneOf') == 'boolean_array':
                    typed_result.append(feature_value.boolean_array.booleans)
                elif feature_value.WhichOneof('FeatureValueOneOf') == 'sparse_string_array':
                    typed_result.append((feature_value.sparse_string_array.index_integers, feature_value.sparse_string_array.value_strings))
                elif feature_value.WhichOneof('FeatureValueOneOf') == 'sparse_bool_array':
                    typed_result.append((feature_value.sparse_bool_array.index_integers, feature_value.sparse_bool_array.value_booleans))
                elif feature_value.WhichOneof('FeatureValueOneOf') == 'sparse_float_array':
                    typed_result.append((feature_value.sparse_float_array.index_integers, feature_value.sparse_float_array.value_floats))
                elif feature_value.WhichOneof('FeatureValueOneOf') == 'sparse_double_array':
                    typed_result.append((feature_value.sparse_double_array.index_integers, feature_value.sparse_double_array.value_doubles))
                elif feature_value.WhichOneof('FeatureValueOneOf') == 'sparse_long_array':
                    typed_result.append((feature_value.sparse_long_array.index_integers, feature_value.sparse_long_array.value_longs))
                else:
                    self.logger.debug("Fail to load the feature type. Maybe a new type that is not supported by this "
                                      "client version")
                    self.logger.debug(f"The raw feature is {raw_feature}.")
                    self.logger.debug(f"The loaded feature is {feature_value}")
                    typed_result.append(None)
            else:
                typed_result.append(raw_feature)
        return typed_result

    def delete_feature_from_redis(self, feature_table, key, feature_name) -> None:
        """
        Delete feature from Redis

        Args:
            feature_table: the name of the feature table
            key: the key of the entity
            feature_name: feature name to be deleted
        """

        redis_key = self._construct_redis_key(feature_table, key)
        if self.redis_client.hexists(redis_key, feature_name):
            self.redis_client.delete(redis_key, feature_name)
            print(f'Deletion successful. {feature_name} is deleted from Redis.')
        else:
            raise RuntimeError(f'Deletion failed. {feature_name} not found in Redis.')

    def _clean_test_data(self, feature_table):
        """
        WARNING: THIS IS ONLY USED FOR TESTING
        Clears a namespace in redis cache.
        This may be very time consuming.

        Args:
          feature_table: str, feature_table i.e your prefix before the separator in the Redis database.
        """
        cursor = '0'
        ns_keys = feature_table + '*'
        while cursor != 0:
            # 5000 count at a scan seems reasonable faster for our testing data
            cursor, keys = self.redis_client.scan(
                cursor=cursor, match=ns_keys, count=5000)
            if keys:
                self.redis_client.delete(*keys)

    def _construct_redis_key(self, feature_table, key):
        if isinstance(key, List):
            key = self._COMPOSITE_KEY_SEPARATOR.join(key)
        return feature_table + self._KEY_SEPARATOR + key

    def _str_to_bool(self, s: str, variable_name = None):
        """Define a function to detect convert string to bool, since Redis client sometimes require a bool and sometimes require a str
        """
        if (isinstance(s, str) and s.casefold() == 'True'.casefold()) or s == True:
            return True
        elif (isinstance(s, str) and s.casefold() == 'False'.casefold()) or s == False:
            return False
        else:
            self.logger.warning(f'{s} is not a valid Bool value. Maybe you want to double check if it is set correctly for {variable_name}.')
            return s

    def _construct_redis_client(self):
        """Constructs the Redis client. The host, port, credential and other parameters can be set via environment
        parameters.
        """
        password = self.env_config.get_from_env_or_akv(REDIS_PASSWORD)
        host = self.redis_host
        port = self.redis_port
        ssl_enabled = self.redis_ssl_enabled
        self.redis_client = redis.Redis(
            host=host,
            port=port,
            password=password,
            ssl=self._str_to_bool(ssl_enabled, "ssl_enabled"))
        self.logger.info('Redis connection is successful and completed.')

    def get_offline_features(self,
                             observation_settings: ObservationSettings,
                             feature_query: Union[FeatureQuery, List[FeatureQuery]],
                             output_path: Union[str, Sink],
                             execution_configurations: Union[SparkExecutionConfiguration ,Dict[str,str]] = {},
                             config_file_name:str = "feature_join_conf/feature_join.conf",
                             dataset_column_names: Set[str] = None,
                             verbose: bool = False
                             ):
        """
        Get offline features for the observation dataset
        Args:
            observation_settings: settings of the observation data, e.g. timestamp columns, input path, etc.
            feature_query: features that are requested to add onto the observation data
            output_path: output path of job, i.e. the observation data with features attached.
            execution_configurations: a dict that will be passed to spark job when the job starts up, i.e. the "spark configurations". Note that not all of the configuration will be honored since some of the configurations are managed by the Spark platform, such as Databricks or Azure Synapse. Refer to the [spark documentation](https://spark.apache.org/docs/latest/configuration.html) for a complete list of spark configurations.
            config_file_name: the name of the config file that will be passed to the spark job. The config file is used to configure the spark job. The default value is "feature_join_conf/feature_join.conf".
            dataset_column_names: column names of observation data set. Will be used to check conflicts with feature names if cannot get real column names from observation data set.
        """
        feature_queries = feature_query if isinstance(feature_query, List) else [feature_query]
        feature_names = []
        for feature_query in feature_queries:
            for feature_name in feature_query.feature_list:
                feature_names.append(feature_name)
        
        if len(feature_names) > 0 and observation_settings.conflicts_auto_correction is None:
            import feathr.utils.job_utils as job_utils
            dataset_column_names_from_path = job_utils.get_cloud_file_column_names(self, observation_settings.observation_path, observation_settings.file_format,observation_settings.is_file_path)
            if (dataset_column_names_from_path is None or len(dataset_column_names_from_path) == 0) and dataset_column_names is None:
                self.logger.warning(f"Feathr is unable to read the Observation data from {observation_settings.observation_path} due to permission issue or invalid path. Please either grant the permission or supply the observation column names in the filed: observation_column_names.")
            else:
                if dataset_column_names_from_path is not None and len(dataset_column_names_from_path) > 0:
                    dataset_column_names = dataset_column_names_from_path
                conflict_names = []
                for feature_name in feature_names:
                    if feature_name in dataset_column_names:
                        conflict_names.append(feature_name)
                if len(conflict_names) != 0:
                    conflict_names = ",".join(conflict_names)
                    raise RuntimeError(f"Feature names exist conflicts with dataset column names: {conflict_names}")
           
        udf_files = _PreprocessingPyudfManager.prepare_pyspark_udf_files(feature_names, self.local_workspace_dir)

        # produce join config
        tm = Template("""
            {{observation_settings.to_feature_config()}}
            featureList: [
                {% for list in feature_lists %}
                    {{list.to_feature_config()}}
                {% endfor %}
            ]
            outputPath: "{{output_path}}"
        """)
        config = tm.render(feature_lists=feature_queries, observation_settings=observation_settings, output_path=output_path)
        config_file_path = os.path.join(self.local_workspace_dir, config_file_name)

        # make sure `FeathrClient.build_features()` is called before getting offline features/materialize features
        # otherwise users will be confused on what are the available features
        # in build_features it will assign anchor_list and derived_feature_list variable, hence we are checking if those two variables exist to make sure the above condition is met
        if 'anchor_list' in dir(self) and 'derived_feature_list' in dir(self):
            self.config_helper.save_to_feature_config_from_context(self.anchor_list, self.derived_feature_list, self.local_workspace_dir)
        else:
            raise RuntimeError("Please call FeathrClient.build_features() first in order to get offline features")

        # Pretty print feature_query
        if verbose and feature_query:
            FeaturePrinter.pretty_print_feature_query(feature_query)

        write_to_file(content=config, full_file_name=config_file_path)
        return self._get_offline_features_with_config(config_file_path,
                                                      output_path=output_path,
                                                      execution_configurations=execution_configurations,
                                                      udf_files=udf_files)

    def _get_offline_features_with_config(self,
                                          feature_join_conf_path='feature_join_conf/feature_join.conf',
                                          output_path: Union[str, Sink] = "",
                                          execution_configurations: Dict[str,str] = {},
                                          udf_files=[]):
        """Joins the features to your offline observation dataset based on the join config.

        Args:
          feature_join_conf_path: Relative path to your feature join config file.
        """
        cloud_udf_paths = [self.feathr_spark_launcher.upload_or_get_cloud_path(udf_local_path) for udf_local_path in udf_files]
        feathr_feature = ConfigFactory.parse_file(feature_join_conf_path)

        feature_join_job_params = FeatureJoinJobParams(join_config_path=os.path.abspath(feature_join_conf_path),
                                                       observation_path=feathr_feature['observationPath'],
                                                       feature_config=os.path.join(self.local_workspace_dir, 'feature_conf/'),
                                                       job_output_path=output_path)
        job_tags = { OUTPUT_PATH_TAG: feature_join_job_params.job_output_path }
        # set output format in job tags if it's set by user, so that it can be used to parse the job result in the helper function
        if execution_configurations is not None and OUTPUT_FORMAT in execution_configurations:
            job_tags[OUTPUT_FORMAT] = execution_configurations[OUTPUT_FORMAT]
        else:
            job_tags[OUTPUT_FORMAT] = "avro"
        '''
        - Job tags are for job metadata and it's not passed to the actual spark job (i.e. not visible to spark job), more like a platform related thing that Feathr want to add (currently job tags only have job output URL and job output format, ). They are carried over with the job and is visible to every Feathr client. Think this more like some customized metadata for the job which would be weird to be put in the spark job itself.
        - Job arguments (or sometimes called job parameters)are the arguments which are command line arguments passed into the actual spark job. This is usually highly related with the spark job. In Feathr it's like the input to the scala spark CLI. They are usually not spark specific (for example if we want to specify the location of the feature files, or want to
        - Job configuration are like "configurations" for the spark job and are usually spark specific. For example, we want to control the no. of write parts for spark
        Job configurations and job arguments (or sometimes called job parameters) have quite some overlaps (i.e. you can achieve the same goal by either using the job arguments/parameters vs. job configurations). But the job tags should just be used for metadata purpose.
        '''

        # submit the jars
        return self.feathr_spark_launcher.submit_feathr_job(
            job_name=self.project_name + '_feathr_feature_join_job',
            main_jar_path=self._FEATHR_JOB_JAR_PATH,
            python_files=cloud_udf_paths,
            job_tags=job_tags,
            main_class_name=JOIN_CLASS_NAME,
            arguments= [
                '--join-config', self.feathr_spark_launcher.upload_or_get_cloud_path(
                    feature_join_job_params.join_config_path),
                '--input', feature_join_job_params.observation_path,
                '--output', feature_join_job_params.job_output_path,
                '--feature-config', self.feathr_spark_launcher.upload_or_get_cloud_path(
                    feature_join_job_params.feature_config),
                '--num-parts', self.output_num_parts
            ]+self._get_offline_storage_arguments(),
            reference_files_path=[],
            configuration=execution_configurations,
            properties=self._collect_secrets(feature_join_job_params.secrets)
        )

    def _get_offline_storage_arguments(self):
        arguments = []
        if self.s3_enabled:
            arguments.append('--s3-config')
            arguments.append(self._get_s3_config_str())
        if self.adls_enabled:
            arguments.append('--adls-config')
            arguments.append(self._get_adls_config_str())
        if self.wasb_enabled:
            arguments.append('--blob-config')
            arguments.append(self._get_blob_config_str())
        if self.jdbc_enabled:
            arguments.append('--sql-config')
            arguments.append(self._get_sql_config_str())
        if self.snowflake_enabled:
            arguments.append('--snowflake-config')
            arguments.append(self._get_snowflake_config_str())
        return arguments

    def get_job_result_uri(self, block=True, timeout_sec=300) -> str:
        """Gets the job output URI
        """
        if not block:
            return self.feathr_spark_launcher.get_job_result_uri()
        # Block the API by pooling the job status and wait for complete
        if self.feathr_spark_launcher.wait_for_completion(timeout_sec):
            return self.feathr_spark_launcher.get_job_result_uri()
        else:
            raise RuntimeError(
                'Spark job failed so output cannot be retrieved.')

    def get_job_tags(self) -> Dict[str, str]:
        """Gets the job tags
        """
        return self.feathr_spark_launcher.get_job_tags()

    def wait_job_to_finish(self, timeout_sec: int = 300):
        """Waits for the job to finish in a blocking way unless it times out
        """
        if self.feathr_spark_launcher.wait_for_completion(timeout_sec):
            return
        else:
            raise RuntimeError('Spark job failed.')

    def monitor_features(self, settings: MonitoringSettings, execution_configurations: Union[SparkExecutionConfiguration ,Dict[str,str]] = {}, verbose: bool = False):
        """Create a offline job to generate statistics to monitor feature data

        Args:
            settings: Feature monitoring settings
            execution_configurations: a dict that will be passed to spark job when the job starts up, i.e. the "spark configurations". Note that not all of the configuration will be honored since some of the configurations are managed by the Spark platform, such as Databricks or Azure Synapse. Refer to the [spark documentation](https://spark.apache.org/docs/latest/configuration.html) for a complete list of spark configurations.
        """
        self.materialize_features(settings, execution_configurations, verbose)

    # Get feature keys given the name of a feature
    # Should search in both 'derived_feature_list' and 'anchor_list'
    # Return related keys(key_column list) or None if cannot find the feature
    def _get_feature_key(self, feature_name: str):
        features = []
        if 'derived_feature_list' in dir(self):
            features += self.derived_feature_list
        if 'anchor_list' in dir(self):
            for anchor in self.anchor_list:
                features += anchor.features
        for feature in features:
            if feature.name == feature_name:
                keys = feature.key
                return set(key.key_column for key in keys)
        self.logger.warning(f"Invalid feature name: {feature_name}. Please call FeathrClient.build_features() first in order to materialize the features.")
        return None

    # Validation on feature keys:
    # Features within a set of aggregation or planned to be merged should have same keys
    # The param "allow_empty_key" shows if empty keys are acceptable
    def _valid_materialize_keys(self, features: List[str], allow_empty_key=False):
        keys = None
        for feature in features:
            new_keys = self._get_feature_key(feature)
            if new_keys is None:
                self.logger.error(f"Key of feature: {feature} is empty. Please confirm the feature is defined. In addition, if this feature is not from INPUT_CONTEXT, you might want to double check on the feature definition to see whether the key is empty or not.")
                return False
            # If only get one key and it's "NOT_NEEDED", it means the feature has an empty key.
            if ','.join(new_keys) == "NOT_NEEDED" and not allow_empty_key:
                self.logger.error(f"Empty feature key is not allowed for features: {features}")
                return False
            if keys is None:
                keys = copy.deepcopy(new_keys)
            else:
                if len(keys) != len(new_keys):
                    self.logger.error(f"Inconsistent feature keys. Current keys are {str(keys)}")
                    return False
                for new_key in new_keys:
                    if new_key not in keys:
                        self.logger.error(f"Inconsistent feature keys. Current keys are {str(keys)}")
                        return False
        return True

    def materialize_features(self, settings: MaterializationSettings, execution_configurations: Union[SparkExecutionConfiguration ,Dict[str,str]] = {}, verbose: bool = False, allow_materialize_non_agg_feature: bool = False):
        """Materialize feature data

        Args:
            settings: Feature materialization settings
            execution_configurations: a dict that will be passed to spark job when the job starts up, i.e. the "spark configurations". Note that not all of the configuration will be honored since some of the configurations are managed by the Spark platform, such as Databricks or Azure Synapse. Refer to the [spark documentation](https://spark.apache.org/docs/latest/configuration.html) for a complete list of spark configurations.
            allow_materialize_non_agg_feature: Materializing non-aggregated features (the features without WindowAggTransformation) doesn't output meaningful results so it's by default set to False, but if you really want to materialize non-aggregated features, set this to True.
        """
        feature_list = settings.feature_names
        if len(feature_list) > 0:
            if 'anchor_list' in dir(self):
                anchors = [anchor for anchor in self.anchor_list if isinstance(anchor.source, InputContext)]
                anchor_feature_names = set(feature.name for anchor in anchors for feature in anchor.features)
                for feature in feature_list:
                    if feature in anchor_feature_names:
                        raise RuntimeError(f"Materializing features that are defined on INPUT_CONTEXT is not supported. {feature} is defined on INPUT_CONTEXT so you should remove it from the feature list in MaterializationSettings.")
            if not self._valid_materialize_keys(feature_list):
                raise RuntimeError(f"Invalid materialization features: {feature_list}, since they have different keys or they are not defined. Currently Feathr only supports materializing features of the same keys.")

        if not allow_materialize_non_agg_feature:
            # Check if there are non-aggregation features in the list
            for fn in feature_list:
                # Check over anchor features
                for anchor in self.anchor_list:
                    for feature in anchor.features:
                        if feature.name == fn and not isinstance(feature.transform, WindowAggTransformation):
                            raise RuntimeError(f"Feature {fn} is not an aggregation feature. Currently Feathr only supports materializing aggregation features. If you want to materialize {fn}, please set allow_materialize_non_agg_feature to True.")
                # Check over derived features
                for feature in self.derived_feature_list:
                    if feature.name == fn and not isinstance(feature.transform, WindowAggTransformation):
                        raise RuntimeError(f"Feature {fn} is not an aggregation feature. Currently Feathr only supports materializing aggregation features. If you want to materialize {fn}, please set allow_materialize_non_agg_feature to True.")

        # Collect secrets from sinks. Get output_path as well if the sink is offline sink (HdfsSink) for later use.
        secrets = []
        output_path = None
        for sink in settings.sinks:
            if hasattr(sink, "get_required_properties"):
                secrets.extend(sink.get_required_properties())
            if isinstance(sink, HdfsSink):
                # Note, for now we only cache one output path from one of HdfsSinks (if one passed multiple sinks).
                output_path = sink.output_path

        results = []
        # produce materialization config
        for end in settings.get_backfill_cutoff_time():
            settings.backfill_time.end = end
            config = _to_materialization_config(settings)
            config_file_name = "feature_gen_conf/auto_gen_config_{}.conf".format(end.timestamp())
            config_file_path = os.path.join(self.local_workspace_dir, config_file_name)
            write_to_file(content=config, full_file_name=config_file_path)

            # make sure `FeathrClient.build_features()` is called before getting offline features/materialize features in the python SDK
            # otherwise users will be confused on what are the available features
            # in build_features it will assign anchor_list and derived_feature_list variable, hence we are checking if those two variables exist to make sure the above condition is met
            if 'anchor_list' in dir(self) and 'derived_feature_list' in dir(self):
                self.config_helper.save_to_feature_config_from_context(self.anchor_list, self.derived_feature_list, self.local_workspace_dir)
            else:
                raise RuntimeError("Please call FeathrClient.build_features() first in order to materialize the features")

            udf_files = _PreprocessingPyudfManager.prepare_pyspark_udf_files(settings.feature_names, self.local_workspace_dir)
            # CLI will directly call this so the experience won't be broken
            result = self._materialize_features_with_config(
                feature_gen_conf_path=config_file_path,
                execution_configurations=execution_configurations,
                udf_files=udf_files,
                secrets=secrets,
                output_path=output_path,
            )
            if os.path.exists(config_file_path) and self.spark_runtime != 'local':
                os.remove(config_file_path)
            results.append(result)

        # Pretty print feature_names of materialized features
        if verbose and settings:
            FeaturePrinter.pretty_print_materialize_features(settings)

        return results

    def _materialize_features_with_config(
        self,
        feature_gen_conf_path: str = 'feature_gen_conf/feature_gen.conf',
        execution_configurations: Dict[str,str] = {},
        udf_files: List = [],
        secrets: List = [],
        output_path: str = None,
    ):
        """Materializes feature data based on the feature generation config. The feature
        data will be materialized to the destination specified in the feature generation config.

        Args
            feature_gen_conf_path: Relative path to the feature generation config you want to materialize.
            execution_configurations: Spark job execution configurations.
            udf_files: UDF files.
            secrets: Secrets to access sinks.
            output_path: The output path of the materialized features when using an offline sink.
        """
        cloud_udf_paths = [self.feathr_spark_launcher.upload_or_get_cloud_path(udf_local_path) for udf_local_path in udf_files]

        # Read all features conf
        generation_config = FeatureGenerationJobParams(
            generation_config_path=os.path.abspath(feature_gen_conf_path),
            feature_config=os.path.join(self.local_workspace_dir, "feature_conf/"))

        # When using offline sink (i.e. output_path is not None)
        job_tags = {}
        if output_path:
            job_tags[OUTPUT_PATH_TAG] = output_path
            # set output format in job tags if it's set by user, so that it can be used to parse the job result in the helper function
            if execution_configurations is not None and OUTPUT_FORMAT in execution_configurations:
                job_tags[OUTPUT_FORMAT] = execution_configurations[OUTPUT_FORMAT]
            else:
                job_tags[OUTPUT_FORMAT] = "avro"
        '''
        - Job tags are for job metadata and it's not passed to the actual spark job (i.e. not visible to spark job), more like a platform related thing that Feathr want to add (currently job tags only have job output URL and job output format, ). They are carried over with the job and is visible to every Feathr client. Think this more like some customized metadata for the job which would be weird to be put in the spark job itself.
        - Job arguments (or sometimes called job parameters)are the arguments which are command line arguments passed into the actual spark job. This is usually highly related with the spark job. In Feathr it's like the input to the scala spark CLI. They are usually not spark specific (for example if we want to specify the location of the feature files, or want to
        - Job configuration are like "configurations" for the spark job and are usually spark specific. For example, we want to control the no. of write parts for spark
        Job configurations and job arguments (or sometimes called job parameters) have quite some overlaps (i.e. you can achieve the same goal by either using the job arguments/parameters vs. job configurations). But the job tags should just be used for metadata purpose.
        '''

        optional_params = []
        if self.env_config.get_from_env_or_akv('KAFKA_SASL_JAAS_CONFIG'):
            optional_params = optional_params + ['--kafka-config', self._get_kafka_config_str()]
        arguments = [
                '--generation-config', self.feathr_spark_launcher.upload_or_get_cloud_path(
                generation_config.generation_config_path),
                # Local Config, comma seperated file names
                '--feature-config', self.feathr_spark_launcher.upload_or_get_cloud_path(
                generation_config.feature_config),
                '--redis-config', self._getRedisConfigStr(),
            ] + self._get_offline_storage_arguments()+optional_params
        monitoring_config_str = self._get_monitoring_config_str()
        if monitoring_config_str:
            arguments.append('--monitoring-config')
            arguments.append(monitoring_config_str)
        return self.feathr_spark_launcher.submit_feathr_job(
            job_name=self.project_name + '_feathr_feature_materialization_job',
            main_jar_path=self._FEATHR_JOB_JAR_PATH,
            python_files=cloud_udf_paths,
            job_tags=job_tags,
            main_class_name=GEN_CLASS_NAME,
            arguments=arguments,
            reference_files_path=[],
            configuration=execution_configurations,
            properties=self._collect_secrets(secrets)
        )

    def wait_job_to_finish(self, timeout_sec: int = 300):
        """Waits for the job to finish in a blocking way unless it times out
        """
        if self.feathr_spark_launcher.wait_for_completion(timeout_sec):
            return
        else:
            raise RuntimeError('Spark job failed.')

    def _getRedisConfigStr(self):
        """Construct the Redis config string. The host, port, credential and other parameters can be set via environment
        variables."""
        password = self.env_config.get_from_env_or_akv(REDIS_PASSWORD)
        host = self.redis_host
        port = self.redis_port
        ssl_enabled = self.redis_ssl_enabled
        config_str = """
        REDIS_PASSWORD: "{REDIS_PASSWORD}"
        REDIS_HOST: "{REDIS_HOST}"
        REDIS_PORT: {REDIS_PORT}
        REDIS_SSL_ENABLED: {REDIS_SSL_ENABLED}
        """.format(REDIS_PASSWORD=password, REDIS_HOST=host, REDIS_PORT=port, REDIS_SSL_ENABLED=str(ssl_enabled))
        return self._reshape_config_str(config_str)

    def _get_s3_config_str(self):
        """Construct the S3 config string. The endpoint, access key, secret key, and other parameters can be set via
        environment variables."""
        endpoint = self.s3_endpoint
        # if s3 endpoint is set in the feathr_config, then we need other environment variables
        # keys can't be only accessed through environment
        access_key = self.env_config.get_from_env_or_akv('S3_ACCESS_KEY')
        secret_key = self.env_config.get_from_env_or_akv('S3_SECRET_KEY')
        # HOCON format will be parsed by the Feathr job
        config_str = """
            S3_ENDPOINT: {S3_ENDPOINT}
            S3_ACCESS_KEY: "{S3_ACCESS_KEY}"
            S3_SECRET_KEY: "{S3_SECRET_KEY}"
            """.format(S3_ENDPOINT=endpoint, S3_ACCESS_KEY=access_key, S3_SECRET_KEY=secret_key)
        return self._reshape_config_str(config_str)

    def _get_adls_config_str(self):
        """Construct the ADLS config string for abfs(s). The Account, access key and other parameters can be set via
        environment variables."""
        account = self.env_config.get_from_env_or_akv('ADLS_ACCOUNT')
        # if ADLS Account is set in the feathr_config, then we need other environment variables
        # keys can't be only accessed through environment
        key = self.env_config.get_from_env_or_akv('ADLS_KEY')
        # HOCON format will be parsed by the Feathr job
        config_str = """
            ADLS_ACCOUNT: {ADLS_ACCOUNT}
            ADLS_KEY: "{ADLS_KEY}"
            """.format(ADLS_ACCOUNT=account, ADLS_KEY=key)
        return self._reshape_config_str(config_str)

    def _get_blob_config_str(self):
        """Construct the Blob config string for wasb(s). The Account, access key and other parameters can be set via
        environment variables."""
        account = self.env_config.get_from_env_or_akv('BLOB_ACCOUNT')
        # if BLOB Account is set in the feathr_config, then we need other environment variables
        # keys can't be only accessed through environment
        key = self.env_config.get_from_env_or_akv('BLOB_KEY')
        # HOCON format will be parsed by the Feathr job
        config_str = """
            BLOB_ACCOUNT: {BLOB_ACCOUNT}
            BLOB_KEY: "{BLOB_KEY}"
            """.format(BLOB_ACCOUNT=account, BLOB_KEY=key)
        return self._reshape_config_str(config_str)

    def _get_sql_config_str(self):
        """Construct the SQL config string for jdbc. The dbtable (query), user, password and other parameters can be set via
        environment variables."""
        table = self.env_config.get_from_env_or_akv('JDBC_TABLE')
        user = self.env_config.get_from_env_or_akv('JDBC_USER')
        password = self.env_config.get_from_env_or_akv('JDBC_PASSWORD')
        driver = self.env_config.get_from_env_or_akv('JDBC_DRIVER')
        auth_flag = self.env_config.get_from_env_or_akv('JDBC_AUTH_FLAG')
        token = self.env_config.get_from_env_or_akv('JDBC_TOKEN')
        # HOCON format will be parsed by the Feathr job
        config_str = """
            JDBC_TABLE: {JDBC_TABLE}
            JDBC_USER: {JDBC_USER}
            JDBC_PASSWORD: {JDBC_PASSWORD}
            JDBC_DRIVER: {JDBC_DRIVER}
            JDBC_AUTH_FLAG: {JDBC_AUTH_FLAG}
            JDBC_TOKEN: {JDBC_TOKEN}
            """.format(JDBC_TABLE=table, JDBC_USER=user, JDBC_PASSWORD=password, JDBC_DRIVER = driver, JDBC_AUTH_FLAG = auth_flag, JDBC_TOKEN = token)
        return self._reshape_config_str(config_str)

    def _get_monitoring_config_str(self):
        """Construct monitoring-related config string."""
        url = self.env_config.get('monitoring__database__sql__url')
        user = self.env_config.get('monitoring__database__sql__user')
        password = self.env_config.get_from_env_or_akv('MONITORING_DATABASE_SQL_PASSWORD')
        if url:
            # HOCON format will be parsed by the Feathr job
            config_str = """
                MONITORING_DATABASE_SQL_URL: "{url}"
                MONITORING_DATABASE_SQL_USER: {user}
                MONITORING_DATABASE_SQL_PASSWORD: {password}
                """.format(url=url, user=user, password=password)
            return self._reshape_config_str(config_str)
        else:
            ""

    def _get_snowflake_config_str(self):
        """Construct the Snowflake config string for jdbc. The url, user, role and other parameters can be set via
        yaml config. Password can be set via environment variables."""
        sf_url = self.env_config.get('offline_store__snowflake__url')
        sf_user = self.env_config.get('offline_store__snowflake__user')
        sf_role = self.env_config.get('offline_store__snowflake__role')
        sf_warehouse = self.env_config.get('offline_store__snowflake__warehouse')
        sf_password = self.env_config.get_from_env_or_akv('JDBC_SF_PASSWORD')
        # HOCON format will be parsed by the Feathr job
        config_str = """
            JDBC_SF_URL: {JDBC_SF_URL}
            JDBC_SF_USER: {JDBC_SF_USER}
            JDBC_SF_ROLE: {JDBC_SF_ROLE}
            JDBC_SF_WAREHOUSE: {JDBC_SF_WAREHOUSE}
            JDBC_SF_PASSWORD: {JDBC_SF_PASSWORD}
            """.format(JDBC_SF_URL=sf_url, JDBC_SF_USER=sf_user, JDBC_SF_PASSWORD=sf_password, JDBC_SF_ROLE=sf_role, JDBC_SF_WAREHOUSE=sf_warehouse)
        return self._reshape_config_str(config_str)

    def _get_kafka_config_str(self):
        """Construct the Kafka config string. The endpoint, access key, secret key, and other parameters can be set via
        environment variables."""
        sasl = self.env_config.get_from_env_or_akv('KAFKA_SASL_JAAS_CONFIG')
        # HOCON format will be parsed by the Feathr job
        config_str = """
            KAFKA_SASL_JAAS_CONFIG: "{sasl}"
            """.format(sasl=sasl)
        return self._reshape_config_str(config_str)

    def _collect_secrets(self, additional_secrets=[]):
        """Collect all values corresponding to the secret names."""
        prop_and_value = {}
        for prop in self.secret_names + additional_secrets:
            prop = prop.upper()
            prop_and_value[prop] = self.env_config.get(prop)
        return prop_and_value

    def get_features_from_registry(self, project_name: str, return_keys: bool = False, verbose: bool = False) -> Union[Dict[str, FeatureBase], Tuple[Dict[str, FeatureBase], Dict[str, Union[TypedKey, List[TypedKey]]]]]:
        """
        Get feature from registry by project name. The features got from registry are automatically built.
        """
        registry_anchor_list, registry_derived_feature_list = self.registry.get_features_from_registry(project_name)
        self.build_features(registry_anchor_list, registry_derived_feature_list)
        feature_dict = {}
        key_dict = {}
        # add those features into a dict for easier lookup
        if verbose and registry_anchor_list:
            logger.info("Get anchor features from registry: ")
        for anchor in registry_anchor_list:
            for feature in anchor.features:
                feature_dict[feature.name] = feature
                key_dict[feature.name] = feature.key
                if verbose:
                    logger.info(json.dumps(feature_to_def(feature), indent=2))
        if verbose and registry_derived_feature_list:
            logger.info("Get derived features from registry: ")
        for feature in registry_derived_feature_list:
                feature_dict[feature.name] = feature
                key_dict[feature.name] = feature.key
                if verbose:
                    logger.info(json.dumps(derived_feature_to_def(feature), indent=2))
        if return_keys:
            return feature_dict, key_dict
        return feature_dict

    def _reshape_config_str(self, config_str:str):
        if self.spark_runtime == 'local':
            return "'{" + config_str + "}'"
        else:
            return config_str
 