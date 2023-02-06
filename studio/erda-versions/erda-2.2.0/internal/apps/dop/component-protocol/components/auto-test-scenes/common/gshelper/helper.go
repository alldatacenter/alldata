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

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/pkg/encoding/jsonparse"
)

const (
	SceneConfigKey    string = "SceneConfigKey"
	SceneSetConfigKey string = "SceneSetConfigKey"
)

type GSHelper struct {
	gs *cptype.GlobalStateData
}

func NewGSHelper(gs *cptype.GlobalStateData) *GSHelper {
	return &GSHelper{gs: gs}
}

func assign(src, dst interface{}) error {
	if src == nil || dst == nil {
		return nil
	}

	return mapstructure.Decode(src, dst)
}

func (h *GSHelper) SetGlobalSelectedSetID(id uint64) {
	if h.gs == nil {
		return
	}
	(*h.gs)["GlobalSelectedSetID"] = id
}

func (h *GSHelper) GetGlobalSelectedSetID() uint64 {
	if h.gs == nil {
		return 0
	}
	v, ok := (*h.gs)["GlobalSelectedSetID"]
	if !ok {
		return 0
	}
	if _, ok = v.(uint64); ok {
		return v.(uint64)
	}
	return uint64(v.(float64))
}

func (h *GSHelper) SetGlobalActiveConfig(key string) {
	if h.gs == nil {
		return
	}
	(*h.gs)["GlobalActiveConfig"] = key
}

func (h *GSHelper) GetGlobalActiveConfig() string {
	if h.gs == nil {
		return ""
	}
	if v, ok := (*h.gs)["GlobalActiveConfig"].(string); ok {
		return v
	}
	return SceneSetConfigKey
}

func (h *GSHelper) SetFileTreeSceneID(id uint64) {
	if h.gs == nil {
		return
	}
	(*h.gs)["FileTreeSceneID"] = id
}

func (h *GSHelper) GetFileTreeSceneID() uint64 {
	if h.gs == nil {
		return 0
	}
	v, ok := (*h.gs)["FileTreeSceneID"]
	if !ok {
		return 0
	}
	if _, ok = v.(uint64); ok {
		return v.(uint64)
	}
	return uint64(v.(float64))
}

func (h *GSHelper) SetFileDetailActiveKey(key apistructs.ActiveKey) {
	if h.gs == nil {
		return
	}
	(*h.gs)["FileDetailActiveKey"] = key
}

func (h *GSHelper) GetFileDetailActiveKey() apistructs.ActiveKey {
	if h.gs == nil {
		return ""
	}
	if v, ok := (*h.gs)["FileDetailActiveKey"].(string); ok {
		return apistructs.ActiveKey(v)
	}
	if v, ok := (*h.gs)["FileDetailActiveKey"].(apistructs.ActiveKey); ok {
		return v
	}
	return ""
}

func (h *GSHelper) SetFileDetailIsChangeScene(isChangeScene bool) {
	if h.gs == nil {
		return
	}
	(*h.gs)["FileDetailIsChangeScene"] = isChangeScene
}

func (h *GSHelper) GetFileDetailIsChangeScene() bool {
	if h.gs == nil {
		return false
	}
	if v, ok := (*h.gs)["FileDetailIsChangeScene"].(bool); ok {
		return v
	}
	return false
}

func (h *GSHelper) SetFileTreeSceneSetKey(id uint64) {
	if h.gs == nil {
		return
	}
	(*h.gs)["FileTreeSceneSetKey"] = id
}

func (h *GSHelper) GetFileTreeSceneSetKey() uint64 {
	if h.gs == nil {
		return 0
	}
	v, ok := (*h.gs)["FileTreeSceneSetKey"]
	if !ok {
		return 0
	}
	if _, ok = v.(uint64); ok {
		return v.(uint64)
	}
	return uint64(v.(float64))
}

func (h *GSHelper) SetExecuteHistoryTablePipelineID(id uint64) {
	if h.gs == nil {
		return
	}
	(*h.gs)["ExecuteHistoryTablePipelineID"] = id
}

func (h *GSHelper) GetExecuteTaskTablePipelineID() uint64 {
	if h.gs == nil {
		return 0
	}
	v, ok := (*h.gs)["ExecuteTaskTablePipelineID"]
	if !ok {
		return 0
	}
	if _, ok = v.(uint64); ok {
		return v.(uint64)
	}
	return uint64(v.(float64))
}

func (h *GSHelper) SetExecuteTaskTablePipelineID(id uint64) {
	if h.gs == nil {
		return
	}
	(*h.gs)["ExecuteTaskTablePipelineID"] = id
}

func (h *GSHelper) GetExecuteHistoryTablePipelineID() uint64 {
	if h.gs == nil {
		return 0
	}
	v, ok := (*h.gs)["ExecuteHistoryTablePipelineID"]
	if !ok {
		return 0
	}
	if _, ok = v.(uint64); ok {
		return v.(uint64)
	}
	return uint64(v.(float64))
}

func (h *GSHelper) SetExecuteTaskBreadcrumbPipelineID(id uint64) {
	if h.gs == nil {
		return
	}
	(*h.gs)["ExecuteTaskBreadcrumbPipelineID"] = id
}

func (h *GSHelper) GetExecuteTaskBreadcrumbPipelineID() uint64 {
	if h.gs == nil {
		return 0
	}
	v, ok := (*h.gs)["ExecuteTaskBreadcrumbPipelineID"]
	if !ok {
		return 0
	}
	if _, ok = v.(uint64); ok {
		return v.(uint64)
	}
	return uint64(v.(float64))
}

func (h *GSHelper) SetExecuteTaskBreadcrumbVisible(visible bool) {
	if h.gs == nil {
		return
	}
	(*h.gs)["ExecuteTaskBreadcrumbVisible"] = visible
}

func (h *GSHelper) GetExecuteTaskBreadcrumbVisible() bool {
	if h.gs == nil {
		return false
	}
	if v, ok := (*h.gs)["ExecuteTaskBreadcrumbVisible"].(bool); ok {
		return v
	}
	return false
}

func (h *GSHelper) SetExecuteButtonActiveKey(key apistructs.ActiveKey) {
	if h.gs == nil {
		return
	}
	(*h.gs)["ExecuteButtonActiveKey"] = key
}

func (h *GSHelper) GetExecuteButtonActiveKey() apistructs.ActiveKey {
	if h.gs == nil {
		return ""
	}
	if v, ok := (*h.gs)["ExecuteButtonActiveKey"].(string); ok {
		return apistructs.ActiveKey(v)
	}
	if v, ok := (*h.gs)["ExecuteButtonActiveKey"].(apistructs.ActiveKey); ok {
		return v
	}
	return ""
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
