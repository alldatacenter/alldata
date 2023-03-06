from dataclasses import dataclass
from typing import Dict, List, Optional

from soda.sodacl.check_cfg import CheckCfg
from soda.sodacl.location import Location


@dataclass
class SchemaValidations:
    required_column_names: Optional[List[str]]
    required_column_types: Optional[Dict[str, str]]
    required_column_indexes: Optional[Dict[str, int]]
    forbidden_column_names: Optional[List[str]]
    is_column_addition_forbidden: bool
    is_column_deletion_forbidden: bool
    is_column_type_change_forbidden: bool
    is_column_index_change_forbidden: bool

    def has_change_validations(self):
        return (
            self.is_column_addition_forbidden
            or self.is_column_deletion_forbidden
            or self.is_column_type_change_forbidden
            or self.is_column_index_change_forbidden
        )


class SchemaCheckCfg(CheckCfg):
    def __init__(
        self,
        source_header: str,
        source_line: str,
        source_configurations: Optional[str],
        location: Location,
        name: Optional[str],
        warn_validations: SchemaValidations,
        fail_validations: SchemaValidations,
        is_automated_monitoring: bool = False,
    ):
        super().__init__(source_header, source_line, source_configurations, location, name)
        self.warn_validations: SchemaValidations = warn_validations
        self.fail_validations: SchemaValidations = fail_validations
        self.is_automated_monitoring = is_automated_monitoring
        self.name = name if name else "Schema Check"

    def has_change_validations(self) -> bool:
        return (self.warn_validations and self.warn_validations.has_change_validations()) or (
            self.fail_validations and self.fail_validations.has_change_validations()
        )
