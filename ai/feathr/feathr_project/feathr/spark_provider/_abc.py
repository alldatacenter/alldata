from abc import ABC, abstractmethod
from typing import Dict, List, Optional, Tuple


class SparkJobLauncher(ABC):
    """This is the abstract class for all the spark launchers. All the Spark launcher should implement those interfaces
    """

    @abstractmethod
    def upload_or_get_cloud_path(self, local_path_or_http_path: str):
        """upload a file from local path or an http path to the current work directory. Should support transferring file from an http path to cloud working storage, or upload directly from a local storage.

        Args:
            local_path_or_http_path (str): local path or http path
        """
        pass

    @abstractmethod
    def submit_feathr_job(self, job_name: str, main_jar_path: str,  main_class_name: str, arguments: List[str],
                          reference_files_path: List[str], job_tags: Dict[str, str] = None,
                          configuration: Dict[str, str] = {}, properties: Dict[str, str] = None):
        """
        Submits the feathr job

        Args:
            job_name (str): name of the job
            main_jar_path (str): main file paths, usually your main jar file
            main_class_name (str): name of your main class
            arguments (str): all the arguments you want to pass into the spark job
            job_tags (str): tags of the job, for example you might want to put your user ID, or a tag with a certain information
            configuration (Dict[str, str]): Additional configs for the spark job
            properties (Dict[str, str]): Additional System Properties for the spark job
        """
        pass

    @abstractmethod
    def wait_for_completion(self, timeout_seconds: Optional[float]) -> bool:
        """Returns true if the job completed successfully

        Args:
            timeout_seconds (Optional[float]): time out secs

        Returns:
            bool: Returns true if the job completed successfully, otherwise False
        """
        pass

    @abstractmethod
    def get_status(self) -> str:
        """
        Get current job status

        Returns:
            str: Status of the current job

        Returns:
            str: _description_
        """
        pass
