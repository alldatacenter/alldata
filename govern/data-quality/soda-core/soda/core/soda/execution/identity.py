from dataclasses import dataclass
from datetime import timedelta
from hashlib import blake2b
from numbers import Number
from typing import Optional


class Identity:
    @staticmethod
    def property(field_name, value):
        return IdentityProperty(field_name, value)

    @staticmethod
    def create_identity(
        identity_type: str,
        data_source_scan: "DataSourceScan",
        partition: "Partition",
        column: "Column",
        name: Optional[str],
        is_automated_monitoring: bool,
        identity_parts: list,
    ):
        parts = [identity_type]
        if data_source_scan.scan._scan_definition_name:
            parts.append(data_source_scan.scan._scan_definition_name)
        parts.append(data_source_scan.data_source.data_source_name)

        table = partition.table if partition else None
        if table:
            parts.append(table.table_name)
        if partition and partition.partition_name is not None:
            parts.append(partition.partition_name)
        if column is not None:
            parts.append(column.column_name)
        if name:
            parts.append(name)
        if is_automated_monitoring:
            parts.append("automated_monitoring")
        if identity_parts:
            hash_builder = ConsistentHashBuilder()
            for identity_hash_part in identity_parts:
                hash_builder.add(identity_hash_part)
            hash = hash_builder.get_hash()
            if hash:
                parts.append(hash)

        return "-".join([str(p) for p in parts])


@dataclass
class IdentityProperty:
    field_name: str
    value: object


class ConsistentHashBuilder:
    def __init__(self, hash_string_length: int = 8):
        if hash_string_length % 2 != 0:
            raise AssertionError(f"hash_string_length must be divisible by 2: {hash_string_length} is not")
        self.hash_string_length = hash_string_length
        self.blake2b = None

    def get_blake2b(self) -> blake2b:
        # Lazy initialization of blake2b in order to return None in the self.get_hash(self) in case nothing was added
        if self.blake2b is None:
            self.blake2b = blake2b(digest_size=int(self.hash_string_length / 2))
        return self.blake2b

    def add(self, value: Optional[str]):
        from soda.sodacl.change_over_time_cfg import ChangeOverTimeCfg
        from soda.sodacl.location import Location
        from soda.sodacl.missing_and_valid_cfg import MissingAndValidCfg
        from soda.sodacl.schema_check_cfg import SchemaValidations
        from soda.sodacl.threshold_cfg import ThresholdCfg

        if value is None:
            return
        elif isinstance(value, str):
            self.get_blake2b().update(value.encode("utf-8"))
        elif isinstance(value, Number) or isinstance(value, bool):
            self.get_blake2b().update(str(value).encode("utf-8"))
        elif isinstance(value, list) or isinstance(value, dict):
            self.add_all(value)
        elif isinstance(value, timedelta):
            self.add(str(value))
        elif isinstance(value, IdentityProperty):
            if value.value is not None:
                self.add(value.field_name)
                self.add(value.value)
        elif (
            isinstance(value, Location)
            or isinstance(value, MissingAndValidCfg)
            or isinstance(value, ChangeOverTimeCfg)
            or isinstance(value, ThresholdCfg)
            or isinstance(value, SchemaValidations)
        ):
            self.add_all(value.get_identity_parts())
        else:
            raise AssertionError(f"Expected str, number or None, not {value}")

    def add_all(self, collection):
        if isinstance(collection, list):
            for e in collection:
                self.add(e)
        elif isinstance(collection, dict):
            for k, v in collection.items():
                self.add(k)
                self.add(v)
        elif collection is not None:
            raise AssertionError(f"Expected list, dict or None, not {collection}")

    def get_hash(self) -> str:
        return self.blake2b.hexdigest() if self.blake2b else None
