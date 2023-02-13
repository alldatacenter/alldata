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

package bdl

import (
	"strconv"
	"strings"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dbclient"
)

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

	// guest
	RoleGuest = "Guest"
)

var (
	OrgMRoles = []string{"manager"}             // 对企业有管理权限的角色列表
	ProMRoles = []string{"owner", "lead", "pm"} // 对项目有管理权限的角色列表
	AppMRoles = []string{"owner", "lead"}       // 对应用有管理权限的角色列表
)

var AllScopeRoleMap = map[apistructs.ScopeType]map[string]RoleInfo{
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

type RolesSet struct {
	orgID                uint64
	userID               string
	orgs, projects, apps map[string][]string // key is scope id, value is role
}

func (p *RolesSet) OrgID() uint64 {
	return p.orgID
}

func (p *RolesSet) UserID() string {
	return p.userID
}

func (p *RolesSet) Orgs() map[string][]string {
	return p.orgs
}

func (p *RolesSet) Projects() map[string][]string {
	return p.projects
}

func (p *RolesSet) Apps() map[string][]string {
	return p.apps
}

// RolesOrgs returns the list of organizations IDs in which the user plays the roles
func (p *RolesSet) RolesOrgs(roles ...string) []string {
	return p.sliceRoles(p.orgs, roles...)
}

// RolesProjects returns the list of project IDs in which the user plays the roles
func (p *RolesSet) RolesProjects(roles ...string) []string {
	return p.sliceRoles(p.projects, roles...)
}

// RolesApps returns the list of applications IDs in which the user plays the roles
// 传入角色, 返回 app id 列表 (由于返回结果不会太多, 所以都没有做去重)
func (p *RolesSet) RolesApps(roles ...string) []string {
	return p.sliceRoles(p.apps, roles...)
}

func (p *RolesSet) sliceRoles(scope map[string][]string, roles ...string) []string {
	var results []string
	if len(roles) == 0 || roles[0] == "*" {
		for id := range scope {
			results = append(results, id)
		}
		return results
	}
	for _, role := range roles {
		results = append(results, p.slice(role, scope)...)
	}
	return results
}

func (p *RolesSet) slice(role string, scope map[string][]string) []string {
	var results []string
	for id, roles := range scope {
		for _, v := range roles {
			if strings.EqualFold(v, role) {
				results = append(results, id)
				continue
			}
		}
	}
	return results
}

type RoleInfo struct {
	Role      string `json:"role"`
	IsHide    bool   `json:"isHide"`
	I18nKey   string `json:"i18nKey"`
	IsManager bool   `json:"isManager"`
	// Level is for ordering
	Level int `json:"-"`
}

// FetchAssetRolesSet fetches the permissions about Asset
func FetchAssetRolesSet(orgID uint64, userID string) *RolesSet {
	var permission = RolesSet{
		orgID:    orgID,
		userID:   userID,
		orgs:     make(map[string][]string),
		projects: make(map[string][]string),
		apps:     make(map[string][]string),
	}

	id := strconv.FormatUint(orgID, 10)
	if access, err := Bdl.ScopeRoleAccess(userID,
		&apistructs.ScopeRoleAccessRequest{Scope: apistructs.Scope{
			Type: apistructs.OrgScope,
			ID:   id,
		}}); err == nil && access.Access {
		permission.orgs[id] = access.Roles
	}

	var models []*apistructs.APIAssetsModel
	if err := dbclient.Sq().Where(map[string]interface{}{"org_id": orgID}).Find(&models).Error; err != nil {
		return &permission
	}

	for _, v := range models {
		if v.ProjectID != nil && *v.ProjectID != 0 {
			id := strconv.FormatUint(*v.ProjectID, 10)
			if _, ok := permission.projects[id]; !ok {
				if access, err := Bdl.ScopeRoleAccess(userID,
					&apistructs.ScopeRoleAccessRequest{Scope: apistructs.Scope{
						Type: apistructs.ProjectScope,
						ID:   id,
					}}); err == nil && access.Access {
					permission.projects[id] = access.Roles
				}
			}
		}
		if v.AppID != nil && *v.AppID != 0 {
			id := strconv.FormatUint(*v.AppID, 10)
			if _, ok := permission.apps[id]; !ok {
				if access, err := Bdl.ScopeRoleAccess(userID,
					&apistructs.ScopeRoleAccessRequest{Scope: apistructs.Scope{
						Type: apistructs.AppScope,
						ID:   id,
					}}); err == nil && access.Access {
					permission.apps[id] = access.Roles
				}
			}
		}
	}

	return &permission
}

// IsManager returns whether the user is a manager or not
func IsManager(userID string, scopeType apistructs.ScopeType, scopeID uint64) (bool, error) {
	req := apistructs.ScopeRoleAccessRequest{
		Scope: apistructs.Scope{
			Type: scopeType,
			ID:   strconv.FormatUint(scopeID, 10),
		},
	}
	scopeRole, err := Bdl.ScopeRoleAccess(userID, &req)
	if err != nil {
		return false, err
	}
	if scopeRole.Access {
		for _, role := range scopeRole.Roles {
			if CheckIfRoleIsManager(role) {
				return true, nil
			}
		}
	}
	return false, nil
}

// CheckIfRoleIsManager returns whether the role is a manger or not
func CheckIfRoleIsManager(role string) bool {
	for _, roleInfos := range GetScopeManagerRoleMap() {
		for roleName := range roleInfos {
			if roleName == role {
				return true
			}
		}
	}
	return false
}

func GetScopeManagerRoleMap() map[apistructs.ScopeType]map[string]RoleInfo {
	mgrRoles := make(map[apistructs.ScopeType]map[string]RoleInfo)
	for scopeType, roles := range AllScopeRoleMap {
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
