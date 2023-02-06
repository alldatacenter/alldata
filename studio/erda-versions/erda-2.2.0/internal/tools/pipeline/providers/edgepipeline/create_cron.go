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

package edgepipeline

import (
	"context"

	cronpb "github.com/erda-project/erda-proto-go/core/pipeline/cron/pb"
	"github.com/erda-project/erda-proto-go/core/pipeline/pb"
	"github.com/erda-project/erda/apistructs"
)

func (s *provider) CreateCron(ctx context.Context, req *cronpb.CronCreateRequest) (*pb.Cron, error) {
	canProxy := s.EdgeRegister.CanProxyToEdge(apistructs.PipelineSource(req.PipelineSource), req.ClusterName)

	if canProxy {
		return s.proxyCreateCronRequestToEdge(ctx, req)
	}

	return s.directCreateCron(ctx, req)
}

func (s *provider) proxyCreateCronRequestToEdge(ctx context.Context, req *cronpb.CronCreateRequest) (*pb.Cron, error) {
	// handle at edge side
	edgeBundle, err := s.EdgeRegister.GetEdgeBundleByClusterName(req.ClusterName)
	if err != nil {
		return nil, err
	}
	c, err := edgeBundle.CronCreate(req)
	if err != nil {
		return nil, err
	}
	return c, nil
}

func (s *provider) directCreateCron(ctx context.Context, req *cronpb.CronCreateRequest) (*pb.Cron, error) {
	resp, err := s.Cron.CronCreate(ctx, req)
	if err != nil {
		return nil, err
	}
	// report
	if s.EdgeRegister.IsEdge() {
		s.EdgeReporter.TriggerOnceTaskReport(resp.Data.ID)
	}
	return resp.Data, nil
}
