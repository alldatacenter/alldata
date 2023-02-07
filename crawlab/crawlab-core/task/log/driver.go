package log

func GetLogDriver(logDriverType string, options interface{}) (driver Driver, err error) {
	switch logDriverType {
	case DriverTypeFile:
		if options == nil {
			options = &FileLogDriverOptions{}
		}
		options, ok := options.(*FileLogDriverOptions)
		if !ok {
			return driver, ErrInvalidType
		}
		driver, err = GetFileLogDriver(options)
		if err != nil {
			return driver, err
		}
	case DriverTypeMongo:
		return driver, ErrNotImplemented
	case DriverTypeEs:
		return driver, ErrNotImplemented
	default:
		return driver, ErrInvalidType
	}
	return driver, nil
}
