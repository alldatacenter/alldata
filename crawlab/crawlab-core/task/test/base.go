package test

import (
	"context"
	"github.com/crawlab-team/crawlab-core/entity"
	gtest "github.com/crawlab-team/crawlab-core/grpc/test"
	"github.com/crawlab-team/crawlab-core/interfaces"
	"github.com/crawlab-team/crawlab-core/models/delegate"
	"github.com/crawlab-team/crawlab-core/models/models"
	"github.com/crawlab-team/crawlab-core/models/service"
	ntest "github.com/crawlab-team/crawlab-core/node/test"
	"github.com/crawlab-team/crawlab-core/spider/fs"
	stest "github.com/crawlab-team/crawlab-core/spider/test"
	"github.com/crawlab-team/crawlab-core/task/handler"
	"github.com/crawlab-team/crawlab-core/task/scheduler"
	"github.com/crawlab-team/crawlab-core/task/stats"
	grpc "github.com/crawlab-team/crawlab-grpc"
	"github.com/crawlab-team/go-trace"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"go.uber.org/dig"
	"io/ioutil"
	"os"
	"path"
	"testing"
	"time"
)

func init() {
	var err error
	T, err = NewTest()
	if err != nil {
		panic(err)
	}
}

var T *Test

type Test struct {
	// dependencies
	schedulerSvc    interfaces.TaskSchedulerService
	handlerSvc      interfaces.TaskHandlerService
	statsSvc        interfaces.TaskStatsService
	modelSvc        service.ModelService
	masterFsSvc     interfaces.SpiderFsService
	workerFsSvc     interfaces.SpiderFsService
	masterFsSvcLong interfaces.SpiderFsService
	masterSyncSvc   interfaces.SpiderSyncService
	client          interfaces.GrpcClient
	server          interfaces.GrpcServer
	sub             grpc.NodeService_SubscribeClient

	// settings
	ReportInterval    time.Duration
	MaxRunners        int
	ExitWatchDuration time.Duration

	// data
	TestNode        interfaces.Node
	TestSpider      interfaces.Spider
	TestSpiderLong  interfaces.Spider
	TestTaskMessage entity.TaskMessage
	ScriptNameLong  string
	ScriptLong      string
}

func (t *Test) Setup(t2 *testing.T) {
	// add test node
	t.TestNode = t.NewNode()
	if err := delegate.NewModelDelegate(t.TestNode).Add(); err != nil {
		panic(err)
	}

	// add test spider
	if _, err := t.modelSvc.GetSpiderById(t.TestSpider.GetId()); err != nil {
		if err == mongo.ErrNoDocuments {
			if err := delegate.NewModelDelegate(t.TestSpider).Add(); err != nil {
				panic(err)
			}
		} else {
			panic(err)
		}
	}

	// add test spider (long task)
	if _, err := t.modelSvc.GetSpiderById(t.TestSpiderLong.GetId()); err != nil {
		if err == mongo.ErrNoDocuments {
			if err := delegate.NewModelDelegate(t.TestSpiderLong).Add(); err != nil {
				panic(err)
			}
		} else {
			panic(err)
		}
	}

	t2.Cleanup(t.Cleanup)
}

func (t *Test) Cleanup() {
	_ = t.modelSvc.DropAll()
}

func (t *Test) NewNode() (n interfaces.Node) {
	return &models.Node{
		Key:              ntest.T.WorkerSvc.GetConfigService().GetNodeKey(),
		Enabled:          true,
		Active:           true,
		AvailableRunners: t.MaxRunners,
		MaxRunners:       t.MaxRunners,
	}
}

func (t *Test) NewTask() (t2 interfaces.Task) {
	return &models.Task{
		SpiderId: t.TestSpider.GetId(),
	}
}

func (t *Test) NewTaskLong() (t2 interfaces.Task) {
	return &models.Task{
		SpiderId: t.TestSpiderLong.GetId(),
	}
}

func (t *Test) StartMasterWorker() {
	ntest.T.StartMasterWorker()
}

func (t *Test) StopMasterWorker() {
	ntest.T.StopMasterWorker()
}

func NewTest() (res *Test, err error) {
	t := &Test{
		ReportInterval:    1 * time.Second,
		MaxRunners:        20,
		ExitWatchDuration: 5 * time.Second,
		ScriptNameLong:    "main.go",
		ScriptLong: `package main
import "fmt"
import "time"
func main() {
  for i := 0; i < 10; i++ {
    fmt.Println("it works")
    time.Sleep(1 * time.Second)
  }
}`,
	}

	// dependency injection
	c := dig.New()
	if err := c.Provide(scheduler.ProvideTaskSchedulerService(ntest.T.MasterSvc.GetConfigPath(), scheduler.WithInterval(5*time.Second))); err != nil {
		return nil, trace.TraceError(err)
	}
	if err := c.Provide(handler.ProvideGetTaskHandlerService(
		ntest.T.WorkerSvc.GetConfigPath(),
		handler.WithReportInterval(t.ReportInterval),
		handler.WithExitWatchDuration(t.ExitWatchDuration),
	)); err != nil {
		return nil, trace.TraceError(err)
	}
	if err := c.Provide(stats.ProvideGetTaskStatsService(
		ntest.T.MasterSvc.GetConfigPath(),
	)); err != nil {
		return nil, trace.TraceError(err)
	}
	if err := c.Provide(service.NewService); err != nil {
		return nil, trace.TraceError(err)
	}
	if err := c.Invoke(func(
		schedulerSvc interfaces.TaskSchedulerService,
		handlerSvc interfaces.TaskHandlerService,
		statsSvc interfaces.TaskStatsService,
		modelSvc service.ModelService,
	) {
		t.schedulerSvc = schedulerSvc
		t.handlerSvc = handlerSvc
		t.statsSvc = statsSvc
		t.modelSvc = modelSvc
	}); err != nil {
		return nil, trace.TraceError(err)
	}
	t.masterFsSvc = stest.T.GetMasterFsSvc()
	t.workerFsSvc = stest.T.GetWorkerFsSvc()
	t.masterSyncSvc = stest.T.GetMasterSyncSvc()

	// test node
	t.TestNode = t.NewNode()

	// test spider
	t.TestSpider = stest.T.TestSpider

	// add file to spider fs
	filePath := path.Join(t.masterFsSvc.GetWorkspacePath(), stest.T.ScriptName)
	if err := ioutil.WriteFile(filePath, []byte(stest.T.Script), os.ModePerm); err != nil {
		panic(err)
	}
	if err := t.masterFsSvc.GetFsService().Commit("initial commit"); err != nil {
		return nil, err
	}
	if err := t.masterSyncSvc.SyncToFs(t.TestSpider.GetId()); err != nil {
		panic(err)
	}

	// long task spider
	t.TestSpiderLong = &models.Spider{
		Id:  primitive.NewObjectID(),
		Cmd: "go run main.go",
	}
	if err := delegate.NewModelDelegate(t.TestSpiderLong).Add(); err != nil {
		return nil, err
	}

	// long task spider dependency injection
	t.masterFsSvcLong, err = fs.GetSpiderFsService(t.TestSpiderLong.GetId(), fs.WithConfigPath(ntest.T.MasterSvc.GetConfigPath()))
	if err != nil {
		return nil, err
	}

	// add file (long task) to spider fs
	filePathLong := path.Join(t.masterFsSvcLong.GetWorkspacePath(), t.ScriptNameLong)
	if err := ioutil.WriteFile(filePathLong, []byte(t.ScriptLong), os.ModePerm); err != nil {
		return nil, trace.TraceError(err)
	}
	if err := t.masterFsSvcLong.GetFsService().Commit("initial commit"); err != nil {
		return nil, err
	}
	if err := t.masterSyncSvc.SyncToFs(t.TestSpiderLong.GetId()); err != nil {
		return nil, trace.TraceError(err)
	}

	// grpc server/client
	grpcT, _ := gtest.NewTest()
	t.server = grpcT.Server
	t.client = grpcT.Client
	if err := t.client.Start(); err != nil {
		return nil, err
	}
	req := &grpc.Request{NodeKey: t.TestNode.GetKey()}
	t.sub, err = t.client.GetNodeClient().Subscribe(context.Background(), req)
	if err != nil {
		return nil, err
	}

	return t, nil
}
