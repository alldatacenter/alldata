#!/usr/bin/env python

#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import enum

APPLICATION_JSON         = 'application/json'


def non_null(obj, defValue):
    return obj if obj is not None else defValue

def type_coerce(obj, objType):
    if isinstance(obj, objType):
        ret = obj
    elif isinstance(obj, dict):
        ret = objType(obj)

        if callable(getattr(ret, 'type_coerce_attrs', None)):
            ret.type_coerce_attrs()
    else:
        ret = None

    return ret

def type_coerce_list(obj, objType):
    if isinstance(obj, list):
        return [ type_coerce(entry, objType) for entry in obj ]
    return None

def type_coerce_dict(obj, objType):
    if isinstance(obj, dict):
        ret = {}
        for k, v in obj.items():
            ret[k] = type_coerce(v, objType)
    else:
        ret = None

    return ret

def type_coerce_dict_list(obj, objType):
    if isinstance(obj, dict):
        ret = {}
        for k, v in obj.items():
            ret[k] = type_coerce_list(v, objType)
    else:
        ret = None

    return ret

def type_coerce_list_dict(obj, objType):
    if isinstance(obj, list):
        return [ type_coerce_dict(entry, objType) for entry in obj ]
    return None

class API:
    def __init__(self, path, method, expected_status, consumes=APPLICATION_JSON, produces=APPLICATION_JSON):
        self.path            = path
        self.method          = method
        self.expected_status = expected_status
        self.consumes        = consumes
        self.produces        = produces

    def format_path(self, params):
        return API(self.path.format(**params), self.method, self.expected_status, self.consumes, self.produces)


class HttpMethod(enum.Enum):
    GET    = "GET"
    PUT    = "PUT"
    POST   = "POST"
    DELETE = "DELETE"


class HTTPStatus:
    OK                     = 200
    CREATED                = 201
    ACCEPTED               = 202
    NO_CONTENT             = 204
    MOVED_PERMANENTLY      = 301
    SEE_OTHER              = 303
    NOT_MODIFIED           = 304
    TEMPORARY_REDIRECT     = 307
    BAD_REQUEST            = 400
    UNAUTHORIZED           = 401
    FORBIDDEN              = 403
    NOT_FOUND              = 404
    NOT_ACCEPTABLE         = 406
    CONFLICT               = 409
    GONE                   = 410
    PRECONDITION_FAILED    = 412
    UNSUPPORTED_MEDIA_TYPE = 415
    INTERNAL_SERVER_ERROR  = 500
    SERVICE_UNAVAILABLE    = 503


