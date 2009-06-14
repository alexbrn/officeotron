/*
 * This file is part of the source of
 * 
 * Office-o-tron - a web-based ODF document validator for Java(tm)
 * 
 * Copyright (C) 2009 Griffin Brown Digitial Publishing Ltd
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See
 * the GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along with this
 * program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.probatron.officeotron;

import org.xml.sax.helpers.AttributesImpl;

public class XMLSniffData
{
    private AttributesImpl atts;
    private String rootNs;
    private String rootElementName;


    /**
     * @return the atts
     */
    public AttributesImpl getAtts()
    {
        return atts;
    }


    /**
     * @param atts the atts to set
     */
    public void setAtts( AttributesImpl atts )
    {
        this.atts = atts;
    }


    /**
     * @return the rootNs
     */
    public String getRootNs()
    {
        return rootNs;
    }


    /**
     * @param rootNs the rootNs to set
     */
    public void setRootNs( String rootNs )
    {
        this.rootNs = rootNs;
    }


    /**
     * @return the rootElementName
     */
    public String getRootElementName()
    {
        return rootElementName;
    }


    /**
     * @param rootElementName the rootElementName to set
     */
    public void setRootElementName( String rootElementName )
    {
        this.rootElementName = rootElementName;
    }

}
