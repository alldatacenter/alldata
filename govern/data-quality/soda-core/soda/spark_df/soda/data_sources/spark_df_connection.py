from pyspark.sql.session import SparkSession
from soda.data_sources.spark_df_cursor import SparkDfCursor


class SparkDfConnection:
    def __init__(self, spark_session: SparkSession):
        self.spark_session = spark_session

    def cursor(self) -> SparkDfCursor:
        return SparkDfCursor(self.spark_session)

    def close(self):
        pass

    def commit(self):
        pass

    def rollback(self):
        pass
