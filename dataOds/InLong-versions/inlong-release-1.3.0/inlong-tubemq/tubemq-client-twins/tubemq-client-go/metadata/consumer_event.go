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

const (
	Disconnect     = 2
	OnlyDisconnect = 20
	Connect        = 1
	OnlyConnect    = 10
)

const (
	Todo       = 0
	Processing = 1
	Done       = 2
	Unknown    = -1
	Failed     = -2
)

// ConsumerEvent represents the metadata of a consumer event
type ConsumerEvent struct {
	rebalanceID   int64
	eventType     int32
	eventStatus   int32
	subscribeInfo []*SubscribeInfo
}

// NewEvent returns a new consumer event.
func NewEvent(rebalanceID int64, eventType int32, subscribeInfo []*SubscribeInfo) *ConsumerEvent {
	return &ConsumerEvent{
		rebalanceID:   rebalanceID,
		eventType:     eventType,
		subscribeInfo: subscribeInfo,
	}
}

// GetRebalanceID returns the rebalanceID of a consumer event.
func (c *ConsumerEvent) GetRebalanceID() int64 {
	return c.rebalanceID
}

// GetEventType returns the event type of a consumer event.
func (c *ConsumerEvent) GetEventType() int32 {
	return c.eventType
}

// GetEventStatus returns the event status of a consumer event.
func (c *ConsumerEvent) GetEventStatus() int32 {
	return c.eventStatus
}

// SetEventType sets the event type.
func (c *ConsumerEvent) SetEventType(eventType int32) {
	c.eventType = eventType
}

// SetEventStatus sets the event status.
func (c *ConsumerEvent) SetEventStatus(eventStatus int32) {
	c.eventStatus = eventStatus
}

// GetSubscribeInfo returns the list of SubscribeInfo of a consumer event.
func (c *ConsumerEvent) GetSubscribeInfo() []*SubscribeInfo {
	return c.subscribeInfo
}
