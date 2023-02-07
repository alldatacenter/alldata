package fs

import (
	"fmt"
	"github.com/crawlab-team/crawlab-core/errors"
	"github.com/crawlab-team/crawlab-core/fs"
	"github.com/crawlab-team/crawlab-core/interfaces"
	"github.com/crawlab-team/go-trace"
	"github.com/spf13/viper"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"os"
	"path"
	"sync"
)

// Service implementation of interfaces.SpiderFsService
// It is a wrapper of interfaces.FsService that manages a spider's fs related functions
type Service struct {
	// settings
	cfgPath           string
	fsPathBase        string
	workspacePathBase string
	repoPathBase      string

	// dependencies
	fsSvc interfaces.FsService

	// internals
	id primitive.ObjectID
}

func (svc *Service) Init() (err error) {
	// workspace
	if _, err := os.Stat(svc.GetWorkspacePath()); err != nil {
		if err := os.MkdirAll(svc.GetWorkspacePath(), os.FileMode(0766)); err != nil {
			return trace.TraceError(err)
		}
	}

	return nil
}

func (svc *Service) GetConfigPath() string {
	return svc.cfgPath
}

func (svc *Service) SetConfigPath(path string) {
	svc.cfgPath = path
}

func (svc *Service) SetId(id primitive.ObjectID) {
	svc.id = id
}

func (svc *Service) List(path string) (files []interfaces.FsFileInfo, err error) {
	return svc.fsSvc.List(path)
}

func (svc *Service) GetFile(path string) (data []byte, err error) {
	return svc.fsSvc.GetFile(path)
}

func (svc *Service) GetFileInfo(path string) (file interfaces.FsFileInfo, err error) {
	return svc.fsSvc.GetFileInfo(path)
}

func (svc *Service) Exists(path string) (ok bool) {
	_, err := svc.fsSvc.GetFileInfo(path)
	if err != nil {
		return false
	}
	return true
}

func (svc *Service) Save(path string, data []byte) (err error) {
	return svc.fsSvc.Save(path, data)
}

func (svc *Service) Rename(path, newPath string) (err error) {
	return svc.fsSvc.Rename(path, newPath)
}

func (svc *Service) Delete(path string) (err error) {
	return svc.fsSvc.Delete(path)
}

func (svc *Service) Copy(path, newPath string) (err error) {
	return svc.fsSvc.Copy(path, newPath)
}

func (svc *Service) Commit(msg string) (err error) {
	panic("implement me")
}

func (svc *Service) GetFsPath() (res string) {
	return fmt.Sprintf("%s/%s", svc.fsPathBase, svc.id.Hex())
}

func (svc *Service) GetWorkspacePath() (res string) {
	return fmt.Sprintf("%s/%s", svc.workspacePathBase, svc.id.Hex())
}

func (svc *Service) GetRepoPath() (res string) {
	return fmt.Sprintf("%s/%s", svc.repoPathBase, svc.id.Hex())
}

func (svc *Service) SetFsPathBase(path string) {
	svc.fsPathBase = path
}

func (svc *Service) SetWorkspacePathBase(path string) {
	svc.workspacePathBase = path
}

func (svc *Service) SetRepoPathBase(path string) {
	svc.repoPathBase = path
}

func (svc *Service) GetFsService() (fsSvc interfaces.FsService) {
	return svc.fsSvc
}

func (svc *Service) getFsPath(p string) (res string) {
	return path.Join(svc.GetFsPath(), p)
}

func NewSpiderFsService(id primitive.ObjectID, opts ...Option) (svc2 interfaces.SpiderFsService, err error) {
	// service
	svc := &Service{
		fsPathBase:        fs.DefaultFsPath,
		workspacePathBase: fs.DefaultWorkspacePath,
		repoPathBase:      fs.DefaultRepoPath,
		id:                id,
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

	// validate
	if svc.id.IsZero() {
		return nil, trace.TraceError(errors.ErrorSpiderMissingRequiredOption)
	}

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
	svc.fsSvc, err = fs.NewFsService(fsOpts...)
	if err != nil {
		return nil, err
	}

	// initialize
	if err := svc.Init(); err != nil {
		return nil, err
	}

	return svc, nil
}

func ProvideSpiderFsService(id primitive.ObjectID, opts ...Option) func() (svc interfaces.SpiderFsService, err error) {
	return func() (svc interfaces.SpiderFsService, err error) {
		return NewSpiderFsService(id, opts...)
	}
}

var spiderFsSvcCache = sync.Map{}

func GetSpiderFsService(id primitive.ObjectID, opts ...Option) (svc interfaces.SpiderFsService, err error) {
	// cache key consisted of id and config path
	// FIXME: this is only for testing purpose
	cfgPath := getConfigPathFromOptions(opts...)
	key := getHashStringFromIdAndConfigPath(id, cfgPath)

	// attempt to load from cache
	res, ok := spiderFsSvcCache.Load(key)
	if !ok {
		// not exists in cache, create a new service
		svc, err = NewSpiderFsService(id, opts...)
		if err != nil {
			return nil, err
		}

		// store in cache
		spiderFsSvcCache.Store(key, svc)
		return svc, nil
	}

	// load from cache successful
	svc, ok = res.(interfaces.SpiderFsService)
	if !ok {
		return nil, trace.TraceError(errors.ErrorFsInvalidType)
	}

	return svc, nil
}

func ProvideGetSpiderFsService(id primitive.ObjectID, opts ...Option) func() (svc interfaces.SpiderFsService, err error) {
	return func() (svc interfaces.SpiderFsService, err error) {
		return GetSpiderFsService(id, opts...)
	}
}
