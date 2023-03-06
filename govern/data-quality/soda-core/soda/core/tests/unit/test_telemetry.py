from typing import Dict, List

import pytest
from helpers.fixtures import soda_telemetry
from helpers.telemetry_helper import telemetry_ensure_no_secrets
from opentelemetry.sdk.trace import ReadableSpan
from soda.__version__ import SODA_CORE_VERSION
from soda.telemetry.memory_span_exporter import MemorySpanExporter
from soda.telemetry.soda_exporter import get_soda_spans
from soda.telemetry.soda_tracer import soda_trace

telemetry_exporter = MemorySpanExporter.get_instance()


def dict_has_keys(dict: Dict, keys: List[str]):
    for key in keys:
        assert key in dict


def test_basic_telemetry_structure():
    """Test for basic keys and values for any created span."""
    telemetry_exporter.reset()

    @soda_trace
    def mock_fn():
        pass

    mock_fn()

    assert len(telemetry_exporter.span_dicts) == 1

    span = telemetry_exporter.span_dicts[0]

    dict_has_keys(span, ["attributes", "context", "start_time", "end_time", "name", "resource", "status"])
    dict_has_keys(span["attributes"], ["user_cookie_id"])
    dict_has_keys(span["context"], ["span_id", "trace_id", "trace_state"])
    dict_has_keys(
        span["resource"]["attributes"],
        [
            "os.architecture",
            "os.type",
            "os.version",
            "platform",
            "python.implementation",
            "python.version",
            "service.name",
            "service.namespace",
            "service.version",
        ],
    )

    resource_attributes = span["resource"]["attributes"]
    assert resource_attributes["service.name"] == "soda"
    assert resource_attributes["service.namespace"] == "soda-core"
    assert resource_attributes["service.version"] == SODA_CORE_VERSION


def test_multi_spans():
    """Test multi spans, the relationship and hierarchy."""
    telemetry_exporter.reset()

    @soda_trace
    def mock_fn_1():
        pass

    @soda_trace
    def mock_fn_2():
        pass

    mock_fn_1()
    mock_fn_2()

    assert len(telemetry_exporter.span_dicts) == 2
    span_1 = telemetry_exporter.span_dicts[0]
    span_2 = telemetry_exporter.span_dicts[1]

    assert span_1["attributes"]["user_cookie_id"] == span_2["attributes"]["user_cookie_id"]
    assert span_1["context"]["trace_id"] == span_2["context"]["trace_id"]
    assert span_1["context"]["span_id"] == span_2["parent_id"]
    assert span_1["context"]["span_id"] != span_2["context"]["span_id"]


def test_add_argument():
    """Test that adding a telemetry argument adds it to a span."""
    telemetry_exporter.reset()

    @soda_trace
    def mock_fn():
        soda_telemetry.set_attribute("test", "something")

    mock_fn()

    assert len(telemetry_exporter.span_dicts) == 1

    span = telemetry_exporter.span_dicts[0]

    assert "test" in span["attributes"]
    assert span["attributes"]["test"] == "something"


@pytest.mark.parametrize("key, value", [("password", "something"), ("something", "secret")])
def test_fail_secret(key: str, value: str):
    """Test that 'no_secrets' test works."""
    telemetry_exporter.reset()

    with pytest.raises(AssertionError) as e:

        @telemetry_ensure_no_secrets()
        def test_fn():
            @soda_trace
            def mock_fn():
                soda_telemetry.set_attribute(key, value)

            mock_fn()

        test_fn()

    error_msg = str(e.value)

    assert "Forbidden telemetry" in error_msg
    assert key in error_msg or value in error_msg


def test_non_soda_span_filtering():
    spans_input = [
        ReadableSpan(name="test_1"),
        ReadableSpan(name="soda_test_span"),
        ReadableSpan(name="test_2"),
    ]

    spans_output = get_soda_spans(spans_input)
    assert len(spans_output) == 1
    assert spans_output[0].name == "soda_test_span"
