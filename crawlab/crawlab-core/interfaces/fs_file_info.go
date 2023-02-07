package interfaces

type FsFileInfo interface {
	GetName() string
	GetPath() string
	GetFullPath() string
	GetExtension() string
	GetMd5() string
	GetIsDir() bool
	GetFileSize() int64
	GetChildren() []FsFileInfo
}
