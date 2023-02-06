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

package source

import (
	"context"
	"reflect"
	"testing"

	"bou.ke/monkey"
	"github.com/gocql/gocql"

	"github.com/erda-project/erda-infra/providers/cassandra"
	"github.com/erda-project/erda-proto-go/msp/apm/trace/pb"
)

func TestCassandraSource_GetSpans(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.GetSpansRequest
	}
	tests := []struct {
		name string
		args args
		want []*pb.Span
	}{
		{"case1", args{req: &pb.GetSpansRequest{}}, nil},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			monkey.UnpatchAll()

			gq := &gocql.Query{}
			monkey.PatchInstanceMethod(reflect.TypeOf(gq), "Iter", func(gq *gocql.Query) *gocql.Iter {
				return &gocql.Iter{}
			})
			cs := &cassandra.Session{}
			monkey.PatchInstanceMethod(reflect.TypeOf(cs), "Session", func(s *cassandra.Session) *gocql.Session {
				return &gocql.Session{}
			})

			cSource := &CassandraSource{
				CassandraSession: &cassandra.Session{},
			}

			if got := cSource.GetSpans(tt.args.ctx, tt.args.req); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("GetSpans() = %v, want %v", got, tt.want)
			}
		})
	}
}
