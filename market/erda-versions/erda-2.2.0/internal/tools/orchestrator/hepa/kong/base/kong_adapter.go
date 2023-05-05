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

package base

import (
	"encoding/json"
	"net/http"
	"strings"
	"time"

	"github.com/pkg/errors"
	log "github.com/sirupsen/logrus"

	"github.com/erda-project/erda/internal/tools/orchestrator/hepa/common/util"
	. "github.com/erda-project/erda/internal/tools/orchestrator/hepa/common/vars"
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
	KongAddr string
	Client   *http.Client
}

func (impl *KongAdapterImpl) KongExist() bool {
	return impl != nil
}

func (impl *KongAdapterImpl) CreateConsumer(req *KongConsumerReqDto) (*KongConsumerRespDto, error) {
	if impl == nil {
		return nil, errors.New("kong can't be attached")
	}
	if req == nil {
		return nil, errors.New(ERR_INVALID_ARG)
	}
	code, body, err := util.DoCommonRequest(impl.Client, "POST", impl.KongAddr+ConsumerRoot, req)
	if err != nil {
		return nil, errors.Wrap(err, "request failed")
	}
	if code == 201 {
		respDto := &KongConsumerRespDto{}
		err = json.Unmarshal(body, respDto)
		if err != nil {
			return nil, errors.Wrap(err, ERR_JSON_FAIL)
		}
		return respDto, nil
	}
	return nil, errors.Errorf("CreateConsumer failed: code[%d] msg[%s]", code, body)
}

func (impl *KongAdapterImpl) DeleteConsumer(id string) error {
	if impl == nil {
		return errors.New("kong can't be attached")
	}
	if len(id) == 0 {
		return errors.New(ERR_INVALID_ARG)
	}
	code, body, err := util.DoCommonRequest(impl.Client, "DELETE", impl.KongAddr+ConsumerRoot+id, nil)
	if err != nil {
		return errors.Wrap(err, "request failed")
	}
	if code == 204 || code == 404 {
		return nil
	}
	return errors.Errorf("DeleteConsumer failed: code[%d] msg[%s]", code, body)
}

func (impl *KongAdapterImpl) UpdateRoute(req *KongRouteReqDto) (*KongRouteRespDto, error) {
	if impl == nil {
		return nil, errors.New("kong can't be attached")
	}
	if req == nil || req.RouteId == "" {
		return nil, errors.New(ERR_INVALID_ARG)
	}
	req.Adjust(Versioning(impl))
	url := impl.KongAddr + RouteRoot + req.RouteId
	code, body, err := util.DoCommonRequest(impl.Client, "PATCH", url, req)
	if err != nil {
		return nil, errors.Wrap(err, "request failed")
	}
	if code == 201 || code == 200 {
		respDto := &KongRouteRespDto{}
		err = json.Unmarshal(body, respDto)
		if err != nil {
			return nil, errors.Wrap(err, ERR_JSON_FAIL)
		}
		return respDto, nil
	}
	// handle invalid path
	if code == 400 {
		log.Errorf("CreateOrUpdateRoute failed: code[%d] msg[%s]", code, body)
		return nil, ErrInvalidReq
	}
	return nil, errors.Errorf("CreateOrUpdateRoute failed: code[%d] msg[%s]", code, body)
}

func (impl *KongAdapterImpl) CreateOrUpdateRoute(req *KongRouteReqDto) (*KongRouteRespDto, error) {
	if impl == nil {
		return nil, errors.New("kong can't be attached")
	}
	if req == nil {
		return nil, errors.New(ERR_INVALID_ARG)
	}
	req.Adjust(Versioning(impl))
	url := impl.KongAddr + RouteRoot
	method := "POST"
	if len(req.RouteId) != 0 {
		url += req.RouteId
		method = "PUT"
		req.RouteId = ""
	}
	code, body, err := util.DoCommonRequest(impl.Client, method, url, req)
	if err != nil {
		return nil, errors.Wrap(err, "request failed")
	}
	if code == 201 || code == 200 {
		respDto := &KongRouteRespDto{}
		err = json.Unmarshal(body, respDto)
		if err != nil {
			return nil, errors.Wrap(err, ERR_JSON_FAIL)
		}
		return respDto, nil
	}
	// handle invalid path
	if code == 400 {
		log.Errorf("CreateOrUpdateRoute failed: code[%d] msg[%s]", code, body)
		return nil, ErrInvalidReq
	}
	return nil, errors.Errorf("CreateOrUpdateRoute failed: code[%d] msg[%s]", code, body)
}

func (impl *KongAdapterImpl) TouchRouteOAuthMethod(id string) error {
	if impl == nil {
		return errors.New("kong can't be attached")
	}
	code, body, err := util.DoCommonRequest(impl.Client, "GET", impl.KongAddr+RouteRoot+id, nil)
	if err != nil {
		return errors.Wrap(err, "request failed")
	}
	needTouch := true
	respDto := &KongRouteRespDto{}
	if code == 200 || code == 201 {
		err = json.Unmarshal(body, respDto)
		if err != nil {
			return errors.Wrap(err, ERR_JSON_FAIL)
		}
		for _, method := range respDto.Methods {
			if method == "POST" {
				needTouch = false
			}
		}
	} else {
		return errors.Errorf("get route info failed: code[%d] msg[%s]", code, body)
	}
	if needTouch {
		reqDto := NewKongRouteReqDto()
		reqDto.Methods = []string{"POST"}
		reqDto.Hosts = respDto.Hosts
		reqDto.Paths = []string{respDto.Paths[0] + "/oauth2/token", respDto.Paths[0] + "/oauth2/authorize"}
		reqDto.Service = &respDto.Service
		code, body, err = util.DoCommonRequest(impl.Client, "POST", impl.KongAddr+RouteRoot, reqDto)
		if code == 201 || code == 200 {
			return nil
		}
		return errors.Errorf("update route method failed, code[%d] msg[%s] err[%+v]", code, body, err)
	}
	return nil
}

func (impl *KongAdapterImpl) DeleteRoute(id string) error {
	if impl == nil {
		return errors.New("kong can't be attached")
	}
	if len(id) == 0 {
		return errors.New(ERR_INVALID_ARG)
	}
	code, body, err := util.DoCommonRequest(impl.Client, "DELETE", impl.KongAddr+RouteRoot+id, nil)
	if err != nil {
		return errors.Wrap(err, "request failed")
	}
	if code < 300 || code == 404 {
		return nil
	}
	return errors.Errorf("DeleteRoute failed: code[%d] msg[%s]", code, body)
}
func (impl *KongAdapterImpl) CreateOrUpdateService(req *KongServiceReqDto) (*KongServiceRespDto, error) {
	if impl == nil {
		return nil, errors.New("kong can't be attached")
	}
	if req == nil {
		return nil, errors.New(ERR_INVALID_ARG)
	}
	url := impl.KongAddr + ServiceRoot
	method := "POST"
	if len(req.ServiceId) != 0 {
		url += req.ServiceId
		method = "PUT"
		req.ServiceId = ""
	}
	code, body, err := util.DoCommonRequest(impl.Client, method, url, req)
	if err != nil {
		return nil, errors.Wrap(err, "request failed")
	}
	if code == 201 || code == 200 {
		respDto := &KongServiceRespDto{}
		err = json.Unmarshal(body, respDto)
		if err != nil {
			return nil, errors.Wrapf(err, "json unmarshal failed [%s]", body)
		}
		return respDto, nil
	}
	return nil, errors.Errorf("CreateOrUpdateService failed: code[%d] msg[%s]", code, body)
}

func (impl *KongAdapterImpl) DeleteService(id string) error {
	if impl == nil {
		return errors.New("kong can't be attached")
	}
	if len(id) == 0 {
		return errors.New(ERR_INVALID_ARG)
	}
	code, body, err := util.DoCommonRequest(impl.Client, "DELETE", impl.KongAddr+ServiceRoot+id, nil)
	if err != nil {
		return errors.Wrap(err, "request failed")
	}
	if code < 300 || code == 404 {
		return nil
	}
	return errors.Errorf("DeleteService failed: code[%d] msg[%s]", code, body)
}

func (impl *KongAdapterImpl) GetPlugin(req *KongPluginReqDto) (*KongPluginRespDto, error) {
	if impl == nil {
		return nil, errors.New("kong can't be attached")
	}
	if req == nil || req.Name == "" {
		return nil, errors.New(ERR_INVALID_ARG)
	}
	var args []string
	args = append(args, "name="+req.Name)
	if req.RouteId != "" {
		args = append(args, "route_id="+req.RouteId)
	}
	if req.ServiceId != "" {
		args = append(args, "service_id="+req.ServiceId)
	}
	if req.ConsumerId != "" {
		args = append(args, "cousumer_id="+req.ConsumerId)
	}
	code, body, err := util.DoCommonRequest(impl.Client, "GET",
		impl.KongAddr+PluginRoot+"?"+strings.Join(args, "&"), nil)
	if err != nil {
		return nil, errors.Wrap(err, "request failed")
	}
	if code == 200 {
		respDto := &KongPluginsDto{}
		err = json.Unmarshal(body, respDto)
		if err != nil {
			return nil, errors.Wrap(err, ERR_JSON_FAIL)
		}
		if respDto.Total > 0 {
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
	url := impl.KongAddr + PluginRoot
	method := "POST"
	if len(req.Id) != 0 {
		method = "PUT"
		req.CreatedAt = time.Now().Unix() * 1000
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
		return respDto, nil
	}
	return nil, errors.Errorf("CreateOrUpdatePlugin failed: code[%d] msg[%s]", code, body)
}

func (impl *KongAdapterImpl) GetVersion() (string, error) {
	if impl == nil {
		return "", errors.New("kong can't be attached")
	}
	code, body, err := util.DoCommonRequest(impl.Client, "GET", impl.KongAddr+"/", nil)
	if err != nil {
		return "", err
	}
	if code == 200 {
		respDto := &KongConfigsDto{}
		err = json.Unmarshal(body, respDto)
		if err != nil {
			return "", errors.Wrapf(err, "json Unmarshal failed, body:%s", body)
		}
		return respDto.Version, nil
	}
	return "", errors.Errorf("get  version failed: code[%d] msg[%s]", code, body)
}

func (impl *KongAdapterImpl) CheckPluginEnabled(pluginName string) (bool, error) {
	if impl == nil {
		return false, errors.New("kong can't be attached")
	}
	code, body, err := util.DoCommonRequest(impl.Client, "GET", impl.KongAddr+"/", nil)
	if err != nil {
		return false, err
	}
	if code == 200 {
		respDto := &KongConfigsDto{}
		err = json.Unmarshal(body, respDto)
		if err != nil {
			return false, errors.Wrapf(err, "json Unmarshal failed, body:%s", body)
		}
		enabled := false
		for _, name := range respDto.Configuration.Plugins {
			if pluginName == name {
				enabled = true
				break
			}
		}
		return enabled, nil
	}
	return false, errors.Errorf("check plugin enabled failed: code[%d] msg[%s]", code, body)
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
	enabled, err := impl.CheckPluginEnabled(req.Name)
	if err != nil {
		return nil, err
	}
	if !enabled {
		log.Warnf("plugin %s not enabled, req:%+v", req.Name, req)
		return nil, nil
	}
	req.Id = req.PluginId
	req.CreatedAt = time.Now().Unix() * 1000
	code, body, err := util.DoCommonRequest(impl.Client, "PUT", impl.KongAddr+PluginRoot, req)
	if err != nil {
		return nil, errors.Wrap(err, "request failed")
	}
	if code == 200 || code == 201 {
		respDto := &KongPluginRespDto{}
		err = json.Unmarshal(body, respDto)
		if err != nil {
			return nil, errors.Wrapf(err, "body[%s] Unmarshal failed", body)
		}
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
		return respDto, nil
	}
	//TODO: support create if not exist
	// if code == 404 {
	// 	// create if not exist
	// 	code, body, err = util.DoCommonRequest(impl.Client, "PUT", impl.KongAddr+PluginRoot+req.PluginId, req)
	// 	if err != nil {
	// 		return nil, errors.Wrap(err, "request failed")
	// 	}
	// 	if code == 200 {
	// 		respDto := &KongPluginRespDto{}
	// 		err = json.Unmarshal(body, respDto)
	// 		if err != nil {
	// 			return nil, errors.Wrapf(err, "body[%s] Unmarshal failed", body)
	// 		}
	// 		return respDto, nil
	// 	}
	// }
	return nil, errors.Errorf("UpdatePlugin failed: code[%d] msg[%s]", code, body)
}
func (impl *KongAdapterImpl) RemovePlugin(id string) error {
	if impl == nil {
		return errors.New("kong can't be attached")
	}
	if len(id) == 0 {
		return errors.New(ERR_INVALID_ARG)
	}
	code, body, err := util.DoCommonRequest(impl.Client, "DELETE", impl.KongAddr+PluginRoot+id, nil)
	if err != nil {
		return errors.Wrap(err, "request failed")
	}
	if code == 204 || code == 404 {
		return nil
	}
	return errors.Errorf("RemovePlugin failed: code[%d] msg[%s]", code, body)
}
func (impl *KongAdapterImpl) CreateCredential(req *KongCredentialReqDto) (*KongCredentialDto, error) {
	if impl == nil {
		return nil, errors.New("kong can't be attached")
	}
	if req == nil {
		return nil, errors.New(ERR_INVALID_ARG)
	}
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
		return respDto, nil
	}
	return nil, errors.Errorf("CreateCredential failed: code[%d] msg[%s]", code, body)
}

func (impl *KongAdapterImpl) DeleteCredential(consumerId, pluginName, credentialId string) error {
	if impl == nil {
		return errors.New("kong can't be attached")
	}
	code, body, err := util.DoCommonRequest(impl.Client, "DELETE", impl.KongAddr+ConsumerRoot+consumerId+"/"+pluginName+"/"+credentialId, nil)
	if err != nil {
		return errors.Wrap(err, "request failed")
	}
	if code == 204 || code == 404 {
		return nil
	}
	return errors.Errorf("DeleteCredential failed: code[%d] msg[%s]", code, body)
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
		if pluginName == "hmac-auth" {
			for i := 0; i < len(respDto.Data); i++ {
				respDto.Data[i].ToHmacResp()
			}

		}
		return respDto, nil
	}
	return nil, errors.Errorf("GetCredentialList failed: code[%d] msg[%s]", code, body)
}

func (impl *KongAdapterImpl) CreateAclGroup(consumerId string, customId string) error {
	if impl == nil {
		return errors.New("kong can't be attached")
	}
	if len(consumerId) == 0 || len(customId) == 0 {
		return errors.New(ERR_INVALID_ARG)
	}
	code, body, err := util.DoCommonRequest(impl.Client, "POST", impl.KongAddr+ConsumerRoot+consumerId+AclRoot,
		[]byte(`{"group":"`+customId+`"}`))
	if err != nil {
		return errors.Wrap(err, "request failed")
	}
	if code < 300 {
		return nil
	}
	return errors.Errorf("CreateAclGroup failed: code[%d] msg[%s]", code, body)
}

func (impl *KongAdapterImpl) CreateUpstream(req *KongUpstreamDto) (*KongUpstreamDto, error) {
	if impl == nil {
		return nil, errors.New("kong can't be attached")
	}
	if req == nil {
		return nil, errors.New(ERR_INVALID_ARG)
	}
	code, body, err := util.DoCommonRequest(impl.Client, "POST", impl.KongAddr+UpstreamRoot, req)
	if err != nil {
		return nil, errors.Wrap(err, "request failed")
	}
	if code == 200 || code == 201 {
		respDto := &KongUpstreamDto{}
		err = json.Unmarshal(body, respDto)
		if err != nil {
			return nil, errors.Wrapf(err, "unmashal body failed, body:%s", body)
		}
		return respDto, nil
	}
	return nil, errors.Errorf("CreateUpstream failed: code[%d] msg[%s]", code, body)
}

func (impl *KongAdapterImpl) GetUpstreamStatus(upstreamId string) (*KongUpstreamStatusRespDto, error) {
	if impl == nil {
		return nil, errors.New("kong can't be attached")
	}
	if upstreamId == "" {
		return nil, errors.New(ERR_INVALID_ARG)
	}
	code, body, err := util.DoCommonRequest(impl.Client, "GET", impl.KongAddr+UpstreamRoot+upstreamId+HealthPath, nil)
	if err != nil {
		return nil, errors.Wrap(err, "request failed")
	}
	if code == 200 {
		respDto := &KongUpstreamStatusRespDto{}
		err = json.Unmarshal(body, respDto)
		if err != nil {
			return nil, errors.Wrapf(err, "unmarshal body failed, body:%s", body)
		}
		return respDto, nil
	}
	return nil, errors.Errorf("GetUpstreamStatus failed: code[%d] msg[%s]", code, body)
}

func (impl *KongAdapterImpl) AddUpstreamTarget(upstreamId string, req *KongTargetDto) (*KongTargetDto, error) {
	if impl == nil {
		return nil, errors.New("kong can't be attached")
	}
	if upstreamId == "" || req == nil {
		return nil, errors.New(ERR_INVALID_ARG)
	}
	code, body, err := util.DoCommonRequest(impl.Client, "POST", impl.KongAddr+UpstreamRoot+upstreamId+TargetPath, req)
	if err != nil {
		return nil, errors.Wrap(err, "request failed")
	}
	if code == 200 || code == 201 {
		respDto := &KongTargetDto{}
		err = json.Unmarshal(body, respDto)
		if err != nil {
			return nil, errors.Wrapf(err, "unmarshal body failed, body:%s", body)
		}
		return respDto, nil
	}
	return nil, errors.Errorf("AddUpstreamTarget failed: code[%d] msg[%s]", code, body)
}

func (impl *KongAdapterImpl) DeleteUpstreamTarget(upstreamId, targetId string) error {
	if impl == nil {
		return errors.New("kong can't be attached")
	}
	if upstreamId == "" || targetId == "" {
		return errors.New(ERR_INVALID_ARG)
	}
	code, body, err := util.DoCommonRequest(impl.Client, "DELETE", impl.KongAddr+UpstreamRoot+upstreamId+TargetPath+targetId, nil)
	if err != nil {
		return errors.Wrap(err, "request failed")
	}
	if code == 204 || code == 404 {
		return nil
	}
	return errors.Errorf("DeleteUpstreamTarget failed: code[%d] msg[%s]", code, body)
}

func (impl *KongAdapterImpl) GetRoutes() ([]KongRouteRespDto, error) {
	if impl == nil {
		return nil, errors.New("kong can't be attached")
	}
	code, body, err := util.DoCommonRequest(impl.Client, "GET", impl.KongAddr+RouteRoot, nil)
	if err != nil {
		return nil, errors.Wrap(err, "request failed")
	}
	if code == 200 {
		respDto := &KongRoutesRespDto{}
		err = json.Unmarshal(body, respDto)
		if err != nil {
			return nil, errors.Wrap(err, ERR_JSON_FAIL)
		}
		return respDto.Routes, nil
	}
	return nil, errors.Errorf("get routes failed: code[%d] msg[%s]", code, body)
}

func (impl *KongAdapterImpl) GetRoutesWithTag(tag string) ([]KongRouteRespDto, error) {
	if impl == nil {
		return nil, errors.New("kong can't be attached")
	}
	code, body, err := util.DoCommonRequest(impl.Client, "GET", impl.KongAddr+RouteRoot+"?tags="+tag, nil)
	if err != nil {
		return nil, errors.Wrap(err, "request failed")
	}
	if code == 200 {
		respDto := &KongRoutesRespDto{}
		err = json.Unmarshal(body, respDto)
		if err != nil {
			return nil, errors.Wrap(err, ERR_JSON_FAIL)
		}
		return respDto.Routes, nil
	}
	return nil, errors.Errorf("get routes failed: code[%d] msg[%s]", code, body)
}
