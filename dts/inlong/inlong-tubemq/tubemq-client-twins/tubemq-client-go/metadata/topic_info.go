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

package metadata

type TopicInfo struct {
	node         *Node
	topic        string
	partitionNum int
	storeNum     int
}

// New topicinfo
func NewTopicInfo(node *Node, topic string, partitionNum int, storeNum int) *TopicInfo {
	return &TopicInfo{
		node:         node,
		topic:        topic,
		partitionNum: partitionNum,
		storeNum:     storeNum,
	}
}

func (p *TopicInfo) GetTopic() string {
	return p.topic
}

func (p *TopicInfo) GetStoreNum() int {
	return p.storeNum
}

func (p *TopicInfo) GetPartitionNum() int {
	return p.partitionNum
}

func (p *TopicInfo) GetNode() *Node {
	return p.node
}
