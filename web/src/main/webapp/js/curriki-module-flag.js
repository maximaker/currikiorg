(function(){Ext.ns("Curriki.module.flag");Ext.ns("Curriki.data.flag");var c=Curriki.module.flag;
var a=Curriki.data.flag;var b=Curriki.ui;c.init=function(){var e="flag.dialog.reason.";
a.reasonList=[];var d=1;while(_(e+d)!==e+d){a.reasonList.push([d,_(e+d),d]);++d}a.reasonList.push(["alt",_(e+"alt"),d]);
c.reasonStore=new Ext.data.SimpleStore({fields:["id","text","sortValue"],sortInfo:{field:"sortValue",direction:"ASC"},data:a.reasonList,id:0});
c.mainDlg=Ext.extend(b.dialog.Actions,{initComponent:function(){Ext.apply(this,{id:"FlagDialogueWindow",title:_("flag.dialog.header"),cls:"flag resource resource-view",autoScroll:false,items:[{xtype:"form",id:"FlagDialoguePanel",formId:"FlagDialogueForm",labelWidth:200,labelAlign:"top",autoScroll:false,border:false,defaults:{labelSeparator:""},monitorValid:true,buttonAlign:"right",buttons:[{text:_("flag.dialog.cancel.btt"),id:"cancelbutton",cls:"button button-cancel",listeners:{click:{fn:function(){Curriki.logView("/features/flag/cancelled");
this.close();if(Ext.isIE){window.location.reload()}},scope:this}}},{text:_("flag.dialog.submit.btt"),id:"nextbutton",cls:"button button-confirm",formBind:true,listeners:{click:{fn:function(){console.log("Flaging",a.resourcePage);
var h=this;var i=function(j){console.log("Flag callback",j);Curriki.logView("/features/flag/flagged/"+a.resourcePage.replace(".","/"));
h.close();c.msgComplete()};var g=Ext.getCmp("flag-reason").getValue();var f=Ext.getCmp("flag-alt-reason")?Ext.getCmp("flag-alt-reason").getValue():"";
Curriki.assets.Flag(a.resourcePage,g,f,i)},scope:this}}}],items:[{xtype:"box",autoEl:{tag:"div",html:_("flag.dialog.guidingquestion1.txt"),cls:"guidingquestion"}},{xtype:"box",autoEl:{tag:"div",html:_("flag.dialog.instruction1.txt"),cls:"instruction"}},{xtype:"box",autoEl:{tag:"div",html:_("flag.dialog.guidingquestion2.txt"),cls:"guidingquestion"}},{xtype:"combo",id:"flag-reason",hiddenName:"flagReason",mode:"local",store:c.reasonStore,displayField:"text",valueField:"id",emptyText:_("flag.dialog.reason.unselected"),width:440,selectOnFocus:true,forceSelection:true,triggerAction:"all",allowBlank:false,editable:false,typeAhead:false,hideLabel:false,labelStyle:"instruction",labelClass:"instructionClass",labelCls:"instructionCls",fieldLabel:_("flag.dialog.required.field.icon")+_("flag.dialog.instruction2.txt"),clearCls:"",listeners:{select:{fn:function(j,f,g){var i=f.id;
var h=Ext.getCmp("FlagDialoguePanel").getForm().findField("flag-alt-reason");if(i==="alt"){h.show()
}else{h.hide()}}},render:{fn:function(f){f.getEl().up(".x-form-item").down("label").addClass("instruction");
f.focus()}}}},{xtype:"textarea",id:"flag-alt-reason",hiddenName:"flagAltReason",width:580,minLength:5,maxLength:300,allowBlank:false,hidden:true,hideLabel:false,labelStyle:"instruction",labelClass:"instructionClass",fieldLabel:_("flag.dialog.required.field.icon")+_("flag.dialog.instruction4.txt"),enableKeyEvents:true,listeners:{hide:{fn:function(f){f.disable();
f.getEl().up(".x-form-item").setDisplayed(false)}},show:{fn:function(f){f.enable();
f.getEl().up(".x-form-item").setDisplayed(true)}},render:{fn:function(f){f.getEl().up(".x-form-item").down("label").addClass("instruction")
}},keypress:{fn:function(g,h){if(g.getValue().length>=300){var f=h.getKey();if(!Ext.isIE&&(h.isNavKeyPress()||f==h.BACKSPACE||(f==h.DELETE&&h.button==-1))){return
}if(Ext.isIE&&(f==h.BACKSPACE||f==h.DELETE||h.isNavKeyPress()||f==h.HOME||f==h.END)){return
}h.preventDefault();h.stopPropagation();h.stopEvent()}}},invalid:{fn:function(f,g){if(f.getValue().length>300){f.setValue(f.getValue().substring(0,300))
}}}}},{xtype:"box",autoEl:{tag:"div",html:_("flag.dialog.instruction5.txt"),cls:"instruction"}}]}]});
c.mainDlg.superclass.initComponent.call(this)}});Ext.reg("flagDialog",c.mainDlg);
c.completeDlg=Ext.extend(b.dialog.Messages,{initComponent:function(){Ext.apply(this,{id:"FlagDialogueWindow",title:_("flag.success.dialog.header"),cls:"flag resource resource-view",autoScroll:false,buttonAlign:"right",buttons:[{text:_("flag.success.dialog.button"),id:"closebutton",cls:"button button-confirm",listeners:{click:{fn:function(){this.close();
if(Ext.isIE){window.location.reload()}},scope:this}}}],items:[{xtype:"box",autoEl:{tag:"div",cls:"mgn-top",html:_("flag.success.dialog.message")}}]});
c.completeDlg.superclass.initComponent.call(this)}});Ext.reg("flagCompleteDialog",c.completeDlg);
c.msgComplete=function(){b.show("flagCompleteDialog")};c.initialized=true;return c.initialized
};c.display=function(d){if(!Ext.isEmpty(d)&&(c.init())){a.resourcePage=d.resourcePage||null;
if(a.resourcePage){b.show("flagDialog");Curriki.logView("/features/flag/started")
}}};c.start=function(d){Ext.onReady(function(){c.display(d)})}})();