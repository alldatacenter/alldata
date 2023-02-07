package scheduler

import (
	"fmt"
	"github.com/apex/log"
	config2 "github.com/crawlab-team/crawlab-core/config"
	"github.com/crawlab-team/crawlab-core/constants"
	"github.com/crawlab-team/crawlab-core/errors"
	"github.com/crawlab-team/crawlab-core/grpc/server"
	"github.com/crawlab-team/crawlab-core/interfaces"
	"github.com/crawlab-team/crawlab-core/models/client"
	"github.com/crawlab-team/crawlab-core/models/delegate"
	"github.com/crawlab-team/crawlab-core/models/models"
	"github.com/crawlab-team/crawlab-core/models/service"
	"github.com/crawlab-team/crawlab-core/node/config"
	"github.com/crawlab-team/crawlab-core/task"
	"github.com/crawlab-team/crawlab-core/task/handler"
	"github.com/crawlab-team/crawlab-core/utils"
	"github.com/crawlab-team/crawlab-db/mongo"
	grpc "github.com/crawlab-team/crawlab-grpc"
	"github.com/crawlab-team/go-trace"
	"github.com/joeshaw/multierror"
	"github.com/spf13/viper"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	mongo2 "go.mongodb.org/mongo-driver/mongo"
	"go.uber.org/dig"
	"math/rand"
	"sync"
	"time"
)

type Service struct {
	// dependencies
	interfaces.TaskBaseService
	nodeCfgSvc interfaces.NodeConfigService
	modelSvc   service.ModelService
	svr        interfaces.GrpcServer
	handlerSvc interfaces.TaskHandlerService

	// settings
	interval time.Duration
}

func (svc *Service) Start() {
	go svc.initTaskStatus()
	//go svc.DequeueAndSchedule()
	svc.Wait()
	svc.Stop()
}

func (svc *Service) Enqueue(t interfaces.Task) (err error) {
	// set task status
	t.SetStatus(constants.TaskStatusPending)

	// user
	var u *models.User
	if !t.GetUserId().IsZero() {
		u, _ = svc.modelSvc.GetUserById(t.GetUserId())
	}

	// add task
	if err = delegate.NewModelDelegate(t, u).Add(); err != nil {
		return err
	}

	// task queue item
	tq := &models.TaskQueueItem{
		Id:       t.GetId(),
		Priority: t.GetPriority(),
		NodeId:   t.GetNodeId(),
	}

	// task stat
	ts := &models.TaskStat{
		Id:       t.GetId(),
		CreateTs: time.Now(),
	}

	// enqueue task
	_, err = mongo.GetMongoCol(interfaces.ModelColNameTaskQueue).Insert(tq)
	if err != nil {
		return trace.TraceError(err)
	}

	// add task stat
	_, err = mongo.GetMongoCol(interfaces.ModelColNameTaskStat).Insert(ts)
	if err != nil {
		return trace.TraceError(err)
	}

	// success
	return nil
}

func (svc *Service) DequeueAndSchedule() {
	for {
		if svc.IsStopped() {
			return
		}

		// wait
		time.Sleep(svc.interval)

		if err := mongo.RunTransaction(func(sc mongo2.SessionContext) error {
			// dequeue tasks
			tasks, err := svc.Dequeue()
			if err != nil {
				return trace.TraceError(err)
			}

			// skip if no tasks available
			if tasks == nil || len(tasks) == 0 {
				return nil
			}

			// schedule tasks
			if err := svc.Schedule(tasks); err != nil {
				return trace.TraceError(err)
			}

			return nil
		}); err != nil {
			trace.PrintError(err)
		}
	}
}

func (svc *Service) Dequeue() (tasks []interfaces.Task, err error) {
	// get task queue items
	tqList, err := svc.getTaskQueueItems()
	if err != nil {
		return nil, err
	}
	if tqList == nil {
		return nil, nil
	}

	// match resources
	tasks, nodesMap, err := svc.matchResources(tqList)
	if err != nil {
		return nil, err
	}
	if tasks == nil {
		return nil, nil
	}

	// update resources
	if err := svc.updateResources(nodesMap); err != nil {
		return nil, err
	}

	// update tasks
	if err := svc.updateTasks(tasks); err != nil {
		return nil, err
	}

	// delete task queue items
	if err := svc.deleteTaskQueueItems(tasks); err != nil {
		return nil, err
	}

	return tasks, nil
}

func (svc *Service) Schedule(tasks []interfaces.Task) (err error) {
	var e multierror.Errors

	// nodes cache
	nodesCache := sync.Map{}

	// wait group
	wg := sync.WaitGroup{}
	wg.Add(len(tasks))

	// iterate tasks and execute each of them
	for _, t := range tasks {
		go func(t interfaces.Task) {
			var err error

			// node of the task
			var n interfaces.Node
			res, ok := nodesCache.Load(t.GetNodeId())
			if !ok {
				// not exists in cache
				n, err = svc.modelSvc.GetNodeById(t.GetNodeId())
				if err == nil {
					nodesCache.Store(n.GetId(), n)
				}
			} else {
				// exists in cache
				n, ok = res.(interfaces.Node)
				if !ok {
					err = errors.ErrorTaskInvalidType
				}
			}
			if err != nil {
				e = append(e, err)
				svc.handleTaskError(n, t, err)
				wg.Done()
				return
			}

			// schedule task
			if n.GetIsMaster() {
				// execute task on master
				err = svc.handlerSvc.Run(t.GetId())
			} else {
				// send to execute task on worker nodes
				err = svc.svr.SendStreamMessageWithData("node:"+n.GetKey(), grpc.StreamMessageCode_RUN_TASK, t)
			}
			if err != nil {
				e = append(e, err)
				svc.handleTaskError(n, t, err)
				wg.Done()
				return
			}

			// success
			wg.Done()
		}(t)
	}

	// wait
	wg.Wait()

	return e.Err()
}

func (svc *Service) Cancel(id primitive.ObjectID, args ...interface{}) (err error) {
	// user
	u := utils.GetUserFromArgs(args...)

	// task
	t, err := svc.modelSvc.GetTaskById(id)
	if err != nil {
		return trace.TraceError(err)
	}

	// set status of pending tasks as "cancelled" and remove from task item queue
	if t.Status == constants.TaskStatusPending {
		// remove from task item queue
		if err := mongo.GetMongoCol(interfaces.ModelColNameTaskQueue).DeleteId(t.GetId()); err != nil {
			trace.PrintError(err)
		}

		// set task status as "cancelled"
		return svc.SaveTask(t, constants.TaskStatusCancelled)
	}

	// whether task is running on master node
	isMasterTask, err := svc.isMasterNode(t)
	if err != nil {
		// when error, force status being set as "cancelled"
		return svc.SaveTask(t, constants.TaskStatusCancelled)
	}

	// node
	n, err := svc.modelSvc.GetNodeById(t.GetNodeId())
	if err != nil {
		// when error, force status being set as "cancelled"
		trace.PrintError(err)
		return svc.SaveTask(t, constants.TaskStatusCancelled)
	}

	if isMasterTask {
		// cancel task on master
		if err := svc.handlerSvc.Cancel(id); err != nil {
			// cancel failed, force status being set as "cancelled"
			trace.PrintError(err)
			t, err := svc.modelSvc.GetTaskById(id)
			if err != nil {
				return err
			}
			t.Status = constants.TaskStatusCancelled
			return delegate.NewModelDelegate(t, u).Save()
		}
		// cancel success
		return nil
	} else {
		// send to cancel task on worker nodes
		if err := svc.svr.SendStreamMessageWithData("node:"+n.GetKey(), grpc.StreamMessageCode_CANCEL_TASK, t); err != nil {
			// cancel failed, force status being set as "cancelled"
			t.Status = constants.TaskStatusCancelled
			return delegate.NewModelDelegate(t, u).Save()
		}
		// cancel success
		return nil
	}
}

func (svc *Service) SetInterval(interval time.Duration) {
	svc.interval = interval
}

func (svc *Service) getTaskQueueItems() (tqList []models.TaskQueueItem, err error) {
	opts := &mongo.FindOptions{
		Sort: bson.D{
			{"p", 1},
			{"_id", 1},
		},
	}
	if err := mongo.GetMongoCol(interfaces.ModelColNameTaskQueue).Find(nil, opts).All(&tqList); err != nil {
		if err == mongo2.ErrNoDocuments {
			return nil, nil
		}
		return nil, err
	}
	return tqList, nil
}

func (svc *Service) getResourcesAndNodesMap() (resources map[string]models.Node, nodesMap map[primitive.ObjectID]models.Node, err error) {
	nodesMap = map[primitive.ObjectID]models.Node{}
	resources = map[string]models.Node{}
	query := bson.M{
		// enabled: true
		"enabled": true,
		// active: true
		"active": true,
		// available_runners > 0
		"available_runners": bson.M{
			"$gt": 0,
		},
	}
	nodes, err := svc.modelSvc.GetNodeList(query, nil)
	if err != nil {
		if err == mongo2.ErrNoDocuments {
			return nil, nil, nil
		}
		return nil, nil, err
	}
	for _, n := range nodes {
		nodesMap[n.Id] = n
		for i := 0; i < n.AvailableRunners; i++ {
			key := fmt.Sprintf("%s:%d", n.Id.Hex(), i)
			resources[key] = n
		}
	}
	return resources, nodesMap, nil
}

func (svc *Service) matchResources(tqList []models.TaskQueueItem) (tasks []interfaces.Task, nodesMap map[primitive.ObjectID]models.Node, err error) {
	// get resources and nodes map
	resources, nodesMap, err := svc.getResourcesAndNodesMap()
	if err != nil {
		return nil, nil, err
	}
	if resources == nil || len(resources) == 0 {
		return nil, nil, nil
	}

	// resources list
	var resourcesList []models.Node
	for _, r := range resources {
		resourcesList = append(resourcesList, r)
	}

	// shuffle resources list
	rand.Seed(time.Now().Unix())
	rand.Shuffle(len(resourcesList), func(i, j int) {
		resourcesList[i], resourcesList[j] = resourcesList[j], resourcesList[i]
	})

	// iterate task queue items
	for _, tq := range tqList {
		// task
		t, err := svc.modelSvc.GetTaskById(tq.GetId())
		if err != nil {
			// remove task queue item if it is not found in tasks
			_ = mongo.GetMongoCol(interfaces.ModelColNameTaskQueue).DeleteId(tq.GetId())

			// set task status as abnormal
			t.Status = constants.TaskStatusAbnormal
			t.Error = err.Error()
			_ = delegate.NewModelDelegate(t, nil).Save()
			continue
		}

		// iterate shuffled resources to match a resource
		for i, r := range resourcesList {
			// If node id is unset or node id of task matches with resource id (node id),
			// assign corresponding resource id to the task
			if t.GetNodeId().IsZero() ||
				t.GetNodeId() == r.GetId() {
				// assign resource id
				t.NodeId = r.GetId()

				// append to tasks
				tasks = append(tasks, t)

				// delete from resources list
				resourcesList = append(resourcesList[:i], resourcesList[(i+1):]...)

				// decrement available runners
				n := nodesMap[r.GetId()]
				n.DecrementAvailableRunners()

				// break loop
				break
			}
		}
	}

	return tasks, nodesMap, nil
}

func (svc *Service) updateResources(nodesMap map[primitive.ObjectID]models.Node) (err error) {
	for _, n := range nodesMap {
		if err := delegate.NewModelNodeDelegate(&n).Save(); err != nil {
			return err
		}
	}
	return nil
}

func (svc *Service) updateTasks(tasks []interfaces.Task) (err error) {
	for _, t := range tasks {
		// save task with node id
		if err := delegate.NewModelDelegate(t).Save(); err != nil {
			return err
		}
	}
	return nil
}

func (svc *Service) deleteTaskQueueItems(tasks []interfaces.Task) (err error) {
	for _, t := range tasks {
		if err := mongo.GetMongoCol(interfaces.ModelColNameTaskQueue).DeleteId(t.GetId()); err != nil {
			log.Warnf("task[id: %s] missing task queue: %s", t.GetId(), err.Error())
			continue
		}
	}
	return nil
}

func (svc *Service) handleTaskError(n interfaces.Node, t interfaces.Task, err error) {
	trace.PrintError(err)
	t.SetStatus(constants.TaskStatusError)
	t.SetError(err.Error())
	if n.GetIsMaster() {
		_ = delegate.NewModelDelegate(t).Save()
	} else {
		_ = client.NewModelDelegate(t).Save()
	}
}

// initTaskStatus initialize task status of existing tasks
func (svc *Service) initTaskStatus() {
	// set status of running tasks as TaskStatusAbnormal
	runningTasks, err := svc.modelSvc.GetTaskList(bson.M{
		"status": constants.TaskStatusRunning,
	}, nil)
	if err != nil {
		if err == mongo2.ErrNoDocuments {
			return
		}
		trace.PrintError(err)
	}
	for _, t := range runningTasks {
		go func(t *models.Task) {
			if err := svc.SaveTask(t, constants.TaskStatusAbnormal); err != nil {
				trace.PrintError(err)
			}
		}(&t)
	}
}

func (svc *Service) isMasterNode(t *models.Task) (ok bool, err error) {
	if t.GetNodeId().IsZero() {
		return false, trace.TraceError(errors.ErrorTaskNoNodeId)
	}
	n, err := svc.modelSvc.GetNodeById(t.GetNodeId())
	if err != nil {
		if err == mongo2.ErrNoDocuments {
			return false, trace.TraceError(errors.ErrorTaskNodeNotFound)
		}
		return false, trace.TraceError(err)
	}
	return n.IsMaster, nil
}

func NewTaskSchedulerService(opts ...Option) (svc2 interfaces.TaskSchedulerService, err error) {
	// base service
	baseSvc, err := task.NewBaseService()
	if err != nil {
		return nil, trace.TraceError(err)
	}

	// service
	svc := &Service{
		TaskBaseService: baseSvc,
		interval:        5 * time.Second,
	}

	// apply options
	for _, opt := range opts {
		opt(svc)
	}

	// dependency injection
	c := dig.New()
	if err := c.Provide(config.ProvideConfigService(svc.GetConfigPath())); err != nil {
		return nil, trace.TraceError(err)
	}
	if err := c.Invoke(func(nodeCfgSvc interfaces.NodeConfigService) {
		svc.nodeCfgSvc = nodeCfgSvc
	}); err != nil {
		return nil, trace.TraceError(err)
	}
	if err := c.Provide(service.NewService); err != nil {
		return nil, trace.TraceError(err)
	}
	if err := c.Provide(server.ProvideGetServer(svc.GetConfigPath())); err != nil {
		return nil, trace.TraceError(err)
	}
	if err := c.Provide(handler.ProvideGetTaskHandlerService(svc.GetConfigPath())); err != nil {
		return nil, trace.TraceError(err)
	}
	if err := c.Invoke(func(
		modelSvc service.ModelService,
		svr interfaces.GrpcServer,
		handlerSvc interfaces.TaskHandlerService,
	) {
		svc.modelSvc = modelSvc
		svc.svr = svr
		svc.handlerSvc = handlerSvc
	}); err != nil {
		return nil, trace.TraceError(err)
	}

	return svc, nil
}

func ProvideTaskSchedulerService(path string, opts ...Option) func() (svc interfaces.TaskSchedulerService, err error) {
	opts = append(opts, WithConfigPath(path))
	return func() (svc interfaces.TaskSchedulerService, err error) {
		return NewTaskSchedulerService(opts...)
	}
}

var store = sync.Map{}

func GetTaskSchedulerService(path string, opts ...Option) (svr interfaces.TaskSchedulerService, err error) {
	if path == "" {
		path = config2.DefaultConfigPath
	}
	opts = append(opts, WithConfigPath(path))
	res, ok := store.Load(path)
	if ok {
		svr, ok = res.(interfaces.TaskSchedulerService)
		if ok {
			return svr, nil
		}
	}
	svr, err = NewTaskSchedulerService(opts...)
	if err != nil {
		return nil, err
	}
	store.Store(path, svr)
	return svr, nil
}

func ProvideGetTaskSchedulerService(path string, opts ...Option) func() (svr interfaces.TaskSchedulerService, err error) {
	// path
	if path != "" || path == config2.DefaultConfigPath {
		if viper.GetString("config.path") != "" {
			path = viper.GetString("config.path")
		} else {
			path = config2.DefaultConfigPath
		}
	}
	opts = append(opts, WithConfigPath(path))

	// interval
	intervalSeconds := viper.GetInt("task.scheduler.interval")
	if intervalSeconds > 0 {
		opts = append(opts, WithInterval(time.Duration(intervalSeconds)*time.Second))
	}

	return func() (svr interfaces.TaskSchedulerService, err error) {
		return GetTaskSchedulerService(path, opts...)
	}
}
