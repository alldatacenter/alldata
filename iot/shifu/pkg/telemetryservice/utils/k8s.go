package utils

import (
	"context"
	"github.com/edgenesis/shifu/pkg/logger"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/kubernetes"
	testclient "k8s.io/client-go/kubernetes/fake"
	"k8s.io/client-go/rest"
	"os"
)

var clientSet kubernetes.Interface
var ns string

func initClient() error {
	config, err := rest.InClusterConfig()
	if err != nil {
		return err
	}
	clientSet, err = kubernetes.NewForConfig(config)
	if err != nil {
		return err
	}
	ns = os.Getenv("EDGEDEVICE_NAMESPACE")
	return nil
}

func GetSecret(name string) (map[string]string, error) {
	if clientSet == nil {
		err := initClient()
		if err != nil {
			logger.Errorf("Can't init k8s client: %v", err)
			return nil, err
		}
	}
	secret, err := clientSet.CoreV1().Secrets(ns).Get(context.TODO(), name, metav1.GetOptions{})
	if err != nil {
		return nil, err
	}
	res := make(map[string]string)
	for k, v := range secret.Data {
		res[k] = string(v)
	}
	return res, nil
}

// SetClient for test
func SetClient(cs *testclient.Clientset, namespace string) {
	clientSet = cs
	ns = namespace
}
