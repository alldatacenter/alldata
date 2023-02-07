package fs

import (
	"github.com/apex/log"
	"github.com/mitchellh/go-homedir"
	"path"
)

func init() {
	rootDir, err := homedir.Dir()
	if err != nil {
		log.Warnf("cannot find home directory: %v", err)
		return
	}
	DefaultWorkspacePath = path.Join(rootDir, "crawlab_workspace")
	DefaultRepoPath = path.Join(rootDir, "crawlab_repo")
}

const DefaultFsPath = "/fs"

var DefaultWorkspacePath string
var DefaultRepoPath string
