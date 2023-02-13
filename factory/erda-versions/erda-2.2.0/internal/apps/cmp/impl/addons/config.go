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

package addons

import (
	"encoding/json"
	"fmt"
	"math"
	"math/rand"
	"net/url"
	"sort"
	"strconv"
	"strings"
	"time"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/bundle"
	"github.com/erda-project/erda/internal/apps/cmp/dbclient"
	"github.com/erda-project/erda/pkg/crypto/uuid"
	"github.com/erda-project/erda/pkg/http/httpclient"
)

type Addons struct {
	db  *dbclient.DBClient
	bdl *bundle.Bundle
}

func New(db *dbclient.DBClient, bdl *bundle.Bundle) *Addons {
	return &Addons{db: db, bdl: bdl}
}

func (a *Addons) GetDBAddonNodeInfo(addonID string) ([]dbclient.AddonNode, error) {
	reader := a.db.AddonNodeReader()
	result, err := reader.ByAddonIDs(addonID).Do()
	if err != nil {
		return nil, err
	}
	return result, nil
}

func (a *Addons) GetDBAddonConfig(addonID string) (*dbclient.AddonManagement, error) {
	reader := a.db.AddonManageReader()
	result, err := reader.ByAddonIDs(addonID).Do()
	if err != nil {
		return nil, err
	}
	if len(result) == 0 {
		return nil, nil
	}
	return &result[0], nil
}

func (a *Addons) UpdateDBAddonConfig(req apistructs.AddonConfigUpdateRequest) error {
	result, err := a.GetDBAddonConfig(req.AddonID)

	if err != nil {
		return err
	}
	if result == nil {
		err := fmt.Errorf("empty record in db")
		return err
	}

	c, err := json.Marshal(req.Config)
	if err != nil {
		return err
	}

	result.AddonConfig = string(c)
	writer := a.db.AddonManageWriter()
	err = writer.Update(*result)
	return err
}

func (a *Addons) UpdateDBAddonScaleInfo(req apistructs.AddonScaleRequest) error {
	result, err := a.GetDBAddonConfig(req.AddonID)

	if err != nil {
		return err
	}
	if result == nil {
		err := fmt.Errorf("empty record in db")
		return err
	}

	result.CPU = req.CPU
	result.Mem = req.Mem
	result.Nodes = req.Nodes

	writer := a.db.AddonManageWriter()
	err = writer.Update(*result)
	return err
}

func (a *Addons) ProjectQuotaCheck(identity apistructs.Identity, req apistructs.ProjectQuotaCheckRequest) (*apistructs.ProjectQuotaCheckResponse, error) {
	// get addon resource need and compare with remained resource
	result, err := a.GetDBAddonConfig(req.AddonID)
	if err != nil {
		logrus.Errorf("get db addon config failed, addonID:%s, error:%v", req.AddonID, err)
		return nil, err
	}
	if result == nil {
		return nil, fmt.Errorf("get empty addon config")
	}

	needCPU := float64(req.Nodes)*req.CPU - float64(result.Nodes)*result.CPU
	needMem := req.Nodes*int(req.Mem) - result.Nodes*int(result.Mem)

	if needMem <= 0 && needCPU <= 0 {
		return &apistructs.ProjectQuotaCheckResponse{
			IsQuotaEnough: true,
		}, nil
	}

	// get raw project info
	pid, err := strconv.Atoi(req.ProjectID)
	projectID := uint64(pid)
	if err != nil {
		return nil, err
	}
	p, err := a.bdl.GetProjectWithSetter(projectID, httpclient.SetParams(url.Values{"withQuota": {"true"}}))
	if err != nil {
		logrus.Errorf("get project failed, pid:%d, error:%v", pid, err)
		return nil, err
	}

	resp, err := a.bdl.ProjectResource([]uint64{projectID})
	if err != nil {
		return nil, err
	}
	v, ok := resp.Data[projectID]
	if !ok {
		return nil, fmt.Errorf("project id %v resource is not available", projectID)
	}
	remainedCPU := p.CpuQuota - v.CpuAddonUsed - v.CpuServiceUsed
	remainedMem := p.MemQuota - v.MemAddonUsed - v.MemServiceUsed

	pqc := apistructs.ProjectQuotaCheckResponse{
		Need: apistructs.BaseResource{
			CPU: needCPU,
			Mem: float64(needMem),
		},
		Remain: apistructs.BaseResource{
			CPU: remainedCPU,
			Mem: remainedMem * 1024,
		},
	}

	if remainedCPU > needCPU && remainedMem > float64(needMem/1024.0) {
		pqc.IsQuotaEnough = true
	}
	return &pqc, nil
}

func (a *Addons) GetAddonConfig(addonID string) (*apistructs.AddonConfigData, error) {
	r, err := a.GetDBAddonConfig(addonID)
	if err != nil {
		return nil, err
	}
	if r == nil {
		return nil, nil
	}

	data := apistructs.AddonConfigData{
		AddonID:    r.AddonID,
		AddonName:  r.Name,
		CPU:        r.CPU,
		Mem:        r.Mem,
		Nodes:      r.Nodes,
		CreateTime: r.CreateTime,
		UpdateTime: r.UpdateTime,
	}

	err = json.Unmarshal([]byte(r.AddonConfig), &data.Config)
	if err != nil {
		return nil, err
	}
	return &data, nil
}

func (a *Addons) GetAddonStatus(namespace, name string) (apistructs.StatusCode, error) {
	// get previous service group
	namespace = strings.Join([]string{"addon-", strings.Replace(strings.Replace(namespace, "terminus-", "", 1), "-operator", "", 1)}, "")

	sg, err := a.bdl.InspectServiceGroup(namespace, name)
	if err != nil {
		if strings.Contains(err.Error(), "not found") {
			return apistructs.StatusStopped, nil
		}
		return apistructs.StatusUnknown, err
	}
	if sg == nil {
		err := fmt.Errorf("empty service group")
		logrus.Errorf("%s, namespace:%s, name: %s", err.Error(), namespace, name)
		return apistructs.StatusUnknown, err
	}
	switch sg.Status {
	case apistructs.StatusReady, apistructs.StatusHealthy:
		return apistructs.StatusHealthy, nil
	case apistructs.StatusError, apistructs.StatusFailing, apistructs.StatusFailed:
		return apistructs.StatusFailed, nil
	case apistructs.StatusUnHealthy:
		return apistructs.StatusUnHealthy, nil
	default:
		return apistructs.StatusProgressing, nil
	}
}

func (a *Addons) UpdateAddonConfig(req apistructs.AddonConfigUpdateRequest) error {
	// get previous service group
	namespace := strings.Join([]string{"addon-", strings.Replace(strings.Replace(req.AddonName, "terminus-", "", 1), "-operator", "", 1)}, "")
	name := req.AddonID

	sg, err := a.bdl.InspectServiceGroup(namespace, name)
	if err != nil {
		logrus.Errorf("inspect service group failed, namespace:%s, name:%s, error:%v", namespace, name, err)
		return err
	}
	if sg == nil {
		err := fmt.Errorf("empty service group")
		logrus.Errorf("%s, namespace:%s, name: %s", err.Error(), namespace, name)
		return err
	}

	// addon service status check, continue update if status is ok
	if sg.Status != apistructs.StatusReady && sg.Status != apistructs.StatusHealthy {
		err := fmt.Errorf("unhealthy service status: %s, check it first", sg.Status)
		logrus.Errorf("%s, namespace:%s, name:%s", err.Error(), namespace, name)
		return err
	}

	// update service group configuration
	for i := range sg.Services {
		if namespace == "addon-elasticsearch" {
			c, err := json.Marshal(req.Config)
			if err != nil {
				return err
			}
			sg.Services[i].Env["config"] = string(c)
		}
	}

	// call scheduler to update service group
	err = a.bdl.ServiceGroupConfigUpdate(*sg)
	if err != nil {
		logrus.Errorf("update service group failed, service group:%+v", sg)
		return err
	}

	// update service group status in db
	err = a.UpdateDBAddonConfig(req)
	if err != nil {
		logrus.Errorf("updaet db addon config failed, requeset:%+v, error:%v", req, err)
		return err
	}
	return nil
}

// getRandomId Generate ID: random letter + uuid (length: 33)
func (a *Addons) getRandomId() string {
	str := "abcdefghijklmnopqrstuvwxyz"
	bytes := []byte(str)
	result := []byte{}
	r := rand.New(rand.NewSource(time.Now().UnixNano()))
	for i := 0; i < 1; i++ {
		result = append(result, bytes[r.Intn(len(bytes))])
	}
	return strings.Join([]string{string(result), uuid.UUID()}, "")
}

func (a *Addons) UpdateAddonNodes(req apistructs.AddonScaleRequest) error {
	// get original addon config
	ac, err := a.GetDBAddonConfig(req.AddonID)
	if err != nil {
		return err
	}
	if ac == nil {
		err := fmt.Errorf("empty record in db")
		return err
	}

	// get original addon node info
	ans, err := a.GetDBAddonNodeInfo(req.AddonID)
	if err != nil {
		return err
	}
	if len(ans) == 0 {
		return fmt.Errorf("empty addon node info")
	}
	activeAns := 0
	for i := range ans {
		if ans[i].Deleted == apistructs.AddonNotDeleted {
			activeAns++
		}
	}

	// check active addon nodes
	if ac.Nodes != activeAns {
		logrus.Warnf("active addons is not equal, addon config num:%d, addon node num:%d", ac.Nodes, activeAns)
	}

	// update addon node info
	sort.Sort(dbclient.AddonNodeList(ans))
	writer := a.db.AddonNodeWriter()

	existPostIndex := true
	tmp := strings.Split(ans[0].NodeName, "-")
	if len(tmp) < 2 {
		existPostIndex = false
	} else if _, err := strconv.Atoi(tmp[len(tmp)-1]); err != nil {
		existPostIndex = false
	}

	namePrefix := ans[0].NodeName
	if existPostIndex {
		namePrefix = strings.Join(tmp[:len(tmp)-1], "-")
	}

	for k, v := range ans {
		if k < req.Nodes {
			// make sure status: not delete && spec[cpu, mem] equal
			if ans[k].Mem != req.Mem || !floatEquals(ans[k].CPU, req.CPU) || ans[k].Deleted == apistructs.AddonDeleted {
				err := writer.Update(dbclient.AddonNode{
					ID:         v.ID,
					InstanceID: v.InstanceID,
					Namespace:  v.Namespace,
					NodeName:   v.NodeName,
					CPU:        req.CPU,
					Mem:        req.Mem,
					Deleted:    apistructs.AddonNotDeleted,
					CreatedAt:  v.CreatedAt,
					UpdatedAt:  time.Now(),
				})
				if err != nil {
					return err
				}
			}
		} else {
			// make sure status deleted
			if v.Deleted != apistructs.AddonDeleted {
				err := writer.Update(dbclient.AddonNode{
					ID:         v.ID,
					InstanceID: v.InstanceID,
					Namespace:  v.Namespace,
					NodeName:   v.NodeName,
					CPU:        v.CPU,
					Mem:        v.Mem,
					Deleted:    apistructs.AddonDeleted,
					CreatedAt:  v.CreatedAt,
					UpdatedAt:  time.Now(),
				})
				if err != nil {
					return err
				}
			}
		}
	}

	// create new item
	if len(ans) < req.Nodes {
		for i := len(ans) + 1; i <= req.Nodes; i++ {
			nodeName := namePrefix
			if existPostIndex {
				nodeName = strings.Join([]string{namePrefix, strconv.Itoa(i)}, "-")
			}
			_, err := writer.Create(&dbclient.AddonNode{
				ID:         a.getRandomId(),
				InstanceID: ans[0].InstanceID,
				Namespace:  ans[0].Namespace,
				NodeName:   nodeName,
				CPU:        req.CPU,
				Mem:        req.Mem,
				Deleted:    apistructs.AddonNotDeleted,
				CreatedAt:  time.Time{},
				UpdatedAt:  time.Time{},
			})
			if err != nil {
				return err
			}
		}
	}

	return nil
}

func (a *Addons) AddonScalePrecheck(req apistructs.AddonScaleRequest) error {
	if req.Nodes <= 0 || req.Nodes%2 == 0 {
		return fmt.Errorf("invalid parm, nodes must be great than 0 and odd")
	}
	if req.Mem < 512 {
		return fmt.Errorf("invalid parm, memory must >= 512M")
	}
	if req.CPU < 0.5-eps {
		return fmt.Errorf("invalid parm, cpu must >= 0.5Core")
	}
	return nil
}

func (a *Addons) AddonScale(identity apistructs.Identity, req apistructs.AddonScaleRequest) error {
	namespace := strings.Join([]string{"addon-", strings.Replace(strings.Replace(req.AddonName, "terminus-", "", 1), "-operator", "", 1)}, "")
	name := req.AddonID
	req.AddonName = namespace

	// request pre check
	if err := a.AddonScalePrecheck(req); err != nil {
		return err
	}

	// Project remain quota check
	pqc, err := a.ProjectQuotaCheck(identity, apistructs.ProjectQuotaCheckRequest(req))
	if err != nil {
		logrus.Errorf("project quota check failed, request:%+v, error:%v", req, err)
		return err
	}
	if !pqc.IsQuotaEnough {
		err := fmt.Errorf("project remain quota is not enough, remain quota:%v, need quota:%v", pqc.Remain, pqc.Need)
		return err
	}

	sg, err := a.bdl.InspectServiceGroup(namespace, name)
	if err != nil {
		logrus.Errorf("inspect service group failed, namespace:%s, name:%s, error:%v", namespace, name, err)
		return err
	}
	if sg == nil {
		err := fmt.Errorf("empty service group")
		logrus.Errorf("%s, namespace:%s, name: %s", err.Error(), namespace, name)
		return err
	}

	// addon service status check, continue update if status is ok
	if sg.Status != apistructs.StatusReady && sg.Status != apistructs.StatusHealthy {
		err := fmt.Errorf("unhealthy service status: %s, check it first", sg.Status)
		logrus.Errorf("%s, namespace:%s, name:%s", err.Error(), namespace, name)
		return err
	}

	// update service group configuration
	for i := range sg.Services {
		if namespace == "addon-elasticsearch" {
			sg.Services[i].Resources.Cpu = req.CPU
			sg.Services[i].Resources.Mem = float64(req.Mem)
			sg.Services[i].Scale = req.Nodes
		}
	}

	// call scheduler to update service group
	err = a.bdl.ServiceGroupConfigUpdate(*sg)
	if err != nil {
		logrus.Errorf("update service group failed, service group:%+v, error:%v", sg, err)
		return err
	}

	err = a.UpdateDBAddonScaleInfo(req)
	if err != nil {
		logrus.Errorf("updaet db addon scale info failed, request:%+v, err:%v", req, err)
		return err
	}

	err = a.UpdateAddonNodes(req)
	if err != nil {
		logrus.Errorf("update addon node info failed, request:%+v, error:%v", req, err)
		return err
	}

	return nil
}

var eps = 0.00000001 // Set tolerance
func floatEquals(a, b float64) bool {
	if math.Abs(a-b) < eps {
		return true
	}
	return false
}
