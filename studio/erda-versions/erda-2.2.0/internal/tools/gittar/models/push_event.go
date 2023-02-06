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

package models

import (
	"github.com/erda-project/erda/internal/tools/gittar/pkg/gitmodule"
)

// PayloadPushEvent struct
// https://docs.gitlab.com/ce/user/project/integrations/webhooks.html#push-events

type PayloadPushEvent struct {
	ObjectKind        string                `json:"object_kind"`
	IsTag             bool                  `json:"is_tag"`
	Ref               string                `json:"ref"`
	After             string                `json:"after"`
	Before            string                `json:"before"`
	IsDelete          bool                  `json:"is_delete"`
	Commits           []PayloadCommit       `json:"commits"`
	TotalCommitsCount int                   `json:"total_commits_count"`
	Pusher            *User                 `json:"pusher"`
	Repository        *gitmodule.Repository `json:"repository"`
}

func (p *PayloadPushEvent) IsCreateNewBranch() bool {
	return p.Before == gitmodule.INIT_COMMIT_ID
}

func (p *PayloadPushEvent) IsDeleteBranch() bool {
	return p.After == gitmodule.INIT_COMMIT_ID
}
