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
 *
 * @author dward
 *
 */
package org.curriki.gwt.client.search;

import com.google.gwt.user.client.HistoryListener;
import org.curriki.gwt.client.search.queries.DoesSearch;

public class SearcherHistory implements HistoryListener
{
    public void onHistoryChanged(String token)
    {
        // TODO: This has to set each of the selectors and possibly start a query
        // Token format:
        //  terms=...&x=...&....

        // Set each of the selectors
    }

    public void addSearcher(DoesSearch searcher)
    {
        // TODO: This should use an interface
    }
}
