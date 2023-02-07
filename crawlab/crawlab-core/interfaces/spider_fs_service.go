package interfaces

import (
	"go.mongodb.org/mongo-driver/bson/primitive"
)

type SpiderFsService interface {
	WithConfigPath
	Init() (err error)
	SetId(id primitive.ObjectID)
	List(path string) (files []FsFileInfo, err error)
	GetFile(path string) (data []byte, err error)
	GetFileInfo(path string) (file FsFileInfo, err error)
	Exists(path string) (ok bool)
	Save(path string, data []byte) (err error)
	Rename(path, newPath string) (err error)
	Delete(path string) (err error)
	Copy(path, newPath string) (err error)
	Commit(msg string) (err error)
	GetFsPath() (res string)
	GetWorkspacePath() (res string)
	GetRepoPath() (res string)
	SetFsPathBase(path string)
	SetWorkspacePathBase(path string)
	SetRepoPathBase(path string)
	GetFsService() (fsSvc FsService)
}
