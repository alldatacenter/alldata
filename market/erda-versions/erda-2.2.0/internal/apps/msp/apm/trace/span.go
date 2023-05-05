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

package trace

const (
	CH_TABLE_SERIES = "spans_series"
	CH_TABLE_META   = "spans_meta"
	// distributed table
	CH_TABLE_SERIES_ALL = "spans_series_all"
	CH_TABLE_META_ALL   = "spans_meta_all"

	OrgNameKey = "org_name"
)

type Span struct {
	StartTime    int64  `json:"start_time"` // timestamp nano
	EndTime      int64  `json:"end_time"`   // timestamp nano
	OrgName      string `json:"org_name"`
	TraceId      string `json:"trace_id"`
	SpanId       string `json:"span_id"`
	ParentSpanId string `json:"parent_span_id"`
	// Deprecated, move to tags
	OperationName string            `json:"operation_name" `
	Tags          map[string]string `json:"tags"`
}

func (s *Span) Hash() uint64 {
	return 0
}

func (s *Span) GetTags() map[string]string {
	if s.Tags == nil {
		s.Tags = map[string]string{}
	}
	return s.Tags
}

type Series struct {
	StartTime    int64             `ch:"start_time"`
	EndTime      int64             `ch:"end_time"`
	SeriesID     uint64            `ch:"series_id"`
	OrgName      string            `ch:"org_name"`
	TraceId      string            `ch:"trace_id"`
	SpanId       string            `ch:"span_id"`
	ParentSpanId string            `ch:"parent_span_id"`
	Tags         map[string]string `ch:"tags"`
}

type Meta struct {
	SeriesID uint64 `ch:"series_id"`
	CreateAt int64  `ch:"create_at"`
	OrgName  string `ch:"org_name"`
	Key      string `ch:"key"`
	Value    string `ch:"value"`
}
