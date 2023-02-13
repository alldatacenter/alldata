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
	"testing"

	"bou.ke/monkey"
	"github.com/pkg/errors"
	"github.com/stretchr/testify/assert"

	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/http/httpclientutil"
)

//import (
//	"testing"
//
//	"github.com/davecgh/go-spew/spew"
//	"github.com/stretchr/testify/require"
//)
//
//var r *Repo
//
//func init() {
//	r = NewRepo("gittar.marathon.l4lb.thisdcos.directory:5566", "terminus-dice-test/pampas-blog")
//}
//
//func TestRepo_Branches(t *testing.T) {
//	branches, err := r.Branches()
//	require.NoError(t, err)
//	spew.Dump(branches)
//}
//
//func TestRepo_Tags(t *testing.T) {
//	tags, err := r.Tags()
//	require.NoError(t, err)
//	spew.Dump(tags)
//}

func TestBranches(t *testing.T) {
	monkey.Patch(httpclientutil.DoJson, func(r *httpclient.Request, o interface{}) error {
		return errors.New("the userID is empty")
	})
	repo := NewRepo("gittar", "/repo")
	_, err := repo.Branches("")
	assert.Error(t, err)
}
