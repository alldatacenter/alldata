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
	"sync"
	"sync/atomic"
	"time"

	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/client"
	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/config"
	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/log"
	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/tdmsg"
)

var lastPrintTime int64
var lastMsgCount int64
var lastPrintCount int64

func main() {
	// Example for using config directly
	// cfg := config.NewDefaultConfig()
	// For topic filter
	// cfg.Consumer.TopicFilters = map[string][]string{"topic1": {"filter1", "filter2"}, "topic2": {"filter3", "filter4"}}
	// For part offset
	// cfg.Consumer.PartitionOffset = map[string]int64{"181895251:test_1": 0, "181895251:test_2": 10}

	// Example for parseAddress
	cfg, err := config.ParseAddress("127.0.0.1:8099?topic=test_1&group=test_group")
	// For topic filter
	// cfg, err := config.ParseAddress("127.0.0.1:8099?topic=Topic1&filters=12312323&filters=1212&" +
	//	"topic=Topic2&filters=121212&filters=2321323&group=test_group")
	if err != nil {
		log.Errorf("Failed to parse address", err.Error())
		panic(err)
	}
	c, err := client.NewConsumer(cfg)
	if err != nil {
		log.Errorf("new consumer error %s", err.Error())
		panic(err)
	}
	numGoRoutine := 15
	var wg sync.WaitGroup
	for i := 0; i < numGoRoutine; i++ {
		wg.Add(1)
		go func(i int) {
			defer wg.Done()
			start := time.Now()
			for {
				elapsed := time.Since(start)
				if elapsed >= 10*time.Minute {
					break
				}
				cr, err := c.GetMessage()
				if err != nil {
					log.Errorf("Go routine %d, Get message error %s", i, err.Error())
					continue
				}
				_, err = c.Confirm(cr.ConfirmContext, true)
				if err != nil {
					log.Errorf("Go routine %d, Confirm error %s", i, err.Error())
					continue
				}
				//parseRawTypeMsg(int64(len(cr.Messages)))
				parseTDMsgTypeMsg(cr.Messages)
			}
			log.Infof("Go routine %d finished", i)
		}(i)
	}
	wg.Wait()
	c.Close()
}

func parseRawTypeMsg(cnt int64) {
	lastTime := atomic.LoadInt64(&lastPrintTime)
	atomic.AddInt64(&lastMsgCount, cnt)
	curCount := atomic.LoadInt64(&lastMsgCount)
	curTime := time.Now().UnixNano() / int64(time.Second)
	if curCount-atomic.LoadInt64(&lastPrintCount) >= 50000 || curTime-lastTime > 90 {
		atomic.StoreInt64(&lastPrintTime, curTime)
		log.Infof("Current time %d Current message count=%d", curTime, atomic.LoadInt64(&lastMsgCount))
		atomic.StoreInt64(&lastPrintCount, curCount)
	}
}

func parseTDMsgTypeMsg(messages []*client.Message) {
	for _, message := range messages {
		log.Infof("Message id is %d, topic is %s", message.ID, message.Topic)
		msg, err := tdmsg.New(message.Data)
		if err != nil {
			log.Errorf("fail to parse td msg")
			break
		}
		for attr, items := range msg.Attr2Data {
			if attrs, err := msg.ParseAttrValue(attr); err != nil {
				log.Errorf("parse attribute error, attr is %s, reason is %s", attr, err.Error())
			} else {
				log.Infof("parsed attrs %v", attrs)
				for k, v := range attrs {
					log.Infof("key is %s, value is %s", k, v)
				}
			}
			for _, item := range items {
				log.Infof("parsed msg data %v", item)
			}
		}
	}
}
