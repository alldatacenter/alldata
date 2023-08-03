package deviceshifutcp

import (
	"encoding/json"
	"github.com/edgenesis/shifu/pkg/deviceshifu/deviceshifubase"
	"github.com/edgenesis/shifu/pkg/deviceshifu/unitest"
	"github.com/edgenesis/shifu/pkg/k8s/api/v1alpha1"
	"github.com/edgenesis/shifu/pkg/logger"
	"github.com/stretchr/testify/assert"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/util/wait"
	"net"
	"net/http"
	"net/http/httptest"
	"os"
	"testing"
)

func TestMain(m *testing.M) {
	err := GenerateConfigMapFromSnippet(MockDeviceCmStr, MockDeviceConfigFolder)
	if err != nil {
		logger.Errorf("error when generateConfigmapFromSnippet,err: %v", err)
		os.Exit(-1)
	}

	listener, err := net.Listen("tcp", UnitTestAddress)
	if err != nil {
		logger.Fatalf("Cannot Listen at %v due to: %v", UnitTestAddress, err.Error())
	}
	go func() {
		for {
			conn, err := listener.Accept()
			if err != nil {
				logger.Error(err)
				continue
			}
			go func(conn net.Conn) {
				buf := make([]byte, 32)
				n, err := conn.Read(buf)
				if err != nil {
					logger.Error(err)
				}
				logger.Infof(string(buf[:n]))
				_, err = conn.Write(buf)
				if err != nil {
					logger.Error(err)
				}
				conn.Close()
			}(conn)
		}
	}()
	defer listener.Close()
	m.Run()
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
		t.Errorf("Failed creating new deviceshifu due to: %v", err.Error())
	}

	if err := mockds.Start(wait.NeverStop); err != nil {
		t.Errorf("DeviceShifu.Start failed due to: %v", err.Error())
	}

	if err := mockds.Stop(); err != nil {
		t.Errorf("unable to stop mock deviceShifu, error: %+v", err)
	}
}

func TestCollectTCPTelemetry(t *testing.T) {
	protocolTCP := v1alpha1.ProtocolTCP
	protocolOPCUA := v1alpha1.ProtocolOPCUA
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
							Protocol: &protocolTCP,
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
							Protocol: &protocolTCP,
						},
					},
				},
			},
			expected:  false,
			expErrStr: "device testDevice does not have an address",
		}, {
			Name: "case3 Protocol is not TCP",
			deviceShifu: &DeviceShifu{
				base: &deviceshifubase.DeviceShifuBase{
					Name: "testDevice",
					EdgeDevice: &v1alpha1.EdgeDevice{
						Spec: v1alpha1.EdgeDeviceSpec{
							Protocol: &protocolOPCUA,
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
							Protocol: &protocolTCP,
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
			ok, err := c.deviceShifu.collectTcpTelemetry()

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

func TestHandleTCPConnection(t *testing.T) {
	Listener, err := net.Listen("tcp", UnitTestProxyAddress)
	if err != nil {
		logger.Fatalf("Cannot Listen at %v due to: %v", UnitTestProxyAddress, err.Error())
	}
	cm := ConnectionMetaData{
		ForwardAddress: UnitTestAddress,
		NetListener:    Listener,
	}
	// start the proxy server
	go func() {
		err := cm.Start(wait.NeverStop)
		if err != nil {
			logger.Errorf("Error starting deviceshifu: %v", err)
		}
	}()

	clientConn, err := net.Dial("tcp", UnitTestProxyAddress)
	if err != nil {
		logger.Fatalf("Cannot Dial to proxy due to: %v", err.Error())
		return
	}

	writeByte := []byte("something")
	readByte := make([]byte, len(writeByte))

	_, err = clientConn.Write(writeByte)
	if err != nil {
		logger.Fatalf("Cannot Write due to: %v", err.Error())
		return
	}
	if cw, ok := clientConn.(*net.TCPConn); ok {
		err := cw.CloseWrite()
		if err != nil {
			logger.Fatalf("can't close write of conn")
		}
	} else {
		logger.Fatalf("no implement CloseWrite in conn")
	}
	_, err = clientConn.Read(readByte)
	if err != nil {
		logger.Fatalf("Cannot Read due to: %v", err.Error())
	}
	clientConn.Close()
	assert.Equal(t, readByte, writeByte)
}

func mockHttpServer(t *testing.T) *httptest.Server {
	protocolTCP := v1alpha1.ProtocolTCP
	mockrs := v1alpha1.EdgeDevice{
		ObjectMeta: metav1.ObjectMeta{
			Name:      "test_name",
			Namespace: "test_namespace",
		},
		Spec: v1alpha1.EdgeDeviceSpec{
			Protocol: &protocolTCP,
			Address:  unitest.ToPointer(UnitTestAddress),
			ProtocolSettings: &v1alpha1.ProtocolSettings{
				TCPSetting: &v1alpha1.TCPSetting{
					NetworkType: unitest.ToPointer("tcp"),
					ListenPort:  unitest.ToPointer("8081"),
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
