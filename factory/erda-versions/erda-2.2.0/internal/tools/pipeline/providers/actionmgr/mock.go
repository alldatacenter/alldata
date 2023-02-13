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

package actionmgr

import (
	"context"

	"github.com/erda-project/erda-proto-go/core/pipeline/action/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/parser/diceyml"
	"github.com/erda-project/erda/pkg/parser/pipelineyml"
)

type MockActionMgr struct{}

func (m *MockActionMgr) List(ctx context.Context, request *pb.PipelineActionListRequest) (*pb.PipelineActionListResponse, error) {
	return nil, nil
}
func (m *MockActionMgr) Save(ctx context.Context, request *pb.PipelineActionSaveRequest) (*pb.PipelineActionSaveResponse, error) {
	return nil, nil
}
func (m *MockActionMgr) Delete(ctx context.Context, request *pb.PipelineActionDeleteRequest) (*pb.PipelineActionDeleteResponse, error) {
	return nil, nil
}
func (m *MockActionMgr) SearchActions(items []string, locations []string, ops ...OpOption) (map[string]*diceyml.Job, map[string]*apistructs.ActionSpec, error) {
	return nil, nil, nil
}
func (m *MockActionMgr) MakeActionTypeVersion(action *pipelineyml.Action) string {
	r := action.Type.String()
	if action.Version != "" {
		r = r + "@" + action.Version
	}
	return r
}
func (m *MockActionMgr) MakeActionLocationsBySource(source apistructs.PipelineSource) []string {
	var locations []string
	switch source {
	case apistructs.PipelineSourceCDPDev, apistructs.PipelineSourceCDPTest, apistructs.PipelineSourceCDPStaging, apistructs.PipelineSourceCDPProd, apistructs.PipelineSourceBigData:
		locations = append(locations, apistructs.PipelineTypeFDP.String()+"/")
	case apistructs.PipelineSourceDice, apistructs.PipelineSourceProject, apistructs.PipelineSourceProjectLocal, apistructs.PipelineSourceOps, apistructs.PipelineSourceQA:
		locations = append(locations, apistructs.PipelineTypeCICD.String()+"/")
	default:
		locations = append(locations, apistructs.PipelineTypeDefault.String()+"/")
	}
	return locations
}
