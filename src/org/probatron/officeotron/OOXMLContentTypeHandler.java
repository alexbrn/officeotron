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
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

public class OOXMLContentTypeHandler implements ContentHandler
{
    static Logger logger = Logger.getLogger( OOXMLContentTypeHandler.class );
    OOXMLTargetCollection col;


    public OOXMLContentTypeHandler( OOXMLTargetCollection col )
    {
        this.col = col;
    }


    public void characters( char[] arg0, int arg1, int arg2 ) throws SAXException
    {
    // do nothing
    }


    public void endDocument() throws SAXException
    {
    // do nothing
    }


    public void endElement( String arg0, String arg1, String arg2 ) throws SAXException
    {
    // do nothing
    }


    public void endPrefixMapping( String arg0 ) throws SAXException
    {
    // do nothing
    }


    public void ignorableWhitespace( char[] arg0, int arg1, int arg2 ) throws SAXException
    {
    // do nothing
    }


    public void processingInstruction( String arg0, String arg1 ) throws SAXException
    {
    // do nothing
    }


    public void setDocumentLocator( Locator arg0 )
    {
    // do nothing
    }


    public void skippedEntity( String arg0 ) throws SAXException
    {
    // do nothing
    }


    public void startDocument() throws SAXException
    {
    // do nothing
    }


    public void startElement( String uri, String localName, String qName, Attributes atts )
            throws SAXException
    {
        if( uri.equals( "http://schemas.openxmlformats.org/package/2006/content-types" ) )
        {
            String name = atts.getValue( "PartName" );
            if( localName.equals( "Override" ) )
            {
                OOXMLTarget t = this.col.getTargetByName( name );
                if( t != null )
                {
                    String ct = atts.getValue( "ContentType" );
                    logger.debug( "Setting MIME type for entry " + name + " as: " + ct );
                    t.setMimeType( ct );
                }
            }
            else if( localName.equals( "Default" ) )
            {
                //TODO: implement
            }
        }

    }


    public void startPrefixMapping( String arg0, String arg1 ) throws SAXException
    {
    // do nothing
    }

}
