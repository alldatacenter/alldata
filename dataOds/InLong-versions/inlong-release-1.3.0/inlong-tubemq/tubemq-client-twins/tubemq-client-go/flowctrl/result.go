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

// Result defines a flow control result.
type Result struct {
	dataSizeLimit int64
	freqMsLimit   int32
}

// NewResult returns a new flow control result.
func NewResult(dataSizeLimit int64, freqMsLimit int32) *Result {
	return &Result{
		dataSizeLimit: dataSizeLimit,
		freqMsLimit:   freqMsLimit,
	}
}

// SetDataSizeLimit sets the dataSizeLimit.
func (r *Result) SetDataSizeLimit(dataSizeLimit int64) {
	r.dataSizeLimit = dataSizeLimit
}

// SetFreqMsLimit sets the freqMsLimit.
func (r *Result) SetFreqMsLimit(freqMsLimit int32) {
	r.freqMsLimit = freqMsLimit
}

// GetDataSizeLimit returns the dataSizeLimit.
func (r *Result) GetDataSizeLimit() int64 {
	return r.dataSizeLimit
}

// GetFreqMsLimit returns the freqMsLimit.
func (r *Result) GetFreqMsLimit() int64 {
	return int64(r.freqMsLimit)
}
