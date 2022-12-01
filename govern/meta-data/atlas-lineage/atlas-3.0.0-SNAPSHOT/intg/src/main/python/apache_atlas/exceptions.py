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


class AtlasServiceException(Exception):
    """Exception raised for errors in API calls.

    Attributes:
        api -- api endpoint which caused the error
        response -- response from the server
    """

    def __init__(self, api, response):
        msg = ""

        if api:
            msg = "Metadata service API {method} : {path} failed".format(**{'method': api.method, 'path': api.path})

        if response.content is not None:
            status = response.status_code if response.status_code is not None else -1
            msg = "Metadata service API with url {url} and method {method} : failed with status {status} and " \
                  "Response Body is :{response}". \
                format(**{'url': response.url, 'method': api.method, 'status': status, 'response': response.json()})

        Exception.__init__(self, msg)
