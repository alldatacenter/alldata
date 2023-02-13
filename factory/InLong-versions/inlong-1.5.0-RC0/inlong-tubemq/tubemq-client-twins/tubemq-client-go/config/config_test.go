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

package config

import (
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
)

func TestParseAddress(t *testing.T) {
	address := "127.0.0.1:9092,127.0.0.1:9093?topic=Topic1&filters=12312323&filters=1212&topic=Topic2&filters=121212&filters=2321323&group=Group&tlsEnable=false&msgNotFoundWait=10000&heartbeatMaxRetryTimes=6"
	topicFilters := make(map[string][]string)
	topicFilters["Topic1"] = []string{"12312323", "1212"}
	topicFilters["Topic2"] = []string{"121212", "2321323"}
	c, err := ParseAddress(address)
	assert.Nil(t, err)
	assert.Equal(t, c.Consumer.Masters, "127.0.0.1:9092,127.0.0.1:9093")
	assert.Equal(t, c.Consumer.Topics, []string{"Topic1", "Topic2"})
	assert.Equal(t, c.Consumer.TopicFilters, topicFilters)
	assert.Equal(t, c.Consumer.Group, "Group")
	assert.Equal(t, c.Consumer.MsgNotFoundWait, 10000*time.Millisecond)

	assert.Equal(t, c.Net.TLS.Enable, false)

	assert.Equal(t, c.Heartbeat.MaxRetryTimes, 6)

	address = ""
	_, err = ParseAddress(address)
	assert.NotNil(t, err)

	address = "127.0.0.1:9092,127.0.0.1:9093?topics=Topic&ttt"
	_, err = ParseAddress(address)
	assert.NotNil(t, err)

	address = "127.0.0.1:9092,127.0.0.1:9093?topics=Topic&ttt=ttt"
	_, err = ParseAddress(address)
	assert.NotNil(t, err)

	address = "127.0.0.1:9092,127.0.0.1:9093?topic=Topic1&filters=12312323&filters=1212"
	c, err = ParseAddress(address)
	delete(topicFilters, "Topic2")
	assert.Nil(t, err)
	assert.Equal(t, c.Consumer.Topics, []string{"Topic1"})
	assert.Equal(t, c.Consumer.TopicFilters, topicFilters)

	address = "127.0.0.1:9092,127.0.0.1:9093?filters=1231232"
	_, err = ParseAddress(address)
	assert.NotNil(t, err)

	address = "127.0.0.1:9092,127.0.0.1:9093?filters=12312323&filters=1212"
	_, err = ParseAddress(address)
	assert.NotNil(t, err)

	address = "127.0.0.1:9092,127.0.0.1:9093?topic=Topic&group=group&filters=12312323&consumePosition=-1"
	c, err = ParseAddress(address)
	assert.Nil(t, err)
	err = c.ValidateConsumer()
	assert.Nil(t, err)

	address = "127.0.0.1:9092,127.0.0.1:9093?topic=Topic&group=group&filters=12312323&consumePosition=1"
	c, err = ParseAddress(address)
	assert.Nil(t, err)
	err = c.ValidateConsumer()
	assert.Nil(t, err)

	address = "127.0.0.1:9092,127.0.0.1:9093?topic=Topic&group=group&filters=12312323&consumePosition=0"
	c, err = ParseAddress(address)
	assert.Nil(t, err)
	err = c.ValidateConsumer()
	assert.Nil(t, err)

	address = "127.0.0.1:9092,127.0.0.1:9093?topic=Topic&group=group&filters=12312323&consumePosition=-2"
	c, err = ParseAddress(address)
	assert.Nil(t, err)
	err = c.ValidateConsumer()
	assert.NotNil(t, err)

	address = "127.0.0.1:9092,127.0.0.1:9093?topic=Topic&group=group&filters=12312323&consumePosition=2"
	c, err = ParseAddress(address)
	assert.Nil(t, err)
	err = c.ValidateConsumer()
	assert.NotNil(t, err)

	address = "127.0.0.1:9092,127.0.0.1:9093?topic=Topic&group=group&filters=12312323&consumePosition=a"
	c, err = ParseAddress(address)
	assert.NotNil(t, err)
}

func TestValidateGroup(t *testing.T) {
	config := NewDefaultConfig()
	err := config.validateGroup("")
	assert.NotNil(t, err)
	err = config.validateGroup("123-456[]")
	assert.NotNil(t, err)
	err = config.validateGroup("test123_456")
	assert.Nil(t, err)
	group := ""
	for i := 0; i < 1025; i++ {
		group += "t"
	}
	err = config.validateGroup(group)
	assert.NotNil(t, group)
}

func TestValidateConsumer(t *testing.T) {
	c := NewDefaultConfig()

	assert.NotNil(t, c.ValidateConsumer())
	WithConsumerMasters("masters")(c)
	assert.NotNil(t, c.ValidateConsumer())

	WithConsumerMasters("127.0.0.1:8000")(c)
	WithGroup("test-group")(c)
	assert.NotNil(t, c.ValidateConsumer())

	WithGroup("test_group")(c)
	assert.Nil(t, c.ValidateConsumer())

	WithTopics([]string{"topic1", "topic2"})(c)
	assert.Nil(t, c.ValidateConsumer())

	WithTopics([]string{"1topic, topic2"})(c)
	assert.NotNil(t, c.ValidateConsumer())

	topicFilters := map[string][]string{"1topic": {"filter1", "filter2"}, "topic2": {"filter3", "filter4"}}
	WithTopicFilters(topicFilters)(c)
	assert.NotNil(t, c.ValidateConsumer())

	topicFilters = map[string][]string{"topic1": {"-=-", "filter2"}, "topic2": {"filter3", "filter4"}}
	WithTopicFilters(topicFilters)(c)
	assert.NotNil(t, c.ValidateConsumer())

	topicFilters = map[string][]string{"topic1": {"filter1", "filter2"}, "topic2": {"filter3", "filter4"}}
	WithTopicFilters(topicFilters)(c)
	assert.Nil(t, c.ValidateConsumer())

	partitionOffset := map[string]int64{"181895251:test_1": 0, "181895251:test_2": 10}
	WithBoundConsume("", 0, true, partitionOffset)(c)
	assert.NotNil(t, c.ValidateConsumer())

	WithBoundConsume("11", 0, true, partitionOffset)(c)
	assert.NotNil(t, c.ValidateConsumer())

	partitionOffset = map[string]int64{"181895251:test_1:1": 0, "181895251:test_2:2": 10}
	WithBoundConsume("11", 0, true, partitionOffset)(c)
	assert.NotNil(t, c.ValidateConsumer())

	partitionOffset = map[string]int64{"181895251:topic1:1": 0, "181895251:topic2:2": 10}
	WithBoundConsume("11", 0, true, partitionOffset)(c)
	assert.Nil(t, c.ValidateConsumer())

	WithConsumePosition(2)(c)
	assert.NotNil(t, c.ValidateConsumer())

	WithConsumePosition(-2)(c)
	assert.NotNil(t, c.ValidateConsumer())

	WithConsumePosition(-1)(c)
	assert.Nil(t, c.ValidateConsumer())

	WithConsumePosition(0)(c)
	assert.Nil(t, c.ValidateConsumer())

	WithConsumePosition(1)(c)
	assert.Nil(t, c.ValidateConsumer())
}
