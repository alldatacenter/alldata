package deviceshifutcp

import (
	"fmt"
	"github.com/edgenesis/shifu/pkg/logger"
	"io"
	"net"

	"github.com/edgenesis/shifu/pkg/deviceshifu/deviceshifubase"
	"github.com/edgenesis/shifu/pkg/k8s/api/v1alpha1"
)

const (
	TCPDefaultPortStr = "8081"
	ProtocolTCPStr    = "tcp"
)

type DeviceShifu struct {
	base          *deviceshifubase.DeviceShifuBase
	TcpConnection *ConnectionMetaData
}

type ConnectionMetaData struct {
	ForwardAddress string
	NetListener    net.Listener
}

func New(deviceShifuMetadata *deviceshifubase.DeviceShifuMetaData) (*DeviceShifu, error) {
	base, mux, err := deviceshifubase.New(deviceShifuMetadata)
	if err != nil {
		return nil, err
	}
	var cm *ConnectionMetaData
	if deviceShifuMetadata.KubeConfigPath != deviceshifubase.DeviceKubeconfigDoNotLoadStr {
		switch protocol := *base.EdgeDevice.Spec.Protocol; protocol {
		case v1alpha1.ProtocolTCP:
			connectionType := base.EdgeDevice.Spec.ProtocolSettings.TCPSetting.NetworkType
			if connectionType == nil || *connectionType != "tcp" {
				return nil, fmt.Errorf(`connection type not specified or wrong connection type specified for deviceShifuTCP, should be "tcp"`)
			}
			listenPort := TCPDefaultPortStr
			if base.EdgeDevice.Spec.ProtocolSettings.TCPSetting.ListenPort != nil {
				listenPort = *base.EdgeDevice.Spec.ProtocolSettings.TCPSetting.ListenPort
			}
			ListenAddress := ":" + listenPort
			Listener, err := net.Listen(ProtocolTCPStr, ListenAddress)
			if err != nil {
				return nil, fmt.Errorf("Listen error")
			}
			cm = &ConnectionMetaData{
				ForwardAddress: *base.EdgeDevice.Spec.Address,
				NetListener:    Listener,
			}
		}
	}
	ds := DeviceShifu{base: base, TcpConnection: cm}
	deviceshifubase.BindDefaultHandler(mux)
	return &ds, nil
}

func (m *ConnectionMetaData) handleTCPConnection(conn net.Conn) {
	// Forward the TCP connection to the destination
	forwardConn, err := net.Dial("tcp", m.ForwardAddress)
	if err != nil {
		logger.Errorf(err.Error())
		return
	}
	defer forwardConn.Close()
	// Copy data between bi-directional connections.
	done := make(chan string)
	createCon := func(name string, dst io.Writer, src io.Reader) {
		_, err := io.Copy(dst, src)
		if err != nil {
			logger.Errorf(err.Error())
		}
		done <- name
	}
	go createCon("client_to_forward", forwardConn, conn)
	go createCon("forward_to_client", conn, forwardConn)
	name := <-done
	logger.Infof("connection %v is done", name)
	name = <-done
	logger.Infof("connection %v is done", name)
}

func (ds *DeviceShifu) collectTcpTelemetry() (bool, error) {
	if ds.base.EdgeDevice.Spec.Address == nil {
		return false, fmt.Errorf("device %v does not have an address", ds.base.Name)
	}

	if ds.base.EdgeDevice.Spec.Protocol != nil {
		switch protocol := *ds.base.EdgeDevice.Spec.Protocol; protocol {
		case v1alpha1.ProtocolTCP:
			conn, err := net.Dial("tcp", *ds.base.EdgeDevice.Spec.Address)
			if err != nil {
				logger.Errorf("error checking telemetry: error: %v", err.Error())
				return false, err
			}
			defer conn.Close()
			return true, nil
		default:
			logger.Warnf("EdgeDevice protocol %v not supported in deviceshifu", protocol)
			return false, nil
		}
	}
	return true, nil
}

func (m *ConnectionMetaData) Start(stopCh <-chan struct{}) error {
	for {
		conn, err := m.NetListener.Accept()
		if err != nil {
			return err
		}
		// create a new goroutine
		go m.handleTCPConnection(conn)
	}
}

func (ds *DeviceShifu) Start(stopCh <-chan struct{}) error {
	err := ds.base.Start(stopCh, ds.collectTcpTelemetry)
	if err != nil {
		return err
	}
	go func() {
		err := ds.TcpConnection.Start(stopCh)
		if err != nil {
			logger.Errorf("Error starting deviceshifuTCP: %v", err)
		}
	}()
	return nil
}

func (ds *DeviceShifu) Stop() error {
	return ds.base.Stop()
}
