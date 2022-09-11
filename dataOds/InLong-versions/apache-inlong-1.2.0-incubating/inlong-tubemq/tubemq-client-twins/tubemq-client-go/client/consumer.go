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

// Package client defines the api and information
// which can be exposed to user.
package client

import (
	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/remote"
)

// ConsumerResult of a consumption.
type ConsumerResult struct {
	TopicName      string
	ConfirmContext string
	PeerInfo       *PeerInfo
	Messages       []*Message
}

// ConfirmResult of a consumption.
type ConfirmResult struct {
	TopicName      string
	ConfirmContext string
	PeerInfo       *PeerInfo
}

var clientID uint64

// Consumer is an interface that abstracts behavior of TubeMQ's consumer
type Consumer interface {
	// GetMessage receive a single message.
	GetMessage() (*ConsumerResult, error)
	// Confirm the consumption of a message.
	Confirm(confirmContext string, consumed bool) (*ConfirmResult, error)
	// GetCurrConsumedInfo returns the consumptions of the consumer.
	GetCurrConsumedInfo() map[string]*remote.ConsumerOffset
	// Close closes the consumer client and release the resources.
	Close()
	// GetClientID returns the clientID of the consumer.
	GetClientID() string
}
