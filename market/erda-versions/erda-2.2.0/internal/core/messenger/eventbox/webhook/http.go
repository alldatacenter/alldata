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

package webhook

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"strconv"
	"strings"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-proto-go/core/messenger/eventbox/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/core/legacy"
	"github.com/erda-project/erda/internal/core/legacy/types"
	"github.com/erda-project/erda/pkg/common/apis"
)

const (
	BadRequestCode        = "WH400"
	InternalServerErrCode = "WH500"
	OtherErrCode          = "WH600"
)

func toCode(e error) string {
	switch e {
	case BadRequestErr:
		return BadRequestCode
	case InternalServerErr:
		return InternalServerErrCode
	}
	return OtherErrCode
}

type WebHookHTTP struct {
	impl        *WebHookImpl
	CoreService legacy.ExposedInterface
}

func NewWebHookHTTP() (*WebHookHTTP, error) {
	impl, err := NewWebHookImpl()
	if err != nil {
		return nil, err
	}
	if err := MakeSureBuiltinHooks(impl); err != nil {
		return nil, err
	}
	return &WebHookHTTP{
		impl: impl,
	}, nil
}

func (w *WebHookHTTP) ListHooks(ctx context.Context, req *pb.ListHooksRequest, vars map[string]string) (*pb.ListHooksResponse, error) {
	//todo check
	location, err := extractHookLocation(req)
	if err != nil {
		return nil, err
	}
	if location.Org == "" {
		return nil, fmt.Errorf("not provide org")
	}
	r, err := w.impl.ListHooks(location)
	if err != nil {
		return nil, err
	}

	if err != nil {
		return nil, err
	}
	data, err := json.Marshal(r)
	if err != nil {
		return nil, err
	}
	hooks := make([]*pb.Hook, 0)
	err = json.Unmarshal(data, &hooks)
	if err != nil {
		return nil, err
	}
	return &pb.ListHooksResponse{
		Data: hooks,
	}, nil
}

// not require 'org' in query param,
// and it should return not found when found-hook's org not match orgID(in header)

func (w *WebHookHTTP) InspectHook(ctx context.Context, req *pb.InspectHookRequest, vars map[string]string) (*pb.InspectHookResponse, error) {
	orgID := apis.GetOrgID(ctx)

	r, err := w.impl.InspectHook(orgID, req.Id)
	if err != nil {
		return &pb.InspectHookResponse{
			Data: nil,
		}, err
	}
	data, err := json.Marshal(r)
	if err != nil {
		return &pb.InspectHookResponse{
			Data: nil,
		}, err
	}
	var hook *pb.Hook
	err = json.Unmarshal(data, &hook)
	return &pb.InspectHookResponse{
		Data: hook,
	}, nil
}

// org, project will be provided in request body
func (w *WebHookHTTP) CreateHook(ctx context.Context, req *pb.CreateHookRequest, vars map[string]string) (*pb.CreateHookResponse, error) {
	h := CreateHookRequest{}
	data, err := json.Marshal(req)
	if err != nil {
		return &pb.CreateHookResponse{
			Data: "",
		}, err
	}
	err = json.Unmarshal(data, &h)
	if err != nil {
		return &pb.CreateHookResponse{
			Data: "",
		}, err
	}
	orgId := apis.GetOrgID(ctx)
	r, err := w.impl.CreateHook(orgId, h)
	if err != nil {
		logrus.Error(err)
		return &pb.CreateHookResponse{
			Data: "",
		}, err
	}
	return &pb.CreateHookResponse{
		Data: string(r),
	}, nil
}

func (w *WebHookHTTP) EditHook(ctx context.Context, req *pb.EditHookRequest, vars map[string]string) (*pb.EditHookResponse, error) {
	e := EditHookRequest{}
	data, err := json.Marshal(req)
	if err != nil {
		return &pb.EditHookResponse{
			Data: "",
		}, err
	}
	err = json.Unmarshal(data, &e)
	if err != nil {
		return &pb.EditHookResponse{
			Data: "",
		}, err
	}
	orgID := apis.GetOrgID(ctx)
	r, err := w.impl.EditHook(orgID, req.Id, e)
	if err != nil {
		logrus.Error(err)
		return &pb.EditHookResponse{
			Data: "",
		}, nil
	}
	return &pb.EditHookResponse{
		Data: string(r),
	}, nil
}

func (w *WebHookHTTP) PingHook(ctx context.Context, req *pb.PingHookRequest, vars map[string]string) (*pb.PingHookResponse, error) {
	orgID := apis.GetOrgID(ctx)
	if err := w.impl.PingHook(orgID, req.Id); err != nil {
		logrus.Error(err)
		return &pb.PingHookResponse{
			Data: "",
		}, err
	}
	return &pb.PingHookResponse{
		Data: "",
	}, nil
}

func (w *WebHookHTTP) DeleteHook(ctx context.Context, req *pb.DeleteHookRequest, vars map[string]string) (*pb.DeleteHookResponse, error) {
	orgID := apis.GetOrgID(ctx)
	if err := w.impl.DeleteHook(orgID, req.Id); err != nil {
		logrus.Error(err)
		return &pb.DeleteHookResponse{
			Data: "",
		}, err
	}
	return &pb.DeleteHookResponse{
		Data: "",
	}, nil
}

type ListHookEventsResponse = apistructs.WebhookListEventsResponseData

func (w *WebHookHTTP) ListHookEvents(ctx context.Context, req *pb.ListHookEventsRequest, vars map[string]string) (*pb.ListHookEventsResponse, error) {
	r := []*pb.HookEvents{
		{
			Key:   "runtime",
			Title: "runtime",
			Desc:  "runtime 的创建，删除",
		},
		{
			Key:   "pipeline",
			Title: "pipeline",
			Desc:  "pipeline 的创建(create)，停止(stop)，完成(finish)",
		},
	}
	return &pb.ListHookEventsResponse{
		Data: r,
	}, nil
}

func extractHookLocation(req *pb.ListHooksRequest) (apistructs.HookLocation, error) {
	var env []string
	if req.Env != "" {
		envraw := strings.Split(req.Env, ",")
		for i := range envraw {
			if envraw[i] != "" {
				normalizedEnv := strings.ToLower(strings.TrimSpace(envraw[i]))
				if normalizedEnv != "dev" && normalizedEnv != "test" && normalizedEnv != "staging" && normalizedEnv != "prod" {
					return apistructs.HookLocation{}, fmt.Errorf("bad env param: %v", envraw[i])
				}
				env = append(env, strings.ToLower(strings.TrimSpace(envraw[i])))
			}
		}
	}
	return apistructs.HookLocation{
		Org:         req.OrgId,
		Project:     req.ProjectId,
		Application: req.ApplicationId,
		Env:         env,
	}, nil
}

func queryGetByOrder(req *http.Request, key ...string) string {
	for _, k := range key {
		v := req.URL.Query().Get(k)
		if v != "" {
			return v
		}
	}
	return ""
}

func extractOrgIDHeader(req *http.Request) string {
	return req.Header.Get("Org-ID")
}

func invalidResponse(msg ...string) error {
	message := "invalid query"
	if len(msg) > 0 {
		message = fmt.Sprintf("invalid query: %s", msg[0])
		return fmt.Errorf("%v", message)
	}
	return nil
}

func checkPermissionParam(req *apistructs.PermissionCheckRequest) error {
	if req.UserID == "" {
		return errors.Errorf("invalid request, user id is empty")
	}

	if _, ok := types.AllScopeRoleMap[req.Scope]; !ok {
		return errors.Errorf("invalid request, scope is invalid")
	}

	if req.Resource == "" {
		return errors.Errorf("invalid request, resource is empty")
	}
	if req.Action == "" {
		return errors.Errorf("invalid request, action is empty")
	}

	return nil
}

func (w *WebHookHTTP) CheckPermission(ctx context.Context, orgIdStr, projectIdStr, applicationIdStr string) error {
	userIdStr := apis.GetUserID(ctx)
	if userIdStr != "" {
		orgId, err := strconv.ParseUint(orgIdStr, 10, 64)
		if err != nil {
			return err
		}
		checkReq := &apistructs.PermissionCheckRequest{
			UserID:   userIdStr,
			Scope:    apistructs.OrgScope,
			ScopeID:  orgId,
			Resource: "webhook",
			Action:   "OPERATE",
		}
		checkPermissionParam(checkReq)
		if err != nil {
			return invalidResponse(err.Error())
		}
		checkResult, err := w.CoreService.CheckPermission(checkReq)
		if !checkResult {
			return invalidResponse("permission denied")
		}
		if projectIdStr != "" {
			projectId, err := strconv.ParseUint(projectIdStr, 10, 64)
			if err != nil {
				return invalidResponse("project")
			}
			checkReq := &apistructs.PermissionCheckRequest{
				UserID:   userIdStr,
				Scope:    apistructs.ProjectScope,
				ScopeID:  projectId,
				Resource: "webhook",
				Action:   "OPERATE",
			}
			checkPermissionParam(checkReq)
			if err != nil {
				return invalidResponse(err.Error())
			}
			checkResult, err := w.CoreService.CheckPermission(checkReq)
			if !checkResult {
				return invalidResponse("permission denied")
			}
		}
		if applicationIdStr != "" {
			appId, err := strconv.ParseUint(applicationIdStr, 10, 64)
			if err != nil {
				return invalidResponse("application")
			}
			checkReq := &apistructs.PermissionCheckRequest{
				UserID:   userIdStr,
				Scope:    apistructs.AppScope,
				ScopeID:  appId,
				Resource: "webhook",
				Action:   "OPERATE",
			}
			checkPermissionParam(checkReq)
			if err != nil {
				return invalidResponse(err.Error())
			}
			checkResult, err := w.CoreService.CheckPermission(checkReq)
			if !checkResult {
				return invalidResponse("permission denied")
			}
		}
	}
	return nil
}
