(function(){Ext.ns("Curriki.module.toc");Ext.ns("Curriki.data.toc");var B=Curriki.module.toc;var A=Curriki.data.toc;B.init=function(){B.vars={};B.getQueryParam=function(D){D=D.replace(/[\[]/,"\\[").replace(/[\]]/,"\\]");var C="[\\?&]"+D+"=([^&#]*)";var F=new RegExp(C);var E=F.exec(window.location.href);if(E==null){return""}else{return E[1]}};Curriki.ui.treeLoader.TOC=function(C){Curriki.ui.treeLoader.TOC.superclass.constructor.call(this)};Ext.extend(Curriki.ui.treeLoader.TOC,Curriki.ui.treeLoader.Base,{setChildHref:true,setFullRollover:true,setTitleInRollover:true,setUniqueId:true,hideInvalid:true});B.displayMainPanel=function(C){B.vars.panel=new Ext.tree.TreePanel({id:"TOCPanel",applyTo:"resource-toc",title:_("curriki.toc.title"),autoHeight:true,cls:"resource resource-toc",border:false,useArrows:true,lines:true,containerScroll:false,enableDD:false,loader:new Curriki.ui.treeLoader.TOC(),root:C,listeners:{beforeclick:{fn:function(D,F){var E=D.getPath().replace(/:\d+(\/|$)/g,"$1").replace(/\//g,";");var G=Curriki.module.toc.getQueryParam("viewer");if(G!==""){G="&viewer="+G}window.location.href="/xwiki/bin/view/"+(D.attributes.pageName||D.id).replace(".","/")+"?bc="+E+G;return false}},load:{fn:function(){if(typeof B.vars.foundSelect==="undefined"){var D=C.findChild("id",A.selected);if(!Ext.isEmpty(D)){D.select();D.ensureVisible();B.vars.foundSelect=true}}}}}})};B.buildTree=function(){var C=A.tocData;C.cls=C.cls+" toc-top";C.listeners={beforecollapse:{fn:function(){return false}}};var D=new Curriki.ui.treeLoader.TOC();C=D.createNode(C);return C};B.display=function(){B.displayMainPanel(B.buildTree())};B.initialized=true;return true};B.start=function(){Ext.onReady(function(){if(B.init()){B.display()}})};Ext.onReady(function(){Curriki.module.toc.start()})})();