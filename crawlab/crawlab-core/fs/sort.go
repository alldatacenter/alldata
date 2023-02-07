package fs

import (
	"github.com/crawlab-team/goseaweedfs"
	"strings"
)

type FilerFileInfoSlice []goseaweedfs.FilerFileInfo

func (f FilerFileInfoSlice) Len() int {
	return len(f)
}

func (f FilerFileInfoSlice) Less(i, j int) bool {
	items := []goseaweedfs.FilerFileInfo(f)
	a := items[i]
	b := items[j]
	if a.IsDir && !b.IsDir {
		return true
	}
	if !a.IsDir && b.IsDir {
		return false
	}
	return strings.ToLower(a.Name) < strings.ToLower(b.Name)
}

func (f FilerFileInfoSlice) Swap(i, j int) {
	items := []goseaweedfs.FilerFileInfo(f)
	tmp := items[i]
	items[i] = items[j]
	items[j] = tmp
}
