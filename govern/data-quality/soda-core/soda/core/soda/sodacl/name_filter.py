from dataclasses import dataclass
from typing import Optional


@dataclass(eq=True, frozen=True)
class NameFilter:
    data_source_name_filter: Optional[str]
    table_name_filter: Optional[str]
    partition_name_filter: Optional[str]
    column_name_filter: Optional[str]
