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
	"path/filepath"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dao"
)

// GenerateTestSetName 生成测试集名，追加 (N)
func (svc *Service) GenerateTestSetName(projectID, parentTsID, testSetID uint64, requestName string) (string, error) {
	finalName := requestName
	for {
		// find by name
		exist, err := svc.db.GetTestSetByNameAndParentIDAndProjectID(projectID, parentTsID, apistructs.RecycledNo, finalName)
		if err != nil {
			return "", err
		}
		// not exist
		if exist == nil {
			return finalName, nil
		}
		// exist but is itself
		if exist.ID == testSetID {
			return finalName, nil
		}
		// exist a ain
		finalName, err = getTitleName(finalName)
		if err != nil {
			return "", err
		}
	}
}

// generateTestSetDirectory 根据父测试集生成目录
func generateTestSetDirectory(parentSet *dao.TestSet, name string) string {
	parentDirectory := "/"
	if parentSet != nil {
		parentDirectory = parentSet.Directory
	}
	return filepath.Join("/", parentDirectory, name)
}

// updateChildDirectory 更新子测试集路径
func (svc *Service) updateChildDirectory(projectID, testSetID uint64) error {
	ts, err := svc.ensureGetTestSet(projectID, testSetID)
	if err != nil {
		return err
	}
	// 更新子测试集路径名称
	testSets, err := svc.db.ListTestSets(apistructs.TestSetListRequest{
		Recycled:   apistructs.RecycledNo,
		ParentID:   &testSetID,
		ProjectID:  &projectID,
		TestSetIDs: nil,
	})
	if err != nil {
		return err
	}
	for _, item := range testSets {
		item.Directory = generateTestSetDirectory(ts, item.Name)
		item.UpdaterID = ts.UpdaterID
		if err := svc.db.UpdateTestSet(&item); err != nil {
			logrus.Errorf("update child testset error, parentID is %d, childID is %d", ts.ID, item.ID)
		}

		svc.updateChildDirectory(projectID, item.ID)
	}
	return nil
}
