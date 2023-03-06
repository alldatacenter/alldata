#  Copyright 2022 Soda
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#   http://www.apache.org/licenses/LICENSE-2.0
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
from __future__ import annotations

import logging
import sys
from datetime import datetime, timezone
from pathlib import Path

import click
from ruamel.yaml import YAML
from ruamel.yaml.main import round_trip_dump
from soda.common.exceptions import SODA_SCIENTIFIC_MISSING_LOG_MESSAGE
from soda.common.file_system import file_system
from soda.common.logs import configure_logging
from soda.scan import Scan
from soda.telemetry.soda_telemetry import SodaTelemetry
from soda.telemetry.soda_tracer import soda_trace, span_setup_function_args

from ..__version__ import SODA_CORE_VERSION

soda_telemetry = SodaTelemetry.get_instance()


@click.version_option(package_name="soda-core", prog_name="soda-core")
@click.group(help=f"Soda Core CLI version {SODA_CORE_VERSION}")
def main():
    pass


if __name__ == "__main__":
    main()


@main.command(
    short_help="Runs a scan",
)
@click.option(
    "-d",
    "--data-source",
    envvar="SODA_DATA_SOURCE",
    required=True,
    multiple=False,
    type=click.STRING,
)
@click.option(
    "-s",
    "--scan-definition",
    envvar="SODA_SCAN_DEFINITION",
    required=False,
    multiple=False,
    type=click.STRING,
    default="Soda Core CLI",
)
@click.option("-v", "--variable", required=False, default=None, multiple=True, type=click.STRING)
@click.option(
    "-c",
    "--configuration",
    required=False,
    multiple=True,
    type=click.STRING,
)
@click.option(
    "-t",
    "--data-timestamp",
    required=False,
    default=datetime.now(tz=timezone.utc).isoformat(timespec="seconds"),
    help="The scan time in ISO8601 format. Example: 2021-04-28T09:00:00+02:00",
    type=click.STRING,
)
@click.option("-V", "--verbose", is_flag=True)
@click.option(
    "-srf",
    "--scan-results-file",
    required=False,
    default=None,
    help="Specify the file path where the scan results as json will be stored",
)
@click.argument("sodacl_paths", nargs=-1, type=click.STRING)
@soda_trace
def scan(
    sodacl_paths: list[str],
    data_source: str,
    scan_definition: str | None,
    configuration: list[str],
    data_timestamp: str,
    variable: list[str],
    verbose: bool | None,
    scan_results_file: Optional[str] = None,
):
    """
    The soda scan command:

      * parses the checks files and reports any errors

      * build and executes SQL queries for the checks

      * evaluates the checks

      * produces a summary of scan results in the console

      * if connected, sends scan results to Soda Cloud

    option -d --data-source Required. Specify the name of the data source in the configuration.

    option -c --configuration Optional. Specify the filepath of the configuration file. The default filepath
    is ~/.soda/configuration.yml.

    option -v --variable Optional. Use to pass a variable to the scan.  You can specify multiple variables:
    -variable "today=2020-04-12" -variable "yesterday=2020-04-11"

    option -s --scan-definition Optional. If connected to Soda Cloud, specify a scan definition name
    to keep check results from different environments (dev, prod, staging) separate. The default scan
    definition name is "Soda Core CLI".

    option -V --verbose Optional. Activate verbose logging, including the SQL queries that Soda executes.

    option -t --data-timestamp Optional. Set the scan data timestamp to backfill the data for a previous date.

    [CHECKS_FILE_PATHS] Required. Specify a list of file paths for checks files. Can be a file or a directory.
    Soda recursively scans directories and adds all files ending with .yml.

    Example command:

    soda scan -d snowflake_customer_data -v TODAY=2022-03-11 -V ./snfk/pipeline_customer_checks.yml
    """

    configure_logging()

    soda_telemetry.set_attribute("cli_command_name", "scan")

    span_setup_function_args(
        {
            "command_argument": {
                "scan_definition": scan_definition,
            },
            "command_option": {
                "sodacl_paths": len(sodacl_paths),
                "variables": len(variable),
                "configuration_paths": len(configuration),
                "offline": False,  # TODO: change after offline mode is supported.
                "non_interactive": False,  # TODO: change after non interactive mode is supported.
                "verbose": verbose,
                "scan_results_file": scan_results_file,
            },
        }
    )

    scan = Scan()
    scan._data_timestamp = datetime.fromisoformat(data_timestamp)

    # Add variables before any other config as they might be used.
    if variable:
        variables_dict = {}
        for v in variable:
            # Partition by first occurrence of "=" as variable value may contain "=" sign.
            variable_key, _, variable_value = v.partition("=")
            if variable_key and variable_value:
                variables_dict[variable_key] = variable_value
        scan.add_variables(variables_dict)

    if verbose:
        scan.set_verbose()

    if isinstance(data_source, str):
        scan.set_data_source_name(data_source)

    if isinstance(scan_definition, str):
        scan.set_scan_definition_name(scan_definition)

    __load_configuration(scan, configuration)

    if sodacl_paths:
        for sodacl_path_element in sodacl_paths:
            scan.add_sodacl_yaml_files(sodacl_path_element)
    else:
        scan._logs.warning("No checks file specified")

    if isinstance(scan_results_file, str):
        scan.set_scan_results_file(scan_results_file)

    sys.exit(scan.execute())


@main.command(
    short_help="Updates contents of a distribution reference file",
)
@click.option(
    "-d",
    "--data-source",
    envvar="SODA_DATA_SOURCE",
    required=True,
    multiple=False,
    type=click.STRING,
)
@click.option(
    "-c",
    "--configuration",
    required=False,
    multiple=False,
    type=click.STRING,
)
@click.option(
    "-n",
    "--name",
    required=False,
    multiple=False,
    type=click.STRING,
)
@click.option("-V", "--verbose", is_flag=True)
@click.argument("distribution_reference_file", type=click.STRING)
def update_dro(
    distribution_reference_file: str,
    data_source: str,
    configuration: str,
    name: str | None,
    verbose: bool | None,
):
    """
    The soda update-dro command:

      * reads the configuration and instantiates a connection to the data source
      * reads the definition properties in the distribution reference file
      * updates bins, labels and/or weights values for key "reference distribution" in the distribution reference file

    option -d --data-source Required. Specify the name of the data source in the configuration.

    option -c --configuration Optional. Specify the filepath of the configuration file. The default filepath
    is ~/.soda/configuration.yml.


    option -V --verbose Optional. Activate verbose logging, including the SQL queries that Soda executes.

    [DISTRIBUTION_REFERENCE_FILE] Required. A distribution reference file.

    Example command:

    soda update-dro -d snowflake_customer_data ./customers_size_distribution_reference.yml
    """

    configure_logging()

    fs = file_system()

    distribution_reference_yaml_str = fs.file_read_as_str(distribution_reference_file)

    if not distribution_reference_yaml_str:
        logging.error(f"Could not read file {distribution_reference_file}")
        return

    yaml = YAML()
    try:
        distribution_reference_dict = yaml.load(distribution_reference_yaml_str)
    except BaseException as e:
        logging.error(f"Could not parse distribution reference file {distribution_reference_file}: {e}")
        return

    named_format = all(isinstance(value, dict) for value in distribution_reference_dict.values())
    unnamed_format = not any(
        isinstance(value, dict) and key != "distribution_reference"
        for key, value in distribution_reference_dict.items()
    )
    correct_format = named_format or unnamed_format
    if not correct_format:
        logging.error(
            f"""Incorrect distribution reference file format in "{distribution_reference_file}". To use multiple DROs in a single distribution"""
            f""" reference file, ensure that each is named. Refer to"""
            f""" \nhhttps://go.soda.io/dro."""
        )
        return

    if name and named_format:
        distribution_dict = distribution_reference_dict.get(name)
        if not distribution_dict:
            logging.error(
                f"""The dro name "{name}" that you provided does not exist in your distribution reference file "{distribution_reference_file}". """
                f"""Refer to \nhttps://go.soda.io/dro."""
            )
            return
    elif named_format:
        logging.error(
            f"""The distribution reference file "{distribution_reference_file}" that you used contains named DROs, but you did not provide"""
            f""" a DRO name with the -n argument. Run soda update with -n "dro_name" to indicate which DRO you want to update."""
            f""" Refer to \nhttps://go.soda.io/dro."""
        )
        return
    else:
        distribution_dict = distribution_reference_dict

    dataset_name = distribution_dict.get("dataset")
    if not dataset_name and "table" in distribution_dict:
        dataset_name = distribution_dict.pop("table")
        distribution_dict["dataset"] = dataset_name

    if not dataset_name:
        logging.error(f"Missing key 'dataset' in distribution reference file {distribution_reference_file}")

    column_name = distribution_dict.get("column")
    if not column_name:
        logging.error(f"Missing key 'column' in distribution reference file {distribution_reference_file}")

    distribution_type = distribution_dict.get("distribution_type")
    if not distribution_type:
        logging.error(f"Missing key 'distribution_type' in distribution reference file {distribution_reference_file}")

    filter = distribution_dict.get("filter")
    filter_clause = ""
    if filter is not None:
        filter_clause = f"WHERE {filter}"

    if dataset_name and column_name and distribution_type:
        query = f"SELECT {column_name} FROM {dataset_name} {filter_clause}"
        logging.info(f"Querying column values to build distribution reference:\n{query}")

        scan = Scan()
        scan.add_configuration_yaml_files(configuration)
        data_source_scan = scan._get_or_create_data_source_scan(data_source_name=data_source)
        if data_source_scan:
            rows = __execute_query(data_source_scan.data_source.connection, query)

            # TODO document what the supported data types are per data source type. And ensure proper Python data type conversion if needed
            column_values = [row[0] for row in rows]

            if not column_values:
                logging.error(
                    f"""{column_name} column does not have any data! To generate a distribution reference object (DRO) your column needs to have more than 0 rows!"""
                )
                return

            if all(i is None for i in column_values):
                logging.error(
                    f"""{column_name} column has only NULL values! To generate a distribution reference object (DRO) your column needs to have more than 0 not null values!"""
                )
                return
            try:
                from soda.scientific.distribution.comparison import RefDataCfg
                from soda.scientific.distribution.generate_dro import DROGenerator
            except ModuleNotFoundError as e:
                logging.error(f"{SODA_SCIENTIFIC_MISSING_LOG_MESSAGE}\n Original error: {e}")
                return

            dro = DROGenerator(RefDataCfg(distribution_type=distribution_type), column_values).generate()
            distribution_dict["distribution_reference"] = dro.dict()
            if "distribution reference" in distribution_dict:
                # To clean up the file and don't leave the old syntax
                distribution_dict.pop("distribution reference")

            new_file_content = round_trip_dump(distribution_reference_dict)

            fs.file_write_from_str(path=distribution_reference_file, file_content_str=new_file_content)


@main.command(short_help="Ingests test results from a different tool")
@click.argument(
    "tool",
    required=True,
    type=click.Choice(["dbt"]),
)
@click.option(
    "-d",
    "--data-source",
    envvar="SODA_DATA_SOURCE",
    required=True,
    multiple=False,
    type=click.STRING,
)
@click.option(
    "-c",
    "--configuration",
    required=False,
    type=click.STRING,
)
@click.option("-V", "--verbose", is_flag=True)
@click.option(
    "--dbt-artifacts",
    help=(
        "Optional. Specify the path that contains both the manifest and run_result JSON files from dbt. Typically, /dbt_project/target/. "
        "If provided, --dbt-manifest and --dbt-run-results are not required."
    ),
    default=None,
    type=Path,
)
@click.option(
    "--dbt-manifest",
    help="Optional. Specify the path to the dbt manifest JSON file",
    default=None,
    type=Path,
)
@click.option(
    "--dbt-run-results",
    help="Optional. Specify the path to the dbt run results JSON file",
    default=None,
    type=Path,
)
@click.option(
    "--dbt-cloud-run-id",
    help=("Optional. Specify the id of the specific dbt job run that contains tests that you want Soda to ingest. "),
    default=None,
    type=click.STRING,
)
@click.option(
    "--dbt-cloud-job-id",
    help="Optional. Specify the job id that contains the latest dbt run results that you want Soda to ingest.",
    default=None,
    type=click.STRING,
)
@soda_trace
def ingest(tool: str, data_source: str, configuration: str, verbose: bool | None, **kwargs):
    """
    The soda ingest command ingests test results from a different tool to send to Soda Cloud.
    """
    configure_logging()
    fs = file_system()

    soda_telemetry.set_attribute("cli_command_name", "ingest")

    telemetry_kwargs = {k: bool(v) for k, v in kwargs.items()}

    span_setup_function_args(
        {
            "command_argument": {
                "tool": tool,
            },
            "command_option": {
                "configuration_path": bool(configuration),
                "verbose": verbose,
                **telemetry_kwargs,
            },
        }
    )

    scan = Scan()
    scan.set_scan_definition_name(f"Ingest - {tool}")
    scan.set_data_source_name(data_source)

    if verbose:
        scan.set_verbose()

    if not fs.exists(configuration):
        scan._logs.error(f"Configuration path '{configuration}' does not exist")
    else:
        scan.add_configuration_yaml_file(configuration)

        if scan._data_source_name not in scan._data_source_manager.data_source_properties_by_name:
            scan._logs.error(f"The data source '{data_source}' is not present in configuration.")

    if not scan._configuration.soda_cloud:
        scan._logs.error("A Soda Cloud configuration is required for the ingest command to work.")

    return_value = 1

    # TODO: Sloppy for now as we support only one tool. Make this command more generic, consider using visitor pattern or some
    # other way of generic implementation of multi tool support.
    ingestor = None

    if tool == "dbt":
        try:
            from soda.cloud.dbt import DbtCloud

            ingestor = DbtCloud(
                scan,
                **kwargs,
            )
        except ModuleNotFoundError:
            scan._logs.error("Unable to import dbt module. Did you install `pip install soda-core-dbt`?")
    else:
        scan._logs.error(f"Unknown tool: {tool}")

    if ingestor and not scan.has_error_logs():
        return_value = ingestor.ingest()
    else:
        scan._logs.info("Unable to proceed with ingest.")

    sys.exit(return_value)


@main.command(
    short_help="Tests a connection",
)
@click.option(
    "-d",
    "--data-source",
    envvar="SODA_DATA_SOURCE",
    required=True,
    multiple=False,
    type=click.STRING,
)
@click.option(
    "-c",
    "--configuration",
    required=False,
    multiple=True,
    type=click.STRING,
)
@click.option("-V", "--verbose", is_flag=True)
@soda_trace
def test_connection(
    data_source: str,
    configuration: list[str],
    verbose: bool | None,
):
    """
    The soda test connection command:

      * tests whether a specified warehouse connection works

    option -d --data-source Required. Specify the name of the data source in the configuration.

    option -c --configuration Optional. Specify the filepath of the configuration file. The default filepath
    is ~/.soda/configuration.yml.

    option -V --verbose Optional. Activate verbose logging.

    Example command:

    soda test-connection -d snowflake_customer_data -c configuration.yml -V
    """
    configure_logging()

    soda_telemetry.set_attribute("cli_command_name", "test-connection")

    span_setup_function_args(
        {
            "command_option": {
                "configuration_paths": len(configuration),
                "verbose": verbose,
            },
        }
    )

    scan = Scan()

    if verbose:
        scan.set_verbose()

    if isinstance(data_source, str):
        scan.set_data_source_name(data_source)

    __load_configuration(scan, configuration)
    scan._get_or_create_data_source_scan(data_source_name=data_source)
    ds = scan._data_source_manager.get_data_source(scan._data_source_name)

    result = 0

    if ds:
        logging.info(f"Successfully connected to '{data_source}'.")
    else:
        logging.error(f"Unable to connect to '{data_source}'.")
        result = 1

    has_valid_connection = ds.has_valid_connection()

    if has_valid_connection:
        logging.info(f"Connection '{data_source}' is valid.")
    else:
        logging.error(f"Unable to run verification query for '{data_source}'.")
        result = 1

    sys.exit(result)


def __execute_query(connection, sql: str) -> list[tuple]:
    try:
        cursor = connection.cursor()
        try:
            cursor.execute(sql)
            return cursor.fetchall()
        finally:
            cursor.close()
    except BaseException as e:
        logging.error(f"Query error: {e}\n{sql}", exception=e)


def __load_configuration(scan: Scan, configuration_paths: list[str] | None):
    fs = file_system()

    if configuration_paths:
        for configuration_path in configuration_paths:
            if not fs.exists(configuration_path):
                scan._logs.error(f"Configuration path '{configuration_path}' does not exist")
            else:
                scan.add_configuration_yaml_files(configuration_path)
    else:
        default_configuration_file_path = "~/.soda/configuration.yml"
        if fs.is_file(default_configuration_file_path):
            scan.add_configuration_yaml_file(default_configuration_file_path)
        elif not fs.exists(default_configuration_file_path):
            scan._logs.warning("No configuration file specified nor found in ~/.soda/configuration.yml")
