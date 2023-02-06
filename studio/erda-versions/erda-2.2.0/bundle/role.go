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

package bundle

import "github.com/erda-project/erda/apistructs"

// RoleInfo Role info
type RoleInfo struct {
	Role      string `json:"role"`
	IsHide    bool   `json:"isHide"`
	I18nKey   string `json:"i18nKey"`
	IsManager bool   `json:"isManager"`
	// 用来排序的字段
	Level int `json:"-"`
}

const (
	// sys
	RoleSysManager = "Manager"

	// org
	RoleOrgManager         = "Manager"
	RoleOrgDev             = "Dev"
	RoleOrgOps             = "Ops"
	RoleOrgSupport         = "Support"
	RoleOrgDataManager     = "DataManager"
	RoleOrgDataEngineer    = "DataEngineer"
	RoleOrgReporter        = "Reporter"
	RoleOrgEdgeAppEngineer = "EdgeOps"
	RoleOrgGuest           = "Guest"

	// project
	RoleProjectOwner    = "Owner"
	RoleProjectLead     = "Lead"
	RoleProjectPM       = "PM"
	RoleProjectPD       = "PD"
	RoleProjectDev      = "Dev"
	RoleProjectQA       = "QA"
	RoleProjectReporter = "Reporter"
	RoleProjectGuest    = "Guest"

	// app
	RoleAppOwner = "Owner"
	RoleAppLead  = "Lead"
	RoleAppDev   = "Dev"
	RoleAppQA    = "QA"
	RoleAppOps   = "Ops"
	RoleAppGuest = "Guest"

	// publisher
	RolePublisherManager = "PublisherManager"
	RolePublisherMember  = "PublisherMember"
)

// allScopeRoleMap Record all role info under all scopes.
var allScopeRoleMap = map[apistructs.ScopeType]map[string]RoleInfo{
	apistructs.SysScope: {
		RoleSysManager: {Role: RoleSysManager, IsHide: false, I18nKey: "SysManagerRole", IsManager: true, Level: 0},
	},
	apistructs.OrgScope: {
		RoleOrgManager:         {Role: RoleSysManager, IsHide: false, I18nKey: "OrgManagerRole", IsManager: true, Level: 0},
		RoleOrgDev:             {Role: RoleOrgDev, IsHide: false, I18nKey: "OrgDevRole", IsManager: false, Level: 1},
		RoleOrgOps:             {Role: RoleOrgOps, IsHide: false, I18nKey: "OrgOpsRole", IsManager: false, Level: 2},
		RoleOrgDataManager:     {Role: RoleOrgDataManager, IsHide: false, I18nKey: "OrgDataManagerRole", IsManager: false, Level: 3},
		RoleOrgDataEngineer:    {Role: RoleOrgDataEngineer, IsHide: false, I18nKey: "OrgDataEngineerRole", IsManager: false, Level: 4},
		RoleOrgSupport:         {Role: RoleOrgSupport, IsHide: true, I18nKey: "OrgSupportRole", IsManager: false, Level: 5},
		RoleOrgReporter:        {Role: RoleOrgReporter, IsHide: false, I18nKey: "OrgReporterRole", IsManager: false, Level: 6},
		RolePublisherManager:   {Role: RolePublisherManager, IsHide: false, I18nKey: "PublisherManagerRole", IsManager: false, Level: 7},
		RoleOrgEdgeAppEngineer: {Role: RoleOrgEdgeAppEngineer, IsHide: false, I18nKey: "RoleOrgEdgeAppEngineer", IsManager: false, Level: 8},
		RoleOrgGuest:           {Role: RoleProjectGuest, IsHide: true, I18nKey: "OrgGuestRole", IsManager: false, Level: 9},
	},
	apistructs.ProjectScope: {
		RoleProjectOwner:    {Role: RoleProjectOwner, IsHide: false, I18nKey: "ProjectOwnerRole", IsManager: true, Level: 0},
		RoleProjectLead:     {Role: RoleProjectLead, IsHide: false, I18nKey: "ProjectLeadRole", IsManager: true, Level: 1},
		RoleProjectPM:       {Role: RoleProjectPM, IsHide: false, I18nKey: "ProjectPMRole", IsManager: true, Level: 2},
		RoleProjectPD:       {Role: RoleProjectPD, IsHide: false, I18nKey: "ProjectPDRole", IsManager: false, Level: 3},
		RoleProjectDev:      {Role: RoleProjectDev, IsHide: false, I18nKey: "ProjectDevRole", IsManager: false, Level: 4},
		RoleProjectQA:       {Role: RoleProjectQA, IsHide: false, I18nKey: "ProjectQARole", IsManager: false, Level: 5},
		RoleProjectReporter: {Role: RoleProjectReporter, IsHide: false, I18nKey: "ProjectReporterRole", IsManager: false, Level: 6},
		RoleProjectGuest:    {Role: RoleProjectGuest, IsHide: true, I18nKey: "ProjectGuestRole", IsManager: false, Level: 7},
	},
	apistructs.AppScope: {
		RoleAppOwner: {Role: RoleAppOwner, IsHide: false, I18nKey: "AppOwnerRole", IsManager: true, Level: 0},
		RoleAppLead:  {Role: RoleAppLead, IsHide: false, I18nKey: "AppLeadRole", IsManager: true, Level: 1},
		RoleAppOps:   {Role: RoleAppOps, IsHide: false, I18nKey: "AppOpsRole", IsManager: false, Level: 2},
		RoleAppDev:   {Role: RoleAppDev, IsHide: false, I18nKey: "AppDevRole", IsManager: false, Level: 3},
		RoleAppQA:    {Role: RoleAppQA, IsHide: false, I18nKey: "AppQARole", IsManager: false, Level: 4},
		RoleAppGuest: {Role: RoleAppGuest, IsHide: true, I18nKey: "AppGuestRole", IsManager: false, Level: 5},
	},
	apistructs.PublisherScope: {
		RolePublisherManager: {Role: RolePublisherManager, IsHide: false, I18nKey: "PublisherManagerRole", IsManager: true, Level: 0},
		RolePublisherMember:  {Role: RolePublisherMember, IsHide: false, I18nKey: "PublisherMemberRole", IsManager: false, Level: 1},
	},
}

// getScopeManagerRoleMap Get all administrator role information map under scopeType
func getScopeManagerRoleMap() map[apistructs.ScopeType]map[string]RoleInfo {
	mgrRoles := make(map[apistructs.ScopeType]map[string]RoleInfo)
	for scopeType, roles := range allScopeRoleMap {
		for roleName, roleInfo := range roles {
			if roleInfo.IsManager {
				if _, ok := mgrRoles[scopeType]; !ok {
					mgrRoles[scopeType] = make(map[string]RoleInfo)
				}
				mgrRoles[scopeType][roleName] = roleInfo
			}
		}
	}
	return mgrRoles
}

// CheckIfRoleIsManager Determine whether role is an administrator.
func (b *Bundle) CheckIfRoleIsManager(role string) bool {
	for _, roleInfos := range getScopeManagerRoleMap() {
		for roleName := range roleInfos {
			if roleName == role {
				return true
			}
		}
	}
	return false
}
