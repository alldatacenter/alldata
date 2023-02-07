package plugin

import (
	"fmt"
	"github.com/crawlab-team/crawlab-core/errors"
	"github.com/crawlab-team/crawlab-core/fs"
	"github.com/crawlab-team/crawlab-core/interfaces"
	"github.com/crawlab-team/go-trace"
	"github.com/spf13/viper"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"os"
	"sync"
)

type FsService struct {
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

func (svc *FsService) GetConfigPath() (path string) {
	return svc.cfgPath
}

func (svc *FsService) SetConfigPath(path string) {
	svc.cfgPath = path
}

func (svc *FsService) Init() (err error) {
	// workspace
	if _, err := os.Stat(svc.GetWorkspacePath()); err != nil {
		if err := os.MkdirAll(svc.GetWorkspacePath(), os.FileMode(0766)); err != nil {
			return trace.TraceError(err)
		}
	}

	return nil
}

func (svc *FsService) SetId(id primitive.ObjectID) {
	svc.id = id
}

func (svc *FsService) GetFsPath() (res string) {
	return fmt.Sprintf("%s/%s", svc.fsPathBase, svc.id.Hex())
}

func (svc *FsService) GetWorkspacePath() (res string) {
	return fmt.Sprintf("%s/%s", svc.workspacePathBase, svc.id.Hex())
}

func (svc *FsService) SetFsPathBase(path string) {
	svc.fsPathBase = path
}

func (svc *FsService) SetWorkspacePathBase(path string) {
	svc.workspacePathBase = path
}

func (svc *FsService) GetFsService() (fsSvc interfaces.FsService) {
	return svc.fsSvc
}

func NewPluginFsService(id primitive.ObjectID, opts ...FsOption) (svc2 interfaces.PluginFsService, err error) {
	// service
	svc := &FsService{
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
		return nil, trace.TraceError(errors.ErrorPluginMissingRequiredOption)
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

func ProvidePluginFsService(id primitive.ObjectID, opts ...FsOption) func() (svc interfaces.PluginFsService, err error) {
	return func() (svc interfaces.PluginFsService, err error) {
		return NewPluginFsService(id, opts...)
	}
}

var pluginFsSvcCache = sync.Map{}

func GetPluginFsService(id primitive.ObjectID, opts ...FsOption) (svc interfaces.PluginFsService, err error) {
	key := id.Hex()

	// attempt to load from cache
	res, ok := pluginFsSvcCache.Load(key)
	if !ok {
		// not exists in cache, create a new service
		svc, err = NewPluginFsService(id, opts...)
		if err != nil {
			return nil, err
		}

		// store in cache
		pluginFsSvcCache.Store(key, svc)
		return svc, nil
	}

	// load from cache successful
	svc, ok = res.(interfaces.PluginFsService)
	if !ok {
		return nil, trace.TraceError(errors.ErrorFsInvalidType)
	}

	return svc, nil
}

func ProvideGetPluginFsService(id primitive.ObjectID, opts ...FsOption) func() (svc interfaces.PluginFsService, err error) {
	return func() (svc interfaces.PluginFsService, err error) {
		return GetPluginFsService(id, opts...)
	}
}
