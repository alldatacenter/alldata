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

// GetHostRule depracated
func (s *registerCenterService) GetHostRule(ctx context.Context, req *pb.CetHostRuleRequest) (*pb.CetHostRuleResponse, error) {
	namespace := req.TenantID
	if len(namespace) <= 0 {
		namespace = req.ProjectID + "_" + strings.ToLower(req.Env)
	}
	host, err := s.getzkProxyHost(req.ClusterName)
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	adp := zkproxy.NewAdapter(req.ClusterName, host)
	rule, err := adp.GetHostRule(req.ProjectID, req.Env, req.AppID, namespace)
	if err != nil {
		return nil, errors.NewServiceInvokingError("zkproxy.GetHostRule", err)
	}
	return &pb.CetHostRuleResponse{
		Data: rule,
	}, nil
}

// CreateHostRule depracated
func (s *registerCenterService) CreateHostRule(ctx context.Context, req *pb.CreateHostRuleRequest) (*pb.CreateHostRuleResponse, error) {
	namespace := req.TenantID
	if len(namespace) <= 0 {
		namespace = req.ProjectID + "_" + strings.ToLower(req.Env)
	}
	host, err := s.getzkProxyHost(req.ClusterName)
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	adp := zkproxy.NewAdapter(req.ClusterName, host)
	rule, err := adp.CreateHostRoute(req.ProjectID, req.Env, req.AppID, namespace, req.Rules)
	if err != nil {
		return nil, errors.NewServiceInvokingError("zkproxy.CreateHostRoute", err)
	}
	return &pb.CreateHostRuleResponse{
		Data: rule,
	}, nil
}

// DeleteHostRule depracated
func (s *registerCenterService) DeleteHostRule(ctx context.Context, req *pb.DeleteHostRuleRequest) (*pb.DeleteHostRuleResponse, error) {
	namespace := req.TenantID
	if len(namespace) <= 0 {
		namespace = req.ProjectID + "_" + strings.ToLower(req.Env)
	}
	host, err := s.getzkProxyHost(req.ClusterName)
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	adp := zkproxy.NewAdapter(req.ClusterName, host)
	rule, err := adp.DeleteHostRoute(req.ProjectID, req.Env, req.AppID, namespace)
	if err != nil {
		return nil, errors.NewServiceInvokingError("zkproxy.DeleteHostRoute", err)
	}
	return &pb.DeleteHostRuleResponse{
		Data: rule,
	}, nil
}
