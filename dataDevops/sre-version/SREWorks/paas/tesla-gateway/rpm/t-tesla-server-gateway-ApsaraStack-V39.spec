Name: t-tesla-server-gateway-ApsaraStack-V39
Version:1.0.0
Release: %(echo $RELEASE)
Summary: Tesla Server: tesla-gateway
Group: alibaba/application
License: Commercial
BuildArch: noarch
AutoReq: no
BuildRequires: t-tesla-maven >= 3.6.0.0

%global _python_bytecompile_errors_terminate_build 0

%define _name t-tesla-server-gateway
%define _prefix /home/admin/tesla/server/gateway
%define _app_name tesla-gateway
%define _application_profile private

%description
Tesla Server: %{_app_name}

%debug_package
# support debuginfo package, to reduce runtime package size

%install
export SRCPWD=$OLDPWD/..
# 初始化
mkdir -p $RPM_BUILD_ROOT%{_prefix}/bin
mkdir -p $RPM_BUILD_ROOT%{_prefix}/resources
mkdir -p $RPM_BUILD_ROOT/usr/lib/systemd/system/
# 拷贝 systemd config
cd $SRCPWD/APP-META-PRIVATE/deploy-config
cp t-tesla-server-gateway.service $RPM_BUILD_ROOT/usr/lib/systemd/system/
# 编译
cd $SRCPWD
find . -type d -name target | xargs rm -rf
rm -rf src/main/resources/application.properties
JAVA_HOME=/opt/taobao/java /usr/local/t-tesla-maven/bin/mvn -f pom_private.xml clean install -Dmaven.test.skip=true
jar_name=`ls -q target | grep ".jar" | grep -v ".original" | grep -v ".source" | awk '{print $1}'`
# 拷贝 jar && 配置文件
cp target/${jar_name} $RPM_BUILD_ROOT%{_prefix}/bin/%{_app_name}.jar
cp src/main/resources/application-%{_application_profile}.properties $RPM_BUILD_ROOT%{_prefix}/resources/application-%{_application_profile}.properties.tpl
cd $SRCPWD; rsync -av APP-META-PRIVATE $RPM_BUILD_ROOT%{_prefix}/

%clean
rm -rf $RPM_BUILD_ROOT/%{_app_name}

%files

%defattr(-,admin,admin)
%{_prefix}
/usr/lib/systemd/system/%{_name}.service

%changelog
* Sat Feb 23 2019 Yaoxing Guo <yaoxing.gyx@alibaba-inc.com>
- add spec of %{_name}
