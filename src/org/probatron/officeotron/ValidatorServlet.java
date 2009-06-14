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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Properties;
import java.util.UUID;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.thaiopensource.util.PropertyMapBuilder;
import com.thaiopensource.validate.ValidateProperty;
import com.thaiopensource.validate.ValidationDriver;

@SuppressWarnings("serial")
public class ValidatorServlet extends HttpServlet
{
    static Logger logger = Logger.getLogger( ValidatorServlet.class );
    private final static String DOC_NS = "urn:oasis:names:tc:opendocument:xmlns:office:1.0";

    private static byte[] schema10;
    private static byte[] schema11;
    private static byte[] schema12;

    static
    {
        //      set up log message format, etc.
        String logLvl = "DEBUG";

        Properties p = new Properties();
        p.setProperty( "log4j.rootCategory", logLvl + ", A1" );
        p.setProperty( "log4j.appender.A1", "org.apache.log4j.ConsoleAppender" );
        p.setProperty( "log4j.appender.A1.target", "System.err" );
        p.setProperty( "log4j.appender.A1.layout", "org.apache.log4j.PatternLayout" );
        p.setProperty( "log4j.appender.A1.layout.ConversionPattern", "[%d{DATE}] %c %p - %m%n" );
        PropertyConfigurator.configure( p );

        // get the schemas from the SDO
        try
        {
            schema10 = Utils
                    .derefUrl( new URL(
                            "http://www.oasis-open.org/committees/download.php/12571/OpenDocument-schema-v1.0-os.rng" ) );
            schema11 = Utils.derefUrl( new URL(
                    "http://docs.oasis-open.org/office/v1.1/OS/OpenDocument-schema-v1.1.rng" ) );
            schema12 = Utils
                    .derefUrl( new URL(
                            "jar:http://www.oasis-open.org/committees/download.php/32891/OpenDocument-schema-v1.2-cd02-rev01.zip!/OpenDocument-schema-v1.2-cd02-rev01.rng" ) );
        }
        catch( MalformedURLException e )
        {
            logger.fatal( e.getMessage() );
        }

    }


    @Override
    protected void doPost( HttpServletRequest req, HttpServletResponse resp )
            throws ServletException, IOException
    {

        Store.init( this );

        // Check if we have a file upload request
        boolean isMultipart = ServletFileUpload.isMultipartContent( req );

        if( ! isMultipart )
        {
            resp.sendError( 406, "Expecting a multipart request" );
            return;
        }

        if( ! contentLengthOkay( req ) )
        {
            resp.sendError( 406, "Request body length exceeds the permitted maximum" );
            return;
        }

        UUID uuid = Store.putMultiPart( req.getInputStream(), req ).get( 0 );
        ValidationReport commentary = new ValidationReport();

        ODFPackageManifest mft = doManifest( uuid, commentary );
        processDocs( uuid, mft, commentary );
        commentary.addComment( "Total count of validity errors: " + commentary.getErrCount() );

        resp.setCharacterEncoding( "UTF-8" );
        resp.setContentType( "text/xml" );
        commentary.streamOut( resp.getOutputStream() );
        Store.delete( uuid );
    }


    ODFPackageManifest doManifest( UUID uuid, ValidationReport commentary )
    {
        XMLSniffer sniffer = new XMLSniffer();
        ODFPackageManifest mft = null;

        String url = Store.asUrlRef( uuid );
        String manifestUrl = "jar:" + url + "!/META-INF/manifest.xml";
        // commentary.addComment( "Sniffing package manifest at " + manifestUrl );

        XMLSniffData sd = sniffer.doSniff( manifestUrl );
        if( sd != null )
        {
            logger.debug( "Found manifest" );
            mft = new ODFPackageManifest();
            mft.process( manifestUrl );
        }
        else
        {
            commentary.addComment( "ERROR", "The package does not contain a  manifest" );

        }

        return mft;

    }


    void processDocs( UUID uuid, ODFPackageManifest mft, ValidationReport commentary )
            throws IOException
    {
        for( int i = 0; i < mft.getItemRefs().size(); i++ )
        {
            String entry = mft.getItemRefs().get( i );

            String url = "jar:" + Store.asUrlRef( uuid ) + "!/" + entry;
            logger.debug( "processing " + url );
            commentary.addComment( "Processing manifest entry: " + entry );
            XMLSniffer sniffer = new XMLSniffer();
            XMLSniffData sd = sniffer.doSniff( url );
            if( sd.getRootNs().equals( DOC_NS ) )
            {
                String ver = Utils.getQAtt( sd.getAtts(), DOC_NS, "version" );
                logger.debug( "version is " + ver );

                commentary.addComment( "Document \"" + entry + "\" has root element &lt;"
                        + sd.getRootElementName() + ">" );
                if( ver != null )
                {
                    commentary.addComment( "It claims to be ODF version " + ver );

                }
                else
                {
                    commentary.addComment( "WARN",
                            "It has no version attribute! (Assuming ODF v1.1). " );
                    ver = "1.1";
                }

                validateDoc( url, ver, commentary );

            }

        }

    }


    private void validateDoc( String url, String ver, ValidationReport commentary )
            throws IOException, MalformedURLException
    {
        logger.debug( "Beginning document validation ..." );
        synchronized( ValidatorServlet.class )
        {
            // ValidationDriver vd = new ValidationDriver();
            byte[] ba = null;
            if( ver.equals( "1.0" ) )
            {
                ba = schema10;
            }
            else if( ver.equals( "1.1" ) )
            {
                ba = schema11;
            }
            else if( ver.equals( "1.2" ) )
            {
                ba = schema12;
            }
            else
            {
                logger.fatal( "No version found ..." );
                return;
            }
            InputSource is = new InputSource( new ByteArrayInputStream( ba ) );
            InputStream dis = null;

            PropertyMapBuilder builder = new PropertyMapBuilder();
            ODFErrorHandler h = new ODFErrorHandler( commentary );

            ValidateProperty.ERROR_HANDLER.put( builder, h );

            ValidationDriver vd = new ValidationDriver( builder.toPropertyMap() );

            try
            {
                logger.debug( "Loading schema" );
                vd.loadSchema( is );
                URLConnection conn = new URL( url ).openConnection();
                dis = conn.getInputStream();
                logger.debug( "Calling validate()" );
                
                commentary.setIndent( 4 );
                boolean r = vd.validate( new InputSource( dis ) );
                
                logger.debug(  "Errors in instance:" +  h.getInstanceErrCount() );
                if( h.getInstanceErrCount() > ODFErrorHandler.THRESHOLD )
                {
                    commentary.addComment( "(<i>"
                            + ( h.getInstanceErrCount() - ODFErrorHandler.THRESHOLD )
                            + " error omitted for the sake of brevity</i>)" );
                }

                commentary.setIndent( 0 );

                if( r == true )
                {
                    commentary.addComment( "The document is valid" );
                }
                else
                {
                    commentary.addComment( "ERROR", "The document is invalid" );
                }
            }
            catch( SAXException e )
            {
                logger.error( e.getMessage() );
            }
            finally
            {
                if( dis != null )
                {
                    dis.close();
                }

            }
        }

    }


    boolean contentLengthOkay( HttpServletRequest req ) throws IOException
    {
        int cl = req.getContentLength();
        ServletContext sc = getServletContext();
        int cmax = Integer.parseInt( sc.getInitParameter( "max-upload" ) );

        return cl <= cmax;

    }

}
