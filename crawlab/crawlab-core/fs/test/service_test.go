package test

import (
	"fmt"
	"github.com/crawlab-team/crawlab-core/errors"
	"github.com/crawlab-team/crawlab-core/fs"
	"github.com/crawlab-team/crawlab-core/interfaces"
	vcs "github.com/crawlab-team/crawlab-vcs"
	"github.com/stretchr/testify/require"
	"io/ioutil"
	"path"
	"testing"
)

func TestService_Save(t *testing.T) {
	var err error
	T.Setup(t)

	// save new file to remote
	content := "it works"
	err = T.masterFsSvc.Save("test_file.txt", []byte(content))
	require.Nil(t, err)

	// get file
	data, err := T.masterFsSvc.GetFile("test_file.txt")
	require.Nil(t, err)
	require.Equal(t, content, string(data))

	// test absolute path
	data, err = T.masterFsSvc.GetFile(fmt.Sprintf("%s%s", T.masterFsSvc.GetFsPath(), "/test_file.txt"), fs.IsAbsolute())
	require.Nil(t, err)
	require.Equal(t, content, string(data))

	// validate local
	filePath := path.Join(T.masterFsSvc.GetWorkspacePath(), "test_file.txt")
	data, err = ioutil.ReadFile(filePath)
	require.Nil(t, err)
	require.Equal(t, content, string(data))
}

func TestService_Rename(t *testing.T) {
	var err error
	T.Setup(t)

	// save new file to remote
	content := "it works"
	err = T.masterFsSvc.Save("test_file.txt", []byte(content))
	require.Nil(t, err)
	ok, err := T.masterFsSvc.GetFs().Exists("/test/test_file.txt")
	require.Nil(t, err)
	require.True(t, ok)

	// rename file
	err = T.masterFsSvc.Rename("test_file.txt", "test_file2.txt")
	require.Nil(t, err)
	ok, err = T.masterFsSvc.GetFs().Exists("/test/test_file.txt")
	require.Nil(t, err)
	require.False(t, ok)
	ok, err = T.masterFsSvc.GetFs().Exists("/test/test_file2.txt")
	require.Nil(t, err)
	require.True(t, ok)

	// validate local
	filePath := path.Join(T.masterFsSvc.GetWorkspacePath(), "test_file2.txt")
	data, err := ioutil.ReadFile(filePath)
	require.Nil(t, err)
	require.Equal(t, content, string(data))

	// rename to existing
	err = T.masterFsSvc.Save("test_file.txt", []byte(content))
	require.Nil(t, err)
	err = T.masterFsSvc.Rename("test_file.txt", "test_file2.txt")
	require.NotNil(t, err)
	require.Equal(t, errors.ErrorFsAlreadyExists.Error(), err.Error())
}

func TestService_Delete(t *testing.T) {
	var err error
	T.Setup(t)

	// save new file to remote
	content := "it works"
	err = T.masterFsSvc.Save("test_file.txt", []byte(content))
	require.Nil(t, err)

	// delete remote file
	err = T.masterFsSvc.Delete("test_file.txt")
	require.Nil(t, err)
	ok, err := T.masterFsSvc.GetFs().Exists("/test/test_file.txt")
	require.Nil(t, err)
	require.False(t, ok)

	// test absolute path
	err = T.masterFsSvc.Save("test_file.txt", []byte(content))
	require.Nil(t, err)
	err = T.masterFsSvc.Delete(fmt.Sprintf("%s%s", T.masterFsSvc.GetFsPath(), "/test_file.txt"), fs.IsAbsolute())
	require.Nil(t, err)
	ok, err = T.masterFsSvc.GetFs().Exists("/test/test_file.txt")
	require.Nil(t, err)
	require.False(t, ok)

	// validate local
	filePath := path.Join(T.masterFsSvc.GetWorkspacePath(), "test_file.txt")
	require.NoFileExists(t, filePath)
}

func TestService_SyncToWorkspace(t *testing.T) {
	var err error
	T.Setup(t)

	// save new file to remote
	content := "it works"
	err = T.masterFsSvc.Save("test_file.txt", []byte(content))
	require.Nil(t, err)

	// sync to workspace
	err = T.workerFsSvc.SyncToWorkspace()
	require.Nil(t, err)

	// validate
	require.FileExists(t, "./tmp/test_worker_workspace/test_file.txt")
	data, err := ioutil.ReadFile("./tmp/test_worker_workspace/test_file.txt")
	require.Nil(t, err)
	require.Equal(t, content, string(data))
}

func TestService_WorkerFsService(t *testing.T) {
	var err error
	T.Setup(t)

	// save new file to remote
	content := "it works"
	err = T.masterFsSvc.Save("test_file.txt", []byte(content))
	require.Nil(t, err)

	// test methods
	_, err = T.workerFsSvc.List("/")
	require.NotNil(t, err)
	require.Equal(t, errors.ErrorFsForbidden.Error(), err.Error())
	_, err = T.workerFsSvc.GetFile("test_file.txt")
	require.NotNil(t, err)
	require.Equal(t, errors.ErrorFsForbidden.Error(), err.Error())
	err = T.workerFsSvc.Save("test_file.txt", []byte("it works"))
	require.NotNil(t, err)
	require.Equal(t, errors.ErrorFsForbidden.Error(), err.Error())
	err = T.workerFsSvc.Rename("test_file.txt", "new_test_file.txt")
	require.NotNil(t, err)
	require.Equal(t, errors.ErrorFsForbidden.Error(), err.Error())
	err = T.workerFsSvc.Delete("test_file.txt")
	require.NotNil(t, err)
	require.Equal(t, errors.ErrorFsForbidden.Error(), err.Error())
	err = T.workerFsSvc.Commit("test commit")
	require.NotNil(t, err)
	require.Equal(t, errors.ErrorFsForbidden.Error(), err.Error())
	err = T.workerFsSvc.SyncToFs()
	require.NotNil(t, err)
	require.Equal(t, errors.ErrorFsForbidden.Error(), err.Error())
	err = T.workerFsSvc.SyncToWorkspace()
	require.Nil(t, err)
	data, err := ioutil.ReadFile("./tmp/test_worker_workspace/test_file.txt")
	require.Nil(t, err)
	require.Equal(t, content, string(data))
}

func TestService_Copy(t *testing.T) {
	var err error
	T.Setup(t)

	// save new files to remote
	content := "it works"
	err = T.masterFsSvc.Save("/old/test_file.txt", []byte(content))
	require.Nil(t, err)
	err = T.masterFsSvc.Save("/old/nested/test_file.txt", []byte(content))
	require.Nil(t, err)

	// test methods
	err = T.masterFsSvc.Copy("/old", "/new")
	require.Nil(t, err)

	// validate results
	files, err := T.masterFsSvc.List("/new")
	require.Nil(t, err)
	require.Greater(t, len(files), 0)
	data, err := T.masterFsSvc.GetFile("/new/test_file.txt")
	require.Nil(t, err)
	require.Equal(t, content, string(data))
	data, err = T.masterFsSvc.GetFile("/new/nested/test_file.txt")
	require.Nil(t, err)
	require.Equal(t, content, string(data))

	// test absolute path
	err = T.masterFsSvc.Copy(
		fmt.Sprintf("%s%s", T.masterFsSvc.GetFsPath(), "/old"),
		fmt.Sprintf("%s%s", T.masterFsSvc.GetFsPath(), "/new_absolute"),
		fs.IsAbsolute(),
	)
	require.Nil(t, err)
	files, err = T.masterFsSvc.List(fmt.Sprintf("%s%s", T.masterFsSvc.GetFsPath(), "/new_absolute"), fs.IsAbsolute())
	require.Nil(t, err)
	require.Greater(t, len(files), 0)

	// validate local
	filePath := path.Join(T.masterFsSvc.GetWorkspacePath(), "/new/test_file.txt")
	data, err = ioutil.ReadFile(filePath)
	require.Nil(t, err)
	require.Equal(t, content, string(data))
}

func TestService_Commit(t *testing.T) {
	var err error
	T.Setup(t)

	// save new file to remote
	content := "it works"
	err = T.masterFsSvc.Save("test_file.txt", []byte(content))
	require.Nil(t, err)

	// commit to repo
	err = T.masterFsSvc.Commit("test commit")
	require.Nil(t, err)

	// new git client from remote repo
	c, err := vcs.NewGitClient(
		vcs.WithPath("./tmp/test_local"),
		vcs.WithRemoteUrl("./tmp/test_master_repo"),
	)
	require.Nil(t, err)
	require.NotNil(t, c)
	require.FileExists(t, "./tmp/test_local/test_file.txt")
	data, err := ioutil.ReadFile("./tmp/test_local/test_file.txt")
	require.Nil(t, err)
	require.Equal(t, content, string(data))
}

func TestService_SyncToFs(t *testing.T) {
	var err error
	T.Setup(t)

	// save new file to remote
	content := "it works"
	err = T.masterFsSvc.Save("test_file.txt", []byte(content))
	require.Nil(t, err)

	// commit
	err = T.masterFsSvc.Commit("initial commit")
	require.Nil(t, err)

	// edit the file
	content2 := "hello world"
	err = T.masterFsSvc.Save("test_file.txt", []byte(content2))
	require.Nil(t, err)

	// test edited file content
	data, err := T.masterFsSvc.GetFile("test_file.txt")
	require.Nil(t, err)
	require.Equal(t, content2, string(data))

	// sync to fs
	err = T.masterFsSvc.SyncToFs()
	require.Nil(t, err)

	// test synced file content
	data, err = T.masterFsSvc.GetFile("test_file.txt")
	require.Nil(t, err)
	require.Equal(t, content, string(data))
}

func TestService_SyncToFs_OnlyFromWorkspace(t *testing.T) {
	var err error
	T.Setup(t)

	// save new file to remote
	content := "it works"
	err = T.masterFsSvc.Save("test_file.txt", []byte(content))
	require.Nil(t, err)

	// commit
	err = T.masterFsSvc.Commit("initial commit")
	require.Nil(t, err)

	// edit the file
	content2 := "hello world"
	err = T.masterFsSvc.Save("test_file.txt", []byte(content2))
	require.Nil(t, err)

	// test edited file content
	data, err := T.masterFsSvc.GetFile("test_file.txt")
	require.Nil(t, err)
	require.Equal(t, content2, string(data))

	// sync to fs
	err = T.masterFsSvc.SyncToFs(interfaces.WithOnlyFromWorkspace())
	require.Nil(t, err)

	// test synced file content
	data, err = T.masterFsSvc.GetFile("test_file.txt")
	require.Nil(t, err)
	require.Equal(t, content2, string(data))
}

func TestService_List(t *testing.T) {
	var err error
	T.Setup(t)

	// save new files to remote
	content := "it works"
	err = T.masterFsSvc.Save("test_file.txt", []byte(content))
	require.Nil(t, err)
	err = T.masterFsSvc.Save("/nested/test_file.txt", []byte(content))
	require.Nil(t, err)

	// list files
	files, err := T.masterFsSvc.List("/")
	require.Nil(t, err)
	isTestFileValid := false
	isNestedValid := false
	for _, f := range files {
		if f.GetName() == "test_file.txt" && !f.GetIsDir() {
			isTestFileValid = true
		}
		if f.GetName() == "nested" &&
			f.GetIsDir() &&
			len(f.GetChildren()) > 0 &&
			f.GetChildren()[0].GetName() == "test_file.txt" &&
			!f.GetChildren()[0].GetIsDir() {
			isNestedValid = true
		}
	}
	require.True(t, isTestFileValid)
	require.True(t, isNestedValid)

	// test absolute path
	files, err = T.masterFsSvc.List(fmt.Sprintf("%s%s", T.masterFsSvc.GetFsPath(), "/"), fs.IsAbsolute())
	require.Nil(t, err)
	require.Greater(t, len(files), 0)
}
