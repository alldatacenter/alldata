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

package testcase

import (
	"fmt"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/dao"
)

func (svc *Service) batchConvertTestCases(projectID uint64, models []dao.TestCase) ([]*apistructs.TestCase, error) {
	if len(models) == 0 {
		return nil, nil
	}
	// batch query api infos
	var tcIDs []uint64
	for _, model := range models {
		tcIDs = append(tcIDs, model.ID)
	}
	apis, err := svc.BatchListAPIs(projectID, tcIDs)
	if err != nil {
		return nil, err
	}

	// batch convert models
	var tcs []*apistructs.TestCase
	for _, model := range models {
		// apiCount
		var apiCount apistructs.TestCaseAPICount
		for _, api := range apis[model.ID] {
			apiCount.Total++
			switch api.Status {
			case apistructs.ApiTestCreated:
				apiCount.Created++
			case apistructs.ApiTestRunning:
				apiCount.Running++
			case apistructs.ApiTestPassed:
				apiCount.Passed++
			case apistructs.ApiTestFailed:
				apiCount.Failed++
			}
		}
		// convert
		testCase := apistructs.TestCase{
			ID:             uint64(model.ID),
			Name:           model.Name,
			Priority:       model.Priority,
			PreCondition:   model.PreCondition,
			Desc:           model.Desc,
			Recycled:       model.Recycled,
			TestSetID:      model.TestSetID,
			ProjectID:      model.ProjectID,
			CreatorID:      model.CreatorID,
			UpdaterID:      model.UpdaterID,
			BugIDs:         nil,
			LabelIDs:       nil,
			Attachments:    nil,
			StepAndResults: model.StepAndResults,
			Labels:         nil,
			APIs:           apis[model.ID],
			APICount:       apiCount,
			CreatedAt:      model.CreatedAt,
			UpdatedAt:      model.UpdatedAt,
		}
		tcs = append(tcs, &testCase)
	}

	return tcs, nil
}

// convertTestCase
// consider if you can use batchConvertTestCases firstly.
func (svc *Service) convertTestCase(model dao.TestCase) (*apistructs.TestCase, error) {
	tcs, err := svc.batchConvertTestCases(model.ProjectID, []dao.TestCase{model})
	if err != nil {
		return nil, err
	}
	if len(tcs) == 0 {
		return nil, fmt.Errorf("not found model")
	}
	return tcs[0], nil
}
