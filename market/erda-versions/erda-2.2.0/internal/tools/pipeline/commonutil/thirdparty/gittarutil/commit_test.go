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
//	"fmt"
//	"testing"
//
//	"github.com/stretchr/testify/require"
//)
//
//func TestRepo_GetCommit(t *testing.T) {
//	commit, err := r.GetCommit("feature/init_sql")
//	require.NoError(t, err)
//	fmt.Printf("%+v\n", commit)
//}

func TestGetCommit(t *testing.T) {
	monkey.Patch(httpclientutil.DoJson, func(r *httpclient.Request, o interface{}) error {
		return errors.New("the userID is empty")
	})
	repo := NewRepo("gittar", "/repo")
	_, err := repo.GetCommit("commitID", "")
	assert.Error(t, err)
}
