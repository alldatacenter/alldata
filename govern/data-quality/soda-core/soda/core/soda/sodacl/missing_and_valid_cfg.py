from __future__ import annotations

from soda.sodacl.location import Location

CFG_VALID_VALUES = "valid values"
CFG_INVALID_VALUES = "invalid values"
CFG_VALID_FORMAT = "valid format"
CFG_INVALID_FORMAT = "invalid format"
CFG_VALID_REGEX = "valid regex"
CFG_INVALID_REGEX = "invalid regex"
CFG_VALID_MIN = "valid min"
CFG_VALID_MAX = "valid max"
CFG_VALID_MIN_LENGTH = "valid min length"
CFG_VALID_MAX_LENGTH = "valid max length"
CFG_VALID_LENGTH = "valid length"

CFG_MISSING_VALUES = "missing values"
CFG_MISSING_FORMAT = "missing format"
CFG_MISSING_REGEX = "missing regex"

CFG_MISSING_VALID_ALL = [
    CFG_VALID_VALUES,
    CFG_INVALID_VALUES,
    CFG_VALID_FORMAT,
    CFG_VALID_REGEX,
    CFG_INVALID_FORMAT,
    CFG_INVALID_REGEX,
    CFG_VALID_MIN,
    CFG_VALID_MAX,
    CFG_VALID_MIN_LENGTH,
    CFG_VALID_MAX_LENGTH,
    CFG_VALID_LENGTH,
    CFG_MISSING_VALUES,
    CFG_MISSING_FORMAT,
    CFG_MISSING_REGEX,
]


class MissingAndValidCfg:
    def __init__(self):
        self.missing_values: list[str] | None = None
        self.missing_values_location: Location | None = None
        self.missing_format: str | None = None
        self.missing_format_location: Location | None = None
        self.missing_regex: str | None = None
        self.missing_regex_location: Location | None = None
        # TODO
        # self.missing_expr: Optional[str] = None
        self.valid_values: list[str] | None = None
        self.valid_values_location: Location | None = None
        self.invalid_values: list[str] | None = None
        self.invalid_values_location: Location | None = None
        self.valid_format: str | None = None
        self.valid_format_location: Location | None = None
        self.invalid_format: str | None = None
        self.invalid_format_location: Location | None = None
        self.valid_regex: str | None = None
        self.valid_regex_location: Location | None = None
        self.invalid_regex: str | None = None
        self.invalid_regex_location: Location | None = None
        self.valid_length: int | None = None
        self.valid_length_location: Location | None = None
        self.valid_min_length: int | None = None
        self.valid_min_length_location: Location | None = None
        self.valid_max_length: int | None = None
        self.valid_max_length_location: Location | None = None
        self.valid_min: float | None = None
        self.valid_min_location: Location | None = None
        self.valid_max: float | None = None
        self.valid_max_location: Location | None = None
        # TODO
        # self.valid_expr: Optional[str] = None

    def get_identity_parts(self) -> list:
        from soda.execution.identity import Identity

        return [
            Identity.property("missing_values", self.missing_values),
            Identity.property("missing_format", self.missing_format),
            Identity.property("missing_regex", self.missing_regex),
            Identity.property("valid_values", self.valid_values),
            Identity.property("valid_format", self.valid_format),
            Identity.property("valid_regex", self.valid_regex),
            Identity.property("valid_length", self.valid_length),
            Identity.property("valid_min_length", self.valid_min_length),
            Identity.property("valid_max_length", self.valid_max_length),
            Identity.property("valid_min", self.valid_min),
            Identity.property("valid_max", self.valid_max),
        ]

    def is_empty(self) -> bool:
        return all([var is None for var in vars(self).values()])

    @staticmethod
    def merge(
        missing_and_valid_cfg: MissingAndValidCfg, column_configurations_cfg: ColumnConfigurationsCfg
    ) -> MissingAndValidCfg | None:
        if missing_and_valid_cfg is not None and missing_and_valid_cfg.is_empty():
            missing_and_valid_cfg = None
        if column_configurations_cfg is not None and column_configurations_cfg.is_empty():
            column_configurations_cfg = None

        if missing_and_valid_cfg is None and column_configurations_cfg is None:
            return None
        if missing_and_valid_cfg is not None and column_configurations_cfg is None:
            return missing_and_valid_cfg
        if missing_and_valid_cfg is None and column_configurations_cfg is not None:
            missing_and_valid_cfg = MissingAndValidCfg()
            missing_and_valid_cfg.__merge(column_configurations_cfg)
            return missing_and_valid_cfg

        # Merge
        missing_and_valid_cfg.__merge(column_configurations_cfg)
        return missing_and_valid_cfg

    def __merge(self, other: MissingAndValidCfg):
        if other.missing_values is not None:
            if self.missing_values is None:
                self.missing_values = []
                self.missing_values_location = other.missing_values_location
            self.missing_values.extend(other.missing_values)

        if self.missing_format is None and other.missing_format is not None:
            self.missing_format = other.missing_format
            self.missing_format_location = other.missing_format_location

        if self.missing_regex is None and other.missing_regex is not None:
            self.missing_regex = other.missing_regex
            self.missing_regex_location = other.missing_regex_location

        if other.valid_values is not None:
            if self.valid_values is None:
                self.valid_values = []
                self.valid_values_location = other.valid_values_location
            self.valid_values.extend(other.valid_values)

        if self.valid_format is None and other.valid_format is not None:
            self.valid_format = other.valid_format
            self.valid_format_location = other.valid_format_location

        if self.valid_regex is None and other.valid_regex is not None:
            self.valid_regex = other.valid_regex
            self.valid_regex_location = other.valid_regex_location

        if self.valid_length is None and other.valid_length is not None:
            self.valid_length = other.valid_length
            self.valid_length_location = other.valid_length_location

        if self.valid_min_length is None and other.valid_min_length is not None:
            self.valid_min_length = other.valid_min_length
            self.valid_min_length_location = other.valid_min_length_location

        if self.valid_max_length is None and other.valid_max_length is not None:
            self.valid_max_length = other.valid_max_length
            self.valid_max_length_location = other.valid_max_length_location

        if self.valid_min is None and other.valid_min is not None:
            self.valid_min = other.valid_min
            self.valid_min_location = other.valid_min_location

        if self.valid_max is None and other.valid_max is not None:
            self.valid_max = other.valid_max
            self.valid_max_location = other.valid_max_location
