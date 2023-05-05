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

	"github.com/erda-project/erda/internal/apps/admin/model"
)

// GetHostByClusterAndIP get host info according cluster & privateAddr
func (client *DBClient) GetHostByClusterAndIP(clusterName, privateAddr string) (*model.Host, error) {
	var host model.Host
	if err := client.Where("cluster = ?", clusterName).
		Where("private_addr = ?", privateAddr).First(&host).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil
		}
		return nil, err
	}

	return &host, nil
}
