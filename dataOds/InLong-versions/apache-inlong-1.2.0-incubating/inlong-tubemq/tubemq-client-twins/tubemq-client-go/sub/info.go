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

// Package sub defines the subscription information of a client.
package sub

import (
	"strconv"
	"sync/atomic"
	"time"

	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/config"
	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/protocol"
)

// InValidOffset represents the offset which is invalid.
const InValidOffset = -2

// SubInfo represents the sub information of the client.
type SubInfo struct {
	clientID              string
	boundConsume          bool
	selectBig             bool
	sourceCount           int32
	sessionKey            string
	notAllocated          int32
	firstRegistered       int32
	subscribedTime        int64
	boundPartitions       string
	topics                []string
	topicConds            []string
	topicFilter           map[string]bool
	assignedPartitions    map[string]int64
	topicFilters          map[string][]string
	authInfo              *protocol.AuthorizedInfo
	masterCertificateInfo *protocol.MasterCertificateInfo
}

// NewSubInfo parses the subscription from the config to SubInfo.
func NewSubInfo(config *config.Config) *SubInfo {
	s := &SubInfo{
		boundConsume:    config.Consumer.BoundConsume,
		subscribedTime:  time.Now().UnixNano() / int64(time.Millisecond),
		firstRegistered: 1,
		notAllocated:    1,
		topics:          config.Consumer.Topics,
		topicFilters:    config.Consumer.TopicFilters,
	}
	s.topicConds = make([]string, 0, len(config.Consumer.TopicFilters))
	s.topicFilter = make(map[string]bool)
	for topic, filters := range config.Consumer.TopicFilters {
		cond := topic + "#"
		count := 0
		if len(filters) > 0 {
			s.topicFilter[topic] = true
		}
		for _, filter := range filters {
			if count > 0 {
				cond += ","
			}
			cond += filter
		}
		s.topicConds = append(s.topicConds, cond)
	}
	if config.Consumer.BoundConsume {
		s.sessionKey = config.Consumer.SessionKey
		s.sourceCount = int32(config.Consumer.SourceCount)
		s.selectBig = config.Consumer.SelectBig
		s.assignedPartitions = config.Consumer.PartitionOffset
		count := 0
		for partition, offset := range s.assignedPartitions {
			if count > 0 {
				s.boundPartitions += ","
			}
			s.boundPartitions += partition
			s.boundPartitions += "="
			s.boundPartitions += strconv.Itoa(int(offset))
			count++
		}
	}
	return s
}

// GetClientID returns the client ID.
func (s *SubInfo) GetClientID() string {
	return s.clientID
}

// IsFiltered returns whether a topic if filtered.
func (s *SubInfo) IsFiltered(topic string) bool {
	if filtered, ok := s.topicFilter[topic]; ok {
		return filtered
	}
	return false
}

// GetTopicFilters returns the topic filters.
func (s *SubInfo) GetTopicFilters() map[string][]string {
	return s.topicFilters
}

// GetAssignedPartOffset returns the assignedPartOffset of the given partitionKey.
func (s *SubInfo) GetAssignedPartOffset(partitionKey string) int64 {
	if s.isFirstRegistered() && s.boundConsume && s.IsNotAllocated() {
		if offset, ok := s.assignedPartitions[partitionKey]; ok {
			return offset
		}
	}
	return InValidOffset
}

// BoundConsume returns whether it is bondConsume.
func (s *SubInfo) BoundConsume() bool {
	return s.boundConsume
}

// GetSubscribedTime returns the subscribedTime.
func (s *SubInfo) GetSubscribedTime() int64 {
	return s.subscribedTime
}

// GetTopics returns the topics.
func (s *SubInfo) GetTopics() []string {
	return s.topics
}

// GetTopicConds returns the topicConds.
func (s *SubInfo) GetTopicConds() []string {
	return s.topicConds
}

// GetSessionKey returns the sessionKey.
func (s *SubInfo) GetSessionKey() string {
	return s.sessionKey
}

// SelectBig returns whether it is selectBig.
func (s *SubInfo) SelectBig() bool {
	return s.selectBig
}

// GetSourceCount returns the sourceCount.
func (s *SubInfo) GetSourceCount() int32 {
	return s.sourceCount
}

// GetBoundPartInfo returns the boundPartitions.
func (s *SubInfo) GetBoundPartInfo() string {
	return s.boundPartitions
}

// IsNotAllocated returns whether it is not allocated.
func (s *SubInfo) IsNotAllocated() bool {
	return atomic.LoadInt32(&s.notAllocated) == 1
}

// GetAuthorizedInfo returns the authInfo.
func (s *SubInfo) GetAuthorizedInfo() *protocol.AuthorizedInfo {
	return s.authInfo
}

// GetMasterCertificateInfo returns the masterCertificateInfo.
func (s *SubInfo) GetMasterCertificateInfo() *protocol.MasterCertificateInfo {
	return s.masterCertificateInfo
}

// SetNotFirstRegistered sets the firstRegistered to false.
func (s *SubInfo) SetNotFirstRegistered() {
	atomic.StoreInt32(&s.firstRegistered, 0)
}

// SetAuthorizedInfo sets the authorizedInfo.
func (s *SubInfo) SetAuthorizedInfo(auth *protocol.AuthorizedInfo) {
	s.authInfo = auth
}

// SetMasterCertificateInfo sets the masterCertificateInfo.
func (s *SubInfo) SetMasterCertificateInfo(info *protocol.MasterCertificateInfo) {
	s.masterCertificateInfo = info
}

// CASIsNotAllocated sets the notAllocated.
func (s *SubInfo) CASIsNotAllocated(expected int32, update int32) {
	atomic.CompareAndSwapInt32(&s.notAllocated, expected, update)
}

// SetClientID sets the clientID.
func (s *SubInfo) SetClientID(clientID string) {
	s.clientID = clientID
}

func (s *SubInfo) isFirstRegistered() bool {
	return atomic.LoadInt32(&s.firstRegistered) == 1
}
