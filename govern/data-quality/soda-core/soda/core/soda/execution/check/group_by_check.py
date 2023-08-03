from __future__ import annotations

import copy

from soda.execution.check.check import Check
from soda.execution.check_outcome import CheckOutcome
from soda.execution.check_type import CheckType
from soda.execution.metric.metric import Metric
from soda.execution.partition import Partition

GROUP_BY_RESULTS = "group_by_results"


class GroupByCheck(Check):
    def __init__(
        self,
        check_cfg: GroupByCheckCfg,
        data_source_scan: DataSourceScan,
        partition: Partition,
    ):
        super().__init__(
            check_cfg=check_cfg,
            data_source_scan=data_source_scan,
            partition=partition,
            column=None,
        )

        self.check_value = None
        self.check_type = CheckType.LOCAL

        from soda.execution.metric.group_by_metric import GroupByMetric

        group_by_metric = data_source_scan.resolve_metric(
            GroupByMetric(
                data_source_scan=self.data_source_scan,
                partition=partition,
                query=self.check_cfg.query,
                check=self,
            )
        )
        self.metrics[GROUP_BY_RESULTS] = group_by_metric

    def evaluate(self, metrics: dict[str, Metric], historic_values: dict[str, object]):
        query_results = metrics[GROUP_BY_RESULTS].value
        group_limit = self.check_cfg.group_limit

        if len(query_results) > group_limit:
            raise Exception(
                f"Total number of groups {len(query_results)} exceeds configured group limit: {group_limit}"
            )

        fields = list(self.check_cfg.fields)
        group_check_cfgs = self.check_cfg.check_cfgs
        groups = [tuple(map(qr.get, fields)) for qr in query_results]

        group_checks = []

        for group in groups:
            for gcc in group_check_cfgs:
                if gcc.name is None:
                    raise Exception("name property is required for the group check")

                group_name = f"{','.join(str(v) for v in group)}"
                config = copy.deepcopy(gcc)
                config.name = gcc.name + f" [{group_name}]"
                config.source_configurations["group_value"] = f"[{group_name}]"
                column = ",".join(fields)
                gc = Check.create(
                    check_cfg=config, data_source_scan=self.data_source_scan, partition=self.partition, column=column
                )
                result = next(filter(lambda qr: tuple(map(qr.get, fields)) == group, query_results))
                if result is not None:
                    gc.check_value = result[config.metric_name]
                    metric = Metric(
                        self.data_source_scan,
                        self.partition,
                        column=None,
                        name=config.name,
                        check=None,
                        identity_parts=[],
                    )

                    # TODO fetch historic values, change over time checks will not work yet
                    # historic_values = {}
                    # if gc.historic_descriptors:
                    #     for hd_key, hd in gc.historic_descriptors.items():
                    #         print(f"hd_key: {hd_key}, hd: {hd}")
                    #         historic_values[hd_key] = self.data_source_scan.scan.__get_historic_data_from_soda_cloud_metric_store(hd)

                    metric.set_value(gc.check_value)
                    self.data_source_scan.scan._add_metric(metric)
                    gc.metrics = {config.metric_name: metric}
                    gc.evaluate(metrics=None, historic_values=None)

                    cloud_group_attr = {
                        "group": {
                            "identity": self.create_identity(with_datasource=True, with_filename=True),
                            "name": gcc.name,
                            "distinctLabel": group_name,
                        }
                    }
                    gc.cloud_dict.update(cloud_group_attr)
                    gc.dict.update(cloud_group_attr)
                group_checks.append(gc)

        self.data_source_scan.scan._checks.extend(group_checks)

        if all(gc.outcome == CheckOutcome.PASS for gc in group_checks):
            self.outcome = CheckOutcome.PASS
        elif any(gc.outcome == CheckOutcome.FAIL for gc in group_checks):
            self.outcome = CheckOutcome.FAIL
        else:
            self.outcome = CheckOutcome.PASS

    def get_cloud_diagnostics_dict(self) -> dict:
        group_by_diagnostics = {}
        return group_by_diagnostics

    def get_log_diagnostic_lines(self) -> list[str]:
        diagnostics_texts: list[str] = []
        return diagnostics_texts
