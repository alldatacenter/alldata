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

package register

import (
	"context"
	"encoding/json"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda-proto-go/core/messenger/eventbox/pb"
	"github.com/erda-project/erda/internal/core/messenger/eventbox/types"
)

type PutRequest struct {
	Key    string                         `json:"key"`
	Labels map[types.LabelKey]interface{} `json:"labels"`
}

type DelRequest struct {
	Key string `json:"key"`
}

type GetResponseContent map[types.LabelKey]interface{}

type RegisterHTTP struct {
	register Register
}

func NewHTTP(register Register) *RegisterHTTP {
	return &RegisterHTTP{
		register: register,
	}
}

func (r *RegisterHTTP) Put(ctx context.Context, req *pb.PutRequest, vars map[string]string) (*pb.PutResponse, error) {
	lab := make(map[types.LabelKey]interface{})
	for k, v := range req.Labels {
		//todo check
		lab[types.LabelKey(k)] = v.AsInterface()
	}
	if err := r.register.Put(req.Key, lab); err != nil {
		err := errors.Errorf("RegisterHTTP Put: %v", err)
		logrus.Error(err)
		return &pb.PutResponse{
			Data: "",
		}, err
	}
	return &pb.PutResponse{
		Data: "",
	}, nil
}

func (r *RegisterHTTP) PrefixGet(ctx context.Context, req *pb.PrefixGetRequest, vars map[string]string) (*pb.PrefixGetResponse, error) {
	if req.Key == "" {
		logrus.Infof("RegisterHTTP Get: request not provide key")
		return &pb.PrefixGetResponse{
			Data: nil,
		}, nil
	}
	labels := r.register.PrefixGet(req.Key)
	if labels == nil {
		logrus.Infof("RegisterHTTP Get (not found): %v", req.Key)
		return &pb.PrefixGetResponse{
			Data: nil,
		}, nil
	}
	data, err := json.Marshal(labels)
	if err != nil {
		logrus.Infof("labels marshal is failed err is %v", err)
		return &pb.PrefixGetResponse{
			Data: nil,
		}, nil
	}
	//todo check
	resp := make(map[string]*pb.PrefixValue)
	err = json.Unmarshal(data, &resp)
	if err != nil {
		logrus.Infof("labels unmarshal is failed err is %v", err)
		return &pb.PrefixGetResponse{
			Data: nil,
		}, nil
	}
	return &pb.PrefixGetResponse{
		Data: resp,
	}, nil
}

func (r *RegisterHTTP) Del(ctx context.Context, req *pb.DelRequest, vars map[string]string) (*pb.DelResponse, error) {
	if err := r.register.Del(req.Key); err != nil {
		return &pb.DelResponse{
			Data: "",
		}, err
	}
	return &pb.DelResponse{
		Data: "",
	}, nil
}
