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

package cassandra

import (
	"bytes"
	"compress/gzip"
	"context"
	"reflect"
	"testing"

	"bou.ke/monkey"
	"github.com/scylladb/gocqlx/qb"
	"gotest.tools/assert"

	"github.com/erda-project/erda-proto-go/core/monitor/log/query/pb"
	"github.com/erda-project/erda/internal/tools/monitor/core/log/storage"
)

func Test_mergeSavedLog(t *testing.T) {
	tests := []struct {
		name string
		a    []*SavedLog
		b    []*SavedLog
		less func(a, b *SavedLog) bool
		want []*SavedLog
	}{
		{
			a: []*SavedLog{
				{
					Timestamp: 100,
					Offset:    10,
				},
				{
					Timestamp: 102,
					Offset:    12,
				},
			},
			b: []*SavedLog{
				{
					Timestamp: 101,
					Offset:    11,
				},
				{
					Timestamp: 103,
					Offset:    13,
				},
			},
			less: lessSavedLog,
			want: []*SavedLog{
				{
					Timestamp: 100,
					Offset:    10,
				},
				{
					Timestamp: 101,
					Offset:    11,
				},
				{
					Timestamp: 102,
					Offset:    12,
				},
				{
					Timestamp: 103,
					Offset:    13,
				},
			},
		},
		{
			a: []*SavedLog{
				{
					Timestamp: 100,
					Offset:    10,
				},
				{
					Timestamp: 102,
					Offset:    12,
				},
			},
			b: []*SavedLog{
				{
					Timestamp: 100,
					Offset:    11,
				},
				{
					Timestamp: 102,
					Offset:    9,
				},
			},
			less: lessSavedLog,
			want: []*SavedLog{
				{
					Timestamp: 100,
					Offset:    10,
				},
				{
					Timestamp: 100,
					Offset:    11,
				},
				{
					Timestamp: 102,
					Offset:    9,
				},
				{
					Timestamp: 102,
					Offset:    12,
				},
			},
		},
		{
			a: []*SavedLog{
				{
					Timestamp: 100,
					Offset:    10,
				},
				{
					Timestamp: 102,
					Offset:    12,
				},
			},
			b:    nil,
			less: lessSavedLog,
			want: []*SavedLog{
				{
					Timestamp: 100,
					Offset:    10,
				},
				{
					Timestamp: 102,
					Offset:    12,
				},
			},
		},
		{
			a: nil,
			b: []*SavedLog{
				{
					Timestamp: 100,
					Offset:    11,
				},
				{
					Timestamp: 102,
					Offset:    9,
				},
			},
			less: lessSavedLog,
			want: []*SavedLog{
				{
					Timestamp: 100,
					Offset:    11,
				},
				{
					Timestamp: 102,
					Offset:    9,
				},
			},
		},
		{
			a: []*SavedLog{
				{
					Timestamp: 102,
					Offset:    12,
				},
				{
					Timestamp: 100,
					Offset:    10,
				},
			},
			b: []*SavedLog{
				{
					Timestamp: 103,
					Offset:    13,
				},
				{
					Timestamp: 101,
					Offset:    11,
				},
			},
			less: reverseLessSavedLog,
			want: []*SavedLog{
				{
					Timestamp: 103,
					Offset:    13,
				},
				{
					Timestamp: 102,
					Offset:    12,
				},
				{
					Timestamp: 101,
					Offset:    11,
				},
				{
					Timestamp: 100,
					Offset:    10,
				},
			},
		},
		{
			a: []*SavedLog{
				{
					Timestamp: 102,
					Offset:    12,
				},
				{
					Timestamp: 100,
					Offset:    10,
				},
			},
			b: []*SavedLog{
				{
					Timestamp: 102,
					Offset:    9,
				},
				{
					Timestamp: 100,
					Offset:    11,
				},
			},
			less: reverseLessSavedLog,
			want: []*SavedLog{
				{
					Timestamp: 102,
					Offset:    12,
				},
				{
					Timestamp: 102,
					Offset:    9,
				},
				{
					Timestamp: 100,
					Offset:    11,
				},
				{
					Timestamp: 100,
					Offset:    10,
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := mergeSavedLog(tt.a, tt.b, tt.less); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("mergeSavedLog() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_logsIterator_Prev(t *testing.T) {
	tests := []struct {
		name string
		it   *logsIterator
		want []*pb.LogItem
	}{
		{
			it: &logsIterator{
				ctx: context.TODO(),
				sel: &storage.Selector{
					Start: 100,
					End:   200,
				},
				queryFunc: func(builder *qb.SelectBuilder, binding qb.M, dest interface{}) error {
					list := dest.(*[]*SavedLog)
					*list = append(*list,
						&SavedLog{
							ID:        "2",
							Source:    "container",
							Stream:    "stdout",
							Timestamp: 101,
							Offset:    1,
							Level:     "info",
							Content:   gzipContent("test 2"),
						},
						&SavedLog{
							ID:        "1",
							Source:    "container",
							Stream:    "stdout",
							Timestamp: 100,
							Offset:    0,
							Level:     "info",
							Content:   gzipContent("test"),
						},
					)
					return nil
				},
				table:     DefaultBaseLogTable,
				cmps:      nil,
				values:    qb.M{},
				matcher:   func(data *pb.LogItem) bool { return true },
				pageSize:  10,
				allStream: false,
				start:     100,
				end:       200,
				offset:    -1,
			},
			want: []*pb.LogItem{
				{
					Id:        "2",
					Source:    "container",
					Stream:    "stdout",
					Timestamp: "101",
					UnixNano:  101,
					Offset:    1,
					Level:     "info",
					Content:   "test 2",
				},
				{
					Id:        "1",
					Source:    "container",
					Stream:    "stdout",
					Timestamp: "100",
					UnixNano:  100,
					Offset:    0,
					Level:     "info",
					Content:   "test",
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			var got []*pb.LogItem
			for tt.it.Prev() {
				got = append(got, tt.it.Value().(*pb.LogItem))
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("logsIterator.Prev() got %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_Iterator(t *testing.T) {
	monkey.Patch((*provider).queryLogMetaWithFilters, func(p *provider, filters qb.M) (*LogMeta, error) {
		return &LogMeta{
			ID:     "id-1",
			Source: "container",
			Tags:   map[string]string{"dice_org_name": "erda"}}, nil
	})
	defer monkey.Unpatch((*provider).queryLogMetaWithFilters)

	p := &provider{
		Cfg: &config{},
	}

	_, err := p.Iterator(context.Background(), &storage.Selector{
		Filters: []*storage.Filter{
			{Key: "id", Value: "id-1"},
			{Key: "source", Value: "container"},
		},
	})
	assert.NilError(t, err)
}

func gzipContent(content string) []byte {
	buf := &bytes.Buffer{}
	w := gzip.NewWriter(buf)
	w.Write([]byte(content))
	w.Flush()
	w.Close()
	return buf.Bytes()
}
