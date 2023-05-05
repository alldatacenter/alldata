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
	"testing"
	"time"

	"github.com/golang/mock/gomock"
	"gotest.tools/assert"

	"github.com/erda-project/erda-infra/base/logs"
	election "github.com/erda-project/erda-infra/providers/etcd-election"
	"github.com/erda-project/erda/internal/tools/monitor/core/storekit/elasticsearch/index/loader"
	"github.com/erda-project/erda/pkg/mock"
)

// -go:generate mockgen -destination=./mock_loader_test.go -package cleaner -source=../loader/interface.go Interface
func Test_getSortedIndices_Should_Success(t *testing.T) {
	now := time.Now()

	ctrl := gomock.NewController(t)
	indices := NewMockInterface(ctrl)
	defer ctrl.Finish()

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
		loader: indices,
	}

	want := []*loader.IndexEntry{
		{
			Index:      "d",
			StoreBytes: 2,
			MaxT:       now.AddDate(0, 0, -3),
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
		{
			Index:      "a",
			StoreBytes: 1,
			MaxT:       now.AddDate(0, 0, -1),
		},
	}

	result := p.getSortedIndices()

	assert.DeepEqual(t, result, want)
}

func Test_provider_runDocsCheckAndClean(t *testing.T) {
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
	}
	tests := []struct {
		name   string
		fields fields
	}{
		{"case1", fields{
			Cfg: &config{DiskClean: diskClean{
				Enable: true,
				TTL: struct {
					Enable            bool   `json:"enable" default:"true"`
					MaxStoreTime      int    `file:"max_store_time" default:"7"`
					TriggerSpecCron   string `file:"trigger_spec_cron" default:"0 0 3 * * *"`
					TaskCheckInterval int64  `json:"task_check_interval" default:"5"`
				}{
					Enable: false,
				},
			}},
		}},
		{"case2", fields{
			Cfg: &config{DiskClean: diskClean{
				Enable: true,
				TTL: struct {
					Enable            bool   `json:"enable" default:"true"`
					MaxStoreTime      int    `file:"max_store_time" default:"7"`
					TriggerSpecCron   string `file:"trigger_spec_cron" default:"0 0 3 * * *"`
					TaskCheckInterval int64  `json:"task_check_interval" default:"5"`
				}{
					Enable:          true,
					TriggerSpecCron: "0 0 3 * * *",
					MaxStoreTime:    7,
				},
			}},
		}},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			var logger *mock.MockLogger
			if tt.fields.Cfg.DiskClean.TTL.Enable {
				ctrl := gomock.NewController(t)
				defer ctrl.Finish()
				logger = mock.NewMockLogger(ctrl)
				logger.EXPECT().Infof(gomock.Any(), gomock.Any())
			}
			p := &provider{
				Cfg: tt.fields.Cfg,
				Log: logger,
			}
			p.runDocsCheckAndClean(context.Background())
		})
	}
}

func Test_provider_AddTask(t *testing.T) {
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
		task *TtlTask
	}
	tests := []struct {
		name   string
		fields fields
		args   args
	}{
		{"case1", fields{ttlTaskCh: make(chan *TtlTask, 1)}, args{task: &TtlTask{TaskId: "id", Indices: []string{"test-index"}}}},
		{"case2", fields{ttlTaskCh: make(chan *TtlTask, 1)}, args{task: nil}},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p := &provider{
				ttlTaskCh: tt.fields.ttlTaskCh,
			}
			p.AddTask(tt.args.task)
		})
	}
}
