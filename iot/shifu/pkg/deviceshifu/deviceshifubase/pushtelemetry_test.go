package deviceshifubase

import (
	"bytes"
	"github.com/edgenesis/shifu/pkg/deviceshifu/unitest"
	"github.com/edgenesis/shifu/pkg/deviceshifu/utils"
	"github.com/edgenesis/shifu/pkg/k8s/api/v1alpha1"
	"github.com/stretchr/testify/assert"
	"io"
	v1 "k8s.io/api/apps/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/kubernetes/scheme"
	"k8s.io/client-go/rest"
	"net/http"
	"net/http/httptest"
	"reflect"
	"strings"
	"testing"

	utiltesting "k8s.io/client-go/util/testing"
)

func mockTestServer(response string, statusCode int, t *testing.T) *httptest.Server {
	fakeHandler := utiltesting.FakeHandler{
		StatusCode:   statusCode,
		ResponseBody: string(response),
		T:            t,
	}
	testServer := httptest.NewServer(&fakeHandler)
	return testServer
}

func mockRestClientFor(resp string, t *testing.T) *rest.RESTClient {
	testServer := mockTestServer(resp, 200, t)
	c, _ := rest.RESTClientFor(&rest.Config{
		Host: testServer.URL,
		ContentConfig: rest.ContentConfig{
			GroupVersion:         &v1.SchemeGroupVersion,
			NegotiatedSerializer: scheme.Codecs.WithoutConversion(),
		},
		Username: "user",
		Password: "pass",
	})

	return c
}

func TestGetTelemetryCollectionServiceMap(t *testing.T) {
	//test cases
	// case 1 no setting
	// case 2 has default with false
	// case 3 has default with true and empty endpoint
	// case 4 has default with true and valid endpoint
	// case 5 has default with true and valid endpoint, has telemetry with empty push setting
	// case 6 has default with true and valid endpoint, has telemetry with no push setting
	// case 7 has default with true and valid endpoint, has telemetry with valid push setting
	// case 8 has default with true and valid endpoint, has telemetry with same endpoint
	// case 9 has default with true and valid endpoint, has telemetry with push false setting
	// case 10 has default without telemetry.
	testCases := []struct {
		Name        string
		inputDevice *DeviceShifuBase
		expectedMap map[string]v1alpha1.TelemetryServiceSpec
		expErrStr   string
	}{
		{
			Name: "case 1 no setting",
			inputDevice: &DeviceShifuBase{
				DeviceShifuConfig: &DeviceShifuConfig{
					Telemetries: &DeviceShifuTelemetries{},
				},
			},
			expectedMap: map[string]v1alpha1.TelemetryServiceSpec(map[string]v1alpha1.TelemetryServiceSpec{}),
			expErrStr:   "",
		},
		{
			Name: "case 2 has default with false",
			inputDevice: &DeviceShifuBase{
				DeviceShifuConfig: &DeviceShifuConfig{
					Telemetries: &DeviceShifuTelemetries{
						DeviceShifuTelemetrySettings: &DeviceShifuTelemetrySettings{
							DeviceShifuTelemetryDefaultPushToServer:      unitest.ToPointer(false),
							DeviceShifuTelemetryDefaultCollectionService: unitest.ToPointer(""),
						},
					},
				},
			},
			expectedMap: map[string]v1alpha1.TelemetryServiceSpec(map[string]v1alpha1.TelemetryServiceSpec{}),
			expErrStr:   "",
		},
		{
			Name: "case 3 has default with true and empty endpoint",
			inputDevice: &DeviceShifuBase{
				DeviceShifuConfig: &DeviceShifuConfig{
					Telemetries: &DeviceShifuTelemetries{
						DeviceShifuTelemetrySettings: &DeviceShifuTelemetrySettings{
							DeviceShifuTelemetryDefaultPushToServer:      unitest.ToPointer(true),
							DeviceShifuTelemetryDefaultCollectionService: unitest.ToPointer(""),
						},
					},
				},
			},
			expectedMap: nil,
			expErrStr:   "you need to configure defaultTelemetryCollectionService if setting defaultPushToServer to true",
		},
		{
			Name: "case 4 has default with true and valid endpoint",
			inputDevice: &DeviceShifuBase{
				Name: "test",
				EdgeDevice: &v1alpha1.EdgeDevice{
					ObjectMeta: metav1.ObjectMeta{
						Namespace: "test_namespace",
					},
				},
				DeviceShifuConfig: &DeviceShifuConfig{
					Telemetries: &DeviceShifuTelemetries{
						DeviceShifuTelemetrySettings: &DeviceShifuTelemetrySettings{
							DeviceShifuTelemetryDefaultPushToServer:      unitest.ToPointer(true),
							DeviceShifuTelemetryDefaultCollectionService: unitest.ToPointer("test_endpoint-1"),
						},
					},
				},
				RestClient: mockRestClientFor("{\"spec\": {\"telemetrySeriveEndpoint\": \"http://192.168.15.48:12345/endpoint1\",\"type\": \"HTTP\"}}", t),
			},
			expectedMap: map[string]v1alpha1.TelemetryServiceSpec(map[string]v1alpha1.TelemetryServiceSpec{}),
			expErrStr:   "",
		},
		{
			Name: "case 5 has default with true and valid endpoint, has telemetry with empty push setting",
			inputDevice: &DeviceShifuBase{
				Name: "test",
				EdgeDevice: &v1alpha1.EdgeDevice{
					ObjectMeta: metav1.ObjectMeta{
						Namespace: "test_namespace",
					},
				},
				DeviceShifuConfig: &DeviceShifuConfig{
					Telemetries: &DeviceShifuTelemetries{
						DeviceShifuTelemetrySettings: &DeviceShifuTelemetrySettings{
							DeviceShifuTelemetryDefaultPushToServer:      unitest.ToPointer(true),
							DeviceShifuTelemetryDefaultCollectionService: unitest.ToPointer("test_endpoint-1"),
						},
						DeviceShifuTelemetries: map[string]*DeviceShifuTelemetry{
							"device_healthy": {
								DeviceShifuTelemetryProperties: DeviceShifuTelemetryProperties{
									PushSettings: &DeviceShifuTelemetryPushSettings{
										DeviceShifuTelemetryPushToServer:      unitest.ToPointer(true),
										DeviceShifuTelemetryCollectionService: unitest.ToPointer(""),
									},
								},
							},
						},
					},
				},
				RestClient: mockRestClientFor("{\"spec\": {\"telemetrySeriveEndpoint\": \"http://192.168.15.48:12345/test_endpoint-1\",\"type\": \"HTTP\"}}", t),
			},
			expectedMap: map[string]v1alpha1.TelemetryServiceSpec(map[string]v1alpha1.TelemetryServiceSpec{
				"device_healthy": {
					TelemetrySeriveEndpoint: unitest.ToPointer(""),
				},
			}),
			expErrStr: "",
		},
		{
			Name: "case 6 has default with true and valid endpoint, has telemetry with no push setting",
			inputDevice: &DeviceShifuBase{
				Name: "test",
				EdgeDevice: &v1alpha1.EdgeDevice{
					ObjectMeta: metav1.ObjectMeta{
						Namespace: "test_namespace",
					},
				},
				DeviceShifuConfig: &DeviceShifuConfig{
					Telemetries: &DeviceShifuTelemetries{
						DeviceShifuTelemetrySettings: &DeviceShifuTelemetrySettings{
							DeviceShifuTelemetryDefaultPushToServer:      unitest.ToPointer(true),
							DeviceShifuTelemetryDefaultCollectionService: unitest.ToPointer("test_endpoint-1"),
						},
						DeviceShifuTelemetries: map[string]*DeviceShifuTelemetry{
							"device_healthy": {
								DeviceShifuTelemetryProperties: DeviceShifuTelemetryProperties{},
							},
						},
					},
				},
				RestClient: mockRestClientFor("{\"spec\": {\"telemetrySeriveEndpoint\": \"http://192.168.15.48:12345/test_endpoint-1\",\"type\": \"HTTP\"}}", t),
			},
			expectedMap: map[string]v1alpha1.TelemetryServiceSpec(map[string]v1alpha1.TelemetryServiceSpec{
				"device_healthy": {
					TelemetrySeriveEndpoint: unitest.ToPointer(""),
				},
			}),
			expErrStr: "",
		},
		{
			Name: "case 7 has default with true and valid endpoint, has telemetry with valid push setting",
			inputDevice: &DeviceShifuBase{
				Name: "test",
				EdgeDevice: &v1alpha1.EdgeDevice{
					ObjectMeta: metav1.ObjectMeta{
						Namespace: "test_namespace",
					},
				},
				DeviceShifuConfig: &DeviceShifuConfig{
					Telemetries: &DeviceShifuTelemetries{
						DeviceShifuTelemetrySettings: &DeviceShifuTelemetrySettings{
							DeviceShifuTelemetryDefaultPushToServer:      unitest.ToPointer(true),
							DeviceShifuTelemetryDefaultCollectionService: unitest.ToPointer("test_endpoint-1"),
						},
						DeviceShifuTelemetries: map[string]*DeviceShifuTelemetry{
							"device_healthy": {
								DeviceShifuTelemetryProperties: DeviceShifuTelemetryProperties{
									PushSettings: &DeviceShifuTelemetryPushSettings{
										DeviceShifuTelemetryPushToServer:      unitest.ToPointer(true),
										DeviceShifuTelemetryCollectionService: unitest.ToPointer("test-healthy-endpoint"),
									},
								},
							},
						},
					},
				},
				RestClient: mockRestClientFor("{\"spec\": {\"telemetrySeriveEndpoint\": \"http://192.168.15.48:12345/test-healthy-endpoint\",\"type\": \"HTTP\"}}", t),
			},
			expectedMap: map[string]v1alpha1.TelemetryServiceSpec(map[string]v1alpha1.TelemetryServiceSpec{
				"device_healthy": {
					TelemetrySeriveEndpoint: unitest.ToPointer("http://192.168.15.48:12345/test-healthy-endpoint"),
				},
			}),
			expErrStr: "",
		},
		{
			Name: "case 8 has default with true and valid endpoint, has telemetry with same endpoint",
			inputDevice: &DeviceShifuBase{
				Name: "test",
				EdgeDevice: &v1alpha1.EdgeDevice{
					ObjectMeta: metav1.ObjectMeta{
						Namespace: "test_namespace",
					},
				},
				DeviceShifuConfig: &DeviceShifuConfig{
					Telemetries: &DeviceShifuTelemetries{
						DeviceShifuTelemetrySettings: &DeviceShifuTelemetrySettings{
							DeviceShifuTelemetryDefaultPushToServer:      unitest.ToPointer(true),
							DeviceShifuTelemetryDefaultCollectionService: unitest.ToPointer("test_endpoint-1"),
						},
						DeviceShifuTelemetries: map[string]*DeviceShifuTelemetry{
							"device_healthy": {
								DeviceShifuTelemetryProperties: DeviceShifuTelemetryProperties{
									PushSettings: &DeviceShifuTelemetryPushSettings{
										DeviceShifuTelemetryPushToServer:      unitest.ToPointer(true),
										DeviceShifuTelemetryCollectionService: unitest.ToPointer("test_endpoint-1"),
									},
								},
							},
						},
					},
				},
				RestClient: mockRestClientFor("{\"spec\": {\"telemetrySeriveEndpoint\": \"http://192.168.15.48:12345/test_endpoint-1\",\"type\": \"HTTP\"}}", t),
			},
			expectedMap: map[string]v1alpha1.TelemetryServiceSpec(map[string]v1alpha1.TelemetryServiceSpec{
				"device_healthy": {
					TelemetrySeriveEndpoint: unitest.ToPointer("http://192.168.15.48:12345/test_endpoint-1"),
				},
			}),
			expErrStr: "",
		},
		{
			Name: "case 9 has default with true and valid endpoint, has telemetry with push false setting",
			inputDevice: &DeviceShifuBase{
				Name: "test",
				EdgeDevice: &v1alpha1.EdgeDevice{
					ObjectMeta: metav1.ObjectMeta{
						Namespace: "test_namespace",
					},
				},
				DeviceShifuConfig: &DeviceShifuConfig{
					Telemetries: &DeviceShifuTelemetries{
						DeviceShifuTelemetrySettings: &DeviceShifuTelemetrySettings{
							DeviceShifuTelemetryDefaultPushToServer:      unitest.ToPointer(true),
							DeviceShifuTelemetryDefaultCollectionService: unitest.ToPointer("test_endpoint-1"),
						},
						DeviceShifuTelemetries: map[string]*DeviceShifuTelemetry{
							"device_healthy": {
								DeviceShifuTelemetryProperties: DeviceShifuTelemetryProperties{
									PushSettings: &DeviceShifuTelemetryPushSettings{
										DeviceShifuTelemetryPushToServer:      unitest.ToPointer(false),
										DeviceShifuTelemetryCollectionService: unitest.ToPointer("test_endpoint-1"),
									},
								},
							},
						},
					},
				},
				RestClient: mockRestClientFor("{\"spec\": {\"telemetrySeriveEndpoint\": \"http://192.168.15.48:12345/test_endpoint-1\",\"type\": \"HTTP\"}}", t),
			},
			expectedMap: map[string]v1alpha1.TelemetryServiceSpec(map[string]v1alpha1.TelemetryServiceSpec{}),
			expErrStr:   "",
		},
		{
			Name: "case 10 no telemetry",
			inputDevice: &DeviceShifuBase{
				DeviceShifuConfig: &DeviceShifuConfig{},
			},
			expectedMap: map[string]v1alpha1.TelemetryServiceSpec(map[string]v1alpha1.TelemetryServiceSpec{}),
			expErrStr:   "",
		},
	}

	for _, c := range testCases {
		t.Run(c.Name, func(t *testing.T) {
			result, err := getTelemetryCollectionServiceMap(c.inputDevice)
			ok := assert.ObjectsAreEqual(c.expectedMap, result)
			assert.Equal(t, true, ok)
			if len(c.expErrStr) == 0 {
				assert.Nil(t, err)
			} else {
				assert.Equal(t, err.Error(), c.expErrStr)
			}
		})
	}

}

func TestCopyHeader(t *testing.T) {
	src := http.Header{
		"Test": {"aa", "bb", "cc"},
	}
	dst := http.Header{}
	utils.CopyHeader(dst, src)

	if !reflect.DeepEqual(dst, src) {
		t.Errorf("Not match")
	}
}

func TestPushToHTTPTelemetryCollectionService(t *testing.T) {
	resp := &http.Response{
		Body: io.NopCloser(strings.NewReader("Hello,World")),
	}

	err := pushToHTTPTelemetryCollectionService(resp, "localhost")
	assert.NotNil(t, err)
}

func TestPushToShifuTelemetryCollectionService(t *testing.T) {
	server := mockMQTTTelemetryServiceServer(t)
	defer server.Close()
	mockServerAddress := server.URL
	testCases := []struct {
		name        string
		message     *http.Response
		request     *v1alpha1.TelemetryRequest
		address     string
		expectedErr string
	}{
		{
			name: "case1 Error address",
			message: &http.Response{
				Body: io.NopCloser(bytes.NewBufferString("TestBody")),
			},
			request: &v1alpha1.TelemetryRequest{
				MQTTSetting: &v1alpha1.MQTTSetting{
					MQTTTopic: unitest.ToPointer("/test/topic"),
				},
			},
			address:     "test",
			expectedErr: "Post \"test\": unsupported protocol scheme \"\"",
		}, {
			name: "case2 pass",
			message: &http.Response{
				Body: io.NopCloser(bytes.NewBufferString("TestBody")),
			},
			request: &v1alpha1.TelemetryRequest{
				MQTTSetting: &v1alpha1.MQTTSetting{
					MQTTTopic: unitest.ToPointer("/test/topic"),
				},
			},
			address:     mockServerAddress,
			expectedErr: "",
		},
	}
	for _, c := range testCases {
		err := pushToShifuTelemetryCollectionService(c.message, c.request, c.address)
		if err != nil {
			assert.Equal(t, err.Error(), c.expectedErr)
		} else {
			assert.Equal(t, c.expectedErr, "")
		}
	}
}

func mockMQTTTelemetryServiceServer(t *testing.T) *httptest.Server {
	handler := http.NewServeMux()
	handler.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
	})
	return httptest.NewServer(handler)
}

func TestPushTelemetryCollectionService(t *testing.T) {
	server := mockMQTTTelemetryServiceServer(t)
	defer server.Close()
	address := server.URL

	testCases := []struct {
		name        string
		spec        *v1alpha1.TelemetryServiceSpec
		message     *http.Response
		expectedErr string
	}{
		{
			name: "case0 empty Settings",
			spec: &v1alpha1.TelemetryServiceSpec{
				TelemetrySeriveEndpoint: unitest.ToPointer(address),
			},
			message: &http.Response{
				Body: io.NopCloser(bytes.NewBufferString("test")),
			},
			expectedErr: "empty telemetryServiceSpec",
		}, {
			name: "case1 http",
			spec: &v1alpha1.TelemetryServiceSpec{
				TelemetrySeriveEndpoint: unitest.ToPointer(address),
				ServiceSettings: &v1alpha1.ServiceSettings{
					HTTPSetting: &v1alpha1.HTTPSetting{},
				},
			},
			message: &http.Response{
				Body: io.NopCloser(bytes.NewBufferString("test")),
			},
		}, {
			name: "case2 MQTT",
			spec: &v1alpha1.TelemetryServiceSpec{
				ServiceSettings: &v1alpha1.ServiceSettings{
					MQTTSetting: &v1alpha1.MQTTSetting{
						MQTTTopic: unitest.ToPointer("/test/topic"),
					},
				},
				TelemetrySeriveEndpoint: unitest.ToPointer(address),
			},
			message: &http.Response{
				Body: io.NopCloser(bytes.NewBufferString("test")),
			},
			expectedErr: "",
		}, {
			name: "case3 SQL",
			spec: &v1alpha1.TelemetryServiceSpec{
				ServiceSettings: &v1alpha1.ServiceSettings{
					SQLSetting: &v1alpha1.SQLConnectionSetting{},
				},
				TelemetrySeriveEndpoint: unitest.ToPointer(address),
			},
			message: &http.Response{
				Body: io.NopCloser(bytes.NewBufferString("test")),
			},
			expectedErr: "",
		}, {
			name: "case4 MinIO",
			spec: &v1alpha1.TelemetryServiceSpec{
				ServiceSettings: &v1alpha1.ServiceSettings{
					MinIOSetting: &v1alpha1.MinIOSetting{},
				},
				TelemetrySeriveEndpoint: unitest.ToPointer(address),
			},
			message: &http.Response{
				Body: io.NopCloser(bytes.NewBufferString("test")),
			},
			expectedErr: "",
		}, {
			name: "case5 OtherProtocol",
			spec: &v1alpha1.TelemetryServiceSpec{
				ServiceSettings: &v1alpha1.ServiceSettings{
					MQTTSetting: &v1alpha1.MQTTSetting{
						MQTTTopic: unitest.ToPointer("/test/topic"),
					},
				},
				TelemetrySeriveEndpoint: unitest.ToPointer(address),
			},
			message: &http.Response{
				Body: io.NopCloser(bytes.NewBufferString("test")),
			},
			expectedErr: "unsupported protocol",
		},
	}

	for _, c := range testCases {
		err := PushTelemetryCollectionService(c.spec, c.message)
		if err != nil {
			assert.Equal(t, err.Error(), c.expectedErr)
		}
	}
}

func TestInjectSecret(t *testing.T) {
	testCases := []struct {
		name         string
		client       *rest.RESTClient
		ts           *v1alpha1.TelemetryService
		ns           string
		specUsername string
		specPassword string
	}{
		{
			name:   "case0 no secrets found",
			client: mockRestClientFor("", t),
			ts: &v1alpha1.TelemetryService{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-ts",
				},
				Spec: v1alpha1.TelemetryServiceSpec{
					ServiceSettings: &v1alpha1.ServiceSettings{
						HTTPSetting: &v1alpha1.HTTPSetting{
							Username: unitest.ToPointer("origin-username"),
							Password: unitest.ToPointer("origin-password"),
						},
					},
				},
			},
			ns:           "test",
			specUsername: "origin-username",
			specPassword: "origin-password",
		},
		{
			name: "case1 have HTTP username secret",
			client: mockRestClientFor(`{
  "data": {
    "username": "b3ZlcndyaXRl"
  },
  "kind": "Secret",
  "metadata": {
    "name": "taosdata",
    "namespace": "devices"
  },
  "type": "Opaque"
}`, t),
			ts: &v1alpha1.TelemetryService{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-ts",
				},
				Spec: v1alpha1.TelemetryServiceSpec{
					ServiceSettings: &v1alpha1.ServiceSettings{
						HTTPSetting: &v1alpha1.HTTPSetting{
							Username: unitest.ToPointer("origin-username"),
							Password: unitest.ToPointer("origin-password"),
						},
					},
				},
			},
			ns:           "test",
			specUsername: "overwrite",
			specPassword: "origin-password",
		},
		{
			name: "case2 have HTTP password secret",
			client: mockRestClientFor(`{
  "data": {
    "password": "b3ZlcndyaXRl"
  },
  "kind": "Secret",
  "metadata": {
    "name": "taosdata",
    "namespace": "devices"
  },
  "type": "Opaque"
}`, t),
			ts: &v1alpha1.TelemetryService{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-ts",
				},
				Spec: v1alpha1.TelemetryServiceSpec{
					ServiceSettings: &v1alpha1.ServiceSettings{
						HTTPSetting: &v1alpha1.HTTPSetting{
							Username: unitest.ToPointer("origin-username"),
							Password: unitest.ToPointer("origin-password"),
						},
					},
				},
			},
			ns:           "test",
			specUsername: "origin-username",
			specPassword: "overwrite",
		},
		{
			name: "case3 have HTTP both secrets",
			client: mockRestClientFor(`{
  "data": {
    "username": "b3ZlcndyaXRl",
    "password": "b3ZlcndyaXRl"
  },
  "kind": "Secret",
  "metadata": {
    "name": "taosdata",
    "namespace": "devices"
  },
  "type": "Opaque"
}`, t),
			ts: &v1alpha1.TelemetryService{
				ObjectMeta: metav1.ObjectMeta{
					Name: "test-ts",
				},
				Spec: v1alpha1.TelemetryServiceSpec{
					ServiceSettings: &v1alpha1.ServiceSettings{
						HTTPSetting: &v1alpha1.HTTPSetting{
							Username: unitest.ToPointer("origin-username"),
							Password: unitest.ToPointer("origin-password"),
						},
					},
				},
			},
			ns:           "test",
			specUsername: "overwrite",
			specPassword: "overwrite",
		},
	}

	for _, c := range testCases {
		injectSecret(c.client, c.ts, c.ns)
		assert.Equal(t, c.specUsername, *c.ts.Spec.ServiceSettings.HTTPSetting.Username)
		assert.Equal(t, c.specPassword, *c.ts.Spec.ServiceSettings.HTTPSetting.Password)
	}
}
