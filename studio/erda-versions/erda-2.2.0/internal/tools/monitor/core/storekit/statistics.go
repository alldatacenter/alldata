// Copyright (c) 2021 Terminus, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package storekit

import (
	"time"
)

// Milliseconds
var DefaultLatencyBuckets = []float64{1, 50, 100, 250, 500, 750, 1000, 5000, 10000}

// ConsumeStatistics .
type ConsumeStatistics interface {
	ReadError(err error)
	WriteError(data []Data, err error)
	ConfirmError(data []Data, err error)
	Success(data []Data)
	ObserveReadLatency(start time.Time)
	ObserveWriteLatency(start time.Time)
}

type nopConsumeStatistics struct{}

var NopConsumeStatistics ConsumeStatistics = &nopConsumeStatistics{}

func (*nopConsumeStatistics) ReadError(err error)                   {}
func (*nopConsumeStatistics) WriteError(data []Data, err error)     {}
func (*nopConsumeStatistics) ConfirmError(data []Data, err error)   {}
func (*nopConsumeStatistics) Success(data []Data)                   {}
func (s *nopConsumeStatistics) ObserveReadLatency(start time.Time)  {}
func (s *nopConsumeStatistics) ObserveWriteLatency(start time.Time) {}
