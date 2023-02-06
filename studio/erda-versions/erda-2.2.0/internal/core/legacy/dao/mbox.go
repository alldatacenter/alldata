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
	"time"

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/core/legacy/model"
)

func (client *DBClient) CreateMBox(mbox *model.MBox) error {
	return client.Create(&mbox).Error
}

// UpdateMbox update mbox
func (client *DBClient) UpdateMbox(mbox *model.MBox) error {
	return client.Save(mbox).Error
}

func (client *DBClient) DeleteMBox(id int64) error {
	var mbox model.MBox
	err := client.Where("id = ?", id).Find(&mbox).Error
	if err != nil {
		return err
	}
	return client.Delete(&mbox).Error
}

func (client *DBClient) SetMBoxReadStatus(request *apistructs.SetMBoxReadStatusRequest) error {
	now := time.Now()
	return client.Model(&model.MBox{}).
		Where("status =?", apistructs.MBoxUnReadStatus).
		Where("id in (?) and org_id=? and user_id =?", request.IDs, request.OrgID, request.UserID).
		Updates(map[string]interface{}{"read_at": &now, "status": apistructs.MBoxReadStatus,
			"unread_count": 0}).Error
}

func (client *DBClient) GetMBoxStats(orgID int64, userID string) (*apistructs.QueryMBoxStatsData, error) {
	var count, unReadCount int64
	if err := client.Model(&model.MBox{}).Where("org_id=? and user_id =?", orgID, userID).
		Where("status =? ", apistructs.MBoxUnReadStatus).Count(&count).Error; err != nil {
		return nil, err
	}
	var mbox []model.MBox
	if err := client.Model(&model.MBox{}).Where("org_id=? and user_id =?", orgID, userID).
		Where("status =? ", apistructs.MBoxUnReadStatus).Where("unread_count > 1 and deduplicate_id != \"\"").
		Find(&mbox).Count(&unReadCount).Error; err != nil {
		return nil, err
	}
	count = count - unReadCount
	for _, v := range mbox {
		count = count + v.UnreadCount
	}
	return &apistructs.QueryMBoxStatsData{
		UnreadCount: count,
	}, nil
}

func (client *DBClient) GetMBox(id int64, orgID int64, userID string) (*apistructs.MBox, error) {
	var mbox model.MBox
	err := client.Where("id =? and org_id=? and user_id =?", id, orgID, userID).First(&mbox).Error
	if err != nil {
		return nil, err
	}
	return mbox.ToApiData(), nil
}

func (client *DBClient) QueryMBox(request *apistructs.QueryMBoxRequest) (*apistructs.QueryMBoxData, error) {
	var mboxList []model.MBox
	query := client.Model(&model.MBox{}).Where("org_id =? and user_id= ?", request.OrgID, request.UserID).Order("created_at desc")
	if request.Status != "" {
		query = query.Where("status =? ", request.Status)
	}
	if request.Type == apistructs.MBoxTypeIssue {
		query = query.Where("deduplicate_id != \"\"")
	} else {
		query = query.Select("id,title,status,label,created_at,read_at,deduplicate_id,unread_count")
	}

	var (
		count int
		total int
	)
	err := query.Count(&count).Error
	if err != nil {
		return nil, err
	}
	total = count

	err = query.Offset((request.PageNo - 1) * request.PageSize).
		Limit(request.PageSize).
		Find(&mboxList).Error
	if err != nil {
		return nil, err
	}

	var (
		unReadMboxs []model.MBox
		unReadCount int
	)
	if err := client.Model(&model.MBox{}).Where("org_id=? and user_id =?", request.OrgID, request.UserID).
		Where("status =? ", apistructs.MBoxUnReadStatus).Where("unread_count > 1 and deduplicate_id != \"\"").
		Find(&unReadMboxs).Count(&unReadCount).Error; err != nil {
		return nil, err
	}
	count = count - unReadCount
	for _, v := range unReadMboxs {
		count = count + int(v.UnreadCount)
	}

	result := &apistructs.QueryMBoxData{
		Total:  total,
		UnRead: count,
		List:   []*apistructs.MBox{},
	}
	for _, mboxItem := range mboxList {
		result.List = append(result.List, mboxItem.ToApiData())
	}
	return result, nil
}

// GetMboxByDeduplicateID get a mbox by deduplicate_id
func (client *DBClient) GetMboxByDeduplicateID(orgID int64, deduplicateID, userID string) (*model.MBox, error) {
	var mbox model.MBox
	if err := client.Where("deduplicate_id = ? and org_id = ? and user_id = ?", deduplicateID, orgID,
		userID).First(&mbox).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil
		}
		return nil, err
	}
	return &mbox, nil
}

// ReadAll read all unread mbox with one click
func (client *DBClient) ReadAll(orgID int64, userID string) error {
	now := time.Now()
	return client.Model(&model.MBox{}).Where("status = ? and user_id = ? and org_id = ?", apistructs.MBoxUnReadStatus,
		userID, orgID).Updates(map[string]interface{}{"read_at": &now, "status": apistructs.MBoxReadStatus,
		"unread_count": 0}).Error
}
