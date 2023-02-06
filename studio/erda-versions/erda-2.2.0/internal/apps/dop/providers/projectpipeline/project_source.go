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
	"encoding/json"
	"path/filepath"

	spb "github.com/erda-project/erda-proto-go/core/pipeline/source/pb"
	"github.com/erda-project/erda-proto-go/dop/projectpipeline/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/providers/projectpipeline/deftype"
	"github.com/erda-project/erda/pkg/common/apis"
)

type ProjectSourceType interface {
	GenerateReq(ctx context.Context, p *ProjectPipelineService, params *pb.CreateProjectPipelineRequest) (*spb.PipelineSourceCreateRequest, error)
	GetPipelineCreateRequestV2() string
}

type ErdaProjectSourceType struct {
	PipelineCreateRequestV2 string `json:"pipelineCreateRequestV2"`
}

type GithubProjectSourceType struct {
	PipelineCreateRequestV2 string `json:"pipelineCreateRequestV2"`
}

func NewProjectSourceType(t string) ProjectSourceType {
	if t == deftype.ErdaProjectPipelineType.String() {
		return new(ErdaProjectSourceType)
	}

	if t == deftype.GithubProjectPipelineType.String() {
		return new(GithubProjectSourceType)
	}

	return nil
}

func (s *ErdaProjectSourceType) GenerateReq(ctx context.Context, p *ProjectPipelineService, params *pb.CreateProjectPipelineRequest) (*spb.PipelineSourceCreateRequest, error) {
	app, err := p.bundle.GetApp(params.AppID)
	if err != nil {
		return nil, err
	}
	createReqV2, err := p.pipelineSvc.ConvertPipelineToV2(&apistructs.PipelineCreateRequest{
		PipelineYmlName:    filepath.Join(params.Path, params.FileName),
		AppID:              params.AppID,
		Branch:             params.Ref,
		PipelineYmlContent: "",
		UserID:             apis.GetUserID(ctx),
	})
	if err != nil {
		return nil, err
	}
	var extra apistructs.PipelineDefinitionExtraValue
	extra.CreateRequest = createReqV2
	b, err := json.Marshal(extra)
	if err != nil {
		return nil, err
	}
	s.PipelineCreateRequestV2 = string(b)
	return &spb.PipelineSourceCreateRequest{
		SourceType:  params.SourceType,
		Remote:      makeRemote(app),
		Ref:         params.Ref,
		Path:        params.Path,
		Name:        params.FileName,
		PipelineYml: createReqV2.PipelineYml,
	}, nil
}

func (s *ErdaProjectSourceType) GetPipelineCreateRequestV2() string {
	return s.PipelineCreateRequestV2
}

func makeRemote(app *apistructs.ApplicationDTO) string {
	return filepath.Join(app.OrgName, app.ProjectName, app.Name)
}

func (s *GithubProjectSourceType) GenerateReq(ctx context.Context, p *ProjectPipelineService, params *pb.CreateProjectPipelineRequest) (*spb.PipelineSourceCreateRequest, error) {
	return nil, nil
}

func (s *GithubProjectSourceType) GetPipelineCreateRequestV2() string {
	return s.PipelineCreateRequestV2
}
