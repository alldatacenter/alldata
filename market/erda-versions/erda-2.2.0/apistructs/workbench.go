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

package apistructs

type WorkbenchRequest struct {
	OrgID      uint64   `json:"orgID"`
	PageNo     int      `json:"pageNo"`
	PageSize   int      `json:"pageSize"`
	IssueSize  int      `json:"issueSize"`
	ProjectIDs []uint64 `json:"projectIDs"`
	IssuePagingRequest
}

type WorkbenchItemType string

const (
	WorkbenchItemProj           WorkbenchItemType = "project"
	WorkbenchItemApp            WorkbenchItemType = "app"
	WorkbenchItemUnreadMes      WorkbenchItemType = "unreadMessages"
	WorkbenchItemTicket         WorkbenchItemType = "tickets"
	WorkbenchItemApproveRequest WorkbenchItemType = "approveRequest"
	WorkbenchItemActivities     WorkbenchItemType = "activities"
	WorkbenchItemDefault                          = WorkbenchItemProj
)

func (w WorkbenchItemType) IsEmpty() bool {
	return string(w) == ""
}

func (w WorkbenchItemType) String() string {
	return string(w)
}

type WorkbenchProjAppRequest struct {
	// e.g: project/app
	Type WorkbenchItemType `json:"type"`
	// e.g query string
	Query string `json:"query"`
	PageRequest
}

type WorkbenchProjOverviewResp struct {
	Header
	Data WorkbenchProjOverviewRespData `json:"data"`
}

type WorkbenchProjOverviewRespData struct {
	Total int                         `json:"total"`
	List  []WorkbenchProjOverviewItem `json:"list"`
}

type WorkbenchProjOverviewItem struct {
	ProjectDTO    ProjectDTO            `json:"projectDTO"`
	IssueInfo     *ProjectIssueInfo     `json:"issueInfo"`
	StatisticInfo *ProjectStatisticInfo `json:"statisticInfo"`
}

type ProjectIssueInfo struct {
	TotalIssueNum       int `json:"totalIssueNum"`
	UnSpecialIssueNum   int `json:"unSpecialIssueNum"`
	ExpiredIssueNum     int `json:"expiredIssueNum"`
	ExpiredOneDayNum    int `json:"expiredOneDayNum"`
	ExpiredTomorrowNum  int `json:"expiredTomorrowNum"`
	ExpiredSevenDayNum  int `json:"expiredSevenDayNum"`
	ExpiredThirtyDayNum int `json:"expiredThirtyDayNum"`
	FeatureDayNum       int `json:"featureDayNum"`
}

type ProjectStatisticInfo struct {
	ServiceCount      int64 `json:"serviceCount,omitempty"`
	Last24HAlertCount int64 `json:"last24hAlertCount,omitempty"`
}

type WorkbenchResponse struct {
	Header
	Data map[uint64]*WorkbenchProjectItem `json:"data"`
}

type WorkbenchResponseData struct {
	TotalProject int                     `json:"totalProject"`
	TotalIssue   int                     `json:"totalIssue"`
	List         []*WorkbenchProjectItem `json:"list"`
}

type WorkbenchProjectItem struct {
	TotalIssueNum       int `json:"totalIssueNum"`
	UnSpecialIssueNum   int `json:"unSpecialIssueNum"`
	ExpiredIssueNum     int `json:"expiredIssueNum"`
	ExpiredOneDayNum    int `json:"expiredOneDayNum"`
	ExpiredTomorrowNum  int `json:"expiredTomorrowNum"`
	ExpiredSevenDayNum  int `json:"expiredSevenDayNum"`
	ExpiredThirtyDayNum int `json:"expiredThirtyDayNum"`
	FeatureDayNum       int `json:"featureDayNum"`
}

type AppWorkBenchItem struct {
	ApplicationDTO
	AppRuntimeNum int `json:"appRuntimeNum"`
	AppOpenMrNum  int `json:"appMrNum"`
}

type AppWorkbenchResponseData struct {
	TotalApps int                `json:"totalApps"`
	List      []AppWorkBenchItem `json:"list"`
}

var StateBelongs = []IssueStateBelong{
	IssueStateBelongOpen,
	IssueStateBelongWorking,
	IssueStateBelongDone,
	IssueStateBelongWontfix,
	IssueStateBelongReopen,
	IssueStateBelongResolved,
	IssueStateBelongClosed,
}

var UnfinishedStateBelongs = []IssueStateBelong{
	IssueStateBelongOpen,
	IssueStateBelongWorking,
	IssueStateBelongWontfix,
	IssueStateBelongReopen,
	IssueStateBelongResolved,
}

var UnclosedStateBelongs = []IssueStateBelong{
	IssueStateBelongOpen,
	IssueStateBelongWorking,
	IssueStateBelongDone,
	IssueStateBelongWontfix,
	IssueStateBelongReopen,
	IssueStateBelongResolved,
}

type WorkbenchMsgRequest struct {
	Type WorkbenchItemType `json:"type"`
	PageRequest
}
