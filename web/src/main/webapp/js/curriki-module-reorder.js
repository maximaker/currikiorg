(function(){Ext.ns("Curriki.module.reorder");Ext.ns("Curriki.data.reorder");var A=Curriki.module.reorder;var C=Curriki.data.reorder;var D=Curriki.ui;var B="mycurriki.collections.reorder.";if(C.place&&C.place==="groups"){B="groups_curriculum_collections_reorder."}A.init=function(){if(!C.place||!C.which){return false}C.orig=[];A.store=new Ext.data.JsonStore({storeId:"CollectionsStore",url:"/xwiki/curriki/"+C.place+"/"+C.which+"/collections?_dc="+(new Date().getTime()),fields:["displayTitle","collectionPage"],autoLoad:true,listeners:{load:{fn:function(F,E,G){var H=[];F.each(function(I){H.push(I.data.collectionPage)});C.orig=H;console.log("Fetched list",H)}}}});A.mainDlg=Ext.extend(D.dialog.Messages,{initComponent:function(){Ext.apply(this,{id:"ReorderDialogueWindow",title:_(B+"dialog_title"),cls:"reorder resource",autoScroll:false,width:634,items:[{xtype:"panel",id:"guidingquestion-container",cls:"guidingquestion-container",items:[{xtype:"box",autoEl:{tag:"div",html:_(B+"guidingquestion"),cls:"guidingquestion"}},{xtype:"box",autoEl:{tag:"div",html:_(B+"instruction"),cls:"instruction"}}]},{xtype:"panel",id:"collections-list-panel",cls:"collections-list",bbar:["->",{text:_(B+"cancel.btt"),id:"cancelbutton",cls:"button button-cancel",listeners:{click:{fn:function(){this.close();if(Ext.isIE){window.location.reload()}},scope:this}}},{text:_(B+"next.btt"),id:"nextbutton",cls:"button button-confirm",listeners:{click:{fn:function(){var E=[];Ext.getCmp("reorderCollectionsMS").store.each(function(H){E.push(H.data.collectionPage)});console.log("Reordering",E);var F=this;var G=function(H){console.log("Reorder callback",H);F.close();A.msgComplete();window.location.reload()};Curriki.assets.ReorderRootCollection(C.place,C.which,C.orig,E,G)},scope:this}}}],items:[{xtype:"box",autoEl:{tag:"div",html:_(B+"listheader"),cls:"listheader"}},{xtype:"form",id:"ReorderDialoguePanel",formId:"ReorderDialogueForm",labelWidth:25,autoScroll:false,border:false,defaults:{labelSeparator:""},listeners:{},items:[{xtype:"multiselect",id:"reorderCollectionsMS",name:"reorderCollections",hideLabel:true,border:false,enableToolbar:false,legend:" ",store:A.store,valueField:"collectionPage",displayField:'[""]}<span class="resource-CollectionComposite"><img class="x-tree-node-icon assettype-icon" src="'+Ext.BLANK_IMAGE_URL+'" /></span> {displayTitle',width:600,height:200,allowBlank:false,preventMark:true,minLength:1,isFormField:true,dragGroup:"reorderMSGroup",dropGroup:"reorderMSGroup"}]}]}]});A.mainDlg.superclass.initComponent.call(this)}});Ext.reg("reorderDialog",A.mainDlg);A.msgComplete=function(){Curriki.logView("/features/reorder/"+(C.place==="groups"?"groups":"mycurriki")+"/saved");alert(_(B+"set.confirm"))};A.initialized=true;return true};A.confirmMsg={first:function(){return confirm(_(B+"checkfirst.dialog"))},after:function(){return confirm(_(B+"checkafter.dialog"))},display:function(){if(!C.reordered){return A.confirmMsg.first()}else{return A.confirmMsg.after()}}};A.display=function(){if(A.init()){D.show("reorderDialog");Curriki.logView("/features/reorder/"+(C.place==="groups"?"groups":"mycurriki")+"/started")}};A.start=function(){Ext.onReady(function(){if(A.confirmMsg.display()){A.display()}})};Ext.onReady(function(){if(!C.place||!C.which){return }var E=Ext.DomQuery.selectNode("#hider-link");if(E){Ext.DomHelper.insertBefore(E,{id:"reorder-link",tag:"a",cls:"reorder-link",onclick:"Curriki.module.reorder.start(); return false;",html:_(B+"link")});Ext.DomHelper.insertBefore(E,{id:"reorder-sep",tag:"span",cls:"separator",html:"|"})}})})();