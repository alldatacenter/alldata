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

//go:build !codeanalysis
// +build !codeanalysis

package gitmodule

import (
	"context"
	"errors"
	"fmt"
	"path"
	"strings"

	"github.com/coreos/etcd/clientv3"
	"github.com/coreos/etcd/mvcc/mvccpb"
	git "github.com/libgit2/git2go/v30"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/internal/tools/gittar/conf"
)

type EditAction string
type EditPathType string

const (
	EDIT_ACTION_ADD    EditAction = "add"
	EDIT_ACTION_UPDATE EditAction = "update"
	EDIT_ACTION_DELETE EditAction = "delete"
	EDIT_ACTION_MOVE   EditAction = "move"
)

const (
	EDIT_PATH_TYPE_TREE EditPathType = "tree"
	EDIT_PATH_TYPE_BLOB EditPathType = "blob"
)

var (
	ErrNotSupportType      = errors.New("not support path type")
	ErrNotSupportAction    = errors.New("not support action")
	ErrFileAlreadyExists   = errors.New("a file with the same name already exists")
	ErrFolderAlreadyExists = errors.New("a folder with the same name already exists")
)

type EditActionItem struct {
	Action   EditAction   `json:"action"`
	Content  string       `json:"content"`
	Path     string       `json:"path"`
	PathType EditPathType `json:"pathType"`
}

type CreateCommit struct {
	Signature *Signature        `json:"-"`
	Message   string            `json:"message"`
	Actions   []*EditActionItem `json:"actions"`
	Branch    string            `json:"branch"`
}

func (req *CreateCommit) Validate() error {
	if strings.TrimSpace(req.Message) == "" {
		return errors.New("the message is empty")
	}
	if strings.TrimSpace(req.Branch) == "" {
		return errors.New("the branch is empty")
	}
	if len(req.Actions) <= 0 {
		return errors.New("the actions is empty")
	}
	return nil
}

// waitDelete wait for the etcd key delete
func waitDelete(key string, cli *clientv3.Client) {
	wch := cli.Watch(context.Background(), key)
	for wr := range wch {
		for _, ev := range wr.Events {
			switch ev.Type {
			case mvccpb.DELETE:
				return
			}
		}
	}
}

func (repo *Repository) CreateCommit(request *CreateCommit, etcdClient *clientv3.Client) (*Commit, error) {
	// check the key is exist or not
	key := fmt.Sprintf("/gittar/repo/%d", repo.ID)
	resp, err := etcdClient.Get(context.Background(), key)
	if err != nil {
		return nil, err
	}
	// if exist should wait
	if len(resp.Kvs) > 0 {
		waitDelete(key, etcdClient)
	}

	// minimum lease TTL is 5-second
	grantResp, err := etcdClient.Grant(context.Background(), 5)
	if err != nil {
		return nil, err
	}

	// put key with lease
	_, err = etcdClient.Put(context.Background(), key, "lock", clientv3.WithLease(grantResp.ID))
	if err != nil {
		return nil, err
	}

	// keep alive
	_, err = etcdClient.KeepAlive(context.Background(), grantResp.ID)
	if err != nil {
		return nil, err
	}

	defer func() {
		_, err = etcdClient.Revoke(context.Background(), grantResp.ID)
		if err != nil {
			logrus.Errorf("failed to revoke etcd, err: %v ", err)
		}
	}()

	branch := request.Branch
	message := request.Message
	isInitCommit := false
	rawRepo, err := repo.GetRawRepo()
	if err != nil {
		return nil, err
	}
	isEmpty, err := rawRepo.IsEmpty()
	if err != nil {
		return nil, err
	}
	if isEmpty {
		isInitCommit = true
	}

	index, err := rawRepo.Index()
	defer index.Free()
	if err != nil {
		return nil, err
	}

	var parentCommits []*git.Commit
	if !isInitCommit {
		// 不是init commit,读取对应分支内容到index
		branchCommit, err := repo.GetBranchCommit(branch)
		if err != nil {
			return nil, err
		}
		oldTreeOid, _ := git.NewOid(branchCommit.TreeSha)
		oldTree, err := rawRepo.LookupTree(oldTreeOid)
		if err != nil {
			return nil, err
		}
		parentOid, _ := git.NewOid(branchCommit.ID)
		parentCommit, err := rawRepo.LookupCommit(parentOid)
		if err != nil {
			return nil, err
		}
		parentCommits = append(parentCommits, parentCommit)
		index.ReadTree(oldTree)
	}

	for _, action := range request.Actions {
		if action.PathType == "" {
			action.PathType = EDIT_PATH_TYPE_BLOB
		}
		if action.PathType != EDIT_PATH_TYPE_BLOB &&
			action.PathType != EDIT_PATH_TYPE_TREE {
			return nil, ErrNotSupportType
		}
		if action.Action != EDIT_ACTION_ADD &&
			action.Action != EDIT_ACTION_UPDATE &&
			action.Action != EDIT_ACTION_DELETE {
			return nil, ErrNotSupportAction
		}
	}

	for _, action := range request.Actions {
		switch action.Action {
		case EDIT_ACTION_ADD, EDIT_ACTION_UPDATE:
			// judge whether files and folders exist
			if action.Action == EDIT_ACTION_ADD {
				if _, err = index.Find(action.Path); err == nil {
					return nil, ErrFileAlreadyExists
				}
				if _, err = index.FindPrefix(action.Path + "/"); err == nil {
					return nil, ErrFolderAlreadyExists
				}
			}

			var (
				content string
				path    string
			)
			if action.PathType == EDIT_PATH_TYPE_TREE {
				content = ""
				path = action.Path + "/.gitkeep"
			} else {
				content = action.Content
				path = action.Path
			}
			oid, err := rawRepo.CreateBlobFromBuffer([]byte(content))
			if err != nil {
				return nil, err
			}
			if err = index.Add(&git.IndexEntry{
				Mode: git.FilemodeBlob,
				Id:   oid,
				Path: path,
			}); err != nil {
				return nil, err
			}
		case EDIT_ACTION_DELETE:
			if action.PathType == EDIT_PATH_TYPE_TREE {
				if err = index.RemoveDirectory(action.Path, 0); err != nil {
					return nil, err
				}
			} else {
				if err = index.RemoveByPath(action.Path); err != nil {
					return nil, err
				}
			}
		}
	}

	err = index.Write()
	if err != nil {
		return nil, err
	}

	sig := &git.Signature{
		Name:  request.Signature.Name,
		Email: request.Signature.Email,
		When:  request.Signature.When,
	}

	newTreeOid, err := index.WriteTree()
	if err != nil {
		return nil, err
	}

	newTree, err := rawRepo.LookupTree(newTreeOid)
	if err != nil {
		return nil, err
	}

	var parentDirPath string
	for _, action := range request.Actions {
		if action.Action == EDIT_ACTION_DELETE {
			parentDirPath = isPathExist(newTree, action.Path)
			break
		}
	}

	newOid, err := rawRepo.CreateCommit(BRANCH_PREFIX+branch, sig, sig, message, newTree, parentCommits...)
	if err != nil {
		return nil, err
	}
	if isInitCommit {
		//把第一次提交的分支设为默认分支
		if err := rawRepo.SetHead(BRANCH_PREFIX + branch); err != nil {
			return nil, err
		}
	}

	commit, err := repo.GetCommit(newOid.String())
	if err != nil {
		return nil, err
	}
	commit.ParentDirPath = parentDirPath

	// 外置仓库推送代码过去
	if repo.IsExternal {
		repoPath := path.Join(conf.RepoRoot(), repo.Path)
		err = PushExternalRepository(repoPath)
		if err != nil {
			return nil, err
		}
	}

	return commit, nil
}

// isPathExist check the path exist or not
func isPathExist(tree *git.Tree, path string) string {
	_, err := tree.EntryByPath(path)
	if err == nil {
		return path
	}
	if !strings.Contains(path, "/") {
		return ""
	}
	return isPathExist(tree, path[:strings.LastIndex(path, "/")])
}
