/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.curriki.xwiki.plugin.asset;

import com.xpn.xwiki.api.Property;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseStringProperty;

import java.io.InputStream;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.lang.reflect.Constructor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.curriki.xwiki.plugin.asset.composite.RootCollectionCompositeAsset;
import org.curriki.xwiki.plugin.asset.composite.FolderCompositeAsset;
import org.curriki.xwiki.plugin.asset.composite.CollectionCompositeAsset;
import org.curriki.xwiki.plugin.asset.attachment.AttachmentAsset;
import org.curriki.xwiki.plugin.asset.attachment.ImageAsset;
import org.curriki.xwiki.plugin.asset.attachment.ArchiveAsset;
import org.curriki.xwiki.plugin.asset.attachment.AudioAsset;
import org.curriki.xwiki.plugin.asset.attachment.AnimationAsset;
import org.curriki.xwiki.plugin.asset.text.TextAsset;
import org.curriki.xwiki.plugin.asset.external.ExternalAsset;
import org.curriki.xwiki.plugin.asset.external.VIDITalkAsset;
import org.curriki.xwiki.plugin.asset.other.InvalidAsset;
import org.curriki.xwiki.plugin.asset.other.ProtectedAsset;

public class Asset extends CurrikiDocument {
    private static final Log LOG = LogFactory.getLog(Asset.class);

    public Asset(XWikiDocument doc, XWikiContext context) {
        super(doc, context);
    }

    public static Asset createTempAsset(String parentAsset, XWikiContext context) throws XWikiException {
        return createTempAsset(parentAsset, null, context);
    }

    public static Asset createTempAsset(String parentAsset, String publishSpace, XWikiContext context) throws XWikiException {
        if (Constants.GUEST_USER.equals(context.getUser())) {
            throw new AssetException(AssetException.ERROR_ASSET_FORBIDDEN, "XWikiGuest cannot create assets.");
        }

        String pageName = context.getWiki().getUniquePageName(Constants.ASSET_TEMPORARY_SPACE, context);

        XWikiDocument newDoc = context.getWiki().getDocument(Constants.ASSET_TEMPORARY_SPACE, pageName, context);

        Asset assetDoc = new Asset(newDoc, context);
        assetDoc.init(parentAsset, publishSpace);
        assetDoc.saveDocument(context.getMessageTool().get("curriki.comment.createnewsourceasset"));

        return assetDoc;
    }

    protected void init(String parentAsset) throws XWikiException {
        init(parentAsset, null);
    }

    protected void init(String parentAsset, String publishSpace) throws XWikiException {
        assertCanEdit();
        doc.setCreator(context.getUser());

        inheritMetadata(parentAsset, publishSpace);

        doc.setCustomClass(getClass().getName());
        setDefaultContent();

        String rights = doc.getObject(Constants.ASSET_CLASS).getStringValue(Constants.ASSET_CLASS_RIGHT);

        applyRightsPolicy(rights);
    }

    protected void setDefaultContent() throws XWikiException {
        assertCanEdit();
        doc.setContent("#includeForm(\"XWiki.AssetTemplate\")");
    }

    protected void initSubType() throws XWikiException {
        assertCanEdit();
        // Empty for Superclass
    }

    public void addAttachment(InputStream iStream, String name) throws XWikiException, IOException {
        assertCanEdit();
        XWikiAttachment att = addAttachment(name, iStream);
        doc.saveAttachmentContent(att, context);
    }

    public String getDisplayTitle() {
        String className = getActiveClass();

        use(Constants.ASSET_CLASS);
        String title = (String) getValue(Constants.ASSET_CLASS_TITLE);

        if (className != null) {
            use(className);
        }

        return (title == null || title.length() == 0) ? Constants.ASSET_DISPLAYTITLE_UNTITLED : title;
    }

    public String getDescription() {
        String className = getActiveClass();

        use(Constants.ASSET_CLASS);
        String description = (String) getValue(Constants.ASSET_CLASS_DESCRIPTION);

        if (className != null) {
            use(className);
        }
        
        return (description == null || description.length() == 0) ? "" : description;
    }

    public void changeOwnership(String newUser) {
        if (hasProgrammingRights()) {
            XWikiDocument assetDoc = getDoc();
            assetDoc.setCreator(newUser);
        }
    }

    /**
     * Set the rights objects based on the current right setting
     *
     * @throws XWikiException
     */
    public void applyRightsPolicy() throws XWikiException {
        applyRightsPolicy(null);
    }

    /**
     * Set the rights object based on the right in param or the current right setting if null
     *
     * @param right
     * @throws XWikiException
     */
    public void applyRightsPolicy(String right) throws XWikiException {
        XWikiDocument assetDoc = getDoc();
        assetDoc.removeObjects("XWiki.XWikiRights");

        BaseObject assetObj = assetDoc.getObject(Constants.ASSET_CLASS);
        String rights;

        if (right == null) {
            // Use existing rights value
            rights = assetObj.getStringValue(Constants.ASSET_CLASS_RIGHT);
        } else {
            rights = right;
            assetObj.setStringValue(Constants.ASSET_CLASS_RIGHT, right);
        }

        // Make sure rights value is valid, default to PUBLIC if not
        if (rights == null
            || !(rights.equals(Constants.ASSET_CLASS_RIGHT_PUBLIC)
                 || rights.equals(Constants.ASSET_CLASS_RIGHT_MEMBERS)
                 || rights.equals(Constants.ASSET_CLASS_RIGHT_PRIVATE))) {
            rights = Constants.ASSET_CLASS_RIGHT_PUBLIC;
            assetObj.setStringValue(Constants.ASSET_CLASS_RIGHT, rights);
        }

        // Rights
        BaseObject rightObj;

        // If collection is user
        String usergroupfield = "users";
        String usergroupvalue = ("".equals(assetDoc.getCreator())) ? context.getUser() : assetDoc.getCreator();

        // If collection is group
        if (assetDoc.getSpace().startsWith(Constants.GROUP_COLLECTION_SPACE_PREFIX)) {
            usergroupfield = "groups";
            usergroupvalue = assetDoc.getSpace().substring(5) + ".MemberGroup";
        }

        // Always let the admin group have edit access
        rightObj = assetDoc.newObject("XWiki.XWikiRights", context);
        rightObj.setStringValue("groups", "XWiki.XWikiAdminGroup");
        rightObj.setStringValue("levels", "edit");
        rightObj.setIntValue("allow", 1);

        // Always let the creator/group edit
        rightObj = assetDoc.newObject("XWiki.XWikiRights", context);
        rightObj.setStringValue(usergroupfield, usergroupvalue);
        rightObj.setStringValue("levels", "edit");
        rightObj.setIntValue("allow", 1);

        if (rights.equals(Constants.ASSET_CLASS_RIGHT_PUBLIC)) {
            // Viewable by all and any member can edit
            rightObj = assetDoc.newObject("XWiki.XWikiRights", context);
            rightObj.setStringValue("groups", "XWiki.XWikiAllGroup");
            rightObj.setStringValue("levels", "edit");
            rightObj.setIntValue("allow", 1);
        } else if (rights.equals(Constants.ASSET_CLASS_RIGHT_MEMBERS)) {
            // Viewable by all, only user/group can edit
        } else {
            // rights == private, so only allow creator/group to view and edit
            rightObj = assetDoc.newObject("XWiki.XWikiRights", context);
            rightObj.setStringValue(usergroupfield, usergroupvalue);
            rightObj.setStringValue("levels", "view");
            rightObj.setIntValue("allow", 1);
        }
    }

    public Class<? extends Asset> determineAssetSubtype() {
        if (hasAccessLevel("view")) {
            if (isFolder()){
                if (isRootCollection()){
                    // Note that the Root Collection doesn't have an Asset Class
                    //  so it has to come before checking if no ASSET_CLASS
                    return RootCollectionCompositeAsset.class;
                } else if (isCollection()){
                    return CollectionCompositeAsset.class;
                } else {
                    return FolderCompositeAsset.class;
                }
            }

            // If category is empty then must be an attachment
            BaseObject obj = doc.getObject(Constants.ASSET_CLASS);
            if (obj == null) {
                return InvalidAsset.class;
            }

            // Check specific objects to find displayer
            if (getObject(Constants.TEXT_ASSET_CLASS) != null){
                return TextAsset.class; // 3 sub-types  HTMLText, WikiText, CBOEText
            }
            if (getObject(Constants.EXTERNAL_ASSET_CLASS) != null){
                return ExternalAsset.class; // Link
            }
            if (getObject(Constants.MIMETYPE_PICTURE_CLASS) != null){
                return ImageAsset.class;
            }
            if (getObject(Constants.VIDITALK_CLASS) != null){ // category == AUDIO at the moment
                return VIDITalkAsset.class;
            }
            if (getObject(Constants.MIMETYPE_ARCHIVE_CLASS) != null){
                return ArchiveAsset.class;
            }

            // Check category
            String category = obj.getStringValue(Constants.ASSET_CLASS_CATEGORY);
            if (category.equals(Constants.CATEGORY_IMAGE)) {
                return ImageAsset.class;
            } else if (category.equals(Constants.CATEGORY_AUDIO)) {
                return AudioAsset.class;
            } else if (category.equals(Constants.CATEGORY_ANIMATION)) {
                return AnimationAsset.class;
            } else if (category.equals(Constants.CATEGORY_TEXT)) {
                return TextAsset.class; // ?? But no text class exists
            } else if (category.equals(Constants.CATEGORY_COLLECTION)) {
                return FolderCompositeAsset.class; // ?? But no collection class exists
            } else if (category.equals(Constants.CATEGORY_LINK)) {
                return ExternalAsset.class; // ?? But no external asset class exists
            } else if (category.equals(Constants.CATEGORY_ARCHIVE)) {
                return ArchiveAsset.class; // ?? But no mimetype archive class exists
            }

            // Last is just an attachment item
            if (doc.getAttachmentList().size() > 0) {
                return AttachmentAsset.class;
            }

            // Otherwise is non-specific yet
            return Asset.class;
        } else {
            return ProtectedAsset.class;
        }
    }

    public boolean isFolder() {
        if (doc.getObjectNumbers(Constants.COMPOSITE_ASSET_CLASS) == 0){
            return false;
        } else {
            // Work around a bug XWIKI-1624
            // TODO: Remove the work-around once XWIKI-1624 is fixed
            List subAssets = doc.getObjects(Constants.COMPOSITE_ASSET_CLASS);
            Iterator i = subAssets.iterator();
            int count = 0;
            while (i.hasNext() && count == 0){
                if (i.next() != null){
                    count++;
                }
            }
            if (count == 0){
                return false;
            }
        }
        return true;
    }
    
    public boolean isCollection() {
        if (isFolder()) {
            com.xpn.xwiki.api.Object obj = getObject(Constants.COMPOSITE_ASSET_CLASS);
            if (getValue(Constants.COMPOSITE_ASSET_CLASS_TYPE, obj).equals(Constants.COMPOSITE_ASSET_CLASS_TYPE_COLLECTION)) {
                return true;
            }
        }
        return false;
    }

    public boolean isRootCollection() {
        if (isFolder()) {
            com.xpn.xwiki.api.Object obj = getObject(Constants.COMPOSITE_ASSET_CLASS);
            if (getValue(Constants.COMPOSITE_ASSET_CLASS_TYPE, obj).equals(Constants.COMPOSITE_ASSET_CLASS_TYPE_ROOT_COLLECTION)) {
                return true;
            }
        }
        return false;
    }

    static public Asset fetchAsset(String assetName, XWikiContext context) throws XWikiException {
        com.xpn.xwiki.api.XWiki xwikiApi = new com.xpn.xwiki.api.XWiki(context.getWiki(), context);
        Document doc = xwikiApi.getDocument(assetName);

        if (doc instanceof Asset){
            return ((Asset) doc).as(null);
        } else {
            throw new AssetException(AssetException.ERROR_ASSET_NOT_FOUND, "Asset could not be found");
        }
    }

    static public Asset fetchAsset(String web, String page, XWikiContext context) throws XWikiException {
        com.xpn.xwiki.api.XWiki xwikiApi = new com.xpn.xwiki.api.XWiki(context.getWiki(), context);
        Document doc = xwikiApi.getDocument(web, page);

        if (doc instanceof Asset){
            return ((Asset) doc).as(null);
        } else {
            throw new AssetException(AssetException.ERROR_ASSET_NOT_FOUND, "Asset could not be found");
        }
    }

    public List<Property> getMetadata() {
        List<Property> md = new ArrayList<Property>();

        com.xpn.xwiki.api.Object assetObj = getObject(Constants.ASSET_CLASS);
        for (Object prop : assetObj.getPropertyNames()) {
            LOG.debug("Adding "+prop+" to metadata list");
            md.add(assetObj.getProperty((String) prop));
        }

        com.xpn.xwiki.api.Object licenseObj = getObject(Constants.ASSET_LICENCE_CLASS);
        for (Object prop : licenseObj.getPropertyNames()) {
            LOG.debug("Adding "+prop+" to metadata list");
            md.add(licenseObj.getProperty((String) prop));
        }

        // Add assetType to metadata
        Class assetType = determineAssetSubtype();
        String fullAssetType = assetType.getCanonicalName();
        BaseStringProperty baseProp = new BaseStringProperty();
        baseProp.setName("fullAssetType");
        baseProp.setValue(fullAssetType);
        Property prop = new Property(baseProp, context);
        md.add(prop);

        // And add shortAssetType to metadata
        String shortAssetType = assetType.getSimpleName();
        if (!shortAssetType.equals("Asset")) {
            shortAssetType = shortAssetType.replaceAll("Asset$", "");
        }
        baseProp = new BaseStringProperty();
        baseProp.setName("assetType");
        baseProp.setValue(shortAssetType);
        prop = new Property(baseProp, context);
        md.add(prop);

        return md;
    }

    public <A extends Asset> A as(Class<? extends Asset> wantedClass) throws XWikiException {
        return (A) as(wantedClass, false);
    }

    public <A extends Asset> A subclassAs(Class<? extends Asset> wantedClass) throws XWikiException {
        return (A) this.as(wantedClass, true);
    }

    public <A extends Asset> A as(Class<? extends Asset> wantedClass, boolean moreSpecific) throws XWikiException {
        // If this is already a subtype of the wanted class then just return this
        if (wantedClass != null && wantedClass.isAssignableFrom(this.getClass())) {
            return (A) this;
        }

        Class returnClass;

        // Make sure the determined type is a subclass of the wanted type
        Class<? extends Asset> assetType = determineAssetSubtype();
        if (!moreSpecific) {
            if (wantedClass == null) {
                // Return as whatever subtype it is
                returnClass = assetType;
            } else if (wantedClass.isAssignableFrom(assetType)) {
                // Return as the desired type
                returnClass = wantedClass;
            } else {
                throw new AssetException(AssetException.ERROR_ASSET_INCOMPATIBLE, "Document of type "+assetType+" cannot become "+wantedClass);
            }
        } else {
            // Subclass an Asset
            if (assetType.equals(wantedClass)) {
                // Already is that type, so don't initialize
                moreSpecific = false;
                returnClass = wantedClass;
            } else if (assetType.equals(Asset.class) && assetType.isAssignableFrom(wantedClass)) {
                // Turn into a specified subtype of Asset (initializing the subtype)
                returnClass = wantedClass;
            } else {
                throw new AssetException(AssetException.ERROR_ASSET_INCOMPATIBLE, "Document of type "+assetType+" cannot become "+wantedClass);
            }
        }

        Class[] parameterTypes = new Class[2];
        parameterTypes[0] = XWikiDocument.class;
        parameterTypes[1] = XWikiContext.class;

        Object[] initargs = new Object[2];
        initargs[0] = doc;
        initargs[1] = context;

        A subtyped;
        try {
            Constructor<A> constructor = returnClass.getConstructor(parameterTypes);
            subtyped = constructor.newInstance(initargs);
            if (moreSpecific) {
                subtyped.initSubType();
            }
        } catch (Exception e) {
            throw new AssetException(AssetException.ERROR_ASSET_INCOMPATIBLE, "Document of type "+assetType+" cannot become "+wantedClass, e);
        }

        return subtyped;
    }

    public void inheritMetadata() throws XWikiException {
        inheritMetadata(null, null);
    }

    public void inheritMetadata(String parentAsset) throws XWikiException {
        inheritMetadata(parentAsset, null);
    }

    public void inheritMetadata(String parentAsset, String publishSpace) throws XWikiException {
        assertCanEdit();

        BaseObject assetObj = doc.getObject(Constants.ASSET_CLASS);
        if (assetObj == null) {
            assetObj = doc.newObject(Constants.ASSET_CLASS, context);
        }

        // CURRIKI-2451 - Make sure group rights are used by default
        if (publishSpace != null && publishSpace.startsWith(Constants.GROUP_COLLECTION_SPACE_PREFIX)) {
            String groupSpace = publishSpace.replaceFirst(Constants.GROUP_COLLECTION_SPACE_PREFIX, Constants.GROUP_SPACE_PREFIX);
            String rights = Constants.ASSET_CLASS_RIGHT_PUBLIC;

            // TODO: This should probably be using the SpaceManager extension
            XWikiDocument groupSpaceDoc = context.getWiki().getDocument(groupSpace+"."+Constants.GROUP_RIGHTS_PAGE, context);
            if (groupSpaceDoc != null){
                // Note that the values for the group access defaults
                //  DO NOT MATCH the values that need to be applied to the collection
                BaseObject rObj = groupSpaceDoc.getObject(Constants.GROUP_RIGHTS_CLASS);
                if (rObj != null){
                    String groupDefaultPrivs = rObj.getStringValue(Constants.GROUP_RIGHTS_PROPERTY);
                    if (groupDefaultPrivs.equals(Constants.GROUP_RIGHT_PRIVATE)){
                        rights = Constants.ASSET_CLASS_RIGHT_PRIVATE;
                    } else if (groupDefaultPrivs.equals(Constants.GROUP_RIGHT_PROTECTED)){
                        rights = Constants.ASSET_CLASS_RIGHT_MEMBERS;
                    } else if (groupDefaultPrivs.equals(Constants.GROUP_RIGHT_PUBLIC)){
                        rights = Constants.ASSET_CLASS_RIGHT_PUBLIC;
                    }
                }
            }

            assetObj.setStringValue(Constants.ASSET_CLASS_RIGHT, rights);
        }

        XWikiDocument parentDoc = null;
        if (parentAsset != null && parentAsset.length() > 0) {
            parentDoc = context.getWiki().getDocument(parentAsset, context);
            if (parentDoc.isNew()) {
                throw new AssetException(AssetException.MODULE_PLUGIN_ASSET, AssetException.ERROR_ASSET_NOT_FOUND, "Parent asset not found");
            }
        }
        // the Root collection does not have an asset class so we can't inherit from it
        if (parentDoc != null && parentDoc.getObject(Constants.ASSET_CLASS) != null) {
            BaseObject parentAssetObj = (BaseObject) parentDoc.getObject(Constants.ASSET_CLASS).clone();

            copyProperty(parentAssetObj, assetObj, Constants.ASSET_CLASS_EDUCATIONAL_LEVEL);
            copyProperty(parentAssetObj, assetObj, Constants.ASSET_CLASS_FRAMEWORK_ITEMS);
            copyProperty(parentAssetObj, assetObj, Constants.ASSET_CLASS_RIGHT);
            copyProperty(parentAssetObj, assetObj, Constants.ASSET_CLASS_KEYWORDS);
            copyProperty(parentAssetObj, assetObj, Constants.ASSET_CLASS_LANGUAGE);
        }

        // make sure default rights value is not empty (default to public)
        String rights = assetObj.getStringValue(Constants.ASSET_CLASS_RIGHT);
        if ((rights==null)||(rights.equals(""))) {
            assetObj.setStringValue(Constants.ASSET_CLASS_RIGHT, Constants.ASSET_CLASS_RIGHT_PUBLIC);
        }

        BaseObject newLicenceObj = doc.getObject(Constants.ASSET_LICENCE_CLASS);
        if (newLicenceObj == null) {
            newLicenceObj = doc.newObject(Constants.ASSET_LICENCE_CLASS, context);
        }
        // the Root collection does not have an asset Licence class
        if (parentDoc != null && parentDoc.getObject(Constants.ASSET_LICENCE_CLASS) != null) {
            BaseObject parentLicenceObjAsset = parentDoc.getObject(Constants.ASSET_LICENCE_CLASS);
            copyProperty(parentLicenceObjAsset, newLicenceObj, Constants.ASSET_LICENCE_ITEM_LICENCE_TYPE);
        } else {
            newLicenceObj.setStringValue(Constants.ASSET_LICENCE_ITEM_LICENCE_TYPE, Constants.ASSET_LICENCE_ITEM_LICENCE_TYPE_DEFAULT);
        }

        // Rights holder should be by default the pretty name of the user
        newLicenceObj.setStringValue(Constants.ASSET_LICENCE_ITEM_RIGHTS_HOLDER, context.getWiki().getLocalUserName(context.getUser(), null, false, context));
    }

    public FolderCompositeAsset makeFolder() throws XWikiException {
        return makeFolder(null);
    }

    public FolderCompositeAsset makeFolder(String page) throws XWikiException {
        assertCanEdit();
        FolderCompositeAsset asset = subclassAs(FolderCompositeAsset.class);

        if (page != null) {
            asset.addSubasset(page);
        }
        saveDocument(context.getMessageTool().get("curriki.comment.createfoldersourceasset"));
        return asset;
    }

    public CollectionCompositeAsset makeCollection() throws XWikiException {
        return makeCollection(null);
    }

    public CollectionCompositeAsset makeCollection(String page) throws XWikiException {
        assertCanEdit();
        CollectionCompositeAsset asset = subclassAs(CollectionCompositeAsset.class);

        if (page != null) {
            asset.addSubasset(page);
        }
        saveDocument(context.getMessageTool().get("curriki.comment.createfoldersourceasset"));
        return asset;
    }

    public ExternalAsset makeExternal(String link) throws XWikiException {
        assertCanEdit();
        ExternalAsset asset = subclassAs(ExternalAsset.class);

        asset.addLink(link);
        saveDocument(context.getMessageTool().get("curriki.comment.createlinksourceasset"));
        return asset;
    }

    public VIDITalkAsset makeVIDITalk(String videoId) throws XWikiException {
        assertCanEdit();
        VIDITalkAsset asset = subclassAs(VIDITalkAsset.class);

        asset.addVideoId(videoId);
        saveDocument(context.getMessageTool().get("curriki.comment.createviditalksourceasset"));
        return asset;
    }

    public TextAsset makeTextAsset(Long type, String content) throws XWikiException {
        assertCanEdit();
        TextAsset asset = subclassAs(TextAsset.class);

        asset.addText(type, content);
        saveDocument(context.getMessageTool().get("curriki.comment.createtextsourceasset"));
        return asset;
    }

    public Boolean isPublished() {
        return !getSpace().equals("AssetTemp");
    }

    public Asset publish(String space) throws XWikiException {
        return publish(space, true);
    }

    public Asset publish(String space, String name) throws XWikiException {
        return publish(space, name, true);
    }

    public Asset publish(String space, boolean checkSpace) throws XWikiException {
        String prettyName = context.getWiki().clearName(doc.getStringValue(Constants.ASSET_CLASS_TITLE), true, true, context);

        return publish(space, prettyName, checkSpace);
    }

    public Asset publish(String space, String name, boolean checkSpace) throws XWikiException {
        assertCanEdit();
        if (!space.startsWith(Constants.COLLECTION_PREFIX)) {
            throw new AssetException("You cannot publish to the space "+space);
        }

        if (!validate()) {
            throw new AssetException("Validation failed.");
        }

        if (checkSpace) {
            CollectionSpace.ensureExists(space, context);
        }

        // Let's choose a nice name for the page
        String prettyName = context.getWiki().clearName(name, true, true, context);
        doc.rename(space + "." + context.getWiki().getUniquePageName(space, prettyName.trim(), context), new ArrayList(), context);
        
        applyRightsPolicy();

        List<String> params = new ArrayList<String>();
        params.add(doc.getStringValue(Constants.ASSET_CLASS_CATEGORY));
        save(context.getMessageTool().get("curriki.comment.finishcreatingsubasset", params));
        
        return this;
    }

    public boolean validate() throws XWikiException {
        // Has the asset been subtyped ?
        if (determineAssetSubtype().equals(Asset.class)) {
            throw new AssetException("This asset is not complete.");
        }

        // Other validation

        // Super's validate
        return super.validate();
    }
}
