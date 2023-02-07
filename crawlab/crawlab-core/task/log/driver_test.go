package log

import (
	"github.com/stretchr/testify/require"
	"testing"
)

func TestNewLogDriver(t *testing.T) {
	// create driver (fs)
	driver, err := GetLogDriver(DriverTypeFile, nil)
	require.Nil(t, err)
	require.NotNil(t, driver)

	// invalid options (fs)
	driver, err = GetLogDriver(DriverTypeFs, "1")
	require.Equal(t, ErrInvalidType, err)

	// create driver (mongo)
	driver, err = GetLogDriver(DriverTypeMongo, nil)
	require.Equal(t, ErrNotImplemented, err)

	// create driver (es)
	driver, err = GetLogDriver(DriverTypeEs, nil)
	require.Equal(t, ErrNotImplemented, err)

	// create invalid-type driver
	driver, err = GetLogDriver("invalid", nil)
	require.Equal(t, ErrInvalidType, err)
}
