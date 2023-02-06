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

package models

import (
	"errors"
	"fmt"
	"strconv"
	"time"

	"github.com/jinzhu/gorm"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/pkg/diceworkspace"
	"github.com/erda-project/erda/internal/tools/gittar/pkg/gitmodule"
	"github.com/erda-project/erda/internal/tools/gittar/uc"
)

var (
	MERGE_REQUEST_OPEN   = "open"
	MERGE_REQUEST_MERGED = "merged"
	MERGE_REQUEST_CLOSED = "closed"

	MERGE_STATUS_UNCHECKED   = "unchecked"
	MERGE_STATUS_CHECKING    = "checking"
	MERGE_STATUS_CONFLICT    = "conflict"
	MERGE_STATUS_NO_CONFLICT = "no_conflict"
)

type BatchSearchMrRequest struct {
	Conditions []BatchSearchMrCondition `json:"conditions"`
}

type BatchSearchMrCondition struct {
	AppID int64 `json:"appID"`
	MrID  int64 `json:"mrID"`
}

type QueryMergeRequestsResult struct {
	List  []*apistructs.MergeRequestInfo `json:"list"`
	Total int                            `json:"total"`
}

type MergeQueryCondition struct {
	State string `json:"state"`
	Query string `json:"query"`
	Page  int    `json:"page"`
	Size  int    `json:"size"`
}

type MergeOptions struct {
	RemoveSourceBranch bool   `json:"removeSourceBranch"`
	CommitMessage      string `json:"CommitMessage"`
}

//MergeRequest model
type MergeRequest struct {
	ID                   int64
	RepoID               int64 `gorm:"size:150;index:idx_repo_id"`
	Title                string
	Description          string `gorm:"type:text"`
	State                string `gorm:"size:150;index:idx_state"`
	AuthorId             string `gorm:"size:150;index:idx_author_id"`
	AssigneeId           string `gorm:"size:150;index:idx_assignee_id"`
	MergeUserId          string
	CloseUserId          string
	MergeCommitSha       string
	RepoMergeId          int
	SourceBranch         string
	SourceSha            string
	TargetBranch         string
	TargetSha            string
	RemoveSourceBranch   bool
	CreatedAt            time.Time
	UpdatedAt            *time.Time
	MergeAt              *time.Time
	CloseAt              *time.Time
	Score                int    `gorm:"size:150;index:idx_score"`
	ScoreNum             int    `gorm:"size:150;index:idx_score_num"`
	JoinTempBranchStatus string `gorm:"join_temp_branch_status"`
	IsJoinTempBranch     bool   `gorm:"is_join_temp_branch"`
}

type MrCheckRun struct {
	ID     string `json:"id"`
	MRID   int64  `json:"mrId"`
	Status string `json:"status"`
	Name   string `json:"name"`
}

func (mergeRequest *MergeRequest) ToInfo(repo *gitmodule.Repository) *apistructs.MergeRequestInfo {
	result := &apistructs.MergeRequestInfo{}
	result.SourceBranch = mergeRequest.SourceBranch
	result.TargetBranch = mergeRequest.TargetBranch
	result.Title = mergeRequest.Title
	result.Description = mergeRequest.Description
	result.RemoveSourceBranch = mergeRequest.RemoveSourceBranch
	result.AssigneeId = mergeRequest.AssigneeId
	result.RepoMergeId = mergeRequest.RepoMergeId
	result.State = mergeRequest.State
	result.UpdatedAt = mergeRequest.UpdatedAt
	result.CreatedAt = mergeRequest.CreatedAt
	result.AuthorId = mergeRequest.AuthorId
	result.CloseUserId = mergeRequest.CloseUserId
	result.CloseAt = mergeRequest.CloseAt
	result.MergeAt = mergeRequest.MergeAt
	result.MergeUserId = mergeRequest.MergeUserId
	result.Id = mergeRequest.ID
	result.SourceSha = mergeRequest.SourceSha
	result.TargetSha = mergeRequest.TargetSha
	result.RepoID = mergeRequest.RepoID
	result.AppID = repo.ApplicationId
	result.Score = mergeRequest.Score
	result.ScoreNum = mergeRequest.ScoreNum
	result.JoinTempBranchStatus = mergeRequest.JoinTempBranchStatus
	result.IsJoinTempBranch = mergeRequest.IsJoinTempBranch

	if mergeRequest.SourceBranch != "" && mergeRequest.TargetBranch != "" {
		result.DefaultCommitMessage = fmt.Sprintf("Merge branch '%s' into '%s'", mergeRequest.SourceBranch, mergeRequest.TargetBranch)
	}

	if result.AuthorId != "" {
		dto, err := uc.FindUserByIdWithDesensitize(result.AuthorId)
		if err == nil {
			result.AuthorUser = dto
		} else {
			logrus.Errorf("get user from uc error: %v", err)
		}
	}

	if result.AssigneeId != "" {
		dto, err := uc.FindUserByIdWithDesensitize(result.AssigneeId)
		if err == nil {
			result.AssigneeUser = dto
		} else {
			logrus.Errorf("get user from uc error: %v", err)
		}
	}

	if result.CloseUserId != "" {
		dto, err := uc.FindUserByIdWithDesensitize(result.CloseUserId)
		if err == nil {
			result.CloseUser = dto
		} else {
			logrus.Errorf("get user from uc error: %v", err)
		}
	}

	if result.MergeUserId != "" {
		dto, err := uc.FindUserByIdWithDesensitize(result.MergeUserId)
		if err == nil {
			result.MergeUser = dto
		} else {
			logrus.Errorf("get user from uc error: %v", err)
		}
	}
	return result
}

func (svc *Service) CreateMergeRequest(repo *gitmodule.Repository, user *User, info *apistructs.MergeRequestInfo) (*apistructs.MergeRequestInfo, error) {
	info.RepoID = repo.ID

	err := svc.CheckPermission(repo, user, PermissionCreateMR, nil)
	if err != nil {
		return nil, err
	}
	var lastMr MergeRequest
	err = svc.db.Where("repo_id = ? ", info.RepoID).Order("repo_merge_id desc").FirstOrInit(&lastMr).Error
	if err != nil {
		return nil, err
	}

	sourceCommit, err := repo.GetBranchCommit(info.SourceBranch)
	if err != nil {
		return nil, err
	}

	targetCommit, err := repo.GetBranchCommit(info.TargetBranch)
	if err != nil {
		return nil, err
	}

	mergeRequest := MergeRequest{
		RepoID:             repo.ID,
		Title:              info.Title,
		Description:        info.Description,
		State:              MERGE_REQUEST_OPEN,
		AuthorId:           user.Id,
		AssigneeId:         info.AssigneeId,
		SourceBranch:       info.SourceBranch,
		TargetBranch:       info.TargetBranch,
		SourceSha:          sourceCommit.ID,
		TargetSha:          targetCommit.ID,
		RemoveSourceBranch: info.RemoveSourceBranch,
		RepoMergeId:        lastMr.RepoMergeId + 1,
	}
	err = svc.db.Create(&mergeRequest).Error
	if err != nil {
		return nil, err
	}

	info.RepoMergeId = mergeRequest.RepoMergeId
	info.AuthorUser = &apistructs.UserInfoDto{
		UserID:   user.Id,
		NickName: user.NickName,
		Username: user.Name,
	}

	return mergeRequest.ToInfo(repo), nil
}

func (svc *Service) UpdateMergeRequest(repo *gitmodule.Repository, user *User, info *apistructs.MergeRequestInfo) (*apistructs.MergeRequestInfo, error) {
	info.RepoID = repo.ID
	var mergeRequest MergeRequest
	err := svc.db.Where("repo_id = ? and repo_merge_id=?", info.RepoID, info.RepoMergeId).First(&mergeRequest).Error
	if err != nil {
		return nil, err
	}

	if mergeRequest.AuthorId != user.Id && info.ScoreNum <= mergeRequest.ScoreNum {
		err = svc.CheckPermission(repo, user, PermissionEditMR, getMrUserRole(mergeRequest, user.Id))
		if err != nil {
			return nil, err
		}
	}

	if info.ScoreNum > mergeRequest.ScoreNum { //更新评分
		mergeRequest.Score = info.Score
		mergeRequest.ScoreNum = info.ScoreNum
	} else { //更新信息
		mergeRequest.SourceBranch = info.SourceBranch
		mergeRequest.TargetBranch = info.TargetBranch
		mergeRequest.Title = info.Title
		mergeRequest.Description = info.Description
		mergeRequest.RemoveSourceBranch = info.RemoveSourceBranch
		mergeRequest.AssigneeId = info.AssigneeId
	}

	if len(info.State) > 0 {
		mergeRequest.State = info.State
	}

	sourceCommit, err := repo.GetBranchCommit(mergeRequest.SourceBranch)
	if err == nil {
		info.SourceSha = sourceCommit.ID
		mergeRequest.SourceSha = sourceCommit.ID
	}
	targetCommit, err := repo.GetBranchCommit(mergeRequest.TargetBranch)
	if err == nil {
		info.TargetSha = targetCommit.ID
		mergeRequest.TargetSha = targetCommit.ID
	}
	err = svc.db.Save(&mergeRequest).Error
	if err != nil {
		return nil, err
	}
	return info, nil
}

func (svc *Service) GetMergeRequestDetail(repo *gitmodule.Repository, mergeId int) (*apistructs.MergeRequestInfo, error) {
	var mergeRequest MergeRequest
	err := svc.db.Where("repo_id = ? and repo_merge_id=?", repo.ID, mergeId).First(&mergeRequest).Error
	if err != nil {
		return nil, err
	}
	//open状态才更新
	if mergeRequest.State == MERGE_REQUEST_OPEN {
		sourceCommit, err := repo.GetBranchCommit(mergeRequest.SourceBranch)
		hasUpdate := false
		if err == nil {
			if mergeRequest.SourceSha != sourceCommit.ID {
				mergeRequest.SourceSha = sourceCommit.ID
				hasUpdate = true
			}
		}
		targetCommit, err := repo.GetBranchCommit(mergeRequest.TargetBranch)
		if err == nil {
			if mergeRequest.TargetSha != targetCommit.ID {
				mergeRequest.TargetSha = targetCommit.ID
				hasUpdate = true
			}
		}
		if hasUpdate {
			err = svc.db.Save(&mergeRequest).Error
			if err != nil {
				return nil, err
			}
		}
	}
	result := mergeRequest.ToInfo(repo)
	result.IsCheckRunValid, err = svc.IsCheckRunsValid(repo, mergeRequest.ID)
	return result, err
}

func (svc *Service) SyncMergeRequest(repo *gitmodule.Repository, branch string, commitID string, userID string, isHookExist bool) error {
	var mergeRequests []MergeRequest
	err := svc.db.Where("repo_id = ? and source_branch = ? and state = ?",
		repo.ID, branch, MERGE_REQUEST_OPEN).Find(&mergeRequests).Error
	if err != nil && err != gorm.ErrRecordNotFound {
		return err
	}
	for _, mergeRequest := range mergeRequests {
		flag := (mergeRequest.SourceSha != commitID)
		mergeRequest.SourceSha = commitID
		err := svc.db.Save(&mergeRequest).Error
		if err != nil {
			return err
		}
		mrInfo := mergeRequest.ToInfo(repo)
		if flag {
			go func(mergeRequest MergeRequest) {
				// check-run
				conflictInfo, err := repo.GetMergeStatus(mergeRequest.SourceBranch, mergeRequest.TargetBranch)
				if err != nil {
					logrus.Info("has conflict, err: ", err)
					return
				}
				if !conflictInfo.HasConflict {
					info := mergeRequest.ToInfo(repo)
					info.MergeUserId = userID
					svc.TriggerEvent(repo, apistructs.CheckRunEvent, info)
				}
			}(mergeRequest)
		}
		if isHookExist {
			go func() {
				rules, err := svc.bundle.GetAppBranchRules(uint64(repo.ApplicationId))
				if err != nil {
					logrus.Errorf("failed to get branch rules err:%s", err)
				}

				mrInfo.TargetBranchRule = diceworkspace.GetValidBranchByGitReference(mrInfo.TargetBranch, rules)
				svc.TriggerEvent(repo, apistructs.GitUpdateMREvent, mrInfo)
			}()
		}

	}
	return nil
}

func (svc *Service) QueryMergeRequests(repo *gitmodule.Repository, queryCondition *apistructs.GittarQueryMrRequest) (*QueryMergeRequestsResult, error) {
	var mergeRequests []MergeRequest
	query := svc.db.Model(&MergeRequest{}).Where("repo_id =? ", repo.ID)
	if queryCondition.State != "all" && queryCondition.State != "" {
		query = query.Where("state = ?", queryCondition.State)
	}
	if queryCondition.Query != "" {
		mergeID, err := strconv.ParseInt(queryCondition.Query, 10, 64)
		if err == nil {
			query = query.Where("title like ? or repo_merge_id = ?", "%"+queryCondition.Query+"%", mergeID)
		} else {
			query = query.Where("title like ?", "%"+queryCondition.Query+"%")
		}
	}
	if queryCondition.AuthorId != "" {
		query = query.Where("author_id = ?", queryCondition.AuthorId)
	}

	if queryCondition.AssigneeId != "" {
		query = query.Where("assignee_id = ?", queryCondition.AssigneeId)
	}

	if queryCondition.TargetBranch != "" {
		query = query.Where("target_branch = ?", queryCondition.TargetBranch)
	}

	if queryCondition.SourceBranch != "" {
		query = query.Where("source_branch = ?", queryCondition.SourceBranch)
	}

	var count int
	err := query.Count(&count).Error
	if err != nil {
		return nil, err
	}
	err = query.Order("id desc").
		Offset((queryCondition.Page - 1) * queryCondition.Size).
		Limit(queryCondition.Size).
		Find(&mergeRequests).Error

	if err != nil {
		return nil, err
	}

	var results []*apistructs.MergeRequestInfo
	for _, mergeRequest := range mergeRequests {
		result := mergeRequest.ToInfo(repo)
		results = append(results, result)
	}

	return &QueryMergeRequestsResult{
		Total: count,
		List:  results,
	}, nil
}

func (svc *Service) BatchGetMergeRequests(request *BatchSearchMrRequest) ([]apistructs.MergeRequestInfo, error) {
	var mergeRequests []apistructs.MergeRequestInfo
	query := svc.db.Table("dice_repo_merge_requests").Joins("INNER JOIN dice_repos ON dice_repos.id = dice_repo_merge_requests.repo_id")
	for _, item := range request.Conditions {
		query = query.Or("dice_repo_repos.app_id = ? and dice_repo_merge_requests.repo_merge_id =?", item.AppID, item.MrID)
	}
	query = query.Select("dice_repo_merge_requests.*,dice_repo_repos.app_id")
	err := query.Scan(&mergeRequests).Error

	if err != nil {
		return nil, err
	}

	return mergeRequests, nil
}

type MergeCount struct {
	AppID uint64
	Count int
}

func (svc *Service) CountMergeRequests(appIDs []string, state string) (map[uint64]int, error) {
	var count []MergeCount
	query := svc.db.Table("dice_repo_merge_requests a").Joins("LEFT JOIN dice_repos b ON b.id = a.repo_id")
	if len(state) > 0 {
		query = query.Where("a.state = ?", state)
	}
	if err := query.Select("count(a.id) as count, b.app_id").Where("b.app_id IN (?)", appIDs).Group("b.id").Find(&count).Error; err != nil {
		return nil, err
	}
	res := make(map[uint64]int)
	for _, i := range count {
		res[i.AppID] = i.Count
	}
	return res, nil
}

func (svc *Service) Merge(repo *gitmodule.Repository, user *User, mergeId int, mergeOptions *MergeOptions) (*gitmodule.Commit, error) {
	var mergeRequest MergeRequest
	err := svc.db.Where("repo_id =? and repo_merge_id=?", repo.ID, mergeId).First(&mergeRequest).Error
	if err != nil {
		return nil, err
	}

	if mergeRequest.State != MERGE_REQUEST_OPEN {
		return nil, errors.New(mergeRequest.State + " 状态无法 merge")
	}

	err = svc.CheckPermission(repo, user, PermissionMergeMR, getMrUserRole(mergeRequest, user.Id))
	if err != nil {
		return nil, err
	}

	mergeStatus, err := repo.GetMergeStatus(mergeRequest.SourceBranch, mergeRequest.TargetBranch)
	if err != nil {
		return nil, err
	}
	if mergeStatus.HasError {
		return nil, errors.New(mergeStatus.ErrorMsg)
	}

	if mergeStatus.HasConflict {
		return nil, errors.New("has conflict")
	}

	if repo.IsProtectBranch(mergeRequest.TargetBranch) ||
		(repo.IsProtectBranch(mergeRequest.SourceBranch) && mergeRequest.RemoveSourceBranch) {
		err = svc.CheckPermission(repo, user, PermissionPushProtectBranch, nil)
		if err != nil {
			return nil, err
		}
	}

	if mergeOptions.CommitMessage == "" {
		mergeOptions.CommitMessage = fmt.Sprintf("Merge branch '%s' into '%s'", mergeRequest.SourceBranch, mergeRequest.TargetBranch)
	}
	_, err = repo.GetBranchCommit(mergeRequest.SourceBranch)
	if err != nil {
		return nil, err
	}

	commit, err := repo.Merge(mergeRequest.SourceBranch, mergeRequest.TargetBranch, user.ToGitSignature(), mergeOptions.CommitMessage)

	now := time.Now()
	if err == nil {
		mergeRequest.State = MERGE_REQUEST_MERGED
		mergeRequest.MergeCommitSha = commit.ID
		mergeRequest.MergeAt = &now
		mergeRequest.MergeUserId = user.Id
		err := svc.db.Save(&mergeRequest).Error
		if err != nil {
			return nil, err
		}
	} else {
		return nil, err
	}

	if mergeOptions.RemoveSourceBranch {
		repo.DeleteBranch(mergeRequest.SourceBranch)
	}

	return commit, nil
}

func (svc *Service) CloseMR(repo *gitmodule.Repository, user *User, mergeId int) (*apistructs.MergeRequestInfo, error) {
	var mergeRequest MergeRequest
	err := svc.db.Where("repo_id=? and repo_merge_id=?", repo.ID, mergeId).First(&mergeRequest).Error
	if err != nil {
		return nil, err
	}

	if mergeRequest.State == MERGE_REQUEST_CLOSED {
		return nil, errors.New("invalid state " + mergeRequest.State)
	}

	if mergeRequest.AuthorId != user.Id {
		err = svc.CheckPermission(repo, user, PermissionCloseMR, getMrUserRole(mergeRequest, user.Id))
		if err != nil {
			return nil, err
		}
	}

	now := time.Now()
	mergeRequest.State = MERGE_REQUEST_CLOSED
	mergeRequest.CloseAt = &now
	mergeRequest.CloseUserId = user.Id
	err = svc.db.Save(&mergeRequest).Error
	if err != nil {
		return nil, err
	}

	return mergeRequest.ToInfo(repo), nil
}

func (svc *Service) ReopenMR(repo *gitmodule.Repository, user *User, mergeId int) (*apistructs.MergeRequestInfo, error) {
	var mergeRequest MergeRequest
	err := svc.db.Where("repo_id=? and repo_merge_id=?", repo.ID, mergeId).First(&mergeRequest).Error
	if err != nil {
		return nil, err
	}

	if mergeRequest.State != MERGE_REQUEST_CLOSED {
		return nil, errors.New("invalid state " + mergeRequest.State)
	}

	mergeRequest.State = MERGE_REQUEST_OPEN
	mergeRequest.AuthorId = user.Id
	err = svc.db.Save(&mergeRequest).Error
	if err != nil {
		return nil, err
	}

	return mergeRequest.ToInfo(repo), nil
}
func (svc *Service) RemoveMR(repository *Repo) error {
	req := &MergeRequest{}
	svc.db.Where("repo_id =? ", repository.ID).Delete(&req)
	svc.RemoveCheckRuns(req.ID)
	return nil
}

func (svc *Service) CountMR(repo *gitmodule.Repository, state string) (int, error) {
	var count int
	query := svc.db.Model(&MergeRequest{}).Where("repo_id = ? ", repo.ID)
	if state != "all" && state != "" {
		query = query.Where("state = ?", state)
	}

	err := query.Count(&count).Error
	if err != nil {
		return 0, err
	}
	return count, nil
}

func getMrUserRole(mergeRequest MergeRequest, userID string) []string {
	var roleList []string
	if mergeRequest.AuthorId == userID {
		roleList = append(roleList, "Creator")
	}
	if mergeRequest.AssigneeId == userID {
		roleList = append(roleList, "Assigner")
	}
	return roleList
}

type MergeRequestState string

const (
	OpenState   MergeRequestState = "open"
	ClosedState MergeRequestState = "closed"
	MergedState MergeRequestState = "merged"
)

var MergeRequestStates = []MergeRequestState{"open", "closed", "merged"}

func (s MergeRequestState) String() string {
	return string(s)
}

type QueryMergeRequestsStatsResult struct {
	Stats []MergeRequestsStats `json:"stats"`
}

type MergeRequestsStats struct {
	State MergeRequestState `json:"state"`
	Total int               `json:"total"`
}

func (svc *Service) QueryMergeRequestsStats(repo *gitmodule.Repository, queryCondition *apistructs.GittarQueryMrRequest) (*QueryMergeRequestsStatsResult, error) {
	var results []MergeRequestsStats
	query := svc.db.Table("dice_repo_merge_requests").Select("state,count(*) AS total").
		Where("repo_id =? ", repo.ID)
	if queryCondition.Query != "" {
		mergeID, err := strconv.ParseInt(queryCondition.Query, 10, 64)
		if err == nil {
			query = query.Where("title like ? or repo_merge_id = ?", "%"+queryCondition.Query+"%", mergeID)
		} else {
			query = query.Where("title like ?", "%"+queryCondition.Query+"%")
		}
	}
	if queryCondition.AuthorId != "" {
		query = query.Where("author_id = ?", queryCondition.AuthorId)
	}

	if queryCondition.AssigneeId != "" {
		query = query.Where("assignee_id = ?", queryCondition.AssigneeId)
	}
	if err := query.Group("state").Find(&results).Error; err != nil {
		return nil, err
	}

	stats := make([]MergeRequestsStats, 0, len(MergeRequestStates))
	for _, state := range MergeRequestStates {
		total := 0
		for _, result := range results {
			if state == result.State {
				total = result.Total
				break
			}
		}
		stats = append(stats, MergeRequestsStats{
			State: state,
			Total: total,
		})
	}

	return &QueryMergeRequestsStatsResult{Stats: stats}, nil
}

func (svc *Service) UpdateJoinTempBranchStatus(id int, status string, isJoinTempStatus *bool) error {
	m := make(map[string]interface{})
	m["join_temp_branch_status"] = status
	if isJoinTempStatus != nil {
		m["is_join_temp_branch"] = *isJoinTempStatus
	}
	err := svc.db.Model(&MergeRequest{}).Where("id = ? ", id).Updates(m).Error
	return err
}
