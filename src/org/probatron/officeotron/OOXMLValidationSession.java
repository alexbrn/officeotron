/*
 * This file is part of the source of
 * 
 * Office-o-tron - a web-based office document validator for Java(tm)
 * 
 * Copyright (c) 2009 Griffin Brown Digital Publishing Ltd.
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.log4j.Logger;
import org.probatron.officeotron.sessionstorage.Store;
import org.probatron.officeotron.sessionstorage.ValidationSession;
import org.probatron.officeotron.utils.CachingResourceResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class OOXMLValidationSession extends ValidationSession
{
    static Logger logger = Logger.getLogger( OOXMLValidationSession.class );

    static MceXmlFilter parser;
	static Validator validator;
    
    static
    {
    	try
    	{
            System.setProperty("org.apache.xerces.xni.parser.XMLParserConfiguration", "org.apache.xerces.parsers.XMLGrammarCachingConfiguration");
            
            XMLReader parent = XMLReaderFactory.createXMLReader();
            parser = new MceXmlFilter( parent );

            ArrayList<String> urls = new ArrayList<String>();
            ArrayList<Source> schemaSources = new ArrayList<Source>();
            
            Set< String > types = OOXMLSchemaMap.getContentTypes();
            for (String type : types)
            {
            	OOXMLSchemaMapping osm = OOXMLSchemaMap.getMappingForContentType( type );
            	if ( !osm.getSchemaName().isEmpty() && !urls.contains( osm.getSchemaName() ) ) {
            		urls.add( osm.getSchemaName() );
            		String schemaUrl = ClassLoader.getSystemResource( "schema/" + osm.getSchemaName() ).toString();

            		if ( !schemaUrl.isEmpty() )
            		{
            			schemaSources.add( new StreamSource( schemaUrl ) );
            		}
            	}
			}
            
            SchemaFactory schemaFactory = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
            schemaFactory.setResourceResolver( new CachingResourceResolver() );
            Schema schema = schemaFactory.newSchema( schemaSources.toArray( new Source[schemaSources.size()] ) );
            
            validator = schema.newValidator();
    	}
    	catch ( Exception e )
    	{
    		logger.error( "Couldn't create parser", e );
    	}
    }

    public OOXMLValidationSession( UUID uuid, ReportFactory reportFactory )
    {
        super( uuid, reportFactory );
    }


    public void validate()
    {
        OPCPackage opc = new OPCPackage( new File( Store.getDirectory( getUuid() ) ) );
        opc.process();
        checkRelationships( opc );

        if( errCount > 0 )
        {
            getCommentary().addComment( "Grand total of errors in submitted package: " + errCount );
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
            // It seems we can determine the claimed MIME type of a part by 3 methods
            //
            // 1. A direct override from [Content_Types].xml
            // or
            // 2. A default extension/type mapping as given in [Content_Types].xml

            OOXMLTarget t = opc.getEntryCollection().get( i );
            String mt = t.getMimeType();

            logger.debug( "Validating entry of MIME type: " + mt );

            OOXMLSchemaMapping osm = OOXMLSchemaMap.getMappingForContentType( mt );

            // not found? see if we can determine type from the extension
            if( osm == null )
            {
                logger.debug( "Trying to find MIME type for extension " + t.getExtension() );
                mt = opc.dtm.get( t.getExtension() );
                osm = OOXMLSchemaMap.getMappingForContentType( mt );

            }

            // still not found ... sorry
            if( osm == null )
            {
                this.getCommentary().addComment(
                        "Cannot determine schema for Part named (\"<![CDATA["
                                + t.getTargetAsPartName() + "]]>\")" );
                logger.debug( "Cannot determine schema for " + t );

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
                    "Validating part \"" + t.getTargetAsPartName() + "\" using schema \""
                            + osm.getSchemaName() + "\" ..." );
            getCommentary().incIndent();

            if( osm.getContentType().equals(
                    "application/vnd.openxmlformats-officedocument.vmlDrawing" ) )
            {
                logger.debug( "VML found" );
                getCommentary().addComment( "WARN",
                        "Warning: deprecated content (VML) detected" );

            }

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
                            .getCommentary(), t.getName() );
                    validator.setErrorHandler( h );

                    String url = getUrlForEntry( t.getTargetAsPartName() ).toString();
                    logger.debug( "Validating: " + url + " using schema "
                                    + osm.getSchemaName() );

                    parser.setVMLStream(osm.getSchemaName().contains("vml"));
                    validator.validate( new SAXSource( parser, new InputSource( url ) ) );

                    if( h.getInstanceErrCount() > 0 )
                    {
                        getCommentary().addComment(
                                "\"" + t.getTargetAsPartName() + "\" contains "
                                        + h.getInstanceErrCount() + " validity error"
                                        + ( h.getInstanceErrCount() > 1 ? "s" : "" ) );
                        errCount += h.getInstanceErrCount();
                    }
                    else
                    {
                        getCommentary().addComment(
                                "\"" + t.getTargetAsPartName() + "\" is schema-valid" );
                    }
                    if( h.getInstanceErrCount() > CommentatingErrorHandler.THRESHOLD )
                    {
                        getCommentary().addComment(
                                        "(<i>"
                                                + ( h.getInstanceErrCount() - CommentatingErrorHandler.THRESHOLD )
                                                + " error(s) omitted for the sake of brevity</i>)" );
                    }

                }
                catch( SAXException e )
                {
                    logger.error( e + " " + e.getMessage() );
                }
                catch( IOException e )
                {
                    logger.error( e + " " + e.getMessage() );
                }
            }

            getCommentary().decIndent();
        }
    }
}
