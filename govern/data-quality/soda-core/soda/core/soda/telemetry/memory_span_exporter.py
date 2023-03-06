import json
from typing import Dict, List, Sequence

from opentelemetry.sdk.trace import ReadableSpan
from opentelemetry.sdk.trace.export import SpanExporter, SpanExportResult


class MemorySpanExporter(SpanExporter):
    """Implementation of :class:`SpanExporter` that saves spans in memory.

    This class can be used for diagnostic purposes, multi-threaded scenarios etc.
    """

    __instance = None
    __spans = []

    @staticmethod
    def get_instance():
        if MemorySpanExporter.__instance is None:
            MemorySpanExporter()
        return MemorySpanExporter.__instance

    def __init__(self):
        if MemorySpanExporter.__instance is not None:
            raise Exception("This class is a singleton!")
        else:
            MemorySpanExporter.__instance = self

    def export(self, spans: Sequence[ReadableSpan]) -> SpanExportResult:
        for span in spans:
            self.__spans.append(span)
        return SpanExportResult.SUCCESS

    def reset(self):
        self.__spans = []

    @property
    def spans(self) -> List[ReadableSpan]:
        return self.__spans

    @property
    def span_dicts(self) -> List[Dict]:
        return [json.loads(span.to_json()) for span in self.spans]
