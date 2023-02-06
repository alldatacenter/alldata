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

package instanceinfosync

import (
	"fmt"
	"strconv"
	"strings"
	"time"

	"github.com/sirupsen/logrus"
	corev1 "k8s.io/api/core/v1"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/tools/orchestrator/i18n"
	orgCache "github.com/erda-project/erda/internal/tools/orchestrator/scheduler/cache/org"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/instanceinfo"
	"github.com/erda-project/erda/pkg/strutil"
)

// exportPodErrInfo export pod error info
func exportPodErrInfo(bdl *bundle.Bundle, podlist *corev1.PodList, orgs []*apistructs.OrgDTO) {
	now := time.Now()
	for i, pod := range podlist.Items {
		var locale string
		if orgs[i] != nil {
			locale = orgs[i].Locale
		}
		for _, containerstatus := range pod.Status.ContainerStatuses {
			// restartcount > 5 && The finish time of the last terminated container is within one hour (to prevent too many false positives)
			if containerstatus.RestartCount > 5 &&
				containerstatus.LastTerminationState.Terminated != nil &&
				containerstatus.LastTerminationState.Terminated.FinishedAt.After(now.Add(-1*time.Hour)) {
				buildErrorInfo(bdl, pod,
					fmt.Sprintf("Pod(%s)-Container(%s), restartcount(%d)",
						pod.Name, containerstatus.Name, containerstatus.RestartCount),
					i18n.Sprintf(locale, "PodRestartManyTimes", pod.Name, containerstatus.RestartCount),
					"restartcount",
				)
			}
		}
		waitingContainerInfos := []string{}
		for _, status := range pod.Status.ContainerStatuses {
			if status.State.Waiting == nil {
				continue
			}
			waitingContainerInfos = append(waitingContainerInfos, fmt.Sprintf("%s:%s", status.State.Waiting.Reason, status.State.Waiting.Message))
		}
		if pod.Status.Phase == "Succeeded" || pod.Status.Phase == "Failed" {
			return
		}
		for _, cond := range pod.Status.Conditions {
			if cond.Status == "True" {
				continue
			}
			switch cond.Type {
			case "PodScheduled":
				now_3 := now.Add(-3 * time.Minute)
				if !cond.LastTransitionTime.IsZero() && cond.LastTransitionTime.Time.Before(now_3) {
					buildErrorInfo(bdl, pod,
						fmt.Sprintf("Pod(%s), PodScheduled failed: %s, %s",
							pod.Name, cond.Message, strutil.Join(waitingContainerInfos, ",")),
						i18n.Sprintf(locale, "FailedToSchedulePod", pod.Name),
						"podscheduled",
					)
					break
				}
			case "ContainersReady":
				now_10 := now.Add(-10 * time.Minute)
				if !cond.LastTransitionTime.IsZero() && cond.LastTransitionTime.Time.Before(now_10) {
					buildErrorInfo(bdl, pod,
						fmt.Sprintf("Pod(%s), ContainerNotReady: %s, %s\n健康检查:\n%s\n 镜像:\n%s",
							pod.Name, cond.Message,
							strutil.Join(waitingContainerInfos, ","),
							pp_healthcheck(pod),
							pp_image(pod)),
						i18n.Sprintf(locale, "PodNotReady", pod.Name),
						"containerready",
					)
					break
				}
			case "Ready": // ignore
			case "Initialized": // ignore
			}
		}
	}
	return
}

func pp_healthcheck(pod corev1.Pod) string {
	r := ""
	for _, container := range pod.Spec.Containers {
		if container.LivenessProbe != nil {
			r += container.Name + ":\n"
			if container.LivenessProbe.Exec != nil {
				r += "\tExec: " + strutil.Join(container.LivenessProbe.Exec.Command, " ") + "\n"
			}
			if container.LivenessProbe.HTTPGet != nil {
				r += "\tHTTP:\n\tPort: " + container.LivenessProbe.HTTPGet.Port.String() + "\n\tPath: " + container.LivenessProbe.HTTPGet.Path + "\n"
			}
		}
	}
	return r
}

func pp_image(pod corev1.Pod) string {
	r := ""
	for _, container := range pod.Spec.Containers {
		r += fmt.Sprintf("%s: %s\n", container.Name, container.Image)
	}
	return r
}

func buildErrorInfo(bdl *bundle.Bundle, pod corev1.Pod, errorinfo string, errorinfo_human string, tp string) {
	addonID, pipelineID, _, _, _, _, _, _,
		_, _, runtimeID, serviceName, _, _, _,
		_, _, _, _ := extractEnvs(pod)
	if addonID != "" {
		//////////////////////
		// addon error info //
		//////////////////////
		dedupid := fmt.Sprintf("%s-%s", addonID, tp)
		if err := bdl.CreateErrorLog(&apistructs.ErrorLogCreateRequest{
			ErrorLog: apistructs.ErrorLog{
				ResourceType:   apistructs.AddonError,
				Level:          apistructs.ErrorLevel,
				ResourceID:     addonID,
				OccurrenceTime: strconv.FormatInt(time.Now().Unix(), 10),
				HumanLog:       errorinfo_human,
				PrimevalLog:    errorinfo,
				DedupID:        dedupid,
			},
		}); err != nil {
			logrus.Errorf("createErrorLog: %v", err)
		}
	} else if runtimeID != "" {
		////////////////////////
		// runtime error info //
		////////////////////////
		dedupid := fmt.Sprintf("%s-%s-%s", runtimeID, serviceName, tp)
		if err := bdl.CreateErrorLog(&apistructs.ErrorLogCreateRequest{
			ErrorLog: apistructs.ErrorLog{
				ResourceType:   apistructs.RuntimeError,
				Level:          apistructs.ErrorLevel,
				ResourceID:     runtimeID,
				OccurrenceTime: strconv.FormatInt(time.Now().Unix(), 10),
				HumanLog:       errorinfo_human,
				PrimevalLog:    errorinfo,
				DedupID:        dedupid,
			},
		}); err != nil {
			logrus.Errorf("createErrorLog: %v", err)
		}
	} else if pipelineID != "" {
		/////////////////////////
		// pipeline error info //
		/////////////////////////
		dedupid := fmt.Sprintf("%s-scheduler", pipelineID)
		if err := bdl.CreateErrorLog(&apistructs.ErrorLogCreateRequest{
			ErrorLog: apistructs.ErrorLog{
				ResourceType:   apistructs.PipelineError,
				ResourceID:     pipelineID,
				Level:          apistructs.ErrorLevel,
				OccurrenceTime: strconv.FormatInt(time.Now().Unix(), 10),
				HumanLog:       errorinfo_human,
				PrimevalLog:    errorinfo,
				DedupID:        dedupid,
			},
		}); err != nil {
			logrus.Errorf("createErrorLog: %v", err)
		}
	}
}

// extractEnvs reference terminus.io/dice/dice/docs/dice-env-vars.md
func extractEnvs(pod corev1.Pod) (
	addonID,
	pipelineID,
	cluster,
	orgName,
	orgID,
	projectName,
	projectID,
	applicationName,
	applicationID,
	runtimeName,
	runtimeID,
	serviceName,
	workspace,
	addonName,
	cpuOriginStr,
	memOriginStr,
	diceComponent,
	edgeApplicationName,
	edgeSites string) {
	envsuffixmap := map[string]*string{
		"ADDON_ID":                   &addonID,
		"PIPELINE_ID":                &pipelineID,
		"DICE_CLUSTER_NAME":          &cluster,
		"DICE_ORG_NAME":              &orgName,
		"DICE_ORG_ID":                &orgID,
		"DICE_PROJECT_NAME":          &projectName,
		"DICE_PROJECT_ID":            &projectID,
		"DICE_APPLICATION_NAME":      &applicationName,
		"DICE_EDGE_APPLICATION_NAME": &edgeApplicationName,
		"DICE_EDGE_SITE":             &edgeSites,
		"DICE_APPLICATION_ID":        &applicationID,
		"DICE_RUNTIME_NAME":          &runtimeName,
		"DICE_RUNTIME_ID":            &runtimeID,
		"DICE_SERVICE_NAME":          &serviceName,
		"DICE_WORKSPACE":             &workspace,
		"DICE_ADDON_NAME":            &addonName,
		"DICE_CPU_ORIGIN":            &cpuOriginStr,
		"DICE_MEM_ORIGIN":            &memOriginStr,
		"DICE_COMPONENT":             &diceComponent,
	}
	for _, container := range pod.Spec.Containers {
		for _, env := range container.Env {
			for k, v := range envsuffixmap {
				if strutil.HasSuffixes(env.Name, k) && *v == "" {
					*v = env.Value
				}
			}
		}
	}
	return
}

// updatePodInstance Update pod information to db
func updatePodAndInstance(dbclient *instanceinfo.Client, podlist *corev1.PodList, delete bool,
	eventmap map[string]*corev1.Event) ([]*apistructs.OrgDTO, error) {
	r := dbclient.InstanceReader()
	w := dbclient.InstanceWriter()
	podr := dbclient.PodReader()
	podw := dbclient.PodWriter()
	var orgs = make([]*apistructs.OrgDTO, len(podlist.Items))

	for i, pod := range podlist.Items {
		var (
			cluster             string
			orgName             string
			orgID               string
			projectName         string
			projectID           string
			applicationName     string
			edgeApplicationName string
			edgeSite            string
			applicationID       string
			runtimeName         string
			runtimeID           string
			workspace           string
			serviceName         string
			addonName           string
			addonID             string
			pipelineID          string

			containerIP  string
			hostIP       string
			cpuOriginStr string
			cpuOrigin    float64
			cpuRequest   float64
			cpuLimit     float64
			memOriginStr string
			memOrigin    int
			memRequest   int
			memLimit     int
			image        string
			lastExitCode int
			exitCode     int

			// Put it in instanceinfo.meta,
			// k8snamespace & k8spodname & k8scontainername these 3 valuea need to be used in the console api of k8s
			k8snamespace     string
			k8spodname       string
			k8scontainername string
			k8spoduid        string

			diceComponent string

			namespace string
			name      string
		)
		// -------------------------------
		// 1. Collect the required information from the pod
		// Some of the envs listed above may not exist
		// -------------------------------
		addonID, pipelineID, cluster, orgName, orgID, projectName, projectID, applicationName,
			applicationID, runtimeName, runtimeID, serviceName, workspace, addonName, cpuOriginStr,
			memOriginStr, diceComponent, edgeApplicationName, edgeSite = extractEnvs(pod)
		containerIP = pod.Status.PodIP
		hostIP = pod.Status.HostIP
		var err error
		if cluster == "" {
			continue
		}
		orgs[i], _ = orgCache.GetOrgByOrgID(orgID)
		cpuRequest, err = strconv.ParseFloat(pod.Spec.Containers[0].Resources.Requests.Cpu().AsDec().String(), 64)
		if err != nil {
			cpuRequest = 0
		}
		cpuLimit, err = strconv.ParseFloat(pod.Spec.Containers[0].Resources.Limits.Cpu().AsDec().String(), 64)
		if err != nil {
			cpuLimit = 0
		}
		cpuOrigin, err = strconv.ParseFloat(cpuOriginStr, 64)
		if err != nil {
			cpuOrigin = 0
		}
		memRequestByte, ok := pod.Spec.Containers[0].Resources.Requests.Memory().AsInt64()
		if !ok {
			memRequestByte = 0
		}
		memRequest = int(memRequestByte / 1024 / 1024)
		memLimitByte, ok := pod.Spec.Containers[0].Resources.Limits.Memory().AsInt64()
		if !ok {
			memLimitByte = 0
		}
		memLimit = int(memLimitByte / 1024 / 1024)
		memOriginInt, err := strconv.ParseFloat(memOriginStr, 64)
		if err != nil {
			memOrigin = 0
		}
		memOrigin = int(memOriginInt)

		var container *corev1.Container
		container = getContainerByServiceName(pod.Spec.Containers, serviceName)
		if container == nil {
			continue
		}
		image = container.Image

		k8snamespace = pod.Namespace
		k8spodname = pod.Name
		k8spoduid = string(pod.UID)
		k8scontainername = container.Name

		// The namespace and name of the servicegroup are written into ENV, so that they cannot be obtained directly from the pod information.
		// the namespace and name are derived from other ENVs in the Pod.
		// addon cannot be deduced.
		if runtimeID != "" && workspace != "" && addonName == "" {
			namespace = "services"
			name = workspace + "-" + runtimeID
		}

		// update or create podinfo
		var startAt *time.Time
		if pod.Status.StartTime != nil {
			startAt = &pod.Status.StartTime.Time
		}
		podMessages := []string{}
		for _, cond := range pod.Status.Conditions {
			if cond.Status == "True" {
				continue
			}
			podMessages = append(podMessages, cond.Message)
		}

		for _, status := range pod.Status.ContainerStatuses {
			if status.State.Waiting == nil {
				continue
			}
			podMessages = append(podMessages, fmt.Sprintf("%s:%s", status.State.Waiting.Reason, status.State.Waiting.Message))
		}
		message := strutil.Join(podMessages, "|", true)
		if len(message) > 1000 {
			message = message[:1000]
		}
		if message == "" {
			message = "Ok"
		}
		memrequest, memlimit, cpurequest, cpulimit := calcPodResource(pod)
		podinfo := instanceinfo.PodInfo{
			Cluster:         cluster,
			Namespace:       namespace,
			Name:            name,
			OrgName:         orgName,
			OrgID:           orgID,
			ProjectName:     projectName,
			ProjectID:       projectID,
			ApplicationName: applicationName,
			ApplicationID:   applicationID,
			RuntimeName:     runtimeName,
			RuntimeID:       runtimeID,
			ServiceName:     serviceName,
			Workspace:       workspace,
			ServiceType:     servicetype(addonID, pipelineID),
			AddonID:         addonID,
			Uid:             string(pod.UID),
			K8sNamespace:    pod.Namespace,
			PodName:         pod.Name,
			Phase:           instanceinfo.PodPhase(pod.Status.Phase),
			Message:         message,
			PodIP:           pod.Status.PodIP,
			HostIP:          pod.Status.HostIP,
			StartedAt:       startAt,
			MemRequest:      memrequest,
			MemLimit:        memlimit,
			CpuRequest:      cpurequest,
			CpuLimit:        cpulimit,
		}
		podsinfo, err := podr.ByUid(string(pod.UID)).Do()
		if len(podsinfo) > 0 {
			podinfo.ID = podsinfo[0].ID
			if delete {
				if err := podw.Delete(podsinfo[0].ID); err != nil {
					return orgs, err
				}
			} else if err := podw.Update(podinfo); err != nil {
				return orgs, err
			}
		} else {
			if err := podw.Create(&podinfo); err != nil {
				return orgs, err
			}
		}

		// -------------------------------
		if len(pod.Status.ContainerStatuses) == 0 {
			// empty ContainerStatuses
			continue
		}

		var (
			prevContainerID         string
			prevContainerStartedAt  time.Time
			prevContainerFinishedAt time.Time
			prevMessage             string

			currentContainerID         string
			currentContainerStartedAt  time.Time
			currentContainerFinishedAt *time.Time = nil
			currentPhase               instanceinfo.InstancePhase
			currentMessage             string
		)

		mainContainer := getMainContainerStatus(pod.Status.ContainerStatuses, container.Name)
		terminatedContainer := mainContainer.LastTerminationState.Terminated
		if terminatedContainer != nil {
			if len(strings.Split(terminatedContainer.ContainerID, "://")) == 2 {
				runtimeAndId := strings.Split(terminatedContainer.ContainerID, "://")
				prevContainerID = runtimeAndId[1]
			} else {
				prevContainerID = strutil.TrimPrefixes(terminatedContainer.ContainerID, "docker://")
			}
			prevContainerStartedAt = terminatedContainer.StartedAt.Time
			if prevContainerStartedAt.Year() < 2000 {
				prevContainerStartedAt, _ = time.Parse("2006-01", "2000-01")
			}
			prevContainerFinishedAt = terminatedContainer.FinishedAt.Time
			if prevContainerFinishedAt.Year() < 2000 {
				prevContainerFinishedAt, _ = time.Parse("2006-01", "2000-01")
			}
			prevMessage = strutil.Join([]string{terminatedContainer.Reason, terminatedContainer.Message}, ", ", true)
			if len(prevMessage) > 1000 {
				prevMessage = prevMessage[:1000]
			}
			lastExitCode = int(terminatedContainer.ExitCode)
		}
		currentContainer := mainContainer.State.Running
		if currentContainer != nil {
			if len(strings.Split(mainContainer.ContainerID, "://")) == 2 {
				runtimeAndId := strings.Split(mainContainer.ContainerID, "://")
				currentContainerID = runtimeAndId[1]
			} else {
				currentContainerID = strutil.TrimPrefixes(mainContainer.ContainerID, "docker://")
			}
			currentContainerStartedAt = mainContainer.State.Running.StartedAt.Time
			currentPhase = instanceinfo.InstancePhaseUnHealthy
			for _, cond := range pod.Status.Conditions {
				if cond.Type == "Ready" && cond.Status == "True" {
					currentPhase = instanceinfo.InstancePhaseHealthy
					currentMessage = "Ready"
				}
			}
			event, ok := eventmap[pod.Namespace+"/"+pod.Name]
			if ok && strutil.HasSuffixes(event.InvolvedObject.FieldPath,
				"{"+mainContainer.Name+"}") {
				currentMessage = event.Message
				if event.Reason == "Unhealthy" {
					currentPhase = instanceinfo.InstancePhaseUnHealthy
				}
			}
		} else {
			currentTerminatedContainer := mainContainer.State.Terminated
			if currentTerminatedContainer != nil {
				if len(strings.Split(mainContainer.ContainerID, "://")) == 2 {
					runtimeAndId := strings.Split(mainContainer.ContainerID, "://")
					currentContainerID = runtimeAndId[1]
				} else {
					currentContainerID = strutil.TrimPrefixes(mainContainer.ContainerID, "docker://")
				}
				currentContainerStartedAt = mainContainer.State.Terminated.StartedAt.Time
				currentContainerFinishedAt = &mainContainer.State.Terminated.FinishedAt.Time
				if currentContainerFinishedAt.Year() < 2000 {
					t, _ := time.Parse("2006-01", "2000-01")
					currentContainerFinishedAt = &t
				}
				currentPhase = instanceinfo.InstancePhaseDead
				if currentMessage == "" {
					currentMessage = strutil.Join([]string{currentTerminatedContainer.Reason, currentTerminatedContainer.Message}, ", ", true)
					if len(currentMessage) > 1000 {
						currentMessage = currentMessage[:1000]
					}
				}

				exitCode = int(currentTerminatedContainer.ExitCode)
			}
		}
		// -------------------------------
		// 2. Update and create InstanceInfo records
		// -------------------------------
		kvFormat := "%s=%s"
		metaSlice := []string{
			fmt.Sprintf(kvFormat, apistructs.K8sNamespace, k8snamespace),
			fmt.Sprintf(kvFormat, apistructs.K8sPodName, k8spodname),
			fmt.Sprintf(kvFormat, apistructs.K8sContainerName, k8scontainername),
			fmt.Sprintf(kvFormat, apistructs.K8sPodUid, k8spoduid),
		}
		if diceComponent != "" {
			metaSlice = append(metaSlice, fmt.Sprintf(kvFormat, "dice_component", diceComponent))
		}

		meta := strings.Join(metaSlice, ",")
		if prevContainerID != "" {
			instances, err := r.ByOrgName(orgName).
				ByProjectName(projectName).
				ByWorkspace(workspace).
				ByContainerID(prevContainerID).
				ByRuntimeID(runtimeID).
				ByApplicationID(applicationID).
				Do()
			if err != nil {
				return orgs, err
			}
			instance := instanceinfo.InstanceInfo{
				Cluster:             cluster,
				Namespace:           namespace,
				Name:                name,
				OrgName:             orgName,
				OrgID:               orgID,
				ProjectName:         projectName,
				ProjectID:           projectID,
				ApplicationName:     applicationName,
				ApplicationID:       applicationID,
				RuntimeName:         runtimeName,
				RuntimeID:           runtimeID,
				ServiceName:         serviceName,
				EdgeApplicationName: edgeApplicationName,
				EdgeSite:            edgeSite,
				Workspace:           workspace,
				ServiceType:         servicetype(addonID, pipelineID),
				AddonID:             addonID,
				Phase:               instanceinfo.InstancePhaseDead,
				Message:             prevMessage,
				ContainerID:         prevContainerID,
				ContainerIP:         containerIP,
				HostIP:              hostIP,
				StartedAt:           prevContainerStartedAt,
				FinishedAt:          &prevContainerFinishedAt,
				CpuOrigin:           cpuOrigin,
				CpuRequest:          cpuRequest,
				CpuLimit:            cpuLimit,
				MemOrigin:           memOrigin,
				MemRequest:          memRequest,
				MemLimit:            memLimit,
				Image:               image,
				TaskID:              apistructs.K8S,
				ExitCode:            lastExitCode,
				Meta:                meta,
			}
			switch len(instances) {
			case 0:
				if err := w.Create(&instance); err != nil {
					return orgs, err
				}
			default:
				for _, ins := range instances {
					instance.ID = ins.ID
					if err := w.Update(instance); err != nil {
						return orgs, err
					}
				}
			}
			// remove dup instances in db
			instances, err = r.ByContainerID(prevContainerID).ByRuntimeID(runtimeID).ByApplicationID(applicationID).Do()
			if err != nil {
				return orgs, err
			}
			if len(instances) > 1 {
				for i := 1; i < len(instances); i++ {
					if err := w.Delete(instances[i].ID); err != nil {
						return orgs, err
					}
				}
			}
		}
		if currentContainerID != "" {
			instances, err := r.ByContainerID(currentContainerID).ByRuntimeID(runtimeID).ByApplicationID(applicationID).Do()
			if err != nil {
				return orgs, err
			}
			instance := instanceinfo.InstanceInfo{
				Cluster:             cluster,
				Namespace:           namespace,
				Name:                name,
				OrgName:             orgName,
				OrgID:               orgID,
				ProjectName:         projectName,
				ProjectID:           projectID,
				ApplicationName:     applicationName,
				ApplicationID:       applicationID,
				EdgeApplicationName: edgeApplicationName,
				EdgeSite:            edgeSite,
				RuntimeName:         runtimeName,
				RuntimeID:           runtimeID,
				ServiceName:         serviceName,
				Workspace:           workspace,
				ServiceType:         servicetype(addonID, pipelineID),
				AddonID:             addonID,
				Phase:               currentPhase,
				Message:             currentMessage,
				ContainerID:         currentContainerID,
				ContainerIP:         containerIP,
				HostIP:              hostIP,
				StartedAt:           currentContainerStartedAt,
				FinishedAt:          currentContainerFinishedAt,
				CpuOrigin:           cpuOrigin,
				CpuRequest:          cpuRequest,
				CpuLimit:            cpuLimit,
				MemOrigin:           memOrigin,
				MemRequest:          memRequest,
				MemLimit:            memLimit,
				Image:               image,
				TaskID:              apistructs.K8S,
				ExitCode:            exitCode,
				Meta:                meta,
			}
			switch len(instances) {
			case 0:
				if delete {
					break
				}
				if err := w.Create(&instance); err != nil {
					return orgs, err
				}
			default:
				for _, ins := range instances {
					instance.ID = ins.ID
					if delete {
						instance.FinishedAt = &(pod.ObjectMeta.DeletionTimestamp.Time)
						instance.Phase = instanceinfo.InstancePhaseDead
					}
					if err := w.Update(instance); err != nil {
						return orgs, err
					}
				}
			}
			// remove dup instances in db
			instances, err = r.ByContainerID(currentContainerID).ByRuntimeID(runtimeID).ByApplicationID(applicationID).Do()
			if err != nil {
				return orgs, err
			}
			if len(instances) > 1 {
				for i := 1; i < len(instances); i++ {
					if err := w.Delete(instances[i].ID); err != nil {
						return orgs, err
					}
				}
			}
		}
	}
	return orgs, nil
}

func updatePodOnWatch(bdl *bundle.Bundle, db *instanceinfo.Client, addr string) (func(*corev1.Pod), func(*corev1.Pod)) {
	addOrUpdateFunc := func(pod *corev1.Pod) {
		orgs, err := updatePodAndInstance(db, &corev1.PodList{Items: []corev1.Pod{*pod}}, false, nil)
		if err != nil {
			logrus.Errorf("failed to update pod: %v", err)
		}
		exportPodErrInfo(bdl, &corev1.PodList{Items: []corev1.Pod{*pod}}, orgs)
	}
	deleteFunc := func(pod *corev1.Pod) {
		if _, err := updatePodAndInstance(db, &corev1.PodList{Items: []corev1.Pod{*pod}}, true, nil); err != nil {
			logrus.Errorf("failed to update(delete) pod: %v", err)
		}
	}
	return addOrUpdateFunc, deleteFunc
}

func servicetype(addonid, pipelineid string) string {
	if addonid != "" {
		return "addon"
	}
	if pipelineid != "" {
		return "job"
	}
	return "stateless-service"
}

func getContainerByServiceName(containers []corev1.Container, serviceName string) *corev1.Container {

	if serviceName == "" {
		return &containers[0]
	}
	for _, c := range containers {
		if c.Name == "istio-proxy" {
			continue
		}
		for _, e := range c.Env {
			if strings.Contains(strings.ToLower(e.Value), strings.ToLower(serviceName)) {
				return &c
			}
		}
	}
	return &containers[0]
}

func getMainContainerStatus(containers []corev1.ContainerStatus, containerName string) corev1.ContainerStatus {
	for _, c := range containers {
		if c.Name == "istio-proxy" {
			continue
		}
		if c.Name == containerName {
			return c
		}
	}
	return containers[0]
}

func calcPodResource(pod corev1.Pod) (memRequest int, memLimit int, cpuRequest float64, cpuLimit float64) {
	for _, container := range pod.Spec.Containers {
		memRequestByte, ok := container.Resources.Requests.Memory().AsInt64()
		if !ok {
			memRequestByte = 0
		}
		memRequest += int(memRequestByte / 1024 / 1024)
		memLimitByte, ok := container.Resources.Limits.Memory().AsInt64()
		if !ok {
			memLimitByte = 0
		}
		memLimit += int(memLimitByte / 1024 / 1024)
		cpuRequestOne, err := strconv.ParseFloat(container.Resources.Requests.Cpu().AsDec().String(), 64)
		if err != nil {
			cpuRequestOne = 0
		}
		cpuRequest += cpuRequestOne
		cpuLimitOne, err := strconv.ParseFloat(container.Resources.Limits.Cpu().AsDec().String(), 64)
		if err != nil {
			cpuLimitOne = 0
		}
		cpuLimit += cpuLimitOne
	}
	return
}
