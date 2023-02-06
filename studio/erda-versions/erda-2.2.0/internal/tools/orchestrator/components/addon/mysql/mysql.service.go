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

package mysql

import (
	"context"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"strconv"
	"time"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
	"google.golang.org/protobuf/types/known/timestamppb"

	"github.com/erda-project/erda-infra/base/logs"
	"github.com/erda-project/erda-proto-go/orchestrator/addon/mysql/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/dbclient"
	"github.com/erda-project/erda/pkg/common/apis"
	"github.com/erda-project/erda/pkg/crypto/encryption"
	"github.com/erda-project/erda/pkg/mysqlhelper"
	"github.com/erda-project/erda/pkg/strutil"
)

type mysqlService struct {
	logger logs.Logger

	kms  KMSWrapper
	perm PermissionWrapper
	db   *dbclient.DBClient

	encrypt *encryption.EnvEncrypt
}

// ListMySQLAccount returns a list of MySQL accounts
func (s *mysqlService) ListMySQLAccount(ctx context.Context, req *pb.ListMySQLAccountRequest) (*pb.ListMySQLAccountResponse, error) {
	userID := apis.GetUserID(ctx)
	if userID == "" {
		return nil, fmt.Errorf("user not login")
	}
	routing, err := s.db.GetInstanceRouting(req.InstanceId)
	if err != nil {
		return nil, err
	}
	if err := s.mustHavePerm(userID, routing, "addon", "GET"); err != nil {
		return nil, err
	}
	accounts, err := s.db.GetMySQLAccountListByRoutingInstanceID(req.InstanceId)
	if err != nil {
		return nil, err
	}
	res := make([]*pb.MySQLAccount, len(accounts))
	for i, acc := range accounts {
		res[i] = s.ToDTO(&acc, true)
	}
	return &pb.ListMySQLAccountResponse{Accounts: res}, nil
}

func (s *mysqlService) ToDTO(acc *dbclient.MySQLAccount, decrypt bool) *pb.MySQLAccount {
	pass := "******"
	if decrypt {
		p, err := s.decrypt(acc)
		if err != nil {
			if s.logger != nil {
				s.logger.Errorf("pass decrypt failed, MySQLAccountID: %s, err: %+v", acc.ID, err)
			}
		}
		pass = p
	}
	return &pb.MySQLAccount{
		Id:         acc.ID,
		InstanceId: acc.RoutingInstanceID,
		Creator:    acc.Creator,
		CreateAt:   timestamppb.New(acc.CreatedAt),
		Username:   acc.Username,
		Password:   pass,
	}
}

type connConfig struct {
	Host string `json:"MYSQL_HOST"`
	Port string `json:"MYSQL_PORT"`
	User string `json:"MYSQL_USERNAME"`
	Pass string `json:"MYSQL_PASSWORD"`
}

func (s *mysqlService) getConnConfig(ins *dbclient.AddonInstance) (*connConfig, error) {
	var mysqlConfig connConfig
	if err := json.Unmarshal([]byte(ins.Config), &mysqlConfig); err != nil {
		return nil, err
	}
	if mysqlConfig.Host == "" || mysqlConfig.Port == "" || mysqlConfig.User == "" || mysqlConfig.Pass == "" {
		return nil, fmt.Errorf("missing key MYSQL_HOST | MYSQL_PASSWORD | MYSQL_PORT | MYSQL_USERNAME")
	}
	decryptData, err := s.kms.Decrypt(mysqlConfig.Pass, ins.KmsKey)
	if err != nil {
		return nil, err
	}
	b, err := base64.StdEncoding.DecodeString(decryptData.PlaintextBase64)
	if err != nil {
		return nil, err
	}
	pass := string(b)
	mysqlConfig.Pass = pass
	return &mysqlConfig, nil
}

func (s *mysqlService) execSql(ins *dbclient.AddonInstance, sql ...string) error {
	if len(sql) == 0 {
		return nil
	}
	mysqlConfig, err := s.getConnConfig(ins)
	if err != nil {
		return err
	}

	var req mysqlhelper.Request
	req.Url = mysqlConfig.Host + ":" + mysqlConfig.Port
	req.User = mysqlConfig.User
	req.Password = mysqlConfig.Pass
	req.Sqls = sql
	req.ClusterKey = ins.Cluster
	return req.Exec()
}

func (s *mysqlService) GenerateMySQLAccount(ctx context.Context, req *pb.GenerateMySQLAccountRequest) (*pb.GenerateMySQLAccountResponse, error) {
	userID := apis.GetUserID(ctx)
	if userID == "" {
		return nil, errors.Errorf("user not login")
	}
	routing, err := s.db.GetInstanceRouting(req.InstanceId)
	if err != nil {
		return nil, err
	}
	if err := s.mustHavePerm(userID, routing, "addon", "UPDATE"); err != nil {
		return nil, err
	}

	ins, err := s.db.GetAddonInstance(routing.RealInstance)
	if err != nil {
		return nil, err
	}

	user := "mysql-" + strutil.RandStr(6)
	pass := strutil.RandStr(12)

	sql := []string{
		fmt.Sprintf(`CREATE USER '%s'@'%%' IDENTIFIED by '%s';`, user, pass),
		fmt.Sprintf(`GRANT ALL ON *.* TO '%s'@'%%';`, user),
		"flush privileges;",
	}

	if err := s.execSql(ins, sql...); err != nil {
		return nil, err
	}

	kr, err := s.kms.CreateKey()
	if err != nil {
		return nil, err
	}
	encryptData, err := s.kms.Encrypt(pass, kr.KeyMetadata.KeyID)
	if err != nil {
		return nil, err
	}

	account := &dbclient.MySQLAccount{
		Username:          user,
		Password:          encryptData.CiphertextBase64,
		KMSKey:            kr.KeyMetadata.KeyID,
		InstanceID:        routing.RealInstance,
		RoutingInstanceID: req.InstanceId,
		Creator:           userID,
	}
	if err := s.db.CreateMySQLAccount(account); err != nil {
		return nil, err
	}
	dto := s.ToDTO(account, false)
	dto.Password = pass

	s.auditNoError(ctx, userID, apis.GetOrgID(ctx), routing, nil, apistructs.CreateMySQLAddonAccountTemplate,
		map[string]interface{}{
			"mysqlUsername": dto.Username,
		},
	)

	return &pb.GenerateMySQLAccountResponse{
		Account: dto,
	}, nil
}

func (s *mysqlService) DeleteMySQLAccount(ctx context.Context, req *pb.DeleteMySQLAccountRequest) (*pb.DeleteMySQLAccountResponse, error) {
	userID := apis.GetUserID(ctx)
	if userID == "" {
		return nil, fmt.Errorf("user not login")
	}
	routing, err := s.db.GetInstanceRouting(req.InstanceId)
	if err != nil {
		return nil, err
	}
	if err := s.mustHavePerm(userID, routing, "addon", "UPDATE"); err != nil {
		return nil, err
	}
	ins, err := s.db.GetAddonInstance(routing.RealInstance)
	if err != nil {
		return nil, err
	}
	account, err := s.db.GetMySQLAccountByID(req.Id)
	if err != nil {
		return nil, err
	}

	sql := []string{
		fmt.Sprintf(`DROP USER IF EXISTS '%s'@'%%';`, account.Username),
		"flush privileges;",
	}

	if err := s.execSql(ins, sql...); err != nil {
		return nil, err
	}

	account.IsDeleted = true
	if err := s.db.UpdateMySQLAccount(account); err != nil {
		return nil, err
	}

	s.auditNoError(ctx, userID, apis.GetOrgID(ctx), routing, nil, apistructs.DeleteMySQLAddonAccountTemplate,
		map[string]interface{}{
			"mysqlUsername": account.Username,
		},
	)

	return &pb.DeleteMySQLAccountResponse{}, nil
}

func (s *mysqlService) ListAttachment(ctx context.Context, req *pb.ListAttachmentRequest) (*pb.ListAttachmentResponse, error) {
	userID := apis.GetUserID(ctx)
	if userID == "" {
		return nil, fmt.Errorf("user not login")
	}
	routing, err := s.db.GetInstanceRouting(req.InstanceId)
	if err != nil {
		return nil, err
	}
	if err := s.mustHavePerm(userID, routing, "addon", "GET"); err != nil {
		return nil, err
	}
	editPerm, err := s.checkPerm(userID, routing, "addon", "UPDATE")
	if err != nil {
		return nil, err
	}
	attachments, err := s.db.GetAttachmentsByRoutingInstanceID(req.InstanceId)
	if err != nil {
		return nil, err
	}
	res := make([]*pb.Attachment, len(*attachments))
	for i, att := range *attachments {
		cf, err := s.getConfig(&att, editPerm)
		if err != nil {
			s.logger.Errorf("get config of instance %s failed, err: %+v", att.InstanceID, err)
		}
		res[i] = &pb.Attachment{
			Id:           att.ID,
			InstanceId:   att.RoutingInstanceID,
			AppId:        att.ApplicationID,
			Workspace:    routing.Workspace,
			RuntimeId:    att.RuntimeID,
			RuntimeName:  att.RuntimeName,
			AccountId:    att.MySQLAccountID,
			PreAccountId: att.PreviousMySQLAccountID,
			AccountState: att.MySQLAccountState,
			Configs:      cf,
		}
	}
	return &pb.ListAttachmentResponse{Attachments: res}, nil
}

func (s *mysqlService) UpdateAttachmentAccount(ctx context.Context, req *pb.UpdateAttachmentAccountRequest) (*pb.UpdateAttachmentAccountResponse, error) {
	userID := apis.GetUserID(ctx)
	if userID == "" {
		return nil, fmt.Errorf("user not login")
	}
	routing, err := s.db.GetInstanceRouting(req.InstanceId)
	if err != nil {
		return nil, err
	}
	if err := s.mustHavePerm(userID, routing, "addon", "UPDATE"); err != nil {
		return nil, err
	}
	att, err := s.db.GetAttachmentByID(req.Id)
	if err != nil {
		return nil, err
	}
	if req.AccountId == att.MySQLAccountID {
		// no need to update
		return &pb.UpdateAttachmentAccountResponse{}, nil
	}
	preAcc, err := s.db.GetMySQLAccountByID(att.MySQLAccountID)
	if err != nil {
		return nil, err
	}
	nextAcc, err := s.db.GetMySQLAccountByID(req.AccountId)
	if err != nil {
		return nil, err
	}
	if att.MySQLAccountState == "PRE" {
		// in switching
		if req.AccountId == att.PreviousMySQLAccountID {
			// switch back
			att.MySQLAccountState = "CUR"
			att.MySQLAccountID = req.AccountId
			att.PreviousMySQLAccountID = ""
		} else {
			// switch to new account
			att.MySQLAccountID = req.AccountId
		}
	} else {
		// normal state
		att.MySQLAccountState = "PRE"
		att.PreviousMySQLAccountID = att.MySQLAccountID
		att.MySQLAccountID = req.AccountId
	}
	if err := s.db.UpdateAttachment(att); err != nil {
		return nil, err
	}

	s.auditNoError(ctx, userID, apis.GetOrgID(ctx), routing, att, apistructs.ResetAttachmentMySQLAddonAccountTemplate,
		map[string]interface{}{
			"mysqlUsername":    nextAcc.Username,
			"preMysqlUsername": preAcc.Username,
		},
	)

	return &pb.UpdateAttachmentAccountResponse{}, nil
}

func (s *mysqlService) decrypt(acc *dbclient.MySQLAccount) (string, error) {
	pass := acc.Password
	if acc.KMSKey != "" {
		dr, err := s.kms.Decrypt(acc.Password, acc.KMSKey)
		if err != nil {
			return pass, err
		}
		rawSecret, err := base64.StdEncoding.DecodeString(dr.PlaintextBase64)
		if err != nil {
			return pass, err
		}
		pass = string(rawSecret)
	} else {
		// try to decrypt in old-style
		_password, err := s.encrypt.DecryptPassword(acc.Password)
		if err != nil {
			logrus.Errorf("failed to decrypt password for mysql account %s: %s", acc.ID, err)
		} else {
			pass = _password
		}
	}
	return pass, nil
}

func (s *mysqlService) getConfig(att *dbclient.AddonAttachment, decrypt bool) (map[string]string, error) {
	ins, err := s.db.GetAddonInstance(att.InstanceID)
	if err != nil {
		return nil, err
	}
	var configMap map[string]string
	if err := json.Unmarshal([]byte(ins.Config), &configMap); err != nil {
		return nil, err
	}
	if att.MySQLAccountID != "" {
		acc, err := s.db.GetMySQLAccountByID(att.MySQLAccountID)
		if err != nil {
			return nil, err
		}
		configMap["MYSQL_USERNAME"] = acc.Username
		pass := "******"
		if decrypt {
			pass, err = s.decrypt(acc)
			if err != nil {
				return nil, err
			}
		}
		configMap["MYSQL_PASSWORD"] = pass
	} else {
		configMap["MYSQL_PASSWORD"] = "******"
	}
	return configMap, nil
}

func (s *mysqlService) mustHavePerm(userID string, routing *dbclient.AddonInstanceRouting, resource string, action string) error {
	r, err := s.checkPerm(userID, routing, resource, action)
	if err != nil {
		return err
	}
	if !r {
		return fmt.Errorf("user %s has no %s permission on %s", userID, action, resource)
	}
	return nil
}

func (s *mysqlService) checkPerm(userID string, routing *dbclient.AddonInstanceRouting, resource string, action string) (bool, error) {
	projectID, err := strutil.Atoi64(routing.ProjectID)
	if err != nil {
		return false, err
	}
	pr, err := s.perm.CheckPermission(&apistructs.PermissionCheckRequest{
		UserID:   userID,
		Scope:    apistructs.ProjectScope,
		ScopeID:  uint64(projectID),
		Resource: resource,
		Action:   action,
	})
	if err != nil {
		return false, err
	}
	return pr.Access, nil
}

func (s *mysqlService) auditNoError(ctx context.Context, userID string, orgID string, routing *dbclient.AddonInstanceRouting, att *dbclient.AddonAttachment,
	tmplName apistructs.TemplateName, tmplCtx map[string]interface{}) {
	err := s.audit(ctx, userID, orgID, routing, att, tmplName, tmplCtx)
	if err != nil {
		s.logger.Errorf("audit %s failed, err: %+v", tmplName, err)
	}
}

func (s *mysqlService) audit(ctx context.Context, userID string, orgID string, routing *dbclient.AddonInstanceRouting, att *dbclient.AddonAttachment,
	tmplName apistructs.TemplateName, tmplCtx map[string]interface{}) error {
	oid, err := strutil.Atoi64(orgID)
	if err != nil {
		return err
	}

	pid, err := strutil.Atoi64(routing.ProjectID)
	if err != nil {
		return err
	}

	project, err := s.perm.GetProject(uint64(pid))
	if err != nil {
		return err
	}
	tmplCtx["projectId"] = routing.ProjectID
	tmplCtx["projectName"] = project.Name
	tmplCtx["instanceId"] = routing.ID
	tmplCtx["workspace"] = routing.Workspace

	var appID uint64
	if att != nil {
		aid, err := strutil.Atoi64(att.ApplicationID)
		if err != nil {
			return err
		}
		appID = uint64(aid)
		app, err := s.perm.GetApp(appID)
		if err != nil {
			return err
		}
		tmplCtx["appId"] = att.ApplicationID
		tmplCtx["appName"] = app.Name
		tmplCtx["runtimeId"] = att.RuntimeID
		tmplCtx["runtimeName"] = att.RuntimeName
	}

	// TODO: direct use time.Time
	now := strconv.FormatInt(time.Now().Unix(), 10)
	audit := apistructs.Audit{
		UserID:       userID,
		ScopeType:    apistructs.ProjectScope,
		ScopeID:      uint64(pid),
		OrgID:        uint64(oid),
		ProjectID:    uint64(pid),
		AppID:        appID,
		Result:       "success",
		StartTime:    now,
		EndTime:      now,
		TemplateName: tmplName,
		Context:      tmplCtx,
		ClientIP:     apis.GetClientIP(ctx),
	}
	return s.perm.CreateAuditEvent(&apistructs.AuditCreateRequest{Audit: audit})
}
