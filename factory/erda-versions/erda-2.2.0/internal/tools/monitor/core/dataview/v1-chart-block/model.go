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

package block

import (
	"database/sql/driver"
	"encoding/json"
	"net/url"
)

// DashboardBlockDTO .
type DashboardBlockDTO struct {
	ID         string         `json:"id"`
	Name       string         `json:"name"`
	Desc       string         `json:"desc"`
	Scope      string         `json:"scope"`
	ScopeID    string         `json:"scopeId"`
	ViewConfig *ViewConfigDTO `json:"viewConfig"`
	DataConfig *dataConfigDTO `json:"-"`
	CreatedAt  int64          `json:"createdAt"`
	UpdatedAt  int64          `json:"updatedAt"`
	Version    string         `json:"version"`
}

func (dash *DashboardBlockDTO) ReplaceVCWithDynamicParameter(query url.Values) *DashboardBlockDTO {
	dash.ViewConfig.replaceWithQuery(query)
	return dash
}

// SystemBlockUpdate .
type SystemBlockUpdate struct {
	Name       *string        `json:"name"`
	Desc       *string        `json:"desc"`
	ViewConfig *ViewConfigDTO `json:"viewConfig"`
}

// UserBlockUpdate .
type UserBlockUpdate struct {
	Name       *string        `json:"name"`
	Desc       *string        `json:"desc"`
	ViewConfig *ViewConfigDTO `json:"viewConfig"`
	DataConfig *dataConfigDTO `json:"dataConfig"`
}

// DashboardBlockResp .
type dashboardBlockResp struct {
	DashboardBlocks []*DashboardBlockDTO `json:"list"`
	Total           int                  `json:"total"`
}

// DataConfigDTO .
type dataConfigDTO []dataConfig

// DataConfig .
type dataConfig struct {
	I          string      `json:"i"`
	StaticData interface{} `json:"staticData"`
}

// Scan .
func (ls *dataConfigDTO) Scan(value interface{}) error {
	if value == nil {
		*ls = dataConfigDTO{}
		return nil
	}
	t := dataConfigDTO{}
	if e := json.Unmarshal(value.([]byte), &t); e != nil {
		return e
	}
	*ls = t
	return nil
}

// Value .
func (ls *dataConfigDTO) Value() (driver.Value, error) {
	if ls == nil {
		return nil, nil
	}
	b, e := json.Marshal(*ls)
	return b, e
}
