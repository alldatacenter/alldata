from pathlib import Path
from pyspark.sql import SparkSession
import pytest

from feathr import FeathrClient


def pytest_addoption(parser):
    """Pytest command line argument options.
    E.g.
    `python -m pytest feathr_project/test/ --resource-prefix your_feathr_resource_prefix`
    """
    parser.addoption(
        "--config-path",
        action="store",
        default=str(Path(__file__).parent.resolve().joinpath("test_user_workspace", "feathr_config.yaml")),
        help="Test config path",
    )


@pytest.fixture
def config_path(request):
    return request.config.getoption("--config-path")


@pytest.fixture(scope="session")
def workspace_dir() -> str:
    """Workspace directory path containing data files and configs for testing."""
    return str(Path(__file__).parent.resolve().joinpath("test_user_workspace"))


@pytest.fixture(scope="function")
def feathr_client(workspace_dir) -> FeathrClient:
    """Test function-scoped Feathr client.
    Note, cluster target (local, databricks, synapse) maybe overriden by the environment variables set at test machine.
    """
    return FeathrClient(config_path=str(Path(workspace_dir, "feathr_config.yaml")))


@pytest.fixture(scope="module")
def spark() -> SparkSession:
    """Generate a spark session for tests."""
    # Set ui port other than the default one (4040) so that feathr spark job may not fail.
    spark_session = (
        SparkSession.builder
        .appName("tests")
        .config("spark.jars.packages", ",".join([
            "org.apache.spark:spark-avro_2.12:3.3.0",
            "io.delta:delta-core_2.12:2.1.1",
        ]))
        .config("spark.sql.extensions", "io.delta.sql.DeltaSparkSessionExtension")
        .config("spark.sql.catalog.spark_catalog", "org.apache.spark.sql.delta.catalog.DeltaCatalog")
        .config("spark.ui.port", "8080")
        .getOrCreate()
    )
    yield spark_session
    spark_session.stop()
