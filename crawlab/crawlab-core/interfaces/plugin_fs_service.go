package interfaces

import (
	"go.mongodb.org/mongo-driver/bson/primitive"
)

type PluginFsService interface {
	WithConfigPath
	Init() (err error)
	SetId(id primitive.ObjectID)
	GetFsPath() (res string)
	GetWorkspacePath() (res string)
	SetFsPathBase(path string)
	SetWorkspacePathBase(path string)
	GetFsService() (fsSvc FsService)
}
