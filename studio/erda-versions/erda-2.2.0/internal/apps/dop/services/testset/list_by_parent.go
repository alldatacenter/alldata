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
	"sort"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/pkg/strutil"
)

// ListTestSetByLeafTestSetIDs 根据叶子测试集反查某一父测试集的下一级测试集列表
// return: layer(by ParentID), error
func (svc *Service) ListTestSetByLeafTestSetIDs(parentID uint64, leafTestSetIDs []uint64) ([]apistructs.TestSet, error) {
	knownTestSetMap := make(map[uint64]apistructs.TestSet)
	if err := svc.RecursiveFindParents(leafTestSetIDs, knownTestSetMap); err != nil {
		return nil, err
	}

	// 不存在
	if parentID != 0 {
		if _, ok := knownTestSetMap[parentID]; !ok {
			return nil, nil
		}
	}

	var layer []apistructs.TestSet
	for _, ts := range knownTestSetMap {
		if ts.ParentID == parentID {
			layer = append(layer, ts)
		}
	}
	sort.SliceStable(layer, func(i, j int) bool {
		return layer[i].Order < layer[j].Order
	})

	return layer, nil
}

func (svc *Service) RecursiveFindParents(leafTestSetIDs []uint64, knownTestSets map[uint64]apistructs.TestSet) error {
	// 校验
	if knownTestSets == nil {
		return fmt.Errorf("knownTestSets is empty")
	}

	// 判断所有叶子是否已查询过
	var filterLeafTestSetIDs []uint64
	for _, tsID := range leafTestSetIDs {
		if _, ok := knownTestSets[tsID]; !ok {
			filterLeafTestSetIDs = append(filterLeafTestSetIDs, tsID)
		}
	}

	// 查询所有叶子
	testSets, err := svc.db.ListTestSetByIDs(filterLeafTestSetIDs)
	if err != nil {
		return err
	}

	// 更新 knownTestSets
	for _, ts := range testSets {
		knownTestSets[ts.ID] = svc.convert(ts)
	}

	// 获取父节点
	var parentIDs []uint64
	for _, ts := range testSets {
		if ts.ParentID != 0 {
			parentIDs = append(parentIDs, ts.ParentID)
		}
	}
	// ignore zero，即父节点为 0 的不需要递归查询
	parentIDs = strutil.DedupUint64Slice(parentIDs, true)
	var filterParentIDs []uint64
	for _, parentID := range parentIDs {
		if _, ok := knownTestSets[parentID]; !ok {
			filterParentIDs = append(filterParentIDs, parentID)
		}
	}
	if len(filterParentIDs) == 0 {
		return nil
	}

	// 递归查询父节点
	return svc.RecursiveFindParents(filterParentIDs, knownTestSets)
}

// FindAncestors find ancestors for testSet.
func (svc *Service) FindAncestors(tsID uint64) ([]apistructs.TestSet, error) {
	ancestorsMap := make(map[uint64]apistructs.TestSet)
	if err := svc.RecursiveFindParents([]uint64{tsID}, ancestorsMap); err != nil {
		return nil, err
	}
	var ancestors []apistructs.TestSet
	testSet := ancestorsMap[tsID]
	parentID := testSet.ParentID
	for parentID != 0 {
		parent := ancestorsMap[parentID]
		// add parent to results
		ancestors = append(ancestors, parent)
		// find next parent
		parentID = parent.ParentID
	}
	// add fake root testSet
	ancestors = append(ancestors, apistructs.TestSet{
		ID:        0,
		Name:      "/",
		ProjectID: testSet.ProjectID,
		ParentID:  0,
		Recycled:  testSet.Recycled,
		Directory: "/",
		Order:     0,
	})
	return ancestors, nil
}
