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
//	"github.com/stretchr/testify/require"
//)
//
//func TestRepo_FetchFiles(t *testing.T) {
//	_, err := r.FetchFiles("feature/init_sql", "pipeline.yml")
//	require.NoError(t, err)
//
//	_, err = r.FetchFiles("feature/init_sql", "dice.yml")
//	require.NoError(t, err)
//
//	_, err = r.FetchFiles("feature/init_sql", "no")
//	require.Error(t, err)
//}
//
//func TestRepo_FetchPipelineYml(t *testing.T) {
//	_, err := r.FetchPipelineYml("feature/empty-pipelineyml")
//	require.Error(t, err)
//}
//
//func TestRepo_FetchFile(t *testing.T) {
//	_, err := r.FetchFile("feature/init_sql", "README.md")
//	require.NoError(t, err)
//}
func TestFetchFile(t *testing.T) {
	monkey.Patch(httpclientutil.DoJson, func(r *httpclient.Request, o interface{}) error {
		return errors.New("the userID is empty")
	})
	repo := NewRepo("gittar", "/repo")
	_, err := repo.FetchFile("commitID", "erda.yml", "")
	assert.Error(t, err)
}
