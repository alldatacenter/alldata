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

package excel

import (
	"os"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestExportExcelByCell(t *testing.T) {
	file, err := os.OpenFile("/tmp/test1.xlsx", os.O_CREATE|os.O_RDWR|os.O_TRUNC, 0755)
	assert.NoError(t, err)
	var data [][]Cell
	// row1
	row1 := []Cell{
		NewVMergeCell("用例编号", 1),
		NewVMergeCell("用例名称", 1),
		NewVMergeCell("测试集", 1),
		NewVMergeCell("优先级", 1),
		NewVMergeCell("前置条件", 1),
		NewHMergeCell("步骤与结果", 1),
		EmptyCell(),
		NewHMergeCell("接口测试", 7),
		EmptyCell(),
		EmptyCell(),
		EmptyCell(),
		EmptyCell(),
		EmptyCell(),
		EmptyCell(),
		EmptyCell(),
	}
	row2 := []Cell{
		EmptyCell(),
		EmptyCell(),
		EmptyCell(),
		EmptyCell(),
		EmptyCell(),
		NewCell("操作步骤"),
		NewCell("预期结果"),
		NewCell("接口名称"),
		NewCell("请求头信息"),
		NewCell("方法"),
		NewCell("接口地址"),
		NewCell("接口参数"),
		NewCell("请求体"),
		NewCell("out 参数"),
		NewCell("断言"),
	}
	data = append(data, row1, row2)
	err = ExportExcelByCell(file, data, "测试用例")
	assert.NoError(t, err)
}
