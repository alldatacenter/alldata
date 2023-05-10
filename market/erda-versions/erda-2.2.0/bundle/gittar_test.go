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

package bundle

//import (
//	"os"
//	"testing"
//
//	"github.com/davecgh/go-spew/spew"
//	"github.com/stretchr/testify/assert"
//)
//
//func TestGetGittarLines(t *testing.T) {
//	// set env
//	os.Setenv("GITTAR_ADDR", "http://gittar.marathon.l4lb.thisdcos.directory:5566")
//
//	bdl := New(WithGittar())
//	gittarLines := &GittarLines{
//		Repo:     "http://gittar.marathon.l4lb.thisdcos.directory:5566/terminus-dice/pampas-blog",
//		CommitID: "0d4cc839a7502a688fe8e80cfe0508e46c7031fe",
//		Path:     "endpoints/showcase-front/shepherd.js",
//		Since:    "83",
//		To:       "83",
//	}
//
//	lines, err := bdl.GetGittarLines(gittarLines, "", "")
//	assert.Nil(t, err)
//	t.Log(lines)
//
//	os.Unsetenv("GITTAR_ADDR")
//}
//
//func TestGetGittarFile(t *testing.T) {
//	// set env
//	os.Setenv("GITTAR_ADDR", "http://gittar.marathon.l4lb.thisdcos.directory:5566")
//
//	bdl := New(WithGittar())
//
//	contents, err := bdl.GetGittarFile(
//		"http://gittar.test.terminus.io/terminus-test-testproject/pampas-sonar",
//		"develop",
//		"pipeline.yml",
//		"",
//		"",
//	)
//	assert.Nil(t, err)
//	t.Log(contents)
//
//	os.Unsetenv("GITTAR_ADDR")
//}
//
//func TestGetGittarCommit(t *testing.T) {
//	os.Setenv("GITTAR_ADDR", "gittar.default.svc.cluster.local:5566")
//
//	bdl := New(WithGittar())
//
//	commit, err := bdl.GetGittarCommit("dcos-terminus/dice", "742dc58410f3c05e4c601c2e9844612404f6737f")
//	assert.NoError(t, err)
//	spew.Dump(commit)
//}
//
//func TestGetGittarTree(t *testing.T) {
//	os.Setenv("GITTAR_ADDR", "gittar.default.svc.cluster.local:5566")
//
//	bdl := New(WithGittar())
//
//	commit, err := bdl.GetGittarTree("/wb/ss_pro1/apm-demo/tree/develop/pipeline.yml", "1")
//	assert.NoError(t, err)
//	spew.Dump(commit)
//}
