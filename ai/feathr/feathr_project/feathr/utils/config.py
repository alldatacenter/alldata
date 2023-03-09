import collections.abc
from copy import deepcopy
import os
import json
from tempfile import NamedTemporaryFile
from typing import Dict
import yaml

from feathr.utils.platform import is_databricks


DEFAULT_FEATHR_CONFIG = {
    "api_version": 1,
    "project_config": {},  # "project_name"
    "feature_registry": {},  # "api_endpoint"
    "spark_config": {
        "spark_cluster": "local",  # Currently support 'azure_synapse', 'databricks', and 'local'
        "spark_result_output_parts": "1",
    },
    "offline_store": {
        "adls": {"adls_enabled": "true"},
        "wasb": {"wasb_enabled": "true"},
    },
    "online_store": {
        "redis": {
            # "host"
            "port": "6380",
            "ssl_enabled": "true",
        }
    }
}

# New databricks job cluster config
DEFAULT_DATABRICKS_CLUSTER_CONFIG = {
    "spark_version": "11.2.x-scala2.12",
    "node_type_id": "Standard_DS3_v2",  # Change this if necessary
    "num_workers": 1,
    "spark_conf": {
        "FEATHR_FILL_IN": "FEATHR_FILL_IN",
        # Exclude conflicting packages if use feathr <= v0.8.0:
        "spark.jars.excludes": "commons-logging:commons-logging,org.slf4j:slf4j-api,com.google.protobuf:protobuf-java,javax.xml.bind:jaxb-api",
    },
}

# New Azure Synapse spark pool config
DEFAULT_AZURE_SYNAPSE_SPARK_POOL_CONFIG = {
    "executor_size": "Small",
    "executor_num": 1,
}


def generate_config(
    resource_prefix: str,
    project_name: str,
    output_filepath: str = None,
    databricks_workspace_token_value: str = None,
    databricks_cluster_id: str = None,
    redis_password: str = None,
    adls_key: str = None,
    **kwargs,
) -> str:
    """Generate a feathr config yaml file. Note, Feathr client will try to read environment variables first before read the config file.
    See details from the FeathrClient docstrings.

    Note:
        This utility function assumes Azure resources are deployed using the Azure Resource Manager (ARM) template,
        and infers resource names based on the given `resource_prefix`. If you deploy resources manually, you may need
        to pass each resource url manually, e.g. `spark_config__azure_synapse__dev_url="your-resource-url"`.

    Args:
        resource_prefix: Resource name prefix used when deploying Feathr resources by using ARM template.
        project_name: Feathr project name.
        output_filepath (optional): Output filepath.
        databricks_workspace_token_value (optional): Databricks workspace token. If provided, the value will be stored
            as the environment variable.
        databricks_cluster_id (optional): Databricks cluster id to use an existing cluster.
        redis_password (optional): Redis password. If provided, the value will be stored as the environment variable.
        adls_key (optional): ADLS key. If provided, the value will be stored as the environment variable.
        **kwargs: Keyword arguments to update the config. Keyword arguments follow the same naming convention as
            the feathr config. E.g. to set Databricks as the target cluster,
            use `spark_config__spark_cluster="databricks"`.
            See https://feathr-ai.github.io/feathr/quickstart_synapse.html#step-4-update-feathr-config for more details.

    Returns:
        str: Generated config file path. This will be identical to `output_filepath` if provided.
    """
    # Set keys
    if databricks_workspace_token_value:
        os.environ["DATABRICKS_WORKSPACE_TOKEN_VALUE"] = databricks_workspace_token_value
    if redis_password:
        os.environ["REDIS_PASSWORD"] = redis_password
    if adls_key:
        os.environ["ADLS_KEY"] = adls_key

    # Set configs
    config = deepcopy(DEFAULT_FEATHR_CONFIG)

    # Maybe update configs with environment variables
    _maybe_update_config_with_env_var(config, "SPARK_CONFIG__SPARK_CLUSTER")
    _maybe_update_config_with_env_var(config, "SPARK_CONFIG__AZURE_SYNAPSE__DEV_URL")
    _maybe_update_config_with_env_var(config, "SPARK_CONFIG__AZURE_SYNAPSE__POOL_NAME")
    _maybe_update_config_with_env_var(config, "SPARK_CONFIG__AZURE_SYNAPSE__WORKSPACE_DIR")
    _maybe_update_config_with_env_var(config, "SPARK_CONFIG__DATABRICKS__WORK_DIR")
    _maybe_update_config_with_env_var(config, "SPARK_CONFIG__DATABRICKS__WORKSPACE_INSTANCE_URL")
    _maybe_update_config_with_env_var(config, "SPARK_CONFIG__DATABRICKS__CONFIG_TEMPLATE")

    config["project_config"]["project_name"] = project_name
    config["feature_registry"]["api_endpoint"] = f"https://{resource_prefix}webapp.azurewebsites.net/api/v1"
    config["online_store"]["redis"]["host"] = f"{resource_prefix}redis.redis.cache.windows.net"

    # Update configs using kwargs
    new_config = _config_kwargs_to_dict(**kwargs)
    _update_config(config, new_config)

    # Set platform specific configurations
    if config["spark_config"]["spark_cluster"] == "local":
        _set_local_spark_config()
    elif config["spark_config"]["spark_cluster"] == "azure_synapse":
        _set_azure_synapse_config(
            config=config,
            resource_prefix=resource_prefix,
            project_name=project_name,
        )
    elif config["spark_config"]["spark_cluster"] == "databricks":
        _set_databricks_config(
            config=config,
            project_name=project_name,
            cluster_id=databricks_cluster_id,
        )

    # Verify config
    _verify_config(config)

    # Write config to file
    if not output_filepath:
        output_filepath = NamedTemporaryFile(mode="w", delete=False).name

    with open(output_filepath, "w") as f:
        yaml.dump(config, f, default_flow_style=False)

    return output_filepath


def _set_local_spark_config():
    """Set environment variables for local spark cluster."""
    os.environ["SPARK_LOCAL_IP"] = os.getenv(
        "SPARK_LOCAL_IP",
        "127.0.0.1",
    )


def _set_azure_synapse_config(
    config: Dict,
    resource_prefix: str,
    project_name: str,
):
    """Set configs for Azure Synapse spark cluster."""

    config["spark_config"]["azure_synapse"] = config["spark_config"].get("azure_synapse", {})

    if not config["spark_config"]["azure_synapse"].get("dev_url"):
        config["spark_config"]["azure_synapse"]["dev_url"] = f"https://{resource_prefix}syws.dev.azuresynapse.net"

    if not config["spark_config"]["azure_synapse"].get("workspace_dir"):
        config["spark_config"]["azure_synapse"]["workspace_dir"] =\
            f"abfss://{resource_prefix}fs@{resource_prefix}dls.dfs.core.windows.net/{project_name}"

    for k, v in DEFAULT_AZURE_SYNAPSE_SPARK_POOL_CONFIG.items():
        if not config["spark_config"]["azure_synapse"].get(k):
            config["spark_config"]["azure_synapse"][k] = v


def _set_databricks_config(
    config: Dict,
    project_name: str,
    cluster_id: str = None,
):
    """Set configs for Databricks spark cluster."""

    config["spark_config"]["databricks"] = config["spark_config"].get("databricks", {})

    if not config["spark_config"]["databricks"].get("work_dir"):
        config["spark_config"]["databricks"]["work_dir"] = f"dbfs:/{project_name}"

    if not config["spark_config"]["databricks"].get("config_template"):
        databricks_config = {
            "run_name": "FEATHR_FILL_IN",
            "libraries": [{"jar": "FEATHR_FILL_IN"}],
            "spark_jar_task": {
                "main_class_name": "FEATHR_FILL_IN",
                "parameters": ["FEATHR_FILL_IN"],
            },
        }
        if cluster_id is None:
            databricks_config["new_cluster"] = DEFAULT_DATABRICKS_CLUSTER_CONFIG
        else:
            databricks_config["existing_cluster_id"] = cluster_id

        config["spark_config"]["databricks"]["config_template"] = json.dumps(databricks_config)


def _config_kwargs_to_dict(**kwargs) -> Dict:
    """Parse config's keyword arguments to dictionary.
    e.g. `spark_config__spark_cluster="local"` will be parsed to `{"spark_config": {"spark_cluster": "local"}}`.
    """
    config = dict()

    for conf_key, conf_value in kwargs.items():
        if conf_value is None:
            continue

        conf = config
        keys = conf_key.split("__")
        for k in keys[:-1]:
            if k not in conf:
                conf[k] = dict()
            conf = conf[k]
        conf[keys[-1]] = conf_value

    return config


def _update_config(config: Dict, new_config: Dict):
    """Update config dictionary with the values in `new_config`."""
    for k, v in new_config.items():
        if k in config and isinstance(v, collections.abc.Mapping):
            _update_config(config[k], v)
        else:
            config[k] = v


def _verify_config(config: Dict):
    """Verify config."""
    if config["spark_config"]["spark_cluster"] == "azure_synapse":
        if not os.environ.get("ADLS_KEY"):
            raise ValueError("ADLS_KEY must be set in environment variables")
        elif (
            not os.environ.get("SPARK_CONFIG__AZURE_SYNAPSE__DEV_URL") and
            config["spark_config"]["azure_synapse"].get("dev_url") is None
        ):
            raise ValueError("Azure Synapse dev endpoint is not provided.")
        elif (
            not os.environ.get("SPARK_CONFIG__AZURE_SYNAPSE__POOL_NAME") and
            config["spark_config"]["azure_synapse"].get("pool_name") is None
        ):
            raise ValueError("Azure Synapse pool name is not provided.")

    elif config["spark_config"]["spark_cluster"] == "databricks":
        if not os.environ.get("DATABRICKS_WORKSPACE_TOKEN_VALUE"):
            raise ValueError("Databricks workspace token is not provided.")
        elif (
            not os.environ.get("SPARK_CONFIG__DATABRICKS__WORKSPACE_INSTANCE_URL") and
            config["spark_config"]["databricks"].get("workspace_instance_url") is None
        ):
            raise ValueError("Databricks workspace url is not provided.")


def _maybe_update_config_with_env_var(config: Dict, env_var_name: str):
    """Update config dictionary with the values in environment variables.
    e.g. `SPARK_CONFIG__SPARK_CLUSTER` will be parsed to `{"spark_config": {"spark_cluster": "local"}}`.
    """
    if not os.environ.get(env_var_name):
        return

    keys = env_var_name.lower().split("__")
    conf = config
    for k in keys[:-1]:
        if k not in conf:
            conf[k] = dict()
        conf = conf[k]

    conf[keys[-1]] = os.environ[env_var_name]
