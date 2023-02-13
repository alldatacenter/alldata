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
	"context"

	"github.com/erda-project/erda-proto-go/core/monitor/metric/pb"
	"github.com/erda-project/erda/internal/tools/monitor/core/metric/query/metricmeta"
	"github.com/erda-project/erda/pkg/common/apis"
)

type metricMetaService struct {
	p    *provider
	meta *metricmeta.Manager
}

func (s *metricMetaService) ListMetricNames(ctx context.Context, req *pb.ListMetricNamesRequest) (*pb.ListMetricNamesResponse, error) {
	list, err := s.meta.MetricNames(apis.Language(ctx), req.Scope, req.ScopeID)
	if err != nil {
		return nil, err
	}
	return &pb.ListMetricNamesResponse{Data: list}, nil
}
func (s *metricMetaService) ListMetricMeta(ctx context.Context, req *pb.ListMetricMetaRequest) (*pb.ListMetricMetaResponse, error) {
	list, err := s.meta.MetricMeta(apis.Language(ctx), req.Scope, req.ScopeID, req.Metrics...)
	if err != nil {
		return nil, err
	}
	return &pb.ListMetricMetaResponse{Data: list}, nil
}
func (s *metricMetaService) ListMetricGroups(ctx context.Context, req *pb.ListMetricGroupsRequest) (*pb.ListMetricGroupsResponse, error) {
	list, err := s.meta.MetricGroups(apis.Language(ctx), req.Scope, req.ScopeID, req.Mode)
	if err != nil {
		return nil, err
	}
	return &pb.ListMetricGroupsResponse{Data: list}, nil
}
func (s *metricMetaService) GetMetricGroup(ctx context.Context, req *pb.GetMetricGroupRequest) (*pb.GetMetricGroupResponse, error) {
	if len(req.Format) <= 0 {
		if req.Version == "v2" {
			req.Format = metricmeta.InfluxFormat
			req.AppendTags = true
		} else if len(req.Format) <= 0 && req.Mode != "analysis" {
			// However, alarm expressions do not support dot format, so metadata queries that are not in alarm mode are all in dot format.
			req.Format = metricmeta.DotFormat
		}
	}
	list, err := s.meta.MetricGroup(apis.Language(ctx), req.Scope, req.ScopeID, req.Id, req.Mode, req.Format, req.AppendTags)
	if err != nil {
		return nil, err
	}
	return &pb.GetMetricGroupResponse{Data: list}, nil
}

func (s *metricMetaService) RegisterMetricMeta(ctx context.Context, req *pb.RegisterMetricMetaRequest) (*pb.RegisterMetricMetaResponse, error) {
	err := s.meta.RegeistMetricMeta(req.Scope, req.ScopeID, req.Group, req.Meta)
	if err != nil {
		return nil, err
	}
	return &pb.RegisterMetricMetaResponse{
		Data: true,
	}, nil
}

func (s *metricMetaService) UnRegisterMetricMeta(ctx context.Context, req *pb.UnRegisterMetricMetaRequest) (*pb.UnRegisterMetricMetaResponse, error) {
	err := s.meta.UnregeistMetricMeta(req.Scope, req.ScopeID, req.Group, req.MetricName)
	if err != nil {
		return nil, err
	}
	return &pb.UnRegisterMetricMetaResponse{
		Data: true,
	}, nil
}
