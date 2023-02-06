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

package pipelinesvc

import (
	"fmt"
	"reflect"
	"testing"

	"bou.ke/monkey"
	"github.com/alecthomas/assert"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/dbclient"
	"github.com/erda-project/erda/internal/tools/pipeline/pkg/action_info"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/actionmgr"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/parser/pipelineyml"
)

func TestPipelineSvc_OperateTask(t *testing.T) {
	type args struct {
		p                *spec.Pipeline
		task             *spec.PipelineTask
		stage            *spec.PipelineStage
		searchStageError error
	}
	tests := []struct {
		name       string
		args       args
		wantErr    bool
		wantStatus apistructs.PipelineStatus
	}{
		{
			name: "empty pipeline TaskOperates empty",
			args: args{
				p: &spec.Pipeline{
					PipelineBase: spec.PipelineBase{},
					PipelineExtra: spec.PipelineExtra{
						Extra: spec.PipelineExtraInfo{},
					},
				},
				task:  &spec.PipelineTask{},
				stage: nil,
			},
			wantErr: false,
		},
		{
			name: "not find match taskAlias",
			args: args{
				p: &spec.Pipeline{
					PipelineBase: spec.PipelineBase{},
					PipelineExtra: spec.PipelineExtra{
						Extra: spec.PipelineExtraInfo{
							TaskOperates: []apistructs.PipelineTaskOperateRequest{
								{
									TaskAlias: "git-checkout",
									Disable:   &[]bool{true}[0],
									Pause:     &[]bool{true}[0],
								},
							},
						},
					},
				},
				task: &spec.PipelineTask{
					Name: "dice",
				},
				searchStageError: fmt.Errorf("error"),
				stage:            nil,
			},
			wantErr: false,
		},
		{
			name: "task have id",
			args: args{
				p: &spec.Pipeline{
					PipelineBase: spec.PipelineBase{},
					PipelineExtra: spec.PipelineExtra{
						Extra: spec.PipelineExtraInfo{
							TaskOperates: []apistructs.PipelineTaskOperateRequest{
								{
									TaskAlias: "git-checkout",
									Disable:   &[]bool{true}[0],
									Pause:     &[]bool{true}[0],
								},
							},
						},
					},
				},
				task: &spec.PipelineTask{
					Name: "git-checkout",
					ID:   1,
				},
				searchStageError: fmt.Errorf("error"),
				stage:            nil,
			},
			wantErr: false,
		},
		{
			name: "disable paused task",
			args: args{
				p: &spec.Pipeline{
					PipelineBase: spec.PipelineBase{},
					PipelineExtra: spec.PipelineExtra{
						Extra: spec.PipelineExtraInfo{
							TaskOperates: []apistructs.PipelineTaskOperateRequest{
								{
									TaskAlias: "git-checkout",
									Disable:   &[]bool{true}[0],
									Pause:     &[]bool{true}[0],
								},
							},
						},
					},
				},
				task: &spec.PipelineTask{
					Name:   "git-checkout",
					Status: apistructs.PipelineStatusPaused,
				},
				stage: nil,
			},
			wantErr: true,
		},
		{
			name: "paused Analyzed task",
			args: args{
				p: &spec.Pipeline{
					PipelineBase: spec.PipelineBase{},
					PipelineExtra: spec.PipelineExtra{
						Extra: spec.PipelineExtraInfo{
							TaskOperates: []apistructs.PipelineTaskOperateRequest{
								{
									TaskAlias: "git-checkout",
									Pause:     &[]bool{true}[0],
								},
							},
						},
					},
				},
				task: &spec.PipelineTask{
					Name:   "git-checkout",
					Status: apistructs.PipelineStatusRunning,
				},
				stage: nil,
			},
			wantErr: true,
		},
		{
			name: "not paused Analyzed task",
			args: args{
				p: &spec.Pipeline{
					PipelineBase: spec.PipelineBase{},
					PipelineExtra: spec.PipelineExtra{
						Extra: spec.PipelineExtraInfo{
							TaskOperates: []apistructs.PipelineTaskOperateRequest{
								{
									TaskAlias: "git-checkout",
									Pause:     &[]bool{false}[0],
								},
							},
						},
					},
				},
				task: &spec.PipelineTask{
					Name:   "git-checkout",
					Status: apistructs.PipelineStatusAnalyzed,
				},
				stage:            &spec.PipelineStage{},
				searchStageError: fmt.Errorf("error"),
			},
			wantErr: true,
		},
		{
			name: "disable task",
			args: args{
				p: &spec.Pipeline{
					PipelineBase: spec.PipelineBase{},
					PipelineExtra: spec.PipelineExtra{
						Extra: spec.PipelineExtraInfo{
							TaskOperates: []apistructs.PipelineTaskOperateRequest{
								{
									TaskAlias: "git-checkout",
									Disable:   &[]bool{true}[0],
								},
							},
						},
					},
				},
				task: &spec.PipelineTask{
					Name:   "git-checkout",
					Status: apistructs.PipelineStatusAnalyzed,
				},
				stage: &spec.PipelineStage{},
			},
			wantErr:    false,
			wantStatus: apistructs.PipelineStatusDisabled,
		},
		{
			name: "paused task",
			args: args{
				p: &spec.Pipeline{
					PipelineBase: spec.PipelineBase{},
					PipelineExtra: spec.PipelineExtra{
						Extra: spec.PipelineExtraInfo{
							TaskOperates: []apistructs.PipelineTaskOperateRequest{
								{
									TaskAlias: "git-checkout",
									Pause:     &[]bool{true}[0],
								},
							},
						},
					},
				},
				task: &spec.PipelineTask{
					Name:   "git-checkout",
					Status: apistructs.PipelineStatusAnalyzed,
				},
				stage: &spec.PipelineStage{},
			},
			wantErr:    false,
			wantStatus: apistructs.PipelineStatusPaused,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			var client *dbclient.Client
			patch := monkey.PatchInstanceMethod(reflect.TypeOf(client), "GetPipelineStage", func(client *dbclient.Client, id interface{}, ops ...dbclient.SessionOption) (spec.PipelineStage, error) {
				return *tt.args.stage, tt.args.searchStageError
			})
			s := &PipelineSvc{
				dbClient: client,
			}

			task, err := s.OperateTask(tt.args.p, tt.args.task)
			if (err != nil) != tt.wantErr {
				t.Errorf("OperateTask() error = %v, wantErr %v", err, tt.wantErr)
			}
			if tt.wantStatus != "" && task != nil {
				assert.Equal(t, task.Status, tt.wantStatus)
			}
			patch.Unpatch()
		})
	}
}

func TestPipelineSvc_getYmlActionTasks(t *testing.T) {
	type args struct {
		pipelineYml          *string
		p                    *spec.Pipeline
		dbStages             []spec.PipelineStage
		passedDataWhenCreate *action_info.PassedDataWhenCreate
	}
	tests := []struct {
		name    string
		args    args
		want    []spec.PipelineTask
		wantErr bool
	}{
		{
			name: "empty pipelineYml",
			args: args{
				pipelineYml: nil,
				p:           nil,
				dbStages:    nil,
			},
			want: nil,
		},
		{
			name: "get yml actions",
			args: args{
				pipelineYml: &[]string{"version: \"1.1\"\nstages:\n  - stage:\n      - git-checkout:\n          alias: git-checkout\n          description: 代码仓库克隆\n  - stage:\n      - java:\n          alias: java-demo\n          description: 针对 java 工程的编译打包任务，产出可运行镜像\n          params:\n            build_type: maven\n            container_type: spring-boot\n            target: ./target/docker-java-app-example.jar\n            workdir: ${git-checkout}\n          caches:\n            - path: /root/.m2/repository\n  - stage:\n      - release:\n          alias: release\n          description: 用于打包完成时，向dicehub 提交完整可部署的dice.yml。用户若没在pipeline.yml里定义该action，CI会自动在pipeline.yml里插入该action\n          params:\n            dice_yml: ${git-checkout}/dice.yml\n            image:\n              java-demo: ${java-demo:OUTPUT:image}\n  - stage:\n      - dice:\n          alias: dice\n          description: 用于 dice 平台部署应用服务\n          params:\n            release_id: ${release:OUTPUT:releaseID}\n  - stage:\n      - snippet:\n          alias: snippet\n"}[0],
				p: &spec.Pipeline{
					PipelineBase: spec.PipelineBase{
						ID:          1,
						ClusterName: "erda",
					},
					PipelineExtra: spec.PipelineExtra{
						Extra: spec.PipelineExtraInfo{
							Namespace: "pipeline-1",
						},
					},
				},
				dbStages: []spec.PipelineStage{
					{
						ID: 1,
					},
					{
						ID: 2,
					},
					{
						ID: 3,
					},
					{
						ID: 4,
					},
					{
						ID: 5,
					},
				},
			},
			want: []spec.PipelineTask{
				{
					Name:       "git-checkout",
					StageID:    1,
					PipelineID: 1,
					Status:     apistructs.PipelineStatusAnalyzed,
				},
				{
					Name:       "java-demo",
					StageID:    2,
					PipelineID: 1,
					Status:     apistructs.PipelineStatusAnalyzed,
				},
				{
					Name:       "release",
					StageID:    3,
					PipelineID: 1,
					Status:     apistructs.PipelineStatusAnalyzed,
				},
				{
					Name:       "dice",
					StageID:    4,
					PipelineID: 1,
					Status:     apistructs.PipelineStatusAnalyzed,
				},
				{
					Name:       "snippet",
					StageID:    5,
					PipelineID: 1,
					IsSnippet:  true,
					Status:     apistructs.PipelineStatusAnalyzed,
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s := &PipelineSvc{actionMgr: &actionmgr.MockActionMgr{}}

			var pipelineYml *pipelineyml.PipelineYml
			var err error
			if tt.args.pipelineYml != nil {
				pipelineYml, err = pipelineyml.New([]byte(*tt.args.pipelineYml))
				assert.NoError(t, err)
			}

			got := s.getYmlActionTasks(pipelineYml, tt.args.p, tt.args.dbStages, tt.args.passedDataWhenCreate)
			for index, task := range tt.want {
				assert.Equal(t, got[index].Name, task.Name, "getYmlActionTasks() name not Equal")
				assert.Equal(t, got[index].StageID, task.StageID, "getYmlActionTasks() stageID not Equal")
				assert.Equal(t, got[index].PipelineID, task.PipelineID, "getYmlActionTasks() PipelineID not Equal")
				assert.Equal(t, got[index].Status, task.Status, "getYmlActionTasks() Status not Equal")
				assert.Equal(t, got[index].IsSnippet, task.IsSnippet, "getYmlActionTasks() IsSnippet not Equal")
			}
		})
	}
}

func Test_ymlTasksMergeDBTasks(t *testing.T) {
	type args struct {
		actionTasks []spec.PipelineTask
		dbTasks     []spec.PipelineTask
	}
	tests := []struct {
		name string
		args args
		want []spec.PipelineTask
	}{
		{
			name: "empty dbTask",
			args: args{
				actionTasks: []spec.PipelineTask{
					{
						Name: "git-checkout",
					},
					{
						Name: "dice",
					},
				},
				dbTasks: []spec.PipelineTask{},
			},
			want: []spec.PipelineTask{
				{
					Name: "git-checkout",
				},
				{
					Name: "dice",
				},
			},
		},
		{
			name: "merge dbTask",
			args: args{
				actionTasks: []spec.PipelineTask{
					{
						Name:       "git-checkout",
						PipelineID: 1,
						StageID:    1,
					},
					{
						Name:       "dice",
						PipelineID: 1,
						StageID:    2,
					},
				},
				dbTasks: []spec.PipelineTask{
					{
						ID:         1,
						Name:       "git-checkout",
						PipelineID: 1,
						StageID:    1,
					},
					{
						ID:         2,
						Name:       "dice",
						PipelineID: 1,
						StageID:    2,
					},
				},
			},
			want: []spec.PipelineTask{
				{
					ID:         1,
					Name:       "git-checkout",
					PipelineID: 1,
					StageID:    1,
				},
				{
					ID:         2,
					Name:       "dice",
					PipelineID: 1,
					StageID:    2,
				},
			},
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got := ymlTasksMergeDBTasks(tt.args.actionTasks, tt.args.dbTasks)
			for index, gotTask := range got {
				assert.Equal(t, gotTask.Name, tt.want[index].Name)
				assert.Equal(t, gotTask.ID, tt.want[index].ID)
			}
		})
	}
}

func TestPipelineSvc_MergePipelineYmlTasks(t *testing.T) {
	type args struct {
		pipelineYml          string
		dbTasks              []spec.PipelineTask
		p                    *spec.Pipeline
		dbStages             []spec.PipelineStage
		passedDataWhenCreate *action_info.PassedDataWhenCreate
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{
			name: "test",
			args: args{
				pipelineYml: "version: \"1.1\"\nstages:\n  - stage:\n      - git-checkout:\n          alias: git-checkout\n          description: 代码仓库克隆\n  - stage:\n      - java:\n          alias: java-demo\n          description: 针对 java 工程的编译打包任务，产出可运行镜像\n          params:\n            build_type: maven\n            container_type: spring-boot\n            target: ./target/docker-java-app-example.jar\n            workdir: ${git-checkout}\n          caches:\n            - path: /root/.m2/repository\n  - stage:\n      - release:\n          alias: release\n          description: 用于打包完成时，向dicehub 提交完整可部署的dice.yml。用户若没在pipeline.yml里定义该action，CI会自动在pipeline.yml里插入该action\n          params:\n            dice_yml: ${git-checkout}/dice.yml\n            image:\n              java-demo: ${java-demo:OUTPUT:image}\n  - stage:\n      - dice:\n          alias: dice\n          description: 用于 dice 平台部署应用服务\n          params:\n            release_id: ${release:OUTPUT:releaseID}\n  - stage:\n      - snippet:\n          alias: snippet\n",
				dbTasks:     nil,
				p: &spec.Pipeline{
					PipelineBase: spec.PipelineBase{
						ID: 1,
					},
					PipelineExtra: spec.PipelineExtra{},
				},
				dbStages: []spec.PipelineStage{
					{
						ID: 1,
					},
					{
						ID: 2,
					},
					{
						ID: 3,
					},
					{
						ID: 4,
					}, {
						ID: 5,
					},
				},
				passedDataWhenCreate: nil,
			},
			wantErr: false,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s := &PipelineSvc{actionMgr: &actionmgr.MockActionMgr{}}
			yml, _ := pipelineyml.New([]byte(tt.args.pipelineYml))
			_, err := s.MergePipelineYmlTasks(yml, tt.args.dbTasks, tt.args.p, tt.args.dbStages, tt.args.passedDataWhenCreate)
			if (err != nil) != tt.wantErr {
				t.Errorf("MergePipelineYmlTasks() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
		})
	}
}

func TestPipelineSvc_createPipelineAndCheckNotEndStatus(t *testing.T) {
	type args struct {
		p *spec.Pipeline
	}
	tests := []struct {
		name    string
		args    args
		wantErr bool
	}{
		{
			name: "test_end_status_error",
			args: args{
				p: &spec.Pipeline{
					PipelineExtra: spec.PipelineExtra{
						Extra: spec.PipelineExtraInfo{
							SnippetChain: []uint64{1},
						},
						PipelineYml: "version: \"1.1\"\nstages:\n  - stage:\n      - git-checkout:\n          alias: git-checkout\n          description: 代码仓库克隆\n  - stage:\n      - java:\n          alias: java-demo\n          description: 针对 java 工程的编译打包任务，产出可运行镜像\n          params:\n            build_type: maven\n            container_type: spring-boot\n            target: ./target/docker-java-app-example.jar\n            workdir: ${git-checkout}\n          caches:\n            - path: /root/.m2/repository\n  - stage:\n      - release:\n          alias: release\n          description: 用于打包完成时，向dicehub 提交完整可部署的dice.yml。用户若没在pipeline.yml里定义该action，CI会自动在pipeline.yml里插入该action\n          params:\n            dice_yml: ${git-checkout}/dice.yml\n            image:\n              java-demo: ${java-demo:OUTPUT:image}\n  - stage:\n      - dice:\n          alias: dice\n          description: 用于 dice 平台部署应用服务\n          params:\n            release_id: ${release:OUTPUT:releaseID}\n  - stage:\n      - snippet:\n          alias: snippet\n",
					},
				},
			},
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s := &PipelineSvc{}

			var db = &dbclient.Client{}
			patch2 := monkey.PatchInstanceMethod(reflect.TypeOf(db), "GetPipelineBase", func(db *dbclient.Client, id uint64, ops ...dbclient.SessionOption) (spec.PipelineBase, bool, error) {
				return spec.PipelineBase{
					Status: apistructs.PipelineStatusSuccess,
				}, true, nil
			})
			defer patch2.Unpatch()

			if err := s.createPipelineAndCheckNotEndStatus(tt.args.p, nil); (err != nil) != tt.wantErr {
				t.Errorf("createPipelineAndCheckNotEndStatus() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}
