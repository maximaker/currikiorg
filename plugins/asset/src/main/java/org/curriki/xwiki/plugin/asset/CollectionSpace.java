package org.curriki.xwiki.plugin.asset;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.doc.XWikiDocument;

import java.util.Map;
import java.util.HashMap;

import org.curriki.xwiki.plugin.asset.composite.CollectionCompositeAsset;

public class CollectionSpace {
    private String spaceName;
    private XWikiContext context;

    public CollectionSpace(String spaceName, XWikiContext context) {
        this.spaceName = spaceName;
        this.context = context;
    }

    public void ensureExists() throws XWikiException {
        if (isExists()){
            return;
        }

        if (!isPreferencesPageExists()) {
            protectSpace();
        }

        if (!isRootCollectionExists()) {
            createRootCollection();
        }

        if (isUserSpace() && !isFavoritesCollectionExists()) {
            createFavoritesCollection();
        }

    }

    static public void ensureExists(String space, XWikiContext context) throws XWikiException {
        CollectionSpace cSpace = new CollectionSpace(space, context);
        cSpace.ensureExists();
    }

    public boolean isExists(){
        return isPreferencesPageExists()
               && isRootCollectionExists()
               && (isGroupSpace() || isFavoritesCollectionExists());
    }

    static public boolean isExists(String space, XWikiContext context) {
        CollectionSpace cSpace = new CollectionSpace(space, context);
        return cSpace.isExists();
    }

    public boolean isPreferencesPageExists() {
        String prefsPage = spaceName+".WebPreferences";
        if (!context.getWiki().exists(prefsPage, context)){
            return false;
        }

        try {
            XWikiDocument ownerDoc = context.getWiki().getDocument(prefsPage, context);
            BaseObject userObj = ownerDoc.getObject("XWiki.XWikiGlobalRights");

            return userObj != null;
        } catch (XWikiException e) {
            return false;
        }
    }

    public boolean isRootCollectionExists() {
        String rootPage = spaceName+"."+Constants.ROOT_COLLECTION_PAGE;

        return context.getWiki().exists(rootPage, context);
    }

    protected void createRootCollection() throws XWikiException {
        Map<String,String> ownerMap = getOwner();

        XWikiDocument doc = context.getWiki().getDocument(spaceName, Constants.ROOT_COLLECTION_PAGE, context);

        BaseObject CompObj = doc.newObject(Constants.COMPOSITE_ASSET_CLASS, context);
        CompObj.set(Constants.COMPOSITE_ASSET_CLASS_TYPE, Constants.COMPOSITE_ASSET_CLASS_TYPE_ROOT_COLLECTION, context);

        doc.setCreator(context.getUser());
        doc.setContent(Constants.COMPOSITE_ASSET_ROOT_COLLECTION_CONTENT);
        doc.setParent(context.getUser());

        String owner = ownerMap.get("owner");
        String ownerType = ownerMap.get("ownerType");

        BaseObject obj = doc.newObject("XWiki.XWikiRights", context);
        obj.setStringValue(ownerType, owner);
        obj.setStringValue("levels", "edit");
        obj.setIntValue("allow", 1);

        context.getWiki().saveDocument(doc, context.getMessageTool().get("curriki.comment.createrootcollection"), context);
    }

    public boolean isFavoritesCollectionExists() {
        String favPage = spaceName+"."+Constants.FAVORITES_COLLECTION_PAGE;

        return context.getWiki().exists(favPage, context);
    }

    protected void createFavoritesCollection() throws XWikiException {
        if (isGroupSpace()) {
            // Do not create for group collection spaces
            return;
        }

        Map<String,String> ownerMap = getOwner();

        if (!ownerMap.get("owner").equals(context.getUser())) {
            // Only create the favorites collection for the user
            return;
        }

        Asset asset = Asset.createTempAsset(null, context);

        CollectionCompositeAsset fav = asset.makeCollection();
        fav.setTitle(Constants.FAVORITES_COLLECTION_NAME);
        fav.set(Constants.ASSET_CLASS_TITLE, Constants.FAVORITES_COLLECTION_NAME);
        fav.set(Constants.ASSET_CLASS_DESCRIPTION, Constants.FAVORITES_COLLECTION_NAME);
        fav.publish(spaceName, false);
    }

    public boolean isGroupSpace() {
        return spaceName.startsWith(Constants.GROUP_COLLECTION_SPACE_PREFIX);
    }

    public boolean isUserSpace() {
        return spaceName.startsWith(Constants.COLLECTION_PREFIX);
    }

    protected Map<String,String> getOwner() throws XWikiException {
        Map<String,String> ownerMap = new HashMap<String,String>(2);

        String owner;
        String ownerType;

        if (isGroupSpace()){
            owner = spaceName.replaceFirst(Constants.GROUP_COLLECTION_PREFIX_SPACE_PREFIX, "") + ".MemberGroup";
        } else if (isUserSpace()){
            owner = "XWiki."+spaceName.replaceFirst(Constants.COLLECTION_PREFIX, "");
        } else {
            throw new AssetException("Cannot determine owner for collection space: "+spaceName);
        }

        if (context.getWiki().exists(owner, context)){
            XWikiDocument ownerDoc = context.getWiki().getDocument(owner, context);
            BaseObject userObj = ownerDoc.getObject("XWiki.XWikiUsers");
            if (userObj != null){
                ownerType = "users";
            } else {
                BaseObject groupObj = ownerDoc.getObject("XWiki.XWikiGroups");
                if (groupObj != null){
                    ownerType = "groups";
                } else {
                    throw new XWikiException(XWikiException.MODULE_XWIKI_GWT_API, XWikiException.ERROR_XWIKI_DOES_NOT_EXIST, "Cannot set owner for "+spaceName+". No user or group exists.");
                }
            }
        } else {
            throw new AssetException("Cannot determine owner for collection space: "+spaceName);
        }

        ownerMap.put("owner", owner);
        ownerMap.put("ownerType", ownerType);

        return ownerMap;
    }

    protected void protectSpace() throws XWikiException {
        Map<String,String> ownerMap = getOwner();

        String owner = ownerMap.get("owner");
        String ownerType = ownerMap.get("ownerType");

        XWikiDocument doc = context.getWiki().getDocument(spaceName, "WebPreferences", context);
        doc.removeObjects("XWiki.XWikiGlobalRights");

        BaseObject obj = doc.newObject("XWiki.XWikiGlobalRights", context);

        obj.setStringValue("groups", "XWiki.XWikiAllGroup, XWiki.EditorGroup");
        obj.setStringValue("levels", "edit");
        obj.setIntValue("allow", 1);

        obj = doc.newObject("XWiki.XWikiGlobalRights", context);
        obj.setStringValue(ownerType, owner);
        obj.setStringValue("levels", "edit");
        obj.setIntValue("allow", 1);

        if (isGroupSpace()){
            doc.setStringValue("XWiki.XWikiPreferences", "parent", spaceName.replaceFirst(Constants.GROUP_COLLECTION_PREFIX_SPACE_PREFIX, ""));
            obj = doc.newObject("XWiki.XWikiGlobalRights", context);
            obj.setStringValue("groups", spaceName.replaceFirst(Constants.GROUP_COLLECTION_PREFIX_SPACE_PREFIX, "") + ".AdminGroup");
            obj.setStringValue("levels", "admin");
            obj.setIntValue("allow", 1);
        }

        context.getWiki().saveDocument(doc, context.getMessageTool().get("curriki.comment.protectspace"), context);
    }
}
