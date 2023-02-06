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
	"github.com/jinzhu/gorm"
)

type ClusterDB struct {
	*gorm.DB
}

// CreateCluster create cluster
func (client *ClusterDB) CreateCluster(cluster *Cluster) error {
	return client.Create(cluster).Error
}

// UpdateCluster update cluster
func (client *ClusterDB) UpdateCluster(cluster *Cluster) error {
	return client.Save(cluster).Error
}

// DeleteCluster delete cluster
func (client *ClusterDB) DeleteCluster(clusterName string) error {
	return client.Where("name = ?", clusterName).Delete(&Cluster{}).Error
}

// GetClusterByName get cluster by name
func (client *ClusterDB) GetClusterByName(clusterName string) (*Cluster, error) {
	var cluster Cluster
	if err := client.Where("name = ?", clusterName).First(&cluster).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil
		}
		return nil, err
	}
	return &cluster, nil
}

// GetClusterByID get cluster by name
func (client *ClusterDB) GetClusterByID(clusterID int64) (*Cluster, error) {
	var cluster Cluster
	if err := client.Where("id = ?", clusterID).First(&cluster).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil
		}
		return nil, err
	}
	return &cluster, nil
}

// ListCluster list cluster
func (client *ClusterDB) ListCluster() (*[]Cluster, error) {
	var clusters []Cluster
	if err := client.Find(&clusters).Error; err != nil {
		return nil, err
	}
	return &clusters, nil
}

// ListClusterByType list cluster filter by cluster type
func (client *ClusterDB) ListClusterByType(clusterType string) (*[]Cluster, error) {
	var clusters []Cluster
	if clusterType != "" {
		if err := client.Where("type = ?", clusterType).Find(&clusters).Error; err != nil {
			return nil, err
		}
	}
	return &clusters, nil
}
