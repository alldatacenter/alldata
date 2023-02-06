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

// Request for API: `POST /api/bi/questions/plugin/execute`
type EffectivenessRequest struct {
	// 插件参数
	PluginParamDto PluginParamDto `query:"remoteUri"`
}

type EffectivenessResponse struct {
	Header
	Data WidgetResponse `json:"data"`
}

type PluginParamDto struct {
	// 数据源Id
	DataSourceId int32 `json:"dataSourceId"`
	// 数据表名称
	TableName string `json:"tableName"`
	// 展示图形类型，可选:default,line,bar,area,pie,cards,radar,gauge,map,dot
	Widget string `json:"widget"`
	// 目标字段列表
	TargetColumns []string `json:"targetColumns"`
	// 筛选字段列表
	FilterColumns map[string]string `json:"filterColumns"`
	// 返回记录数
	Limit int32 `json:"limit"`
	// 查询其实位置
	Offset int32 `json:"offset"`
	// 聚合字段列表
	GroupByColumns []string `json:"groupByColumns"`
}

type WidgetResponse struct {
	// 图形名称
	Name string `json:"name"`
	// 字段名列表
	Names []string `json:"names"`
	// 图形标题
	Titles []interface{} `json:"titles"`
	// 字段数据列表
	Datas []interface{} `json:"datas"`
}
