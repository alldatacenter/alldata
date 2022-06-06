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
# RPM Spec file for Ganglia Add-ons for HDP Monitoring Dashboard
#

%define name  hdp_mon_ganglia_addons
%define release %(cat %{_sourcedir}/release.txt)
%define version %(cat %{_sourcedir}/version.txt)
%define buildroot %{_tmppath}/%{name}-%{version}-buildroot


Summary: Ganglia Add-ons for HDP Monitoring Dashboard
Name: %{name}
Version: %{version}
URL: http://incubator.apache.org/ambari
Release: %{release}%{?dist}
License: Apache License, Version 2.0
Group: System Environment/Base
Source: %{name}-%{version}.tar.gz
Buildroot: %{buildroot}
Requires: gweb >= 2.2

%if 0%{?suse_version}
%define graphd_dir /srv/www/htdocs/ganglia/graph.d/
%else
%define graphd_dir /var/www/html/ganglia/graph.d/
%endif
%define gconf_dir /var/lib/ganglia/conf/

BuildArchitectures: noarch

%description
This package provides add-on graphs and configurations for ganglia to provide 
for a better monitoring integration with a Hadoop Cluster

%prep
%setup -q -n %{name}-%{version}
%build

%install
# Flush any old RPM build root
%__rm -rf $RPM_BUILD_ROOT

%__mkdir -p $RPM_BUILD_ROOT/%{graphd_dir}/
%__mkdir -p $RPM_BUILD_ROOT/%{gconf_dir}/

%__cp -rf conf/* $RPM_BUILD_ROOT/%{gconf_dir}/
%__cp -rf graph.d/* $RPM_BUILD_ROOT/%{graphd_dir}/


%files
%defattr(-,root,root)
%{graphd_dir}/*
%{gconf_dir}/*

%clean
%__rm -rf $RPM_BUILD_ROOT

%changelog
* Fri Feb 17 2011 Hortonworks <ambari-group@hortonworks.com>
- Initial version
