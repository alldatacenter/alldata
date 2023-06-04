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
/* Default class modification */
$.extend($.fn.dataTable.ext.classes, {
    sLengthSelect: "min",
    sFilterInput: 'm'
});

// 弹层简单封装
var Dialog = function (opt) {
    this.init(opt);
};

// 页面元素提示
var languageVal = {
    searchPlaceholder: '请输入消费组或者Topic名称',
    search: "搜索:",
    //lengthMenu: "每页显示 _MENU_ 条",
    lengthMenu: '每页显示 <select class="min">' +
    '<option value="10">10</option>' +
    '<option value="20">20</option>' +
    '<option value="30">30</option>' +
    '<option value="40">40</option>' +
    '<option value="50">50</option>' +
    '<option value="-1">All</option>' +
    '</select> 条',
    info: "",
    infoEmpty: "",
    infoFiltered: "(_MAX_ 条数据的搜索结果) ",
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
};

Dialog.prototype.init = function (opt) {
    opt = opt || {};
    this.$body = $('body');
    this.$html = $('html');
    this.$popupWrap = this.createMask();
    this.$close = $('.js_close');
    this.hidedHook = opt.hidedHook || null;
    this.successHook = opt.successHook || null;
    this.bindEvent();
};

Dialog.prototype.bindEvent = function () {
    var self = this;
    self.$popupWrap.click(function (e) {
        if (event.target == this) {
            self.hideWrap();
        }
    });
    self.$popupWrap.on('click', '.js_close', function () {
        self.hideWrap();
        return false;
    });
};

Dialog.prototype.createMask = function () {
    if (!$('#dlg-mask').length) {
        this.$body.append('<div class="dlg-mask" style="display:none;" id="dlg-mask"></div>');
    }
    return $('#dlg-mask');
};

Dialog.prototype.showWrap = function () {
    this.$html.addClass('dlg-show');
    this.$popupWrap.show();
};

Dialog.prototype.hideWrap = function () {
    this.$html.removeClass('dlg-show');
    this.$popupWrap.hide();
    if (this.hidedHook) {
        this.hidedHook();
        this.hidedHook = null;
    }
};

Dialog.prototype.showTips = function (msg) {
    msg = msg || '操作无效';

    var self = this;
    var html = '<div class="dlg show s">' +
        '    <a href="javascript:" class="dlg-close js_close">×</a>' +
        '    <div class="dlg-hd">' +
        '        <h3 class="tit">系统提示</h3>' +
        '    </div>' +
        '    <div class="dlg-cnt">' +
        '        <div class="inner">' + msg + '</div>' +
        '    </div>' +
        '    <div class="dlg-btns">' +
        '        <a class="btn btn-primary s js_close" href="javascript:;">确认</a>' +
        '    </div>' +
        '</div>';

    self.$popupWrap.html(html);
    self.showWrap();

    // setTimeout(function() {
    //   self.hideWrap();
    // }, 3000)
};

Dialog.prototype.confirm = function (msg, okCallback) {
    msg = msg || '操作无效';

    var self = this;
    var html = '<div class="dlg show s">' +
        '    <a href="javascript:" class="dlg-close js_close">×</a>' +
        '    <div class="dlg-hd">' +
        '        <h3 class="tit">系统提示</h3>' +
        '    </div>' +
        '    <form class="dlg-cnt" id="brokerForm">' +
        '        <div class="inner">' + msg + '</div>' +
        '        <div class="promt-wp">' +
        '            <div class="promt-txt">请输入机器授权字段，验证操作权限</div>' +
        '            <input type="text" class="l" id="passwd">' +
        '        </div>' +
        '    </form>' +
        '    <div class="dlg-btns">' +
        '        <a class="btn btn-primary s js_ok" href="javascript:;">确认</a>' +
        '        <a class="btn btn-white s js_close" href="javascript:;">取消</a>' +
        '    </div>' +
        '</div>';

    self.$popupWrap.html(html);
    self.showWrap();

    var initEvent = function () {
        self.$popupWrap.off('click.js_ok').on('click.js_ok', '.js_ok', function () {
            var formData = $('#brokerForm').serialize();
            var passwd = $('#passwd').val();
            okCallback && okCallback(passwd, formData);
        });
    };

    initEvent();

};

Dialog.prototype.confirmBrokerInfo = function (type, brokerIds, callback) {
    if (brokerIds.length < 1) {
        this.showTips('请选择需要操作的broker');
        return false;
    }
    callback = callback || {};
    var self = this;
    var types = {
        'reload': {
            'text': '重载',
            'api': 'admin_reload_broker_configure'
        },
        'online': {
            'text': '上线',
            'api': 'admin_online_broker_configure'
        },
        'offline': {
            'text': '下线',
            'api': 'admin_offline_broker_configure'
        },
        'delete': {
            'text': '删除',
            'api': 'admin_delete_broker_configure'
        }
    };

    var msg = '确认<span class="hl">' + types[type].text + '</span>以下的broker吗？';
    var html = '<div class="dlg show l">' +
        '    <a href="javascript:" class="dlg-close js_close">×</a>' +
        '    <div class="dlg-hd">' +
        '        <h3 class="tit">系统提示</h3>' +
        '    </div>' +
        '    <div class="dlg-cnt">' +
        '        <div class="inner">' + msg + '</div>' +
        '        <!-- 表 -->' +
        '        <div class="table-wp">' +
        '            <div class="tit">' +
        '                <table id="popup_brokerTable">' +
        '                    <thead><tr>' +
        '                        <th>Broker</th>' +
        '                        <th>当前管理状态</th>' +
        '                        <th>当前运行状态</th>' +
        '                        <th>当前运行子状态</th>' +
        '                        <th>可发布</th>' +
        '                        <th>可订阅</th>' +
        '                    </tr></thead>' +
        '                </table>' +
        '            </div>' +
        '        </div>' +
        '        <!-- 确定 -->' +
        '        <div class="promt-wp">' +
        '            <div class="promt-txt">请输入机器授权字段，验证操作权限</div>' +
        '            <input type="text" class="xl" id="passwd">' +
        '        </div>' +
        '    </div>' +
        '    <div class="dlg-btns">' +
        '        <a class="btn btn-primary m js_ok" href="javascript:;">确认</a>' +
        '        <a class="btn btn-white m js_close" href="javascript:;">取消</a>' +
        '    </div>' +
        '</div>';

    var initEvent = function () {
        self.$popupWrap.off('click.js_ok').on('click.js_ok', '.js_ok', function () {
            var passwd = $('#passwd').val();
            $.getJSON(G_CONFIG.HOST + '?type=op_modify&method=' + types[type].api
                + '&modifyUser=webapi&confModAuthToken=' + passwd + '&callback=?&brokerId='
                + brokerIds.join(',')).done(function (data) {
                if (data.errCode === 0) {
                    self.showTips('操作成功');
                    callback.ok && callback.ok();
                    self.successHook && self.successHook();
                    self.successHook && self.successHook();
                } else {
                    self.showTips(data.errMsg);
                }
            });
        });
    };

    var initTable = function () {
        $('#popup_brokerTable').DataTable({
            "ordering": false,
            scrollY: '220px',
            scrollCollapse: true,
            paging: false,
            "ajax": {
                "url": G_CONFIG.HOST
                + "?type=op_query&method=admin_query_broker_run_status&brokerId="
                + brokerIds.join(','),
                "dataType": "jsonp"
            },
            "columns": [{
                "data": null,
                "render": function (data, type, full, meta) {
                    return full.brokerId + '#' + full.brokerIp
                        + ':' + full.brokerPort;
                }
            }, {
                "data": "manageStatus"
            }, {
                "data": "runStatus"
            }, {
                "data": "subStatus"
            }, {
                "data": "acceptPublish"
            }, {
                "data": "acceptSubscribe"
            }],
            language: languageVal,
            'pagingType': "full_numbers",
            "dom": '<"scroll-wp"rt><"pg-wp"ilp>'
        });
    };

    self.$popupWrap.html(html);
    self.showWrap();

    initEvent();
    initTable();
};

Dialog.prototype.addBrokerInfo = function (type, brokerId, callback) {
    callback = callback || {};
    var self = this;
    var types = {
        'add': {
            'text': '新增',
            'api': 'admin_add_broker_configure&createUser=webapi'
        },
        'mod': {
            'text': '修改',
            'api': 'admin_update_broker_configure&modifyUser=webapi'
        }
    };
    var data = {
        'brokerId': '0',
        'brokerIp': '',
        'brokerPort': '8123',
        'deletePolicy': 'delete,168h',
        'numPartitions': '3',
        'unflushThreshold': '1000',
        'unflushInterval': '10000',
        'acceptPublish': 'true',
        'acceptSubscribe': 'true'
    };

    var renderHtml = function (title, data) {
        var html = '<div class="dlg show l">' +
            '            <a href="javascript:" class="dlg-close js_close">×</a>' +
            '            <div class="dlg-hd">' +
            '                <h3 class="tit">' + title + ' Broker</h3>' +
            '            </div>' +
            '            <form class="dlg-cnt" id="brokerForm">' +
            '                <div class="form-wp sep-2">' +
            '                    <div class="row">' +
            '                        <div class="tit">BrokerID</div>' +
            '                        <div class="cnt">' +
            '                            <input type="text" class="m" value="'
            + data.brokerId + '" name="brokerId">' +
            '                        </div>' +
            '                    </div>' +
            '                    <div class="row">' +
            '                        <div class="tit">BrokerIP</div>' +
            '                        <div class="cnt">' +
            '                            <input type="text" class="m" value="'
            + data.brokerIp + '" name="brokerIp">' +
            '                        </div>' +
            '                    </div>' +
            '                    <div class="row">' +
            '                        <div class="tit">unflushThreshold</div>' +
            '                        <div class="cnt">' +
            '                            <input type="text" class="m" value="'
            + data.unflushThreshold + '" name="unflushThreshold">' +
            '                        </div>' +
            '                    </div>' +
            '                    <div class="row">' +
            '                        <div class="tit">acceptPublish</div>' +
            '                        <div class="cnt">' +
            '                            <input type="text" class="m" value="'
            + data.acceptPublish + '" name="acceptPublish">' +
            '                        </div>' +
            '                    </div>' +
            '                </div>' +
            '                <div class="form-wp sep-2">' +
            '                    <div class="row">' +
            '                        <div class="tit">numPartitions</div>' +
            '                        <div class="cnt">' +
            '                            <input type="text" class="m" value="'
            + data.numPartitions + '" name="numPartitions">' +
            '                        </div>' +
            '                    </div>' +
            '                    <div class="row">' +
            '                        <div class="tit">brokerPort</div>' +
            '                        <div class="cnt">' +
            '                            <input type="text" class="m" value="'
            + data.brokerPort + '" name="brokerPort">' +
            '                        </div>' +
            '                    </div>' +
            '                    <div class="row">' +
            '                        <div class="tit">deletePolicy</div>' +
            '                        <div class="cnt">' +
            '                            <input type="text" class="m" value="'
            + data.deletePolicy + '" name="deletePolicy">' +
            '                        </div>' +
            '                    </div>' +
            '                    <div class="row">' +
            '                        <div class="tit">unflushInterval</div>' +
            '                        <div class="cnt">' +
            '                            <input type="text" class="m" value="'
            + data.unflushInterval + '" name="unflushInterval">' +
            '                        </div>' +
            '                    </div>' +
            '                    <div class="row">' +
            '                        <div class="tit">acceptSubscribe</div>' +
            '                        <div class="cnt">' +
            '                            <input type="text" class="m" value="'
            + data.acceptSubscribe + '" name="acceptSubscribe">' +
            '                        </div>' +
            '                    </div>' +
            '                </div>' +
            '            </form>' +
            '            <div class="dlg-cnt">' +
            '               <div class="promt-wp">' +
            '               <div class="promt-txt">请输入机器授权字段，验证操作权限</div>' +
            '                 <input type="text" class="xl" id="passwd">' +
            '               </div>' +
            '            </div>' +
            '            <div class="dlg-btns">' +
            '                <a class="btn btn-primary m js_ok" href="javascript:;">确认</a>' +
            '                <a class="btn btn-white m js_close" href="javascript:;">取消</a>'
            +
            '            </div>' +
            '        </div>';
        self.$popupWrap.html(html);
        self.showWrap();
    };

    var initEvent = function () {
        self.$popupWrap.off('click.js_ok').on('click.js_ok', '.js_ok', function () {
            var formData = $('#brokerForm').serialize();
            var passwd = $('#passwd').val();
            $.getJSON(G_CONFIG.HOST + '?type=op_modify&method=' + types[type].api + '&' + formData
                + '&confModAuthToken=' + passwd + '&callback=?').done(function (data) {
                if (data.errCode === 0) {
                    self.showTips('操作成功');
                    callback.ok && callback.ok();
                    self.successHook && self.successHook();
                } else {
                    self.showTips(data.errMsg);
                }
            });
        });
    };

    if (type === 'mod') {
        var url = G_CONFIG.HOST + "?type=op_query&method=admin_query_broker_configure&brokerId="
            + brokerId + "&callback=?";
        $.getJSON(url)
            .done(function (res) {
                if (res.errCode === 0) {
                    var data = res.data[0];
                    renderHtml(types[type].text, data);
                } else {
                    self.showTips('无法获取broker信息');
                }
            });
    } else {
        renderHtml(types[type].text, data);
    }

    initEvent();
};

Dialog.prototype.confirmTopicInfo = function (type, topicName, selectedBrokerid, callback) {
    callback = callback || {};
    selectedBrokerid = selectedBrokerid || [];
    var self = this;
    var types = {
        'delete': {
            'text': '删除',
            'api': 'admin_delete_topic_info'
        },
        'allowPub': {
            'text': '允许可发布',
            'api': 'admin_modify_topic_info&modifyUser=webapi&acceptPublish=true'
        },
        'disPub': {
            'text': '禁止可发布',
            'api': 'admin_modify_topic_info&modifyUser=webapi&acceptPublish=false'
        },
        'allowSub': {
            'text': '允许可订阅',
            'api': 'admin_modify_topic_info&modifyUser=webapi&acceptSubscribe=true'
        },
        'disSub': {
            'text': '禁止可订阅',
            'api': 'admin_modify_topic_info&modifyUser=webapi&acceptSubscribe=false'
        }
    };

    var msg = '确认 <span class="hl">' + types[type].text
        + '</span> 以下broker列表的 topic (<span class="hl" style="color:red">' + topicName
        + '</span>) 吗？';
    var html = '<div class="dlg show l">' +
        '    <a href="javascript:" class="dlg-close js_close">×</a>' +
        '    <div class="dlg-hd">' +
        '        <h3 class="tit">系统提示</h3>' +
        '    </div>' +
        '    <div class="dlg-cnt">' +
        '        <div class="inner">' + msg + '</div>' +
        '        <!-- 表 -->' +
        '        <div class="table-wp">' +
        '            <div class="tit">' +
        '                <table id="popup_brokerTable">' +
        '                    <thead><tr>' +
        '                        <th><span class="checkbox js_all_check"><i class="icon icon_check"></i><label for=""></label><input type="checkbox" checked=""></span></th>'
        +
        '                        <th>Broker</th>' +
        '                        <th>分区数</th>' +
        '                        <th>当前运行状态</th>' +
        '                        <th>可发布</th>' +
        '                        <th>可订阅</th>' +
        '                    </tr></thead>' +
        '                </table>' +
        '            </div>' +
        '        </div>' +
        '        <!-- 确定 -->' +
        '        <div class="promt-wp">' +
        '            <div class="promt-txt">请输入机器授权字段，验证操作权限</div>' +
        '            <input type="text" class="xl" id="passwd">' +
        '        </div>' +
        '    </div>' +
        '    <div class="dlg-btns">' +
        '        <a class="btn btn-primary m js_ok" href="javascript:;">确认</a>' +
        '        <a class="btn btn-white m js_close" href="javascript:;">取消</a>' +
        '    </div>' +
        '</div>';

    var initEvent = function () {
        // 单选绑定
        $popup_brokerTable =
            $('#popup_brokerTable').off('click.js_check')
                .on('click.js_check', '.js_check', function () {
                    var $this = $(this);
                    var brokerId = $this.attr('data-id');
                    var idx = $.inArray(brokerId, selectedBrokerid);

                    var checkedClass = 'checked';

                    if ($this.hasClass(checkedClass)) {
                        $this.removeClass(checkedClass);
                        // Remove from the 'open' array
                        selectedBrokerid.splice(idx, 1);
                    } else {
                        $this.addClass(checkedClass);

                        // Add to the 'open' array
                        if (idx === -1) {
                            selectedBrokerid.push(brokerId);
                        }
                    }
                });
        // 多选绑定
        self.$popupWrap.off('click.js_all_check')
            .on('click.js_all_check', '.js_all_check', function () {
                var $this = $(this);
                var $checkBoxs = $popup_brokerTable.find('.js_check');

                var checkedClass = 'checked';
                var isCheck = $this.hasClass(checkedClass);
                if (isCheck) {
                    $this.removeClass(checkedClass);
                } else {
                    $this.addClass(checkedClass);
                }

                $checkBoxs.each(function () {
                    var $that = $(this);
                    var brokerId = $that.attr('data-id');
                    var idx = $.inArray(brokerId, selectedBrokerid);
                    if (isCheck) {
                        $that.removeClass(checkedClass);
                        selectedBrokerid.splice(idx, 1);
                    } else {
                        $that.addClass(checkedClass);
                        if (idx === -1) {
                            selectedBrokerid.push(brokerId);
                        }
                    }
                });

            });
        self.$popupWrap.off('click.js_ok').on('click.js_ok', '.js_ok', function () {
            if (selectedBrokerid.length < 1) {
                self.showTips('请选择需要操作的broker');
                return false;
            }
            var passwd = $('#passwd').val();
            $.getJSON(G_CONFIG.HOST + '?type=op_modify&method=' + types[type].api
                + '&modifyUser=webapi&confModAuthToken=' + passwd + '&callback=?&topicName='
                + topicName + '&brokerId=' + selectedBrokerid.join(','))
                .done(function (data) {
                    if (data.errCode === 0) {
                        self.showTips('操作成功');
                        callback.ok && callback.ok();
                        self.successHook && self.successHook();
                    } else {
                        self.showTips(data.errMsg);
                    }
                });
        });
    };

    var initTableInfo = function (topicName, brokerIds) {
        var url = G_CONFIG.HOST + "?type=op_query&method=admin_query_topic_info&topicName="
            + topicName + "&brokerId=" + brokerIds.join(',') + "&callback=?";
        $.getJSON(url)
            .done(function (res) {
                if (res.errCode === 0) {
                    var data = res.data[0];
                    initTable(data.topicInfo);
                }
            });
    }

    var initTable = function (dataSet) {
        var translation2Boolean = {
            'true': '是',
            'false': '否',
            '-': '-'
        };

        $('#popup_brokerTable').DataTable({
            "ordering": false,
            scrollY: '220px',
            scrollCollapse: true,
            paging: false,
            data: dataSet,
            "columns": [{
                "data": null,
                "orderable": false,
                "render": function (data, type, full, meta) {
                    return '<span class="checkbox  js_check" data-id="'
                        + full.brokerId + '">' +
                        '    <i class="icon icon_check"></i>' +
                        '    <label for=""></label>' +
                        '    <input type="checkbox" checked>' +
                        '</span>';
                }
            }, {
                "data": "brokerId",
                "render": function (data, type, full, meta) {
                    return full.brokerId + '#' + full.brokerIp
                        + ':' + full.brokerPort;
                }
            }, {
                "data": "runInfo.numPartitions"
            }, {
                "data": "runInfo.brokerManageStatus"
            }, {
                "data": "runInfo.acceptPublish",
                "orderable": false,
                "render": function (data, type, full, meta) {
                    return translation2Boolean[data];
                }
            }, {
                "data": "runInfo.acceptSubscribe",
                "orderable": false,
                "render": function (data, type, full, meta) {
                    return translation2Boolean[data];
                }
            }],
            language: languageVal,
            'pagingType': "full_numbers",
            "dom": '<"scroll-wp"rt><"pg-wp"ilp>',
            "initComplete": function () {
                if (selectedBrokerid.length > 0) {
                    self.$popupWrap.find('.js_all_check').click();
                }
            }
        });
    };

    self.$popupWrap.html(html);
    self.showWrap();

    initEvent();
    initTableInfo(topicName, selectedBrokerid);

};

Dialog.prototype.addTopicInfo = function (type, topicName, data) {
    var self = this;
    var types = {
        'add': {
            'text': '新增',
            'api': 'admin_add_new_topic_record&createUser=webapi'
        },
        'mod': {
            'text': '修改',
            'api': 'admin_modify_topic_info&modifyUser=webapi'
        }
    };
    data = data || {
            'topicName': '',
            'deletePolicy': 'delete,168h',
            'numPartitions': '3',
            'unflushThreshold': '1000',
            'unflushInterval': '10000',
            'acceptPublish': 'true',
            'acceptSubscribe': 'true'
        };
    topicName = topicName || '';

    var renderHtml = function (title, data) {
        var html = '<div class="dlg show l">' +
            '            <a href="javascript:" class="dlg-close js_close">×</a>' +
            '            <div class="dlg-hd">' +
            '                <h3 class="tit">' + title + ' Topic</h3>' +
            '            </div>' +
            '            <form class="dlg-cnt" id="brokerForm">' +
            '                <div class="form-wp sep-2">' +
            '                   <div class="row">' +
            '                        <div class="tit">topicName</div>' +
            '                        <div class="cnt">' +
            '                            <input type="text" class="m" value="'
            + data.topicName + '" name="topicName">' +
            '                        </div>' +
            '                    </div>' +
            '                    <div class="row">' +
            '                        <div class="tit">unflushThreshold</div>' +
            '                        <div class="cnt">' +
            '                            <input type="text" class="m" value="'
            + data.unflushThreshold + '" name="unflushThreshold">' +
            '                        </div>' +
            '                    </div>' +
            '                    <div class="row">' +
            '                        <div class="tit">acceptPublish</div>' +
            '                        <div class="cnt">' +
            '                            <input type="text" class="m" value="'
            + data.acceptPublish + '" name="acceptPublish">' +
            '                        </div>' +
            '                    </div>' +

            '                </div>' +
            '                <div class="form-wp sep-2">' +
            '                   <div class="row">' +
            '                        <div class="tit">numPartitions</div>' +
            '                        <div class="cnt">' +
            '                            <input type="text" class="m" value="'
            + data.numPartitions + '" name="numPartitions">' +
            '                        </div>' +
            '                    </div>' +
            '                    <div class="row">' +
            '                        <div class="tit">deletePolicy</div>' +
            '                        <div class="cnt">' +
            '                            <input type="text" class="m" value="'
            + data.deletePolicy + '" name="deletePolicy">' +
            '                        </div>' +
            '                    </div>' +
            '                    <div class="row">' +
            '                        <div class="tit">unflushInterval</div>' +
            '                        <div class="cnt">' +
            '                            <input type="text" class="m" value="'
            + data.unflushInterval + '" name="unflushInterval">' +
            '                        </div>' +
            '                    </div>' +
            '                    <div class="row">' +
            '                        <div class="tit">acceptSubscribe</div>' +
            '                        <div class="cnt">' +
            '                            <input type="text" class="m" value="'
            + data.acceptSubscribe + '" name="acceptSubscribe">' +
            '                        </div>' +
            '                    </div>' +
            '                </div>' +
            '            </form>' +
            '            <div class="dlg-btns">' +
            '                <a class="btn btn-primary m js_ok" href="javascript:;">确认</a>' +
            '                <a class="btn btn-white m js_close" href="javascript:;">取消</a>'
            +
            '            </div>' +
            '        </div>';
        self.$popupWrap.html(html);
        self.showWrap();
    };

    var initEvent = function () {
        self.$popupWrap.off('click.js_ok').on('click.js_ok', '.js_ok', function () {
            var formData = $.param($('#brokerForm').serializeArray());
            console.log(formData)
            var passwd = $('#passwd').val();
            self.confirmBroker2Topic(type, topicName, formData);
        });
    };

    renderHtml(types[type].text, data);

    initEvent();
};

Dialog.prototype.confirmBroker2Topic = function (type, topicName, formData) {
    var brokerIds = brokerIds || [];
    var selectedBrokerid = [];
    var self = this;
    var types = {
        'add': {
            'text': '新增',
            'api': 'admin_add_new_topic_record&createUser=webapi'
        },
        'mod': {
            'text': '修改',
            'api': 'admin_modify_topic_info&modifyUser=webapi'
        }
    };

    var msg = '确认 <span class="hl">' + types[type].text + '</span> 以下broker列表的 topic 吗？';
    var html = '<div class="dlg show l">' +
        '    <a href="javascript:" class="dlg-close js_close">×</a>' +
        '    <div class="dlg-hd">' +
        '        <h3 class="tit">系统提示</h3>' +
        '    </div>' +
        '    <div class="dlg-cnt">' +
        '        <div class="inner">' + msg + '</div>' +
        '        <div class="table-wp">' +
        '                <div class="tit">' +
        '                    <table id="popup_brokerTable">' +
        '                        <thead><tr>' +
        '                            <th><span class="checkbox js_all_check"><i class="icon icon_check"></i><label for=""></label><input type="checkbox" checked=""></span></th>'
        +
        '                            <th>Broker</th>' +
        '                            <th>实例数</th>' +
        '                            <th>当前运行状态</th>' +
        '                            <th>可发布</th>' +
        '                            <th>可订阅</th>' +
        '                        </tr></thead>' +
        '                    </table>' +
        '                </div>' +
        '        </div>' +
        '        <!-- 确定 -->' +
        '        <div class="promt-wp" >' +
        '            <div class="promt-txt">请输入机器授权字段，验证操作权限</div>' +
        '            <input type="text" class="xl" id="passwd">' +
        '        </div>' +
        '    </div>' +
        '    <div class="dlg-btns">' +
        '        <a class="btn btn-primary m js_ok" href="javascript:;">确认</a>' +
        '        <a class="btn btn-white m js_close" href="javascript:;">取消</a>' +
        '    </div>' +
        '</div>';

    var initEvent = function () {
        // 单选绑定
        $popup_brokerTable =
            $('#popup_brokerTable').off('click.js_check')
                .on('click.js_check', '.js_check', function () {
                    var $this = $(this);
                    var brokerId = $this.attr('data-id');
                    var idx = $.inArray(brokerId, selectedBrokerid);

                    var checkedClass = 'checked';

                    if ($this.hasClass(checkedClass)) {
                        $this.removeClass(checkedClass);
                        // Remove from the 'open' array
                        selectedBrokerid.splice(idx, 1);
                    } else {
                        $this.addClass(checkedClass);

                        // Add to the 'open' array
                        if (idx === -1) {
                            selectedBrokerid.push(brokerId);
                        }
                    }
                });
        // 多选绑定
        self.$popupWrap.off('click.js_all_check')
            .on('click.js_all_check', '.js_all_check', function () {
                var $this = $(this);
                var $checkBoxs = $popup_brokerTable.find('.js_check');

                var checkedClass = 'checked';
                var isCheck = $this.hasClass(checkedClass);
                if (isCheck) {
                    $this.removeClass(checkedClass);
                } else {
                    $this.addClass(checkedClass);
                }

                $checkBoxs.each(function () {
                    var $that = $(this);
                    var brokerId = $that.attr('data-id');
                    var idx = $.inArray(brokerId, selectedBrokerid);
                    if (isCheck) {
                        $that.removeClass(checkedClass);
                        selectedBrokerid.splice(idx, 1);
                    } else {
                        $that.addClass(checkedClass);
                        if (idx === -1) {
                            selectedBrokerid.push(brokerId);
                        }
                    }
                });

            });
        // 确定按钮绑定
        self.$popupWrap.off('click.js_ok').on('click.js_ok', '.js_ok', function () {
            console.log(selectedBrokerid)
            var passwd = $('#passwd').val();
            $.getJSON(G_CONFIG.HOST + '?type=op_modify&method=' + types[type].api + '&' + formData
                + '&brokerId=' + selectedBrokerid.join(',') + '&confModAuthToken=' + passwd
                + '&callback=?').done(function (data) {
                if (data.errCode === 0) {
                    self.showTips('操作成功');
                    self.successHook && self.successHook();
                } else {
                    self.showTips(data.errMsg);
                }
            });
        });
    };

    var initTableInfo = function (topicName, brokerIds) {
        var url = G_CONFIG.HOST + "?type=op_query&method=admin_query_broker_topic_config_info&topicName="
            + topicName + "&brokerId=" + brokerIds.join(',') + "&callback=?";
        $.getJSON(url)
            .done(function (res) {
                if (res.errCode === 0) {
                    var data = res.data;
                    initTable(data);
                }
            });
    }

    var initTable = function (dataSet) {
        var translation2Boolean = {
            'true': '是',
            'false': '否',
            '-': '-'
        };

        $('#popup_brokerTable').DataTable({
            "ordering": false,
            scrollY: '220px',
            scrollCollapse: true,
            paging: false,
            data: dataSet,
            "columns": [{
                "data": null,
                "orderable": false,
                "render": function (data, type, full, meta) {
                    return '<span class="checkbox  js_check" data-id="'
                        + full.brokerId + '">' +
                        '    <i class="icon icon_check"></i>' +
                        '    <label for=""></label>' +
                        '    <input type="checkbox" checked>' +
                        '</span>';
                }
            }, {
                "data": "brokerId",
                "render": function (data, type, full, meta) {
                    return full.brokerId + '#' + full.brokerIp
                        + ':' + full.brokerPort;
                }
            }, {
                "data": "storeTotalCfgCnt"
            }, {
                "data": "manageStatus"
            }, {
                "data": "acceptPublish",
                "orderable": false,
                "render": function (data, type, full, meta) {
                    return translation2Boolean[data];
                }
            }, {
                "data": "acceptSubscribe",
                "orderable": false,
                "render": function (data, type, full, meta) {
                    return translation2Boolean[data];
                }
            }],
            language: languageVal,
            'pagingType': "full_numbers",
            "dom": '<"scroll-wp"rt><"pg-wp"ilp>'
        });
    };

    self.$popupWrap.html(html);
    self.showWrap();

    initEvent();
    initTableInfo(topicName, brokerIds);
};

Dialog.prototype.addConsumerGroup = function (type, topicName) {
    var self = this;
    var types = {
        'add': {
            'text': '新增',
            'api': 'admin_add_authorized_consumergroup_info&createUser=webapi'
        },
        'mod': {
            'text': '修改',
            'api': 'admin_update_broker_configure&modifyUser=webapi'
        }
    };

    var data = {
        groupName: ''
    };

    var renderHtml = function (title, data) {
        var html = '<div class="dlg show l">' +
            '            <a href="javascript:" class="dlg-close js_close">×</a>' +
            '            <div class="dlg-hd">' +
            '                <h3 class="tit">' + title + ' 授权消费组</h3>' +
            '            </div>' +
            '            <form class="dlg-cnt" id="brokerForm">' +
            '                <div class="form-wp sep-2">' +
            '                    <div class="row">' +
            '                        <div class="tit">授权消费组</div>' +
            '                        <div class="cnt">' +
            '                            <input type="text" class="m" value="'
            + data.groupName + '" name="groupName">' +
            '                        </div>' +
            '                    </div>' +
            '                </div>' +
            '            </form>' +
            '            <div class="dlg-cnt">' +
            '               <div class="promt-wp">' +
            '               <div class="promt-txt">请输入机器授权字段，验证操作权限</div>' +
            '                 <input type="text" class="xl" id="passwd">' +
            '               </div>' +
            '            </div>' +
            '            <div class="dlg-btns">' +
            '                <a class="btn btn-primary m js_ok" href="javascript:;">确认</a>' +
            '                <a class="btn btn-white m js_close" href="javascript:;">取消</a>'
            +
            '            </div>' +
            '        </div>';
        self.$popupWrap.html(html);
        self.showWrap();
    };

    var initEvent = function () {
        self.$popupWrap.off('click.js_ok').on('click.js_ok', '.js_ok', function () {
            var formData = $('#brokerForm').serialize();
            var passwd = $('#passwd').val();
            $.getJSON(G_CONFIG.HOST + '?type=op_modify&method=' + types[type].api + '&' + formData
                + '&topicName=' + topicName + '&confModAuthToken=' + passwd + '&callback=?')
                .done(function (data) {
                    if (data.errCode === 0) {
                        self.showTips('操作成功');
                        self.successHook && self.successHook();
                    } else {
                        self.showTips(data.errMsg);
                    }
                });
        });
    };

    renderHtml(types[type].text, data);

    initEvent();
};

// CheckBox简单封装
var CheckBox = function () {
    this.init();
};

CheckBox.prototype.init = function () {

};

CheckBox.prototype.process = function (type, $target, dialogInstance, ext, callback) {
    callback = callback || {};
    var checkedClass = 'checked';
    var state = $target.hasClass(checkedClass);
    var stateStr = ['true', 'false'][+state];
    if (type === 'setTopicAuth') {
        var translation2Boolean = {
            'true': '启动',
            'false': '屏蔽'
        };
    } else {
        var translation2Boolean = {
            'true': '允许',
            'false': '禁止'
        };
    }

    var types = {
        'sub': {
            'text': '订阅broker',
            'api': 'admin_set_broker_read_or_write&acceptSubscribe=' + stateStr + '&brokerId='
            + ext
        },
        'pub': {
            'text': '发布broker',
            'api': 'admin_set_broker_read_or_write&acceptPublish=' + stateStr + '&brokerId=' + ext
        },
        'setTopicAuth': {
            'text': 'topic的消费组授权控制',
            'api': 'admin_set_topic_authorize_control&createUser=webapi&isEnable=' + stateStr
            + '&topicName=' + ext
        }
    };

    dialogInstance.confirm(
        '确认<span class="hl">' + translation2Boolean[stateStr] + '' + types[type].text + '</span>吗？',
        function (passwd, formData) {
            $.getJSON(G_CONFIG.HOST + '?type=op_modify&method=' + types[type].api
                + '&modifyUser=webapi&confModAuthToken=' + passwd + '&callback=?')
                .done(function (data) {
                    if (data.errCode === 0) {
                        dialogInstance.showTips('操作成功');
                        if (state) {
                            $target.removeClass(checkedClass);
                        } else {
                            $target.addClass(checkedClass);
                        }
                        callback.ok && callback.ok();
                        dialogInstance.successHook && dialogInstance.successHook();
                    } else {
                        dialogInstance.showTips(data.errMsg);
                    }
                });
        });
};

CheckBox.prototype.processTopic = function (type, $target, dialogInstance, ext, callback) {
    callback = callback || {};
    var checkedClass = 'checked';
    var state = $target.hasClass(checkedClass);
    var stateStr = ['true', 'false'][+state];
    if (type === 'setTopicAuth') {
        var translation2Boolean = {
            'true': '启动',
            'false': '屏蔽'
        };
    } else {
        var translation2Boolean = {
            'true': '允许',
            'false': '禁止'
        };
    }

    var types = {
        'sub': {
            'text': '订阅broker',
            'api': 'admin_set_broker_read_or_write&acceptSubscribe=' + stateStr + '&brokerId='
            + ext
        },
        'pub': {
            'text': '发布broker',
            'api': 'admin_set_broker_read_or_write&acceptPublish=' + stateStr + '&brokerId=' + ext
        },
        'setTopicAuth': {
            'text': 'topic的消费组授权控制',
            'api': 'admin_set_topic_authorize_control&createUser=webapi&isEnable=' + stateStr
            + '&topicName=' + ext
        }
    };

    dialogInstance.confirm(
        '确认<span class="hl">' + translation2Boolean[stateStr] + '' + types[type].text + '</span>吗？',
        function (passwd, formData) {
            $.getJSON(G_CONFIG.HOST + '?type=op_modify&method=' + types[type].api
                + '&modifyUser=webapi&confModAuthToken=' + passwd + '&callback=?')
                .done(function (data) {
                    if (data.errCode === 0) {
                        dialogInstance.showTips('操作成功');
                        if (state) {
                            $target.removeClass(checkedClass);
                        } else {
                            $target.addClass(checkedClass);
                        }
                        callback.ok && callback.ok();
                        self.successHook && self.successHook();
                    } else {
                        dialogInstance.showTips(data.errMsg);
                    }
                });
        });

    selectedBrokerid = selectedBrokerid || [];
    var self = this;
    var types = {
        'delete': {
            'text': '删除',
            'api': 'admin_delete_topic_info'
        },
        'pub': {
            'text': '允许可发布',
            'api': 'admin_modify_topic_info&modifyUser=webapi&acceptPublish='
        },
        'sub': {
            'text': '删除',
            'api': 'admin_delete_topic_info'
        }
    };

    var msg = '确认 <span class="hl">' + types[type].text
        + '</span> 以下broker列表的 topic (<span class="hl" style="color:red">' + topicName
        + '</span>) 吗？';
    var html = '<div class="dlg show l">' +
        '    <a href="javascript:" class="dlg-close js_close">×</a>' +
        '    <div class="dlg-hd">' +
        '        <h3 class="tit">系统提示</h3>' +
        '    </div>' +
        '    <div class="dlg-cnt">' +
        '        <div class="inner">' + msg + '</div>' +
        '        <!-- 表 -->' +
        '        <div class="table-wp">' +
        '            <div class="tit">' +
        '                <table id="popup_brokerTable">' +
        '                    <thead><tr>' +
        '                        <th><span class="checkbox js_all_check"><i class="icon icon_check"></i><label for=""></label><input type="checkbox" checked=""></span></th>'
        +
        '                        <th>Broker</th>' +
        '                        <th>分区数</th>' +
        '                        <th>当前运行状态</th>' +
        '                        <th>可发布</th>' +
        '                        <th>可订阅</th>' +
        '                    </tr></thead>' +
        '                </table>' +
        '            </div>' +
        '        </div>' +
        '        <!-- 确定 -->' +
        '        <div class="promt-wp">' +
        '            <div class="promt-txt">请输入机器授权字段，验证操作权限</div>' +
        '            <input type="text" class="xl" id="passwd">' +
        '        </div>' +
        '    </div>' +
        '    <div class="dlg-btns">' +
        '        <a class="btn btn-primary m js_ok" href="javascript:;">确认</a>' +
        '        <a class="btn btn-white m js_close" href="javascript:;">取消</a>' +
        '    </div>' +
        '</div>';

    var initEvent = function () {
        // 单选绑定
        $popup_brokerTable =
            $('#popup_brokerTable').off('click.js_check')
                .on('click.js_check', '.js_check', function () {
                    var $this = $(this);
                    var brokerId = $this.attr('data-id');
                    var idx = $.inArray(brokerId, selectedBrokerid);

                    var checkedClass = 'checked';

                    if ($this.hasClass(checkedClass)) {
                        $this.removeClass(checkedClass);
                        // Remove from the 'open' array
                        selectedBrokerid.splice(idx, 1);
                    } else {
                        $this.addClass(checkedClass);

                        // Add to the 'open' array
                        if (idx === -1) {
                            selectedBrokerid.push(brokerId);
                        }
                    }
                });
        // 多选绑定
        self.$popupWrap.off('click.js_all_check')
            .on('click.js_all_check', '.js_all_check', function () {
                var $this = $(this);
                var $checkBoxs = $popup_brokerTable.find('.js_check');

                var checkedClass = 'checked';
                var isCheck = $this.hasClass(checkedClass);
                if (isCheck) {
                    $this.removeClass(checkedClass);
                } else {
                    $this.addClass(checkedClass);
                }

                $checkBoxs.each(function () {
                    var $that = $(this);
                    var brokerId = $that.attr('data-id');
                    var idx = $.inArray(brokerId, selectedBrokerid);
                    if (isCheck) {
                        $that.removeClass(checkedClass);
                        selectedBrokerid.splice(idx, 1);
                    } else {
                        $that.addClass(checkedClass);
                        if (idx === -1) {
                            selectedBrokerid.push(brokerId);
                        }
                    }
                });

            });
        self.$popupWrap.off('click.js_ok').on('click.js_ok', '.js_ok', function () {
            if (selectedBrokerid.length < 1) {
                self.showTips('请选择需要操作的broker');
                return false;
            }
            var passwd = $('#passwd').val();
            $.getJSON(G_CONFIG.HOST + '?type=op_modify&method=' + types[type].api
                + '&modifyUser=webapi&confModAuthToken=' + passwd + '&callback=?&topicName='
                + topicName + '&brokerId=' + selectedBrokerid.join(','))
                .done(function (data) {
                    if (data.errCode === 0) {
                        self.showTips('操作成功');
                    } else {
                        self.showTips(data.errMsg);
                    }
                });
        });
    };

    var initTableInfo = function (topicName, brokerIds) {
        var url = G_CONFIG.HOST + "?type=op_query&method=admin_query_topic_info&topicName="
            + topicName + "&brokerId=" + brokerIds.join(',') + "&callback=?";
        $.getJSON(url)
            .done(function (res) {
                if (res.errCode === 0) {
                    var data = res.data[0];
                    initTable(data.topicInfo);
                }
            });
    }

    var initTable = function (dataSet) {
        var translation2Boolean = {
            'true': '是',
            'false': '否',
            '-': '-'
        };

        $('#popup_brokerTable').DataTable({
            "ordering": false,
            scrollY: '220px',
            scrollCollapse: true,
            paging: false,
            data: dataSet,
            "columns": [{
                "data": null,
                "orderable": false,
                "render": function (data, type, full, meta) {
                    return '<span class="checkbox  js_check" data-id="'
                        + full.brokerId + '">' +
                        '    <i class="icon icon_check"></i>' +
                        '    <label for=""></label>' +
                        '    <input type="checkbox" checked>' +
                        '</span>';
                }
            }, {
                "data": "brokerId",
                "render": function (data, type, full, meta) {
                    return full.brokerId + '#' + full.brokerIp
                        + ':' + full.brokerPort;
                }
            }, {
                "data": "runInfo.numPartitions"
            }, {
                "data": "runInfo.brokerManageStatus"
            }, {
                "data": "runInfo.acceptPublish",
                "orderable": false,
                "render": function (data, type, full, meta) {
                    return translation2Boolean[data];
                }
            }, {
                "data": "runInfo.acceptSubscribe",
                "orderable": false,
                "render": function (data, type, full, meta) {
                    return translation2Boolean[data];
                }
            }],
            language: languageVal,
            'pagingType': "full_numbers",
            "dom": '<"scroll-wp"rt><"pg-wp"ilp>',
            "initComplete": function () {
                if (selectedBrokerid.length > 0) {
                    self.$popupWrap.find('.js_all_check').click();
                }
            }
        });
    };

    self.$popupWrap.html(html);
    self.showWrap();

    initEvent();
    initTableInfo(topicName, selectedBrokerid);
};