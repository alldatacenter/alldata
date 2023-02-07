package fs

import "github.com/crawlab-team/crawlab-core/interfaces"

type Option func(svc interfaces.FsService)

func WithConfigPath(path string) Option {
	return func(svc interfaces.FsService) {
		svc.SetConfigPath(path)
	}
}

func IsAbsolute() interfaces.ServiceCrudOption {
	return func(opts *interfaces.ServiceCrudOptions) {
		opts.IsAbsolute = true
	}
}

func WithFsPath(path string) Option {
	return func(svc interfaces.FsService) {
		svc.SetFsPath(path)
	}
}

func WithWorkspacePath(path string) Option {
	return func(svc interfaces.FsService) {
		svc.SetWorkspacePath(path)
	}
}

func WithRepoPath(path string) Option {
	return func(svc interfaces.FsService) {
		svc.SetRepoPath(path)
	}
}
