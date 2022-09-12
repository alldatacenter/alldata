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

// Package flowctrl defines the rule and handle logic of flow control.
package flowctrl

import (
	"encoding/json"
	"fmt"
	"math"
	"sort"
	"strconv"
	"strings"
	"sync"
	"sync/atomic"
	"time"

	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/log"
	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/util"
)

// RuleHandler represents the flow control handler.
type RuleHandler struct {
	lastUpdate         int64
	flowCtrlInfo       string
	qrypriorityID      int64
	dataSizeLimit      int64
	flowCtrlID         int64
	minZeroCount       int64
	minDataDltLimit    int64
	dataLimitStartTime int64
	dataLimitEndTime   int64
	configMu           sync.Mutex
	flowCtrlRules      map[int32][]*Item
	flowCtrlItem       *Item
}

// NewRuleHandler returns the a default RuleHandler.
func NewRuleHandler() *RuleHandler {
	return &RuleHandler{
		flowCtrlID:         util.InvalidValue,
		minZeroCount:       math.MaxInt64,
		qrypriorityID:      util.InvalidValue,
		minDataDltLimit:    math.MaxInt64,
		dataLimitStartTime: 2500,
		dataLimitEndTime:   util.InvalidValue,
		lastUpdate:         time.Now().UnixNano() / int64(time.Millisecond),
		flowCtrlItem:       NewItem(),
	}
}

// GetQryPriorityID returns the qrypriorityID.
func (h *RuleHandler) GetQryPriorityID() int64 {
	return atomic.LoadInt64(&h.qrypriorityID)
}

// GetFlowCtrID returns the flowCtrlID.
func (h *RuleHandler) GetFlowCtrID() int64 {
	return atomic.LoadInt64(&h.flowCtrlID)
}

// SetQryPriorityID sets the qrypriorityID
func (h *RuleHandler) SetQryPriorityID(qryPriorityID int64) {
	atomic.StoreInt64(&h.qrypriorityID, qryPriorityID)
}

// UpdateDefFlowCtrlInfo updates the flow control information.
func (h *RuleHandler) UpdateDefFlowCtrlInfo(isDefault bool, qrypriorityID int64, flowCtrlID int64, info string) error {
	curFlowCtrlID := atomic.LoadInt64(&h.flowCtrlID)
	if curFlowCtrlID == flowCtrlID {
		return nil
	}
	var flowCtrlItems map[int32][]*Item
	var err error
	if len(info) > 0 {
		flowCtrlItems, err = parseFlowCtrlInfo(info)
		if err != nil {
			return err
		}
	}
	h.configMu.Lock()
	defer h.configMu.Unlock()
	h.clearStatisticData()
	atomic.StoreInt64(&h.flowCtrlID, flowCtrlID)
	atomic.StoreInt64(&h.qrypriorityID, qrypriorityID)
	if len(flowCtrlItems) == 0 {
		h.flowCtrlRules = make(map[int32][]*Item)
		info = ""
	} else {
		h.flowCtrlRules = flowCtrlItems
		h.flowCtrlInfo = info
		h.initStatisticData()
	}
	h.lastUpdate = time.Now().UnixNano() / int64(time.Millisecond)
	if isDefault {
		log.Infof("[Flow Ctrl] Default FlowCtrl's flow ctrl id from %d to %d", curFlowCtrlID, flowCtrlID)
	} else {
		log.Infof("[Flow Ctrl] Group FlowCtrl's flow ctrl id from %d to %d", curFlowCtrlID, flowCtrlID)
	}
	return nil
}

func (h *RuleHandler) initStatisticData() {
	for id, rules := range h.flowCtrlRules {
		if id == 0 {
			for _, rule := range rules {
				if rule.tp != 0 {
					continue
				}

				if rule.datadlt < atomic.LoadInt64(&h.minDataDltLimit) {
					atomic.StoreInt64(&h.minDataDltLimit, rule.datadlt)
				}
				if rule.startTime < atomic.LoadInt64(&h.dataLimitStartTime) {
					atomic.StoreInt64(&h.dataLimitStartTime, rule.startTime)
				}
				if rule.endTime > atomic.LoadInt64(&h.dataLimitEndTime) {
					atomic.StoreInt64(&h.dataLimitEndTime, rule.endTime)
				}
			}
		}
		if id == 1 {
			for _, rule := range rules {
				if rule.tp != 1 {
					continue
				}
				if rule.zeroCnt < atomic.LoadInt64(&h.minZeroCount) {
					atomic.StoreInt64(&h.minZeroCount, rule.zeroCnt)
				}
			}
		}
		if id == 3 {
			for _, rule := range rules {
				if rule.tp != 3 {
					continue
				}
				h.flowCtrlItem.SetTp(RequestFrequencyControl)
				h.flowCtrlItem.SetDataSizeLimit(rule.dataSizeLimit)
				h.flowCtrlItem.SetFreqLimit(rule.freqMsLimit)
				h.flowCtrlItem.SetZeroCnt(rule.zeroCnt)
			}
		}
	}
}

func (h *RuleHandler) clearStatisticData() {
	atomic.StoreInt64(&h.minZeroCount, util.InvalidValue)
	atomic.StoreInt64(&h.minDataDltLimit, math.MaxInt64)
	atomic.StoreInt64(&h.qrypriorityID, util.InvalidValue)
	atomic.StoreInt64(&h.dataSizeLimit, 2500)
	atomic.StoreInt64(&h.dataLimitEndTime, util.InvalidValue)
	h.flowCtrlItem.clear()
}

// GetCurDataLimit returns the flow control result based on lastDataDlt.
func (h *RuleHandler) GetCurDataLimit(lastDataDlt int64) *Result {
	l := time.FixedZone("GMT", 3600*8)
	now := time.Now()
	hour := now.In(l).Hour()
	min := now.In(l).Minute()
	curTime := int64(hour*100 + min)
	if lastDataDlt < atomic.LoadInt64(&h.minDataDltLimit) ||
		curTime < atomic.LoadInt64(&h.dataLimitStartTime) ||
		curTime > atomic.LoadInt64(&h.dataLimitEndTime) {
		return nil
	}
	h.configMu.Lock()
	defer h.configMu.Unlock()
	if _, ok := h.flowCtrlRules[0]; !ok {
		return nil
	}
	for _, rule := range h.flowCtrlRules[0] {
		result := rule.GetDataLimit(lastDataDlt, curTime)
		if result != nil {
			return result
		}
	}
	return nil
}

// GetFilterCtrlItem returns the flow control item.
func (h *RuleHandler) GetFilterCtrlItem() *Item {
	h.configMu.Lock()
	defer h.configMu.Unlock()
	return h.flowCtrlItem
}

// GetCurFreqLimitTime returns curFreqLimitTime.
func (h *RuleHandler) GetCurFreqLimitTime(msgZeroCnt int32, receivedLimit int64) int64 {
	limitData := receivedLimit
	if int64(msgZeroCnt) < atomic.LoadInt64(&h.minZeroCount) {
		return limitData
	}
	h.configMu.Lock()
	defer h.configMu.Unlock()
	if _, ok := h.flowCtrlRules[1]; !ok {
		return limitData
	}
	for _, rule := range h.flowCtrlRules[1] {
		limit := rule.getFreLimit(msgZeroCnt)
		if limit >= 0 {
			limitData = limit
			break
		}
	}
	return limitData
}

// GetMinZeroCnt returns the minZeroCount.
func (h *RuleHandler) GetMinZeroCnt() int64 {
	return atomic.LoadInt64(&h.minZeroCount)
}

func parseFlowCtrlInfo(info string) (map[int32][]*Item, error) {
	if len(info) == 0 {
		return nil, nil
	}
	var docs []map[string]interface{}
	if err := json.Unmarshal([]byte(info), &docs); err != nil {
		return nil, err
	}
	flowCtrlMap := make(map[int32][]*Item)
	for _, doc := range docs {
		if _, ok := doc["type"]; !ok {
			return nil, fmt.Errorf("field not existed")
		}
		tp, err := parseInt(doc, "type", false, util.InvalidValue)
		if err != nil {
			return nil, err
		}

		if tp < 0 || tp > 3 {
			return nil, fmt.Errorf("illegal value, not required value content")
		}

		rule, err := parseRule(doc)
		if err != nil {
			return nil, err
		}
		switch tp {
		case 1:
			flowCtrlItems, err := parseFreqLimit(rule)
			if err != nil {
				return nil, err
			}
			flowCtrlMap[1] = flowCtrlItems
		case 3:
			flowCtrlItems, err := parseLowFetchLimit(rule)
			if err != nil {
				return nil, err
			}
			flowCtrlMap[3] = flowCtrlItems
		case 0:
			flowCtrlItems, err := parseDataLimit(rule)
			if err != nil {
				return nil, err
			}
			flowCtrlMap[0] = flowCtrlItems
		}
	}
	return flowCtrlMap, nil
}

func parseRule(doc map[string]interface{}) ([]interface{}, error) {
	if _, ok := doc["rule"]; !ok {
		return nil, fmt.Errorf("rule field not existed")
	}
	if _, ok := doc["rule"].([]interface{}); !ok {
		return nil, fmt.Errorf("rule should be interface")
	}
	v := doc["rule"].([]interface{})
	return v, nil
}

func parseDataLimit(rules []interface{}) ([]*Item, error) {
	items := make([]*Item, 0, len(rules))
	for i, rule := range rules {
		if _, ok := rule.(map[string]interface{}); !ok {
			return nil, fmt.Errorf("rule should be a map")
		}
		v := rule.(map[string]interface{})
		start, err := parseTime(v, "start")
		if err != nil {
			return nil, err
		}
		end, err := parseTime(v, "end")
		if err != nil {
			return nil, err
		}
		if start > end {
			return nil, fmt.Errorf("start value must lower than the End value in index(%d) of data limit rule", i)
		}
		datadlt, err := parseInt(v, "dltInM", false, -1)
		if err != nil {
			return nil, fmt.Errorf("dltInM key is required in index(%d) of data limit rule", i)
		}
		datadlt = datadlt * 1024 * 204
		dataSizeLimit, err := parseInt(v, "limitInM", false, -1)
		if err != nil {
			return nil, fmt.Errorf("limitInM key is required in index(%d) of data limit rule", i)
		}
		if dataSizeLimit < 0 {
			return nil, fmt.Errorf("limitInM value must over than equal or bigger than zero in index(%d) of data limit rule", i)
		}
		dataSizeLimit = dataSizeLimit * 1024 * 1024
		freqMsLimit, err := parseInt(v, "freqInMs", false, -1)
		if err != nil {
			return nil, fmt.Errorf("freqInMs key is required in index((%d) of data limit rule", i)
		}
		if freqMsLimit < 200 {
			return nil, fmt.Errorf("freqInMs value must over than equal or bigger than 200 in index(%d) of data limit rule", i)
		}
		item := NewItem()
		item.SetTp(CurrentLimit)
		item.SetStartTime(start)
		item.SetEndTime(end)
		item.SetDatadlt(datadlt)
		item.SetDataSizeLimit(dataSizeLimit)
		item.SetFreqLimit(freqMsLimit)
		items = append(items, item)
	}
	if len(items) > 0 {
		sort.Slice(items, func(i, j int) bool {
			return items[i].startTime <= items[j].startTime
		})
	}
	return items, nil
}

func parseLowFetchLimit(rules []interface{}) ([]*Item, error) {
	items := make([]*Item, 0, len(rules))
	for _, rule := range rules {
		var filterFreqMs int64
		var err error
		if _, ok := rule.(map[string]interface{}); !ok {
			return nil, fmt.Errorf("rule should be a map")
		}
		v := rule.(map[string]interface{})
		if _, ok := v["filterFreqInMs"]; ok {
			filterFreqMs, err = parseInt(v, "filterFreqInMs", false, -1)
			if err != nil {
				return nil, fmt.Errorf("decode failure: %s of filterFreqInMs field in parse low fetch limit", err.Error())
			}
		}
		if filterFreqMs < 0 || filterFreqMs > 300000 {
			return nil, fmt.Errorf("decode failure: filterFreqInMs value must in [0, 300000] in index(%d) of low fetch limit rule", filterFreqMs)
		}

		var minFilteFreqMs int64
		if _, ok := v["minDataFilterFreqInMs"]; ok {
			minFilteFreqMs, err = parseInt(v, "minDataFilterFreqInMs", false, -1)
			if err != nil {
				return nil, fmt.Errorf("decode failure: %s of minDataFilterFreqInMs field in parse low fetch limit", err.Error())
			}
		}
		if minFilteFreqMs < 0 || minFilteFreqMs > 300000 {
			return nil, fmt.Errorf("decode failure: minDataFilterFreqInMs value must in [0, 300000] in index(%d) of low fetch limit rule", filterFreqMs)
		}
		if minFilteFreqMs < filterFreqMs {
			return nil, fmt.Errorf("decode failure: minDataFilterFreqInMs must lower than filterFreqInMs in index(%d) of low fetch limit rule", filterFreqMs)
		}
		var normFreqMs int64
		if _, ok := v["normFreqInMs"]; ok {
			normFreqMs, err = parseInt(v, "normFreqInMs", false, -1)
			if err != nil {
				return nil, fmt.Errorf("decode failure: %s of normFreqInMs field in parse low fetch limit", err.Error())
			}
			if normFreqMs < 0 || normFreqMs > 30000 {
				return nil, fmt.Errorf("decode failure: normFreqInMs value must in [0, 300000] in index(%d) of low fetch limit rule", filterFreqMs)
			}
		}
		item := NewItem()
		item.SetTp(RequestFrequencyControl)
		item.SetDataSizeLimit(normFreqMs)
		item.SetFreqLimit(filterFreqMs)
		item.SetZeroCnt(minFilteFreqMs)
		items = append(items, item)
	}
	return items, nil
}

func parseFreqLimit(rules []interface{}) ([]*Item, error) {
	items := make([]*Item, 0, len(rules))
	for _, rule := range rules {
		if _, ok := rule.(map[string]interface{}); !ok {
			return nil, fmt.Errorf("rule should be a map")
		}
		v := rule.(map[string]interface{})
		zeroCnt, err := parseInt(v, "zeroCnt", false, util.InvalidValue)
		if err != nil {
			return nil, err
		}
		freqLimit, err := parseInt(v, "freqInMs", false, util.InvalidValue)
		if err != nil {
			return nil, err
		}
		item := NewItem()
		item.SetTp(FrequencyLimit)
		item.SetZeroCnt(zeroCnt)
		item.SetFreqLimit(freqLimit)
		items = append(items, item)
	}
	if len(items) > 0 {
		sort.Slice(items, func(i, j int) bool {
			return items[i].zeroCnt > items[j].zeroCnt
		})
	}
	return items, nil
}

func parseInt(doc map[string]interface{}, key string, compare bool, required int64) (int64, error) {
	if _, ok := doc[key]; !ok {
		return util.InvalidValue, fmt.Errorf("field not existed")
	}
	if _, ok := doc[key].(float64); !ok {
		return util.InvalidValue, fmt.Errorf("illegal value, must be int type")
	}
	v := doc[key].(float64)
	if compare && int64(v) != required {
		return util.InvalidValue, fmt.Errorf("illegal value, not required value content")
	}
	return int64(v), nil
}

func parseTime(doc map[string]interface{}, key string) (int64, error) {
	if _, ok := doc[key]; !ok {
		return util.InvalidValue, fmt.Errorf("field %s not existed", key)
	}
	if _, ok := doc[key].(string); !ok {
		return util.InvalidValue, fmt.Errorf("field %s must be int type", key)
	}
	s := doc[key].(string)
	pos1 := strings.Index(s, ":")
	if pos1 == -1 {
		return util.InvalidValue, fmt.Errorf("field %s must be 'aa:bb' and 'aa','bb' must be int value format", key)
	}
	h, err := strconv.Atoi(s[:pos1])
	if err != nil {
		return util.InvalidValue, err
	}
	if h < 0 || h > 24 {
		return util.InvalidValue, fmt.Errorf("field %s -hour value must in [0,23]", key)
	}
	m, err := strconv.Atoi(s[pos1+1:])
	if err != nil {
		return util.InvalidValue, err
	}
	if m < 0 || m > 59 {
		return util.InvalidValue, fmt.Errorf("field %s -minute value must in [0,59]", key)
	}
	return int64(h*100 + m), nil
}
