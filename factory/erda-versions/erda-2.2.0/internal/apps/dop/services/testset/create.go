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

	"github.com/jinzhu/gorm"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dao"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
)

// Create 创建测试集
func (svc *Service) Create(req apistructs.TestSetCreateRequest) (*apistructs.TestSet, error) {
	// 参数校验
	if req.ProjectID == nil {
		return nil, apierrors.ErrCreateTestCase.MissingParameter("projectID")
	}
	if req.ParentID == nil {
		return nil, apierrors.ErrCreateTestSet.MissingParameter("parentID")
	}
	if req.Name == "" {
		return nil, apierrors.ErrCreateTestSet.MissingParameter("name")
	}

	// 创建入库 model
	newTsModel, err := svc.makeTestSetForCreate(*req.ProjectID, *req.ParentID, req.Name, req.IdentityInfo.UserID, "")
	if err != nil {
		return nil, apierrors.ErrCreateTestSet.InternalError(err)
	}
	if err := svc.db.CreateTestSet(newTsModel); err != nil {
		return nil, apierrors.ErrCreateTestSet.InternalError(err)
	}
	converted := svc.convert(*newTsModel)
	return &converted, nil
}

// ensureGetTestSet return queried testSet or fakeRootTestSet if testSetID=0
func (svc *Service) ensureGetTestSet(projectID, testSetID uint64) (*dao.TestSet, error) {
	if testSetID == 0 {
		fakeRootTs := dao.FakeRootTestSet(projectID, false)
		return &fakeRootTs, nil
	}
	ts, err := svc.db.GetTestSetByID(testSetID)
	if err != nil {
		if gorm.IsRecordNotFoundError(err) {
			return nil, fmt.Errorf("testset not exist, id: %d", testSetID)
		}
		returnErr := fmt.Errorf("failed to parent testset, id: %d, err: %v", testSetID, err)
		logrus.Errorf("%v", returnErr)
		return nil, returnErr
	}
	return ts, nil
}

// makeTestSetForCreate
func (svc *Service) makeTestSetForCreate(projectID, parentTsID uint64, name string, creatorID, updaterID string) (*dao.TestSet, error) {
	parentTs, err := svc.ensureGetTestSet(projectID, parentTsID)
	if err != nil {
		return nil, err
	}
	// orderNum
	currentMaxOrderNum, err := svc.db.GetMaxOrderNumUnderParentTestSet(projectID, parentTsID, false)
	if err != nil {
		return nil, fmt.Errorf("failed to get max order num under parent testset, parentTestSetID: %d, err: %v", parentTsID, err)
	}
	maxOrderNum := currentMaxOrderNum + 1
	// name
	polishedName, err := svc.GenerateTestSetName(projectID, parentTsID, 0, name)
	if err != nil {
		return nil, fmt.Errorf("failed to polish testset name, name: %s, err: %v", name, err)
	}
	// directory
	directory := generateTestSetDirectory(parentTs, polishedName)
	if err := dao.ValidateTestSetDirectoryLength(directory); err != nil {
		return nil, fmt.Errorf("directory too long, err: %v", err)
	}

	newTs := dao.TestSet{
		Name:      polishedName,
		ProjectID: projectID,
		ParentID:  parentTsID,
		OrderNum:  maxOrderNum,
		Recycled:  false,
		Directory: directory,
		CreatorID: creatorID,
		UpdaterID: updaterID,
	}
	return &newTs, nil
}
