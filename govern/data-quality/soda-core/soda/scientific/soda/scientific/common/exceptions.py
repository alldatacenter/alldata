class LoggableException(Exception):
    """Base exception that will call soda-core wrapper to log instead of raising and crashing."""
