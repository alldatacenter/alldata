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
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
)

// BatchCleanFromRecycleBin 从回收站彻底删除测试用例
func (svc *Service) BatchCleanFromRecycleBin(req apistructs.TestCaseBatchCleanFromRecycleBinRequest) error {
	// 确认所有 testCaseIDs 存在
	tcs, err := svc.db.ListTestCasesByIDs(req.TestCaseIDs)
	if err != nil {
		return apierrors.ErrBatchCleanTestCasesFromRecycleBin.InvalidParameter(err)
	}

	// 校验 recycled
	var notRecycledIDs []uint64
	for _, tc := range tcs {
		if !*tc.Recycled {
			notRecycledIDs = append(notRecycledIDs, uint64(tc.ID))
		}
	}
	if len(notRecycledIDs) > 0 {
		return apierrors.ErrBatchCleanTestCasesFromRecycleBin.InvalidParameter(fmt.Errorf("some testcase not in recycle bin, ids: %v", notRecycledIDs))
	}

	// 批量删除缺陷测试计划用例关联
	if err := svc.db.DeleteIssueTestCaseRelationsByTestCaseIDs(req.TestCaseIDs); err != nil {
		return apierrors.ErrBatchCleanTestCasesFromRecycleBin.InternalError(err)
	}

	// 批量删除测试用例
	if err := svc.db.BatchDeleteTestCases(req.TestCaseIDs); err != nil {
		return apierrors.ErrBatchCleanTestCasesFromRecycleBin.InternalError(err)
	}

	return nil
}
