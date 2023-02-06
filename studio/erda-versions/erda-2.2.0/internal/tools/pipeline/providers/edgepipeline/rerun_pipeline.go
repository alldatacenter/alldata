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
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

func (s *provider) RerunPipeline(ctx context.Context, p *spec.Pipeline, req *apistructs.PipelineRerunRequest) (*apistructs.PipelineDTO, error) {
	canProxy := s.EdgeRegister.CanProxyToEdge(p.PipelineSource, p.ClusterName)

	if canProxy {
		s.Log.Infof("proxy rerun pipeline to edge, pipelineID: %d", p.ID)
		return s.proxyRerunPipelineRequestToEdge(ctx, p, req)
	}

	return s.directRerunPipeline(ctx, p, req)
}

func (s *provider) proxyRerunPipelineRequestToEdge(ctx context.Context, p *spec.Pipeline, req *apistructs.PipelineRerunRequest) (*apistructs.PipelineDTO, error) {
	// handle at edge side
	edgeBundle, err := s.EdgeRegister.GetEdgeBundleByClusterName(p.ClusterName)
	if err != nil {
		return nil, err
	}
	return edgeBundle.RerunPipeline(*req)
}

func (s *provider) directRerunPipeline(ctx context.Context, newP *spec.Pipeline, req *apistructs.PipelineRerunRequest) (*apistructs.PipelineDTO, error) {
	newP, err := s.pipelineSvc.Rerun(ctx, req)
	if err != nil {
		return nil, err
	}
	// report
	if s.EdgeRegister.IsEdge() {
		s.EdgeReporter.TriggerOncePipelineReport(newP.ID)
	}
	return s.pipelineSvc.ConvertPipeline(newP), nil
}
