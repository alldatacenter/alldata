from __future__ import annotations

import numbers
from datetime import date, datetime

from soda.common.logs import Logs


class AttributeHandler:
    def __init__(self, logs: Logs) -> None:
        self.logs = logs

    def validate(self, attributes: dict[str, any], schema: list[dict]) -> tuple[dict[str, any]]:
        """Validates given attributes against given schema. Returns a dict with valid and a dict with invalid attributes."""
        valid = {}
        invalid = {}
        for k, v in attributes.items():
            is_valid = self.validate_attribute(k, v, schema)
            if is_valid:
                valid[k] = v
            else:
                invalid[k] = v

        return valid, invalid

    def validate_attribute(self, key: str, value: any, schema: list[dict]) -> bool:
        is_valid = False
        schema_type = None

        for schema_item in schema:
            if key == schema_item["name"]:
                schema_type = schema_item["type"]
                validity_method = f"is_valid_{schema_type.lower()}"

                try:
                    is_valid = getattr(self, validity_method)(key, value, schema_item)
                except AttributeError:
                    self.logs.error(f"Unsupported attribute type '{schema_type}'.")

        if not schema_type:
            self.logs.error(f"Soda Cloud does not recognize '{key}' attribute name.")

        return is_valid

    def format_attribute(self, value: any):
        # Introduce formatting methods similar to validation methods if this gets more complex.
        if isinstance(value, date):
            value = datetime.combine(value, datetime.min.time())

        if isinstance(value, datetime):
            if value.tzinfo is None:
                value = value.replace(tzinfo=datetime.now().astimezone().tzinfo)
            return value.isoformat()

        # Numeric attributes need to be sent to Cloud as strings.
        if isinstance(value, numbers.Number):
            value = str(value)

        return value

    def is_valid_checkbox(self, key: str, value: any, schema: dict[str, any]) -> bool:
        if value not in schema["allowedValues"]:
            self._log_unrecognized_value(key, value, schema["allowedValues"])
            return False

        return True

    def is_valid_datetime(self, key: str, value: any, schema: dict[str, any]) -> bool:
        if isinstance(value, datetime) or isinstance(value, date):
            return True

        try:
            datetime.fromisoformat(value)
            return True
        except (TypeError, ValueError):
            self.logs.error(f"Soda Cloud expects an ISO formatted date or datetime value for the '{key}' attribute.")
            return False

    def is_valid_multiselect(self, key: str, value: any, schema: dict[str, any]) -> bool:
        if isinstance(value, list):
            invalid_values = []
            for v in value:
                if v not in schema["allowedValues"]:
                    invalid_values.append(v)

            if invalid_values:
                self._log_unrecognized_value(key, invalid_values, schema["allowedValues"])
                return False

            return True

        self.logs.error(
            f"Soda Cloud expects '{key}' to be a list. Refer to a list of valid '{key}' values in your Soda Cloud organization configuration."
        )
        return False

    def is_valid_number(self, key: str, value: any, schema: dict[str, any]) -> bool:
        if not isinstance(value, int) and not isinstance(value, float):
            self._log_unrecognized_type(key, value, type(value).__name__, ["int", "float"])
            return False

        return True

    def is_valid_singleselect(self, key: str, value: any, schema: dict[str, any]) -> bool:
        if value not in schema["allowedValues"]:
            self._log_unrecognized_value(key, value, schema["allowedValues"])
            return False

        return True

    def is_valid_text(self, key: str, value: any, schema: dict[str, any]) -> bool:
        if not isinstance(value, str):
            self._log_unrecognized_type(key, value, type(value).__name__, ["string"])
            return False
        return True

    def _log_unrecognized_value(self, key: str, value: any, allowed_values: list[any]) -> None:
        self.logs.error(
            f"Soda Cloud does not recognize '{key}': '{value}' attribute value. Valid attribute value(s): {allowed_values}."
        )

    def _log_unrecognized_type(self, key: str, value: any, type: str, expected_types: list(str)) -> None:
        self.logs.error(
            f"Soda Cloud does not recognize '{type}' type of attribute '{key}'. It expects the following type(s): {expected_types}"
        )
