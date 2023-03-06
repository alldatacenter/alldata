from __future__ import annotations

import logging
from dataclasses import dataclass
from datetime import datetime, timezone
from enum import Enum
from textwrap import indent

from soda.common.exception_helper import get_exception_stacktrace
from soda.sodacl.location import Location

logger = logging.getLogger("soda.scan")


class LogLevel(Enum):
    WARNING = "WARNING"
    ERROR = "ERROR"
    INFO = "INFO"
    DEBUG = "DEBUG"


@dataclass
class Log:
    __log_level_mappings = {
        LogLevel.ERROR: logging.ERROR,
        LogLevel.WARNING: logging.WARNING,
        LogLevel.INFO: logging.INFO,
        LogLevel.DEBUG: logging.DEBUG,
    }
    __soda_cloud_level_mappings = {
        LogLevel.ERROR: "error",
        LogLevel.WARNING: "warning",
        LogLevel.INFO: "info",
        LogLevel.DEBUG: "debug",
    }

    def __init__(
        self,
        level: LogLevel,
        message: str,
        location: Location | None,
        doc: str | None,
        exception: BaseException | None,
        timestamp: datetime | None = None,
    ):
        self.level: LogLevel = level
        self.message: str = message
        self.location: Location | None = location
        self.doc: str | None = doc
        self.exception: BaseException | None = exception
        self.timestamp: datetime = timestamp if isinstance(timestamp, datetime) else datetime.now(tz=timezone.utc)
        self.index = self.get_next_index()

    __index = 0

    @classmethod
    def get_next_index(cls):
        cls.__index += 1
        return cls.__index

    def __str__(self):
        location_str = f" | {self.location}" if self.location else ""
        doc_str = f" | https://go.soda.io/{self.doc}" if self.doc else ""
        exception_str = f" | {self.exception}" if self.exception else ""
        return f"{self.level.value.ljust(7)}| {self.message}{location_str}{doc_str}{exception_str}"

    def get_cloud_dict(self) -> dict:
        log_cloud_dict = {
            "level": self.__soda_cloud_level_mappings[self.level],
            "message": self.message,
            "timestamp": self.timestamp,
            "index": self.index,
        }
        if self.location:
            location_cloud_dict = self.location.get_cloud_dict()
            log_cloud_dict["errorLocation"] = location_cloud_dict
            log_cloud_dict["location"] = location_cloud_dict
        if self.doc:
            log_cloud_dict["doc"] = self.doc
        return log_cloud_dict

    def get_dict(self) -> dict:
        return {
            "level": self.level,
            "message": self.message,
            "timestamp": self.timestamp,
            "index": self.index,
            "doc": self.doc if self.doc else None,
            "location": self.location.get_dict() if self.location else None,
        }

    @staticmethod
    def log_errors(error_logs):
        logger.info(f"ERRORS:")
        for error_log in error_logs:
            error_log.log_to_python_logging()

    def log_to_python_logging(self):
        python_log_level = Log.__log_level_mappings[self.level]
        time = datetime.now().strftime("%H:%M:%S")
        logger.log(python_log_level, f"[{time}] {self.message}")
        if self.exception is not None:
            exception_str = str(self.exception)
            exception_str = indent(text=exception_str, prefix="  | ")
            logger.log(python_log_level, exception_str)
            # Logging the stack trace
            from soda.scan import verbose

            if verbose:
                stacktrace = get_exception_stacktrace(self.exception)
                indented_stacktrace = indent(text=stacktrace, prefix="  | ")
                logger.log(python_log_level, f"  | Stacktrace:")
                logger.log(python_log_level, indented_stacktrace)
        if self.level in [LogLevel.WARNING, LogLevel.ERROR] and self.location is not None:
            logger.log(python_log_level, f"  +-> {self.location}")
        if isinstance(self.doc, str):
            logger.log(python_log_level, f"  +-> See https://go.soda.io/{self.doc}")
