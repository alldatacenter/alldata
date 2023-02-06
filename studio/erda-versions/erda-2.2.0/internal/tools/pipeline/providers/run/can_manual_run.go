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

package run

import (
	"context"
	"fmt"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

func (s *provider) CanManualRun(ctx context.Context, p *spec.Pipeline) (reason string, can bool) {
	can = false

	if p.Status != apistructs.PipelineStatusAnalyzed {
		reason = fmt.Sprintf("pipeline already begin run")
		return
	}
	if p.Extra.ShowMessage != nil && p.Extra.ShowMessage.AbortRun {
		reason = "abort run, please check PreCheck result"
		return
	}
	if p.Type == apistructs.PipelineTypeRerunFailed && p.Extra.RerunFailedDetail != nil {
		rerunPipelineID := p.Extra.RerunFailedDetail.RerunPipelineID
		if rerunPipelineID > 0 {
			origin, err := s.dbClient.GetPipeline(rerunPipelineID)
			if err != nil {
				reason = fmt.Sprintf("failed to get origin pipeline when set canManualRun, rerunPipelineID: %d, err: %v", rerunPipelineID, err)
				return
			}
			if origin.Extra.CompleteReconcilerGC {
				reason = fmt.Sprintf("dependent rerun pipeline already been cleaned, rerunPipelineID: %d", rerunPipelineID)
				return
			}
		}
	}

	// default
	return "", true
}
