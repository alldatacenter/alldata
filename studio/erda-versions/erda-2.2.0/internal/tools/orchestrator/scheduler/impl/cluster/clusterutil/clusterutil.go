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

// Package clusterutil cluster utils
package clusterutil

import (
	"context"
	"encoding/json"
	"fmt"
	"strings"
	"sync/atomic"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/conf"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/executortypes"
	"github.com/erda-project/erda/pkg/jsonstore"
	"github.com/erda-project/erda/pkg/schedule/executorconfig"
	"github.com/erda-project/erda/pkg/strutil"
)

const (
	// ServiceKindMarathon marathon executor type
	ServiceKindMarathon = "MARATHON"
	// ServiceKindK8S k8s executor type
	ServiceKindK8S = "K8S"
	// ServiceKindEdas edas executor type
	ServiceKindEdas = "EDAS"
	// JobKindMetronome metronome executor type
	JobKindMetronome = "METRONOME"
	// JobKindK8S k8s job executor type
	JobKindK8S = "K8SJOB"
	// EdasKindInK8s k8s service executor in edas
	EdasKindInK8s = "EDASINK8S"
	// DiceCluster DICE_CLUSTER env key
	DiceCluster = "DICE_CLUSTER"
	// k8sKindFlink Identify k8s flink type
	K8SKindFlink = "K8SFLINK"
	// k8sKindSpark Identify k8s spark type
	K8SKindSpark = "K8SSpark"
	// k8sSparkVersion Identify k8s flink version
	K8SSparkVersion = "2.4.0"
)

// SetRuntimeExecutorByCluster When the runtime executor is empty, set the executor according to the cluster value
func SetRuntimeExecutorByCluster(runtime *apistructs.ServiceGroup) error {
	if len(runtime.Executor) > 0 {
		return nil
	}
	if runtime.ClusterName == "" {
		return errors.Errorf("runtime(%s/%s) neither executor nor cluster is set", runtime.Type, runtime.ID)
	}

	runtime.Executor = GenerateExecutorByCluster(runtime.ClusterName, ServiceKindMarathon)

	logrus.Infof("generate executor(%s) for runtime(%s) in cluster(%s)", runtime.Executor, runtime.ID, runtime.ClusterName)
	return nil
}

// SetJobExecutorByCluster When the job executor is empty, set the executor according to the cluster value
func SetJobExecutorByCluster(job *apistructs.Job) error {
	if len(job.Executor) > 0 {
		return nil
	}
	if job.ClusterName == "" {
		return errors.Errorf("job(%s/%s) neither executor nor cluster is set", job.Namespace, job.Name)
	}

	if job.Kind == "" { // FIXME Compatible with the old untransmitted Kind situation, which can be removed later
		job.Kind = JobKindMetronome
	}

	job.Executor = GenerateExecutorByCluster(job.ClusterName, strutil.ToUpper(job.Kind))

	logrus.Infof("generate executor(%s) for job(%s) in cluster(%s)", job.Executor, job.Name, job.ClusterName)
	return nil
}

func SetJobVolumeExecutorByCluster(jobvolume *apistructs.JobVolume) error {
	if len(jobvolume.Executor) > 0 {
		return nil
	}
	if jobvolume.ClusterName == "" {
		return errors.Errorf("jobvolume(%s/%s) neither executor nor cluster is set",
			jobvolume.Namespace, jobvolume.Name)
	}
	if jobvolume.Kind == "" {
		jobvolume.Kind = JobKindMetronome
	}
	jobvolume.Executor = GenerateExecutorByCluster(jobvolume.ClusterName, strutil.ToUpper(jobvolume.Kind))
	return nil
}

var (
	preFetcher, _ = jsonstore.New()
)

// GenerateExecutorByCluster Get executorname according to `cluster` and `executorType`
func GenerateExecutorByCluster(cluster, executorType string) string {
	cname := cluster
	switch cname {
	case "xhsd-t2":
		cname = "XINHUATEST"
	case "fsg-prod":
		cname = "WAIFU"
	case "xhsd-prod":
		cname = "XINHUAPROD"
	}

	var executor string
	if err := preFetcher.Get(context.Background(),
		fmt.Sprintf("/dice/clustertoexecutor/%s/%s", cluster, executorType), &executor); err == nil {
		return executor
	}
	// The service executor that handles edas
	if executorType == ServiceKindEdas {
		return "EDASFOR" + strutil.ToUpper(strings.Replace(cname, "-", "", -1))
	}
	// terminus-y -> terminusy
	c := strings.Replace(cname, "-", "", -1)
	//e.g. MARATHONFORTERIMINUSY, METRONOMEFORTERMINUSY
	return executorType + "FOR" + strutil.ToUpper(c)
}

func GenerateExecutorByClusterName(clustername string) string {
	return GenerateExecutorByCluster(clustername, ServiceKindMarathon)
}

func GetExecuteConfigByExecutorName(name executortypes.Name) (string, *executorconfig.ExecutorConfig, error) {
	var (
		count  int64
		retKey string
		ret    = executorconfig.ExecutorConfig{}
	)

	f := func(s string, bytes []byte) error {
		e := executorconfig.ExecutorConfig{}
		if err := json.Unmarshal(bytes, &e); err != nil {
			return err
		}

		if e.Name != name.String() {
			return nil
		}

		if count != 0 {
			return errors.Errorf("found multi name executor %s", name)
		}

		ret = e
		retKey = s
		atomic.AddInt64(&count, 1)
		return nil
	}

	if err := preFetcher.ForEachRaw(context.Background(), conf.CLUSTERS_CONFIG_PATH, f); err != nil {
		return "", nil, errors.Errorf("failed to for each find executor, err: %v", err)
	}

	if count == 0 {
		return "", nil, errors.Errorf("can't find executor by name %s", name)
	}

	return retKey, &ret, nil
}
