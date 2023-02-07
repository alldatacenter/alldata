package fs

import (
	"fmt"
	"github.com/crawlab-team/crawlab-core/errors"
	"github.com/crawlab-team/crawlab-core/fs"
	"github.com/crawlab-team/crawlab-core/interfaces"
	"github.com/crawlab-team/go-trace"
	"github.com/spf13/viper"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"path/filepath"
)

type Service struct {
	// settings
	cfgPath           string
	fsPathBase        string
	workspacePathBase string
	repoPathBase      string

	// dependencies
	fsSvc interfaces.FsService

	// internals
	tid primitive.ObjectID // models.Task Id
	sid primitive.ObjectID // models.Spider Id
}

func (svc *Service) Init() (err error) {
	// fs service
	var fsOpts []fs.Option
	if svc.cfgPath != "" {
		fsOpts = append(fsOpts, fs.WithConfigPath(svc.cfgPath))
	}
	if svc.GetFsPath() != "" {
		fsOpts = append(fsOpts, fs.WithFsPath(svc.GetFsPath()))
	}
	if svc.GetWorkspacePath() != "" {
		fsOpts = append(fsOpts, fs.WithWorkspacePath(svc.GetWorkspacePath()))
	}
	if svc.repoPathBase != "" {
		fsOpts = append(fsOpts, fs.WithRepoPath(svc.GetRepoPath()))
	}

	// fs service
	svc.fsSvc, err = fs.NewFsService(fsOpts...)
	if err != nil {
		return err
	}

	return nil
}

func (svc *Service) GetFsService() (fsSvc interfaces.FsService) {
	return svc.fsSvc
}

func (svc *Service) SetFsPathBase(path string) {
	svc.fsPathBase = path
}

func (svc *Service) SetRepoPathBase(path string) {
	svc.repoPathBase = path
}

func (svc *Service) SetWorkspacePathBase(path string) {
	svc.workspacePathBase = path
}

func (svc *Service) GetConfigPath() (path string) {
	return svc.cfgPath
}

func (svc *Service) SetConfigPath(path string) {
	svc.cfgPath = path
}

func (svc *Service) GetFsPath() (res string) {
	return fmt.Sprintf("%s/%s", svc.fsPathBase, svc.sid.Hex())
}

func (svc *Service) GetWorkspacePath() (res string) {
	return filepath.Join(svc.workspacePathBase, svc.sid.Hex(), svc.tid.Hex())
}

func (svc *Service) GetRepoPath() (res string) {
	return fmt.Sprintf("%s/%s", svc.repoPathBase, svc.sid.Hex())
}

func NewTaskFsService(taskId, spiderId primitive.ObjectID, opts ...Option) (svc2 interfaces.TaskFsService, err error) {
	// service
	svc := &Service{
		fsPathBase:        fs.DefaultFsPath,
		workspacePathBase: fs.DefaultWorkspacePath,
		repoPathBase:      fs.DefaultRepoPath,
		tid:               taskId,
		sid:               spiderId,
	}

	// validate
	if svc.tid.IsZero() || svc.sid.IsZero() {
		return nil, trace.TraceError(errors.ErrorTaskMissingRequiredOption)
	}

	// workspace path
	if viper.GetString("fs.workspace.path") != "" {
		svc.workspacePathBase = viper.GetString("fs.workspace.path")
	}

	// repo path
	if viper.GetString("fs.repo.path") != "" {
		svc.repoPathBase = viper.GetString("fs.repo.path")
	}

	// workspace path
	if viper.GetString("fs.workspace.path") != "" {
		svc.workspacePathBase = viper.GetString("fs.workspace.path")
	}

	// repo path
	if viper.GetString("fs.repo.path") != "" {
		svc.repoPathBase = viper.GetString("fs.repo.path")
	}

	// apply options
	for _, opt := range opts {
		opt(svc)
	}

	// init
	if err := svc.Init(); err != nil {
		return nil, err
	}

	return svc, nil
}
