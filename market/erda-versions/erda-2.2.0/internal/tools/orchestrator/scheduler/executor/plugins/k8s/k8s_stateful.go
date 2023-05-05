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

package k8s

import (
	"strconv"
	"strings"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	extensionsv1beta1 "k8s.io/api/extensions/v1beta1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/clusterinfo"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/k8serror"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/util"
	"github.com/erda-project/erda/pkg/strutil"
)

func groupStatefulset(sg *apistructs.ServiceGroup) ([]*apistructs.ServiceGroup, error) {
	group, ok := getGroupNum(sg)
	if !ok || group == "1" {
		return []*apistructs.ServiceGroup{sg}, nil
	}

	num, err := strutil.Atoi64(group)
	if err != nil {
		return nil, errors.Errorf("failed to parse addon group number, (%v)", err)
	}

	logrus.Infof("parsed multi addon group, name: %s, number: %v", sg.ID, num)

	// store The mapping between the group ID and the service under the ID
	// e.g. redis : [redis-1, redis-2]
	store := make(map[string][]apistructs.Service)
	// rstore is the reverse storage of store, the mapping between service and group identification
	rstore := make(map[string]string)
	// The dependency between the group ID and the group ID, the key depends on the value
	groupDep := make(map[string][]string)

	for i := range sg.Services {
		key, ok := getGroupID(&sg.Services[i])
		if !ok {
			return nil, errors.Errorf("failed to get GROUP_ID, name: %s", sg.Services[i].Name)
		}

		store[key] = append(store[key], sg.Services[i])
		rstore[sg.Services[i].Name] = key
	}

	if int(num) != len(store) {
		return nil, errors.Errorf("addons group num error, specified num: %v, group num: %v", num, len(store))
	}

	var groups []*apistructs.ServiceGroup
	for k, v := range store {
		serviceGroup := &apistructs.ServiceGroup{
			ClusterName:   sg.ClusterName,
			Extra:         sg.Extra,
			ScheduleInfo:  sg.ScheduleInfo,
			ScheduleInfo2: sg.ScheduleInfo2,
		}

		serviceGroup.Dice.ID = k
		serviceGroup.Dice.Type = sg.Dice.Type
		serviceGroup.Dice.Services = v
		// Record all service names under a group, e.g. redis has [redis-1, redis-2] under this groupID
		groupSrv := map[string]bool{}
		for i := range v {
			groupSrv[v[i].Name] = true
		}

		// Fix the depends field of service
		// The dependency of any service under the group can only be the service under the group
		// The dependency of the service across the group is converted into the dependency between the groups, that is, the order of creation between statefulsets
		for i := range v {
			idx := 0
			for _, depend := range v[i].Depends {
				if _, ok := groupSrv[depend]; ok {
					v[i].Depends[idx] = depend
					idx++
				} else if !existedInSlice(groupDep[k], rstore[depend]) {
					groupDep[k] = append(groupDep[k], rstore[depend])
				}
			}
			v[i].Depends = v[i].Depends[:idx]
			logrus.Infof("service and its depends, services: %s, depends: %v", v[i].Name, v[i].Depends)
		}
		groups = append(groups, serviceGroup)
	}

	// Construct a virtualGroup, adjust the order of groups according to groupDep, and ensure that the dependent statefulset is created first
	virtualGroup := &apistructs.ServiceGroup{}
	for i := range groups {
		virtualGroup.Services = append(virtualGroup.Services, apistructs.Service{
			Name: groups[i].ID,
		})
	}

	for k, v := range groupDep {
		for i := range virtualGroup.Services {
			if k == virtualGroup.Services[i].Name {
				virtualGroup.Services[i].Depends = v
				break
			}
		}
	}
	layers, err := util.ParseServiceDependency(virtualGroup)
	if err != nil {
		return nil, errors.Errorf("failed to adjust group sequence, (%v)", err)
	}
	// sortedGroup arranges group id in the order of dependency arranged by virtualGroup
	sortedGroup := []string{}
	for i := range layers {
		for j := range layers[i] {
			sortedGroup = append(sortedGroup, layers[i][j].Name)
		}
	}
	//
	for i := range groups {
		if groups[i].ID == sortedGroup[i] {
			continue
		}
		for j := i + 1; j < len(groups); j++ {
			if groups[j].ID != sortedGroup[i] {
				continue
			}
			groups[i], groups[j] = groups[j], groups[i]
			break
		}
	}
	return groups, nil
}

// Initialize annotations, the purpose of annotations is to record the original service name corresponding to the number
// annotations not only need to record the number (NO, N1...) in a statefulset,
// Also record the group number of the group, because some environment variables will depend on cross-group
// For example, 1 master 1 slave 3 sentinel redis runtime, 1 master 1 slave is a group (within a statefulset),
// The three sentinels are a group, and sentinel has the dependency of the master and slave environment variables.
// That is, there are environment variables such as ${REDIS_MASTER_HOST}, ${REDIS_SLAVE_PORT} in sentinel

// format: G0_N0
// globalSeq is the global group number, which represents how many groups the service belongs to
// N0 is the sequence number in a group
func initAnnotations(layers [][]*apistructs.Service, globalSeq int) map[string]string {
	order := 0
	annotations := map[string]string{}
	for _, layer := range layers {
		for j := range layer {
			// Record the original service name corresponding to the number
			// e.g. annotations["G0_N1"]="redis-slave"
			key := strutil.Concat("G", strconv.Itoa(globalSeq), "_N", strconv.Itoa(order))
			annotations[key] = layer[j].Name

			// e.g. annotations["redis-slave"]="G0_N1"
			annotations[layer[j].Name] = key

			// e.g. annotations["G0_ID"]="redis"
			// annotations[strutil.Concat("G", strconv.Itoa(globalSeq), "_ID")] = layer[j].Labels[groupID]
			annotations[strutil.Concat("G", strconv.Itoa(globalSeq), "_ID")], _ = getGroupID(layer[j])

			// Record the PORT of each service
			// redis-slave -> redis_slave -> REDIS_SLAVE_PORT
			if len(layer[j].Ports) > 0 {
				name := strings.Replace(layer[j].Name, "-", "_", -1)
				annotations[strutil.Concat(strings.ToUpper(name), "_PORT")] = strconv.Itoa(layer[j].Ports[0].Port)
			}

			if j == len(layer)-1 {
				break
			}
			order++
		}
		order++
	}
	return annotations
}

// 1， Determine the number of each service, starting from 0
// 2， Collect environment variables of each service
func (k *Kubernetes) initGroupEnv(layers [][]*apistructs.Service, annotations map[string]string) map[string]string {
	order := 0
	allEnv := make(map[string]string)

	for _, layer := range layers {
		for j := range layer {
			for k, v := range layer[j].Env {
				globalKey := strutil.Concat("N", strconv.Itoa(order), "_", k)
				if str, ok := parseSpecificEnv(v, annotations); ok {
					v = str
				}
				allEnv[globalKey] = v
			}

			ciEnvs, err := k.ClusterInfo.Get()
			if err != nil {
				logrus.Error(err)
			} else {
				clusterName, ok := ciEnvs[clusterinfo.DiceClusterName]
				if ok {
					globalKey := strutil.Concat("N", strconv.Itoa(order), "_", clusterinfo.DiceClusterName)
					allEnv[globalKey] = clusterName
				}
			}

			if j == len(layer)-1 {
				break
			}
			order++
		}
		order++
	}
	return allEnv
}

// Resolve the environment variables with variables in the middleware, such as TERMINUS_ZOOKEEPER_1_HOST=${terminus-zookeeper-1}
func parseSpecificEnv(val string, annotations map[string]string) (string, bool) {
	results := envReg.FindAllString(val, -1)
	if len(results) == 0 {
		return "", false
	}
	replace := make(map[string]string)

	logrus.Infof("in parsing specific env: %s, annotations: %+v", val, annotations)

	for _, str := range results {
		if len(str) <= 3 {
			continue
		}
		// e.g. ${REDIS_HOST} -> REDIS_HOST
		key := str[2 : len(str)-1]
		// Currently only supports parsing _HOST, _PORT type variables in variables
		if strings.Contains(key, "_HOST") {
			pos := strings.LastIndex(key, "_")
			name := strings.TrimSuffix(key[:pos], "_HOST")
			name = toServiceName(name)

			// The seq value "G0_N1" represents the 0th group (statefulset), the serial number in this group is 1
			bigSeq, ok := annotations[name]
			if !ok {
				logrus.Errorf("failed to parse env as not found in annotations,"+
					" var: %s, name: %s, annotations: %+v", key, name, annotations)
				break
			}
			seqs := strings.Split(bigSeq, "_")
			if len(seqs) != 2 {
				logrus.Errorf("failed to parse env seq, key: %s, bigSeq: %s", key, bigSeq)
				break
			}
			// "N1" -> "1"
			seq := seqs[1][1:]
			// e.g. G0_ID, G1_ID, Group ID
			id, ok := annotations[strutil.Concat(seqs[0], "_ID")]
			if !ok {
				logrus.Errorf("failed to get group id from annotations, key: %s, groupseq: %s", key, seqs[0])
			}

			bracedKey := strutil.Concat("${", key, "}")

			ns, ok := annotations["K8S_NAMESPACE"]
			if ok {
				replace[bracedKey] = strutil.Concat(id, "-", seq, ".", id, ".", ns, ".svc.cluster.local")
			} else {
				// e.g. The id set by the user is web, and the instance serial number in the statefulset is 1, then the short domain name of the pod is web-1.web
				replace[bracedKey] = strutil.Concat(id, "-", seq, ".", id)
			}

		} else if strings.Contains(key, "_PORT") {
			port, ok := annotations[key]
			if !ok {
				logrus.Errorf("failed to parse env as not found in annotations, key: %s, annotations: %+v", key, annotations)
				break
			}
			bracedKey := strutil.Concat("${", key, "}")
			replace[bracedKey] = port
		}
	}

	if len(replace) == 0 {
		logrus.Infof("debug parseSpecificEnv empty replace")
		return "", false
	}

	before := val
	for k, v := range replace {
		val = strings.Replace(val, k, v, 1)
	}
	logrus.Infof("succeed to convert env, before: %s, after: %s, replace: %+v", before, val, replace)
	return val, true
}

// "TERMINUS_ZOOKEEPER_1" -> "terminus-zookeeper-1"
func toServiceName(origin string) string {
	return strings.Replace(strings.ToLower(origin), "_", "-", -1)
}

// Create a statefulset service, each instance under statefulset has a corresponding dns domain name
// Domain name rules for each instance：{podName}.{serviceName}.{namespace}.svc.cluster.local
// Not in use headless service
func (k *Kubernetes) createStatefulService(sg *apistructs.ServiceGroup) error {
	if len(sg.Services[0].Ports) == 0 {
		return nil
	}
	// TODO: Distinguish from stateless services
	// Build a statefulset service
	svc := sg.Services[0]

	svc.Name = statefulsetName(sg)
	k8sSvc := newService(&svc, nil)

	if err := k.service.Create(k8sSvc); err != nil {
		return err
	}
	v, ok := sg.Services[0].Labels["HAPROXY_0_VHOST"]
	// No external domain name
	if !ok {
		return nil
	}
	// Forward the domain name/vip set corresponding to HAPROXY_0_VHOST in the label to the 0th port of the service
	publicHosts := strings.Split(v, ",")
	if len(publicHosts) == 0 {
		return nil
	}
	// create ingress
	rules := buildRules(publicHosts, svc.Name, sg.Services[0].Ports[0].Port)
	tls := buildTLS(publicHosts)
	ingress := &extensionsv1beta1.Ingress{
		TypeMeta: metav1.TypeMeta{
			APIVersion: "extensions/v1beta1",
			Kind:       "Ingress",
		},
		ObjectMeta: metav1.ObjectMeta{
			Name:      svc.Name,
			Namespace: svc.Namespace,
		},
		Spec: extensionsv1beta1.IngressSpec{
			Rules: rules,
			TLS:   tls,
		},
	}

	return k.ingress.Create(ingress)
}

// TODO: State need more precise
func (k *Kubernetes) GetStatefulStatus(sg *apistructs.ServiceGroup) (apistructs.StatusDesc, error) {
	var status apistructs.StatusDesc
	namespace := MakeNamespace(sg)
	statefulName := sg.Services[0].Name
	idx := strings.LastIndex(statefulName, "-")
	if idx > 0 {
		statefulName = statefulName[:idx]
	}

	logrus.Infof("in getStatefulStatus, name: %s, namespace: %s", statefulName, namespace)

	// only one statefulset
	if !strings.HasPrefix(namespace, "group-") {
		return k.getOneStatus(namespace, statefulName)
	}
	// have many statefulset， Need to combine state
	groups, err := groupStatefulset(sg)
	if err != nil {
		return status, err
	}
	for i := range groups {
		//name := groups[i].Services[0].Name
		name := groups[i].ID
		oneStatus, err := k.getOneStatus(namespace, name)
		if err != nil {
			return status, err
		}
		if oneStatus.Status == apistructs.StatusProgressing {
			return oneStatus, nil
		}
	}
	status.Status = apistructs.StatusReady
	return status, nil
}

func (k *Kubernetes) getOneStatus(namespace, name string) (apistructs.StatusDesc, error) {
	logrus.Infof("in getOneStatus, name: %s, namespace: %s", name, namespace)
	var status apistructs.StatusDesc
	set, err := k.sts.Get(namespace, name)
	if err != nil {
		if err == k8serror.ErrNotFound {
			status.Status = apistructs.StatusProgressing
			status.LastMessage = "currently could not get the pod"
			return status, nil
		}
		return status, err
	}
	var replica int32 = 1
	if set.Spec.Replicas != nil {
		replica = *set.Spec.Replicas
	}
	if replica == set.Status.Replicas &&
		replica == set.Status.ReadyReplicas &&
		replica == set.Status.UpdatedReplicas {
		status.Status = apistructs.StatusReady
	} else {
		status.Status = apistructs.StatusProgressing
	}

	msgList, err := k.event.AnalyzePodEvents(namespace, name)
	if err != nil {
		logrus.Errorf("failed to analyze k8s events, namespace: %s, name: %s, (%v)",
			namespace, name, err)
	}
	if len(msgList) > 0 {
		status.LastMessage = msgList[len(msgList)-1].Comment
	}

	return status, nil
}

func (k *Kubernetes) inspectOne(g *apistructs.ServiceGroup, namespace, name string, groupNum int) (*OneGroupInfo, error) {
	set, err := k.sts.Get(namespace, name)
	if err != nil {
		return nil, err
	}
	sg := &apistructs.ServiceGroup{
		ClusterName:  g.ClusterName,
		Executor:     g.Executor,
		ScheduleInfo: g.ScheduleInfo,
		Dice: apistructs.Dice{
			ID:   set.Annotations["RUNTIME_NAME"],
			Type: set.Annotations["RUNTIME_NAMESPACE"],
		},
	}
	replica := *(set.Spec.Replicas)
	container := &set.Spec.Template.Spec.Containers[0]

	envs := make(map[string]string)
	for _, env := range container.Env {
		envs[env.Name] = env.Value
	}

	for i := 0; i < int(replica); i++ {
		podName := strutil.Concat(container.Name, "-", strconv.Itoa(i))
		pod, err := k.pod.Get(namespace, podName)
		if err != nil && err != k8serror.ErrNotFound {
			return nil, err
		}
		key := strutil.Concat("G", strconv.Itoa(groupNum), "_N", strconv.Itoa(i))
		serviceName, ok := set.Annotations[key]
		if !ok {
			return nil, errors.Errorf("failed to get key from annotations, podname: %s, key: %s, annotations: %v",
				podName, key, set.Annotations)
		}

		podStatus := apistructs.StatusProgressing
		podIP := ""
		if err == nil {
			podStatus = convertStatus(pod.Status)
			podIP = pod.Status.PodIP
		}

		msgList, err := k.event.AnalyzePodEvents(namespace, podName)
		if err != nil {
			logrus.Errorf("failed to analyze job events, namespace: %s, name: %s, (%v)",
				namespace, name, err)
		}

		var lastMsg string
		if len(msgList) > 0 {
			lastMsg = msgList[len(msgList)-1].Comment
		}

		sg.Services = append(sg.Services, apistructs.Service{
			Name:     serviceName,
			Vip:      strutil.Concat(name, ".", namespace, ".svc.cluster.local"),
			ShortVIP: name,
			Env:      envs,
			StatusDesc: apistructs.StatusDesc{
				Status:      podStatus,
				LastMessage: lastMsg,
			},
			Image:         container.Image,
			InstanceInfos: []apistructs.InstanceInfo{{Ip: podIP}},
		})
	}

	sg.Status = apistructs.StatusReady
	for i := range sg.Services {
		if sg.Services[i].Status != apistructs.StatusReady {
			sg.Status = apistructs.StatusProgressing
			break
		}
	}
	groupInfo := &OneGroupInfo{
		sg:  sg,
		sts: set,
	}
	return groupInfo, nil
}

// inspect multiple statefulset
func (k *Kubernetes) inspectGroup(g *apistructs.ServiceGroup, namespace, name string) (*apistructs.ServiceGroup, error) {
	mygroups, err := groupStatefulset(g)
	if err != nil {
		logrus.Errorf("failed to get groups sequence in inspectgroup, namespace: %s, name: %s", g.Type, g.ID)
		return nil, err
	}

	var groupsInfo []*OneGroupInfo
	for i, group := range mygroups {
		// First find groupNum
		//for k, v := range
		oneGroup, err := k.inspectOne(g, namespace, group.ID, i)
		if err != nil {
			return nil, err
		}
		groupsInfo = append(groupsInfo, oneGroup)
	}
	logrus.Infof("debug: inspectGroup, groupsInfo: %+v, Extra: %+v", groupsInfo, g.Extra)

	if len(groupsInfo) <= 1 {
		return nil, errors.Errorf("failed to parse multi group, namespace: %s, name: %s", namespace, name)
	}
	isReady := true
	for i := range g.Services {
		for j := range groupsInfo {
			for k := range groupsInfo[j].sg.Services {
				if groupsInfo[j].sg.Services[k].Name != g.Services[i].Name {
					continue
				}
				g.Services[i] = groupsInfo[j].sg.Services[k]
				if g.Services[i].Status != apistructs.StatusReady {
					isReady = false
				}
			}
		}
	}
	if isReady {
		g.Status = apistructs.StatusReady
	}

	return g, nil
}

func existedInSlice(array []string, elem string) bool {
	for _, x := range array {
		if x == elem {
			return true
		}
	}
	return false
}

// todo: Compatible with old labels
func getGroupNum(sg *apistructs.ServiceGroup) (string, bool) {
	if group, ok := sg.Labels[groupNum]; ok {
		return group, ok
	}
	group, ok := sg.Labels[groupNum2]
	return group, ok
}

func getGroupID(svc *apistructs.Service) (string, bool) {
	if id, ok := svc.Labels[groupID]; ok {
		return id, ok
	}
	id, ok := svc.Labels[groupID2]
	return id, ok
}
