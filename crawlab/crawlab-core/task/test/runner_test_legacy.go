package test

//import (
//	"fmt"
//	"github.com/crawlab-team/crawlab-core/constants"
//	"github.com/crawlab-team/crawlab-core/fs"
//	"github.com/crawlab-team/crawlab-core/interfaces"
//	models2 "github.com/crawlab-team/crawlab-core/models/models"
//	"github.com/crawlab-team/crawlab-core/models/service"
//	"github.com/crawlab-team/crawlab-core/task/handler"
//	"github.com/crawlab-team/crawlab-core/utils"
//	"github.com/crawlab-team/crawlab-db/mongo"
//	cfs "github.com/crawlab-team/crawlab-fs"
//	clog "github.com/crawlab-team/crawlab-log"
//	"github.com/spf13/viper"
//	"github.com/stretchr/testify/require"
//	"go.mongodb.org/mongo-driver/bson/primitive"
//	"os"
//	"strings"
//	"testing"
//	"time"
//)
//
//
//func setupTaskRunner() (to *TaskRunnerTestObject, err error) {
//	// test object
//	to = &TaskRunnerTestObject{}
//	to.spiderId = primitive.NewObjectID()
//	to.taskId = primitive.NewObjectID()
//	to.fsPath = fmt.Sprintf("%s/%s", "/spiders", to.spiderId.Hex())
//	to.repoPath = fmt.Sprintf("./tmp/repo/%s", to.spiderId.Hex())
//
//	// set debug
//	viper.Set("debug", true)
//
//	// mongo
//	viper.Set("mongo.host", "localhost")
//	viper.Set("mongo.port", "27017")
//	viper.Set("mongo.db", "test")
//	if err := mongo.InitMongo(); err != nil {
//		return to, err
//	}
//
//	// set paths
//	viper.Set("log.path", "/logs")
//	viper.Set("spider.path", "/spiders")
//	viper.Set("spider.workspace", "./tmp/workspace")
//
//	// cleanup
//	cleanupTaskRunner(to)
//
//	// fs
//	to.fs, err = fs.NewFsService(&fs.FileSystemServiceOptions{
//		IsMaster: true,
//		FsPath:   to.fsPath,
//		RepoPath: to.repoPath,
//	})
//	if err != nil {
//		return to, err
//	}
//
//	// spider
//	to.spider = models2.Spider{
//		Id:   to.spiderId,
//		Name: "test_spider",
//		Type: constants.Customized,
//		Cmd:  "python main.py",
//		Envs: []models2.Env{
//			{Name: "Env1", Value: "Value1"},
//			{Name: "Env2", Value: "Value2"},
//		},
//	}
//	if err := to.spider.Add(); err != nil {
//		return to, err
//	}
//
//	// task
//	to.task = models2.Task{
//		Id:       to.taskId,
//		SpiderId: to.spiderId,
//		Type:     constants.TaskTypeSpider,
//	}
//	if err := to.task.Add(); err != nil {
//		return to, err
//	}
//
//	// add python script
//	pythonScript := `print('it works')`
//	if err := to.fs.Save("main.py", []byte(pythonScript), nil); err != nil {
//		return to, err
//	}
//
//	// commit
//	if err := to.fs.Commit("initial commit"); err != nil {
//		return to, err
//	}
//
//	return to, nil
//}
//
//func cleanupTaskRunner(to *TaskRunnerTestObject) {
//	var modelSvc service.ModelService
//	utils.MustResolveModule("", modelSvc)
//
//	if m, err := cfs.NewSeaweedFsManager(); err == nil {
//		_ = m.DeleteDir("/logs")
//		_ = m.DeleteDir("/spiders")
//	}
//	_ = modelSvc.GetBaseService(interfaces.ModelIdSpider).DeleteById(to.spiderId)
//	_ = modelSvc.GetBaseService(interfaces.ModelIdTask).DeleteById(to.spiderId)
//	_ = os.RemoveAll("./tmp/repo")
//}
//
//func TestNewTaskRunner(t *testing.T) {
//	to, err := setupTaskRunner()
//	require.Nil(t, err)
//
//	// create task runner
//	runner, err := handler.NewTaskRunner(&TaskRunnerOptions{
//		TaskId:        to.task.Id,
//		LogDriverType: clog.DriverTypeFs,
//	})
//	require.Nil(t, err)
//	require.NotNil(t, runner.fs)
//	require.NotNil(t, runner.t)
//	require.NotNil(t, runner.s)
//	require.NotNil(t, runner.l)
//	require.NotNil(t, runner.ch)
//
//	cleanupTaskRunner(to)
//}
//
//func TestTaskRunner_Run(t *testing.T) {
//	to, err := setupTaskRunner()
//	require.Nil(t, err)
//
//	// create task runner
//	runner, err := handler.NewTaskRunner(&TaskRunnerOptions{
//		TaskId:        to.task.Id,
//		LogDriverType: clog.DriverTypeFs,
//	})
//	require.Nil(t, err)
//
//	// run
//	err = runner.Schedule()
//	require.Nil(t, err)
//
//	// test logs
//	lines, err := runner.l.Find("", 0, 100)
//	require.Nil(t, err)
//	require.Len(t, lines, 1)
//	require.Equal(t, "it works", lines[0])
//
//	cleanupTaskRunner(to)
//}
//
//func TestTaskRunner_RunWithError(t *testing.T) {
//	to, err := setupTaskRunner()
//	require.Nil(t, err)
//
//	// add error python script
//	pythonScript := `
//raise Exception('an error')
//`
//	err = to.fs.Save("main.py", []byte(pythonScript), nil)
//	require.Nil(t, err)
//
//	// create task runner
//	runner, err := handler.NewTaskRunner(&TaskRunnerOptions{
//		TaskId:        to.task.Id,
//		LogDriverType: clog.DriverTypeFs,
//	})
//	require.Nil(t, err)
//
//	// run
//	err = runner.Schedule()
//	require.Equal(t, constants.ErrTaskError, err)
//
//	// test logs
//	lines, err := runner.l.Find("", 0, 100)
//	require.Nil(t, err)
//	require.Greater(t, len(lines), 0)
//	hasExceptionLog := false
//	for _, line := range lines {
//		if strings.Contains(strings.ToLower(line), "exception") {
//			hasExceptionLog = true
//			break
//		}
//	}
//	require.True(t, hasExceptionLog)
//
//	cleanupTaskRunner(to)
//}
//
//func TestTaskRunner_RunLong(t *testing.T) {
//	to, err := setupTaskRunner()
//	require.Nil(t, err)
//
//	// add a long task python script
//	n := 5
//	pythonScript := fmt.Sprintf(`
//import time
//import sys
//for i in range(%d):
//    print('line: ' + str(i))
//    time.sleep(1)
//    sys.stdout.flush()
//`, n)
//	err = to.fs.Save("main.py", []byte(pythonScript), nil)
//	require.Nil(t, err)
//
//	// create task runner
//	runner, err := handler.NewTaskRunner(&TaskRunnerOptions{
//		TaskId:        to.task.Id,
//		LogDriverType: clog.DriverTypeFs,
//	})
//	require.Nil(t, err)
//
//	// run
//	err = runner.Schedule()
//	require.Nil(t, err)
//
//	// test logs
//	lines, err := runner.l.Find("", 0, 100)
//	require.Nil(t, err)
//	require.Equal(t, n, len(lines))
//	for i, line := range lines {
//		require.Equal(t, fmt.Sprintf("line: %d", i), line)
//	}
//
//	cleanupTaskRunner(to)
//}
//
//func TestTaskRunner_Cancel(t *testing.T) {
//	to, err := setupTaskRunner()
//	require.Nil(t, err)
//
//	// add a long task python script
//	n := 10
//	pythonScript := fmt.Sprintf(`
//import time
//import sys
//for i in range(%d):
//    print('line: ' + str(i))
//    time.sleep(1)
//    sys.stdout.flush()
//`, n)
//	err = to.fs.Save("main.py", []byte(pythonScript), nil)
//	require.Nil(t, err)
//
//	// create task runner
//	runner, err := handler.NewTaskRunner(&TaskRunnerOptions{
//		TaskId:        to.task.Id,
//		LogDriverType: clog.DriverTypeFs,
//	})
//	require.Nil(t, err)
//
//	// cancel
//	go func() {
//		time.Sleep(5 * time.Second)
//		err = runner.Cancel()
//		require.Nil(t, err)
//	}()
//
//	// run
//	err = runner.Schedule()
//	require.Equal(t, constants.ErrTaskCancelled, err)
//
//	// test logs
//	lines, err := runner.l.Find("", 0, 100)
//	require.Nil(t, err)
//	require.Greater(t, len(lines), 0)
//
//	cleanupTaskRunner(to)
//}
