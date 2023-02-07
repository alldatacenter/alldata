package fs

import (
	"github.com/crawlab-team/crawlab-core/interfaces"
	"go.mongodb.org/mongo-driver/bson/primitive"
)

type Option func(svc interfaces.SpiderFsService)

func WithConfigPath(path string) Option {
	return func(svc interfaces.SpiderFsService) {
		svc.SetConfigPath(path)
	}
}

func WithId(id primitive.ObjectID) Option {
	return func(fsSvc interfaces.SpiderFsService) {
		fsSvc.SetId(id)
	}
}

func WithFsPathBase(path string) Option {
	return func(svc interfaces.SpiderFsService) {
		svc.SetFsPathBase(path)
	}
}

func WithWorkspacePathBase(path string) Option {
	return func(svc interfaces.SpiderFsService) {
		svc.SetWorkspacePathBase(path)
	}
}

func WithRepoPathBase(path string) Option {
	return func(svc interfaces.SpiderFsService) {
		svc.SetRepoPathBase(path)
	}
}
