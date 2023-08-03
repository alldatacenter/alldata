from pyspark.sql import DataFrame
from pyspark.sql.functions import col

def add_new_dropoff_and_fare_amount_column(df: DataFrame):
    df = df.withColumn("new_lpep_dropoff_datetime", col("lpep_dropoff_datetime"))
    df = df.withColumn("new_fare_amount", col("fare_amount") + 1000000)
    return df

def add_new_fare_amount(df: DataFrame) -> DataFrame:
    df = df.withColumn("fare_amount_new", col("fare_amount") + 8000000)

    return df