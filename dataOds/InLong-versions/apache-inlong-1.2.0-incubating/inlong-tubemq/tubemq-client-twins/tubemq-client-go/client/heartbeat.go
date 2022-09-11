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
	"context"
	"strings"
	"sync"
	"sync/atomic"
	"time"

	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/errs"
	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/log"
	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/metadata"
	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/protocol"
	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/util"
)

type heartbeatMetadata struct {
	numConnections int
	timer          *time.Timer
}

type heartbeatManager struct {
	consumer   *consumer
	heartbeats map[string]*heartbeatMetadata
	mu         sync.Mutex
}

func newHBManager(consumer *consumer) *heartbeatManager {
	return &heartbeatManager{
		consumer:   consumer,
		heartbeats: make(map[string]*heartbeatMetadata),
	}
}

func (h *heartbeatManager) registerMaster(address string) {
	h.mu.Lock()
	defer h.mu.Unlock()
	if _, ok := h.heartbeats[address]; !ok {
		h.heartbeats[address] = &heartbeatMetadata{
			numConnections: 1,
			timer:          time.AfterFunc(h.consumer.config.Heartbeat.Interval/2, h.consumerHB2Master),
		}
	}
	hm := h.heartbeats[address]
	hm.numConnections++
}

func (h *heartbeatManager) registerBroker(broker *metadata.Node) {
	h.mu.Lock()
	defer h.mu.Unlock()

	if _, ok := h.heartbeats[broker.GetAddress()]; !ok {
		h.heartbeats[broker.GetAddress()] = &heartbeatMetadata{
			numConnections: 1,
			timer:          time.AfterFunc(h.consumer.config.Heartbeat.Interval, func() { h.consumerHB2Broker(broker) }),
		}
	}
	hm := h.heartbeats[broker.GetAddress()]
	hm.numConnections++
}

func (h *heartbeatManager) consumerHB2Master() {
	if time.Now().UnixNano()/int64(time.Millisecond)-h.consumer.lastMasterHb > 30000 {
		h.consumer.rmtDataCache.HandleExpiredPartitions(h.consumer.config.Consumer.MaxConfirmWait)
	}
	m := &metadata.Metadata{}
	node := &metadata.Node{}
	node.SetHost(util.GetLocalHost())
	node.SetAddress(h.consumer.master.Address)
	m.SetNode(node)
	sub := &metadata.SubscribeInfo{}
	sub.SetGroup(h.consumer.config.Consumer.Group)
	m.SetSubscribeInfo(sub)
	auth := &protocol.AuthenticateInfo{}
	if h.consumer.needGenMasterCertificateInfo(true) {
		util.GenMasterAuthenticateToken(auth, h.consumer.config.Net.Auth.UserName, h.consumer.config.Net.Auth.Password)
	}
	h.consumer.unreportedTimes++
	if h.consumer.unreportedTimes > h.consumer.config.Consumer.MaxSubInfoReportInterval {
		m.SetReportTimes(true)
		h.consumer.unreportedTimes = 0
	}

	rsp, err := h.sendHeartbeatC2M(m)
	if err != nil {
		log.Errorf("consumer hb err %s", err.Error())
		h.consumer.masterHBRetry++
	} else {
		if !rsp.GetSuccess() {
			h.consumer.masterHBRetry++
			if rsp.GetErrCode() == errs.RetErrHBNoNode || strings.Index(rsp.GetErrMsg(), "StandbyException") != -1 {
				log.Warnf("[CONSUMER] hb2master found no-node or standby, re-register, client=%s", h.consumer.clientID)
				address := h.consumer.master.Address
				go func() {
					err := h.consumer.register2Master(rsp.GetErrCode() != errs.RetErrHBNoNode)
					if err != nil {
						return
					}
					h.resetMasterHeartbeat()
				}()
				if rsp.GetErrCode() != errs.RetErrHBNoNode {
					h.mu.Lock()
					defer h.mu.Unlock()
					hm := h.heartbeats[address]
					hm.numConnections--
					if hm.numConnections == 0 {
						delete(h.heartbeats, address)
					}
					return
				}
				log.Warnf("[CONSUMER] heartBeat2Master failure to (%s) : %s, client=%s", h.consumer.master.Address, rsp.GetErrMsg(), h.consumer.clientID)
				return
			}
		}
	}
	h.consumer.masterHBRetry = 0
	h.processHBResponseM2C(rsp)
	h.resetMasterHeartbeat()
}

func (h *heartbeatManager) resetMasterHeartbeat() {
	h.mu.Lock()
	defer h.mu.Unlock()
	hm := h.heartbeats[h.consumer.master.Address]
	hm.timer.Reset(h.nextHeartbeatInterval())
}

func (h *heartbeatManager) sendHeartbeatC2M(m *metadata.Metadata) (*protocol.HeartResponseM2C, error) {
	ctx, cancel := context.WithTimeout(context.Background(), h.consumer.config.Net.ReadTimeout)
	defer cancel()
	rsp, err := h.consumer.client.HeartRequestC2M(ctx, m, h.consumer.subInfo, h.consumer.rmtDataCache)
	return rsp, err
}

func (h *heartbeatManager) processHBResponseM2C(rsp *protocol.HeartResponseM2C) {
	h.consumer.masterHBRetry = 0
	if !rsp.GetNotAllocated() {
		h.consumer.subInfo.CASIsNotAllocated(1, 0)
	}
	if rsp.GetDefFlowCheckId() != 0 || rsp.GetGroupFlowCheckId() != 0 {
		if rsp.GetDefFlowCheckId() != 0 {
			h.consumer.rmtDataCache.UpdateDefFlowCtrlInfo(rsp.GetDefFlowCheckId(), rsp.GetDefFlowControlInfo())
		}
		qryPriorityID := h.consumer.rmtDataCache.GetQryPriorityID()
		if rsp.GetQryPriorityId() != 0 {
			qryPriorityID = rsp.GetQryPriorityId()
		}
		h.consumer.rmtDataCache.UpdateGroupFlowCtrlInfo(qryPriorityID, rsp.GetGroupFlowCheckId(), rsp.GetGroupFlowControlInfo())
	}
	if rsp.GetAuthorizedInfo() != nil {
		h.consumer.processAuthorizedToken(rsp.GetAuthorizedInfo())
	}
	if rsp.GetRequireAuth() {
		atomic.StoreInt32(&h.consumer.nextAuth2Master, 1)
	}
	if rsp.GetEvent() != nil {
		event := rsp.GetEvent()
		subscribeInfo := make([]*metadata.SubscribeInfo, 0, len(event.GetSubscribeInfo()))
		for _, sub := range event.GetSubscribeInfo() {
			s, err := metadata.NewSubscribeInfo(sub)
			if err != nil {
				continue
			}
			subscribeInfo = append(subscribeInfo, s)
		}
		e := metadata.NewEvent(event.GetRebalanceId(), event.GetOpType(), subscribeInfo)
		h.consumer.rmtDataCache.OfferEventAndNotify(e)
	}
}

func (h *heartbeatManager) nextHeartbeatInterval() time.Duration {
	interval := h.consumer.config.Heartbeat.Interval
	if h.consumer.masterHBRetry >= h.consumer.config.Heartbeat.MaxRetryTimes {
		interval = h.consumer.config.Heartbeat.AfterFail
	}
	return interval
}

func (h *heartbeatManager) consumerHB2Broker(broker *metadata.Node) {
	h.mu.Lock()
	defer h.mu.Unlock()

	partitions := h.consumer.rmtDataCache.GetPartitionByBroker(broker)
	if len(partitions) == 0 {
		h.resetBrokerTimer(broker)
		return
	}

	rsp, err := h.sendHeartbeatC2B(broker)
	if err != nil {
		log.Warnf("[Heartbeat2Broker] request network to failure %s", err.Error())
		h.resetBrokerTimer(broker)
		return
	}
	partitionKeys := make([]string, 0, len(partitions))
	if rsp.GetErrCode() == errs.RetCertificateFailure {
		for _, partition := range partitions {
			partitionKeys = append(partitionKeys, partition.GetPartitionKey())
		}
		log.Warnf("[Heartbeat2Broker] request (%s) CertificateFailure", broker.GetAddress())
		h.consumer.rmtDataCache.RemovePartition(partitionKeys)
	}
	if rsp.GetSuccess() && rsp.GetHasPartFailure() {
		for _, fi := range rsp.GetFailureInfo() {
			pos := strings.Index(fi, ":")
			if pos == -1 {
				continue
			}
			partition, err := metadata.NewPartition(fi[pos+1:])
			if err != nil {
				continue
			}
			log.Tracef("[Heartbeat2Broker] found partition(%s) hb failure!", partition.GetPartitionKey())
			partitionKeys = append(partitionKeys, partition.GetPartitionKey())
		}
		h.consumer.rmtDataCache.RemovePartition(partitionKeys)
	}
	log.Tracef("[Heartbeat2Broker] out hb response process, add broker(%s) timer!", broker.GetAddress())
	h.resetBrokerTimer(broker)
}

func (h *heartbeatManager) sendHeartbeatC2B(broker *metadata.Node) (*protocol.HeartBeatResponseB2C, error) {
	m := &metadata.Metadata{}
	m.SetReadStatus(h.consumer.getConsumeReadStatus(false))
	m.SetNode(broker)
	sub := &metadata.SubscribeInfo{}
	sub.SetGroup(h.consumer.config.Consumer.Group)
	m.SetSubscribeInfo(sub)
	auth := h.consumer.genBrokerAuthenticInfo(true)
	h.consumer.subInfo.SetAuthorizedInfo(auth)
	ctx, cancel := context.WithTimeout(context.Background(), h.consumer.config.Net.ReadTimeout)
	defer cancel()
	rsp, err := h.consumer.client.HeartbeatRequestC2B(ctx, m, h.consumer.subInfo, h.consumer.rmtDataCache)
	return rsp, err
}

func (h *heartbeatManager) resetBrokerTimer(broker *metadata.Node) {
	interval := h.consumer.config.Heartbeat.Interval
	partitions := h.consumer.rmtDataCache.GetPartitionByBroker(broker)
	if len(partitions) == 0 {
		delete(h.heartbeats, broker.GetAddress())
	} else {
		hm := h.heartbeats[broker.GetAddress()]
		hm.timer.Reset(interval)
	}
}

func (h *heartbeatManager) close() {
	h.mu.Lock()
	defer h.mu.Unlock()

	for _, heartbeat := range h.heartbeats {
		if !heartbeat.timer.Stop() {
			<-heartbeat.timer.C
		}
		heartbeat.timer = nil
	}
	h.heartbeats = nil
}
