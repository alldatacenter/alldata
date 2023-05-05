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

// CICDPipelineListRequest /api/cicds 获取 pipeline 列表
type CICDPipelineListRequest struct {
	Branches string `schema:"branches"`
	Sources  string `schema:"sources"`
	YmlNames string `schema:"ymlNames"`
	Statuses string `schema:"statuses"`
	AppID    uint64 `schema:"appID"`
	PageNum  int    `schema:"pageNum"` // deprecated, use PageNo
	PageNo   int    `schema:"pageNo"`  // UI standard compatible, same with PageNum
	PageSize int    `schema:"pageSize"`
}

func (r *CICDPipelineListRequest) EnsurePageNo() int {
	// pageNo first
	if r.PageNo > 0 {
		r.PageNum = r.PageNo
		return r.PageNo
	}
	r.PageNo = r.PageNum
	return r.PageNum
}

// CICD pipeline detail
type CICDPipelineDetailRequest struct {
	SimplePipelineBaseResult bool   `json:"simplePipelineBaseResult"`
	PipelineID               uint64 `json:"pipelineID"`
}

// CICDPipelineYmlListRequest /api/cicds/actions/pipelineYmls 获取 pipeline yml列表
type CICDPipelineYmlListRequest struct {
	AppID  int64  `schema:"appID"`
	Branch string `schema:"branch"`
}

// CICDPipelineYmlListResponse
type CICDPipelineYmlListResponse struct {
	Data []string `json:"data"`
}
