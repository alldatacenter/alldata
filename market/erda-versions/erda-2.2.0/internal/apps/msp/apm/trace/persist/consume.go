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

package persist

import (
	"encoding/json"
	"errors"
	"fmt"
	"strconv"
	"time"

	oap "github.com/erda-project/erda-proto-go/oap/trace/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/trace"
	metrics "github.com/erda-project/erda/internal/tools/monitor/core/metric"
)

func (p *provider) decodeSpotSpan(key, value []byte, topic *string, timestamp time.Time) (interface{}, error) {
	data := &metrics.Metric{}
	if err := json.Unmarshal(value, data); err != nil {
		p.stats.DecodeError(value, err)
		if p.Cfg.PrintInvalidSpan {
			p.Log.Warnf("unknown format spot span data: %s", string(value))
		} else {
			p.Log.Warnf("failed to decode spot span: %v", err)
		}
		return nil, err
	}

	span, err := metricToSpan(data)
	if err != nil {
		if p.Cfg.PrintInvalidSpan {
			p.Log.Warnf("failed to decode spot span: %s, err: %v", string(value), err)
		} else {
			p.Log.Warnf("failed to decode spot span: %v", err)
		}
		return nil, err
	}

	if err := p.validator.Validate(span); err != nil {
		p.stats.ValidateError(span)
		if p.Cfg.PrintInvalidSpan {
			p.Log.Warnf("invalid spot span data: %s", string(value))
		} else {
			p.Log.Warnf("invalid spot span: %v", err)
		}
		return nil, err
	}
	if err := p.metadata.Process(span); err != nil {
		p.stats.MetadataError(span, err)
		p.Log.Errorf("failed to process spot span metadata: %v", err)
	}
	return span, nil
}

func (p *provider) decodeOapSpan(key, value []byte, topic *string, timestamp time.Time) (interface{}, error) {
	data := &oap.Span{}
	if err := json.Unmarshal(value, data); err != nil {
		p.stats.DecodeError(value, err)
		if p.Cfg.PrintInvalidSpan {
			p.Log.Warnf("unknown format oap span data: %s", string(value))
		} else {
			p.Log.Warnf("failed to decode oap span: %v", err)
		}
		return nil, err
	}

	span := &trace.Span{
		OperationName: data.Name,
		StartTime:     int64(data.StartTimeUnixNano),
		EndTime:       int64(data.EndTimeUnixNano),
		TraceId:       data.TraceID,
		SpanId:        data.SpanID,
		ParentSpanId:  data.ParentSpanID,
		Tags:          data.Attributes,
	}

	if err := p.validator.Validate(span); err != nil {
		p.stats.ValidateError(span)
		if p.Cfg.PrintInvalidSpan {
			p.Log.Warnf("invalid oap span data: %s", string(value))
		} else {
			p.Log.Warnf("invalid oap span: %v", err)
		}
		return nil, err
	}
	if err := p.metadata.Process(span); err != nil {
		p.stats.MetadataError(span, err)
		p.Log.Errorf("failed to process oap span metadata: %v", err)
	}
	return span, nil
}

func (p *provider) handleReadError(err error) error {
	p.Log.Errorf("failed to read spans from kafka: %s", err)
	return nil // return nil to continue read
}

func (p *provider) handleWriteError(list []interface{}, err error) error {
	p.Log.Errorf("failed to write into storage: %s", err)
	return nil // return nil to continue consume
}

func (p *provider) confirmErrorHandler(err error) error {
	p.Log.Errorf("failed to confirm span from kafka: %s", err)
	return err // return error to exit
}

// metricToSpan .
func metricToSpan(metric *metrics.Metric) (*trace.Span, error) {
	var span trace.Span
	span.Tags = metric.Tags

	traceID, ok := metric.Tags["trace_id"]
	if !ok {
		return nil, errors.New("trace_id cannot be null")
	}
	span.TraceId = traceID

	spanID, ok := metric.Tags["span_id"]
	if !ok {
		return nil, errors.New("span_id cannot be null")
	}
	span.SpanId = spanID

	parentSpanID, _ := metric.Tags["parent_span_id"]
	span.ParentSpanId = parentSpanID

	opName, ok := metric.Tags["operation_name"]
	if !ok {
		return nil, errors.New("operation_name cannot be null")
	}
	span.OperationName = opName

	value, ok := metric.Fields["start_time"]
	if !ok {
		return nil, errors.New("start_time cannot be null")
	}
	startTime, err := toInt64(value)
	if err != nil {
		return nil, fmt.Errorf("invalid start_time: %s", value)
	}
	span.StartTime = startTime

	value, ok = metric.Fields["end_time"]
	if !ok {
		return nil, errors.New("end_time cannot be null")
	}
	endTime, err := toInt64(value)
	if err != nil {
		return nil, fmt.Errorf("invalid end_time: %s", value)
	}
	span.EndTime = endTime
	return &span, nil
}

// toInt64 .
func toInt64(obj interface{}) (int64, error) {
	switch val := obj.(type) {
	case int:
		return int64(val), nil
	case int8:
		return int64(val), nil
	case int16:
		return int64(val), nil
	case int32:
		return int64(val), nil
	case int64:
		return val, nil
	case uint:
		return int64(val), nil
	case uint8:
		return int64(val), nil
	case uint16:
		return int64(val), nil
	case uint32:
		return int64(val), nil
	case uint64:
		return int64(val), nil
	case float32:
		return int64(val), nil
	case float64:
		return int64(val), nil
	case string:
		v, err := strconv.ParseInt(val, 10, 64)
		if err != nil {
			return 0, err
		}
		return v, nil
	}
	return 0, fmt.Errorf("invalid type")
}
