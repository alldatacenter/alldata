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

package scenesStages

import (
	"context"
	"encoding/json"
	"fmt"

	"github.com/erda-project/erda-infra/providers/component-protocol/cptype"
	"github.com/erda-project/erda-infra/providers/component-protocol/utils/cputil"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/common/gshelper"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/components/auto-test-scenes/rightPage/fileDetail/fileConfig/scenesConfig/stages"
	"github.com/erda-project/erda/internal/apps/dop/component-protocol/types"
	autotestv2 "github.com/erda-project/erda/internal/apps/dop/services/autotest_v2"
)

type SceneStage struct {
	CommonStageForm

	sdk        *cptype.SDK
	atTestPlan *autotestv2.Service
	event      cptype.ComponentEvent
	gsHelper   *gshelper.GSHelper
}

type CommonStageForm struct {
	Version    string                 `json:"version,omitempty"`
	Name       string                 `json:"name,omitempty"`
	Type       string                 `json:"type,omitempty"`
	State      State                  `json:"state,omitempty"`
	Operations map[string]interface{} `json:"operations,omitempty"`
	Data       Data                   `json:"data,omitempty"`
	InParams   InParams               `json:"inParams,omitempty"`
	Props      map[string]interface{} `json:"props,omitempty"`
}

type Data struct {
	List []StageData `json:"value"`
	Type string      `json:"type"`
}

type StageData struct {
	Title      string                 `json:"title"`
	ID         uint64                 `json:"id"`
	GroupID    int                    `json:"groupId"`
	Operations map[string]interface{} `json:"operations"`
	Tags       []stages.Tag           `json:"tags"`
}

type InParams struct {
	SceneID    string `json:"sceneId__urlQuery"`
	SceneSetID uint64 `json:"sceneSetId__urlQuery"`
}

type DragParams struct {
	DragGroupKey int64 `json:"dragGroupKey"`
	DropGroupKey int64 `json:"dropGroupKey"`
	DragKey      int64 `json:"dragKey"`
	DropKey      int64 `json:"dropKey"`
	Position     int64 `json:"position"`
}

type State struct {
	Visible               bool       `json:"visible"`
	DragParams            DragParams `json:"dragParams"`
	SetID                 uint64     `json:"setID"`
	StepId                uint64     `json:"stepId"`
	ShowScenesSetDrawer   bool       `json:"showScenesSetDrawer"`
	ActionType            string     `json:"actionType"`
	SceneID               uint64     `json:"sceneId"`
	SceneSetKey           uint64     `json:"sceneSetKey"`
	PageNo                int        `json:"pageNo"`
	SelectedKeys          []string   `json:"selectedKeys"`
	IsClickScene          bool       `json:"isClickScene"`
	IsClickFolderTableRow bool       `json:"isClickFolderTableRow"`
	ClickFolderTableRowID uint64     `json:"clickFolderTableRowID"`
	IsAddParallel         bool       `json:"isAddParallel"`
}

type OperationBaseInfo struct {
	FillMeta    string `json:"fillMeta"`
	Key         string `json:"key"`
	Icon        string `json:"icon"`
	HoverTip    string `json:"hoverTip"`
	HoverShow   bool   `json:"hoverShow"`
	Text        string `json:"text"`
	Confirm     string `json:"confirm,omitempty"`
	Reload      bool   `json:"reload"`
	Disabled    bool   `json:"disabled"`
	DisabledTip string `json:"disabledTip"`
	Group       string `json:"group"`
}

type OpMetaData struct {
	Type   apistructs.StepAPIType   `json:"type"`   // 类型
	Method apistructs.StepAPIMethod `json:"method"` // method
	Value  string                   `json:"value"`  // 值
	Name   string                   `json:"name"`   // 名称
	ID     uint64                   `json:"id"`
}

type OpMetaInfo struct {
	ID   uint64                 `json:"id"`
	Data map[string]interface{} `json:"data"`
}

type OperationInfo struct {
	OperationBaseInfo
	Meta OpMetaInfo `json:"meta"`
}

func (s *SceneStage) initFromProtocol(ctx context.Context, c *cptype.Component, event cptype.ComponentEvent, gs *cptype.GlobalStateData) error {
	b, err := json.Marshal(c)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(b, s); err != nil {
		return err
	}

	// sdk
	s.sdk = cputil.SDK(ctx)
	s.event = event
	s.atTestPlan = ctx.Value(types.AutoTestPlanService).(*autotestv2.Service)
	s.gsHelper = gshelper.NewGSHelper(gs)
	// clear
	s.State.Visible = false
	s.State.ActionType = ""
	s.State.IsAddParallel = false
	s.State.IsClickFolderTableRow = false
	return nil
}

func (s *SceneStage) setToComponent(c *cptype.Component) error {
	b, err := json.Marshal(s)
	if err != nil {
		return err
	}
	if err := json.Unmarshal(b, &c); err != nil {
		return err
	}
	return nil
}

func NewStageData(ss *SceneStage, scene apistructs.AutoTestScene, svc *autotestv2.Service) (s StageData) {
	s.Title = fmt.Sprintf("#%d %s: %s", scene.ID, ss.sdk.I18n("scene"), scene.Name)
	if scene.RefSetID > 0 {
		s.Tags = []stages.Tag{
			{
				Label: ss.sdk.I18n("sceneSetRefer"),
				Color: "red",
			},
			{
				Label: ss.sdk.I18n(scene.Policy.ToString()),
				Color: "blue",
			},
		}
	}
	s.ID = scene.ID
	s.GroupID = func() int {
		if scene.GroupID == 0 {
			return int(scene.ID)
		}
		return int(scene.GroupID)
	}()
	s.Operations = make(map[string]interface{})
	s.Operations["add"] = OperationInfo{
		OperationBaseInfo: OperationBaseInfo{
			Key:       AddParallelOperationKey.String(),
			Icon:      "add",
			HoverTip:  ss.sdk.I18n("addParallelScene"),
			HoverShow: true,
			Reload:    true,
			Disabled:  false,
		},
		Meta: OpMetaInfo{
			ID: scene.ID,
		},
	}
	s.Operations["delete"] = OperationInfo{
		OperationBaseInfo: OperationBaseInfo{
			Key:      DeleteOperationKey.String(),
			Icon:     "shanchu",
			Reload:   true,
			Disabled: false,
			Confirm:  ss.sdk.I18n("delete"),
		},
		Meta: OpMetaInfo{
			ID: scene.ID,
		},
	}
	s.Operations["split"] = OperationInfo{
		OperationBaseInfo: OperationBaseInfo{
			Key:       SplitOperationKey.String(),
			Icon:      "split",
			HoverTip:  ss.sdk.I18n("changeToSerial"),
			HoverShow: true,
			Reload:    true,
			Disabled: func() bool {
				scenes, _ := svc.ListAutotestSceneByGroupID(scene.SetID, uint64(s.GroupID))
				return len(scenes) <= 1
			}(),
		},
		Meta: OpMetaInfo{
			ID: scene.ID,
			Data: map[string]interface{}{
				"groupID": s.GroupID,
			},
		},
	}
	if scene.RefSetID == 0 {
		s.Operations["copy"] = OperationInfo{
			OperationBaseInfo: OperationBaseInfo{
				Key:       CopyParallelOperationKey.String(),
				Icon:      "fz1",
				HoverShow: true,
				Text:      ss.sdk.I18n("copyScene"),
				Reload:    true,
				Disabled:  false,
				Group:     "copy",
			},
			Meta: OpMetaInfo{
				ID: scene.ID,
			},
		}
		s.Operations["copyTo"] = OperationInfo{
			OperationBaseInfo: OperationBaseInfo{
				Key:       CopyToOperationKey.String(),
				Icon:      "fz1",
				HoverShow: true,
				Text:      ss.sdk.I18n("copyToAnotherSceneSet"),
				Reload:    true,
				Disabled:  false,
				Group:     "copy",
			},
			Meta: OpMetaInfo{
				ID: scene.ID,
			},
		}
	}
	s.Operations["edit"] = OperationInfo{
		OperationBaseInfo: OperationBaseInfo{
			Key:       EditOperationKey.String(),
			Icon:      "bianji",
			HoverShow: true,
			Text:      ss.sdk.I18n("editScene"),
			Reload:    true,
			Disabled:  false,
		},
		Meta: OpMetaInfo{
			ID: scene.ID,
		},
	}
	return
}

func GetOpsInfo(opsData interface{}) (*OpMetaInfo, error) {
	if opsData == nil {
		err := fmt.Errorf("empty operation data")
		return nil, err
	}
	var op OperationInfo
	cont, err := json.Marshal(opsData)
	if err != nil {
		return nil, err
	}
	err = json.Unmarshal(cont, &op)
	if err != nil {
		return nil, err
	}
	meta := op.Meta
	return &meta, nil
}
