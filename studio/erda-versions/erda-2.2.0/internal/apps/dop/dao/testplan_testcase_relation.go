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
	"fmt"
	"sync"
	"time"

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/pkg/database/dbengine"
	"github.com/erda-project/erda/pkg/strutil"
	"github.com/erda-project/erda/pkg/ucauth"
)

// TestPlanCaseRel
type TestPlanCaseRel struct {
	dbengine.BaseModel
	TestPlanID uint64
	TestSetID  uint64
	TestCaseID uint64
	ExecStatus apistructs.TestCaseExecStatus
	CreatorID  string
	UpdaterID  string
	ExecutorID string
}

// TableName 表名
func (TestPlanCaseRel) TableName() string {
	return "dice_test_plan_case_relations"
}

// CreateTestPlanCaseRel Create testPlanCaseRel
func (client *DBClient) CreateTestPlanCaseRel(testPlanCaseRel *TestPlanCaseRel) error {
	return client.Create(testPlanCaseRel).Error
}

func (client *DBClient) BatchCreateTestPlanCaseRels(rels []TestPlanCaseRel) error {
	return client.BulkInsert(rels)
}

func (client *DBClient) GetTestPlanCaseRelByPlanIDAndCaseID(planID, caseID uint64) (*TestPlanCaseRel, error) {
	var rel TestPlanCaseRel
	if err := client.Where("`test_plan_id` = ?", planID).Where("`test_case_id` = ?", caseID).First(&rel).Error; err != nil {
		return nil, err
	}
	return &rel, nil
}

// CheckTestPlanCaseRelIDsExistOrNot 返回已存在和不存在的 id 列表
// return: existIDs, notExistIDs, error
func (client *DBClient) CheckTestPlanCaseRelIDsExistOrNot(planID uint64, caseIDs []uint64) ([]uint64, []uint64, error) {
	var existRels []TestPlanCaseRel
	if err := client.Select("`test_case_id`").Where("`test_plan_id` = ?", planID).
		Where("`test_case_id` IN (?)", caseIDs).Find(&existRels).Error; err != nil {
		return nil, nil, err
	}
	var existIDs []uint64
	for _, rel := range existRels {
		existIDs = append(existIDs, rel.TestCaseID)
	}
	allIDMap := make(map[uint64]struct{})
	for _, id := range caseIDs {
		allIDMap[id] = struct{}{}
	}
	for _, existID := range existIDs {
		delete(allIDMap, existID)
	}
	var notExistIDs []uint64
	for id := range allIDMap {
		notExistIDs = append(notExistIDs, id)
	}
	return existIDs, notExistIDs, nil
}

// UpdateTestPlanTestCaseRel Update testPlanCaseRel
func (client *DBClient) UpdateTestPlanTestCaseRel(testPlanCaseRel *TestPlanCaseRel) error {
	return client.Save(testPlanCaseRel).Error
}

// BatchUpdateTestPlanCaseRels 批量更新测试计划用例
func (client *DBClient) BatchUpdateTestPlanCaseRels(req apistructs.TestPlanCaseRelBatchUpdateRequest) error {
	db := client.Model(TestPlanCaseRel{}).Where("`test_plan_id` = ?", req.TestPlanID)
	if len(req.RelationIDs) > 0 {
		db = db.Where("`id` IN (?)", req.RelationIDs)
	}
	updateFields := make(map[string]interface{})
	if req.ExecutorID != "" {
		updateFields["executor_id"] = req.ExecutorID
	}
	if req.ExecStatus != "" {
		updateFields["exec_status"] = req.ExecStatus
	}
	if req.IdentityInfo.UserID != "" {
		updateFields["updater_id"] = req.IdentityInfo.UserID
	}
	return db.Updates(updateFields).Error
}

// DeleteTestPlanCaseRelations
func (client *DBClient) DeleteTestPlanCaseRelations(testPlanID uint64, relIDs []uint64) error {
	sql := client.Where("test_plan_id = ?", testPlanID)
	if len(relIDs) > 0 {
		sql = sql.Where("id IN (?)", relIDs)
	}
	return sql.Delete(&TestPlanCaseRel{}).Error
}

// DeleteTestPlanCaseRelationsByTestCaseIds
func (client *DBClient) DeleteTestPlanCaseRelationsByTestCaseIds(testCaseIds []uint64) error {
	sql := client.Where("test_case_id IN (?)", testCaseIds)
	return sql.Delete(&TestPlanCaseRel{}).Error
}

// DeleteTestPlanCaseRelByTestPlanID Delete relations by testPlanID
func (client *DBClient) DeleteTestPlanCaseRelByTestPlanID(testPlanID uint64) error {
	return client.Where("testplan_id = ?", testPlanID).Delete(TestPlanCaseRel{}).Error
}

// DeleteTestPlanCaseRelByTestCaseID Delete relations by testCaseID
func (client *DBClient) DeleteTestPlanCaseRelByTestCaseID(testCaseID uint64) error {
	return client.Where("usecase_id = ?", testCaseID).Delete(TestPlanCaseRel{}).Error
}

// GetTestPlanCaseRel Fetch testPlanCaseRel
func (client *DBClient) GetTestPlanCaseRel(relID uint64) (*TestPlanCaseRel, error) {
	var testPlanCaseRel TestPlanCaseRel
	if err := client.Where("id = ?", relID).First(&testPlanCaseRel).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil
		}
		return nil, err
	}
	return &testPlanCaseRel, nil
}

// ListTestPlanCaseRels List testPlanCaseRel
func (client *DBClient) ListTestPlanCaseRels(req apistructs.TestPlanCaseRelListRequest) ([]TestPlanCaseRel, error) {
	var rels []TestPlanCaseRel
	sql := client.DB
	if len(req.IDs) > 0 {
		sql = sql.Where("`id` IN (?)", req.IDs)
	}
	if len(req.TestPlanIDs) > 0 {
		sql = sql.Where("`test_plan_id` IN (?)", req.TestPlanIDs)
	}
	if len(req.TestSetIDs) > 0 {
		sql = sql.Where("`test_set_id` IN (?)", req.TestSetIDs)
	}
	if len(req.CreatorIDs) > 0 {
		sql = sql.Where("`creator_id` IN (?)", req.CreatorIDs)
	}
	if len(req.UpdaterIDs) > 0 {
		sql = sql.Where("`updater_id` IN (?)", req.UpdaterIDs)
	}
	if len(req.ExecutorIDs) > 0 {
		if strutil.Exist(req.ExecutorIDs, "unassigned") {
			sql = sql.Where("`executor_id` IN (?) OR `executor_id` = ''", req.ExecutorIDs)
		} else {
			sql = sql.Where("`executor_id` IN (?)", req.ExecutorIDs)
		}
	}
	if len(req.ExecStatuses) > 0 {
		sql = sql.Where("`exec_status` IN (?)", req.ExecStatuses)
	}
	if req.UpdatedAtBeginInclude != nil {
		sql = sql.Where("`updated_at` >= ?", req.UpdatedAtBeginInclude)
	}
	if req.UpdatedAtEndInclude != nil {
		sql = sql.Where("`updated_at` <= ?", req.UpdatedAtEndInclude)
	}
	if req.IDOnly {
		sql = sql.Select("`id`")
	}
	sql = sql.Order("`id` DESC")
	if err := sql.Find(&rels).Error; err != nil {
		return nil, err
	}

	return rels, nil
}

// ListTestPlanTestSetIDs 获取测试计划下的测试集 ID 列表，从关联关系而来
func (client *DBClient) ListTestPlanTestSetIDs(testPlanID uint64) ([]uint64, error) {
	var rels []TestPlanCaseRel
	sql := client.DB.Where("`test_plan_id` = ?", testPlanID).Find(&rels)
	if err := sql.Error; err != nil {
		return nil, err
	}
	testSetIDMap := make(map[uint64]struct{})
	for _, rel := range rels {
		testSetIDMap[rel.TestSetID] = struct{}{}
	}
	var testSetIDs []uint64
	for tsID := range testSetIDMap {
		testSetIDs = append(testSetIDs, tsID)
	}
	return testSetIDs, nil
}

func (client *DBClient) ListTestPlanCaseRelsCount(testPlanIDs []uint64) (map[uint64]apistructs.TestPlanRelsCount, error) {
	if len(testPlanIDs) == 0 {
		return nil, nil
	}

	// temp is a struct for `select test_plan_id, exec_status, count(*) as count from ...`
	type temp struct {
		TestPlanCaseRel
		Count uint64
	}
	var rels []temp
	sql := client.Table(TestPlanCaseRel{}.TableName()).
		Select([]string{"`test_plan_id`", "`exec_status`", "count(*) as count"}).
		Where("`test_plan_id` IN (?)", testPlanIDs).
		Group("`test_plan_id`, `exec_status`").Find(&rels)
	if err := sql.Error; err != nil {
		return nil, err
	}

	result := make(map[uint64]apistructs.TestPlanRelsCount)
	for _, rel := range rels {
		c, ok := result[rel.TestPlanID]
		if !ok {
			c = apistructs.TestPlanRelsCount{}
		}
		switch rel.ExecStatus {
		case apistructs.CaseExecStatusInit:
			c.Init = rel.Count
		case apistructs.CaseExecStatusSucc:
			c.Succ = rel.Count
		case apistructs.CaseExecStatusFail:
			c.Fail = rel.Count
		case apistructs.CaseExecStatusBlocked:
			c.Block = rel.Count
		}
		c.Total += rel.Count
		result[rel.TestPlanID] = c
	}

	return result, nil
}

type TestPlanCaseRelDetail struct {
	TestPlanCaseRel
	Name           string
	ProjectID      uint64
	Priority       apistructs.TestCasePriority
	PreCondition   string
	StepAndResults TestCaseStepAndResults
	Desc           string
	Recycled       *bool
	From           apistructs.TestCaseFrom
}

func (rel TestPlanCaseRelDetail) ConvertForPaging() apistructs.TestPlanCaseRel {
	return apistructs.TestPlanCaseRel{
		ID:         rel.ID,
		Name:       rel.Name,
		Priority:   rel.Priority,
		TestPlanID: rel.TestPlanID,
		TestSetID:  rel.TestSetID,
		TestCaseID: rel.TestCaseID,
		ExecStatus: rel.ExecStatus,
		CreatorID:  rel.CreatorID,
		UpdaterID:  rel.UpdaterID,
		ExecutorID: rel.ExecutorID,
		CreatedAt:  rel.CreatedAt,
		UpdatedAt:  rel.UpdatedAt,
	}
}

func (client *DBClient) PagingTestPlanCaseRelations(req apistructs.TestPlanCaseRelPagingRequest) ([]TestPlanCaseRelDetail, uint64, error) {
	// validate request
	if err := validateTestPlanCaseRelPagingRequest(req); err != nil {
		return nil, 0, err
	}
	// set default for request
	setDefaultForTestPlanCaseRelPagingRequest(&req)
	// query base test set if necessary, then use `directory` to do `like` query
	var baseTestSet TestSet
	if req.TestSetID > 0 {
		ts, err := client.GetTestSetByID(req.TestSetID)
		if err != nil {
			return nil, 0, err
		}
		baseTestSet = *ts
	}

	baseSQL := client.DB.Table(TestPlanCaseRel{}.TableName() + " AS `rel`").Select("*")
	baseSQL = baseSQL.Joins("LEFT JOIN " + TestCase{}.TableName() + " AS `tc` ON `rel`.`test_case_id` = `tc`.`id`")
	// left join test_sets
	// use left join because test_set with id = 0 is not exists in test_sets table
	baseSQL = baseSQL.Joins("LEFT JOIN " + TestSet{}.TableName() + " AS `ts` ON `rel`.`test_set_id` = `ts`.`id`")

	// where clauses
	// testplan
	baseSQL = baseSQL.Where("`rel`.`test_plan_id` = ?", req.TestPlanID)
	// testset
	if req.TestSetID > 0 {
		baseSQL = baseSQL.Where("`ts`.`directory` LIKE ? OR `ts`.`directory` = ?", baseTestSet.Directory+"/%", baseTestSet.Directory)
	}
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
		baseSQL = baseSQL.Where("`rel`.`updater_id` IN (?)", req.UpdaterIDs)
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
	// executor
	if len(req.ExecutorIDs) > 0 {
		baseSQL = baseSQL.Where("`rel`.`executor_id` IN (?)", req.ExecutorIDs)
	}
	// executorStatus
	if len(req.ExecStatuses) > 0 {
		baseSQL = baseSQL.Where("`rel`.`exec_status` IN (?)", req.ExecStatuses)
	}
	// relIDs
	if len(req.RelIDs) > 0 {
		baseSQL = baseSQL.Where("`rel`.`id` IN (?)", req.RelIDs)
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
		planCaseRels        []TestPlanCaseRelDetail
		total               uint64
		pagingErr, countErr error
	)

	// do paging
	go func() {
		defer wg.Done()

		// offset, limit
		offset := (req.PageNo - 1) * req.PageSize
		limit := req.PageSize
		pagingErr = pagingSQL.Offset(offset).Limit(limit).Find(&planCaseRels).Error
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
		return nil, 0, apierrors.ErrPagingTestPlanCaseRels.InternalError(pagingErr)
	}
	if countErr != nil {
		return nil, 0, apierrors.ErrPagingTestPlanCaseRels.InternalError(countErr)
	}

	return planCaseRels, total, nil
}

func validateTestPlanCaseRelPagingRequest(req apistructs.TestPlanCaseRelPagingRequest) error {
	if req.TestPlanID == 0 {
		return apierrors.ErrPagingTestPlanCaseRels.MissingParameter("testPlanID")
	}
	for _, priority := range req.Priorities {
		if !priority.IsValid() {
			return apierrors.ErrPagingTestPlanCaseRels.InvalidParameter(fmt.Sprintf("priority: %s", priority))
		}
	}
	if req.OrderByPriorityAsc != nil && req.OrderByPriorityDesc != nil {
		return apierrors.ErrPagingTestPlanCaseRels.InvalidParameter("order by priority ASC or DESC?")
	}
	if req.OrderByUpdaterIDAsc != nil && req.OrderByUpdaterIDDesc != nil {
		return apierrors.ErrPagingTestPlanCaseRels.InvalidParameter("order by updaterID ASC or DESC?")
	}
	if req.OrderByUpdatedAtAsc != nil && req.OrderByUpdatedAtDesc != nil {
		return apierrors.ErrPagingTestPlanCaseRels.InvalidParameter("order by updatedAt ASC or DESC?")
	}
	if req.OrderByIDAsc != nil && req.OrderByIDDesc != nil {
		return apierrors.ErrPagingTestPlanCaseRels.InvalidParameter("order by id ASC or DESC?")
	}
	if req.OrderByTestSetIDAsc != nil && req.OrderByTestSetIDDesc != nil {
		return apierrors.ErrPagingTestPlanCaseRels.InvalidParameter("order by testSetID ASC or DESC?")
	}
	if req.OrderByTestSetNameAsc != nil && req.OrderByTestSetNameDesc != nil {
		return apierrors.ErrPagingTestPlanCaseRels.InvalidParameter("order by testSetName ASC or DESC?")
	}

	return nil
}

func setDefaultForTestPlanCaseRelPagingRequest(req *apistructs.TestPlanCaseRelPagingRequest) {
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
	req.ExecutorIDs = ucauth.PolishUnassignedAsEmptyStr(req.ExecutorIDs)
	req.UpdaterIDs = ucauth.PolishUnassignedAsEmptyStr(req.UpdaterIDs)
}
