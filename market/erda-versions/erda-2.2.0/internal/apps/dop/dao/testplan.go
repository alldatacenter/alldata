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
	"database/sql/driver"
	"encoding/json"
	"time"

	"github.com/jinzhu/gorm"
	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/database/dbengine"
	"github.com/erda-project/erda/pkg/strutil"
	"github.com/erda-project/erda/pkg/ucauth"
)

// TestPlan 测试计划
type TestPlan struct {
	dbengine.BaseModel
	Name        string
	Status      apistructs.TPStatus // DOING/PAUSE/DONE
	ProjectID   uint64
	CreatorID   string
	IterationID uint64
	UpdaterID   string
	Summary     string
	StartedAt   *time.Time
	EndedAt     *time.Time
	Type        apistructs.TestPlanType
	IsArchived  bool
	Inode       string
}

type PartnerIDs []string

func (ids PartnerIDs) Value() (driver.Value, error) {
	if b, err := json.Marshal(ids); err != nil {
		return nil, errors.Errorf("failed to marshal partnerIDs, err: %v", err)
	} else {
		return string(b), nil
	}
}
func (ids *PartnerIDs) Scan(value interface{}) error {
	if value == nil {
		return nil
	}
	v, ok := value.([]byte)
	if !ok {
		return errors.New("invalid scan source for partner_ids")
	}
	if len(v) == 0 {
		return nil
	}
	if err := json.Unmarshal(v, ids); err != nil {
		return errors.Wrapf(err, "failed to unmarshal partner_ids")
	}
	return nil
}

// TableName 表名
func (TestPlan) TableName() string {
	return "dice_test_plans"
}

// CreateTestPlan Create test plan
func (client *DBClient) CreateTestPlan(testPlan *TestPlan) error {
	return client.Create(testPlan).Error
}

// UpdateTestPlan Update test plan
func (client *DBClient) UpdateTestPlan(testPlan *TestPlan) error {
	return client.Save(testPlan).Error
}

// DeleteTestPlan Delete test plan
func (client *DBClient) DeleteTestPlan(testPlanID uint64) error {
	return client.Where("id = ?", testPlanID).Delete(TestPlan{}).Error
}

// GetTestPlan Fetch test plan
func (client *DBClient) GetTestPlan(testPlanID uint64) (*TestPlan, error) {
	var testPlan TestPlan
	if err := client.Where("id = ?", testPlanID).Find(&testPlan).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil
		}
		return nil, err
	}
	return &testPlan, nil
}

// GetTestPlanByName
func (client *DBClient) GetTestPlanByName(projectID uint64, name string) (*TestPlan, error) {
	var testPlan TestPlan
	if err := client.Where("project_id = ?", projectID).
		Where("name = ?", name).Find(&testPlan).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil
		}
		return nil, err
	}
	return &testPlan, nil
}

// PagingTestPlan List test plan
func (client *DBClient) PagingTestPlan(req apistructs.TestPlanPagingRequest) (uint64, []TestPlan, error) {
	var (
		total     uint64
		testPlans []TestPlan
	)
	// set unassigned ids
	req.OwnerIDs = ucauth.PolishUnassignedAsEmptyStr(req.OwnerIDs)
	req.PartnerIDs = ucauth.PolishUnassignedAsEmptyStr(req.PartnerIDs)
	req.UserIDs = ucauth.PolishUnassignedAsEmptyStr(req.UserIDs)

	var tpIDs []uint64
	var tpIDsAssigned = false
	if len(req.OwnerIDs) > 0 {
		ownerTpIDs, err := client.ListTestPlanIDsByOwnerIDs(req.OwnerIDs)
		if err != nil {
			return 0, nil, err
		}
		tpIDs = append(tpIDs, ownerTpIDs...)
		tpIDsAssigned = true
	}
	if len(req.PartnerIDs) > 0 {
		partnerTpIDs, err := client.ListTestPlanIDsByPartnerIDs(req.PartnerIDs)
		if err != nil {
			return 0, nil, err
		}
		if tpIDsAssigned {
			tpIDs = strutil.IntersectionUin64Slice(tpIDs, partnerTpIDs)
		} else {
			tpIDsAssigned = true
			tpIDs = append(tpIDs, partnerTpIDs...)
		}
	}
	if len(req.UserIDs) > 0 {
		userTpIDs, err := client.ListTestPlanIDsByUserIDs(req.UserIDs)
		if err != nil {
			return 0, nil, err
		}
		if tpIDsAssigned {
			tpIDs = strutil.IntersectionUin64Slice(tpIDs, userTpIDs)
		} else {
			tpIDs = append(tpIDs, userTpIDs...)
		}
	}

	cond := TestPlan{
		ProjectID: req.ProjectID,
		Type:      req.Type,
	}
	sql := client.Where(cond)
	if len(tpIDs) > 0 {
		sql = sql.Where("`id` IN (?)", tpIDs)
	}
	if req.Name != "" {
		sql = sql.Where("name LIKE ?", "%"+req.Name+"%")
	}
	if len(req.Statuses) > 0 {
		sql = sql.Where("status IN (?)", req.Statuses)
	}
	if req.IsArchived != nil {
		sql = sql.Where("is_archived = ?", req.IsArchived)
	}
	if len(req.IterationIDs) > 0 {
		sql = sql.Where("iteration_id IN (?)", req.IterationIDs)
	}
	if err := sql.Offset((req.PageNo - 1) * req.PageSize).Limit(req.PageSize).
		Order("`id` DESC").Find(&testPlans).
		Offset(0).Limit(-1).Count(&total).Error; err != nil {
		return 0, nil, err
	}

	return total, testPlans, nil
}
