package test

//import (
//	"fmt"
//	"github.com/crawlab-team/crawlab-core/constants"
//	"github.com/crawlab-team/crawlab-core/fs"
//	"github.com/crawlab-team/crawlab-core/interfaces"
//	models2 "github.com/crawlab-team/crawlab-core/models/models"
//	"github.com/crawlab-team/crawlab-core/models/service"
//	"github.com/crawlab-team/crawlab-core/services"
//	"github.com/crawlab-team/crawlab-core/utils"
//	"github.com/crawlab-team/crawlab-db/mongo"
//	"github.com/crawlab-team/crawlab-db/redis"
//	cfs "github.com/crawlab-team/crawlab-fs"
//	"github.com/spf13/viper"
//	"github.com/stretchr/testify/require"
//	"go.mongodb.org/mongo-driver/bson/primitive"
//	"os"
//	"testing"
//	"time"
//)
//
//type TaskTestObject struct {
//	nodes    []*models2.Node
//	spiders  []*models2.Spider
//	tasks    []*models2.Task
//	fs       *services.fileSystemService
//	fsPath   string
//	repoPath string
//}
//
//func (to *TaskTestObject) GetFsPath(s models2.Spider) (fsPath string) {
//	return fmt.Sprintf("%s/%s", "/spiders", s.Id.Hex())
//}
//
//func (to *TaskTestObject) GetRepoPath(s models2.Spider) (fsPath string) {
//	return fmt.Sprintf("./tmp/repo/%s", s.Id.Hex())
//}
//
//func (to *TaskTestObject) CreateNode(name string, isMaster bool, key string) (n *models2.Node, err error) {
//	n = &models2.Node{
//		Id:       primitive.NewObjectID(),
//		Name:     name,
//		IsMaster: isMaster,
//		Key:      key,
//	}
//	if err := n.Add(); err != nil {
//		return n, err
//	}
//	to.nodes = append(to.nodes, n)
//	return n, nil
//}
//
//func (to *TaskTestObject) CreateSpider(name string) (s *models2.Spider, err error) {
//	s = &models2.Spider{
//		Id:   primitive.NewObjectID(),
//		Name: name,
//		Type: constants.Customized,
//		Cmd:  "python main.py",
//		Envs: []models2.Env{
//			{Name: "Env1", Value: "Value1"},
//			{Name: "Env2", Value: "Value2"},
//		},
//	}
//	if err := s.Add(); err != nil {
//		return s, err
//	}
//	to.spiders = append(to.spiders, s)
//	return s, nil
//}
//
//func (to *TaskTestObject) CreateTask(s *models2.Spider) (t *models2.Task, err error) {
//	t = &models2.Task{
//		Id:       primitive.NewObjectID(),
//		SpiderId: s.Id,
//		Type:     constants.TaskTypeSpider,
//	}
//	if err := t.Add(); err != nil {
//		return t, err
//	}
//	to.tasks = append(to.tasks, t)
//	return t, nil
//}
//
//func setupTask() (to *TaskTestObject, err error) {
//	// test object
//	to = &TaskTestObject{}
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
//	// redis
//	viper.Set("redis.address", "localhost")
//	viper.Set("redis.port", "6379")
//	viper.Set("redis.database", "0")
//	if err := redis.InitRedis(); err != nil {
//		return to, err
//	}
//
//	// set paths
//	viper.Set("log.path", "/logs")
//	viper.Set("spider.path", "/spiders")
//	viper.Set("spider.workspace", "./tmp/workspace")
//
//	// set node register
//	viper.Set("server.register.type", "ip")
//	viper.Set("server.register.ip", "192.168.0.1")
//
//	// cleanup
//	cleanupTask(to)
//
//	// fs (global)
//	to.fsPath = viper.GetString("spider.path")
//	to.fs, err = fs.NewFsService(&fs.FileSystemServiceOptions{
//		IsMaster: true,
//		FsPath:   to.fsPath,
//	})
//	if err != nil {
//		return to, err
//	}
//
//	// init services
//	if err := InitNodeService(); err != nil {
//		return to, err
//	}
//	//if err := InitSpiderService(); err != nil {
//	//	return to, err
//	//}
//	if err := InitTaskService(); err != nil {
//		return to, err
//	}
//
//	// nodes
//	ips := []string{
//		"192.168.0.1",
//		"192.168.0.2",
//	}
//	nn := 2
//	for i := 0; i < nn; i++ {
//		name := fmt.Sprintf("node%d", i+1)
//		isMaster := false
//		if i == 0 {
//			isMaster = true
//		}
//		n, err := to.CreateNode(name, isMaster, ips[i])
//		if err != nil {
//			return to, err
//		}
//		to.nodes = append(to.nodes, n)
//	}
//
//	// spiders
//	ns := 3
//	for i := 0; i < ns; i++ {
//		name := fmt.Sprintf("s%d", i+1)
//		_, err := to.CreateSpider(name)
//		if err != nil {
//			return to, err
//		}
//	}
//
//	// add scripts
//	py1 := `print('it works')`
//	if err := to.fs.Save(fmt.Sprintf("/%s/main.py", to.spiders[0].Id.Hex()), []byte(py1), nil); err != nil {
//		return to, err
//	}
//	py2 := `
//import time
//import sys
//for i in range(3):
//    print('line: ' + str(i))
//    time.sleep(1)
//    sys.stdout.flush()
//`
//	if err := to.fs.Save(fmt.Sprintf("/%s/main.py", to.spiders[1].Id.Hex()), []byte(py2), nil); err != nil {
//		return to, err
//	}
//	py3 := `print('it works')`
//	if err := to.fs.Save(fmt.Sprintf("/%s/main.py", to.spiders[2].Id.Hex()), []byte(py3), nil); err != nil {
//		return to, err
//	}
//
//	return to, nil
//}
//
//func cleanupTask(to *TaskTestObject) {
//	var modelSvc service.ModelService
//	utils.MustResolveModule("", modelSvc)
//
//	if m, err := cfs.NewSeaweedFsManager(); err == nil {
//		_ = m.DeleteDir("/logs")
//		_ = m.DeleteDir("/spiders")
//	}
//	for _, s := range to.spiders {
//		_ = modelSvc.GetBaseService(interfaces.ModelIdSpider).DeleteById(s.Id)
//	}
//	to.spiders = []*models2.Spider{}
//	for _, t := range to.tasks {
//		_ = modelSvc.GetBaseService(interfaces.ModelIdTask).DeleteById(t.Id)
//	}
//	to.tasks = []*models2.Task{}
//	_ = os.RemoveAll("./tmp/repo")
//	_ = redis.RedisClient.Del("tasks:public")
//	CloseTaskService()
//}
//
//func TestNewTaskService(t *testing.T) {
//	to, err := setupTask()
//	require.Nil(t, err)
//
//	// create master Service
//	s, err := NewTaskService(&TaskServiceOptions{
//		IsMaster: true,
//	})
//	require.Nil(t, err)
//	require.Equal(t, 0, s.runnersCount)
//	require.Equal(t, true, s.opts.IsMaster)
//	require.Equal(t, 8, s.opts.MaxRunners)
//	require.Equal(t, 5, s.opts.PollWaitSeconds)
//
//	cleanupTask(to)
//}
//
//func TestTaskService_Init(t *testing.T) {
//	var modelSvc service.ModelService
//	utils.MustResolveModule("", modelSvc)
//
//	to, err := setupTask()
//	require.Nil(t, err)
//
//	// create master Service
//	s, err := NewTaskService(&TaskServiceOptions{
//		IsMaster:        true,
//		PollWaitSeconds: 1,
//	})
//	require.Nil(t, err)
//	defer s.Close()
//
//	// test init
//	go s.Init()
//
//	// test assign task
//	task, err := to.CreateTask(to.spiders[1])
//	require.Nil(t, err)
//	err = s.Assign(task)
//	require.Nil(t, err)
//	task, err = modelSvc.GetTaskById(task.Id)
//	require.Nil(t, err)
//	require.Equal(t, constants.StatusPending, task.Status)
//	time.Sleep(2 * time.Second)
//	task, err = modelSvc.GetTaskById(task.Id)
//	require.Nil(t, err)
//	require.Equal(t, constants.StatusRunning, task.Status)
//	time.Sleep(3 * time.Second)
//	task, err = modelSvc.GetTaskById(task.Id)
//	require.Nil(t, err)
//	require.Equal(t, constants.StatusFinished, task.Status)
//
//	cleanupTask(to)
//}
//
//func TestTaskService_Assign(t *testing.T) {
//	to, err := setupTask()
//	require.Nil(t, err)
//
//	// create master Service
//	s, err := NewTaskService(&TaskServiceOptions{
//		IsMaster: true,
//	})
//	require.Nil(t, err)
//	defer s.Close()
//
//	// test assign (without init)
//	task, err := to.CreateTask(to.spiders[0])
//	require.Nil(t, err)
//	err = s.Assign(task)
//	require.Nil(t, err)
//	count, err := redis.RedisClient.LLen("tasks:public")
//	require.Nil(t, err)
//	require.Equal(t, 1, count)
//	result, err := redis.RedisClient.LPop("tasks:public")
//	require.Nil(t, err)
//	require.NotEmpty(t, result)
//	count, err = redis.RedisClient.LLen("tasks:public")
//	require.Nil(t, err)
//	require.Equal(t, 0, count)
//
//	cleanupTask(to)
//}
//
//func TestTaskService_Fetch(t *testing.T) {
//	to, err := setupTask()
//	require.Nil(t, err)
//
//	// create master Service
//	s, err := NewTaskService(&TaskServiceOptions{
//		IsMaster: true,
//	})
//	require.Nil(t, err)
//	defer s.Close()
//
//	// test fetch (without init)
//	task, err := to.CreateTask(to.spiders[0])
//	require.Nil(t, err)
//	err = s.Assign(task)
//	require.Nil(t, err)
//	count, err := redis.RedisClient.LLen("tasks:public")
//	require.Nil(t, err)
//	require.Equal(t, 1, count)
//	task2, err := s.Fetch()
//	require.Nil(t, err)
//	require.Equal(t, task.Id, task2.Id)
//	require.Nil(t, err)
//	count, err = redis.RedisClient.LLen("tasks:public")
//	require.Nil(t, err)
//	require.Equal(t, 0, count)
//
//	cleanupTask(to)
//}
//
//func TestTaskService_Run(t *testing.T) {
//	var modelSvc service.ModelService
//	utils.MustResolveModule("", modelSvc)
//
//	to, err := setupTask()
//	require.Nil(t, err)
//
//	// create master Service
//	s, err := NewTaskService(&TaskServiceOptions{
//		IsMaster: true,
//	})
//	require.Nil(t, err)
//	defer s.Close()
//
//	// test run (full process: assign -> fetch -> run)
//	task, err := to.CreateTask(to.spiders[1])
//	require.Nil(t, err)
//	err = s.Assign(task)
//	require.Nil(t, err)
//	task, err = s.Fetch()
//	require.Nil(t, err)
//	err = s.Schedule(task.Id)
//	require.Nil(t, err)
//	err = s.Schedule(task.Id)
//	require.Equal(t, constants.ErrAlreadyExists, err)
//	require.Equal(t, 1, s.runnersCount)
//	time.Sleep(1 * time.Second)
//	task, err = modelSvc.GetTaskById(task.Id)
//	require.Nil(t, err)
//	require.Equal(t, constants.StatusRunning, task.Status)
//	time.Sleep(3 * time.Second)
//	task, err = modelSvc.GetTaskById(task.Id)
//	require.Nil(t, err)
//	require.Equal(t, constants.StatusFinished, task.Status)
//
//	cleanupTask(to)
//}
//
//func TestTaskService_RunMultipleTasks(t *testing.T) {
//	to, err := setupTask()
//	require.Nil(t, err)
//
//	// create master Service
//	s, err := NewTaskService(&TaskServiceOptions{
//		IsMaster:        true,
//		PollWaitSeconds: 1,
//	})
//	require.Nil(t, err)
//	defer s.Close()
//
//	// test init
//	go s.Init()
//
//	// test assign multiple tasks
//	nt := 3
//	for i := 0; i < nt; i++ {
//		task, err := to.CreateTask(to.spiders[1])
//		require.Nil(t, err)
//		err = s.Assign(task)
//		require.Nil(t, err)
//	}
//	maxRunnersCount := 0
//	for i := 0; i < 20; i++ {
//		if maxRunnersCount < s.runnersCount {
//			maxRunnersCount = s.runnersCount
//		}
//		if maxRunnersCount == nt {
//			break
//		}
//		time.Sleep(500 * time.Millisecond)
//	}
//	require.Equal(t, nt, maxRunnersCount)
//
//	cleanupTask(to)
//}
//
//func TestTaskService_RunMultipleTasksDifferentSpiders(t *testing.T) {
//	to, err := setupTask()
//	require.Nil(t, err)
//
//	// create master Service
//	s, err := NewTaskService(&TaskServiceOptions{
//		IsMaster:        true,
//		PollWaitSeconds: 1,
//	})
//	require.Nil(t, err)
//	defer s.Close()
//
//	// test init
//	go s.Init()
//
//	// test assign multiple tasks for different spiders
//	nt := 3
//	for i := 0; i < nt; i++ {
//		task, err := to.CreateTask(to.spiders[i])
//		require.Nil(t, err)
//		err = s.Assign(task)
//		require.Nil(t, err)
//	}
//	time.Sleep(4 * time.Second)
//	for i := 0; i < nt; i++ {
//		task := to.tasks[i]
//		spider := to.spiders[i]
//		r, err := s.getTaskRunner(task.Id)
//		require.Nil(t, err)
//		require.NotNil(t, r.t)
//		require.Equal(t, spider.Id.Hex(), r.t.SpiderId.Hex())
//	}
//
//	cleanupTask(to)
//}
