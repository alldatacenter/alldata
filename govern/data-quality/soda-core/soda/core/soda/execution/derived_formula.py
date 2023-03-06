from typing import Callable, Dict


class DerivedFormula:
    def __init__(self, function: Callable, metric_dependencies: Dict[str, "Metric"]):
        self.function: Callable = function
        self.metric_dependencies: Dict[str, "Metric"] = metric_dependencies

    def collect_failed_rows_sample_refs(self):
        if isinstance(self.metric_dependencies, dict):
            return [
                metric.failed_rows_sample_ref
                for metric in self.metric_dependencies.values()
                if metric.failed_rows_sample_ref is not None
            ]
        return []
