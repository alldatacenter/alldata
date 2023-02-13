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

import "github.com/erda-project/erda-proto-go/dop/issue/core/pb"

func (p *provider) SyncIssueChildrenIteration(issue *pb.Issue, iterationID int64) error {
	if iterationID == 0 {
		return nil
	}
	u := &IssueUpdated{
		Id:          uint64(issue.Id),
		projectID:   issue.ProjectID,
		IterationID: iterationID,
	}
	c := &issueValidationConfig{}
	if iterationID > 0 {
		iteration, err := p.db.GetIteration(uint64(iterationID))
		if err != nil {
			return err
		}
		c.iteration = iteration
	}
	return p.UpdateIssueChildrenIteration(u, c)
}
