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

package common

import "github.com/erda-project/erda-proto-go/core/hepa/pb"

var (
	serialVersionUID  int64 = -802234107812549630
	DEFAULT_PAGE_SIZE int64 = 15
	DEFAULT_TOTAL_NUM int64 = 0
)

type Page pb.Page

func NewPage() *pb.Page {
	return &pb.Page{
		PageSize:   DEFAULT_PAGE_SIZE,
		CurPage:    1,
		TotalNum:   DEFAULT_TOTAL_NUM,
		StartIndex: 0,
		EndIndex:   0,
	}
}

func NewPage2(PageSize int64, CurPage int64) *pb.Page {
	return &pb.Page{
		PageSize:   PageSize,
		CurPage:    CurPage,
		TotalNum:   DEFAULT_TOTAL_NUM,
		StartIndex: 0,
		EndIndex:   0,
	}
}

func NewPage3(PageSize int64, CurPage int64, TotalNum int64) *pb.Page {
	return &pb.Page{
		PageSize:   PageSize,
		CurPage:    CurPage,
		TotalNum:   TotalNum,
		StartIndex: 0,
		EndIndex:   0,
	}
}

/**
 * 获取分页后页面总数
 */
func (page *Page) GetTotalPageNum() int64 {
	if page.TotalNum == 0 {
		return 0
	}
	num := page.TotalNum / page.PageSize
	if (page.TotalNum % page.PageSize) != 0 {
		num++
	}
	return num
}

/**
 * 设置每页条数
 */
func (page *Page) SetPageSize(size int64) {
	page.PageSize = size
	page.caculatIndex()
}

/**
 * 获取每页条数
 */
func (page *Page) GetPageSize() int64 {
	return page.PageSize
}

/**
 * 获取分页当前页码
 */
func (page *Page) GetCurPage() int64 {
	return page.CurPage
}

/**
 * 设置当前页码
 */
func (page *Page) SetCurPage(num int64) {
	page.CurPage = num
	page.caculatIndex()
}

/**
 * 获取查询结果总条数
 */
func (page *Page) GetTotalNum() int64 {
	return page.TotalNum
}

/**
 * 设置结果总条数
 */
func (page *Page) SetTotalNum(num int64) {
	page.TotalNum = num
	page.caculatIndex()
}

func (page *Page) caculatIndex() {
	if page.CurPage < 1 {
		page.CurPage = 1
	}
	if page.PageSize <= 0 {
		page.PageSize = DEFAULT_PAGE_SIZE
	}
	page.StartIndex = (page.CurPage - 1) * page.PageSize
	page.EndIndex = page.CurPage * page.PageSize
	if page.EndIndex > page.TotalNum {
		page.EndIndex = page.TotalNum
	}

}

func (page *Page) GetStartIndex() int64 {
	return page.StartIndex
}

func (page *Page) GetEndIndex() int64 {
	return page.EndIndex
}
