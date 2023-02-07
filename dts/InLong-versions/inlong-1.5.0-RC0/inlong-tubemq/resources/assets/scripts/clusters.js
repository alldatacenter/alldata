/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
(function () {
    var page = {
        init: function () {
            this.dialogInstance = new Dialog();
            this.initTable();
            this.initEvent();

        },
        initEvent: function () {
            var self = this;
            $('body').on('click', '.js_toggle', function () {
                self.dialogInstance.confirm('确认<span class="hl">切换</span>集群吗？',
                    function (passwd, formData) {
                        $.getJSON(G_CONFIG.HOST
                            + '?type=op_modify&method=admin_transfer_current_master&confModAuthToken='
                            + passwd + '&callback=?')
                            .done(function (data) {
                                if (data.errCode === 0) {
                                    self.dialogInstance.showTips('操作成功');
                                } else {
                                    self.dialogInstance.showTips(
                                        data.errMsg);
                                }
                            });
                    });
            });
        },
        initTable: function () {

            var renderHtml = function (cluster) {
                var trHtml = '';

                var nodesInfo = cluster.data;
                for (var nodeItem in nodesInfo) {
                    var node = nodesInfo[nodeItem];
                    if (nodeItem == 0) {
                        trHtml +=
                            '<tr><td rowspan="3">' + cluster.groupName + '</td><td rowspan="3">'
                            + cluster.groupStatus + '</td><td>' + node.name + '</td><td>'
                            + node.hostName + ':' + node.port + '</td><td>'
                            + node.statusInfo.nodeStatus
                            + '</td><td rowspan="3"><a href="javascript:;" class="link js_toggle">切换</a></td></tr>';
                    } else {
                        trHtml +=
                            '<tr><td>' + node.name + '</td><td>' + node.hostName + ':' + node.port
                            + '</td><td>' + node.statusInfo.nodeStatus + '</td></tr>';
                    }
                }

                $('#clustersInfo').html(trHtml);
            };

            $.getJSON(
                G_CONFIG.HOST + "?type=op_query&method=admin_query_master_group_info&callback=?")
                .done(function (data) {
                    if (data.errCode === 0) {
                        renderHtml(data)
                    }
                })
        }
    };

    page.init();
})();