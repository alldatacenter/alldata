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

package events

import (
	"context"
	"fmt"
	"path/filepath"
	"strings"
	"sync"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/events/eventtypes"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/executortypes"
	"github.com/erda-project/erda/pkg/discover"
	"github.com/erda-project/erda/pkg/dlock"
	"github.com/erda-project/erda/pkg/jsonstore"
	"github.com/erda-project/erda/pkg/jsonstore/storetypes"
)

var (
	EventboxDir = "/eventbox"
	MessageDir  = filepath.Join(EventboxDir, "messages")
)
var eventMgr EventMgr

// GetEventManager returns global instance
func GetEventManager() *EventMgr {
	return &eventMgr
}

// RegisterEventCallback Register the event handler function
func (m *EventMgr) RegisterEventCallback(name string, cb executortypes.EventCbFn) error {
	if _, ok := m.executorCbMap.Load(name); ok {
		return errors.Errorf("duplicate executor registering for executor(%s)", name)
	}

	m.executorCbMap.Store(name, cb)
	logrus.Debugf("register executor(%s) event cb", name)
	return nil
}

// UnRegisterEventCallback Unregister the event handler function
func (m *EventMgr) UnRegisterEventCallback(name string) {
	if _, ok := m.executorCbMap.Load(name); !ok {
		return
	}
	m.executorCbMap.Delete(name)
	logrus.Debugf("unregister executor(%s) event cb", name)
}

var js jsonstore.JsonStore

func New(sender string, dest map[string]interface{}) (Notifier, error) {
	var err error
	if js == nil {
		if js, err = jsonstore.New(); err != nil {
			return nil, err
		}
	}
	return &NotifierImpl{
		sender: sender,
		labels: dest,
		dir:    MessageDir,
		js:     js,
	}, nil
}

func WithDest(dest map[string]interface{}) OpOperation {
	return func(op *Op) {
		op.dest = dest
	}
}

func WithSender(sender string) OpOperation {
	return func(op *Op) {
		op.sender = sender
	}
}

func OnlyOne(ctx context.Context, lock *dlock.DLock) (func(), error) {
	var isCanceled bool
	var locked bool

	cleanup := func() {
		if isCanceled {
			return
		}
		logrus.Infof("Onlyone: etcdlock: unlock, key: %s", lock.Key())
		if err := lock.Unlock(); err != nil {
			logrus.Errorf("Onlyone: etcdlock unlock err: %v", err)
			return
		}
	}

	go func() {
		time.Sleep(3 * time.Second)
		if !locked && !isCanceled {
			logrus.Warnf("Onlyone: not get lock yet after 3s")
		}
	}()
	if err := lock.Lock(ctx); err != nil {
		if err == context.Canceled {
			isCanceled = true
			logrus.Infof("Onlyone: etcdlock: %v", err)
			return cleanup, nil
		}
		return cleanup, err
	}
	locked = true
	logrus.Infof("Onlyone: etcdlock: lock, key: %s", lock.Key())

	return cleanup, nil
}

func init() {
	// TODO: need set timeout
	eventMgr.ctx = context.Background()
	eventMgr.executorCbMap = sync.Map{}

	allExecutorCallback := func(key string, value interface{}, t storetypes.ChangeType) error {
		f := func(k, v interface{}) bool {
			v.(executortypes.EventCbFn)(key, value, t)
			return true
		}
		eventMgr.executorCbMap.Range(f)
		return nil
	}

	store, err := jsonstore.New()
	if err != nil {
		panic(fmt.Sprintf("new jsonstore: %v", err))
	}
	watchstore := store.IncludeWatch()
	if watchstore == nil {
		panic(fmt.Sprintf("failed to new jsonstore with watch"))
	}
	go func() {
		for {
			select {
			case <-time.After(time.Duration(5) * time.Second):
			}
			if err := watchstore.Watch(eventMgr.ctx, WATCHED_DIR, true, false, false, apistructs.ServiceGroup{}, allExecutorCallback); err != nil {
				logrus.Errorf("watchstore watch err: %v", err)
			}
		}
	}()
	eventMgr.MemEtcdStore = watchstore

	notifier, err := New("", nil)
	if err != nil {
		panic(fmt.Sprintf("new eventbox api error: %v", err))
	}
	eventMgr.notifier = notifier
}

func getLayerInfoFromEvent(id, eventType string) (*EventLayer, error) {
	err := errors.Errorf("event's taskId(%s) format error", id)
	// status_update_event id(taskId) Format: runtimes_v1_services_staging-773_web.e622bf15-9300-11e8-ad54-70b3d5800001
	// instance_health_changed_event id(instanceId) Format: runtimes_v1_services_staging-790_web.marathon-0ad6d3ce-946c-11e8-ad54-70b3d5800001
	strs := strings.Split(id, ".")
	if len(strs) < 2 {
		return nil, err
	}
	var e EventLayer
	fields := strings.Split(strs[0], "_")
	if l := len(fields); l > 3 {
		if eventType == INSTANCE_HEALTH_CHANGED_EVENT {
			e.InstanceId = strings.Replace(strs[1], "marathon-", "", 1)
		} else if eventType == STATUS_UPDATE_EVENT {
			e.InstanceId = strs[1]
		}
		e.ServiceName = fields[l-1]
		e.RuntimeName = fields[l-3] + "/" + fields[l-2]
		return &e, nil
	}
	return nil, err
}

// HandleOneExecutorEvent Process the event logic of a single plug-in executor
func HandleOneExecutorEvent(name string, ch chan *eventtypes.StatusEvent, lstore *sync.Map, cb executortypes.EventCbFn, stopCh chan struct{}) {
	evm := GetEventManager()
	if err := evm.RegisterEventCallback(name, cb); err != nil {
		logrus.Errorf("register executor failed, err: %v", err)
		return
	}

	if strings.Contains(name, "EDAS") {
		return
	}
	o11 := discover.Orchestrator()
	eventAddr := "http://" + o11 + "/api/events/runtimes/actions/sync"
	defer func() {
		// TODO: clear lstore
		logrus.Infof("executor(%s) event handler exit", name)
	}()

	// cache window's keys
	var eventsInWindow []WindowStatus
	nowTime := time.Now()
	for {
		select {
		case <-stopCh:
			logrus.Errorf("executor(%s) got stop chan message for receiving events", name)
			return
		default:
		}
		// Time window + statistics status
		if time.Since(nowTime) >= 10*time.Second {
			nowTime = time.Now()

			// firstly send event
			for _, k := range eventsInWindow {
				re, ok := lstore.Load(k.key)
				if !ok {
					logrus.Errorf("in statistics cannot get key(%s) from lstore, type: %v", k.key, k.statusType)
					continue
				}
				ev := re.(RuntimeEvent)
				computeServiceStatus(&ev)
				ev.EventType = EVENTS_INCR

				if err := evm.notifier.Send(ev, WithSender(name+SUFFIX_NORMAL), WithDest(map[string]interface{}{"HTTP": []string{eventAddr}})); err != nil {
					logrus.Errorf("eventbox send api err: %v", err)
					continue
				}
				// Clean up the instances of KILLED/FINISHED/FAILED recorded in the event to avoid repeated sending
				for i := range ev.ServiceStatuses {
					for j := len(ev.ServiceStatuses[i].InstanceStatuses) - 1; j >= 0; j-- {
						is := ev.ServiceStatuses[i].InstanceStatuses[j].InstanceStatus
						if is == "Killed" || is == "Finished" || is == "Failed" {
							ev.ServiceStatuses[i].InstanceStatuses = append(ev.ServiceStatuses[i].InstanceStatuses[:j],
								ev.ServiceStatuses[i].InstanceStatuses[j+1:]...)
						}
					}
				}
				lstore.Store(k, ev)
			}
			// clear the window
			if len(eventsInWindow) != 0 {
				eventsInWindow = nil
			}
		}

		select {
		case e := <-ch:
			if e.Cluster != name {
				logrus.Infof("event's executor(%s) not matched this executor(%s)", e.Cluster, name)
				break
			}
			// Filter KILLING/STARTING events
			if e.Status == KILLING || e.Status == STARTING || e.Status == STAGING || e.Status == DROPPED {
				break
			}
			// Send instance change events through eventbox hook
			if err := handleInstanceStatusChangedEvents(e, lstore); err != nil {
				logrus.Errorf("handle instance status with err: %v", err)
			}

			// Events for addons are not delivered to orchestrator
			if strings.HasPrefix(e.TaskId, "runtimes_v1_addon") {
				break
			}

			var eKey string
			var err error
			if e.Type == INSTANCE_HEALTH_CHANGED_EVENT {
				if eKey, err = handleHealthStatusChanged(e, lstore); err != nil {
					logrus.Errorf("executor(%s) handle health status changed event err: %v", name, err)
					break
				}
			} else if e.Type == STATUS_UPDATE_EVENT {
				if eKey, err = handleStatusUpdateEvent(e, lstore); err != nil {
					logrus.Errorf("executor(%s) handle status update event err: %v", name, err)
					break
				}
			}

			// Record events in this time window for sending certain status events after the time expires
			// eventsInWindow is based on runtime unit，sending events of the entire runtime
			evExisted := false
			for i, ev := range eventsInWindow {
				if ev.key != eKey {
					continue
				}
				eventsInWindow[i].statusType = e.Status
				evExisted = true
				break

			}
			if !evExisted {
				eventsInWindow = append(eventsInWindow, WindowStatus{eKey, e.Status})
			}

		default:
			time.Sleep(1 * time.Second)
		}
	}
}

func handleStatusUpdateEvent(e *eventtypes.StatusEvent, lstore *sync.Map) (string, error) {
	layer, err := getLayerInfoFromEvent(e.TaskId, e.Type)
	if err != nil {
		return "", errors.Errorf("got layer info from event error: %v", err)
	}
	eKey := layer.RuntimeName
	eInstanceID := layer.InstanceId
	srvName := layer.ServiceName

	re, ok := lstore.Load(eKey)
	if !ok {
		return "", errors.Errorf("key(%s) not found in lstore, event type: %v, status: %s, drop it as not found in lstore", eKey, e.Type, e.Status)
	}

	run := re.(RuntimeEvent)

	foundEvInSrv := false
	for i, srv := range run.ServiceStatuses {
		//The event belongs to an instance under the service
		if !strings.EqualFold(srv.ServiceName, srvName) {
			continue
		}
		foundEvInSrv = true
		foundInstance := false
		// Record the case of killing the instance if it is caused by the health check timeout
		recordInstanceExited := func(insKey string, status string, instance *InstanceStatus) {
			// The container has exited and the key in the cache needs to be cleared
			defer lstore.Delete(insKey)
			// Failed or Finished must be the container exit caused by the user's own reasons, and the default mark is in the container startup stage
			if status == INSTANCE_FAILED || status == INSTANCE_FINISHED {
				instance.Stage = "BeforeHealthCheckTimeout"
				return
			}
			v, ok := lstore.Load(insKey)
			if !ok {
				return
			}
			//Handling the case where the container is Killed

			d := run.ServiceStatuses[i].HealthCheckDuration
			if d < apistructs.HealthCheckDuration {
				logrus.Infof("instance(%s) health check duration change from %v to %v", insKey, d, apistructs.HealthCheckDuration)
				d = apistructs.HealthCheckDuration
			}
			startHcTime := v.(int64)
			current := time.Now().Unix()
			expectedKilledTime := startHcTime + int64(d)

			//Determine that this instance was killed by the health check timeout, and the killing time was after the (start health time + duration) time and did not exceed too long
			//startHcTime is set to 0 when the instance is healthy
			if startHcTime > 0 &&
				current-expectedKilledTime > LEFT_EDGE &&
				current-expectedKilledTime < RIGHT_DEGE {
				instance.Stage = "HealthCheckTimeout"
			} else if current-expectedKilledTime < LEFT_EDGE {
				instance.Stage = "BeforeHealthCheckTimeout"
			} else if current-expectedKilledTime > RIGHT_DEGE {
				instance.Stage = "AfterHealthCheckTimeout"
			}

		}
		for j := range srv.InstanceStatuses {
			instance := &(run.ServiceStatuses[i].InstanceStatuses[j])
			if instance.ID != e.TaskId {
				continue
			}
			foundInstance = true
			switch e.Status {
			case KILLED:
				// KILLED It is possible that the health check was overtime and was killed, distinguish this situation
				instance.InstanceStatus = INSTANCE_KILLED
				recordInstanceExited(e.TaskId+START_HC_TIME_SUFFIX, INSTANCE_KILLED, instance)

			case RUNNING:
				// It is forbidden for an instance to degenerate from healthy / unhealthy to running, because whether it is healthy or not depends on the healthy event.
				if instance.InstanceStatus != HEALTHY && instance.InstanceStatus != UNHEALTHY {
					instance.InstanceStatus = INSTANCE_RUNNING
				}

				// If the health check sets delaySeconds, there will be two TASK_RUNNING events
				// The next TASK_RUNNING will start the health check
				// So what is recorded is the time when the health check started
				lstore.Store(e.TaskId+START_HC_TIME_SUFFIX, time.Now().Unix())

			case FINISHED:
				// FINISHED It must be the business side's own reason to withdraw
				instance.InstanceStatus = INSTANCE_FINISHED
				recordInstanceExited(e.TaskId+START_HC_TIME_SUFFIX, INSTANCE_FINISHED, instance)

			case FAILED:
				// FAILED It must be the business side's own reason to withdraw
				instance.InstanceStatus = INSTANCE_FAILED
				recordInstanceExited(e.TaskId+START_HC_TIME_SUFFIX, INSTANCE_FAILED, instance)

			default:
				instance.InstanceStatus = e.Status
				logrus.Debugf("service(%s) instance(%s) has default status: %s",
					srv.ServiceName, instance.ID, e.Status)
			}

			// marathon bug: marathon instance KILLED In the event, the container ip is the host ip in most cases
			if e.Status != KILLED {
				instance.Ip = e.IP
			}
			break
		}

		// instanceId has been recorded in the status of the current service
		if foundInstance {
			lstore.Store(eKey, run)
			break
		}

		// instanceId not recorded in the status of the current service
		hasVacancy := false
		for j := range run.ServiceStatuses[i].InstanceStatuses {
			instance := &(run.ServiceStatuses[i].InstanceStatuses[j])
			if instance.ID != "" {
				continue
			}
			hasVacancy = true
			instance.ID = e.TaskId
			instance.Ip = e.IP

			switch e.Status {
			case RUNNING:
				instance.InstanceStatus = INSTANCE_RUNNING
				lstore.Store(e.TaskId+START_HC_TIME_SUFFIX, time.Now().Unix())

			case FAILED:
				instance.InstanceStatus = INSTANCE_FAILED
				recordInstanceExited(e.TaskId+START_HC_TIME_SUFFIX, INSTANCE_FAILED, instance)

			case FINISHED:
				instance.InstanceStatus = INSTANCE_FINISHED
				recordInstanceExited(e.TaskId+START_HC_TIME_SUFFIX, INSTANCE_FINISHED, instance)

			case KILLED:
				// the instance of KILLING/KILLED will be recorded before normal
				logrus.Errorf("event taskID(%s) not found in previous status but its status is %s", eInstanceID, e.Status)
				instance.InstanceStatus = INSTANCE_KILLED
				recordInstanceExited(e.TaskId+START_HC_TIME_SUFFIX, INSTANCE_KILLED, instance)

			default:
				//continue
				instance.InstanceStatus = e.Status
				logrus.Debugf("service(%s) instance(%s) has other status: %s",
					srv.ServiceName, instance.ID, e.Status)
			}

			break
		}

		//No space indicates that it is known from etcd metadata information that a certain service A has B copies (scale), and the number of different instances currently recorded has exceeded the number of copies
		//Often visible during restarts and rolling upgrades
		if !hasVacancy {
			var status string
			ins := InstanceStatus{
				ID:    e.TaskId,
				Ip:    e.IP,
				Extra: make(map[string]interface{}),
			}

			switch e.Status {
			case RUNNING:
				status = INSTANCE_RUNNING
				lstore.Store(ins.ID+START_HC_TIME_SUFFIX, time.Now().Unix())
			case FAILED:
				status = INSTANCE_FAILED
				recordInstanceExited(ins.ID+START_HC_TIME_SUFFIX, INSTANCE_FAILED, &ins)
			case FINISHED:
				status = INSTANCE_FINISHED
				recordInstanceExited(ins.ID+START_HC_TIME_SUFFIX, INSTANCE_FINISHED, &ins)
			case KILLED:
				status = INSTANCE_KILLED
				recordInstanceExited(ins.ID+START_HC_TIME_SUFFIX, INSTANCE_KILLED, &ins)
			}
			ins.InstanceStatus = status
			run.ServiceStatuses[i].InstanceStatuses = append(run.ServiceStatuses[i].InstanceStatuses, ins)
		}

		lstore.Store(eKey, run)
		break
	}

	if !foundEvInSrv {
		return "", errors.Errorf("event taskId(%s) instance(%s) not found in runtime(%s)'s any service",
			e.TaskId, eInstanceID, run.RuntimeName)
	}
	return eKey, nil
}

func handleHealthStatusChanged(e *eventtypes.StatusEvent, lstore *sync.Map) (string, error) {
	layer, err := getLayerInfoFromEvent(e.TaskId, e.Type)
	if err != nil {
		return "", errors.Errorf("got layer info from event error: %v", err)
	}

	eKey := layer.RuntimeName
	srvName := layer.ServiceName
	re, ok := lstore.Load(eKey)
	if !ok {
		return "", errors.Errorf("key(%s) not found in lstore, event type: %v, status: %v, drop it as not found in lstore", eKey, e.Type, e.Status)
	}

	run := re.(RuntimeEvent)

	foundEvInSrv := false
	for i, srv := range run.ServiceStatuses {
		// The event belongs to an instance under the service
		if !strings.EqualFold(srv.ServiceName, srvName) {
			continue
		}
		foundEvInSrv = true
		foundInstance := false
		for j, instance := range srv.InstanceStatuses {
			//if instance.ID == eInstanceID {
			if instance.ID != e.TaskId {
				continue
			}
			foundInstance = true
			switch e.Status {
			case HEALTHY:
				run.ServiceStatuses[i].InstanceStatuses[j].InstanceStatus = HEALTHY
				// Set to 0 to indicate that the instance has passed the health check at least
				lstore.Store(instance.ID+START_HC_TIME_SUFFIX, int64(0))
			case UNHEALTHY:
				run.ServiceStatuses[i].InstanceStatuses[j].InstanceStatus = UNHEALTHY
			}
			break
		}

		// instanceId Not recorded in the status of the current service, theoretically it will not happen
		if !foundInstance {
			return "", errors.Errorf("healthy instance(%s) not found in service(%s)", e.TaskId, srvName)
		}
		lstore.Store(eKey, run)
		break
	}

	if !foundEvInSrv {
		return "", errors.Errorf("event taskId(%s) not found in runtime(%s)'s any service",
			e.TaskId, run.RuntimeName)
	}
	return eKey, nil
}

func computeServiceStatus(e *RuntimeEvent) {
	for i, srv := range e.ServiceStatuses {
		runningReplica := 0
		healthyReplica := 0

		// Indicates that the runtime is deleted
		if e.IsDeleted {
			e.ServiceStatuses[i].ServiceStatus = "Deleted"
			continue
		}

		for j, instance := range srv.InstanceStatuses {
			switch instance.InstanceStatus {
			case INSTANCE_RUNNING:
				runningReplica++
				e.ServiceStatuses[i].InstanceStatuses[j].InstanceStatus = string(apistructs.StatusStarting)
			case UNHEALTHY:
				runningReplica++
			case HEALTHY:
				runningReplica++
				healthyReplica++
			}
		}
		// In order to facilitate the display of the upper layer, it is agreed that the service status is set to Healthy if the number of instances is 0 and no instances are running.
		if healthyReplica == srv.Replica && runningReplica == srv.Replica {
			//e.ServiceStatuses[i].ServiceStatus = string(spec.StatusReady)
			e.ServiceStatuses[i].ServiceStatus = HEALTHY
		} else {
			// spec.StatusProgressing || spec.StatusRunning -> spec.StatusUnHealthy
			e.ServiceStatuses[i].ServiceStatus = UNHEALTHY
		}
	}
}
