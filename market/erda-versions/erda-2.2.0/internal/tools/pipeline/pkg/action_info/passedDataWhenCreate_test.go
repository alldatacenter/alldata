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

package action_info

import (
	"fmt"
	"reflect"
	"sync"
	"testing"

	"bou.ke/monkey"
	"github.com/alecthomas/assert"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/actionmgr"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/parser/diceyml"
	"github.com/erda-project/erda/pkg/parser/pipelineyml"
)

func Test_passedDataWhenCreate_putPassedDataByPipelineYml(t *testing.T) {
	type fields struct {
		actionMgr        actionmgr.Interface
		actionJobDefines map[string]*diceyml.Job
		actionJobSpecs   map[string]*apistructs.ActionSpec
	}
	type args struct {
		pipelineYml string
		p           *spec.Pipeline
	}
	tests := []struct {
		name                 string
		fields               fields
		args                 args
		wantErr              bool
		wantActionJobDefines []string
		wantActionJobSpecs   []string
	}{
		{
			name: "normal",
			fields: fields{
				actionJobSpecs:   map[string]*apistructs.ActionSpec{},
				actionJobDefines: map[string]*diceyml.Job{},
			},
			args: args{
				pipelineYml: "version: \"1.1\"\nstages:\n  - stage:\n      - git-checkout:\n          alias: git-checkout\n          description: 代码仓库克隆\n  - stage:\n      - java:\n          alias: java-demo\n          description: 针对 java 工程的编译打包任务，产出可运行镜像\n          version: \"1.0\"\n          params:\n            build_type: maven\n            container_type: spring-boot\n            jdk_version: \"11\"\n            target: ./target/docker-java-app-example.jar\n            workdir: ${git-checkout}\n  - stage:\n      - release:\n          alias: release\n          description: 用于打包完成时，向dicehub 提交完整可部署的dice.yml。用户若没在pipeline.yml里定义该action，CI会自动在pipeline.yml里插入该action\n          params:\n            dice_yml: ${git-checkout}/dice.yml\n            image:\n              java-demo: ${java-demo:OUTPUT:image}\n  - stage:\n      - dice:\n          alias: dice\n          description: 用于 dice 平台部署应用服务\n          params:\n            release_id: ${release:OUTPUT:releaseID}\n",
				p: &spec.Pipeline{
					PipelineBase: spec.PipelineBase{
						PipelineSource: apistructs.PipelineSourceDice,
					},
				},
			},
			wantActionJobDefines: []string{"git-checkout", "java@1.0", "release", "dice"},
			wantActionJobSpecs:   []string{"git-checkout", "java@1.0", "release", "dice"},
		},
		{
			name: "want_error",
			fields: fields{
				actionJobSpecs:   map[string]*apistructs.ActionSpec{},
				actionJobDefines: map[string]*diceyml.Job{},
			},
			args: args{
				pipelineYml: "version: \"1.1\"\nstages:\n  - stage:\n      - git-checkout:\n          alias: git-checkout\n          description: 代码仓库克隆\n  - stage:\n      - java:\n          alias: java-demo\n          description: 针对 java 工程的编译打包任务，产出可运行镜像\n          version: \"1.0\"\n          params:\n            build_type: maven\n            container_type: spring-boot\n            jdk_version: \"11\"\n            target: ./target/docker-java-app-example.jar\n            workdir: ${git-checkout}\n  - stage:\n      - release:\n          alias: release\n          description: 用于打包完成时，向dicehub 提交完整可部署的dice.yml。用户若没在pipeline.yml里定义该action，CI会自动在pipeline.yml里插入该action\n          params:\n            dice_yml: ${git-checkout}/dice.yml\n            image:\n              java-demo: ${java-demo:OUTPUT:image}\n  - stage:\n      - dice:\n          alias: dice\n          description: 用于 dice 平台部署应用服务\n          params:\n            release_id: ${release:OUTPUT:releaseID}\n",
				p: &spec.Pipeline{
					PipelineBase: spec.PipelineBase{
						PipelineSource: apistructs.PipelineSourceDice,
					},
				},
			},
			wantActionJobDefines: []string{},
			wantActionJobSpecs:   []string{},
			wantErr:              true,
		},
		{
			name: "api-test_1.0",
			fields: fields{
				actionJobSpecs:   map[string]*apistructs.ActionSpec{},
				actionJobDefines: map[string]*diceyml.Job{},
			},
			args: args{
				pipelineYml: "version: '1.1'\nstages:\n  - - alias: api-test\n      type: api-test\n      description: 执行单个接口测试。上层可以通过 pipeline.yml 编排一组接口测试的执行顺序。\n      version: '1.0'\n      params:\n        body:\n          type: none\n        method: GET\n        url: /api/user\n      resources: {}\n      displayName: 接口测试\n      logoUrl: >-\n        //terminus-paas.oss-cn-hangzhou.aliyuncs.com/paas-doc/2020/10/10/24195384-07b7-4203-93e1-666373639af4.png\n  - - alias: api-test1\n      type: api-test\n      description: 执行单个接口测试。上层可以通过 pipeline.yml 编排一组接口测试的执行顺序。\n      version: '1.0'\n      params:\n        body:\n          type: none\n        method: GET\n        url: /api/user\n      resources: {}\n      displayName: 接口测试\n      logoUrl: >-\n        //terminus-paas.oss-cn-hangzhou.aliyuncs.com/paas-doc/2020/10/10/24195384-07b7-4203-93e1-666373639af4.png\nflatActions: null\nlifecycle: null\n",
				p: &spec.Pipeline{
					PipelineBase: spec.PipelineBase{
						PipelineSource: apistructs.PipelineSourceDice,
					},
				},
			},
			wantActionJobDefines: []string{"api-test@1.0"},
			wantActionJobSpecs:   []string{"api-test@1.0"},
		},
		{
			name: "api-test_1.0_with_2.0",
			fields: fields{
				actionJobSpecs:   map[string]*apistructs.ActionSpec{},
				actionJobDefines: map[string]*diceyml.Job{},
			},
			args: args{
				pipelineYml: "version: '1.1'\nstages:\n  - - alias: api-test\n      type: api-test\n      description: 执行单个接口测试。上层可以通过 pipeline.yml 编排一组接口测试的执行顺序。\n      version: '2.0'\n      params:\n        body:\n          type: none\n        method: GET\n        url: /api/user\n      resources: {}\n      displayName: 接口测试\n      logoUrl: >-\n        //terminus-paas.oss-cn-hangzhou.aliyuncs.com/paas-doc/2020/10/10/24195384-07b7-4203-93e1-666373639af4.png\n  - - alias: api-test1\n      type: api-test\n      description: 执行单个接口测试。上层可以通过 pipeline.yml 编排一组接口测试的执行顺序。\n      version: '1.0'\n      params:\n        body:\n          type: none\n        method: GET\n        url: /api/user\n      resources: {}\n      displayName: 接口测试\n      logoUrl: >-\n        //terminus-paas.oss-cn-hangzhou.aliyuncs.com/paas-doc/2020/10/10/24195384-07b7-4203-93e1-666373639af4.png\nflatActions: null\nlifecycle: null\n",
				p: &spec.Pipeline{
					PipelineBase: spec.PipelineBase{
						PipelineSource: apistructs.PipelineSourceDice,
					},
				},
			},
			wantActionJobDefines: []string{"api-test@1.0", "api-test@2.0"},
			wantActionJobSpecs:   []string{"api-test@1.0", "api-test@2.0"},
		},
		{
			name: "snippet",
			fields: fields{
				actionJobSpecs:   map[string]*apistructs.ActionSpec{},
				actionJobDefines: map[string]*diceyml.Job{},
			},
			args: args{
				pipelineYml: "version: '1.1'\nstages:\n  - - alias: api-test\n      type: api-test\n      description: 执行单个接口测试。上层可以通过 pipeline.yml 编排一组接口测试的执行顺序。\n      version: '1.0'\n      params:\n        body:\n          type: none\n        method: GET\n        url: /api/user\n      resources: {}\n      displayName: 接口测试\n      logoUrl: >-\n        //terminus-paas.oss-cn-hangzhou.aliyuncs.com/paas-doc/2020/10/10/24195384-07b7-4203-93e1-666373639af4.png\n  - - alias: snippet\n      type: snippet\n      description: 嵌套流水线可以声明嵌套的其他 pipeline.yml\n      resources: {}\n      displayName: 嵌套流水线\n      logoUrl: >-\n        http://terminus-paas.oss-cn-hangzhou.aliyuncs.com/paas-doc/2020/10/22/410935c6-e399-463a-b87b-0b774240d12e.png\nflatActions: null\nlifecycle: null\n",
				p: &spec.Pipeline{
					PipelineBase: spec.PipelineBase{
						PipelineSource: apistructs.PipelineSourceDice,
					},
				},
			},
			wantActionJobDefines: []string{"api-test@1.0"},
			wantActionJobSpecs:   []string{"api-test@1.0"},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {

			var actionJobDefines = sync.Map{}
			var actionJobSpecs = sync.Map{}

			for key, value := range tt.fields.actionJobDefines {
				actionJobDefines.Store(key, value)
			}

			for key, value := range tt.fields.actionJobSpecs {
				actionJobSpecs.Store(key, value)
			}

			that := &PassedDataWhenCreate{
				actionJobDefines: &actionJobDefines,
				actionJobSpecs:   &actionJobSpecs,
			}
			yml, err := pipelineyml.New([]byte(tt.args.pipelineYml))
			assert.NoError(t, err)

			mockActionMgr := &actionmgr.MockActionMgr{}
			that.actionMgr = mockActionMgr
			patch := monkey.PatchInstanceMethod(reflect.TypeOf(mockActionMgr), "SearchActions", func(_ *actionmgr.MockActionMgr, items []string, location []string, ops ...actionmgr.OpOption) (map[string]*diceyml.Job, map[string]*apistructs.ActionSpec, error) {
				actionJobMap := make(map[string]*diceyml.Job)
				actionSpecMap := make(map[string]*apistructs.ActionSpec)
				for _, item := range items {
					actionJobMap[item] = &diceyml.Job{}
					actionSpecMap[item] = &apistructs.ActionSpec{}
				}
				if tt.wantErr {
					return nil, nil, fmt.Errorf("want error")
				}
				return actionJobMap, actionSpecMap, nil
			})

			if err := that.PutPassedDataByPipelineYml(yml, tt.args.p); (err != nil) != tt.wantErr {
				t.Errorf("putPassedDataByPipelineYml() error = %v, wantErr %v", err, tt.wantErr)
			}

			var actionJobDefinesLen int
			that.actionJobDefines.Range(func(key, value interface{}) bool {
				actionJobDefinesLen += 1
				return true
			})

			var actionJobSpecsLen int
			that.actionJobSpecs.Range(func(key, value interface{}) bool {
				actionJobSpecsLen += 1
				return true
			})

			assert.Equal(t, actionJobDefinesLen, len(tt.wantActionJobDefines), "wantActionJobDefines")
			assert.Equal(t, actionJobSpecsLen, len(tt.wantActionJobSpecs), "wantActionJobSpecs")

			for _, v := range tt.wantActionJobSpecs {
				value, ok := that.actionJobSpecs.Load(v)
				assert.True(t, ok)
				assert.NotEmpty(t, value)
			}
			for _, v := range tt.wantActionJobDefines {
				value, ok := that.actionJobDefines.Load(v)
				assert.True(t, ok)
				assert.NotEmpty(t, value)
			}

			patch.Unpatch()
		})
	}
}

func TestPassedDataWhenCreate_InitData(t *testing.T) {
	var d *PassedDataWhenCreate
	d.InitData(nil, nil)
	if d != nil {
		t.Fatalf("should be nil")
	}

	d = &PassedDataWhenCreate{}
	d.InitData(nil, nil)
	if d.actionJobDefines == nil {
		t.Fatalf("actionJobDefines should be initialized")
	}
	if d.actionJobSpecs == nil {
		t.Fatalf("actionJobSpecs should be initialized")
	}

	d = &PassedDataWhenCreate{}
	actionMgr := &actionmgr.MockActionMgr{}
	d.InitData(nil, actionMgr)
	if d.actionMgr == nil {
		t.Fatalf("actionMgr should not be nil")
	}
}
