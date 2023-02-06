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

package gitmodule

import (
	"io/ioutil"
	"os"
	"runtime"
	"strings"
	"testing"
	"time"

	git "github.com/libgit2/git2go/v30"
)

func TestIsPathExist(t *testing.T) {

	repo, p := createTestRepo(t, "a/b/c", "foo.txt")
	defer cleanupTestRepo(t, repo)

	_, treeID := seedTestRepo(t, repo, p)
	tree, err := repo.LookupTree(treeID)
	checkFatal(t, err)

	tt := []struct {
		path     string
		wantPath string
	}{
		{"a/b/c", "a/b/c"},
		{"a/b/foo.txt", "a/b"},
		{"a/foo.txt", "a"},
		{"foo.txt", ""},
	}
	for _, v := range tt {
		if v.wantPath != isPathExist(tree, v.path) {
			t.Fatalf("fail")
		}
	}
}

func cleanupTestRepo(t *testing.T, r *git.Repository) {
	var err error
	if r.IsBare() {
		err = os.RemoveAll(r.Path())
	} else {
		err = os.RemoveAll(r.Workdir())
	}
	checkFatal(t, err)

	r.Free()
}

func createTestRepo(t *testing.T, path, f string) (*git.Repository, string) {
	// figure out where we can create the test repo
	rootPath, err := ioutil.TempDir("", "repo")
	checkFatal(t, err)
	repo, err := git.InitRepository(rootPath, false)
	checkFatal(t, err)

	l := len(rootPath)
	paths := strings.Split(path, "/")
	for _, v := range paths {
		if v != "" {
			rootPath = rootPath + "/" + v
			os.Mkdir(rootPath, 0777)
		}
	}

	rootPath = rootPath + "/" + f
	err = ioutil.WriteFile(rootPath, []byte("foo\n"), 0644)

	checkFatal(t, err)

	return repo, rootPath[l+1:]
}

func seedTestRepo(t *testing.T, repo *git.Repository, path string) (*git.Oid, *git.Oid) {
	loc, err := time.LoadLocation("Europe/Berlin")
	checkFatal(t, err)
	sig := &git.Signature{
		Name:  "Rand Om Hacker",
		Email: "random@hacker.com",
		When:  time.Date(2013, 03, 06, 14, 30, 0, 0, loc),
	}

	idx, err := repo.Index()
	checkFatal(t, err)
	err = idx.AddByPath(path)
	checkFatal(t, err)
	err = idx.Write()
	checkFatal(t, err)
	treeId, err := idx.WriteTree()
	checkFatal(t, err)

	message := "This is a commit\n"
	tree, err := repo.LookupTree(treeId)
	checkFatal(t, err)
	commitId, err := repo.CreateCommit("HEAD", sig, sig, message, tree)
	checkFatal(t, err)

	return commitId, treeId
}

func checkFatal(t *testing.T, err error) {
	if err == nil {
		return
	}

	// The failure happens at wherever we were called, not here
	_, file, line, ok := runtime.Caller(1)
	if !ok {
		t.Fatalf("Unable to get caller")
	}
	t.Fatalf("Fail at %v:%v; %v", file, line, err)
}
