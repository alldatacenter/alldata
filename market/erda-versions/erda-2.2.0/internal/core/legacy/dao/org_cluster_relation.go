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
	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda/internal/core/legacy/model"
)

//CreateOrgClusterRelation 创建企业集群关系
func (client *DBClient) CreateOrgClusterRelation(relation *model.OrgClusterRelation) error {
	return client.Create(relation).Error
}

// DeleteOrgClusterRelationByCluster 根据 clusterName 删除企业集群关联关系
func (client *DBClient) DeleteOrgClusterRelationByCluster(clusterName string) error {
	return client.Where("cluster_name = ?", clusterName).Delete(&model.OrgClusterRelation{}).Error
}

// DeleteOrgClusterRelationByClusterAndOrg 根据 clusterName、orgID 删除企业集群关联关系
func (client *DBClient) DeleteOrgClusterRelationByClusterAndOrg(clusterName string, orgID int64) error {
	return client.Where("cluster_name = ?", clusterName).Where("org_id = ?", orgID).Delete(&model.OrgClusterRelation{}).Error
}

// GetOrgClusterRelationByOrgAndCluster 获取企业集群关系
func (client *DBClient) GetOrgClusterRelationByOrgAndCluster(orgID, clusterID int64) (*model.OrgClusterRelation, error) {
	var relation model.OrgClusterRelation
	if err := client.Where("org_id = ?", orgID).
		Where("cluster_id = ?", clusterID).Find(&relation).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil
		}
		return nil, err
	}
	return &relation, nil
}

// GetOrgClusterRelationsByOrg 根据 orgID 获取企业对应集群关系
func (client *DBClient) GetOrgClusterRelationsByOrg(orgID int64) ([]model.OrgClusterRelation, error) {
	var relations []model.OrgClusterRelation
	if err := client.Where("org_id = ?", orgID).Find(&relations).Error; err != nil {
		return nil, err
	}
	return relations, nil
}

// GetOrgClusterRelationsByCluster 根据 cluster_name 获取企业对应集群关系
func (client *DBClient) GetOrgClusterRelationsByCluster(clusterName string) ([]model.OrgClusterRelation, error) {
	var relations []model.OrgClusterRelation
	if err := client.Where("cluster_name = ?", clusterName).Find(&relations).Error; err != nil {
		return nil, err
	}
	return relations, nil
}

// ListAllOrgClusterRelations 获取所有企业对应集群关系
func (client *DBClient) ListAllOrgClusterRelations() ([]model.OrgClusterRelation, error) {
	var relations []model.OrgClusterRelation
	if err := client.Find(&relations).Error; err != nil {
		return nil, err
	}
	return relations, nil
}
