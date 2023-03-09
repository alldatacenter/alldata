"""Test platform utilities.
Currently, we only test the negative cases, running on non-notebook platform.
We may submit the test codes to databricks and synapse cluster to confirm the behavior in the future.
"""
from feathr.utils.platform import is_jupyter, is_databricks, is_synapse


def test_is_jupyter():
    assert not is_jupyter()


def test_is_databricks():
    assert not is_databricks()


def test_is_synapse():
    assert not is_synapse()
