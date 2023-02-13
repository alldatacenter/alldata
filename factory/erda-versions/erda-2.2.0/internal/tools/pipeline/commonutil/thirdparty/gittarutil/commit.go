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

package gittarutil

import (
	"github.com/pkg/errors"

	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/http/httpclientutil"
	"github.com/erda-project/erda/pkg/http/httputil"
)

type Commit struct {
	ID        string `json:"id"`
	Committer struct {
		Email string `json:"email"`
		Name  string `json:"name"`
		When  string `json:"when"`
	} `json:"committer"`
	CommitMessage string `json:"commitMessage"`
}

func (r *Repo) GetCommit(ref, userID string) (Commit, error) {
	var commit []Commit
	req := httpclient.New().Get(r.GittarAddr).
		Header(httputil.UserHeader, userID).
		Path("/"+r.Repo+"/commits/"+ref).
		Param("pageNo", "1").Param("pageSize", "1")

	if err := httpclientutil.DoJson(req, &commit); err != nil {
		return Commit{}, err
	}
	if len(commit) == 0 {
		return Commit{}, errors.New("no commit found")
	}
	return commit[0], nil
}
