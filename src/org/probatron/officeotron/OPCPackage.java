/*
 * This file is part of the source of
 * 
 * Office-o-tron - a web-based office document validator for Java(tm)
 * 
 * Copyright (c) 2009-2010 Griffin Brown Digital Publishing Ltd.
 * 
 * All rights reserved world-wide.
 * 
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at http://www.mozilla.org/MPL/MPL-1.1.html
 * 
 * Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY
 * OF ANY KIND, either express or implied. See the License for the specific language governing
 * rights and limitations under the License.
 */

package org.probatron.officeotron;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class OPCPackage extends Package
{
    static Logger logger = Logger.getLogger( OPCPackage.class );
    private OOXMLTargetCollection col = new OOXMLTargetCollection();
    private ArrayList< String > foldersProbed = new ArrayList< String >();
    private UUID uuid;


    // public String systemId;

    public OPCPackage( UUID uuid )
    {
        assert uuid != null : "null UUID";
        this.uuid = uuid;
        logger.trace( "Creating OPC package for submission: " + uuid );
    }


    public void process()
    {
        procRels( "_rels/.rels" );

        // enrich with MIME type info from the Content Types
        try
        {
            XMLReader parser = XMLReaderFactory.createXMLReader();
            OOXMLContentTypeHandler h = new OOXMLContentTypeHandler( this.col );
            parser.setContentHandler( h );

            String ctu = Store.urlForEntry( uuid, "[Content_Types].xml" ).toString();
            parser.parse( ctu );
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


    /**
     * Spiders within a package to pull out all Relationships. They are collected in the {@Link
     *  col} object.
     * 
     * @param entry
     *            the entry within the package to start spidering from
     */
    private void procRels( String entry )
    {

        String partUrl = Store.urlForEntry( uuid, entry ).toString();
        logger.debug( "Retrieving relationship part from OPC package:" + partUrl + " (uuid = " + uuid + ")" );

        try
        {
            XMLReader parser = XMLReaderFactory.createXMLReader();
            OOXMLRelationshipPartHandler h = new OOXMLRelationshipPartHandler( entry );
            parser.setContentHandler( h );
            parser.parse( partUrl );

            // the handler collects the Relationship; process them ...
            Iterator< OOXMLTarget > iter = h.col.iterator();
            logger.trace( "Number of Relationships found: " + h.col.size() );

            while( iter.hasNext() )
            {
                OOXMLTarget t = iter.next();
                this.col.add( t );

                String f = t.getTargetFolder();
                if( !this.foldersProbed.contains( f ) )
                {
                    this.foldersProbed.add( f );
                    String potentialRelsUrl = f + "/_rels/" + t.getFilename() + ".rels";
                    logger.debug( "Probing new target folder: " + potentialRelsUrl );
                    procRels( potentialRelsUrl ); // recurse
                }

            }

        }
        catch( Exception e )
        {
            logger.info( e.getMessage() ); // we expect some of our attempts to fail

        }
    }


    public OOXMLTargetCollection getEntryCollection()
    {
        return this.col;
    }

}
