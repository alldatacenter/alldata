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

// Package metadata defines all the metadata of the TubeMQ broker and producer.
package metadata

// Metadata represents the metadata of
type Metadata struct {
	node          *Node
	subscribeInfo *SubscribeInfo
	readStatus    int32
	reportTimes   bool
}

// GetNode returns the node of the metadata.
func (m *Metadata) GetNode() *Node {
	return m.node
}

// GetSubscribeInfo returns the SubscribeInfo of the metadata.
func (m *Metadata) GetSubscribeInfo() *SubscribeInfo {
	return m.subscribeInfo
}

// GetReadStatus returns the read status.
func (m *Metadata) GetReadStatus() int32 {
	return m.readStatus
}

// GetReportTimes returns the report times.
func (m *Metadata) GetReportTimes() bool {
	return m.reportTimes
}

// SetNode sets the node.
func (m *Metadata) SetNode(node *Node) {
	m.node = node
}

// SetSubscribeInfo sets the subscribeInfo.
func (m *Metadata) SetSubscribeInfo(sub *SubscribeInfo) {
	m.subscribeInfo = sub
}

// ReadStatus sets the status.
func (m *Metadata) SetReadStatus(status int32) {
	m.readStatus = status
}

// SetReportTimes sets the reportTimes.
func (m *Metadata) SetReportTimes(reportTimes bool) {
	m.reportTimes = reportTimes
}
