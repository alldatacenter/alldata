package interfaces

type TaskFsService interface {
	Init() (err error)
	GetFsPath() (res string)
	GetWorkspacePath() (res string)
	GetRepoPath() (res string)
	SetFsPathBase(path string)
	SetWorkspacePathBase(path string)
	SetRepoPathBase(path string)
	GetFsService() (fsSvc FsService)
}
