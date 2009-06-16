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
import java.util.HashMap;
import java.util.Iterator;
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

    private static byte[] schema10;
    private static byte[] schema11;
    private static byte[] schema12;

    static
    {
        //  set up log message format, etc.
        String logLvl = System.getProperty( "property://probatron.org/officeotron-log-level" );
        logLvl = ( logLvl == null ) ? "WARN" : logLvl;

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
            resp.sendError( 412, "Request body length exceeds the permitted maximum" );
            return;
        }

        HashMap<String, UUID> itemMap = Store.putMultiPart( req );
        UUID candidateUuid = itemMap.get( "candidate" );

        ValidationReport commentary = new ValidationReport();
        ODFPackageManifest mft = doManifest( candidateUuid, commentary );
        if( mft == null )
        {
            resp.sendError( 412, "Submitted resource must be a recognisable ODF package" );
            return;
        }

        boolean forceIs = forceIsRequest( itemMap );
        processDocs( candidateUuid, mft, commentary, forceIs );
        cleanup( itemMap );

        commentary.addComment( "Total count of validity errors: " + commentary.getErrCount() );

        resp.setCharacterEncoding( "UTF-8" );
        resp.setContentType( "application/xml" );
        commentary.streamOut( resp.getOutputStream() );
    }


    boolean forceIsRequest( HashMap<String, UUID> itemMap )
    {
        boolean r = false;
        UUID uuid = itemMap.get( "force-is" );
        if( uuid != null )
        {
            String s = new String( Store.getBytes( uuid ) );
            logger.debug( "force-is setting val " + s );
            r = s.equalsIgnoreCase( "true" );
        }

        logger.debug( "force-is setting is " + r );
        return r;
    }


    void cleanup( HashMap<String, UUID> itemMap )
    {
        Iterator<String> iter = itemMap.keySet().iterator();
        while( iter.hasNext() )
        {
            Store.delete( itemMap.get( iter.next() ) );
        }
    }


    ODFPackageManifest doManifest( UUID candidateUuid, ValidationReport commentary )
    {
        XMLSniffer sniffer = new XMLSniffer();
        ODFPackageManifest mft = null;

        String url = Store.asUrlRef( candidateUuid );
        String manifestUrl = "jar:" + url + "!/META-INF/manifest.xml";

        XMLSniffData sd = null;
        try
        {
            sd = sniffer.doSniff( manifestUrl );
        }
        catch( Exception e )
        {
            logger.fatal( "Cannot find/parse a manifest:" + e.getMessage() );
        }

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


    void processDocs( UUID candidateUuid, ODFPackageManifest mft, ValidationReport commentary,
            boolean forceIs ) throws IOException
    {
        for( int i = 0; i < mft.getItemRefs().size(); i++ )
        {
            String entry = mft.getItemRefs().get( i );

            String url = "jar:" + Store.asUrlRef( candidateUuid ) + "!/" + entry;
            logger.debug( "processing " + url );
            commentary.addComment( "Processing manifest entry: " + entry );
            ODFSniffer sniffer = new ODFSniffer();
            XMLSniffData sd;

            try
            {
                sd = sniffer.doSniff( url );
            }
            catch( SAXException e )
            {
                logger.fatal( "Referenced resource in manifest cannot be found" );
                commentary.addComment( "FATAL",
                        "Referenced resource in manifest cannot be found" );
                return;
            }

            if( sd.getRootNs().equals( ODFSniffer.ODF_DOC_NS ) )
            {
                String ver = Utils.getQAtt( sd.getAtts(), ODFSniffer.ODF_DOC_NS, "version" );
                logger.debug( "version is " + ver );

                commentary.addComment( "Document \"" + entry + "\" has root element &lt;"
                        + sd.getRootElementName() + ">" );

                if( forceIs )
                {
                    commentary.addComment( "WARN", "Forcing validation against ISO/IEC 26300" );
                    ver = "1.0";
                }
                else
                {
                    if( ver != null )
                    {
                        commentary.addComment( "It claims to be ODF version " + ver );
                    }
                    else
                    {
                        commentary.addComment( "WARN",
                                "It has no version attribute! (ODF v1.1 probably intended)" );
                        ver = "1.1";
                    }

                }

                validateODFDoc( url, ver, commentary );
            }

            if( sniffer.getGenerator() != "" )
            {
                commentary.addComment( "The generator value is: \"<b>" + sniffer.getGenerator()
                        + "</b>\"" );
            }
        }
    }


    /**
     * Validates the given ODF XML document.
     * 
     * @param url the URL of the candidate
     * @param ver the version; must be "1.0", "1.1" or "1.2"
     * @param commentary where to report the validation narrative
     * @throws IOException
     * @throws MalformedURLException
     */
    private void validateODFDoc( String url, String ver, ValidationReport commentary )
            throws IOException, MalformedURLException
    {
        logger.debug( "Beginning document validation ..." );
        synchronized( ValidatorServlet.class )
        {
            // Create the Jing ValidationDriver
            PropertyMapBuilder builder = new PropertyMapBuilder();
            ODFErrorHandler h = new ODFErrorHandler( commentary );
            ValidateProperty.ERROR_HANDLER.put( builder, h );
            ValidationDriver driver = new ValidationDriver( builder.toPropertyMap() );

            InputStream candidateStream = null;
            try
            {
                logger.debug( "Loading schema version " + ver );
                byte[] schemaBytes = getSchemaForVersion( ver );
                driver.loadSchema( new InputSource( new ByteArrayInputStream( schemaBytes ) ) );

                URLConnection conn = new URL( url ).openConnection();
                candidateStream = conn.getInputStream();
                logger.debug( "Calling validate()" );

                commentary.setIndent( 4 );
                boolean isValid = driver.validate( new InputSource( candidateStream ) );
                logger.debug( "Errors in instance:" + h.getInstanceErrCount() );
                if( h.getInstanceErrCount() > ODFErrorHandler.THRESHOLD )
                {
                    commentary.addComment( "(<i>"
                            + ( h.getInstanceErrCount() - ODFErrorHandler.THRESHOLD )
                            + " errors omitted for the sake of brevity</i>)" );
                }
                commentary.setIndent( 0 );

                if( isValid )
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
                commentary.addComment( "FATAL", "The resource is not conformant XML: "
                        + e.getMessage() );
                logger.error( e.getMessage() );
            }
            finally
            {
                Utils.streamClose( candidateStream );
            }
        }

    }


    private byte[] getSchemaForVersion( String ver )
    {
        byte[] schemaBytes = null;
        if( ver.equals( "1.0" ) )
        {
            schemaBytes = schema10;
        }
        else if( ver.equals( "1.1" ) )
        {
            schemaBytes = schema11;
        }
        else if( ver.equals( "1.2" ) )
        {
            schemaBytes = schema12;
        }
        else
        {
            logger.fatal( "No version found ..." );
            return null;
        }
        return schemaBytes;
    }


    boolean contentLengthOkay( HttpServletRequest req ) throws IOException
    {
        int cl = req.getContentLength();
        ServletContext sc = getServletContext();
        int cmax = Integer.parseInt( sc.getInitParameter( "max-upload" ) );
        return cl <= cmax;
    }

}
