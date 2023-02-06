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

	"github.com/jinzhu/gorm"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/database/dbengine"
	"github.com/erda-project/erda/pkg/strutil"
)

const (
	// related to: 20211109-manual-test-fix-slow-sql.sql #9
	maxTestSetDirectoryLength = 5000
)

func ValidateTestSetDirectoryLength(dir string) error {
	return strutil.Validate(dir, strutil.MaxRuneCountValidator(maxTestSetDirectoryLength))
}

// TestSet 测试集
type TestSet struct {
	dbengine.BaseModel
	// 测试集的中文名,可重名
	Name string
	// 上一级的所属测试集id,顶级时为0
	ParentID uint64
	// 是否回收
	Recycled bool
	// 项目ID
	ProjectID uint64
	// 路径地址
	Directory string
	// 排序编号
	OrderNum int
	// 创建人ID
	CreatorID string
	// 更新人ID
	UpdaterID string
}

// TableName 数据库表名
func (TestSet) TableName() string {
	return "dice_test_sets"
}

func FakeRootTestSet(projectID uint64, recycled bool) TestSet {
	return TestSet{
		BaseModel: dbengine.BaseModel{ID: 0},
		Name:      "/",
		ParentID:  0,
		Recycled:  recycled,
		ProjectID: projectID,
		Directory: "/",
		OrderNum:  0,
	}
}

// CreateTestSet insert testset
func (db *DBClient) CreateTestSet(testset *TestSet) error {
	return db.Create(testset).Error
}

// UpdateTestSet update testset
func (db *DBClient) UpdateTestSet(testset *TestSet) error {
	return db.Save(testset).Error
}

// ListTestSetsRecursive 获取测试集列表，可选是否包含子测试集
func (db *DBClient) ListTestSetsRecursive(req apistructs.TestSetListRequest) ([]uint64, []TestSet, error) {
	if req.ProjectID == nil || *req.ProjectID == 0 {
		return nil, nil, fmt.Errorf("missing projectID")
	}

	var (
		allTestSetIDs []uint64
		allTestSets   []TestSet
	)

	if req.ParentID != nil && *req.ParentID != 0 {
		// 查询父级
		parent, err := db.GetTestSetByID(*req.ParentID)
		if err != nil {
			return nil, nil, err
		}
		allTestSetIDs = append(allTestSetIDs, parent.ID)
		allTestSets = append(allTestSets, *parent)
	} else {
		allTestSetIDs = append(allTestSetIDs, 0)
		// add fake root testSet
		allTestSets = append(allTestSets, FakeRootTestSet(*req.ProjectID, req.Recycled))
	}

	if req.NoSubTestSets {
		return allTestSetIDs, allTestSets, nil
	}

	subTestSets, err := db.GetTestSetByParentIDAndProjectIDAsc([]uint64{*req.ParentID}, *req.ProjectID, req.Recycled, req.TestSetIDs)
	if err != nil {
		return nil, nil, err
	}

	if len(subTestSets) == 0 {
		return allTestSetIDs, allTestSets, nil
	}

	subTsIDs := []uint64{}
	for _, subTs := range subTestSets {
		allTestSetIDs = append(allTestSetIDs, subTs.ID)
		allTestSets = append(allTestSets, subTs)
		subTsIDs = append(subTsIDs, subTs.ID)
	}

	for {
		if len(subTsIDs) == 0 {
			break
		}
		subTestSets, err := db.GetTestSetByParentIDAndProjectIDAsc(subTsIDs, *req.ProjectID, req.Recycled, req.TestSetIDs)
		if err != nil {
			return nil, nil, err
		}
		// 更新到子set
		subTsIDs = []uint64{}
		for _, subTs := range subTestSets {
			allTestSetIDs = append(allTestSetIDs, subTs.ID)
			allTestSets = append(allTestSets, subTs)
			subTsIDs = append(subTsIDs, subTs.ID)
		}
	}

	return allTestSetIDs, allTestSets, nil
}

func (db *DBClient) ListTestSetByIDs(ids []uint64) ([]TestSet, error) {
	var results []TestSet
	if err := db.Where("`id` IN (?)", ids).Find(&results).Error; err != nil {
		return nil, err
	}
	return results, nil
}

// ListTestSets
func (db *DBClient) ListTestSets(req apistructs.TestSetListRequest) ([]TestSet, error) {
	// 参数校验
	if req.ProjectID == nil || *req.ProjectID == 0 {
		return nil, fmt.Errorf("missing projectID")
	}

	// 获取匹配搜索结果总量
	var testsets []TestSet

	sql := db.Where("`project_id` = ?", *req.ProjectID).Where("`recycled` = ?", req.Recycled)

	if req.ParentID != nil {
		sql = sql.Where("`parent_id` = ?", *req.ParentID)
	}
	if len(req.TestSetIDs) > 0 {
		sql = sql.Where("`id` IN (?)", strutil.DedupUint64Slice(req.TestSetIDs, true))
	}

	sql = sql.Order("`order_num` DESC")

	if err := sql.Find(&testsets).Error; err != nil {
		return nil, err
	}
	return testsets, nil
}

// GetTestSetByParentID 根据父ID和项目ID获取测试集
func (db *DBClient) GetTestSetByParentID(parentID, projectID uint64) (*[]TestSet, error) {
	// 获取匹配搜索结果总量
	var testsets []TestSet

	client := db.Where("project_id = ?", projectID).
		Where("parent_id = ?", parentID)

	if err := client.
		Find(&testsets).Error; err != nil {
		return nil, err
	}
	return &testsets, nil
}

// GetTestSetByParentIDAndProjectIDAsc 根据父ID和项目ID获取子测试集信息，升序排列
func (db *DBClient) GetTestSetByParentIDAndProjectIDAsc(parentIDs []uint64, projectID uint64, recycled bool, testSetIDs []uint64) ([]TestSet, error) {
	// 获取匹配搜索结果总量
	var testSets []TestSet
	sql := db.
		Where("project_id = ?", projectID).
		Where("parent_id IN (?)", parentIDs).
		Where("recycled = ?", recycled).
		Order("order_num asc")
	if len(testSetIDs) > 0 {
		sql = sql.Where("`id` IN (?)", testSetIDs)
	}
	if err := sql.Find(&testSets).Error; err != nil {
		return nil, err
	}
	return testSets, nil
}

// GetTestSetByParentIDsAndProjectID 根据父ID和项目ID获取测试集
func (db *DBClient) GetTestSetByParentIDsAndProjectID(parentIDs []uint64, projectID uint64, recycled bool) ([]TestSet, error) {
	// 获取匹配搜索结果总量
	var testsets []TestSet
	if err := db.
		Where("project_id = ?", projectID).
		Where("parent_id in (?)", parentIDs).
		Where("recycled = ?", recycled).
		Order("order_num asc").
		Find(&testsets).Error; err != nil {
		return nil, err
	}
	return testsets, nil
}

// GetMaxOrderNumUnderParentTestSet 返回当前父测试集下的最大 order num
// 若当前父测试集下没有子测试集，则返回 -1
func (db *DBClient) GetMaxOrderNumUnderParentTestSet(projectID, parentID uint64, recycled bool) (int, error) {
	var ts TestSet
	if err := db.
		Where("project_id = ?", projectID).
		Where("parent_id = ?", parentID).
		Where("recycled = ?", recycled).
		Order("order_num desc").
		First(&ts).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return -1, nil
		}
		return 0, err
	}
	return ts.OrderNum, nil

}

// ListTestSets 根据父ID和项目ID获取测试集
func (db *DBClient) GetTestSetByNameAndParentIDAndProjectID(projectID, parentID uint64, recycled bool, name string) (*TestSet, error) {
	// 获取匹配搜索结果总量
	var testset TestSet
	if err := db.
		Where("project_id = ?", projectID).
		Where("parent_id = ?", parentID).
		Where("name = ?", name).
		Where("recycled = ?", recycled).
		Order("order_num desc").
		First(&testset).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, nil
		}
		return nil, err
	}
	return &testset, nil
}

// GetTestSetByID 根据ID获取测试集
func (db *DBClient) GetTestSetByID(id uint64) (*TestSet, error) {
	var testset TestSet
	if err := db.
		Where("id = ?", id).
		Find(&testset).Error; err != nil {
		return nil, err
	}
	return &testset, nil
}

// GetTestSetDirectoryByID 根据ID获取测试集的路径
func (db *DBClient) GetTestSetDirectoryByID(id uint64) (string, error) {
	var testset TestSet
	if err := db.Select("directory").
		Where("id = ?", id).
		Find(&testset).Error; err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return "/", nil
		}
		return "", err
	}
	return testset.Directory, nil
}

func (db *DBClient) GetAllTestSets() ([]TestSet, error) {
	var testsets []TestSet
	if err := db.Where("recycled = ?", false).
		Find(&testsets).Error; err != nil {
		return nil, err
	}

	return testsets, nil
}

func (db *DBClient) RecycleTestSet(testSetID uint64, newParentID *uint64) error {
	sql := db.Model(&TestSet{}).Where("`id` = ?", testSetID)
	updateFields := map[string]interface{}{"recycled": true}
	if newParentID != nil {
		updateFields["parent_id"] = *newParentID
	}
	return sql.Updates(updateFields).Error
}

func (db *DBClient) CleanTestSetFromRecycleBin(testSetID uint64) error {
	return db.Where("`id` = ?", testSetID).Delete(TestSet{}).Error
}

func (db *DBClient) RecoverTestSet(testSetID, targetTestSetID uint64, name string) error {
	return db.Model(&TestSet{}).Where("`id` = ?", testSetID).Updates(map[string]interface{}{
		"recycled":  false,
		"parent_id": targetTestSetID,
		"name":      name,
	}).Error
}
