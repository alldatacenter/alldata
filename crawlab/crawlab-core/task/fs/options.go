package fs

import (
	"github.com/crawlab-team/crawlab-core/interfaces"
)

type Option func(svc interfaces.TaskFsService)

func WithFsPathBase(path string) Option {
	return func(svc interfaces.TaskFsService) {
		svc.SetFsPathBase(path)
	}
}

func WithWorkspacePathBase(path string) Option {
	return func(svc interfaces.TaskFsService) {
		svc.SetWorkspacePathBase(path)
	}
}

func WithRepoPathBase(path string) Option {
	return func(svc interfaces.TaskFsService) {
		svc.SetRepoPathBase(path)
	}
}
