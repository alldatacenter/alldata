package plugin

import (
	"github.com/crawlab-team/crawlab-core/interfaces"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"time"
)

type Option func(svc interfaces.PluginService)

func WithFsPathBase(path string) Option {
	return func(svc interfaces.PluginService) {
		svc.SetFsPathBase(path)
	}
}

func WithMonitorInterval(interval time.Duration) Option {
	return func(svc interfaces.PluginService) {
		svc.SetMonitorInterval(interval)
	}
}

type FsOption func(svc interfaces.PluginFsService)

func WithFsConfigPath(path string) FsOption {
	return func(svc interfaces.PluginFsService) {
		svc.SetConfigPath(path)
	}
}

func WithFsId(id primitive.ObjectID) FsOption {
	return func(fsSvc interfaces.PluginFsService) {
		fsSvc.SetId(id)
	}
}

func WithFsFsPathBase(path string) FsOption {
	return func(svc interfaces.PluginFsService) {
		svc.SetFsPathBase(path)
	}
}

func WithFsWorkspacePathBase(path string) FsOption {
	return func(svc interfaces.PluginFsService) {
		svc.SetWorkspacePathBase(path)
	}
}
