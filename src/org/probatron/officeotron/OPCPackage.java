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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class OPCPackage
{
    static Logger logger = Logger.getLogger( OPCPackage.class );
    private OOXMLTargetCollection col = new OOXMLTargetCollection();
    private ArrayList< String > foldersProbed = new ArrayList< String >();
    public String systemId;


    public OPCPackage( String systemId )
    {
        logger.trace( "Creating OPC package for resource: " + systemId );
        this.systemId = systemId;
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
            String ctu = "jar:" + this.systemId + "!/[Content_Types].xml";
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

        String partUrl = "jar:" + this.systemId + "!/" + entry;
        logger.debug( "Retrieving relationship part from OPC package:" + partUrl );

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
