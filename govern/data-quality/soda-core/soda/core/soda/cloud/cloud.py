from __future__ import annotations

import logging
import math
import re
from abc import ABC
from typing import TYPE_CHECKING

import requests
from requests import Response
from soda.__version__ import SODA_CORE_VERSION
from soda.cloud.historic_descriptor import HistoricDescriptor
from soda.common.json_helper import JsonHelper
from soda.common.logs import Logs

logger = logging.getLogger(__name__)

if TYPE_CHECKING:
    from soda.scan import Scan


class Cloud(ABC):
    CSV_TEXT_MAX_LENGTH = math.inf

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
        # TODO: revisit if this is generic enough to be in core.
        self.host = host
        self.port = f":{port}" if port else ""
        self.scheme = scheme if scheme else "https"
        self.api_url = f"{self.scheme}://{self.host}{self.port}/api"
        self.api_key_id = api_key_id
        self.api_key_secret = api_key_secret
        self.token: str | None = token
        self.headers = {"User-Agent": f"SodaCore/{SODA_CORE_VERSION}"}
        self.logs = logs
        self._organization_configuration = None

    @property
    def organization_configuration(self) -> dict:
        return self._organization_configuration

    @staticmethod
    def build_scan_results(scan) -> dict:
        checks = [
            check.get_dict()
            for check in scan._checks
            if (check.outcome is not None or check.force_send_results_to_cloud is True) and check.archetype is None
        ]
        automated_monitoring_checks = [
            check.get_dict()
            for check in scan._checks
            if (check.outcome is not None or check.force_send_results_to_cloud is True) and check.archetype is not None
        ]

        profiling = [
            profile_table.get_dict()
            for profile_table in scan._profile_columns_result_tables + scan._sample_tables_result_tables
        ]

        query_list = []
        for query in scan._queries:
            query_list += query.get_dicts()

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
                "logs": [log.get_dict() for log in scan._logs.logs],
            }
        )

    @staticmethod
    def _serialize_file_upload_value(value):
        if value is None or isinstance(value, str) or isinstance(value, int) or isinstance(value, float):
            return value
        return str(value)

    def upload_sample(
        self, scan: Scan, sample_rows: tuple[tuple], sample_file_name: str, samples_limit: int | None
    ) -> str:
        raise NotImplementedError()

    def _fileify(self, name: str):
        return re.sub(r"\W+", "_", name).lower()

    def get_historic_data(self, historic_descriptor: HistoricDescriptor):
        raise NotImplementedError()

    def is_samples_disabled(self) -> bool:
        return False

    def _execute_query(self, query: dict, query_name: str):
        return self._execute_request("query", query, False, query_name)

    def _execute_command(self, command: dict, command_name: str):
        return self._execute_request("command", command, False, command_name)

    def _execute_request(self, request_type: str, request_body: dict, is_retry: bool, request_name: str):
        from soda.scan import verbose

        try:
            request_body["token"] = self._get_token()
            if verbose:
                logger.debug(f"{JsonHelper.to_json_pretty(request_body)}")
            response = self._http_post(
                url=f"{self.api_url}/{request_type}", headers=self.headers, json=request_body, request_name=request_name
            )
            response_json = response.json()
            if response.status_code == 401 and not is_retry:
                logger.debug("Authentication failed. Probably token expired. Re-authenticating...")
                self.token = None
                response_json = self._execute_request(request_type, request_body, True, request_name)
            elif response.status_code != 200:
                self.logs.error(
                    f"Error while executing Soda Cloud {request_type} response code: {response.status_code}"
                )
                if verbose:
                    self.logs.debug(response.text)
            return response_json
        except Exception as e:
            self.logs.error(f"Error while executing Soda Cloud {request_type}", exception=e)

    def _http_post(self, request_name: str = None, **kwargs) -> Response:
        response = requests.post(**kwargs)

        if request_name:
            trace_id = response.headers.get("X-Soda-Trace-Id")
            if trace_id:
                self.soda_cloud_trace_ids[request_name] = trace_id

        return response

    def _get_token(self):
        raise NotImplementedError()
