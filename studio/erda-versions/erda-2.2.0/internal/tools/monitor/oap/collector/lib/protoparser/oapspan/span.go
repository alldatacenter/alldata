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

package oapspan

import (
	"fmt"
	"sync"

	oap "github.com/erda-project/erda-proto-go/oap/trace/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/trace"
	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/lib/common"
	"github.com/erda-project/erda/internal/tools/monitor/oap/collector/lib/common/unmarshalwork"
)

func ParseOapSpan(buf []byte, callback func(span *trace.Span) error) error {
	uw := newUnmarshalWork(buf, callback)
	uw.wg.Add(1)
	unmarshalwork.Schedule(uw)
	uw.wg.Wait()
	if uw.err != nil {
		return fmt.Errorf("parse oapSpan err: %w", uw.err)
	}
	return nil
}

type unmarshalWork struct {
	buf      []byte
	err      error
	wg       sync.WaitGroup
	callback func(span *trace.Span) error
}

func newUnmarshalWork(buf []byte, callback func(span *trace.Span) error) *unmarshalWork {
	return &unmarshalWork{buf: buf, callback: callback}
}

// TODO. Better error handle
func (uw *unmarshalWork) Unmarshal() {
	defer uw.wg.Done()
	data := &oap.Span{}
	if err := common.JsonDecoder.Unmarshal(uw.buf, data); err != nil {
		uw.err = fmt.Errorf("json umarshal failed: %w", err)
		return
	}

	span := &trace.Span{
		StartTime:    int64(data.StartTimeUnixNano),
		EndTime:      int64(data.EndTimeUnixNano),
		TraceId:      data.TraceID,
		SpanId:       data.SpanID,
		ParentSpanId: data.ParentSpanID,
		Tags:         data.Attributes,
	}
	span.Tags["operation_name"] = data.Name
	if v, ok := span.Tags[trace.OrgNameKey]; ok {
		span.OrgName = v
	} else {
		uw.err = fmt.Errorf("must have %q", trace.OrgNameKey)
		return
	}

	if err := uw.callback(span); err != nil {
		uw.err = err
	}
}
