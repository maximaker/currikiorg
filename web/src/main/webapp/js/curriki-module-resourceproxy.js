function postMessageHandler(c){var b=c.data;var a=b.substr(0,b.indexOf(":"));var d=b.substr(b.indexOf(":")+1);
switch(a){case"resize":console.log("resource display: recieved resize event");resizeCurrikiIframe(d);
break}}function resizeCurrikiIframe(a){document.getElementById("curriki_resource_frame").setAttribute("style",a)
}if(typeof window.attachEvent==="function"||typeof window.attachEvent==="object"){console.log("search: attached Listener to evenet via window.attachEvent");
window.attachEvent("onmessage",postMessageHandler)}else{if(typeof window.addEventListener==="function"){console.log("search: attached Listener to evenet via window.addEvenListener");
window.addEventListener("message",postMessageHandler,false)}else{if(typeof document.attachEvent==="function"){console.log("search: cors iframe communication is not possible");
document.attachEvent("onmessage",postMessageHandler)}else{console.log("Frame communication not possible")
}}}(function(){Ext.ns("Curriki.module.resourceproxy");var a=Curriki.module.resourceproxy;
a.settings={proxyUrl:"http://current.dev.curriki.org"};a.run=function(){console.log("resourceproxy: starting");
var b=a.getResourceUrlFromParams();a.renderPage(b)};a.getResourceUrlFromParams=function(){var b=Ext.urlDecode(location.search.substring(1));
if(!(typeof b.resourceurl==="undefined")){return b.resourceurl}else{document.write("Please provide a resource to display");
throw"EmbeddedDisplay Error: No ressourceurl defined"}};a.renderPage=function(c){var b=document.getElementById("curriki_resource_frame");
b.setAttribute("src",a.settings.proxyUrl+unescape(c))};Ext.onReady(function(){a.run()
})})();