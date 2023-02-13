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

package cache

import (
	"reflect"
	"testing"

	"bou.ke/monkey"

	"github.com/erda-project/erda/internal/tools/pipeline/dbclient"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/parser/pipelineyml"
)

func Test_getOrSetStagesFromContext(t *testing.T) {
	type args struct {
		stages []spec.PipelineStage
	}
	tests := []struct {
		name       string
		args       args
		wantStages []spec.PipelineStage
		wantErr    bool
	}{
		{
			name: "get caches stages",
			args: args{
				stages: []spec.PipelineStage{
					{
						ID: 1,
					},
				},
			},
			wantStages: []spec.PipelineStage{
				{
					ID: 1,
				},
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			var client *dbclient.Client
			patch := monkey.PatchInstanceMethod(reflect.TypeOf(client), "ListPipelineStageByPipelineID", func(client *dbclient.Client, pipelineID uint64, ops ...dbclient.SessionOption) ([]spec.PipelineStage, error) {
				return tt.args.stages, nil
			})

			var p provider
			p.dbClient = client

			gotStages, err := p.GetOrSetStagesFromContext(1)
			if (err != nil) != tt.wantErr {
				t.Errorf("getOrSetStagesFromContext() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(gotStages, tt.wantStages) {
				t.Errorf("getOrSetStagesFromContext() gotStages = %v, want %v", gotStages, tt.wantStages)
			}

			if !reflect.DeepEqual(gotStages, p.getStagesCachesFromContextByPipelineID(1)) {
				t.Errorf("getOrSetStagesFromContext() gotStages = %v, want %v", gotStages, tt.wantStages)
			}

			p.ClearReconcilerPipelineContextCaches(1)
			patch.Unpatch()
		})
	}
}

func Test_getOrSetPipelineRerunSuccessTasksFromContext(t *testing.T) {
	type args struct {
		tasks map[string]*spec.PipelineTask
	}
	tests := []struct {
		name                          string
		args                          args
		wantPipelineRerunSuccessTasks map[string]*spec.PipelineTask
		wantErr                       bool
	}{
		{
			name: "get caches stages",
			args: args{
				tasks: map[string]*spec.PipelineTask{
					"git-checkout": {
						ID: 1,
					},
				},
			},
			wantPipelineRerunSuccessTasks: map[string]*spec.PipelineTask{
				"git-checkout": {
					ID: 1,
				},
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			var client *dbclient.Client
			patch := monkey.PatchInstanceMethod(reflect.TypeOf(client), "GetPipeline", func(client *dbclient.Client, id interface{}, ops ...dbclient.SessionOption) (spec.Pipeline, error) {
				return spec.Pipeline{}, nil
			})

			patch1 := monkey.PatchInstanceMethod(reflect.TypeOf(client), "ParseRerunFailedDetail", func(client *dbclient.Client, detail *spec.RerunFailedDetail) (map[string]*spec.PipelineTask, map[string]*spec.PipelineTask, error) {
				return tt.args.tasks, nil, nil
			})

			var p provider
			p.dbClient = client

			gotTasks, err := p.GetOrSetPipelineRerunSuccessTasksFromContext(1)
			if (err != nil) != tt.wantErr {
				t.Errorf("getOrSetPipelineRerunSuccessTasksFromContext() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(gotTasks, tt.wantPipelineRerunSuccessTasks) {
				t.Errorf("getOrSetPipelineRerunSuccessTasksFromContext() gotStages = %v, want %v", gotTasks, tt.wantPipelineRerunSuccessTasks)
			}

			cacheTasks := p.getPipelineRerunSuccessTasksFromContextByPipelineID(1)
			if !reflect.DeepEqual(gotTasks, cacheTasks) {
				t.Errorf("getOrSetPipelineRerunSuccessTasksFromContext() gotStages = %v, want %v", gotTasks, cacheTasks)
			}

			patch.Unpatch()
			patch1.Unpatch()
			p.ClearReconcilerPipelineContextCaches(1)
		})
	}
}

func Test_getOrSetPipelineYmlFromContext(t *testing.T) {
	type args struct {
		yml string
	}
	tests := []struct {
		name    string
		args    args
		wantYml string
		wantErr bool
	}{
		{
			name: "get caches pipelineYml",
			args: args{
				yml: "version: \"1.1\"\nstages: []\n",
			},
			wantYml: "version: \"1.1\"\nstages: []\n",
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			var client *dbclient.Client
			patch := monkey.PatchInstanceMethod(reflect.TypeOf(client), "GetPipeline", func(client *dbclient.Client, id interface{}, ops ...dbclient.SessionOption) (spec.Pipeline, error) {
				var pipeline = spec.Pipeline{
					PipelineExtra: spec.PipelineExtra{
						PipelineYml: tt.args.yml,
					},
				}
				return pipeline, nil
			})

			var p provider
			p.dbClient = client

			gotYml, err := p.GetOrSetPipelineYmlFromContext(1)
			if (err != nil) != tt.wantErr {
				t.Errorf("getOrSetPipelineYmlFromContext() error = %v, wantErr %v", err, tt.wantErr)
				return
			}

			pipelineYml, _ := pipelineyml.New(
				[]byte(tt.wantYml),
			)
			if !reflect.DeepEqual(gotYml, pipelineYml) {
				t.Errorf("getOrSetPipelineYmlFromContext() gotStages = %v, want %v", gotYml, pipelineYml)
			}

			cacheYml := p.getPipelineYmlCachesFromContextByPipelineID(1)
			if !reflect.DeepEqual(gotYml, cacheYml) {
				t.Errorf("getOrSetPipelineYmlFromContext() gotStages = %v, want %v", gotYml, cacheYml)
			}

			patch.Unpatch()
			p.ClearReconcilerPipelineContextCaches(1)
		})
	}
}
