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


class RangerServiceException(Exception):
    """Exception raised for errors in API calls.

    Attributes:
        api      -- api endpoint which caused the error
        response -- response from the server
    """

    def __init__(self, api, response):
        self.method          = api.method.name
        self.path            = api.path
        self.expected_status = api.expected_status
        self.statusCode      = -1
        self.msgDesc         = None
        self.messageList     = None

        print(response)

        if api is not None and response is not None:
            if response.content:
              try:
                respJson         = response.json()
                self.msgDesc     = respJson['msgDesc']     if respJson is not None and 'msgDesc'     in respJson else None
                self.messageList = respJson['messageList'] if respJson is not None and 'messageList' in respJson else None
              except Exception:
                self.msgDesc     = response.content
                self.messageList = [ response.content ]

            self.statusCode  = response.status_code

        Exception.__init__(self, "{} {} failed: expected_status={}, status={}, message={}".format(self.method, self.path, self.expected_status, self.statusCode, self.msgDesc))
