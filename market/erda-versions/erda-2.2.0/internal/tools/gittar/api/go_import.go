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
	"net/http"
	"strings"

	"github.com/erda-project/erda/internal/tools/gittar/conf"
	"github.com/erda-project/erda/internal/tools/gittar/webcontext"
)

func GetGoImportMeta(ctx *webcontext.Context) {
	// go import 元数据 相关参考
	// https://golang.org/cmd/go/#hdr-Remote_import_paths
	// https://www.jianshu.com/p/90ef66e41f3c
	// https://blog.zhaoweiguo.com/2019/09/24/golang-env-private-git/
	repoUrl := conf.UIPublicURL() + "/" + ctx.HttpRequest().URL.Path
	module := strings.Replace(repoUrl, "http://", "", 1)
	module = strings.Replace(repoUrl, "https://", "", 1)
	if ctx.Query("go-get") == "1" {
		ctx.Data(http.StatusOK,
			"text/html; charset=utf-8",
			[]byte(fmt.Sprintf(`<html>
		   <head>
		       <meta name="go-import" content="%s git %s" >
		   </head>
</html>`, module, repoUrl)))
	} else {
		ctx.Success("")
	}
}
