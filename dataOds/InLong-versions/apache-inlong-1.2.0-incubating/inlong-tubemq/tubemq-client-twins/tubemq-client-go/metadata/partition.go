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
	"math"
	"strconv"
	"strings"
	"time"

	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/errs"
	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/flowctrl"
	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/util"
)

// Partition represents the metadata of a partition.
type Partition struct {
	topic        string
	broker       *Node
	partitionID  int32
	partitionKey string
	offset       int64
	lastConsumed bool
	consumeData  *ConsumeData
	flowCtrl     *flowctrl.Result
	freqCtrl     *flowctrl.Item
	strategyData *strategyData
	totalZeroCnt int32
}

// ConsumeData represents the consumption metadata of a partition.
type ConsumeData struct {
	time        int64
	errCode     int32
	escLimit    bool
	msgSize     int32
	dltLimit    int64
	curDataDlt  int64
	requireSlow bool
}

type strategyData struct {
	nextStageUpdate   int64
	nextSliceUpdate   int64
	limitSliceMsgSize int64
	curStageMsgSize   int64
	curSliceMsgSize   int64
}

// NewConsumeData returns a consume data.
func NewConsumeData(time int64, errCode int32, escLimit bool, msgSize int32, dltLimit int64,
	curDataDlt int64, requireSlow bool) *ConsumeData {
	return &ConsumeData{
		time:        time,
		errCode:     errCode,
		escLimit:    escLimit,
		msgSize:     msgSize,
		dltLimit:    dltLimit,
		curDataDlt:  curDataDlt,
		requireSlow: requireSlow,
	}
}

// NewPartition parses a partition from the given string.
// The format of partition string: brokerInfo#topic:partitionId
func NewPartition(partition string) (*Partition, error) {
	s := strings.Split(partition, "#")
	if len(s) == 1 {
		return nil, errs.ErrInvalidPartitionString
	}
	b, err := NewNode(true, s[0])
	if err != nil {
		return nil, err
	}
	topicPartitionID := strings.Split(s[1], ":")
	if len(topicPartitionID) == 1 {
		return nil, errs.ErrInvalidPartitionString
	}
	topic := topicPartitionID[0]
	partitionID, err := strconv.Atoi(topicPartitionID[1])
	if err != nil {
		return nil, err
	}
	partitionKey := strconv.Itoa(int(b.id)) + ":" + topic + ":" + strconv.Itoa(partitionID)
	item := flowctrl.NewItem()
	item.SetTp(flowctrl.RequestFrequencyControl)
	return &Partition{
		topic:        topic,
		broker:       b,
		partitionID:  int32(partitionID),
		strategyData: &strategyData{},
		partitionKey: partitionKey,
		consumeData: &ConsumeData{
			curDataDlt: util.InvalidValue,
		},
		freqCtrl: item,
	}, nil
}

// GetLastConsumed returns lastConsumed of a partition.
func (p *Partition) GetLastConsumed() bool {
	return p.lastConsumed
}

// GetPartitionID returns the partition id of a partition.
func (p *Partition) GetPartitionID() int32 {
	return p.partitionID
}

// GetPartitionKey returns the partition key of a partition.
func (p *Partition) GetPartitionKey() string {
	return p.partitionKey
}

// GetTopic returns the topic of the partition subscribed to.
func (p *Partition) GetTopic() string {
	return p.topic
}

// GetBroker returns the broker.
func (p *Partition) GetBroker() *Node {
	return p.broker
}

// String returns the metadata of a Partition as a string.
func (p *Partition) String() string {
	return p.broker.String() + "#" + p.topic + ":" + strconv.Itoa(int(p.partitionID))
}

// SetLastConsumed sets the last consumed.
func (p *Partition) SetLastConsumed(lastConsumed bool) {
	p.lastConsumed = lastConsumed
}

// BookConsumeData sets the consumeData.
func (p *Partition) BookConsumeData(data *ConsumeData) {
	p.consumeData = data
}

// ProcConsumeResult processes consume result.
func (p *Partition) ProcConsumeResult(defHandler *flowctrl.RuleHandler, groupHandler *flowctrl.RuleHandler,
	filterConsume bool, lastConsumed bool) int64 {
	dltTime := time.Now().UnixNano()/int64(time.Millisecond) - p.consumeData.time
	p.updateStrategyData(defHandler, groupHandler)
	p.lastConsumed = lastConsumed
	switch p.consumeData.errCode {
	case errs.RetSuccess, errs.RetErrNotFound:
		if p.consumeData.msgSize == 0 && p.consumeData.errCode != errs.RetSuccess {
			p.totalZeroCnt++
		} else {
			p.totalZeroCnt = 0
		}
		if p.totalZeroCnt > 0 {
			if groupHandler.GetMinZeroCnt() != math.MaxInt64 {
				return groupHandler.GetCurFreqLimitTime(p.totalZeroCnt, p.consumeData.dltLimit) - dltTime
			} else {
				return defHandler.GetCurFreqLimitTime(p.totalZeroCnt, p.consumeData.dltLimit) - dltTime
			}
		}
		if p.consumeData.escLimit {
			return 0
		} else {
			if p.strategyData.curStageMsgSize >= p.flowCtrl.GetDataSizeLimit() ||
				p.strategyData.curSliceMsgSize >= p.strategyData.limitSliceMsgSize {
				if p.flowCtrl.GetFreqMsLimit() > p.consumeData.dltLimit {
					return p.flowCtrl.GetFreqMsLimit() - dltTime
				}
				return p.consumeData.dltLimit - dltTime
			}
			if p.consumeData.errCode == errs.RetSuccess {
				if filterConsume && p.freqCtrl.GetFreqMsLimit() >= 0 {
					if p.consumeData.requireSlow {
						return p.freqCtrl.GetZeroCnt() - dltTime
					} else {
						return p.freqCtrl.GetFreqMsLimit() - dltTime
					}
				} else if !filterConsume && p.freqCtrl.GetDataSizeLimit() >= 0 {
					return p.freqCtrl.GetDataSizeLimit() - dltTime
				}
			}
			return p.consumeData.dltLimit - dltTime
		}
	default:
		return p.consumeData.dltLimit - dltTime
	}
}

func (p *Partition) updateStrategyData(defHandler *flowctrl.RuleHandler, groupHandler *flowctrl.RuleHandler) {
	p.strategyData.curStageMsgSize += int64(p.consumeData.msgSize)
	p.strategyData.curSliceMsgSize += int64(p.consumeData.msgSize)
	curTime := time.Now().UnixNano() / int64(time.Millisecond)
	if curTime > p.strategyData.nextStageUpdate {
		p.strategyData.curStageMsgSize = 0
		p.strategyData.curSliceMsgSize = 0
		if p.consumeData.curDataDlt < 0 {
			p.flowCtrl = flowctrl.NewResult(math.MaxInt64, 20)
		} else {
			p.flowCtrl = groupHandler.GetCurDataLimit(p.consumeData.curDataDlt)
			if p.flowCtrl == nil {
				p.flowCtrl = defHandler.GetCurDataLimit(p.consumeData.curDataDlt)
			}
			if p.flowCtrl == nil {
				p.flowCtrl = flowctrl.NewResult(math.MaxInt64, 0)
			}
			p.freqCtrl = groupHandler.GetFilterCtrlItem()
			if p.freqCtrl.GetFreqMsLimit() < 0 {
				p.freqCtrl = defHandler.GetFilterCtrlItem()
			}
		}
		p.strategyData.limitSliceMsgSize = p.flowCtrl.GetDataSizeLimit() / 12
		p.strategyData.nextStageUpdate = curTime + 60000
		p.strategyData.nextSliceUpdate = curTime + 5000
	} else if curTime > p.strategyData.nextSliceUpdate {
		p.strategyData.curSliceMsgSize = 0
		p.strategyData.nextSliceUpdate = curTime + 5000
	}
}
