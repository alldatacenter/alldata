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
	"errors"
	"fmt"
	"strings"
)

var (
	ERROR_PATH_NOT_FOUND = errors.New("路径不存在")
)

func (t *Tree) GetTreeEntryByPath(relpath string) (*TreeEntry, error) {
	repo := t.Repo
	tree := t
	if relpath == "" {
		return &TreeEntry{
			ID:      tree.ID,
			Type:    OBJECT_TREE,
			Mode:    ENTRY_MODE_TREE,
			PtrTree: tree,
			repo:    t.Repo,
		}, nil
	}

	pathParts := strings.Split(relpath, "/")
	matchCount := 0
	for _, pathPart := range pathParts {
		entries, _ := tree.ListEntries(false)
		for _, entry := range entries {
			if entry.Name == pathPart {
				matchCount += 1
				if entry.IsDir() {
					tree, _ = repo.GetTree(entry.ID)
					break
				} else {
					//如果刚好最后一级一样,当文件处理,其他当匹配失败
					if matchCount == len(pathParts) {
						entry.repo = t.Repo
						return entry, nil
					} else {
						//中间遇到文件返回错误
						return nil, ERROR_PATH_NOT_FOUND
					}
				}
			}

		}
	}
	if matchCount == len(pathParts) {
		return &TreeEntry{
			ID:      tree.ID,
			PtrTree: tree,
			Type:    OBJECT_TREE,
			Mode:    ENTRY_MODE_TREE,
			repo:    repo,
		}, nil
	} else {
		return nil, fmt.Errorf("路径不存在: %s", relpath)
	}
}

func (t *Tree) GetBlobByPath(relpath string) (*Blob, error) {
	entry, err := t.GetTreeEntryByPath(relpath)
	if err != nil {
		return nil, err
	}

	if !entry.IsDir() {
		return entry.Blob(), nil
	}

	return nil, ErrNotExist{"", relpath}
}
