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

package cleaner

import (
	"context"
	"errors"
	"reflect"
	"testing"
	"time"

	"bou.ke/monkey"
	"github.com/golang/mock/gomock"
	"github.com/olivere/elastic"

	"github.com/erda-project/erda-infra/base/logs"
	election "github.com/erda-project/erda-infra/providers/etcd-election"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit/elasticsearch/index/loader"
	"github.com/erda-project/erda/pkg/mock"
)

func Test_provider_getIndicesList(t *testing.T) {
	type args struct {
		ctx     context.Context
		indices *loader.IndexGroup
	}
	tests := []struct {
		name     string
		args     args
		wantList []string
	}{
		{"case1", args{
			ctx: context.Background(),
			indices: &loader.IndexGroup{
				Groups: map[string]*loader.IndexGroup{"test-group": {
					List: []*loader.IndexEntry{{Index: "test-index1"}, {Index: "test-index"}},
				}},
			},
		}, []string{"test-index1", "test-index"}},
		{"case2", args{
			ctx: context.Background(),
			indices: &loader.IndexGroup{
				Groups: map[string]*loader.IndexGroup{"test-group": {
					List: []*loader.IndexEntry{{Index: "test-index1"}, {Index: "test-index"}},
				}},
				List: []*loader.IndexEntry{{Index: "test-index3"}},
			},
		}, []string{"test-index3", "test-index1", "test-index"}},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p := &provider{}
			if gotList := p.getIndicesList(tt.args.ctx, tt.args.indices); !reflect.DeepEqual(gotList, tt.wantList) {
				t.Errorf("getIndicesList() = %v, want %v", gotList, tt.wantList)
			}
		})
	}
}

func Test_provider_forceMerge(t *testing.T) {
	type fields struct {
		Cfg                      *config
		Log                      logs.Logger
		election                 election.Interface
		loader                   loader.Interface
		retentions               RetentionStrategy
		clearCh                  chan *clearRequest
		minIndicesStoreInDisk    int64
		rolloverBodyForDiskClean string
		rolloverAliasPatterns    []*indexAliasPattern
		ttlTaskCh                chan *TtlTask
	}
	type args struct {
		ctx     context.Context
		indices []string
	}
	tests := []struct {
		name      string
		fields    fields
		args      args
		wantError bool
	}{
		{"case1", fields{}, args{
			ctx:     context.Background(),
			indices: []string{"test"},
		}, false},
		{"case2", fields{}, args{
			ctx:     context.Background(),
			indices: []string{"error"},
		}, true},
		{"case3", fields{}, args{
			ctx:     context.Background(),
			indices: []string{"error-shards"},
		}, true},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			monkey.UnpatchAll()
			ctrl := gomock.NewController(t)
			indices := NewMockInterface(ctrl)
			defer ctrl.Finish()
			indices.EXPECT().Client()
			var logger *mock.MockLogger
			defer ctrl.Finish()
			logger = mock.NewMockLogger(ctrl)

			p := &provider{loader: indices, Log: logger}

			esClient := &elastic.Client{}
			monkey.PatchInstanceMethod(reflect.TypeOf(esClient), "Forcemerge", func(client *elastic.Client, indices ...string) *elastic.IndicesForcemergeService {
				return &elastic.IndicesForcemergeService{}
			})

			s := &elastic.IndicesForcemergeService{}
			if tt.args.indices[0] == "test" {
				monkey.PatchInstanceMethod(reflect.TypeOf(s), "Do", func(client *elastic.IndicesForcemergeService, ctx context.Context) (*elastic.IndicesForcemergeResponse, error) {
					return &elastic.IndicesForcemergeResponse{
						Shards: &elastic.ShardsInfo{
							Failures: []*elastic.ShardFailure{},
						},
					}, nil
				})
				logger.EXPECT().Infof(gomock.Any(), gomock.Any())
			}

			if tt.args.indices[0] == "error" {
				monkey.PatchInstanceMethod(reflect.TypeOf(s), "Do", func(client *elastic.IndicesForcemergeService, ctx context.Context) (*elastic.IndicesForcemergeResponse, error) {
					return nil, errors.New("error")
				})
				logger.EXPECT().Error(gomock.Any())
			}

			if tt.args.indices[0] == "error-shards" {
				monkey.PatchInstanceMethod(reflect.TypeOf(s), "Do", func(client *elastic.IndicesForcemergeService, ctx context.Context) (*elastic.IndicesForcemergeResponse, error) {
					return &elastic.IndicesForcemergeResponse{
						Shards: &elastic.ShardsInfo{
							Failures: []*elastic.ShardFailure{
								{
									Index: "error-test",
								},
							},
						},
					}, nil
				})
				logger.EXPECT().Errorf(gomock.Any(), gomock.Any())
			}

			err := p.forceMerge(tt.args.ctx, tt.args.indices...)
			if (err != nil) != tt.wantError {
				t.Errorf("forceMerge(), wantError %v", tt.wantError)
			}
		})
	}
}

func Test_provider_deleteByQuery(t *testing.T) {
	type fields struct {
		Cfg                      *config
		Log                      logs.Logger
		election                 election.Interface
		loader                   loader.Interface
		retentions               RetentionStrategy
		clearCh                  chan *clearRequest
		minIndicesStoreInDisk    int64
		rolloverBodyForDiskClean string
		rolloverAliasPatterns    []*indexAliasPattern
		ttlTaskCh                chan *TtlTask
	}
	tests := []struct {
		name   string
		fields fields
	}{
		{
			"case1", fields{
				Cfg:                      nil,
				Log:                      nil,
				election:                 nil,
				loader:                   nil,
				retentions:               nil,
				clearCh:                  nil,
				minIndicesStoreInDisk:    0,
				rolloverBodyForDiskClean: "",
				rolloverAliasPatterns:    nil,
				ttlTaskCh:                nil,
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			ctrl := gomock.NewController(t)
			var logger *mock.MockLogger
			defer ctrl.Finish()
			logger = mock.NewMockLogger(ctrl)
			logger.EXPECT().Infof(gomock.Any(), gomock.Any())

			indices := NewMockInterface(ctrl)
			defer ctrl.Finish()
			now := time.Now()
			indices.EXPECT().WaitAndGetIndices(context.Background())
			indices.EXPECT().AllIndices().Return(&loader.IndexGroup{
				List: []*loader.IndexEntry{
					{
						Index:      "d",
						StoreBytes: 2,
						MaxT:       now.AddDate(0, 0, -3),
					},
					{
						Index:      "a",
						StoreBytes: 1,
						MaxT:       now.AddDate(0, 0, -1),
					},
					{
						Index:      "b",
						StoreBytes: 1,
						MaxT:       now.AddDate(0, 0, -2),
					},
					{
						Index:      "c",
						StoreBytes: 2,
						MaxT:       now.AddDate(0, 0, -2),
					},
				}})

			p := &provider{
				Log:    logger,
				loader: indices,
			}

			esClient := &elastic.Client{}
			monkey.PatchInstanceMethod(reflect.TypeOf(esClient), "WaitAndGetIndices", func(client *elastic.Client, indices ...string) *elastic.IndicesForcemergeService {
				return &elastic.IndicesForcemergeService{}
			})
			p.deleteByQuery()
		})
	}
}
