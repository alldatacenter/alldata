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

package validator

import (
	"fmt"
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/erda-project/erda/pkg/secret"
	"github.com/erda-project/erda/pkg/secret/hmac"
)

const maxExpireTime = time.Minute * 10

type Result struct {
	Ok      bool
	Message string
}

type Validator interface {
	Verify(r *http.Request) Result
	VerifySignString(signString string, expected string) Result
}

type HMACValidator struct {
	signer    *hmac.Signer
	pair      secret.AkSkPair
	reqExpire time.Duration
	reqTime   *time.Time
	signedMap map[string]string
}

type Option func(hv *HMACValidator)

func WithMaxExpireInterval(d time.Duration) Option {
	return func(hv *HMACValidator) {
		hv.reqExpire = d
	}
}

func NewHMACValidator(pair secret.AkSkPair, ops ...Option) Validator {
	hv := &HMACValidator{
		reqExpire: maxExpireTime,
		pair:      pair,
	}
	for _, op := range ops {
		op(hv)
	}
	return hv
}

func (hv *HMACValidator) Verify(r *http.Request) Result {
	hv.parseRequest(r)

	if hv.reqTime != nil {
		if time.Now().Sub(*hv.reqTime) > hv.reqExpire {
			return Result{
				Ok:      false,
				Message: fmt.Sprintf("req received, but exceed max expire duration %s", hv.reqExpire),
			}
		}
	}

	return hv.VerifySignString(hv.signer.GetSignString(r), hv.signedMap[hmac.ErdaSignature])
}

func (hv *HMACValidator) VerifySignString(signString string, expected string) Result {
	sig := hv.signer.Signature(signString)
	pass := sig == expected
	if !pass {
		return Result{
			Ok:      false,
			Message: "verify signature failed. got signature: " + sig,
		}
	}
	return Result{
		Ok: true,
	}
}

func (hv *HMACValidator) parseReqTimestamp(ts string) (time.Time, bool) {
	if ts == "" {
		return time.Time{}, false
	}
	i, err := strconv.ParseInt(ts, 10, 64)
	if err != nil {
		return time.Time{}, false
	}
	tm := time.Unix(i, 0)
	return tm, true
}

func (hv *HMACValidator) parseRequest(r *http.Request) {

	signedMap := make(map[string]string)
	sourceString := ""
	if authStringInHeader(r) {
		sourceString = r.Header.Get("Authorization")
	}
	if authStringInQueryString(r) {
		sourceString = r.URL.RawQuery
	}
	for _, item := range strings.Split(strings.TrimSpace(sourceString), "&") {
		kv := strings.Split(item, "=")
		if len(kv) == 2 && strings.HasPrefix(kv[0], hmac.ErdaHeaderPrefix) {
			signedMap[kv[0]] = kv[1]
		}
	}
	hv.signedMap = signedMap

	if t, ok := hv.parseReqTimestamp(signedMap[hmac.ErdaSignTimestamp]); ok {
		hv.signer = hmac.New(hv.pair, hmac.WithTimestamp(t))
		hv.reqTime = &t
	} else {
		hv.signer = hmac.New(hv.pair)
	}

}

func GetAccessKeyID(r *http.Request) (string, bool) {
	if authStringInHeader(r) {
		for _, item := range strings.Split(r.Header.Get("Authorization"), "&") {
			if strings.HasPrefix(item, hmac.ErdaAccessKeyID) {
				kv := strings.Split(item, "=")
				if len(kv) == 2 {
					return kv[1], true
				}
			}
		}
	}
	if authStringInQueryString(r) {
		if val := r.URL.Query().Get(hmac.ErdaAccessKeyID); val != "" {
			return val, true
		}
	}
	return "", false
}

func authStringInHeader(r *http.Request) bool {
	return strings.Index(r.Header.Get("Authorization"), hmac.ErdaAccessKeyID) != -1
}

func authStringInQueryString(r *http.Request) bool {
	return r.URL.Query().Get(hmac.ErdaAccessKeyID) != ""
}
