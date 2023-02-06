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

package issue

import (
	"strings"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dao"
)

// genrateButtonMap 生成按钮map，目前没带权限
func (svc *Issue) genrateButtonMap(projectID uint64, issueTypes []apistructs.IssueType) (map[apistructs.IssueType]map[int64][]apistructs.IssueStateButton, error) {
	result := make(map[apistructs.IssueType]map[int64][]apistructs.IssueStateButton, 0)

	relations := make(map[dao.IssueStateRelation]bool)
	// issueTypeState 保存每种任务类型的全部状态
	issueTypeState := make(map[apistructs.IssueType][]apistructs.IssueStateButton)
	issueTypeStateIDs := make(map[apistructs.IssueType][]int64, 0)

	stateRelations, err := svc.db.GetIssuesStateRelations(projectID, "")
	if err != nil {
		return nil, err
	}
	states, err := svc.db.GetIssuesStatesByProjectID(projectID, "")
	if err != nil {
		return nil, err
	}
	// 获取全部状态
	for _, r := range states {
		issueTypeState[r.IssueType] = append(issueTypeState[r.IssueType], apistructs.IssueStateButton{
			StateID:     int64(r.ID),
			StateName:   r.Name,
			StateBelong: r.Belong,
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
				var button []apistructs.IssueStateButton
				// 遍历一个起始状态到所有的终态
				for _, r := range issueTypeState[iType] {
					if relations[dao.IssueStateRelation{StartStateID: startState, EndStateID: r.StateID}] != true {
						r.Permission = false
					}
					button = append(button, r)
				}
				// 存入所有的状态结果
				if _, ok := result[iType]; !ok {
					result[iType] = make(map[int64][]apistructs.IssueStateButton, 0)
				}
				result[iType][startState] = button
			}
		}
	}

	return result, nil
}

// batchGenerateButton 批量处理 button，优化调用次数
// buttons: key: issue, value: button
func (svc *Issue) batchGenerateButton(buttons map[dao.Issue][]apistructs.IssueStateButton, identityInfo apistructs.IdentityInfo) error {
	// store 存储指定情况下的鉴权情况，防止相同情况多次调用网络请求，造成严重浪费，例如分页接口
	// key: see makeButtonCheckPermItem
	// value: access or not
	store := make(map[string]bool)
	// relations 保存全部流转情况，防止调用多次接口
	relations := make(map[dao.IssueStateRelation]bool)
	// issueTypeState 保存每种任务类型的全部状态
	issueTypeState := make(map[apistructs.IssueType][]apistructs.IssueStateButton)

	if len(buttons) < 1 {
		return nil
	}

	var projectID int64
	for k := range buttons {
		projectID = int64(k.ID)
		break
	}

	stateRelations, err := svc.db.GetIssuesStateRelations(uint64(projectID), "")
	if err != nil {
		return err
	}
	states, err := svc.db.GetIssuesStatesByProjectID(uint64(projectID), "")
	if err != nil {
		return err
	}
	// 获取全部状态
	for _, r := range states {
		issueTypeState[r.IssueType] = append(issueTypeState[r.IssueType], apistructs.IssueStateButton{
			StateID:     int64(r.ID),
			StateName:   r.Name,
			StateBelong: r.Belong,
			Permission:  true,
		})
	}
	// 获取状态流转关系
	for _, r := range stateRelations {
		if r.EndStateID > 0 {
			relations[dao.IssueStateRelation{StartStateID: r.ID, EndStateID: r.EndStateID}] = true
		}
	}
	for issueModel := range buttons {
		// 批量处理时，所有 状态 均鉴权，permCheckItems == nil
		button, err := svc.generateButton(issueModel, identityInfo, nil, store, relations, issueTypeState)
		if err != nil {
			return err
		}
		buttons[issueModel] = button
	}
	return nil
}

// generateButton 生成状态流转按钮
// needCheck: 鉴权项，若非空，则只鉴权非空项；调用后的鉴权结果可从 value 中获取
// store: 鉴权前，从这里获取结果，若有则直接返回，若无，则调用后写入鉴权结果
func (svc *Issue) generateButton(issueModel dao.Issue, identityInfo apistructs.IdentityInfo,
	permCheckItems map[string]bool, store map[string]bool, relations map[dao.IssueStateRelation]bool,
	typeState map[apistructs.IssueType][]apistructs.IssueStateButton) ([]apistructs.IssueStateButton, error) {

	button, err := svc.generateButtonWithoutPerm(issueModel, relations, typeState)
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
		if !identityInfo.IsInternalClient() {
			pcr := &apistructs.PermissionCheckRequest{
				UserID:  identityInfo.UserID,
				Scope:   apistructs.ProjectScope,
				ScopeID: issueModel.ProjectID,
			}
			tmpAccess, err := svc.StateCheckPermission(pcr, issueModel.State, button[i].StateID)
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
func (svc *Issue) generateButtonWithoutPerm(issueModel dao.Issue, relations map[dao.IssueStateRelation]bool, typeState map[apistructs.IssueType][]apistructs.IssueStateButton) ([]apistructs.IssueStateButton, error) {
	var button []apistructs.IssueStateButton
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
	stateRelations, err := svc.db.GetIssuesStateRelations(issueModel.ProjectID, issueModel.Type)
	if err != nil {
		return nil, err
	}
	states, err := svc.db.GetIssuesStatesByProjectID(issueModel.ProjectID, issueModel.Type)
	if err != nil {
		return nil, err
	}
	// stateID => states的下标
	buttonMap := make(map[int64]int)

	// 获取全部状态，初始鉴权false
	for i, s := range states {
		button = append(button, apistructs.IssueStateButton{
			StateID:     int64(s.ID),
			StateName:   s.Name,
			StateBelong: s.Belong,
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
	var resButton []apistructs.IssueStateButton
	for _, b := range button {
		if b.Permission == true || b.StateID == issueModel.State {
			resButton = append(resButton, b)
		}
	}
	return resButton, nil
}

// 事件处理者鉴权
func getResourceRoles(issueModel dao.Issue, identityInfo apistructs.IdentityInfo) string {
	var resourceRoles []string
	if identityInfo.UserID == issueModel.Creator {
		resourceRoles = append(resourceRoles, "Creator")
	}
	if identityInfo.UserID == issueModel.Assignee {
		resourceRoles = append(resourceRoles, "Assignee")
	}

	return strings.Join(resourceRoles, ",")
}
