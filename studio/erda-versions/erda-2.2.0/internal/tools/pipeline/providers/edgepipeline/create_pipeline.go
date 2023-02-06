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

	"github.com/erda-project/erda/apistructs"
)

func (s *provider) CreatePipeline(ctx context.Context, req *apistructs.PipelineCreateRequestV2) (*apistructs.PipelineDTO, error) {
	canProxy := s.EdgeRegister.CanProxyToEdge(req.PipelineSource, req.ClusterName)

	if canProxy {
		s.Log.Infof("proxy create pipeline to edge, source: %s, yamlName: %s", req.PipelineSource, req.PipelineYmlName)
		return s.proxyCreatePipelineRequestToEdge(ctx, req)
	}

	return s.directCreatePipeline(ctx, req)
}

func (s *provider) proxyCreatePipelineRequestToEdge(ctx context.Context, req *apistructs.PipelineCreateRequestV2) (*apistructs.PipelineDTO, error) {
	// handle at edge side
	edgeBundle, err := s.EdgeRegister.GetEdgeBundleByClusterName(req.ClusterName)
	if err != nil {
		return nil, err
	}
	p, err := edgeBundle.CreatePipeline(req)
	if err != nil {
		return nil, err
	}
	return p, nil
}

func (s *provider) directCreatePipeline(ctx context.Context, req *apistructs.PipelineCreateRequestV2) (*apistructs.PipelineDTO, error) {
	p, err := s.pipelineSvc.CreateV2(ctx, req)
	if err != nil {
		return nil, err
	}
	// report
	if s.EdgeRegister.IsEdge() {
		s.EdgeReporter.TriggerOncePipelineReport(p.ID)
	}
	return s.pipelineSvc.ConvertPipeline(p), nil
}
