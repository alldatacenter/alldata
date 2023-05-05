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

package engine

import (
	"encoding/json"
	"fmt"
)

// endpoints
const (
	reportTaskPath = "/api/org/report/tasks"
	// systemBlocksPath  = "/api/dashboard/system/blocks"
	userBlocksPath    = "/api/dashboard/blocks"
	reportHistoryPath = "/api/report/histories"
	headlessPath      = "/api/cdp/screenshot"

	eventboxPath = "/api/dice/eventbox/message/create"
)

type baseResponse struct {
	Success bool        `json:"success"`
	Data    interface{} `json:"data"`
	Err     struct {
		Code string      `json:"code"`
		Msg  string      `json:"msg"`
		Ctx  interface{} `json:"ctx"`
	} `json:"err"`
}

func (br *baseResponse) SetDataReceiver(dataEntity interface{}) {
	br.Data = dataEntity
}

func (br baseResponse) Error() string {
	return fmt.Sprintf("response error:code: %s msg: %s ctx: %v", br.Err.Code, br.Err.Msg, br.Err.Ctx)
}

type baseEntity struct {
	ID      int    `json:"id,omitempty"`
	Scope   string `json:"scope"`
	ScopeID string `json:"scopeId"`
}

type eventboxEntity struct {
	Sender  string                 `json:"sender"`
	Content map[string]interface{} `json:"content"`
	Labels  map[string]int         `json:"labels"`
}

type notifyChannel struct {
	Name     string                 `json:"name"`
	Template string                 `json:"template"`
	Params   map[string]interface{} `json:"params"`
}

type reportTaskEntity struct {
	baseEntity
	Name                   string       `json:"name"`
	Type                   string       `json:"type"`
	DashboardBlockTemplate *blockEntity `json:"dashboardBlockTemplate"`
	Notifier               *notifier    `json:"notifyTarget"`
}

type notifier struct {
	GroupID   int    `json:"groupId"`
	GroupType string `json:"groupType"`
}

type blockEntity struct {
	ID         string      `json:"id,omitempty"`
	Scope      string      `json:"scope"`
	ScopeID    string      `json:"scopeId"`
	Desc       string      `json:"desc"`
	Name       string      `json:"name"`
	ViewConfig interface{} `json:"viewConfig"`
	DataConfig []*viewData `json:"dataConfig"`
}

type historyEntity struct {
	baseEntity
	TaskID      int    `json:"TaskID"`
	DashboardID string `json:"dashboardId"`
	End         int    `json:"end"`
}

type apiEntity struct {
	URL    string                 `json:"url"`
	Method string                 `json:"method"`
	Query  map[string]interface{} `json:"query"`
	Body   interface{}            `json:"body"`
	Header map[string]string      `json:"header"`
}

type viewData struct {
	I          string           `json:"i"`
	StaticData *json.RawMessage `json:"staticData"`
}

type Resource struct {
	Block      *blockEntity
	ReportTask *reportTaskEntity
}

func NewResource(report *reportTaskEntity) (r *Resource, err error) {
	r = &Resource{
		Block:      report.DashboardBlockTemplate,
		ReportTask: report,
	}
	return r, nil
}
