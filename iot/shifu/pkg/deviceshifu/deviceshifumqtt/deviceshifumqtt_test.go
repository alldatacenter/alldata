package deviceshifumqtt

import (
	"context"
	"fmt"
	"sync"

	"net/http"
	"net/http/httptest"

	"k8s.io/client-go/kubernetes/scheme"
	"k8s.io/client-go/rest"

	"errors"
	"io"
	"os"
	"strings"
	"testing"
	"time"

	"github.com/edgenesis/shifu/pkg/k8s/api/v1alpha1"
	"github.com/edgenesis/shifu/pkg/logger"
	"github.com/stretchr/testify/assert"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"

	mqtt "github.com/eclipse/paho.mqtt.golang"
	"github.com/mochi-co/mqtt/server"
	"github.com/mochi-co/mqtt/server/listeners"

	"github.com/edgenesis/shifu/pkg/deviceshifu/deviceshifubase"
	"github.com/edgenesis/shifu/pkg/deviceshifu/unitest"
	"k8s.io/apimachinery/pkg/util/wait"

	v1 "k8s.io/api/apps/v1"
)

const (
	unitTestServerAddress = "localhost:18928"
)

func TestMain(m *testing.M) {
	err := GenerateConfigMapFromSnippet(MockDeviceCmStr, MockDeviceConfigFolder)
	if err != nil {
		logger.Errorf("error when generateConfigmapFromSnippet,err: %v", err)
		os.Exit(-1)
	}
	m.Run()
	err = os.RemoveAll(MockDeviceConfigPath)
	if err != nil {
		logger.Fatal(err)
	}
}

func TestStart(t *testing.T) {
	deviceShifuMetadata := &deviceshifubase.DeviceShifuMetaData{
		Name:           "TestStart",
		ConfigFilePath: "etc/edgedevice/config",
		KubeConfigPath: deviceshifubase.DeviceKubeconfigDoNotLoadStr,
		Namespace:      "",
	}

	mockds, err := New(deviceShifuMetadata)
	if err != nil {
		t.Errorf("Failed creating new deviceshifu %v", err.Error())
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
		Namespace:      "",
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

func TestCommandHandleMQTTFunc(t *testing.T) {
	hs := mockHandlerServer(t)
	defer hs.Close()
	addr := strings.Split(hs.URL, "//")[1]
	properties := &MQTTProtocolProperty{
		MQTTTopic: "test/test1",
	}
	mockHandlerHTTP := &DeviceCommandHandlerMQTT{
		HandlerMetaData: &HandlerMetaData{
			edgeDeviceSpec: v1alpha1.EdgeDeviceSpec{
				Address: &addr,
			},
			properties: properties,
		},
	}

	ds := mockDeviceServer(mockHandlerHTTP, t)
	defer ds.Close()
	dc := mockRestClient(ds.URL, "testing")

	stop := make(chan struct{}, 1)
	wg := sync.WaitGroup{}
	wg.Add(1)
	t.Setenv("SERVER_LISTEN_PORT", ":18926")
	go func() {
		mockMQTTServer(stop)
		wg.Done()
	}()
	
	opts := mqtt.NewClientOptions()
	opts.AddBroker(fmt.Sprintf("tcp://%s", *unitest.ToPointer(unitTestServerAddress)))
	opts.SetClientID("shifu-service")
	opts.SetDefaultPublishHandler(messagePubHandler)
	opts.OnConnect = connectHandler
	opts.OnConnectionLost = connectLostHandler
	client = mqtt.NewClient(opts)

	requestBody := "moving_the_device"

	// test post method when MQTTServer not connected
	r := dc.Post().Body([]byte(requestBody)).Do(context.TODO())
	assert.Equal(t, "the server rejected our request for an unknown reason", r.Error().Error())

	// test post method when MQTTServer connected
	var token mqtt.Token
	// try to connect to MQTT server three times
	for i := 0; i < 3; i++ {
		if token = client.Connect(); token.Wait() && token.Error() == nil {
			logger.Infof("Connected to %v MQTT server suceess!", unitTestServerAddress)
			defer client.Disconnect(0)
			break
		}
		logger.Errorf("Error when connect to server for the %d time: %v", i+1, token.Error())
		time.Sleep(100 * time.Millisecond)
	}
	assert.Nil(t, token.Error())

	ConfigFiniteStateMachine(map[string]string{"moving_the_device": "device_finish_moving"})
	r = dc.Post().Body([]byte(requestBody)).Do(context.TODO())
	assert.Nil(t, r.Error())
	r = dc.Post().Body([]byte(requestBody)).Do(context.TODO())
	assert.NotNil(t, r.Error()) // should be blocked
	// reset mutex
	MutexProcess("test/test1", "device_finish_moving")
	r = dc.Post().Body([]byte(requestBody)).Do(context.TODO())
	assert.Nil(t, r.Error()) // not blocked

	// test put method
	r = dc.Put().Do(context.TODO())
	assert.Equal(t, "the server rejected our request for an unknown reason", r.Error().Error())

	// test Cannot Encode message to json
	r = dc.Get().Do(context.TODO())
	assert.Nil(t, r.Error())

	stop <- struct{}{}
	wg.Wait()
}

func mockMQTTServer(stop <-chan struct{}) {
	tcp := listeners.NewTCP("t1", unitTestServerAddress)
	server := server.NewServer(nil)
	err := server.AddListener(tcp, nil)
	if err != nil {
		logger.Fatalf("Error when Listen at %v, error: %v", unitTestServerAddress, err)
	}

	err = server.Serve()
	if err != nil {
		logger.Fatalf("Error when MQTT Server Serve, error: %v", err)
	}

	<-stop
	server.Close()
	logger.Infof("Server Closed")
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
			logger.Info("handler get the instruction and executed.")
		default:
			w.WriteHeader(http.StatusOK)
			logger.Info("hs get default request, path:", path)
		}

	}))
	return server
}

func TestCollectMQTTTelemetry(t *testing.T) {
	instructionName := "get_reading"
	testTelemetryInstructionName := &instructionName

	testCases := []struct {
		Name        string
		inputDevice *DeviceShifu
		expected    bool
		err         error
	}{
		{
			"case 1 Protocol is nil",
			&DeviceShifu{
				base: &deviceshifubase.DeviceShifuBase{
					Name: "test",
					EdgeDevice: &v1alpha1.EdgeDevice{
						Spec: v1alpha1.EdgeDeviceSpec{
							Address: unitest.ToPointer("localhost"),
						},
					},
				},
			},
			false,
			nil,
		},
		{
			"case 2 Address is nil",
			&DeviceShifu{
				base: &deviceshifubase.DeviceShifuBase{
					Name: "test",
					EdgeDevice: &v1alpha1.EdgeDevice{
						Spec: v1alpha1.EdgeDeviceSpec{
							Protocol: unitest.ToPointer(v1alpha1.ProtocolMQTT),
						},
					},
					DeviceShifuConfig: &deviceshifubase.DeviceShifuConfig{
						Telemetries: &deviceshifubase.DeviceShifuTelemetries{},
					},
				},
			},
			false,
			errors.New("device test does not have an address"),
		},
		{
			"case 3 DeviceShifuTelemetry Update",
			&DeviceShifu{
				mqttInstructions: &MQTTInstructions{
					Instructions: map[string]*MQTTInstruction{
						"get_reading": &MQTTInstruction{
							MQTTProtocolProperty: &MQTTProtocolProperty{
								MQTTTopic: "test/test1",
							},
						},
					},
				},
				base: &deviceshifubase.DeviceShifuBase{
					Name: "test",
					EdgeDevice: &v1alpha1.EdgeDevice{
						ObjectMeta: metav1.ObjectMeta{
							Namespace: "test_namespace",
						},
						Spec: v1alpha1.EdgeDeviceSpec{
							Address:  unitest.ToPointer("localhost"),
							Protocol: unitest.ToPointer(v1alpha1.ProtocolMQTT),
						},
					},
					DeviceShifuConfig: &deviceshifubase.DeviceShifuConfig{
						Telemetries: &deviceshifubase.DeviceShifuTelemetries{
							DeviceShifuTelemetrySettings: &deviceshifubase.DeviceShifuTelemetrySettings{
								DeviceShifuTelemetryUpdateIntervalInMilliseconds: unitest.ToPointer(time.Now().UnixMilli()),
							},
							DeviceShifuTelemetries: map[string]*deviceshifubase.DeviceShifuTelemetry{
								"health": &deviceshifubase.DeviceShifuTelemetry{
									DeviceShifuTelemetryProperties: deviceshifubase.DeviceShifuTelemetryProperties{
										DeviceInstructionName: testTelemetryInstructionName,
									},
								},
							},
						},
					},
				},
			},
			true,
			nil,
		},
		{
			"case 4 Protocol is http",
			&DeviceShifu{
				base: &deviceshifubase.DeviceShifuBase{
					Name: "test",
					EdgeDevice: &v1alpha1.EdgeDevice{
						Spec: v1alpha1.EdgeDeviceSpec{
							Address:  unitest.ToPointer("localhost"),
							Protocol: unitest.ToPointer(v1alpha1.ProtocolHTTP),
						},
					},
				},
			},
			false,
			nil,
		},
		{
			"case 5 interval is nil",
			&DeviceShifu{
				mqttInstructions: &MQTTInstructions{
					Instructions: map[string]*MQTTInstruction{
						"get_reading": &MQTTInstruction{
							MQTTProtocolProperty: &MQTTProtocolProperty{
								MQTTTopic: "test/test1",
							},
						},
					},
				},
				base: &deviceshifubase.DeviceShifuBase{
					Name: "test",
					EdgeDevice: &v1alpha1.EdgeDevice{
						Spec: v1alpha1.EdgeDeviceSpec{
							Address:  unitest.ToPointer("localhost"),
							Protocol: unitest.ToPointer(v1alpha1.ProtocolMQTT),
						},
					},
					DeviceShifuConfig: &deviceshifubase.DeviceShifuConfig{
						Telemetries: &deviceshifubase.DeviceShifuTelemetries{
							DeviceShifuTelemetrySettings: &deviceshifubase.DeviceShifuTelemetrySettings{},
							DeviceShifuTelemetries: map[string]*deviceshifubase.DeviceShifuTelemetry{
								"health": &deviceshifubase.DeviceShifuTelemetry{
									DeviceShifuTelemetryProperties: deviceshifubase.DeviceShifuTelemetryProperties{
										DeviceInstructionName: testTelemetryInstructionName,
									},
								},
							},
						},
					},
				},
			},
			true,
			nil,
		},
	}

	mqttMessageReceiveTimestampMap["test/test1"] = time.Now()
	for _, c := range testCases {
		t.Run(c.Name, func(t *testing.T) {
			got, err := c.inputDevice.collectMQTTTelemetry()
			if got {
				assert.Equal(t, c.expected, got)
				assert.Nil(t, err)
			} else {
				assert.Equal(t, c.expected, got)
				assert.Equal(t, c.err, err)
			}
		})
	}
}
