/*
 * This file is part of the source of
 * 
 * Office-o-tron - a web-based office document validator for Java(tm)
 * 
 * Copyright (C) 2009 Griffin Brown Digital Publishing Ltd
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

import org.apache.log4j.Logger;
import org.xml.sax.helpers.AttributesImpl;

public class OOXMLTarget
{

    static Logger logger = Logger.getLogger( OOXMLTarget.class );

    public final static String NS_PACKAGE_RELATIONSHIPS = "http://schemas.openxmlformats.org/package/2006/relationships";
    String hostPartEntryName;
    String type;
    String mimeType;
    String name;


    public OOXMLTarget( String hostPartEntryName, AttributesImpl atts )
    {
        this.hostPartEntryName = hostPartEntryName;
        this.type = atts.getValue( "Type" );
        this.name = atts.getValue( "Target" );

        logger.debug( "Created new OOXMLTarget " + this );
    }


    public String getType()
    {
        return type;
    }


    public void setType( String type )
    {
        this.type = type;
    }


    public String getMimeType()
    {
        return mimeType;
    }


    public void setMimeType( String mimeType )
    {
        this.mimeType = mimeType;
    }


    public String getHostPartEntryName()
    {
        return hostPartEntryName;
    }


    public String getName()
    {
        return name;
    }


    public void setName( String target )
    {
        this.name = target;
    }


    public String getTargetFolder()
    {
        String s = this.name.replaceFirst( "/[^/]+$", "" );
        return s;
    }
    
    public String getFilename()
    {
        String s = this.name.replaceFirst( "^.*/", "" );
        return s;
    }


    @Override
    public String toString()
    {
        return "type=" + type + "; target=" + name;
    }

}
