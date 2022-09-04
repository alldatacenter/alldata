Name: t-tesla-middleware-nacos-OXS
Version:20.0.33
Release: %(echo $RELEASE)
Summary: Tesla Server: tesla-nacos
Group: alibaba/application
License: Commercial
BuildArch: noarch
AutoReq: no
BuildRequires: t-tesla-maven >= 3.6.0.0

%global _python_bytecompile_errors_terminate_build 0

%define _name t-tesla-middleware-nacos
%define _prefix /home/admin/tesla/middleware/nacos
%define _app_name tesla-nacos
%define _start_module tesla-nacos-start
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
cp t-tesla-middleware-nacos.service $RPM_BUILD_ROOT/usr/lib/systemd/system/
# 编译
cd $SRCPWD
# find . -type d -name target | xargs rm -rf
# rm -rf %{_start_module}/src/main/resources/application.properties
JAVA_HOME=/opt/taobao/java /usr/local/t-tesla-maven/bin/mvn -f pom_private.xml clean install -Dmaven.test.skip=true
jar_name=`ls -q %{_start_module}/target | grep ".jar" | grep -v ".original" | grep -v ".source" | awk '{print $1}'`
# 拷贝 jar && 配置文件
cp %{_start_module}/target/${jar_name} $RPM_BUILD_ROOT%{_prefix}/bin/%{_app_name}.jar
cp %{_start_module}/src/main/resources/application-%{_application_profile}.properties $RPM_BUILD_ROOT%{_prefix}/resources/application-%{_application_profile}.properties.tpl
cd $SRCPWD; rsync -av APP-META-PRIVATE $RPM_BUILD_ROOT%{_prefix}/

%clean
rm -rf $RPM_BUILD_ROOT/%{_app_name}

%files

%defattr(-,admin,admin)
%{_prefix}
/usr/lib/systemd/system/%{_name}.service

%changelog
* Sat Sep 2 2019 Qiang Qiu <qiuqiang.qq@alibaba-inc.com>
- add spec of %{_name}
