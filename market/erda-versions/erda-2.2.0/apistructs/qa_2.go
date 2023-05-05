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

package apistructs

import (
	"time"
)

type TPRecord struct {
	ID        int64     `json:"id"`
	CreatedAt time.Time `json:"createdAt"`
	UpdatedAt time.Time `json:"updatedAt"`

	ApplicationID   int64             `json:"applicationId"`
	ProjectID       int64             `json:"projectId"`
	BuildID         int64             `json:"buildId"`
	Name            string            `json:"name"`
	UUID            string            `json:"uuid"`
	ApplicationName string            `json:"applicationName"`
	Output          string            `json:"output"`
	Desc            string            `json:"desc"`
	OperatorID      string            `json:"operatorId"`
	OperatorName    string            `json:"operatorName"`
	CommitID        string            `json:"commitId"`
	Branch          string            `json:"branch"`
	GitRepo         string            `json:"gitRepo"`
	CaseDir         string            `json:"caseDir"`
	Application     string            `json:"application"`
	Avatar          string            `json:"avatar,omitempty"`
	TType           TestType          `json:"type"`
	Totals          *TestTotals       `json:"totals"`
	ParserType      string            `json:"parserType"`
	Extra           map[string]string `json:"extra,omitempty"`
	Envs            map[string]string `json:"envs"`
	Workspace       DiceWorkspace     `json:"workspace"`
	Suites          []*TestSuite      `json:"suites"`
}
