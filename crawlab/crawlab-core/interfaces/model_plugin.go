package interfaces

type Plugin interface {
	ModelWithNameDescription
	GetShortName() (shortName string)
	SetShortName(shortName string)
	GetFullName() (fullName string)
	SetFullName(fullName string)
	GetInstallUrl() (url string)
	SetInstallUrl(url string)
	GetInstallType() (t string)
	SetInstallType(t string)
	GetInstallCmd() (cmd string)
	SetInstallCmd(cmd string)
}
