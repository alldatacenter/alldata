// Barebone deviceshifu source code. You need to modify this accordingly.
package deviceshifutemplate

import (
	"github.com/edgenesis/shifu/pkg/deviceshifu/deviceshifubase"
)

type DeviceShifu struct {
	//base *deviceshifubase.DeviceShifuBase
}

func New(deviceShifuMetadata *deviceshifubase.DeviceShifuMetaData) (*DeviceShifu, error) {
	return nil, nil
}

// Start starts the telemetry service
func (ds *DeviceShifu) Start(stopCh <-chan struct{}) error {
	return nil
}

// Stop stops the http server
func (ds *DeviceShifu) Stop() error {
	return nil
}
