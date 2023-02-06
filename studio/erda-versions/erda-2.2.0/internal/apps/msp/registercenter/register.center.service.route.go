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

package registercenter

import (
	"context"
	"strings"

	"github.com/erda-project/erda-proto-go/msp/registercenter/pb"
	"github.com/erda-project/erda/internal/apps/msp/registercenter/zkproxy"
	"github.com/erda-project/erda/pkg/common/errors"
)

// GetRouteRule depracated
func (s *registerCenterService) GetRouteRule(ctx context.Context, req *pb.GetRouteRuleRequest) (*pb.GetRouteRuleResponse, error) {
	namespace := req.TenantID
	if len(namespace) <= 0 {
		namespace = req.ProjectID + "_" + strings.ToLower(req.Env)
	}
	host, err := s.getzkProxyHost(req.ClusterName)
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	adp := zkproxy.NewAdapter(req.ClusterName, host)
	rule, err := adp.GetRouteRule(req.InterfaceName, req.ProjectID, req.Env, namespace)
	if err != nil {
		return nil, errors.NewServiceInvokingError("zkproxy.GetRouteRule", err)
	}
	return &pb.GetRouteRuleResponse{
		Data: rule,
	}, nil
}

// CreateRouteRule depracated
func (s *registerCenterService) CreateRouteRule(ctx context.Context, req *pb.CreateRouteRuleRequest) (*pb.CreateRouteRuleResponse, error) {
	namespace := req.TenantID
	if len(namespace) <= 0 {
		namespace = req.ProjectID + "_" + strings.ToLower(req.Env)
	}
	host, err := s.getzkProxyHost(req.ClusterName)
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	adp := zkproxy.NewAdapter(req.ClusterName, host)
	rule, err := adp.CreateRouteRule(req.InterfaceName, req.ProjectID, req.Env, namespace, req.Rule)
	if err != nil {
		return nil, errors.NewServiceInvokingError("zkproxy.CreateRouteRule", err)
	}
	return &pb.CreateRouteRuleResponse{Data: rule}, nil
}

// DeleteRouteRule depracated
func (s *registerCenterService) DeleteRouteRule(ctx context.Context, req *pb.DeleteRouteRuleRequest) (*pb.DeleteRouteRuleResponse, error) {
	namespace := req.TenantID
	if len(namespace) <= 0 {
		namespace = req.ProjectID + "_" + strings.ToLower(req.Env)
	}
	host, err := s.getzkProxyHost(req.ClusterName)
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	adp := zkproxy.NewAdapter(req.ClusterName, host)
	rule, err := adp.DeleteRouteRule(req.InterfaceName, req.ProjectID, req.Env, namespace)
	if err != nil {
		return nil, errors.NewServiceInvokingError("zkproxy.DeleteRouteRule", err)
	}
	return &pb.DeleteRouteRuleResponse{Data: rule}, nil
}
