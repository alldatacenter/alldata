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

package flowctrl

import (
	"github.com/apache/incubator-inlong/inlong-tubemq/tubemq-client-twins/tubemq-client-go/util"
)

const (
	CurrentLimit = iota
	FrequencyLimit
	SSDTransfer
	RequestFrequencyControl
)

// Item defines a flow control item.
type Item struct {
	tp            int32
	startTime     int64
	endTime       int64
	datadlt       int64
	dataSizeLimit int64
	freqMsLimit   int64
	zeroCnt       int64
}

// NewItem returns a default flow control item.
func NewItem() *Item {
	return &Item{
		startTime:     2500,
		endTime:       util.InvalidValue,
		datadlt:       util.InvalidValue,
		dataSizeLimit: util.InvalidValue,
		freqMsLimit:   util.InvalidValue,
		zeroCnt:       util.InvalidValue,
	}
}

// SetTp sets the type.
func (i *Item) SetTp(tp int32) {
	i.tp = tp
}

// SetStartTime sets the startTime.
func (i *Item) SetStartTime(startTime int64) {
	i.startTime = startTime
}

// SetEndTime sets the endTime.
func (i *Item) SetEndTime(endTime int64) {
	i.endTime = endTime
}

// SetDatadlt sets the datadlt.
func (i *Item) SetDatadlt(datadlt int64) {
	i.datadlt = datadlt
}

// SetDataSizeLimit sets the dataSizeLimit.
func (i *Item) SetDataSizeLimit(dataSizeLimit int64) {
	i.dataSizeLimit = dataSizeLimit
}

// SetFreqLimit sets the freqLimit.
func (i *Item) SetFreqLimit(freqLimit int64) {
	i.freqMsLimit = freqLimit
}

// SetZeroCnt sets the zeroCnt.
func (i *Item) SetZeroCnt(zeroCnt int64) {
	i.zeroCnt = zeroCnt
}

func (i *Item) clear() {
	i.tp = 0
	i.startTime = 2500
	i.endTime = util.InvalidValue
	i.datadlt = util.InvalidValue
	i.dataSizeLimit = util.InvalidValue
	i.freqMsLimit = util.InvalidValue
	i.zeroCnt = util.InvalidValue
}

// GetDataLimit returns the flow control result.
func (i *Item) GetDataLimit(dlt int64, time int64) *Result {
	if i.tp != 0 || dlt <= i.datadlt {
		return nil
	}
	if time < i.startTime || time > i.endTime {
		return nil
	}
	return NewResult(i.dataSizeLimit, int32(i.freqMsLimit))
}

// GetFreqMsLimit returns the freqMsLimit.
func (i *Item) GetFreqMsLimit() int64 {
	return i.freqMsLimit
}

func (i *Item) getFreLimit(cnt int32) int64 {
	if i.tp != 1 {
		return -1
	}
	if int64(cnt) >= i.zeroCnt {
		return i.freqMsLimit
	}
	return -1
}

// GetDataSizeLimit returns the dataSizeLimit.
func (i *Item) GetDataSizeLimit() int64 {
	return i.dataSizeLimit
}

// GetZeroCnt returns the zeroCnt.
func (i *Item) GetZeroCnt() int64 {
	return i.zeroCnt
}
