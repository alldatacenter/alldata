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
	"sort"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/services/i18n"
	"github.com/erda-project/erda/pkg/strutil"
	"github.com/erda-project/erda/pkg/xmind"
)

func (svc *Service) convert2XMind(tcs []apistructs.TestCaseWithSimpleSetInfo, locale string) (xmind.XMLContent, error) {
	l := svc.bdl.GetLocale(locale)
	// 定义临时结构体
	type TmpTestCases struct {
		TestSetID  uint64
		TestSetDir string
		TestCases  []apistructs.TestCase
	}
	// 按照父测试集 ID 进行分类
	tcGroupMapByTsID := make(map[uint64]*TmpTestCases)
	for _, tc := range tcs {
		tmp := tcGroupMapByTsID[tc.TestSetID]
		if tmp == nil {
			tmp = &TmpTestCases{}
		}
		tmp.TestSetID = tc.TestSetID
		tmp.TestCases = append(tmp.TestCases, tc.TestCase)
		tmp.TestSetDir = tc.Directory
		tcGroupMapByTsID[tc.TestSetID] = tmp
	}
	// 将 map 转换为 list，并用 testSetID 倒序排序
	tmpTcsList := make([]*TmpTestCases, 0, len(tcGroupMapByTsID))
	for _, tmp := range tcGroupMapByTsID {
		tmpTcsList = append(tmpTcsList, tmp)
	}
	sort.Slice(tmpTcsList, func(i, j int) bool {
		// 倒序排序
		return tmpTcsList[i].TestSetID < tmpTcsList[j].TestSetID
	})

	var content xmind.XMLContent
	rootTopic := &xmind.XMLTopic{Title: l.Get(i18n.I18nKeyTestCaseSheetName)}
	content.Sheet.Topic = rootTopic

	for _, tmpTcs := range tmpTcsList {
		// 插入目录节点
		currentTopic := rootTopic
		for _, dir := range strutil.Split(tmpTcs.TestSetDir, "/", true) {
			currentTopic = currentTopic.AddAttachedChildTopic(dir, true)
		}
		// 插入测试用例节点
		for _, tc := range tmpTcs.TestCases {
			insertTestCaseTopic(currentTopic, tc)
		}
	}

	return content, nil
}
