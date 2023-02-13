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

func (r *Repo) FetchFile(ref string, filename, userID string) (b []byte, err error) {
	defer func() {
		err = errors.Wrapf(err, "failed to fetch file from gittar, ref [%s], filename [%s]", ref, filename)
	}()
	var content struct {
		Content string `json:"content"`
	}
	req := httpclient.New().
		Get(r.GittarAddr, httpclient.RetryOption{}).
		Header(httputil.UserHeader, userID).
		Path("/"+r.Repo+"/blob/"+ref+"/"+filename).
		Param("expand", "false").Param("comment", "false")
	if err = httpclientutil.DoJson(req, &content); err != nil {
		return nil, err
	}
	if len(content.Content) == 0 {
		return nil, errors.New("file's content is empty")
	}
	return []byte(content.Content), nil
}
