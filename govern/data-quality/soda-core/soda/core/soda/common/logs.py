from __future__ import annotations

import logging
import sys
from logging import Logger

from soda.common.log import Log, LogLevel
from soda.sodacl.location import Location


def configure_logging():
    sys.stderr = sys.stdout
    logging.getLogger("urllib3").setLevel(logging.WARNING)
    logging.getLogger("botocore").setLevel(logging.WARNING)
    logging.getLogger("pyathena").setLevel(logging.WARNING)
    logging.getLogger("faker").setLevel(logging.ERROR)
    logging.getLogger("snowflake").setLevel(logging.WARNING)
    logging.getLogger("matplotlib").setLevel(logging.WARNING)
    logging.getLogger("pyspark").setLevel(logging.ERROR)
    logging.getLogger("py4j").setLevel(logging.INFO)
    logging.basicConfig(
        level=logging.DEBUG,
        force=True,  # Override any previously set handlers.
        # https://docs.python.org/3/library/logging.html#logrecord-attributes
        # %(name)s
        format="%(message)s",
        handlers=[logging.StreamHandler(sys.stdout)],
    )


class Logs:
    def __init__(self, logger: Logger):
        self.logger: Logger = logger
        self.logs: list[Log] = []
        self.logs_buffer: list[Log] = []
        self.verbose: bool = False

    def error(
        self,
        message: str,
        location: Location | None = None,
        doc: str | None = None,
        exception: BaseException | None = None,
    ) -> None:
        self.log(
            level=LogLevel.ERROR,
            message=message,
            location=location,
            doc=doc,
            exception=exception,
        )

    def warning(
        self,
        message: str,
        location: Location | None = None,
        doc: str | None = None,
        exception: BaseException | None = None,
    ) -> None:
        self.log(
            level=LogLevel.WARNING,
            message=message,
            location=location,
            doc=doc,
            exception=exception,
        )

    def info(
        self,
        message: str,
        location: Location | None = None,
        doc: str | None = None,
        exception: BaseException | None = None,
    ) -> None:
        self.log(
            level=LogLevel.INFO,
            message=message,
            location=location,
            doc=doc,
            exception=exception,
        )

    def debug(
        self,
        message: str,
        location: Location | None = None,
        doc: str | None = None,
        exception: BaseException | None = None,
    ) -> None:
        if self.verbose:
            self.log(
                level=LogLevel.DEBUG,
                message=message,
                location=location,
                doc=doc,
                exception=exception,
            )

    def log(self, level, message, location, doc, exception):
        log = Log(
            level=level,
            message=message,
            location=location,
            doc=doc,
            exception=exception,
        )
        log.log_to_python_logging()
        self.logs.append(log)

    def log_into_buffer(self, level, message, location, doc, exception):
        log = Log(
            level=level,
            message=message,
            location=location,
            doc=doc,
            exception=exception,
        )
        self.logs_buffer.append(log)

    def flush_buffer(self):
        for log in self.logs_buffer:
            log.log_to_python_logging()
            self.logs.append(log)
        self.logs_buffer = []

    def error_into_buffer(
        self,
        message: str,
        location: Location | None = None,
        doc: str | None = None,
        exception: BaseException | None = None,
    ) -> None:
        self.log_into_buffer(
            level=LogLevel.ERROR,
            message=message,
            location=location,
            doc=doc,
            exception=exception,
        )

    def warning_into_buffer(
        self,
        message: str,
        location: Location | None = None,
        doc: str | None = None,
        exception: BaseException | None = None,
    ) -> None:
        self.log_into_buffer(
            level=LogLevel.WARNING,
            message=message,
            location=location,
            doc=doc,
            exception=exception,
        )

    def info_into_buffer(
        self,
        message: str,
        location: Location | None = None,
        doc: str | None = None,
        exception: BaseException | None = None,
    ) -> None:
        self.log_into_buffer(
            level=LogLevel.INFO,
            message=message,
            location=location,
            doc=doc,
            exception=exception,
        )

    def debug_into_buffer(
        self,
        message: str,
        location: Location | None = None,
        doc: str | None = None,
        exception: BaseException | None = None,
    ) -> None:
        if self.verbose:
            self.log_into_buffer(
                level=LogLevel.DEBUG,
                message=message,
                location=location,
                doc=doc,
                exception=exception,
            )

    def log_message_present(self, message: str, full_match: bool = False) -> bool:
        for log in self.logs:
            if full_match:
                if log.message == message:
                    return True
            else:
                if message in log.message:
                    return True
        return False
