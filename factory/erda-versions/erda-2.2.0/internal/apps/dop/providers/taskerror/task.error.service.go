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

package taskerror

import (
	"context"

	"github.com/erda-project/erda-proto-go/core/dop/taskerror/pb"
	errboxpb "github.com/erda-project/erda-proto-go/core/services/errorbox/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/dop/services/apierrors"
	"github.com/erda-project/erda/pkg/common/apis"
)

type TaskErrorService struct {
	p         *provider
	bdl       *bundle.Bundle
	errBoxSvc errboxpb.ErrorBoxServiceServer
}

type Option func(service *TaskErrorService)

func (s *TaskErrorService) WithErrorBoxSvc(errBoxSvc errboxpb.ErrorBoxServiceServer) {
	s.errBoxSvc = errBoxSvc
}

func (s *TaskErrorService) ListErrorLog(ctx context.Context, req *pb.ErrorLogListRequest) (*pb.ErrorLogListResponseData, error) {
	if !apis.IsInternalClient(ctx) {
		permissionReq := apistructs.PermissionCheckRequest{
			UserID:   apis.GetUserID(ctx),
			Scope:    apistructs.ScopeType(req.ScopeType),
			ScopeID:  req.ScopeId,
			Resource: req.ScopeType,
			Action:   apistructs.GetAction,
		}
		if access, err := s.bdl.CheckPermission(&permissionReq); err != nil || !access.Access {
			return nil, apierrors.ErrListErrorLog.AccessDenied()
		}
	}
	logReq := apistructs.ErrorLogListRequest{
		ScopeType:    apistructs.ScopeType(req.ScopeType),
		ScopeID:      req.ScopeId,
		ResourceID:   req.ResourceId,
		StartTime:    req.StartTime,
		ResourceType: apistructs.ErrorResourceType(req.ResourceType),
	}
	if err := logReq.Check(); err != nil {
		return nil, apierrors.ErrListErrorLog.InvalidParameter(err)
	}

	errLogs, err := s.List(&logReq)
	if err != nil {
		return nil, apierrors.ErrListErrorLog.InternalError(err)
	}

	return &pb.ErrorLogListResponseData{List: errLogs}, nil
}
