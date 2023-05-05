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
	"errors"

	git "github.com/libgit2/git2go/v30"
)

type MergeStatusInfo struct {
	HasConflict bool   `json:"hasConflict"`
	IsMerged    bool   `json:"isMerged"`
	HasError    bool   `json:"hasError"`
	ErrorMsg    string `json:"errorMsg"`
}

type MergeInfo struct {
	OurBranch   string
	TheirBranch string
	OurCommit   *Commit
	TheirCommit *Commit
	BaseCommit  *Commit
	BaseTree    *git.Tree
	OurTree     *git.Tree
	TheirTree   *git.Tree
}

func (repo *Repository) GetMergeBase(last *Commit, before *Commit) (*Commit, error) {
	rawRepo, err := repo.GetRawRepo()

	if err != nil {
		return nil, err
	}
	if before == nil {
		return nil, errors.New("beforeCommit is nil")
	}
	oidLast, err := git.NewOid(last.ID)
	if err != nil {
		return nil, err
	}
	oidBefore, err := git.NewOid(before.ID)
	if err != nil {
		return nil, err
	}
	baseOid, err := rawRepo.MergeBase(oidBefore, oidLast)
	if err != nil {
		return nil, errors.New("2个分支不是同源")
	}
	return repo.GetCommit(baseOid.String())
}

func (repo *Repository) getMergeInfo(ourBranch string, theirBranch string) (*MergeInfo, error) {
	ourCommit, err := repo.GetBranchCommit(ourBranch)
	if err != nil {
		return nil, err
	}
	theirCommit, err := repo.GetBranchCommit(theirBranch)
	if err != nil {
		return nil, err
	}

	rawRepo, err := repo.GetRawRepo()
	if err != nil {
		return nil, err
	}

	baseCommit, err := repo.GetMergeBase(ourCommit, theirCommit)

	if err != nil {
		return nil, err
	}

	oidTheir, err := git.NewOid(theirCommit.TreeSha)
	if err != nil {
		return nil, err
	}
	treeTheir, err := rawRepo.LookupTree(oidTheir)
	if err != nil {
		return nil, err
	}

	oidOur, err := git.NewOid(ourCommit.TreeSha)
	if err != nil {
		return nil, err
	}
	treeOur, err := rawRepo.LookupTree(oidOur)
	if err != nil {
		return nil, err
	}

	oidBase, err := git.NewOid(baseCommit.TreeSha)
	if err != nil {
		return nil, err
	}
	treeBase, err := rawRepo.LookupTree(oidBase)
	if err != nil {
		return nil, err
	}

	return &MergeInfo{
		OurBranch:   ourBranch,
		TheirBranch: theirBranch,
		OurCommit:   ourCommit,
		TheirCommit: theirCommit,
		BaseCommit:  baseCommit,
		BaseTree:    treeBase,
		OurTree:     treeOur,
		TheirTree:   treeTheir,
	}, nil

}
func (repo *Repository) GetMergeStatus(ourBranch string, theirBranch string) (*MergeStatusInfo, error) {

	info, err := repo.getMergeInfo(ourBranch, theirBranch)
	if err != nil {
		return &MergeStatusInfo{
			HasError: true,
			ErrorMsg: err.Error(),
		}, nil
	}

	result := &MergeStatusInfo{
		HasConflict: false,
	}
	//没有commits差异 认已经合并
	commitsCount, err := repo.CommitsCountBetween(info.OurCommit, info.BaseCommit)
	if err != nil {
		return nil, err
	}
	if commitsCount == 0 {
		result.IsMerged = true
		return result, nil
	}

	rawRepo, err := repo.GetRawRepo()
	if err != nil {
		return nil, err
	}

	options, _ := git.DefaultMergeOptions()
	index, err := rawRepo.MergeTrees(info.BaseTree, info.OurTree, info.TheirTree, &options)
	if err != nil {
		return nil, err
	}
	result.HasConflict = index.HasConflicts()

	return result, nil
}

func (repo *Repository) Merge(ourBranch string, theirBranch string, signature *Signature, message string) (*Commit, error) {

	info, err := repo.getMergeInfo(ourBranch, theirBranch)
	if err != nil {
		return nil, err
	}

	rawRepo, err := repo.GetRawRepo()
	if err != nil {
		return nil, err
	}

	options, err := git.DefaultMergeOptions()
	if err != nil {
		return nil, err
	}
	index, err := rawRepo.MergeTrees(info.BaseTree, info.OurTree, info.TheirTree, &options)

	if err != nil {
		return nil, err
	}

	if index.HasConflicts() {
		return nil, errors.New("has conflict")
	}
	newTreeOid, err := index.WriteTreeTo(rawRepo)
	if err != nil {
		return nil, err
	}

	newTree, err := rawRepo.LookupTree(newTreeOid)
	if err != nil {
		return nil, err
	}

	sig := &git.Signature{
		Name:  signature.Name,
		Email: signature.Email,
		When:  signature.When,
	}

	parentOid, err := git.NewOid(info.TheirCommit.ID)
	if err != nil {
		return nil, err
	}
	parentCommit, err := rawRepo.LookupCommit(parentOid)
	if err != nil {
		return nil, err
	}
	parentOid2, err := git.NewOid(info.OurCommit.ID)
	if err != nil {
		return nil, err
	}
	parentCommit2, err := rawRepo.LookupCommit(parentOid2)

	if err != nil {
		return nil, err
	}
	newOid, err := rawRepo.CreateCommit(BRANCH_PREFIX+theirBranch, sig, sig, message, newTree, parentCommit, parentCommit2)

	if err != nil {
		return nil, err
	}
	return repo.GetCommit(newOid.String())
}
