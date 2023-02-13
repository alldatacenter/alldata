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

// Cell 单元格
//    A  B  C
// 1  A1 B1 C1
// 1  A2 B2 C2
// 1  A3 B3 C3
type Cell struct {
	// 单元格的值
	Value string
	// 水平合并其他几个单元格
	// 以 A0 为例，默认为 0 表示不合并其他单元格，1 表示合并 A0,B1 两个单元格，2 表示合并 A0,B1,C2 三个单元格
	HorizontalMergeNum int
	// 垂直合并其他几个单元格
	// 以 A0 为例，默认为 0 表示不合并其他单元格，1 表示合并 A0,A2 两个单元格，2 表示合并 A0,A1,A2 三个单元格
	VerticalMergeNum int

	// TODO style here
}

func NewCell(value string) Cell {
	return Cell{Value: value}
}
func EmptyCell() Cell {
	return Cell{}
}
func EmptyCells(count int) []Cell {
	var cells []Cell
	for i := 0; i < count; i++ {
		cells = append(cells, Cell{})
	}
	return cells
}

// NewHMergeCell 需要配合 hMergeNum 个 EmptyCell 使用
func NewHMergeCell(value string, hMergeNum int) Cell {
	return Cell{Value: value, HorizontalMergeNum: hMergeNum}
}
func NewVMergeCell(value string, vMergeNum int) Cell {
	return Cell{Value: value, VerticalMergeNum: vMergeNum}
}
