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
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;

public class Submission
{
    static Logger logger = Logger.getLogger( Submission.class );

    private HttpServletRequest request;
    private HashMap<String, String> optionMap = new HashMap<String, String>();
    private String responseErr;
    private UUID candidateUuid;


    public Submission( HttpServletRequest req )
    {
        this.request = req;
    }


    public int fetchFromClient() throws IOException
    {
        int ret = 200;

        boolean isMultipart = ServletFileUpload.isMultipartContent( this.request );

        if( isMultipart )
        {
            parseMultiPart( this.request );
        }
        else
        {
            parseDirect( this.request );
        }

        return ret;
    }


    /**
     * Retrieves items contained in a multipart servlet request.
     * 
     * @param req
     * @return a map, mapping between item names and the UUIDs for their stored values
     * @throws IOException
     */
    public void parseMultiPart( HttpServletRequest req ) throws IOException
    {
        logger.debug( "Processing multipart submission" );
        ServletFileUpload upload = new ServletFileUpload();

        try
        {
            FileItemIterator iter = upload.getItemIterator( req );

            while( iter.hasNext() )
            {
                FileItemStream item = iter.next();
                String name = item.getFieldName();
                logger.trace( "Got file item named " + name );
                InputStream sis = item.openStream();

                if( name.equalsIgnoreCase( "candidate" ) )
                {
                    this.candidateUuid = Store.put( sis ); // closes stream 
                    logger.debug( "Persisted candidate item with UUID: " + this.candidateUuid );

                }
                else
                {
                    byte[] ba = Utils.getBytesToEndOfStream( sis, true ); // closes stream
                    String val = new String( ba );
                    logger.debug( "Setting option " + name + " = " + val );
                    this.optionMap.put( name, val );
                }
            }
        }
        catch( FileUploadException e )
        {
            logger.fatal( e.getMessage() );
            throw new RuntimeException( e.getMessage() );
        }
    }


    @SuppressWarnings("unchecked")
    public void parseDirect( HttpServletRequest request ) throws IOException
    {
        logger.debug( "Processing direct submission" );
        InputStream sis = request.getInputStream();
        this.candidateUuid = Store.put( sis ); // closes stream

        Enumeration<String> en = request.getParameterNames();
        while( en.hasMoreElements() )
        {
            String name = en.nextElement();
            String val = request.getParameter( name );
            this.optionMap.put( name, val );
        }
    }


    public InputStream getCandidateStream()
    {
        return Store.getStream( candidateUuid );
    }


    public byte[] getCandidateBytes()
    {
        return Store.getBytes( candidateUuid );
    }


    public String getCandidateUrl()
    {
        return Store.asUrlRef( candidateUuid );
    }


    public String getResponseErr()
    {
        return responseErr;
    }


    public String getOption( String name )
    {
        return optionMap.get( name );
    }


    public boolean getBooleanOption( String name )
    {
        String s = optionMap.get( name );        
        return  (s == null) ? false : s.equalsIgnoreCase( "true" );
    }


    void cleanup()
    {
        Store.delete( candidateUuid );
    }

}
