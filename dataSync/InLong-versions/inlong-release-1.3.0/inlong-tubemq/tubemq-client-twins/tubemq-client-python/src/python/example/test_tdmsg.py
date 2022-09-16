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

from __future__ import unicode_literals
import time
import tubemq_config
import tubemq

topic_list = {'test_topic'}  # consum all of topic
# topic_list = {'test_topic': {'test_tid1', 'test_tid2'}}  # filter by tids
MASTER_ADDR = '127.0.0.1:8000'
GROUP_NAME = 'test_group'

# Start consumer
consumer = tubemq.Consumer(MASTER_ADDR, GROUP_NAME, topic_list,
                           consume_osition=tubemq_config.ConsumePosition.kConsumeFromFirstOffset)

# Test consumer
start_time = time.time()
tubeMsgsArr = []
while True:
    messageList = consumer.receive()
    if messageList:
        print("GetMessage success, msssage count =", len(messageList))
        for message in messageList:
            attributeMap = message.getProperties()
            attribute = ''
            for (key, value) in attributeMap.items():
                attribute = attribute + key + '=' + value + ','
            attribute = attribute[:-1]
            print("tube msg attribute ", attribute)
            data = message.getMsgData()
            decodeMsgs = tubemq.TubeMsg.parse_tdmsg_type_msg(message.getMsgData())
            for msgIter in decodeMsgs:
                tube_msg = tubemq.TubeMsg(attribute, msgIter)
                tubeMsgsArr.append(tube_msg)
                print("tube msg msgIter ", msgIter)

    consumer.acknowledge()

    # used for test, consume 10 minutes only
    stop_time = time.time()
    if stop_time - start_time > 10 * 60:
        break

# Stop consumer
consumer.stop()
