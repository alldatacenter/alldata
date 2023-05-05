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

package stages

import (
	"context"
	"encoding/json"
	"fmt"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
)

// SetCtxBundle 设置bundle
func (i *ComponentStageForm) SetCtxBundle(b protocol.ContextBundle) error {
	if b.Bdl == nil || b.I18nPrinter == nil {
		err := fmt.Errorf("invalie context bundle")
		return err
	}
	logrus.Infof("inParams:%+v, identity:%+v", b.InParams, b.Identity)
	i.ctxBdl = b
	return nil
}

// GenComponentState 获取state
func (i *ComponentStageForm) GenComponentState(c *apistructs.Component) error {
	if c == nil || c.State == nil {
		return nil
	}
	var state State
	cont, err := json.Marshal(c.State)
	if err != nil {
		logrus.Errorf("marshal component state failed, content:%v, err:%v", c.State, err)
		return err
	}
	err = json.Unmarshal(cont, &state)
	if err != nil {
		logrus.Errorf("unmarshal component state failed, content:%v, err:%v", cont, err)
		return err
	}
	i.State = state
	return nil
}

func (i *ComponentStageForm) RenderProtocol(c *apistructs.Component, g *apistructs.GlobalStateData) {
	if c.Data == nil {
		d := make(apistructs.ComponentData)
		c.Data = d
	}
	if c.Operations == nil {
		d := make(apistructs.ComponentOps)
		c.Operations = d
	}
	if c.Props == nil {
		d := make(map[string]interface{})
		c.Props = d
	}
	(*c).Data["value"] = i.Data.List
	c.Data["type"] = i.Data.Type
	c.State["showScenesSetDrawer"] = i.State.ShowScenesSetDrawer
	c.State["testPlanId"] = i.State.TestPlanId
	c.State["stepId"] = i.State.StepId
	c.Operations = i.Operations
	c.Props = i.Props
}

func GetOpsInfo(opsData interface{}) (*OpMetaInfo, error) {
	if opsData == nil {
		err := fmt.Errorf("empty operation data")
		return nil, err
	}
	var op OperationInfo
	cont, err := json.Marshal(opsData)
	if err != nil {
		logrus.Errorf("marshal inParams failed, content:%v, err:%v", opsData, err)
		return nil, err
	}
	err = json.Unmarshal(cont, &op)
	if err != nil {
		logrus.Errorf("unmarshal move out request failed, content:%v, err:%v", cont, err)
		return nil, err
	}
	meta := op.Meta
	return &meta, nil
}

func (i *ComponentStageForm) GetInParams() error {
	var inParams InParams
	cont, err := json.Marshal(i.ctxBdl.InParams)
	if err != nil {
		logrus.Errorf("marshal inParams failed, content:%v, err:%v", i.ctxBdl.InParams, err)
		return err
	}
	err = json.Unmarshal(cont, &inParams)
	if err != nil {
		logrus.Errorf("unmarshal move out request failed, content:%v, err:%v", cont, err)
		return err
	}
	i.InParams = inParams
	return nil
}

func (i *ComponentStageForm) Render(ctx context.Context, c *apistructs.Component, _ apistructs.ComponentProtocolScenario, event apistructs.ComponentEvent, gs *apistructs.GlobalStateData) (err error) {
	bdl := ctx.Value(protocol.GlobalInnerKeyCtxBundle.String()).(protocol.ContextBundle)
	err = i.SetCtxBundle(bdl)
	if err != nil {
		return
	}
	err = i.GenComponentState(c)
	if err != nil {
		return
	}
	if err = i.GetInParams(); err != nil {
		logrus.Errorf("get filter request failed, content:%+v, err:%v", *gs, err)
		return
	}

	i.Props = make(map[string]interface{})

	// init
	i.State.ShowScenesSetDrawer = false
	i.State.StepId = 0

	visible := make(map[string]interface{})
	visible["visible"] = true
	if i.State.Visible == false {
		visible["visible"] = false
		c.Props = visible
		c.Data = make(map[string]interface{})
		c.Data["value"] = []StageData{}
		return
	}
	i.Props = visible
	i.Props["groupDraggable"] = true
	switch event.Operation {
	case apistructs.InitializeOperation, apistructs.RenderingOperation:
		err = i.RenderListStageForm()
		if err != nil {
			return err
		}
	case apistructs.AutoTestSceneStepMoveItemOperationKey:
		err = i.RenderItemMoveStagesForm()
		if err != nil {
			return err
		}
		err = i.RenderListStageForm()
		if err != nil {
			return err
		}
	case apistructs.AutoTestSceneStepMoveGroupOperationKey:
		err = i.RenderGroupMoveStagesForm()
		if err != nil {
			return err
		}
		err = i.RenderListStageForm()
		if err != nil {
			return err
		}
	case apistructs.AutoTestSceneStepCreateOperationKey:
		if err := i.RenderCreateStagesForm(event.OperationData); err != nil {
			return err
		}
		if err := i.RenderListStageForm(); err != nil {
			return err
		}
	case apistructs.AutoTestSceneStepDeleteOperationKey:
		err = i.RenderDeleteStagesForm(event.OperationData)
		if err != nil {
			return err
		}
		err = i.RenderListStageForm()
		if err != nil {
			return err
		}
	case "clickItem":
		i.State.ShowScenesSetDrawer = true
		meta, err := GetOpsInfo(event.OperationData)
		if err != nil {
			return err
		}
		i.State.StepId = uint64(meta.Data["id"].(float64))
		if err := i.RenderListStageForm(); err != nil {
			return err
		}
	case apistructs.AutoTestSceneStepSplitOperationKey:
		err = i.RenderSplitStagesForm(event.OperationData)
		if err != nil {
			return err
		}
		err = i.RenderListStageForm()
		if err != nil {
			return err
		}
	}
	i.RenderProtocol(c, gs)
	return
}

func RenderCreator() protocol.CompRender {
	return &ComponentStageForm{}
}
