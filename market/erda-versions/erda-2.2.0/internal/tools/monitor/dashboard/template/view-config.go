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

import (
	"database/sql/driver"
	"encoding/json"
)

// ViewConfigDTO .
type ViewConfigDTO []*ViewConfigItem

// ViewConfig .
type ViewConfigItem struct {
	W    int64  `json:"w"`
	H    int64  `json:"h"`
	X    int64  `json:"x"`
	Y    int64  `json:"y"`
	I    string `json:"i"`
	View *View  `json:"view"`
}

// Config .
type config struct {
	OptionProps      *map[string]interface{} `json:"optionProps,omitempty"`
	DataSourceConfig interface{}             `json:"dataSourceConfig,omitempty"`
	Option           interface{}             `json:"option,omitempty"`
}

// ViewResp .
type View struct {
	Title          string      `json:"title"`
	Description    string      `json:"description"`
	ChartType      string      `json:"chartType"`
	DataSourceType string      `json:"dataSourceType"`
	StaticData     interface{} `json:"staticData"`
	Config         config      `json:"config"`
	API            *API        `json:"api"`
	Controls       interface{} `json:"controls"`
}

// API .
type API struct {
	URL       string                 `json:"url"`
	Query     map[string]interface{} `json:"query"`
	Body      map[string]interface{} `json:"body"`
	Header    map[string]interface{} `json:"header"`
	ExtraData map[string]interface{} `json:"extraData"`
	Method    string                 `json:"method"`
}

func (vc *ViewConfigDTO) Scan(value interface{}) error {
	if value == nil {
		*vc = ViewConfigDTO{}
		return nil
	}
	t := ViewConfigDTO{}
	if e := json.Unmarshal(value.([]byte), &t); e != nil {
		return e
	}
	*vc = t
	return nil
}

// Value .
func (vc *ViewConfigDTO) Value() (driver.Value, error) {
	if vc == nil {
		return nil, nil
	}
	b, e := json.Marshal(*vc)
	return b, e
}
