Highcharts.setOptions({
	global : {
		useUTC : false
	},
	// 禁用版权信息
	credits : {
		enabled : false
	},
	lang:{
		printChart: '打印',
		downloadPNG: '下载PNG',
		downloadJPEG: '下载JPEG',
		downloadPDF: '下载PDF',
		downloadSVG: '下载SVG',
		contextButtonTitle: '下载'
	},
	// 设置颜色
	colors : ['#ff7f50', '#6AF9C4', '#87cefa', '#da70d6', '#FFF263',
	          '#32cd32', '#6495ed', '#ff69b4', '#FF9655', '#ba55d3',
	          '#cd5c5c', '#ffa500', '#40e0d0', '#64E572', '#1e90ff',
	          '#ff6347', '#7b68ee', '#00fa9a', '#ffd700', '#6b8e23',
	          '#24CBE5', '#ff00ff', '#3cb371', '#b8860b', '#30e0e0',
	          '#058DC7', '#50B432', '#ED561B', '#DDDF00'],
	title: {
		text: null
	},
	plotOptions: {
	    area: {
	        stacking: 'percent',
	        lineColor: '#ffffff',
	        lineWidth: 1,
	        marker: {
	            lineWidth: 1,
	            lineColor: '#ffffff'
	        }
	    },
	    pie: {
            showInLegend: true
        },
        line: {
            tooltip: {
                pointFormat: '{series.name}:<b>{point.y}</b><br/>',
                shared: true
            }
        },
        column: {
            tooltip: {
                pointFormat: '{series.name}:<b>{point.y}</b><br/>'
            }
        }
	},
	tooltip: {
        pointFormat: '<span stype="color:{series.color}">{series.name}</span>:<b>{point.y:,0.1f}</b><br/>',
        shared: true
    },
    chart: {
        plotBackgroundColor: null,
        plotBorderWidth: null,
        plotShadow: false
    }
});

Number.prototype.toPercent = function(){
    return Number((Math.round(this * 10000)/100).toFixed(2));
};

Number.prototype.formatThousands = function() {
    if (this == 0) {
        return "0";
    }
    var num = (this).toString();
    var tmp = num.length % 3;
    switch(tmp) {
    case 1:
        num = '00' + num;
        break;
    case 2:
        num = '0' + num;
    }
    return num.match(/\d{3}/g).join(",").replace(/^0+/, '');
};

function showNoConnent(id, msg) {
    var str = "数据库中没有对应时间的数据，请修改时间后重新查询。";
    if (msg) {
        str = msg;
    }
    $("#" + id).html('<div class="ibox no-margins"><div class="ibox-title">提示信息:</div><div class="ibox-content"><span class="font-bold">' + str + '</span></div></div>');
};

function cleanContent(id) {
    $("#" + id).html("");
};

function uniqueArray(arr) {
    arr.sort();
    var re = [arr[0]];
    for (var i = 1; i < arr.length; i++) {
        if (arr[i] !== re[re.length - 1]) {
            re.push(arr[i]);
        }
    }
    return re;
};

function indexOfValue(array, item) {
    for (var i = 0; i < array.length; i++) {
        if (item == array[i]) {
        	return i;
        }
    }
    return -1;
};