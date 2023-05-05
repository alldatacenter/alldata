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
	"time"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/pkg/database/cimysql"
)

// APITestEnv 存储API测试环境变量
type APITestEnv struct {
	ID        int64     `xorm:"pk autoincr" json:"id"`
	CreatedAt time.Time `xorm:"created" json:"createdAt"`
	UpdatedAt time.Time `xorm:"updated" json:"updatedAt"`

	EnvID   int64  `xorm:"env_id" json:"envID"`
	EnvType string `xorm:"env_type" json:"envType"`
	Name    string `xorm:"name" json:"name"`
	Domain  string `xorm:"domain" json:"domain"`
	Header  string `xorm:"header" json:"header"`
	Global  string `xorm:"global" json:"global"`
}

// TableName APITestEnv对应的数据库表dice_api_test_env
func (APITestEnv) TableName() string {
	return "dice_api_test_env"
}

// CreateTestEnv 创建测试环境变量信息
func CreateTestEnv(env *APITestEnv) (int64, error) {
	_, err := cimysql.Engine.InsertOne(env)
	if err != nil {
		return 0, errors.Errorf("failed to create project test env info, (%+v)", err)
	}

	return env.ID, nil
}

// UpdateTestEnv 更新测试环境变量信息
func UpdateTestEnv(env *APITestEnv) error {
	_, err := cimysql.Engine.Id(env.ID).Update(env)
	if err != nil {
		return errors.Errorf("failed to update project test env info, ID:%d, (%+v)", env.ID, err)
	}

	return nil
}

// GetTestEnv 根据envID获取测试环境变量信息
func GetTestEnv(envID int64) (*APITestEnv, error) {
	testEnv := new(APITestEnv)
	_, err := cimysql.Engine.ID(envID).Get(testEnv)
	if err != nil {
		return nil, errors.Errorf("failed to get api test env info, ID:%d, (%+v)", envID, err)
	}

	return testEnv, nil
}

// DeleteTestEnv 删除测试环境变量信息
func DeleteTestEnv(envID int64) error {
	testEnv := new(APITestEnv)
	_, err := cimysql.Engine.ID(envID).Delete(testEnv)
	if err != nil {
		return errors.Errorf("failed to get project test env info, ID:%d, (%+v)", envID, err)
	}

	return nil
}

// GetTestEnvListByEnvID 根据envID获取测试环境变量信息
func GetTestEnvListByEnvID(envID int64, envType string) ([]APITestEnv, error) {
	testEnvList := []APITestEnv{}
	err := cimysql.Engine.Where("env_id = ? AND env_type = ?", envID, envType).Find(&testEnvList)
	if err != nil {
		return nil, errors.Errorf("failed to get api test env list, envID:%d, (%+v)", envID, err)
	}

	return testEnvList, nil
}
