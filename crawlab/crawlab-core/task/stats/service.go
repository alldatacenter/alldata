package stats

import (
	config2 "github.com/crawlab-team/crawlab-core/config"
	"github.com/crawlab-team/crawlab-core/entity"
	"github.com/crawlab-team/crawlab-core/interfaces"
	"github.com/crawlab-team/crawlab-core/models/service"
	"github.com/crawlab-team/crawlab-core/node/config"
	"github.com/crawlab-team/crawlab-core/result"
	"github.com/crawlab-team/crawlab-core/task"
	"github.com/crawlab-team/crawlab-core/task/log"
	"github.com/crawlab-team/crawlab-db/mongo"
	"github.com/crawlab-team/go-trace"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.uber.org/dig"
	"sync"
	"time"
)

type Service struct {
	// dependencies
	interfaces.TaskBaseService
	nodeCfgSvc interfaces.NodeConfigService
	modelSvc   service.ModelService

	// internals
	mu             sync.Mutex
	resultServices *entity.TTLMap
	logDriver      log.Driver
}

func (svc *Service) InsertData(id primitive.ObjectID, records ...interface{}) (err error) {
	resultSvc, err := svc.getResultService(id)
	if err != nil {
		return err
	}
	if err := resultSvc.Insert(records...); err != nil {
		return err
	}
	go svc.updateTaskStats(id, len(records))
	return nil
}

func (svc *Service) InsertLogs(id primitive.ObjectID, logs ...string) (err error) {
	return svc.logDriver.WriteLines(id.Hex(), logs)
}

func (svc *Service) getResultService(id primitive.ObjectID) (resultSvc interfaces.ResultService, err error) {
	// attempt to get from cache
	res := svc.resultServices.Load(id.Hex())
	if res != nil {
		// hit in cache
		resultSvc, ok := res.(interfaces.ResultService)
		if ok {
			return resultSvc, nil
		}
	}

	// task
	t, err := svc.modelSvc.GetTaskById(id)
	if err != nil {
		return nil, err
	}

	// result service
	resultSvc, err = result.GetResultService(t.SpiderId)
	if err != nil {
		return nil, err
	}

	// store in cache
	svc.resultServices.Store(id.Hex(), resultSvc)

	return resultSvc, nil
}

func (svc *Service) updateTaskStats(id primitive.ObjectID, resultCount int) {
	_ = mongo.GetMongoCol(interfaces.ModelColNameTaskStat).UpdateId(id, bson.M{
		"$inc": bson.M{
			"result_count": resultCount,
		},
	})
}

func NewTaskStatsService(opts ...Option) (svc2 interfaces.TaskStatsService, err error) {
	// base service
	baseSvc, err := task.NewBaseService()
	if err != nil {
		return nil, trace.TraceError(err)
	}

	// service
	svc := &Service{
		TaskBaseService: baseSvc,
		resultServices:  entity.NewTTLMap(5 * time.Second),
	}

	// apply options
	for _, opt := range opts {
		opt(svc)
	}

	// node config service
	nodeCfgSvc, err := config.NewNodeConfigService()
	if err != nil {
		return nil, trace.TraceError(err)
	}
	svc.nodeCfgSvc = nodeCfgSvc

	// dependency injection
	c := dig.New()
	if err := c.Provide(service.GetService); err != nil {
		return nil, trace.TraceError(err)
	}
	if err := c.Invoke(func(modelSvc service.ModelService) {
		svc.modelSvc = modelSvc
	}); err != nil {
		return nil, trace.TraceError(err)
	}

	// log driver
	svc.logDriver, err = log.GetLogDriver(log.DriverTypeFile, nil)
	if err != nil {
		return nil, err
	}

	return svc, nil
}

var store = sync.Map{}

func GetTaskStatsService(path string, opts ...Option) (svr interfaces.TaskStatsService, err error) {
	if path == "" {
		path = config2.DefaultConfigPath
	}
	opts = append(opts, WithConfigPath(path))
	res, ok := store.Load(path)
	if ok {
		svr, ok = res.(interfaces.TaskStatsService)
		if ok {
			return svr, nil
		}
	}
	svr, err = NewTaskStatsService(opts...)
	if err != nil {
		return nil, err
	}
	store.Store(path, svr)
	return svr, nil
}

func ProvideGetTaskStatsService(path string, opts ...Option) func() (svr interfaces.TaskStatsService, err error) {
	return func() (svr interfaces.TaskStatsService, err error) {
		return GetTaskStatsService(path, opts...)
	}
}
