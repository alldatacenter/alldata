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

// Request for API: `GET /api/analysis`
type OneDataAnalysisRequest struct {
	// 模型远程仓库地址
	RemoteUri string `query:"remoteUri"`
}

type OneDataAnalysisResponse struct {
	Header
	Data OneDataDTO `json:"data"`
}

type BusinessDomainDTO struct {
	BaseParam
}

type DataDomainDTO struct {
	BaseParam
}

type MarketDomainDTO struct {
	BaseParam
}

type OneDataDTO struct {
	BusinessDomain BusinessDomainDTO `json:"businessDomain"`
	DataDomains    []DataDomainDTO   `json:"dataDomains"`
	MarketDomains  []MarketDomainDTO `json:"marketDomains"`
}

type OneDataAnalysisBussProcRequest struct {
	// 本地仓库文件绝对路径
	FilePath string `query:"filePath"`
}

type OneDataAnalysisBussProcResponse struct {
	Header
	Data BusinessProcessDTO `json:"data"`
}

// Request for API: `GET /api/analysis/businessProcesses`
type OneDataAnalysisBussProcsRequest struct {
	// 模型远程仓库地址
	RemoteUri string `query:"remoteUri"`

	// 业务板块
	BusinessDomain string `query:"businessDomain"`

	// 数据域
	DataDomain string `query:"dataDomain"`

	// 搜索关键字
	KeyWord string `query:"keyWord"`

	// 行数
	PageSize uint32 `query:"pageSize"`

	// 页码
	PageNo uint32 `query:"pageNo"`
}

type OneDataAnalysisBussProcsResponse struct {
	Header
	Data BusinessProcessData `json:"data"`
}

type BusinessProcessDTO struct {
	ExtBaseParam
}

type BusinessProcessData struct {
	total uint32               `json:"total"`
	list  []BusinessProcessDTO `json:"list"`
}

// Request for API: `GET /api/analysis/dim`
type OneDataAnalysisDimRequest struct {
	// 本地仓库文件绝对路径
	FilePath string `query:"filePath"`
}

type OneDataAnalysisDimResponse struct {
	Header
	Data DimDTO `json:"data"`
}

type DimDTO struct {
	ExtBaseParam
	Relations []RelationDTO `json:"relations"`
}

type RelationDTO struct {
	SourceAttr string `json:"sourceAttr"`
	RelAttr    string `json:"relAttr"`
	IsPK       bool   `json:"isPK"`
}

// Request for API: `GET /api/analysis/fuzzyAttrs`
type OneDataAnalysisFuzzyAttrsRequest struct {
	// 本地仓库文件绝对路径
	FilePath string `query:"filePath"`

	// 搜索关键字
	KeyWord string `query:"keyWord"`

	// 行数
	PageSize uint32 `query:"pageSize"`

	// 页码
	PageNo uint32 `query:"pageNo"`
}

type OneDataAnalysisFuzzyAttrsResponse struct {
	Header
	Data AttrData `json:"data"`
}

type AttrDTO struct {
	BaseParam
	Type string `json:"type"`
}

type AttrData struct {
	total uint32    `json:"total"`
	list  []AttrDTO `json:"list"`
}

// Request for API: `GET /api/analysis/outputTables`
type OneDataAnalysisOutputTablesRequest struct {
	// 模型远程仓库地址
	RemoteUri string `query:"remoteUri"`

	// 业务板块
	BusinessDomain string `query:"businessDomain"`

	// 集市域
	MarketDomain string `query:"marketDomain"`

	// 搜索关键字
	KeyWord string `query:"keyWord"`

	// 行数
	PageSize uint32 `query:"pageSize"`

	// 页码
	PageNo uint32 `query:"pageNo"`
}

type OneDataAnalysisOutputTablesResponse struct {
	Header
	Data OutputTableData `json:"data"`
}

type OutputTableDTO struct {
	ExtBaseParam
}

type OutputTableData struct {
	total uint32           `json:"total"`
	list  []OutputTableDTO `json:"list"`
}

// Request for API: `GET /api/analysis/star`
type OneDataAnalysisStarRequest struct {
	// 本地仓库文件绝对路径
	FilePath string `query:"filePath"`
}

type OneDataAnalysisStarResponse struct {
	Header
	Data StarDTO `json:"data"`
}

type DerivativeIndexDTO struct {
	ExtBaseParam
	Type   string    `json:"type"`
	preiod BaseParam `json:"preiod"`
	adj    BaseParam `json:"adj"`
}

type AtomicIndexDTO struct {
	ExtBaseParam
	Type            string             `json:"type"`
	derivativeIndex DerivativeIndexDTO `json:"derivativeIndex"`
}

type StarDTO struct {
	Dims          []DimDTO                 `json:"dims"`
	AtomicIndices []AtomicIndexDTO         `json:"atomicIndices"`
	RelationGroup map[string][]RelationDTO `json:"relationGroup"`
}

//onedata基本参数
type BaseParam struct {
	EnName string `json:"enName"`
	CnName string `json:"cnName"`
	Desc   string `json:"desc"`
}

//onedata扩展参数
type ExtBaseParam struct {
	BaseParam
	Table string `json:"table"`
	File  string `json:"file"`
}
