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

package logic

import (
	"bytes"
	"encoding/json"
	"fmt"
	"html/template"
	"strings"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/pipeline/conf"
	"github.com/erda-project/erda/internal/tools/pipeline/pkg/task_uuid"
	"github.com/erda-project/erda/internal/tools/pipeline/spec"
	"github.com/erda-project/erda/pkg/k8s/storage"
	"github.com/erda-project/erda/pkg/parser/diceyml"
	"github.com/erda-project/erda/pkg/parser/pipelineyml/pipelineymlv1"
	"github.com/erda-project/erda/pkg/strutil"
)

// ParseJobHostBindTemplate Analyze the hostPath template and convert it to the cluster info value
func ParseJobHostBindTemplate(hostPath string, clusterInfo apistructs.ClusterInfoData) (string, error) {
	var b bytes.Buffer

	if hostPath == "" {
		return "", errors.New("hostPath is empty")
	}

	t, err := template.New("jobBind").
		Option("missingkey=error").
		Parse(hostPath)
	if err != nil {
		return "", errors.Errorf("failed to parse bind, hostPath: %s, (%v)", hostPath, err)
	}

	err = t.Execute(&b, &clusterInfo)
	if err != nil {
		return "", errors.Errorf("failed to execute bind, hostPath: %s, (%v)", hostPath, err)
	}

	return b.String(), nil
}

// GenerateK8SVolumes According to job configuration, production volume related configuration
func GenerateK8SVolumes(job *apistructs.JobFromUser, clusterInfo ...apistructs.ClusterInfoData) ([]corev1.Volume, []corev1.VolumeMount, []*corev1.PersistentVolumeClaim) {
	ci := apistructs.ClusterInfoData{}
	if len(clusterInfo) != 0 {
		ci = clusterInfo[0]
	}

	vols := []corev1.Volume{}
	volMounts := []corev1.VolumeMount{}
	pvcs := []*corev1.PersistentVolumeClaim{}
	for i, v := range job.Volumes {
		var pvcid string
		if v.ID == nil { // if ID == nil, we need to create a new pvc
			sc := WhichStorageClass(v.Storage, ci[apistructs.CSIVendor])
			id := fmt.Sprintf("%s-%s-%d", job.Namespace, job.Name, i)
			pvcs = append(pvcs, &corev1.PersistentVolumeClaim{
				ObjectMeta: metav1.ObjectMeta{
					Name:      id,
					Namespace: job.Namespace,
				},
				Spec: corev1.PersistentVolumeClaimSpec{
					AccessModes: []corev1.PersistentVolumeAccessMode{corev1.ReadWriteOnce},
					Resources: corev1.ResourceRequirements{
						Requests: corev1.ResourceList{
							corev1.ResourceStorage: resource.MustParse("10Gi"),
						},
					},
					StorageClassName: &sc,
				},
			})
			pvcid = id
		} else {
			pvcs = append(pvcs, nil) // append a placeholder
			pvcid = *v.ID
		}
		volName := fmt.Sprintf("vol-%d", i)
		vols = append(vols, corev1.Volume{
			Name: volName,
			VolumeSource: corev1.VolumeSource{
				PersistentVolumeClaim: &corev1.PersistentVolumeClaimVolumeSource{
					ClaimName: pvcid,
				},
			},
		})
		volMounts = append(volMounts, corev1.VolumeMount{
			Name:      volName,
			MountPath: v.Path,
		})
	}
	return vols, volMounts, pvcs
}

func WhichStorageClass(tp string, vendor string) string {
	diskType := apistructs.VolumeTypeDiceLOCAL

	switch tp {
	case "local":
		diskType = apistructs.VolumeTypeDiceLOCAL
	case "nfs":
		diskType = apistructs.VolumeTypeDiceNAS
	}

	if vendor != "" {
		diskType = apistructs.VolumeTypeNAS
	}

	sc, err := storage.VolumeTypeToSCName(diskType, vendor)
	if err != nil {
		logrus.Errorf("failed to parse volume type to sc, default return %s, err: %v", diskType, err)
		return apistructs.DiceLocalVolumeSC
	}

	return sc
}

func MakeJobName(task *spec.PipelineTask) string {
	return strutil.Concat(task.Extra.Namespace, ".", task_uuid.MakeJobID(task))
}

func TransferToSchedulerJob(task *spec.PipelineTask) (job apistructs.JobFromUser, err error) {
	defer func() {
		if r := recover(); r != nil {
			err = errors.Errorf("%v", r)
		}
	}()

	return apistructs.JobFromUser{
		Name: task_uuid.MakeJobID(task),
		Kind: func() string {
			switch task.Type {
			case string(pipelineymlv1.RES_TYPE_FLINK):
				return string(apistructs.Flink)
			case string(pipelineymlv1.RES_TYPE_SPARK):
				return string(apistructs.Spark)
			default:
				return ""
			}
		}(),
		Namespace:               task.Extra.Namespace,
		NotPipelineControlledNs: task.Extra.NotPipelineControlledNs,
		ClusterName: func() string {
			if len(task.Extra.ClusterName) == 0 {
				panic(errors.New("missing cluster name in pipeline task"))
			}
			return task.Extra.ClusterName
		}(),
		Image:          task.Extra.Image,
		Cmd:            strings.Join(append([]string{task.Extra.Cmd}, task.Extra.CmdArgs...), " "),
		CPU:            task.Extra.RuntimeResource.CPU,
		Memory:         task.Extra.RuntimeResource.Memory,
		MaxCPU:         task.Extra.RuntimeResource.MaxCPU,
		MaxMemory:      task.Extra.RuntimeResource.MaxMemory,
		Binds:          task.Extra.Binds,
		Volumes:        MakeVolume(task),
		PreFetcher:     task.Extra.PreFetcher,
		Env:            task.Extra.PublicEnvs,
		Labels:         task.Extra.Labels,
		TaskContainers: task.Extra.TaskContainers,
		// flink/spark
		Resource:  task.Extra.FlinkSparkConf.JarResource,
		MainClass: task.Extra.FlinkSparkConf.MainClass,
		MainArgs:  task.Extra.FlinkSparkConf.MainArgs,
		// 重试不依赖 scheduler，由 pipeline engine 自己实现，保证所有 action executor 均适用
		Params:                    task.Extra.Action.Params,
		ContainerInstanceProvider: task.Extra.ContainerInstanceProvider,
	}, nil
}

func MakeVolume(task *spec.PipelineTask) []diceyml.Volume {
	diceVolumes := make([]diceyml.Volume, 0)
	for _, vo := range task.Extra.Volumes {
		if vo.Type == string(spec.StoreTypeDiceVolumeFake) || vo.Type == string(spec.StoreTypeDiceCacheNFS) {
			// fake volume,没有实际挂载行为,不传给scheduler
			continue
		}
		diceVolume := diceyml.Volume{
			Path: vo.Value,
			Storage: func() string {
				switch vo.Type {
				case string(spec.StoreTypeDiceVolumeNFS):
					return "nfs"
				case string(spec.StoreTypeDiceVolumeLocal):
					return "local"
				default:
					panic(errors.Errorf("%q has not supported volume type: %s", vo.Name, vo.Type))
				}
			}(),
		}
		if vo.Labels != nil {
			if id, ok := vo.Labels["ID"]; ok {
				diceVolume.ID = &id
				goto AppendDiceVolume
			}
		}
		// labels == nil or labels["ID"] not exist
		// 如果 id 不存在，说明上一次没有生成 volume，并且是 optional 的，则不创建 diceVolume
		if vo.Optional {
			continue
		}
	AppendDiceVolume:
		diceVolumes = append(diceVolumes, diceVolume)
	}
	return diceVolumes
}

func GetBigDataConf(task *spec.PipelineTask) (apistructs.BigdataConf, error) {
	conf := apistructs.BigdataConf{
		BigdataMetadata: apistructs.BigdataMetadata{
			Name:      task.Extra.UUID,
			Namespace: task.Extra.Namespace,
		},
		Spec: apistructs.BigdataSpec{},
	}
	value, ok := task.Extra.Action.Params["bigDataConf"]
	if !ok {
		return conf, fmt.Errorf("missing big data conf from task: %s", task.Extra.UUID)
	}

	if err := json.Unmarshal([]byte(value.(string)), &conf.Spec); err != nil {
		return conf, fmt.Errorf("unmarshal bigdata config error: %s", err.Error())
	}
	return conf, nil
}

// GetPullImagePolicy specify Image Pull Policy with IfNotPresent,Always,Never
func GetPullImagePolicy() corev1.PullPolicy {
	var imagePullPolicy corev1.PullPolicy
	switch corev1.PullPolicy(conf.SpecifyImagePullPolicy()) {
	case corev1.PullAlways:
		imagePullPolicy = corev1.PullAlways
	case corev1.PullNever:
		imagePullPolicy = corev1.PullNever
	default:
		imagePullPolicy = corev1.PullIfNotPresent
	}

	return imagePullPolicy
}

// MakeJobLabelSelector return LabelSelector like job-name=pipeline-1.pipeline-task-1
func MakeJobLabelSelector(task *spec.PipelineTask) string {
	return fmt.Sprintf("job-name=%s", MakeJobName(task))
}
