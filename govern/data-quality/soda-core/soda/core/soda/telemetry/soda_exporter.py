import logging
from typing import Dict, Optional, Sequence

from opentelemetry.exporter.otlp.proto.http import Compression
from opentelemetry.exporter.otlp.proto.http.trace_exporter import OTLPSpanExporter
from opentelemetry.sdk.trace import ReadableSpan
from opentelemetry.sdk.trace.export import ConsoleSpanExporter, SpanExportResult

logger = logging.getLogger(__name__)


def get_soda_spans(spans: Sequence[ReadableSpan]) -> Sequence[ReadableSpan]:
    result = []
    for span in spans:
        if span.name.startswith("soda"):
            result.append(span)
        else:
            logger.debug(f"Open Telemetry: Skipping non-soda span '{span.name}'.")

    return result


class SodaConsoleSpanExporter(ConsoleSpanExporter):
    """Soda version of console exporter.

    Does not export any non-soda spans for security and privacy reasons."""

    def export(self, spans: Sequence[ReadableSpan]) -> SpanExportResult:
        return super().export(get_soda_spans(spans))


class SodaOTLPSpanExporter(OTLPSpanExporter):
    """Soda version of OTLP exporter.

    Does not export any non-soda spans for security and privacy reasons."""

    def __init__(
        self,
        endpoint: Optional[str] = None,
        certificate_file: Optional[str] = None,
        headers: Optional[Dict[str, str]] = None,
        timeout: Optional[int] = None,
        compression: Optional[Compression] = None,
    ):
        super().__init__(
            endpoint,
            certificate_file,
            headers,
            timeout,
            compression,
        )

    def export(self, spans: Sequence[ReadableSpan]) -> SpanExportResult:
        return super().export(get_soda_spans(spans))
