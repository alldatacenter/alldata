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

package template

type templateDTO struct {
	ID          string         `json:"id"`
	Name        string         `json:"name"`
	Description string         `json:"description"`
	Scope       string         `json:"scope"`
	ScopeID     string         `json:"scopeId"`
	ViewConfig  *ViewConfigDTO `json:"viewConfig"`
	CreatedAt   int64          `json:"createdAt"`
	UpdatedAt   int64          `json:"updatedAt"`
	Version     string         `json:"version"`
	Type        int64          `json:"type"`
}

type templateUpdate struct {
	Name        *string        `json:"name"`
	Description *string        `json:"description"`
	ViewConfig  *ViewConfigDTO `json:"viewConfig"`
}

type templateOverview struct {
	ID          string `json:"id"`
	Name        string `json:"name"`
	Description string `json:"description"`
	Scope       string `json:"scope"`
	ScopeID     string `json:"scopeId"`
	CreatedAt   int64  `json:"createdAt"`
	UpdatedAt   int64  `json:"updatedAt"`
	Version     string `json:"version"`
	Type        int64  `json:"type"`
}

type templateResp struct {
	TemplateDTO []*templateOverview `json:"list"`
	Total       int                 `json:"total"`
}

type templateSearch struct {
	ID       string `query:"id"`
	Scope    string `query:"scope" validate:"required"`
	ScopeID  string `query:"scopeId" validate:"required"`
	PageNo   int64  `query:"pageNo" validate:"gte=1" default:"20"`
	PageSize int64  `query:"pageSize" validate:"gte=1" default:"20"`
	Type     int64  `query:"type"`
	Name     string `query:"name"`
}

type templateType struct {
	Type int64
}
