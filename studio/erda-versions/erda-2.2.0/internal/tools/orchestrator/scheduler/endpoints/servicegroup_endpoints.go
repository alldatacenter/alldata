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

package endpoints

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/impl/servicegroup"
	"github.com/erda-project/erda/pkg/http/httpserver"
)

func (h *HTTPEndpoints) ServiceGroupCreate(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	//h.metric.TotalCounter.WithLabelValues(metric.ServiceCreateTotal).Add(1)
	req := apistructs.ServiceGroupCreateV2Request{}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		errstr := fmt.Sprintf("decode create servicegroup request fail: %v", err)
		return mkResponse(apistructs.ServiceGroupCreateV2Response{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: errstr},
			}})
	}
	logrus.Infof("ServiceGroupCreate Request:  %#v", req)
	logrus.Infof("ServiceGroupCreate Request req.DiceYml.Services: %#v", req.DiceYml.Services)
	for name, s := range req.DiceYml.Services {
		logrus.Infof("ServiceGroupCreate Request req.DiceYml.Services %s Volumes: %#v", name, s.Volumes)
		for i, v := range s.Volumes {
			logrus.Infof("ServiceGroupCreate Request req.DiceYml.Services %s Volumes[%d]: %#v", name, i, v)
		}
	}

	sg, err := h.ServiceGroupImpl.Create(req)
	if err != nil {
		errstr := fmt.Sprintf("create servicegroup fail: %v", err)
		//h.metric.ErrorCounter.WithLabelValues(metric.ServiceCreateError).Add(1)
		return mkResponse(apistructs.ServiceGroupCreateV2Response{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: errstr},
			}})
	}
	return mkResponse(apistructs.ServiceGroupCreateV2Response{
		Header: apistructs.Header{Success: true},
		Data: apistructs.ServiceGroupCreateV2Data{
			ID:   sg.ID,
			Type: sg.Type,
		},
	})
}

/*
func (h *HTTPEndpoints) ServiceGroupUpdate(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	req := apistructs.ServiceGroupUpdateV2Request{}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		errstr := fmt.Sprintf("decode update servicegroup request fail: %v", err)
		return mkResponse(apistructs.ServiceGroupUpdateV2Response{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: errstr},
			}})
	}
	sg, err := h.ServiceGroupImpl.Update(req)
	if err != nil {
		errstr := fmt.Sprintf("update servicegroup fail: %v", err)
		return mkResponse(apistructs.ServiceGroupUpdateV2Response{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: errstr},
			}})
	}
	return mkResponse(apistructs.ServiceGroupUpdateV2Response{
		Header: apistructs.Header{Success: true},
		Data: apistructs.ServiceGroupCreateV2Data{
			Type: sg.Type,
			ID:   sg.ID,
		},
	})
}
*/

func (h *HTTPEndpoints) ServiceGroupDelete(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	//h.metric.TotalCounter.WithLabelValues(metric.ServiceRemoveTotal).Add(1)

	namespace := r.URL.Query().Get("namespace")
	name := r.URL.Query().Get("name")
	force := r.URL.Query().Get("force")
	if namespace == "" || name == "" {
		errstr := fmt.Sprintf("empty namespace or name")
		return mkResponse(apistructs.ServiceGroupDeleteV2Response{
			apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: errstr},
			}})
	}

	if err := h.ServiceGroupImpl.Delete(namespace, name, force); err != nil {
		errstr := fmt.Sprintf("delete servicegroup fail: %v", err)
		//h.metric.ErrorCounter.WithLabelValues(metric.ServiceRemoveError).Add(1)
		if err.Error() == servicegroup.DeleteNotFound.Error() {
			return mkResponse(apistructs.ServiceGroupDeleteV2Response{
				apistructs.Header{
					Success: false,
					Error: apistructs.ErrorResponse{
						Code: "404",
						Msg:  errstr,
					},
				}})
		}

		return mkResponse(apistructs.ServiceGroupDeleteV2Response{
			apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: errstr},
			}})
	}
	return mkResponse(apistructs.ServiceGroupDeleteV2Response{
		apistructs.Header{Success: true}})
}

func (h *HTTPEndpoints) ServiceGroupInfo(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	namespace := r.URL.Query().Get("namespace")
	name := r.URL.Query().Get("name")
	if namespace == "" || name == "" {
		errstr := fmt.Sprintf("empty namespace or name")
		return mkResponse(apistructs.ServiceGroupInfoResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: errstr},
			},
		})
	}

	sg, err := h.ServiceGroupImpl.Info(ctx, namespace, name)
	if err != nil {
		errstr := fmt.Sprintf("get servicegroup info fail: %v", err)
		return mkResponse(apistructs.ServiceGroupInfoResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: errstr},
			},
		})
	}
	return mkResponse(apistructs.ServiceGroupInfoResponse{
		apistructs.Header{Success: true},
		sg,
	})
}

func (h *HTTPEndpoints) ServiceGroupPrecheck(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	req := apistructs.ServiceGroupPrecheckRequest{}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		errstr := fmt.Sprintf("decode precheck servicegroup request fail: %v", err)
		return mkResponse(apistructs.ServiceGroupPrecheckResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: errstr},
			}})
	}
	res, err := h.ServiceGroupImpl.Precheck(req)
	if err != nil {
		errstr := fmt.Sprintf("precheck servicegroup fail: %v", err)
		return mkResponse(apistructs.ServiceGroupPrecheckResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: errstr},
			},
		})
	}
	return mkResponse(apistructs.ServiceGroupPrecheckResponse{
		Header: apistructs.Header{Success: true},
		Data:   res,
	})

}

func (h *HTTPEndpoints) ServiceGroupKillPod(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	req := apistructs.ServiceGroupKillPodRequest{}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		errstr := fmt.Sprintf("decode killpod request fail: %v", err)
		return mkResponse(apistructs.ServiceGroupKillPodResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: errstr},
			}})
	}
	if req.Namespace == "" || req.Name == "" || req.PodName == "" {
		errstr := fmt.Sprintf("empty namespace or name or containerID")
		return mkResponse(apistructs.ServiceGroupKillPodResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: errstr},
			},
		})
	}

	err := h.ServiceGroupImpl.KillPod(ctx, req.Namespace, req.Name, req.PodName)
	if err != nil {
		return mkResponse(apistructs.ServiceGroupKillPodResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: err.Error()},
			},
		})
	}
	return mkResponse(apistructs.ServiceGroupKillPodResponse{
		Header: apistructs.Header{
			Success: true,
		},
	})
}

func (h *HTTPEndpoints) ServiceGroupConfigUpdate(ctx context.Context, r *http.Request, vars map[string]string) (
	httpserver.Responser, error) {
	req := apistructs.ServiceGroup{}
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		errstr := fmt.Sprintf("configupdate decode servicegroup request fail: %v", err)
		return mkResponse(apistructs.ServiceGroupConfigUpdateResponse{
			Header: apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: errstr},
			}})
	}
	namespace := req.Type
	name := req.ID
	if namespace == "" || name == "" {
		errstr := fmt.Sprintf("empty namespace or name")
		return mkResponse(apistructs.ServiceGroupConfigUpdateResponse{
			apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: errstr},
			},
		})
	}
	if err := h.ServiceGroupImpl.ConfigUpdate(req); err != nil {
		errstr := fmt.Sprintf("configupdate servicegroup fail: %v", err)
		return mkResponse(apistructs.ServiceGroupConfigUpdateResponse{
			apistructs.Header{
				Success: false,
				Error:   apistructs.ErrorResponse{Msg: errstr},
			},
		})
	}
	return mkResponse(apistructs.ServiceGroupConfigUpdateResponse{
		apistructs.Header{Success: true},
	})
}
