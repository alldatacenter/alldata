from __future__ import annotations

from dataclasses import dataclass
from datetime import timedelta

from soda.execution.identity import Identity


@dataclass
class ThresholdCfg:
    # value must be greater than or equal to be ok
    gte: float | str = None
    # value must be greater than to be ok
    gt: float | str = None
    # value must be less than to be ok
    lt: float | str = None
    # value must be less than or equal to be ok
    lte: float | str = None

    is_split_zone: bool = False

    def get_identity_parts(self) -> list:
        return [
            Identity.property("gte", self.gte),
            Identity.property("gt", self.gt),
            Identity.property("lt", self.lt),
            Identity.property("lte", self.lte),
            Identity.property("is_split_zone", self.is_split_zone),
        ]

    def get_freshness_threshold(self) -> timedelta | None:
        if isinstance(self.gte, timedelta):
            return self.gte
        elif isinstance(self.gt, timedelta):
            return self.gt

    def get_inverse(self) -> ThresholdCfg:
        has_lower_limit = self.gt is not None or self.gte is not None
        has_upper_limit = self.lt is not None or self.lte is not None
        new_is_split_zone = not self.is_split_zone if has_lower_limit and has_upper_limit else self.is_split_zone
        return ThresholdCfg(
            gte=self.lt if self.lt is not None else None,
            gt=self.lte if self.lte is not None else None,
            lt=self.gte if self.gte is not None else None,
            lte=self.gt if self.gt is not None else None,
            is_split_zone=new_is_split_zone,
        )

    def resolve(self, f):
        """
        Resolve if the values are set to be strings
        :param f:
        :return:
        """
        if isinstance(self.lt, str):
            self.lt = float(f(self.lt))
        if isinstance(self.gte, str):
            self.gte = float(f(self.gte))
        if isinstance(self.gt, str):
            self.gt = float(f(self.gt))
        if isinstance(self.lte, str):
            self.lte = float(f(self.lte))
        return self

    def is_bad(self, value) -> bool:
        if self.is_split_zone:
            return ((self.gte is None or value >= self.gte) and (self.gt is None or value > self.gt)) or (
                (self.lt is None or value < self.lt) and (self.lte is None or value <= self.lte)
            )
        else:
            return (
                (self.gte is None or value >= self.gte)
                and (self.gt is None or value > self.gt)
                and (self.lt is None or value < self.lt)
                and (self.lte is None or value <= self.lte)
            )

    def is_valid_anomaly_threshold(self) -> bool:
        lower_limit = self.gte if self.gte is not None else self.gt
        return (
            isinstance(lower_limit, float)
            and 0 <= lower_limit <= 1
            and self.lt is None
            and self.lte is None
            and self.is_split_zone is False
        )

    def to_soda_cloud_diagnostics_json(self):
        json = {}
        if self.gte is not None:
            json["greaterThanOrEqual"] = self.gte
        if self.gt is not None:
            json["greaterThan"] = self.gt
        if self.lt is not None:
            json["lessThan"] = self.lt
        if self.lte is not None:
            json["lessThanOrEqual"] = self.lte
        return json
