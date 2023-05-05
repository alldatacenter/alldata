// Copyright (c) 2021 Terminus, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package compensator

import (
	"context"
	"fmt"
	"reflect"
	"time"

	v3 "github.com/coreos/etcd/clientv3"
	"github.com/pkg/errors"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-infra/base/servicehub"
	"github.com/erda-project/erda-infra/providers/etcd"
	"github.com/erda-project/erda-infra/providers/mysqlxorm"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/conf"
	"github.com/erda-project/erda/internal/tools/pipeline/dbclient"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/cron/db"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/edgepipeline_register"
	"github.com/erda-project/erda/internal/tools/pipeline/providers/leaderworker"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/jsonstore"
	"github.com/erda-project/erda/pkg/limit_sync_group"
	"github.com/erda-project/erda/pkg/parser/pipelineyml"
	"github.com/erda-project/erda/pkg/strutil"
)

const (
	waitTimeIfQueryDBError   = time.Minute
	indexFirst               = 0
	etcdNeedCompensatePrefix = "/devops/pipeline/compensate/"
)

type config struct {
}

// +provider
type provider struct {
	Log                  logs.Logger
	LeaderWorker         leaderworker.Interface `autowired:"leader-worker"`
	ETCD                 etcd.Interface         // autowired
	EtcdClient           *v3.Client
	MySQL                mysqlxorm.Interface `autowired:"mysql-xorm"`
	EdgePipelineRegister edgepipeline_register.Interface

	jsonStore    jsonstore.JsonStore
	dbClient     *dbclient.Client
	cronDBClient *db.Client

	// TODO remove
	pipelineFunc PipelineFunc
}

func (p *provider) WithPipelineFunc(pipelineFunc PipelineFunc) {
	p.pipelineFunc = pipelineFunc
}

func (p *provider) Init(ctx servicehub.Context) error {
	p.dbClient = &dbclient.Client{Engine: p.MySQL.DB()}
	p.cronDBClient = &db.Client{Interface: p.MySQL}
	jsonStore, err := jsonstore.New()
	if err != nil {
		return err
	}
	p.jsonStore = jsonStore

	return nil
}

func (p *provider) Run(ctx context.Context) error {
	p.LeaderWorker.OnLeader(func(ctx context.Context) {
		p.ContinueCompensate(ctx)
	})
	return nil
}

func (p *provider) PipelineCronCompensate(ctx context.Context, pipelineID uint64) {
	pipelineWithTasks, err := p.dbClient.GetPipelineWithTasks(pipelineID)
	if err != nil {
		p.Log.Errorf("failed to do pipeline cron compensate, failed to get pipelineWithTasks, pipelineID: %d, err: %v", pipelineID, err)
		return
	}

	if pipelineWithTasks == nil || pipelineWithTasks.Pipeline == nil || pipelineWithTasks.Pipeline.CronID == nil {
		return
	}

	// Monitor whether the compensation is blocked during execution, and immediately compensate if it is blocked,
	// obtain the id of the cron of the current pipeline in etcd, and then immediately delete the corresponding etcd value
	notFound, err := p.jsonStore.Notfound(ctx, fmt.Sprint(etcdNeedCompensatePrefix, *pipelineWithTasks.Pipeline.CronID))
	if err != nil {
		p.Log.Errorf("can not get cronID: %d, err: %v", *pipelineWithTasks.Pipeline.CronID, err)
		return
	}

	if !notFound {
		p.Log.Infof("ready to Compensate, cronID: %d", *pipelineWithTasks.Pipeline.CronID)

		// perform the compensation operation
		if err := p.doNonExecuteCompensateByCronID(ctx, *pipelineWithTasks.Pipeline.CronID); err != nil {
			p.Log.Infof("to Compensate error, cronID: %d, err : %v",
				*pipelineWithTasks.Pipeline.CronID, err)
		}

		// remove cronID
		if err := p.jsonStore.Remove(ctx, fmt.Sprint(etcdNeedCompensatePrefix, *pipelineWithTasks.Pipeline.CronID), nil); err != nil {
			p.Log.Infof("can not delete etcd key, key: %s, cronID: %d, err : %v",
				fmt.Sprint(etcdNeedCompensatePrefix, *pipelineWithTasks.Pipeline.CronID), *pipelineWithTasks.Pipeline.CronID, err)
		}
	}
}

func getCronCompensateInterval(interval int64) time.Duration {
	return time.Duration(interval) * time.Minute
}

func getCronInterruptCompensateInterval(interval int64) time.Duration {
	return time.Duration(interval) * time.Minute * 2
}

func (p *provider) ContinueCompensate(ctx context.Context) {
	p.Log.Info("cron compensator: start")
	// Execute Policy Compensation Scheduled Tasks
	ticker := time.NewTicker(getCronCompensateInterval(conf.CronCompensateTimeMinute()))
	// Interrupt Compensation Scheduled Task
	interruptTicker := time.NewTicker(getCronInterruptCompensateInterval(conf.CronCompensateTimeMinute()))

	// Execute an interruption compensation first when the project starts
	p.traverseDoCompensate(ctx, p.doInterruptCompensate, true)

	for {
		select {
		case <-ctx.Done():
			// stop
			p.Log.Info("stop cron compensate, received cancel signal from channel")
			ticker.Stop()
			interruptTicker.Stop()
			return
		case <-interruptTicker.C:
			// Why synchronization is used for interrupt compensation here,
			// because data will be added here,
			// and it is not yet confirmed whether there is idempotent, so synchronize first
			p.traverseDoCompensate(ctx, p.doInterruptCompensate, true)
		case <-ticker.C:
			// Because the compensation (strategy) is not executed,
			// the pipeline used to execute the pipeline is idempotent internally,
			// that is, only one pipeline is executing at the same time
			p.traverseDoCompensate(ctx, p.doStrategyCompensate, false)
		}
	}
}

func (p *provider) doInterruptCompensate(ctx context.Context, pc db.PipelineCron) {
	err := p.cronInterruptCompensate(ctx, pc)
	if err != nil {
		p.Log.Errorf("failed to do interrupt-compensate, cronID: %d, err: %v", pc.ID, err)
	}
}

func (p *provider) doStrategyCompensate(ctx context.Context, pc db.PipelineCron) {
	err := p.cronNonExecuteCompensate(ctx, pc)
	if err != nil {
		p.Log.Errorf("failed to do notexecute-compensate, cronID: %d, err: %v", pc.ID, err)
	}
}

func (p *provider) traverseDoCompensate(ctx context.Context, doCompensate func(ctx context.Context, cron db.PipelineCron), sync bool) {

	if doCompensate == nil {
		return
	}

	// get all enabled crons
	enabledCrons, err := p.cronDBClient.ListPipelineCrons(&[]bool{true}[0])
	if err != nil {
		p.Log.Errorf("failed to list enabled pipeline crons from db, try again later, err: %v", err)
		time.Sleep(waitTimeIfQueryDBError)
		return
	}

	group := limit_sync_group.NewSemaphore(int(conf.CronCompensateConcurrentNumber()))
	for _, pc := range enabledCrons {
		if p.isCronShouldBeIgnored(pc) {
			triggerTime := time.Now()
			p.Log.Warnf("crond compensator: pipelineCronID: %d, triggered compensate but ignored, triggerTime: %s, cronStartFrom: %s",
				pc.ID, triggerTime, *pc.Extra.CronStartFrom)
			continue
		}

		// center should skip compensate. do compensate at edge side.
		ok := p.EdgePipelineRegister.CanProxyToEdge(pc.PipelineSource, pc.Extra.ClusterName)
		if ok {
			continue
		}

		if sync {
			doCompensate(ctx, pc)
		} else {
			group.Add(1)
			go func(pc db.PipelineCron) {
				defer group.Done()
				doCompensate(ctx, pc)
			}(pc)
		}
	}
	group.Wait()
}

// cronInterruptCompensate Timing interrupt compensation
func (p *provider) cronInterruptCompensate(ctx context.Context, pc db.PipelineCron) error {

	// Calculate interrupt compensation start time
	beforeCompensateFromTime := getCompensateFromTime(pc)

	// current time minus a certain time，prevent conflicts with cron create pipeline
	now := time.Unix(time.Now().Unix(), 0)
	var thisCompensateFromTime = now.Add(time.Second * -time.Duration(conf.CronFailureCreateIntervalCompensateTimeSecond()))

	// Use cron expr to calculate all required trigger times from the starting compensation point
	needTriggerTimes, err := pipelineyml.ListNextCronTime(pc.CronExpr,
		pipelineyml.WithCronStartEndTime(&beforeCompensateFromTime, &thisCompensateFromTime),
		pipelineyml.WithListNextScheduleCount(100),
	)
	if err != nil {
		return errors.Errorf("[alert] failed to list next crontimes, cronID: %d, err: %v", pc.ID, err)
	}

	if len(needTriggerTimes) > 0 {
		// Pipeline search record created based on createlyname + source
		result, err := p.dbClient.PageListPipelines(apistructs.PipelinePageListRequest{
			Sources:          []apistructs.PipelineSource{pc.PipelineSource},
			YmlNames:         []string{pc.PipelineYmlName},
			TriggerModes:     []apistructs.PipelineTriggerMode{apistructs.PipelineTriggerModeCron},
			StartTimeCreated: beforeCompensateFromTime.Add(time.Second * -time.Duration(conf.CronFailureCreateIntervalCompensateTimeSecond())),
			EndTimeCreated:   thisCompensateFromTime.Add(time.Second * time.Duration(conf.CronFailureCreateIntervalCompensateTimeSecond())),
			PageNum:          1,
			PageSize:         100,
			LargePageSize:    true,
		})
		if err != nil {
			return errors.Errorf("[alert] failed to list existPipelines, cronID: %d, err: %v", pc.ID, err)
		}
		existPipelines := result.Pipelines

		// Convert to map for query
		existPipelinesMap := make(map[time.Time]spec.Pipeline, len(existPipelines))
		for _, p := range existPipelines {
			existPipelinesMap[getTriggeredTime(p)] = p
		}

		// Traverse needTriggerTimes. If it is not created, you need to interrupt the creation of compensation
		for _, ntt := range needTriggerTimes {
			pipeline, ok := existPipelinesMap[ntt]
			if ok {
				p.Log.Infof("no need do interrupt-compensate, cronID: %d, triggerTime: %v, exist pipelineID: %d", pc.ID, ntt, pipeline.ID)
				continue
			}
			p.Log.Infof("need do interrupt-compensate, cronID: %d, triggerTime: %v", pc.ID, ntt)
			// create
			created, err := p.createCronCompensatePipeline(ctx, pc, ntt)
			if err != nil {
				p.Log.Errorf("failed to do interrupt-compensate, cronID: %d, triggerTime: %v, err: %v", pc.ID, ntt, err)
				continue
			}
			p.Log.Infof("success to do interrupt-compensate, cronID: %d, triggerTime: %v, createdPipelineID: %d", pc.ID, ntt, created.ID)
		}
	}

	// After the interrupt compensation is completed, the thisCompensateFromTime field of cron needs to be updated
	pc.Extra.LastCompensateAt = &thisCompensateFromTime
	// If the compensator is empty, it indicates that it is an old cron, and the default configuration will be used automatically
	if pc.Extra.Compensator == nil {
		pc.Extra.Compensator = &apistructs.CronCompensator{
			Enable:               pipelineyml.DefaultCronCompensator.Enable,
			LatestFirst:          pipelineyml.DefaultCronCompensator.LatestFirst,
			StopIfLatterExecuted: pipelineyml.DefaultCronCompensator.StopIfLatterExecuted,
		}
	}
	if err := p.cronDBClient.UpdatePipelineCron(pc.ID, &pc); err != nil {
		return errors.Errorf("failed to update pipelineCron for lastCompensateAt field, err: %v", err)
	}

	return nil
}

func (p *provider) doNonExecuteCompensateByCronID(ctx context.Context, id uint64) error {
	cron, found, err := p.cronDBClient.GetPipelineCron(id)
	if err != nil {
		return err
	}
	if !found {
		return fmt.Errorf("not found")
	}

	return p.cronNonExecuteCompensate(ctx, cron)
}

// cronNonExecuteCompensate timing compensation not performed
// Only within one day
func (p *provider) cronNonExecuteCompensate(ctx context.Context, pc db.PipelineCron) error {

	// Notexecute compensate is not enabled, exit
	if pc.Enable == nil || *pc.Enable == false || pc.Extra.Compensator == nil || pc.Extra.Compensator.Enable == false {
		return nil
	}

	now := time.Unix(time.Now().Unix(), 0)
	oneDayBeforeNow := now.AddDate(0, 0, -1)

	// Get to execute list
	// Search the created pipeline records according to source + ymlname + ID
	// Here, why take 10 by ID? Because when the expression granularity is very small, a lot of data will be lost. However, the ID order of interrupt compensation is different from that of execution
	// Therefore, neutralization first takes 10 items in positive or reverse order with ID, and then doCronCompensate selects the most suitable time for execution according to the specific execution time of the 10 items
	// When the time granularity is very large, in essence, one is OK
	request := apistructs.PipelinePageListRequest{
		Sources:          []apistructs.PipelineSource{pc.PipelineSource},
		YmlNames:         []string{pc.PipelineYmlName},
		Statuses:         []string{apistructs.PipelineStatusAnalyzed.String()},
		TriggerModes:     []apistructs.PipelineTriggerMode{apistructs.PipelineTriggerModeCron},
		StartTimeCreated: oneDayBeforeNow,
		EndTimeCreated:   now,
		PageNum:          1,
		PageSize:         10,
		LargePageSize:    true,
	}

	if (*pc.Extra.Compensator).LatestFirst {
		request.DescCols = []string{apistructs.PipelinePageListRequestIdColumn}
	} else {
		request.AscCols = []string{apistructs.PipelinePageListRequestIdColumn}
	}

	result, err := p.dbClient.PageListPipelines(request)
	if err != nil {
		return errors.Errorf("failed to list notexecute pipelines, cronID: %d, err: %v", pc.ID, err)
	}
	existPipelines := result.Pipelines
	return p.doCronCompensate(ctx, *pc.Extra.Compensator, existPipelines, pc)
}

func (p *provider) doCronCompensate(ctx context.Context, compensator apistructs.CronCompensator, notRunPipelines []spec.Pipeline, pipelineCron db.PipelineCron) error {
	var order string

	if len(notRunPipelines) <= 0 {
		return nil
	}

	// Select the most suitable time point from the non execution in good order according to the strategy
	if compensator.LatestFirst {
		order = "DESC"
	} else {
		order = "ASC"
	}
	// doCronCompensate selects the most suitable time for execution according to the specific execution time of Article 10
	firstOrLastPipeline := orderByCronTriggerTime(notRunPipelines, order)[indexFirst]

	// According to the policy decision, if it is the last pipeline, when it is the StopIfLatterExecuted policy,
	// it should be compared with the pipeline in the latest success status. Only the ID greater than the successful ID can be executed
	if compensator.LatestFirst && compensator.StopIfLatterExecuted {
		// Get the pipeline successfully executed
		result, err := p.dbClient.PageListPipelines(apistructs.PipelinePageListRequest{
			Sources:  []apistructs.PipelineSource{pipelineCron.PipelineSource},
			YmlNames: []string{pipelineCron.PipelineYmlName},
			Statuses: func() []string {
				var endStatuses []string
				for _, endStatus := range apistructs.PipelineEndStatuses {
					endStatuses = append(endStatuses, endStatus.String())
				}
				return endStatuses
			}(),
			PageNum:  1,
			PageSize: 1,
			DescCols: []string{apistructs.PipelinePageListRequestIdColumn},
		})
		if err != nil {
			p.Log.Infof("latestFirst=true, stopIfLatterExecuted=true, get PipelineStatusSuccess pipeline error, cronID: %d", pipelineCron.ID)
		}
		var endStatusPipelines []spec.Pipeline
		if result != nil {
			endStatusPipelines = result.Pipelines
		}

		if len(endStatusPipelines) <= 0 {
			return nil
		}

		// If the latest successful ID is greater than the compensated ID, no compensation will be made
		lastEndStatusPipeline := endStatusPipelines[indexFirst]
		if lastEndStatusPipeline.ID > firstOrLastPipeline.ID {
			return nil
		}
	}
	_, err := p.pipelineFunc.RunPipeline(ctx, &apistructs.PipelineRunRequest{
		PipelineID:   firstOrLastPipeline.ID,
		Secrets:      firstOrLastPipeline.Extra.IncomingSecrets,
		IdentityInfo: apistructs.IdentityInfo{InternalClient: firstOrLastPipeline.Extra.InternalClient, UserID: firstOrLastPipeline.GetUserID()},
	})

	// Print one line of record after successful execution
	if err == nil {
		p.Log.Infof("[doCronCompensate] Compensate success, pipelineId %d", firstOrLastPipeline.ID)
		return nil
	}

	// If there is a conflict or internal error in compensation, the listener should be notified and the compensation scheduling should be directly executed next time,
	// If the sum of the corresponding cron expression interval and execution time is greater than the scheduling time without compensation,
	// the whole scheduling will be executed as the configured policy. If it is less than, there will be competitive execution
	p.Log.Infof("[doCronCompensate] run Compensate err, put cronId into etcd wait callback: cronId %d", pipelineCron.ID)
	// Create etcd lease
	lease := v3.NewLease(p.EtcdClient)
	if grant, err := lease.Grant(context.Background(), conf.CronCompensateTimeMinute()*60); err == nil {
		// Set cronid to key and wait
		if _, err := p.EtcdClient.Put(context.Background(),
			fmt.Sprint(etcdNeedCompensatePrefix, pipelineCron.ID),
			"", v3.WithLease(grant.ID)); err != nil {
			// Failed to write etcd. This compensation failed. Wait for the next compensation
			p.Log.Errorf("[alert] failed to write cronId to etcd: cronId %d, err: %v", pipelineCron.ID, err)
			return err
		}
	} else {
		p.Log.Errorf("[alert] failed to create etcd lease : cronId %d, err: %v", pipelineCron.ID, err)
		return err
	}

	p.Log.Infof("[doCronCompensate] put cronId into etcd suucess: cronId %d ", pipelineCron.ID)

	return errors.Errorf("[doCronCompensate] failed to run notexecute pipeline, cronID: %d, pipelineID: %d, err: %v", pipelineCron.ID, firstOrLastPipeline.ID, err)

}

// orderByCronTriggerTime order
// order: ASC/DESC
func orderByCronTriggerTime(inputs []spec.Pipeline, order string) []spec.Pipeline {
	var result []spec.Pipeline
	for i := range inputs {
		input := inputs[i]
		if input.Extra.CronTriggerTime == nil {
			continue
		}
		if len(result) == 0 {
			result = append(result, input)
			continue
		}
		if strutil.ToUpper(order) == "DESC" && (*input.Extra.CronTriggerTime).After(*result[0].Extra.CronTriggerTime) {
			result = append([]spec.Pipeline{input}, result...)
		} else {
			result = append(result, input)
		}
	}
	return result
}

// getCompensateFromTime calculates the start time point of interrupt compensation
// If extra LastCompensateAt is empty, which means it has not been compensated. The cron update time is used as the compensation start time
// The maximum compensation is one day
//
// There is a special scenario:
// 1.  Manually change the enable field to 0 and restart the pipeline to stop cron
// 2.  Do some operations that need to temporarily stop cron, such as database migration, cluster adjustment, etc
// 3.  Manually modify enable = 1, restart the pipeline to make cron effective, and interrupt compensation is required
// During the process, the LastCompensateAt field is not updated, and cron is temporarily stopped. In this case, the cron update time is also used as the compensation start time
func getCompensateFromTime(pc db.PipelineCron) (t time.Time) {
	now := time.Unix(time.Now().Unix(), 0)
	defer func() {
		if now.Sub(t) > time.Hour*24 {
			t = now.Add(-time.Hour * 24)
		}
	}()

	if pc.Extra.LastCompensateAt != nil {
		return *pc.Extra.LastCompensateAt
	}
	return pc.TimeUpdated
}

// getTriggeredTime gets the creation time. The pipeline created regularly uses CronTriggerTime
func getTriggeredTime(p spec.Pipeline) time.Time {
	if p.Extra.CronTriggerTime != nil {
		return time.Unix((*p.Extra.CronTriggerTime).Unix(), 0)
	}
	return time.Unix(p.TimeCreated.Unix(), 0)
}

func (p *provider) createCronCompensatePipeline(ctx context.Context, pc db.PipelineCron, triggerTime time.Time) (*spec.Pipeline, error) {
	// generate new label map avoid concurrent map problem
	pc.Extra.NormalLabels = pc.GenCompensateCreatePipelineReqNormalLabels(triggerTime)
	pc.Extra.FilterLabels = pc.GenCompensateCreatePipelineReqFilterLabels()

	return p.pipelineFunc.CreatePipeline(ctx, &apistructs.PipelineCreateRequestV2{
		PipelineYml:            pc.Extra.PipelineYml,
		ClusterName:            pc.Extra.ClusterName,
		PipelineYmlName:        pc.PipelineYmlName,
		PipelineSource:         pc.PipelineSource,
		Labels:                 pc.Extra.FilterLabels,
		NormalLabels:           pc.Extra.NormalLabels,
		Envs:                   pc.Extra.Envs,
		ConfigManageNamespaces: pc.Extra.ConfigManageNamespaces,
		Secrets:                pc.Extra.IncomingSecrets,
		AutoRunAtOnce:          false,
		AutoStartCron:          false,
		IdentityInfo: apistructs.IdentityInfo{
			UserID:         pc.Extra.NormalLabels[apistructs.LabelUserID],
			InternalClient: "system-cron-compensator",
		},
	})
}

// isCronShouldIgnore If the cron trigger time is not triggered at this time, it should be skipped
func (p *provider) isCronShouldBeIgnored(pc db.PipelineCron) bool {
	if pc.Extra.CronStartFrom == nil {
		return false
	}
	triggerTime := time.Now()
	return triggerTime.Before(*pc.Extra.CronStartFrom)
}

func init() {
	servicehub.Register("cron-compensator", &servicehub.Spec{
		Services:   []string{"cron-compensator"},
		Types:      []reflect.Type{reflect.TypeOf((*Interface)(nil)).Elem()},
		ConfigFunc: func() interface{} { return &config{} },
		Creator:    func() servicehub.Provider { return &provider{} },
	})
}
