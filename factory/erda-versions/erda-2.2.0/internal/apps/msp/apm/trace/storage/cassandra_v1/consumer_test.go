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

package cassandra_v1

import (
	"fmt"
	"log"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/internal/pkg/monitor"
	metrics "github.com/erda-project/erda/internal/tools/monitor/core/metric"
)

// TestMetricToSpan .
func TestMetricToSpan(t *testing.T) {
	metric := metrics.Metric{
		Name:      "span",
		Timestamp: time.Now().UnixNano(),
		Tags: map[string]string{"_lt": "transient", "test_tag": "test", "operation_name": "component",
			"parent_span_id": "test-parent-trance-id", "span_id": "test-span-id", "trace_id": "test-trace-id"},
		Fields: map[string]interface{}{"start_time": time.Now().Add(-1).UnixNano(), "end_time": time.Now().UnixNano(),
			"_lt": "transient", "operation_name": "component", "parent_span_id": "test-parent-trance-id",
			"span_id": "test-span-id", "test_tag": "test", "trace_id": "test-trace-id"},
	}
	span, err := metricToSpan(&metric)
	if err != nil {
		log.Fatalln(err)
	}
	if assert.NotNil(t, span, "not nil") {

		assert.Equal(t, metric.Tags["trace_id"], "test-trace-id")
		assert.Equal(t, metric.Tags["parent_span_id"], "test-parent-trance-id")
		assert.Equal(t, metric.Tags["operation_name"], "component")
		assert.Equal(t, metric.Tags["span_id"], "test-span-id")
	}
	fmt.Println(metric)
}

// TestToInt64 .
func TestToInt64(t *testing.T) {
	_int := 10
	_int8 := int8(10)
	_int16 := int16(10)
	_int32 := int32(10)
	_int64 := int64(10)
	_uint := 10
	_uint8 := uint8(10)
	_uint16 := uint16(10)
	_uint32 := uint32(10)
	_uint64 := uint64(10)
	_float32 := float32(10)
	_float64 := float64(10)
	_string := "10"

	num, err := toInt64(_int)
	if err != nil {
		log.Fatal(err)
	}
	assert.Equal(t, num, int64(10))

	num, err = toInt64(_int8)
	if err != nil {
		log.Fatal(err)
	}
	assert.Equal(t, num, int64(10))

	num, err = toInt64(_int16)
	if err != nil {
		log.Fatal(err)
	}
	assert.Equal(t, num, int64(10))

	num, err = toInt64(_int32)
	if err != nil {
		log.Fatal(err)
	}
	assert.Equal(t, num, int64(10))

	num, err = toInt64(_int64)
	if err != nil {
		log.Fatal(err)
	}
	assert.Equal(t, num, int64(10))

	num, err = toInt64(_uint)
	if err != nil {
		log.Fatal(err)
	}
	assert.Equal(t, num, int64(10))

	num, err = toInt64(_uint8)
	if err != nil {
		log.Fatal(err)
	}
	assert.Equal(t, num, int64(10))

	num, err = toInt64(_uint16)
	if err != nil {
		log.Fatal(err)
	}
	assert.Equal(t, num, int64(10))

	num, err = toInt64(_uint32)
	if err != nil {
		log.Fatal(err)
	}
	assert.Equal(t, num, int64(10))

	num, err = toInt64(_uint64)
	if err != nil {
		log.Fatal(err)
	}
	assert.Equal(t, num, int64(10))

	num, err = toInt64(_float32)
	if err != nil {
		log.Fatal(err)
	}
	assert.Equal(t, num, int64(10))

	num, err = toInt64(_float64)
	if err != nil {
		log.Fatal(err)
	}
	assert.Equal(t, num, int64(10))

	num, err = toInt64(_string)
	if err != nil {
		log.Fatal(err)
	}
	assert.Equal(t, num, int64(10))
}

func Test_getTimeRange(t *testing.T) {
	tests := []struct {
		name          string
		span          *monitor.Span
		wantStartTime int64
		wantEndTime   int64
	}{
		{
			span: &monitor.Span{
				SpanID:    "bc703bc4-9ba4-40d5-a092-533183290cb0",
				StartTime: 1635906581184000000,
				EndTime:   1635906581186000000,
			},
			wantStartTime: 1635906581184064675,
			wantEndTime:   1635906581186064675,
		},
		{
			span: &monitor.Span{
				SpanID:    "165c3f71-730b-4843-8da7-d000b08575b4",
				StartTime: 1635906581185000000,
				EndTime:   1635906581185000000,
			},
			wantStartTime: 1635906581185019809,
			wantEndTime:   1635906581185019809,
		},
		{
			span: &monitor.Span{
				SpanID:    "dc76bc0a-40f3-4dbc-9f26-962fb3bd7556",
				StartTime: 1635906581232000000,
				EndTime:   1635906581237000000,
			},
			wantStartTime: 1635906581232019363,
			wantEndTime:   1635906581237019363,
		},
		{
			span: &monitor.Span{
				SpanID:    "314287dd-bdaf-4ea3-9caa-035655b82355",
				StartTime: 1635906581232000000,
				EndTime:   1635906581237000000,
			},
			wantStartTime: 1635906581232043715,
			wantEndTime:   1635906581237043715,
		},
		{
			span: &monitor.Span{
				SpanID:    "314287dd-bdaf-4ea3-9caa-035655b82355",
				StartTime: 1635906581232000001,
				EndTime:   1635906581237000001,
			},
			wantStartTime: 1635906581232000001,
			wantEndTime:   1635906581237000001,
		},
		{
			span: &monitor.Span{
				SpanID:    "314287dd-bdaf-4ea3-9caa-035655b82355",
				StartTime: 100 * millisecond,
				EndTime:   200 * millisecond,
			},
			wantStartTime: 100043715,
			wantEndTime:   200043715,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			startTime, endTime := getTimeRange(tt.span)
			if startTime != tt.wantStartTime {
				t.Errorf("getTimePair() got startTime = %v, want %v", startTime, tt.wantStartTime)
			}
			if endTime != tt.wantEndTime {
				t.Errorf("getTimePair() got endTime = %v, want %v", endTime, tt.wantEndTime)
			}
		})
	}
}
