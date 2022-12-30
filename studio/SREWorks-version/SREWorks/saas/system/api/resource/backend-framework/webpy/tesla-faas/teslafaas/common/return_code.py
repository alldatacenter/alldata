# coding: utf-8
"""
Tesla HTTP API return codes. Referenced HTTP response code.

Summary:
    1xx Informational
    2xx Success
    3xx Redirection
    4xx Client Error
    5xx Server Error
"""

# OK, Success!
OK = 200
CREATED = 201

# Client error
BAD_REQ = 400  # Used for client argument errors
UNAUTHORIZED = 401
FORBIDDEN = 402
NOT_FOUND = 404
METHOD_NOT_ALLOWED = 405
CONFLICT = 409
PRECONDITION_FAILED = 412

# API Server error
INTERNAL_SERVER_ERROR = 500  # API server exception
NOT_IMPLEMENTED = 501
SERVICE_UNAVAILABLE = 503