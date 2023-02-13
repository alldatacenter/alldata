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

package db

import (
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
)

func (client *Client) UpdatePipelineEdgeReportStatus(pipelineID uint64, status apistructs.EdgeReportStatus) error {
	_, err := client.ID(pipelineID).Cols("edge_report_status").Update(&spec.PipelineBase{EdgeReportStatus: status})
	return err
}

func (client *Client) ListEdgePipelineIDsForCompensatorReporter() (bases []spec.PipelineBase, err error) {
	err = client.Select("id").Where("is_edge = 1").Where("edge_report_status != ?", apistructs.DoneEdgeReportStatus).Find(&bases)
	return
}
