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

import (
	"time"
)

// ImageCreateRequest 创建镜像API(POST api/images)使用。此API暂无人用，后续可下线
type ImageCreateRequest struct {
	// 关联release
	ReleaseID string `json:"releaseId"`

	//
	ImageName string `json:"imageName"`
	ImageTag  string `json:"imageTag"`
	Image     string `json:"image"`
}

// ImageCreateResponse 创建镜像API返回结构
type ImageCreateResponse struct {
	Header
	Data ImageCreateResponseData `json:"data"`
}

// ImageCreateResponseData 创建镜像API实际返回数据
type ImageCreateResponseData struct {
	ImageID int64 `json:"imageId"`
}

// ImageUpdateRequest 更新镜像API(PUT api/images)使用。此API暂无人用，后续可下线
type ImageUpdateRequest struct {
	ImageIDOrImage string `json:"-" path:"imageIdOrImage"`
	Body           struct {
		ID int64 `json:"id"`
		ImageCreateRequest
	} `json:"body"`
}

// ImageUpdateResponse 更新镜像API返回结构
type ImageUpdateResponse struct {
	Header
	Data interface{} `json:"data"`
}

// ImageGetRequest 镜像详情API(GET /api/imagges/{imageId}),打包去重使用
type ImageGetRequest struct {
	ImageIDOrImage string `json:"-" path:"imageIdOrImage"`
}

// ImageGetResponse 镜像详情API返回数据结构
type ImageGetResponse struct {
	Header
	Data ImageGetResponseData `json:"data"`
}

// ImageGetResponseData 镜像详情实际返回数据
type ImageGetResponseData struct {
	ID        int64     `json:"id"`
	ReleaseID string    `json:"releaseId"` // 关联release
	ImageName string    `json:"imageName"` // 镜像名称
	ImageTag  string    `json:"imageTag"`  // 镜像Tag
	Image     string    `json:"image"`     // 镜像地址
	CreatedAt time.Time `json:"createdAt"`
	UpdatedAt time.Time `json:"updatedAt"`
}

// ImageListRequest 镜像列表API(GET /api/images)
type ImageListRequest struct {
	// 分页大小,默认值20
	PageSize int64 `json:"-" query:"pageSize"`

	// 当前页号，默认值1
	PageNum int64 `json:"-" query:"pageNum"`
}

// ImageListResponse 镜像列表API返回数据结构
type ImageListResponse struct {
	Header
	Data ImageListResponseData `json:"data"`
}

// ImageListResponseData 镜像列表响应数据
type ImageListResponseData struct {
	Total int64                  `json:"total"`
	List  []ImageGetResponseData `json:"list"`
}

// ImageSearchRequest 镜像搜索API(GET /api/search/images?q=xxx)
type ImageSearchRequest struct {
	// 查询参数，eg:app:test
	Query string `json:"-" query:"q"`

	// 分页大小,默认值20
	PageSize int64 `json:"-" query:"pageSize"`

	// 当前页号，默认值1
	PageNum int64 `json:"-" query:"pageNum"`
}

// ImageSearchResponse 镜像搜索API返回数据结构
type ImageSearchResponse struct {
	Header
	Data []ImageGetResponseData `json:"data"`
}

// ImageUploadResponse 图片上传响应
type ImageUploadResponse struct {
	Header
	Data ImageUploadResponseData `json:"data"`
}

// ImageUploadResponseData 图片上传响应数据
type ImageUploadResponseData struct {
	URL string `json:"url"`
}
