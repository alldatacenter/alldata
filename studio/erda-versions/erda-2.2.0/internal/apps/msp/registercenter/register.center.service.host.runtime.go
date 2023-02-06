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

// GetHostRuntimeRule depracated
func (s *registerCenterService) GetHostRuntimeRule(ctx context.Context, req *pb.GetHostRuntimeRuleRequest) (*pb.GetHostRuntimeRuleResponse, error) {
	namespace := req.TenantID
	if len(namespace) <= 0 {
		namespace = req.ProjectID + "_" + strings.ToLower(req.Env)
	}
	host, err := s.getzkProxyHost(req.ClusterName)
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	adp := zkproxy.NewAdapter(req.ClusterName, host)
	rule, err := adp.GetHostRuntimeRule(req.ProjectID, req.Env, req.Host, namespace)
	if err != nil {
		return nil, errors.NewServiceInvokingError("zkproxy.GetHostRuntimeRule", err)
	}
	return &pb.GetHostRuntimeRuleResponse{Data: rule}, nil
}

// CreateHostRuntimeRule depracated
func (s *registerCenterService) CreateHostRuntimeRule(ctx context.Context, req *pb.CreateHostRuntimeRuleRequest) (*pb.CreateHostRuntimeRuleResponse, error) {
	namespace := req.TenantID
	if len(namespace) <= 0 {
		namespace = req.ProjectID + "_" + strings.ToLower(req.Env)
	}
	host, err := s.getzkProxyHost(req.ClusterName)
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	adp := zkproxy.NewAdapter(req.ClusterName, host)
	rule, err := adp.CreateHostRuntimeRule(req.ProjectID, req.Env, req.Host, namespace, req.Rules)
	if err != nil {
		return nil, errors.NewServiceInvokingError("zkproxy.CreateHostRuntimeRule", err)
	}
	return &pb.CreateHostRuntimeRuleResponse{Data: rule}, nil
}

// GetAllHostRuntimeRules depracated
func (s *registerCenterService) GetAllHostRuntimeRules(ctx context.Context, req *pb.GetAllHostRuntimeRulesRequest) (*pb.GetAllHostRuntimeRulesResponse, error) {
	namespace := req.TenantID
	if len(namespace) <= 0 {
		namespace = req.ProjectID + "_" + strings.ToLower(req.Env)
	}
	host, err := s.getzkProxyHost(req.ClusterName)
	if err != nil {
		return nil, errors.NewDatabaseError(err)
	}
	adp := zkproxy.NewAdapter(req.ClusterName, host)
	rules, err := adp.GetAllHostRuntimeRules(req.ProjectID, req.Env, req.AppID, namespace)
	if err != nil {
		return nil, errors.NewServiceInvokingError("zkproxy.GetAllHostRuntimeRules", err)
	}
	return &pb.GetAllHostRuntimeRulesResponse{Data: rules}, nil
}
