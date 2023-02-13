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

package autotestv2

import (
	"encoding/json"
	"regexp"
	"strconv"
	"time"

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda/pkg/expression"

	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dao"
)

// CreateAutoTestSceneStep 添加场景步骤
func (svc *Service) CreateAutoTestSceneStep(req apistructs.AutotestSceneRequest) (uint64, error) {
	//if ok, _ := regexp.MatchString("^[a-zA-Z\u4e00-\u9fa50-9_-]*$", req.Name); !ok {
	//	return 0, apierrors.ErrCreateAutoTestSceneStep.InvalidState("名称只可输入中文、英文、数字、中划线或下划线")
	//}
	// TODO 放开限制，直接把特殊字符删掉
	reg := regexp.MustCompile("[^a-zA-Z\u4e00-\u9fa50-9_-]")
	req.Name = reg.ReplaceAllString(req.Name, "")

	total, err := svc.db.GetAutoTestSceneStepNumber(req.SceneID)
	if err != nil {
		return 0, err
	}
	if total >= 100 {
		return 0, apierrors.ErrUpdateAutoTestSceneStep.InvalidState("一个场景下，限制100个步骤")
	}

	total, err = svc.db.GetAutoTestSceneStepNumber(req.SceneID)
	if err != nil {
		return 0, err
	}
	if total >= 100000 {
		return 0, apierrors.ErrUpdateAutoTestSceneStep.InvalidState("一个空间下，限制10万个步骤")
	}

	// ID != 0 复制节点
	if req.ID != 0 {
		err := svc.db.CopyAutoTestSceneStep(req)
		if err != nil {
			return 0, err
		}
		step, err := svc.db.GetAutoTestSceneStepByPreID(req.ID, apistructs.PreTypeParallel)
		if err != nil {
			return 0, err
		}
		return step.ID, err
	}

	preID, err := svc.FindStepPosition(&req)
	if err != nil {
		return 0, err
	}
	id, err := svc.InsertAutoTestSceneStep(req, preID)
	if err != nil {
		return 0, nil
	}

	if err := svc.db.UpdateAutotestSceneUpdater(req.SceneID, req.UserID); err != nil {
		return 0, err
	}
	return id, nil
}

// UpdateAutoTestSceneStep 更新场景步骤
func (svc *Service) UpdateAutoTestSceneStep(req apistructs.AutotestSceneRequest) (uint64, error) {
	//if ok, _ := regexp.MatchString("^[a-zA-Z\u4e00-\u9fa50-9_-]*$", req.Name); !ok {
	//	return 0, apierrors.ErrCreateAutoTestSceneStep.InvalidState("名称只可输入中文、英文、数字、中划线或下划线")
	//}

	// TODO 放开限制，直接把特殊字符删掉
	reg := regexp.MustCompile("[^a-zA-Z\u4e00-\u9fa50-9_\\-()/\\s]")
	req.Name = reg.ReplaceAllString(req.Name, "")
	step, err := svc.db.GetAutoTestSceneStep(req.ID)
	if err != nil {
		return 0, nil
	}

	// 检查所属测试空间是否被锁定
	sc, err := svc.GetAutotestScene(apistructs.AutotestSceneRequest{SceneID: step.SceneID})
	if err != nil {
		return 0, err
	}
	sp, err := svc.GetSpace(sc.SpaceID)
	if err != nil {
		return 0, err
	}
	if !sp.IsOpen() {
		return 0, apierrors.ErrUpdateAutoTestSceneStep.InvalidState("所属测试空间已锁定")
	}

	step.Value = req.Value
	step.Name = req.Name
	step.UpdaterID = req.UserID
	step.APISpecID = req.APISpecID
	if req.IsDisabled != nil {
		step.IsDisabled = *req.IsDisabled
	}
	if err := svc.db.UpdateAutotestSceneStep(step); err != nil {
		return 0, err
	}

	if err := svc.db.UpdateAutotestSceneUpdater(req.SceneID, req.UserID); err != nil {
		return 0, err
	}

	if err := svc.UpdateAutotestSceneUpdateTime(step.SceneID); err != nil {
		return 0, err
	}
	return step.ID, nil
}

// MoveAutoTestSceneStep 更新场景步骤顺序
func (svc *Service) MoveAutoTestSceneStep(req apistructs.AutotestSceneRequest) error {
	// 如果是整组移动逻辑不一样
	if req.IsGroup == true {
		return svc.db.MoveAutoTestSceneStepGroup(req)
	}
	if req.GroupID == -1 {
		// 将步骤单独成组
		list, err := svc.ListAutoTestSceneStep(req.SceneID)
		if err != nil {
			return err
		}
		if req.Target == -1 {
			// 步骤在最后单独成组
			req.Target = int64(list[len(list)-1].ID)
		} else {
			//  步骤在中间单独成组
			for _, v := range list {
				if v.ID == req.ID {
					req.Target = int64(v.ID)
					break
				}
				flag := false
				for _, c := range v.Children {
					if c.ID == req.ID {
						flag = true
						req.Target = int64(v.ID)
						break
					}
				}
				if flag == true {
					break
				}
			}
		}
		return svc.db.MoveAutoTestSceneStepToGroup(req)
	}
	if err := svc.db.UpdateAutotestSceneUpdater(req.SceneID, req.UserID); err != nil {
		return err
	}
	if err := svc.db.UpdateAutotestSceneUpdateAt(req.SceneID, time.Now()); err != nil {
		return err
	}
	return svc.db.MoveAutoTestSceneStep(req)
}

// DeleteAutoTestSceneStep 删除场景步骤
func (svc *Service) DeleteAutoTestSceneStep(id uint64) error {
	step, err := svc.db.GetAutoTestSceneStep(id)
	if err != nil {
		return err
	}
	err = svc.CutAutoTestSceneStep(step)
	if err != nil {
		return err
	}
	err = svc.db.DeleteAutoTestSceneStep(step.ID)
	if err != nil {
		return err
	}

	return nil
}

// ListAutoTestSceneStep 获取场景步骤列表
func (svc *Service) ListAutoTestSceneStep(sceneID uint64) ([]apistructs.AutoTestSceneStep, error) {
	scs, err := svc.db.ListAutoTestSceneStep(sceneID)
	if err != nil {
		return nil, err
	}
	type idType struct {
		PreID   uint64
		PreType apistructs.PreType
	}
	stepMap := make(map[idType]*apistructs.AutoTestSceneStep)
	for _, v := range scs {
		stepMap[idType{v.PreID, v.PreType}] = v.Convert()
	}
	var steps []apistructs.AutoTestSceneStep

	// 获取串行节点列表
	for head := uint64(0); ; {
		s, ok := stepMap[idType{head, apistructs.PreTypeSerial}]
		if !ok {
			break
		}
		head = s.ID
		// 获取并行节点列表
		for head2 := s.ID; ; {
			s2, ok := stepMap[idType{head2, apistructs.PreTypeParallel}]
			if !ok {
				break
			}
			head2 = s2.ID
			s.Children = append(s.Children, *s2)
		}
		steps = append(steps, *s)
	}

	return steps, nil
}

// CutAutoTestSceneStep 切断步骤的联系
func (svc *Service) CutAutoTestSceneStep(now *dao.AutoTestSceneStep) error {
	var pnext, next *dao.AutoTestSceneStep // 并行的下一个节点,串行的下一个节点
	// 下一个步骤节点
	next, err := svc.db.GetAutoTestSceneStepByPreID(now.ID, now.PreType)
	if err != nil {
		if gorm.IsRecordNotFoundError(err) {
			next = nil
		} else {
			return err
		}
	}
	// 如果切断串行节点，需要判断它下面有没有并行节点
	if now.PreType == apistructs.PreTypeSerial {
		pnext, err = svc.db.GetAutoTestSceneStepByPreID(now.ID, apistructs.PreTypeParallel)
		if err != nil {
			if gorm.IsRecordNotFoundError(err) {
				pnext = nil
			} else {
				return err
			}
		}
	}
	// 当前节点是串行节点而且有并行节点指向它
	if pnext != nil {
		pnext.PreType = apistructs.PreTypeSerial
		pnext.PreID = now.PreID
		if next != nil {
			next.PreID = pnext.ID
		}
	} else {
		if next != nil {
			next.PreID = now.PreID
		}
	}

	if next != nil {
		err := svc.db.UpdateAutotestSceneStep(next)
		if err != nil {
			return err
		}
	}

	if pnext != nil {
		err := svc.db.UpdateAutotestSceneStep(pnext)
		if err != nil {
			return err
		}
	}
	return nil
}

// InsertAutoTestSceneStep 插入步骤
func (svc *Service) InsertAutoTestSceneStep(req apistructs.AutotestSceneRequest, preID uint64) (uint64, error) {
	return svc.db.InsertAutoTestSceneStep(req, preID)
}

// FindStepPosition 获取步骤位置
func (svc *Service) FindStepPosition(req *apistructs.AutotestSceneRequest) (uint64, error) {
	// 在末尾添加
	if req.Target == -1 && req.GroupID == -1 {
		list, err := svc.ListAutoTestSceneStep(req.SceneID)
		if err != nil {
			return 0, err
		}
		preID := uint64(0)
		if len(list) > 0 {
			preID = list[len(list)-1].ID
		}
		req.PreType = apistructs.PreTypeSerial
		return preID, nil
	}

	if req.Position == 1 {
		return uint64(req.Target), nil
	}
	next, err := svc.db.GetAutoTestSceneStep(uint64(req.Target))
	if err != nil {
		return 0, err
	}
	return next.PreID, nil
}

// GetAutoTestSceneStep 获取步骤
func (svc *Service) GetAutoTestSceneStep(stepID uint64) (*dao.AutoTestSceneStep, error) {
	step, err := svc.db.GetAutoTestSceneStep(stepID)
	if err != nil {
		return nil, err
	}
	return step, nil

}

// AutoTestGetStepOutPut 获取步骤接口出参
func (svc *Service) AutoTestGetStepOutPut(steps []apistructs.AutoTestSceneStep) (map[string]string, error) {
	var outputs = map[string]string{}
	for _, step := range steps {
		err := appendStepOutput(step, outputs)
		if err != nil {
			return nil, err
		}
		for _, childStep := range step.Children {
			err := appendStepOutput(childStep, outputs)
			if err != nil {
				return nil, err
			}
		}
	}
	return outputs, nil
}

func appendStepOutput(step apistructs.AutoTestSceneStep, outputs map[string]string) error {

	if outputs == nil {
		outputs = map[string]string{}
	}

	if step.Value == "" || step.Type != apistructs.StepTypeAPI {
		return nil
	}

	type Value struct {
		ApiInfo apistructs.APIInfoV2 `json:"apiSpec"`
	}
	var value Value
	err := json.Unmarshal([]byte(step.Value), &value)
	if err != nil {
		return err
	}
	for _, v := range value.ApiInfo.OutParams {
		outputs["#"+strconv.Itoa(int(step.ID))+" "+step.Name+":"+v.Key] = expression.LeftPlaceholder + " outputs." + strconv.Itoa(int(step.ID)) + "." + v.Key + " " + expression.RightPlaceholder
	}

	return nil
}
