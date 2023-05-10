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

func (s *provider) RunPipeline(ctx context.Context, p *spec.Pipeline, req *apistructs.PipelineRunRequest) error {
	if s.EdgeRegister.IsCenter() && p.IsEdge {
		s.Log.Infof("proxy run pipeline to edge, pipelineID: %d", p.ID)
		return s.proxyRunPipelineRequestToEdge(ctx, p, req)
	}

	return s.directRunPipeline(ctx, p, req)
}

func (s *provider) proxyRunPipelineRequestToEdge(ctx context.Context, p *spec.Pipeline, req *apistructs.PipelineRunRequest) error {
	// handle at edge side
	edgeBundle, err := s.EdgeRegister.GetEdgeBundleByClusterName(p.ClusterName)
	if err != nil {
		return err
	}
	return edgeBundle.RunPipeline(*req)
}

func (s *provider) directRunPipeline(ctx context.Context, p *spec.Pipeline, req *apistructs.PipelineRunRequest) error {
	if _, err := s.PipelineRun.RunOnePipeline(ctx, req); err != nil {
		return err
	}
	// report
	if s.EdgeRegister.IsEdge() {
		s.EdgeReporter.TriggerOncePipelineReport(p.ID)
	}
	return nil
}
