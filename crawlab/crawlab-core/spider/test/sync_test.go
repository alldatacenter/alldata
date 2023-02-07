package test

import (
	"fmt"
	"github.com/crawlab-team/crawlab-core/fs"
	"github.com/stretchr/testify/require"
	"io/ioutil"
	"os"
	"path"
	"testing"
)

func TestFsService_SyncToFs(t *testing.T) {
	var err error
	T.Setup(t)

	// save file to local
	filePath := path.Join(T.masterFsSvc.GetWorkspacePath(), T.ScriptName)
	err = ioutil.WriteFile(filePath, []byte(T.Script), os.ModePerm)
	require.Nil(t, err)

	// commit
	err = T.masterFsSvc.GetFsService().Commit("initial commit")
	require.Nil(t, err)

	// sync to fs
	err = T.masterSyncSvc.SyncToFs(T.TestSpider.Id)
	require.Nil(t, err)

	// validate
	remotePath := fmt.Sprintf("%s/%s/%s", fs.DefaultFsPath, T.TestSpider.Id.Hex(), T.ScriptName)
	data, err := T.fsSvc.GetFs().GetFile(remotePath)
	require.Nil(t, err)
	require.Equal(t, T.Script, string(data))
}

func TestFsService_SyncToWorkspace(t *testing.T) {
	var err error
	T.Setup(t)

	// save file to local
	require.Nil(t, err)
	err = T.masterFsSvc.GetFsService().Save(T.ScriptName, []byte(T.Script))

	// sync to fs
	err = T.workerSyncSvc.SyncToWorkspace(T.TestSpider.Id)
	require.Nil(t, err)

	// validate
	filePath := path.Join(T.workerFsSvc.GetWorkspacePath(), T.ScriptName)
	data, err := ioutil.ReadFile(filePath)
	require.Nil(t, err)
	require.Equal(t, T.Script, string(data))
}
