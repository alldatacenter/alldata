// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package main

import (
	"strconv"
	"time"

	"github.com/apache/inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/client"
	"github.com/apache/inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/config"
	"github.com/apache/inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/log"
	"github.com/apache/inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/util"
)

func main() {
	// Example for parseAddress
	cfg, err := config.ParseAddress("127.0.0.1:8715?topic=demo_0")
	cfg.Producer.Topics = []string{"demo", "demo_0", "demo_1"}
	msgCount := 100
	msgDataSize := 1024
	sentData := util.BuildTestData(msgDataSize)

	if err != nil {
		log.Errorf("Failed to parse address", err.Error())
		panic(err)
	}

	// Register to master
	p, err := client.NewProducer(cfg)
	if err != nil {
		log.Errorf("new producer error %s", err.Error())
		panic(err)
	}

	// wait the first heartbeat completed
	time.Sleep(10 * time.Second)

	topicNum := len(cfg.Producer.Topics)
	for i := 0; i < msgCount; i++ {
		tmpSentData := sentData + strconv.Itoa(i)
		msg := client.Message{
			Topic:   cfg.Producer.Topics[i%topicNum],
			Data:    []byte(tmpSentData),
			DataLen: int32(len(tmpSentData)),
		}
		success, errCode, errMsg := p.SendMessage(&msg)

		if !success {
			println("Send fail: errCode: %d, errMsg: %s", errCode, errMsg)
		} else {
			println("Send success!!!")
		}
	}
}
