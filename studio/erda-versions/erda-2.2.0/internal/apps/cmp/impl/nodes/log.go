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

package nodes

import (
	"errors"
	"fmt"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
)

func (n *Nodes) Logs(orgName string, req apistructs.OpLogsRequest) (*apistructs.DashboardSpotLogData, error) {
	records, err := n.db.RecordsReader().ByIDs(fmt.Sprintf("%d", req.RecordID)).Do()
	if err != nil {
		errstr := fmt.Sprintf("failed to get record: %v", err)
		logrus.Errorf(errstr)
		return nil, errors.New(errstr)
	}
	if len(records) == 0 {
		errstr := fmt.Sprintf("failed to get record: id: %d", req.RecordID)
		logrus.Errorf(errstr)
		return nil, errors.New(errstr)
	}
	record := records[0]
	task, err := n.bdl.GetPipelineTask(record.PipelineID, req.TaskID)
	if err != nil {
		errstr := fmt.Sprintf("failed to get pipelinetaskinfo: pipelineid: %d, taskid: %d, err: %v", record.PipelineID, req.TaskID, err)
		logrus.Errorf(errstr)
		return nil, errors.New(errstr)
	}
	logData, err := n.bdl.GetLog(orgName, apistructs.DashboardSpotLogRequest{
		ID:     task.Extra.UUID,
		Source: apistructs.DashboardSpotLogSourceJob,
		Stream: apistructs.DashboardSpotLogStream(req.Stream),
		Count:  req.Count,
		Start:  req.Start,
		End:    req.End,
	})
	if err != nil {
		errstr := fmt.Sprintf("failed to getlog: id: %s, err: %v", task.Extra.UUID, err)
		logrus.Errorf(errstr)
		return nil, errors.New(errstr)
	}
	return logData, nil
}
