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
    var dialogInstance = new Dialog();
    var page = {
        init: function () {
            var self = this;
            self.initEvent();
            self.initTable();
        },
        initEvent: function () {
            var self = this;

            $('body').on('click', '#queryBtn', function () {
                var topicName = $('#topicNameInput').val();
                var consumeGroup = $('#consumeGroupInput').val();

                self.initTable({
                    topicName: topicName,
                    consumeGroup: consumeGroup
                });
            });

        },
        initTable: function (opts) {
            opts = opts || {
                    topicName: '',
                    consumeGroup: ''
                };

            var url = G_CONFIG.HOST + "?type=op_query&method=admin_query_sub_info&" + $.param(opts);
            if (!this.$dataTable) {
                this.$dataTable = $('#list').DataTable({
                    "ajax": {
                        "url": url,
                        "dataType": "jsonp"
                    },
                    //"ordering": false,
                    "columns": [{
                        "data": "consumeGroup",
                        "className": 'h1',
                        "render": function (data, type, full,
                                            meta) {
                            var html = '<a href="detail.html?consumeGroup='
                                + data
                                + '" class="link" >'
                                + data + '</a>';
                            return html;
                        }
                    }, {
                        "data": "topicSet",
                        "className": 'h1'
                    }, {
                        "data": "consumerNum",
                        "defaultContent": '--'
                    }],
                    language: {
                        searchPlaceholder: '请输入消费组或者Topic名称',
                        processing: "Loading...",
                        search: "搜索:",
                        //lengthMenu: "每页显示 _MENU_ 条",
                        lengthMenu: '每页显示 <select class="min">'
                        +
                        '<option value="10">10</option>'
                        +
                        '<option value="20">20</option>'
                        +
                        '<option value="30">30</option>'
                        +
                        '<option value="40">40</option>'
                        +
                        '<option value="50">50</option>'
                        +
                        '<option value="-1">全部</option>'
                        +
                        '</select> 条',
                        info: "当前显示由 _START_ 到 _END_ 条，共 _TOTAL_ 条记录， ",
                        infoEmpty: "",
                        //infoFiltered:   "(_MAX_ 条数据搜索结果)",
                        infoFiltered: "",
                        infoPostFix: "",
                        loadingRecords: "正在加载...",
                        zeroRecords: "暂无记录",
                        emptyTable: "暂无记录",
                        paginate: {
                            first: '<i class="i-first"></i>',
                            previous: '<i class="i-prev"></i>',
                            next: '<i class="i-next"></i>',
                            last: '<i class="i-last"></i>'
                        }
                    },
                    'pagingType': "full_numbers",
                    "dom": '<"scroll-wp"rt><"pg-wp"ilp>',
                });
            } else {
                this.$dataTable.ajax.url(url).load();
            }

        }
    };

    page.init();
})();