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
	"fmt"
	"sync"
	"time"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/pkg/database/dbengine"
	"github.com/erda-project/erda/pkg/strutil"
	"github.com/erda-project/erda/pkg/ucauth"
)

// TestCase 测试用例
type TestCase struct {
	dbengine.BaseModel
	Name           string
	ProjectID      uint64
	TestSetID      uint64
	Priority       apistructs.TestCasePriority
	PreCondition   string
	StepAndResults TestCaseStepAndResults
	Desc           string
	Recycled       *bool
	From           apistructs.TestCaseFrom
	CreatorID      string
	UpdaterID      string
}

type TestCaseStepAndResults []apistructs.TestCaseStepAndResult

// TableName 设置模型对应数据库表名称
func (TestCase) TableName() string {
	return "dice_test_cases"
}

func (sr TestCaseStepAndResults) Value() (driver.Value, error) {
	if b, err := json.Marshal(sr); err != nil {
		return nil, errors.Errorf("failed to marshal stepAndResults, err: %v", err)
	} else {
		return string(b), nil
	}
}
func (sr *TestCaseStepAndResults) Scan(value interface{}) error {
	if value == nil {
		return nil
	}
	v, ok := value.([]byte)
	if !ok {
		return errors.New("invalid scan source for stepAndResults")
	}
	if len(v) == 0 {
		return nil
	}
	if err := json.Unmarshal(v, sr); err != nil {
		return errors.Wrapf(err, "failed to unmarshal stepAndResults")
	}
	return nil
}

// CreateTestCase 创建测试用例
func (client *DBClient) CreateTestCase(uc *TestCase) error {
	return client.Create(uc).Error
}

// UpdateTestCase 更新测试用例
func (client *DBClient) UpdateTestCase(uc *TestCase) error {
	return client.Save(uc).Error
}

func (client *DBClient) GetTestCaseByID(id uint64) (*TestCase, error) {
	var tc TestCase
	err := client.First(&tc, id).Error
	return &tc, err
}

func (client *DBClient) ListTestCasesByIDs(ids []uint64) ([]TestCase, error) {
	var tcs []TestCase
	if err := client.Where("`id` IN (?)", ids).Find(&tcs).Error; err != nil {
		return nil, err
	}
	if len(tcs) != len(ids) {
		m := make(map[uint64]struct{})
		for _, tc := range tcs {
			m[uint64(tc.ID)] = struct{}{}
		}
		var notFoundIDs []uint64
		for _, id := range ids {
			if _, ok := m[id]; !ok {
				notFoundIDs = append(notFoundIDs, id)
			}
		}
		if len(notFoundIDs) > 0 {
			return nil, fmt.Errorf("some test case not found, ids: %v", notFoundIDs)
		}
	}
	return tcs, nil
}

func (client *DBClient) ListTestCasesByTestSetIDs(req apistructs.TestCaseListRequest) ([]TestCase, error) {
	sql := client.DB
	if req.ProjectID > 0 {
		sql = sql.Where("`project_id` = ?", req.ProjectID)
	}
	if len(req.IDs) > 0 {
		sql = sql.Where("`id` IN (?)", req.IDs)
	}
	if len(req.TestSetIDs) > 0 {
		sql = sql.Where("`test_set_id` IN (?)", req.TestSetIDs)
	}
	if req.IDOnly {
		sql = sql.Select("`id`")
	}
	var tcs []TestCase
	if err := sql.Find(&tcs).Error; err != nil {
		return nil, err
	}
	return tcs, nil
}

func (client *DBClient) BatchUpdateTestCases(req apistructs.TestCaseBatchUpdateRequest) error {
	if len(req.TestCaseIDs) == 0 {
		return fmt.Errorf("no testcase selected")
	}

	sql := client.Model(TestCase{}).Where("`id` IN (?)", req.TestCaseIDs)

	kvs := make(map[string]interface{})

	if req.Priority != "" {
		kvs["priority"] = req.Priority
	}
	if req.Recycled != nil {
		kvs["recycled"] = req.Recycled
	}
	if req.MoveToTestSetID != nil {
		kvs["test_set_id"] = *req.MoveToTestSetID
	}

	return sql.Updates(kvs).Error
}

func (client *DBClient) BatchCopyTestCases(req apistructs.TestCaseBatchCopyRequest) error {
	if len(req.TestCaseIDs) == 0 {
		return fmt.Errorf("no testcase selected")
	}
	return nil
}

// RecycledTestCasesByTestSetID 回收测试集下的测试用例
func (client *DBClient) RecycledTestCasesByTestSetID(projectID, testSetID uint64) error {
	var useCase TestCase
	return client.Model(&useCase).
		Where("project_id = ?", projectID).
		Where("test_set_id = ?", testSetID).Updates("recycled", apistructs.RecycledYes).Error
}

// RecoverTestCasesByTestSetID 回收站恢复测试集下的测试用例
func (client *DBClient) RecoverTestCasesByTestSetID(projectID, testSetID uint64) error {
	var useCase TestCase
	return client.Model(&useCase).
		Where("project_id = ?", projectID).
		Where("test_set_id = ?", testSetID).Updates("recycled", apistructs.RecycledNo).Error
}

// CleanTestCasesByTestSetID 彻底删除测试集下的测试用例
func (client *DBClient) CleanTestCasesByTestSetID(projectID, testSetID uint64) error {
	return client.
		Where("project_id = ?", projectID).
		Where("test_set_id = ?", testSetID).
		Delete(TestCase{}).Error
}

func (client *DBClient) BatchDeleteTestCases(ids []uint64) error {
	return client.Where("`id` IN (?)", ids).Delete(TestCase{}).Error
}

// order
const (
	tcFieldPriority    = "priority"
	tcFieldID          = "id"
	tcFieldTestSetID   = "test_set_id"
	tcFieldTestSetIDV2 = "testSetID"
	tcFieldUpdaterID   = "updater_id"
	tcFieldUpdaterIDV2 = "updaterID"
	tcFieldUpdatedAt   = "updated_at"
	tcFieldUpdatedAtV2 = "updatedAt"
)

func (client *DBClient) PagingTestCases(req apistructs.TestCasePagingRequest) ([]TestCase, uint64, error) {
	// validate request
	if err := validateTestCasePagingRequest(req); err != nil {
		return nil, 0, err
	}
	// set default for request
	setDefaultForTestCasePagingRequest(&req)
	// query base test set if necessary, then use `directory` to do `like` query
	var baseTestSet TestSet
	if req.TestSetID > 0 {
		ts, err := client.GetTestSetByID(req.TestSetID)
		if err != nil {
			return nil, 0, err
		}
		baseTestSet = *ts
	}

	baseSQL := client.DB.Table(TestCase{}.TableName() + " AS `tc`").Select("*")

	// left join test_plan_case_relations
	if len(req.NotInTestPlanIDs) > 0 {
		baseSQL = baseSQL.Joins(
			"LEFT JOIN ("+
				"     SELECT * FROM "+TestPlanCaseRel{}.TableName()+" WHERE `test_plan_id` IN (?) GROUP BY `test_case_id`"+
				") AS `rel` ON `tc`.`id` = `rel`.`test_case_id`",
			req.NotInTestPlanIDs,
		)
		baseSQL = baseSQL.Where("`rel`.`test_plan_id` IS NULL OR `rel`.`test_plan_id` NOT IN (?)", req.NotInTestPlanIDs)
	}

	// left join test_sets
	// use left join because test_set with id = 0 is not exists in test_sets table
	baseSQL = baseSQL.Joins("LEFT JOIN " + TestSet{}.TableName() + " AS `ts` ON `tc`.`test_set_id` = `ts`.`id`")

	// where clauses
	// project id
	baseSQL = baseSQL.Where("`tc`.`project_id` = ?", req.ProjectID)
	// test set id
	if req.TestSetID > 0 {
		baseSQL = baseSQL.Where("`ts`.`directory` LIKE ? OR `ts`.`directory` = ?", baseTestSet.Directory+"/%", baseTestSet.Directory)
	}
	// recycled
	baseSQL = baseSQL.Where("`tc`.`recycled` = ?", req.Recycled)
	// name
	if req.Query != "" {
		baseSQL = baseSQL.Where("`tc`.`name` LIKE ?", strutil.Concat("%", req.Query, "%"))
	}
	// priority
	if len(req.Priorities) > 0 {
		baseSQL = baseSQL.Where("`tc`.`priority` IN (?)", req.Priorities)
	}
	// updater
	if len(req.UpdaterIDs) > 0 {
		baseSQL = baseSQL.Where("`tc`.`updater_id` IN (?)", req.UpdaterIDs)
	}
	// updatedAtBegin (Left closed Section)
	if req.TimestampSecUpdatedAtBegin != nil {
		t := time.Unix(int64(*req.TimestampSecUpdatedAtBegin), 0)
		req.UpdatedAtBeginInclude = &t
	}
	if req.UpdatedAtBeginInclude != nil {
		baseSQL = baseSQL.Where("`tc`.`updated_at` >= ?", req.UpdatedAtBeginInclude)
	}
	// updatedAtEnd (Right closed Section)
	if req.TimestampSecUpdatedAtEnd != nil {
		t := time.Unix(int64(*req.TimestampSecUpdatedAtEnd), 0)
		req.UpdatedAtEndInclude = &t
	}
	if req.UpdatedAtEndInclude != nil {
		baseSQL = baseSQL.Where("`tc`.`updated_at` <= ?", req.UpdatedAtEndInclude)
	}
	// testCaseIDs
	if len(req.TestCaseIDs) > 0 {
		baseSQL = baseSQL.Where("`tc`.`id` IN (?)", req.TestCaseIDs)
	}
	if len(req.NotInTestCaseIDs) > 0 {
		baseSQL = baseSQL.Where("`tc`.`id` NOT IN (?)", req.NotInTestCaseIDs)
	}

	pagingSQL := baseSQL.NewScope(nil).DB()
	countSQL := baseSQL.NewScope(nil).DB()

	// order by fields
	for _, orderField := range req.OrderFields {
		switch orderField {
		case tcFieldID:
			if req.OrderByIDAsc != nil {
				pagingSQL = pagingSQL.Order("`tc`.`id` ASC")
			}
			if req.OrderByIDDesc != nil {
				pagingSQL = pagingSQL.Order("`tc`.`id` DESC")
			}
		case tcFieldTestSetID, tcFieldTestSetIDV2:
			if req.OrderByTestSetIDAsc != nil {
				pagingSQL = pagingSQL.Order("`tc`.`test_set_id` ASC")
			}
			if req.OrderByTestSetIDDesc != nil {
				pagingSQL = pagingSQL.Order("`tc`.`test_set_id` DESC")
			}
		case tcFieldPriority:
			if req.OrderByPriorityAsc != nil {
				pagingSQL = pagingSQL.Order("`tc`.`priority` ASC")
			}
			if req.OrderByPriorityDesc != nil {
				pagingSQL = pagingSQL.Order("`tc`.`priority` DESC")
			}
		case tcFieldUpdaterID, tcFieldUpdaterIDV2:
			if req.OrderByUpdaterIDAsc != nil {
				pagingSQL = pagingSQL.Order("`tc`.`updater_id` ASC")
			}
			if req.OrderByUpdaterIDDesc != nil {
				pagingSQL = pagingSQL.Order("`tc`.`updater_id` DESC")
			}
		case tcFieldUpdatedAt, tcFieldUpdatedAtV2:
			if req.OrderByUpdatedAtAsc != nil {
				pagingSQL = pagingSQL.Order("`tc`.`updated_at` ASC")
			}
			if req.OrderByUpdatedAtDesc != nil {
				pagingSQL = pagingSQL.Order("`tc`.`updated_at` DESC")
			}
		}
	}

	// concurrent do paging and count
	var wg sync.WaitGroup
	wg.Add(2)

	// result
	var (
		testCases           []TestCase
		total               uint64
		pagingErr, countErr error
	)

	// do paging
	go func() {
		defer wg.Done()

		// offset, limit
		offset := (req.PageNo - 1) * req.PageSize
		limit := req.PageSize
		pagingErr = pagingSQL.Offset(offset).Limit(limit).Find(&testCases).Error
	}()

	// do count
	go func() {
		defer wg.Done()

		// reset offset & limit before count
		countErr = countSQL.Offset(0).Limit(-1).Count(&total).Error
	}()

	// wait
	wg.Wait()

	if pagingErr != nil {
		return nil, 0, apierrors.ErrPagingTestCases.InternalError(pagingErr)
	}
	if countErr != nil {
		return nil, 0, apierrors.ErrPagingTestCases.InternalError(countErr)
	}

	return testCases, total, nil
}

func validateTestCasePagingRequest(req apistructs.TestCasePagingRequest) error {
	if req.ProjectID == 0 {
		return apierrors.ErrPagingTestCases.MissingParameter("projectID")
	}
	for _, priority := range req.Priorities {
		if !priority.IsValid() {
			return apierrors.ErrPagingTestCases.InvalidParameter(fmt.Sprintf("priority: %s", priority))
		}
	}
	if req.OrderByPriorityAsc != nil && req.OrderByPriorityDesc != nil {
		return apierrors.ErrPagingTestCases.InvalidParameter("order by priority ASC or DESC?")
	}
	if req.OrderByUpdaterIDAsc != nil && req.OrderByUpdaterIDDesc != nil {
		return apierrors.ErrPagingTestCases.InvalidParameter("order by updaterID ASC or DESC?")
	}
	if req.OrderByUpdatedAtAsc != nil && req.OrderByUpdatedAtDesc != nil {
		return apierrors.ErrPagingTestCases.InvalidParameter("order by updatedAt ASC or DESC?")
	}
	if req.OrderByIDAsc != nil && req.OrderByIDDesc != nil {
		return apierrors.ErrPagingTestCases.InvalidParameter("order by id ASC or DESC?")
	}
	if req.OrderByTestSetIDAsc != nil && req.OrderByTestSetIDDesc != nil {
		return apierrors.ErrPagingTestCases.InvalidParameter("order by testSetID ASC or DESC?")
	}
	if req.OrderByTestSetNameAsc != nil && req.OrderByTestSetNameDesc != nil {
		return apierrors.ErrPagingTestCases.InvalidParameter("order by testSetName ASC or DESC?")
	}

	return nil
}

func setDefaultForTestCasePagingRequest(req *apistructs.TestCasePagingRequest) {
	// must order by testSet
	if req.OrderByTestSetIDAsc == nil && req.OrderByTestSetIDDesc == nil &&
		req.OrderByTestSetNameAsc == nil && req.OrderByTestSetNameDesc == nil {
		// default order by `test_set_id` ASC
		req.OrderByTestSetIDAsc = &[]bool{true}[0]
		req.OrderFields = append(req.OrderFields, tcFieldTestSetID)
	}

	// set default order inside a testSet
	if req.OrderByPriorityAsc == nil && req.OrderByPriorityDesc == nil &&
		req.OrderByUpdaterIDAsc == nil && req.OrderByUpdaterIDDesc == nil &&
		req.OrderByUpdatedAtAsc == nil && req.OrderByUpdatedAtDesc == nil &&
		req.OrderByIDAsc == nil && req.OrderByIDDesc == nil {
		// default order by `id` ASC
		req.OrderByIDAsc = &[]bool{true}[0]
		req.OrderFields = append(req.OrderFields, tcFieldID)
	}

	if req.PageNo == 0 {
		req.PageNo = 1
	}
	if req.PageSize == 0 {
		req.PageSize = 20
	}

	// set unassigned ids
	req.UpdaterIDs = ucauth.PolishUnassignedAsEmptyStr(req.UpdaterIDs)
}
