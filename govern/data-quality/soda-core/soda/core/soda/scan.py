from __future__ import annotations

import json
import logging
import os
import textwrap
from datetime import datetime, timezone

from soda.__version__ import SODA_CORE_VERSION
from soda.common.json_helper import JsonHelper
from soda.common.log import Log, LogLevel
from soda.common.logs import Logs
from soda.common.undefined_instance import undefined
from soda.execution.check.check import Check
from soda.execution.check_outcome import CheckOutcome
from soda.execution.data_source_scan import DataSourceScan
from soda.execution.metric.derived_metric import DerivedMetric
from soda.execution.metric.metric import Metric
from soda.profiling.discover_table_result_table import DiscoverTablesResultTable
from soda.profiling.profile_columns_result import ProfileColumnsResultTable
from soda.profiling.sample_tables_result import SampleTablesResultTable
from soda.sampler.default_sampler import DefaultSampler
from soda.sampler.sampler import Sampler
from soda.soda_cloud.historic_descriptor import HistoricDescriptor
from soda.soda_cloud.soda_cloud import SodaCloud
from soda.sodacl.check_cfg import CheckCfg
from soda.sodacl.location import Location
from soda.sodacl.sodacl_cfg import SodaCLCfg
from soda.telemetry.soda_telemetry import SodaTelemetry

logger = logging.getLogger(__name__)
verbose = False

soda_telemetry = SodaTelemetry.get_instance()


class Scan:
    def __init__(self):
        from soda.configuration.configuration import Configuration
        from soda.execution.check.check import Check
        from soda.execution.data_source_manager import DataSourceManager
        from soda.execution.query.query import Query

        # Using this instead of utcnow() as that creates tz naive object, this has explicitly utc set. More info https://docs.python.org/3/library/datetime.html#datetime.datetime.utcnow
        now = datetime.now(tz=timezone.utc)
        self.sampler: Sampler | None = None
        self._logs = Logs(logger)
        self._scan_definition_name: str | None = None
        self._scan_results_file: str | None = None
        self._data_source_name: str | None = None
        self._variables: dict[str, object] = {"NOW": now.isoformat()}
        self._configuration: Configuration = Configuration(scan=self)
        self._sodacl_cfg: SodaCLCfg = SodaCLCfg(scan=self)
        self._file_paths: set[str] = set()
        self._data_timestamp: datetime = now
        self._scan_start_timestamp: datetime = now
        # FIXME: this attribute cannot be None if typed as `datetime`
        self._scan_end_timestamp: datetime | None = None
        self._data_source_manager = DataSourceManager(self._logs, self._configuration)
        self._data_source_scans: list[DataSourceScan] = []
        self._metrics: set[Metric] = set()
        self._checks_configs: list[CheckCfg] = []
        self._checks: list[Check] = []
        self._queries: list[Query] = []
        self._profile_columns_result_tables: list[ProfileColumnsResultTable] = []
        self._discover_tables_result_tables: list[DiscoverTablesResultTable] = []
        self._sample_tables_result_tables: list[SampleTablesResultTable] = []
        self._logs.info(f"Soda Core {SODA_CORE_VERSION}")
        self.scan_results: dict = {}

    def build_scan_results(self) -> dict:
        checks = [check.get_dict() for check in self._checks if check.outcome is not None and check.archetype is None]
        automated_monitoring_checks = [
            check.get_dict() for check in self._checks if check.outcome is not None and check.archetype is not None
        ]

        # TODO: [SODA-608] separate profile columns and sample tables by aligning with the backend team
        profiling = [
            profile_table.get_dict()
            for profile_table in self._profile_columns_result_tables + self._sample_tables_result_tables
        ]

        query_list = []
        for query in self._queries:
            query_list += query.get_cloud_dicts()

        return JsonHelper.to_jsonnable(  # type: ignore
            {
                "definitionName": self._scan_definition_name,
                "defaultDataSource": self._data_source_name,
                "dataTimestamp": self._data_timestamp,
                "scanStartTimestamp": self._scan_start_timestamp,
                "scanEndTimestamp": self._scan_end_timestamp,
                "hasErrors": self.has_error_logs(),
                "hasWarnings": self.has_check_warns(),
                "hasFailures": self.has_check_fails(),
                "metrics": [metric.get_dict() for metric in self._metrics],
                # If archetype is not None, it means that check is automated monitoring
                "checks": checks,
                "queries": query_list,
                "automatedMonitoringChecks": automated_monitoring_checks,
                "profiling": profiling,
                "metadata": [
                    discover_tables_result.get_dict() for discover_tables_result in self._discover_tables_result_tables
                ],
                "logs": [log.get_dict() for log in self._logs.logs],
            }
        )

    def set_data_source_name(self, data_source_name: str):
        """
        Specifies which datasource to use for the checks.
        """
        self._data_source_name = data_source_name

    def set_scan_definition_name(self, scan_definition_name: str):
        """
        The scan definition name is required if the scan is connected to Soda Cloud in order to correlate subsequent scans from the same pipeline.
        """
        self._scan_definition_name = scan_definition_name

    def set_verbose(self, verbose_var: bool = True):
        self._logs.verbose = verbose_var
        global verbose
        verbose = verbose_var

    def set_scan_results_file(self, set_scan_results_file: str):
        self._scan_results_file = set_scan_results_file

    def add_configuration_yaml_file(self, file_path: str):
        """
        Adds configurations from a YAML file on the given path.
        :param str file_path: is a string file_path pointing to a configuration file.
                              ~ will be expanded to the user home dir.
        """
        try:
            configuration_yaml_str = self._read_file("configuration", file_path)
            self._parse_configuration_yaml_str(
                configuration_yaml_str=configuration_yaml_str,
                file_path=file_path,
            )
        except Exception as e:
            self._logs.error(
                f"Could not add configuration from file path {file_path}",
                exception=e,
            )

    def add_configuration_yaml_files(self, path: str, recursive: bool | None = True, suffixes: str | None = None):
        """
        Adds all configurations all YAML files matching the given file path or scanning the given path as a directory.
        :param str path: is a string that typically is the path to a directory, but it can also be a configuration file.
                         ~ will be expanded to the user home dir the directory in which to search for configuration files.
        :param bool recursive: controls if nested directories also will be scanned.  Default recursive=True.
        :param List[str] suffixes: is optional and is used when recursive scanning directories to only load files
                                   having a given extension or suffix. Default suffixes=[".yml", ".yaml"]
        """
        try:
            configuration_yaml_file_paths = self._collect_file_paths(path=path, recursive=recursive, suffixes=suffixes)
            for configuration_yaml_file_path in configuration_yaml_file_paths:
                self.add_configuration_yaml_file(file_path=configuration_yaml_file_path)
        except Exception as e:
            self._logs.error(f"Could not add configuration files from dir {path}", exception=e)

    def add_configuration_yaml_str(self, environment_yaml_str: str, file_path: str = "yaml string"):
        """
        Adds configurations from a YAML formatted string.
        Parameter file_path is optional and can be used to get the location of the log/error in the logs.
        """
        try:
            self._parse_configuration_yaml_str(
                configuration_yaml_str=environment_yaml_str,
                file_path=file_path,
            )
        except Exception as e:
            self._logs.error(
                "Could not add environment configurations from string",
                exception=e,
            )

    def _parse_configuration_yaml_str(self, configuration_yaml_str: str, file_path: str = "yaml string"):
        from soda.configuration.configuration_parser import ConfigurationParser

        # First round of template resolve right when loading a configuration string.
        configuration_yaml_str = self.jinja_resolve(configuration_yaml_str)

        environment_parse = ConfigurationParser(
            configuration=self._configuration,
            logs=self._logs,
            file_path=file_path,
        )
        environment_parse.parse_environment_yaml_str(configuration_yaml_str)

    def add_duckdb_connection(self, duckdb_connection, data_source_name: str = "duckdb"):
        """
        Adds a duckdb connection to the scan. Only requireed in case of using a pre-existing
        duckdb connection object as a data source.
        """
        try:
            self._configuration.add_duckdb_connection(
                data_source_name=data_source_name, duckdb_connection=duckdb_connection
            )
        except Exception as e:
            self._logs.error(
                f"Could not add duckdb connection for data_source {data_source_name}",
                exception=e,
            )

    def add_spark_session(self, spark_session, data_source_name: str = "spark_df"):
        """
        Pass a spark_session to the scan.  Only required in case of PySpark scans.
        """
        try:
            self._configuration.add_spark_session(data_source_name=data_source_name, spark_session=spark_session)
        except Exception as e:
            self._logs.error(
                f"Could not add environment spark session for data_source {data_source_name}",
                exception=e,
            )

    def add_dask_dataframe(self, dataset_name: str, dask_df) -> None:
        context = self._get_or_create_dask_context(required_soda_module="soda-core-pandas-dask")
        context.create_table(dataset_name, dask_df)

    def add_pandas_dataframe(self, dataset_name: str, pandas_df):
        context = self._get_or_create_dask_context(required_soda_module="soda-core-pandas-dask")
        from dask.dataframe import from_pandas

        dask_df = from_pandas(pandas_df, npartitions=1)
        context.create_table(dataset_name, dask_df)

    def _get_or_create_dask_context(self, required_soda_module: str):
        try:
            from dask_sql import Context
        except ImportError:
            raise Exception(f"{required_soda_module} is not installed. Please install {required_soda_module}")

        if "dask" not in self._configuration.data_source_properties_by_name:
            self._configuration.add_dask_context(data_source_name="dask", dask_context=Context())
        return self._configuration.data_source_properties_by_name["dask"]["context"]

    def add_sodacl_yaml_files(
        self,
        path: str,
        recursive: bool | None = True,
        suffixes: list[str] | None = None,
    ):
        """
        Adds all the files in the given directory to the scan as SodaCL files.
        :param str path: is a string that typically represents a directory, but it can also be a SodaCL file.
                         ~ will be expanded to the user home dir the directory in which to search for SodaCL files.
        :param bool recursive: controls if nested directories also will be scanned.  Default recursive=True.
        :param List[str] suffixes: is optional and is used when recursive scanning directories to only load files
                                   having a given extension or suffix. Default suffixes=[".yml", ".yaml"]
        """
        try:
            sodacl_yaml_file_paths = self._collect_file_paths(path=path, recursive=recursive, suffixes=suffixes)
            for sodacl_yaml_file_path in sodacl_yaml_file_paths:
                self.add_sodacl_yaml_file(file_path=sodacl_yaml_file_path)
        except Exception as e:
            self._logs.error(f"Could not add SodaCL files from dir {dir}", exception=e)

    def _collect_file_paths(
        self,
        path: str,
        recursive: bool | None,
        suffixes: list[str] | None,
    ) -> list[str]:
        if isinstance(path, str):
            if path.endswith("/"):
                path = path[:-1]
            file_system = self._configuration.file_system
            path = file_system.expand_user(path)
            paths_to_scan = [path]
            file_paths = []
            is_root = True
            while len(paths_to_scan) > 0:
                path = paths_to_scan.pop()
                if file_system.exists(path):
                    if file_system.is_file(path) and (
                        suffixes is None or any(suffix is None or path.endswith(suffix) for suffix in suffixes)
                    ):
                        file_paths.append(path)
                    elif file_system.is_dir(path) and (is_root or recursive):
                        is_root = False
                        if suffixes is None:
                            suffixes = [".yml", ".yaml"]
                        for dir_entry in file_system.scan_dir(path):
                            paths_to_scan.append(f"{path}/{dir_entry.name}")
                else:
                    self._logs.error(f'Path "{path}" does not exist')
            return file_paths
        else:
            self._logs.error(f"Path is not a string: {type(path).__name__}")
        return []

    def add_sodacl_yaml_file(self, file_path: str):
        """
        Add a SodaCL YAML file to the scan on the given file_path.
        """
        try:
            sodacl_yaml_str = self._read_file("SodaCL", file_path)
            if file_path not in self._file_paths:
                self._file_paths.add(file_path)
                self._parse_sodacl_yaml_str(sodacl_yaml_str=sodacl_yaml_str, file_path=file_path)
            else:
                self._logs.debug(f"Skipping duplicate file addition for {file_path}")
        except Exception as e:
            self._logs.error(f"Could not add SodaCL file {file_path}", exception=e)

    def add_sodacl_yaml_str(self, sodacl_yaml_str: str):
        """
        Add a SodaCL YAML string to the scan.
        """
        try:
            unique_name = "sodacl_string"
            if unique_name in self._file_paths:
                number: int = 2
                while f"{unique_name}_{number}" in self._file_paths:
                    number += 1
                unique_name = f"{unique_name}_{number}"
            file_path = f"{unique_name}.yml"
            self._parse_sodacl_yaml_str(sodacl_yaml_str=sodacl_yaml_str, file_path=file_path)
        except Exception as e:
            self._logs.error(f"Could not add SodaCL string", exception=e)

    def _parse_sodacl_yaml_str(self, sodacl_yaml_str: str, file_path: str = None):
        from soda.sodacl.sodacl_parser import SodaCLParser

        sodacl_parser = SodaCLParser(
            sodacl_cfg=self._sodacl_cfg,
            logs=self._logs,
            file_path=file_path,
            data_source_name=self._data_source_name,
        )
        sodacl_parser.parse_sodacl_yaml_str(sodacl_yaml_str)

    def _read_file(self, file_type: str, file_path: str) -> str:
        file_location = Location(file_path)
        file_system = self._configuration.file_system
        resolved_file_path = file_system.expand_user(file_path)
        if not file_system.exists(resolved_file_path):
            self._logs.error(
                f"File {resolved_file_path} does not exist",
                location=file_location,
            )
            return None
        if file_system.is_dir(resolved_file_path):
            self._logs.error(
                f"File {resolved_file_path} exists, but is a directory",
                location=file_location,
            )
            return None
        try:
            self._logs.debug(f'Reading {file_type} file "{resolved_file_path}"')
            file_content_str = file_system.file_read_as_str(resolved_file_path)
            if not isinstance(file_content_str, str):
                self._logs.error(
                    f"Error reading file {resolved_file_path} from the file system",
                    location=file_location,
                )
            return file_content_str
        except Exception as e:
            self._logs.error(
                f"Error reading file {resolved_file_path} from the file system",
                location=file_location,
                exception=e,
            )

    def add_variables(self, variables: dict[str, str]):
        """
        Add variables to the scan. Keys and values must be strings.
        """
        try:
            self._variables.update(variables)
        except Exception as e:
            variables_text = json.dumps(variables)
            self._logs.error(f"Could not add variables {variables_text}", exception=e)

    def disable_telemetry(self):
        """
        Disables all telemetry.  For more information see Soda's public statements on telemetry.  TODO add links.
        """
        self._configuration.telemetry = None

    def execute(self) -> int:
        self._logs.debug("Scan execution starts")
        exit_value = 0
        try:
            from soda.execution.column import Column
            from soda.execution.metric.column_metrics import ColumnMetrics
            from soda.execution.partition import Partition
            from soda.execution.table import Table

            # Disable Soda Cloud if it is not properly configured
            if self._configuration.soda_cloud:
                if not isinstance(self._scan_definition_name, str):
                    self._logs.error(
                        "scan.set_scan_definition_name(...) is not set and it is required to make the Soda Cloud integration work.  For this scan, Soda Cloud will be disabled."
                    )
                    self._configuration.soda_cloud = None
                else:
                    if self._configuration.soda_cloud.is_samples_disabled():
                        self._configuration.sampler = DefaultSampler()
            else:
                self._configuration.sampler = DefaultSampler()

            # Override the sampler, if it is configured programmatically
            if self.sampler is not None:
                self._configuration.sampler = self.sampler

            if self._configuration.sampler:
                # ensure the sampler is configured with the scan logs
                self._configuration.sampler.logs = self._logs

            # Resolve the for each table checks and add them to the scan_cfg data structures
            self.__resolve_for_each_dataset_checks()
            # Resolve the for each column checks and add them to the scan_cfg data structures
            self.__resolve_for_each_column_checks()

            # For each data_source, build up the DataSourceScan data structures
            for data_source_scan_cfg in self._sodacl_cfg.data_source_scan_cfgs.values():
                # This builds up the data structures that correspond to the cfg model
                data_source_scan = self._get_or_create_data_source_scan(data_source_scan_cfg.data_source_name)
                if data_source_scan:
                    for check_cfg in data_source_scan_cfg.check_cfgs:
                        # Data source checks are created here, i.e. no dataset associated (e.g. failed rows check)
                        self.__create_check(check_cfg, data_source_scan)

                    for table_cfg in data_source_scan_cfg.tables_cfgs.values():
                        table: Table = data_source_scan.get_or_create_table(table_cfg.table_name)

                        for column_configurations_cfg in table_cfg.column_configurations_cfgs.values():
                            column: Column = table.get_or_create_column(column_configurations_cfg.column_name)
                            column.set_column_configuration_cfg(column_configurations_cfg)

                        for partition_cfg in table_cfg.partition_cfgs:
                            partition: Partition = table.get_or_create_partition(partition_cfg.partition_name)
                            partition.set_partition_cfg(partition_cfg)

                            for check_cfg in partition_cfg.check_cfgs:
                                self.__create_check(check_cfg, data_source_scan, partition)

                            if partition_cfg.column_checks_cfgs:
                                for column_checks_cfg in partition_cfg.column_checks_cfgs.values():
                                    column_metrics: ColumnMetrics = partition.get_or_create_column_metrics(
                                        column_checks_cfg.column_name
                                    )
                                    column_metrics.set_column_check_cfg(column_checks_cfg)
                                    if column_checks_cfg.check_cfgs:
                                        for check_cfg in column_checks_cfg.check_cfgs:
                                            self.__create_check(
                                                check_cfg,
                                                data_source_scan,
                                                partition,
                                                column_metrics.column,
                                            )

            # Handle check attributes before proceeding.
            invalid_check_attributes = None
            invalid_checks = []
            for check in self._checks:
                if check.check_cfg.source_configurations:
                    check_attributes = {
                        self.jinja_resolve(k): self.jinja_resolve(v)
                        for k, v in check.check_cfg.source_configurations.get("attributes", {}).items()
                    }

                    if self._configuration.soda_cloud:
                        # Validate attributes if Cloud is available
                        if check_attributes:
                            from soda.common.attributes_handler import AttributeHandler

                            attribute_handler = AttributeHandler(self._logs)
                            attributes_schema = self._configuration.soda_cloud.get_check_attributes_schema()

                            check_attributes, invalid_check_attributes = attribute_handler.validate(
                                check_attributes, attributes_schema
                            )

                            # Skip (remove) the check if invalid attributes are present.
                            if invalid_check_attributes:
                                invalid_checks.append(check)

                    check.attributes = check_attributes

            if invalid_check_attributes:
                attributes_page_url = f"https://{self._configuration.soda_cloud.host}/organization/attributes"
                self._logs.info(f"Refer to list of valid attributes and values at {attributes_page_url}.")

            if not invalid_checks:
                # Each data_source is asked to create metric values that are returned as a list of query results
                for data_source_scan in self._data_source_scans:
                    data_source_scan.execute_queries()

                # Compute derived metric values
                for metric in self._metrics:
                    if isinstance(metric, DerivedMetric):
                        metric.compute_derived_metric_values()

                # Run profiling, data samples, automated monitoring, sample tables
                try:
                    self.run_data_source_scan()
                except Exception as e:
                    self._logs.error("""An error occurred while executing data source scan""", exception=e)

                # Evaluates the checks based on all the metric values
                for check in self._checks:
                    # First get the metric values for this check
                    check_metrics = {}
                    missing_value_metrics = []
                    for check_metric_name, metric in check.metrics.items():
                        if metric.value is not undefined:
                            check_metrics[check_metric_name] = metric
                        else:
                            missing_value_metrics.append(metric)

                    check_historic_data = {}
                    # For each check get the historic data
                    if check.historic_descriptors:
                        for hd_key, hd in check.historic_descriptors.items():
                            check_historic_data[hd_key] = self.__get_historic_data_from_soda_cloud_metric_store(hd)

                    if not missing_value_metrics:
                        try:
                            check.evaluate(check_metrics, check_historic_data)
                        except BaseException as e:
                            self._logs.error(
                                f"Evaluation of check {check.check_cfg.source_line} failed: {e}",
                                location=check.check_cfg.location,
                                exception=e,
                            )
                    else:
                        missing_metrics_str = ",".join([str(metric) for metric in missing_value_metrics])
                        self._logs.error(
                            f"Metrics '{missing_metrics_str}' were not computed for check '{check.check_cfg.source_line}'"
                        )

            self._logs.info("Scan summary:")
            self.__log_queries(having_exception=False)
            self.__log_queries(having_exception=True)

            checks_pass_count = self.__log_checks(CheckOutcome.PASS)
            checks_warn_count = self.__log_checks(CheckOutcome.WARN)
            warn_text = "warning" if checks_warn_count == 1 else "warnings"
            checks_fail_count = self.__log_checks(CheckOutcome.FAIL)
            fail_text = "failure" if checks_warn_count == 1 else "failures"
            error_count = len(self.get_error_logs())
            error_text = "error" if error_count == 1 else "errors"
            self.__log_checks(None)
            checks_not_evaluated = len(self._checks) - checks_pass_count - checks_warn_count - checks_fail_count

            if len(self._checks) == 0:
                self._logs.warning("No valid checks found, 0 checks evaluated.")
            if checks_not_evaluated:
                self._logs.info(f"{checks_not_evaluated} checks not evaluated.")
            if error_count > 0:
                self._logs.info(f"{error_count} errors.")
            if checks_warn_count + checks_fail_count + error_count == 0 and len(self._checks) > 0:
                if checks_not_evaluated:
                    self._logs.info(
                        "Apart from the checks that have not been evaluated, no failures, no warnings and no errors."
                    )
                else:
                    self._logs.info("All is good. No failures. No warnings. No errors.")
            elif error_count > 0:
                exit_value = 3
                self._logs.info(
                    f"Oops! {error_count} {error_text}. {checks_fail_count} {fail_text}. {checks_warn_count} {warn_text}. {checks_pass_count} pass."
                )
            elif checks_fail_count > 0:
                exit_value = 2
                self._logs.info(
                    f"Oops! {checks_fail_count} {fail_text}. {checks_warn_count} {warn_text}. {error_count} {error_text}. {checks_pass_count} pass."
                )
            elif checks_warn_count > 0:
                exit_value = 1
                self._logs.info(
                    f"Only {checks_warn_count} {warn_text}. {checks_fail_count} {fail_text}. {error_count} {error_text}. {checks_pass_count} pass."
                )

            if error_count > 0:
                Log.log_errors(self.get_error_logs())

            # Telemetry data
            soda_telemetry.set_attributes(
                {
                    "pass_count": checks_pass_count,
                    "error_count": error_count,
                    "failures_count": checks_fail_count,
                }
            )

        except Exception as e:
            exit_value = 3
            self._logs.error("Error occurred while executing scan.", exception=e)
        finally:
            try:
                self._scan_end_timestamp = datetime.now(tz=timezone.utc)
                if self._configuration.soda_cloud:
                    self._logs.info("Sending results to Soda Cloud")
                    self._configuration.soda_cloud.send_scan_results(self)

                    if "send_scan_results" in self._configuration.soda_cloud.soda_cloud_trace_ids:
                        cloud_trace_id = self._configuration.soda_cloud.soda_cloud_trace_ids["send_scan_results"]
                        self._logs.info(f"Soda Cloud Trace: {cloud_trace_id}")
                    else:
                        self._logs.info("Soda Cloud Trace ID not available.")

            except Exception as e:
                exit_value = 3
                self._logs.error("Error occurred while sending scan results to soda cloud.", exception=e)

            self._close()
            self.scan_results = self.build_scan_results()

        if self._scan_results_file is not None:
            logger.info(f"Saving scan results to {self._scan_results_file}")
            with open(self._scan_results_file, "w") as f:
                json.dump(SodaCloud.build_scan_results(self), f)

        # Telemetry data
        soda_telemetry.set_attributes(
            {
                "scan_exit_code": exit_value,
                "checks_count": len(self._checks),
                "queries_count": len(self._queries),
                "metrics_count": len(self._metrics),
            }
        )

        return exit_value

    def run_data_source_scan(self):
        for data_source_scan in self._data_source_scans:
            for data_source_cfg in data_source_scan.data_source_scan_cfg.data_source_cfgs:
                data_source_name = data_source_scan.data_source_scan_cfg.data_source_name
                data_source_scan = self._get_or_create_data_source_scan(data_source_name)
                if data_source_scan:
                    data_source_scan.run(data_source_cfg, self)
                else:
                    data_source_names = ", ".join(self._data_source_manager.data_source_properties_by_name.keys())
                    self._logs.error(
                        f"Could not run monitors on data_source {data_source_name} because It is not "
                        f"configured: {data_source_names}"
                    )

    def __checks_to_text(self, checks: list[Check]):
        return "\n".join([str(check) for check in checks])

    def _close(self):
        self._data_source_manager.close_all_connections()

    def __create_check(self, check_cfg, data_source_scan=None, partition=None, column=None):
        from soda.execution.check.check import Check

        check = Check.create(
            check_cfg=check_cfg,
            data_source_scan=data_source_scan,
            partition=partition,
            column=column,
        )
        self._checks.append(check)

    def __resolve_for_each_dataset_checks(self):
        data_source_name = self._data_source_name

        for index, for_each_dataset_cfg in enumerate(self._sodacl_cfg.for_each_dataset_cfgs):
            include_tables = [include.table_name_filter for include in for_each_dataset_cfg.includes]
            exclude_tables = [include.table_name_filter for include in for_each_dataset_cfg.excludes]

            data_source_scan = self._get_or_create_data_source_scan(data_source_name)
            if data_source_scan:
                query_name = f"for_each_dataset_{for_each_dataset_cfg.table_alias_name}[{index}]"
                table_names = data_source_scan.data_source.get_table_names(
                    include_tables=include_tables,
                    exclude_tables=exclude_tables,
                    query_name=query_name,
                )

                logger.info(f"Instantiating for each for {table_names}")

                for table_name in table_names:
                    data_source_scan_cfg = self._sodacl_cfg.get_or_create_data_source_scan_cfgs(data_source_name)
                    table_cfg = data_source_scan_cfg.get_or_create_table_cfg(table_name)
                    partition_cfg = table_cfg.find_partition(None, None)
                    for check_cfg_template in for_each_dataset_cfg.check_cfgs:
                        check_cfg = check_cfg_template.instantiate_for_each_dataset(
                            name=self.jinja_resolve(
                                check_cfg_template.name,
                                variables={for_each_dataset_cfg.table_alias_name: table_name},
                            ),
                            table_alias=for_each_dataset_cfg.table_alias_name,
                            table_name=table_name,
                            partition_name=partition_cfg.partition_name,
                        )
                        column_name = check_cfg.get_column_name()
                        if column_name:
                            column_checks_cfg = partition_cfg.get_or_create_column_checks(column_name)
                            column_checks_cfg.add_check_cfg(check_cfg)
                        else:
                            partition_cfg.add_check_cfg(check_cfg)

    def __resolve_for_each_column_checks(self):
        if self._sodacl_cfg.for_each_column_cfgs:
            raise NotImplementedError("TODO")

    def _get_or_create_data_source_scan(self, data_source_name: str) -> DataSourceScan:
        from soda.execution.data_source import DataSource
        from soda.sodacl.data_source_scan_cfg import DataSourceScanCfg

        data_source_scan = next(
            (
                data_source_scan
                for data_source_scan in self._data_source_scans
                if data_source_scan.data_source.data_source_name == data_source_name
            ),
            None,
        )
        if data_source_scan is None:
            data_source_scan_cfg = self._sodacl_cfg.data_source_scan_cfgs.get(data_source_name)
            if data_source_scan_cfg is None:
                data_source_scan_cfg = DataSourceScanCfg(data_source_name)
            data_source_name = data_source_scan_cfg.data_source_name
            data_source: DataSource = self._data_source_manager.get_data_source(data_source_name)
            if data_source:
                data_source_scan = data_source.create_data_source_scan(self, data_source_scan_cfg)
                self._data_source_scans.append(data_source_scan)

        return data_source_scan

    def jinja_resolve(
        self,
        definition: str,
        variables: dict[str, object] = None,
        location: Location | None = None,
    ):
        if isinstance(definition, str) and "${" in definition:
            from soda.common.jinja import Jinja

            jinja_variables = self._variables.copy()
            if isinstance(variables, dict):
                jinja_variables.update(variables)
            try:
                return Jinja.resolve(definition, jinja_variables)
            except BaseException as e:
                self._logs.error(
                    message=f"Error resolving Jinja template {definition}: {e}",
                    location=location,
                    exception=e,
                )
        else:
            return definition

    def __get_historic_data_from_soda_cloud_metric_store(
        self, historic_descriptor: HistoricDescriptor
    ) -> dict[str, object]:
        if self._configuration.soda_cloud:
            return self._configuration.soda_cloud.get_historic_data(historic_descriptor)
        else:
            self._logs.error("Soda Core must be configured to connect to Soda Cloud to use change-over-time checks.")
        return {}

    def _find_existing_metric(self, metric) -> Metric:
        return next(
            (existing_metric for existing_metric in self._metrics if existing_metric == metric),
            None,
        )

    def _add_metric(self, metric):
        self._metrics.add(metric)

    def __log_queries(self, having_exception: bool) -> int:
        count = sum((query.exception is None) != having_exception for query in self._queries)
        if count > 0:
            status_text = "ERROR" if having_exception else "OK"
            queries_text = "query" if len(self._queries) == 1 else "queries"
            self._logs.debug(f"{count}/{len(self._queries)} {queries_text} {status_text}")
            for query in self._queries:
                query_text = f"\n{query.sql}" if query.exception else ""
                self._logs.debug(f"  {query.query_name} [{status_text}] {query.duration}{query_text}")
                if query.exception:
                    exception_str = str(query.exception)
                    exception_str = textwrap.indent(text=exception_str, prefix="    ")
                    self._logs.debug(exception_str)
        return count

    def __log_checks(self, check_outcome: CheckOutcome | None) -> int:
        count = sum(check.outcome == check_outcome for check in self._checks)
        if count > 0:
            outcome_text = "NOT EVALUATED" if check_outcome is None else f"{check_outcome.value.upper()}ED"
            checks_text = "check" if len(self._checks) == 1 else "checks"
            self._logs.info(f"{count}/{len(self._checks)} {checks_text} {outcome_text}: ")

            checks_by_partition = {}
            other_checks = []
            for check in self._checks:
                if check.outcome == check_outcome:
                    partition = check.partition
                    if partition:
                        partition_name = f" [{partition.partition_name}]" if partition.partition_name else ""
                        partition_title = f"{partition.table.table_name}{partition_name} in {partition.data_source_scan.data_source.data_source_name}"
                        checks_by_partition.setdefault(partition_title, []).append(check)
                    else:
                        other_checks.append(check)

            for (
                partition_title,
                partition_checks,
            ) in checks_by_partition.items():
                if len(partition_checks) > 0:
                    self._logs.info(f"    {partition_title}")
                    self.__log_check_group(partition_checks, "      ", check_outcome, outcome_text)
            if len(other_checks) > 0:
                self.__log_check_group(other_checks, "    ", check_outcome, outcome_text)
        return count

    def __log_check_group(self, checks, indent, check_outcome, outcome_text):
        for check in checks:
            location = ""
            if verbose:
                location = f"[{check.check_cfg.location.file_path}] "

            self._logs.info(f"{indent}{check.name} {location}[{outcome_text}]")
            if self._logs.verbose or check_outcome != CheckOutcome.PASS:
                for diagnostic in check.get_log_diagnostic_lines():
                    self._logs.info(f"{indent}  {diagnostic}")

    def get_variable(self, variable_name: str, default_value: str | None = None) -> str | None:
        # Note: ordering here must be the same as in Jinja.OsContext.resolve_or_missing: First env vars, then scan vars
        if variable_name in os.environ:
            return os.environ[variable_name]
        elif variable_name in self._variables:
            return self._variables[variable_name]
        return default_value

    def get_scan_results(self) -> dict:
        return self.scan_results

    def get_logs_text(self) -> str | None:
        return self.__logs_to_text(self._logs.logs)

    def has_error_logs(self) -> bool:
        return any(log.level == LogLevel.ERROR for log in self._logs.logs)

    def get_error_logs(self) -> list[Log]:
        return [log for log in self._logs.logs if log.level == LogLevel.ERROR]

    def get_error_logs_text(self) -> str | None:
        return self.__logs_to_text(self.get_error_logs())

    def assert_no_error_logs(self) -> None:
        if self.has_error_logs():
            raise AssertionError(self.get_error_logs_text())

    def has_error_or_warning_logs(self) -> bool:
        return any(log.level in [LogLevel.ERROR, LogLevel.WARNING] for log in self._logs.logs)

    def get_error_or_warning_logs(self) -> list[Log]:
        return [log for log in self._logs.logs if log.level in [LogLevel.ERROR, LogLevel.WARNING]]

    def get_error_or_warning_logs_text(self) -> str | None:
        return self.__logs_to_text(self.get_error_or_warning_logs())

    def assert_no_error_nor_warning_logs(self) -> None:
        if self.has_error_or_warning_logs():
            raise AssertionError(self.get_logs_text())

    def assert_has_error(self, expected_error_message: str):
        if all(
            [
                expected_error_message not in log.message and expected_error_message not in str(log.exception)
                for log in self.get_error_logs()
            ]
        ):
            raise AssertionError(
                f'Expected error message "{expected_error_message}" did not occur in the error logs:\n{self.get_logs_text()}'
            )

    def __logs_to_text(self, logs: list[Log]):
        if len(logs) == 0:
            return None
        return "\n".join([str(log) for log in logs])

    def has_check_fails(self) -> bool:
        for check in self._checks:
            if check.outcome == CheckOutcome.FAIL:
                return True
        return False

    def has_check_warns(self) -> bool:
        for check in self._checks:
            if check.outcome == CheckOutcome.WARN:
                return True
        return False

    def has_check_warns_or_fails(self) -> bool:
        for check in self._checks:
            if check.outcome in [CheckOutcome.FAIL, CheckOutcome.WARN]:
                return True
        return False

    def assert_no_checks_fail(self):
        if len(self.get_checks_fail()):
            raise AssertionError(f"Check results failed: \n{self.get_checks_fail_text()}")

    def get_checks_fail(self) -> list[Check]:
        return [check for check in self._checks if check.outcome == CheckOutcome.FAIL]

    def get_checks_fail_text(self) -> str | None:
        return self.__checks_to_text(self.get_checks_fail())

    def assert_no_checks_warn_or_fail(self):
        if len(self.get_checks_warn_or_fail()):
            raise AssertionError(f"Check results having warn or fail outcome: \n{self.get_checks_warn_or_fail_text()}")

    def get_checks_warn_or_fail(self) -> list[Check]:
        return [check for check in self._checks if check.outcome in [CheckOutcome.WARN, CheckOutcome.FAIL]]

    def has_checks_warn_or_fail(self) -> bool:
        return len(self.get_checks_warn_or_fail()) > 0

    def get_checks_warn_or_fail_text(self) -> str | None:
        return self.__checks_to_text(self.get_checks_warn_or_fail())

    def get_all_checks_text(self) -> str | None:
        return self.__checks_to_text(self._checks)

    def has_soda_cloud_connection(self):
        return self._configuration.soda_cloud is not None
