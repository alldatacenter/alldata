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

type SavedSpan struct {
	TraceId       string            `db:"trace_id"`
	SpanId        string            `db:"span_id"`
	ParentSpanId  string            `db:"parent_span_id"`
	OperationName string            `db:"operation_name"`
	StartTime     int64             `db:"start_time"`
	EndTime       int64             `db:"end_time"`
	Tags          map[string]string `db:"tags"`
}

// DefaultSpanTable .
var DefaultSpanTable = "spot_prod.spans"
