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

package query

import (
	"fmt"
	"strconv"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	commonpb "github.com/erda-project/erda-proto-go/common/pb"
	"github.com/erda-project/erda-proto-go/dop/issue/core/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
)

func (p *provider) GenerateButtonMap(projectID uint64, issueTypes []string) (map[string]map[int64][]*pb.IssueStateButton, error) {
	result := make(map[string]map[int64][]*pb.IssueStateButton, 0)

	relations := make(map[dao.IssueStateRelation]bool)
	// issueTypeState 保存每种任务类型的全部状态
	issueTypeState := make(map[string][]pb.IssueStateButton)
	issueTypeStateIDs := make(map[string][]int64, 0)

	stateRelations, err := p.db.GetIssuesStateRelations(projectID, "")
	if err != nil {
		return nil, err
	}
	states, err := p.db.GetIssuesStatesByProjectID(projectID, "")
	if err != nil {
		return nil, err
	}
	// 获取全部状态
	for _, r := range states {
		s := pb.IssueStateBelongEnum_StateBelong_value[r.Belong]
		issueTypeState[r.IssueType] = append(issueTypeState[r.IssueType], pb.IssueStateButton{
			StateID:     int64(r.ID),
			StateName:   r.Name,
			StateBelong: pb.IssueStateBelongEnum_StateBelong(s),
			Permission:  true,
		})
		issueTypeStateIDs[r.IssueType] = append(issueTypeStateIDs[r.IssueType], int64(r.ID))
	}

	for _, r := range stateRelations {
		if r.EndStateID > 0 {
			relations[dao.IssueStateRelation{StartStateID: r.ID, EndStateID: r.EndStateID}] = true
		}
	}

	if relations != nil {
		for _, iType := range issueTypes {
			// 遍历所有的起始状态
			for _, startState := range issueTypeStateIDs[iType] {
				var button []*pb.IssueStateButton
				// 遍历一个起始状态到所有的终态
				for _, r := range issueTypeState[iType] {
					var permission = true
					if relations[dao.IssueStateRelation{StartStateID: startState, EndStateID: r.StateID}] != true {
						permission = false
					}
					button = append(button, &pb.IssueStateButton{
						StateID:     r.StateID,
						StateName:   r.StateName,
						StateBelong: r.StateBelong,
						Permission:  permission,
					})
				}
				// 存入所有的状态结果
				if _, ok := result[iType]; !ok {
					result[iType] = make(map[int64][]*pb.IssueStateButton, 0)
				}
				result[iType][startState] = button
			}
		}
	}

	return result, nil
}

// generateButton 生成状态流转按钮
// needCheck: 鉴权项，若非空，则只鉴权非空项；调用后的鉴权结果可从 value 中获取
// store: 鉴权前，从这里获取结果，若有则直接返回，若无，则调用后写入鉴权结果
func (p *provider) GenerateButton(issueModel dao.Issue, identityInfo *commonpb.IdentityInfo,
	permCheckItems map[string]bool, store map[string]bool, relations map[dao.IssueStateRelation]bool,
	typeState map[string][]*pb.IssueStateButton) ([]*pb.IssueStateButton, error) {

	button, err := p.GenerateButtonWithoutPerm(issueModel, relations, typeState)
	if err != nil {
		return nil, err
	}
	// 如果是内部调用或者创建者，则无需进一步鉴权
	// if identityInfo.IsInternalClient() {
	// 	return button, nil
	// }

	// 获取全部鉴权为permission的状态，再次鉴权
	for i := range button {
		if button[i].Permission == false {
			continue
		}
		// 生成鉴权项
		permCheckItem := makeButtonCheckPermItem(issueModel, button[i].StateID)
		// 鉴权列表非空且当前鉴权项不在鉴权列表中，无需鉴权
		if permCheckItems != nil {
			if _, ok := permCheckItems[permCheckItem]; !ok {
				continue
			}
		}
		// 鉴权前，判断该鉴权项是否已经鉴权
		if store != nil {
			// 若已鉴权，则直接使用结果
			if access, ok := store[permCheckItem]; ok {
				// 将鉴权结果写入 button
				button[i].Permission = access
				continue
			}
		}

		var access bool
		// 调用鉴权服务，判断当前用户是否有推进到某个状态的权限
		if identityInfo != nil && identityInfo.InternalClient == "" {
			pcr := &apistructs.PermissionCheckRequest{
				UserID:  identityInfo.UserID,
				Scope:   apistructs.ProjectScope,
				ScopeID: issueModel.ProjectID,
			}
			tmpAccess, err := p.StateCheckPermission(pcr, issueModel.State, button[i].StateID)
			if err != nil {
				return nil, errors.Errorf("failed to check permission when generateButton, op: %v, err: %v", button[i], err)
			}
			access = tmpAccess
		} else {
			access = true
		}
		button[i].Permission = access
		// 保存本次结果至 permCheckItems
		if permCheckItems != nil {
			permCheckItems[permCheckItem] = access
		}
		// 保存本次鉴权结果至 store
		if store != nil {
			store[permCheckItem] = access
		}
	}
	return button, nil
}

// generateButtonWithoutPerm 根据 issue 类型和当前状态生成未鉴权的按钮，see: https://yuque.antfin-inc.com/terminus_paas_dev/vqkrzf/bnurmf
func (p *provider) GenerateButtonWithoutPerm(issueModel dao.Issue, relations map[dao.IssueStateRelation]bool, typeState map[string][]*pb.IssueStateButton) ([]*pb.IssueStateButton, error) {
	var button []*pb.IssueStateButton
	// 如果是批量处理接口 入参有map，遍历map将实际没有的关联的权限设成false
	if relations != nil {
		for _, r := range typeState[issueModel.Type] {
			if relations[dao.IssueStateRelation{StartStateID: issueModel.State, EndStateID: r.StateID}] != true {
				r.Permission = false
			}
			button = append(button, r)
		}
		return button, nil
	}
	// 如果是处理单个事件的button,入参map为nil
	stateRelations, err := p.db.GetIssuesStateRelations(issueModel.ProjectID, issueModel.Type)
	if err != nil {
		return nil, err
	}
	states, err := p.db.GetIssuesStatesByProjectID(issueModel.ProjectID, issueModel.Type)
	if err != nil {
		return nil, err
	}
	// stateID => states的下标
	buttonMap := make(map[int64]int)

	// 获取全部状态，初始鉴权false
	for i, s := range states {
		button = append(button, &pb.IssueStateButton{
			StateID:     int64(s.ID),
			StateName:   s.Name,
			StateBelong: pb.IssueStateBelongEnum_StateBelong(pb.IssueStateBelongEnum_StateBelong_value[s.Belong]),
			Permission:  false,
		})
		buttonMap[int64(s.ID)] = i
	}
	// 将实际有的状态流转的鉴权设成true
	for _, r := range stateRelations {
		if r.EndStateID > 0 && r.ID == issueModel.State {
			button[buttonMap[r.EndStateID]].Permission = true
		}
	}
	var resButton []*pb.IssueStateButton
	for _, b := range button {
		if b.Permission == true || b.StateID == issueModel.State {
			resButton = append(resButton, b)
		}
	}
	return resButton, nil
}

// makeButtonCheckPermItem 用于生成 开关 CanXxx 在 true 的基础上，再次校验用户权限时需要的 key
// key: ${issue-type}/CanXxx 开关项二次检查只与 issue 类型和 目标状态 有关，与当前状态无关 (permission.yml 定义)
func makeButtonCheckPermItem(issue dao.Issue, newStateID int64) string {
	return fmt.Sprintf("%s/%s", issue.Type, strconv.FormatInt(newStateID, 10))
}

// StateCheckPermission 事件状态Button鉴权
func (p *provider) StateCheckPermission(req *apistructs.PermissionCheckRequest, st int64, ed int64) (bool, error) {
	logrus.Debugf("invoke permission, time: %s, req: %+v", time.Now().Format(time.RFC3339), req)
	// 是否是内部服务账号
	resp, err := p.bdl.StateCheckPermission(req)
	if err != nil {
		return false, err
	}
	if resp.Access {
		return true, nil
	}
	for _, role := range resp.Roles {
		rp, err := p.db.GetIssueStatePermission(role, st, ed)
		if err != nil {
			return false, err
		}
		if rp != nil {
			return true, nil
		}
	}
	return false, nil
}
