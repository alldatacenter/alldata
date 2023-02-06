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
	"fmt"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol/scenarios/auto-test-plan-detail/i18n"
)

func (i *ComponentStageForm) RenderStage(step apistructs.TestPlanV2Step) (StageData, error) {
	groupID := int(step.GroupID)
	if groupID == 0 {
		groupID = int(step.ID)
	}
	i18nLocale := i.ctxBdl.Bdl.GetLocale(i.ctxBdl.Locale)
	pd := StageData{
		Title:      fmt.Sprintf("#%d %s: %s", step.ID, i18nLocale.Get(i18n.I18nKeySceneSet), step.SceneSetName),
		ID:         step.ID,
		GroupID:    groupID,
		Operations: make(map[string]interface{}),
	}
	o := CreateOperation{}
	o.Key = apistructs.AutoTestSceneStepCreateOperationKey.String()
	o.Icon = "add"
	o.HoverTip = i18nLocale.Get(i18n.I18nKeyAddStep)
	o.Disabled = false
	o.Reload = true
	o.HoverShow = true
	o.Meta.ID = step.ID
	pd.Operations["add"] = o

	oc := OperationInfo{}
	oc.Key = apistructs.AutoTestSceneStepDeleteOperationKey.String()
	oc.Icon = "shanchu"
	oc.Disabled = false
	oc.Reload = true
	oc.Meta.ID = step.ID
	pd.Operations["delete"] = oc

	os := OperationInfo{
		OperationBaseInfo: OperationBaseInfo{
			Icon:     "split",
			Key:      apistructs.AutoTestSceneStepSplitOperationKey.String(),
			HoverTip: i18nLocale.Get(i18n.I18nKeyChangeSerial),
			Disabled: true,
			Reload:   true,
		},
	}
	os.Meta.ID = step.ID
	m := map[string]interface{}{
		"groupID": groupID,
	}
	os.Meta.Data = m
	stepGroup, err := i.ctxBdl.Bdl.ListTestPlanV2Step(step.PlanID, uint64(groupID))
	if err != nil {
		return StageData{}, err
	}
	if len(stepGroup) > 1 {
		os.Disabled = false
	}
	pd.Operations["split"] = os

	return pd, nil
}

func (i *ComponentStageForm) RenderListStageForm() error {
	rsp, err := i.ctxBdl.Bdl.GetTestPlanV2(i.State.TestPlanId)
	if err != nil {
		return err
	}
	var list []StageData
	for _, v := range rsp.Data.Steps {
		stageData, err := i.RenderStage(*v)
		if err != nil {
			return err
		}
		list = append(list, stageData)
	}

	i.Data.List = list
	i.Data.Type = "sort-item"

	i.Operations = make(map[string]interface{})
	omi := OperationInfo{
		OperationBaseInfo: OperationBaseInfo{
			Key:    apistructs.AutoTestSceneStepMoveItemOperationKey.String(),
			Reload: true,
		},
	}
	omg := OperationInfo{
		OperationBaseInfo: OperationBaseInfo{
			Key:    apistructs.AutoTestSceneStepMoveGroupOperationKey.String(),
			Reload: true,
		},
	}
	ocl := OperationInfo{
		OperationBaseInfo: OperationBaseInfo{
			Key:      "clickItem",
			Reload:   true,
			FillMeta: "data",
		},
		Meta: OpMetaInfo{},
	}
	i.Operations["moveItem"] = omi
	i.Operations["moveGroup"] = omg
	i.Operations["clickItem"] = ocl

	return nil
}

func (i *ComponentStageForm) RenderCreateStagesForm(opsData interface{}) error {
	meta, err := GetOpsInfo(opsData)
	if err != nil {
		return err
	}
	preStep, err := i.ctxBdl.Bdl.GetTestPlanV2Step(meta.ID)
	if err != nil {
		return err
	}
	groupID := preStep.GroupID
	if groupID == 0 {
		groupID = preStep.ID
	}

	_, err = i.ctxBdl.Bdl.CreateTestPlansV2Step(apistructs.TestPlanV2StepAddRequest{
		PreID:      meta.ID,
		TestPlanID: i.State.TestPlanId,
		GroupID:    groupID,
		IdentityInfo: apistructs.IdentityInfo{
			UserID: i.ctxBdl.Identity.UserID,
		},
	})
	if err != nil {
		return err
	}
	return nil
}

func (i *ComponentStageForm) RenderDeleteStagesForm(opsData interface{}) error {
	meta, err := GetOpsInfo(opsData)
	if err != nil {
		return err
	}

	err = i.ctxBdl.Bdl.DeleteTestPlansV2Step(apistructs.TestPlanV2StepDeleteRequest{
		StepID:     meta.ID,
		TestPlanID: i.State.TestPlanId,
		IdentityInfo: apistructs.IdentityInfo{
			UserID: i.ctxBdl.Identity.UserID,
		},
	})
	if err != nil {
		return err
	}
	return nil
}

func (i *ComponentStageForm) RenderGroupMoveStagesForm() (err error) {
	var (
		req apistructs.TestPlanV2StepMoveRequest
	)
	dragGroupKey := uint64(i.State.DragParams.DragGroupKey)
	dropGroupKey := uint64(i.State.DragParams.DropGroupKey)

	req.UserID = i.ctxBdl.Identity.UserID
	req.TestPlanID = i.State.TestPlanId
	req.IsGroup = true

	stepDragGroup, err := i.ctxBdl.Bdl.ListTestPlanV2Step(i.State.TestPlanId, dragGroupKey)
	if err != nil {
		return err
	}
	if len(stepDragGroup) <= 0 {
		return errors.New("the dragGroupKey is not exists")
	}
	firstStepDrag, lastStepDrag := findFirstLastStepInGroup(stepDragGroup)
	req.StepID = firstStepDrag.ID
	req.LastStepID = lastStepDrag.ID

	switch i.State.DragParams.Position {
	case 0: // inside target
		return nil
	case -1: // in front of the target
		stepDropGroup, err := i.ctxBdl.Bdl.ListTestPlanV2Step(i.State.TestPlanId, dropGroupKey)
		if err != nil {
			return err
		}
		if len(stepDropGroup) <= 0 {
			return errors.New("the dropGroupKey is not exists")
		}
		firstStepDrop, _ := findFirstLastStepInGroup(stepDropGroup)
		req.PreID = firstStepDrop.PreID
		req.TargetStepID = firstStepDrop.ID
		// the order of the linked list has not changed
		if req.PreID == req.LastStepID {
			return nil
		}
	case 1: // behind the target
		stepDropGroup, err := i.ctxBdl.Bdl.ListTestPlanV2Step(i.State.TestPlanId, dropGroupKey)
		if err != nil {
			return err
		}
		if len(stepDropGroup) <= 0 {
			return errors.New("the dropGroupKey is not exists")
		}
		_, lastStepDrop := findFirstLastStepInGroup(stepDropGroup)
		req.PreID = lastStepDrop.ID
		req.TargetStepID = lastStepDrop.ID
		// the order of the linked list has not changed
		if req.PreID == firstStepDrag.PreID {
			return nil
		}
	default:
		return errors.New("unknown position")
	}
	return i.ctxBdl.Bdl.MoveTestPlansV2Step(req)
}

func findFirstLastStepInGroup(steps []*apistructs.TestPlanV2Step) (firstStep, lastStep *apistructs.TestPlanV2Step) {
	stepIDMap := make(map[uint64]*apistructs.TestPlanV2Step, len(steps))
	preIDMap := make(map[uint64]*apistructs.TestPlanV2Step, len(steps))
	for _, v := range steps {
		stepIDMap[v.ID] = v
		preIDMap[v.PreID] = v
	}
	for k := range preIDMap {
		if _, ok := stepIDMap[k]; !ok {
			firstStep = preIDMap[k]
			break
		}
	}
	for k := range stepIDMap {
		if _, ok := preIDMap[k]; !ok {
			lastStep = stepIDMap[k]
			break
		}
	}
	return
}

func (i *ComponentStageForm) RenderItemMoveStagesForm() (err error) {
	var (
		step     *apistructs.TestPlanV2Step
		req      apistructs.TestPlanV2StepMoveRequest
		testPlan *apistructs.TestPlanV2GetResponse
	)
	dragGroupKey := uint64(i.State.DragParams.DragGroupKey)
	dropGroupKey := uint64(i.State.DragParams.DropGroupKey)

	req.UserID = i.ctxBdl.Identity.UserID
	req.TestPlanID = i.State.TestPlanId
	req.StepID = uint64(i.State.DragParams.DragKey)
	req.LastStepID = uint64(i.State.DragParams.DragKey)
	req.IsGroup = false
	if i.State.DragParams.DropKey == -1 { // move to the end and be independent group
		req.TargetStepID = 0
	} else {
		req.TargetStepID = uint64(i.State.DragParams.DropKey)
	}

	// find preID
	if i.State.DragParams.DropKey == -1 { // move to the end and be independent group
		testPlan, err = i.ctxBdl.Bdl.GetTestPlanV2(i.State.TestPlanId)
		if err != nil {
			return err
		}
		req.PreID = testPlan.Data.Steps[len(testPlan.Data.Steps)-1].ID
	} else {
		switch i.State.DragParams.Position {
		case 0: // inside target
			return nil
		case 1: // behind the target
			step, err = i.ctxBdl.Bdl.GetTestPlanV2Step(uint64(i.State.DragParams.DragKey))
			if err != nil {
				return
			}
			req.PreID = uint64(i.State.DragParams.DropKey)
			// the order of the linked list has not changed in the same group
			if req.PreID == step.PreID && dragGroupKey == dropGroupKey {
				return nil
			}

		case -1: // in front of the target
			step, err = i.ctxBdl.Bdl.GetTestPlanV2Step(uint64(i.State.DragParams.DropKey))
			if err != nil {
				return
			}
			req.PreID = step.PreID
			// the order of the linked list has not changed in the same group
			if req.PreID == req.LastStepID && dragGroupKey == dropGroupKey {
				return nil
			}
		default:
			return errors.New("unknown position")
		}
	}
	return i.ctxBdl.Bdl.MoveTestPlansV2Step(req)
}

func (i *ComponentStageForm) RenderSplitStagesForm(opsData interface{}) (err error) {
	meta, err := GetOpsInfo(opsData)
	if err != nil {
		return err
	}

	var req apistructs.TestPlanV2StepMoveRequest

	req.UserID = i.ctxBdl.Identity.UserID
	req.TestPlanID = i.State.TestPlanId
	req.StepID = meta.ID
	req.LastStepID = meta.ID
	req.IsGroup = false
	req.TargetStepID = 0

	stepGroup, err := i.ctxBdl.Bdl.ListTestPlanV2Step(i.State.TestPlanId, uint64(meta.Data["groupID"].(float64)))
	if err != nil {
		return err
	}
	if len(stepGroup) <= 0 {
		return errors.New("the groupID is not exists")
	}
	if len(stepGroup) == 1 {
		return nil
	}
	_, lastStep := findFirstLastStepInGroup(stepGroup)
	req.PreID = lastStep.ID
	return i.ctxBdl.Bdl.MoveTestPlansV2Step(req)
}
