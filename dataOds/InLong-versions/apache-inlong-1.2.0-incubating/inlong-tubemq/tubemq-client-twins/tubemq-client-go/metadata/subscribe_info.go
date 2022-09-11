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

import (
	"fmt"
	"strings"

	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/errs"
)

// SubscribeInfo represents the metadata of the subscribe info.
type SubscribeInfo struct {
	group      string
	consumerID string
	partition  *Partition
}

// GetGroup returns the group name.
func (s *SubscribeInfo) GetGroup() string {
	return s.group
}

// GetConsumerID returns the consumer id.
func (s *SubscribeInfo) GetConsumerID() string {
	return s.consumerID
}

// GetPartition returns the partition.
func (s *SubscribeInfo) GetPartition() *Partition {
	return s.partition
}

// String returns the contents of SubscribeInfo as a string.
func (s *SubscribeInfo) String() string {
	return fmt.Sprintf("%s@%s#%s", s.consumerID, s.group, s.partition.String())
}

// NewSubscribeInfo constructs a SubscribeInfo from a given string.
// If the given is invalid, it will return error.
// The format of subscribeInfo string: consumerId@group#broker_info#topic:partitionId
func NewSubscribeInfo(subscribeInfo string) (*SubscribeInfo, error) {
	s := strings.SplitN(subscribeInfo, "#", 2)
	if len(s) == 1 {
		return nil, errs.ErrInvalidSubscribeInfoString
	}
	consumerInfo := strings.Split(s[0], "@")
	if len(consumerInfo) == 1 {
		return nil, errs.ErrInvalidSubscribeInfoString
	}
	partition, err := NewPartition(s[1])
	if err != nil {
		return nil, err
	}
	return &SubscribeInfo{
		group:      consumerInfo[1],
		consumerID: consumerInfo[0],
		partition:  partition,
	}, nil
}

// SetPartition sets the partition.
func (s *SubscribeInfo) SetPartition(partition *Partition) {
	s.partition = partition
}

// SetGroup sets the group.
func (s *SubscribeInfo) SetGroup(group string) {
	s.group = group
}

// SetConsumerID sets the consumerID.
func (s *SubscribeInfo) SetConsumerID(id string) {
	s.consumerID = id
}
