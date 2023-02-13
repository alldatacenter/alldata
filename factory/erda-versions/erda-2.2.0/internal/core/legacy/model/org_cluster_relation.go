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

package model

// OrgClusterRelation 企业集群关联关系
type OrgClusterRelation struct {
	BaseModel
	OrgID       uint64 `gorm:"unique_index:idx_org_cluster_id"`
	OrgName     string
	ClusterID   uint64 `gorm:"unique_index:idx_org_cluster_id"`
	ClusterName string
	Creator     string
}

// TableName 设置模型对应数据库表名称
func (OrgClusterRelation) TableName() string {
	return "dice_org_cluster_relation"
}
