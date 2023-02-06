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
	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/http/httpclientutil"
	"github.com/erda-project/erda/pkg/http/httputil"
)

type Ref struct {
	Name string `json:"name"`
}

func (r *Repo) Branches(userID string) ([]string, error) {
	var refs []Ref
	req := httpclient.New().Get(r.GittarAddr).
		Header(httputil.UserHeader, userID).
		Path("/" + r.Repo + "/branches")
	if err := httpclientutil.DoJson(req, &refs); err != nil {
		return nil, err
	}
	var branches []string
	for _, ref := range refs {
		branches = append(branches, ref.Name)
	}
	return branches, nil
}

func (r *Repo) Tags(userID string) ([]string, error) {
	var refs []Ref
	req := httpclient.New().Get(r.GittarAddr).
		Header(httputil.UserHeader, userID).
		Path("/" + r.Repo + "/tags")
	if err := httpclientutil.DoJson(req, &refs); err != nil {
		return nil, err
	}
	var tags []string
	for _, ref := range refs {
		tags = append(tags, ref.Name)
	}
	return tags, nil
}
