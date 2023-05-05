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
	"fmt"
	"time"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/pkg/database/cimysql"
)

// ApiTest 存储pmp上接口测试相关的信息，对应数据库表dice_api_test
type ApiTest struct {
	ID        int64     `xorm:"pk autoincr" json:"id"`
	CreatedAt time.Time `xorm:"created" json:"createdAt"`
	UpdatedAt time.Time `xorm:"updated" json:"updatedAt"`

	UsecaseID    int64  `xorm:"usecase_id" json:"usecaseID"`
	UsecaseOrder int64  `xorm:"usecase_order" json:"usecaseOrder"`
	ProjectID    int64  `xorm:"project_id" json:"projectID"`
	PipelineID   int64  `xorm:"pipeline_id" json:"pipelineID"`
	Status       string `xorm:"status" json:"status"`
	ApiInfo      string `xorm:"text" json:"apiInfo"`
	ApiRequest   string `xorm:"longtext" json:"apiRequest"`
	ApiResponse  string `xorm:"longtext" json:"apiResponse"`
	AssertResult string `xorm:"text" json:"assertResult"`
}

// TableName DiceApiTest对应的数据库表dice_api_test
func (ApiTest) TableName() string {
	return "dice_api_test"
}

// CreateApiTest 创建apiTest信息
func CreateApiTest(at *ApiTest) (int64, error) {
	ID, err := cimysql.Engine.InsertOne(at)
	if err != nil {
		return 0, errors.Errorf("failed to insert api test info, (%+v)", err)
	}

	return ID, nil
}

// UpdateApiTest 更新apiTest信息
func UpdateApiTest(at *ApiTest) (int64, error) {
	ID, err := cimysql.Engine.Id(at.ID).Update(at)
	if err != nil {
		return 0, errors.Errorf("failed to update api test info, ID:%d, (%+v)", at.ID, err)
	}

	return ID, nil
}

// UpdateApiTestResults 更新apiTest结果信息：status，api_response，assert_result
func UpdateApiTestResults(at *ApiTest) (int64, error) {
	ID, err := cimysql.Engine.Id(at.ID).Cols("status", "api_request", "api_response", "assert_result").Update(at)
	if err != nil {
		return 0, errors.Errorf("failed to update api test info, ID:%d, (%+v)", at.ID, err)
	}

	return ID, nil
}

// GetApiTest 根据apiID获取apiTest信息
func GetApiTest(apiID int64) (*ApiTest, error) {
	apiTest := new(ApiTest)
	_, err := cimysql.Engine.ID(apiID).Get(apiTest)
	if err != nil {
		return nil, errors.Errorf("failed to get api test info, ID:%d, (%+v)", apiID, err)
	}

	return apiTest, nil
}

// DeleteApiTest 删除apiTest信息
func DeleteApiTest(apiID int64) error {
	apiTest := new(ApiTest)
	_, err := cimysql.Engine.ID(apiID).Delete(apiTest)
	if err != nil {
		return errors.Errorf("failed to get api test info, ID:%d, (%+v)", apiID, err)
	}

	return nil
}

// GetApiTestListByUsecaseID 根据usecaseID获取apiTest列表
func GetApiTestListByUsecaseID(usecaseID int64) ([]ApiTest, error) {
	apiTestsList := []ApiTest{}
	err := cimysql.Engine.Where("usecase_id = ?", usecaseID).Asc("usecase_order").Find(&apiTestsList)
	if err != nil {
		return nil, errors.Errorf("failed to get api test list, usecaseID:%d, (%+v)", usecaseID, err)
	}

	return apiTestsList, nil
}

func ListAPIsByTestCaseIDs(projectID uint64, tcIDs []uint64) (map[uint64][]*ApiTest, error) {
	var apis []ApiTest
	sql := cimysql.Engine.In("usecase_id", tcIDs)
	if len(tcIDs) > 1000 {
		sql = cimysql.Engine.Where("project_id = ?", projectID)
	}
	err := sql.Find(&apis)
	if err != nil {
		return nil, fmt.Errorf("failed to list apis by testcases, err: %v", err)
	}
	// filter by tcIDs
	tcIDMap := make(map[uint64]struct{})
	for _, tcID := range tcIDs {
		tcIDMap[tcID] = struct{}{}
	}
	m := make(map[uint64][]*ApiTest)
	for _, api := range apis {
		api := api
		tcID := uint64(api.UsecaseID)
		if _, ok := tcIDMap[tcID]; !ok {
			continue
		}
		m[tcID] = append(m[tcID], &api)
	}
	return m, nil
}
