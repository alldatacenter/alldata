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
	"container/list"
	"errors"
	"fmt"
	"strconv"
	"strings"
	"time"

	git "github.com/libgit2/git2go/v30"

	"github.com/erda-project/erda/internal/tools/gittar/pkg/gitmodule/tool"
)

const REMOTE_PREFIX = "refs/remotes/"

// getRefCommitID returns the last commit ID string of given reference (branch or tag).
func (repo *Repository) getRefCommitID(name string) (string, error) {
	rawrepo, err := repo.GetRawRepo()
	if err != nil {
		return "", err
	}
	branch, err := rawrepo.LookupBranch(name, git.BranchLocal)
	if err != nil {
		return "", err
	}
	oid := branch.Target()
	return oid.String(), nil
}

// GetBranchCommitID returns last commit ID string of given branch.
func (repo *Repository) GetBranchCommitID(name string) (string, error) {
	return repo.getRefCommitID(name)
}

func (repo *Repository) GetCommit(commitId string) (*Commit, error) {
	var result *Commit
	err := Setting.CommitCache.Get(commitId, &result)
	if err == nil {
		log("Hit cache: %s", commitId)
		return result, nil
	}

	rawrepo, err := repo.GetRawRepo()
	if err != nil {
		return nil, err
	}
	oid, err := git.NewOid(commitId)
	if err != nil {
		return nil, err
	}

	rawCommit, err := rawrepo.LookupCommit(oid)
	if err != nil {
		return nil, err
	}

	commit := NewCommitFromLibgit2(repo, rawCommit)

	Setting.CommitCache.Set(commitId, commit)
	return commit, nil
}

// GetBranchCommit returns the last commit of given branch.
func (repo *Repository) GetBranchCommit(name string) (*Commit, error) {
	commitID, err := repo.GetBranchCommitID(name)
	if err != nil {
		return nil, errors.New("branch [" + name + "] not exist ")
	}
	return repo.GetCommit(commitID)
}

// GetDefaultBranch 获取默认分支
func (repo *Repository) GetDefaultBranch() (string, error) {
	rawrepo, err := repo.GetRawRepo()
	if err != nil {
		return "", err
	}
	reference, err := rawrepo.Head()
	if err != nil {
		if e1 := repo.AutoSetDefaultBranch(); e1 != nil {
			return "", err
		}
		return repo.GetDefaultBranch()
	}
	branch := strings.TrimPrefix(reference.Name(), BRANCH_PREFIX)
	if branch == "" {
		return "", errors.New("reference name is empty")
	}
	return branch, nil
}

func (repo *Repository) GetMergedBranches(baseBranch string) (map[string]int, error) {
	stdout, err := NewCommand("branch", "--merged", baseBranch).RunInDirBytes(repo.DiskPath())
	if err != nil {
		return nil, err
	}
	result := map[string]int{}
	for _, branch := range strings.Split(string(stdout), "\n") {
		result[strings.TrimSpace(branch)] = 1
	}
	result[baseBranch] = 1
	return result, nil
}

// AutoSetDefaultBranch 自动选择一个分支作为默认分支，优先选择 master / develop 分支
func (repo *Repository) AutoSetDefaultBranch() error {
	branches, err := repo.GetBranches()
	if err != nil {
		return err
	}
	if len(branches) == 0 {
		return errors.New("no branch")
	}
	defaultBranch := branches[0]
	if tool.IsKeyInArray(branches, "develop") {
		defaultBranch = "develop"
	}
	if tool.IsKeyInArray(branches, "master") {
		defaultBranch = "master"
	}
	return repo.SetDefaultBranch(defaultBranch)
}

// SetDefaultBranch 设置默认分支
func (repo *Repository) SetDefaultBranch(branch string) error {
	_, err := repo.GetBranchCommit(branch)
	if err != nil {
		return fmt.Errorf("branch not found: %s", branch)
	}
	rawrepo, err := repo.GetRawRepo()
	if err != nil {
		return err
	}
	return rawrepo.SetHead(BRANCH_PREFIX + branch)
}

// GetTagCommit returns the commit of given tag.
func (repo *Repository) GetTagCommit(name string) (*Commit, error) {
	rawrepo, err := repo.GetRawRepo()
	if err != nil {
		return nil, err
	}
	ref, err := rawrepo.References.Lookup(TAG_PREFIX + name)
	if err != nil {
		return nil, err
	}
	object, err := rawrepo.Lookup(ref.Target())
	if err != nil {
		return nil, err
	}

	rawCommit, err := object.AsCommit()
	if err != nil {
		//不是轻量级tag  尝试 附注tag
		rawTag, err := object.AsTag()
		if err != nil {
			return nil, err
		} else {
			commitId := rawTag.TargetId().String()
			return repo.GetCommit(commitId)
		}
	} else {
		//轻量级tag, tag就是commit本身别名
		commit := NewCommitFromLibgit2(repo, rawCommit)
		return commit, nil
	}
}

func (repo *Repository) getCommitByPathWithID(id string, relpath string) (*Commit, error) {
	cacheKey := id + "-" + relpath
	var result *Commit
	err := Setting.PathCommitCache.Get(cacheKey, &result)
	if err == nil {
		return result, nil
	}
	// File Name starts with ':' must be escaped.
	if len(relpath) > 0 && relpath[0] == ':' {
		relpath = `\` + relpath
	}

	logCmd := NewCommand("log", "-1", _PRETTY_LOG_FORMAT, id, "--", relpath)

	//根路径不需要传path参数
	if relpath == "" {
		logCmd = NewCommand("log", "-1", _PRETTY_LOG_FORMAT, id)
	}

	id, err = logCmd.RunInDir(repo.DiskPath())
	if err != nil {
		return nil, err
	}

	commit, err := repo.GetCommit(id)
	if err == nil {
		Setting.PathCommitCache.Set(cacheKey, commit)
	}
	return commit, err
}

// GetCommitByPath returns the last commit of relative path.
func (repo *Repository) GetCommitByPath(relpath string) (*Commit, error) {
	stdout, err := NewCommand("log", "-1", _PRETTY_LOG_FORMAT, "--", relpath).RunInDirBytes(repo.DiskPath())
	if err != nil {
		return nil, err
	}
	commits, err := repo.parsePrettyFormatLogToList(stdout)
	if err != nil {
		return nil, err
	}

	return commits[0], nil
}

func (repo *Repository) CommitsByRangeSize(id string, page, size int) ([]*Commit, error) {
	/*
		commit, err := repo.GetCommitByAny(id)
		if err != nil {
			return nil, err
		}
		rawrepo, err := repo.GetRawRepo()
		if err != nil {
			return nil, err
		}

		walker, err := rawrepo.Walk()

		if err != nil {
			return nil, err
		}

		oid, _ := git.NewOid(commit.ID)
		walker.Push(oid)
		walker.Sorting(git.SortTime)
		commits := []*Commit{}
		startPos := page * size
		stopPos := startPos + size
		hasIterated := 0

		walker.Iterate(func(rawCommit *git.Commit) bool {
			if hasIterated < startPos {
				hasIterated += 1
				return true
			} else {
				hasIterated += 1
				commits = append(commits, NewCommitFromLibgit2(repo, rawCommit))
				return hasIterated < stopPos
			}
		})
		return commits, nil
	*/

	//todo 使用git命令行性能更好
	stdout, err := NewCommand("log", id, "--skip="+strconv.Itoa((page-1)*size),
		"--max-count="+strconv.Itoa(size), _PRETTY_LOG_FORMAT).RunInDirBytes(repo.DiskPath())
	if err != nil {
		return nil, err
	}
	return repo.parsePrettyFormatLogToList(stdout)
}

var DefaultCommitsPageSize = 30

func (repo *Repository) CommitsByRange(revision string, page int) ([]*Commit, error) {
	return repo.CommitsByRangeSize(revision, page, DefaultCommitsPageSize)
}

func (repo *Repository) searchCommits(id string, keyword string) ([]*Commit, error) {
	stdout, err := NewCommand("log", id, "-100", "-i", "--grep="+keyword, _PRETTY_LOG_FORMAT).RunInDirBytes(repo.DiskPath())
	if err != nil {
		return nil, err
	}
	return repo.parsePrettyFormatLogToList(stdout)
}

func (repo *Repository) getFilesChanged(id1 string, id2 string) ([]string, error) {
	stdout, err := NewCommand("diff", "--Name-only", id1, id2).RunInDirBytes(repo.DiskPath())
	if err != nil {
		return nil, err
	}
	return strings.Split(string(stdout), "\n"), nil
}

func (repo *Repository) FileCommitsCount(revision, file string) (int64, error) {
	return commitsCount(repo.DiskPath(), revision, file)
}

func (repo *Repository) CommitsByFileAndRangeSize(revision, file string, searchText string, page, size int) ([]*Commit, error) {
	args := []string{"log", revision, "--skip=" + strconv.Itoa((page-1)*size),
		"--max-count=" + strconv.Itoa(size), _PRETTY_LOG_FORMAT}

	if searchText != "" {
		args = append(args, "--grep="+searchText)
	}

	if file != "" {
		args = append(args, "--", file)
	}

	stdout, err := NewCommand(args...).RunInDirBytes(repo.DiskPath())
	if err != nil {
		return nil, err
	}
	return repo.parsePrettyFormatLogToList(stdout)
}

func (repo *Repository) FilesCountBetween(startCommitID, endCommitID string) (int, error) {
	stdout, err := NewCommand("diff", "--Name-only", startCommitID+"..."+endCommitID).RunInDir(repo.DiskPath())
	if err != nil {
		return 0, err
	}

	return len(strings.Split(stdout, "\n")) - 1, nil
}

// CommitsBetween returns a list that contains commits between [last, before).
func (repo *Repository) CommitsBetweenLimit(last *Commit, before *Commit, skip int, limit int) ([]*Commit, error) {
	if before == nil {
		stdout, err := NewCommand("rev-list", last.ID, "--all", "--skip="+strconv.Itoa(skip), "--max-count="+strconv.Itoa(limit)).RunInDirBytes(repo.DiskPath())
		if err != nil {
			return nil, err
		}
		return repo.parsePrettyFormatLogToList(bytes.TrimSpace(stdout))
	}
	stdout, err := NewCommand("rev-list", before.ID+"..."+last.ID, "--skip="+strconv.Itoa(skip), "--max-count="+strconv.Itoa(limit)).RunInDirBytes(repo.DiskPath())
	if err != nil {
		return nil, err
	}
	return repo.parsePrettyFormatLogToList(bytes.TrimSpace(stdout))
}

// CommitsBetween returns a list that contains commits between [last, before).
func (repo *Repository) CommitsCountBetween(last *Commit, before *Commit) (int, error) {
	if before == nil {
		stdout, err := NewCommand("rev-list", "--count", last.ID, "--all").RunInDirBytes(repo.DiskPath())
		if err != nil {
			return 0, err
		}

		return strconv.Atoi(strings.TrimSpace(string(stdout)))
	}
	stdout, err := NewCommand("rev-list", "--count", before.ID+"..."+last.ID).RunInDirBytes(repo.DiskPath())
	if err != nil {
		return 0, err
	}

	return strconv.Atoi(strings.TrimSpace(string(stdout)))
}

// CommitsBetween returns a list that contains commits between [last, before).
func (repo *Repository) CommitsBetween(last *Commit, before *Commit) ([]*Commit, error) {

	//rawrepo, err := repo.GetRawRepo()
	//if err != nil {
	//	return nil, err
	//}
	//walker, err := rawrepo.Walk()
	//
	//if err != nil {
	//	return nil, err
	//}
	////lastCommit, _ := git.NewOid(last.ID)
	////walker.Push(lastCommit)
	//walker.PushRange(last.ID+".."+before.ID)
	////beforeCommit, _ := git.NewOid(before.ID)
	//
	//commits := []*Commit{}
	//statusCountToAdd := 20
	//err = walker.Iterate(func(commit *git.Commit) bool {
	//	println(commit.Id().String())
	//	if commit.Id().String() == before.ID {
	//		return false
	//	}
	//	commits = append(commits, NewCommitFromLibgit2(repo, commit))
	//	statusCountToAdd -= 1
	//	return true
	//})
	//if err != nil {
	//	return nil, err
	//}
	//return commits, nil

	stdout, err := NewCommand("rev-list", before.ID+"..."+last.ID).RunInDirBytes(repo.DiskPath())
	if err != nil {
		return nil, err
	}
	return repo.parsePrettyFormatLogToList(bytes.TrimSpace(stdout))
}

func (repo *Repository) CommitsBetweenIDs(last, before string) ([]*Commit, error) {
	lastCommit, err := repo.GetCommit(last)
	if err != nil {
		return nil, err
	}
	beforeCommit, err := repo.GetCommit(before)
	if err != nil {
		return nil, err
	}
	return repo.CommitsBetween(lastCommit, beforeCommit)
}

// The limit is depth, not total number of returned commits.
func (repo *Repository) commitsBefore(l *list.List, parent *list.Element, id string, current, limit int) error {
	// Reach the limit
	if limit > 0 && current > limit {
		return nil
	}

	commit, err := repo.GetCommit(id)
	if err != nil {
		return fmt.Errorf("GetCommit: %v", err)
	}

	var e *list.Element
	if parent == nil {
		e = l.PushBack(commit)
	} else {
		var in = parent
		for {
			if in == nil {
				break
			} else if in.Value.(*Commit).ID == (commit.ID) {
				return nil
			} else if in.Next() == nil {
				break
			}

			if in.Value.(*Commit).Committer.When.Equal(commit.Committer.When) {
				break
			}

			if in.Value.(*Commit).Committer.When.After(commit.Committer.When) &&
				in.Next().Value.(*Commit).Committer.When.Before(commit.Committer.When) {
				break
			}

			in = in.Next()
		}

		e = l.InsertAfter(commit, in)
	}

	pr := parent
	if commit.ParentCount() > 1 {
		pr = e
	}

	for i := 0; i < commit.ParentCount(); i++ {
		id, err := commit.ParentID(i)
		if err != nil {
			return err
		}
		err = repo.commitsBefore(l, pr, id, current+1, limit)
		if err != nil {
			return err
		}
	}

	return nil
}

func (repo *Repository) getCommitsBefore(id string) (*list.List, error) {
	l := list.New()
	return l, repo.commitsBefore(l, nil, id, 1, 0)
}

func (repo *Repository) getCommitsBeforeLimit(id string, num int) ([]*Commit, error) {
	l := []*Commit{}
	return l, nil
	//return l, repo.commitsBefore(l, nil, id, 1, num)
}

// CommitsAfterDate returns a list of commits which committed after given date.
// The format of date should be in RFC3339.
func (repo *Repository) CommitsAfterDate(date string) ([]*Commit, error) {
	stdout, err := NewCommand("log", _PRETTY_LOG_FORMAT, "--since="+date).RunInDirBytes(repo.DiskPath())
	if err != nil {
		return nil, err
	}

	return repo.parsePrettyFormatLogToList(stdout)
}

// CommitsCount returns number of total commits of until given revision.
func CommitsCount(repoPath, revision string) (int64, error) {
	return commitsCount(repoPath, revision, "")
}

// GetLatestCommitDate returns the date of latest commit of repository.
// If branch is empty, it returns the latest commit across all branches.
func GetLatestCommitDate(repoPath, branch string) (time.Time, error) {
	cmd := NewCommand("for-each-ref", "--count=1", "--sort=-committerdate", "--format=%(committerdate:iso8601)")
	if len(branch) > 0 {
		cmd.AddArguments("refs/heads/" + branch)
	}
	stdout, err := cmd.RunInDir(repoPath)
	if err != nil {
		return time.Time{}, err
	}

	return time.Parse("2006-01-02 15:04:05 -0700", strings.TrimSpace(stdout))
}
