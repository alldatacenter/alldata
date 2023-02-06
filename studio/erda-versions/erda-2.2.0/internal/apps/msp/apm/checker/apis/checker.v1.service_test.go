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

package apis

import (
	"context"
	"reflect"
	"testing"
	"time"

	"bou.ke/monkey"
	"github.com/pkg/errors"
	"google.golang.org/protobuf/types/known/structpb"

	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda-infra/providers/i18n"
	checkerpb "github.com/erda-project/erda-proto-go/msp/apm/checker/pb"
	"github.com/erda-project/erda/internal/apps/msp/apm/checker/storage/db"
)

func Test_checkerV1Service_DescribeCheckersV1(t *testing.T) {
	type args struct {
		ctx context.Context
		req *checkerpb.DescribeCheckersV1Request
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{"case1", args{ctx: transport.WithHeader(context.Background(), transport.Header{"lang": []string{"zh"}}), req: &checkerpb.DescribeCheckersV1Request{ProjectID: -1, Env: "TEST", TenantId: "test"}}, true},
		{"case2", args{ctx: transport.WithHeader(context.Background(), transport.Header{"lang": []string{"zh"}}), req: &checkerpb.DescribeCheckersV1Request{ProjectID: 0, Env: "TEST", TenantId: "error"}}, true},
		{"case2-1", args{ctx: transport.WithHeader(context.Background(), transport.Header{"lang": []string{"zh"}}), req: &checkerpb.DescribeCheckersV1Request{ProjectID: 0, Env: "TEST", TenantId: "test"}}, false},
		{"case3", args{ctx: transport.WithHeader(context.Background(), transport.Header{"lang": []string{"zh"}}), req: &checkerpb.DescribeCheckersV1Request{ProjectID: 1, Env: "TEST", TenantId: "test"}}, false},
		{"case4", args{ctx: transport.WithHeader(context.Background(), transport.Header{"lang": []string{"zh"}}), req: &checkerpb.DescribeCheckersV1Request{ProjectID: -2, Env: "TEST", TenantId: "test"}}, true},
		{"case5", args{ctx: transport.WithHeader(context.Background(), transport.Header{"lang": []string{"zh"}}), req: &checkerpb.DescribeCheckersV1Request{ProjectID: 2, Env: "TEST", TenantId: "test"}}, true},
		{"case6", args{ctx: transport.WithHeader(context.Background(), transport.Header{"lang": []string{"zh"}}), req: &checkerpb.DescribeCheckersV1Request{ProjectID: 3, Env: "TEST", TenantId: "test"}}, false},
		{"case7", args{ctx: transport.WithHeader(context.Background(), transport.Header{"lang": []string{"zh"}}), req: &checkerpb.DescribeCheckersV1Request{ProjectID: 4, Env: "TEST", TenantId: "test"}}, false},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			var prodb *db.ProjectDB
			monkey.PatchInstanceMethod(reflect.TypeOf(prodb), "GetByProjectID", func(prodb *db.ProjectDB, projectID int64) (*db.Project, error) {
				if projectID == -1 {
					return nil, errors.New("no project")
				}
				if projectID == 0 || projectID == 2 {
					return nil, nil
				}
				return &db.Project{ID: projectID}, nil
			})
			var metricdb *db.MetricDB
			monkey.PatchInstanceMethod(reflect.TypeOf(metricdb), "Update", func(metricdb *db.MetricDB, m *db.Metric) error {
				if m.TenantId == "error" {
					return errors.New("error")
				}
				return nil
			})

			monkey.PatchInstanceMethod(reflect.TypeOf(metricdb), "ListByProjectIDAndEnv", func(metricdb *db.MetricDB, projectID int64, env string) ([]*db.Metric, error) {
				var ms []*db.Metric
				if projectID == -1 {
					return nil, errors.New("no project")
				}
				if projectID == -2 {
					return nil, errors.New("no project")
				}
				if projectID == -3 && env == "ERROR" {
					return nil, errors.New("no project")
				}
				for i := 0; i < 2; i++ {
					ms = append(ms, &db.Metric{
						ID:         0,
						ProjectID:  0,
						ServiceID:  0,
						Name:       "",
						URL:        "",
						Mode:       "HTTP",
						Status:     0,
						Env:        "TEST",
						Config:     "{\"body\":{\"content\":\"{}\",\"type\":\"application/json\"}}",
						CreateTime: time.Now(),
						UpdateTime: time.Now(),
						IsDeleted:  "N",
					})
				}
				if projectID == 3 {
					ms = append(ms, &db.Metric{
						ID:         0,
						ProjectID:  0,
						ServiceID:  0,
						Name:       "",
						URL:        "",
						Mode:       "HTTP",
						Status:     0,
						Env:        "TEST",
						Config:     "{xxx}",
						CreateTime: time.Now(),
						UpdateTime: time.Now(),
						IsDeleted:  "N",
					})
				}
				if projectID == 4 {
					ms = append(ms, &db.Metric{
						ID:         0,
						ProjectID:  0,
						ServiceID:  0,
						Name:       "",
						URL:        "",
						Mode:       "HTTP",
						Status:     0,
						Env:        "TEST",
						Config:     "{\"body\":{\"type\":\"application/json\"}}",
						CreateTime: time.Now(),
						UpdateTime: time.Now(),
						IsDeleted:  "N",
					})
				}
				return ms, nil
			})
			var cv1s *checkerV1Service
			monkey.PatchInstanceMethod(reflect.TypeOf(cv1s), "QueryCheckersLatencySummaryByProject", func(cv1s *checkerV1Service, lang i18n.LanguageCodes, projectID int64, metrics map[int64]*checkerpb.DescribeItemV1) error {
				if projectID == -2 || projectID == 2 {
					return errors.New("no project")
				}
				return nil
			})

			s := &checkerV1Service{}
			_, err := s.DescribeCheckersV1(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("DescribeCheckersV1() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
		})
	}
}

func Test_checkerV1Service_DescribeCheckerV1(t *testing.T) {
	type args struct {
		ctx context.Context
		req *checkerpb.DescribeCheckerV1Request
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{"case1", args{ctx: transport.WithHeader(context.Background(), transport.Header{"lang": []string{"zh"}}), req: &checkerpb.DescribeCheckerV1Request{Id: -1, Period: ""}}, true},
		{"case2", args{ctx: transport.WithHeader(context.Background(), transport.Header{"lang": []string{"zh"}}), req: &checkerpb.DescribeCheckerV1Request{Id: 0, Period: ""}}, false},
		{"case3", args{ctx: transport.WithHeader(context.Background(), transport.Header{"lang": []string{"zh"}}), req: &checkerpb.DescribeCheckerV1Request{Id: 1, Period: ""}}, false},
		{"case4", args{ctx: transport.WithHeader(context.Background(), transport.Header{"lang": []string{"zh"}}), req: &checkerpb.DescribeCheckerV1Request{Id: -2, Period: ""}}, true},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			monkey.UnpatchAll()
			var metricdb *db.MetricDB
			monkey.PatchInstanceMethod(reflect.TypeOf(metricdb), "GetByID", func(metricdb *db.MetricDB, id int64) (*db.Metric, error) {
				if id == -1 {
					// err
					return nil, errors.New("no metric")
				}
				if id == 0 {
					// config ""
					return &db.Metric{ID: id, ProjectID: 1, Mode: "http", Name: "test", URL: "http://xxx.com"}, nil
				}
				return &db.Metric{ID: id, ProjectID: 1, Mode: "http", Name: "test", URL: "http://xxx.com",
					Config: "{\"body\":{\"test\": \"test\"},\"frequency\":15,\"headers\":\"\",\"method\":\"GET\",\"retry\":3,\"triggering\":[{\"key\":\"http_code\",\"operate\":\"eq\",\"value\":200},{\"key\":\"body\",\"operate\":\"eq\",\"value\":\"xxx\"}],\"url\":\"http://xxx.com\"}",
				}, nil
			})

			var cv1s *checkerV1Service
			monkey.PatchInstanceMethod(reflect.TypeOf(cv1s), "QueryCheckersLatencySummary", func(cv1s *checkerV1Service, lang i18n.LanguageCodes, metricID int64, timeUnit string, metrics map[int64]*checkerpb.DescribeItemV1) error {
				if metricID == -2 {
					return errors.New("no metric")
				}
				return nil
			})

			s := &checkerV1Service{}
			_, err := s.DescribeCheckerV1(tt.args.ctx, tt.args.req)
			if (err != nil) != tt.wantErr {
				t.Errorf("DescribeCheckerV1() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
		})
	}
}

func Test_oldConfig(t *testing.T) {
	type args struct {
		item       *db.Metric
		config     map[string]*structpb.Value
		wantConfig map[string]*structpb.Value
		wantErr    bool
	}
	tests := []struct {
		name string
		args args
	}{
		{"case1", args{item: &db.Metric{Name: "test", URL: "http://127.0.0.1:8080", Mode: "http"}, config: make(map[string]*structpb.Value), wantConfig: map[string]*structpb.Value{"url": structpb.NewStringValue("http://127.0.0.1:8080"), "method": structpb.NewStringValue("GET")}, wantErr: false}},
		{"case2", args{item: &db.Metric{Name: "test", URL: "http://127.0.0.1:8080", Mode: "http"}, config: make(map[string]*structpb.Value), wantConfig: make(map[string]*structpb.Value), wantErr: true}},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			oldConfig(tt.args.item, tt.args.config)
			if !reflect.DeepEqual(tt.args.config, tt.args.wantConfig) {
				if !tt.args.wantErr {
					t.Errorf("Test_oldConfig = %v, want %v", tt.args.config, tt.args.wantConfig)
				}
			}
		})
	}
}
