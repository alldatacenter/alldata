<html>

<head>
    <meta charset="GB2312">
    <title>test1</title>
    <script type="text/javascript" src="./js/analytics.js"></script>
</head>
<body>
test page3<br/>
<label>orderid:123456</label><br/>
<label>orderName:test_order_123456</label><br/>

<label>currencyAmount:524.01</label><br/>

<label>paymentType:alipay</label><br/>

<button onclick="__AE__.onEventDuration('event_category_name', 'event_action_name', {'key1':'value1', 'key2':'value2'}, 1245)">map and duration event
</button>

<button onclick="__AE__.onEventDuration('event_category_name', 'event_action_name')">no map and duration event</button>

refer to:
<a href="demo.jsp">demo</a>
<a href="demo2.jsp">demo2</a>
<a href="demo3.jsp">demo3</a>
<a href="demo4.jsp">demo4</a>

</body>
</html>
