#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#

from __future__ import print_function
import tubemq_tdmsg


class TubeMsg:
    def __init__(self, header, data):
        self.header = header
        self.data = data

    def __str__(self):
        return 'TubeMsg (%d, %d)' % (self.header, self.data)

    @staticmethod
    def parse_tdmsg_type_msg(data):
        err_info = ''
        tube_tdmsg = tubemq_tdmsg.TubeMQTDMsg()
        parsedMsgs = []
        if tube_tdmsg.parseTDMsg(data, err_info):
            tube_datamap = tube_tdmsg.getAttr2DataMap()
            for parsed_data_val in tube_datamap.values():
                for data_it in parsed_data_val:
                    parsedMsgs.append(data_it.getData())
        else:
            print(err_info)
        return parsedMsgs
