from copy import deepcopy
from datetime import datetime
import json
import os
from pathlib import Path
from shlex import split
from subprocess import STDOUT, Popen
import time
from typing import Any, Dict, List, Optional

from loguru import logger
from pyspark import *

from feathr.constants import OUTPUT_PATH_TAG
from feathr.version import get_maven_artifact_fullname
from feathr.spark_provider._abc import SparkJobLauncher


class _FeathrLocalSparkJobLauncher(SparkJobLauncher):
    """Class to interact with local Spark. This class is not intended to be used in Production environments.
    It is intended to be used for testing and development purposes. No authentication is required to use this class.

    Args:
        workspace_path (str): Path to the workspace
    """

    def __init__(
        self,
        workspace_path: str,
        master: str = None,
        debug_folder: str = "debug",
        clean_up: bool = True,
        retry: int = 3,
        retry_sec: int = 30,
    ):
        """Initialize the Local Spark job launcher"""
        self.workspace_path = (workspace_path,)
        self.debug_folder = debug_folder
        self.spark_job_num = 0
        self.clean_up = clean_up
        self.retry = retry
        self.retry_sec = retry_sec
        self.packages = self._get_default_package()
        self.master = master or "local[*]"
        self.job_tags = None

    def upload_or_get_cloud_path(self, local_path_or_http_path: str):
        """For Local Spark Case, no need to upload to cloud workspace."""
        return local_path_or_http_path

    def submit_feathr_job(
        self,
        job_name: str,
        main_jar_path: str,
        main_class_name: str,
        arguments: List[str] = None,
        python_files: List[str] = None,
        job_tags: Dict[str, str] = None,
        configuration: Dict[str, str] = {},
        properties: Dict[str, str] = {},
        **_,
    ) -> Any:
        """Submits the Feathr job to local spark, using subprocess args.
        Note that the Spark application will automatically run on YARN cluster mode. You cannot change it if
        you are running with Azure Synapse.

        Args:
            job_name: name of the job
            main_jar_path: main file paths, usually your main jar file
            main_class_name: name of your main class
            arguments: all the arguments you want to pass into the spark job
            python_files: required .zip, .egg, or .py files of spark job
            job_tags: tags of the job, for example you might want to put your user ID, or a tag with a certain information
            configuration: Additional configs for the spark job
            properties: System properties configuration
            **_: Not used arguments in local spark mode, such as reference_files_path
        """
        logger.warning(
            f"Local Spark Mode only support basic params right now and should be used only for testing purpose."
        )
        self.cmd_file, self.log_path = self._get_debug_file_name(self.debug_folder, prefix=job_name)

        # Get conf and package arguments
        cfg = configuration.copy() if configuration else {}
        maven_dependency_without_feathr = f"{cfg.pop('spark.jars.packages', self.packages)}"
        maven_dependency = f"{cfg.pop('spark.jars.packages', self.packages)},{get_maven_artifact_fullname()}"
        spark_args = self._init_args(job_name=job_name, confs=cfg)
        # Add additional repositories
        spark_args.extend(["--repositories", "https://repository.mulesoft.org/nexus/content/repositories/public/,https://linkedin.jfrog.io/artifactory/open-source/"])

        if not main_jar_path:
            # We don't have the main jar, use Maven
            if not python_files:
                # This is a JAR job
                # Azure Synapse/Livy doesn't allow JAR job starts from Maven directly, we must have a jar file uploaded.
                # so we have to use a dummy jar as the main file.
                logger.info(f"Main JAR file is not set, using default package '{get_maven_artifact_fullname()}' from Maven")
                # Use the no-op jar as the main file
                # This is a dummy jar which contains only one `org.example.Noop` class with one empty `main` function
                # which does nothing
                current_dir = Path(__file__).parent.resolve()
                main_jar_path = os.path.join(current_dir, "noop-1.0.jar")
                spark_args.extend(["--packages", maven_dependency, "--class", main_class_name, main_jar_path])
            else:
                spark_args.extend(["--packages", maven_dependency])
                # This is a PySpark job, no more things to
                if python_files.__len__() > 1:
                    spark_args.extend(["--py-files", ",".join(python_files[1:])])
                logger.info(f"Creating python files in {python_files}")
                spark_args.append(python_files[0])
        else:
            if not python_files:
                # This is a JAR job
                spark_args.extend(["--class", main_class_name, main_jar_path])
            else:
                spark_args.extend(["--jars", main_jar_path])
                spark_args.extend(["--packages", maven_dependency_without_feathr])
                # This is a PySpark job, no more things to
                if python_files.__len__() > 1:
                    spark_args.extend(["--py-files", ",".join(python_files[1:])])
                spark_args.append(python_files[0])


        if arguments:
            spark_args.extend(arguments)

        if properties:
            spark_args.extend(["--system-properties", json.dumps(properties)])

        cmd = " ".join(spark_args)

        log_append = open(f"{self.log_path}_{self.spark_job_num}.txt", "a")
        # remove stderr=STDOUT per https://stackoverflow.com/a/40046887
        # reference code: https://github.com/lyft/airflow/blob/main/airflow/providers/apache/spark/hooks/spark_submit.py#L391
        proc = Popen(split(cmd), shell=False, stdout=log_append)
        logger.info(f"Detail job stdout and stderr are in {self.log_path}.")

        self.spark_job_num += 1

        with open(self.cmd_file, "a") as c:
            c.write(" ".join(proc.args))
            c.write("\n")

        self.latest_spark_proc = proc

        logger.info(f"Local Spark job submit with pid: {proc.pid}.")

        self.job_tags = deepcopy(job_tags)

        return proc

    def wait_for_completion(self, timeout_seconds: Optional[float] = 500) -> bool:
        """This function track local spark job commands and process status.
        Files will be write into `debug` folder under your workspace.
        """
        logger.info(f"{self.spark_job_num} local spark job(s) in this Launcher, only the latest will be monitored.")
        logger.info(f"Please check auto generated spark command in {self.cmd_file} and detail logs in {self.log_path}.")

        proc = self.latest_spark_proc
        start_time = time.time()
        retry = self.retry

        log_read = open(f"{self.log_path}_{self.spark_job_num-1}.txt", "r")
        while proc.poll() is None and (((timeout_seconds is None) or (time.time() - start_time < timeout_seconds))):
            time.sleep(1)
            try:
                last_line = log_read.readlines()[-1]
                if retry < 1:
                    logger.warning(
                        f"Spark job has hang for {self.retry * self.retry_sec} seconds. latest msg is {last_line}. \
                            Please check {log_read.name}"
                    )
                    if self.clean_up:
                        self._clean_up()
                        proc.wait()
                    break
                retry = self.retry
                if last_line == []:
                    print("_", end="")
                else:
                    print(">", end="")
                    if last_line.__contains__("Feathr Pyspark job completed"):
                        logger.info(f"Pyspark job Completed")
                        proc.terminate()
            except IndexError as e:
                print("x", end="")
                time.sleep(self.retry_sec)
                retry -= 1

        job_duration = time.time() - start_time
        log_read.close()

        if proc.returncode == None:
            logger.warning(
                f"Spark job with pid {self.latest_spark_proc.pid} not completed after {timeout_seconds} sec \
                    time out setting. Spark Logs:"
            )
            with open(log_read.name) as f:
                contents = f.read()
                logger.error(contents)
            if self.clean_up:
                self._clean_up()
                proc.wait()
                return True
        elif proc.returncode == 1:
            logger.warning(f"Spark job with pid {self.latest_spark_proc.pid} is not successful. Spark Logs:")
            with open(log_read.name) as f:
                contents = f.read()
                logger.error(contents)
            return False
        elif proc.returncode == 143:
            # Handle the return code 143 separately.
            # Normally, the Popen method will return a handle that we can poll, and the poll result will be "None" if it's still running, and will be other values if the subprocess is finished.
            # However when calling `Popen` with `spark-submit`, for some reason, the poll result will always return "None", and the process will hang there forever
            # due to this issue, additional handling is needed where we detect the output logs and see if the job is finished.
            # If the logs gives out hint that the job is finished, even the poll result is None (indicating the process is still running) we will still terminate it.
            # by calling `proc.terminate()`
            # if the process is terminated with this way, the return code will be 143. We assume this will still be a successful run.
            logger.info(
                f"Spark job with pid {self.latest_spark_proc.pid} finished in: {int(job_duration)} seconds."
            )
            return True
        else:
            logger.info(
                f"Spark job with pid {self.latest_spark_proc.pid} finished in: {int(job_duration)} seconds \
                    with returncode {proc.returncode}"
            )
            return True

    def _clean_up(self, proc: Popen = None):
        logger.warning(f"Terminate the spark job due to as clean_up is set to True.")
        if not proc:
            self.latest_spark_proc.terminate()
        else:
            proc.terminate()

    def get_status(self) -> str:
        """Get the status of the job, only a placeholder for local spark"""
        return self.latest_spark_proc.returncode

    def get_job_result_uri(self) -> str:
        """Get job output path

        Returns:
            str: output_path
        """
        return self.job_tags.get(OUTPUT_PATH_TAG, None) if self.job_tags else None

    def get_job_tags(self) -> Dict[str, str]:
        """Get job tags

        Returns:
            Dict[str, str]: a dict of job tags
        """
        return self.job_tags

    def _init_args(self, job_name: str, confs: Dict[str, str]) -> List[str]:
        logger.info(f"Spark job: {job_name} is running on local spark with master: {self.master}.")
        args = [
            "spark-submit",
            "--master",
            self.master,
            "--name",
            job_name,
            "--conf",
            "spark.hadoop.fs.wasbs.impl=org.apache.hadoop.fs.azure.NativeAzureFileSystem",
            "--conf",
            "spark.hadoop.fs.wasbs=org.apache.hadoop.fs.azure.NativeAzureFileSystem",
        ]

        for k, v in confs.items():
            args.extend(["--conf", f"{k}={v}"])

        return args

    def _get_debug_file_name(self, debug_folder: str = "debug", prefix: str = None):
        """Auto generated command will be write into cmd file.
        Spark job output will be write into log path with job number as suffix.
        """
        prefix += datetime.now().strftime("%Y%m%d%H%M%S")
        debug_path = os.path.join(debug_folder, prefix)

        logger.info(f"Spark log path is {debug_path}")
        if not os.path.exists(debug_path):
            os.makedirs(debug_path)

        cmd_file = os.path.join(debug_path, f"command.sh")
        log_path = os.path.join(debug_path, f"log")

        return cmd_file, log_path

    def _get_default_package(self):
        # default packages of Feathr Core, requires manual update when new dependency introduced or package updated.
        # TODO: automate this process, e.g. read from pom.xml
        # TODO: dynamical modularization: add package only when it's used in the job, e.g. data source dependencies.
        packages = []
        packages.append("org.apache.spark:spark-avro_2.12:3.3.0")
        packages.append("com.microsoft.sqlserver:mssql-jdbc:10.2.0.jre8")
        packages.append("com.microsoft.azure:spark-mssql-connector_2.12:1.2.0")
        packages.append("org.apache.logging.log4j:log4j-core:2.17.2,com.typesafe:config:1.3.4")
        packages.append("com.fasterxml.jackson.core:jackson-databind:2.12.6.1")
        packages.append("org.apache.hadoop:hadoop-mapreduce-client-core:2.7.7")
        packages.append("org.apache.hadoop:hadoop-common:2.7.7")
        packages.append("org.apache.hadoop:hadoop-azure:3.2.0")
        packages.append("org.apache.avro:avro:1.8.2,org.apache.xbean:xbean-asm6-shaded:4.10")
        packages.append("org.apache.spark:spark-sql-kafka-0-10_2.12:3.1.3")
        packages.append("com.microsoft.azure:azure-eventhubs-spark_2.12:2.3.21")
        packages.append("org.apache.kafka:kafka-clients:3.1.0")
        packages.append("com.google.guava:guava:31.1-jre")
        packages.append("it.unimi.dsi:fastutil:8.1.1")
        packages.append("org.mvel:mvel2:2.2.8.Final")
        packages.append("com.fasterxml.jackson.module:jackson-module-scala_2.12:2.13.3")
        packages.append("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.12.6")
        packages.append("com.fasterxml.jackson.dataformat:jackson-dataformat-csv:2.12.6")
        packages.append("com.jasonclawson:jackson-dataformat-hocon:1.1.0")
        packages.append("com.redislabs:spark-redis_2.12:3.1.0")
        packages.append("org.apache.xbean:xbean-asm6-shaded:4.10")
        packages.append("com.google.protobuf:protobuf-java:3.19.4")
        packages.append("net.snowflake:snowflake-jdbc:3.13.18")
        packages.append("net.snowflake:spark-snowflake_2.12:2.10.0-spark_3.2")
        packages.append("org.apache.commons:commons-lang3:3.12.0")
        packages.append("org.xerial:sqlite-jdbc:3.36.0.3")
        packages.append("com.github.changvvb:jackson-module-caseclass_2.12:1.1.1")
        packages.append("com.azure.cosmos.spark:azure-cosmos-spark_3-1_2-12:4.11.1")
        packages.append("org.eclipse.jetty:jetty-util:9.3.24.v20180605")
        packages.append("commons-io:commons-io:2.6")
        packages.append("org.apache.hadoop:hadoop-azure:2.7.4")
        packages.append("com.microsoft.azure:azure-storage:8.6.4")
        return ",".join(packages)
