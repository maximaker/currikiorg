package org.curriki.xwiki.servlet.restlet.router;

import com.google.gson.Gson;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.XWikiDocument;
import org.apache.commons.httpclient.methods.GetMethod;
import org.curriki.plugin.spacemanager.impl.CurrikiSpaceManager;
import org.curriki.xwiki.plugin.curriki.CurrikiPlugin;
import org.jfree.util.Log;
import org.restlet.data.MediaType;
import org.restlet.resource.StreamRepresentation;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

/**
 */
public class CTVRepresentation extends StreamRepresentation {

    private final static MediaType jsonMediaType = MediaType.APPLICATION_JSON; // register("application/json?charset=utf-8","JavaScript Object Notation");
    private final static Charset UTF8 = Charset.forName("UTF-8");

    private final XWiki xwiki;
    private CurrikiPlugin currikiPlugin = null;
    private final String docFullName;
    private final String solrField;
    private final Type type;

    private boolean isBackEndStream = false;
    private GetMethod get;
    private Object objectToOutput;
    private Set userGroups;
    private String userName;
    private boolean userIsAdmin;
    private String childRightsS = null, childPagesS = null;
    private static ThreadLocal<Gson> gson = new ThreadLocal<Gson>() {
        protected Gson initialValue() { return new Gson(); }};


    public CTVRepresentation(String targetDocument, Type type, XWikiContext context)  throws IOException, XWikiException {
        super(jsonMediaType);
        this.xwiki =context.getWiki();
        this.userGroups = new TreeSet<String>(((CurrikiSpaceManager) xwiki.getPlugin("csm",context)).getSpaceNames(context.getUser(), null, context));
        this.type = type;
        this.userName = context.getUser();
        if(userName.startsWith("XWiki.")) userName = userName.substring("XWiki.".length());
        this.userIsAdmin = xwiki.checkAccess("admin", xwiki.getDocument("XWiki.XWikiPreferences", context), context);
        this.docFullName = targetDocument;
        this.currikiPlugin = (CurrikiPlugin) xwiki.getPlugin("curriki",context);

        boolean useBackEnd = true;

        if(!currikiPlugin.solrCheckIsUp()) useBackEnd = false;

        // identify docs to queries
        if(type==Type.USER_COLLECTIONS) {
            solrField = "userCollections";
        } else if(type==Type.USER_GROUPS) {
            solrField = "userGroups";
        } else if(type==Type.GROUP_COLLECTIONS) {
            solrField = "childInfo";
        } else if(type==Type.COLLECTION_CONTENT) {
            solrField = "childInfo";
        } else {
            throw new UnsupportedEncodingException();
        }
    }

    public void init(XWikiContext context) throws XWikiException, IOException {
        // first check versions
        XWikiDocument doc = xwiki.getDocument(docFullName, context);
        String xwikiVersion = doc.getVersion();

        String fn = "fullname:" + docFullName;
        String solrRev = currikiPlugin.solrGetSingleValue(fn, "revisionNumber");
        if(xwikiVersion.equals(solrRev)) {
            isBackEndStream = true;
            get = currikiPlugin.solrCreateQueryGetMethod(fn, solrField, 0, 100);
            currikiPlugin.solrCollectResultsFromQuery(
                    "fullname:" + docFullName, "childRights,childPages", 0, 1, new CurrikiPlugin.SolrResultCollector() {
                public void status(int statusCode, int qTime, int numFound, int start) {}
                public void newDocument() { }

                public void addValue(String name, String value) {
                    if("childRights".equals(name)) childRightsS = value;
                    else if("childPages".equals(name)) childPagesS = value;
                }
            });
        } else {
            isBackEndStream = false;
            objectToOutput = currikiPlugin.fetchCollectionsInfo(docFullName, context);
        }


    }

    private long getLength() {
        if(isBackEndStream)
            return get.getResponseContentLength();
        else return -1;
        //return jsonBytes.length;
    }

    private static String ghostSubAssetInfoJson = createPrivatisedSubAssetInfo();
    private static String createPrivatisedSubAssetInfo() {
        Map<String,Object> subInfo = new TreeMap<String,Object>();
        subInfo.put("displayTitle", "");
        subInfo.put("description", "");
        subInfo.put("revision", "");
        subInfo.put("ict", "");
        subInfo.put("fwItems", new String[]{});
        subInfo.put("levels", new String[]{});
        subInfo.put("category", "");
        subInfo.put("subcategory", "");
        subInfo.put("ict", "");
        subInfo.put("assetType", "Protected"); // TODO: a "question" asset-type? order??

        // remove leading {, it will be added after a few other things at output time
        return new Gson().toJson(subInfo).substring(1);
    }


    private boolean canUserRead(String rights) {
        if(userIsAdmin) return true;
        if(rights.startsWith("privateToGroup:"))
            return userGroups.contains(rights.substring("privateToGroup:".length()));
        else if(rights.startsWith("privateToUser:"))
            return userName.equals(rights.substring("privateToUser:".length()));
        else return true;
    }

    private boolean canUserModify(String rights) {
        if(userIsAdmin) return true;
        if("public".equals(rights)) return true;
        if(rights.startsWith("privateToUser:"))
            return userName.equals(rights.substring("privateToUser:".length()));
        if(rights.startsWith("privateToGroup:")) {
            String owner = rights.substring("privateToGroup:".length());
            return userGroups.contains(owner);
        }
        if(rights.startsWith("protectedToUser:") )
            return userName.equals(rights.substring("protectedToUser:".length()));
        if(rights.startsWith("protectedToGroup:")) {
            String owner = rights.substring("protectedToGroup:".length());
            return userGroups.contains(owner);
        }
        throw new IllegalStateException("Grmbl, rights: \"" + rights + "\" is not understandable to me.");
    }

    private boolean canUserDelete(String docName) {
        if(userIsAdmin) return true;
        if(docName.startsWith("Coll_Group_")) {
            int p = docName.indexOf('.');
            String ownerGroup = docName.substring("Coll_".length(), p);
            return userGroups.contains(ownerGroup);
        }
        else if(docName.startsWith("Coll_")) {
            return docName.startsWith("Coll_" + userName);
        }
        throw new IllegalStateException("Grmbl, can't read right to delete from docName: \"" + docName+ "\".");
    }

    public void write(final Writer out) throws IOException {
        try {
            if(isBackEndStream) {
                boolean isSubassetsQuery = type==Type.COLLECTION_CONTENT || type==Type.GROUP_COLLECTIONS;
                final StringTokenizer childRights=  isSubassetsQuery ? new StringTokenizer(childRightsS, ",") : null,
                childPages= isSubassetsQuery ? new StringTokenizer(childPagesS, ",") : null;
                currikiPlugin.startSolrMethod(get);
                if(isSubassetsQuery) {
                    out.write("[");
                    CurrikiPlugin.SolrResultCollector collector =new CurrikiPlugin.SolrResultCollector() {
                        public void status(int statusCode, int qTime, int numFound, int start) { }

                        public void newDocument() { }

                        public void addValue(String name, String value) {
                            if(!value.startsWith("{")) value=value.trim();
                            if(!value.startsWith("{")) throw new IllegalStateException("Child metadata value \"" + value + "\" incompatible!");
                            String right = childRights!=null ? childRights.nextToken() : null;
                            String assetpage = childPages.nextToken();
                            try {
                                if(childRights!=null && canUserRead(right)) {
                                    System.err.println("Rights: view: " + canUserRead(right) + ", edit:" + canUserModify(right) + ", delete:" + canUserDelete(assetpage) +".");
                                    // TODO: convert the label "rights" to "ownership"
                                    if(canUserModify(right))
                                        out.write("{rights:{view:true, edit:true, delete: ");
                                    else
                                        out.write("{rights:{view:true, edit:false, delete: ");
                                    if(canUserDelete(assetpage)) out.write("true},"); else out.write("false},");
                                    out.write(value.substring(1));
                                } else { // no read allowance
                                    out.write("{rights:{view:false, edit:false, delete: false}, assetpage: \"" + assetpage + "\",");
                                    out.write(ghostSubAssetInfoJson);
                                }
                                out.write(",\n");
                            } catch (Exception e) {
                                throw new IllegalStateException(e);
                            }
                        }
                    };
                    currikiPlugin.feedFieldFromXmlStream(get, collector, solrField);
                    out.write("]");
                } else { // Type.USER_COLLECTIONS, Type.USER_GROUPS: always requested from own user currently, can be static
                    currikiPlugin.feedFieldFromXmlStream(get, out, solrField);
                }
            } else {
                new Gson().toJson(objectToOutput,  out);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void write(OutputStream out) throws IOException {
        OutputStreamWriter writer = new OutputStreamWriter(out, UTF8);
        this.write(writer);
        writer.flush();
    }

    @Override
    public InputStream getStream() throws IOException {
        throw new UnsupportedOperationException("Can't create a stream thus far.");
        // could do so by joining the input streams (and strings)... just... a bit of work
    }

    public static enum Type {
        USER_COLLECTIONS, USER_GROUPS, GROUP_COLLECTIONS, COLLECTION_CONTENT
    }
}
