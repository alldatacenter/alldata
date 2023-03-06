from __future__ import annotations


class DiscoverTablesResultColumn:
    def __init__(self, column_name: str, column_type: str):
        self.column_name: str = column_name
        self.column_type: str = column_type

    def get_cloud_dict(self) -> dict:
        cloud_dict = {
            "columnName": self.column_name,
            "sourceDataType": self.column_type,
        }
        return cloud_dict

    def get_dict(self) -> dict:
        return {
            "columnName": self.column_name,
            "sourceDataType": self.column_type,
        }
