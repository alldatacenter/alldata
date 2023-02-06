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
	"encoding/json"
	"strconv"

	"github.com/ghodss/yaml"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/database/dbengine"
	"github.com/erda-project/erda/pkg/strutil"
)

// PublishItemVersion 发布版本
type PublishItemVersion struct {
	dbengine.BaseModel
	Version          string
	BuildID          string `gorm:"column:build_id"`
	PackageName      string `gorm:"column:package_name"`
	Public           bool
	IsDefault        bool
	Logo             string
	Spec             string `gorm:"type:longtext"`
	Swagger          string `gorm:"type:longtext"`
	Readme           string `gorm:"type:longtext"`
	Desc             string //版本描述信息
	Resources        string //版本资源信息
	Meta             string // 元信息，应用 项目 release id等
	OrgID            int64
	PublishItemID    int64
	MobileType       string `gorm:"column:mobile_type"` // ios, android, h5
	Creator          string
	VersionStates    apistructs.PublishItemVersionStates `gorm:"column:version_states"`
	GrayLevelPercent int                                 `gorm:"column:gray_level_percent"` // 灰度百分比，0-100
}

// TableName 设置模型对应数据库表名称
func (PublishItemVersion) TableName() string {
	return "dice_publish_item_versions"
}

// IsLater 校验两个版本新旧
func (publishItemVersion *PublishItemVersion) IsLater(version *PublishItemVersion) bool {
	v1, v2 := strutil.ParseVersion(publishItemVersion.Version), strutil.ParseVersion(version.Version)
	// 比较版本号
	if v1 == v2 {
		// 比较buildID
		b1, err := strconv.ParseInt(publishItemVersion.BuildID, 10, 64)
		if err != nil {
			logrus.Errorf("get buildID err: %v", err)
		}
		b2, err := strconv.ParseInt(version.BuildID, 10, 64)
		if err != nil {
			logrus.Errorf("get buildID err: %v", err)
		}
		if b1 == b2 {
			// 比较创建时间
			return publishItemVersion.CreatedAt.After(version.CreatedAt)
		}
		return b1 > b2
	}

	return v1 > v2
}

func (publishItemVersion *PublishItemVersion) ToApiData() *apistructs.PublishItemVersion {
	var resourceData interface{}
	if publishItemVersion.Resources != "" {
		json.Unmarshal([]byte(publishItemVersion.Resources), &resourceData)
	}
	var metaData map[string]interface{}
	if publishItemVersion.Meta != "" {
		json.Unmarshal([]byte(publishItemVersion.Meta), &metaData)
	}
	var swaggerData interface{}
	if publishItemVersion.Swagger != "" {
		swaggerJson, _ := yaml.YAMLToJSON([]byte(publishItemVersion.Swagger))
		json.Unmarshal(swaggerJson, &swaggerData)
	}
	return &apistructs.PublishItemVersion{
		ID:               publishItemVersion.ID,
		Version:          publishItemVersion.Version,
		BuildID:          publishItemVersion.BuildID,
		PackageName:      publishItemVersion.PackageName,
		Public:           publishItemVersion.Public,
		OrgID:            publishItemVersion.OrgID,
		IsDefault:        publishItemVersion.IsDefault,
		Desc:             publishItemVersion.Desc,
		CreatedAt:        publishItemVersion.CreatedAt,
		UpdatedAt:        publishItemVersion.UpdatedAt,
		Resources:        resourceData,
		Swagger:          swaggerData,
		Meta:             metaData,
		Logo:             publishItemVersion.Logo,
		Spec:             publishItemVersion.Spec,
		Readme:           publishItemVersion.Readme,
		MobileType:       publishItemVersion.MobileType,
		VersionStates:    publishItemVersion.VersionStates,
		GrayLevelPercent: publishItemVersion.GrayLevelPercent,
	}
}

func (client *DBClient) GetPublishItemVersion(id int64) (*PublishItemVersion, error) {
	var itemVersion PublishItemVersion
	err := client.Where("id = ?", id).First(&itemVersion).Error
	if err != nil {
		return nil, err
	}
	return &itemVersion, nil
}

func (client *DBClient) GetPublishItemVersionByName(orgId int64, itemID int64, mobileType apistructs.ResourceType,
	versionInfo apistructs.VersionInfo) (*PublishItemVersion, error) {
	// ios android 版本不应区分包名
	packageName := versionInfo.PackageName
	if mobileType != apistructs.ResourceTypeH5 {
		packageName = ""
	}

	var itemVersion PublishItemVersion
	db := client.Where("org_id =? and publish_item_id = ? and version = ? and mobile_type = ? and build_id = ?", orgId,
		itemID, versionInfo.Version, mobileType, versionInfo.BuildID)
	if packageName != "" {
		db = db.Where("package_name = ?", packageName)
	}
	if err := db.First(&itemVersion).Error; err != nil {
		return nil, err
	}
	return &itemVersion, nil
}

func (client *DBClient) ListPublishItemVersionByNames(orgId int64, itemID int64, versions []string, mobileType apistructs.ResourceType) ([]PublishItemVersion, error) {
	var itemVersions []PublishItemVersion
	err := client.Where("org_id =? and publish_item_id = ? and mobile_type = ?", orgId, itemID, mobileType).
		Where("version in (?)", versions).Find(&itemVersions).Error
	if err != nil {
		return nil, err
	}
	return itemVersions, nil
}

func (client *DBClient) CreatePublishItemVersion(itemVersion *PublishItemVersion) error {
	return client.Create(itemVersion).Error
}

func (client *DBClient) UpdatePublishItemVersion(itemVersion *PublishItemVersion) error {
	return client.Save(itemVersion).Error
}

func (client *DBClient) DeletePublishItemVersion(itemVersion *PublishItemVersion) error {
	return client.Delete(itemVersion).Error
}

func (client *DBClient) SetPublishItemVersionDefault(itemID, itemVersionID int64) error {
	err := client.Model(&PublishItemVersion{}).
		Where("publish_item_id =? and is_default =?", itemID, true).
		Update(map[string]interface{}{"is_default": false}).Error
	if err != nil {
		return err
	}
	return client.Model(&PublishItemVersion{}).
		Where("id =? and publish_item_id =?", itemVersionID, itemID).
		Update(map[string]interface{}{"is_default": true}).Error
}

func (client *DBClient) SetPublishItemVersionPublic(id, itemID int64) error {
	return client.Model(&PublishItemVersion{}).
		Where("id = ? and publish_item_id =?", id, itemID).
		Update(map[string]interface{}{"public": true}).Error
}

func (client *DBClient) SetPublishItemVersionUnPublic(id, itemID int64) error {
	return client.Model(&PublishItemVersion{}).
		Where("id = ? and publish_item_id =?", id, itemID).
		Update(map[string]interface{}{"public": false}).Error
}

func (client *DBClient) DeletePublishItemVersionsByItemID(itemID int64) error {
	return client.Where("publish_item_id =?", itemID).Delete(&PublishItemVersion{}).Error
}

func (client *DBClient) QueryPublishItemVersions(request *apistructs.QueryPublishItemVersionRequest) (*apistructs.QueryPublishItemVersionData, error) {
	var itemVersions []PublishItemVersion
	var count int
	query := client.Model(&PublishItemVersion{}).Where("publish_item_id = ?", request.ItemID)
	if request.Public != "" {
		public, err := strconv.ParseBool(request.Public)
		if err == nil {
			query = query.Where("public =?", public)
		}
	}
	if request.IsDefault != "" {
		isDefault, err := strconv.ParseBool(request.IsDefault)
		if err == nil {
			query = query.Where("is_default =?", isDefault)
		}
	}
	if request.MobileType != "" {
		query = query.Where("mobile_type = ?", request.MobileType)
	}
	if request.PackageName != "" {
		query = query.Where("package_name = ?", request.PackageName)
	}
	err := query.Count(&count).Error
	if err != nil {
		return nil, err
	}
	err = query.Order("created_at desc").
		Offset((request.PageNo - 1) * request.PageSize).
		Limit(request.PageSize).
		Find(&itemVersions).Error
	if err != nil {
		return nil, err
	}
	results := []*apistructs.PublishItemVersion{}
	for _, item := range itemVersions {
		result := item.ToApiData()

		if item.MobileType == string(apistructs.ResourceTypeH5) {
			targetMobile := make(map[string][]string, 0)
			targetRelations, err := client.GetTargetsByH5Version(item.ID)
			if err != nil {
				return nil, err
			}
			for _, v := range targetRelations {
				targetMobile[v.TargetMobileType] = append(targetMobile[v.TargetMobileType], v.TargetVersion)
			}
			result.TargetMobiles = targetMobile
		}

		results = append(results, result)
	}

	return &apistructs.QueryPublishItemVersionData{
		Total: count,
		List:  results,
	}, nil
}

// GetPublicVersion 获取已上架的版本信息
func (client *DBClient) GetPublicVersion(itemID int64, mobileType apistructs.ResourceType, packageName string) (int, []PublishItemVersion, error) {
	var itemVersions []PublishItemVersion
	var count int
	db := client.Model(&PublishItemVersion{}).Where("publish_item_id = ?", itemID).Where("public = ?", true).
		Where("mobile_type = ?", mobileType)
	if packageName != "" {
		db = db.Where("package_name = ?", packageName)
	}

	if err := db.Find(&itemVersions).Count(&count).Error; err != nil {
		return 0, nil, err
	}

	return count, itemVersions, nil
}

// GetH5VersionByItemID 获取H5的包名列表
func (client *DBClient) GetH5VersionByItemID(itemID int64) ([]PublishItemVersion, error) {
	var itemVersions []PublishItemVersion
	if err := client.Table("dice_publish_item_versions").Select("package_name").Where("publish_item_id = ?", itemID).
		Where("mobile_type = ?", apistructs.ResourceTypeH5).Group("package_name").
		Find(&itemVersions).Error; err != nil {
		return nil, err
	}

	return itemVersions, nil
}

// UpdatePublicVersionByID 根据id更新PublicVersion
func (client *DBClient) UpdatePublicVersionByID(versionID int64, fileds map[string]interface{}) error {
	return client.Model(&PublishItemVersion{}).Where("id = ?", versionID).Update(fileds).Error
}

// MigrationFordice320 3.20灰度逻辑迁移，待所有的 is_migration 都等于1时代码可删
func (client *DBClient) MigrationFordice320(itemID int64) error {
	var (
		item                        PublishItem
		versions                    []PublishItemVersion
		releaseVersion, betaVersion *PublishItemVersion
		publicedVersionID           []uint64
	)
	if err := client.Where("id = ?", itemID).First(&item).Error; err != nil {
		return err
	}
	graylevel := item.GrayLevelPercent

	if err := client.Where("publish_item_id = ?", itemID).Where("public = ?", true).Order("created_at desc").
		Find(&versions).Error; err != nil {
		return err
	}

	if len(versions) == 0 {
		goto LABEL2
	}

	releaseVersion = &versions[0]
	releaseVersion.Public, releaseVersion.VersionStates, releaseVersion.GrayLevelPercent = true, apistructs.PublishItemReleaseVersion, 100-graylevel
	if err := client.Save(releaseVersion).Error; err != nil {
		return err
	}
	publicedVersionID = append(publicedVersionID, releaseVersion.ID)

	if len(versions) == 1 {
		goto LABEL1
	}

	for _, v := range versions[1:] {
		if !v.IsDefault {
			betaVersion = &v
			break
		}
	}
	if betaVersion != nil {
		betaVersion.Public, betaVersion.VersionStates, betaVersion.GrayLevelPercent = true, apistructs.PublishItemBetaVersion, graylevel
		if err := client.Save(betaVersion).Error; err != nil {
			return err
		}
		publicedVersionID = append(publicedVersionID, betaVersion.ID)
	}

LABEL1:
	if err := client.Model(&PublishItemVersion{}).Where("publish_item_id = ?", itemID).
		Where("id not in (?)", publicedVersionID).
		Update(map[string]interface{}{"public": false, "gray_level_percent": 0, "version_states": ""}).
		Error; err != nil {
		return err
	}

LABEL2:
	item.IsMigration = true
	if err := client.Save(&item).Error; err != nil {
		return err
	}

	return nil
}
