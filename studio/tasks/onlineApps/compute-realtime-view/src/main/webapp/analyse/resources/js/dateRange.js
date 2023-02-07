/**
 *=======================================================================
 *日期选择器js组件。
 *@author ：johnnyzheng(johnnyzheng@tencent.com) 郑灿双
 *@version ： 2012-07-11
 *@modification list：2012-08-16  规范样式名称
 *                    2013-01-04  增加主题设置接口
 *                    2013-01-31  增加自定义灰掉周末 周几的选项，增加自动初始化自动提交的功能
 *                    2013-03-15  支持一个页面多个日期选择器，快捷日期选择
 *                    2013-03-26  增加确认、取消按钮的隐藏，而直接自动提交
 * 					  2013-08-01  扩展接口，增加最近90天，增加自定义可选时间
 * 					  2013-08-12  日期选择器框体宽度超出视窗大小的时候制动鼓靠右对齐
 *					  2014-02-25  增加业务接口：获取当前日期对象的的选中日期
 *					  2014-10-13  扩展参数，支持日期下拉选择自定义年和月份，配合theme:ta来使用。
 *=======================================================================
*/
	/**
 	* @description 整个日期选择器对象的构造函数入口，支持丰富的接口参数传递，大多数提供默认配置，可传入覆盖
 	* @param {String} inputId 日期选择器ID
 	* @param {object} options 配置数组
 	*/
	function pickerDateRange(inputId, options) {
    /**
     * 默认配置参数数据，每个参数涵义在后解释
     */
    var defaults = {
		aToday : 'aToday', //今天
		aYesterday : 'aYesterday', //昨天
		aRecent7Days : 'aRecent7Days', //最近7天
		aRecent14Days : 'aRecent14Days',//最近14天
		aRecent30Days : 'aRecent30Days', //最近30天
		aRecent90Days : 'aRecent90Days', //最近90天
                startDate : '', // 开始日期
                endDate : '', // 结束日期
                startCompareDate : '', // 对比开始日期
                endCompareDate : '', // 对比结束日期
	        minValidDate : '315507600', //最小可用时间，控制日期选择器的可选力度
            maxValidDate : '', // 最大可用时间，与stopToday 配置互斥
        success : function(obj) {return true;}, //回调函数，选择日期之后执行何种操作
        startDateId : 'startDate', // 开始日期输入框ID
        startCompareDateId : 'startCompareDate', // 对比开始日期输入框ID
        endDateId : 'endDate', // 结束日期输入框ID
        endCompareDateId : 'endCompareDate', // 对比结束日期输入框ID
        target : '', // 日期选择框的目标，一般为 <form> 的ID值
        needCompare : false, // 是否需要进行日期对比
		suffix : '', //相应控件的后缀
		inputTrigger : 'input_trigger',
		compareTrigger : 'compare_trigger',
        compareCheckboxId : 'needCompare', // 比较选择框
        calendars : 2, // 展示的月份数，最大是2
        dayRangeMax : 0, // 日期最大范围(以天计算)
        monthRangeMax : 12, // 日期最大范围(以月计算)
        dateTable : 'dateRangeDateTable', // 日期表格的CSS类
        selectCss : 'dateRangeSelected', // 时间选择的样式
        compareCss : 'dateRangeCompare', // 比较时间选择的样式
        coincideCss : 'dateRangeCoincide', // 重合部分的样式
		firstCss : 'first', //起始样式
		lastCss : 'last', //结束样式
		clickCss : 'today', //点击样式
        disableGray : 'dateRangeGray', // 非当前月的日期样式
        isToday : 'dateRangeToday', // 今天日期的样式
        joinLineId : 'joinLine',
        isSingleDay : false,
        defaultText : ' 至 ',
        singleCompare : false,
        stopToday : true,
        isTodayValid : false,
		weekendDis : false, //灰掉周末不可选。
		disCertainDay : [], //不可用的周日期设置数组，如：[1,3]是要周一， 周三 两天不可选，每个周的周一，周三都不可选择。
        disCertainDate : [],//不可用的日期设置数组，如:[1,3]是要1号，3号 两天不可选，特别的，[true,1,3]则反之，只有1，3可选，其余不可选。
		shortOpr : false, //结合单天日期选择的短操作，不需要确定和取消的操作按钮。
		noCalendar : false, //日期输入框是否展示
		theme : 'gri', //日期选择器的主题，目前支持 'gri' / 'ta'
		magicSelect : false, //用户自定义选择年、月，与{theme:ta}配合使用。
		autoCommit : false, //加载后立马自动提交
		autoSubmit : false, //没有确定，取消按钮，直接提交 
		replaceBtn : 'btn_compare'
    };
    //将对象赋给__method变量
    var __method = this;
	
    this.inputId = inputId;
	this.inputCompareId = inputId + 'Compare';
	this.compareInputDiv = 'div_compare_'+inputId;
    // 配置参数
    this.mOpts = $.extend({}, defaults, options);
	//默认日历参数最大是3
	this.mOpts.calendars = Math.min(this.mOpts.calendars, 3);
	//根据不同主题需要初始化的变量
	this.mOpts.compareCss = this.mOpts.theme == 'ta' ? this.mOpts.selectCss :this.mOpts.compareCss
    //昨天,今天,最近7天,最近14天,最近30天	
	this.periodObj = {};
	this.periodObj[__method.mOpts.aToday] = 0;
	this.periodObj[__method.mOpts.aYesterday] = 1;
	this.periodObj[__method.mOpts.aRecent7Days] = 6;
	this.periodObj[__method.mOpts.aRecent14Days] = 13;
	this.periodObj[__method.mOpts.aRecent30Days] = 29;
	this.periodObj[__method.mOpts.aRecent90Days] = 89;
    // 记录初始默认时间
    this.startDefDate = '';
    // 随机ID后缀
    var suffix = '' == this.mOpts.suffix ? (new Date()).getTime() : this.mOpts.suffix;
    // 日期选择框DIV的ID
    this.calendarId = 'calendar_' + suffix;
    // 日期列表DIV的ID
    this.dateListId = 'dateRangePicker_' + suffix;
    // 日期比较层
    this.dateRangeCompareDiv = 'dateRangeCompareDiv_' + suffix;
	//日期选择层
	this.dateRangeDiv = 'dateRangeDiv_' + suffix;
    // 日期对比选择控制的checkbox
    this.compareCheckBoxDiv = 'dateRangeCompareCheckBoxDiv_' + suffix;
    // 时间选择的确认按钮
    this.submitBtn = 'submit_' + suffix;
    // 日期选择框关闭按钮
    this.closeBtn = 'closeBtn_' + suffix;
    // 上一个月的按钮
    this.preMonth = 'dateRangePreMonth_' + suffix;
    // 下一个月的按钮
    this.nextMonth = 'dateRangeNextMonth_' + suffix;
    // 表单中开始、结束、开始对比、结束对比时间
    this.startDateId = this.mOpts.startDateId + '_' + suffix;
    this.endDateId = this.mOpts.endDateId + '_' + suffix;
    this.compareCheckboxId = this.mOpts.compareCheckboxId + '_' + suffix;
    this.startCompareDateId = this.mOpts.startCompareDateId + '_' + suffix;
    this.endCompareDateId = this.mOpts.endCompareDateId + '_' + suffix;
    // 初始化日期选择器面板的HTML代码串
    var wrapper = {
		gri :[
			'<div id="' + this.calendarId + '" class="gri_dateRangeCalendar">',
				'<table class="gri_dateRangePicker"><tr id="' + this.dateListId + '"></tr></table>',
				'<div class="gri_dateRangeOptions" '+ (this.mOpts.autoSubmit ? ' style="display:none" ' : '') +'>',
					'<div class="gri_dateRangeInput" id="' + this.dateRangeDiv + '" >',
						'<input type="text" class="gri_dateRangeInput" name="' + this.startDateId + '" id="' + this.startDateId + '" value="' + this.mOpts.startDate + '" readonly />',
						'<span id="' + this.mOpts.joinLineId + '"> - </span>',
						'<input type="text" class="gri_dateRangeInput" name="' + this.endDateId + '" id="' + this.endDateId + '" value="' + this.mOpts.endDate + '" readonly /><br />',
					'</div>',
					'<div class="gri_dateRangeInput" id="' + this.dateRangeCompareDiv + '">',
						'<input type="text" class="gri_dateRangeInput" name="' + this.startCompareDateId + '" id="' + this.startCompareDateId + '" value="' + this.mOpts.startCompareDate + '" readonly />',
						'<span class="' + this.mOpts.joinLineId + '"> - </span>',
						'<input type="text" class="gri_dateRangeInput" name="' + this.endCompareDateId + '" id="' + this.endCompareDateId + '" value="' + this.mOpts.endCompareDate + '" readonly />',
					'</div>',
					'<div>',
						'<input type="button" name="' + this.submitBtn + '" id="' + this.submitBtn + '" value="确定" />',
						'&nbsp;<a id="' + this.closeBtn + '" href="javascript:;">关闭</a>',
					'</div>',
				'</div>',
			'</div>'
		],
		ta:[
			'<div id="' + this.calendarId + '" class="ta_calendar ta_calendar2 cf">',
				'<div class="ta_calendar_cont cf" id="'+ this.dateListId +'">',
					//'<table class="dateRangePicker"><tr id="' + this.dateListId + '"></tr></table>',
				'</div>',
				'<div class="ta_calendar_footer cf" '+ (this.mOpts.autoSubmit ? ' style="display:none" ' : '') +'>',
					'<div class="frm_msg">',
						'<div id="' + this.dateRangeDiv + '">',
							'<input type="text" class="ta_ipt_text_s" name="' + this.startDateId + '" id="' + this.startDateId + '" value="' + this.mOpts.startDate + '" readonly />',
							'<span class="' + this.mOpts.joinLineId + '"> - </span>',
							'<input type="text" class="ta_ipt_text_s" name="' + this.endDateId + '" id="' + this.endDateId + '" value="' + this.mOpts.endDate + '" readonly /><br />',
						'</div>',
						'<div id="' + this.dateRangeCompareDiv + '">',
							'<input type="text" class="ta_ipt_text_s" name="' + this.startCompareDateId + '" id="' + this.startCompareDateId + '" value="' + this.mOpts.startCompareDate + '" readonly />',
							'<span class="' + this.mOpts.joinLineId + '"> - </span>',
							'<input type="text" class="ta_ipt_text_s" name="' + this.endCompareDateId + '" id="' + this.endCompareDateId + '" value="' + this.mOpts.endCompareDate + '" readonly />',
						'</div>',
					'</div>',
					'<div class="frm_btn">',
						'<input class="ta_btn ta_btn_primary" type="button" name="' + this.submitBtn + '" id="' + this.submitBtn + '" value="确定" />',
						'<input class="ta_btn" type="button" id="' + this.closeBtn + '" value="取消"/>',
					'</div>',
				'</div>',
			'</div>'
		]
	};
	
	
	//对比日期框体的html串
	var checkBoxWrapper = {
	gri:[
		'<label class="gri_contrast" for ="' + this.compareCheckboxId + '">',
            '<input type="checkbox" class="gri_pc" name="' + this.compareCheckboxId + '" id="' + this.compareCheckboxId + '" value="1"/>对比',
        '</label>',
		'<input type="text" name="'+this.inputCompareId+'" id="'+this.inputCompareId+'" value="" class="gri_date"/>'
	],
	ta:[
		'<label class="contrast" for ="' + this.compareCheckboxId + '">',
            '<input type="checkbox" class="pc" name="' + this.compareCheckboxId + '" id="' + this.compareCheckboxId + '" value="1"/>对比',
        '</label>',
		'<div class="ta_date" id="'+this.compareInputDiv+'">',
		'	<span name="dateCompare" id="'+this.inputCompareId+'" class="date_title"></span>',
		'	<a class="opt_sel" id="'+ this.mOpts.compareTrigger +'" href="#">',
        '		<i class="i_orderd"></i>',
        '	</a>',
		'</div>'
	]
	};
	//把checkbox放到页面的相应位置,放置到inputid后面 added by johnnyzheng
	
	if(this.mOpts.theme == 'ta'){
		$(checkBoxWrapper[this.mOpts.theme].join('')).insertAfter($('#div_' + this.inputId));
	}else{
		$(checkBoxWrapper[this.mOpts.theme].join('')).insertAfter($('#' + this.inputId));
	}
	//根据传入参数决定是否展示日期输入框
	if(this.mOpts.noCalendar){
		$('#' + this.inputId).css('display', 'none');
		$('#' + this.compareCheckboxId).parent().css('display','none');
	}
    // 把时间选择框放到页面中
    $(0 < $('#appendParent').length ? '#appendParent' : document.body).append(wrapper[this.mOpts.theme].join(''));
    $('#' + this.calendarId).css('z-index', 9999);
    // 初始化目标地址的元素
    if(1 > $('#' + this.mOpts.startDateId).length) {
        $(''!=this.mOpts.target?'#'+this.mOpts.target:'body').append('<input type="hidden" id="' + this.mOpts.startDateId + '" name="' + this.mOpts.startDateId + '" value="' + this.mOpts.startDate + '" />');
    } else {
        $('#' + this.mOpts.startDateId).val(this.mOpts.startDate);
    }
    if(1 > $('#' + this.mOpts.endDateId).length) {
        $(''!=this.mOpts.target?'#'+this.mOpts.target:'body').append('<input type="hidden" id="' + this.mOpts.endDateId + '" name="' + this.mOpts.endDateId + '" value="' + this.mOpts.endDate + '" />');
    } else {
        $('#' + this.mOpts.endDateId).val(this.mOpts.endDate);
    }
    if(1 > $('#' + this.mOpts.compareCheckboxId).length) {
        $(''!=this.mOpts.target?'#'+this.mOpts.target:'body').append('<input type="checkbox" id="' + this.mOpts.compareCheckboxId + '" name="' + this.mOpts.compareCheckboxId + '" value="0" style="display:none;" />');
    }
    // 如果不需要比较日期，则需要隐藏比较部分的内容
    if(false == this.mOpts.needCompare) {
		$('#' + this.compareInputDiv).css('display', 'none');
        $('#' + this.compareCheckBoxDiv).css('display', 'none');
        $('#' + this.dateRangeCompareDiv).css('display', 'none');
        $('#' + this.compareCheckboxId).attr('disabled', true);
        $('#' + this.startCompareDateId).attr('disabled', true);
        $('#' + this.endCompareDateId).attr('disabled', true);
		//隐藏对比的checkbox
		$('#' + this.compareCheckboxId).parent().css('display','none');
		$('#'+ this.mOpts.replaceBtn).length > 0 && $('#'+ this.mOpts.replaceBtn).hide();
    } else {
        if(1 > $('#' + this.mOpts.startCompareDateId).length) {
            $(''!=this.mOpts.target?'#'+this.mOpts.target:'body').append('<input type="hidden" id="' + this.mOpts.startCompareDateId + '" name="' + this.mOpts.startCompareDateId + '" value="' + this.mOpts.startCompareDate + '" />');
        } else {
            $('#' + this.mOpts.startCompareDateId).val(this.mOpts.startCompareDate);
        }
        if(1 > $('#' + this.mOpts.endCompareDateId).length) {
            $(''!=this.mOpts.target?'#'+this.mOpts.target:'body').append('<input type="hidden" id="' + this.mOpts.endCompareDateId + '" name="' + this.mOpts.endCompareDateId + '" value="' + this.mOpts.endCompareDate + '" />');
        } else {
            $('#' + this.mOpts.endCompareDateId).val(this.mOpts.endCompareDate);
        }
        if('' == this.mOpts.startCompareDate || '' == this.mOpts.endCompareDate) {
            $('#' + this.compareCheckboxId).attr('checked', false);
            $('#' + this.mOpts.compareCheckboxId).attr('checked', false);
        } else {
            $('#' + this.compareCheckboxId).attr('checked', true);
            $('#' + this.mOpts.compareCheckboxId).attr('checked', true);
        }
		
    }
    // 输入框焦点定在第一个输入框
    this.dateInput = this.startDateId;
    // 为新的输入框加背景色
    this.changeInput(this.dateInput);

    // 开始时间 input 的 click 事件
    $('#' + this.startDateId).bind('click', function() {
        // 如果用户在选择基准结束时间时，换到对比时间了，则
        if(__method.endCompareDateId == __method.dateInput) {
            $('#' + __method.startCompareDateId).val(__method.startDefDate);
        }
        __method.startDefDate = '';
        __method.removeCSS(1);
        //__method.addCSS(1);
        __method.changeInput(__method.startDateId);
        return false;
    });
    $('#' + this.calendarId).bind('click', function(event) {
        //event.preventDefault();
        // 防止冒泡
        event.stopPropagation();
    });
    // 开始比较时间 input 的 click 事件
    $('#' + this.startCompareDateId).bind('click', function() {
        // 如果用户在选择基准结束时间时，换到对比时间了，则
        if(__method.endDateId == __method.dateInput) {
            $('#' + __method.startDateId).val(__method.startDefDate);
        }
        __method.startDefDate = '';
        __method.removeCSS(0);
        //__method.addCSS(0);
        __method.changeInput(__method.startCompareDateId);
        return false;
    });
    /**
     * 设置回调句柄，点击成功后，返回一个时间对象，包含开始结束时间
     * 和对比开始结束时间
     */
    $('#' + this.submitBtn).bind('click', function() {
        __method.close(1);
        __method.mOpts.success({'startDate': $('#' + __method.mOpts.startDateId).val(), 
								'endDate': $('#' + __method.mOpts.endDateId).val(), 
								'needCompare' : $('#' + __method.mOpts.compareCheckboxId).val(),
								'startCompareDate':$('#' + __method.mOpts.startCompareDateId).val(), 
								'endCompareDate':$('#' + __method.mOpts.endCompareDateId).val()
								});
        return false;
    });
    // 日期选择关闭按钮的 click 事件
    $('#' + this.closeBtn).bind('click', function() {
        __method.close();
        return false;
    });
    // 为输入框添加click事件
    $('#' + this.inputId).bind('click', function() {
        __method.init();
        __method.show(false, __method);
        return false;
    });
	$('#' + this.mOpts.inputTrigger).bind('click', function() {
        __method.init();
        __method.show(false, __method);
        return false;
    });
	$('#' + this.mOpts.compareTrigger).bind('click', function() {
        __method.init(true);
        __method.show(true, __method);
        return false;
    });
	   // 为输入框添加click事件
    $('#' + this.inputCompareId).bind('click', function() {
        __method.init(true);
        __method.show(true, __method);
        return false;
    });
	
	//判断是否是实时数据,如果是将时间默认填充进去 added by johnnyzheng 12-06
	if(this.mOpts.singleCompare){
		if(this.mOpts.theme === 'ta'){
			$('#' + __method.startDateId).val(__method.mOpts.startDate);
			$('#' + __method.endDateId).val(__method.mOpts.startDate);
			$('#' + __method.startCompareDateId).val(__method.mOpts.startCompareDate);
			$('#' + __method.endCompareDateId).val(__method.mOpts.startCompareDate);
		}
		else{
			$('#' + __method.startDateId).val(__method.mOpts.startDate);
			$('#' + __method.endDateId).val(__method.mOpts.startDate);
			$('#' + __method.startCompareDateId).val(__method.mOpts.startCompareDate);
			$('#' + __method.endCompareDateId).val(__method.mOpts.startCompareDate);
			$('#' + this.compareCheckboxId).attr('checked',true);
			$('#' + this.mOpts.compareCheckboxId).attr('checked',true);
		}
		
		
	}
    // 时间对比
    $('#' + this.dateRangeCompareDiv).css('display', $('#' + this.compareCheckboxId).attr('checked') ? '' : 'none');
	$('#' + this.compareInputDiv).css('display', $('#' + this.compareCheckboxId).attr('checked') ? '' : 'none');
    $('#' + this.compareCheckboxId).bind('click', function() {
		$('#' + __method.inputCompareId).css('display', this.checked ? '' : 'none');
        // 隐藏对比时间选择
        $('#' + __method.dateRangeCompareDiv).css('display', this.checked ? '' : 'none');
		$('#' + __method.compareInputDiv).css('display', this.checked ? '' : 'none');
        // 把两个对比时间框置为不可用
        $('#' + __method.startCompareDateId).css('disabled', this.checked ? false : true);
        $('#' + __method.endCompareDateId).css('disabled', this.checked ? false : true);
        // 修改表单的 checkbox 状态
        $('#' + __method.mOpts.compareCheckboxId).attr('checked', $('#' + __method.compareCheckboxId).attr('checked'));
        // 修改表单的值
        $('#' + __method.mOpts.compareCheckboxId).val($('#' + __method.compareCheckboxId).attr('checked')?1:0);
        // 初始化选框背景
        if($('#' + __method.compareCheckboxId).attr('checked')) {
            sDate = __method.str2date($('#' + __method.startDateId).val());
            sTime = sDate.getTime();
            eDate = __method.str2date($('#' + __method.endDateId).val());
			eTime = eDate.getTime();
            scDate = $('#' + __method.startCompareDateId).val();
            ecDate = $('#' + __method.endCompareDateId).val();
            if('' == scDate || '' == ecDate) {
                ecDate = __method.str2date(__method.date2ymd(sDate).join('-'));
                ecDate.setDate(ecDate.getDate() - 1);
				scDate = __method.str2date(__method.date2ymd(sDate).join('-'));
                scDate.setDate(scDate.getDate() - ((eTime - sTime) / 86400000) - 1);
				//这里要和STATS_START_TIME的时间进行对比，如果默认填充的对比时间在这个时间之前 added by johnnyzheng
				if(ecDate.getTime() < __method.mOpts.minValidDate * 1000){
					scDate = sDate;
					ecDate = eDate;
				}
				if(ecDate.getTime() >= __method.mOpts.minValidDate * 1000 && scDate.getTime() < __method.mOpts.minValidDate * 1000){
					scDate.setTime(__method.mOpts.minValidDate * 1000)
					scDate = __method.str2date(__method.date2ymd(scDate).join('-'));
					ecDate.setDate(scDate.getDate() + ((eTime - sTime) / 86400000) - 1);
				}
                $('#' + __method.startCompareDateId).val(__method.formatDate(__method.date2ymd(scDate).join('-')));
                $('#' + __method.endCompareDateId).val(__method.formatDate(__method.date2ymd(ecDate).join('-')));
            }
            __method.addCSS(1);
            // 输入框焦点切换到比较开始时间
            __method.changeInput(__method.startCompareDateId);

        } else {
            __method.removeCSS(1);
            // 输入框焦点切换到开始时间
            __method.changeInput(__method.startDateId);
        }
		//用户点击默认自动提交 added by johnnyzheng 12-08
		__method.close(1);
		__method.mOpts.success({'startDate': $('#' + __method.mOpts.startDateId).val(), 
								'endDate': $('#' + __method.mOpts.endDateId).val(), 
								'needCompare' : $('#' + __method.mOpts.compareCheckboxId).val(),
								'startCompareDate':$('#' + __method.mOpts.startCompareDateId).val(), 
								'endCompareDate':$('#' + __method.mOpts.endCompareDateId).val()
								});
    });

    // 初始化开始
    this.init();
    // 关闭日期选择框，并把结果反显到输入框
    this.close(1);
	if(this.mOpts.replaceBtn && $('#'+this.mOpts.replaceBtn).length > 0){
		$('#'+ __method.compareCheckboxId).hide();
		$('.contrast').hide();
		$('#'+this.mOpts.replaceBtn).bind('click', function(){
			var self = this;
			$('#'+ __method.compareCheckboxId).attr('checked')
			? $('#'+ __method.compareCheckboxId).removeAttr('checked')
			: $('#'+ __method.compareCheckboxId).attr('checked', 'checked');
			$('#'+ __method.compareCheckboxId).click();
			$('#'+ __method.compareCheckboxId).attr('checked')
			? function(){
					$('#'+ __method.compareCheckboxId).removeAttr('checked');
					$('.contrast').hide();
					$(self).text('按时间对比');
				}()
			: function(){
					$('#'+ __method.compareCheckboxId).attr('checked', 'checked');
					$('.contrast').show();
					$(self).text('取消对比');
				}();
		});
	}
	
	if(this.mOpts.autoCommit){
		this.mOpts.success({'startDate': $('#' + __method.mOpts.startDateId).val(), 
								'endDate': $('#' + __method.mOpts.endDateId).val(),
								'needCompare' : $('#' + __method.mOpts.compareCheckboxId).val(), 
								'startCompareDate':$('#' + __method.mOpts.startCompareDateId).val(), 
								'endCompareDate':$('#' + __method.mOpts.endCompareDateId).val()
								});
	}
    //让用户点击页面即可关闭弹窗
    $(document).bind('click', function () {
       __method.close();
    });
};

/**
 * @description 日期选择器的初始化方法，对象原型扩展
 * @param {Boolean} isCompare 标识当前初始化选择面板是否是对比日期
 */
pickerDateRange.prototype.init = function(isCompare) {
    var __method = this;
    var minDate, maxDate;
	var isNeedCompare = typeof(isCompare) != 'undefined'? isCompare && $("#" + __method.compareCheckboxId).attr('checked') : $("#" + __method.compareCheckboxId).attr('checked');
    // 清空日期列表的内容
    $("#" + this.dateListId).empty();
	
    // 如果开始日期为空，则取当天的日期为开始日期
    var endDate = '' == this.mOpts.endDate ? (new Date()) : this.str2date(this.mOpts.endDate);
    // 日历结束时间
    this.calendar_endDate = new Date(endDate.getFullYear(), endDate.getMonth() + 1, 0);

	//如果是magicSelect 自定义年和月份，则自定义填充日期
	if(this.mOpts.magicSelect && this.mOpts.theme == 'ta'){
		var i = 0;
		do{
			var td = null;
			if(i==0){
				td = this.fillDate(this.str2date($('#'+this.endDateId).val()).getFullYear(), this.str2date($('#'+this.endDateId).val()).getMonth(), i);
				$("#" + this.dateListId).append(td);
			}
			else{
				td = this.fillDate(this.str2date($('#'+this.startDateId).val()).getFullYear(), this.str2date($('#'+this.startDateId).val()).getMonth(), i);
				var firstTd = (this.mOpts.theme == 'ta' ? $("#" + this.dateListId).find('table').get(0) : $("#" + this.dateListId).find('td').get(0));
				$(firstTd).before(td);
			}
			i++;
		}while(i<2);
		// 日历开始时间
		this.calendar_startDate = new Date(this.str2date($('#'+this.startDateId).val()).getFullYear(), this.str2date($('#'+this.startDateId).val()).getMonth(), 1);
	
	}else{
		// 计算并显示以 endDate 为结尾的最近几个月的日期列表
		for(var i = 0; i < this.mOpts.calendars; i ++) {
			var td = null;
			if(this.mOpts.theme == 'ta'){
				td = this.fillDate(endDate.getFullYear(), endDate.getMonth(), i);
			}
			else{
				td = document.createElement('td');
				$(td).append(this.fillDate(endDate.getFullYear(), endDate.getMonth(), i));
				$(td).css('vertical-align', 'top');
			}
			if(0 == i) {
				$("#" + this.dateListId).append(td);
			} else {
				var firstTd = (this.mOpts.theme == 'ta' ? $("#" + this.dateListId).find('table').get(0) : $("#" + this.dateListId).find('td').get(0));
				$(firstTd).before(td);
			}
			endDate.setMonth(endDate.getMonth() - 1, 1);
		}
		 // 日历开始时间
		this.calendar_startDate = new Date(endDate.getFullYear(), endDate.getMonth() + 1, 1);
	}

    // 上一个月
    $('#' + this.preMonth).bind('click', function() {
        __method.calendar_endDate.setMonth(__method.calendar_endDate.getMonth() - 1, 1);
        __method.mOpts.endDate = __method.date2ymd(__method.calendar_endDate).join('-');
        __method.init(isCompare);
		//如果是单月选择的时候，要控制input输入框 added by johnnyzheng 2011-12-19
		if(1 == __method.mOpts.calendars){
			if('' == $('#' + __method.startDateId).val()){
				__method.changeInput(__method.startDateId);
			}
			else{
				__method.changeInput(__method.endDateId);
			}
		}
        return false;
    });
    // 下一个月
    $('#' + this.nextMonth).bind('click', function() {
        __method.calendar_endDate.setMonth(__method.calendar_endDate.getMonth() + 1, 1);
        __method.mOpts.endDate = __method.date2ymd(__method.calendar_endDate).join('-');
		__method.init(isCompare);
		//如果是单月选择的时候，要控制input输入框 added by johnnyzheng 2011-12-19
		if(1 == __method.mOpts.calendars){
			if('' == $('#' + __method.startDateId).val()){
				__method.changeInput(__method.startDateId);
			}
			else{
				__method.changeInput(__method.endDateId);
			}
		}
        return false;
    });
	
	//如果有用户自定义选择月份，则为其绑定事件
	if(this.mOpts.magicSelect) this.bindChangeForSelect();
	

    // 初始化时间选区背景
    if(this.endDateId != this.dateInput && this.endCompareDateId != this.dateInput) {
         (isNeedCompare && typeof(isCompare) !='undefined') ? this.addCSS(1) : this.addCSS(0);
    }
	
	if(isNeedCompare && typeof(isCompare) !='undefined'){
		__method.addCSS(1);
	}
	else{
		__method.addCSS(0);
	
	}
	
	// 隐藏对比日期框
	$('#' + __method.inputCompareId).css('display', isNeedCompare ? '' : 'none');
	$('#' + this.compareInputDiv).css('display', $('#' + this.compareCheckboxId).attr('checked') ? '' : 'none');
	//昨天，今天，最近7天，最近30天快捷的点击，样式要自己定义，id可以传递默认，也可覆盖
	for(var property in __method.periodObj){
		if($('#'+ property).length > 0){
			$('#' +  property).unbind('click');
			$('#' +  property).bind('click' , function(){
				//处理点击样式
				var cla = __method.mOpts.theme == 'ta' ? 'active' : 'a';
				$(this).parent().nextAll().removeClass(cla);
				$(this).parent().prevAll().removeClass(cla);
				$(this).parent().addClass(cla);
				//拼接提交时间串
				var timeObj = __method.getSpecialPeriod(__method.periodObj[$(this).attr('id')]);
				$('#' + __method.startDateId).val(__method.formatDate(timeObj.otherday));
				$('#' + __method.endDateId).val(__method.formatDate(timeObj.today));
				$('#' + __method.mOpts.startDateId).val($('#' + __method.startDateId).val());
				$('#' + __method.mOpts.endDateId).val($('#' + __method.endDateId).val());
				__method.mOpts.theme == 'ta' ? $('#'+__method.compareInputDiv).hide() : $('#' + __method.inputCompareId).css('display','none');
				$('#' + __method.compareCheckboxId).attr('checked', false);
				$('#' + __method.mOpts.compareCheckboxId).attr('checked', false);
				$('#' + this.compareInputDiv).css('display', $('#' + this.compareCheckboxId).attr('checked') ? '' : 'none');
                __method.close(1);
				//于此同时清空对比时间框的时间
				$('#' + __method.startCompareDateId).val('');
				$('#' + __method.endCompareDateId).val('');
				$('#' + __method.mOpts.startCompareDateId).val('');
				$('#' + __method.mOpts.endCompareDateId).val('');
				$('#' + __method.mOpts.compareCheckboxId).val(0);
				
				if($('#'+ __method.mOpts.replaceBtn).length > 0){
					$('.contrast').hide();
					$('#'+ __method.mOpts.replaceBtn).text('按时间对比');
				}
				//点击提交
				__method.mOpts.success({'startDate': $('#' + __method.mOpts.startDateId).val(), 
								'endDate': $('#' + __method.mOpts.endDateId).val(), 
								'needCompare' : $('#' + __method.mOpts.compareCheckboxId).val(),
								'startCompareDate':$('#' + __method.mOpts.startCompareDateId).val(), 
								'endCompareDate':$('#' + __method.mOpts.endCompareDateId).val()
								});
			}); 
		}
	}

    // 让用户手动关闭或提交日历，每次初始化的时候绑定，关闭的时候解绑 by zacharycai
    $(document).bind('click', function () {
        __method.close();
    });

    //完全清空日期控件的值 by zacharycai
    $('#' + this.inputId).bind('change', function(){
        if ($(this).val() === ''){
            $('#' + __method.startDateId).val('');
            $('#' + __method.endDateId).val('');
            $('#' + __method.startCompareDateId).val('');
            $('#' + __method.endCompareDateId).val('');
        }
    })
};

pickerDateRange.prototype.bindChangeForSelect = function(){
	var __method = this;
	//气泡弹窗
	var _popup = function(btn, ctn, wrap, css) {
			css = css || 'open';
			var ITEMS_TIMEOUT = null, time_out = 500;
	
			function hidePop() {
				$('#' + ctn).removeClass(css);
			}
	
			function showPop() {
				$('#' + ctn).addClass(css);
			}
	
			function isPopShow() {
				return $('#' + ctn).attr('class') == css;
			}
	
	
			$("#" + btn).click(function() {
				isPopShow() ? hidePop() : showPop();
			}).mouseover(function() {
				clearTimeout(ITEMS_TIMEOUT);
			}).mouseout(function() {
				ITEMS_TIMEOUT = setTimeout(hidePop, time_out);
			});
	
			$('#' + wrap).mouseover(function() {
				clearTimeout(ITEMS_TIMEOUT);
			}).mouseout(function() {
				ITEMS_TIMEOUT = setTimeout(hidePop, time_out);
			});
		};
	
	//自定义选择的触发动作
	try{
		$("#" + this.dateListId).find('div[id*="selected"]').each(function(){
				//绑定pop
				var _match = $(this).attr('id').match(/(\w+)_(\d)/i);
				if(_match){
					var _name = _match[1];//名称
					var _idx = _match[2];//下标
					
					if(_name=='yselected'){
						_popup('_ybtn_'+_idx, $(this).attr('id'), '_yctn_'+_idx);
					}
					else if(_name=='mselected'){
						_popup('_mbtn_'+_idx, $(this).attr('id'), '_mctn_'+_idx);
					}
					
					$(this).find('li a').each(function(){
						$(this).click(function() {
							var match = $(this).parents('.select_wrap').attr('id').match(/(\w+)_(\d)/i);
							//if(match){
								var name = match[1];//名称
								var idx = match[2];//下标
								var nt = null;
								if(idx^1 == 0){
								//开始
									if(name == 'yselected'){
										__method.calendar_startDate.setYear($(this).text()*1 , 1);
										//__method.calendar_startDate.setMonth(__method.str2date($('#'+__method.startDateId).val()).getMonth(), 1);
									}
									else if(name='mselected'){
										//__method.calendar_startDate.setYear(__method.str2date($('#'+__method.startDateId).val()).getFullYear(), 1);
										__method.calendar_startDate.setMonth($(this).text()*1-1, 1);
									}
									__method.mOpts.startDate = __method.date2ymd(__method.calendar_startDate).join('-');
									nt = __method.fillDate(__method.calendar_startDate.getFullYear(), __method.calendar_startDate.getMonth(), idx);
								}
								else{
									//结束
									if(name == 'yselected'){
										__method.calendar_endDate.setYear($(this).text()*1 , 1);
										//__method.calendar_endDate.setMonth(__method.str2date($('#'+__method.endDateId).val()).getMonth(), 1);
									}
									else if(name='mselected'){
										//__method.calendar_endDate.setYear(__method.str2date($('#'+__method.endDateId).val()).getFullYear(), 1);
										__method.calendar_endDate.setMonth($(this).text()*1-1, 1);
									}
									__method.mOpts.endDate = __method.date2ymd(__method.calendar_endDate).join('-');
									nt = __method.fillDate(__method.calendar_endDate.getFullYear(), __method.calendar_endDate.getMonth(), idx);
								}
								var tb = $("#" + __method.dateListId).find('table').get(idx^1);
								$(tb).replaceWith(nt);
							//}
							__method.removeCSS(0);
							__method.bindChangeForSelect();
						});		
					});
				}
			});
		}catch(e){
			window.console && console.log(e);
		}
}
/**
 * @description 计算今天，昨天，最近7天，最近30天返回的时间范围
 * @param {Num} period 快捷选择的时间段，今天、昨天、最近7天、最近30天
 */
pickerDateRange.prototype.getSpecialPeriod = function(period){
	var __method = this;
	var date = new Date();
	//如果今天不可用，则从昨天向前推 added by johnnyzheng 12-07
	(true == __method.mOpts.isTodayValid && ('' != __method.mOpts.isTodayValid) || 2 > period)? '' : date.setTime(date.getTime() - ( 1 * 24 * 60 * 60 * 1000));
	var timeStamp = ((date.getTime()- ( period * 24 * 60 * 60 * 1000)) < (__method.mOpts.minValidDate * 1000)) ? (__method.mOpts.minValidDate * 1000) : (date.getTime()- ( period * 24 * 60 * 60 * 1000)) ;
	var todayStr = date.getFullYear() + '-' + (date.getMonth()+ 1 ) + '-' + date.getDate();
	date.setTime(timeStamp);
	var otherdayStr = date.getFullYear() + '-' + (date.getMonth()+ 1 ) + '-' + date.getDate();
	if(period == __method.periodObj.aYesterday){
		todayStr = otherdayStr;
	}
	return {today: todayStr , otherday : otherdayStr};
}

pickerDateRange.prototype.getCurrentDate = function(){
    return {
            'startDate': $('#' + this.mOpts.startDateId).val(), 
            'endDate': $('#' + this.mOpts.endDateId).val(), 
            'needCompare' : $('#' + this.mOpts.compareCheckboxId).val(),
            'startCompareDate':$('#' + this.mOpts.startCompareDateId).val(), 
            'endCompareDate':$('#' + this.mOpts.endCompareDateId).val()
            };
};

/**
 * @description 移除选择日期面板的样式
 * @param {Boolean} isCompare 是否是对比日期面板
 * @param {String} specialClass 特殊的样式，这里默认是常规和对比日期两种样式的重合样式
 */
pickerDateRange.prototype.removeCSS = function(isCompare, specialClass) {
    // 初始化对比时间重合部分的样式类
    if('undefined' == typeof(specialClass)) {
        specialClass = this.mOpts.theme + '_' + this.mOpts.coincideCss;
    }
    // 是否移除对比部分的样式:0 日期选择;1 对比日期选择
    if('undefined' == typeof(isCompare)) {
        isCompare = 0;
    }
	
    // 整个日期列表的开始日期
	var s_date = this.calendar_startDate;
	var e_date = this.calendar_endDate;
	//如果是用户自定义选择的话，需要充值样式边界日期
	if(this.mOpts.magicSelect){
		s_date = this.str2date($('#'+this.startDateId).val());
		e_date = this.str2date($('#'+this.endDateId).val());
	}
    var bDate = new Date(s_date.getFullYear(), s_date.getMonth(), s_date.getDate());
    var cla = '';
    // 从开始日期循环到结束日期
    for(var d = new Date(bDate); d.getTime() <= e_date.getTime(); d.setDate(d.getDate() + 1)) {
            if(0 == isCompare) {
                // 移除日期样式
                cla = this.mOpts.theme + '_' + this.mOpts.selectCss;
            } else {
                // 移除对比日期样式
                cla = this.mOpts.theme + '_' + this.mOpts.compareCss;
            }
        // 移除指定样式
        $('#'+ this.calendarId + '_'  + this.date2ymd(d).join('-')).removeClass(cla);
		$('#'+ this.calendarId + '_'  + this.date2ymd(d).join('-')).removeClass(this.mOpts.firstCss).removeClass(this.mOpts.lastCss).removeClass(this.mOpts.clickCss);
    }
};

/**
 * @description 为选中的日期加上样式：1=比较时间；0=时间范围
 * @param {Boolean} isCompare 是否是对比日期面板
 * @param {String} specialClass 特殊的样式，这里默认是常规和对比日期两种样式的重合样式
 */
pickerDateRange.prototype.addCSS = function(isCompare, specialClass) {

    // 初始化对比时间重合部分的样式类
    if('undefined' == typeof(specialClass)) {
        specialClass = this.mOpts.theme + '_' + this.mOpts.coincideCss;
    }
    // 是否移除对比部分的样式:0 日期选择;1 对比日期选择
    if('undefined' == typeof(isCompare)) {
        isCompare = 0;
    }
    // 获取4个日期
    var startDate = this.str2date($('#' + this.startDateId).val());
    var endDate = this.str2date($('#' + this.endDateId).val());
    var startCompareDate = this.str2date($('#' + this.startCompareDateId).val());
    var endCompareDate = this.str2date($('#' + this.endCompareDateId).val());

    // 循环开始日期
    var sDate = 0 == isCompare ? startDate : startCompareDate;
    // 循环结束日期
    var eDate = 0 == isCompare ? endDate : endCompareDate;
    var cla = '';
    for(var d = new Date(sDate); d.getTime() <= eDate.getTime(); d.setDate(d.getDate() + 1)) {
            if(0 == isCompare) {
                // 添加日期样式
                cla = this.mOpts.theme + '_' + this.mOpts.selectCss;
				$('#' + this.calendarId + '_' + this.date2ymd(d).join('-')).removeClass(this.mOpts.firstCss).removeClass(this.mOpts.lastCss).removeClass(this.mOpts.clickCss);
				$('#' + this.calendarId + '_' + this.date2ymd(d).join('-')).removeClass(cla);
            } else {
                // 添加对比日期样式
                cla = this.mOpts.theme + '_' + this.mOpts.compareCss;
            }

        $('#' + this.calendarId + '_' + this.date2ymd(d).join('-')).attr('class', cla);
    }
	if(this.mOpts.theme == 'ta'){
		//为开始结束添加特殊样式
		$('#' + this.calendarId + '_' + this.date2ymd(new Date(sDate)).join('-')).removeClass().addClass(this.mOpts.firstCss);
		$('#' + this.calendarId + '_' + this.date2ymd(new Date(eDate)).join('-')).removeClass().addClass(this.mOpts.lastCss);
		//如果开始结束时间相同
		sDate.getTime() == eDate.getTime() && $('#'+ this.calendarId + '_' + this.date2ymd(new Date(eDate)).join('-')).removeClass().addClass(this.mOpts.clickCss);
	}
};

/**
 * @description 判断开始、结束日期是否处在允许的范围内
 * @param {String} startYmd 开始时间字符串
 * @param {String} endYmd 结束时间字符串
 */
pickerDateRange.prototype.checkDateRange = function(startYmd, endYmd) {
    var sDate = this.str2date(startYmd);
    var eDate = this.str2date(endYmd);
    var sTime = sDate.getTime();
    var eTime = eDate.getTime();
    var minEDate, maxEDate;

    if(eTime >= sTime) {
        // 判断是否超过最大日期外
        maxEDate = this.str2date(startYmd);
        maxEDate.setMonth(maxEDate.getMonth() + this.mOpts.monthRangeMax);
        maxEDate.setDate(maxEDate.getDate() + this.mOpts.dayRangeMax - 1);
        if(maxEDate.getTime() < eTime) {
            alert('结束日期不能大于：' + this.date2ymd(maxEDate).join('-'));
            return false;
        }
    } else {
        // 判断是否超过最大日期外
        //maxEDate = this.str2date(stPartYmd);
		maxEDate = this.str2date(endYmd);
        maxEDate.setMonth(maxEDate.getMonth() - this.mOpts.monthRangeMax);
        maxEDate.setDate(maxEDate.getDate() - this.mOpts.dayRangeMax + 1);
        if(maxEDate.getTime() > eTime) {
            alert('开始日期不能小于：' + this.date2ymd(maxEDate).join('-'));
            return false;
        }
    }
    return true;
}

/**
 *  @description 选择日期
 *  @param {String} ymd 时间字符串
 */
pickerDateRange.prototype.selectDate = function(ymd) {
    //点击日期点的时候添加对应输入框的样式，而不是之前的 聚焦到输入框时显示样式 by zacharycai
    this.changeInput(this.dateInput);
    // 格式化日期
    var ymdFormat = this.formatDate(ymd);

    // start <-> end 切换
    if(this.startDateId == this.dateInput) {
        // 移除样式
        this.removeCSS(0);
		this.removeCSS(1);
        // 为当前点加样式
        $('#'+ this.calendarId + '_'  + ymd).attr('class', (this.mOpts.theme == 'ta' ? this.mOpts.clickCss  : this.mOpts.theme + '_' + this.mOpts.selectCss));
		// 获取开始时间的初始值
		this.startDefDate = $('#' + this.dateInput).val();
		// 更改对应输入框的值
		$('#' + this.dateInput).val(ymdFormat);
        // 切换输入框焦点,如果是实时数据那么选择一天的数据
        if (true == this.mOpts.singleCompare || true == this.mOpts.isSingleDay) {
            this.dateInput = this.startDateId;
			$('#' + this.endDateId).val(ymdFormat);
			(this.mOpts.shortOpr || this.mOpts.autoSubmit) && this.close(1);
            this.mOpts.success({'startDate': $('#' + this.mOpts.startDateId).val(),
                'endDate': $('#' + this.mOpts.endDateId).val(),
                'needCompare' : $('#' + this.mOpts.compareCheckboxId).val(),
                'startCompareDate':$('#' + this.mOpts.startCompareDateId).val(),
                'endCompareDate':$('#' + this.mOpts.endCompareDateId).val()
            });

        } else {
            this.dateInput = this.endDateId;
        }
		
    } else if(this.endDateId == this.dateInput) {
        // 如果开始时间未选
        if('' == $('#' + this.startDateId).val()) {
            this.dateInput = this.startDateId;
            this.selectDate(ymd);
            return false;
        }
        // 判断用户选择的时间范围
        if(false == this.checkDateRange($('#' + this.startDateId).val(), ymd)) {
            return false;
        }
        // 如果结束时间小于开始时间
        if(-1 == this.compareStrDate(ymd, $('#' + this.startDateId).val())) {
            // 更改对应输入框的值(结束时间)
            $('#' + this.dateInput).val($('#' + this.startDateId).val());
            // 更改对应输入框的值(开始时间)
            $('#' + this.startDateId).val(ymdFormat);
            ymdFormat = $('#' + this.dateInput).val();
        }
        // 更改对应输入框的值
        $('#' + this.dateInput).val(ymdFormat);
        // 切换输入框焦点
        this.dateInput = this.startDateId;
		this.removeCSS(0);
        this.addCSS(0);
		//this.addCSS(0, this.mOpts.coincideCss);
        this.startDefDate = '';
		if(this.mOpts.autoSubmit){
			this.close(1);
            this.mOpts.success({'startDate': $('#' + this.mOpts.startDateId).val(),
                'endDate': $('#' + this.mOpts.endDateId).val(),
                'needCompare' : $('#' + this.mOpts.compareCheckboxId).val(),
                'startCompareDate':$('#' + this.mOpts.startCompareDateId).val(),
                'endCompareDate':$('#' + this.mOpts.endCompareDateId).val()
            });
		}
    } else if(this.startCompareDateId == this.dateInput) {
        // 移除样式
        this.removeCSS(1);
		this.removeCSS(0);
        // 为当前点加样式
		$('#'+ this.calendarId + '_'  + ymd).attr('class', (this.mOpts.theme == 'ta' ? this.mOpts.clickCss  : this.mOpts.theme + '_' + this.mOpts.compareCss));
        // 获取开始时间的初始值
        this.startDefDate = $('#' + this.dateInput).val();
        // 更改对应输入框的值
        $('#' + this.dateInput).val(ymdFormat);
        // 切换输入框焦点
		if (true == this.mOpts.singleCompare || true == this.mOpts.isSingleDay) {
            this.dateInput = this.startCompareDateId;
			$('#' + this.endCompareDateId).val(ymdFormat);
			(this.mOpts.shortOpr || this.mOpts.autoSubmit) && this.close(1);
            this.mOpts.success({'startDate': $('#' + this.mOpts.startDateId).val(),
                'endDate': $('#' + this.mOpts.endDateId).val(),
                'needCompare' : $('#' + this.mOpts.compareCheckboxId).val(),
                'startCompareDate':$('#' + this.mOpts.startCompareDateId).val(),
                'endCompareDate':$('#' + this.mOpts.endCompareDateId).val()
            });
        }
		else{
		     this.dateInput = this.endCompareDateId;
		}
		
    } else if(this.endCompareDateId == this.dateInput) {
        // 如果开始时间未选
        if('' == $('#' + this.startCompareDateId).val()) {
            this.dateInput = this.startCompareDateId;
            this.selectDate(ymd);
            return false;
        }
        // 判断用户选择的时间范围
        if(false == this.checkDateRange($('#' + this.startCompareDateId).val(), ymd)) {
            return false;
        }
        // 如果结束时间小于开始时间
        if(-1 == this.compareStrDate(ymd, $('#' + this.startCompareDateId).val())) {
            // 更改对应输入框的值(结束时间)
            $('#' + this.dateInput).val($('#' + this.startCompareDateId).val());
            // 更改对应输入框的值(开始时间)
            $('#' + this.startCompareDateId).val(ymdFormat);
            ymdFormat = $('#' + this.dateInput).val();
        }
        // 更改对应输入框的值
        $('#' + this.dateInput).val(ymdFormat);
        // 切换输入框焦点
        this.dateInput = this.startCompareDateId;
        //this.addCSS(1, this.mOpts.coincideCss);
		this.removeCSS(1);
		this.addCSS(1);
        this.startDefDate = '';
		if(this.mOpts.autoSubmit){
			this.close(1);
            this.mOpts.success({'startDate': $('#' + this.mOpts.startDateId).val(),
                'endDate': $('#' + this.mOpts.endDateId).val(),
                'needCompare' : $('#' + this.mOpts.compareCheckboxId).val(),
                'startCompareDate':$('#' + this.mOpts.startCompareDateId).val(),
                'endCompareDate':$('#' + this.mOpts.endCompareDateId).val()
            });
		}
    }
    // 切换到下一个输入框
//    this.changeInput(this.dateInput);
};

/**
 * @description显示日期选择框
 * @param {Boolean} isCompare 是否是对比日期选择框
 * @param {Object} __method 时期选择器超级对象
 */ 
pickerDateRange.prototype.show = function(isCompare, __method) {
	$('#' + __method.dateRangeDiv).css('display', isCompare ? 'none' : '');
	$('#' + __method.dateRangeCompareDiv).css('display', isCompare ? '' : 'none');
    var pos = isCompare ?  $('#' + this.inputCompareId).offset() : $('#' + this.inputId).offset();
	var offsetHeight = isCompare ? $('#' + this.inputCompareId).height() : $('#' + this.inputId).height();
    var clientWidth = parseInt($(document.body)[0].clientWidth);
    var left = pos.left;
    $("#" + this.calendarId).css('display', 'block');
    if (true == this.mOpts.singleCompare || true == this.mOpts.isSingleDay) {
        $('#' + this.endDateId).css('display', 'none');
		$('#' + this.endCompareDateId).css('display','none');
        $('#' + this.mOpts.joinLineId).css('display', 'none');
		$('.' + this.mOpts.joinLineId).css('display', 'none');
    }
    // 如果和输入框左对齐时超出了宽度范围，则右对齐
    if(0 < clientWidth && $("#" + this.calendarId).width() + pos.left > clientWidth) {
        left = pos.left + $('#' + this.inputId).width() - $("#" + this.calendarId).width() + ((/msie/i.test(navigator.userAgent) && !(/opera/i.test(navigator.userAgent)))? 5 : 0) ;
		__method.mOpts.theme=='ta' && (left += 50);
	}
    $("#" + this.calendarId).css('left', left  + 'px');
    //$("#" + this.calendarId).css('top', pos.top + (offsetHeight ? offsetHeight- 1 : (__method.mOpts.theme=='ta'?35:22)) + 'px');
	$("#" + this.calendarId).css('top', pos.top + (__method.mOpts.theme=='ta'?35:22) + 'px');
	//第一次显示的时候，一定要初始化输入框
	isCompare ? this.changeInput(this.startCompareDateId) : this.changeInput(this.startDateId);
    return false;
};

/**
 * @description 关闭日期选择框
 * @param {Boolean} btnSubmit 是否是点击确定按钮关闭的 
 */
pickerDateRange.prototype.close = function(btnSubmit) {
	var __method = this;
    //by zacharycai 关闭后就解绑了
    //$(document).unbind('click');

    // 把开始、结束时间显示到输入框 (PS:如果选择的今日，昨日，则只填入一个日期)
    // 如果开始和结束同个时间也照样分段by zacharycai
    //$('#' + this.inputId).val($('#' + this.startDateId).val() + ($('#' + this.startDateId).val() == $('#' + this.endDateId).val() ? '' : this.mOpts.defaultText + $('#' + this.endDateId).val()));
	if(btnSubmit){
		//如果是单日快捷选择
		if (this.mOpts.shortOpr === true){
			$('#' + this.inputId).val($('#' + this.startDateId).val());
			$('#' + this.inputCompareId).val($('#' + this.startCompareDateId).val());
		}else{
			$('#' + this.inputId).val($('#' + this.startDateId).val() + ('' == $('#' + this.endDateId).val() ? '' : this.mOpts.defaultText + $('#' + this.endDateId).val()));
		}
		//判断当前天是否可选，来决定从后往前推修改日期是从哪一点开始
		var nDateTime = ((true == this.mOpts.isTodayValid && '' != this.mOpts.isTodayValid)) ? new Date().getTime() : new Date().getTime() - (1 * 24 * 60 * 60 * 1000);
		var bDateTime = this.str2date($('#' + this.startDateId).val()).getTime();
		var eDateTime = this.str2date($('#' + this.endDateId).val()).getTime();
		//如果endDateTime小于bDateTime 相互交换
		if(eDateTime < bDateTime){
			var tmp = $('#' + this.startDateId).val();
			$('#' + this.startDateId).val($('#' + this.endDateId).val());
			$('#' + this.endDateId).val(tmp);
		}
		 var _val = this.mOpts.shortOpr == true ? $('#' + this.startDateId).val() : ($('#' + this.startDateId).val() + ('' == $('#' + this.endDateId).val() ? '' : this.mOpts.defaultText + $('#' + this.endDateId).val()));
		// 把开始、结束时间显示到输入框 (PS:如果选择的今日，昨日，则只填入一个日期)
		var input = document.getElementById(this.inputId);
		if(input && input.tagName == 'INPUT'){
			$('#' + this.inputId).val(_val);
			$('#'+this.inputCompareId).is(':visible') && $('#'+this.inputCompareId).val(_compareVal);
		}else{
			$('#' + this.inputId).html(_val);
			$('#'+this.inputCompareId).is(':visible') && $('#'+this.inputCompareId).html(_compareVal);
		}
		//	//在js侧就做好日期校准，以前面的日期选择的跨度为准，如果后面的跨度超过了当前可用时间，则以当前可用时间向前推 added by johnnyzheng 11-29
		if(this.mOpts.theme != 'ta'){
			if('' !=  $('#' + this.startCompareDateId).val() && '' != $('#' + this.endCompareDateId).val()){
				var bcDateTime = this.str2date($('#' + this.startCompareDateId).val()).getTime();
				var ecDateTime = this.str2date($('#' + this.endCompareDateId).val()).getTime();
				var _ecDateTime = bcDateTime + eDateTime - bDateTime;
				if(_ecDateTime > nDateTime){
				//如果计算得到的时间超过了当前可用时间，那么就和服务器端保持一致，将当前可用的天数向前推日期选择器的跨度 added by johnnyzheng 11-29
					_ecDateTime = nDateTime;
					$('#' + this.startCompareDateId).val(this.formatDate(this.date2ymd(new Date(_ecDateTime + bDateTime - eDateTime)).join('-')));
				}
				$('#' + this.endCompareDateId).val(this.formatDate(this.date2ymd(new Date(_ecDateTime)).join('-')));
				
				//把开始结束对比时间大小重新矫正一下
				var bcDateTime = this.str2date($('#' + this.startCompareDateId).val()).getTime();
				var ecDateTime = this.str2date($('#' + this.endCompareDateId).val()).getTime();
				if(ecDateTime < bcDateTime){
					var tmp = $('#' + this.startCompareDateId).val();
					$('#' + this.startCompareDateId).val($('#' + this.endCompareDateId).val());
					$('#' + this.endCompareDateId).val(tmp);
				}
			}
		}
		//把对比时间填入输入框 (PS:如果选择今日，昨日，则只填入一个日期)
		//$('#' + this.inputCompareId).val($('#' + this.startCompareDateId).val() + this.mOpts.defaultText + $('#' + this.endCompareDateId).val());
		var _compareVal = this.mOpts.shortOpr == true ? $('#' + this.startCompareDateId).val() : ($('#' + this.startCompareDateId).val() + ('' == $('#' + this.endCompareDateId).val() ? '' : this.mOpts.defaultText + $('#' + this.endCompareDateId).val()));
		if(input && input.tagName == 'INPUT'){
				$('#' + this.inputCompareId).val(_compareVal);
		}else{
				$('#' + this.inputCompareId).html(_compareVal);
		}
		// 计算相隔天数
		var step = (bDateTime - eDateTime) / 86400000;

		// 更改目标元素值
		$('#' + this.mOpts.startDateId).val($('#' + this.startDateId).val());
		$('#' + this.mOpts.endDateId).val($('#' + this.endDateId).val());
		$('#' + this.mOpts.startCompareDateId).val($('#' + this.startCompareDateId).val());
		$('#' + this.mOpts.endCompareDateId).val($('#' + this.endCompareDateId).val());
		//点击确定按钮进行查询后将取消所有的今天 昨天 最近7天的快捷链接 added by johnnyzheng 11-29
		for(var property in this.periodObj){
			if($('#' + this.mOpts[property])){
				$('#' + this.mOpts[property]).parent().removeClass('a');
			}
		}
	}
	// 隐藏日期选择框 延迟200ms 关闭日期选择框
	$("#" + __method.calendarId).css('display', 'none');
    return false;
};

/**
 * @description 日期填充函数
 * @param {Num} year 年
 * @param {Num} month 月
 */ 
pickerDateRange.prototype.fillDate = function(year, month, index) {
    var __method = this;
	var isTaTheme = this.mOpts.theme == 'ta';
    // 当月第一天
    var firstDayOfMonth = new Date(year, month, 1);
    var dateBegin = new Date(year, month, 1);
    var w = dateBegin.getDay();
    // 计算应该开始的日期
    dateBegin.setDate(1 - w);

    // 当月最后一天
    var lastDayOfMonth = new Date(year, month + 1, 0);
    var dateEnd = new Date(year, month + 1, 0);
    w = dateEnd.getDay();
    // 计算应该结束的日期
    dateEnd.setDate(dateEnd.getDate() + 6 - w);

    var today = new Date();
    var dToday = today.getDate();
    var mToday = today.getMonth();
    var yToday = today.getFullYear();
	
	var table = document.createElement('table');
	if(isTaTheme){
		table.className = this.mOpts.dateTable;

		cap = document.createElement('caption');
		
		//如果是magicSelect，用户自定义的选择年和月份
		if(this.mOpts.magicSelect){
			var yh = ['<div class="select_wrap" id="yselected_'+index+'"><div class="select" id="_ybtn_'+index+'">'+year+'</div><div class="dropdown" id="_yctn_'+index+'"><ul class="list_menu">']
			var mh = ['<div class="select_wrap" id="mselected_'+index+'"><div class="select" id="_mbtn_'+index+'">'+(month+1)+'</div><div class="dropdown" id="_mctn_'+index+'"><ul class="list_menu">']
		
			//var yh = ['<select name="yselected_'+index+'" class="xxxs">'];
			//var mh = ['<select name="mselected_'+index+'" class="xxxs">'];
			i=1;
			yt = yToday;
			do{
				//yh.push('<option value="'+yt+'" '+(yt == year? 'selected' : '')+'>'+(yt--)+'</option>');
				//mh.push('<option value="'+i+'" '+(i == (month+1)? 'selected' : '')+'>'+(i++)+'</option>');
				yh.push('<li><a href="javascript:;">'+(yt--)+'</a></li>');
				mh.push('<li><a href="javascript:;">'+(i++)+'</a></li>');
			}while(i <= 12);
			//yh.push('</select>');
			//mh.push('</select>');		
			yh.push('</ul></div></div>');
			mh.push('</ul></div></div>');			
			$(cap).append(yh.join('') +'<span class="joinLine"> 年 </span>'+mh.join('')+'<span class="joinLine"> 月 </span>');

		}
		else{
			$(cap).append(year + '年' + (month + 1) + '月');
		}
		
		$(table).append(cap);
		thead = document.createElement('thead');
		tr = document.createElement('tr');
		var days = ['日', '一', '二', '三', '四', '五', '六'];
		for(var i = 0; i < 7; i ++) {
			th = document.createElement('th');
			$(th).append(days[i]);
			$(tr).append(th);
		}
		$(thead).append(tr);
		$(table).append(thead);
		
		tr = document.createElement('tr');
		td = document.createElement('td');
		// 如果是最后一个月的日期，则加上下一个月的链接
		if(!this.mOpts.magicSelect){
			if(0 == index) {
				$(td).append('<a href="javascript:void(0);" id="' + this.nextMonth + '"><i class="i_next"></i></a>');
			}
			// 如果是第一个月的日期，则加上上一个月的链接
			if(index + 1 == this.mOpts.calendars) {
				$(td).append('<a href="javascript:void(0);" id="' + this.preMonth + '"><i class="i_pre"></i></a>');
			}
		}
		
	//    $(td).append('<span style="font-size:16px">' + year + '年' + (month + 1) + '月' + '</span>');
		$(td).attr('colSpan', 7);
		$(td).css('text-align', 'center');
		$(tr).append(td);
		$(table).append(tr);
	}
	else{
		table.className = this.mOpts.theme + '_' + this.mOpts.dateTable;

		tr = document.createElement('tr');
		td = document.createElement('td');
		// 如果是最后一个月的日期，则加上下一个月的链接
		if(0 == index) {
			$(td).append('<a href="javascript:void(0);" id="' + this.nextMonth + '" class="gri_dateRangeNextMonth"><span>next</span></a>');
		}
		// 如果是第一个月的日期，则加上上一个月的链接
		if(index + 1 == this.mOpts.calendars) {
			$(td).append('<a href="javascript:void(0);" id="' + this.preMonth + '" class="gri_dateRangePreMonth"><span>pre</span></a>');
		}
		$(td).append(year + '年' + (month + 1) + '月');
		$(td).attr('colSpan', 7);
		$(td).css('text-align', 'center');
		$(td).css('background-color', '#F9F9F9');
		$(tr).append(td);
		$(table).append(tr);

		var days = ['日', '一', '二', '三', '四', '五', '六'];
		tr = document.createElement('tr');
		for(var i = 0; i < 7; i ++) {
			td = document.createElement('td');
			$(td).append(days[i]);
			$(tr).append(td);
		}
		$(table).append(tr);
	}
    // 当前月的所有日期(包括空白位置填充的日期)
    var tdClass = '', deviation = 0, ymd = '';
    for(var d = dateBegin; d.getTime() <= dateEnd.getTime(); d.setDate(d.getDate() + 1)) {
        if(d.getTime() < firstDayOfMonth.getTime()) { // 当前月之前的日期
            tdClass = this.mOpts.theme + '_' + this.mOpts.disableGray;
            deviation = '-1';
        } else if(d.getTime() > lastDayOfMonth.getTime()) { // 当前月之后的日期
            tdClass = this.mOpts.theme + '_' + this.mOpts.disableGray;
            deviation = '1';
        } else if((this.mOpts.stopToday == true && d.getTime() > today.getTime()) || d.getTime() < __method.mOpts.minValidDate * 1000 || ('' !== __method.mOpts.maxValidDate && d.getTime() > __method.mOpts.maxValidDate * 1000)) { // 当前时间之后的日期，或者开启统计之前的日期
            tdClass = this.mOpts.theme + '_' + this.mOpts.disableGray;
            deviation = '2';
        } else { // 当前月日期
            deviation = '0';
            if(d.getDate() == dToday && d.getMonth() == mToday && d.getFullYear() == yToday) {
                if (true == this.mOpts.isTodayValid) {
                    tdClass = this.mOpts.theme + '_' + this.mOpts.isToday;
                } else {
                    tdClass = this.mOpts.theme + '_' + this.mOpts.disableGray;
                    deviation = '2';
                }
            }
			else {
                tdClass = '';
            }
			//让周末不可选不可选
			if(this.mOpts.weekendDis && (d.getDay()==6 || d.getDay()==0)){
				tdClass = this.mOpts.theme + '_' + this.mOpts.disableGray;
				deviation = '3';
			}
			//让周几不可选
			if(this.mOpts.disCertainDay && this.mOpts.disCertainDay.length > 0 ){
				for(var p in this.mOpts.disCertainDay){
					if(!isNaN(this.mOpts.disCertainDay[p]) && d.getDay() === this.mOpts.disCertainDay[p]){
						tdClass = this.mOpts.theme + '_' + this.mOpts.disableGray;
						deviation = '4';
					}
				}
			}
            //让几号不可选
            if(this.mOpts.disCertainDate && this.mOpts.disCertainDate.length > 0 ){
                var isDisabled = false;

                for(var p in this.mOpts.disCertainDate){
                    if(!isNaN(this.mOpts.disCertainDate[p]) || isNaN(parseInt(this.mOpts.disCertainDate[p]))){
                        if ( this.mOpts.disCertainDate[0] === true ){
                            isDisabled = !!(d.getDate() !== this.mOpts.disCertainDate[p]);
                            if ( !isDisabled ){
                                break;
                            }
                        }else {
                            isDisabled = !!(d.getDate() === this.mOpts.disCertainDate[p]);
                            if ( isDisabled ){
                                break;
                            }
                        }

                    }
                }

                if ( isDisabled ){
                    tdClass = this.mOpts.theme + '_' + this.mOpts.disableGray;
                    deviation = '4';
                }

            }
        }

        // 如果是周日
        if(0 == d.getDay()) {
            tr = document.createElement('tr');
        }

        td = document.createElement('td');
        td.innerHTML = d.getDate();
        if('' != tdClass) {
            $(td).attr('class', tdClass);
        }

        // 只有当前月可以点击
        if(0 == deviation) {
            ymd = d.getFullYear() + '-' + (d.getMonth() + 1) + '-' + d.getDate();
            $(td).attr('id', __method.calendarId + '_' + ymd);
			$(td).css('cursor','pointer');
            (function(ymd) {
                $(td).bind("click", ymd, function() {
                    __method.selectDate(ymd);
                    return false;
                });
            })(ymd);
        }

        $(tr).append(td);

        // 如果是周六
        if(6 == d.getDay()) {
            $(table).append(tr);
        }
    }

    return table;
};

/**
 * @description 把时间字串转成时间格式
 * @param {String} str 时间字符串
 */ 
pickerDateRange.prototype.str2date = function(str) {
    var ar = str.split('-');
    // 返回日期格式
    return new Date(ar[0], ar[1] - 1, ar[2]);
};

/**
 * @description 比较两个时间字串的大小:1 大于; 0 等于; -1 小于
 * @param {String} b 待比较时间串1
 * @param {String} e 待比较时间串2
 */
pickerDateRange.prototype.compareStrDate = function(b, e) {
    var bDate = this.str2date(b);
    var eDate = this.str2date(e);

    // 1 大于; 0 等于; -1 小于
    if(bDate.getTime() > eDate.getTime()) {
        return 1;
    } else if(bDate.getTime() == eDate.getTime()) {
        return 0;
    } else {
        return -1;
    }
};

/**
 * @description 把时间格式转成对象
 * @param {Date} d 时间
 */ 
pickerDateRange.prototype.date2ymd = function(d) {
    return [d.getFullYear(), (d.getMonth() + 1), d.getDate()];
};

/**
 * @description 切换焦点到当前输入框
 * @param {String} 日期框体ID
 */
pickerDateRange.prototype.changeInput = function(ipt) {
    // 强制修改为开始输入框
    if (true == this.mOpts.isSingleDay) {
        ipt = this.startDateId;
    }
    // 所有4个输入框
    var allInputs = [this.startDateId, this.startCompareDateId, this.endDateId, this.endCompareDateId];

    // 如果 ipt 是日期输入框，则为日期样式，否则为对比日期样式
    var cla = '';
    if(ipt == this.startDateId || ipt == this.endDateId) {
        cla = this.mOpts.theme + '_' + this.mOpts.selectCss;
    } else {
        cla = this.mOpts.theme + '_' + this.mOpts.compareCss;
    }
    if(ipt == this.endDateId && this.mOpts.singleCompare) {
        cla = this.mOpts.theme + '_' + this.mOpts.compareCss;
    }

    // 移除所有输入框的附加样式
    for(var i in allInputs) {
        $('#' + allInputs[i]).removeClass(this.mOpts.theme + '_' + this.mOpts.selectCss);
        $('#' + allInputs[i]).removeClass(this.mOpts.theme + '_' + this.mOpts.compareCss);
    }

    // 为指定输入框添加样式
    $('#' + ipt).addClass(cla);
	//背景图repeat
	$('#' + ipt).css('background-repeat', 'repeat');
    // 把输入焦点移到指定输入框
    this.dateInput = ipt;
};

/**
 * @description 日期格式化，加前导零
 */ 
pickerDateRange.prototype.formatDate = function(ymd) {
    return ymd.replace(/(\d{4})\-(\d{1,2})\-(\d{1,2})/g, function(ymdFormatDate, y, m, d){
        if(m < 10){
            m = '0' + m;
        }
        if(d < 10){
            d = '0' + d;
        }
        return y + '-' + m + '-' + d;
    });
};

// cookie util
var CookieUtil = {
    __prefix: '_lvae_',
    get: function(name) {
        name = this.__prefix + name;
        var cookieName = encodeURIComponent(name) + "=",
            cookieStart = document.cookie.indexOf(cookieName),
            cookieValue = null;

        if (cookieStart > -1) {
            var cookieEnd = document.cookie.indexOf(";", cookieStart);
            if (cookieEnd == -1) {
                cookieEnd = document.cookie.length;
            }
            cookieValue = decodeURIComponent(document.cookie.substring(cookieStart + cookieName.length, cookieEnd));
        }

        return cookieValue;
    },

    set: function(name, value, expires, path, domain, secure) {
        name = this.__prefix + name;
        var cookieText = encodeURIComponent(name) + "=" + encodeURIComponent(value);
        
        if (expires) {
            var expiresTime = new Date();
            expiresTime.setTime(expires);
            cookieText += "; expires=" + expiresTime.toGMTString();
        }

        if (path) {
            cookieText += "; path=" + path;
        } else {
            cookieText += "; path=/";
        }

        if (domain) {
            cookieText += "; domain=" + domain;
        } else {
            //cookieText += "; domain=.spark.com";
        }

        if (secure) {
            cookieText += "; secure";
        }

        document.cookie = cookieText;
    },

    unset: function(name, path, domain, secure) {
        this.set(name, "", new Date(0), path, domain, secure);
    },
};

//  spark analyse engine date object
function AE_Date(showDateId, triggerId, callback, startDate, endDate, isDate) {
	var startDateStr = '';
	var endDateStr = '';
	if(isDate) {
		startDateStr = date2YmdStr(startDate);
		endDateStr = date2YmdStr(endDate);
	} else if(startDate){
		startDateStr = startDate;
		endDateStr = endDate;
	} else {
		startDateStr = date2YmdStr(getYesterDay());
		endDateStr = startDateStr;
	}
	new pickerDateRange(showDateId, {
		isTodayValid : true,
		startDate : startDateStr,
		endDate : endDateStr,
		needCompare : false,
		defaultText : ' 至 ',
		autoSubmit : true,
		inputTrigger : triggerId,
		theme : 'ta',
		success : function(obj) {
			setStartDate(obj.startDate); // 更新时间
            setEndDate(obj.endDate); // 更新时间
			callback(obj.startDate, obj.endDate);
		}
	});
};

function getYesterDay() {
	var today=new Date();
	var yesterday_milliseconds=today.getTime()-1000*60*60*24;
	var yesterday=new Date();
	yesterday.setTime(yesterday_milliseconds);
	return yesterday;
	// 直接使用最后一天
	//return ymdStr2Date(getEndDate());
};

function ymdStr2Date(str) {
    return new Date(str.replace(/-/g, "/"));
};

function date2YmdStr(date){
	var y = date.getFullYear();
	var m = (date.getMonth() + 1);
	var d = date.getDate();
	if(m < 10){
		m = '0' + m;
    }
    if(d < 10){
        d = '0' + d;
	}
    return y + '-' + m + '-' + d;
};


function getSpecialDate(d, amount) {
    var mesc = d.getTime() + amount * 60 *60 *24 * 1000;
    var dd = new Date();
    dd.setTime(mesc);
    return dd;
};

function setStartDate(sd) {
    CookieUtil.set("startDate", sd);
};

function getStartDate() {
    var sd = CookieUtil.get("startDate");
    if (sd) {
        return sd;
    } else {
        sd = date2YmdStr(getSpecialDate(new Date(), -8));
        setStartDate(sd);
    }
    return sd;
};

function setEndDate(ed) {
    CookieUtil.set("endDate", ed);
};

function getEndDate() {
    var ed = CookieUtil.get("endDate");
    if (ed) {
        return ed;
    } else {
        ed = date2YmdStr(getSpecialDate(new Date(), -1));
        setEndDate(ed);
    }
    return ed;
};

function formatSecs(secs) {
	if (secs && !isNaN(secs)) {
		var hour = Math.floor(secs / 3600);
		var minute = Math.floor((secs - hour * 3600) / 60);
		var secs = secs - hour * 3600 - minute * 60;
		var result = "";
		if (hour > 0) {
			result += hour + "小时";
		}
		if (minute > 0) {
			result += minute + "分钟";
		}
		if (secs > 0) {
			result += secs + "秒";
		}
		return result;
	}
	return secs;
};

