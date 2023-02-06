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

package dbclient

import (
	"strconv"
	"strings"

	uuid "github.com/satori/go.uuid"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/database/dbengine"
	"github.com/erda-project/erda/pkg/strutil"
)

// PublishItem 发布
type PublishItem struct {
	dbengine.BaseModel
	Name             string
	PublisherID      int64
	Type             string
	Logo             string
	Public           bool
	DisplayName      string
	OrgID            int64 // 应用关联组织Id
	Desc             string
	Creator          string
	AK               string
	AI               string
	NoJailbreak      bool    // 是否禁止越狱设置
	GeofenceLon      float64 // 地理围栏，坐标经度
	GeofenceLat      float64 // 地理围栏，坐标纬度
	GeofenceRadius   float64 // 地理围栏，合理半径
	GrayLevelPercent int     // 灰度百分比，0-100
	IsMigration      bool    // 灰度逻辑是否已迁移到最新版本（default --> release/beta）
	PreviewImages    string  `gorm:"column:preview_images"` // 预览图
	BackgroundImage  string  `gorm:"gorm:background_image"` // 背景图
}

// TableName 设置模型对应数据库表名称
func (PublishItem) TableName() string {
	return "dice_publish_items"
}

func (publishItem *PublishItem) ToApiData() *apistructs.PublishItem {
	return &apistructs.PublishItem{
		ID:               int64(publishItem.ID),
		Name:             publishItem.Name,
		DisplayName:      publishItem.DisplayName,
		PublisherID:      publishItem.PublisherID,
		Type:             publishItem.Type,
		Public:           publishItem.Public,
		OrgID:            publishItem.OrgID,
		Desc:             publishItem.Desc,
		Logo:             publishItem.Logo,
		Creator:          publishItem.Creator,
		CreatedAt:        publishItem.CreatedAt,
		UpdatedAt:        publishItem.UpdatedAt,
		AK:               publishItem.AK,
		AI:               publishItem.AI,
		NoJailbreak:      publishItem.NoJailbreak,
		GeofenceLon:      publishItem.GeofenceLon,
		GeofenceRadius:   publishItem.GeofenceRadius,
		GeofenceLat:      publishItem.GeofenceLat,
		GrayLevelPercent: publishItem.GrayLevelPercent,
		PreviewImages:    strutil.SplitIfEmptyString(publishItem.PreviewImages, ","),
		BackgroundImage:  publishItem.BackgroundImage,
	}
}

func (client *DBClient) GetPublishItem(id int64) (*PublishItem, error) {
	var publishItem PublishItem
	err := client.Where("id = ?", id).First(&publishItem).Error
	if err != nil {
		return nil, err
	}
	return &publishItem, nil
}

func (client *DBClient) CreatePublishItem(publishItem *PublishItem) error {
	return client.Create(publishItem).Error
}

func (client *DBClient) UpdatePublishItem(publishItem *PublishItem) error {
	return client.Save(publishItem).Error
}

func (client *DBClient) UpdatePublishItemUpdateTime(publishItem *PublishItem) error {
	return client.Save(publishItem).Error
}

func (client *DBClient) DeletePublishItem(publishItem *PublishItem) error {
	return client.Delete(publishItem).Error
}

func (client *DBClient) QueryPublishItem(request *apistructs.QueryPublishItemRequest) (*apistructs.QueryPublishItemData, error) {
	var publishItems []PublishItem
	var count int
	query := client.Model(&PublishItem{})
	if request.PublisherId > 0 {
		query = query.Where("publisher_id = ?", request.PublisherId)
	}
	if request.OrgID > 0 {
		query = query.Where("org_id = ?", request.OrgID)
	}

	if request.Name != "" {
		query = query.Where("name =?", request.Name)
	}

	if request.Type != "" {
		query = query.Where("type =?", request.Type)
	}

	if request.Q != "" {
		query = query.Where("name like ?", "%"+request.Q+"%")
	}
	if request.Ids != "" {
		ids := strings.Split(request.Ids, ",")
		query = query.Where("id in (?)", ids)
	}

	if request.Public != "" {
		public, err := strconv.ParseBool(request.Public)
		if err == nil {
			query = query.Where("public =?", public)
		}
	}
	err := query.Count(&count).Error
	if err != nil {
		return nil, err
	}
	if request.PageSize > 0 {
		err = query.Order("updated_at desc").
			Offset((request.PageNo - 1) * request.PageSize).
			Limit(request.PageSize).
			Find(&publishItems).Error
		if err != nil {
			return nil, err
		}
	}

	results := []*apistructs.PublishItem{}
	for _, item := range publishItems {
		result := item.ToApiData()
		results = append(results, result)
	}

	return &apistructs.QueryPublishItemData{
		Total: count,
		List:  results,
	}, nil
}

func (client *DBClient) GetPublishItemCountByPublisher(publisherId int64) (int64, error) {
	var count int64
	if err := client.Where("publisher_id = ?", publisherId).
		Model(&PublishItem{}).Count(&count).Error; err != nil {
		return 0, err
	}
	return count, nil
}

// GetPublishItemByAKAI 通过离线包的AKAI获取publishItem信息
func (client *DBClient) GetPublishItemByAKAI(ak, ai string) (*PublishItem, error) {
	var publishItem PublishItem
	if err := client.Where("ak = ? and ai = ?", ak, ai).
		Model(&PublishItem{}).First(&publishItem).Error; err != nil {
		return nil, err
	}
	return &publishItem, nil
}

// GeneratePublishItemKey 生成itemKey
func (client *DBClient) GeneratePublishItemKey() string {
	return strings.Replace(uuid.NewV4().String(), "-", "", -1)
}

func (client *DBClient) GetALLItem() ([]PublishItem, error) {
	var publishItems []PublishItem
	if err := client.Where("is_migration = ?", 0).Find(&publishItems).Error; err != nil {
		return nil, err
	}

	return publishItems, nil
}
