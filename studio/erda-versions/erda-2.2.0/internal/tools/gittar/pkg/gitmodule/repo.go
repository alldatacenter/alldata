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
	"errors"
	"fmt"
	"os"
	"os/exec"
	"path"
	"path/filepath"
	"strings"
	"time"

	git "github.com/libgit2/git2go/v30"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/pkg/diceworkspace"
)

// Repository represents a Git repository.
type Repository struct {
	ID              int64  `json:"id"`
	ProjectId       int64  `json:"project_id"`
	ProjectName     string `json:"project_name"`
	ApplicationId   int64  `json:"application_id"`
	ApplicationName string `json:"application_name"`
	OrgId           int64  `json:"org_id"`
	OrgName         string `json:"org_name"`
	Url             string `json:"url"`
	Path            string `json:"path"` //相对路径
	Bundle          *bundle.Bundle
	branchRules     []*apistructs.BranchRule

	Size       int64   `json:"-"`
	RootPath   string  `json:"-"`
	RefName    string  `json:"-"` //需要调用ParseRefAndTreePath才能得到
	TreePath   string  `json:"-"` //需要调用ParseRefAndTreePath才能得到
	RefType    string  `json:"-"`
	Commit     *Commit `json:"-"`
	tree       *Tree   `json:"-"` //只有RefType是tree才有值
	IsLocked   bool    `json:"-"`
	IsExternal bool    // 是否是外置仓库
}

const (
	REF_TYPE_BRANCH = "branch"
	REF_TYPE_TAG    = "tag"
	REF_TYPE_COMMIT = "commit"
	REF_TYPE_TREE   = "tree"
)

func (repo *Repository) IsProtectBranch(branch string) bool {
	// repo是http请求级别的实例，一个请求中不重复更新规则
	if repo.branchRules == nil {
		rules, err := repo.Bundle.GetAppBranchRules(uint64(repo.ApplicationId))
		if err != nil {
			return false
		}
		repo.branchRules = rules
	}
	gitReference := diceworkspace.GetValidBranchByGitReference(branch, repo.branchRules)
	return gitReference.IsProtect
}

func (repo *Repository) IsProtectBranchWithRules(branch string, rules []*apistructs.BranchRule) bool {
	gitReference := diceworkspace.GetValidBranchByGitReference(branch, rules)
	return gitReference.IsProtect
}

func (repo *Repository) ParseRefAndTreePath(path string) error {
	hasRefMatched := false
	var refName string
	path = strings.Trim(path, "/")
	parts := strings.Split(path, "/")
	//探测tag branch
	for i := range parts {
		refName = strings.Join(parts[:i+1], "/")

		if repo.IsBranchExist(refName) ||
			repo.IsTagExist(refName) {
			if i < len(parts)-1 {
				repo.TreePath = strings.Join(parts[i+1:], "/")
			}
			hasRefMatched = true
			break
		}
	}

	if !hasRefMatched && len(parts[0]) == 40 {
		refName = parts[0]
		repo.TreePath = strings.Join(parts[1:], "/")
	}

	var err error
	if repo.IsBranchExist(refName) {
		repo.Commit, err = repo.GetBranchCommit(refName)
		repo.RefName = BRANCH_PREFIX + refName
		repo.RefType = REF_TYPE_BRANCH
		if err != nil {
			return err
		}
	} else if repo.IsTagExist(refName) {
		repo.Commit, err = repo.GetTagCommit(refName)
		repo.RefName = TAG_PREFIX + refName
		repo.RefType = REF_TYPE_TAG
		if err != nil {
			return err
		}
	} else if len(refName) >= 6 {
		//commit id 和tree id都尝试解析
		repo.Commit, err = repo.GetCommit(refName)
		if err == nil {
			repo.RefName = "sha/" + refName
			repo.RefType = REF_TYPE_COMMIT
			return nil
		}
		//尝试 treeId
		tree, err := repo.GetTree(refName)
		if err != nil {
			return fmt.Errorf("无效分支 : %s ", refName)
		}
		repo.tree = tree
		repo.RefName = "tree_sha/" + refName
		repo.RefType = REF_TYPE_TREE
		return nil
	} else {
		return fmt.Errorf("branch or tag not exist: %s", refName)
	}
	return nil
}

func (repo *Repository) GetParsedTreeEntry() (*TreeEntry, error) {
	if repo.RefType == REF_TYPE_TREE {
		return repo.tree.GetTreeEntryByPath(repo.TreePath)
	} else {
		return repo.GetTreeEntryByPath(repo.Commit.ID, repo.TreePath)
	}

}
func (repo *Repository) GetCommitByAny(ref string) (*Commit, error) {
	var err error
	var commit *Commit
	commit, err = repo.GetBranchCommit(ref)
	if err == nil {
		return commit, nil
	}
	commit, err = repo.GetTagCommit(ref)
	if err == nil {
		return commit, nil
	}
	commit, err = repo.GetCommit(ref)
	if err == nil {
		return commit, nil
	}
	return nil, fmt.Errorf("unknown ref %s", ref)
}

func (repo *Repository) GetRepoStats(path string) (map[string]interface{}, error) {
	if path == "" {
		path = "/"
	}
	tags, _ := repo.GetTags()
	branches, _ := repo.GetBranches()

	var commit *Commit
	var err error
	defaultBranch, _ := repo.GetDefaultBranch()
	if path != "/" {
		err := repo.ParseRefAndTreePath(path)
		if err != nil {
			return nil, err
		}
		commit = repo.Commit
	} else {
		if defaultBranch != "" {
			commit, err = repo.GetBranchCommit(defaultBranch)
			if err != nil {
				return nil, err
			}
			repo.RefName = BRANCH_PREFIX + defaultBranch
		} else {
			//没有分支判断为空仓库
			result := map[string]interface{}{
				"commitsCount":     0,
				"contributorCount": 0,
				"tags":             tags,
				"branches":         branches,
				"defaultBranch":    defaultBranch,
				"empty":            true,
				"commitId":         "",
			}
			return result, nil
		}
	}
	rawrepo, err := repo.GetRawRepo()
	if err != nil {
		return nil, err
	}

	key := repo.FullName() + "-" + commit.ID

	refName := repo.RefName
	if refName == "" {
		refName = "sha/" + commit.ID
	}

	var result map[string]interface{}
	err = Setting.RepoStatsCache.Get(key, &result)
	if err == nil {
		//以下信息不使用缓存
		result["branches"] = branches
		result["tags"] = tags
		result["defaultBranch"] = defaultBranch
		result["commitId"] = commit.ID
		result["refName"] = refName
		return result, nil
	}

	walker, err := rawrepo.Walk()
	if err != nil {
		return nil, err
	}

	oid, _ := git.NewOid(commit.ID)
	err = walker.Push(oid)
	if err != nil {
		return nil, err
	}

	var repoCommitsCount int
	contributors := map[string]map[string]interface{}{}
	if defaultBranch != "" {
		walker.Iterate(func(commit *git.Commit) bool {
			repoCommitsCount++

			author := commit.Author()
			if contributors[author.Email] == nil {
				stats := map[string]interface{}{
					"name":         author.Name,
					"commitsCount": 1,
				}
				contributors[author.Email] = stats
			} else {
				stats := contributors[author.Email]
				count := stats["commitsCount"].(int)
				count++
				stats["commitsCount"] = count
			}
			//if commit.ParentCount() == 0 {
			//  repoCreatedAt = commit.Author().When
			//  return false
			//}
			return true
		})
	}

	result = map[string]interface{}{
		"commitsCount":     repoCommitsCount,
		"contributorCount": len(contributors),
		"tags":             tags,
		"branches":         branches,
		"defaultBranch":    defaultBranch,
		"empty":            false,
		"refName":          refName,
		"commitId":         commit.ID,
	}

	cacheStats := map[string]interface{}{
		"commitsCount":     repoCommitsCount,
		"contributorCount": len(contributors),
		"empty":            false,
	}
	Setting.RepoStatsCache.Set(key, cacheStats)
	return result, nil

}

func (repo *Repository) Root() string {
	return repo.RootPath
}

func (repo *Repository) DiskPath() string {
	return path.Join(repo.Root(), repo.Path)
}

func (repo *Repository) FullName() string {
	return repo.Path
}

const _PRETTY_LOG_FORMAT = `--pretty=format:%H`

func (repo *Repository) parsePrettyFormatLogToList(logs []byte) ([]*Commit, error) {
	commits := []*Commit{}
	if len(logs) == 0 {
		return commits, nil
	}

	parts := bytes.Split(logs, []byte{'\n'})

	for _, commitId := range parts {
		commit, err := repo.GetCommit(string(commitId))
		if err != nil {
			return nil, err
		}
		commits = append(commits, commit)
	}

	return commits, nil
}

func (repo *Repository) GetRawRepo() (*git.Repository, error) {
	return git.OpenRepository(repo.DiskPath())
}

type NetworkOptions struct {
	URL     string
	Timeout time.Duration
}

// InitRepository initializes a new Git repository.
func InitRepository(repoPath string, bare bool) error {
	if _, err := os.Stat(repoPath); os.IsNotExist(err) {
		logrus.Infof("init repo %s", repoPath)
		os.MkdirAll(repoPath, 0755)
		_, err := git.InitRepository(repoPath, true)
		logrus.Printf("git repository %s is created!", repoPath)
		if err != nil {
			return err
		}
	}
	return nil
}

// InitExternalRepository 初始化外部托管仓库
func InitExternalRepository(repoPath string, url string, username string, password string) error {
	if _, err := os.Stat(repoPath); os.IsNotExist(err) {
		logrus.Infof("init repo %s", repoPath)
		os.MkdirAll(repoPath, 0755)
		urlWithBasicAuth, err := GetUrlWithBasicAuth(url, username, password)
		if err != nil {
			return err
		}
		cmd := exec.Command("git", "clone", "--mirror", urlWithBasicAuth, repoPath)
		output, err := cmd.CombinedOutput()
		if err != nil {
			return fmt.Errorf("err:%s output:%s", err, RemoveAuthInfo(string(output), urlWithBasicAuth))
		}
	}
	return nil
}

// UpdateExternalRepository 更新远程仓库配置
func UpdateExternalRepository(repoPath string, url string, username string, password string) error {
	urlWithBasicAuth, err := GetUrlWithBasicAuth(url, username, password)
	if err != nil {
		return err
	}
	cmd := exec.Command("git", "remote", "set-url", "origin", urlWithBasicAuth)
	cmd.Dir = repoPath
	output, err := cmd.CombinedOutput()
	if err != nil {
		return fmt.Errorf("err:%s output:%s", err, RemoveAuthInfo(string(output), urlWithBasicAuth))
	}
	return nil
}

// SyncExternalRepository 同步外部仓库
func SyncExternalRepository(repoPath string) error {
	cmd := exec.Command("git", "remote", "update", "--prune")
	cmd.Dir = repoPath
	output, err := cmd.CombinedOutput()
	if err != nil {
		return fmt.Errorf("err:%s output:%s", err, string(output))
	}
	return nil
}

// PushExternalRepository 推送到外部仓库
func PushExternalRepository(repoPath string) error {
	mirrorRepoUrl := GetMirrorRepoUrl(repoPath)

	cmd := exec.Command("git", "push", "origin")
	cmd.Dir = repoPath
	output, err := cmd.CombinedOutput()
	if err != nil {
		return fmt.Errorf("err:%s output:%s", err, RemoveAuthInfo(string(output), mirrorRepoUrl))
	}
	logrus.Info("PushExternalRepository: ", RemoveAuthInfo(string(output), mirrorRepoUrl))

	return nil
}

func OpenRepository(rootPath string, path string) (*Repository, error) {
	repository := &Repository{
		Path:     path,
		RootPath: rootPath,
	}
	repoPath, err := filepath.Abs(repository.DiskPath())
	if err != nil {
		return nil, err
	} else if !isDir(repoPath) {
		return nil, errors.New("repo path not exist")
	}
	return repository, nil
}

func OpenRepositoryWithInit(rootPath string, repoPath string) (*Repository, error) {
	err := InitRepository(path.Join(rootPath, repoPath), true)
	if err != nil {
		return nil, err
	}
	return OpenRepository(rootPath, repoPath)
}

type CloneRepoOptions struct {
	Mirror  bool
	Bare    bool
	Quiet   bool
	Branch  string
	Timeout time.Duration
}

type CheckoutOptions struct {
	Branch    string
	OldBranch string
	Timeout   time.Duration
}

// Checkout checkouts a branch
func Checkout(repoPath string, opts CheckoutOptions) error {
	cmd := NewCommand("checkout")
	if len(opts.OldBranch) > 0 {
		cmd.AddArguments("-b")
	}

	cmd.AddArguments(opts.Branch)

	if len(opts.OldBranch) > 0 {
		cmd.AddArguments(opts.OldBranch)
	}
	if opts.Timeout <= 0 {
		opts.Timeout = -1
	}
	_, err := cmd.RunInDirTimeout(opts.Timeout, repoPath)
	return err
}

// MoveFile moves a file to another file or directory.
func MoveFile(repoPath, oldTreeName, newTreeName string) error {
	_, err := NewCommand("mv").AddArguments(oldTreeName, newTreeName).RunInDir(repoPath)
	return err
}

// CountObject represents disk usage report of Git repository.
type CountObject struct {
	Count         int64
	Size          int64
	InPack        int64
	Packs         int64
	SizePack      int64
	PrunePackable int64
	Garbage       int64
	SizeGarbage   int64
}

const (
	_STAT_COUNT          = "count: "
	_STAT_SIZE           = "size: "
	_STAT_IN_PACK        = "in-pack: "
	_STAT_PACKS          = "packs: "
	_STAT_SIZE_PACK      = "size-pack: "
	_STAT_PRUNE_PACKABLE = "prune-packable: "
	_STAT_GARBAGE        = "garbage: "
	_STAT_SIZE_GARBAGE   = "size-garbage: "
)

// CalcRepoSize returns disk usage report of repository in given path.
func (repo *Repository) CalcRepoSize() (int64, error) {

	cmd := NewCommand("count-objects", "-v")
	repoPath := repo.DiskPath()
	stdout, err := cmd.RunInDir(repoPath)
	if err != nil {
		return 0, err
	}

	countObject := new(CountObject)
	for _, line := range strings.Split(stdout, "\n") {
		switch {
		case strings.HasPrefix(line, _STAT_COUNT):
			countObject.Count = StrTo(line[7:]).MustInt64()
		case strings.HasPrefix(line, _STAT_SIZE):
			countObject.Size = StrTo(line[6:]).MustInt64() * 1024
		case strings.HasPrefix(line, _STAT_IN_PACK):
			countObject.InPack = StrTo(line[9:]).MustInt64()
		case strings.HasPrefix(line, _STAT_PACKS):
			countObject.Packs = StrTo(line[7:]).MustInt64()
		case strings.HasPrefix(line, _STAT_SIZE_PACK):
			countObject.SizePack = StrTo(line[11:]).MustInt64() * 1024
		case strings.HasPrefix(line, _STAT_PRUNE_PACKABLE):
			countObject.PrunePackable = StrTo(line[16:]).MustInt64()
		case strings.HasPrefix(line, _STAT_GARBAGE):
			countObject.Garbage = StrTo(line[9:]).MustInt64()
		case strings.HasPrefix(line, _STAT_SIZE_GARBAGE):
			countObject.SizeGarbage = StrTo(line[14:]).MustInt64() * 1024
		}
	}
	size := countObject.Size + countObject.SizeGarbage + countObject.SizePack
	return size, nil
}

// InfoPacksPath for Repository
func (r *Repository) InfoPacksPath() string {
	return path.Join(r.DiskPath(), "info", "packs")
}

// LooseObjectPath for Repository
func (r *Repository) LooseObjectPath(prefix string, suffix string) string {
	return path.Join(r.DiskPath(), "objects", prefix, suffix)
}

// PackIdxPath for Repository
func (r *Repository) PackIdxPath(pack string) string {
	return path.Join(r.DiskPath(), "pack", pack)
}
