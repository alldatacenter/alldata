package admin

import (
	"context"
	"github.com/apex/log"
	config2 "github.com/crawlab-team/crawlab-core/config"
	"github.com/crawlab-team/crawlab-core/constants"
	"github.com/crawlab-team/crawlab-core/errors"
	"github.com/crawlab-team/crawlab-core/interfaces"
	"github.com/crawlab-team/crawlab-core/models/models"
	"github.com/crawlab-team/crawlab-core/models/service"
	"github.com/crawlab-team/crawlab-core/node/config"
	"github.com/crawlab-team/crawlab-core/spider/fs"
	"github.com/crawlab-team/crawlab-core/task/scheduler"
	"github.com/crawlab-team/crawlab-core/utils"
	"github.com/crawlab-team/go-trace"
	"github.com/robfig/cron/v3"
	"github.com/spf13/viper"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.uber.org/dig"
	"sync"
	"time"
)

type Service struct {
	// dependencies
	nodeCfgSvc   interfaces.NodeConfigService
	modelSvc     service.ModelService
	schedulerSvc interfaces.TaskSchedulerService
	cron         *cron.Cron
	syncLock     bool

	// settings
	cfgPath string
}

func (svc *Service) GetConfigPath() (path string) {
	return svc.cfgPath
}

func (svc *Service) SetConfigPath(path string) {
	svc.cfgPath = path
}

func (svc *Service) Start() (err error) {
	return svc.SyncGit()
}

func (svc *Service) Schedule(id primitive.ObjectID, opts *interfaces.SpiderRunOptions) (err error) {
	// spider
	s, err := svc.modelSvc.GetSpiderById(id)
	if err != nil {
		return err
	}

	// assign tasks
	if err := svc.scheduleTasks(s, opts); err != nil {
		return err
	}

	return nil
}

func (svc *Service) Clone(id primitive.ObjectID, opts *interfaces.SpiderCloneOptions) (err error) {
	// TODO: implement
	return nil
}

func (svc *Service) Delete(id primitive.ObjectID) (err error) {
	panic("implement me")
}

func (svc *Service) SyncGit() (err error) {
	if _, err = svc.cron.AddFunc("* * * * *", svc.syncGit); err != nil {
		return trace.TraceError(err)
	}
	svc.cron.Start()
	return nil
}

func (svc *Service) scheduleTasks(s *models.Spider, opts *interfaces.SpiderRunOptions) (err error) {
	// main task
	mainTask := &models.Task{
		SpiderId:   s.Id,
		Mode:       opts.Mode,
		NodeIds:    opts.NodeIds,
		Cmd:        opts.Cmd,
		Param:      opts.Param,
		ScheduleId: opts.ScheduleId,
		Priority:   opts.Priority,
		UserId:     opts.UserId,
	}

	// normalize
	if mainTask.Mode == "" {
		mainTask.Mode = s.Mode
	}
	if mainTask.NodeIds == nil {
		mainTask.NodeIds = s.NodeIds
	}
	if mainTask.Cmd == "" {
		mainTask.Cmd = s.Cmd
	}
	if mainTask.Param == "" {
		mainTask.Param = s.Param
	}
	if mainTask.Priority == 0 {
		mainTask.Priority = s.Priority
	}

	if svc.isMultiTask(opts) {
		// multi tasks
		// TODO: implement associated tasks
		//mainTask.HasSub = true
		//if err := delegate.NewModelDelegate(mainTask).Add(); err != nil {
		//	return err
		//}
		nodeIds, err := svc.getNodeIds(opts)
		if err != nil {
			return err
		}
		for _, nodeId := range nodeIds {
			t := &models.Task{
				SpiderId: s.Id,
				// TODO: implement associated tasks
				//ParentId: mainTask.Id,
				Mode:       opts.Mode,
				Cmd:        s.Cmd,
				Param:      opts.Param,
				NodeId:     nodeId,
				ScheduleId: opts.ScheduleId,
				Priority:   opts.Priority,
				UserId:     opts.UserId,
			}
			if err := svc.schedulerSvc.Enqueue(t); err != nil {
				return err
			}
		}
	} else {
		// single task
		nodeIds, err := svc.getNodeIds(opts)
		if err != nil {
			return err
		}
		if len(nodeIds) > 0 {
			mainTask.NodeId = nodeIds[0]
		}
		if err := svc.schedulerSvc.Enqueue(mainTask); err != nil {
			return err
		}
	}

	return nil
}

func (svc *Service) getNodeIds(opts *interfaces.SpiderRunOptions) (nodeIds []primitive.ObjectID, err error) {
	if opts.Mode == constants.RunTypeAllNodes {
		query := bson.M{
			"active":  true,
			"enabled": true,
			"status":  constants.NodeStatusOnline,
		}
		nodes, err := svc.modelSvc.GetNodeList(query, nil)
		if err != nil {
			return nil, err
		}
		for _, node := range nodes {
			nodeIds = append(nodeIds, node.GetId())
		}
	} else if opts.Mode == constants.RunTypeSelectedNodes {
		nodeIds = opts.NodeIds
	}
	return nodeIds, nil
}

func (svc *Service) isMultiTask(opts *interfaces.SpiderRunOptions) (res bool) {
	if opts.Mode == constants.RunTypeAllNodes {
		query := bson.M{
			"active":  true,
			"enabled": true,
			"status":  constants.NodeStatusOnline,
		}
		nodes, err := svc.modelSvc.GetNodeList(query, nil)
		if err != nil {
			trace.PrintError(err)
			return false
		}
		return len(nodes) > 1
	} else if opts.Mode == constants.RunTypeRandom {
		return false
	} else if opts.Mode == constants.RunTypeSelectedNodes {
		return len(opts.NodeIds) > 1
	} else {
		return false
	}
}

func (svc *Service) syncGit() {
	if svc.syncLock {
		log.Infof("[SpiderAdminService] sync git is locked, skip")
		return
	}
	log.Infof("[SpiderAdminService] start to sync git")

	svc.syncLock = true
	defer func() {
		svc.syncLock = false
	}()

	gits, err := svc.modelSvc.GetGitList(bson.M{"auto_pull": true}, nil)
	if err != nil {
		trace.PrintError(err)
		return
	}

	wg := sync.WaitGroup{}
	wg.Add(len(gits))
	for _, g := range gits {
		go func(g models.Git) {
			svc.syncGitOne(g)
			wg.Done()
		}(g)
	}
	wg.Wait()

	log.Infof("[SpiderAdminService] finished sync git")
}

func (svc *Service) syncGitOne(g models.Git) {
	ctx, cancel := context.WithTimeout(context.Background(), 50*time.Second)
	defer cancel()

	// spider fs service
	fsSvc, err := fs.NewSpiderFsService(g.Id)
	if err != nil {
		trace.PrintError(err)
		return
	}

	// git client
	gitClient := fsSvc.GetFsService().GetGitClient()

	// set auth
	utils.InitGitClientAuth(&g, gitClient)

	// check if remote has changes
	ok, err := gitClient.IsRemoteChanged()
	if err != nil {
		trace.PrintError(err)
		return
	}
	if !ok {
		// no change
		return
	}

	// pull and sync to workspace
	go func() {
		if err := gitClient.Pull(); err != nil {
			trace.PrintError(err)
			return
		}
		if err := fsSvc.GetFsService().SyncToFs(); err != nil {
			trace.PrintError(err)
			return
		}
	}()

	// wait for context to end
	<-ctx.Done()
}

func NewSpiderAdminService(opts ...Option) (svc2 interfaces.SpiderAdminService, err error) {
	svc := &Service{
		cfgPath: config2.DefaultConfigPath,
	}

	// apply options
	for _, opt := range opts {
		opt(svc)
	}

	// dependency injection
	c := dig.New()
	if err := c.Provide(config.ProvideConfigService(svc.cfgPath)); err != nil {
		return nil, trace.TraceError(err)
	}
	if err := c.Provide(service.NewService); err != nil {
		return nil, trace.TraceError(err)
	}
	if err := c.Provide(scheduler.ProvideGetTaskSchedulerService(svc.cfgPath)); err != nil {
		return nil, trace.TraceError(err)
	}
	if err := c.Invoke(func(nodeCfgSvc interfaces.NodeConfigService, modelSvc service.ModelService, schedulerSvc interfaces.TaskSchedulerService) {
		svc.nodeCfgSvc = nodeCfgSvc
		svc.modelSvc = modelSvc
		svc.schedulerSvc = schedulerSvc
	}); err != nil {
		return nil, trace.TraceError(err)
	}

	// cron
	svc.cron = cron.New()

	// validate node type
	if !svc.nodeCfgSvc.IsMaster() {
		return nil, trace.TraceError(errors.ErrorSpiderForbidden)
	}

	return svc, nil
}

func ProvideSpiderAdminService(path string, opts ...Option) func() (svc interfaces.SpiderAdminService, err error) {
	if path != "" || path == config2.DefaultConfigPath {
		if viper.GetString("config.path") != "" {
			path = viper.GetString("config.path")
		} else {
			path = config2.DefaultConfigPath
		}
	}
	opts = append(opts, WithConfigPath(path))
	return func() (svc interfaces.SpiderAdminService, err error) {
		return NewSpiderAdminService(opts...)
	}
}

var _service interfaces.SpiderAdminService

func GetSpiderAdminService(opts ...Option) (svc2 interfaces.SpiderAdminService, err error) {
	if _service != nil {
		return _service, nil
	}

	_service, err = NewSpiderAdminService(opts...)
	if err != nil {
		return nil, err
	}

	return _service, nil
}

func ProvideGetSpiderAdminService(path string, opts ...Option) func() (svc interfaces.SpiderAdminService, err error) {
	if path != "" || path == config2.DefaultConfigPath {
		if viper.GetString("config.path") != "" {
			path = viper.GetString("config.path")
		} else {
			path = config2.DefaultConfigPath
		}
	}
	opts = append(opts, WithConfigPath(path))
	return func() (svc interfaces.SpiderAdminService, err error) {
		return GetSpiderAdminService(opts...)
	}
}
