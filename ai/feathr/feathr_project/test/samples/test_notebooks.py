from pathlib import Path
import yaml

import pytest
try:
    import papermill as pm
    import scrapbook as sb
except ImportError:
    pass  # disable error while collecting tests for non-notebook environments


SAMPLES_DIR = (
    Path(__file__)
    .parent     # .../samples
    .parent     # .../test
    .parent     # .../feathr_project
    .parent     # .../feathr (root of the repo)
    .joinpath("docs", "samples")
)
NOTEBOOK_PATHS = {
    "nyc_taxi_demo": str(SAMPLES_DIR.joinpath("nyc_taxi_demo.ipynb")),
    "feature_embedding": str(SAMPLES_DIR.joinpath("feature_embedding.ipynb")),
    "fraud_detection_demo": str(SAMPLES_DIR.joinpath("fraud_detection_demo.ipynb")),
    "product_recommendation_demo_advanced": str(SAMPLES_DIR.joinpath("product_recommendation_demo_advanced.ipynb")),
}


@pytest.mark.notebooks
def test__nyc_taxi_demo(config_path, tmp_path):
    notebook_name = "nyc_taxi_demo"

    output_notebook_path = str(tmp_path.joinpath(f"{notebook_name}.ipynb"))

    print(f"Running {notebook_name} notebook as {output_notebook_path}")

    pm.execute_notebook(
        input_path=NOTEBOOK_PATHS[notebook_name],
        output_path=output_notebook_path,
        # kernel_name="python3",
        parameters=dict(
            FEATHR_CONFIG_PATH=config_path,
            USE_CLI_AUTH=False,
            REGISTER_FEATURES=False,
            SCRAP_RESULTS=True,
        ),
    )

    # Read results from the Scrapbook and assert expected values
    nb = sb.read_notebook(output_notebook_path)
    outputs = nb.scraps

    assert outputs["materialized_feature_values"].data["239"] == pytest.approx([1480., 5707.], abs=1.)
    assert outputs["materialized_feature_values"].data["265"] == pytest.approx([4160., 10000.], abs=1.)
    assert outputs["rmse"].data == pytest.approx(5., abs=2.)
    assert outputs["mae"].data == pytest.approx(2., abs=1.)


@pytest.mark.databricks
def test__feature_embedding(tmp_path):
    notebook_name = "feature_embedding"
    output_notebook_path = str(tmp_path.joinpath(f"{notebook_name}.ipynb"))

    print(f"Running {notebook_name} notebook as {output_notebook_path}")

    pm.execute_notebook(
        input_path=NOTEBOOK_PATHS[notebook_name],
        output_path=output_notebook_path,
        # kernel_name="python3",
        parameters=dict(
            DATABRICKS_NODE_SIZE="Standard_D4s_v5",  # use Dv5 due to the quota limit of Dv2 at US East 2 region.
            USE_CLI_AUTH=False,
            REGISTER_FEATURES=False,
            CLEAN_UP=True,
        ),
    )


@pytest.mark.notebooks
def test__fraud_detection_demo(config_path, tmp_path):
    notebook_name = "fraud_detection_demo"

    output_notebook_path = str(tmp_path.joinpath(f"{notebook_name}.ipynb"))

    print(f"Running {notebook_name} notebook as {output_notebook_path}")

    pm.execute_notebook(
        input_path=NOTEBOOK_PATHS[notebook_name],
        output_path=output_notebook_path,
        # kernel_name="python3",
        parameters=dict(
            FEATHR_CONFIG_PATH=config_path,
            USE_CLI_AUTH=False,
            SCRAP_RESULTS=True,
        ),
    )

    # Read results from the Scrapbook and assert expected values
    nb = sb.read_notebook(output_notebook_path)
    outputs = nb.scraps

    assert outputs["materialized_feature_values"].data == pytest.approx([False, 0, 9, 239.0, 1, 1, 239.0, 0.0], abs=1.)
    assert outputs["precision"].data > 0.5
    assert outputs["recall"].data > 0.5
    assert outputs["f1"].data > 0.5


@pytest.mark.notebooks
def test__product_recommendation_demo_advanced(config_path, tmp_path):
    notebook_name = "product_recommendation_demo_advanced"

    output_notebook_path = str(tmp_path.joinpath(f"{notebook_name}.ipynb"))

    print(f"Running {notebook_name} notebook as {output_notebook_path}")

    pm.execute_notebook(
        input_path=NOTEBOOK_PATHS[notebook_name],
        output_path=output_notebook_path,
        # kernel_name="python3",
        parameters=dict(
            FEATHR_CONFIG_PATH=config_path,
            USE_CLI_AUTH=False,
            REGISTER_FEATURES=False,
            SCRAP_RESULTS=True,
        ),
    )

    # Read results from the Scrapbook and assert expected values
    nb = sb.read_notebook(output_notebook_path)
    outputs = nb.scraps

    assert outputs["user_features"].data == pytest.approx([17, 300.0], abs=0.1)
    assert outputs["product_features"].data == pytest.approx([17.0], abs=0.1)
    assert outputs["rmse"].data == pytest.approx(0.49343, abs=2.0)
