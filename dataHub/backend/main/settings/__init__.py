try:
    from .local import *  # NOQA
except ModuleNotFoundError:
    from .development import *  # NOQA
