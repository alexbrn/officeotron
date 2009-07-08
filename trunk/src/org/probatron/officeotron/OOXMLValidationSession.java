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

import org.apache.log4j.Logger;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class OOXMLValidationSession extends ValidationSession
{
    static Logger logger = Logger.getLogger( OOXMLValidationSession.class );
    private String schemaUrlBase;


    public OOXMLValidationSession( Submission submission, String schemaUrlBase )
    {
        super( submission );
        this.schemaUrlBase = schemaUrlBase;
    }


    public void validate()
    {
        OPCPackage opc = new OPCPackage( this.getSubmission().getCandidateUrl() );
        opc.process();
        checkRelationships( opc );

        if( errCount > 0 )
        {
            getCommentary().addComment( "ERROR",
                    "Grand total of errors in submitted package: " + errCount );
        }
    }


    public void checkRelationships( OPCPackage opc )
    {
        logger.trace( "Beginning package integrity test" );
        this.getCommentary().addComment( "Checking OPC Package ..." );
        this.getCommentary().incIndent();

        logger.trace( "Collection size: " + opc.getEntryCollection().size() );

        for( int i = 0; i < opc.getEntryCollection().size(); i++ )
        {
            OOXMLTarget t = opc.getEntryCollection().get( i );
            String mt = t.getMimeType();

            logger.trace( "Testing entry of MIME type: " + mt );

            OOXMLSchemaMapping osm = OOXMLSchemaMap.getMappingForContentType( mt );

            if( osm == null )
            {
                logger.info( "No mapping found for entry" );
                continue;
            }

            if( !t.getType().equals( osm.getRelType() ) )
            {
                logger.debug( "Relationship type mismatch" );
                this.errCount++;
                this.getCommentary().addComment(
                        "ERROR",
                        "Entry with MIME type \"" + mt
                                + "\" has unrecognized relationship type \"" + t.getType()
                                + "\" (see ISO/IEC 29500-1:2008, Clause " + osm.getClause()
                                + ")" );
            }

        }

        if( this.errCount > 0 )
        {
            this.getCommentary().addComment(
                    "ERROR",
                    "" + this.errCount + " problem" + ( this.errCount > 1 ? "s" : "" )
                            + " found with OPC package" );

        }
        else
        {
            this.getCommentary().addComment( "No problems found with OPC package" );

        }

        this.getCommentary().decIndent();

        this.getCommentary().addComment(
                "Validating " + opc.getEntryCollection().size() + " parts ..." );
        for( int i = 0; i < opc.getEntryCollection().size(); i++ )
        {

            OOXMLTarget t = opc.getEntryCollection().get( i );
            String mt = t.getMimeType();

            logger.trace( "Validating entry of MIME type: " + mt );

            OOXMLSchemaMapping osm = OOXMLSchemaMap.getMappingForContentType( mt );
            if( osm == null )
            {
                this.getCommentary().addComment( "Cannot determine schema for this part" );

            }
            else
            {
                validateTarget( t, osm );
            }

        }

    }


    void validateTarget( OOXMLTarget t, OOXMLSchemaMapping osm )
    {
        synchronized( OOXMLValidationSession.class )
        {
            String schemaName = osm.getSchemaName();
            getCommentary().addComment(
                    "Validating part \"" + t.getQPartname() + "\" using schema \""
                            + osm.getSchemaName() + "\" ..." );
            getCommentary().incIndent();

            if( schemaName == null || schemaName.length() == 0 )
            {
                this.getCommentary().addComment(
                        "No schema known to validate content of type: " + osm.getContentType() );
            }
            else
            {

                try
                {
                    CommentatingErrorHandler h = new CommentatingErrorHandler( this
                            .getCommentary() );
                    XMLReader parser = getConfiguredParser( osm, h );

                    String packageUrl = this.getSubmission().getCandidateUrl();

                    String url = "jar:" + packageUrl + "!" + t.getQPartname();
                    logger
                            .debug( "Validating: " + url + " using schema "
                                    + osm.getSchemaName() );

                    parser.parse( url );

                    if( h.getInstanceErrCount() > 0 )
                    {
                        getCommentary().addComment(
                                "\"" + t.getQPartname() + "\" contains "
                                        + h.getInstanceErrCount() + " validity error"
                                        + ( h.getInstanceErrCount() > 1 ? "s" : "" ) );
                        errCount += h.getInstanceErrCount();
                    }
                    else
                    {
                        getCommentary().addComment(
                                "\"" + t.getQPartname() + "\" is schema-valid" );
                    }
                    if( h.getInstanceErrCount() > CommentatingErrorHandler.THRESHOLD )
                    {
                        getCommentary()
                                .addComment(
                                        "(<i>"
                                                + ( h.getInstanceErrCount() - CommentatingErrorHandler.THRESHOLD )
                                                + " error(s) omitted for the sake of brevity</i>)" );
                    }

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

            getCommentary().decIndent();
        }
    }


    XMLReader getConfiguredParser( OOXMLSchemaMapping osm, ErrorHandler h )
    {
        XMLReader parser = null;

        try
        {
            parser = XMLReaderFactory.createXMLReader();

            parser.setErrorHandler( h );

            String schemaUrl = this.schemaUrlBase + osm.getSchemaName();
            logger.debug( "Selecting XSD schema: " + schemaUrl );

            parser.setFeature( "http://xml.org/sax/features/validation", true );
            parser.setFeature( "http://apache.org/xml/features/validation/schema", true );
            parser.setProperty(
                    "http://apache.org/xml/properties/schema/external-schemaLocation", osm
                            .getNs()
                            + " " + schemaUrl );
        }
        catch( SAXNotRecognizedException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch( SAXNotSupportedException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch( SAXException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return parser;

    }

}
