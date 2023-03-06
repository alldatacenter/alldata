from soda.execution.metric.derived_metric import (
    derive_duplicate_percentage,
    derive_invalid_percentage,
    derive_missing_percentage,
)


def test_derive_missing_percentage():
    assert derive_missing_percentage({"missing_count": 0, "row_count": 0}) == 0.0
    assert derive_missing_percentage({"missing_count": 0, "row_count": 99}) == 0.0
    assert derive_missing_percentage({"missing_count": 1, "row_count": 1}) == 100.0
    assert derive_missing_percentage({"missing_count": 5, "row_count": 10}) == 50.0
    assert derive_missing_percentage({"missing_count": 40, "row_count": 320}) == 12.5
    assert derive_missing_percentage({"missing_count": 3, "row_count": 9}) == 33.33
    assert derive_missing_percentage({"missing_count": 6, "row_count": 9}) == 66.67


def test_derive_invalid_percentage():
    assert derive_invalid_percentage({"invalid_count": 0, "row_count": 0}) == 0.0
    assert derive_invalid_percentage({"invalid_count": 0, "row_count": 99}) == 0.0
    assert derive_invalid_percentage({"invalid_count": 1, "row_count": 1}) == 100.0
    assert derive_invalid_percentage({"invalid_count": 5, "row_count": 10}) == 50.0
    assert derive_invalid_percentage({"invalid_count": 40, "row_count": 320}) == 12.5
    assert derive_invalid_percentage({"invalid_count": 3, "row_count": 9}) == 33.33
    assert derive_invalid_percentage({"invalid_count": 6, "row_count": 9}) == 66.67


def test_derive_duplicate_percentage():
    assert derive_duplicate_percentage({"duplicate_count": 0, "row_count": 0}) == 0.0
    assert derive_duplicate_percentage({"duplicate_count": 0, "row_count": 99}) == 0.0
    assert derive_duplicate_percentage({"duplicate_count": 1, "row_count": 1}) == 100.0
    assert derive_duplicate_percentage({"duplicate_count": 5, "row_count": 10}) == 50.0
    assert derive_duplicate_percentage({"duplicate_count": 40, "row_count": 320}) == 12.5
    assert derive_duplicate_percentage({"duplicate_count": 3, "row_count": 9}) == 33.33
    assert derive_duplicate_percentage({"duplicate_count": 6, "row_count": 9}) == 66.67
