Ext.ns("Curriki.module.unnominate");Curriki.module.unnominate.confirm=function(){Ext.Msg.show({title:_("curriki.crs.review.form.dialog.confirm_header"),msg:_("curriki.crs.review.unnominateconfirm"),buttons:Ext.Msg.OKCANCEL,fn:function(a){if(a=="ok"){var b=Ext.get("assetFullName").dom.value;
Curriki.assets.UnnominateAsset(b,function(c){window.location.pathname="xwiki/bin/view/CRS/Reviews"
})}else{document.getElementById("unnominate").checked=false}},icon:Ext.MessageBox.QUESTION})
};