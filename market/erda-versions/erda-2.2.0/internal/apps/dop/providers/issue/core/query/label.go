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

import (
	"fmt"
	"strconv"

	commonpb "github.com/erda-project/erda-proto-go/common/pb"
	syncpb "github.com/erda-project/erda-proto-go/dop/issue/sync/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
)

func (p *provider) UpdateLabels(id, projectID uint64, labelNames []string) (err error) {
	if err = p.db.DeleteLabelRelations(apistructs.LabelTypeIssue, strconv.FormatUint(id, 10), nil); err != nil {
		return
	}
	labels, err := p.bdl.ListLabelByNameAndProjectID(projectID, labelNames)
	if err != nil {
		return
	}
	labelRelations := make([]dao.LabelRelation, 0, len(labels))
	for _, v := range labels {
		labelRelations = append(labelRelations, dao.LabelRelation{
			LabelID: uint64(v.ID),
			RefType: apistructs.LabelTypeIssue,
			RefID:   strconv.FormatUint(id, 10),
		})
	}
	return p.db.BatchCreateLabelRelations(labelRelations)
}

func (p *provider) SyncLabels(value *syncpb.Value, issueIDs []uint64) error {
	if value == nil {
		return fmt.Errorf("value is empty")
	}
	labelsAdd := value.Addition
	labelsDelete := value.Deletion
	for _, id := range issueIDs {
		issue, err := p.GetIssue(int64(id), &commonpb.IdentityInfo{InternalClient: apistructs.SystemOperator})
		if err != nil {
			return err
		}
		currentLabelMap := make(map[int64]bool)
		for _, v := range issue.LabelDetails {
			currentLabelMap[v.Id] = true
		}
		labelRelations := make([]dao.LabelRelation, 0, len(labelsAdd))
		for _, v := range labelsAdd {
			labelID := int64(v.GetNumberValue())
			if _, ok := currentLabelMap[labelID]; !ok {
				labelRelations = append(labelRelations, dao.LabelRelation{
					LabelID: uint64(labelID),
					RefType: apistructs.LabelTypeIssue,
					RefID:   strconv.FormatUint(id, 10),
				})
			}
		}
		if len(labelRelations) > 0 {
			if err := p.db.BatchCreateLabelRelations(labelRelations); err != nil {
				return err
			}
		}

		labelIDs := make([]uint64, 0, len(labelsDelete))
		for _, v := range labelsDelete {
			labelID := int64(v.GetNumberValue())
			if _, ok := currentLabelMap[labelID]; ok {
				labelIDs = append(labelIDs, uint64(labelID))
			}
		}
		if len(labelIDs) > 0 {
			if err := p.db.DeleteLabelRelations(apistructs.LabelTypeIssue, strconv.FormatUint(id, 10), labelIDs); err != nil {
				return err
			}
		}

		if err = p.Stream.CreateIssueStreamBySystem(id, map[string][]interface{}{
			"label": {"", "", apistructs.ParentLabelsChanged},
		}); err != nil {
			return err
		}
	}
	return nil
}
