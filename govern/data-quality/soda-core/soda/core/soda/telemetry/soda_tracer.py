import ast
import inspect
import textwrap
from functools import wraps
from typing import Dict, Optional

from opentelemetry import trace
from opentelemetry.trace.propagation.tracecontext import TraceContextTextMapPropagator
from opentelemetry.trace.span import Span
from opentelemetry.trace.status import Status, StatusCode
from soda.telemetry.soda_telemetry import SodaTelemetry

trace_context_propagator = TraceContextTextMapPropagator()
trace_context_carrier = {}

soda_telemetry = SodaTelemetry.get_instance()
tracer = trace.get_tracer_provider().get_tracer(__name__)


def get_decorators(function):
    """
    Fancy introspection - very WIP
    """
    decorators = {}

    def visit_FunctionDef(node):
        decorators[node.name] = {}
        for n in node.decorator_list:
            print(ast.dump(n))
            name = ""
            if isinstance(n, ast.Call):
                group = n.func.value.id
                name = n.func.attr if isinstance(n.func, ast.Attribute) else n.func.id

                for a in n.args:
                    print(ast.dump(a))
            else:
                group = n.attr if isinstance(n, ast.Attribute) else n.id
                name = None

            if group not in decorators[node.name]:
                decorators[node.name][group] = []

            if name:
                decorators[node.name][group].append({name})

    node_iter = ast.NodeVisitor()
    node_iter.visit_FunctionDef = visit_FunctionDef
    node_iter.visit(ast.parse(textwrap.dedent(inspect.getsource(function))))
    return decorators


def soda_trace(fn: callable):
    def _before_exec(span: Span, fn: callable):
        span.set_attribute("user_cookie_id", soda_telemetry.user_cookie_id)

    def _after_exec(span: Span, error: Optional[BaseException] = None):
        span.set_status(Status(StatusCode.OK))
        if str(error) == "3":
            # Only error code 3 means actual execution error, 1 and 2 are reserved for other use.
            span.set_status(Status(StatusCode.ERROR))

    @wraps(fn)
    def wrapper(*original_args, **original_kwargs):
        ctx = trace_context_propagator.extract(carrier=trace_context_carrier)
        with tracer.start_as_current_span(f"{fn.__module__}.{fn.__name__}", context=ctx) as span:
            trace_context_propagator.inject(carrier=trace_context_carrier)
            result = None
            try:
                _before_exec(span, fn)
                result = fn(*original_args, **original_kwargs)
                span.set_status(Status(StatusCode.OK))
                _after_exec(span)
            except BaseException as e:
                # Catch base exception to deal with system exit codes as well.
                _after_exec(span, e)
                # A bit of a workaround - re-raise the error so that stacktrace is intact. Open telemetry takes care of setting the error code.
                raise e
        return result

    return wrapper


def span_setup_function_args(args: Dict):
    for prefix, values in args.items():
        for key, value in values.items():
            soda_telemetry.set_attribute(f"{prefix}_{key}", value or "")
