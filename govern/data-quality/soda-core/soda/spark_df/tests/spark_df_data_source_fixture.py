import decimal
import logging
from datetime import datetime

from helpers.data_source_fixture import DataSourceFixture
from helpers.test_scan import TestScan
from helpers.test_table import TestTable
from pyspark.sql import SparkSession, types
from soda.execution.data_type import DataType
from soda.scan import Scan

logger = logging.getLogger(__name__)


class SparkDfDataSourceFixture(DataSourceFixture):
    def __init__(self, test_data_source: str):
        super().__init__(test_data_source)
        self.spark_session = SparkSession.builder.master("local").appName("test").getOrCreate()

    def create_test_scan(self) -> TestScan:
        scan = super().create_test_scan()
        scan.add_spark_session(spark_session=self.spark_session)
        return scan

    def _test_session_starts(self):
        scan = Scan()
        scan.add_spark_session(spark_session=self.spark_session, data_source_name=self.data_source_name)
        self.data_source = scan._data_source_manager.get_data_source(self.data_source_name)
        scan._get_or_create_data_source_scan(self.data_source_name)

    def _drop_schema_if_exists(self):
        logger.debug("Schema drop is skipped for spark_df")

    def _test_session_ends(self):
        pass

    def _create_and_insert_test_table(self, test_table: TestTable):
        spark_columns = []
        for test_column in test_table.test_columns:
            column_name = test_column.name
            spark_type = self.build_spark_type(test_column.data_type)
            spark_column = types.StructField(column_name, spark_type)
            spark_columns.append(spark_column)

        spark_rows = []
        if test_table.values:
            for test_row in test_table.values:
                spark_row = {}
                for i in range(0, len(spark_columns)):
                    spark_column = spark_columns[i]
                    test_column = test_table.test_columns[i]
                    test_value = test_row[i]
                    spark_value = self.convert_test_value_to_spark_value(test_value, test_column, spark_column.dataType)
                    spark_row[spark_column.name] = spark_value
                spark_rows.append(spark_row)

        spark_schema = types.StructType(spark_columns)
        spark_session = self.data_source.connection.spark_session
        df = spark_session.createDataFrame(data=spark_rows, schema=spark_schema)
        logging.debug(f"Created table {test_table.unique_table_name}:")
        df.printSchema()
        df.show()
        df.createOrReplaceTempView(test_table.unique_table_name)

    @staticmethod
    def build_spark_type(data_type: str) -> types.DataType:
        if data_type.startswith("array[") and data_type.endswith("]"):
            element_data_type = data_type[6:-1].strip()
            element_spark_type = SparkDfDataSourceFixture.build_spark_type(element_data_type)
            return types.ArrayType(element_spark_type)

        if data_type.startswith("struct[") and data_type.endswith("]"):
            spark_field_types = []
            field_types = data_type[7:-1].strip().split(",")
            for field_type in field_types:
                field_type_parts = field_type.split(":", 1)
                field_name = field_type_parts[0]
                field_data_type = field_type_parts[1]
                field_spark_type = SparkDfDataSourceFixture.build_spark_type(field_data_type)
                spark_field_types.append(types.StructField(field_name, field_spark_type))
            return types.StructType(spark_field_types)

        if data_type == DataType.TEXT:
            return types.StringType()
        if data_type == DataType.INTEGER:
            return types.IntegerType()
        if data_type == DataType.DECIMAL:
            return types.DoubleType()
        if data_type == DataType.DATE:
            return types.DateType()
        if data_type == DataType.TIMESTAMP:
            return types.TimestampType()
        if data_type == DataType.TIMESTAMP_TZ:
            return types.TimestampType()
        if data_type == DataType.BOOLEAN:
            return types.BooleanType()

        if data_type == DataType.TIME:
            raise NotImplementedError(
                "Don't know how to convert time values to timestamp as Spark doesn't support times"
            )
        raise NotImplementedError(f"Test column type {data_type} not supported in spark dataframe testing")

    @staticmethod
    def convert_test_value_to_spark_value(test_value, test_column, sparkDataType):
        # see _acceptable_types in .venv/lib/python3.8/site-packages/pyspark/sql/types.py
        if test_value is None:
            return None
        if type(sparkDataType) in [types.FloatType, types.DoubleType]:
            return float(test_value)
        if isinstance(sparkDataType, types.DecimalType):
            return decimal.Decimal(test_value)
        if test_column.data_type == DataType.TIMESTAMP_TZ:
            return datetime.utcfromtimestamp(test_value.timestamp())
        return test_value
