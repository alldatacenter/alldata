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

package sub

import (
	"testing"

	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/config"
	"github.com/stretchr/testify/assert"
)

func TestNewSubInfo(t *testing.T) {
	address := "127.0.0.1:9092,127.0.0.1:9093?topic=Topic1&filters=12312323&filters=1212&topic=Topic2&filters=121212&filters=2321323&group=Group&tlsEnable=false&msgNotFoundWait=10000&heartbeatMaxRetryTimes=6"
	c, err := config.ParseAddress(address)
	assert.Nil(t, err)

	topicFilters := make(map[string][]string)
	topicFilters["Topic1"] = []string{"12312323", "1212"}
	topicFilters["Topic2"] = []string{"121212", "2321323"}

	s := NewSubInfo(c)
	assert.Equal(t, s.topics, []string{"Topic1", "Topic2"})
	assert.Equal(t, s.topicFilters, topicFilters)
	assert.Equal(t, s.topicFilter, map[string]bool{"Topic1": true, "Topic2": true})
}
