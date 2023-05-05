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

package api

import (
	"fmt"
	"sync/atomic"
	"time"

	"github.com/aliyun/alibaba-cloud-sdk-go/sdk/requests"
	"github.com/aliyun/alibaba-cloud-sdk-go/sdk/responses"
	"go.uber.org/ratelimit"

	"github.com/erda-project/erda/pkg/aliyunclient"
)

type CloudVendor interface {
	DoReq(request interface{}, response interface{}) error
}

// vendors
type aliyun struct {
	cfg              *AliyunConfig
	limiter          ratelimit.Limiter
	reqLimitDuration time.Duration
	client           *aliyunclient.Client
	ak, sk           string
	bucket           uint64
	done             chan struct{}
}

type AliyunConfig struct {
	Timeout          time.Duration
	MaxQPS           int
	ReqLimit         uint64
	ReqLimitDuration time.Duration
}

func (a *aliyun) limit(timeout time.Duration) error {
	timer := time.After(timeout)
	for {
		select {
		case <-timer:
			return fmt.Errorf("limit timeout")
		default:
		}

		if atomic.LoadUint64(&a.bucket) == 0 {
			continue
		} else {
			atomic.AddUint64(&a.bucket, ^uint64(0))
			break
		}
	}
	a.limiter.Take()
	return nil
}

func (a *aliyun) DoReq(request interface{}, response interface{}) error {
	if err := a.limit(a.cfg.Timeout); err != nil {
		return fmt.Errorf("limit req. err=%s", err)
	}

	errConvert := fmt.Errorf("convert failed")
	req, ok := request.(requests.AcsRequest)
	if !ok {
		return errConvert
	}
	resp, ok := response.(responses.AcsResponse)
	if !ok {
		return errConvert
	}
	return a.client.DoAction(req, resp)
}

func (a *aliyun) init() {
	go func() {
		for {
			select {
			case <-a.done:
				return
			default:
			}
			time.Sleep(a.reqLimitDuration)

			atomic.StoreUint64(&a.bucket, a.cfg.ReqLimit)
		}
	}()

}

func NewAliyunVendor(ak, sk string, cfg AliyunConfig) (v *aliyun, err error) {
	client := &aliyunclient.Client{}
	err = client.InitWithAccessKey("", ak, sk)
	if err != nil {
		return nil, fmt.Errorf("failed create aliyun client, err %s", err)
	}
	v = &aliyun{
		ak:               ak,
		sk:               sk,
		client:           client,
		done:             make(chan struct{}),
		bucket:           cfg.ReqLimit,
		reqLimitDuration: cfg.ReqLimitDuration,
		limiter:          ratelimit.New(cfg.MaxQPS),
		cfg:              &cfg,
	}
	v.init()

	return v, nil
}
