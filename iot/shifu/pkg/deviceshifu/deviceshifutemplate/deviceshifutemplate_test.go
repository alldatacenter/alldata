// Barebone deviceshifu test code. You need to modify this accordingly.
package deviceshifutemplate

import (
	"testing"

	"github.com/edgenesis/shifu/pkg/deviceshifu/deviceshifubase"
	"k8s.io/apimachinery/pkg/util/wait"
)

func TestNew(t *testing.T) {
	_, _ = New(&deviceshifubase.DeviceShifuMetaData{})
}

func TestStart(t *testing.T) {
	mockds := &DeviceShifu{}
	_ = mockds.Start(wait.NeverStop)
}

func TestStop(t *testing.T) {
	mockds := &DeviceShifu{}
	_ = mockds.Stop()
}
