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

package scheduler

import (
	"context"
	"fmt"
	"sync"
	"time"

	"github.com/gorilla/mux"
	"github.com/sirupsen/logrus"

	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/endpoints"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/events"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/cap"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/cluster"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/clusterinfo"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/instanceinfo"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/job"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/labelmanager"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/resourceinfo"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/servicegroup"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/volume"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/volume/driver"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/task"
	"github.com/erda-project/erda/pkg/discover"
	"github.com/erda-project/erda/pkg/http/httpserver"
	"github.com/erda-project/erda/pkg/jsonstore"
)

type Responser = httpserver.Responser

type HTTPResponse = httpserver.HTTPResponse

type Scheduler struct {
	router *mux.Router
	//listenAddr    string
	sched         *task.Sched
	store         jsonstore.JsonStore
	runtimeMap    map[string]*apistructs.ServiceGroup
	l             sync.Mutex
	notifier      events.Notifier
	Httpendpoints *endpoints.HTTPEndpoints
}

// NewSched, p.ClusterSvc, p.ClusterSvculer creates a scheduler instance, it be used to handle servicegroup create/update/delete actions.
func NewScheduler(instanceinfoImpl instanceinfo.InstanceInfo, clusterSvc clusterpb.ClusterServiceServer) *Scheduler {
	option := jsonstore.UseCacheEtcdStore(context.Background(), "/dice", 500)
	store, err := jsonstore.New(option)
	if err != nil {
		panic(err)
	}
	sched, err := task.NewSched()
	if err != nil {
		panic(err)
	}
	notifier, err := events.New("dice-scheduler", nil)
	if err != nil {
		panic(err)
	}

	volumeStore, err := jsonstore.New(jsonstore.UseMemEtcdStore(context.Background(), volume.ETCDVolumeMetadataDir, nil, nil))
	if err != nil {
		panic(err)
	}
	drivers := map[apistructs.VolumeType]volume.Volume{
		apistructs.LocalVolume: driver.NewLocalVolumeDriver(volumeStore),
		apistructs.NasVolume:   driver.NewNasVolumeDriver(volumeStore),
	}
	js, err := jsonstore.New()
	volumeImpl := volume.NewVolumeImpl(drivers)
	//metricImpl := metric.New()
	clusterImpl := cluster.NewClusterImpl(store)
	clusterinfoImpl := clusterinfo.NewClusterInfoImpl(js)
	servicegroupImpl := servicegroup.NewServiceGroupImpl(store, sched, clusterinfoImpl)
	jobImpl := job.NewJobImpl(store, sched)
	labelManagerImpl := labelmanager.NewLabelManager()
	// instanceinfoImpl := instanceinfo.NewInstanceInfoImpl()
	componentImpl := instanceinfo.NewComponentInfoImpl()
	resourceinfoImpl := resourceinfo.NewResourceInfoImpl()
	capImpl := cap.NewCapImpl()

	if err != nil {
		panic(err)
	}

	httpendpoints := endpoints.NewHTTPEndpoints(volumeImpl, servicegroupImpl, clusterImpl, jobImpl, labelManagerImpl, instanceinfoImpl, clusterinfoImpl, componentImpl, resourceinfoImpl, capImpl, clusterSvc)

	scheduler := &Scheduler{
		sched:         sched,
		store:         store,
		runtimeMap:    make(map[string]*apistructs.ServiceGroup),
		l:             sync.Mutex{},
		notifier:      notifier,
		Httpendpoints: httpendpoints,
	}

	go scheduler.clearMap()

	// Register cluster event hook
	if err := registerClusterHook(); err != nil {
		panic(err)
	}

	return scheduler
}

func (s *Scheduler) clearMap() {
	logrus.Infof("enter clearMap")
	for {
		select {
		case <-time.After(30 * time.Second):
			ks := make([]string, 0)
			for k := range s.runtimeMap {
				//delete(s.runtimeMap, k)
				ks = append(ks, k)
			}

			for _, k := range ks {
				s.l.Lock()
				delete(s.runtimeMap, k)
				s.l.Unlock()
			}
			//s.runtimeMap = make(map[string]*spec.ServiceGroup)
		}
	}
}

// registerClusterHook register cluster webhook in eventBox
func registerClusterHook() error {
	bdl := bundle.New(bundle.WithCoreServices())

	ev := apistructs.CreateHookRequest{
		Name:   "scheduler-clusterhook",
		Events: []string{"cluster"},
		URL:    fmt.Sprintf("http://%s/clusterhook", discover.Orchestrator()),
		Active: true,
		HookLocation: apistructs.HookLocation{
			Org:         "-1",
			Project:     "-1",
			Application: "-1",
		},
	}

	if err := bdl.CreateWebhook(ev); err != nil {
		logrus.Warnf("failed to register cluster event, (%v)", err)
		return err
	}

	logrus.Infof("register cluster event success")
	return nil
}
