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

package uc

import (
	"bytes"
	"io"
	"net/http"

	"github.com/sirupsen/logrus"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/pkg/user"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apierrors"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/api/apis"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/auth"
	"github.com/erda-project/erda/internal/tools/openapi/legacy/conf"
	"github.com/erda-project/erda/pkg/excel"
	"github.com/erda-project/erda/pkg/http/httpserver/errorresp"
	"github.com/erda-project/erda/pkg/i18n"
	"github.com/erda-project/erda/pkg/strutil"
	"github.com/erda-project/erda/pkg/ucauth"
)

var UC_USER_EXPORT = apis.ApiSpec{
	Path:          "/api/users/actions/export",
	Scheme:        "http",
	Method:        "GET",
	Custom:        exportUsers,
	RequestType:   apistructs.UserPagingRequest{},
	CheckLogin:    false,
	TryCheckLogin: true,
	CheckToken:    true,
	IsOpenAPI:     true,
	ChunkAPI:      true,
	Doc:           "summary: 导出用户",
}

func exportUsers(w http.ResponseWriter, r *http.Request) {
	operatorID, err := user.GetUserID(r)
	if err != nil {
		apierrors.ErrAdminUser.NotLogin().Write(w)
		return
	}

	if err := checkPermission(operatorID, apistructs.GetAction); err != nil {
		errorresp.ErrWrite(err, w)
		return
	}

	token, err := auth.GetDiceClientToken()
	if err != nil {
		logrus.Errorf("failed to get token: %v", err)
		apierrors.ErrListUser.InternalError(err).
			Write(w)
		return
	}

	// check login & permission
	_, err = mustManageUsersPerm(r, apierrors.ErrListUser)
	if err != nil {
		errorresp.ErrWrite(err, w)
		return
	}

	req, err := getPagingUsersReq(r)
	if err != nil {
		errorresp.ErrWrite(err, w)
		return
	}
	req.PageSize = 1024

	var users []apistructs.UserInfoExt
	for i := 0; i < 100; i++ {
		data, err := ucauth.HandlePagingUsers(req, token)
		if err != nil {
			errorresp.ErrWrite(err, w)
			return
		}
		u := ucauth.ConvertToUserInfoExt(data)
		users = append(users, u.List...)
		if len(u.List) < 1024 {
			break
		}
		req.PageNo++
	}

	var withRole bool
	if r.URL.Query().Get("withRole") == "true" || conf.ExportUserWithRole() {
		withRole = true
	}

	local := i18n.GetLocaleNameByRequest(r)
	if local == "" {
		local = "zh-CN"
	}
	loginMethodMap, err := getLoginMethodMap(token, local)
	if err != nil {
		errorresp.ErrWrite(err, w)
		return
	}

	reader, tablename, err := exportExcel(users, withRole, loginMethodMap)
	if err != nil {
		errorresp.ErrWrite(err, w)
		return
	}

	w.Header().Add("Content-Disposition", "attachment;fileName="+tablename+".xlsx")
	w.Header().Add("Content-Type", "application/vnd.ms-excel")
	if _, err := io.Copy(w, reader); err != nil {
		errorresp.ErrWrite(err, w)
		return
	}
}

// getLoginMethodMap get the mapping relationship between login mode value and display name
func getLoginMethodMap(token ucauth.OAuthToken, local string) (map[string]string, error) {
	res, err := handleListLoginMethod(token)
	if err != nil {
		return nil, err
	}

	valueDisplayNameMap := make(map[string]string, 0)
	deDubMap := make(map[string]string, 0)
	for _, v := range res.RegistryType {
		tmp := getLoginTypeByUC(v)
		if _, ok := deDubMap[tmp["marks"]]; ok {
			continue
		}
		valueDisplayNameMap[tmp["marks"]] = tmp[local]
		deDubMap[tmp["marks"]] = ""
	}

	return valueDisplayNameMap, nil
}

func exportExcel(users []apistructs.UserInfoExt, withRole bool, loginMethodMap map[string]string) (io.Reader, string, error) {
	var (
		tabel     [][]string
		err       error
		tabelName = "users"
		buf       = bytes.NewBuffer([]byte{})
	)

	if withRole {
		tabel, err = convertUserToExcelListWithRoles(users, loginMethodMap)
		if err != nil {
			return nil, "", err
		}
	} else {
		tabel = convertUserToExcelList(users, loginMethodMap)
	}

	if err := excel.ExportExcel(buf, tabel, tabelName); err != nil {
		return nil, "", err
	}

	return buf, tabelName, nil
}

func convertUserToExcelList(users []apistructs.UserInfoExt, loginMethodMap map[string]string) [][]string {
	r := [][]string{{"用户名", "昵称", "邮箱", "手机号", "登录方式", "上次登录时间", "密码过期时间", "状态"}}
	for _, user := range users {
		var state string
		if user.Locked {
			state = "冻结"
		} else {
			state = "未冻结"
		}
		r = append(r, append(append([]string{user.Name, user.Nick, user.Email, user.Phone, loginMethodMap[user.Source], user.LastLoginAt, user.PwdExpireAt, state})))
	}

	return r
}

// 生成二维数组，每个用户在他所属的最小组织单位下的角色是一行记录，之后再合并单元格
func convertUserToExcelListWithRoles(users []apistructs.UserInfoExt, loginMethodMap map[string]string) ([][]string, error) {
	orgnazation, err := bdl.GetAllOrganizational()
	if err != nil {
		return nil, err
	}

	target := [][]string{{"用户名", "昵称", "邮箱", "手机号", "登录方式", "上次登录时间", "密码过期时间", "状态", "企业",
		"企业角色", "项目", "项目角色", "应用", "应用角色"}}

	for _, user := range users {
		var state string
		if user.Locked {
			state = "冻结"
		} else {
			state = "未冻结"
		}
		fixedData := []string{user.Name, user.Nick, user.Email, user.Phone, loginMethodMap[user.Source], user.LastLoginAt, user.PwdExpireAt, state}
		if _, ok := orgnazation.Members[user.ID]; !ok {
			target = append(target, fixedData)
			continue
		}

		// 企业级
		for org, prjs := range orgnazation.Organization {
			orgData := make([]string, 8, 8)
			copy(orgData, fixedData)
			// 未加入该企业
			if _, ok := orgnazation.Members[user.ID]["org"][org]; !ok {
				continue
			}
			orgData = append(orgData, org, strutil.Join(orgnazation.Members[user.ID]["org"][org].Roles, ","))
			// 该企业下没有任何项目或者该用户没有加入该企业下的任何项目，该企业是该用户最小的组织单位
			if len(prjs) == 0 || !orgnazation.IsInPrj(prjs, "prj", user.ID) {
				target = append(target, orgData)
				continue
			}
			// 项目级
			for prj, apps := range prjs {
				prjData := make([]string, 10, 10)
				copy(prjData, orgData)
				// 未加入该项目
				if _, ok := orgnazation.Members[user.ID]["prj"][prj]; !ok {
					continue
				}
				prjData = append(prjData, prj, strutil.Join(orgnazation.Members[user.ID]["prj"][prj].Roles, ","))
				// 该项目下没有任何应用或者该用户没有加入该项目下的任何应用，该项目是该用户最小的组织单位
				if len(apps) == 0 || !orgnazation.IsInApp(apps, "app", user.ID) {
					target = append(target, prjData)
					continue
				}
				// 应用级
				for _, app := range apps {
					appData := make([]string, 12, 12)
					copy(appData, prjData)
					// 未加入该应用
					if _, ok := orgnazation.Members[user.ID]["app"][app]; !ok {
						continue
					}
					// 该应用是该用户最小的组织单位
					appData = append(appData, app, strutil.Join(orgnazation.Members[user.ID]["app"][app].Roles, ","))
					target = append(target, appData)
				}
			}
		}
	}

	return target, nil
}

// mergeInfo [][]int{{0, 7 ,1}, {8, 10, 2}}
// 该行的第0列到第7列，垂直合并1个单元格
// 该行的第8列到第10列，垂直合并2个单元格
// func fillMergeMap(mergeMap map[int]map[int]*int, mergeInfo [][]int, row int) {
// 	if _, ok := mergeMap[row]; !ok {
// 		mergeMap[row] = make(map[int]int, 0)
// 	}
//
// 	for _, v := range mergeInfo {
// 		for i := v[0]; i <= v[1]; i++ {
// 			mergeMap[row][i] = v[2]
// 		}
// 	}
// }
//
// func genCell(data []string) []excel.Cell {
// 	cells := make([]excel.Cell, 0)
// 	for i, v := range data {
// 		cells = append(cells, excel.NewCell(v))
// 	}
// 	return cells
// }
