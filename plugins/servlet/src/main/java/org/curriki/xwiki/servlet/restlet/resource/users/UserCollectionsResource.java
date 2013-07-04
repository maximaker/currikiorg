package org.curriki.xwiki.servlet.restlet.resource.users;

import org.curriki.xwiki.plugin.asset.composite.CompositeAsset;
import org.curriki.xwiki.servlet.restlet.router.CTVRepresentation;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;
import org.restlet.resource.ResourceException;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.curriki.xwiki.servlet.restlet.resource.BaseResource;
import org.curriki.xwiki.plugin.asset.Asset;
import org.curriki.xwiki.plugin.asset.composite.RootCollectionCompositeAsset;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONException;
import com.xpn.xwiki.XWikiException;

/**
 */
public class UserCollectionsResource extends BaseResource {
    public UserCollectionsResource(Context context, Request request, Response response) {
        super(context, request, response);
        setReadable(true);
        setModifiable(true);
        defaultVariants();
    }

    @Override public Representation represent(Variant variant) throws ResourceException {
        setupXWiki();
        Request request = getRequest();

        String forUser = (String) request.getAttributes().get("userName");
        try {
            CTVRepresentation rep = new CTVRepresentation(forUser, CTVRepresentation.Type.USER_COLLECTIONS, xwikiContext);
            rep.init(xwikiContext);
            return rep;
        } catch (IOException e) {
            new ResourceException(Status.CONNECTOR_ERROR_COMMUNICATION, e).printStackTrace();
            throw error(Status.CONNECTOR_ERROR_COMMUNICATION, e.getMessage());
        } catch (XWikiException e) {
            new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, e).printStackTrace();
            throw error(Status.CLIENT_ERROR_NOT_FOUND, e.getMessage());
        }

        // -- previously:
        //   Map<String,Object> results;
        //   results = plugin.fetchUserCollectionsInfo(forUser);
        //   JSONArray json = flattenMapToJSONArray(results, resultList, "collectionPage");

    }

    @Override public void storeRepresentation(Representation representation) throws ResourceException {
        setupXWiki();

        Request request = getRequest();
        String forUser = (String) request.getAttributes().get("userName");

        JSONObject json = representationToJSONObject(representation);

        Asset asset;
        try {
            asset = plugin.fetchRootCollection(forUser);
        } catch (XWikiException e) {
            new ResourceException(Status.CLIENT_ERROR_NOT_FOUND, e).printStackTrace();
            throw error(Status.CLIENT_ERROR_NOT_FOUND, e.getMessage());
        }
        if (asset == null) {
            throw error(Status.CLIENT_ERROR_NOT_FOUND, "Collection for "+forUser+" not found.");
        }

        JSONArray orig;
        try {
            orig = json.getJSONArray("original");
            if (orig.isEmpty()){
                new ResourceException(Status.CLIENT_ERROR_NOT_ACCEPTABLE, "You must provide the orignal order.").printStackTrace();
                throw error(Status.CLIENT_ERROR_NOT_ACCEPTABLE, "You must provide the orignal order.");
            }
        } catch (JSONException e) {
            new ResourceException(Status.CLIENT_ERROR_NOT_ACCEPTABLE, "You must provide the original order.").printStackTrace();
            throw error(Status.CLIENT_ERROR_NOT_ACCEPTABLE, "You must provide the original order.");
        }

        JSONArray want;
        try {
            want = json.getJSONArray("wanted");
            if (want.isEmpty()){
                new ResourceException(Status.CLIENT_ERROR_NOT_ACCEPTABLE, "You must provide a new order.").printStackTrace();
                throw error(Status.CLIENT_ERROR_NOT_ACCEPTABLE, "You must provide a new order.");
            }
        } catch (JSONException e) {
            new ResourceException(Status.CLIENT_ERROR_NOT_ACCEPTABLE, "You must provide a new order.").printStackTrace();
            throw error(Status.CLIENT_ERROR_NOT_ACCEPTABLE, "You must provide a new order.");
        }

        if (asset instanceof RootCollectionCompositeAsset) {
            try {
                RootCollectionCompositeAsset fAsset = asset.as(RootCollectionCompositeAsset.class);
                fAsset.reorder(orig, want);
                fAsset.save(xwikiContext.getMessageTool().get("curriki.comment.reordered"));
            } catch (XWikiException e) {
                new ResourceException(Status.CLIENT_ERROR_PRECONDITION_FAILED, e).printStackTrace();
                throw error(Status.CLIENT_ERROR_PRECONDITION_FAILED, e.getMessage());
            }
        } else {
            new ResourceException(Status.CLIENT_ERROR_PRECONDITION_FAILED, "Asset is not a root collection.").printStackTrace();
            throw error(Status.CLIENT_ERROR_PRECONDITION_FAILED, "Asset is not a root collection.");
        }

        getResponse().setEntity(represent(getPreferredVariant()));
    }
}
