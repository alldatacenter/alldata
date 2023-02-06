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

package v2

import (
	"encoding/json"
	"strings"

	"github.com/pkg/errors"
	log "github.com/sirupsen/logrus"

	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common/util"
	. "github.com/erda-project/erda/internal/tools/orchestrator/hepa/common/vars"
	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/kong/base"
	. "github.com/erda-project/erda/internal/tools/orchestrator/hepa/kong/dto"
)

const (
	ConsumerRoot string = "/consumers/"
	PluginRoot   string = "/plugins/"
	ServiceRoot  string = "/services/"
	RouteRoot    string = "/routes/"
	AclRoot      string = "/acls/"
	UpstreamRoot string = "/upstreams/"
	HealthPath   string = "/health/"
	TargetPath   string = "/targets/"
)

var (
	ErrInvalidReq = errors.New("kongAdapter: invalid request")
)

type KongAdapterImpl struct {
	*base.KongAdapterImpl
}

func (impl *KongAdapterImpl) KongExist() bool {
	if impl == nil || impl.KongAdapterImpl == nil {
		return false
	}
	return true
}

func (impl *KongAdapterImpl) GetPlugin(req *KongPluginReqDto) (*KongPluginRespDto, error) {
	if impl == nil {
		return nil, errors.New("kong can't be attached")
	}
	if req == nil || req.Name == "" {
		return nil, errors.New(ERR_INVALID_ARG)
	}
	url := impl.KongAddr + PluginRoot
	var args []string
	args = append(args, "name="+req.Name)
	if req.RouteId != "" {
		url = impl.KongAddr + RouteRoot + req.RouteId + PluginRoot
	} else if req.ServiceId != "" {
		url = impl.KongAddr + ServiceRoot + req.ServiceId + PluginRoot
	} else if req.ConsumerId != "" {
		url = impl.KongAddr + ConsumerRoot + req.ConsumerId + PluginRoot
	}
	code, body, err := util.DoCommonRequest(impl.Client, "GET",
		url+"?"+strings.Join(args, "&"), nil)
	if err != nil {
		return nil, errors.Wrap(err, "request failed")
	}
	if code == 200 {
		respDto := &KongPluginsDto{}
		err = json.Unmarshal(body, respDto)
		if err != nil {
			return nil, errors.Wrap(err, ERR_JSON_FAIL)
		}
		if len(respDto.Data) > 0 {
			respDto.Data[0].Compatiable()
			return &respDto.Data[0], nil
		}
		return nil, nil
	}
	return nil, errors.Errorf("get plugin failed: code[%d] msg[%s]", code, body)
}

func (impl *KongAdapterImpl) CreateOrUpdatePluginById(req *KongPluginReqDto) (*KongPluginRespDto, error) {
	if impl == nil {
		return nil, errors.New("kong can't be attached")
	}
	if req == nil {
		return nil, errors.New(ERR_INVALID_ARG)
	}
	req.ToV2()
	url := impl.KongAddr + PluginRoot
	method := "POST"
	if len(req.Id) != 0 {
		method = "PUT"
		url += req.Id
		req.Id = ""
	}
	code, body, err := util.DoCommonRequest(impl.Client, method, url, req)
	if err != nil {
		return nil, errors.Wrap(err, "request failed")
	}
	if code == 201 || code == 200 {
		respDto := &KongPluginRespDto{}
		err = json.Unmarshal(body, respDto)
		if err != nil {
			return nil, errors.Wrap(err, ERR_JSON_FAIL)
		}
		respDto.Compatiable()
		return respDto, nil
	}
	return nil, errors.Errorf("CreateOrUpdatePlugin failed: code[%d] msg[%s]", code, body)
}

func (impl *KongAdapterImpl) DeletePluginIfExist(req *KongPluginReqDto) error {
	enabled, err := impl.CheckPluginEnabled(req.Name)
	if err != nil {
		return err
	}
	if !enabled {
		log.Warnf("plugin %s not enabled, req:%+v", req.Name, req)
		return nil
	}
	exist, err := impl.GetPlugin(req)
	if err != nil {
		return err
	}
	if exist == nil {
		return nil
	}
	return impl.RemovePlugin(exist.Id)
}

func (impl *KongAdapterImpl) CreateOrUpdatePlugin(req *KongPluginReqDto) (*KongPluginRespDto, error) {
	enabled, err := impl.CheckPluginEnabled(req.Name)
	if err != nil {
		return nil, err
	}
	if !enabled {
		log.Warnf("plugin %s not enabled, req:%+v", req.Name, req)
		return nil, nil
	}
	exist, err := impl.GetPlugin(req)
	if err != nil {
		return nil, err
	}
	if exist == nil {
		return impl.AddPlugin(req)
	}
	req.Id = exist.Id
	req.PluginId = exist.Id
	return impl.PutPlugin(req)
}

func (impl *KongAdapterImpl) AddPlugin(req *KongPluginReqDto) (*KongPluginRespDto, error) {
	if impl == nil {
		return nil, errors.New("kong can't be attached")
	}
	if req == nil {
		return nil, errors.New(ERR_INVALID_ARG)
	}
	req.ToV2()
	enabled, err := impl.CheckPluginEnabled(req.Name)
	if err != nil {
		return nil, err
	}
	if !enabled {
		log.Warnf("plugin %s not enabled, req:%+v", req.Name, req)
		return nil, nil
	}
	code, body, err := util.DoCommonRequest(impl.Client, "POST", impl.KongAddr+PluginRoot, req)
	if err != nil {
		return nil, errors.Wrap(err, "request failed")
	}
	if code == 201 {
		respDto := &KongPluginRespDto{}
		err = json.Unmarshal(body, respDto)
		if err != nil {
			return nil, errors.Wrap(err, ERR_JSON_FAIL)
		}
		respDto.Compatiable()
		return respDto, nil
	}
	return nil, errors.Errorf("AddPlugin failed: code[%d] msg[%s]", code, body)
}

func (impl *KongAdapterImpl) PutPlugin(req *KongPluginReqDto) (*KongPluginRespDto, error) {
	if impl == nil {
		return nil, errors.New("kong can't be attached")
	}
	if req == nil {
		return nil, errors.New(ERR_INVALID_ARG)
	}
	req.ToV2()
	enabled, err := impl.CheckPluginEnabled(req.Name)
	if err != nil {
		return nil, err
	}
	if !enabled {
		log.Warnf("plugin %s not enabled, req:%+v", req.Name, req)
		return nil, nil
	}
	code, body, err := util.DoCommonRequest(impl.Client, "PUT", impl.KongAddr+PluginRoot+req.PluginId, req)
	if err != nil {
		return nil, errors.Wrap(err, "request failed")
	}
	if code == 200 || code == 201 {
		respDto := &KongPluginRespDto{}
		err = json.Unmarshal(body, respDto)
		if err != nil {
			return nil, errors.Wrapf(err, "body[%s] Unmarshal failed", body)
		}
		respDto.Compatiable()
		return respDto, nil
	}
	return nil, errors.Errorf("UpdatePlugin failed: code[%d] msg[%s]", code, body)
}

func (impl *KongAdapterImpl) UpdatePlugin(req *KongPluginReqDto) (*KongPluginRespDto, error) {
	if impl == nil {
		return nil, errors.New("kong can't be attached")
	}
	if req == nil {
		return nil, errors.New(ERR_INVALID_ARG)
	}
	req.ToV2()
	code, body, err := util.DoCommonRequest(impl.Client, "PATCH", impl.KongAddr+PluginRoot+req.PluginId, req)
	if err != nil {
		return nil, errors.Wrap(err, "request failed")
	}
	if code == 200 || code == 201 {
		respDto := &KongPluginRespDto{}
		err = json.Unmarshal(body, respDto)
		if err != nil {
			return nil, errors.Wrapf(err, "body[%s] Unmarshal failed", body)
		}
		respDto.Compatiable()
		return respDto, nil
	}
	return nil, errors.Errorf("UpdatePlugin failed: code[%d] msg[%s]", code, body)
}
func (impl *KongAdapterImpl) CreateCredential(req *KongCredentialReqDto) (*KongCredentialDto, error) {
	if impl == nil {
		return nil, errors.New("kong can't be attached")
	}
	if req == nil {
		return nil, errors.New(ERR_INVALID_ARG)
	}
	req.Config.ToV2()
	if req.PluginName == "hmac-auth" {
		req.Config.ToHmacReq()
	}
	code, body, err := util.DoCommonRequest(impl.Client, "POST", impl.KongAddr+ConsumerRoot+req.ConsumerId+"/"+req.PluginName, req.Config)
	if err != nil {
		return nil, errors.Wrap(err, "request failed")
	}
	if code == 201 {
		respDto := &KongCredentialDto{}
		err = json.Unmarshal(body, respDto)
		if err != nil {
			return nil, errors.Wrap(err, ERR_JSON_FAIL)
		}
		if req.PluginName == "hmac-auth" {
			respDto.ToHmacResp()
		}
		respDto.Compatiable()
		return respDto, nil
	}
	return nil, errors.Errorf("CreateCredential failed: code[%d] msg[%s]", code, body)
}

func (impl *KongAdapterImpl) GetCredentialList(consumerId, pluginName string) (*KongCredentialListDto, error) {
	if impl == nil {
		return nil, errors.New("kong can't be attached")
	}
	code, body, err := util.DoCommonRequest(impl.Client, "GET", impl.KongAddr+ConsumerRoot+consumerId+"/"+pluginName, nil)
	if err != nil {
		return nil, errors.Wrap(err, "request failed")
	}
	if code == 200 {
		respDto := &KongCredentialListDto{}
		err = json.Unmarshal(body, respDto)
		if err != nil {
			return nil, errors.Wrapf(err, "json unmashal failed, body:%s", body)
		}
		for i := 0; i < len(respDto.Data); i++ {
			respDto.Data[i].AdjustCreatedAt()
			respDto.Data[i].Compatiable()
			if pluginName == "hmac-auth" {
				respDto.Data[i].ToHmacResp()
			}
		}
		respDto.Total = int64(len(respDto.Data))
		return respDto, nil
	}
	return nil, errors.Errorf("GetCredentialList failed: code[%d] msg[%s]", code, body)
}
