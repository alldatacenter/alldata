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

package prehandle

import (
	"context"
	"net/http"
	"strings"
	"sync"

	"github.com/go-redis/redis"

	"github.com/erda-project/erda/internal/tools/openapi/legacy/auth"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/conf"
)

var filterCookieLock sync.Once
var rediscli *redis.Client

// filter session cookie which is exist in redis, put it at req.context
func FilterCookie(ctx context.Context, rw http.ResponseWriter, req *http.Request) {
	filterCookieLock.Do(func() {
		rediscli = redis.NewFailoverClient(&redis.FailoverOptions{
			MasterName:    conf.RedisMasterName(),
			SentinelAddrs: strings.Split(conf.RedisSentinelAddrs(), ","),
			Password:      conf.RedisPwd(),
		})
	})
	cs := req.Cookies()
	sessions := []*http.Cookie{}
	for _, c := range cs {
		if c.Name == conf.SessionCookieName() {
			sessions = append(sessions, c)
		}
	}
	if len(sessions) >= 1 {
		for _, session := range sessions {
			if conf.OryEnabled() {
				// TODO
				*req = *(req.WithContext(context.WithValue(req.Context(), "session", session.Value)))
				return
			}
			if _, err := rediscli.Get(auth.MkSessionKey(session.Value)).Result(); err == redis.Nil {
				continue
			} else if err != nil {
				continue
			}
			*req = *(req.WithContext(context.WithValue(req.Context(), "session", session.Value)))
			return
		}
	}
}
