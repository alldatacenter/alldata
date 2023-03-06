from typing import Tuple

from soda.sampler.sample import Sample
from soda.sampler.sample_schema import SampleColumn, SampleSchema


class DbSample(Sample):
    def __init__(self, cursor, data_source):
        self.cursor = cursor
        self.data_source = data_source
        self.rows = None

    def get_rows(self) -> Tuple[Tuple]:
        # This might be dangerous if a big number of rows is fetched, consider cleaning up the memory when this object is not needed any more.
        if not self.rows:
            self.rows = self.cursor.fetchall()

        return self.rows

    def get_schema(self) -> SampleSchema:
        return self._convert_python_db_schema_to_sample_schema(self.cursor.description)

    def _convert_python_db_schema_to_sample_schema(self, dbapi_description) -> SampleSchema:
        columns = SampleColumn.create_sample_columns(dbapi_description, self.data_source)
        return SampleSchema(columns=columns)
