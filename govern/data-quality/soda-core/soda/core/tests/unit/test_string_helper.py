import pytest
from soda.common.string_helper import string_matches_simple_pattern


@pytest.mark.parametrize(
    "input, pattern, should_match",
    [
        pytest.param("table_1", "table*", True),
        pytest.param("table_1", "*table", False),
        pytest.param("table_1", "*", True),
        pytest.param("table_1", "*_*", True),
        pytest.param("table-1", "*_*", False),
        pytest.param("something-table-1", "table-1*", False),
    ],
)
def test_string_matches_simple_pattern(input: str, pattern: str, should_match: bool):
    assert string_matches_simple_pattern(input, pattern) == should_match
