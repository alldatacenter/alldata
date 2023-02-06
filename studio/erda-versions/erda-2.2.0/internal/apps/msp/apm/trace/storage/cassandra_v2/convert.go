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

package cassandra_v2

import "github.com/erda-project/erda-proto-go/msp/apm/trace/pb"

func convertToPbSpans(list []*SavedSpan) []interface{} {
	spans := make([]interface{}, 0, len(list))
	for _, log := range list {
		data := wrapToPbSpan(log)
		spans = append(spans, data)
	}
	return spans
}

func wrapToPbSpan(ss *SavedSpan) *pb.Span {
	return &pb.Span{
		Id:            ss.SpanId,
		TraceId:       ss.TraceId,
		OperationName: ss.OperationName,
		ParentSpanId:  ss.ParentSpanId,
		StartTime:     ss.StartTime,
		EndTime:       ss.EndTime,
		Tags:          ss.Tags,
	}
}
