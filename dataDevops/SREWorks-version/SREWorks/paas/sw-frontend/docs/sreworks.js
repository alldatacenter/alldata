
console.log('sreworks document works!');

if(window.self !== window.top){
	var links = document.querySelectorAll(".chapter a");
	for (var i=0; i<links.length; i++)
	{
        links[i].addEventListener('click', function(event) {
            var href = '/#/help/book/'+this.href.split("/docs/")[1].replace(/^\/|\/$/g, '');
            console.log(href);
            window.top.history.pushState('default', 'default', href);
        })
	}

}


