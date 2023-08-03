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

package client

import (
	"math"
	"math/rand"
	"sync"

	"github.com/apache/inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/metadata"
	"github.com/apache/inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/util"
)

type RoundRobinPartitionRouter struct {
	steppedCounter int
	routerMap      map[string]int
	routerMu       sync.Mutex
}

func NewPartitionRouter() *RoundRobinPartitionRouter {
	return &RoundRobinPartitionRouter{
		steppedCounter: 0,
		routerMap:      make(map[string]int),
	}
}

func (router *RoundRobinPartitionRouter) GetPartition(message *Message, partitionList []*metadata.Partition) int {
	router.routerMu.Lock()
	defer router.routerMu.Unlock()

	if len(partitionList) == 0 {
		return -1
	}

	topicName := message.Topic
	_, ok := router.routerMap[topicName]
	if !ok {
		router.routerMap[topicName] = rand.Int()
	}

	partitionIndex := -1
	partitionSize := len(partitionList)
	for i := 0; i < partitionSize; i++ {
		router.routerMap[topicName]++
		partitionIndex = ((router.routerMap[topicName] & math.MaxInt) % partitionSize)
		if partitionList[partitionIndex].GetDelayTimestamp() < util.CurrentTimeMillis() {
			return partitionIndex
		}
	}

	router.steppedCounter++
	return (router.steppedCounter & math.MaxInt) % partitionSize
}
