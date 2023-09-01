/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package webhook

import (
	"context"
	"crypto/tls"
	"crypto/x509"
	"fmt"
	"net"

	corev1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/kubernetes"
	"k8s.io/klog/v2"
	ctrl "sigs.k8s.io/controller-runtime"
	"sigs.k8s.io/controller-runtime/pkg/manager"

	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/utils"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/webhook/config"
	webhookconstants "github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/webhook/constants"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/webhook/inspector"
	"github.com/apache/incubator-uniffle/deploy/kubernetes/operator/pkg/webhook/syncer"
)

const (
	certsSecretName = "rss-webhook-certs"

	serverCert = "server.crt"
	serverKey  = "server.key"
	caCert     = "ca.crt"
)

// AdmissionManager manages admission webhook server for shuffle servers.
type AdmissionManager interface {
	manager.Runnable
}

// NewAdmissionManager creates an AdmissionManager.
func NewAdmissionManager(cfg *config.Config) AdmissionManager {
	return newAdmissionManager(cfg)
}

// newAdmissionManager creates an admissionManager.
func newAdmissionManager(cfg *config.Config) *admissionManager {
	am := &admissionManager{
		externalService: cfg.ExternalService,
		kubeClient:      cfg.KubeClient,
	}
	if !cfg.NeedLoadCertsFromSecret() {
		am.caCertBody = cfg.GetCaCert()
		am.tlsConfig = cfg.TLSConfig()
	} else {
		am.loadCertsFromSecret()
	}
	mgr, err := ctrl.NewManager(cfg.RESTConfig, ctrl.Options{
		LeaderElection:          true,
		LeaderElectionID:        cfg.LeaderElectionID(),
		LeaderElectionNamespace: utils.GetCurrentNamespace(),
	})
	if err != nil {
		klog.Fatalf("build manager for admission manager failed: %v", err)
	}
	am.mgr = mgr
	am.syncer = syncer.NewConfigSyncer(am.caCertBody, cfg.ExternalService, cfg.KubeClient)
	am.inspector = inspector.NewInspector(cfg, am.tlsConfig)
	return am
}

// admissionManager implements the AdmissionManager interface.
type admissionManager struct {
	externalService string
	caCertBody      []byte
	tlsConfig       *tls.Config

	kubeClient kubernetes.Interface

	mgr       manager.Manager
	syncer    syncer.ConfigSyncer
	inspector inspector.Inspector
}

// Start starts the AdmissionManager.
func (am *admissionManager) Start(ctx context.Context) error {
	stopCh := ctx.Done()
	if err := am.mgr.Add(am.syncer); err != nil {
		klog.Errorf("add syncer to mgr of admission manager failed: %v", err)
		return err
	}
	go func() {
		if err := am.mgr.Start(ctx); err != nil {
			klog.Fatalf("mgr of admission manager started failed: %v", err)
		}
	}()
	go func() {
		if err := am.inspector.Start(ctx); err != nil {
			klog.Fatalf("start webhook server failed: %v", err)
		}
	}()
	klog.V(2).Info("admission manager started")
	<-stopCh
	return nil
}

// generateCerts generates certificate and privateKey and ca certificate for admission webhook
// server, and they will be saved in a secret.
func (am *admissionManager) generateCerts(create bool) (
	serverCertificate, serverPrivateKey, caCertificate []byte,
	err error) {
	var caPrivateKey []byte
	caPrivateKey, err = utils.SetUpCaKey()
	if err != nil {
		klog.Errorf("set up ca key failed %v", err)
		return nil, nil, nil, err
	}
	caCertificate, err = utils.SetUpCaCert(webhookconstants.ComponentName, caPrivateKey)
	if err != nil {
		klog.Errorf("set up ca cert failed %v", err)
		return nil, nil, nil, err
	}
	namespace := utils.GetCurrentNamespace()
	domains, ips := subjectAltNames(namespace, am.externalService)
	serverCertificate, serverPrivateKey, err = utils.SetUpSignedCertAndKey(domains, ips,
		webhookconstants.ComponentName,
		caPrivateKey, caCertificate, []x509.ExtKeyUsage{x509.ExtKeyUsageServerAuth})
	if err != nil {
		klog.Errorf("set up server cert error %v", err)
		return nil, nil, nil, err
	}
	if create {
		// try to create a new secret to save certificate and privateKey and ca certificate.
		_, err = am.kubeClient.CoreV1().Secrets(namespace).Create(context.Background(),
			&corev1.Secret{
				ObjectMeta: metav1.ObjectMeta{
					Name:      certsSecretName,
					Namespace: namespace,
				},
				Data: map[string][]byte{
					serverCert: serverCertificate,
					serverKey:  serverPrivateKey,
					caCert:     caCertificate,
				},
			}, metav1.CreateOptions{})
		if err != nil {
			klog.Errorf("create new certificate secret error %v", err)
			return nil, nil, nil, err
		}
	} else {
		// try to update an old secret to save certificate and privateKey and ca certificate.
		if err = utils.UpdateSecret(am.kubeClient, namespace, certsSecretName,
			func(secret *corev1.Secret) {
				secret.Data = map[string][]byte{
					serverCert: serverCertificate,
					serverKey:  serverPrivateKey,
					caCert:     caCertificate,
				}
			}); err != nil {
			return nil, nil, nil, err
		}
	}
	return caCertificate, serverCertificate, serverPrivateKey, nil
}

// loadCertsFromSecret loads certificate and privateKey and ca certificate from the secret.
func (am *admissionManager) loadCertsFromSecret() {
	namespace := utils.GetCurrentNamespace()
	create := false
	secret, err := am.kubeClient.CoreV1().Secrets(namespace).Get(context.Background(),
		certsSecretName, metav1.GetOptions{})
	if err != nil {
		if !errors.IsNotFound(err) {
			klog.Fatalf("get secret of %v/%v failed: %v", namespace, certsSecretName, err)
		}
		create = true
	}
	var serverCertBody, serverKeyBody, caCertBody []byte
	if secret == nil || secret.Data == nil || len(secret.Data[serverCert]) == 0 || len(secret.Data[serverKey]) == 0 ||
		len(secret.Data[caCert]) == 0 {
		caCertBody, serverCertBody, serverKeyBody, err = am.generateCerts(create)
		if err != nil {
			klog.Fatalf("generate certs failed: %v", err)
		}
	} else {
		caCertBody = secret.Data[caCert]
		serverCertBody = secret.Data[serverCert]
		serverKeyBody = secret.Data[serverKey]
	}
	am.caCertBody = caCertBody
	cert, err := tls.X509KeyPair(serverCertBody, serverKeyBody)
	if err != nil {
		klog.Fatalf("generate key pair error :%v", err)
	}
	am.tlsConfig = &tls.Config{
		Certificates: []tls.Certificate{cert},
	}
}

// subjectAltNames builds subject alt names by namespace and service name.
func subjectAltNames(namespace, svcName string) ([]string, []net.IP) {
	return []string{
		"localhost",
		svcName,
		fmt.Sprintf("%v.%v.svc", svcName, namespace),
		fmt.Sprintf("%v.%v.svc.cluster.local", svcName, namespace),
	}, []net.IP{net.ParseIP("127.0.0.1")}
}
