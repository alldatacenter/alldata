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
	"bytes"
	"context"
	"errors"
	"regexp"
	"strings"

	git "github.com/libgit2/git2go/v30"
)

// Tree represents a flat directory listing.
type Tree struct {
	ID   string      `json:"id"`
	Repo *Repository `json:"-"`

	// parent tree
	ptree *Tree

	entries       Entries
	entriesParsed bool
}

// Predefine []byte variables to avoid runtime allocations.
var (
	escapedSlash = []byte(`\\`)
	regularSlash = []byte(`\`)
	escapedTab   = []byte(`\t`)
	regularTab   = []byte("\t")
)

// UnescapeChars reverses escaped characters.
func UnescapeChars(in []byte) []byte {
	// LEGACY [Go 1.7]: use more expressive bytes.ContainsAny
	if bytes.IndexAny(in, "\\\t") == -1 {
		return in
	}

	out := bytes.Replace(in, escapedSlash, regularSlash, -1)
	out = bytes.Replace(out, escapedTab, regularTab, -1)
	return out
}

// ListEntries returns all entries of current tree.
func (t *Tree) ListEntries(autoExpandDir bool) (Entries, error) {
	if t.entriesParsed {
		return t.entries, nil
	}

	entries, err := t.innerListEntries()
	if err != nil {
		return nil, err
	}

	if autoExpandDir {
		for _, treeEntry := range entries {
			if treeEntry.Type == OBJECT_TREE {
				tree := &Tree{
					ID:   treeEntry.ID,
					Repo: t.Repo,
				}
				for true {
					entries, _ := tree.innerListEntries()
					if len(entries) == 1 && entries[0].Type == OBJECT_TREE {
						treeEntry.Name += "/" + entries[0].Name
						tree = &Tree{
							ID:   entries[0].ID,
							Repo: t.Repo,
						}
					} else {
						break
					}
				}
			}
		}
	}

	t.entries = entries
	return t.entries, err
}

func (t *Tree) Search(depth int64, pattern string, ctx context.Context) (Entries, error) {
	//转换成正则
	regexPattern := strings.Replace(pattern, ".", "\\.", -1)
	regexPattern = strings.Replace(regexPattern, "*", ".*?", -1)
	pathRegex, err := regexp.Compile(regexPattern)
	if err != nil {
		return nil, err
	}

	entries, err := t.innerListEntries()
	if err != nil {
		return nil, err
	}

	queue := []*TreeEntry{}
	result := []*TreeEntry{}
	for _, v := range entries {
		if v.Type == OBJECT_BLOB {
			if pathRegex.MatchString(v.Name) {
				result = append(result, v)
			}
		} else if v.Type == OBJECT_TREE {
			v.path = v.Name
			if pathRegex.MatchString(v.Name) {
				result = append(result, v)
			}
			queue = append(queue, v)
		}
	}
	depth -= 1

	for depth > 0 && len(queue) > 0 {
		newQueue := []*TreeEntry{}
		for _, parentTreeEntry := range queue {
			select {
			case <-ctx.Done():
				return nil, errors.New("search timeout")
			default:
			}
			listEntries, err := parentTreeEntry.ListEntries()
			if err != nil {
				return nil, err
			}
			for _, v := range listEntries {
				if v.Type == OBJECT_BLOB {
					v.Name = parentTreeEntry.path + "/" + v.Name
					if pathRegex.MatchString(v.Name) {
						result = append(result, v)
					}
				} else if v.Type == OBJECT_TREE {
					if parentTreeEntry.path != "" {
						v.path = parentTreeEntry.path + "/" + v.Name
					}
					if pathRegex.MatchString(v.Name) {
						result = append(result, v)
					}
					newQueue = append(newQueue, v)
				}
			}
		}
		depth -= 1
		queue = newQueue
	}

	return result, err
}

func (t *Tree) innerListEntries() (Entries, error) {
	if t.entriesParsed {
		return t.entries, nil
	}

	t.entriesParsed = true
	entries := make([]*TreeEntry, 0, 10)

	rawrepo, err := t.Repo.GetRawRepo()
	if err != nil {
		return nil, err
	}

	oid, _ := git.NewOid(t.ID)
	tree, err := rawrepo.LookupTree(oid)
	if err != nil {
		return nil, err
	}

	tree.Walk(func(s string, entry *git.TreeEntry) int {
		var mode EntryMode
		switch entry.Filemode {
		case git.FilemodeTree:
			mode = ENTRY_MODE_TREE
		case git.FilemodeBlob:
			mode = ENTRY_MODE_BLOB
		case git.FilemodeCommit:
			mode = ENTRY_MODE_COMMIT
		case git.FilemodeBlobExecutable:
			mode = ENTRY_MODE_EXEC
		case git.FilemodeLink:
			mode = ENTRY_MODE_SYMLINK
		}
		treeEntry := new(TreeEntry)
		treeEntry.PtrTree = t
		treeEntry.Type = ObjectType(strings.ToLower(entry.Type.String()))
		treeEntry.Mode = mode
		treeEntry.ID = entry.Id.String()
		treeEntry.Name = entry.Name
		treeEntry.repo = t.Repo
		treeEntry.Size()
		entries = append(entries, treeEntry)
		return 1
	})

	t.entries = entries
	return t.entries, err
}
