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

package db

import (
	"testing"
	"time"

	"github.com/alecthomas/assert"
	"github.com/ghodss/yaml"
	"google.golang.org/protobuf/types/known/structpb"
	"google.golang.org/protobuf/types/known/timestamppb"

	"github.com/erda-project/erda-proto-go/core/pipeline/action/pb"
)

const releaseFetchSpec = `
name: release-fetch
version: "1.0"
type: action
logoUrl: http://terminus-paas.oss-cn-hangzhou.aliyuncs.com/paas-doc/2022/04/15/b23127cf-0ba7-48d1-b763-b8cd8c6e26d4.png
category: deploy_management
desc: Fetch release by query
public: true
supportedErdaVersions:
  - ">= 1.5"

params:
  - name: application_name
    desc: The name of the application
  - name: branch
    desc: git branch to fetch first matching release

outputs:
  - name: release_id
    desc: release id got from query

accessibleAPIs:
  - path: /api/applications
    method: GET
    schema: http

  - path: /api/releases
    method: GET
    schema: http
`

const javaBuildSpec = `
name: java-build
version: "1.0"
type: action
category: build_management
displayName: ${{ i18n.displayName }}
logoUrl: //terminus-paas.oss-cn-hangzhou.aliyuncs.com/paas-doc/2020/09/28/d74a0d23-523d-4451-9647-f32f47b2000d.png
desc: ${{ i18n.desc }}
labels:
  maintainer: xxx
  project_level_app: true
public: true
supportedVersions: # Deprecated. Please use supportedErdaVersions instead.
  - ">= 3.9"
supportedErdaVersions:
  - ">= 1.0"

params:
  - name: jdk_version
    desc: ${{ i18n.params.jdk_version.desc }}
    required: true
  - name: build_cmd
    type: string_array
    desc: ${{ i18n.params.build_cmd.desc }}
    required: true
  - name: workdir
    desc: ${{ i18n.params.workdir.desc }}
    default: "."
accessibleAPIs:
  # api compatibility check
  - path: /api/gateway/check-compatibility
    method: POST
    schema: http

outputs:
  - name: buildPath
    desc: ${{ i18n.outputs.buildPath.desc }}
  - name: JAVA_OPTS
    desc: ${{ i18n.outputs.JAVA_OPTS.desc }}

locale:
  zh-CN:
    desc: 针对 java 工程的编译打包任务
    displayName: Java 工程编译打包
    outputs.JAVA_OPTS.desc: 需要假如的监控agent入参
    outputs.buildPath.desc: 包的位置
    params.build_cmd.desc: 构建命令,如:./gradlew :user:build，mvn package
    params.jdk_version.desc: 构建使用的jdk版本,支持8,11
    params.workdir.desc: 工作目录一般为仓库路径 ${git-checkout}

  en-US:
    desc: Build and package a Java project
    displayName: Java project build and package
    outputs.JAVA_OPTS.desc: The parameters for monitoring agent
    outputs.buildPath.desc: The path of the package
    params.build_cmd.desc: The build command, such as ./gradlew :user:build, mvn package
    params.jdk_version.desc: The JDK version to build, support 8, 11
    params.workdir.desc: The working directory, usually the repository path ${git-checkout}	
`

func TestPipelineAction_Convert(t *testing.T) {
	type fields struct {
		TimeCreated time.Time
		TimeUpdated time.Time
		DisplayName string
		Desc        string
		Dice        string
		Spec        string
	}
	type args struct {
		yamlFormat bool
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    *pb.Action
		wantErr bool
	}{
		{
			name: "test no local",
			fields: fields{
				Spec: releaseFetchSpec,
			},
			args: args{
				yamlFormat: true,
			},
			want: &pb.Action{
				Spec: func() *structpb.Value {
					var specInterface = map[string]interface{}{}
					err := yaml.Unmarshal([]byte(releaseFetchSpec), &specInterface)
					if err != nil {
						return nil
					}

					result, err := yaml.Marshal(specInterface)
					if err != nil {
						return nil
					}
					return structpb.NewStringValue(string(result))
				}(),
				Dice:        structpb.NewStringValue(""),
				Desc:        "Fetch release by query",
				TimeCreated: timestamppb.New(time.Unix(-62135596800, 0)),
				TimeUpdated: timestamppb.New(time.Unix(-62135596800, 0)),
			},
			wantErr: false,
		},

		{
			name: "test no local",
			fields: fields{
				Spec: releaseFetchSpec,
			},
			args: args{
				yamlFormat: false,
			},
			want: &pb.Action{
				Spec: func() *structpb.Value {
					var specInterface = map[string]interface{}{}
					err := yaml.Unmarshal([]byte(releaseFetchSpec), &specInterface)
					if err != nil {
						return nil
					}
					result, err := structpb.NewValue(specInterface)
					if err != nil {
						return nil
					}
					return result
				}(),
				Dice: func() *structpb.Value {
					var dice = map[string]interface{}{}
					err := yaml.Unmarshal([]byte(""), &dice)
					if err != nil {
						return nil
					}
					dicePbValue, err := structpb.NewValue(dice)
					if err != nil {
						return nil
					}
					return dicePbValue
				}(),
				Desc:        "Fetch release by query",
				TimeCreated: timestamppb.New(time.Unix(-62135596800, 0)),
				TimeUpdated: timestamppb.New(time.Unix(-62135596800, 0)),
			},
			wantErr: false,
		},
		{
			name: "test local",
			fields: fields{
				Spec: javaBuildSpec,
			},
			args: args{
				yamlFormat: true,
			},
			want: &pb.Action{
				Spec: func() *structpb.Value {
					_, specInterface := SpecI18nReplace(javaBuildSpec)
					result, err := yaml.Marshal(specInterface)
					if err != nil {
						return nil
					}
					return structpb.NewStringValue(string(result))
				}(),
				Dice:        structpb.NewStringValue(""),
				Desc:        "针对 java 工程的编译打包任务",
				DisplayName: "Java 工程编译打包",
				TimeCreated: timestamppb.New(time.Unix(-62135596800, 0)),
				TimeUpdated: timestamppb.New(time.Unix(-62135596800, 0)),
			},
			wantErr: false,
		},
		{
			name: "test local",
			fields: fields{
				Spec: javaBuildSpec,
			},
			args: args{
				yamlFormat: false,
			},
			want: &pb.Action{
				Spec: func() *structpb.Value {
					_, specInterfaceValue := SpecI18nReplace(javaBuildSpec)
					specInterface := specInterfaceValue.(map[string]interface{})
					result, err := structpb.NewValue(specInterface)
					if err != nil {
						return nil
					}
					return result
				}(),
				Dice: func() *structpb.Value {
					var dice = map[string]interface{}{}
					err := yaml.Unmarshal([]byte(""), &dice)
					if err != nil {
						return nil
					}
					dicePbValue, err := structpb.NewValue(dice)
					if err != nil {
						return nil
					}
					return dicePbValue
				}(),
				Desc:        "针对 java 工程的编译打包任务",
				DisplayName: "Java 工程编译打包",
				TimeCreated: timestamppb.New(time.Unix(-62135596800, 0)),
				TimeUpdated: timestamppb.New(time.Unix(-62135596800, 0)),
			},
			wantErr: false,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			action := &PipelineAction{
				TimeCreated: tt.fields.TimeCreated,
				TimeUpdated: tt.fields.TimeUpdated,
				DisplayName: tt.fields.DisplayName,
				Desc:        tt.fields.Desc,
				Dice:        tt.fields.Dice,
				Spec:        tt.fields.Spec,
			}
			got, err := action.Convert(tt.args.yamlFormat)
			if (err != nil) != tt.wantErr {
				t.Errorf("Convert() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			assert.EqualValues(t, got, tt.want)
		})
	}
}
