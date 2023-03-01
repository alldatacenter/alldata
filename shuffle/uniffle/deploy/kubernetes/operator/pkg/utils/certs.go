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

package utils

import (
	"crypto"
	"crypto/rand"
	"crypto/rsa"
	"crypto/x509"
	"crypto/x509/pkix"
	"encoding/pem"
	"errors"
	"fmt"
	"math"
	"math/big"
	"net"
	"strings"
	"time"

	"k8s.io/client-go/util/cert"
	"k8s.io/client-go/util/keyutil"
	"k8s.io/klog/v2"
)

const (
	certificateBlockType = "CERTIFICATE"
	rsaKeySize           = 2048
	duration365d         = time.Hour * 24 * 365 * 100
	blockPrivateType     = "RSA PRIVATE KEY"
	blockCertType        = "CERTIFICATE"
)

// newPrivateKey generates a private key.
func newPrivateKey() (*rsa.PrivateKey, error) {
	return rsa.GenerateKey(rand.Reader, rsaKeySize)
}

// SetUpCaKey sets up a new ca private key
func SetUpCaKey() ([]byte, error) {
	signingKey, err := newPrivateKey()
	if err != nil {
		return nil, fmt.Errorf("failed to create CA private key %v", err)
	}
	privateSigningKeyPEM, err := keyutil.MarshalPrivateKeyToPEM(signingKey)
	if err != nil {
		klog.Errorf("marshall private key to pem error:%v", err)
		return nil, err
	}
	return privateSigningKeyPEM, nil
}

// SetUpCaCert sets up a new ca certification.
func SetUpCaCert(commonName string, pemCAKey []byte) ([]byte, error) {
	caKey, err := decodePemToRSA(pemCAKey)
	if err != nil {
		klog.Errorf("decode pem to rsa private error %v", err)
		return nil, err
	}
	signingCert, err := cert.NewSelfSignedCACert(cert.Config{CommonName: commonName}, caKey)
	if err != nil {
		klog.Errorf("self sign error %v", err)
		return nil, fmt.Errorf("failed to create CA cert for apiserver %v", err)
	}
	return encodeCertPEM(signingCert), nil
}

// SetUpSignedCertAndKey uses the ca certificate and ca private key to generate certificates.
func SetUpSignedCertAndKey(domains []string, ips []net.IP, commonName string,
	pemCAKey, pemCACert []byte, usage []x509.ExtKeyUsage) (
	[]byte, []byte, error) {
	// decode ca private key in PEM format.
	caKey, err := decodePemToRSA(pemCAKey)
	if err != nil {
		klog.Errorf("decode pem to rsa private error %v", err)
		return nil, nil, err
	}
	// decode ca certificate in PEM format.
	caCert, err := decodePemToCert(pemCACert)
	if err != nil {
		klog.Errorf("decode pem to cert error %v", err)
		return nil, nil, err
	}
	// create a new private key.
	signedKey, err := newPrivateKey()
	if err != nil {
		return nil, nil, fmt.Errorf("failed to create private key %v", err)
	}
	// generate a new certificate by new private key, ca certificate and ca private key.
	signedCert, err := newSignedCert(
		&cert.Config{
			CommonName: commonName,
			AltNames:   cert.AltNames{DNSNames: domains, IPs: ips},
			Usages:     usage,
		},
		signedKey, caCert, caKey,
	)
	if err != nil {
		return nil, nil, fmt.Errorf("failed to create cert %v", err)
	}
	privateSigningKeyPEM, err := keyutil.MarshalPrivateKeyToPEM(signedKey)
	if err != nil {
		klog.Errorf("marshall private key to pem error:%v", err)
		return nil, nil, err
	}
	// return a new certificate and private key in PEM format.
	return encodeCertPEM(signedCert), privateSigningKeyPEM, nil
}

// SetupServerCert sets up the server certificate and private key.
func SetupServerCert(domain, commonName string) ([]byte, []byte, []byte, error) {
	signingKey, err := newPrivateKey()
	if err != nil {
		return nil, nil, nil, fmt.Errorf("failed to create CA private key %v", err)
	}
	signingCert, err := cert.NewSelfSignedCACert(cert.Config{CommonName: commonName}, signingKey)
	if err != nil {
		return nil, nil, nil, fmt.Errorf("failed to create CA cert for apiserver %v", err)
	}
	key, err := newPrivateKey()
	if err != nil {
		return nil, nil, nil, fmt.Errorf("failed to create private key for %v", err)
	}

	signedCert, err := newSignedCert(
		&cert.Config{
			CommonName: commonName,
			AltNames:   cert.AltNames{DNSNames: strings.Split(domain, ",")},
			Usages:     []x509.ExtKeyUsage{x509.ExtKeyUsageServerAuth},
		},
		key, signingCert, signingKey,
	)
	if err != nil {
		return nil, nil, nil, fmt.Errorf("failed to create cert %v", err)
	}
	privateKeyPEM, err := keyutil.MarshalPrivateKeyToPEM(key)
	if err != nil {
		return nil, nil, nil, fmt.Errorf("failed to marshal key %v", err)
	}
	return encodeCertPEM(signedCert), privateKeyPEM, encodeCertPEM(signingCert), nil
}

// newSignedCert generates s self-signed cert.
func newSignedCert(cfg *cert.Config, key crypto.Signer, caCert *x509.Certificate,
	caKey crypto.Signer) (*x509.Certificate, error) {
	serial, err := rand.Int(rand.Reader, new(big.Int).SetInt64(math.MaxInt64))
	if err != nil {
		return nil, err
	}
	if len(cfg.CommonName) == 0 {
		return nil, errors.New("must specify a CommonName")
	}
	if len(cfg.Usages) == 0 {
		return nil, errors.New("must specify at least one ExtKeyUsage")
	}

	certTmpl := x509.Certificate{
		Subject: pkix.Name{
			CommonName:   cfg.CommonName,
			Organization: cfg.Organization,
		},
		DNSNames:     cfg.AltNames.DNSNames,
		IPAddresses:  cfg.AltNames.IPs,
		SerialNumber: serial,
		NotBefore:    caCert.NotBefore,
		NotAfter:     time.Now().Add(duration365d).UTC(),
		KeyUsage:     x509.KeyUsageKeyEncipherment | x509.KeyUsageDigitalSignature,
		ExtKeyUsage:  cfg.Usages,
	}
	certDERBytes, err := x509.CreateCertificate(rand.Reader, &certTmpl, caCert, key.Public(), caKey)
	if err != nil {
		return nil, err
	}
	return x509.ParseCertificate(certDERBytes)
}

// encodeCertPEM encodes a certificate.
func encodeCertPEM(cert *x509.Certificate) []byte {
	block := pem.Block{
		Type:  certificateBlockType,
		Bytes: cert.Raw,
	}
	return pem.EncodeToMemory(&block)
}

// decodePemToRSA decodes pem key to RSA private key.
func decodePemToRSA(pemKey []byte) (*rsa.PrivateKey, error) {
	block, _ := pem.Decode(pemKey)
	if block == nil || block.Type != blockPrivateType {
		err := errors.New("block is nil or block type is wrong")
		klog.Errorf("decode pem key error %v", err)
		return nil, err
	}
	pri, err := x509.ParsePKCS1PrivateKey(block.Bytes)
	if err != nil {
		klog.Errorf("parse pem key error %v", err)
		return nil, err
	}
	return pri, nil
}

// decodePemToCert decodes pem cert to x509 certificate.
func decodePemToCert(pemCert []byte) (*x509.Certificate, error) {
	block, _ := pem.Decode(pemCert)
	if block == nil || block.Type != blockCertType {
		err := errors.New("block is nil or block type is wrong")
		klog.Errorf("decode pem cert error %v", err)
		return nil, err
	}
	certificate, err := x509.ParseCertificate(block.Bytes)
	if err != nil {
		klog.Errorf("parse pem cert error %v", err)
		return nil, err
	}
	return certificate, nil
}
