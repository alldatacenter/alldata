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

package dataview

import (
	"bytes"
	"context"
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"io"
	"io/ioutil"
	"net/http"
	"regexp"
	"strings"
	"time"

	"github.com/mitchellh/mapstructure"
	"github.com/recallsong/go-utils/encoding/jsonx"
	uuid "github.com/satori/go.uuid"

	"github.com/erda-project/erda-infra/pkg/transport"
	"github.com/erda-project/erda-proto-go/core/monitor/dataview/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/monitor/core/dataview/db"
	api "github.com/erda-project/erda/pkg/common/httpapi"
)

func (p *provider) ParseDashboardTemplate(r *http.Request, params struct {
	Scope   string `json:"scope"`
	ScopeId string `json:"scopeId"`
}) interface{} {
	file, _, err := r.FormFile("file")
	if err != nil {
		return api.Failure(http.StatusInternalServerError, err)
	}
	buff := bytes.NewBuffer([]byte{})
	io.Copy(buff, file)
	if err != nil {
		return api.Failure(http.StatusInternalServerError, err)
	}
	valid := json.Valid(buff.Bytes())
	if !valid {
		return api.Failure(http.StatusInternalServerError, "json file parse failed")
	}
	var dashboards []map[string]interface{}
	err = jsonx.Unmarshal(string(buff.Bytes()), &dashboards)
	if err != nil {
		return api.Failure(http.StatusInternalServerError, err)
	}

	for _, dashboard := range dashboards {
		if v, ok := dashboard["name"]; !ok || v == "" {
			return api.Failure(http.StatusInternalServerError, "dashboard name not exist")
		}
		if v, ok := dashboard["scope"]; !ok || v != params.Scope {
			return api.Failure(http.StatusInternalServerError, fmt.Sprintf("%s type dashboard can't import %s type dashboard", v, params.Scope))
		}
		if v, ok := dashboard["scopeId"]; !ok || v == params.ScopeId {
			return api.Failure(http.StatusInternalServerError, fmt.Sprintf("can't import the same environment"))
		}
	}
	return api.Success(nil, http.StatusOK)
}

var matchNumberAndLetters = `([0-9a-zA-Z]*)`

var exprs = []string{
	`__metric_scope_id\\":\\"([0-9a-zA-Z]*)\\",`,
	`__metric_scope_id":"([0-9a-zA-Z]*)",`,
	`terminus_key\\":\\"([0-9a-zA-Z]*)\\",`,
	`terminus_key":"([0-9a-zA-Z]*)",`,
}

func CompileToDest(scope, scopeId, data string) string {
	switch scope {
	case "org":
	case "micro_service":
		for _, expr := range exprs {
			compile, _ := regexp.Compile(expr)
			all := strings.ReplaceAll(expr, matchNumberAndLetters, scopeId)
			all = strings.ReplaceAll(all, "\\\\", "\\")
			if !compile.MatchString(data) {
				continue
			}
			data = compile.ReplaceAllString(data, all)
		}
	}
	return data
}

func (p *provider) ImportDashboardFile(r *http.Request, params struct {
	Scope   string `json:"scope"`
	ScopeId string `json:"scopeId"`
}) interface{} {
	file, _, err := r.FormFile("file")
	if err != nil {
		return api.Failure(http.StatusInternalServerError, err)
	}
	buff := bytes.NewBuffer([]byte{})
	io.Copy(buff, file)
	if err != nil {
		return api.Failure(http.StatusInternalServerError, err)
	}
	userID := r.Header.Get("USER-ID")
	history := &db.ErdaDashboardHistory{
		ID:         hex.EncodeToString(uuid.NewV4().Bytes()),
		Scope:      params.Scope,
		ScopeId:    params.ScopeId,
		Type:       pb.OperatorType_Import.String(),
		Status:     pb.OperatorStatus_Processing.String(),
		OperatorId: userID,
		CreatedAt:  time.Now(),
		UpdatedAt:  time.Now(),
	}
	history, err = p.history.Save(history)
	if err != nil {
		return api.Failure(http.StatusInternalServerError, err)
	}

	src := string(buff.Bytes())
	dest := CompileToDest(params.Scope, params.ScopeId, src)
	var dashboards []map[string]interface{}
	err = jsonx.Unmarshal(dest, &dashboards)
	if err != nil {
		p.history.UpdateStatusAndFileUUID(history.ID, pb.OperatorStatus_Failure.String(), "", err.Error())
		return api.Failure(http.StatusInternalServerError, err)
	}

	for _, dashboard := range dashboards {
		request := &pb.CreateCustomViewRequest{}
		err := mapstructure.Decode(dashboard, request)
		if err != nil {
			p.history.UpdateStatusAndFileUUID(history.ID, pb.OperatorStatus_Failure.String(), "", err.Error())
			return api.Failure(http.StatusInternalServerError, err)
		}
		var blocks []*pb.Block
		viewConfig := dashboard["viewConfig"]
		err = json.Unmarshal([]byte(viewConfig.(string)), &blocks)
		if err != nil {
			p.history.UpdateStatusAndFileUUID(history.ID, pb.OperatorStatus_Failure.String(), "", err.Error())
			return api.Failure(http.StatusInternalServerError, err)
		}
		var dataItem []*pb.DataItem
		dataConfig := dashboard["dataConfig"]
		err = json.Unmarshal([]byte(dataConfig.(string)), &dataItem)
		if err != nil {
			p.history.UpdateStatusAndFileUUID(history.ID, pb.OperatorStatus_Failure.String(), "", err.Error())
			return api.Failure(http.StatusInternalServerError, err)
		}
		request.Id = ""
		request.Scope = params.Scope
		request.ScopeID = params.ScopeId
		request.Blocks = blocks
		request.Data = dataItem

		ctx := transport.WithHTTPHeaderForServer(context.Background(), r.Header)
		if _, err = p.dataViewService.CreateCustomView(ctx, request); err != nil {
			p.history.UpdateStatusAndFileUUID(history.ID, pb.OperatorStatus_Failure.String(), "", err.Error())
			return api.Failure(http.StatusInternalServerError, err)
		}
	}

	err = p.history.UpdateStatusAndFileUUID(history.ID, pb.OperatorStatus_Success.String(), "", "")
	if err != nil {
		p.Log.Error(err)
	}
	return api.Success(nil, http.StatusOK)
}

func (p *provider) ExportDashboardFile(r *http.Request, params struct {
	Scope         string   `json:"scope"`
	ScopeId       string   `json:"scopeId"`
	TargetScope   string   `json:"targetScope"`
	TargetScopeId string   `json:"targetScopeId"`
	StartTime     int64    `json:"startTime"`
	EndTime       int64    `json:"endTime"`
	Name          string   `json:"name"`
	Description   string   `json:"description"`
	ViewIds       []string `json:"viewIds"`
	CreatorId     []string `json:"creatorId"`
}) interface{} {

	if params.Scope == "" || params.ScopeId == "" {
		return api.Failure(http.StatusInternalServerError, "scope or scopeId not found")
	}

	likeFields := map[string]interface{}{}
	if params.Name != "" {
		likeFields["Name"] = params.Name
	}
	if params.Description != "" {
		likeFields["Desc"] = params.Description
	}

	fields := map[string]interface{}{
		"Scope":   params.Scope,
		"ScopeID": params.ScopeId,
	}
	var views []*db.CustomView
	if params.ViewIds != nil && len(params.ViewIds) != 0 {
		list, err := p.custom.ListByIds(params.ViewIds)
		if err != nil {
			return api.Failure(http.StatusInternalServerError, err)
		}
		views = list
	} else {
		list, err := p.custom.ListByFields(params.StartTime, params.EndTime, params.CreatorId, fields, likeFields)
		if err != nil {
			return api.Failure(http.StatusInternalServerError, err)
		}
		views = list
	}

	if views == nil || len(views) == 0 {
		return api.Failure(http.StatusInternalServerError, "all records not found")
	}

	userID := r.Header.Get("USER-ID")
	jsonFile, _ := json.Marshal(views)
	history := &db.ErdaDashboardHistory{
		ID:            hex.EncodeToString(uuid.NewV4().Bytes()),
		Scope:         params.Scope,
		ScopeId:       params.ScopeId,
		OrgId:         params.ScopeId,
		TargetScope:   params.TargetScope,
		TargetScopeId: params.TargetScopeId,
		Type:          pb.OperatorType_Export.String(),
		Status:        pb.OperatorStatus_Processing.String(),
		OperatorId:    userID,
		File:          string(jsonFile),
		CreatedAt:     time.Now(),
		UpdatedAt:     time.Now(),
	}
	history, err := p.history.Save(history)
	if err != nil {
		return api.Failure(http.StatusInternalServerError, err)
	}

	if params.TargetScope != "" && params.TargetScopeId != "" {
		// export to target env
		tx := p.custom.Begin()
		for _, view := range views {
			view.ID = hex.EncodeToString(uuid.NewV4().Bytes())
			view.Scope = params.TargetScope
			view.ScopeID = params.TargetScopeId
			view.ViewConfig = CompileToDest(view.Scope, view.ScopeID, view.ViewConfig)
			view.DataConfig = CompileToDest(view.Scope, view.ScopeID, view.DataConfig)
			view.CreatedAt = time.Now()
			view.UpdatedAt = time.Now()
			view.CreatorID = userID
			err = tx.Save(view).Error
			if err != nil {
				tx.Rollback()
				err = p.history.UpdateStatusAndFileUUID(history.ID, pb.OperatorStatus_Failure.String(), "", err.Error())
				if err != nil {
					return api.Failure(http.StatusInternalServerError, err)
				}
				break
			}
		}
		err = tx.Commit().Error
		if err != nil {
			if err = p.history.UpdateStatusAndFileUUID(history.ID, pb.OperatorStatus_Failure.String(), "", err.Error()); err != nil {
				return api.Failure(http.StatusInternalServerError, err)
			}
			return api.Failure(http.StatusInternalServerError, err)
		}
		err = p.history.UpdateStatusAndFileUUID(history.ID, pb.OperatorStatus_Success.String(), "", "")
		if err != nil {
			return api.Failure(http.StatusInternalServerError, err)
		}
		return api.Success(nil, http.StatusOK)
	} else {
		// export to file
		p.ExportChannel <- history.ID
	}
	return api.Success(nil, http.StatusOK)
}

func dashboardFilename(scope, scopeId string) string {
	nowTime := time.Now().Format("2006-01-02 15:04:05")
	fileNameWithoutExt := fmt.Sprintf("%s-%s-%s", scope, scopeId, nowTime)
	encode := base64.StdEncoding.EncodeToString([]byte(fileNameWithoutExt))
	filename := fmt.Sprintf("%s.%s", encode, "json")
	return filename
}

func (p *provider) ExportTask(id string) {
	history, err := p.history.FindById(id)
	if err != nil {
		p.Log.Error(err)
		return
	}

	// export to file
	filename := dashboardFilename(history.Scope, history.ScopeId)
	reader := strings.NewReader(history.File)
	request := apistructs.FileUploadRequest{
		FileNameWithExt: filename,
		ByteSize:        int64(reader.Len()),
		FileReader:      ioutil.NopCloser(reader),
		From:            "Custom Dashboard",
		IsPublic:        true,
		ExpiredAt:       nil,
	}
	file, err := p.bdl.UploadFile(request)
	if err != nil {
		err := p.history.UpdateStatusAndFileUUID(id, pb.OperatorStatus_Failure.String(), "", err.Error())
		if err != nil {
			p.Log.Error(err)
		}
		return
	}
	err = p.history.UpdateStatusAndFileUUID(id, pb.OperatorStatus_Success.String(), file.UUID, "")
	if err != nil {
		p.Log.Error(err)
		return
	}
}
