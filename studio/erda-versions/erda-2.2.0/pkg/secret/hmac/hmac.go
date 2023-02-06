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

// HMAC Algorithm
package hmac

import (
	"crypto/hmac"
	"crypto/sha1" // nolint
	"encoding/hex"
	"net/http"
	"sort"
	"strings"
	"time"

	"github.com/erda-project/erda/pkg/secret"
)

const (
	ErdaHeaderPrefix  = "X-Erda-"
	ErdaSignAlgorithm = ErdaHeaderPrefix + "Sign-Algorithm"
	ErdaSignTimestamp = ErdaHeaderPrefix + "Sign-Timestamp"
	ErdaAccessKeyID   = ErdaHeaderPrefix + "Ak"
	ErdaSignature     = ErdaHeaderPrefix + "Signature"
)

type Signer struct {
	timestampEnable   bool
	nowTimestamp      string
	authInQueryString bool
	keyPair           secret.AkSkPair
}

func New(keyPair secret.AkSkPair, opts ...Option) *Signer {
	s := &Signer{
		keyPair: keyPair,
	}
	for _, o := range opts {
		o(s)
	}
	return s
}

// Sign HTTP Request
func (s *Signer) SignCanonicalRequest(r *http.Request) {
	authString := s.getAuthString(s.Signature(s.GetSignString(r)))
	if s.authInQueryString {
		appendAuthString(r, authString)
	} else {
		r.Header.Set("Authorization", authString)
	}
}

// hash and get Signature
func (s *Signer) Signature(signString string) string {
	h := hmac.New(sha1.New, []byte(s.keyPair.SecretKey))
	h.Write([]byte(signString))
	return hex.EncodeToString(h.Sum(nil))
}

func (s *Signer) GetSignString(r *http.Request) string {
	var ss strings.Builder
	ss.WriteString(strings.ToUpper(r.Method) + "\n")

	if s.timestampEnable {
		ss.WriteString(s.nowTimestamp)
	} else {
		ss.WriteString("")
	}
	ss.WriteString("\n")

	ss.WriteString(canonicalResource(r) + "\n")
	ss.WriteString(canonicalQueryString(r) + "\n")
	ss.WriteString(canonicalHeaders(r))
	return ss.String()
}

func (s *Signer) getAuthString(sig string) string {
	var sb strings.Builder
	sb.WriteString(ErdaAccessKeyID + "=" + s.keyPair.AccessKeyID + "&")
	sb.WriteString(ErdaSignature + "=" + sig + "&")
	sb.WriteString(ErdaSignAlgorithm + "=" + "hmac-sha1" + "&")
	if s.timestampEnable {
		sb.WriteString(ErdaSignTimestamp + "=" + s.nowTimestamp + "&")
	}
	return sb.String()[:sb.Len()-1]
}

func canonicalQueryString(r *http.Request) string {
	pairs := make([]string, 0, len(r.URL.Query()))
	for k, vals := range r.URL.Query() {
		if !strings.HasPrefix(k, ErdaHeaderPrefix) {
			for _, val := range vals {
				pairs = append(pairs, k+"="+val)
			}
		}
	}
	sort.Strings(pairs)
	return strings.Join(pairs, "&")
}

func canonicalHeaders(r *http.Request) string {
	pairs := make([]string, 0, len(r.Header))
	for k, vals := range r.Header {
		for _, val := range vals {
			if strings.HasPrefix(k, ErdaHeaderPrefix) {
				pairs = append(pairs, k+"="+val)
			}
		}
	}
	sort.Strings(pairs)
	return strings.Join(pairs, "&")
}

func canonicalResource(r *http.Request) string {
	return r.URL.Path
}

func timestampSecond(t time.Time) int {
	return int(t.UnixNano() / 1000000000)
}

func appendAuthString(r *http.Request, authString string) {
	qr := r.URL.Query()
	for _, pair := range strings.Split(authString, "&") {
		kv := strings.Split(pair, "=")
		if len(kv) == 2 {
			qr.Set(kv[0], kv[1])
		}
	}
	r.URL.RawQuery = qr.Encode()
}
