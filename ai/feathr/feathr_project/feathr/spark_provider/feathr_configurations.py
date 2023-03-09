from typing import Any, Dict, List, Optional, Tuple


class SparkExecutionConfiguration:
    """A wrapper class to enable Spark Execution Configurations which will be passed to the underlying spark engine.
    Attributes:
        spark_execution_configuration: dict[str, str] which will be passed to the underlying spark engine
    Returns:
        dict[str, str]
    """
    def __new__(cls, spark_execution_configuration = Dict[str, str]) -> Dict[str, str]:
        return spark_execution_configuration
