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

package gshelper

import (
	"encoding/json"

	"github.com/mitchellh/mapstructure"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/pkg/encoding/jsonparse"
)

type GSHelper struct {
	gs *apistructs.GlobalStateData
}

func NewGSHelper(gs *apistructs.GlobalStateData) *GSHelper {
	return &GSHelper{gs: gs}
}

func assign(src, dst interface{}) error {
	if src == nil || dst == nil {
		return nil
	}

	return mapstructure.Decode(src, dst)
}

func (h *GSHelper) SetPipelineInfo(pipeline apistructs.PipelineDetailDTO) {
	if h.gs == nil {
		return
	}
	(*h.gs)["GlobalPipelineInfo"] = jsonparse.JsonOneLine(pipeline)
}

func (h *GSHelper) ClearPipelineInfo() {
	if h.gs == nil {
		return
	}
	delete(*h.gs, "GlobalPipelineInfo")
}

func (h *GSHelper) GetPipelineInfo() *apistructs.PipelineDetailDTO {
	if h.gs == nil {
		return nil
	}

	info := (*h.gs)["GlobalPipelineInfo"]
	if info == "" || info == nil {
		return nil
	}
	var v apistructs.PipelineDetailDTO
	err := json.Unmarshal([]byte(info.(string)), &v)
	if err != nil {
		return nil
	}

	return &v
}

func (h *GSHelper) GetPipelineInfoWithPipelineID(pipelineID uint64, bdl *bundle.Bundle) *apistructs.PipelineDetailDTO {
	value := h.GetPipelineInfo()
	if value != nil && value.ID == pipelineID {
		return value
	}
	rsp, err := bdl.GetPipeline(pipelineID)
	if err != nil {
		return nil
	}

	h.SetPipelineInfo(*rsp)
	return rsp
}
