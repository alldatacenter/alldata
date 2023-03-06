from datetime import date, datetime

from helpers.data_source_fixture import DataSourceFixture
from soda.common.attributes_handler import AttributeHandler

test_schema = [
    {"type": "text", "id": "string", "label": "string", "name": "text"},
    {"type": "number", "id": "string", "label": "string", "name": "number"},
    {"type": "number", "id": "string", "label": "string", "name": "number_decimal"},
    {
        "type": "multiSelect",
        "allowedValues": ["a", "b", "c"],
        "id": "string",
        "label": "string",
        "name": "multiselect",
    },
    {
        "type": "singleSelect",
        "allowedValues": ["value", "some-value"],
        "id": "string",
        "label": "string",
        "name": "singleselect",
    },
    {"type": "datetime", "id": "string", "label": "string", "name": "datetime"},
    {"type": "datetime", "id": "string", "label": "string", "name": "datetime_str"},
    {"type": "datetime", "id": "string", "label": "string", "name": "date"},
    {"type": "datetime", "id": "string", "label": "string", "name": "date_str"},
    {"type": "checkbox", "allowedValues": [True], "id": "string", "name": "checkbox"},
]


def test_validation_passing(data_source_fixture: DataSourceFixture):
    scan = data_source_fixture.create_test_scan()
    attributes_handler = AttributeHandler(scan._logs)

    attributes = {
        "text": "something",
        "number": 1,
        "number_decimal": 1.1,
        "multiselect": ["a", "b", "c"],
        "singleselect": "some-value",
        "datetime": datetime(2022, 1, 1, 12, 0, 0),
        "datetime_str": "2022-01-01T12:00:00",
        "date": date(2022, 1, 1),
        "date_str": "2022-01-01",
        "checkbox": True,
    }

    valid, invalid = attributes_handler.validate(attributes, test_schema)

    assert sorted(valid.keys()) == sorted(attributes.keys())
    assert invalid == {}


def test_validation_failing(data_source_fixture: DataSourceFixture):
    scan = data_source_fixture.create_test_scan()
    attributes_handler = AttributeHandler(scan._logs)

    attributes = {
        "text": 1,
        "number": "1",
        "number_decimal": "1.1",
        "multiselect": ["d"],
        "singleselect": "non-existing",
        "datetime": "today",
        "datetime_str": 1,
        "date": "today",
        "date_str": 1,
        "checkbox": False,
    }

    valid, invalid = attributes_handler.validate(attributes, test_schema)

    assert sorted(invalid.keys()) == sorted(attributes.keys())
    assert valid == {}


def test_validation_unsupported_type(data_source_fixture: DataSourceFixture):
    scan = data_source_fixture.create_test_scan()
    attributes_handler = AttributeHandler(scan._logs)

    attributes = {
        "text": "something",
    }

    schema = [
        {"type": "unsupported", "id": "string", "label": "string", "name": "text"},
    ]

    valid, invalid = attributes_handler.validate(attributes, schema)

    assert sorted(invalid.keys()) == sorted(attributes.keys())
    assert valid == {}
    scan.assert_has_error("Unsupported attribute type 'unsupported'.")
