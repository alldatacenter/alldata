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

package testset

import (
	"fmt"
	"strconv"
	"strings"

	"github.com/jinzhu/gorm"
	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
)

func (svc *Service) Get(id uint64) (*apistructs.TestSet, error) {
	ts, err := svc.db.GetTestSetByID(id)
	if err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, apierrors.ErrGetTestSet.NotFound()
		}
		return nil, apierrors.ErrGetTestSet.InternalError(err)
	}
	converted := svc.convert(*ts)
	return &converted, nil
}

// List 测试集列表返回
func (svc *Service) List(req apistructs.TestSetListRequest) ([]apistructs.TestSet, error) {
	if req.ProjectID == nil {
		return nil, apierrors.ErrListTestSets.MissingParameter("projectID")
	}

	var (
		testSets []dao.TestSet
		err      error
	)
	testSets, err = svc.db.ListTestSets(req)
	if err != nil {
		return nil, apierrors.ErrListTestSets.InternalError(err)
	}

	results := make([]apistructs.TestSet, 0, len(testSets))
	for _, item := range testSets {
		results = append(results, svc.convert(item))
	}
	return results, nil
}

// Update 测试集更新
func (svc *Service) Update(req apistructs.TestSetUpdateRequest) error {
	// 参数校验
	if (req.Name == nil || *req.Name == "") && req.MoveToParentID == nil {
		return nil
	}
	// 当前测试集信息
	if req.TestSetID == 0 {
		return apierrors.ErrUpdateTestSet.MissingParameter("testSetID")
	}
	ts, err := svc.db.GetTestSetByID(req.TestSetID)
	if err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return apierrors.ErrUpdateTestSet.NotFound()
		}
		logrus.Errorf("failed to query testset, id: %d, err: %v", req.TestSetID, err)
		return apierrors.ErrUpdateTestSet.InternalError(fmt.Errorf("failed to query testset, id: %d", req.TestSetID))
	}

	// 移动目录校验
	if req.MoveToParentID != nil {
		if *req.MoveToParentID == req.TestSetID {
			return apierrors.ErrUpdateTestSet.InvalidParameter("cannot move to itself")
		}
		findInSub, err := svc.findTargetTestSetIDInSubTestSets([]uint64{ts.ID}, ts.ProjectID, *req.MoveToParentID)
		if err != nil {
			return apierrors.ErrUpdateTestSet.InternalError(err)
		}
		if findInSub {
			return apierrors.ErrUpdateTestSet.InvalidParameter("cannot move to sub testset")
		}
	}

	// 父目录 ID
	tsParentID := ts.ParentID
	if req.MoveToParentID != nil {
		tsParentID = *req.MoveToParentID
	}
	// 获取父测试集信息
	parentTs, err := svc.ensureGetTestSet(ts.ProjectID, tsParentID)
	if err != nil {
		return apierrors.ErrUpdateTestSet.InvalidParameter(err)
	}

	// 测试集名
	tsName := ts.Name
	if req.Name != nil && *req.Name != "" {
		tsName = *req.Name
	}

	// 更新 orderNum
	currentMaxOrderNum, err := svc.db.GetMaxOrderNumUnderParentTestSet(ts.ProjectID, tsParentID, false)
	if err != nil {
		return apierrors.ErrUpdateTestSet.InternalError(err)
	}
	maxOrderNum := currentMaxOrderNum + 1

	// 校验测试集名是否重复
	polishedName, err := svc.GenerateTestSetName(ts.ProjectID, tsParentID, ts.ID, tsName)
	if err != nil {
		return apierrors.ErrUpdateTestSet.InvalidParameter(err)
	}
	ts.Name = polishedName
	ts.ParentID = tsParentID
	ts.Directory = generateTestSetDirectory(parentTs, ts.Name)
	ts.OrderNum = maxOrderNum
	ts.UpdaterID = req.IdentityInfo.UserID
	if err := svc.db.UpdateTestSet(ts); err != nil {
		return err
	}

	// 更新子测试集目录
	if err := svc.updateChildDirectory(ts.ProjectID, ts.ID); err != nil {
		logrus.Errorf("failed to update child directory when update testset, testSetID: %d, err: %v", ts.ID, err)
	}

	return nil
}

// Clean 测试集删除
func (svc *Service) Clean(userID, testsetID, projectID uint64) error {
	if testsetID <= 0 {
		return errors.New("测试集不存在.")
	}
	testSetLocal, err := svc.db.GetTestSetByID(testsetID)
	if err != nil {
		return err
	}
	if testSetLocal == nil {
		return errors.New("测试集不存在.")
	}
	// 逻辑删除测试集 && 删除测试集下的用例
	if err := svc.db.UpdateTestSet(testSetLocal); err != nil {
		return err
	}
	if err := svc.db.CleanTestCasesByTestSetID(testSetLocal.ProjectID, testSetLocal.ID); err != nil {
		logrus.Errorf("clean usecase fail, testSetID: %d", testSetLocal.ID)
	}

	// 回收子测试集
	svc.cleanChildTestSet(testSetLocal)

	return nil
}

// Restore 回收站测试集恢复
func (svc *Service) Recover(userID, testsetID, projectID, targetTestSetID uint64) error {
	if testsetID <= 0 {
		return errors.New("测试集不存在.")
	}
	testSetLocal, err := svc.db.GetTestSetByID(testsetID)
	if err != nil {
		return err
	}
	if testSetLocal == nil {
		return errors.New("测试集不存在.")
	}
	// 更新测试集名称
	testSetLocal.Recycled = false
	testSetLocal.ParentID = targetTestSetID
	if err := svc.db.UpdateTestSet(testSetLocal); err != nil {
		return err
	}
	if err := svc.db.RecoverTestCasesByTestSetID(testSetLocal.ProjectID, testSetLocal.ID); err != nil {
		logrus.Errorf("recover usecase fail, testSetID: %d", testSetLocal.ID)
	}

	// 恢复子测试集
	svc.restoreChildTestSet(testSetLocal)

	return nil
}

func (svc *Service) convert(ts dao.TestSet) apistructs.TestSet {
	return apistructs.TestSet{
		ID:        ts.ID,
		Name:      ts.Name,
		ProjectID: ts.ProjectID,
		ParentID:  ts.ParentID,
		Recycled:  ts.Recycled,
		Directory: ts.Directory,
		Order:     ts.OrderNum,
		CreatorID: ts.CreatorID,
		UpdaterID: ts.UpdaterID,
	}
}

// getTitleName 根据文件名去递增生产序号 例如传入文件夹(1) 变为 文件夹(2)
func getTitleName(requestName string) (string, error) {
	begin := strings.LastIndex(requestName, "(")
	end := strings.LastIndex(requestName, ")")
	if begin < 0 || end < 0 {
		return fmt.Sprintf("%s%s", requestName, "(1)"), nil
	} else {
		num, err := strconv.Atoi(requestName[begin+1 : end])
		if err != nil {
			return "", err
		}
		num = num + 1
		return fmt.Sprintf("%s%s%d%s", requestName[0:begin], "(", num, ")"), nil
	}
}

// cleanChildTestSet 彻底删除子测试集
func (svc *Service) cleanChildTestSet(testset *dao.TestSet) error {
	// 更新子测试集路径名称
	testSetList, err := svc.db.GetTestSetByParentID(testset.ID, testset.ProjectID)
	if err != nil {
		return err
	}
	if testSetList != nil && len(*testSetList) > 0 {
		for _, item := range *testSetList {
			item.UpdaterID = testset.UpdaterID
			if err := svc.db.UpdateTestSet(&item); err != nil {
				logrus.Errorf("update child testset error, parentID is %d, childID is %d", testset.ID, item.ID)
			}
			if err := svc.db.CleanTestCasesByTestSetID(item.ProjectID, item.ID); err != nil {
				logrus.Errorf("clean usecase fail, testSetID: %d", item.ID)
			}

			svc.recycledChildTestSet(&item)
		}
	}

	return nil
}

// recycledChildTestSet 回收子测试集
func (svc *Service) recycledChildTestSet(testset *dao.TestSet) error {
	// 更新子测试集路径名称
	testSets, err := svc.db.ListTestSets(apistructs.TestSetListRequest{
		Recycled:   apistructs.RecycledNo,
		ParentID:   &testset.ID,
		ProjectID:  &testset.ProjectID,
		TestSetIDs: nil,
	})
	if err != nil {
		return err
	}
	for _, item := range testSets {
		item.UpdaterID = testset.UpdaterID
		item.Recycled = apistructs.RecycledYes
		if err := svc.db.UpdateTestSet(&item); err != nil {
			logrus.Errorf("update child testset error, parentID is %d, childID is %d", testset.ID, item.ID)
		}
		if err := svc.db.RecycledTestCasesByTestSetID(item.ProjectID, item.ID); err != nil {
			logrus.Errorf("recycled usecase fail, testSetID: %d", item.ID)
		}

		// TODO 回收测试集下的测试用例
		svc.recycledChildTestSet(&item)
	}

	return nil
}

// restoreChildTestSet 恢复子测试集
func (svc *Service) restoreChildTestSet(testset *dao.TestSet) error {
	// 更新子测试集路径名称
	testSets, err := svc.db.ListTestSets(apistructs.TestSetListRequest{
		Recycled:   apistructs.RecycledYes,
		ParentID:   &testset.ID,
		ProjectID:  &testset.ProjectID,
		TestSetIDs: nil,
	})
	if err != nil {
		return err
	}
	for _, item := range testSets {
		item.UpdaterID = testset.UpdaterID
		item.Recycled = apistructs.RecycledNo
		if err := svc.db.UpdateTestSet(&item); err != nil {
			logrus.Errorf("update child testset error, parentID is %d, childID is %d", testset.ID, item.ID)
		}
		if err := svc.db.RecoverTestCasesByTestSetID(item.ProjectID, item.ID); err != nil {
			logrus.Errorf("recover usecase fail, testSetID: %d", item.ID)
		}

		svc.recycledChildTestSet(&item)
	}

	return nil
}

// pasteChildTestSet 复制子测试集
func (svc *Service) pasteChildTestSet(testset, newTestSetLocal *dao.TestSet) error {
	// 更新子测试集路径名称
	testSets, err := svc.db.ListTestSets(apistructs.TestSetListRequest{
		Recycled:   apistructs.RecycledNo,
		ParentID:   &testset.ID,
		ProjectID:  &testset.ProjectID,
		TestSetIDs: nil,
	})
	if err != nil {
		return err
	}
	for _, item := range testSets {
		newTestSet := dao.TestSet{
			Name:      item.Name,
			ParentID:  newTestSetLocal.ID,
			Recycled:  apistructs.RecycledNo,
			ProjectID: newTestSetLocal.ProjectID,
			Directory: generateTestSetDirectory(newTestSetLocal, item.Name),
			OrderNum:  item.OrderNum,
			CreatorID: newTestSetLocal.CreatorID,
			UpdaterID: newTestSetLocal.UpdaterID,
		}
		svc.db.CreateTestSet(&newTestSet)

		// TODO 回收测试集下的测试用例
		svc.pasteChildTestSet(&item, &newTestSet)
	}

	return nil
}

// findTargetTestSetIDInSubTestSets 是否在遍历子测试集中能找到目标测试集
func (svc *Service) findTargetTestSetIDInSubTestSets(testsetIDs []uint64, projectID, targetTestSetID uint64) (bool, error) {
	testSetList, err := svc.db.GetTestSetByParentIDsAndProjectID(testsetIDs, projectID, apistructs.RecycledNo)
	if err != nil {
		return false, err
	}
	if len(testSetList) > 0 {
		childList := make([]uint64, 0, len(testSetList))
		for _, item := range testSetList {
			if item.ID == targetTestSetID {
				return true, nil
			}
			childList = append(childList, item.ID)
		}
		if len(childList) == 0 {
			return false, nil
		}
		return svc.findTargetTestSetIDInSubTestSets(childList, projectID, targetTestSetID)
	}

	return false, nil
}
