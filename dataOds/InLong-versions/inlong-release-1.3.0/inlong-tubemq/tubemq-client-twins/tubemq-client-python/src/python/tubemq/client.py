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
import os
import tubemq_client
import tubemq_config
import tubemq_errcode
import tubemq_return
import tubemq_tdmsg  # pylint: disable=unused-import
import tubemq_message  # pylint: disable=unused-import


class Consumer(tubemq_client.TubeMQConsumer):
    def __init__(self,
                 master_addr,
                 group_name,
                 topic_list,
                 rpc_read_timeout_ms=20000,
                 consume_osition=tubemq_config.ConsumePosition.kConsumeFromLatestOffset,
                 conf_file=os.path.join(os.path.dirname(__file__), 'client.conf')):

        super(Consumer, self).__init__()

        consumer_config = tubemq_config.ConsumerConfig()
        consumer_config.setRpcReadTimeoutMs(rpc_read_timeout_ms)
        consumer_config.setConsumePosition(consume_osition)

        err_info = ''
        result = consumer_config.setMasterAddrInfo(err_info, master_addr)
        if not result:
            print("Set Master AddrInfo failure:", err_info)
            exit(1)

        result = consumer_config.setGroupConsumeTarget(err_info, group_name, topic_list)
        if not result:
            print("Set GroupConsume Target failure:", err_info)
            exit(1)

        result = tubemq_client.startTubeMQService(err_info, conf_file)
        if not result:
            print("StartTubeMQService failure:", err_info)
            exit(1)

        result = self.start(err_info, consumer_config)
        if not result:
            print("Initial consumer failure, error is:", err_info)
            exit(1)

        self.getRet = tubemq_return.ConsumerResult()
        self.confirm_result = tubemq_return.ConsumerResult()

    def receive(self):
        result = self.getMessage(self.getRet)
        if result:
            return self.getRet.getMessageList()
        else:
            # 2.2.1 if failure, check error code
            # print error message if errcode not in
            # [no partitions assigned, all partitions in use,
            #    or all partitons idle, reach max position]
            if not self.getRet.getErrCode() == tubemq_errcode.Result.kErrNotFound \
                    or not self.getRet.getErrCode() == tubemq_errcode.Result.kErrNoPartAssigned \
                    or not self.getRet.getErrCode() == tubemq_errcode.Result.kErrAllPartInUse \
                    or not self.getRet.getErrCode() == tubemq_errcode.Result.kErrAllPartWaiting:
                print('GetMessage failure, err_code=%d, err_msg is:%s',
                      self.getRet.getErrCode(), self.getRet.getErrMessage())

    def acknowledge(self):
        self.confirm(self.getRet.getConfirmContext(), True, self.confirm_result)

    def stop(self):
        err_info = ''
        result = self.shutDown()
        result = tubemq_client.stopTubeMQService(err_info)
        if not result:
            print("StopTubeMQService failure, reason is:" + err_info)
            exit(1)
