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

// maintainer: 陈忠润

package bundle

import (
	"encoding/base64"
	"encoding/json"
	"net/url"
	"path/filepath"
	"strconv"
	"strings"

	"github.com/pkg/errors"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle/apierrors"
)

// GittarFileTree 用以描述 gittar 文件路径
type GittarFileTree struct {
	url.Values
}

func NewGittarFileTree(inode string) (*GittarFileTree, error) {
	if inode == "" {
		return &GittarFileTree{Values: make(url.Values, 0)}, nil
	}
	inodeRaw, err := base64.StdEncoding.DecodeString(inode)
	if err != nil {
		return nil, err
	}
	values, err := url.ParseQuery(string(inodeRaw))
	if err != nil {
		return nil, err
	}
	return &GittarFileTree{Values: values}, nil
}

func (t *GittarFileTree) Clone() *GittarFileTree {
	if t == nil {
		newT, _ := NewGittarFileTree("")
		return newT
	}
	var c = GittarFileTree{Values: make(url.Values, 0)}
	for k, v := range t.Values {
		c.Values[k] = v
	}
	return &c
}

func (t *GittarFileTree) ProjectID() string {
	return t.Values.Get("P")
}

func (t *GittarFileTree) ProjectName() string {
	return t.Values.Get("p")
}

func (t *GittarFileTree) SetProjectIDName(id, name string) *GittarFileTree {
	t.Values.Set("P", id)
	t.Values.Set("p", name)
	return t
}

func (t *GittarFileTree) ApplicationID() string {
	return t.Values.Get("A")
}

func (t *GittarFileTree) ApplicationName() string {
	return t.Values.Get("a")
}

func (t *GittarFileTree) SetApplicationIDName(id, name string) *GittarFileTree {
	t.Values.Set("A", id)
	t.Values.Set("a", name)
	return t
}

func (t *GittarFileTree) BranchName() string {
	return t.Values.Get("b")
}

func (t *GittarFileTree) SetBranchName(name string) *GittarFileTree {
	t.Values.Set("b", name)
	return t
}

func (t *GittarFileTree) PathFromRepoRoot() string {
	return t.Values.Get("d")
}

func (t *GittarFileTree) SetPathFromRepoRoot(name string) *GittarFileTree {
	t.Values.Set("d", name)
	return t
}

func (t *GittarFileTree) DeletePathFromRepoRoot() *GittarFileTree {
	delete(t.Values, "d")
	return t
}

// /wb/{projectName}/{appName}/blob/{branch}/{pathFromRepoRoot}
func (t *GittarFileTree) BlobPath() string {
	return filepath.Join("/wb", t.ProjectName(), t.ApplicationName(), "blob", t.BranchName(), t.PathFromRepoRoot())
}

// /wb/{projectName}/{appName}/tree/{branch}/{pathFromRepoRoot}
func (t *GittarFileTree) TreePath() string {
	return filepath.Join("/wb", t.ProjectName(), t.ApplicationName(), "tree", t.BranchName(), t.PathFromRepoRoot())
}

func (t *GittarFileTree) BranchesPath() string {
	return filepath.Join("/wb", t.ProjectName(), t.ApplicationName(), "branches")
}

func (t *GittarFileTree) RepoPath() string {
	return filepath.Join("/wb", t.ProjectName(), t.ApplicationName())
}

func (t *GittarFileTree) Inode() string {
	return base64.StdEncoding.EncodeToString([]byte(t.Values.Encode()))
}

func (b *Bundle) GetGittarTreeNodeInfo(treePath, orgID, userID string) (*apistructs.GittarTreeRspData, error) {
	host, err := b.urls.Gittar()
	if err != nil {
		return nil, err
	}

	var response apistructs.BaseResponse

	resp, err := b.hc.Get(host).
		Header("Org-ID", orgID).
		Header("User-ID", userID).
		Path(treePath).Do().JSON(&response)
	if err != nil {
		return nil, errors.Wrapf(err, "repo: %s", treePath)
	}
	if !resp.IsOK() {
		return nil, errors.New("failed to GetGittarTreeNodeInfo")
	}

	var data apistructs.GittarTreeRspData
	if err = json.Unmarshal(response.Data, &data); err != nil {
		return nil, err
	}

	return &data, nil
}

func (b *Bundle) GetGittarBlobNodeInfo(blobPath string, orgID, userID string) (*apistructs.GittarBlobRspData, error) {
	host, err := b.urls.Gittar()
	if err != nil {
		return nil, err
	}

	var response apistructs.BaseResponse
	resp, err := b.hc.Get(host).
		Header("Org-ID", orgID).
		Header("User-ID", userID).
		Path(blobPath).Do().JSON(&response)
	if err != nil {
		return nil, errors.Wrapf(err, "repo: %s", blobPath)
	}
	if !resp.IsOK() {
		return nil, errors.Errorf("failed to GetGittarBlobNodeInfo: %v", response.Err)
	}

	if !response.Success {
		return nil, errors.Errorf("failed to GetGittarBlobNodeInfo: %v", response.Err)
	}

	var data apistructs.GittarBlobRspData
	if err = json.Unmarshal(response.Data, &data); err != nil {
		return nil, err
	}

	return &data, nil
}

// CreateGittarCommit 创建commit
func (b *Bundle) CreateGittarCommitV3(orgID uint64, userID, repo string,
	commit *apistructs.GittarCreateCommitRequest) (*apistructs.GittarCreateCommitResponse, error) {

	if commit == nil {
		return nil, errors.New("no commit")
	}

	repo = "/" + strings.TrimLeft(repo, "/")
	path_ := repo + "/commits"

	hc := b.hc
	host, err := b.urls.Gittar()
	if err != nil {
		return nil, err
	}

	var createCommitResp apistructs.GittarCreateCommitResponse
	resp, err := hc.Post(host).
		Path(path_).
		Header("Org-ID", strconv.FormatUint(orgID, 10)).
		Header("User-ID", userID).
		JSONBody(commit).
		Do().JSON(&createCommitResp)
	if err != nil {
		return nil, apierrors.ErrInvoke.InternalError(err)
	}
	if !resp.IsOK() || !createCommitResp.Success {
		return nil, toAPIError(resp.StatusCode(), createCommitResp.Error)
	}
	return &createCommitResp, nil
}
