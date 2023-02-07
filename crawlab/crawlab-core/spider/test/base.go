package test

import (
	"github.com/crawlab-team/crawlab-core/fs"
	"github.com/crawlab-team/crawlab-core/interfaces"
	"github.com/crawlab-team/crawlab-core/models/delegate"
	"github.com/crawlab-team/crawlab-core/models/models"
	"github.com/crawlab-team/crawlab-core/models/service"
	ntest "github.com/crawlab-team/crawlab-core/node/test"
	"github.com/crawlab-team/crawlab-core/spider/admin"
	"github.com/crawlab-team/crawlab-core/spider/sync"
	"github.com/crawlab-team/go-trace"
	"go.uber.org/dig"
	"os"
	"testing"
	"time"
)

func init() {
	// remove tmp directory
	if _, err := os.Stat("./tmp"); err == nil {
		if err := os.RemoveAll("./tmp"); err != nil {
			panic(err)
		}
	}
	if err := os.MkdirAll("./tmp", os.ModePerm); err != nil {
		panic(err)
	}

	var err error
	T, err = NewTest()
	if err != nil {
		panic(err)
	}
}

var T *Test

type Test struct {
	// dependencies
	adminSvc      interfaces.SpiderAdminService
	masterSyncSvc interfaces.SpiderSyncService
	masterFsSvc   interfaces.SpiderFsService
	workerSyncSvc interfaces.SpiderSyncService
	workerFsSvc   interfaces.SpiderFsService
	modelSvc      service.ModelService
	fsSvc         interfaces.FsService
	// data
	TestSpider *models.Spider
	ScriptName string
	Script     string
}

// Setup spider fs service test setup
func (t *Test) Setup(t2 *testing.T) {
	_ = os.MkdirAll(t.masterFsSvc.GetWorkspacePath(), os.ModePerm)
	_ = os.MkdirAll(t.workerFsSvc.GetWorkspacePath(), os.ModePerm)
	t2.Cleanup(t.Cleanup)
}

// Cleanup spider fs service test cleanup
func (t *Test) Cleanup() {
	// wait to avoid caching
	time.Sleep(500 * time.Millisecond)
}

func (t *Test) GetMasterFsSvc() interfaces.SpiderFsService {
	return t.masterFsSvc
}

func (t *Test) GetWorkerFsSvc() interfaces.SpiderFsService {
	return t.workerFsSvc
}

func (t *Test) GetMasterSyncSvc() interfaces.SpiderSyncService {
	return t.masterSyncSvc
}

func NewTest() (res *Test, err error) {
	// test
	t := &Test{
		TestSpider: &models.Spider{
			Name: "test_spider",
			Cmd:  "go run main.go",
		},
		ScriptName: "main.go",
		Script: `package main
import "fmt"
func main() {
  fmt.Println("it works")
}`,
	}

	// add spider to db
	if err := delegate.NewModelDelegate(t.TestSpider).Add(); err != nil {
		return nil, err
	}

	// spider service
	c := dig.New()
	if err := c.Provide(service.NewService); err != nil {
		return nil, trace.TraceError(err)
	}
	if err := c.Provide(fs.NewFsService); err != nil {
		return nil, trace.TraceError(err)
	}
	if err := c.Provide(admin.ProvideSpiderAdminService(ntest.T.MasterSvc.GetConfigPath())); err != nil {
		return nil, trace.TraceError(err)
	}
	if err := c.Invoke(func(modelSvc service.ModelService, fsSvc interfaces.FsService, adminSvc interfaces.SpiderAdminService) {
		t.modelSvc = modelSvc
		t.fsSvc = fsSvc
		t.adminSvc = adminSvc
	}); err != nil {
		return nil, trace.TraceError(err)
	}

	// master sync service
	t.masterSyncSvc, err = sync.NewSpiderSyncService(
		sync.WithConfigPath(ntest.T.MasterSvc.GetConfigPath()),
		sync.WithFsPathBase("/fs"),
		sync.WithRepoPathBase("./tmp/test_master_repo"),
		sync.WithWorkspacePathBase("./tmp/test_master_workspace"),
	)
	if err != nil {
		return nil, trace.TraceError(err)
	}

	// master fs service
	t.masterFsSvc, err = t.masterSyncSvc.ForceGetFsService(t.TestSpider.Id)
	if err != nil {
		return nil, trace.TraceError(err)
	}

	// worker sync service
	t.workerSyncSvc, err = sync.NewSpiderSyncService(
		sync.WithConfigPath(ntest.T.WorkerSvc.GetConfigPath()),
		sync.WithFsPathBase("/fs"),
		sync.WithWorkspacePathBase("./tmp/test_worker_workspace"),
	)
	if err != nil {
		return nil, trace.TraceError(err)
	}

	// worker fs service
	t.workerFsSvc, err = t.workerSyncSvc.ForceGetFsService(t.TestSpider.Id)
	if err != nil {
		return nil, trace.TraceError(err)
	}
	_ = os.MkdirAll(t.workerFsSvc.GetWorkspacePath(), os.ModePerm)

	return t, nil
}
