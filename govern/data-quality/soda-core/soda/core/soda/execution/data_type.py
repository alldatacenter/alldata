from __future__ import annotations


class DataType:
    """
    Generic data types that data_sources can translate to specific types
    """

    TEXT = "text"
    INTEGER = "integer"
    DECIMAL = "decimal"
    DATE = "date"
    TIME = "time"
    TIMESTAMP = "timestamp"
    TIMESTAMP_TZ = "timestamptz"
    BOOLEAN = "boolean"

    @staticmethod
    def array(element_data_type: str) -> str:
        return f"array[{element_data_type}]"

    @staticmethod
    def struct(fields: dict[str, str]) -> str:
        field_data_types = ",".join(f"{field_name}:{field_type}" for field_name, field_type in fields.items())
        return f"struct[{field_data_types}]"
