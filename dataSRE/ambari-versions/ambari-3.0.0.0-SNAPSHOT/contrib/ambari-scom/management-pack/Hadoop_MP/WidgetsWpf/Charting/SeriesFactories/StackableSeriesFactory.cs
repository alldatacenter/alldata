// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

using System.ComponentModel.Composition;
using Microsoft.EnterpriseManagement.Presentation.Controls;
using Microsoft.EnterpriseManagement.Presentation.DataAccess;

namespace Ambari.SCOM.Widgets.Charting.SeriesFactories {
    [Export, PartCreationPolicy(CreationPolicy.NonShared)]
    public class StackableSeriesFactory : Series2DFactoryBase {
        // There is no converter implementation for StackedSeries itself, thus nothing will be shown
        private const string DefaultStackedSeriesType = "xsd://Ambari.SCOM.Presentation!Ambari.SCOM.Presentation.Schema.ChartDataTypes/StackedSeries";

        protected override string DataTypeName {
            get { return ChartingConstants.StackableSeriesType; }
        }

        protected override IDataObject BuildSeriesInternal(IDataObject data, IDataObject configuration, IDataModel dataModel) {
            var ido = base.BuildSeriesInternal(data, configuration, dataModel);
            ido[ChartingConstants.StackedSeriesTypePropertyName] = configuration[ChartingConstants.StackedSeriesTypePropertyName] ?? DefaultStackedSeriesType;
            ido[ChartingConstants.SeriesOrderPropertyName] = configuration[ChartingConstants.SeriesOrderPropertyName] ?? 0;
            ido[ChartingConstants.StackedSeriesSizePropertyName] = configuration[ChartingConstants.StackedSeriesSizePropertyName] ?? 0;
            return ido;
        }

        protected override IDataModel BuildDataModel(IDataObject data) {
            var model = base.BuildDataModel(data) as ObservableDataModel;
            var type = model.Types[DataTypeName];
            type.Properties.Create(ChartingConstants.StackedSeriesTypePropertyName, typeof(string));
            type.Properties.Create(ChartingConstants.SeriesOrderPropertyName, typeof(int));
            type.Properties.Create(ChartingConstants.StackedSeriesSizePropertyName, typeof(int));
            return model;
        }
    }
}
