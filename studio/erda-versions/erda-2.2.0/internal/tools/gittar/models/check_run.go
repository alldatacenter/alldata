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
	"encoding/json"
	"strconv"
	"time"

	"github.com/jinzhu/gorm"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/gittar/pkg/gitmodule"
)

type CheckRun struct {
	ID          int64                     `json:"id"`
	MrID        int64                     `json:"mrId"`
	Name        string                    `json:"name"`       // 名称 golang-lint/test
	Type        string                    `json:"type"`       // 类型 CI
	ExternalID  string                    `json:"externalId"` // 外部系统 ID
	PipelineID  string                    `json:"pipelineId"` // 流水线 ID
	Commit      string                    `json:"commit"`
	Status      apistructs.CheckRunStatus `json:"status"` //progress/completed
	Result      apistructs.CheckRunResult `json:"result"` // success failed cancel
	Output      string                    `json:"output"`
	CreatedAt   time.Time                 `json:"createdAt"`
	CompletedAt *time.Time                `json:"completedAt"`
	RepoID      int64                     `json:"repoId"`
}

type CheckRuns struct {
	CheckRun []*CheckRun               `json:"checkrun"`
	Result   apistructs.CheckRunResult `json:"result"`
	Mergable bool                      `json:"mergable"`
}

func (svc *Service) CreateOrUpdateCheckRun(repo *gitmodule.Repository, request *apistructs.CheckRun) (*apistructs.CheckRun, error) {
	var checkRun CheckRun
	err := svc.db.Model(&CheckRun{}).
		Where("pipeline_id =?", request.PipelineID).
		First(&checkRun).Error
	if err == nil {
		// 已存在,更新
		checkRun.Status = request.Status
		checkRun.Result = request.Result
		checkRun.Output = request.Output
		if request.Status == apistructs.CheckRunStatusCompleted {
			now := time.Now()
			checkRun.CompletedAt = &now
		}
		err = svc.db.Save(&checkRun).Error
		request.ID = checkRun.ID
		return request, err
	} else if err == gorm.ErrRecordNotFound {
		// 不存在更新,创建新纪录
		checkRun = CheckRun{
			MrID:        request.MrID,
			Name:        request.Name,
			Type:        request.Type,
			ExternalID:  request.ExternalID,
			PipelineID:  request.PipelineID,
			Commit:      request.Commit,
			Status:      request.Status,
			Result:      request.Result,
			Output:      request.Output,
			CreatedAt:   time.Now(),
			CompletedAt: nil,
			RepoID:      repo.ID,
		}
		err := svc.db.Save(&checkRun).Error
		if err != nil {
			return nil, err
		}
		request.ID = checkRun.ID
		request.CreatedAt = checkRun.CreatedAt
		return request, nil
	} else {
		return nil, err
	}
}

func (svc *Service) QueryCheckRuns(repo *gitmodule.Repository, mrID string) (apistructs.CheckRuns, error) {
	var checkRuns []*CheckRun
	id, err := strconv.Atoi(mrID)
	if err != nil {
		return apistructs.CheckRuns{}, err
	}
	mergeRequestInfo, err := svc.GetMergeRequestDetail(repo, id)
	if err != nil {
		return apistructs.CheckRuns{}, err
	}
	query := svc.db.Model(&CheckRun{}).Where("mr_id =? and repo_id =? and commit =?", mrID, repo.ID, mergeRequestInfo.SourceSha)
	err = query.Find(&checkRuns).Error
	if err != nil {
		return apistructs.CheckRuns{}, err
	}

	res := CheckRuns{
		CheckRun: checkRuns,
		Result:   apistructs.CheckRunResultSuccess,
		Mergable: true,
	}
	for _, each := range res.CheckRun {
		if each.Status == apistructs.CheckRunStatusInProgress {
			res.Mergable = false
			continue
		}
		if each.Result != apistructs.CheckRunResultSuccess {
			res.Result = apistructs.CheckRunResultFailure
		}
	}
	var state apistructs.CheckRuns
	cont, err := json.Marshal(res)
	if err != nil {
		logrus.Errorf("marshal component state failed, content:%v, err:%v", res, err)
		return apistructs.CheckRuns{}, err
	}
	err = json.Unmarshal(cont, &state)
	if err != nil {
		logrus.Errorf("unmarshal component state failed, content:%v, err:%v", cont, err)
		return apistructs.CheckRuns{}, err
	}
	return state, err
}

func (svc *Service) IsCheckRunsValid(repo *gitmodule.Repository, mrID int64) (bool, error) {
	query := svc.db.Model(&CheckRun{}).Where("mr_id =? and (result <> ? or status <> ?)",
		mrID, apistructs.CheckRunResultSuccess, apistructs.CheckRunStatusCompleted)
	var count int64
	err := query.Count(&count).Error
	if err != nil {
		return false, err
	}
	return count == 0, nil
}

func (svc *Service) RemoveCheckRuns(mrID int64) error {
	svc.db.Where("mr_id =? ", mrID).Delete(&CheckRun{})
	return nil
}
