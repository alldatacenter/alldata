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
	"time"

	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/client"
	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/config"
	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/log"
)

func main() {
	// Example for using config directly
	// topicFilters := map[string][]string{"topic1": {"filter1", "filter2"}, "topic2": {"filter3", "filter4"}}
	// partitionOffset := map[string]int64{"181895251:topic1:1": 0, "181895251:topic2:2": 10}
	// cfg := config.New(config.WithMasters("127.0.0.1:8099"),
	// config.WithGroup("group"),
	// For topic filters
	// config.WithTopicFilters(topicFilters),
	// For bound consume
	// config.WithBoundConsume("ss", 1, true, partitionOffset))

	// Example for parseAddress
	cfg, err := config.ParseAddress("127.0.0.1:8099?topic=test_1&group=test_group")
	// For topic filter
	// cfg, err := config.ParseAddress("127.0.0.1:8099?topic=Topic1&filters=12312323" +
	//	"&filters=1212&topic=Topic2&filters=121212&filters=2321323&group=test_group")
	if err != nil {
		log.Errorf("Failed to parse address", err.Error())
		panic(err)
	}
	c, err := client.NewConsumer(cfg)
	if err != nil {
		log.Errorf("new consumer error %s", err.Error())
		panic(err)
	}
	start := time.Now()
	for {
		elapsed := time.Since(start)
		if elapsed >= 10*time.Minute {
			break
		}
		cr, err := c.GetMessage()
		if err != nil {
			log.Errorf("Get message error %s", err.Error())
			continue
		}
		_, err = c.Confirm(cr.ConfirmContext, true)
		if err != nil {
			log.Errorf("Confirm error %s", err.Error())
			continue
		}
	}
	c.Close()
}
