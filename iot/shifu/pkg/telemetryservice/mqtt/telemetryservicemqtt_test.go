package mqtt

import (
	"bytes"
	"encoding/json"
	"io"
	"net/http"
	"net/http/httptest"
	"os"
	"sync"
	"testing"
	"time"

	mqtt "github.com/eclipse/paho.mqtt.golang"
	"github.com/edgenesis/shifu/pkg/telemetryservice/utils"
	v1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	testclient "k8s.io/client-go/kubernetes/fake"

	"github.com/edgenesis/shifu/pkg/deviceshifu/unitest"
	"github.com/edgenesis/shifu/pkg/k8s/api/v1alpha1"
	"github.com/edgenesis/shifu/pkg/logger"

	"github.com/mochi-co/mqtt/server"
	"github.com/mochi-co/mqtt/server/listeners"
	"github.com/stretchr/testify/assert"
)

const (
	unitTestServerAddress = "localhost:18928"
)

func TestMain(m *testing.M) {
	stop := make(chan struct{}, 1)
	wg := sync.WaitGroup{}
	os.Setenv("SERVER_LISTEN_PORT", ":18926")
	wg.Add(1)
	go func() {
		mockMQTTServer(stop)
		logger.Infof("Server Closed")
		wg.Done()
	}()
	m.Run()
	stop <- struct{}{}
	wg.Wait()
	os.Unsetenv("SERVER_LISTEN_PORT")
}

func TestConnectToMQTT(t *testing.T) {
	testCases := []struct {
		name        string
		setting     *v1alpha1.MQTTSetting
		isConnected bool
		expectedErr string
	}{
		{
			name: "case1 wrong address",
			setting: &v1alpha1.MQTTSetting{
				MQTTTopic:         unitest.ToPointer("/default/topic"),
				MQTTServerAddress: unitest.ToPointer("wrong address"),
			},
			isConnected: false,
			expectedErr: "no servers defined to connect to",
		},
		{
			name: "case2 pass",
			setting: &v1alpha1.MQTTSetting{
				MQTTTopic:         unitest.ToPointer("/default/topic"),
				MQTTServerAddress: unitest.ToPointer(unitTestServerAddress),
			},
			isConnected: true,
			expectedErr: "",
		},
	}

	for _, c := range testCases {
		t.Run(c.name, func(t *testing.T) {
			var client *mqtt.Client
			var err error
			connectAttempts := 3
			for i := 0; i < connectAttempts; i++{
				client, err = connectToMQTT(c.setting)
				if err == nil {
					break
				}
				time.Sleep(100 * time.Millisecond)
			}
			if err != nil {
				assert.Equal(t, c.expectedErr, err.Error())
				return
			}
			connected := (*client).IsConnected()
			assert.Equal(t, c.isConnected, connected)
		})
	}
}

func TestMessagePubHandler(t *testing.T) {
	messagePubHandler(nil, nil)
}

func TestConnectHandler(t *testing.T) {
	connectHandler(nil)
}

func TestConnectLostHandler(t *testing.T) {
	connectLostHandler(nil, nil)
}

func mockMQTTServer(stop <-chan struct{}) {
	tcp := listeners.NewTCP("t1", unitTestServerAddress)
	server := server.NewServer(nil)
	err := server.AddListener(tcp, nil)
	if err != nil {
		logger.Fatalf("Error when Listen at %v, error: %v", unitTestServerAddress, err)
	}
	go func() {
		<-stop
		server.Close()
	}()

	err = server.Serve()
	if err != nil {
		logger.Fatalf("Error when MQTT Server Serve, error: %v", err)
	}
}

func TestBindMQTTServicehandler(t *testing.T) {
	testCases := []struct {
		desc        string
		requestBody *v1alpha1.TelemetryRequest
		expectResp  string
	}{
		{
			desc:       "testCase1 RequestBody is not a JSON",
			expectResp: "unexpected end of JSON input\n",
		},
		{
			desc:       "testCase2 wrong address",
			expectResp: "Error to connect to server\n",
			requestBody: &v1alpha1.TelemetryRequest{
				MQTTSetting: &v1alpha1.MQTTSetting{
					MQTTServerAddress: unitest.ToPointer("wrong address"),
				},
			},
		},
		{
			desc:       "testCase3 pass",
			expectResp: "",
			requestBody: &v1alpha1.TelemetryRequest{
				MQTTSetting: &v1alpha1.MQTTSetting{
					MQTTTopic:         unitest.ToPointer("/test/test"),
					MQTTServerAddress: unitest.ToPointer(unitTestServerAddress)},
				RawData: []byte("123"),
			},
		},
	}
	for _, tC := range testCases {
		t.Run(tC.desc, func(t *testing.T) {
			req := httptest.NewRequest(http.MethodPost, "test:80", bytes.NewBuffer([]byte("")))
			if tC.requestBody != nil {
				requestBody, err := json.Marshal(tC.requestBody)
				assert.Nil(t, err)
				req = httptest.NewRequest(http.MethodPost, "test:80", bytes.NewBuffer(requestBody))
			}

			rr := httptest.NewRecorder()
			BindMQTTServicehandler(rr, req)
			body, err := io.ReadAll(rr.Result().Body)
			assert.Nil(t, err)
			assert.Equal(t, tC.expectResp, string(body))

		})
	}

}

func TestInjectSecret(t *testing.T) {
	testNamespace := "test-namespace"
	testCases := []struct {
		name         string
		client       *testclient.Clientset
		ns           string
		setting      *v1alpha1.MQTTSetting
		specPassword string
	}{
		{
			name:   "case0 no secrets found",
			client: testclient.NewSimpleClientset(),
			ns:     testNamespace,
			setting: &v1alpha1.MQTTSetting{
				MQTTServerSecret: unitest.ToPointer("test-secret"),
			},
			specPassword: "test-secret",
		},
		{
			name: "case2 have HTTP password secret",
			client: testclient.NewSimpleClientset(&v1.Secret{
				ObjectMeta: metav1.ObjectMeta{
					Name:      "test-secret",
					Namespace: testNamespace,
				},
				Data: map[string][]byte{
					"password": []byte("overwrite"),
				},
			}),
			ns: testNamespace,
			setting: &v1alpha1.MQTTSetting{
				MQTTServerSecret: unitest.ToPointer("test-secret"),
			},
			specPassword: "overwrite",
		},
	}

	for _, c := range testCases {
		utils.SetClient(c.client, c.ns)
		injectSecret(c.setting)
		assert.Equal(t, c.specPassword, *c.setting.MQTTServerSecret)
	}
}
