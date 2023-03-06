from functools import wraps
from typing import Dict, List, Tuple, Union

from soda.telemetry.soda_telemetry import MemorySpanExporter

telemetry_exporter = MemorySpanExporter.get_instance()


def telemetry_ensure_no_secrets(*o_args, **o_kwargs):
    default_secret_keys = ["secret", "password"]
    default_secret_values = ["secret", "password", "sodasql"]

    def iteritems_recursive(collection: Union[Dict, List, Tuple]):
        """Iterates over provided collection and visits every key

        Some magic is present:
            - tuples and lists are treated as dicts, with numeric indexes added for simplicity
            - "collection" value is yielded when value is not a simple type - this is so that the key in such case is not missed.
        """
        if isinstance(collection, dict):
            items = collection
        elif isinstance(collection, tuple) or isinstance(collection, list):
            items = {i: collection[i] for i in range(0, len(collection))}

        for key, value in items.items():
            if isinstance(value, dict) or isinstance(value, tuple) or isinstance(value, list):
                yield key, "collection"
                yield from iteritems_recursive(value)
            else:
                yield key, value

    def decorate(func):
        @wraps(func)
        def wrapper(*args, **kwargs):
            telemetry_exporter.reset()
            result = func(*args, **kwargs)

            secret_keys = o_kwargs.get("secret_keys", default_secret_keys)
            secret_values = o_kwargs.get("secret_values", default_secret_values)

            for span in telemetry_exporter.span_dicts:
                for key, value in iteritems_recursive(span):
                    error_msg = f"Forbidden telemetry key:value pair '{key}:{value}'."
                    assert key not in secret_keys, error_msg
                    assert value not in secret_values, error_msg
            return result

        return wrapper

    return decorate
