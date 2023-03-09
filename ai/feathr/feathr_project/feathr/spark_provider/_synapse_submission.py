from copy import deepcopy
import json
import os
import pathlib
import re
import time
import urllib.request
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple
from urllib.parse import urlparse
from os.path import basename
from enum import Enum
import tempfile
from azure.identity import (ChainedTokenCredential, DefaultAzureCredential,
                            DeviceCodeCredential, EnvironmentCredential,
                            ManagedIdentityCredential)
from azure.storage.filedatalake import DataLakeServiceClient, DataLakeDirectoryClient
from azure.synapse.spark import SparkClient
from azure.synapse.spark.models import SparkBatchJobOptions
from loguru import logger
from requests import request
from tqdm import tqdm

from feathr.spark_provider._abc import SparkJobLauncher
from feathr.constants import *
from feathr.version import get_maven_artifact_fullname

class LivyStates(Enum):
    """ Adapt LivyStates over to relax the dependency for azure-synapse-spark pacakge.
    Definition is here:
    https://github.com/Azure/azure-sdk-for-python/blob/main/sdk/synapse/azure-synapse-spark/azure/synapse/spark/models/_spark_client_enums.py#L38
    """

    NOT_STARTED = "not_started"
    STARTING = "starting"
    IDLE = "idle"
    BUSY = "busy"
    SHUTTING_DOWN = "shutting_down"
    ERROR = "error"
    DEAD = "dead"
    KILLED = "killed"
    SUCCESS = "success"
    RUNNING = "running"
    RECOVERING = "recovering"


class _FeathrSynapseJobLauncher(SparkJobLauncher):
    """
    Submits spark jobs to a Synapse spark cluster.
    """

    def __init__(self, synapse_dev_url: str, pool_name: str, datalake_dir: str, executor_size: str, executors: int, credential=None):
        # use DeviceCodeCredential if EnvironmentCredential is not available
        self.credential = credential
        # use the same credential for authentication to avoid further login.
        self._api = _SynapseJobRunner(
            synapse_dev_url, pool_name, executor_size=executor_size, executors=executors, credential=self.credential)
        self._datalake = _DataLakeFiler(
            datalake_dir, credential=self.credential)
        # Save Synapse parameters to retrieve driver log
        self._synapse_dev_url = synapse_dev_url
        self._pool_name = pool_name

    def upload_or_get_cloud_path(self, local_path_or_cloud_src_path: str, tar_dir_path: Optional[str] = None):
        """
        Supports transferring file from an http path to cloud working storage, or upload directly from a local storage,
        or copying files from a source datalake directory to a target datalake directory
        """
        if local_path_or_cloud_src_path.startswith('abfs') or local_path_or_cloud_src_path.startswith('wasb'):
            if tar_dir_path is None or not (tar_dir_path.startswith('abfs') or tar_dir_path.startswith('wasb')):
                raise RuntimeError(
                f"Failed to copy files from dbfs directory: {local_path_or_cloud_src_path}. {tar_dir_path} is not a valid target directory path"
            )
            [_, source_exist] = self._datalake._dir_exists(local_path_or_cloud_src_path)
            if not source_exist:
                raise RuntimeError(f"Source folder:{local_path_or_cloud_src_path} doesn't exist. Please make sure it's a valid path")
            [dir_client, target_exist] = self._datalake._dir_exists(tar_dir_path)
            if target_exist:
                logger.warning('Target cloud directory {} already exists. Please use another one.', tar_dir_path)
                return tar_dir_path
            dir_client.create_directory()
            tem_dir_obj = tempfile.TemporaryDirectory()
            self._datalake.download_file(local_path_or_cloud_src_path, tem_dir_obj.name)
            self._datalake.upload_file_to_workdir(tem_dir_obj.name, tar_dir_path, dir_client)
            logger.info('{} is uploaded to location: {}',
                    local_path_or_cloud_src_path, tar_dir_path)
            return tar_dir_path
                            
        logger.info('Uploading {} to cloud..', local_path_or_cloud_src_path)
        res_path = self._datalake.upload_file_to_workdir(
            local_path_or_cloud_src_path)

        logger.info('{} is uploaded to location: {}',
                    local_path_or_cloud_src_path, res_path)
        return res_path

    def download_result(self, result_path: str, local_folder: str, is_file_path: bool = False):
        """
        Supports downloading files from the result folder
        """
        if is_file_path:
            paths = result_path.rsplit('/',1)
            if len(paths) != 2:
                raise RuntimeError(f"Invalid single file path: {result_path}")
            return self._datalake.download_file(paths[0]+'/', local_folder, paths[1])
        return self._datalake.download_file(result_path, local_folder, None)
    
    
    def cloud_dir_exists(self, dir_path: str) -> bool:
        """
        Checks if a directory already exists in the datalake
        """
        
        [_, exists] = self._datalake._dir_exists(dir_path)
        return exists

    def submit_feathr_job(self, job_name: str, main_jar_path: str = None,  main_class_name: str = None, arguments: List[str] = None,
                          python_files: List[str]= None, reference_files_path: List[str] = None, job_tags: Dict[str, str] = None,
                          configuration: Dict[str, str] = {}, properties: Dict[str, str] = {}):
        """
        Submits the feathr job
        Refer to the Apache Livy doc for more details on the meaning of the parameters:
        https://livy.apache.org/docs/latest/rest-api.html

        reference files: put everything there and the function will automatically categorize them based on the
        extension name to either the "files" argument in the Livy API, or the "jars" argument in the Livy API. The
        path can be local path and this function will automatically upload the function to the corresponding azure
        storage

        Also, note that the Spark application will automatically run on YARN cluster mode. You cannot change it if
        you are running with Azure Synapse.

        Args:
            job_name (str): name of the job
            main_jar_path (str): main file paths, usually your main jar file
            main_class_name (str): name of your main class
            arguments (str): all the arguments you want to pass into the spark job
            job_tags (str): tags of the job, for example you might want to put your user ID, or a tag with a certain information
            configuration (Dict[str, str]): Additional configs for the spark job
            properties (Dict[str, str]): Additional System Properties for the spark job
        """

        if properties:
            arguments.append("--system-properties=%s" % json.dumps(properties))

        if configuration:
            cfg = configuration.copy()  # We don't want to mess up input parameters
        else:
            cfg = {}
        if not main_jar_path:
            # We don't have the main jar, use Maven
            # Add Maven dependency to the job configuration
            logger.info(f"Main JAR file is not set, using default package '{get_maven_artifact_fullname()}' from Maven")
            if "spark.jars.packages" in cfg:
                cfg["spark.jars.packages"] = ",".join(
                    [cfg["spark.jars.packages"], get_maven_artifact_fullname()])
            else:
                cfg["spark.jars.packages"] = get_maven_artifact_fullname()

            if not python_files:
                # This is a JAR job
                # Azure Synapse/Livy doesn't allow JAR job starts from Maven directly, we must have a jar file uploaded.
                # so we have to use a dummy jar as the main file.
                # Use the no-op jar as the main file
                # This is a dummy jar which contains only one `org.example.Noop` class with one empty `main` function which does nothing
                current_dir = pathlib.Path(__file__).parent.resolve()
                main_jar_path = os.path.join(current_dir, "noop-1.0.jar")
            else:
                # This is a PySpark job, no more things to do
                pass
        main_jar_cloud_path = None
        if main_jar_path:
            # Now we have a main jar, either feathr or noop
            if main_jar_path.startswith('abfs'):
                main_jar_cloud_path = main_jar_path
                logger.info(
                    'Cloud path {} is used for running the job: {}', main_jar_path, job_name)
            else:
                logger.info('Uploading jar from {} to cloud for running job: {}',
                            main_jar_path, job_name)
                main_jar_cloud_path = self._datalake.upload_file_to_workdir(main_jar_path)
                logger.info('{} is uploaded to {} for running job: {}',
                            main_jar_path, main_jar_cloud_path, job_name)
        else:
            # We don't have the main Jar, and this is a PySpark job so we don't use `noop.jar` either
            # Keep `main_jar_cloud_path` as `None` as we already added maven package into cfg
            pass

        reference_file_paths = []
        for file_path in reference_files_path:
            reference_file_paths.append(
                self._datalake.upload_file_to_workdir(file_path))

        self.current_job_info = self._api.create_spark_batch_job(job_name=job_name,
                                                                 main_file=main_jar_cloud_path,
                                                                 class_name=main_class_name,
                                                                 python_files=python_files,
                                                                 arguments=arguments,
                                                                 reference_files=reference_files_path,
                                                                 tags=job_tags,
                                                                 configuration=cfg)
        logger.info('See submitted job here: https://web.azuresynapse.net/en-us/monitoring/sparkapplication')
        return self.current_job_info

    def wait_for_completion(self, timeout_seconds: Optional[float]) -> bool:
        """
        Returns true if the job completed successfully
        """          
        start_time = time.time()
        while (timeout_seconds is None) or (time.time() - start_time < timeout_seconds):
            status = self.get_status()
            logger.info('Current Spark job status: {}', status)
            if status in {LivyStates.SUCCESS.value}:
                return True
            elif status in {LivyStates.ERROR.value, LivyStates.DEAD.value, LivyStates.KILLED.value}:
                logger.error("Feathr job has failed.")
                error_msg = self._api.get_driver_log(self.current_job_info.id).decode('utf-8')
                logger.error(error_msg)
                logger.error("The size of the whole error log is: {}. The logs might be truncated in some cases (such as in Visual Studio Code) so only the top a few lines of the error message is displayed. If you cannot see the whole log, you may want to extend the setting for output size limit.", len(error_msg))
                return False
            else:
                time.sleep(30)
        else:
            raise TimeoutError('Timeout waiting for job to complete')

    def get_status(self) -> str:
        """Get current job status

        Returns:
            str: Status of the current job
        """
        job = self._api.get_spark_batch_job(self.current_job_info.id)
        assert job is not None
        return job.state

    def get_job_result_uri(self) -> str:
        """Get job output uri

        Returns:
            str: `output_path` field in the job tags
        """
        tags = self._api.get_spark_batch_job(self.current_job_info.id).tags
        # in case users call this API even when there's no tags available
        return None if tags is None else tags[OUTPUT_PATH_TAG]

    def get_job_tags(self) -> Dict[str, str]:
        """Get job tags

        Returns:
            Dict[str, str]: a dict of job tags
        """
        return self._api.get_spark_batch_job(self.current_job_info.id).tags

class _SynapseJobRunner(object):
    """
    Class to interact with Synapse Spark cluster
    """
    def __init__(self, synapse_dev_url, spark_pool_name, credential=None, executor_size='Small', executors=2):
        self._synapse_dev_url = synapse_dev_url
        self._spark_pool_name = spark_pool_name
        if credential is None:
            logger.warning('No valid Azure credential detected. Using DefaultAzureCredential')
            credential = DefaultAzureCredential()
        self._credential = credential

        self.client = SparkClient(
            credential=credential,
            endpoint=synapse_dev_url,
            spark_pool_name=spark_pool_name
        )

        self._executor_size = executor_size
        self._executors = executors
        self.EXECUTOR_SIZE = {'Small': {'Cores': 4, 'Memory': '28g'}, 'Medium': {'Cores': 8, 'Memory': '56g'},
                              'Large': {'Cores': 16, 'Memory': '112g'}}

    def _categorized_files(self, reference_files: List[str]):
        """categorize files to make sure they are in the ready to submissio format

        Args:
            reference_files (List[str]): a list of reference files, can be either jars, pyfiles, etc.

        Returns:
            a tuple with all the files and jars clasified into list
        """
        if reference_files == None:
            return None, None

        files = []
        jars = []
        for file in reference_files:
            file = file.strip()
            if file.endswith('.jar'):
                jars.append(file)
            else:
                files.append(file)
        return files, jars

    def get_spark_batch_job(self, job_id:int):
        """
        Get the job object by searching a certain ID
        """

        return self.client.spark_batch.get_spark_batch_job(job_id, detailed=True)

    def get_spark_batch_jobs(self):
        """
        Get all the jobs in a certain Spark pool
        """

        return self.client.spark_batch.get_spark_batch_jobs(detailed=True)

    def cancel_spark_batch_job(self, job_id:int):
        """
        Cancel a job by searching a certain ID
        """

        return self.client.spark_batch.cancel_spark_batch_job(job_id)

    def create_spark_batch_job(self, job_name, main_file, class_name=None,
                               arguments=None, python_files=None, reference_files=None, archives=None, configuration=None, tags=None):
        """
        Submit a spark job to a certain cluster
        """

        files, jars = self._categorized_files(reference_files)
        driver_cores = self.EXECUTOR_SIZE[self._executor_size]['Cores']
        driver_memory = self.EXECUTOR_SIZE[self._executor_size]['Memory']
        executor_cores = self.EXECUTOR_SIZE[self._executor_size]['Cores']
        executor_memory = self.EXECUTOR_SIZE[self._executor_size]['Memory']

        # If we have a main jar, it needs to be added as dependencies for pyspark job
        # Otherwise it's a PySpark job with Feathr JAR from Maven
        if main_file:
            jars = jars + [main_file]
        elif not python_files:
            # These 2 parameters should not be empty at the same time
            raise ValueError("Main JAR is not set for the Spark job")

        # If file=main_file, then it's using only Scala Spark
        # If file=python_files[0], then it's using Pyspark
        spark_execution_file = python_files[0] if python_files else main_file

        spark_batch_job_options = SparkBatchJobOptions(
            tags=tags,
            name=job_name,
            file=spark_execution_file,
            class_name=class_name,
            python_files=python_files[1:],
            arguments=arguments,
            jars=jars,
            files=files,
            archives=archives,
            configuration=configuration,
            driver_memory=driver_memory,
            driver_cores=driver_cores,
            executor_memory=executor_memory,
            executor_cores=executor_cores,
            executor_count=self._executors)

        return self.client.spark_batch.create_spark_batch_job(spark_batch_job_options, detailed=True)

    def get_driver_log(self, job_id) -> str:
        # @see: https://docs.microsoft.com/en-us/azure/synapse-analytics/spark/connect-monitor-azure-synapse-spark-application-level-metrics
        app_id = self.get_spark_batch_job(job_id).app_id
        url = "%s/sparkhistory/api/v1/sparkpools/%s/livyid/%s/applications/%s/driverlog/stderr/?isDownload=true" % (self._synapse_dev_url, self._spark_pool_name, job_id, app_id)
        token = self._credential.get_token("https://dev.azuresynapse.net/.default").token
        req = urllib.request.Request(url=url, headers={"authorization": "Bearer %s" % token})
        resp = urllib.request.urlopen(req)
        return resp.read()


class _DataLakeFiler(object):
    """
    Class to interact with Azure Data Lake Storage.
    """
    def __init__(self, datalake_dir, credential=None):
        # A datalake path would be something like this:
        # "abfss://feathrazuretest3fs@feathrazuretest3storage.dfs.core.windows.net/frame_getting_started" after this
        # split, datalake_path_split should give out something like this; ['abfss:', '', 'feathrazuretest3fs',
        # 'feathrazuretest3storage.dfs.core.windows.net', 'frame_getting_started'] datalake_path_split[0] should be
        # the protocal, datalake_path_split[1] is empty, datalake_path_split[2] is the file system used (aka
        # container name), datalake_path_split[3] should be the full name of this target path,
        # and datalake_path_split[3:] would be all the directory in this particular container split datalake names by
        # "/" or "@"
        datalake_path_split = list(filter(None, re.split('/|@', datalake_dir)))
        assert len(datalake_path_split) >= 3

        if credential is None:
            raise RuntimeError("Invalid credential provided.")

        account_url = "https://" + datalake_path_split[2]

        self.file_system_client = DataLakeServiceClient(
            credential=credential,
            account_url=account_url
        ).get_file_system_client(datalake_path_split[1])

        if len(datalake_path_split) > 3:
            # directory exists in datalake path
            self.dir_client = self.file_system_client.get_directory_client(
                '/'.join(datalake_path_split[3:]))
            self.dir_client.create_directory()
        else:
            # otherwise use root folder instead
            self.dir_client = self.file_system_client.get_directory_client('/')

        self.datalake_dir = datalake_dir + \
            '/' if datalake_dir[-1] != '/' else datalake_dir

    def upload_file_to_workdir(self, src_file_path: str, tar_dir_path: Optional[str] = "", tar_dir_client: Optional[DataLakeDirectoryClient] = None) -> str:
        """
        Handles file upload to the corresponding datalake storage. If a path starts with "wasb" or "abfs",
        it will skip uploading and return the original path; otherwise it will upload the source file to the working
        dir
        """

        src_parse_result = urlparse(src_file_path)
        if src_parse_result.scheme.startswith('http'):
            file_name = basename(src_file_path)
            file_client = self.dir_client.create_file(file_name)
            # returned paths for the uploaded file
            returned_path = self.datalake_dir + file_name
            with urllib.request.urlopen(src_file_path) as f:
                data = f.read()
                file_client.upload_data(data, overwrite=True)
                logger.info("{} is downloaded and then uploaded to location: {}", src_file_path, returned_path)
        elif src_parse_result.scheme.startswith('abfs') or src_parse_result.scheme.startswith('wasb'):
            # passed a cloud path
            logger.info("Skip uploading file {} as it's already in the cloud", src_file_path)
            returned_path = src_file_path
        else:
            # else it should be a local file path or dir
            if os.path.isdir(src_file_path):
                logger.info("Uploading folder {}", src_file_path)
                dest_paths = []
                if tar_dir_client is not None:
                    # Only supports uploading local files/dir to datalake dir for now
                     for item in Path(src_file_path).iterdir():
                        returned_path = self.upload_file(item.resolve(), tar_dir_path, tar_dir_client)
                        dest_paths.extend([returned_path]) 
                else:
                    for item in Path(src_file_path).glob('**/*.conf'):
                        returned_path = self.upload_file(item.resolve())
                        dest_paths.extend([returned_path])
                returned_path = ','.join(dest_paths)
            else:
                returned_path = self.upload_file(src_file_path)
        return returned_path

    def upload_file(self, src_file_path, tar_dir_path: Optional[str]="", tar_dir_client: Optional[DataLakeDirectoryClient] = None)-> str:
        file_name = basename(src_file_path)
        logger.info("Uploading file {}", file_name)
        # TODO: add handling for only tar_dir_client or tar_dir_path is provided
        file_client = self.dir_client.create_file(file_name) if tar_dir_client is None else tar_dir_client.create_file(file_name)
        returned_path = self.datalake_dir + file_name if tar_dir_path == "" else tar_dir_path + file_name
        with open(src_file_path, 'rb') as f:
            data = f.read()
            file_client.upload_data(data, overwrite=True)
        logger.info("{} is uploaded to location: {}", src_file_path, returned_path)
        return returned_path
        

    def download_file(self, target_adls_directory: str, local_dir_cache: str, file_name: str = None):
        """
        Download file to a local cache. Supporting download a folder and the content in its subfolder.
        Note that the code will just download the content in the root folder, and the folder in the next level (rather than recursively for all layers of folders)

        Args:
            target_adls_directory (str): target ADLS directory
            local_dir_cache (str): local cache to store local results
            file_name (str): only download the file with name 'file_name' under the target directory if it's provided (default as None)
        """
        logger.info('Beginning reading of results from {}',
                    target_adls_directory)
        parse_result = urlparse(target_adls_directory)
        if parse_result.path == '':
            parse_result.path = '/'
        directory_client = self.file_system_client.get_directory_client(
            parse_result.path)
        
        if file_name is not None:
            local_paths = [os.path.join(local_dir_cache, file_name)]
            self._download_file_list(local_paths, [file_name], directory_client)
            logger.info('Finish downloading file {} from {} to {}.',
                    file_name, target_adls_directory, local_dir_cache)
            return
        
        # returns the paths to all the files in the target director in ADLS
        # get all the paths that are not under a directory
        result_paths = [basename(file_path.name) for file_path in self.file_system_client.get_paths(
            path=parse_result.path, recursive=False) if not file_path.is_directory]

        # get all the paths that are directories and download them
        result_folders = [file_path.name for file_path in self.file_system_client.get_paths(
            path=parse_result.path) if file_path.is_directory]

        # list all the files under the certain folder, and download them preserving the hierarchy
        for folder in result_folders:
            folder_name = basename(folder)
            file_in_folder = [os.path.join(folder_name, basename(file_path.name)) for file_path in self.file_system_client.get_paths(
                path=folder, recursive=False) if not file_path.is_directory]
            local_paths = [os.path.join(local_dir_cache, file_name)
                       for file_name in file_in_folder]
            self._download_file_list(local_paths, file_in_folder, directory_client)

        # download files that are in the result folder
        local_paths = [os.path.join(local_dir_cache, file_name)
                       for file_name in result_paths]
        self._download_file_list(local_paths, result_paths, directory_client)

        logger.info('Finish downloading files from {} to {}.',
                    target_adls_directory, local_dir_cache)
        
    def _download_file_list(self, local_paths: List[str], result_paths, directory_client):
        '''
        Download filelist to local
        '''
        for idx, file_to_write in enumerate(tqdm(result_paths,desc="Downloading result files: ")):
            try:
                os.makedirs(os.path.dirname(local_paths[idx]), exist_ok=True)
                local_file = open(local_paths[idx], 'wb')
                file_client = directory_client.get_file_client(file_to_write)
                download = file_client.download_file()
                downloaded_bytes = download.readall()
                local_file.write(downloaded_bytes)
                local_file.close()               
            except Exception as e:
                logger.error(e)       
                
    def _dir_exists(self, dir_path:str):
        '''
        Check if a directory in datalake already exists. Will also return the directory client
        '''
        datalake_path_split = list(filter(None, re.split('/|@', dir_path)))
        if len(datalake_path_split) <= 3:
            raise RuntimeError("Invalid directory path for datalake: {dir_path}")
        dir_client = self.file_system_client.get_directory_client(
                '/'.join(datalake_path_split[3:]))
        return [dir_client, dir_client.exists()]
        