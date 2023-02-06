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

package dbclient

import (
	"fmt"
)

func (client *Client) DeletePipelineRelated(pipelineID uint64, ops ...SessionOption) error {
	// 获取 pipeline
	p, err := client.GetPipeline(pipelineID, ops...)
	if err != nil {
		return err
	}
	// 校验当前流水线是否可被删除
	can, reason := p.CanDelete()
	if !can {
		return fmt.Errorf("cannot delete, reason: %s", reason)
	}

	// pipelines
	if err := client.DeletePipeline(pipelineID, ops...); err != nil {
		return err
	}

	// related pipeline stages
	if err := client.DeletePipelineStagesByPipelineID(pipelineID, ops...); err != nil {
		return err
	}

	// related pipeline tasks
	if err := client.DeletePipelineTasksByPipelineID(pipelineID, ops...); err != nil {
		return err
	}

	// related pipeline labels
	if err := client.DeletePipelineLabelsByPipelineID(pipelineID, ops...); err != nil {
		return err
	}

	// related pipeline reports
	if err := client.DeletePipelineReportsByPipelineID(pipelineID, ops...); err != nil {
		return err
	}

	return nil
}
