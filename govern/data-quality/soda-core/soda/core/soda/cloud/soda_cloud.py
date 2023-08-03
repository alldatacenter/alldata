from __future__ import annotations

import json
import logging
import tempfile
from datetime import date, datetime, timedelta, timezone
from typing import TYPE_CHECKING

from soda.__version__ import SODA_CORE_VERSION
from soda.cloud.cloud import Cloud
from soda.cloud.historic_descriptor import (
    HistoricChangeOverTimeDescriptor,
    HistoricCheckResultsDescriptor,
    HistoricDescriptor,
    HistoricMeasurementsDescriptor,
)
from soda.common.json_helper import JsonHelper
from soda.common.logs import Logs
from soda.execution.check_type import CheckType

logger = logging.getLogger(__name__)

if TYPE_CHECKING:
    from soda.scan import Scan


class SodaCloud(Cloud):
    ORG_CONFIG_KEY_DISABLE_COLLECTING_WH_DATA = "disableCollectingWarehouseData"

    CSV_TEXT_MAX_LENGTH = 1500

    def __init__(
        self,
        host: str,
        api_key_id: str,
        api_key_secret: str,
        token: str | None,
        port: str | None,
        logs: Logs,
        scheme: str = "https",
    ):
        self.host = host
        self.port = f":{port}" if port else ""
        self.scheme = scheme if scheme else "https"
        self.api_url = f"{self.scheme}://{self.host}{self.port}/api"
        self.api_key_id = api_key_id
        self.api_key_secret = api_key_secret
        self.token: str | None = token
        self.headers = {"User-Agent": f"SodaCore/{SODA_CORE_VERSION}"}
        self.logs = logs
        self.soda_cloud_trace_ids = {}
        self._organization_configuration = None

    @property
    def organization_configuration(self) -> dict:
        if isinstance(self._organization_configuration, dict):
            return self._organization_configuration

        response_json_dict = self._execute_query(
            {"type": "sodaCoreCloudConfiguration"},
            query_name="get_organization_configuration",
        )
        self._organization_configuration = response_json_dict if isinstance(response_json_dict, dict) else {}

        return self._organization_configuration

    @staticmethod
    def build_scan_results(scan) -> dict:
        checks = [
            check.get_cloud_dict()
            for check in scan._checks
            if check.check_type == CheckType.CLOUD
            and (check.outcome is not None or check.force_send_results_to_cloud is True)
            and check.archetype is None
        ]
        automated_monitoring_checks = [
            check.get_cloud_dict()
            for check in scan._checks
            if (check.outcome is not None or check.force_send_results_to_cloud is True) and check.archetype is not None
        ]

        # TODO: [SODA-608] separate profile columns and sample tables by aligning with the backend team
        profiling = [
            profile_table.get_cloud_dict()
            for profile_table in scan._profile_columns_result_tables + scan._sample_tables_result_tables
        ]

        query_list = []
        for query in scan._queries:
            query_list += query.get_cloud_dicts()

        return JsonHelper.to_jsonnable(  # type: ignore
            {
                "definitionName": scan._scan_definition_name,
                "defaultDataSource": scan._data_source_name,
                "dataTimestamp": scan._data_timestamp,
                # Can be changed by user, this is shown in Cloud as time of a scan.
                "scanStartTimestamp": scan._scan_start_timestamp,  # Actual time when the scan started.
                "scanEndTimestamp": scan._scan_end_timestamp,  # Actual time when scan ended.
                "hasErrors": scan.has_error_logs(),
                "hasWarnings": scan.has_check_warns(),
                "hasFailures": scan.has_check_fails(),
                "metrics": [metric.get_cloud_dict() for metric in scan._metrics],
                # If archetype is not None, it means that check is automated monitoring
                "checks": checks,
                "queries": query_list,
                "automatedMonitoringChecks": automated_monitoring_checks,
                "profiling": profiling,
                "metadata": [
                    discover_tables_result.get_cloud_dict()
                    for discover_tables_result in scan._discover_tables_result_tables
                ],
                "logs": [log.get_cloud_dict() for log in scan._logs.logs],
                "sourceOwner": "soda-core",
            }
        )

    def upload_sample(
        self, scan: Scan, sample_rows: tuple[tuple], sample_file_name: str, samples_limit: int | None
    ) -> str:
        """
        :param sample_file_name: file name without extension
        :return: Soda Cloud file_id
        """

        # Keep the interface of this method backward compatible and allow for samples limit to be None, but do not continue with no limit in such case.
        if not samples_limit:
            samples_limit = 100

        try:
            scan_definition_name = scan._scan_definition_name
            scan_data_timestamp = scan._data_timestamp
            scan_folder_name = (
                f"{self._fileify(scan_definition_name)}"
                f'_{scan_data_timestamp.strftime("%Y%m%d%H%M%S")}'
                f'_{datetime.now(tz=timezone.utc).strftime("%Y%m%d%H%M%S")}'
            )

            with tempfile.TemporaryFile() as temp_file:
                for row in sample_rows[0:samples_limit]:
                    row = [self._serialize_file_upload_value(v) for v in row]
                    rows_json_str = json.dumps(row)
                    rows_json_bytes = bytearray(rows_json_str, "utf-8")
                    temp_file.write(rows_json_bytes)
                    temp_file.write(b"\n")

                temp_file_size_in_bytes = temp_file.tell()
                temp_file.seek(0)

                file_path = f"{scan_folder_name}/" + f"{sample_file_name}.jsonl"

                file_id = self._upload_sample_http(scan_definition_name, file_path, temp_file, temp_file_size_in_bytes)

                return file_id

        except Exception as e:
            self.logs.error(f"Soda cloud error: Could not upload sample {sample_file_name}", exception=e)

    def _upload_sample_http(self, scan_definition_name: str, file_path, temp_file, file_size_in_bytes: int):
        headers = {
            "Authorization": self._get_token(),
            "Content-Type": "application/octet-stream",
            "Is-V3": "true",
            "File-Path": file_path,
        }

        if file_size_in_bytes == 0:
            # because of https://github.com/psf/requests/issues/4215 we can't send content size
            # when the size is 0 since requests blocks then on I/O indefinitely
            self.logs.warning("Empty file upload detected, not sending Content-Length header")
        else:
            headers["Content-Length"] = str(file_size_in_bytes)

        upload_response = self._http_post(url=f"{self.api_url}/scan/upload", headers=headers, data=temp_file)
        upload_response_json = upload_response.json()

        if "fileId" not in upload_response_json:
            logger.error(f"No fileId received in response: {upload_response_json}")
            return None
        else:
            return upload_response_json["fileId"]

    def send_scan_results(self, scan: Scan):
        scan_results = self.build_scan_results(scan)
        scan_results["type"] = "sodaCoreInsertScanResults"
        return self._execute_command(scan_results, command_name="send_scan_results")

    def get_historic_data(self, historic_descriptor: HistoricDescriptor):
        measurements = {}
        check_results = {}

        if type(historic_descriptor) == HistoricMeasurementsDescriptor:
            measurements = self._get_historic_measurements(historic_descriptor)
        elif type(historic_descriptor) == HistoricCheckResultsDescriptor:
            check_results = self._get_historic_check_results(historic_descriptor)
        elif type(historic_descriptor) == HistoricChangeOverTimeDescriptor:
            measurements = self._get_historic_changes_over_time(historic_descriptor)
        else:
            logger.error(f"Invalid Historic Descriptor provided {historic_descriptor}")

        return {"measurements": measurements, "check_results": check_results}

    def is_samples_disabled(self) -> bool:
        return self.organization_configuration.get(self.ORG_CONFIG_KEY_DISABLE_COLLECTING_WH_DATA, True)

    def get_check_attributes_schema(self) -> list(dict):
        response_json_dict = self._execute_query(
            {"type": "sodaCoreAvailableCheckAttributes"},
            query_name="get_check_attributes",
        )

        if response_json_dict and "results" in response_json_dict:
            return response_json_dict["results"]

        return []

    def _get_historic_changes_over_time(self, hd: HistoricChangeOverTimeDescriptor):
        query = {
            "type": "sodaCoreHistoricMeasurements",
            "filter": {
                "type": "and",
                "andExpressions": [
                    {
                        "type": "equals",
                        "left": {"type": "columnValue", "columnName": "metric.identity"},
                        "right": {"type": "string", "value": hd.metric_identity},
                    }
                ],
            },
        }

        previous_time_start = None
        previous_time_end = None
        today = date.today()

        if hd.change_over_time_cfg.same_day_last_week:
            last_week = today - timedelta(days=7)
            previous_time_start = datetime(
                year=last_week.year, month=last_week.month, day=last_week.day, tzinfo=timezone.utc
            )
            previous_time_end = datetime(
                year=last_week.year,
                month=last_week.month,
                day=last_week.day,
                hour=23,
                minute=59,
                second=59,
                tzinfo=timezone.utc,
            )

        if previous_time_start and previous_time_end:
            query["filter"]["andExpressions"].append(
                {
                    "type": "greaterThanOrEqual",
                    "left": {"type": "columnValue", "columnName": "measurement.dataTime"},
                    "right": {"type": "time", "scanTime": False, "time": previous_time_start.isoformat()},
                }
            )
            query["filter"]["andExpressions"].append(
                {
                    "type": "lessThanOrEqual",
                    "left": {"type": "columnValue", "columnName": "measurement.dataTime"},
                    "right": {"type": "time", "scanTime": False, "time": previous_time_end.isoformat()},
                }
            )

        return self._execute_query(
            query,
            query_name="get_hisoric_changes_over_time",
        )

    def _get_historic_measurements(self, hd: HistoricMeasurementsDescriptor):
        historic_measurements = self._execute_query(
            {
                "type": "sodaCoreHistoricMeasurements",
                "limit": hd.limit,
                "filter": {
                    "type": "and",
                    "andExpressions": [
                        {
                            "type": "equals",
                            "left": {"type": "columnValue", "columnName": "metric.identity"},
                            "right": {"type": "string", "value": hd.metric_identity},
                        }
                    ],
                },
            },
            query_name="get_hisotric_check_results",
        )
        # Filter out historic_measurements not having 'value' key
        historic_measurements["results"] = [
            measurement for measurement in historic_measurements["results"] if "value" in measurement
        ]
        return historic_measurements

    def _get_historic_check_results(self, hd: HistoricCheckResultsDescriptor):
        return self._execute_query(
            {
                "type": "sodaCoreHistoricCheckResults",
                "limit": hd.limit,
                "filter": {
                    "type": "and",
                    "andExpressions": [
                        {
                            "type": "equals",
                            "left": {"type": "columnValue", "columnName": "check.identity"},
                            "right": {"type": "string", "value": hd.check_identity},
                        }
                    ],
                },
            },
            query_name="get_hisotric_check_results",
        )

    def _get_token(self):
        if not self.token:
            login_command = {"type": "login"}
            if self.api_key_id and self.api_key_secret:
                login_command["apiKeyId"] = self.api_key_id
                login_command["apiKeySecret"] = self.api_key_secret
            else:
                raise RuntimeError("No API KEY and/or SECRET provided ")

            login_response = self._http_post(
                url=f"{self.api_url}/command", headers=self.headers, json=login_command, request_name="get_token"
            )
            if login_response.status_code != 200:
                raise AssertionError(f"Soda Cloud login failed {login_response.status_code}. Check credentials.")
            login_response_json = login_response.json()

            self.token = login_response_json.get("token")
            assert self.token, "No token in login response?!"
        return self.token
