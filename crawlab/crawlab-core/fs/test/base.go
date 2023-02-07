package test

import (
	"github.com/crawlab-team/crawlab-core/fs"
	"github.com/crawlab-team/crawlab-core/interfaces"
	"github.com/crawlab-team/crawlab-core/node/test"
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
	masterFsSvc interfaces.FsService
	workerFsSvc interfaces.FsService
}

// Setup fs service test setup
func (t *Test) Setup(t2 *testing.T) {
	t2.Cleanup(t.Cleanup)
}

// Cleanup fs service test cleanup
func (t *Test) Cleanup() {
	// remove remote directory
	svc, err := fs.NewFsService(fs.WithFsPath("/test"))
	if err != nil {
		panic(err)
	}
	ok, err := svc.GetFs().Exists("/test")
	if err != nil {
		panic(err)
	}
	if ok {
		if err := svc.GetFs().DeleteDir("/test"); err != nil {
			panic(err)
		}
	}

	// remove tmp directory
	//if _, err := os.Stat("./tmp"); err == nil {
	//	if err := os.RemoveAll("./tmp"); err != nil {
	//		panic(err)
	//	}
	//}
	//if err := os.MkdirAll("./tmp", os.ModePerm); err != nil {
	//	panic(err)
	//}

	// node service cleanup
	test.T.Cleanup()

	// wait to avoid caching
	time.Sleep(500 * time.Millisecond)
}

func NewTest() (res *Test, err error) {
	// test
	t := &Test{}

	// master fs service
	t.masterFsSvc, err = fs.NewFsService(
		fs.WithConfigPath(test.T.MasterSvc.GetConfigPath()),
		fs.WithFsPath("/test"),
		fs.WithRepoPath("./tmp/test_master_repo"),
		fs.WithWorkspacePath("./tmp/test_master_workspace"),
	)
	if err != nil {
		return nil, err
	}

	// worker fs service
	t.workerFsSvc, err = fs.NewFsService(
		fs.WithConfigPath(test.T.WorkerSvc.GetConfigPath()),
		fs.WithFsPath("/test"),
		fs.WithRepoPath("./tmp/test_worker_repo"),
		fs.WithWorkspacePath("./tmp/test_worker_workspace"),
	)
	if err != nil {
		return nil, err
	}

	return t, nil
}
