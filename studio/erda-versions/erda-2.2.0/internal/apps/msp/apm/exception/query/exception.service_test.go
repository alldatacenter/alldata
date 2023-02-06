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

package query

import (
	"context"
	"reflect"
	"testing"

	"bou.ke/monkey"
	"github.com/pkg/errors"

	"github.com/erda-project/erda-proto-go/msp/apm/exception/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/exception/query/source"
)

func Test_exceptionService_GetExceptions_cassandra(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.GetExceptionsRequest
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{"case1", args{ctx: nil, req: &pb.GetExceptionsRequest{ScopeID: "error"}}, true},
		{"case2", args{ctx: nil, req: &pb.GetExceptionsRequest{}}, false},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			defer monkey.UnpatchAll()

			var cassandraSource *source.CassandraSource
			monkey.PatchInstanceMethod(reflect.TypeOf(cassandraSource), "GetExceptions", func(_ *source.CassandraSource, ctx context.Context, req *pb.GetExceptionsRequest) ([]*pb.Exception, error) {
				if tt.args.req.ScopeID == "error" {
					return nil, errors.New("error")
				}
				return nil, nil
			})

			s := &exceptionService{source: cassandraSource}
			_, err := s.GetExceptions(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("GetExceptions() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
		})
	}
}

func Test_exceptionService_GetExceptions_elasticsearch(t *testing.T) {
	type args struct {
		ctx context.Context
		req *pb.GetExceptionsRequest
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{"case1", args{ctx: nil, req: &pb.GetExceptionsRequest{ScopeID: "error"}}, true},
		{"case2", args{ctx: nil, req: &pb.GetExceptionsRequest{}}, false},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			defer monkey.UnpatchAll()

			var elasticsearchSource *source.ElasticsearchSource
			monkey.PatchInstanceMethod(reflect.TypeOf(elasticsearchSource), "GetExceptions", func(_ *source.ElasticsearchSource, ctx context.Context, req *pb.GetExceptionsRequest) ([]*pb.Exception, error) {
				if tt.args.req.ScopeID == "error" {
					return nil, errors.New("error")
				}
				return nil, nil
			})

			s := &exceptionService{source: elasticsearchSource}
			_, err := s.GetExceptions(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("GetExceptions() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
		})
	}
}
