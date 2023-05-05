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
	"bytes"
	"io"

	"github.com/erda-project/erda-proto-go/dop/issue/core/pb"
	"github.com/erda-project/erda/internal/apps/dop/providers/issue/dao"
	"github.com/erda-project/erda/internal/apps/dop/services/i18n"
	"github.com/erda-project/erda/pkg/excel"
	"github.com/erda-project/erda/pkg/strutil"
)

func (p *provider) ExportExcel(issues []*pb.Issue, properties []*pb.IssuePropertyIndex, projectID uint64, isDownload bool, orgID int64, locale string) (io.Reader, string, error) {
	// list of  issue stage
	stages, err := p.db.GetIssuesStageByOrgID(orgID)
	if err != nil {
		return nil, "", err
	}
	// get the stageMap
	stageMap := getStageMap(stages)

	table, err := p.convertIssueToExcelList(issues, properties, projectID, isDownload, stageMap, locale)
	if err != nil {
		return nil, "", err
	}
	// replace userids by usernames
	userids := []string{}
	for _, t := range table[1:] {
		if t[4] != "" {
			userids = append(userids, t[4])
		}
		if t[5] != "" {
			userids = append(userids, t[5])
		}
		if t[6] != "" {
			userids = append(userids, t[6])
		}
	}
	userids = strutil.DedupSlice(userids, true)
	users, err := p.uc.FindUsers(userids)
	if err != nil {
		return nil, "", err
	}
	usernames := map[string]string{}
	for _, u := range users {
		usernames[u.ID] = u.Nick
	}
	for i := 1; i < len(table); i++ {
		if table[i][4] != "" {
			if name, ok := usernames[table[i][4]]; ok {
				table[i][4] = name
			}
		}
		if table[i][5] != "" {
			if name, ok := usernames[table[i][5]]; ok {
				table[i][5] = name
			}
		}
		if table[i][6] != "" {
			if name, ok := usernames[table[i][6]]; ok {
				table[i][6] = name
			}
		}
	}
	tablename := "issuetable"
	if len(issues) > 0 {
		if issues[0].IterationID == -1 {
			tablename = "待办事项"
		} else {
			tablename = issues[0].Type.String()
		}
	}

	// insert sample issue
	if isDownload {
		table = append(table, p.getIssueExportDataI18n(locale, i18n.I18nKeyIssueExportSample))
	}
	buf := bytes.NewBuffer([]byte{})
	if err := excel.ExportExcel(buf, table, tablename); err != nil {
		return nil, "", err
	}
	return buf, tablename, nil
}

// getStageMap return a map,the key is the struct of dice_issue_stage.Value and dice_issue_stage.IssueType,
// the value is dice_issue_stage.Name
func getStageMap(stages []dao.IssueStage) map[IssueStage]string {
	stageMap := make(map[IssueStage]string, len(stages))
	for _, v := range stages {
		if v.Value != "" && v.IssueType != "" {
			stage := IssueStage{
				Type:  v.IssueType,
				Value: v.Value,
			}
			stageMap[stage] = v.Name
		}
	}
	return stageMap
}
