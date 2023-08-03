from dataclasses import dataclass
from typing import List, Optional

from soda.sodacl.check_cfg import CheckCfg
from soda.sodacl.location import Location


@dataclass
class GroupValidations:
    required_group_names: Optional[List[str]]
    forbidden_group_names: Optional[List[str]]
    is_group_addition_forbidden: bool
    is_group_deletion_forbidden: bool

    def has_change_validations(self):
        return self.is_group_addition_forbidden or self.is_group_deletion_forbidden


class GroupEvolutionCheckCfg(CheckCfg):
    def __init__(
        self,
        source_header: str,
        source_line: str,
        source_configurations: Optional[str],
        location: Location,
        name: Optional[str],
        query: str,
        warn_validations: GroupValidations,
        fail_validations: GroupValidations,
    ):
        super().__init__(source_header, source_line, source_configurations, location, name)
        self.warn_validations: GroupValidations = warn_validations
        self.fail_validations: GroupValidations = fail_validations
        self.name = name if name else "Group Evolution Check"
        self.query = query

    def has_change_validations(self) -> bool:
        return (self.warn_validations and self.warn_validations.has_change_validations()) or (
            self.fail_validations and self.fail_validations.has_change_validations()
        )
