package org.curriki.gwt.client.wizard;

import asquare.gwt.tk.client.ui.ModalDialog;
import asquare.gwt.tk.client.ui.behavior.TabFocusController;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FormHandler;
import com.google.gwt.user.client.ui.FormSubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormSubmitEvent;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.gwt.api.client.Document;
import org.curriki.gwt.client.AssetDocument;
import org.curriki.gwt.client.CurrikiAsyncCallback;
import org.curriki.gwt.client.CurrikiService;
import org.curriki.gwt.client.Main;
import org.curriki.gwt.client.Constants;
import org.curriki.gwt.client.utils.ClickListenerMetadata;
import org.curriki.gwt.client.widgets.metadata.MetadataEdit;
import org.curriki.gwt.client.widgets.siteadd.ChooseCollectionDialog;
import org.curriki.gwt.client.widgets.template.ChooseTemplateDialog;

/** See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software;you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation;either version2.1of
 * the License,or(at your option)any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software;if not,write to the Free
 * Software Foundation,Inc.,51 Franklin St,Fifth Floor,Boston,MA
 * 02110-1301 USA,or see the FSF site:http://www.fsf.org.
 *
 * @author ldubost
 */

public class AddFromTemplateWizard {
    private static ChooseCollectionDialog collections;
    private static ChooseTemplateDialog chooseTemplateDialog;
    private static ModalDialog metaPanel;
    private String collectionName;

    private Document newDoc;

    public void addFromTemplate() {
        // 1. Choose a collection to add it to (if more than just the default)
        // 2. Choose the template
        // 3. Fill out Metadata
        // 4. Redirect to composite asset
        ClickListener next = new ClickListener(){
            public void onClick(Widget sender){
                if (collections == null || collections.getSelectedItem() == null || collections.getSelectedItem().getPageName() == null ||
                    collections.getSelectedItem().getPageName().equals("__NOSELECT__")){
                    Window.alert(Main.getTranslation("template.selectcollection"));
                } else{
                    if (collections.isAttached()){
                        collections.hide();
                    }
                    addFromTemplate(collections.getSelectedItem().getPageName());
                }
            }
        };
        ClickListener cancel =  new ClickListener(){
            public void onClick(Widget sender){
                if (collections.isAttached()){
                    collections.hide();
                }
            }
        };
        collections = new ChooseCollectionDialog("choosecollection.create_a_learning_resource", Constants.DIALOG_CHOOSE_COLLECTION_CREATE, next, cancel);

    }

    public void addFromTemplate(String collection) {
        this.collectionName = collection;

        AsyncCallback callback = new AsyncCallback(){
            public void onFailure(Throwable throwable) {
                chooseTemplateDialog.hide();
            }

            public void onSuccess(Object result) {
                // We should now have a file or link
                chooseTemplateDialog.hide();
                // We retrieve a temporary document from the copy
                newDoc = (AssetDocument) result;
                // let's init the meta data with less work since we have retrieved the template
                initMetadataUI();
            }
        };
        chooseTemplateDialog = new ChooseTemplateDialog(collectionName, callback);
    }

    public void initMetadataUI(){
        MetadataEdit meta = new MetadataEdit(newDoc, false);

        // This really should be somewhere else
        meta.SetHiddenCategoryValue("");

        // Add an event handler to the form.
        meta.addFormHandler(new FormHandler() {
            public void onSubmit(FormSubmitEvent formSubmitEvent) {
                Main.getSingleton().startLoading();
            }

            public void onSubmitComplete(FormSubmitCompleteEvent event) {
                Main.getSingleton().finishLoading();
                finishWizard();
            }
        });

        metaPanel = new ModalDialog();
        metaPanel.removeController(metaPanel.getController(TabFocusController.class));
        metaPanel.addStyleName("dialog-metadata");
        metaPanel.setCaption(Main.getTranslation("template.describethelearningresource"), false);

        metaPanel.add(meta);

        Button bttCancel = new Button(Main.getTranslation("editor.btt_cancel"), new ClickListener() {
            public void onClick(Widget sender){
                // TODO: We really should delete from AssetTemp here
                metaPanel.hide();
            }
        });
        bttCancel.addStyleName("dialog-metadata-button-cancel");

        Button bttNext = new Button(Main.getTranslation("editor.btt_finish"), new ClickListenerMetadata(
            meta));
        bttNext.addStyleName("gwt-ButtonOrange");
        bttNext.addStyleName("dialog-metadata-button-finish");

        HorizontalPanel buttonPanel = new HorizontalPanel();
        buttonPanel.addStyleName("dialog-metadata-button-panel");
        buttonPanel.add(bttCancel);
        buttonPanel.add(bttNext);

        metaPanel.add(buttonPanel);
        metaPanel.show();
    }

    private void finishWizard() {
        metaPanel.hide();

        // We need to move the asset to this collection
        CurrikiService.App.getInstance().finalizeAssetCreation(newDoc.getFullName(), collectionName, -1,
                new CurrikiAsyncCallback(){
                    public void onFailure(Throwable caught) {
                        super.onFailure(caught);
                        Main.getSingleton().showError(caught);
                    }

                    public void onSuccess(Object result) {
                        super.onSuccess(result);
                        Document finalDoc = (Document) result;
                        String editURL = Main.getTranslation("params.gwturl") + "page="+finalDoc.getFullName()+"&new=1";
                        Window.open(editURL, "_blank", "");
                    }
                });
    }

}
