from __future__ import annotations

from dataclasses import dataclass


@dataclass
class SampleColumn:
    name: str
    type: str

    def get_cloud_dict(self):
        return {"name": self.name, "type": self.type}

    def get_dict(self):
        return {"name": self.name, "type": self.type}

    @staticmethod
    def create_sample_columns(dbapi_description, data_source) -> list[SampleColumn]:
        return [
            SampleColumn._convert_python_db_column_to_sample_column(dbapi_column, data_source)
            for dbapi_column in dbapi_description
        ]

    @staticmethod
    def _convert_python_db_column_to_sample_column(dbapi_column, data_source):
        type_code = dbapi_column[1]
        type_name = data_source.get_type_name(type_code)
        return SampleColumn(name=dbapi_column[0], type=type_name)


@dataclass
class SampleSchema:
    columns: list[SampleColumn]

    def get_dict(self):
        return [x.get_dict() for x in self.columns]
