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

package autotest_cookie_keep_before

import (
	"fmt"
	"strings"
)

// appendOrReplaceSetCookiesToCookie
// - append cookie item if cookie name not exist
// - replace cookie item if cookie already exist
//
// @setCookies: [{COOKIE1=def; Path=/; Domain=xxx; Expires=Fri, 03 Sep 2021 15:12:15 GMT},{...}]
// @originCookie: COOKIE2=abc; COOKIE2=abc
func appendOrReplaceSetCookiesToCookie(setCookies []string, originCookie string) string {
	// transfer originCookie to map
	cookieItems := strings.Split(originCookie, ";")
	cookieKvMap := map[string]string{}
	orderedCookieKeys := make([]string, 0, len(cookieItems)+len(setCookies))
	for _, item := range cookieItems {
		item = strings.TrimSpace(item)
		kv := strings.SplitN(item, "=", 2)
		if len(kv) != 2 {
			continue
		}
		k := strings.TrimSpace(kv[0])
		v := strings.TrimSpace(kv[1])
		cookieKvMap[k] = v
		orderedCookieKeys = append(orderedCookieKeys, k)
	}

	// handle each set-cookie
	for _, setCookie := range setCookies {
		if len(setCookie) == 0 {
			continue
		}

		// cookieKV: COOKIE1=def
		cookieKV := strings.SplitN(setCookie, ";", 2)[0]
		kv := strings.SplitN(cookieKV, "=", 2)
		if len(kv) != 2 {
			continue
		}
		k := strings.TrimSpace(kv[0])
		v := strings.TrimSpace(kv[1])

		// set order: add new cookie key at the end
		if _, ok := cookieKvMap[k]; !ok {
			orderedCookieKeys = append(orderedCookieKeys, k)
		}

		// put value from set-cookie into cookie map
		cookieKvMap[k] = v
	}

	// construct kv to cookie
	newCookieItems := make([]string, 0, len(cookieKvMap))
	for _, key := range orderedCookieKeys {
		item := fmt.Sprintf("%s=%s", key, cookieKvMap[key])
		newCookieItems = append(newCookieItems, item)
	}
	newCookieStr := strings.Join(newCookieItems, "; ")

	return newCookieStr
}
