package interfaces

import (
	"go.mongodb.org/mongo-driver/bson/primitive"
)

type SpiderSyncService interface {
	WithConfigPath
	SetFsPathBase(path string)
	SetWorkspacePathBase(path string)
	SetRepoPathBase(path string)
	GetFsService(id primitive.ObjectID) (fsSvc SpiderFsService, err error)
	ForceGetFsService(id primitive.ObjectID) (fsSvc SpiderFsService, err error)
	SyncToFs(id primitive.ObjectID) (err error)
	SyncToWorkspace(id primitive.ObjectID) (err error)
}
