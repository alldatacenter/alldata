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

package clusters

import (
	"context"
	"fmt"
	"strings"

	"github.com/sirupsen/logrus"
	"google.golang.org/grpc/metadata"
	corev1 "k8s.io/api/core/v1"
	k8serrors "k8s.io/apimachinery/pkg/api/errors"
	v1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/kubernetes"

	"github.com/erda-project/erda-infra/pkg/transport"
	clusterpb "github.com/erda-project/erda-proto-go/core/clustermanager/cluster/pb"
	tokenpb "github.com/erda-project/erda-proto-go/core/token/pb"
	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/apps/cmp/conf"
	"github.com/erda-project/erda/internal/apps/cmp/dbclient"
	"github.com/erda-project/erda/pkg/http/httputil"
	"github.com/erda-project/erda/pkg/k8sclient"
	"github.com/erda-project/erda/pkg/oauth2/tokenstore/mysqltokenstore"
)

// GetAccessKey get access key with cluster name.
func (c *Clusters) GetAccessKey(clusterName string) (*tokenpb.QueryTokensResponse, error) {
	return c.credential.QueryTokens(context.Background(), &tokenpb.QueryTokensRequest{
		Scope:   strings.ToLower(tokenpb.ScopeEnum_CMP_CLUSTER.String()),
		ScopeId: clusterName,
	})
}

// GetOrCreateAccessKey get or create access key
func (c *Clusters) GetOrCreateAccessKey(ctx context.Context, clusterName string) (*tokenpb.Token, error) {
	ak, err := c.GetAccessKey(clusterName)
	if err != nil {
		logrus.Errorf("get access key error when create precheck: %v", err)
		return nil, err
	}

	// if accessKey already exist, return the latest
	if ak.Total != 0 {
		return ak.Data[0], nil
	}

	// Check cluster exist or not
	if err = c.CheckCluster(ctx, clusterName); err != nil {
		return nil, err
	}

	// Create accessKey
	res, err := c.credential.CreateToken(context.Background(), &tokenpb.CreateTokenRequest{
		Scope:   strings.ToLower(tokenpb.ScopeEnum_CMP_CLUSTER.String()),
		ScopeId: clusterName,
		Type:    mysqltokenstore.AccessKey.String(),
	})

	if err != nil {
		logrus.Errorf("create access key error: %v", err)
		return nil, err
	}

	return res.Data, nil
}

// GetOrCreateAccessKeyWithRecord get or create access key with record
func (c *Clusters) GetOrCreateAccessKeyWithRecord(ctx context.Context, clusterName, userID, orgID string) (*tokenpb.Token, error) {
	var (
		detailInfo string
		err        error
		res        = &tokenpb.Token{}
		status     = dbclient.StatusTypeSuccess
	)

	if clusterName == conf.ErdaClusterName() {
		if cs, err := k8sclient.NewForInCluster(); err == nil {
			res, err = c.ResetAccessKeyWithClientSet(ctx, clusterName, cs.ClientSet)
		}
	} else {
		res, err = c.GetOrCreateAccessKey(ctx, clusterName)
	}

	if err != nil {
		detailInfo = err.Error()
		status = dbclient.StatusTypeFailed
	}

	// Record create credential success or error
	_, recordError := c.db.RecordsWriter().Create(&dbclient.Record{
		RecordType:  dbclient.RecordTypeCreateClusterCredential,
		UserID:      userID,
		OrgID:       orgID,
		ClusterName: clusterName,
		Status:      status,
		Detail:      detailInfo,
	})

	if recordError != nil {
		logrus.Errorf("recorde create cluster credential error: %v", recordError)
	}

	if err != nil {
		return nil, err
	}

	return res, nil
}

// DeleteAccessKey Delete access key
func (c *Clusters) DeleteAccessKey(clusterName string) error {
	res, err := c.GetAccessKey(clusterName)
	if err != nil {
		return err
	}

	// Doesn't need delete
	if res.Total == 0 {
		return nil
	}

	var errResp string

	// Clear all access key about clusterName
	for _, item := range res.Data {
		_, err = c.credential.DeleteToken(context.Background(), &tokenpb.DeleteTokenRequest{
			Id: item.Id,
		})
		if err != nil {
			logrus.Errorf("delete accesskey error: %v", err)
			errResp += err.Error()
		}
	}

	if len(errResp) != 0 {
		return fmt.Errorf("delete accessKey abount cluster %v error: %v", clusterName, err)
	}

	return nil
}

// ResetAccessKey reset access key
func (c *Clusters) ResetAccessKey(ctx context.Context, clusterName string) (*tokenpb.Token, error) {
	// In cluster use Inner clientSet priority.
	if clusterName == conf.ErdaClusterName() {
		kc, err := k8sclient.NewForInCluster()
		if err != nil {
			tipErr := fmt.Errorf("get inCluster kubernetes client error: %v", err)
			logrus.Errorf(tipErr.Error())
			return nil, tipErr
		}
		return c.ResetAccessKeyWithClientSet(ctx, clusterName, kc.ClientSet)
	}

	// Get configmap and PreCheck cluster connection
	kc, err := k8sclient.NewWithTimeOut(clusterName, getClusterTimeout)
	if err != nil {
		logrus.Errorf("get kubernetes client error when update accesskey, err: %v", err)
		tipErr := fmt.Errorf("connect to cluster: %s error: %v", clusterName, err)
		return nil, tipErr
	}

	// Get kubernetes clientSet
	cs := kc.ClientSet

	return c.ResetAccessKeyWithClientSet(ctx, clusterName, cs)
}

// ResetAccessKeyWithClientSet reset access key with specified clientSet
func (c *Clusters) ResetAccessKeyWithClientSet(ctx context.Context, clusterName string, cs kubernetes.Interface) (*tokenpb.Token, error) {
	// Get worker namespace
	workerNs := getWorkerNamespace()

	sec, err := cs.CoreV1().Secrets(workerNs).Get(context.Background(), apistructs.ErdaClusterCredential, v1.GetOptions{})
	if err != nil {
		if !k8serrors.IsNotFound(err) {
			tipErr := fmt.Errorf("get worker cluster config err: %v", err)
			logrus.Error(tipErr)
			return nil, tipErr
		}

		logrus.Info("cluster credential secret doesn't exist, create it.")
		sec, err = cs.CoreV1().Secrets(workerNs).Create(context.Background(), &corev1.Secret{
			ObjectMeta: v1.ObjectMeta{
				Name: apistructs.ErdaClusterCredential,
			},
		}, v1.CreateOptions{})
		if err != nil {
			logrus.Errorf("create cluster credential secret error: %v", err)
			return nil, err
		}
	}

	// Delete access key
	if err = c.DeleteAccessKey(clusterName); err != nil {
		logrus.Errorf("delete access key error: %v", err)
		return nil, err
	}

	// Create new accessKey
	newAk, err := c.GetOrCreateAccessKey(ctx, clusterName)
	if err != nil {
		logrus.Errorf("create access key error: %v", err)
		return nil, err
	}

	if sec.Data == nil {
		sec.Data = make(map[string][]byte, 0)
	}

	// Update secret
	sec.Data[apistructs.ClusterAccessKey] = []byte(newAk.AccessKey)

	_, err = cs.CoreV1().Secrets(workerNs).Update(context.Background(), sec, v1.UpdateOptions{})
	if err != nil {
		logrus.Errorf("update worker cluster credential error: %v", err)
		return nil, err
	}

	return newAk, nil
}

// ResetAccessKeyWithRecord reset ak with record
func (c *Clusters) ResetAccessKeyWithRecord(ctx context.Context, clusterName, userID, orgID string) (*tokenpb.Token, error) {
	var (
		detailInfo string
		status     = dbclient.StatusTypeSuccess
	)

	res, err := c.ResetAccessKey(ctx, clusterName)
	if err != nil {
		detailInfo = err.Error()
		status = dbclient.StatusTypeFailed
	}

	// Record reset credential success or error
	_, recordError := c.db.RecordsWriter().Create(&dbclient.Record{
		RecordType:  dbclient.RecordTypeResetClusterCredential,
		UserID:      userID,
		OrgID:       orgID,
		ClusterName: clusterName,
		Status:      status,
		Detail:      detailInfo,
	})

	if recordError != nil {
		logrus.Errorf("recorde reset cluster credneital error: %v", recordError)
	}

	if err != nil {
		return nil, err
	}

	return res, nil
}

// CheckCluster check cluster
func (c *Clusters) CheckCluster(ctx context.Context, clusterName string) error {
	// check cluster exist or not
	ctx = transport.WithHeader(ctx, metadata.New(map[string]string{httputil.InternalHeader: "true"}))
	_, err := c.clusterSvc.GetCluster(ctx, &clusterpb.GetClusterRequest{IdOrName: clusterName})
	if err != nil {
		logrus.Errorf("check cluster error: %v", err)
		return err
	}
	return nil
}
