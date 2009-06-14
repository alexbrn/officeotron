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

import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class ODFPackageManifest implements ContentHandler
{
    static Logger logger = Logger.getLogger( ODFPackageManifest.class );
    
    private final static String MANIFEST_NS = "urn:oasis:names:tc:opendocument:xmlns:manifest:1.0";

    private ArrayList<String> itemRefs = new ArrayList<String>();


    void process( String sysId )
    {
        try
        {
            XMLReader parser = XMLReaderFactory.createXMLReader();
            parser.setContentHandler( this );
            parser.parse( sysId );
        }
        catch( SAXException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch( IOException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }


    /* (non-Javadoc)
     * @see org.xml.sax.ContentHandler#characters(char[], int, int)
     */
    public void characters( char[] arg0, int arg1, int arg2 ) throws SAXException
    {
    // TODO Auto-generated method stub

    }


    /* (non-Javadoc)
     * @see org.xml.sax.ContentHandler#endDocument()
     */
    public void endDocument() throws SAXException
    {
    // TODO Auto-generated method stub

    }


    /* (non-Javadoc)
     * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
     */
    public void endElement( String arg0, String arg1, String arg2 ) throws SAXException
    {
    // TODO Auto-generated method stub

    }


    /* (non-Javadoc)
     * @see org.xml.sax.ContentHandler#endPrefixMapping(java.lang.String)
     */
    public void endPrefixMapping( String arg0 ) throws SAXException
    {
    // TODO Auto-generated method stub

    }


    /* (non-Javadoc)
     * @see org.xml.sax.ContentHandler#ignorableWhitespace(char[], int, int)
     */
    public void ignorableWhitespace( char[] arg0, int arg1, int arg2 ) throws SAXException
    {
    // TODO Auto-generated method stub

    }


    /* (non-Javadoc)
     * @see org.xml.sax.ContentHandler#processingInstruction(java.lang.String, java.lang.String)
     */
    public void processingInstruction( String arg0, String arg1 ) throws SAXException
    {
    // TODO Auto-generated method stub

    }


    /* (non-Javadoc)
     * @see org.xml.sax.ContentHandler#setDocumentLocator(org.xml.sax.Locator)
     */
    public void setDocumentLocator( Locator arg0 )
    {
    // TODO Auto-generated method stub

    }


    /* (non-Javadoc)
     * @see org.xml.sax.ContentHandler#skippedEntity(java.lang.String)
     */
    public void skippedEntity( String arg0 ) throws SAXException
    {
    // TODO Auto-generated method stub

    }


    /* (non-Javadoc)
     * @see org.xml.sax.ContentHandler#startDocument()
     */
    public void startDocument() throws SAXException
    {
    // TODO Auto-generated method stub

    }


    /* (non-Javadoc)
     * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    public void startElement( String uri, String localName, String qName, Attributes atts )
            throws SAXException
    {
        if( uri.equals( MANIFEST_NS ) && localName.equals( "file-entry" ) )
        {
            logger.debug( "processing file-entry in manifest" );
            String mimeType = Utils.getQAtt( atts, MANIFEST_NS, "media-type" );
            if( mimeType.indexOf( "/xml" ) != -1 )
            {
                String entryName = Utils.getQAtt( atts, MANIFEST_NS, "full-path" );
                itemRefs.add( entryName );
                logger.debug( "Found entry: " + entryName );
            }
        }
    }


    /* (non-Javadoc)
     * @see org.xml.sax.ContentHandler#startPrefixMapping(java.lang.String, java.lang.String)
     */
    public void startPrefixMapping( String arg0, String arg1 ) throws SAXException
    {
    // TODO Auto-generated method stub

    }


    /**
     * @return the itemRefs
     */
    public ArrayList<String> getItemRefs()
    {
        return itemRefs;
    }
    
    

}
