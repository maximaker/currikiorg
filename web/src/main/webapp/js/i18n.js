function _(){if(arguments.length<1){return""}if(i18nDict&&typeof i18nDict[arguments[0]]==="string"){var b=i18nDict[arguments[0]];
var a;if(Object.isArray(arguments[1])){a=arguments[1]}else{a=$A(arguments);a.shift()
}a.each(function(d,c){b=b.replace("{"+c+"}",d)});return b}else{return arguments[0]
}};