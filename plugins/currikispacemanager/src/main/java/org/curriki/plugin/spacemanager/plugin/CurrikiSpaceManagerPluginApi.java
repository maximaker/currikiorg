package org.curriki.plugin.spacemanager.plugin;

import java.util.List;

import org.curriki.plugin.spacemanager.impl.CurrikiSpaceManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.spacemanager.api.SpaceManagerException;
import com.xpn.xwiki.plugin.spacemanager.plugin.SpaceManagerPluginApi;

/**
 * Created by IntelliJ IDEA.
 * User: ludovic
 * Date: 17 d�c. 2007
 * Time: 08:59:52
 * To change this template use File | Settings | File Templates.
 */
public class CurrikiSpaceManagerPluginApi extends SpaceManagerPluginApi {

    public CurrikiSpaceManagerPluginApi(CurrikiSpaceManager plugin, XWikiContext context)
    {
        super(plugin, context);
    }

    protected CurrikiSpaceManager getCurrikiSpaceManager() {
        return (CurrikiSpaceManager) getPlugin();
    }

    public List getSpacesByTopic(String topic, int nb, int start) throws SpaceManagerException {
        return getCurrikiSpaceManager().getSpacesByTopic(topic, nb, start, context);
    }

    public List getSpaceNamesByTopic(String topic, int nb, int start) throws SpaceManagerException {
        return getCurrikiSpaceManager().getSpaceNamesByTopic(topic, nb, start, context);
    }

    public List countSpacesByTopic(String parentTopic) throws SpaceManagerException {
        return getCurrikiSpaceManager().countSpacesByTopic(parentTopic, context);
    }
}
