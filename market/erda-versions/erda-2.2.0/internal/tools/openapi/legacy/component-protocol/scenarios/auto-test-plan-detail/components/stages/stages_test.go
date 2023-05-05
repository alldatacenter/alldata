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
	"reflect"
	"testing"

	"bou.ke/monkey"
	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	protocol "github.com/erda-project/erda/internal/tools/openapi/legacy/component-protocol"
)

func TestFindFirstLastStepInGroup(t *testing.T) {
	tt := []struct {
		steps     []*apistructs.TestPlanV2Step
		wantFirst uint64
		wantLast  uint64
	}{
		{
			steps: []*apistructs.TestPlanV2Step{
				{ID: 2, PreID: 1},
				{ID: 3, PreID: 2},
				{ID: 4, PreID: 3},
				{ID: 5, PreID: 4},
			},
			wantFirst: 2,
			wantLast:  5,
		},
		{
			steps: []*apistructs.TestPlanV2Step{
				{ID: 4, PreID: 0},
				{ID: 2, PreID: 4},
				{ID: 3, PreID: 5},
				{ID: 5, PreID: 2},
			},
			wantFirst: 4,
			wantLast:  3,
		},
		{
			steps: []*apistructs.TestPlanV2Step{
				{ID: 1, PreID: 0},
			},
			wantFirst: 1,
			wantLast:  1,
		},
	}
	for _, v := range tt {
		firstStep, lastStep := findFirstLastStepInGroup(v.steps)
		firstStepID, lastStepID := firstStep.ID, lastStep.ID
		assert.Equal(t, v.wantFirst, firstStepID)
		assert.Equal(t, v.wantLast, lastStepID)
	}
}

// TestRenderItemMoveStagesFormFrontTarget [1,2] 3 move 2 to the front of 3
func TestRenderItemMoveStagesFormFrontTarget(t *testing.T) {
	var bdl *bundle.Bundle
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetTestPlanV2Step", func(*bundle.Bundle, uint64) (*apistructs.TestPlanV2Step, error) {
		return &apistructs.TestPlanV2Step{
			SceneSetID: 1,
			PreID:      1,
			PlanID:     1,
			GroupID:    1,
			ID:         2,
		}, nil
	})
	defer monkey.UnpatchAll()

	i := ComponentStageForm{
		ctxBdl: protocol.ContextBundle{},
		CommonStageForm: CommonStageForm{
			State: State{
				DragParams: DragParams{
					DragGroupKey: 1,
					DropGroupKey: 3,
					DragKey:      2,
					DropKey:      3,
					Position:     -1,
				},
				TestPlanId: 1,
			},
		},
	}
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "MoveTestPlansV2Step", func(*bundle.Bundle, apistructs.TestPlanV2StepMoveRequest) error {
		return nil
	})
	if i.RenderItemMoveStagesForm() != nil {
		t.Error("fail")
	}
}

// TestRenderItemMoveStagesFormBehindTarget [1,2] [3] move 2 to the behind of 3
func TestRenderItemMoveStagesFormBehindTarget(t *testing.T) {
	var bdl *bundle.Bundle
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetTestPlanV2Step", func(*bundle.Bundle, uint64) (*apistructs.TestPlanV2Step, error) {
		return &apistructs.TestPlanV2Step{
			SceneSetID: 1,
			PreID:      1,
			PlanID:     1,
			GroupID:    1,
			ID:         2,
		}, nil
	})
	defer monkey.UnpatchAll()

	i := ComponentStageForm{
		ctxBdl: protocol.ContextBundle{},
		CommonStageForm: CommonStageForm{
			State: State{
				DragParams: DragParams{
					DragGroupKey: 1,
					DropGroupKey: 3,
					DragKey:      2,
					DropKey:      3,
					Position:     1,
				},
				TestPlanId: 1,
			},
		},
	}
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "MoveTestPlansV2Step", func(*bundle.Bundle, apistructs.TestPlanV2StepMoveRequest) error {
		return nil
	})
	if i.RenderItemMoveStagesForm() != nil {
		t.Error("fail")
	}
}

// TestRenderItemMoveStagesFormBehindTarget [1,2] [3] move 1 to the front of 2
func TestRenderItemMoveStagesFormNoChange(t *testing.T) {
	var bdl *bundle.Bundle
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetTestPlanV2Step", func(*bundle.Bundle, uint64) (*apistructs.TestPlanV2Step, error) {
		return &apistructs.TestPlanV2Step{
			PreID:   1,
			GroupID: 1,
			ID:      1,
		}, nil
	})
	defer monkey.UnpatchAll()

	i := ComponentStageForm{
		ctxBdl: protocol.ContextBundle{},
		CommonStageForm: CommonStageForm{
			State: State{
				DragParams: DragParams{
					DragGroupKey: 1,
					DropGroupKey: 1,
					DragKey:      1,
					DropKey:      2,
					Position:     -1,
				},
				TestPlanId: 1,
			},
		},
	}
	if i.RenderItemMoveStagesForm() != nil {
		t.Error("fail")
	}
}

// TestRenderItemMoveStagesFormBehindTarget2 [1,2] [3] move 2 to the behind of 1
func TestRenderItemMoveStagesFormNoChange2(t *testing.T) {
	var bdl *bundle.Bundle
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "GetTestPlanV2Step", func(*bundle.Bundle, uint64) (*apistructs.TestPlanV2Step, error) {
		return &apistructs.TestPlanV2Step{
			PreID:   1,
			GroupID: 1,
			ID:      2,
		}, nil
	})
	defer monkey.UnpatchAll()

	i := ComponentStageForm{
		ctxBdl: protocol.ContextBundle{},
		CommonStageForm: CommonStageForm{
			State: State{
				DragParams: DragParams{
					DragGroupKey: 1,
					DropGroupKey: 1,
					DragKey:      2,
					DropKey:      1,
					Position:     1,
				},
				TestPlanId: 1,
			},
		},
	}
	if i.RenderItemMoveStagesForm() != nil {
		t.Error("fail")
	}
}

// TestRenderGroupMoveStagesFormNoChange [1,2] [3] move [1,2] to the front of [3]
func TestRenderGroupMoveStagesFormNoChange(t *testing.T) {
	var bdl *bundle.Bundle
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "ListTestPlanV2Step", func(bdl *bundle.Bundle, planID, groupID uint64) ([]*apistructs.TestPlanV2Step, error) {
		if groupID == 1 {
			return []*apistructs.TestPlanV2Step{
				{ID: 1, PreID: 0},
				{ID: 2, PreID: 1},
			}, nil
		}
		return []*apistructs.TestPlanV2Step{
			{ID: 3, PreID: 2},
		}, nil

	})
	defer monkey.UnpatchAll()

	i := ComponentStageForm{
		ctxBdl: protocol.ContextBundle{},
		CommonStageForm: CommonStageForm{
			State: State{
				DragParams: DragParams{
					DragGroupKey: 1,
					DropGroupKey: 3,
					Position:     -1,
				},
				TestPlanId: 1,
			},
		},
	}
	if i.RenderGroupMoveStagesForm() != nil {
		t.Error("fail")
	}
}

// TestRenderGroupMoveStagesFormNoChange2 [1,2] [3] move [3] to the behind of [1,2]
func TestRenderGroupMoveStagesFormNoChange2(t *testing.T) {
	var bdl *bundle.Bundle
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "ListTestPlanV2Step", func(bdl *bundle.Bundle, planID, groupID uint64) ([]*apistructs.TestPlanV2Step, error) {
		if groupID == 3 {
			return []*apistructs.TestPlanV2Step{
				{ID: 3, PreID: 2},
			}, nil
		}
		return []*apistructs.TestPlanV2Step{
			{ID: 1, PreID: 0},
			{ID: 2, PreID: 1},
		}, nil

	})
	defer monkey.UnpatchAll()

	i := ComponentStageForm{
		ctxBdl: protocol.ContextBundle{},
		CommonStageForm: CommonStageForm{
			State: State{
				DragParams: DragParams{
					DragGroupKey: 3,
					DropGroupKey: 1,
					Position:     1,
				},
				TestPlanId: 1,
			},
		},
	}
	if i.RenderGroupMoveStagesForm() != nil {
		t.Error("fail")
	}
}

func TestRenderSplitStagesFormWithOneStep(t *testing.T) {
	var bdl *bundle.Bundle
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "ListTestPlanV2Step", func(*bundle.Bundle, uint64, uint64) ([]*apistructs.TestPlanV2Step, error) {
		return []*apistructs.TestPlanV2Step{
			{
				PreID:   0,
				PlanID:  1,
				GroupID: 1,
				ID:      1,
			},
		}, nil
	})
	defer monkey.UnpatchAll()

	i := ComponentStageForm{
		ctxBdl: protocol.ContextBundle{},
		CommonStageForm: CommonStageForm{
			State: State{
				DragParams: DragParams{
					DragGroupKey: 3,
					DropGroupKey: 1,
					Position:     1,
				},
				TestPlanId: 1,
			},
		},
	}

	opsData := OperationInfo{
		OperationBaseInfo: OperationBaseInfo{},
		Meta: OpMetaInfo{
			ID: 1,
			Data: map[string]interface{}{
				"groupID": 1,
			},
		},
	}
	if i.RenderSplitStagesForm(opsData) != nil {
		t.Error("fail")
	}
}

func TestRenderSplitStagesFormWithNilStep(t *testing.T) {
	var bdl *bundle.Bundle
	monkey.PatchInstanceMethod(reflect.TypeOf(bdl), "ListTestPlanV2Step", func(*bundle.Bundle, uint64, uint64) ([]*apistructs.TestPlanV2Step, error) {
		return nil, nil
	})
	defer monkey.UnpatchAll()

	i := ComponentStageForm{
		ctxBdl: protocol.ContextBundle{},
		CommonStageForm: CommonStageForm{
			State: State{
				DragParams: DragParams{
					DragGroupKey: 3,
					DropGroupKey: 1,
					Position:     1,
				},
				TestPlanId: 1,
			},
		},
	}

	opsData := OperationInfo{
		OperationBaseInfo: OperationBaseInfo{},
		Meta: OpMetaInfo{
			ID: 1,
			Data: map[string]interface{}{
				"groupID": 1,
			},
		},
	}
	if i.RenderSplitStagesForm(opsData).Error() != "the groupID is not exists" {
		t.Error("fail")
	}
}
