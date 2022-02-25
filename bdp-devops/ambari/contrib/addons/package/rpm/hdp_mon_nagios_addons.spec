##
#
#/*
# * Licensed to the Apache Software Foundation (ASF) under one
# * or more contributor license agreements.  See the NOTICE file
# * distributed with this work for additional information
# * regarding copyright ownership.  The ASF licenses this file
# * to you under the Apache License, Version 2.0 (the
# * "License"); you may not use this file except in compliance
# * with the License.  You may obtain a copy of the License at
# *
# *     http://www.apache.org/licenses/LICENSE-2.0
# *
# * Unless required by applicable law or agreed to in writing, software
# * distributed under the License is distributed on an "AS IS" BASIS,
# * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# * See the License for the specific language governing permissions and
# * limitations under the License.
# */

#
# RPM Spec file for Nagios Add-ons for HDP Monitoring Dashboard
#

%define name hdp_mon_nagios_addons
%define release %(cat %{_sourcedir}/release.txt)
%define version %(cat %{_sourcedir}/version.txt)
%define buildroot %{_tmppath}/%{name}-%{version}-buildroot

Summary: Nagios Add-ons for HDP Monitoring Dashboard
Name: %{name}
Version: %{version}
URL: http://incubator.apache.org/ambari
Release: %{release}%{?dist}
License: Apache License, Version 2.0
Group: System Environment/Base
Source: %{name}-%{version}.tar.gz
Buildroot: %{buildroot}
Requires: nagios = 3.5.0-99, nagios-plugins = 1.4.9, php >= 5

%define nagioshdpscripts_dir %{_prefix}/share/hdp/nagios
%define nagiosplugin_dir %{_libdir}/nagios/plugins
%if 0%{?suse_version}
%define httpd_confdir %{_sysconfdir}/apache2/conf.d
%else
%define httpd_confdir %{_sysconfdir}/httpd/conf.d
%endif
BuildArchitectures: noarch

%description
This package provides add-on helper scripts and plugins for nagios for 
monitoring of a Hadoop Cluster

%prep
%setup -q -n %{name}-%{version}
%build

%install
# Flush any old RPM build root
%__rm -rf $RPM_BUILD_ROOT

%__mkdir -p $RPM_BUILD_ROOT/%{nagioshdpscripts_dir}/
%__mkdir -p $RPM_BUILD_ROOT/%{nagiosplugin_dir}/
%__mkdir -p $RPM_BUILD_ROOT/%{httpd_confdir}/

%__cp -rf scripts/* $RPM_BUILD_ROOT/%{nagioshdpscripts_dir}/
%__cp -rf plugins/* $RPM_BUILD_ROOT/%{nagiosplugin_dir}/
echo "Alias /ambarinagios %{_prefix}/share/hdp" >> $RPM_BUILD_ROOT/%{httpd_confdir}/hdp_mon_nagios_addons.conf
echo "<Directory /usr/share/hdp>" >> $RPM_BUILD_ROOT/%{httpd_confdir}/hdp_mon_nagios_addons.conf
echo "  Options None" >> $RPM_BUILD_ROOT/%{httpd_confdir}/hdp_mon_nagios_addons.conf
echo "  AllowOverride None" >> $RPM_BUILD_ROOT/%{httpd_confdir}/hdp_mon_nagios_addons.conf
echo "  Order allow,deny" >> $RPM_BUILD_ROOT/%{httpd_confdir}/hdp_mon_nagios_addons.conf
echo "  Allow from all" >> $RPM_BUILD_ROOT/%{httpd_confdir}/hdp_mon_nagios_addons.conf
echo "</Directory>" >> $RPM_BUILD_ROOT/%{httpd_confdir}/hdp_mon_nagios_addons.conf

%files
%defattr(-,root,root)
%{nagioshdpscripts_dir}/*
%attr(0755,root,root)%{nagiosplugin_dir}/*
%{httpd_confdir}/hdp_mon_nagios_addons.conf

%clean
%__rm -rf $RPM_BUILD_ROOT

%changelog
* Fri Feb 17 2011 Hortonworks <ambari-group@hortonworks.com>
- Initial version
