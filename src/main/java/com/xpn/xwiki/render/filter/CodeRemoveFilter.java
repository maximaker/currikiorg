/*
 * Copyright 2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
 */
package com.xpn.xwiki.render.filter;

import org.radeox.filter.context.FilterContext;
import org.radeox.filter.regex.RegexTokenFilter;
import org.radeox.regex.MatchResult;

/**
 * Escape everything inside the {code} macro so that its content isn't rendered. This filter needs to 
 * configured to run first in the Radeox com.xpn.xwiki.render.filter.XWikiFilter configuration file.
 */
public class CodeRemoveFilter extends RegexTokenFilter
{
	public static final String CODE_MACRO_CONTENT = "codeMacroContent";
	
    public CodeRemoveFilter()
    {
        super("(\\{(code)(?::([^\\}]*))?\\})(.*)\\{code}", MULTILINE);
    }

    public void handleMatch(StringBuffer buffer, MatchResult result, FilterContext context)
    {
    	// Remove the content inside the code macro. It'll be put back in CodeRestoreFilter
    	// We save the content in the Filter context so that it can restored later on.
    	context.getRenderContext().set(CODE_MACRO_CONTENT, result.group(4));
    	buffer.append(result.group(1));
    	buffer.append("{code}");
    }
}
