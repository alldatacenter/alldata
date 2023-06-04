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

import tubemq
import tubemq_message
import tubemq_errcode
import argparse
import time
import datetime

from threading import Lock 

kTotalCounter = 0
kSuccessCounter = 0
kFailCounter = 0
counter_lock = Lock()


# Reference: java producer: MixedUtils.buildTestData, only for demo
def build_test_data(msg_data_size):
    transmit_data = "This is a test data!"
    data = ""
    while (len(data) + len(transmit_data)) <= msg_data_size:
        data += transmit_data
    if len(data) < msg_data_size:
        data += transmit_data[:msg_data_size - len(data)]
    return data


def send_callback(error_code):
    global counter_lock
    global kTotalCounter
    global kSuccessCounter
    global kFailCounter
    with counter_lock:
        kTotalCounter += 1
        if error_code == tubemq_errcode.kErrSuccess:
            kSuccessCounter += 1
        else:
            kFailCounter += 1

parser = argparse.ArgumentParser()
parser.add_argument("--master_servers", type=str, required=True,
                    help="The master address(es) to connect to, the format is master1_ip:port[,master2_ip:port]")
parser.add_argument("--topics", type=str, required=True, help="The topic names.")
parser.add_argument("--conf_file", type=str, default="/tubemq-python/src/python/tubemq/client.conf", 
                    help="The path of configuration file.")
parser.add_argument("--sync_produce", type=int, default=0, help="Whether synchronous production.")
parser.add_argument("--msg_count", type=int, default=10, help="The number of messages to send.")
parser.add_argument("--msg_data_size", type=int, default=1000, help="The message size, (0, 1024 * 1024) bytes.")

params = parser.parse_args()

# start producer
producer = tubemq.Producer(params.master_servers)
producer.publish(params.topics)

# wait for the first heartbeath to master ready
time.sleep(10)

send_data = build_test_data(params.msg_data_size)
curr_time = datetime.datetime.now().strftime("%Y%m%d%H%M")
# for test, only take the first topic
first_topic = params.topics if isinstance(params.topics, str) else params.topics.split(",")[0]

t0 = time.time()
for i in range(params.msg_count):
    msg = tubemq_message.Message(first_topic, send_data, len(send_data))
    msg.putSystemHeader(str(i), curr_time)
    if params.sync_produce:
        res = producer.send(msg, is_sync=True)
        kTotalCounter += 1
        if res:
            kSuccessCounter += 1
        else:
            kFailCounter += 1
    else:
        producer.send(msg, callback=send_callback)

while kTotalCounter < params.msg_count:
    time.sleep(1e-6)

t1 = time.time()
print("Python producer send costs {} seconds.".format(t1 - t0))

# Stop producer
producer.stop()
 