package deviceshifuhttp

import (
	"context"

	"io"
	"net/http"
	"net/http/httptest"
	"os"
	"reflect"
	"sort"
	"strings"
	"testing"

	"github.com/edgenesis/shifu/pkg/deviceshifu/deviceshifubase"
	"github.com/edgenesis/shifu/pkg/deviceshifu/unitest"
	"github.com/edgenesis/shifu/pkg/k8s/api/v1alpha1"
	"github.com/edgenesis/shifu/pkg/logger"
	"github.com/stretchr/testify/assert"

	v1 "k8s.io/api/apps/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/util/wait"
	"k8s.io/client-go/kubernetes/scheme"
	"k8s.io/client-go/rest"
)

func TestMain(m *testing.M) {
	err := GenerateConfigMapFromSnippet(MockDeviceCmStr, MockDeviceConfigFolder)
	if err != nil {
		logger.Errorf("error when generateConfigmapFromSnippet, err: %v", err)
		os.Exit(-1)
	}
	m.Run()
	err = os.RemoveAll(MockDeviceConfigPath)
	if err != nil {
		logger.Fatal(err)
	}

	err = GenerateConfigMapFromSnippet(MockDeviceCmStr2, MockDeviceConfigFolder)
	if err != nil {
		logger.Errorf("error when generateConfigmapFromSnippet2, err: %v", err)
		os.Exit(-1)
	}
	m.Run()
	err = os.RemoveAll(MockDeviceConfigPath)
	if err != nil {
		logger.Fatal(err)
	}
}

func TestDeviceShifuEmptyNamespace(t *testing.T) {
	deviceShifuMetadata := &deviceshifubase.DeviceShifuMetaData{
		Name:           "TestDeviceShifuEmptyNamespace",
		ConfigFilePath: "etc/edgedevice/config",
		KubeConfigPath: deviceshifubase.DeviceKubeconfigDoNotLoadStr,
	}

	_, err := New(deviceShifuMetadata)
	if err != nil {
		logger.Errorf("%v", err)
	} else {
		t.Errorf("DeviceShifuHTTP Test with empty namespace failed")
	}
}

func TestStart(t *testing.T) {
	deviceShifuMetadata := &deviceshifubase.DeviceShifuMetaData{
		Name:           "TestStart",
		ConfigFilePath: "etc/edgedevice/config",
		KubeConfigPath: deviceshifubase.DeviceKubeconfigDoNotLoadStr,
		Namespace:      "TestStartNamespace",
	}

	mockds, err := New(deviceShifuMetadata)
	if err != nil {
		t.Errorf("Failed creating new deviceshifu")
	}

	if err := mockds.Start(wait.NeverStop); err != nil {
		t.Errorf("DeviceShifuHTTP.Start failed due to: %v", err.Error())
	}

	if err := mockds.Stop(); err != nil {
		t.Errorf("unable to stop mock deviceShifu, error: %+v", err)
	}
}

func TestDeviceHealthHandler(t *testing.T) {
	deviceShifuMetadata := &deviceshifubase.DeviceShifuMetaData{
		Name:           "TeststartHTTPServer",
		ConfigFilePath: "etc/edgedevice/config",
		KubeConfigPath: deviceshifubase.DeviceKubeconfigDoNotLoadStr,
		Namespace:      "TeststartHTTPServerNamespace",
	}

	mockds, err := New(deviceShifuMetadata)
	if err != nil {
		t.Errorf("Failed creating new deviceshifu")
	}

	if err := mockds.Start(wait.NeverStop); err != nil {
		t.Errorf("DeviceShifu.Start failed due to: %v", err.Error())
	}

	resp, err := unitest.RetryAndGetHTTP("http://localhost:8080/health", 3)
	if err != nil {
		t.Errorf("HTTP GET returns an error %v", err.Error())
	}

	defer resp.Body.Close()
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		t.Errorf("unable to read response body, error: %v", err.Error())
	}

	if string(body) != deviceshifubase.DeviceIsHealthyStr {
		t.Errorf("%+v", body)
	}

	if err := mockds.Stop(); err != nil {
		t.Errorf("unable to stop mock deviceShifu, error: %+v", err)
	}
}

func TestCreateHTTPCommandlineRequestString(t *testing.T) {
	req, err := http.NewRequest("GET", "http://localhost:8081/start?time=10:00:00&flags_no_parameter=-a,-c,--no-dependency&target=machine2", nil)
	logger.Infof("%v", req.URL.Query())
	createdRequestString := createHTTPCommandlineRequestString(req, "/usr/local/bin/python /usr/src/driver/python-car-driver.py", "start")
	if err != nil {
		t.Errorf("Cannot create HTTP commandline request: %v", err.Error())
	}

	createdRequestArguments := strings.Fields(createdRequestString)

	expectedRequestString := "/usr/local/bin/python /usr/src/driver/python-car-driver.py start time=10:00:00 target=machine2 -a -c --no-dependency"
	expectedRequestArguments := strings.Fields(expectedRequestString)

	sort.Strings(createdRequestArguments)
	sort.Strings(expectedRequestArguments)

	if !reflect.DeepEqual(createdRequestArguments, expectedRequestArguments) {
		t.Errorf("created request: '%v' does not match the expected req: '%v'", createdRequestString, expectedRequestString)
	}
}

func TestCreateHTTPCommandlineRequestString2(t *testing.T) {
	req, err := http.NewRequest("GET", "http://localhost:8081/issue_cmd?cmdTimeout=10&flags_no_parameter=ping,8.8.8.8,-t", nil)
	createdReq := createHTTPCommandlineRequestString(req, "poweshell.exe", deviceshifubase.DeviceDefaultCMDDoNotExec)
	if err != nil {
		t.Errorf("Cannot create HTTP commandline request: %v", err.Error())
	}
	expectedReq := "ping 8.8.8.8 -t"
	if createdReq != expectedReq {
		t.Errorf("created request: '%v' does not match the expected req: '%v'\n", createdReq, expectedReq)
	}
}

func TestCreatehttpURIString(t *testing.T) {
	expectedURIString := "http://localhost:8081/start?time=10:00:00&target=machine1&target=machine2"
	req, err := http.NewRequest("POST", expectedURIString, nil)
	if err != nil {
		t.Errorf("Cannot create HTTP commandline request: %v", err.Error())
	}

	logger.Infof("%v", req.URL.Query())
	createdURIString := createURIFromRequest("localhost:8081", "start", req)

	createdURIStringWithoutQueries := strings.Split(createdURIString, "?")[0]
	createdQueries := strings.Split(strings.Split(createdURIString, "?")[1], "&")
	expectedURIStringWithoutQueries := strings.Split(expectedURIString, "?")[0]
	expectedQueries := strings.Split(strings.Split(expectedURIString, "?")[1], "&")

	sort.Strings(createdQueries)
	sort.Strings(expectedQueries)
	if createdURIStringWithoutQueries != expectedURIStringWithoutQueries || !reflect.DeepEqual(createdQueries, expectedQueries) {
		t.Errorf("createdQuery '%v' is different from the expectedQuery '%v'", createdURIString, expectedURIString)
	}
}

func TestCreatehttpURIStringNoQuery(t *testing.T) {
	expectedURIString := "http://localhost:8081/start"
	req, err := http.NewRequest("POST", expectedURIString, nil)
	if err != nil {
		t.Errorf("Cannot create HTTP commandline request: %v", err.Error())
	}

	logger.Infof("%v", req.URL.Query())
	createdURIString := createURIFromRequest("localhost:8081", "start", req)

	createdURIStringWithoutQueries := strings.Split(createdURIString, "?")[0]
	expectedURIStringWithoutQueries := strings.Split(expectedURIString, "?")[0]

	if createdURIStringWithoutQueries != expectedURIStringWithoutQueries {
		t.Errorf("createdQuery '%v' is different from the expectedQuery '%v'", createdURIString, expectedURIString)
	}
}

// TODO will remove this after the test method didn't depends on the device initialization
// This function is for one method debug using to initialize new device and get some local var have values
// func initTestEnv() {
// 	err := GenerateConfigMapFromSnippet(MockDeviceCmStr, MockDeviceConfigFolder)
// 	if err != nil {
// 		logger.Errorf("error when generateConfigmapFromSnippet, err: %v", err)
// 		os.Exit(-1)
// 	}
// 	deviceShifuMetadata := &deviceshifubase.DeviceShifuMetaData{
// 		Name:           "TestStart",
// 		ConfigFilePath: "etc/edgedevice/config",
// 		KubeConfigPath: deviceshifubase.DeviceKubeconfigDoNotLoadStr,
// 		Namespace:      "TestStartNamespace",
// 	}

// 	New(deviceShifuMetadata)
// }

func TestCommandHandleHTTPFunc(t *testing.T) {
	hs := mockHandlerServer(t)
	defer hs.Close()
	addr := strings.Split(hs.URL, "//")[1]

	hc := mockRestClient(addr, "")
	mockHandlerHTTP := &DeviceCommandHandlerHTTP{
		client: hc,
		HandlerMetaData: &HandlerMetaData{
			edgeDeviceSpec: v1alpha1.EdgeDeviceSpec{
				Address: &addr,
			},
			instruction: "test_instruction",
			properties:  mockDeviceShifuInstruction(),
		},
	}

	ds := mockDeviceServer(mockHandlerHTTP, t)
	defer ds.Close()
	dc := mockRestClient(ds.URL, "testing")

	// start device client testing
	r := dc.Get().Param("timeout", "1").Do(context.TODO())
	assert.Nil(t, r.Error())

	// test invalid timeout case
	r = dc.Get().Param("timeout", "aa").Do(context.TODO())
	assert.Equal(t, "the server rejected our request for an unknown reason", r.Error().Error())

	// test no timeout case
	r = dc.Get().Do(context.TODO())
	assert.Nil(t, r.Error())

	// test post method
	r = dc.Post().Do(context.TODO())
	assert.Nil(t, r.Error())

	// test not supported http method case
	r = dc.Delete().Do(context.TODO())
	assert.Equal(t, "the server rejected our request for an unknown reason", r.Error().Error())
}

func TestCommandHandleFuncHTTPCommandLine(t *testing.T) {
	hs := mockHandlerServer(t)
	defer hs.Close()
	addr := strings.Split(hs.URL, "//")[1]

	hc := mockRestClient(addr, "")
	mockHandlerHTTPCli := &DeviceCommandHandlerHTTPCommandline{
		client: hc,
		CommandlineHandlerMetadata: &CommandlineHandlerMetadata{
			edgeDeviceSpec: v1alpha1.EdgeDeviceSpec{
				Address: &addr,
			},
			instruction: "test_instruction",
			properties:  mockDeviceShifuInstruction(),
		},
	}

	ds := mockDeviceServer(mockHandlerHTTPCli, t)
	defer ds.Close()
	dc := mockRestClient(ds.URL, "testing")

	// start device client testing
	r := dc.Post().Param("timeout", "1").Param("stub_toleration", "1").Do(context.TODO())
	assert.Nil(t, r.Error())

	r = dc.Post().Param("timeout", "aa").Param("stub_toleration", "1").Do(context.TODO())
	assert.Equal(t, "the server rejected our request for an unknown reason", r.Error().Error())

	r = dc.Post().Param("timeout", "-1").Param("stub_toleration", "aa").Do(context.TODO())
	assert.Equal(t, "the server rejected our request for an unknown reason", r.Error().Error())

}

func mockRestClient(host string, path string) *rest.RESTClient {
	c, err := rest.RESTClientFor(
		&rest.Config{
			Host:    host,
			APIPath: path,
			ContentConfig: rest.ContentConfig{
				GroupVersion:         &v1.SchemeGroupVersion,
				NegotiatedSerializer: scheme.Codecs.WithoutConversion(),
			},
		},
	)
	if err != nil {
		logger.Errorf("mock client for host %s, apipath: %s failed,", host, path)
		return nil
	}

	return c
}

type MockCommandHandler interface {
	commandHandleFunc() http.HandlerFunc
}

func mockDeviceServer(h MockCommandHandler, t *testing.T) *httptest.Server {
	// catch device http request and response properly with specific paths
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		path := r.URL.Path
		switch path {
		case "/testing/apps/v1":
			logger.Info("ds get testing call, calling the handler server")
			assert.Equal(t, "/testing/apps/v1", path)
			f := h.commandHandleFunc()
			f.ServeHTTP(w, r)
		default:
			w.Header().Add("Content-Type", "application/json")
			w.WriteHeader(http.StatusOK)
			logger.Info("ds default request, path:", path)
		}
	}))
	return server
}

func mockHandlerServer(t *testing.T) *httptest.Server {
	// catch handler http request and response properly with specific paths
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		path := r.URL.Path
		switch path {
		case "/test_instruction":
			w.Header().Add("Content-Type", "application/json")
			w.WriteHeader(http.StatusOK)
			assert.Equal(t, "/test_instruction", path)
			logger.Info("handler get the instruction and executed.")
		default:
			w.WriteHeader(http.StatusOK)
			logger.Info("hs get default request, path:", path)
		}

	}))
	return server
}

func mockDeviceShifuInstruction() *deviceshifubase.DeviceShifuInstruction {
	return &deviceshifubase.DeviceShifuInstruction{
		DeviceShifuInstructionProperties: []deviceshifubase.DeviceShifuInstructionProperty{
			{
				ValueType:    "testing",
				ReadWrite:    "rw",
				DefaultValue: "0",
			},
		},
		DeviceShifuProtocolProperties: map[string]string{
			"test_key": "test_value",
		},
	}
}

func TestCollectHTTPTelemtries(t *testing.T) {
	ts := mockTelemetryServer(t)
	addr := strings.Split(ts.URL, "//")[1]
	_, err := unitest.RetryAndGetHTTP(ts.URL, 10)
	assert.Nil(t, err)
	mockDevice := &DeviceShifuHTTP{
		base: &deviceshifubase.DeviceShifuBase{
			Name: "test",
			EdgeDevice: &v1alpha1.EdgeDevice{
				ObjectMeta: metav1.ObjectMeta{
					Namespace: "test_namespace",
				},
				Spec: v1alpha1.EdgeDeviceSpec{
					Address:  &addr,
					Protocol: (*v1alpha1.Protocol)(unitest.ToPointer(string(v1alpha1.ProtocolHTTP))),
				},
			},
			DeviceShifuConfig: &deviceshifubase.DeviceShifuConfig{
				Telemetries: &deviceshifubase.DeviceShifuTelemetries{
					DeviceShifuTelemetrySettings: &deviceshifubase.DeviceShifuTelemetrySettings{
						DeviceShifuTelemetryTimeoutInMilliseconds:    unitest.ToPointer(int64(10)),
						DeviceShifuTelemetryDefaultPushToServer:      unitest.ToPointer(true),
						DeviceShifuTelemetryDefaultCollectionService: unitest.ToPointer("test_endpoint-1"),
					},
					DeviceShifuTelemetries: map[string]*deviceshifubase.DeviceShifuTelemetry{
						"device_healthy": {
							DeviceShifuTelemetryProperties: deviceshifubase.DeviceShifuTelemetryProperties{
								DeviceInstructionName: unitest.ToPointer("telemetry_health"),
								PushSettings: &deviceshifubase.DeviceShifuTelemetryPushSettings{
									DeviceShifuTelemetryPushToServer:      unitest.ToPointer(false),
									DeviceShifuTelemetryCollectionService: unitest.ToPointer("test_endpoint-1"),
								},
								InitialDelayMs: unitest.ToPointer(int64(1)),
							},
						},
					},
				},
			},
			RestClient: mockRestClient(addr, ""),
		},
	}

	res, err := mockDevice.collectHTTPTelemtries()
	assert.Equal(t, true, res)
	assert.Nil(t, err)

}

func mockTelemetryServer(t *testing.T) *httptest.Server {
	// catch handler http request and response properly with specific paths
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		path := r.URL.Path
		switch path {
		case "/telemetry_health":
			logger.Info("telemetry detected.")
			assert.Equal(t, "/telemetry_health", path)
			w.Header().Add("Content-Type", "application/json")
			w.WriteHeader(http.StatusOK)
		default:
			w.WriteHeader(http.StatusOK)
			logger.Info("ts get default request, path:", path)
		}

	}))
	return server
}
