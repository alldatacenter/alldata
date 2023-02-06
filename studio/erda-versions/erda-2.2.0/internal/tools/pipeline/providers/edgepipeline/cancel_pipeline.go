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

func (s *provider) CancelPipeline(ctx context.Context, p *spec.Pipeline, req *apistructs.PipelineCancelRequest) error {
	if s.EdgeRegister.IsCenter() && p.IsEdge {
		s.Log.Infof("proxy cancel pipeline to edge, pipelineID: %d", p.ID)
		return s.proxyCancelPipelineRequestToEdge(ctx, p, req)
	}

	return s.directCancelPipeline(ctx, p, req)
}

func (s *provider) proxyCancelPipelineRequestToEdge(ctx context.Context, p *spec.Pipeline, req *apistructs.PipelineCancelRequest) error {
	// handle at edge side
	edgeBundle, err := s.EdgeRegister.GetEdgeBundleByClusterName(p.ClusterName)
	if err != nil {
		return err
	}
	return edgeBundle.CancelPipeline(*req)
}

func (s *provider) directCancelPipeline(ctx context.Context, p *spec.Pipeline, req *apistructs.PipelineCancelRequest) error {
	if err := s.PipelineCancel.CancelOnePipeline(ctx, req); err != nil {
		return err
	}
	// report
	if s.EdgeRegister.IsEdge() {
		s.EdgeReporter.TriggerOncePipelineReport(p.ID)
	}
	return nil
}
