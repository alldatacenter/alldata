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

package types

import (
	"time"

	"github.com/erda-project/erda/apistructs"
)

const (
	UnknownType = "unknown"
)

// ModelHeader metadata header
type ModelHeader struct {
	ID        int64 `gorm:"primary_key"`
	CreatedAt time.Time
	UpdatedAt time.Time
}

// MetaserverMSG kafka容器信息格式
type MetaserverMSG struct {
	Name      string                 `json:"name"`      // metaserver_container、metaserver_all_containers
	TimeStamp int64                  `json:"timestamp"` // 纳秒
	Fields    map[string]interface{} `json:"fields"`    // 全量容器事件时 key: containerID value: container info
	Tags      map[string]string      `json:"tags,omitempty"`
}

var ContainerStatusIndex = map[InstanceStatus]int{
	InstanceStatusStarting:  1,
	InstanceStatusRunning:   1,
	InstanceStatusHealthy:   2,
	InstanceStatusUnHealthy: 2,
	InstanceStatusUnknown:   2,
	InstanceStatusStopped:   3,
	InstanceStatusFailed:    3,
	InstanceStatusFinished:  3,
	InstanceStatusKilled:    4,
	InstanceStatusOOM:       5,
}

// InstanceStatus instance status
type InstanceStatus string

const (
	InstanceStatusStarting  InstanceStatus = "Starting" // 已启动，但未收到健康检查事件，瞬态
	InstanceStatusRunning   InstanceStatus = "Running"
	InstanceStatusHealthy   InstanceStatus = "Healthy"
	InstanceStatusUnHealthy InstanceStatus = "UnHealthy" // 已启动但收到未通过健康检查事件
	InstanceStatusFinished  InstanceStatus = "Finished"  // 已完成，退出码为0
	InstanceStatusFailed    InstanceStatus = "Failed"    // 已退出，退出码非0
	InstanceStatusKilled    InstanceStatus = "Killed"    // 已被杀
	InstanceStatusStopped   InstanceStatus = "Stopped"   // 已停止，Scheduler与DCOS断连期间事件丢失，后续补偿时，需将Healthy置为Stopped
	InstanceStatusUnknown   InstanceStatus = "Unknown"
	InstanceStatusOOM       InstanceStatus = "OOM"
	InstanceStatusDead      InstanceStatus = "Dead"
)

func IsValidSchedulerInstanceStatus(status string) bool {
	switch InstanceStatus(status) {
	case InstanceStatusHealthy, InstanceStatusUnHealthy, InstanceStatusFailed, InstanceStatusFinished, InstanceStatusKilled:
		return true
	default:
		return false
	}
}

func IsValidAgentInstanceStatus(status string) bool {
	switch InstanceStatus(status) {
	case InstanceStatusStarting, InstanceStatusStopped, InstanceStatusKilled, InstanceStatusOOM:
		return true
	default:
		return false
	}
}

// CmService 服务
type CmService struct {
	ModelHeader     `json:"-"`
	Cluster         string `json:"-"`            // 集群名
	DiceProject     string `json:"-"`            // 所在大项目名称
	DiceApplication string `json:"-"`            // 所在项目
	DiceRuntime     string `json:"-"`            // 所在runtime
	DiceService     string `json:"-"`            // 所属应用
	PrivateAddr     string `json:"private_addr"` // 服务内部地址(lb)
	PublicAddr      string `json:"public_addr"`  // 服务对外地址(lb)
}

const (
	DefaultWorkspace apistructs.DiceWorkspace = "DEFAULT"
	// DevWorkspace 开发环境
	DevWorkspace apistructs.DiceWorkspace = "DEV"
	// TestWorkspace 测试环境
	TestWorkspace apistructs.DiceWorkspace = "TEST"
	// StagingWorkspace 预发环境
	StagingWorkspace apistructs.DiceWorkspace = "STAGING"
	// ProdWorkspace 生产环境
	ProdWorkspace apistructs.DiceWorkspace = "PROD"
)

// NotFound error define for notfound
var NotFound = "not found"

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

// RoleInfo 角色信息
type RoleInfo struct {
	Role      string `json:"role"`
	IsHide    bool   `json:"isHide"`
	I18nKey   string `json:"i18nKey"`
	IsManager bool   `json:"isManager"`
	// 用来排序的字段
	Level int `json:"-"`
}

// AllScopeRoleMap 记录所有 scope 下所有 角色信息
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

// GetScopeManagerRoleMap 获取所有 scopeType 下的管理员角色信息 map
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

// GetScopeManagerRoleNames 获取 scopeType 下的管理员角色名
func GetScopeManagerRoleNames(scopeType apistructs.ScopeType) []string {
	var result []string
	for name := range GetScopeManagerRoleMap()[scopeType] {
		result = append(result, name)
	}
	return result
}

// CheckIfRoleIsManager 判断 role 是否是管理员角色
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

// CheckIfRoleIsOwner 判断 role 是否是项目/应用所有者角色
func CheckIfRoleIsOwner(role string) bool {
	return role == RoleProjectOwner
}

// CheckIfRoleIsValid 判断 role 是否合法
func CheckIfRoleIsValid(role string) bool {
	if role == "" {
		return false
	}
	for _, roleInfos := range AllScopeRoleMap {
		for roleName := range roleInfos {
			if roleName == role {
				return true
			}
		}
	}
	return false
}

// MemberLabelInfo 成员标签信息
type MemberLabelInfo struct {
	Label   apistructs.MemeberLabelName `json:"label"`
	I18nKey string                      `json:"i18nKey"`
}

// AllLabelsMap 记录所有的成员标签
var AllLabelsMap = map[apistructs.MemeberLabelName]MemberLabelInfo{
	apistructs.LabelNameOutsource: {Label: apistructs.LabelNameOutsource, I18nKey: "MemberLabelOutsource"},
	apistructs.LabelNamePartner:   {Label: apistructs.LabelNamePartner, I18nKey: "MemberLabelPartner"},
}

// CheckIfMemberLabelIsValid 判断 label 是否合法
func CheckIfMemberLabelIsValid(label string) bool {
	if label == "" {
		return false
	}

	if _, ok := AllLabelsMap[apistructs.MemeberLabelName(label)]; !ok {
		return false
	}

	return true
}

// AbilityAppReq 能力请求
type AbilityAppReq struct {
	OrgID           int64 `json:"orgId"`
	ClusterID       int64 `json:"clusterId"`
	ClusterName     string
	ApplicationName string
	UserID          string `json:"operator"`
}
