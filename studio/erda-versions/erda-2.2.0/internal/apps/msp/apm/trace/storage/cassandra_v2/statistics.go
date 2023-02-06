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

import (
	"context"

	"github.com/scylladb/gocqlx/qb"
)

func (p *provider) Count(ctx context.Context, traceId string) int64 {
	var count int64

	var cmps []qb.Cmp
	values := make(qb.M)
	cmps = append(cmps, qb.Eq("trace_id"))
	values["trace_id"] = traceId

	builder := qb.Select(DefaultSpanTable).Where(cmps...).Count("trace_id")
	err := p.queryFunc(builder, values, &count)
	if err != nil {
		return 0
	}
	return count
}
