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

package projectpipeline

import (
	"context"
	"fmt"
	"reflect"
	"sort"
	"testing"
	"time"

	"bou.ke/monkey"
	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda-infra/base/logs"
	cronpb "github.com/erda-project/erda-proto-go/core/pipeline/cron/pb"
	dpb "github.com/erda-project/erda-proto-go/core/pipeline/definition/pb"
	ppb "github.com/erda-project/erda-proto-go/core/pipeline/pb"
	spb "github.com/erda-project/erda-proto-go/core/pipeline/source/pb"
	"github.com/erda-project/erda-proto-go/dop/projectpipeline/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/providers/projectpipeline/deftype"
	"github.com/erda-project/erda/pkg/strutil"
)

func TestGetRulesByCategoryKey(t *testing.T) {
	tt := []struct {
		key  apistructs.PipelineCategory
		want []string
	}{
		{apistructs.CategoryBuildDeploy, apistructs.CategoryKeyRuleMap[apistructs.CategoryBuildDeploy]},
		{apistructs.CategoryBuildArtifact, apistructs.CategoryKeyRuleMap[apistructs.CategoryBuildArtifact]},
		{apistructs.CategoryOthers, append(append(append(apistructs.CategoryKeyRuleMap[apistructs.CategoryBuildDeploy],
			apistructs.CategoryKeyRuleMap[apistructs.CategoryBuildArtifact]...), apistructs.CategoryKeyRuleMap[apistructs.CategoryBuildCombineArtifact]...),
			apistructs.CategoryKeyRuleMap[apistructs.CategoryBuildIntegration]...)},
	}
	for _, v := range tt {
		for i := range v.want {
			if !strutil.InSlice(v.want[i], getRulesByCategoryKey(v.key)) {
				t.Errorf("fail")
			}
		}
	}
}

func TestPipelineYmlsFilterIn(t *testing.T) {
	pipelineYmls := []string{"pipeline.yml", ".erda/pipelines/ci-artifact.yml", "a.yml", "b.yml"}
	uncategorizedPipelineYmls := pipelineYmlsFilterIn(pipelineYmls, func(yml string) bool {
		for k := range apistructs.GetRuleCategoryKeyMap() {
			if k == yml {
				return false
			}
		}
		return true
	})
	if len(uncategorizedPipelineYmls) != 2 {
		t.Errorf("fail")
	}
}

func TestGetFilePath(t *testing.T) {
	tt := []struct {
		path string
		want string
	}{
		{
			path: "pipeline.yml",
			want: "",
		},
		{
			path: ".erda/pipelines/pipeline.yml",
			want: ".erda/pipelines",
		},
	}
	for _, v := range tt {
		assert.Equal(t, v.want, getFilePath(v.path))
	}
}

func TestIsSameSourceInApp(t *testing.T) {
	tt := []struct {
		source *spb.PipelineSource
		params *pb.UpdateProjectPipelineRequest
		want   bool
	}{
		{
			source: &spb.PipelineSource{
				Ref:  "master",
				Path: "",
				Name: "pipeline.yml",
			},
			params: &pb.UpdateProjectPipelineRequest{
				ProjectPipelineSource: &pb.ProjectPipelineSource{
					Ref:      "master",
					Path:     "",
					FileName: "pipeline.yml",
				},
			},
			want: true,
		},
		{
			source: &spb.PipelineSource{
				Ref:  "master",
				Path: "",
				Name: "pipeline.yml",
			},
			params: &pb.UpdateProjectPipelineRequest{
				ProjectPipelineSource: &pb.ProjectPipelineSource{
					Ref:      "master",
					Path:     ".erda/pipelines",
					FileName: "pipeline.yml",
				},
			},
			want: false,
		},
		{
			source: &spb.PipelineSource{
				Ref:  "master",
				Path: "",
				Name: "pipeline.yml",
			},
			params: &pb.UpdateProjectPipelineRequest{
				ProjectPipelineSource: &pb.ProjectPipelineSource{
					Ref:      "develop",
					Path:     "",
					FileName: "pipeline.yml",
				},
			},
			want: false,
		},
		{
			source: &spb.PipelineSource{
				Ref:  "master",
				Path: "",
				Name: "pipeline.yml",
			},
			params: &pb.UpdateProjectPipelineRequest{
				ProjectPipelineSource: &pb.ProjectPipelineSource{
					Ref:      "master",
					Path:     "",
					FileName: "ci.yml",
				},
			},
			want: false,
		},
	}

	for _, v := range tt {
		assert.Equal(t, v.want, isSameSourceInApp(v.source, v.params))
	}

}

func Test_getRemotes(t *testing.T) {
	type args struct {
		appNames    []string
		orgName     string
		projectName string
	}
	tests := []struct {
		name string
		args args
		want []string
	}{
		{
			name: "test remote",
			args: args{
				appNames:    []string{"erda-release", "dice"},
				orgName:     "org",
				projectName: "erda",
			},
			want: []string{"org/erda/erda-release", "org/erda/dice"},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := getRemotes(tt.args.appNames, tt.args.orgName, tt.args.projectName); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("getRemotes() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestProjectPipelineService_fetchRemotePipeline(t *testing.T) {
	type fields struct {
		logger logs.Logger
	}
	type args struct {
		source *spb.PipelineSource
		orgID  string
		userID string
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    string
		wantErr bool
	}{
		{
			name:   "test fetch fetchRemotePipeline",
			fields: fields{},
			args: args{
				source: &spb.PipelineSource{
					Remote: "org/erda/erda-release",
				},
				orgID:  "",
				userID: "",
			},
			want: `version: "1.1"
stages:
  - stage:
      - git-checkout:
          alias: git-checkout
          description: 代码仓库克隆`,
			wantErr: false,
		},
	}

	var bdl *bundle.Bundle
	pm := monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetGittarBlobNode",
		func(bdl *bundle.Bundle, repo, orgID, userID string) (string, error) {
			return `version: "1.1"
stages:
  - stage:
      - git-checkout:
          alias: git-checkout
          description: 代码仓库克隆`, nil
		})
	defer pm.Unpatch()

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p := &ProjectPipelineService{
				bundle: bdl,
			}
			got, err := p.fetchRemotePipeline(tt.args.source, tt.args.orgID, tt.args.userID)
			if (err != nil) != tt.wantErr {
				t.Errorf("fetchRemotePipeline() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if got != tt.want {
				t.Errorf("fetchRemotePipeline() got = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_getNameByRemote(t *testing.T) {
	type args struct {
		remote string
	}
	tests := []struct {
		name string
		args args
		want RemoteName
	}{
		{
			name: "test getNameByRemote",
			args: args{remote: "org/erda/erda-release"},
			want: RemoteName{
				OrgName:     "org",
				ProjectName: "erda",
				AppName:     "erda-release",
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := getNameByRemote(tt.args.remote); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("getNameByRemote() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestProjectPipelineService_makeLocationByAppID(t *testing.T) {
	type fields struct {
		bundle *bundle.Bundle
	}
	type args struct {
		appID uint64
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    string
		wantErr bool
	}{
		{
			name:    "test makeLocationByAppID",
			fields:  fields{},
			args:    args{appID: 1},
			want:    "cicd/org/erda",
			wantErr: false,
		},
	}

	var bdl *bundle.Bundle
	pm := monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetApp",
		func(bdl *bundle.Bundle, appID uint64) (*apistructs.ApplicationDTO, error) {
			return &apistructs.ApplicationDTO{
				ID:          1,
				Name:        "erda-release",
				OrgName:     "org",
				ProjectName: "erda",
			}, nil
		})
	defer pm.Unpatch()

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p := &ProjectPipelineService{
				bundle: bdl,
			}
			got, err := p.makeLocationByAppID(tt.args.appID)
			if (err != nil) != tt.wantErr {
				t.Errorf("makeLocationByAppID() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if got != tt.want {
				t.Errorf("makeLocationByAppID() got = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestProjectPipelineService_makeLocationByProjectID(t *testing.T) {
	type fields struct {
		bundle *bundle.Bundle
	}
	type args struct {
		projectID uint64
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    string
		wantErr bool
	}{
		{
			name:    "test makeLocationByProjectID",
			fields:  fields{},
			args:    args{},
			want:    "cicd/org/erda",
			wantErr: false,
		},
	}

	var bdl *bundle.Bundle
	pm := monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetProject",
		func(bdl *bundle.Bundle, id uint64) (*apistructs.ProjectDTO, error) {
			return &apistructs.ProjectDTO{
				ID:   1,
				Name: "erda",
			}, nil
		})
	defer pm.Unpatch()

	pm2 := monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetOrg",
		func(bdl *bundle.Bundle, id interface{}) (*apistructs.OrgDTO, error) {
			return &apistructs.OrgDTO{
				ID:   1,
				Name: "org",
			}, nil
		})
	defer pm2.Unpatch()

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p := &ProjectPipelineService{
				bundle: bdl,
			}
			got, err := p.makeLocationByProjectID(tt.args.projectID)
			if (err != nil) != tt.wantErr {
				t.Errorf("makeLocationByProjectID() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if got != tt.want {
				t.Errorf("makeLocationByProjectID() got = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestProjectPipelineService_checkDataPermission(t *testing.T) {
	type fields struct {
		logger logs.Logger
	}
	type args struct {
		project *apistructs.ProjectDTO
		org     *apistructs.OrgDTO
		source  *spb.PipelineSource
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		wantErr bool
	}{
		{
			name:   "test has permission",
			fields: fields{},
			args: args{
				project: &apistructs.ProjectDTO{Name: "erda"},
				org:     &apistructs.OrgDTO{Name: "org"},
				source:  &spb.PipelineSource{Remote: "org/erda/erda-release"},
			},
			wantErr: false,
		},
		{
			name:   "test no permission",
			fields: fields{},
			args: args{
				project: &apistructs.ProjectDTO{Name: "dice"},
				org:     &apistructs.OrgDTO{Name: "org"},
				source:  &spb.PipelineSource{Remote: "org/erda/erda-release"},
			},
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p := &ProjectPipelineService{
				logger: tt.fields.logger,
			}
			if err := p.checkDataPermission(tt.args.project, tt.args.org, tt.args.source); (err != nil) != tt.wantErr {
				t.Errorf("checkDataPermission() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}

func Test_makePipelineName(t *testing.T) {
	type args struct {
		params      *pb.CreateProjectPipelineRequest
		pipelineYml string
	}
	tests := []struct {
		name string
		args args
		want string
	}{
		{
			name: "test with params' name",
			args: args{
				params: &pb.CreateProjectPipelineRequest{
					Name:     "ci-deploy",
					FileName: "ci-deploy.yml",
				},
				pipelineYml: `version: "1.1"
name: ci-deploy-dev
stages:
  - stage:
      - git-checkout:
          alias: git-checkout
          version: "1.0"
          params:
            branch: ((gittar.branch))
            depth: 1
            password: ((gittar.password))
            uri: ((gittar.repo))
            username: ((gittar.username))
          timeout: 3600`,
			},
			want: "ci-deploy",
		},
		{
			name: "test with params' name",
			args: args{
				params: &pb.CreateProjectPipelineRequest{
					Name:     "",
					FileName: "ci-deploy.yml",
				},
				pipelineYml: `version: "1.1"
name: ci-deploy-dev
stages:
  - stage:
      - git-checkout:
          alias: git-checkout
          version: "1.0"
          params:
            branch: ((gittar.branch))
            depth: 1
            password: ((gittar.password))
            uri: ((gittar.repo))
            username: ((gittar.username))
          timeout: 3600`,
			},
			want: "ci-deploy-dev",
		},
		{
			name: "test with params' name",
			args: args{
				params: &pb.CreateProjectPipelineRequest{
					Name:     "",
					FileName: "ci-deploy.yml",
				},
				pipelineYml: `version: "1.1"
stages:
  - stage:
      - git-checkout:
          alias: git-checkout
          version: "1.0"
          params:
            branch: ((gittar.branch))
            depth: 1
            password: ((gittar.password))
            uri: ((gittar.repo))
            username: ((gittar.username))
          timeout: 3600`,
			},
			want: "ci-deploy.yml",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := makePipelineName(tt.args.params, tt.args.pipelineYml); got != tt.want {
				t.Errorf("makePipelineName() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_getBranchFromRef(t *testing.T) {
	type args struct {
		ref string
	}
	tests := []struct {
		name string
		args args
		want string
	}{
		{
			name: "test with branch",
			args: args{
				ref: "refs/heads/master",
			},
			want: "master",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := getBranchFromRef(tt.args.ref); got != tt.want {
				t.Errorf("getBranchFromRef() = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestProjectPipelineService_listPipelineYmlByApp(t *testing.T) {
	type fields struct {
		logger logs.Logger
		bundle *bundle.Bundle
	}
	type args struct {
		app    *apistructs.ApplicationDTO
		branch string
		userID string
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    []*pb.PipelineYmlList
		wantErr bool
	}{
		{
			name:   "test listPipelineYmlByApp",
			fields: fields{},
			args:   args{},
			want: []*pb.PipelineYmlList{
				{
					YmlName: "pipeline.yml",
					YmlPath: "",
				},
				{
					YmlName: "pipeline.yml",
					YmlPath: ".dice/pipelines",
				},
				{
					YmlName: "pipeline.yml",
					YmlPath: ".erda/pipelines",
				},
			},
			wantErr: false,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			s := &ProjectPipelineService{
				logger: tt.fields.logger,
				bundle: tt.fields.bundle,
			}

			monkey.PatchInstanceMethod(reflect.TypeOf(s), "GetPipelineYml", func(s *ProjectPipelineService, app *apistructs.ApplicationDTO, userID string, branch string, findPath string) ([]*pb.PipelineYmlList, error) {
				if findPath == apistructs.DefaultPipelinePath {
					return []*pb.PipelineYmlList{{
						YmlName: "pipeline.yml",
						YmlPath: "",
					}}, nil
				} else if findPath == apistructs.DicePipelinePath {
					return []*pb.PipelineYmlList{{
						YmlName: "pipeline.yml",
						YmlPath: ".dice/pipelines",
					}}, nil
				} else if findPath == apistructs.ErdaPipelinePath {
					return []*pb.PipelineYmlList{{
						YmlName: "pipeline.yml",
						YmlPath: ".erda/pipelines",
					}}, nil
				}
				return nil, nil
			})
			defer monkey.UnpatchAll()
			got, err := s.ListPipelineYmlByApp(tt.args.app, tt.args.branch, tt.args.userID)
			if (err != nil) != tt.wantErr {
				t.Errorf("listPipelineYmlByApp() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			sort.Slice(got, func(i, j int) bool {
				return got[i].YmlPath < got[j].YmlPath
			})
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("listPipelineYmlByApp() got = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestProjectPipelineService_BatchCreateByGittarPushHook(t *testing.T) {
	type fields struct {
		logger logs.Logger
	}
	type args struct {
		ctx    context.Context
		params *pb.GittarPushPayloadEvent
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    *pb.BatchCreateProjectPipelineResponse
		wantErr bool
	}{
		{
			name:   "test BatchCreateByGittarPushHook",
			fields: fields{},
			args: args{
				ctx: context.Background(),
				params: &pb.GittarPushPayloadEvent{
					Event:         "git_push",
					Action:        "git_push",
					OrgID:         "1",
					ProjectID:     "1",
					ApplicationID: "1",
					Content: &pb.Content{
						Ref:    "refs/heads/master",
						After:  "0000000000000000000000000000000000000abc",
						Before: "0000000000000000000000000000000000000000",
						Pusher: &pb.Pusher{
							ID:       "10001",
							Name:     "erda",
							NickName: "erda",
						},
					},
				},
			},
			want:    &pb.BatchCreateProjectPipelineResponse{},
			wantErr: false,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			var bdl *bundle.Bundle

			p := &ProjectPipelineService{
				logger: tt.fields.logger,
				bundle: bdl,
			}

			monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetApp", func(bdl *bundle.Bundle, id uint64) (*apistructs.ApplicationDTO, error) {
				return &apistructs.ApplicationDTO{
					ID:   1,
					Name: "erda",
				}, nil
			})
			defer monkey.UnpatchAll()
			monkey.PatchInstanceMethod(reflect.TypeOf(p), "CheckBranchRule", func(p *ProjectPipelineService, branch string, projectID int64) (bool, error) {
				return true, nil
			})

			monkey.PatchInstanceMethod(reflect.TypeOf(p), "ListPipelineYmlByApp", func(p *ProjectPipelineService, app *apistructs.ApplicationDTO, branch, userID string) ([]*pb.PipelineYmlList, error) {
				return []*pb.PipelineYmlList{{
					YmlName: "pipeline.yml",
					YmlPath: "",
				}}, nil
			})

			monkey.PatchInstanceMethod(reflect.TypeOf(p), "CreateOne", func(p *ProjectPipelineService, ctx context.Context, params *pb.CreateProjectPipelineRequest) (*pb.ProjectPipeline, error) {
				return &pb.ProjectPipeline{
					ID:   "1",
					Name: "pipeline.yml",
				}, nil
			})
			got, err := p.BatchCreateByGittarPushHook(tt.args.ctx, tt.args.params)
			if (err != nil) != tt.wantErr {
				t.Errorf("BatchCreateByGittarPushHook() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("BatchCreateByGittarPushHook() got = %v, want %v", got, tt.want)
			}
		})
	}
}

func TestProjectPipelineService_GetRemotesByAppID(t *testing.T) {
	var bdl *bundle.Bundle
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetApp", func(bdl *bundle.Bundle, id uint64) (*apistructs.ApplicationDTO, error) {
		if id == 1 {
			return &apistructs.ApplicationDTO{
				ID:   1,
				Name: "erda-ui",
			}, nil
		}
		return nil, fmt.Errorf("the app is not found")
	})

	type fields struct {
		bundle *bundle.Bundle
	}
	type args struct {
		appID       uint64
		orgName     string
		projectName string
	}
	tests := []struct {
		name    string
		fields  fields
		args    args
		want    []string
		wantErr bool
	}{
		{
			name: "test with nil",
			fields: fields{
				bundle: bdl,
			},
			args: args{
				appID:       0,
				orgName:     "terminus",
				projectName: "erda",
			},
			want:    nil,
			wantErr: false,
		},
		{
			name: "test with err",
			fields: fields{
				bundle: bdl,
			},
			args: args{
				appID:       2,
				orgName:     "terminus",
				projectName: "erda",
			},
			want:    nil,
			wantErr: true,
		},
		{
			name: "test with correct",
			fields: fields{
				bundle: bdl,
			},
			args: args{
				appID:       1,
				orgName:     "terminus",
				projectName: "erda",
			},
			want:    []string{"terminus/erda/erda-ui"},
			wantErr: false,
		},
	}
	defer monkey.UnpatchAll()
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p := &ProjectPipelineService{
				bundle: tt.fields.bundle,
			}
			got, err := p.GetRemotesByAppID(tt.args.appID, tt.args.orgName, tt.args.projectName)
			if (err != nil) != tt.wantErr {
				t.Errorf("GetRemotesByAppID() error = %v, wantErr %v", err, tt.wantErr)
				return
			}
			if !reflect.DeepEqual(got, tt.want) {
				t.Errorf("GetRemotesByAppID() got = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_getApplicationNameFromDefinitionRemote(t *testing.T) {
	type args struct {
		remote string
	}
	tests := []struct {
		name string
		args args
		want string
	}{
		{
			name: "test with remote",
			args: args{remote: "terminus/erda/ui"},
			want: "ui",
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := getApplicationNameFromDefinitionRemote(tt.args.remote); got != tt.want {
				t.Errorf("getApplicationNameFromDefinitionRemote() = %v, want %v", got, tt.want)
			}
		})
	}
}

func Test_makeListPipelineExecHistoryResponse(t *testing.T) {
	type args struct {
		data *apistructs.PipelinePageListData
	}

	date := time.Date(1970, 1, 1, 0, 0, 0, 0, time.UTC)
	tests := []struct {
		name string
		args args
		want *pb.ListPipelineExecHistoryResponse
	}{
		{
			name: "test with make ListPipelineExecHistory",
			args: args{
				data: &apistructs.PipelinePageListData{
					Pipelines: []apistructs.PagePipeline{
						{
							ID:          10001,
							Status:      "Success",
							CostTimeSec: 100,
							TimeBegin:   &date,
							DefinitionPageInfo: &apistructs.DefinitionPageInfo{
								Name:         "deploy",
								Creator:      "1",
								Executor:     "1",
								SourceRemote: "terminus/erda/ui",
								SourceRef:    "master",
							},
							Extra: apistructs.PipelineExtra{
								RunUser: &apistructs.PipelineUser{
									ID: 1,
								},
							},
						},
					},
					Total:           1,
					CurrentPageSize: 1,
				},
			},
			want: &pb.ListPipelineExecHistoryResponse{
				Total:           1,
				CurrentPageSize: 1,
				ExecHistories: []*pb.PipelineExecHistory{
					{
						PipelineName:   "deploy",
						PipelineStatus: "Success",
						CostTimeSec:    100,
						AppName:        "ui",
						Branch:         "master",
						Executor:       "1",
						TimeBegin:      nil,
						PipelineID:     10001,
					},
				},
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := makeListPipelineExecHistoryResponse(tt.args.data); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("makeListPipelineExecHistoryResponse() = %v, want %v", got, tt.want)
			}
		})
	}
}

type MockPipelineCron struct {
}

func (m MockPipelineCron) CronCreate(ctx context.Context, request *cronpb.CronCreateRequest) (*cronpb.CronCreateResponse, error) {
	panic("implement me")
}

func (m MockPipelineCron) CronPaging(ctx context.Context, request *cronpb.CronPagingRequest) (*cronpb.CronPagingResponse, error) {
	if strutil.InSlice("pipeline1.yml", request.YmlNames) {
		return &cronpb.CronPagingResponse{
			Total: 1,
			Data: []*ppb.Cron{{
				ID:                   1,
				PipelineDefinitionID: "1",
			}},
		}, nil
	} else if strutil.InSlice("pipeline2.yml", request.YmlNames) {
		return &cronpb.CronPagingResponse{
			Total: 0,
			Data:  nil,
		}, nil
	}

	return nil, fmt.Errorf("failed")
}

func (m MockPipelineCron) CronStart(ctx context.Context, request *cronpb.CronStartRequest) (*cronpb.CronStartResponse, error) {
	panic("implement me")
}

func (m MockPipelineCron) CronStop(ctx context.Context, request *cronpb.CronStopRequest) (*cronpb.CronStopResponse, error) {
	panic("implement me")
}

func (m MockPipelineCron) CronDelete(ctx context.Context, request *cronpb.CronDeleteRequest) (*cronpb.CronDeleteResponse, error) {
	panic("implement me")
}

func (m MockPipelineCron) CronGet(ctx context.Context, request *cronpb.CronGetRequest) (*cronpb.CronGetResponse, error) {
	panic("implement me")
}

func (m MockPipelineCron) CronUpdate(ctx context.Context, request *cronpb.CronUpdateRequest) (*cronpb.CronUpdateResponse, error) {
	panic("implement me")
}

func TestProjectPipelineService_createCronIfNotExist(t *testing.T) {
	type fields struct {
		bundle       *bundle.Bundle
		PipelineCron cronpb.CronServiceServer
	}
	type args struct {
		definition          *dpb.PipelineDefinition
		projectPipelineType ProjectSourceType
	}

	mockPipelineCron := &MockPipelineCron{}

	bdl := &bundle.Bundle{}
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "CreatePipeline", func(dbl *bundle.Bundle, req interface{}) (*apistructs.PipelineDTO, error) {
		return nil, nil
	})
	defer monkey.UnpatchAll()

	sourceType1 := NewProjectSourceType(deftype.ErdaProjectPipelineType.String())
	sourceType1.(*ErdaProjectSourceType).PipelineCreateRequestV2 = `
{
    "createRequest": {
        "pipelineYmlName": "pipeline1.yml",
        "pipelineSource": "dice"
    },
    "runParams": null
}`
	sourceType2 := NewProjectSourceType(deftype.ErdaProjectPipelineType.String())
	sourceType2.(*ErdaProjectSourceType).PipelineCreateRequestV2 = `
{
    "createRequest": {
        "pipelineYmlName": "pipeline2.yml",
        "pipelineSource": "dice"
    },
    "runParams": null
}`
	sourceType3 := NewProjectSourceType(deftype.ErdaProjectPipelineType.String())
	sourceType3.(*ErdaProjectSourceType).PipelineCreateRequestV2 = `
{
    "createRequest": {
        "pipelineYmlName": "pipeline3.yml",
        "pipelineSource": "dice"
    },
    "runParams": null
}`
	tests := []struct {
		name    string
		fields  fields
		args    args
		wantErr bool
	}{
		{
			name: "test with exist",
			fields: fields{
				bundle:       bdl,
				PipelineCron: mockPipelineCron,
			},
			args: args{
				definition: &dpb.PipelineDefinition{
					ID: "1",
				},
				projectPipelineType: sourceType1,
			},
			wantErr: false,
		},
		{
			name: "test with not exist",
			fields: fields{
				bundle:       bdl,
				PipelineCron: mockPipelineCron,
			},
			args: args{
				definition: &dpb.PipelineDefinition{
					ID: "1",
				},
				projectPipelineType: sourceType2,
			},
			wantErr: false,
		},
		{
			name: "test with error",
			fields: fields{
				bundle:       bdl,
				PipelineCron: mockPipelineCron,
			},
			args: args{
				definition: &dpb.PipelineDefinition{
					ID: "1",
				},
				projectPipelineType: sourceType3,
			},
			wantErr: true,
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			p := &ProjectPipelineService{
				bundle:       tt.fields.bundle,
				PipelineCron: tt.fields.PipelineCron,
			}
			if err := p.createCronIfNotExist(tt.args.definition, tt.args.projectPipelineType); (err != nil) != tt.wantErr {
				t.Errorf("createCronIfNotExist() error = %v, wantErr %v", err, tt.wantErr)
			}
		})
	}
}

func Test_makePipelinePageListRequest(t *testing.T) {
	type args struct {
		params    *pb.ListPipelineExecHistoryRequest
		jsonValue []byte
	}
	tests := []struct {
		name string
		args args
		want apistructs.PipelinePageListRequest
	}{
		{
			name: "test with make",
			args: args{
				params: &pb.ListPipelineExecHistoryRequest{
					Name:           "pipeline.yml",
					Executors:      nil,
					AppNames:       []string{"erda"},
					Statuses:       []string{"Success"},
					PageNo:         1,
					PageSize:       10,
					StartTimeBegin: nil,
					StartTimeEnd:   nil,
					DescCols:       nil,
					AscCols:        nil,
					ProjectID:      0,
					Branches:       []string{"master"},
				},
				jsonValue: nil,
			},
			want: apistructs.PipelinePageListRequest{
				CommaBranches:                       "",
				CommaSources:                        "",
				CommaYmlNames:                       "",
				CommaStatuses:                       "",
				AppID:                               uint64(0),
				Branches:                            nil,
				Sources:                             nil,
				AllSources:                          true,
				YmlNames:                            nil,
				Statuses:                            []string{"Success"},
				NotStatuses:                         nil,
				TriggerModes:                        nil,
				ClusterNames:                        nil,
				IncludeSnippet:                      false,
				StartTimeBegin:                      time.Time{},
				StartTimeBeginTimestamp:             int64(0),
				StartTimeBeginCST:                   "",
				EndTimeBegin:                        time.Time{},
				EndTimeBeginTimestamp:               int64(0),
				EndTimeBeginCST:                     "",
				StartTimeCreated:                    time.Time{},
				StartTimeCreatedTimestamp:           int64(0),
				EndTimeCreated:                      time.Time{},
				EndTimeCreatedTimestamp:             int64(0),
				MustMatchLabelsJSON:                 "",
				MustMatchLabelsQueryParams:          []string{"branch=master"},
				MustMatchLabels:                     nil,
				AnyMatchLabelsJSON:                  "",
				AnyMatchLabelsQueryParams:           nil,
				AnyMatchLabels:                      nil,
				PageNum:                             1,
				PageNo:                              0,
				PageSize:                            10,
				LargePageSize:                       false,
				CountOnly:                           false,
				SelectCols:                          nil,
				AscCols:                             nil,
				DescCols:                            nil,
				PipelineDefinitionRequest:           nil,
				PipelineDefinitionRequestJSONBase64: "",
			},
		},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := makePipelinePageListRequest(tt.args.params, tt.args.jsonValue); !reflect.DeepEqual(got, tt.want) {
				t.Errorf("makePipelinePageListRequest() = %v, want %v", got, tt.want)
			}
		})
	}
}
