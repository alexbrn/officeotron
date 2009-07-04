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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.log4j.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.thaiopensource.util.PropertyMapBuilder;
import com.thaiopensource.validate.ValidateProperty;
import com.thaiopensource.validate.ValidationDriver;

/*
 * * Represents validation of an ODF Package.
 *
 * <p>The {@link Submission} object passed to the constructor must represent a candidate that is
 * a potential ODF package (i.e. that has a manifest.xml entry located as expected in the
 * archive.</p> <p>The submission may have further options set:</p> <ul> <li> "check-ids"
 * &#x2010; whether to perform ID/IDREF integrity testing during validation <li>"force-is"
 * &#x2010; whether to force validation against the ISO/IEC 26300 schema </ul>
 */
public class ODFValidationSession extends ValidationSession
{
    static Logger logger = Logger.getLogger( ODFValidationSession.class );

    private static byte[] schema10;
    private static byte[] schema11;
    private static byte[] schema12;

    private boolean forceIs;
    private boolean checkIds;

    static
    {
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


    public ODFValidationSession( Submission submission )
    {
        super( submission );
        this.forceIs = getSubmission().getBooleanOption( "force-is" );
        this.checkIds = getSubmission().getBooleanOption( "check-ids" );
        logger.trace( "Creating ODFValidationSession. forceIs=" + this.forceIs + "; checkIds="
                + this.checkIds );
    }


    public void validate()
    {
        ODFPackageManifest mft = parseManifest();
        processManifestDocs( mft );
        getSubmission().cleanup();
        getCommentary().addComment(
                "Grand total count of validity errors: " + getCommentary().getErrCount() );
    }


    private ODFPackageManifest parseManifest()
    {
        String url = this.getSubmission().getCandidateUrl();
        String manifestUrl = "jar:" + url + "!/META-INF/manifest.xml";
        ODFPackageManifest mft = new ODFPackageManifest();
        mft.process( manifestUrl );
        return mft;
    }


    private void processManifestDocs( ODFPackageManifest mft )
    {
        for( int i = 0; i < mft.getItemRefs().size(); i++ )
        {
            String entry = mft.getItemRefs().get( i );
            String packageUrl = this.getSubmission().getCandidateUrl();
            String entryUrl = null;

            entryUrl = "jar:" + packageUrl + "!/" + entry;
            entryUrl = entryUrl.replaceAll( " ", "%20" );

            logger.debug( "processing " + entryUrl );
            getCommentary().addComment( "Processing manifest entry: " + entry );

            getCommentary().incIndent();
            ODFSniffer sniffer = new ODFSniffer( getCommentary(), checkIds );
            XMLSniffData sd;

            try
            {
                sd = sniffer.doSniff( entryUrl );
            }
            catch( Exception e )
            {
                logger.fatal( "Referenced resource in manifest cannot be found" );
                getCommentary().addComment( "FATAL",
                        "Referenced resource in manifest cannot be found/processed" );
                return;
            }

            if( sd.getRootNs().equals( ODFSniffer.ODF_OFFICE_NS ) )
            {
                processODFDocument( entryUrl, sd );
            }

            logger.trace( "Done document processing" );

            if( sniffer.getGenerator() != "" )
            {
                getCommentary().addComment(
                        "The generator value is: \"<b>" + sniffer.getGenerator() + "</b>\"" );
            }
            getCommentary().decIndent();
        }
    }


    private void processODFDocument( String entryUrl, XMLSniffData sd )
    {
        String ver = Utils.getQAtt( sd.getAtts(), ODFSniffer.ODF_OFFICE_NS, "version" );
        logger.debug( "version is " + ver );

        getCommentary().addComment(
                "It has root element named &lt;" + sd.getRootElementName()
                        + "> in the namespace <tt>" + sd.getRootNs() + "</tt>" );

        logger.trace( "beginning validation with force setting of " + this.forceIs );
        if( this.forceIs )
        {
            getCommentary().addComment( "WARN", "Forcing validation against ISO/IEC 26300" );
            ver = "1.0";
        }
        else
        {
            if( ver != null )
            {
                getCommentary().addComment( "It claims to be ODF version " + ver );
            }
            else
            {
                getCommentary().addComment( "WARN",
                        "It has no version attribute! (ODF v1.1 probably intended)" );
                ver = "1.1";
            }
        }

        try
        {
            validateODFDoc( entryUrl, ver, getCommentary() );
        }
        catch( Exception e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    /**
     * Validates the given ODF XML document.
     *
     * @param url
     *            the URL of the candidate
     * @param ver
     *            the version; must be "1.0", "1.1" or "1.2"
     * @param commentary
     *            where to report the validation narrative
     * @throws IOException
     * @throws MalformedURLException
     */
    private void validateODFDoc( String url, String ver, ValidationReport commentary )
            throws IOException, MalformedURLException
    {
        logger.debug( "Beginning document validation ..." );
        synchronized( ODFValidationSession.class )
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

                commentary.incIndent();
                boolean isValid = driver.validate( new InputSource( candidateStream ) );
                logger.debug( "Errors in instance:" + h.getInstanceErrCount() );
                if( h.getInstanceErrCount() > ODFErrorHandler.THRESHOLD )
                {
                    commentary.addComment( "(<i>"
                            + ( h.getInstanceErrCount() - ODFErrorHandler.THRESHOLD )
                            + " error(s) omitted for the sake of brevity</i>)" );
                }
                commentary.decIndent();

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

}
