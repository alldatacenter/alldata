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

// Package remote defines the remote data which is returned from TubeMQ.
package remote

import (
	"fmt"
	"math"
	"sync"
	"sync/atomic"
	"time"

	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/errs"
	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/flowctrl"
	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/metadata"
	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/util"
)

// RmtDataCache represents the data returned from TubeMQ.
type RmtDataCache struct {
	consumerID         string
	groupName          string
	underGroupCtrl     int32
	lastCheck          int64
	defFlowCtrlID      int64
	groupFlowCtrlID    int64
	partitionSubInfo   map[string]*metadata.SubscribeInfo
	rebalanceResults   []*metadata.ConsumerEvent
	eventWriteMu       sync.Mutex
	eventReadMu        sync.Mutex
	metaMu             sync.Mutex
	dataBookMu         sync.Mutex
	brokerPartitions   map[string]map[string]bool
	qryPriorityID      int32
	partitions         map[string]*metadata.Partition
	usedPartitions     map[string]int64
	indexPartitions    []string
	partitionTimeouts  map[string]*time.Timer
	topicPartitions    map[string]map[string]bool
	partitionRegBooked map[string]bool
	partitionOffset    map[string]*ConsumerOffset
	groupHandler       *flowctrl.RuleHandler
	defHandler         *flowctrl.RuleHandler
	// EventCh is the channel for consumer to consume
	EventCh chan *metadata.ConsumerEvent
}

// ConsumerOffset of a consumption.
type ConsumerOffset struct {
	PartitionKey string
	CurrOffset   int64
	MaxOffset    int64
	UpdateTime   int64
}

// NewRmtDataCache returns a default rmtDataCache.
func NewRmtDataCache() *RmtDataCache {
	r := &RmtDataCache{
		defFlowCtrlID:      util.InvalidValue,
		groupFlowCtrlID:    util.InvalidValue,
		qryPriorityID:      int32(util.InvalidValue),
		partitionSubInfo:   make(map[string]*metadata.SubscribeInfo),
		brokerPartitions:   make(map[string]map[string]bool),
		partitions:         make(map[string]*metadata.Partition),
		usedPartitions:     make(map[string]int64),
		partitionTimeouts:  make(map[string]*time.Timer),
		topicPartitions:    make(map[string]map[string]bool),
		partitionRegBooked: make(map[string]bool),
		partitionOffset:    make(map[string]*ConsumerOffset),
		groupHandler:       flowctrl.NewRuleHandler(),
		defHandler:         flowctrl.NewRuleHandler(),
		EventCh:            make(chan *metadata.ConsumerEvent, 1),
	}
	return r
}

// GetUnderGroupCtrl returns the underGroupCtrl.
func (r *RmtDataCache) GetUnderGroupCtrl() bool {
	return atomic.LoadInt32(&r.underGroupCtrl) == 0
}

// GetDefFlowCtrlID returns the defFlowCtrlID.
func (r *RmtDataCache) GetDefFlowCtrlID() int64 {
	return r.defHandler.GetFlowCtrID()
}

// GetGroupFlowCtrlID returns the groupFlowCtrlID.
func (r *RmtDataCache) GetGroupFlowCtrlID() int64 {
	return r.groupHandler.GetFlowCtrID()
}

// GetGroupName returns the group name.
func (r *RmtDataCache) GetGroupName() string {
	return r.groupName
}

// GetSubscribeInfo returns the partitionSubInfo.
func (r *RmtDataCache) GetSubscribeInfo() []*metadata.SubscribeInfo {
	r.metaMu.Lock()
	defer r.metaMu.Unlock()
	subInfos := make([]*metadata.SubscribeInfo, 0, len(r.partitionSubInfo))
	for _, sub := range r.partitionSubInfo {
		subInfos = append(subInfos, sub)
	}
	return subInfos
}

// GetQryPriorityID returns the QryPriorityID.
func (r *RmtDataCache) GetQryPriorityID() int32 {
	return int32(r.groupHandler.GetQryPriorityID())
}

// PollEventResult polls the first event result from the rebalanceResults.
func (r *RmtDataCache) PollEventResult() *metadata.ConsumerEvent {
	r.eventWriteMu.Lock()
	defer r.eventWriteMu.Unlock()
	if len(r.rebalanceResults) > 0 {
		event := r.rebalanceResults[0]
		r.rebalanceResults = r.rebalanceResults[1:]
		return event
	}
	return nil
}

// GetPartitionByBroker returns the subscribed partitions of the given broker.
func (r *RmtDataCache) GetPartitionByBroker(broker *metadata.Node) []*metadata.Partition {
	r.metaMu.Lock()
	defer r.metaMu.Unlock()

	if partitionMap, ok := r.brokerPartitions[broker.String()]; ok {
		partitions := make([]*metadata.Partition, 0, len(partitionMap))
		for partition := range partitionMap {
			partitions = append(partitions, r.partitions[partition])
		}
		return partitions
	}
	return nil
}

// SetConsumerInfo sets the consumer information including consumerID and groupName.
func (r *RmtDataCache) SetConsumerInfo(consumerID string, group string) {
	r.consumerID = consumerID
	r.groupName = group
}

// UpdateDefFlowCtrlInfo updates the defFlowCtrlInfo.
func (r *RmtDataCache) UpdateDefFlowCtrlInfo(flowCtrlID int64, flowCtrlInfo string) {
	if flowCtrlID != r.defHandler.GetFlowCtrID() {
		r.defHandler.UpdateDefFlowCtrlInfo(true, util.InvalidValue, flowCtrlID, flowCtrlInfo)
	}
}

// UpdateGroupFlowCtrlInfo updates the groupFlowCtrlInfo.
func (r *RmtDataCache) UpdateGroupFlowCtrlInfo(qryPriorityID int32, flowCtrlID int64, flowCtrlInfo string) {
	if flowCtrlID != r.groupHandler.GetFlowCtrID() {
		r.groupHandler.UpdateDefFlowCtrlInfo(false, int64(qryPriorityID), flowCtrlID, flowCtrlInfo)
	}
	if int64(qryPriorityID) != r.groupHandler.GetQryPriorityID() {
		r.groupHandler.SetQryPriorityID(int64(qryPriorityID))
	}
	cur := time.Now().UnixNano() / int64(time.Millisecond)
	if cur-atomic.LoadInt64(&r.lastCheck) > 10000 {
		result := r.groupHandler.GetCurDataLimit(math.MaxInt64)
		if result != nil {
			atomic.StoreInt32(&r.underGroupCtrl, 1)
		} else {
			atomic.StoreInt32(&r.underGroupCtrl, 0)
		}
		atomic.StoreInt64(&r.lastCheck, cur)
	}
}

// OfferEventAndNotify offers a consumer event and notifies the consumer method and notify the consumer to consume.
func (r *RmtDataCache) OfferEventAndNotify(event *metadata.ConsumerEvent) {
	r.eventReadMu.Lock()
	defer r.eventReadMu.Unlock()
	r.rebalanceResults = append(r.rebalanceResults, event)
	e := r.rebalanceResults[0]
	r.rebalanceResults = r.rebalanceResults[1:]
	r.EventCh <- e
}

// ClearEvent clears all the events.
func (r *RmtDataCache) ClearEvent() {
	r.eventWriteMu.Lock()
	defer r.eventWriteMu.Unlock()
	r.rebalanceResults = r.rebalanceResults[:0]
}

// OfferEventResult offers a consumer event.
func (r *RmtDataCache) OfferEventResult(event *metadata.ConsumerEvent) {
	r.eventWriteMu.Lock()
	defer r.eventWriteMu.Unlock()

	r.rebalanceResults = append(r.rebalanceResults, event)
}

// RemoveAndGetPartition removes the given partitions.
func (r *RmtDataCache) RemoveAndGetPartition(subscribeInfos []*metadata.SubscribeInfo,
	processingRollback bool, partitions map[*metadata.Node][]*metadata.Partition) {
	if len(subscribeInfos) == 0 {
		return
	}
	r.metaMu.Lock()
	defer r.metaMu.Unlock()
	for _, sub := range subscribeInfos {
		partitionKey := sub.GetPartition().GetPartitionKey()
		if partition, ok := r.partitions[partitionKey]; ok {
			if _, ok := r.usedPartitions[partitionKey]; ok {
				if processingRollback {
					partition.SetLastConsumed(false)
				} else {
					partition.SetLastConsumed(true)
				}
			}
			if _, ok := partitions[partition.GetBroker()]; !ok {
				partitions[partition.GetBroker()] = []*metadata.Partition{partition}
			} else {
				partitions[partition.GetBroker()] = append(partitions[partition.GetBroker()], partition)
			}
			r.removeMetaInfo(partitionKey)
		}
		r.resetIdlePartition(partitionKey, false)
	}
}

func (r *RmtDataCache) removeMetaInfo(partitionKey string) {
	if partition, ok := r.partitions[partitionKey]; ok {
		if partitions, ok := r.topicPartitions[partition.GetTopic()]; ok {
			delete(partitions, partitionKey)
			if len(partitions) == 0 {
				delete(r.topicPartitions, partition.GetTopic())
			}
		}
		if partitions, ok := r.brokerPartitions[partition.GetBroker().String()]; ok {
			delete(partitions, partition.GetPartitionKey())
			if len(partitions) == 0 {
				delete(r.brokerPartitions, partition.GetBroker().String())
			}
		}
		delete(r.partitions, partitionKey)
		delete(r.partitionSubInfo, partitionKey)
	}
}

func (r *RmtDataCache) resetIdlePartition(partitionKey string, reuse bool) {
	delete(r.usedPartitions, partitionKey)
	delete(r.partitionTimeouts, partitionKey)
	r.removeFromIndexPartitions(partitionKey)
	if reuse {
		if _, ok := r.partitions[partitionKey]; ok {
			r.indexPartitions = append(r.indexPartitions, partitionKey)
		}
	}
}

// FilterPartitions returns the unsubscribed partitions.
func (r *RmtDataCache) FilterPartitions(subInfos []*metadata.SubscribeInfo) []*metadata.Partition {
	r.metaMu.Lock()
	defer r.metaMu.Unlock()
	unsubPartitions := make([]*metadata.Partition, 0, len(subInfos))
	if len(r.partitions) == 0 {
		for _, sub := range subInfos {
			unsubPartitions = append(unsubPartitions, sub.GetPartition())
		}
	} else {
		for _, sub := range subInfos {
			if _, ok := r.partitions[sub.GetPartition().GetPartitionKey()]; !ok {
				unsubPartitions = append(unsubPartitions, sub.GetPartition())
			}
		}
	}
	return unsubPartitions
}

// AddNewPartition append a new partition.
func (r *RmtDataCache) AddNewPartition(newPartition *metadata.Partition) {
	sub := &metadata.SubscribeInfo{}
	sub.SetPartition(newPartition)
	sub.SetConsumerID(r.consumerID)
	sub.SetGroup(r.groupName)

	r.metaMu.Lock()
	defer r.metaMu.Unlock()
	partitionKey := newPartition.GetPartitionKey()
	if _, ok := r.partitions[partitionKey]; !ok {
		r.partitions[partitionKey] = newPartition
		if partitions, ok := r.topicPartitions[newPartition.GetTopic()]; !ok {
			newPartitions := make(map[string]bool)
			newPartitions[partitionKey] = true
			r.topicPartitions[newPartition.GetTopic()] = newPartitions
		} else if _, ok := partitions[partitionKey]; !ok {
			partitions[partitionKey] = true
		}
		if partitions, ok := r.brokerPartitions[newPartition.GetBroker().String()]; !ok {
			newPartitions := make(map[string]bool)
			newPartitions[partitionKey] = true
			r.brokerPartitions[newPartition.GetBroker().String()] = newPartitions
		} else if _, ok := partitions[partitionKey]; !ok {
			partitions[partitionKey] = true
		}
		r.partitionSubInfo[partitionKey] = sub
	}
	r.resetIdlePartition(partitionKey, true)
}

// HandleExpiredPartitions handles the expired partitions.
func (r *RmtDataCache) HandleExpiredPartitions(wait time.Duration) {
	r.metaMu.Lock()
	defer r.metaMu.Unlock()
	expired := make(map[string]bool, len(r.usedPartitions))
	if len(r.usedPartitions) > 0 {
		curr := time.Now().UnixNano() / int64(time.Millisecond)
		for partition, time := range r.usedPartitions {
			if curr-time > wait.Milliseconds() {
				expired[partition] = true
				if p, ok := r.partitions[partition]; ok {
					p.SetLastConsumed(false)
				}
			}
		}
		if len(expired) > 0 {
			for partition := range expired {
				r.resetIdlePartition(partition, true)
			}
		}
	}
}

// RemovePartition removes the given partition keys.
func (r *RmtDataCache) RemovePartition(partitionKeys []string) {
	r.metaMu.Lock()
	defer r.metaMu.Unlock()

	for _, partitionKey := range partitionKeys {
		r.resetIdlePartition(partitionKey, false)
		r.removeMetaInfo(partitionKey)
	}
}

// IsFirstRegister returns whether the given partition is first registered.
func (r *RmtDataCache) IsFirstRegister(partitionKey string) bool {
	r.dataBookMu.Lock()
	defer r.dataBookMu.Unlock()

	if _, ok := r.partitionRegBooked[partitionKey]; !ok {
		r.partitionRegBooked[partitionKey] = true
		return true
	}
	return false
}

// GetCurConsumeStatus returns the current consumption status.
func (r *RmtDataCache) GetCurConsumeStatus() int32 {
	r.metaMu.Lock()
	defer r.metaMu.Unlock()

	if len(r.partitions) == 0 {
		return errs.RetErrNoPartAssigned
	}
	if len(r.indexPartitions) == 0 {
		if len(r.usedPartitions) > 0 {
			return errs.RetErrAllPartInUse
		} else {
			return errs.RetErrAllPartWaiting
		}
	}
	return 0
}

// SelectPartition returns a partition which is available to be consumed.
// If no partition can be use, an error will be returned.
func (r *RmtDataCache) SelectPartition() (*metadata.Partition, int64, error) {
	r.metaMu.Lock()
	defer r.metaMu.Unlock()

	if len(r.partitions) == 0 {
		return nil, 0, errs.ErrNoPartAssigned
	} else {
		if len(r.indexPartitions) == 0 {
			if len(r.usedPartitions) > 0 {
				return nil, 0, errs.ErrAllPartInUse
			} else {
				return nil, 0, errs.ErrAllPartWaiting
			}
		}
	}

	partitionKey := r.indexPartitions[0]
	r.indexPartitions = r.indexPartitions[1:]
	if partition, ok := r.partitions[partitionKey]; !ok {
		return nil, 0, errs.ErrAllPartInUse
	} else {
		bookedTime := time.Now().UnixNano() / int64(time.Millisecond)
		r.usedPartitions[partitionKey] = bookedTime
		return partition, bookedTime, nil
	}
}

// ReleasePartition releases a booked partition.
func (r *RmtDataCache) ReleasePartition(checkDelay bool, filterConsume bool,
	confirmContext string, isConsumed bool) error {
	partitionKey, bookedTime, err := util.ParseConfirmContext(confirmContext)
	if err != nil {
		return err
	}
	r.metaMu.Lock()
	defer r.metaMu.Unlock()

	if partition, ok := r.partitions[partitionKey]; !ok {
		delete(r.usedPartitions, partitionKey)
		r.removeFromIndexPartitions(partitionKey)
		return fmt.Errorf("not found the partition in Consume Partition set")
	} else {
		if t, ok := r.usedPartitions[partitionKey]; !ok {
			r.removeFromIndexPartitions(partitionKey)
			r.indexPartitions = append(r.indexPartitions, partitionKey)
		} else {
			if t == bookedTime {
				delete(r.usedPartitions, partitionKey)
				r.removeFromIndexPartitions(partitionKey)
				delay := int64(0)
				if checkDelay {
					delay = partition.ProcConsumeResult(r.defHandler, r.groupHandler, filterConsume, isConsumed)
				}
				if delay > 10 {
					r.partitionTimeouts[partitionKey] = time.AfterFunc(time.Duration(delay)*time.Millisecond, func() {
						r.metaMu.Lock()
						defer r.metaMu.Unlock()
						r.resetIdlePartition(partitionKey, true)
					})
				} else {
					r.indexPartitions = append(r.indexPartitions, partitionKey)
				}
			} else {
				return fmt.Errorf("illegal confirmContext content: context not equal")
			}
		}
	}
	return nil
}

func (r *RmtDataCache) removeFromIndexPartitions(partitionKey string) {
	pos := -1
	for i, p := range r.indexPartitions {
		if p == partitionKey {
			pos = i
			break
		}
	}
	if len(r.indexPartitions) == 1 && pos == 0 {
		r.indexPartitions = nil
		return
	}
	if pos == -1 || pos >= len(r.indexPartitions) {
		return
	}
	r.indexPartitions = append(r.indexPartitions[:pos], r.indexPartitions[pos+1:]...)
}

// BookPartitionInfo books a given partition of offset.
func (r *RmtDataCache) BookPartitionInfo(partitionKey string, currOffset int64, maxOffset int64) {
	r.dataBookMu.Lock()
	defer r.dataBookMu.Unlock()
	if _, ok := r.partitionOffset[partitionKey]; !ok {
		co := &ConsumerOffset{
			CurrOffset:   util.InvalidValue,
			PartitionKey: partitionKey,
			MaxOffset:    util.InvalidValue,
			UpdateTime:   util.InvalidValue,
		}
		r.partitionOffset[partitionKey] = co
	}
	updated := false
	co := r.partitionOffset[partitionKey]
	if currOffset >= 0 {
		co.CurrOffset = currOffset
	}
	if maxOffset >= 0 {
		co.MaxOffset = maxOffset
	}
	if updated {
		co.UpdateTime = time.Now().UnixNano() / int64(time.Millisecond)
	}
}

// BookConsumeData books a given partition.
func (r *RmtDataCache) BookConsumeData(partitionKey string, data *metadata.ConsumeData) {
	r.metaMu.Lock()
	defer r.metaMu.Unlock()
	if partition, ok := r.partitions[partitionKey]; ok {
		partition.BookConsumeData(data)
	}
}

// IsPartitionInUse returns whether the given partition is in use.
func (r *RmtDataCache) IsPartitionInUse(partitionKey string, bookedTime int64) bool {
	r.metaMu.Lock()
	defer r.metaMu.Unlock()

	bt, ok := r.usedPartitions[partitionKey]
	if !ok {
		return false
	}
	if bt != bookedTime {
		return false
	}
	return true
}

// GetPartition returns the partition of the given key.
func (r *RmtDataCache) GetPartition(key string) *metadata.Partition {
	r.metaMu.Lock()
	defer r.metaMu.Unlock()

	if partition, ok := r.partitions[key]; ok {
		return partition
	}
	return nil
}

// GetAllClosedBrokerParts will return the partitions which should be closed.
func (r *RmtDataCache) GetAllClosedBrokerParts() map[*metadata.Node][]*metadata.Partition {
	r.metaMu.Lock()
	defer r.metaMu.Unlock()

	brokerPartitions := make(map[*metadata.Node][]*metadata.Partition)
	for _, partition := range r.partitions {
		partitions, ok := brokerPartitions[partition.GetBroker()]
		if !ok {
			brokerPartitions[partition.GetBroker()] = []*metadata.Partition{partition}
		} else {
			partitions = append(partitions, partition)
		}
	}
	return brokerPartitions
}

// GetCurPartitionOffset returns the partition to offset map.
func (r *RmtDataCache) GetCurPartitionOffset() map[string]*ConsumerOffset {
	r.dataBookMu.Lock()
	defer r.dataBookMu.Unlock()
	return r.partitionOffset
}
