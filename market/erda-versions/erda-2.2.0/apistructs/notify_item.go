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

package apistructs

// NotifyItem 通知项
type NotifyItem struct {
	ID               int64  `json:"id"`
	Name             string `json:"name"`
	DisplayName      string `json:"displayName"`
	Category         string `json:"category"`
	MarkdownTemplate string `json:"markdownTemplate"`
	EmailTemplate    string `json:"emailTemplate"`
	MobileTemplate   string `json:"mobileTemplate"`
	DingdingTemplate string `json:"dingdingTemplate"`
	MBoxTemplate     string `json:"mboxTemplate" gorm:"column:mbox_template"`
	// 语音通知模版
	VMSTemplate string `json:"vmsTemplate" gorm:"column:vms_template"`
	// 语音通知的被叫显号，语音模版属于公共号码池外呼的时候，被叫显号必须是空
	// 属于专属号码外呼的时候，被叫显号不能为空
	CalledShowNumber string `json:"calledShowNumber" gorm:"column:called_show_number"`
	ScopeType        string `json:"scopeType"`
	Label            string `json:"label"`
	Params           string `json:"params"`
}

// CreateNotifyItemRequest 创建通知项请求
type CreateNotifyItemRequest struct {
	Name           string `json:"name"`
	DisplayName    string `json:"displayName"`
	Category       string `json:"category"`
	EmailTemplate  string `json:"emailTemplate"`
	MobileTemplate string `json:"mobileTemplate"`
	Module         string `json:"module"`
}

// CreateNotifyItemResponse 创建通知项响应
type CreateNotifyItemResponse struct {
	Header
	// 创建通知项的id
	Data int64 `json:"data"`
}

// QueryNotifyItemRequest 查询通知项列表请求
type QueryNotifyItemRequest struct {
	PageNo    int64  `query:"pageNo"`
	PageSize  int64  `query:"pageSize"`
	Category  string `query:"category"`
	Label     string `json:"label"`
	ScopeType string `query:"scopeType"`
}

// QueryNotifyItemResponse 查询通知项列表请求
type QueryNotifyItemResponse struct {
	Header
	Data QueryNotifyItemData `json:"data"`
}

// QueryNotifyItemData 通知项列表数据结构
type QueryNotifyItemData struct {
	List  []*NotifyItem `json:"list"`
	Total int           `json:"total"`
}

// UpdateNotifyItemRequest 更新通知项请求
type UpdateNotifyItemRequest struct {
	ID             int64  `json:"id"`
	MobileTemplate string `json:"mobileTemplate"`
}

// UpdateNotifyItemResponse 更新通知项响应
type UpdateNotifyItemResponse struct {
	Header
}
