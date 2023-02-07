package entity

import (
	"github.com/crawlab-team/crawlab-core/interfaces"
)

type FsFileInfo struct {
	Name      string                  `json:"name"`      // file name
	Path      string                  `json:"path"`      // file path
	FullPath  string                  `json:"full_path"` // file full path
	Extension string                  `json:"extension"` // file extension
	Md5       string                  `json:"md5"`       // MD5 hash
	IsDir     bool                    `json:"is_dir"`    // whether it is directory
	FileSize  int64                   `json:"file_size"` // file size (bytes)
	Children  []interfaces.FsFileInfo `json:"children"`  // children for sub-directory
}

func (f *FsFileInfo) GetName() string {
	return f.Name
}

func (f *FsFileInfo) GetPath() string {
	return f.Path
}

func (f *FsFileInfo) GetFullPath() string {
	return f.FullPath
}

func (f *FsFileInfo) GetExtension() string {
	return f.Extension
}

func (f *FsFileInfo) GetMd5() string {
	return f.Md5
}

func (f *FsFileInfo) GetIsDir() bool {
	return f.IsDir
}

func (f *FsFileInfo) GetFileSize() int64 {
	return f.FileSize
}

func (f *FsFileInfo) GetChildren() []interfaces.FsFileInfo {
	return f.Children
}
