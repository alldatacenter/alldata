from __future__ import annotations

import logging
import os
import re

import boto3
from helpers.data_source_fixture import DataSourceFixture

logger = logging.getLogger(__name__)


class AthenaDataSourceFixture(DataSourceFixture):
    def __init__(self, test_data_source: str):
        super().__init__(test_data_source)
        self.s3_test_dir = os.environ["ATHENA_S3_TEST_DIR"]
        if self.s3_test_dir.endswith("/"):
            self.s3_test_dir = self.s3_test_dir[:-1]

    def _build_configuration_dict(self, schema_name: str | None = None) -> dict:
        return {
            "data_source athena": {
                "type": "athena",
                "access_key_id": os.getenv("ATHENA_ACCESS_KEY_ID"),
                "secret_access_key": os.getenv("ATHENA_SECRET_ACCESS_KEY"),
                "region_name": os.getenv("ATHENA_REGION_NAME", "eu-west-1"),
                "staging_dir": f"{self.s3_test_dir}/staging-dir",
                "schema": schema_name if schema_name else os.getenv("ATHENA_SCHEMA"),
                "role_arn": os.getenv("ATHENA_ROLE_ARN"),
            }
        }

    def _drop_schema_if_exists(self):
        super()._drop_schema_if_exists()
        self._delete_s3_schema_files()

    def _drop_schema_if_exists_sql(self) -> str:
        return f"DROP SCHEMA IF EXISTS {self.schema_name} CASCADE"

    def _create_schema_if_not_exists_sql(self) -> str:
        return f"CREATE SCHEMA IF NOT EXISTS {self.schema_name}"

    def _create_test_table_sql_compose(self, qualified_table_name, columns_sql) -> str:
        table_part = qualified_table_name.replace('"', "")
        table_part = re.sub("[^0-9a-zA-Z]+", "_", table_part)
        location = f"{self._get_3_schema_dir()}/{table_part}"
        return f"CREATE EXTERNAL TABLE {qualified_table_name} ( \n{columns_sql} \n)LOCATION '{location}/'"

    def _get_3_schema_dir(self):
        return f"{self.s3_test_dir}/{self.schema_name}"

    def _delete_s3_schema_files(self):
        s3_schema_dir = self._get_3_schema_dir()
        logging.debug(f"Deleting all s3 files under %s...", s3_schema_dir)
        bucket = self._extract_s3_bucket(s3_schema_dir)
        folder = self._extract_s3_folder(s3_schema_dir)
        s3_client = self._create_s3_client()
        response = s3_client.list_objects_v2(Bucket=bucket, Prefix=folder)
        object_keys = self._extract_object_keys(response)
        logging.debug(f"Found {len(object_keys)} to be deleted")
        max_objects = 200
        assert len(object_keys) < max_objects, (
            f"This method is intended for tests and hence limited to a maximum of {max_objects} objects, "
            f"{len(object_keys)} objects exceeds the limit."
        )
        if len(object_keys) > 0:
            response: dict = s3_client.delete_objects(Bucket=bucket, Delete={"Objects": object_keys})
            deleted_list = response.get("Deleted") if isinstance(response, dict) else None
            deleted_count = (
                len(deleted_list) if isinstance(deleted_list, list) else "Unknown aws delete objects response"
            )
            logging.debug(f"Deleted {deleted_count}")

    def _create_s3_client(self):
        self.filter_false_positive_boto3_warning()
        aws_credentials = self.schema_data_source.aws_credentials
        aws_credentials = aws_credentials.resolve_role("soda_sql_test_cleanup")
        return boto3.client(
            "s3",
            region_name=aws_credentials.region_name,
            aws_access_key_id=aws_credentials.access_key_id,
            aws_secret_access_key=aws_credentials.secret_access_key,
            aws_session_token=aws_credentials.session_token,
        )

    def _extract_object_keys(self, response):
        object_keys = []
        if "Contents" in response:
            objects = response["Contents"]
            for summary in objects:
                key = summary["Key"]
                object_keys.append({"Key": key})
        return object_keys

    S3_URI_PATTERN = r"(^s3://)([^/]*)/(.*$)"

    @classmethod
    def _extract_s3_folder(cls, uri):
        return re.search(cls.S3_URI_PATTERN, uri).group(3)

    @classmethod
    def _extract_s3_bucket(cls, uri):
        return re.search(cls.S3_URI_PATTERN, uri).group(2)

    def filter_false_positive_boto3_warning(self):
        # see
        # https://github.com/boto/boto3/issues/454#issuecomment-380900404
        import warnings

        warnings.filterwarnings("ignore", category=ResourceWarning, message="unclosed <ssl.SSLSocket")
        warnings.filterwarnings("ignore", category=DeprecationWarning, message="the imp module is deprecated")
        warnings.filterwarnings("ignore", category=DeprecationWarning, message="Using or importing the ABCs")
