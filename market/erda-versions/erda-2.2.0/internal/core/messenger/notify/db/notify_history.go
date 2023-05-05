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

package db

import (
	"encoding/json"
	"fmt"
	"strconv"
	"time"

	"github.com/jinzhu/gorm"
	"google.golang.org/protobuf/types/known/timestamppb"

	"github.com/erda-project/erda-proto-go/core/messenger/notify/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/core/messenger/notify/model"
)

type NotifyHistoryDB struct {
	*gorm.DB
}

type NotifyHistory struct {
	model.BaseModel
	NotifyName            string `gorm:"size:150;index:idx_notify_name"`
	NotifyItemDisplayName string `gorm:"size:150"`
	Channel               string `gorm:"size:150"`
	TargetData            string `gorm:"type:text"`
	SourceData            string `gorm:"type:text"`
	Status                string `gorm:"size:150"`
	OrgID                 int64  `gorm:"index:idx_org_id"`
	SourceType            string `gorm:"size:150"`
	SourceID              string `gorm:"size:150"`
	ErrorMsg              string `gorm:"type:text"`
	// 模块类型 cdp/workbench/monitor
	Label       string `gorm:"size:150;index:idx_module"`
	ClusterName string
}

// TableName 设置模型对应数据库表名称
func (NotifyHistory) TableName() string {
	return "dice_notify_histories"
}

func (notifyHistory *NotifyHistory) ToApiData() (*pb.NotifyHistory, error) {
	var (
		targets    []*pb.NotifyTarget
		oldTargets []apistructs.OldNotifyTarget
		source     *pb.NotifySource
	)
	if notifyHistory.TargetData != "" {
		if err := json.Unmarshal([]byte(notifyHistory.TargetData), &targets); err != nil {
			// 兼容老数据
			json.Unmarshal([]byte(notifyHistory.TargetData), &oldTargets)
			for _, v := range oldTargets {
				data, err := json.Marshal(v)
				if err != nil {
					return nil, err
				}
				var target pb.NotifyTarget
				err = json.Unmarshal(data, &target)
				if err != nil {
					return nil, err
				}
				targets = append(targets, &target)
			}
		}
	}

	timestamppb.New(notifyHistory.CreatedAt)
	if notifyHistory.SourceData != "" {
		json.Unmarshal([]byte(notifyHistory.SourceData), &source)
	}
	data := &pb.NotifyHistory{
		Id:                    notifyHistory.ID,
		NotifyName:            notifyHistory.NotifyName,
		NotifyItemDisplayName: notifyHistory.NotifyItemDisplayName,
		Channel:               notifyHistory.Channel,
		CreatedAt:             timestamppb.New(notifyHistory.CreatedAt),
		NotifyTargets:         targets,
		NotifySource:          source,
		Status:                notifyHistory.Status,
		Label:                 notifyHistory.Label,
		ErrorMsg:              notifyHistory.ErrorMsg,
	}
	return data, nil
}

func (db *NotifyHistoryDB) CreateNotifyHistory(request *NotifyHistory) (*NotifyHistory, error) {
	err := db.Save(request).Error
	if err != nil {
		return nil, err
	}
	return request, nil
}

func (db *NotifyHistoryDB) QueryNotifyHistories(request *model.QueryNotifyHistoriesRequest) ([]NotifyHistory, int64, error) {
	var notifyHistories []NotifyHistory
	query := db.Model(&NotifyHistory{}).Where("org_id = ?", request.OrgID)
	if request.Label != "" {
		query = query.Where("label = ?", request.Label)
	}
	if request.Channel != "" {
		query = query.Where("channel = ?", request.Channel)
	}
	if request.NotifyName != "" {
		query = query.Where("notify_name = ?", request.NotifyName)
	}
	if request.ClusterName != "" {
		query = query.Where("cluster_name = ?", request.ClusterName)
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
		return nil, 0, err
	}
	err = query.Order("created_at desc").
		Offset((request.PageNo - 1) * request.PageSize).
		Limit(request.PageSize).Find(&notifyHistories).Error
	if err != nil {
		return nil, 0, err
	}
	return notifyHistories, int64(count), nil
}

func (db *NotifyHistoryDB) FilterStatus(request *model.FilterStatusRequest) ([]*model.FilterStatusResult, error) {
	result := make([]*model.FilterStatusResult, 0)
	startTime, err := ToTime(request.StartTime)
	if err != nil {
		return nil, err
	}
	endTime, err := ToTime(request.EndTime)
	if err != nil {
		return nil, err
	}
	err = db.Model(&NotifyHistory{}).Select("status,count(1) as count").
		Where("source_type = ?", request.ScopeType).
		Where("source_id = ?", request.ScopeId).
		Where("created_at >= ?", startTime).Where("created_at <= ?", endTime).
		Group("status").Scan(&result).Error
	if err != nil {
		return nil, err
	}
	return result, nil
}

func ToTime(timestampStr string) (time.Time, error) {
	timestampInt, err := strconv.ParseInt(timestampStr, 10, 64)
	if err != nil {
		return time.Time{}, err
	}
	tm := time.Unix(0, timestampInt*int64(time.Millisecond))
	tm.Format("2006-01-02 15:04:05")
	return tm, nil
}

func (db *NotifyHistoryDB) QueryNotifyValue(key string, orgId int, scopeId, scopeType string, interval, startTime, endTime int64) ([]*model.NotifyValue, error) {
	result := make([]*model.NotifyValue, 0)
	sTime := time.Unix(0, startTime*int64(time.Millisecond))
	eTime := time.Unix(0, endTime*int64(time.Millisecond))
	err := db.Model(&NotifyHistory{}).Select(fmt.Sprintf("FROM_UNIXTIME(UNIX_TIMESTAMP(created_at) - MOD(UNIX_TIMESTAMP(created_at),%v)) as round_time,%s as field,count(*) as count", interval/1000, key)).
		Where("created_at >= ?", sTime).
		Where("created_at <= ?", eTime).
		Where("org_id = ?", orgId).
		Where("source_id = ?", scopeId).
		Where("source_type = ?", scopeType).
		Group("round_time,field").
		Scan(&result).Error
	if err != nil {
		return nil, err
	}
	return result, nil
}

func (db *NotifyHistoryDB) NotifyHistoryType(key string, orgId int, scopeId, scopeType string, startTime, endTime int64) ([]*model.NotifyValue, error) {
	db.LogMode(true)
	result := make([]*model.NotifyValue, 0)
	sTime := time.Unix(0, startTime*int64(time.Millisecond))
	eTime := time.Unix(0, endTime*int64(time.Millisecond))
	err := db.Model(&NotifyHistory{}).Select(fmt.Sprintf("%s as field,count(*) as count", key)).
		Where("created_at >= ?", sTime).
		Where("created_at <= ?", eTime).
		Where("org_id = ?", orgId).
		Where("source_id = ?", scopeId).
		Where("source_type = ?", scopeType).
		Group("field").
		Scan(&result).Error
	if err != nil {
		return nil, err
	}
	return result, nil
}

func (db *NotifyHistoryDB) GetAlertNotifyHistory(id int64) (*NotifyHistory, error) {
	var notifyHistory NotifyHistory
	err := db.Where("id = ?", id).Find(&notifyHistory).Error
	if err != nil {
		return nil, err
	}
	return &notifyHistory, nil
}
