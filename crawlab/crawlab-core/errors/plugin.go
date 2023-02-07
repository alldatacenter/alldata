package errors

func NewPluginError(msg string) (err error) {
	return NewError(ErrorPrefixPlugin, msg)
}

var ErrorPluginNotImplemented = NewPluginError("not implemented")
var ErrorPluginPathNotExists = NewPluginError("path not exists")
var ErrorPluginPluginJsonNotExists = NewPluginError("plugin.json not exists")
var ErrorPluginMissingRequiredOption = NewPluginError("missing required option")
var ErrorPluginNotExists = NewPluginError("not exists")
var ErrorPluginMissingProcess = NewPluginError("missing process")
var ErrorPluginInvalidType = NewPluginError("invalid type")
var ErrorPluginForbidden = NewPluginError("forbidden")
