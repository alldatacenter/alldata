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

package publish_item

import (
	"fmt"
	"log"
	"net/http"
	"strings"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/dicehub/conf"
	"github.com/erda-project/erda/internal/apps/dop/dicehub/dbclient"
	"github.com/erda-project/erda/internal/apps/dop/dicehub/service/apierrors"
	"github.com/erda-project/erda/pkg/strutil"
)

// PublishItem
type PublishItem struct {
	db  *dbclient.DBClient
	bdl *bundle.Bundle
}

// Option 定义 PublishItem 对象的配置选项
type Option func(*PublishItem)

// New 新建 PublishItem 实例，操作 PublishItem 资源
func New(options ...Option) *PublishItem {
	app := &PublishItem{}
	for _, op := range options {
		op(app)
	}
	return app
}

// WithDBClient 配置 db client
func WithDBClient(db *dbclient.DBClient) Option {
	return func(a *PublishItem) {
		a.db = db
	}
}

// WithBundle 配置 bundle
func WithBundle(bdl *bundle.Bundle) Option {
	return func(a *PublishItem) {
		a.bdl = bdl
	}
}

// CreatePublishItem 创建发布内容
func (i *PublishItem) CreatePublishItem(req *apistructs.CreatePublishItemRequest) (*apistructs.PublishItem, error) {
	if req.Name == "" {
		return nil, apierrors.ErrCreatePublishItem.InvalidParameter("name is null")
	}
	if len(req.Name) > 50 {
		return nil, apierrors.ErrCreatePublishItem.InvalidParameter("name too long,limit 50")
	}
	queryNameResult, err := i.db.QueryPublishItem(&apistructs.QueryPublishItemRequest{
		PageNo:      0,
		PageSize:    0,
		PublisherId: req.PublisherID,
		Name:        req.Name,
		OrgID:       req.OrgID,
	})
	if err != nil {
		return nil, apierrors.ErrCreatePublishItem.InternalError(err)
	}
	if queryNameResult.Total > 0 {
		return nil, apierrors.ErrCreatePublishItem.InvalidParameter("name already exist")
	}

	publishItem := dbclient.PublishItem{
		Name:             req.Name,
		PublisherID:      req.PublisherID,
		Type:             req.Type,
		Logo:             req.Logo,
		Public:           req.Public,
		DisplayName:      req.DisplayName,
		OrgID:            req.OrgID,
		Desc:             req.Desc,
		Creator:          req.Creator,
		AK:               i.db.GeneratePublishItemKey(),
		AI:               req.Name,
		NoJailbreak:      req.NoJailbreak,
		GeofenceLon:      req.GeofenceLon,
		GeofenceLat:      req.GeofenceLat,
		GeofenceRadius:   req.GeofenceRadius,
		GrayLevelPercent: req.GrayLevelPercent,
		PreviewImages:    strings.Join(req.PreviewImages, ","),
		BackgroundImage:  req.BackgroundImage,
	}
	err = i.db.CreatePublishItem(&publishItem)
	if err != nil {
		return nil, err
	}
	// 创建流水线ak，ai的配置挪到了更新 publishItem 和 app的关联关系处
	// internal/cmdb/services/application/appliation.go UpdatePublishItemRelations
	// if err := i.PipelineCmsConfigRequest(&publishItem); err != nil {
	// 	return nil, err
	// }

	return publishItem.ToApiData(), nil
}

// GetPublishItem 获取发布内容详情
func (i *PublishItem) GetPublishItem(id int64) (*apistructs.PublishItem, error) {
	publishItem, err := i.db.GetPublishItem(id)
	if err != nil {
		return nil, err
	}
	result := publishItem.ToApiData()
	result.DownloadUrl = fmt.Sprintf("%s/download/%d", conf.SiteUrl(), result.ID)
	return result, nil
}

// GetPublishItem
func (i *PublishItem) GetPublishItemDistribution(id int64, mobileType apistructs.ResourceType, packageName string,
	w http.ResponseWriter, r *http.Request) (*apistructs.PublishItemDistributionData, error) {
	publishItem, err := i.db.GetPublishItem(id)
	if err != nil {
		return nil, err
	}
	result := &apistructs.PublishItemDistributionData{
		Name:            publishItem.Name,
		DisplayName:     publishItem.DisplayName,
		Desc:            publishItem.Desc,
		Logo:            publishItem.Logo,
		CreatedAt:       publishItem.CreatedAt,
		PreviewImages:   strutil.SplitIfEmptyString(publishItem.PreviewImages, ","), // 预览图
		BackgroundImage: publishItem.BackgroundImage,                                // 背景图
	}
	if mobileType == "" {
		result.Versions = &apistructs.QueryPublishItemVersionData{List: []*apistructs.PublishItemVersion{}, Total: 0}
		return result, nil
	}

	versions, err := i.db.QueryPublishItemVersions(&apistructs.QueryPublishItemVersionRequest{
		Public:      "true",
		PageNo:      1,
		PageSize:    10,
		ItemID:      int64(publishItem.ID),
		MobileType:  mobileType,
		PackageName: packageName,
	})
	if err != nil {
		return nil, err
	}
	result.Versions = versions

	if publishItem.Type == apistructs.PublishItemTypeMobile {
		// 移动应用灰度分发
		err = i.GrayDistribution(w, r, *publishItem, result, mobileType, packageName)
		if err != nil {
			return nil, err
		}
	}

	return result, nil
}

// UpdatePublishItem 更新发布内容
func (i *PublishItem) UpdatePublishItem(req *apistructs.UpdatePublishItemRequest) error {
	item, err := i.db.GetPublishItem(req.ID)
	if err != nil {
		return err
	}
	item.Desc = req.Desc
	item.Public = req.Public
	item.Logo = req.Logo
	item.DisplayName = req.DisplayName
	item.GeofenceLat = req.GeofenceLat
	item.GeofenceLon = req.GeofenceLon
	item.GeofenceRadius = req.GeofenceRadius
	item.NoJailbreak = req.NoJailbreak
	item.GrayLevelPercent = req.GrayLevelPercent
	item.PreviewImages = strings.Join(req.PreviewImages, ",")
	item.BackgroundImage = req.BackgroundImage
	return i.db.UpdatePublishItem(item)
}

// DeletePublishItem 删除发布内容
func (i *PublishItem) DeletePublishItem(id int64) error {
	item, err := i.db.GetPublishItem(id)
	if err != nil {
		return err
	}
	err = i.bdl.RemoveAppPublishItemRelations(int64(item.ID))
	if err != nil {
		return err
	}
	err = i.db.DeletePublishItemVersionsByItemID(id)
	if err != nil {
		return err
	}
	return i.db.Delete(item).Error
}

// QueryPublishItems 查询发布内容
func (i *PublishItem) QueryPublishItems(req *apistructs.QueryPublishItemRequest) (*apistructs.QueryPublishItemData, error) {
	queryPublishItemResult, err := i.db.QueryPublishItem(req)
	if err != nil {
		return nil, err
	}
	for _, item := range queryPublishItemResult.List {
		item.DownloadUrl = fmt.Sprintf("%s/download/%d", conf.SiteUrl(), item.ID)
		if item.Type == string(apistructs.ApplicationModeLibrary) {
			versions, err := i.QueryPublishItemVersions(&apistructs.QueryPublishItemVersionRequest{
				Public:   "true",
				ItemID:   item.ID,
				OrgID:    item.OrgID,
				PageSize: 1,
			})
			if err == nil && len(versions.List) > 0 {
				item.LatestVersion = versions.List[0].Version
			}
		}
	}

	return queryPublishItemResult, nil
}

// PipelineCmsConfigRequest 请求pipeline cms，将publisherKey和publishItemKey设置进配置管理
// func (i *PublishItem) PipelineCmsConfigRequest(publishItem *dbclient.PublishItem) error {
// 	if publishItem != nil {
// 		publisher, err := i.bdl.FetchPublisher(uint64(publishItem.PublisherID))
// 		if err != nil {
// 			return err
// 		}
// 		// bundle req
// 		var req = apistructs.PipelineCmsUpdateConfigsRequest{}
// 		var valueMap = make(map[string]apistructs.PipelineCmsConfigValue, 2)
// 		valueMap["AK"] = apistructs.PipelineCmsConfigValue{
// 			Value: publisher.PublisherKey,
// 		}
// 		valueMap["AI"] = apistructs.PipelineCmsConfigValue{
// 			Value: publishItem.PublishItemKey,
// 		}
// 		req.KVs = valueMap
// 		req.PipelineSource = apistructs.PipelineSourceDice
// 		if err := i.bdl.CreateOrUpdatePipelineCmsNsConfigs(buildPipelineCmsNs(publishItem.ID), req); err != nil {
// 			return err
// 		}
// 	}
// 	return nil
// }

// 生成namespace
func buildPipelineCmsNs(itemID uint64) string {
	return fmt.Sprintf("publish-item-monitor-%d", itemID)
}

// Migration320 3.20 灰度逻辑迁移，3.21删除
func (i *PublishItem) Migration320() {
	iterms, err := i.db.GetALLItem()
	if err != nil {
		log.Fatal("migration publish item gray error")
	}

	// 迁移该 publishItem 的灰度逻辑
	for _, item := range iterms {
		if !item.IsMigration {
			if err := i.db.MigrationFordice320(int64(item.ID)); err != nil {
				log.Fatalf("migration publish item %v gray error", item.Name)
			}
		}
	}
}
