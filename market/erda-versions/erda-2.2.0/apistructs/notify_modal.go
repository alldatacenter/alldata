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

type EditOrCreateModalData struct {
	Name     string   `json:"name"`   //通知名称
	Target   int      `json:"target"` //选中的通知群组id
	Items    []string `json:"items"`  //选中的通知模版id
	Id       int      `json:"id"`
	Channels []string `json:"channels"` //通知方式
}
type InParams struct {
	ScopeType string `json:"scopeType"`
	ScopeId   string `json:"scopeId"`
}
type NotifyDetailResponse struct {
	Header
	Data DetailResponse `json:"data"`
}

type DetailResponse struct {
	Id         int64  `json:"id"`
	NotifyID   string `json:"notifyId"`
	NotifyName string `json:"notifyName"`
	Target     string `json:"target"`
	GroupType  string `json:"groupType"`
}

type AllTemplatesResponse struct {
	Header
	Data []*TemplateRes `json:"data"`
}

type TemplateRes struct {
	ID   string `json:"id"`
	Name string `json:"name"`
}
