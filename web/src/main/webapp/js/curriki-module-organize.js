(function(){Ext.ns("Curriki.module.organize");Ext.ns("Curriki.data.organize");var A=Curriki.module.organize;var B=Curriki.data.organize;var C=Curriki.ui;A.init=function(){B.removed=[];B.moved=[];B.changedFolders=[];B.selected=null;B.confirmedCallback=Ext.emptyFn;C.treeLoader.Organize=function(D){C.treeLoader.Organize.superclass.constructor.call(this)};Ext.extend(C.treeLoader.Organize,C.treeLoader.Base,{setFullRollover:true,setAllowDrag:true});A.logCancelled=function(){var D=B.resource.replace(".","/");Curriki.logView("/features/resources/organize/"+D+"/cancelled")};A.logCompleted=function(){var D=B.resource.replace(".","/");Curriki.logView("/features/resources/organize/"+D+"/completed")};A.getMovedList=function(D){if("undefined"!=typeof D.attributes.origLocation){B.moved.push(D)}if(D.hasChildNodes()){D.eachChild(A.getMovedList)}};A.mainDlg=Ext.extend(C.dialog.Messages,{initComponent:function(){Ext.apply(this,{id:"OrganizeDialogueWindow",title:_("organize.dialog_header"),cls:"organize resource resource-edit",autoScroll:false,width:634,bbar:[{text:_("organize.dialog.remove_button"),id:"organize-remove-btn",cls:"button remove",disabled:true,listeners:{click:function(D,E){if(B.selected!==null){B.removed.push(B.selected.remove());D.disable()}}}},"->",{text:_("organize.dialog.cancel_button"),id:"organize-cancel-btn",cls:"button cancel",listeners:{click:{fn:function(D,E){A.logCancelled();this.close();if(Ext.isIE){window.location.reload()}},scope:this}}},{text:_("organize.dialog.done_button"),id:"organize-done-btn",cls:"button done",disabled:true,listeners:{click:{fn:function(F,H){var G=[];console.log("Organizing",G);Ext.getCmp("organize-tree-cmp").getRootNode().eachChild(A.getMovedList);B.changedFolders=B.moved.concat(B.removed).collect(function(I){return I.attributes.origLocation.parentNode}).concat(B.moved.collect(function(I){return I.parentNode})).uniq();var E=function(){var I=function(){A.logCompleted();Curriki.hideLoading()};B.changedFolders.each(function(K){var J=I;I=function(){var L=K.childNodes.collect(function(O){return O.id});var N="";console.log("saving folder",K.id,L,K);var M=[];if("undefined"!=typeof K.attributes.addedNodes){K.attributes.addedNodes.uniq().each(function(O){M.push(O)})}if("undefined"!=typeof K.attributes.removedNodes){K.attributes.removedNodes.uniq().each(function(O){if(M.indexOf(O)==-1){N+=_("organize.history.removed_note",O.id,O.attributes.order+1)+" "}else{M.remove(O)}})}if("undefined"!=typeof K.attributes.addedNodes){M.each(function(O){N+=_("organize.history.inserted_note",O.id,L.indexOf(O.id)+1)+" "})}console.log("logging",N);if("function"==typeof J){J()}}});I()};B.changedFolders.each(function(J){var I=E;E=function(){Curriki.assets.GetMetadata(J.id,function(K){if(K.revision!=J.attributes.revision){Curriki.hideLoading();alert(_("organize.error.concurrency_text"));this.close();Ext.getCmp("OrganizeDialogueWindow").close();A.start(B.startInfo)}else{if("function"==typeof I){I()}}})}});var D=E;E=function(){Curriki.showLoading();D()};B.confirmedCallback=E;B.confirmMsg="";B.removed.each(function(I){B.confirmMsg+="<br />"+_("organize.confirmation.dialog_removed_listing",I.text,I.attributes.origLocation.parentNode.text)});B.moved.uniq().each(function(I){if(B.removed.indexOf(I)==-1){B.confirmMsg+="<br />"+_("organize.confirmation.dialog_moved_listing",I.text,I.attributes.origLocation.index,I.attributes.origLocation.parentNode.text,I.parentNode.indexOf(I)+1,I.parentNode.text)}});C.show("confirmOrganizeDlg")},scope:this}}}],items:[{xtype:"panel",id:"guidingquestion-container",cls:"guidingquestion-container",items:[{xtype:"box",autoEl:{tag:"div",html:_("organize.dialog.guidingquestion_text"),cls:"guidingquestion"}},{xtype:"box",autoEl:{tag:"div",html:_("organize.dialog.instruction_text"),cls:"instruction"}}]},{xtype:"panel",id:"organize-panel",cls:"organize-panel",items:[{xtype:"treepanel",loader:new C.treeLoader.Organize(),id:"organize-tree-cmp",autoScroll:true,maxHeight:390,useArrows:true,border:false,hlColor:"93C53C",hlDrop:false,cls:"organize-tree",animate:true,enableDD:true,ddScroll:true,containerScroll:true,rootVisible:true,listeners:{render:function(D){console.log("set up selectionchange",D);D.getSelectionModel().on("selectionchange",function(E,F){console.log("selection change",F,E);B.selected=F;if(F!==null){Ext.getCmp("organize-remove-btn").enable()}else{Ext.getCmp("organize-remove-btn").disable()}})},expandnode:{fn:function(E){var D=this.findById("organize-tree-cmp");if(!Ext.isEmpty(D)){D.fireEvent("afterlayout",D,D.getLayout())}},scope:this},afterlayout:function(E,D){if(this.afterlayout_maxheight){}else{if(E.getBox().height>E.maxHeight){E.setHeight(E.maxHeight);E.findParentByType("organizeDlg").center();this.afterlayout_maxheight=true}else{E.setHeight("auto")}}},movenode:function(D,H,F,G,E){console.log("moved node",H,F,G,E,D);if("undefined"===typeof G.attributes.addedNodes){G.attributes.addedNodes=[]}G.attributes.addedNodes.push(H)},beforeremove:function(D,F,G){console.log("before remove node",G,F,D);if("undefined"==typeof G.attributes.origLocation){var E=F.indexOf(G)+1;G.attributes.origLocation={parentResource:F.id,index:E,parentNode:F}}},remove:function(D,E,F){console.log("removed node",F,E,D);if("undefined"!=typeof E.attributes.addedNodes){E.attributes.addedNodes.remove(F)}if("undefined"===typeof F.attributes.origLocation.parentNode.attributes.removedNodes){F.attributes.origLocation.parentNode.attributes.removedNodes=[]}F.attributes.origLocation.parentNode.attributes.removedNodes.push(F);Ext.getCmp("organize-done-btn").enable()}},root:B.root}]}]});A.mainDlg.superclass.initComponent.call(this)}});Ext.reg("organizeDlg",A.mainDlg);A.confirmDlg=Ext.extend(C.dialog.Messages,{initComponent:function(){Ext.apply(this,{id:"ConfirmOrganizeDialogueWindow",title:_("organize.confirmation.dialog_header"),cls:"organize resource resource-edit",autoScroll:false,bbar:["->",{text:_("organize.dialog.cancel_button"),id:"organize-confirm-cancel-button",cls:"button cancel",listeners:{click:{fn:function(E,D){A.logCancelled();this.close()},scope:this}}},{text:_("organize.dialog.ok_button"),id:"organize-confirm-ok-button",cls:"button next",listeners:{click:{fn:function(E,D){B.confirmedCallback();this.close()},scope:this}}}],items:[{xtype:"box",autoEl:{tag:"div",html:_("organize.confirmation.dialog_summary_text")+"<br />"+B.confirmMsg+(B.removed.size()>0?("<br /><br />"+_("organize.confirmation.dialog_note_text")):"")+"<br /><br />"+_("organize.confirmation.dialog_instruction_text")}}]});A.confirmDlg.superclass.initComponent.call(this)}});Ext.reg("confirmOrganizeDlg",A.confirmDlg);A.intentionDlg=Ext.extend(C.dialog.Messages,{initComponent:function(){Ext.apply(this,{id:"IntentionOrganizeDialogueWindow",title:_("organize.intention.dialog_title"),cls:"organize resource resource-edit",autoScroll:false,bbar:[{text:_("organize.intention.message.continue_button"),id:"organize-intention-continue-button",cls:"button continue",listeners:{click:{fn:function(E,D){C.show("organizeDlg");this.close()},scope:this}}},{text:_("organize.dialog.cancel_button"),id:"organize-intention-cancel-button",cls:"button cancel",listeners:{click:{fn:function(E,D){A.logCancelled();this.close()},scope:this}}}],items:[{xtype:"box",autoEl:{tag:"div",html:_("organize.intention.message_text",B.title,B.creatorName)}}]});A.intentionDlg.superclass.initComponent.call(this)}});Ext.reg("intentOrganizeDlg",A.intentionDlg);A.display=function(){if(B.creator==Curriki.global.username||Curriki.global.isAdmin){C.show("organizeDlg")}else{C.show("intentOrganizeDlg")}};A.startMetadata=function(D){Curriki.assets.GetMetadata(D,function(E){console.log("returned",E);E.fwItems=E.fw_items;E.levels=E.educational_level;E.ict=E.instructional_component;E.displayTitle=E.title;E.rights=E.rightsList;E.leaf=false;E.allowDrag=false;E.allowDrop=true;E.expanded=true;var F=new C.treeLoader.Organize();B.root=F.createNode(E);console.log("created",B.root);B.root.addListener("beforecollapse",function(){return false});B.resource=D;Ext.onReady(function(){A.display()})})};return true};A.start=function(D){if(A.init()){if("undefined"==typeof D||"undefined"==typeof D.assetPage){alert("No resource to organize given.");return false}B.resource=D.assetPage;if("undefined"==typeof D.creator||((D.creator!=Curriki.global.username&&!Curriki.global.isAdmin)&&("undefined"==typeof D.title||"undefined"==typeof D.creatorName))){Curriki.assets.GetAssetInfo(B.resource,function(E){A.start(E)})}else{B.startInfo=D;B.creator=D.creator;B.title=D.title||"No Title Given";B.creatorName=D.creatorName||"No Username Given";A.startMetadata(B.resource)}}else{alert("ERROR: Could not start Organize.");return false}}})();