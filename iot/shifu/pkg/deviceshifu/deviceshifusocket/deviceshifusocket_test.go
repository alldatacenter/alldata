package deviceshifusocket

import (
	"context"
	"encoding/json"
	"io"
	"net"
	"net/http"
	"net/http/httptest"
	"os"
	"reflect"
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
		logger.Errorf("error when generateConfigmapFromSnippet,err: %v", err)
		os.Exit(-1)
	}

	listener, err := net.Listen("tcp", UnitTestAddress)
	if err != nil {
		logger.Fatalf("Cannot Listen at %v", UnitTestAddress)
	}

	go func() {
		_, _ = listener.Accept()
	}()
	defer listener.Close()
	m.Run()
	err = os.RemoveAll(MockDeviceConfigPath)
	if err != nil {
		logger.Fatal(err)
	}
}

func TestStart(t *testing.T) {
	deviceShifuMetadata := &deviceshifubase.DeviceShifuMetaData{
		Name:           "test_name",
		Namespace:      "test_namespace",
		ConfigFilePath: "etc/edgedevice/config",
		KubeConfigPath: MockConfigFile,
	}

	server := mockHttpServer(t)
	writeMockConfigFile(t, server.URL)

	defer server.Close()

	mockds, err := New(deviceShifuMetadata)
	if err != nil {
		t.Errorf("Failed creating new deviceshifu")
	}

	if err := mockds.Start(wait.NeverStop); err != nil {
		t.Errorf("DeviceShifu.Start failed due to: %v", err.Error())
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

func TestDecodeCommand(t *testing.T) {
	input := "1230000abc"
	var outputHex = []byte{18, 48, 0, 10, 188}

	output, err := decodeCommand(input, v1alpha1.HEX)
	if err != nil {
		t.Errorf("Error when decodeCommand on test1, error:%v", err)
	}
	if !reflect.DeepEqual(output, outputHex) {
		t.Errorf("not match with current output, output: %v", output)
	}

	output, err = decodeCommand(input, v1alpha1.UTF8)
	if err != nil {
		t.Errorf("Error when decodeCommand on test2, error: %v", err)
	}
	if input != string(output) {
		t.Errorf("not match with current output, output: %v", output)
	}
}

func TestEncodeMessage(t *testing.T) {
	var inputHex = []byte{18, 48, 0, 10, 188}
	var output = "1230000abc"

	output1, err := encodeMessage(inputHex, v1alpha1.HEX)
	if err != nil {
		t.Errorf("Error when decodeCommand on test1, error: %v", err)
	}
	if output1 != output {
		t.Errorf("not match with current output, output: %v", output)
	}

	var inputUtf8 = []byte{49, 50, 51, 48, 48, 48, 48, 97, 98, 99}
	output2, err := encodeMessage(inputUtf8, v1alpha1.UTF8)
	if err != nil {
		t.Errorf("Error when decodeCommand on test1, error: %v", err)
	}
	if output2 != output {
		t.Errorf("not match with current output, output: %v", output)
	}
}

func TestCollectSocketTelemetry(t *testing.T) {
	socketProtocol := v1alpha1.ProtocolSocket
	httpProtocol := v1alpha1.ProtocolHTTP
	address := UnitTestAddress
	emptyAddress := ""

	testCases := []struct {
		Name        string
		deviceShifu *DeviceShifu
		expected    bool
		expErrStr   string
	}{
		{
			Name: "case1 pass",
			deviceShifu: &DeviceShifu{
				base: &deviceshifubase.DeviceShifuBase{
					Name: "testDevice",
					EdgeDevice: &v1alpha1.EdgeDevice{
						Spec: v1alpha1.EdgeDeviceSpec{
							Protocol: &socketProtocol,
							Address:  &address,
						},
					},
				},
			},
			expected:  true,
			expErrStr: "",
		}, {
			Name: "case2 address is nil",
			deviceShifu: &DeviceShifu{
				base: &deviceshifubase.DeviceShifuBase{
					Name: "testDevice",
					EdgeDevice: &v1alpha1.EdgeDevice{
						Spec: v1alpha1.EdgeDeviceSpec{
							Protocol: &socketProtocol,
						},
					},
				},
			},
			expected:  false,
			expErrStr: "Device testDevice does not have an address",
		}, {
			Name: "case3 Protocol is not Socket",
			deviceShifu: &DeviceShifu{
				base: &deviceshifubase.DeviceShifuBase{
					Name: "testDevice",
					EdgeDevice: &v1alpha1.EdgeDevice{
						Spec: v1alpha1.EdgeDeviceSpec{
							Protocol: &httpProtocol,
							Address:  &address,
						},
					},
				},
			},
			expected:  false,
			expErrStr: "",
		}, {
			Name: "case4 wrong ip address",
			deviceShifu: &DeviceShifu{
				base: &deviceshifubase.DeviceShifuBase{
					Name: "testDevice",
					EdgeDevice: &v1alpha1.EdgeDevice{
						Spec: v1alpha1.EdgeDeviceSpec{
							Protocol: &socketProtocol,
							Address:  &emptyAddress,
						},
					},
				},
			},
			expected:  false,
			expErrStr: "dial tcp: missing address",
		}, {
			Name: "case5 empty protocol",
			deviceShifu: &DeviceShifu{
				base: &deviceshifubase.DeviceShifuBase{
					Name: "testDevice",
					EdgeDevice: &v1alpha1.EdgeDevice{
						Spec: v1alpha1.EdgeDeviceSpec{
							Address: &address,
						},
					},
				},
			},
			expected:  true,
			expErrStr: "",
		},
	}

	for _, c := range testCases {
		t.Run(c.Name, func(t *testing.T) {
			ok, err := c.deviceShifu.collectSocketTelemetry()

			assert.Equal(t, c.expected, ok)
			if err != nil {
				if len(c.expErrStr) == 0 {
					assert.Nil(t, err.Error())
				} else {
					assert.Equal(t, err.Error(), c.expErrStr)
				}
			}
		})
	}
}

func TestDeviceCommandHandlerSocket(t *testing.T) {
	hexEncoding := v1alpha1.HEX
	bufferLength := int64(10)
	readBuffer := make([]byte, bufferLength)
	ds := &DeviceShifu{}
	server, client := net.Pipe()
	_ = ds
	go func() {
		for {
			_, err := server.Read(readBuffer)
			if err != nil {
				t.Error("Error when Read from pipe")
			}
			_, err = server.Write(readBuffer)
			if err != nil {
				t.Error("Error when Write to pipe")
			}
		}
	}()
	metadata := &HandlerMetaData{
		connection: &client,
		edgeDeviceSpec: v1alpha1.EdgeDeviceSpec{
			ProtocolSettings: &v1alpha1.ProtocolSettings{
				SocketSetting: &v1alpha1.SocketSetting{
					Encoding:     &hexEncoding,
					BufferLength: &bufferLength,
				},
			},
		},
	}

	requestBody := &RequestBody{
		Command: "1234567890",
		Timeout: 1,
	}

	failRequestBody := &RequestBody{
		Command: "a", // The length of `hex` must be a multiple of two
		Timeout: 1,
	}

	body, err := json.Marshal(requestBody)
	if err != nil {
		t.Errorf("Error when marshal request body to []byte, error: %v", err)
	}
	failBody, err := json.Marshal(failRequestBody)
	if err != nil {
		t.Errorf("Error when marshal failRequestBody to []byte, error: %v", err)
	}

	hs := httptest.NewServer(deviceCommandHandlerSocket(metadata))
	defer hs.Close()

	dc := mockRestClient(hs.URL, "testing")

	testCases := []struct {
		name         string
		request      *rest.Request
		responseBody string
		expErrStr    string
	}{
		{
			name:      "case1 not set header 'content-Type'",
			request:   dc.Post(),
			expErrStr: "the server rejected our request for an unknown reason",
		},
		{
			name:      "case2 request Body is empty",
			request:   dc.Post().SetHeader("Content-Type", "application/json"),
			expErrStr: "the server rejected our request for an unknown reason",
		}, {
			name:      "case3 requestBody Encode is error",
			request:   dc.Post().SetHeader("Content-Type", "application/json").Body(failBody),
			expErrStr: "the server rejected our request for an unknown reason",
		}, {
			name:      "case pass",
			request:   dc.Post().SetHeader("Content-Type", "application/json").Body(body),
			expErrStr: "",
		},
	}

	for _, c := range testCases {
		t.Run(c.name, func(t *testing.T) {
			err := c.request.Do(context.TODO()).Error()

			if len(c.expErrStr) == 0 {
				assert.Nil(t, err)
			} else {
				assert.Equal(t, err.Error(), c.expErrStr)
			}
		})
	}
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

func mockHttpServer(t *testing.T) *httptest.Server {
	socketProtocol := v1alpha1.ProtocolSocket
	mockrs := v1alpha1.EdgeDevice{
		ObjectMeta: metav1.ObjectMeta{
			Name:      "test_name",
			Namespace: "test_namespace",
		},
		Spec: v1alpha1.EdgeDeviceSpec{
			Protocol: &socketProtocol,
			Address:  unitest.ToPointer(UnitTestAddress),
			ProtocolSettings: &v1alpha1.ProtocolSettings{
				SocketSetting: &v1alpha1.SocketSetting{
					NetworkType: unitest.ToPointer("tcp"),
				},
			},
		},
	}

	dsByte, _ := json.Marshal(mockrs)

	// Implements the http.Handler interface to be passed to httptest.NewServer
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		path := r.URL.Path
		switch path {
		case "/apis/shifu.edgenesis.io/v1alpha1/namespaces/test_namespace/edgedevices/test_name":
			w.Header().Add("Content-Type", "application/json")
			w.WriteHeader(http.StatusOK)
			_, err := w.Write(dsByte)
			if err != nil {
				t.Errorf("failed to write response")
			}
		default:
			t.Errorf("Not expected to request: %s", r.URL.Path)
		}
	}))
	return server
}
