// Copyright 2015 The Gogs Authors. All rights reserved.
// Use of this source code is governed by a MIT-style
// license that can be found in the LICENSE file.

//go:build !codeanalysis
// +build !codeanalysis

package gitmodule

import (
	"path"

	git "github.com/libgit2/git2go/v30"
)

// Find the tree object in the repository.
func (repo *Repository) GetTree(idStr string) (*Tree, error) {
	rawRepo, err := repo.GetRawRepo()
	if err != nil {
		return nil, err
	}

	oid, err := git.NewOid(idStr)
	if err != nil {
		return nil, err
	}
	tree, err := rawRepo.LookupTree(oid)
	if err != nil {
		return nil, err
	}
	return &Tree{
		ID:   tree.Id().String(),
		Repo: repo,
	}, nil
}

// Find the tree object in the repository.
func (repo *Repository) GetTreeEntryByPath(idStr string, path string) (*TreeEntry, error) {
	commit, err := repo.GetCommit(idStr)
	if err != nil {
		return nil, err
	}
	rootTreeId := commit.TreeSha

	tree, err := repo.GetTree(rootTreeId)
	if err != nil {
		return nil, err
	}
	return tree.GetTreeEntryByPath(path)
}

func (repo *Repository) FillTreeEntriesCommitInfo(id string, replath string, entries ...*TreeEntry) error {
	if len(entries) == 0 {
		return nil
	}
	maxConcurrency := 5
	revChan := make(chan commitInfo, maxConcurrency)
	doneChan := make(chan error)
	go func() {
		i := 0
		for info := range revChan {
			if info.err != nil {
				doneChan <- info.err
				return
			}
			i++
			if i == len(entries) {
				break
			}
		}
		doneChan <- nil
	}()

	for _, entry := range entries {
		go func(entry *TreeEntry) {
			commit, err := repo.getCommitByPathWithID(id, path.Join(replath, entry.Name))
			if err == nil {
				entry.Commit = commit
			}
			revChan <- commitInfo{
				err: err,
			}
		}(entry)
	}

	if err := <-doneChan; err != nil {
		return err
	}
	return nil
}
