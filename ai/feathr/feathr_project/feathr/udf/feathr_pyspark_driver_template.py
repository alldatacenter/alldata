
from pyspark.sql import SparkSession, DataFrame, SQLContext
import sys
from pyspark.sql.functions import *

# This is executed in Spark driver
# The logger doesn't work in Pyspark so we just use print
print("Feathr Pyspark job started.")
spark = SparkSession.builder.appName('FeathrPyspark').getOrCreate()


def to_java_string_array(arr):
    """Convert a Python string list to a Java String array.
    """
    jarr = spark._sc._gateway.new_array(spark._sc._jvm.java.lang.String, len(arr))
    for i in range(len(arr)):
        jarr[i] = arr[i]
    return jarr


def submit_spark_job(feature_names_funcs):
    """Submit the Pyspark job to the cluster. This should be used when there is Python UDF preprocessing for sources.
    It loads the source DataFrame from Scala spark. Then preprocess the DataFrame with Python UDF in Pyspark. Later,
    the real Scala FeatureJoinJob or FeatureGenJob is executed with preprocessed DataFrames instead of the original
    source DataFrames.

        Args:
            feature_names_funcs: Map of feature names concatenated to preprocessing UDF function.
            For example {"f1,f2": df1, "f3,f4": df2} (the feature names in the key will be sorted)
    """
    # Prepare job parameters
    # sys.argv has all the arguments passed by submit job.
    # In pyspark job, the first param is the python file.
    # For example: ['pyspark_client.py', '--join-config', 'abfss://...', ...]
    has_gen_config = False
    has_join_config = False
    if '--generation-config' in sys.argv:
        has_gen_config = True
    if '--join-config' in sys.argv:
        has_join_config = True

    py4j_feature_job = None
    if has_gen_config and has_join_config:
        raise RuntimeError("Both FeatureGenConfig and FeatureJoinConfig are provided. "
                           "Only one of them should be provided.")
    elif has_gen_config:
        py4j_feature_job = spark._jvm.com.linkedin.feathr.offline.job.FeatureGenJob
        print("FeatureGenConfig is provided. Executing FeatureGenJob.")
    elif has_join_config:
        py4j_feature_job = spark._jvm.com.linkedin.feathr.offline.job.FeatureJoinJob
        print("FeatureJoinConfig is provided. Executing FeatureJoinJob.")
    else:
        raise RuntimeError("None of FeatureGenConfig and FeatureJoinConfig are provided. "
                           "One of them should be provided.")
    job_param_java_array = to_java_string_array(sys.argv)

    print("submit_spark_job: feature_names_funcs: ")
    print(feature_names_funcs)
    print("set(feature_names_funcs.keys()): ")
    print(set(feature_names_funcs.keys()))

    print("submit_spark_job: Load DataFrame from Scala engine.")

    dataframeFromSpark = py4j_feature_job.loadSourceDataframe(job_param_java_array, set(feature_names_funcs.keys())) # TODO: Add data handler support here
    print("Submit_spark_job: dataframeFromSpark: ")
    print(dataframeFromSpark)

    # per comment https://stackoverflow.com/a/54738984, use explicit way to initialize SQLContext
    # Otherwise it might fail when calling `DataFrame.collect()` or other APIs that's related with SQLContext
    # do not use `sql_ctx = SQLContext(spark)`
    sql_ctx = SQLContext(sparkContext=spark.sparkContext, sparkSession=spark)
    new_preprocessed_df_map = {}
    for feature_names, scala_dataframe in dataframeFromSpark.items():
        # Need to convert java DataFrame into python DataFrame
        py_df = DataFrame(scala_dataframe, sql_ctx)
        # Preprocess the DataFrame via UDF
        user_func = feature_names_funcs[feature_names]
        preprocessed_udf = user_func(py_df)
        new_preprocessed_df_map[feature_names] = preprocessed_udf._jdf

    print("submit_spark_job: running Feature job with preprocessed DataFrames:")
    print("Preprocessed DataFrames are: ")
    print(new_preprocessed_df_map)

    py4j_feature_job.mainWithPreprocessedDataFrame(job_param_java_array, new_preprocessed_df_map)
    return None

