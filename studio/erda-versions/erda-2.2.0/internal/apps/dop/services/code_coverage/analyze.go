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

package code_coverage

import (
	"github.com/erda-project/erda/apistructs"
)

func getAnalyzeJson(projectID uint64, projectName string, data []byte) ([]*apistructs.CodeCoverageNode, float64, error) {
	report, err := apistructs.ConvertXmlToReport(data)
	report.ProjectID = projectID
	report.ProjectName = projectName
	if err != nil {
		return nil, 0, err
	}

	root, coverage := apistructs.ConvertReportToTree(report)
	return root, coverage, nil
}
