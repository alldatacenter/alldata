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
	"bytes"
	"database/sql/driver"
	"encoding/json"
	"fmt"
	"net/url"
	"strconv"
	"strings"

	"github.com/erda-project/erda/internal/tools/monitor/utils"
)

// ViewConfigDTO .
type ViewConfigDTO []*ViewConfigItem

func (vc *ViewConfigDTO) replaceWithQuery(query url.Values) {
	if vc == nil {
		return
	}
	data := []*ViewConfigItem(*vc)
	for i := 0; i < len(data); i++ {
		data[i].View.dynamicReplaceView(query)
	}
	return
}

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
	I18N           interface{} `json:"i18n"`
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

// Scan .
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

// use r_xxx to replace {{xxx}} in API.Query
func (v *View) dynamicReplaceView(query url.Values) {
	for k, vals := range query {
		if k == "r_timestamp" && len(vals) > 0 { // 转换成当前时间点的前后1小时
			tt, err := strconv.Atoi(vals[0])
			if err != nil {
				continue
			}
			v.API.Query["r_timestamp"] = vals[0] // ms
			v.API.Query["end"] = tt
			start, err := utils.ConvertStringToMS("before_1h", int64(tt))
			if err != nil {
				continue
			}
			v.API.Query["start"] = start
			continue
		}

		if !strings.HasPrefix(k, "r_") {
			continue
		}
		key := strings.TrimLeft(k, "r_")

		for _, val := range vals {
			api, err := replaceAny(v.API.Query, []byte(fmt.Sprintf("{{%s}}", key)), []byte(val))
			if err != nil {
				fmt.Printf("=======replace error=====, err=%s\n", err)
				continue
			}
			v.API.Query = api

			ex, err := replaceAny(v.API.ExtraData, []byte(fmt.Sprintf("{{%s}}", key)), []byte(val))
			if err != nil {
				fmt.Printf("=======replace error=====, err=%s\n", err)
				continue
			}
			v.API.ExtraData = ex
		}
	}
	return
}

func replaceAny(in map[string]interface{}, old, new []byte) (out map[string]interface{}, err error) {
	buf, err := json.Marshal(in)
	if err != nil {
		return nil, err
	}
	buf = bytes.ReplaceAll(buf, old, new)
	err = json.Unmarshal(buf, &out)
	if err != nil {
		return nil, err
	}
	return
}
