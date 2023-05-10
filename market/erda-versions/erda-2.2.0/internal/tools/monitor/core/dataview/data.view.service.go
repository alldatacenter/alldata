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
	"context"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"time"

	uuid "github.com/satori/go.uuid"
	"google.golang.org/protobuf/types/known/structpb"

	"github.com/erda-project/erda-proto-go/core/monitor/dataview/pb"
	"github.com/erda-project/erda/cmd/monitor/monitor/conf"
	"github.com/erda-project/erda/internal/pkg/audit"
	"github.com/erda-project/erda/internal/tools/monitor/core/dataview/db"
	"github.com/erda-project/erda/pkg/common/apis"
	"github.com/erda-project/erda/pkg/common/errors"
)

type dataViewService struct {
	p       *provider
	sys     *db.SystemViewDB
	custom  *db.CustomViewDB
	history *db.ErdaDashboardHistoryDB
}

func (s *dataViewService) parseViewBlocks(view *pb.View, config, data string) *pb.View {
	if len(config) > 0 {
		err := json.Unmarshal([]byte(config), &view.Blocks)
		if err != nil {
			s.p.Log.Warn("invalid config in view (%s): %s", view.Id, err)
			return view
		}
		if len(data) > 0 {
			var values []struct {
				I          string          `json:"i"`
				StaticData *structpb.Value `json:"staticData"`
			}
			err = json.Unmarshal([]byte(data), &values)
			if err != nil {
				s.p.Log.Warn("invalid data in view (%s): %s", view.Id, err)
				return view
			}
			empty, _ := structpb.NewValue(map[string]interface{}{})
			for _, block := range view.Blocks {
				if block == nil || block.Chart == nil {
					continue
				}
				block.Chart.StaticData = empty
				for _, val := range values {
					if block.I == val.I {
						block.Chart.StaticData = val.StaticData
					}
				}
			}
		}
	}
	return view
}

func (s *dataViewService) ListSystemViews(ctx context.Context, req *pb.ListSystemViewsRequest) (*pb.ListSystemViewsResponse, error) {
	views := *conf.GetSystemChartview()

	vlist := &pb.ViewList{}
	var vs []*pb.View
	for _, v := range views {
		viewFileUnmarshal, err := conf.FileUnmarshal(conf.JsonFileExtension, v.Content)
		view := viewFileUnmarshal.(map[string]interface{})
		if err != nil {
			continue
		}
		scope, scopeId := getScopeScopeID(view)

		if scope == req.Scope || scopeId == req.ScopeID {
			marshal, err := json.Marshal(view)
			if err != nil {
				return nil, errors.NewInternalServerError(err)
			}
			view := pb.View{}
			err = json.Unmarshal(marshal, &view)
			view.Name = s.p.Tran.Text(apis.Language(ctx), view.Name)
			vs = append(vs, &view)
		}
	}
	vlist.List = vs
	vlist.Total = int64(len(vs))
	return &pb.ListSystemViewsResponse{Data: vlist}, nil
}

func getScopeScopeID(view map[string]interface{}) (string, string) {
	scope, scopeId := "", ""
	if v, ok := view["scope"].(string); ok {
		scope = v
	}
	if v, ok := view["scopeId"].(string); ok {
		scopeId = v
	}
	return scope, scopeId
}

func (s *dataViewService) GetSystemView(ctx context.Context, req *pb.GetSystemViewRequest) (*pb.GetSystemViewResponse, error) {
	if req.Id == "" {
		return nil, errors.NewMissingParameterError("id")
	}
	chartView := (*conf.GetSystemChartview())[req.Id]
	if chartView == nil {
		return nil, errors.NewNotFoundError(req.Id)
	}
	view := pb.View{}
	err := json.Unmarshal((*chartView).Content, &view)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	view.Name = s.p.Tran.Text(apis.Language(ctx), view.Name)
	return &pb.GetSystemViewResponse{Data: &view}, nil
}

func (s *dataViewService) GetCustomViewsCreator(ctx context.Context, req *pb.GetCustomViewsCreatorRequest) (*pb.GetCustomViewsCreatorResponse, error) {
	creatorIds, err := s.custom.GetCreatorsByFields(map[string]interface{}{
		"Scope":   req.Scope,
		"ScopeID": req.ScopeID,
	})
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	return &pb.GetCustomViewsCreatorResponse{Data: &pb.Creator{Creators: creatorIds}, UserIDs: creatorIds}, nil
}

func (s *dataViewService) ListCustomViews(ctx context.Context, req *pb.ListCustomViewsRequest) (*pb.ListCustomViewsResponse, error) {
	if req.PageNo < 1 {
		req.PageNo = 1
	}
	if req.PageSize < 0 {
		req.PageSize = 0
	}
	if req.PageSize >= 1000 {
		req.PageSize = 1000
	}
	likeFields := map[string]interface{}{}
	if req.Name != "" {
		likeFields["Name"] = req.Name
	}
	if req.Description != "" {
		likeFields["Desc"] = req.Description
	}

	fields := map[string]interface{}{
		"Scope":   req.Scope,
		"ScopeID": req.ScopeID,
	}

	list, total, err := s.custom.ListByFieldsAndPage(req.PageNo, req.PageSize, req.StartTime, req.EndTime, req.CreatorId, fields, likeFields)
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	views := &pb.ViewList{}
	var userIDs []string
	userIDMap := make(map[string]bool)
	for _, item := range list {
		view := s.parseViewBlocks(&pb.View{
			Id:        item.ID,
			Scope:     item.Scope,
			ScopeID:   item.ScopeID,
			Version:   item.Version,
			Name:      item.Name,
			Desc:      item.Desc,
			Creator:   item.CreatorID,
			CreatedAt: item.CreatedAt.UnixNano() / int64(time.Millisecond),
			UpdatedAt: item.UpdatedAt.UnixNano() / int64(time.Millisecond),
		}, item.ViewConfig, item.DataConfig)
		views.List = append(views.List, view)
		userId := item.CreatorID
		if userId != "" && !userIDMap[userId] {
			userIDs = append(userIDs, userId)
			userIDMap[userId] = true
		}
	}
	views.Total = total
	return &pb.ListCustomViewsResponse{Data: views, UserIDs: userIDs}, nil
}

func (s *dataViewService) GetCustomView(ctx context.Context, req *pb.GetCustomViewRequest) (*pb.GetCustomViewResponse, error) {
	data, err := s.custom.GetByFields(map[string]interface{}{
		"ID": req.Id,
	})
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	if data == nil {
		return nil, errors.NewNotFoundError(fmt.Sprintf("view/%s", req.Id))
	}
	view := s.parseViewBlocks(&pb.View{
		Id:        data.ID,
		Scope:     data.Scope,
		ScopeID:   data.ScopeID,
		Version:   data.Version,
		Name:      data.Name,
		Desc:      data.Desc,
		CreatedAt: data.CreatedAt.UnixNano() / int64(time.Millisecond),
		UpdatedAt: data.UpdatedAt.UnixNano() / int64(time.Millisecond),
	}, data.ViewConfig, data.DataConfig)
	return &pb.GetCustomViewResponse{Data: view}, nil
}

func (s *dataViewService) CreateCustomView(ctx context.Context, req *pb.CreateCustomViewRequest) (*pb.CreateCustomViewResponse, error) {
	if req.Blocks == nil {
		req.Blocks = make([]*pb.Block, 0)
	}
	if req.Data == nil {
		req.Data = make([]*pb.DataItem, 0)
	}
	blocks, _ := json.Marshal(req.Blocks)
	data, _ := json.Marshal(req.Data)
	now := time.Now()
	userId := apis.GetUserID(ctx)
	model := &db.CustomView{
		ID:         req.Id,
		Name:       req.Name,
		Version:    req.Version,
		Desc:       req.Desc,
		Scope:      req.Scope,
		ScopeID:    req.ScopeID,
		ViewConfig: string(blocks),
		DataConfig: string(data),
		CreatorID:  userId,
		CreatedAt:  now,
		UpdatedAt:  now,
	}
	if len(model.ID) <= 0 {
		model.ID = hex.EncodeToString(uuid.NewV4().Bytes())
	}
	err := s.custom.DB.Save(model).Error
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	result := &pb.CreateCustomViewResponse{Data: s.parseViewBlocks(&pb.View{
		Id:        model.ID,
		Name:      model.Name,
		Version:   model.Version,
		Desc:      model.Desc,
		Scope:     model.Scope,
		ScopeID:   model.ScopeID,
		CreatedAt: model.CreatedAt.UnixNano() / int64(time.Millisecond),
		UpdatedAt: model.UpdatedAt.UnixNano() / int64(time.Millisecond),
	}, model.ViewConfig, model.DataConfig)}

	// bypass reportengine's call, just return
	if userId == "" {
		return result, nil
	}

	err = s.auditContextMap(ctx, req.Name, req.Scope)
	if err != nil {
		return nil, errors.NewInternalServerError(fmt.Errorf("auditContextMap: %w", err))
	}
	return result, nil
}

func (s *dataViewService) getOrgName(ctx context.Context) (string, error) {
	orgId := apis.GetOrgID(ctx)
	org, err := s.p.bdl.GetOrg(orgId)
	if err != nil {
		return "", err
	}
	return org.Name, nil
}

func (s *dataViewService) UpdateCustomView(ctx context.Context, req *pb.UpdateCustomViewRequest) (*pb.UpdateCustomViewResponse, error) {
	data, err := s.custom.GetByFields(map[string]interface{}{
		"ID": req.Id,
	})
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	err = s.custom.UpdateView(req.Id, fieldsForUpdate(req))
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	result := &pb.UpdateCustomViewResponse{Data: true}
	err = s.auditContextMap(ctx, data.Name, data.Scope)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	return result, nil
}

func (s *dataViewService) DeleteCustomView(ctx context.Context, req *pb.DeleteCustomViewRequest) (*pb.DeleteCustomViewResponse, error) {
	data, err := s.custom.GetByFields(map[string]interface{}{
		"ID": req.Id,
	})
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	err = s.custom.DB.Where("id=?", req.Id).Delete(&db.CustomView{}).Error
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	err = s.auditContextMap(ctx, data.Name, data.Scope)
	if err != nil {
		return nil, errors.NewInternalServerError(err)
	}
	return &pb.DeleteCustomViewResponse{Data: true}, nil
}

func (s *dataViewService) ListCustomDashboardHistory(ctx context.Context, req *pb.ListCustomDashboardHistoryRequest) (*pb.ListCustomDashboardHistoryResponse, error) {
	if req.PageNo < 1 {
		req.PageNo = 1
	}
	if req.PageSize < 0 {
		req.PageSize = 0
	}
	if req.PageSize >= 1000 {
		req.PageSize = 1000
	}
	historiesDB, total, err := s.history.ListByPage(req.PageNo, req.PageSize, req.Scope, req.ScopeId)
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	var histories []*pb.CustomDashboardHistory
	for _, historyDB := range historiesDB {
		history := &pb.CustomDashboardHistory{
			Id:            historyDB.ID,
			Type:          historyDB.Type,
			Status:        historyDB.Status,
			Scope:         historyDB.Scope,
			ScopeId:       historyDB.ScopeId,
			OperatorId:    historyDB.OperatorId,
			FileUuid:      historyDB.FileUUID,
			ErrorMessage:  historyDB.ErrorMessage,
			TargetScope:   historyDB.TargetScope,
			TargetScopeId: historyDB.TargetScopeId,
			CreatedAt:     historyDB.CreatedAt.Format("2006-01-02 15:04:05"),
		}
		histories = append(histories, history)
	}
	return &pb.ListCustomDashboardHistoryResponse{Total: total, Histories: histories}, nil
}

func (s *dataViewService) auditContextMap(ctx context.Context, dashboardName, scope string) error {
	orgName, err := s.getOrgName(ctx)
	if err != nil {
		return err
	}
	auditContext := map[string]interface{}{
		"orgName":       orgName,
		"dashboardName": dashboardName,
		"scope":         scope,
	}
	if scope != "org" {
		auditContext["projectName"] = apis.GetHeader(ctx, "erda-projectName")
		auditContext["workspace"] = apis.GetHeader(ctx, "erda-workspace")
	}
	audit.ContextEntryMap(ctx, auditContext)
	return nil
}

func fieldsForUpdate(req *pb.UpdateCustomViewRequest) map[string]interface{} {
	blocks, _ := json.Marshal(req.Blocks)
	data, _ := json.Marshal(req.Data)
	fields := map[string]interface{}{
		"UpdatedAt": time.Now(),
	}
	switch req.UpdateType {
	case pb.UpdateType_MetaType.String():
		fields["Name"] = req.Name
		fields["Desc"] = req.Desc
	case pb.UpdateType_ViewType.String():
		fields["ViewConfig"] = string(blocks)
		fields["DataConfig"] = string(data)
	}
	return fields
}
