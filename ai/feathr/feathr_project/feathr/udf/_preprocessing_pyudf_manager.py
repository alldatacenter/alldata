import ast
import inspect
import os
import pickle
from pathlib import Path
from typing import List, Optional, Union

from jinja2 import Template

from feathr.definition.anchor import FeatureAnchor
from feathr.definition.source import HdfsSource


# Some metadata that are only needed by Feathr
FEATHR_PYSPARK_METADATA = 'generated_feathr_pyspark_metadata'
# UDFs that are provided by users and persisted by Feathr into this file.
# It will be uploaded into the Pyspark cluster as a dependency for execution
FEATHR_CLIENT_UDF_FILE_NAME = 'client_udf_repo.py'
# Pyspark driver code that is executed by the Pyspark driver
FEATHR_PYSPARK_DRIVER_FILE_NAME = 'feathr_pyspark_driver.py'
FEATHR_PYSPARK_DRIVER_TEMPLATE_FILE_NAME = 'feathr_pyspark_driver_template.py'
# Feathr provided imports for pyspark UDFs all go here
PROVIDED_IMPORTS = ['\nfrom pyspark.sql import SparkSession, DataFrame\n'] + \
                   ['from pyspark.sql.functions import *\n']


class _PreprocessingPyudfManager(object):
    """This class manages Pyspark UDF preprocessing related artifacts, like UDFs from users, the pyspark_client etc.
    """
    @staticmethod
    def build_anchor_preprocessing_metadata(anchor_list: List[FeatureAnchor], local_workspace_dir):
        """When the client build features, UDFs and features that need preprocessing will be stored as metadata. Those
        metadata will later be used when uploading the Pyspark jobs.
        """
        # feature names concatenated to UDF map
        # for example, {'f1,f2,f3': my_udf1, 'f4,f5':my_udf2}
        feature_names_to_func_mapping = {}
        # features that have preprocessing defined. This is used to figure out if we need to kick off Pyspark
        # preprocessing for requested features.
        features_with_preprocessing = []
        client_udf_repo_path = os.path.join(local_workspace_dir, FEATHR_CLIENT_UDF_FILE_NAME)
        metadata_path = os.path.join(local_workspace_dir, FEATHR_PYSPARK_METADATA)
        pyspark_driver_path = os.path.join(local_workspace_dir, FEATHR_PYSPARK_DRIVER_FILE_NAME)

        # delete the file if it already exists to avoid caching previous results
        for f in [client_udf_repo_path, metadata_path,  pyspark_driver_path]:
            if os.path.exists(f):
                os.remove(f)

        for anchor in anchor_list:
            # only support batch source preprocessing for now.
            if not hasattr(anchor.source, "preprocessing"):
                continue
            preprocessing_func = anchor.source.preprocessing
            if preprocessing_func:
                _PreprocessingPyudfManager.persist_pyspark_udf_to_file(preprocessing_func, local_workspace_dir)
                feature_names = [feature.name for feature in anchor.features]
                features_with_preprocessing = features_with_preprocessing + feature_names
                feature_names.sort()
                string_feature_list = ','.join(feature_names)
                if isinstance(anchor.source.preprocessing, str):
                    feature_names_to_func_mapping[string_feature_list] = _PreprocessingPyudfManager._parse_function_str_for_name(anchor.source.preprocessing)
                else:
                    # it's a callable function
                    feature_names_to_func_mapping[string_feature_list] = anchor.source.preprocessing.__name__

        if not features_with_preprocessing:
            return

        _PreprocessingPyudfManager.write_feature_names_to_udf_name_file(feature_names_to_func_mapping, local_workspace_dir)

        # Save necessary preprocessing-related metadata locally in your workspace
        # Typically it's used as a metadata for join/gen job to figure out if there is preprocessing UDF
        # exist for the features requested
        feathr_pyspark_metadata_abs_path = os.path.join(local_workspace_dir, FEATHR_PYSPARK_METADATA)
        with open(feathr_pyspark_metadata_abs_path, 'wb') as file:
            pickle.dump(features_with_preprocessing, file)


    @staticmethod
    def _parse_function_str_for_name(fn_str: str) -> str:
        """Use AST to parse the function string and get the name out.

        Args:
            fn_str: Function code in string.

        Returns:
            Name of the function.
        """
        if not fn_str:
            return None

        tree = ast.parse(fn_str)

        # tree.body contains a list of function definition objects parsed from the input string.
        # Currently, we only accept a single function.
        if len(tree.body) != 1 or not isinstance(tree.body[0], ast.FunctionDef):
            raise ValueError("provided code fragment is not a single function")

        # Get the function name from the function definition.
        return tree.body[0].name


    @staticmethod
    def persist_pyspark_udf_to_file(user_func, local_workspace_dir):
        """persist the pyspark UDF to a file in `local_workspace_dir` for later usage.
        The user_func could be either a string that represents a function body, or a callable object.
        The reason being - if we are defining a regular Python function, it will be a callable object;
        however if we retrieve features from registry, the current implementation is to use plain strings to store the function body. In that case, the user_fuc will be string.
        """
        if isinstance(user_func, str):
            udf_source_code = [user_func]
        else:
            udf_source_code = inspect.getsourcelines(user_func)[0]
        lines = []
        # Some basic imports will be provided
        lines = lines + PROVIDED_IMPORTS
        lines = lines + udf_source_code
        lines.append('\n')

        client_udf_repo_path = os.path.join(local_workspace_dir, FEATHR_CLIENT_UDF_FILE_NAME)

        # the directory may actually not exist yet, so create the directory first
        Path(local_workspace_dir).mkdir(parents=True, exist_ok=True)

        # Append to file, Create it if doesn't exist
        with open(client_udf_repo_path, "a+") as handle:
            print("".join(lines), file=handle)

    @staticmethod
    def write_feature_names_to_udf_name_file(feature_names_to_func_mapping, local_workspace_dir):
        """Persist feature names(sorted) of an anchor to the corresponding preprocessing function name to source path
        under the local_workspace_dir.
        """
        # indent in since python needs correct indentation
        # Don't change the indentation
        tm = Template("""
feature_names_funcs = {
{% for key, value in func_maps.items() %}
    "{{key}}" : {{value}},
{% endfor %}
}
        """)
        new_file = tm.render(func_maps=feature_names_to_func_mapping)

        full_file_name = os.path.join(local_workspace_dir, FEATHR_CLIENT_UDF_FILE_NAME)
        # Append to file, Create it if doesn't exist
        with open(full_file_name, "a+") as text_file:
            print(new_file, file=text_file)

    @staticmethod
    def prepare_pyspark_udf_files(feature_names: List[str], local_workspace_dir):
        """Prepare the Pyspark driver code that will be executed by the Pyspark cluster.
        The Pyspark driver code file has two parts:
            1. The driver code itself
            2. The UDFs.
            3. The features that need preprocessing in a map(from feature names to UDFs)
        """
        py_udf_files = []

        # Load pyspark_metadata which stores what features contains preprocessing UDFs
        feathr_pyspark_metadata_abs_path = os.path.join(local_workspace_dir, FEATHR_PYSPARK_METADATA)
        # if the preprocessing metadata file doesn't exist or is empty, then we just skip
        if not Path(feathr_pyspark_metadata_abs_path).is_file():
            return py_udf_files
        with open(feathr_pyspark_metadata_abs_path, 'rb') as pyspark_metadata_file:
            features_with_preprocessing = pickle.load(pyspark_metadata_file)
        # if there is not features that needs preprocessing, just return.
        if not features_with_preprocessing:
            return py_udf_files

        # Figure out if we need to preprocess via UDFs for requested features.
        # Only if the requested features contain preprocessing logic, we will load Pyspark. Otherwise just use Scala
        # spark.
        has_py_udf_preprocessing = False
        for feature_name in feature_names:
            if feature_name in features_with_preprocessing:
                has_py_udf_preprocessing = True
                break

        if has_py_udf_preprocessing:
            pyspark_driver_path = os.path.join(local_workspace_dir, FEATHR_PYSPARK_DRIVER_FILE_NAME)
            pyspark_driver_template_abs_path = str(Path(Path(__file__).parent / FEATHR_PYSPARK_DRIVER_TEMPLATE_FILE_NAME).absolute())
            client_udf_repo_path = os.path.join(local_workspace_dir, FEATHR_CLIENT_UDF_FILE_NAME)
            # write pyspark_driver_template_abs_path and then client_udf_repo_path
            filenames = [pyspark_driver_template_abs_path, client_udf_repo_path]

            with open(pyspark_driver_path, 'w') as outfile:
                for fname in filenames:
                    with open(fname) as infile:
                        for line in infile:
                            outfile.write(line)
            lines = [
                '\n',
                'print("pyspark_client.py: Preprocessing via UDFs and submit Spark job.")\n',
                'submit_spark_job(feature_names_funcs)\n',
                'print("pyspark_client.py: Feathr Pyspark job completed.")\n',
                '\n',
            ]
            with open(pyspark_driver_path, "a") as handle:
                print("".join(lines), file=handle)

            py_udf_files = [pyspark_driver_path]
        return py_udf_files
