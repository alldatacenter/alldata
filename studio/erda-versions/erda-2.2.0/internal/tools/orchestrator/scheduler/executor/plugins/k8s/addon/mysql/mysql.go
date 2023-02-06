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
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"time"

	"github.com/sirupsen/logrus"
	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	store "kmodules.xyz/objectstore-api/api/v1"
	ofst "kmodules.xyz/offshoot-api/api/v1"

	"github.com/erda-project/erda/apistructs"
	"github.com/erda-project/erda/internal/tools/orchestrator/scheduler/executor/plugins/k8s/addon"
	"github.com/erda-project/erda/pkg/http/httpclient"
	"github.com/erda-project/erda/pkg/parser/diceyml"
	"github.com/erda-project/erda/pkg/schedule/schedulepolicy/constraintbuilders"
	"github.com/erda-project/erda/pkg/strutil"
)

type MysqlOperator struct {
	k8s    addon.K8SUtil
	ns     addon.NamespaceUtil
	secret addon.SecretUtil
	pvc    addon.PVCUtil
	client *httpclient.HTTPClient
}

func New(k8s addon.K8SUtil, ns addon.NamespaceUtil, secret addon.SecretUtil, pvc addon.PVCUtil, client *httpclient.HTTPClient) *MysqlOperator {
	return &MysqlOperator{
		k8s:    k8s,
		ns:     ns,
		secret: secret,
		pvc:    pvc,
		client: client,
	}
}

func (my *MysqlOperator) IsSupported() bool {
	// At present, the mysql operator test is still a bit problematic, and it is temporarily set to not support
	return false

	// resp, err := my.client.Get(my.k8s.GetK8SAddr().Host).
	// 	Path("/apis/kubedb.com").
	// 	Do().
	// 	DiscardBody()
	// if err != nil {
	// 	logrus.Errorf("failed to query /apis/kubedb.com, host: %v, err: %v",
	// 		my.k8s.GetK8SAddr().Host, err)
	// 	return false
	// }
	// if !resp.IsOK() {
	// 	return false
	// }
	// return true
}

func (my *MysqlOperator) Validate(sg *apistructs.ServiceGroup) error {
	operator, ok := sg.Labels["USE_OPERATOR"]
	if !ok {
		return fmt.Errorf("[BUG] sg need USE_OPERATOR label")
	}
	if strutil.ToLower(operator) != "mysql" {
		return fmt.Errorf("[BUG] value of label USE_OPERATOR should be 'mysql'")
	}
	if len(sg.Services) != 1 {
		return fmt.Errorf("illegal services num: %d", len(sg.Services))
	}
	if sg.Services[0].Name != "mysql" {
		return fmt.Errorf("illegal service: %s, should be 'mysql'", sg.Services[0].Name)
	}
	if sg.Services[0].Env["MYSQL_ROOT_PASSWORD"] == "" {
		return fmt.Errorf("illegal service: %s, need env 'MYSQL_ROOT_PASSWORD'", sg.Services[0].Name)
	}
	return nil
}

type mysqlSecretBackupPVC struct {
	secret    *corev1.Secret
	mysql     *MySQL
	backuppvc *corev1.PersistentVolumeClaim
}

func (my *MysqlOperator) Convert(sg *apistructs.ServiceGroup) interface{} {
	mysql := sg.Services[0]
	scname := "dice-local-volume"
	nfsscname := "dice-nfs-volume"
	capacity := "20Gi"

	// volumes in an addon service has same storageclass
	if len(mysql.Volumes) > 0 {
		if mysql.Volumes[0].SCVolume.Capacity >= diceyml.AddonVolumeSizeMin && mysql.Volumes[0].SCVolume.Capacity <= diceyml.AddonVolumeSizeMax {
			capacity = fmt.Sprintf("%dGi", mysql.Volumes[0].SCVolume.Capacity)
		}

		if mysql.Volumes[0].SCVolume.Capacity > diceyml.AddonVolumeSizeMax {
			capacity = fmt.Sprintf("%dGi", diceyml.AddonVolumeSizeMax)
		}

		if mysql.Volumes[0].SCVolume.StorageClassName != "" {
			scname = mysql.Volumes[0].SCVolume.StorageClassName
			nfsscname = mysql.Volumes[0].SCVolume.StorageClassName
		}
	}

	replica := int32(mysql.Scale)
	passwd := mysql.Env["MYSQL_ROOT_PASSWORD"]
	secret := corev1.Secret{
		ObjectMeta: metav1.ObjectMeta{
			Name:      "mysql-root-password",
			Namespace: genK8SNamespace(sg.Type, sg.ID),
		},
		Data: map[string][]byte{
			"username": []byte("root"),
			"password": []byte(passwd),
		},
	}
	scheinfo := sg.ScheduleInfo2
	scheinfo.Stateful = true
	affinity := constraintbuilders.K8S(&scheinfo, nil, nil, nil).Affinity
	backupPVC := corev1.PersistentVolumeClaim{
		ObjectMeta: metav1.ObjectMeta{
			Name:      "backup-pvc",
			Namespace: genK8SNamespace(sg.Type, sg.ID),
		},
		Spec: corev1.PersistentVolumeClaimSpec{
			AccessModes: []corev1.PersistentVolumeAccessMode{corev1.ReadWriteOnce},
			Resources: corev1.ResourceRequirements{
				Requests: corev1.ResourceList{
					"storage": resource.MustParse(capacity),
				},
			},
			StorageClassName: &nfsscname,
		},
	}

	// set pvc annotations for snapshot
	if len(mysql.Volumes) > 0 {
		if mysql.Volumes[0].SCVolume.Snapshot.MaxHistory > 0 {
			if scname == apistructs.AlibabaSSDSC {
				backupPVC.Annotations = make(map[string]string)
				vs := diceyml.VolumeSnapshot{
					MaxHistory: mysql.Volumes[0].SCVolume.Snapshot.MaxHistory,
				}
				vsMap := map[string]diceyml.VolumeSnapshot{}
				vsMap[scname] = vs
				data, _ := json.Marshal(vsMap)
				backupPVC.Annotations[apistructs.CSISnapshotMaxHistory] = string(data)
			} else {
				logrus.Warnf("Service %s pvc volume use storageclass %s, it do not support snapshot. Only volume.type SSD for Alibaba disk SSD support snapshot\n", mysql.Name, scname)
			}
		}
	}

	mysqlstruct := MySQL{
		TypeMeta: metav1.TypeMeta{
			APIVersion: "kubedb.com/v1alpha1",
			Kind:       "MySQL",
		},
		ObjectMeta: metav1.ObjectMeta{
			Name:      sg.ID,
			Namespace: genK8SNamespace(sg.Type, sg.ID),
		},
		Spec: MySQLSpec{
			Version:  "5.7.25",
			Replicas: &replica,
			Storage: &corev1.PersistentVolumeClaimSpec{
				StorageClassName: &scname,
				AccessModes:      []corev1.PersistentVolumeAccessMode{"ReadWriteOnce"},
				Resources: corev1.ResourceRequirements{
					Requests: corev1.ResourceList{
						"storage": resource.MustParse(capacity),
					},
				},
			},
			DatabaseSecret: &corev1.SecretVolumeSource{
				SecretName: "mysql-root-password",
			},
			PodTemplate: ofst.PodTemplateSpec{
				Spec: ofst.PodSpec{
					Affinity: &affinity,
				},
			},
			BackupSchedule: &BackupScheduleSpec{
				CronExpression: "@every 1m",
				Backend: store.Backend{
					Local: &store.LocalSpec{
						VolumeSource: corev1.VolumeSource{
							PersistentVolumeClaim: &corev1.PersistentVolumeClaimVolumeSource{
								ClaimName: "backup-pvc",
							},
						},
						MountPath: "/backup",
					},
				},
			},
		},
	}

	// set Labels and annotations
	mysqlstruct.Labels = make(map[string]string)
	mysqlstruct.Annotations = make(map[string]string)
	addon.SetAddonLabelsAndAnnotations(mysql, mysqlstruct.Labels, mysqlstruct.Annotations)

	return mysqlSecretBackupPVC{
		secret:    &secret,
		mysql:     &mysqlstruct,
		backuppvc: &backupPVC,
	}
}

func (my *MysqlOperator) Create(k8syml interface{}) error {
	mysqlSecretBackupPVC, ok := k8syml.(mysqlSecretBackupPVC)
	if !ok {
		return fmt.Errorf("[BUG] this k8syml should be mysqlAndSecret")
	}
	mysql := mysqlSecretBackupPVC.mysql
	secret := mysqlSecretBackupPVC.secret
	backuppvc := mysqlSecretBackupPVC.backuppvc
	if err := my.ns.Exists(mysql.Namespace); err != nil {
		if err := my.ns.Create(mysql.Namespace, nil); err != nil {
			return err
		}
	}
	if err := my.secret.Create(secret); err != nil {
		return fmt.Errorf("failed to create mysql secret, %s/%s, err: %v", mysql.Namespace, mysql.Name, err)
	}
	if err := my.pvc.Create(backuppvc); err != nil {
		return fmt.Errorf("failed to create mysql backup pvc, %s/%s, err: %v", mysql.Namespace, mysql.Name, err)
	}
	var b bytes.Buffer
	resp, err := my.client.Post(my.k8s.GetK8SAddr()).
		Path(fmt.Sprintf("/apis/kubedb.com/v1alpha1/namespaces/%s/mysqls", mysql.Namespace)).
		JSONBody(mysql).
		Do().
		Body(&b)
	if err != nil {
		return fmt.Errorf("failed to create mysql, %s/%s, err: %v", mysql.Namespace, mysql.Name, err)
	}
	if !resp.IsOK() {
		return fmt.Errorf("failed to create mysql, %s/%s, statuscode: %v, body: %v",
			mysql.Namespace, mysql.Name, resp.StatusCode(), b.String())
	}
	return nil
}

func (my *MysqlOperator) Inspect(sg *apistructs.ServiceGroup) (*apistructs.ServiceGroup, error) {
	var b bytes.Buffer
	resp, err := my.client.Get(my.k8s.GetK8SAddr()).
		Path(fmt.Sprintf("/apis/kubedb.com/v1alpha1/namespaces/%s/mysqls/%s", genK8SNamespace(sg.Type, sg.ID), sg.ID)).
		Do().
		Body(&b)
	if err != nil {
		return nil, fmt.Errorf("failed to inspect mysql, %s/%s, err: %v",
			genK8SNamespace(sg.Type, sg.ID), sg.ID, err)
	}
	if !resp.IsOK() {
		return nil, fmt.Errorf("failed to inspect mysql, %s/%s, statuscode: %d, body: %v",
			genK8SNamespace(sg.Type, sg.ID), sg.ID, resp.StatusCode(), b.String())
	}
	var mysql MySQL
	if err := json.NewDecoder(&b).Decode(&mysql); err != nil {
		return nil, err
	}

	status := map[DatabasePhase]apistructs.StatusCode{
		DatabasePhaseRunning:      apistructs.StatusHealthy,
		DatabasePhaseCreating:     apistructs.StatusProgressing,
		DatabasePhaseInitializing: apistructs.StatusProgressing,
		DatabasePhaseFailed:       apistructs.StatusFailed,
		"":                        apistructs.StatusUnknown,
	}[mysql.Status.Phase]

	mysqlsvc := &(sg.Services[0])
	mysqlsvc.Status = status
	sg.Status = status

	mysqlsvc.Vip = strutil.Join([]string{sg.ID + "-gvr", genK8SNamespace(sg.Type, sg.ID), "svc.cluster.local"}, ".")

	secret, err := my.secret.Get(genK8SNamespace(sg.Type, sg.ID), "mysql-root-password")
	if err != nil {
		return nil, fmt.Errorf("failed to get secret: %s/%s, %v", genK8SNamespace(sg.Type, sg.ID), sg.ID, err)
	}
	sg.Labels["PASSWORD"] = string(secret.Data["password"])

	return sg, nil
}

func (my *MysqlOperator) Remove(sg *apistructs.ServiceGroup) error {
	k8snamespace := genK8SNamespace(sg.Type, sg.ID)
	var b bytes.Buffer
	resp, err := my.client.Delete(my.k8s.GetK8SAddr()).
		Path(fmt.Sprintf("/apis/kubedb.com/v1alpha1/namespaces/%s/mysqls/%s", k8snamespace, sg.ID)).
		Do().
		Body(&b)
	if err != nil {
		return fmt.Errorf("failed to remove mysql, %s/%s, err: %v", k8snamespace, sg.ID, err)
	}
	if !resp.IsOK() {
		return fmt.Errorf("failed to remove mysql, %s/%s, statuscode: %v, body: %v",
			k8snamespace, sg.ID, resp.StatusCode(), b.String())
	}
	// After mysql is deleted, drmn will be generated, and drmn needs to be deleted to completely delete mysql, otherwise pvc, secret, etc. are still there
	ctx, _ := context.WithTimeout(context.Background(), 5*time.Minute)
	if err := my.waitMysqlDeleted(ctx, k8snamespace, sg.ID); err != nil {
		if err == ctx.Err() {
			return fmt.Errorf("wait mysql deleted timeout(5min), %s/%s", k8snamespace, sg.ID)
		}
		return err
	}

	if err := my.ns.Delete(k8snamespace); err != nil {
		logrus.Errorf("failed to delete ns: %s, %v", k8snamespace, err)
		return nil
	}

	return nil
}

func (my *MysqlOperator) Update(k8syml interface{}) error {
	// TODO:
	return fmt.Errorf("mysqloperator not impl Update yet")
}

func (my *MysqlOperator) waitMysqlDeleted(ctx context.Context, namespace, name string) error {
	for {
		select {
		case <-ctx.Done():
			return ctx.Err()
		default:
		}
		var b bytes.Buffer
		resp, err := my.client.Get(my.k8s.GetK8SAddr()).
			Path(fmt.Sprintf("/apis/kubedb.com/v1alpha1/namespaces/%s/mysqls/%s", namespace, name)).
			Do().
			Body(&b)
		if err != nil {
			return fmt.Errorf("failed to get mysql, %s/%s, %v", namespace, name, err)
		}
		if resp.StatusCode() == 404 { // 已经被删除
			return nil
		}
		if !resp.IsOK() {
			return fmt.Errorf("failed to get mysql, %s/%s, statuscode:%v, body: %v",
				namespace, name, resp.StatusCode(), b.String())
		}
	}
	return nil
}

// TODO: Put it in k8sutil
func genK8SNamespace(namespace, name string) string {
	return strutil.Concat(namespace, "--", name)
}
