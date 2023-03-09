from logging import PlaceHolder
from typing import List
from feathr.definition.sink import Sink
from feathr.definition.source import Source

class FeatureJoinJobParams:
    """Parameters related to feature join job.

    Attributes:
        join_config_path: Path to the join config.
        observation_path: Absolute path in Cloud to the observation data path.
        feature_config: Path to the features config.
        job_output_path: Absolute path in Cloud that you want your output data to be in.
    """

    def __init__(self, join_config_path, observation_path, feature_config, job_output_path, secrets:List[str]=[]):
        self.secrets = secrets
        self.join_config_path = join_config_path
        if isinstance(observation_path, str):
            self.observation_path = observation_path
        elif isinstance(observation_path, Source):
            self.observation_path = observation_path.to_argument()
            if hasattr(observation_path, "get_required_properties"):
                self.secrets.extend(observation_path.get_required_properties())
        else:
            raise TypeError("observation_path must be a string or a Sink")
        self.feature_config = feature_config
        if isinstance(job_output_path, str):
            self.job_output_path = job_output_path
        elif isinstance(job_output_path, Sink):
            self.job_output_path = job_output_path.to_argument()
            if hasattr(job_output_path, "get_required_properties"):
                self.secrets.extend(job_output_path.get_required_properties())
        else:
            raise TypeError("job_output_path must be a string or a Sink")

class FeatureGenerationJobParams:
    """Parameters related to feature generation job.

    Attributes:
        generation_config_path: Path to the feature generation config.
        feature_config: Path to the features config.
        secrets: secret names from data sources, the values will be taken from env or KeyVault
    """

    def __init__(self, generation_config_path, feature_config, secrets=[]):
        self.generation_config_path = generation_config_path
        self.feature_config = feature_config
        self.secrets = secrets
