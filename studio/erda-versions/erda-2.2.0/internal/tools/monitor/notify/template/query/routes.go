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

package query

import (
	"encoding/json"
	"net/http"
	"strconv"

	"gopkg.in/yaml.v2"

	"github.com/erda-project/erda-infra/providers/httpserver"
	"github.com/erda-project/erda/internal/tools/monitor/common"
	"github.com/erda-project/erda/internal/tools/monitor/common/permission"
	"github.com/erda-project/erda/internal/tools/monitor/notify/template/db"
	"github.com/erda-project/erda/internal/tools/monitor/notify/template/model"
	api "github.com/erda-project/erda/pkg/common/httpapi"
)

func (p *provider) initRoutes(routes httpserver.Router) error {
	//provided to Flink,don't need authentication,
	//this function provide all system's notify templates
	routes.GET("/api/notify/all-templates", p.getAllNotifyTemplates)

	routes.GET("/api/notify/templates", p.getNotifyTemplate, permission.Intercepter(
		permission.ValueGetter(p.getScope), p.getScopeID,
		common.ResourceNotify, permission.ActionCreate))
	routes.POST("/api/notify/records", p.createNotify, permission.Intercepter(
		permission.ValueGetter(p.getScope), p.getScopeID,
		common.ResourceNotify, permission.ActionCreate))
	routes.DELETE("/api/notify/records/:id", p.deleteNotify, permission.Intercepter(
		permission.ValueGetter(p.getScope), p.getScopeID,
		common.ResourceNotify, permission.ActionDelete))
	routes.PUT("/api/notify/records/:id", p.updateNotify, permission.Intercepter(
		permission.ValueGetter(p.getScope), p.getScopeID,
		common.ResourceNotify, permission.ActionUpdate))
	routes.GET("/api/notify/records", p.getUserNotifyList, permission.Intercepter(
		permission.ValueGetter(p.getScope), p.getScopeID,
		common.ResourceNotify, permission.ActionGet))
	routes.PUT("/api/notify/:id/switch", p.notifyEnable, permission.Intercepter(
		permission.ValueGetter(p.getScope), p.getScopeID,
		common.ResourceNotify, permission.ActionUpdate))
	routes.POST("/api/notify/user-define/templates", p.createUserDefineNotifyTemplate, permission.Intercepter(
		permission.ValueGetter(p.getScope), p.getScopeID,
		common.ResourceNotify, permission.ActionCreate))
	routes.GET("/api/notify/:id/detail", p.getNotifyDetail)
	routes.GET("/api/notify/all-group", p.getAllGroups)
	return nil
}

func (p *provider) getAllNotifyTemplates(r *http.Request) interface{} {
	allNotifyTemplates := getAllNotifyTemplates()
	userDefineTemplate, err := p.N.GetAllUserDefineTemplates()
	if err != nil {
		return err
	}
	for _, v := range *userDefineTemplate {
		metaData := model.Metadata{}
		err = yaml.Unmarshal([]byte(v.Metadata), &metaData)
		if err != nil {
			return api.Errors.Internal(err)
		}
		behavior := model.Behavior{}
		err = yaml.Unmarshal([]byte(v.Behavior), &behavior)
		if err != nil {
			return api.Errors.Internal(err)
		}
		templates := make([]model.Templates, 0)
		err = yaml.Unmarshal([]byte(v.Templates), &templates)
		if err != nil {
			return api.Errors.Internal(err)
		}
		model := model.Model{
			ID:        v.NotifyID,
			Metadata:  metaData,
			Behavior:  behavior,
			Templates: templates,
		}
		allNotifyTemplates = append(allNotifyTemplates, model)
	}
	return api.Success(allNotifyTemplates)
}

//get notify template list
func (p *provider) getNotifyTemplate(r *http.Request, params struct {
	Scope   string `query:"scope" validate:"required"`
	ScopeID string `query:"scopeId" validate:"required"`
	Name    string `query:"name"`
	NType   string `query:"type"`
}) interface{} {
	data := make([]*model.GetNotifyRes, 0)
	//filter system notify templates
	sysNotify := getNotifyTemplateList(params.Scope, params.Name, params.NType)
	//filter user define templates
	cusNotify, err := p.getUserDefineTemplate(params.ScopeID, params.Scope, params.Name, params.NType)
	if err != nil {
		return api.Errors.Internal(err)
	}
	data = append(data, sysNotify...)
	data = append(data, cusNotify...)
	return api.Success(data)
}

func (p *provider) createNotify(r *http.Request, params model.CreateNotifyReq) interface{} {
	err := params.CheckNotify()
	if err != nil {
		return api.Errors.Internal(err)
	}
	exist, err := p.N.CheckNotifyNameExist(params.ScopeID, params.Scope, params.NotifyName)
	if err != nil {
		return api.Errors.Internal(err)
	}
	if exist {
		return api.Errors.AlreadyExists(err)
	}
	groupDetail, err := p.N.GetNotifyGroup(params.NotifyGroupID)
	if err != nil {
		return api.Errors.Internal(err)
	}
	var targetData []model.NotifyTarget
	err = json.Unmarshal([]byte(groupDetail.TargetData), &targetData)
	if err != nil {
		return api.Errors.Internal(err)
	}
	t := model.Target{
		GroupID:  params.NotifyGroupID,
		Channels: params.Channels,
	}
	if targetData[0].Type == model.DingDingTarget {
		t.DingDingUrl = targetData[0].Values[0].Receiver
	}
	target, err := json.Marshal(t)
	if err != nil {
		return api.Errors.Internal(err)
	}
	notifyId, err := json.Marshal(params.TemplateID)
	if err != nil {
		return api.Errors.Internal(err)
	}
	attribute, err := json.Marshal(params.Attribute)
	if err != nil {
		return api.Errors.Internal(err)
	}
	notifyRecord := db.Notify{
		NotifyName: params.NotifyName,
		Target:     string(target),
		ScopeID:    params.ScopeID,
		Scope:      params.Scope,
		NotifyID:   string(notifyId),
		Enable:     true,
		Attributes: string(attribute),
	}

	err = p.N.CreateNotifyRecord(&notifyRecord)
	if err != nil {
		return api.Errors.Internal(err)
	}
	return api.Success(notifyRecord.ID)
}

func (p *provider) deleteNotify(r *http.Request, params struct {
	ID      int64  `param:"id" validate:"required"`
	Scope   string `query:"scope" validate:"required"`
	ScopeID string `query:"scopeId" validate:"required"`
}) interface{} {
	err := p.N.DeleteNotifyRecord(params.ID)
	if err != nil {
		return api.Errors.Internal(err)
	}
	return api.Success(params.ID)
}

func (p *provider) updateNotify(r *http.Request, params model.UpdateNotifyReq) interface{} {
	err := params.CheckNotify()
	if err != nil {
		return api.Errors.Internal(err)
	}
	_, err = p.N.GetNotify(params.ID)
	if err != nil {
		return model.ErrUpdateNotify.InternalError(err).ToResp()
	}
	notify, err := ToNotify(&params)
	if err != nil {
		return model.ErrUpdateNotify.InternalError(err).ToResp()
	}
	err = p.N.UpdateNotify(notify)
	if err != nil {
		api.Errors.Internal(err)
	}
	return api.Success(params.ID)
}

//query user's notify configuration
func (p *provider) getUserNotifyList(r *http.Request, params struct {
	Scope   string `query:"scope" validate:"required"`
	ScopeID string `query:"scopeId" validate:"required"`
}) interface{} {
	queryList := &model.QueryNotifyListReq{
		Scope:   params.Scope,
		ScopeID: params.ScopeID,
	}
	notifies, err := p.N.GetNotifyList(queryList)
	if err != nil {
		return api.Errors.Internal(err)
	}
	resp := model.QueryNotifyListRes{}
	for _, v := range notifies {
		notifyInfo := model.NotifyRes{
			CreatedAt:  v.CreatedAt,
			Id:         int64(v.ID),
			NotifyID:   v.NotifyID,
			Target:     v.Target,
			Enable:     v.Enable,
			NotifyName: v.NotifyName,
		}
		//get groupId from target
		var target model.Target
		err = json.Unmarshal([]byte(v.Target), &target)
		if err != nil {
			return api.Errors.Internal(err)
		}
		//splice items
		templateNames := make([]string, 0)
		templateIds := make([]string, 0)
		err = json.Unmarshal([]byte(v.NotifyID), &templateIds)
		if err != nil {
			return api.Errors.Internal(err)
		}
		for _, v := range templateIds {
			if value, ok := templateMap[v]; ok {
				templateNames = append(templateNames, value.Metadata.Name)
			} else {
				//user define templates
				userDefine, err := p.N.GetUserDefine(v)
				if err != nil {
					return api.Errors.Internal(err)
				}
				metaData := model.Metadata{}
				err = yaml.Unmarshal([]byte(userDefine.Metadata), &metaData)
				if err != nil {
					return api.Errors.Internal(err)
				}
				templateNames = append(templateNames, metaData.Name)
			}
		}
		notifyInfo.Items = templateNames
		notifyGroup, err := p.N.GetNotifyGroup(target.GroupID)
		if err != nil {
			return api.Errors.Internal(err)
		}
		var targetData []model.NotifyTarget
		err = json.Unmarshal([]byte(notifyGroup.TargetData), &targetData)
		if err != nil {
			return api.Errors.Internal(err)
		}
		notifyInfo.NotifyTarget = targetData
		resp.List = append(resp.List, notifyInfo)
	}
	return api.Success(resp)
}

//whether to enable this notify
func (p *provider) notifyEnable(r *http.Request, params struct {
	ID      int64  `param:"id" validate:"required"`
	ScopeID string `query:"scopeId" validate:"required"`
	Scope   string `query:"scope" validate:"required"`
}) interface{} {
	err := p.N.UpdateEnable(params.ID)
	if err != nil {
		return api.Errors.Internal(err)
	}
	return api.Success(nil)
}

func (p *provider) createUserDefineNotifyTemplate(r *http.Request, params model.CreateUserDefineNotifyTemplate) interface{} {
	templates, err := p.N.CheckNotifyTemplateExist(params.Scope, params.ScopeID)
	if err != nil {
		return api.Errors.Internal(err)
	}
	templateArr := *templates
	if len(templateArr) > 0 {
		for _, v := range templateArr {
			metadata := model.Metadata{}
			err := yaml.Unmarshal([]byte(v.Metadata), &metadata)
			if err != nil {
				return api.Errors.Internal(err)
			}
			if metadata.Name == params.Name {
				return api.Errors.AlreadyExists(err)
			}
		}
	}
	customize, err := ToNotifyConfig(&params)
	if err != nil {
		return api.Errors.Internal(err)
	}
	err = p.N.CreateUserDefineNotifyTemplate(customize)
	if err != nil {
		return api.Errors.Internal(err)
	}
	return api.Success(customize.ID)
}

func (p *provider) getNotifyDetail(r *http.Request, params struct {
	Id int64 `param:"id"`
}) interface{} {
	data, err := p.N.GetNotify(params.Id)
	if err != nil {
		return api.Errors.Internal(err)
	}
	var target model.Target
	err = json.Unmarshal([]byte(data.Target), &target)
	if err != nil {
		return api.Errors.Internal(err)
	}
	notifyGroup, err := p.N.GetNotifyGroup(target.GroupID)
	if err != nil {
		return api.Errors.Internal(err)
	}
	notifyTarget := make([]model.NotifyTarget, 0)
	err = json.Unmarshal([]byte(notifyGroup.TargetData), &notifyTarget)
	if err != nil {
		return api.Errors.Internal(err)
	}
	notifyDetailResponse := model.NotifyDetailResponse{
		Id:         int64(data.ID),
		NotifyID:   data.NotifyID,
		NotifyName: data.NotifyName,
		Target:     data.Target,
		GroupType:  notifyTarget[0].Type,
	}
	return api.Success(notifyDetailResponse)
}

func (p *provider) getAllGroups(r *http.Request, params struct {
	Scope   string `query:"scope"`
	ScopeId string `query:"scopeId"`
}) interface{} {
	orgID, err := strconv.ParseInt(r.Header.Get("Org-ID"), 10, 64)
	if err != nil {
		return model.ErrCreateNotify.InvalidParameter(err.Error()).ToResp()
	}
	data, err := p.N.GetAllNotifyGroup(params.Scope, params.ScopeId, orgID)
	if err != nil {
		return api.Errors.Internal(err)
	}
	return api.Success(data)
}
