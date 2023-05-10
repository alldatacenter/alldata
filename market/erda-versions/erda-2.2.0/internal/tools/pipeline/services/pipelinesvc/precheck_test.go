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
	"context"
	"fmt"
	"reflect"
	"testing"

	"bou.ke/monkey"
	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda-proto-go/core/pipeline/action/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/dbclient"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/actionmgr"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/parser/diceyml"
	"github.com/erda-project/erda/pkg/parser/pipelineyml"
)

type mockActionAgent struct{}

func (m *mockActionAgent) List(context.Context, *pb.PipelineActionListRequest) (*pb.PipelineActionListResponse, error) {
	return nil, nil
}

func (m *mockActionAgent) Save(context.Context, *pb.PipelineActionSaveRequest) (*pb.PipelineActionSaveResponse, error) {
	return nil, nil
}

func (m *mockActionAgent) Delete(context.Context, *pb.PipelineActionDeleteRequest) (*pb.PipelineActionDeleteResponse, error) {
	return nil, nil
}

func (m *mockActionAgent) SearchActions(items []string, locations []string, ops ...actionmgr.OpOption) (map[string]*diceyml.Job, map[string]*apistructs.ActionSpec, error) {
	return nil, nil, nil
}

type mockSecret struct{}

func (m *mockSecret) FetchSecrets(ctx context.Context, p *spec.Pipeline) (secrets, cmsDiceFiles map[string]string, holdOnKeys, encryptSecretKeys []string, err error) {
	return nil, nil, nil, nil, nil
}

func (m *mockSecret) FetchPlatformSecrets(ctx context.Context, p *spec.Pipeline, ignoreKeys []string) (map[string]string, error) {
	return nil, nil
}

func (m *mockActionAgent) MakeActionTypeVersion(action *pipelineyml.Action) string {
	return fmt.Sprintf("%s@%s", action.Alias, action.Version)
}

func (m *mockActionAgent) MakeActionLocationsBySource(source apistructs.PipelineSource) []string {
	return nil
}

func TestPreCheck(t *testing.T) {
	pWithDisableTasks := &spec.Pipeline{
		PipelineExtra: spec.PipelineExtra{
			PipelineYml: `version: "1.1"
stages:
  - stage:
      - git-checkout:
          alias: git-checkout
          description: 代码仓库克隆
          version: "1.0"
          disable: true
          params:
            branch: ((gittar.branch))
            depth: 1
            password: ((gittar.password))
            uri: ((gittar.repo))
            username: ((gittar.username))
          timeout: 3600
  - stage:
      - golang:
          alias: go-demo
          description: golang action
          disable: true
          params:
            command: go build -o web-server main.go
            context: ${git-checkout}
            service: ${{ outputs.checkout.data }}
            target: web-server`,
		},
	}
	stages := []spec.PipelineStage{
		{
			ID: 1,
		},
		{
			ID: 2,
		},
	}
	dbClient := &dbclient.Client{}
	pm := monkey.PatchInstanceMethod(reflect.TypeOf(dbClient), "UpdatePipelineShowMessage", func(_ *dbclient.Client, pipelineID uint64, showMessage apistructs.ShowMessage, ops ...dbclient.SessionOption) error {
		return nil
	})
	defer pm.Unpatch()
	svc := &PipelineSvc{actionMgr: &mockActionAgent{}, secret: &mockSecret{}, dbClient: dbClient}
	err := svc.PreCheck(pWithDisableTasks, stages, "1", false)
	assert.NoError(t, err)
}
