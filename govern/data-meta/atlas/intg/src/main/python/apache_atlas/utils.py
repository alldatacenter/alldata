#!/usr/bin/env/python
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
import enum
import time
from functools import reduce

BASE_URI = "api/atlas/"
APPLICATION_JSON = 'application/json'
APPLICATION_OCTET_STREAM = 'application/octet-stream'
MULTIPART_FORM_DATA = 'multipart/form-data'
PREFIX_ATTR = "attr:"
PREFIX_ATTR_ = "attr_"

s_nextId = milliseconds = int(round(time.time() * 1000)) + 1


def next_id():
    global s_nextId

    s_nextId += 1

    return "-" + str(s_nextId)


def list_attributes_to_params(attributes_list, query_params=None):
    if query_params is None:
        query_params = {}

    for i, attr in enumerate(attributes_list):
        for key, value in attr.items():
            new_key = PREFIX_ATTR_ + str(i) + ":" + key
            query_params[new_key] = value

    return query_params


def attributes_to_params(attributes, query_params=None):
    if query_params is None:
        query_params = {}

    if attributes:
        for key, value in attributes:
            new_key = PREFIX_ATTR + key
            query_params[new_key] = value

    return query_params


def non_null(obj, defValue):
    return obj if obj is not None else defValue


def type_coerce(obj, objType):
    if isinstance(obj, objType):
        ret = obj
    elif isinstance(obj, dict):
        ret = objType(obj)

        ret.type_coerce_attrs()
    else:
        ret = None

    return ret


def type_coerce_list(obj, objType):
    if isinstance(obj, list):
        ret = []
        for entry in obj:
            ret.append(type_coerce(entry, objType))
    else:
        ret = None

    return ret


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


class API:
    def __init__(self, path, method, expected_status, consumes=APPLICATION_JSON, produces=APPLICATION_JSON):
        self.path = path
        self.method = method
        self.expected_status = expected_status
        self.consumes = consumes
        self.produces = produces

    def multipart_urljoin(self, base_path, *path_elems):
        """Join a base path and multiple context path elements. Handle single
        leading and trailing `/` characters transparently.

        Args:
            base_path (string): the base path or url (ie. `http://atlas/v2/`)
            *path_elems (string): multiple relative path elements (ie. `/my/relative`, `/path`)

        Returns:
            string: the result of joining the base_path with the additional path elements
        """
        def urljoin_pair(left, right):
            return "/".join([left.rstrip('/'), right.strip('/')])

        return reduce(urljoin_pair, path_elems, base_path)

    def format_path(self, params):
        return API(self.path.format(**params), self.method, self.expected_status, self.consumes, self.produces)

    def format_path_with_params(self, *params):
        request_path = self.multipart_urljoin(self.path, *params)
        return API(request_path, self.method, self.expected_status, self.consumes, self.produces)


class HTTPMethod(enum.Enum):
    GET = "GET"
    PUT = "PUT"
    POST = "POST"
    DELETE = "DELETE"


class HTTPStatus:
    OK = 200
    NO_CONTENT = 204
    SERVICE_UNAVAILABLE = 503
