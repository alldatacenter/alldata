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

package dao

import (
	"encoding/json"
	"time"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/core/legacy/model"
)

func (client *DBClient) CreateNotifyHistory(request *apistructs.CreateNotifyHistoryRequest) (int64, error) {
	targetData, _ := json.Marshal(request.NotifyTargets)
	sourceData, _ := json.Marshal(request.NotifySource)
	history := model.NotifyHistory{
		NotifyName:            request.NotifyName,
		NotifyItemDisplayName: request.NotifyItemDisplayName,
		Channel:               request.Channel,
		TargetData:            string(targetData),
		SourceData:            string(sourceData),
		Status:                request.Status,
		OrgID:                 request.OrgID,
		Label:                 request.Label,
		ClusterName:           request.ClusterName,
		SourceType:            request.NotifySource.SourceType,
		SourceID:              request.NotifySource.SourceID,
		ErrorMsg:              request.ErrorMsg,
	}
	err := client.Save(&history).Error
	if err != nil {
		return 0, err
	}
	return history.ID, nil
}

func (client *DBClient) QueryNotifyHistories(request *apistructs.QueryNotifyHistoryRequest) (*apistructs.QueryNotifyHistoryData, error) {
	var notifyHistories []model.NotifyHistory
	query := client.Model(&model.NotifyHistory{}).Where("org_id = ?", request.OrgID)

	if request.Label != "" {
		query = query.Where("label = ?", request.Label)
	}
	if request.Channel != "" {
		query = query.Where("channel =?", request.Channel)
	}
	if request.NotifyName != "" {
		query = query.Where("notify_name =?", request.NotifyName)
	}
	if request.ClusterName != "" {
		query = query.Where("cluster_name =?", request.ClusterName)
	}

	timeFormat := "2006-01-02 15:04:05"
	loc, _ := time.LoadLocation("Local")
	if request.StartTime != "" {
		startTime, err := time.Parse(time.RFC3339, request.StartTime)
		if err != nil {
			startTime, err = time.ParseInLocation(timeFormat, request.StartTime, loc)
		}
		if err == nil {
			query = query.Where("created_at >= ?", startTime)
		}
	}

	if request.EndTime != "" {
		endTime, err := time.Parse(time.RFC3339, request.EndTime)
		if err != nil {
			endTime, err = time.ParseInLocation(timeFormat, request.EndTime, loc)
		}
		if err == nil {
			query = query.Where("created_at <= ?", endTime)
		}
	}

	var count int
	err := query.Count(&count).Error
	if err != nil {
		return nil, err
	}

	err = query.Order("created_at desc").
		Offset((request.PageNo - 1) * request.PageSize).
		Limit(request.PageSize).
		Find(&notifyHistories).Error
	if err != nil {
		return nil, err
	}

	result := &apistructs.QueryNotifyHistoryData{
		Total: count,
		List:  []*apistructs.NotifyHistory{},
	}
	for _, notifyHistory := range notifyHistories {
		result.List = append(result.List, notifyHistory.ToApiData())
	}
	return result, nil
}
