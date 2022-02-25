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


namespace Ambari.SCOM.Widgets.Charting {
    static class ChartingConstants {
        public const string ColorFactoryType = "Microsoft.SystemCenter.Visualization.Library!Microsoft.SystemCenter.Visualization.ColorConverter";

        public const string StackableSeriesType = "xsd://Ambari.SCOM.Presentation!Ambari.SCOM.Presentation.Schema.ChartDataTypes/StackableSeries";
        public const string AreaSeriesType = "xsd://Ambari.SCOM.Presentation!Ambari.SCOM.Presentation.Schema.ChartDataTypes/AreaSeries";

        public const string HexColorDataType = "xsd://Microsoft.SystemCenter.Visualization.Library!Microsoft.SystemCenter.Visualization.ChartDataTypes/HexColor";
        public const string HexColorPropertyName = "HexString";

        public const string StackedSeriesTypePropertyName = "StackedSeriesType";
        public const string SeriesOrderPropertyName = "Order";
        public const string StackedSeriesSizePropertyName = "StackSize";

        public static readonly string GridLinesColorPropertyName = "GridLinesHexColor";
    }
}
