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
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class ValidationReport
{
    static Logger logger = Logger.getLogger( ValidationReport.class );

    private StringBuffer sb = new StringBuffer();

    private int indent;
    private int errCount;


    public ValidationReport()
    {
        sb.append( "<?xml version='1.0' standalone='yes'?>"
                + "<div xmlns='http://www.w3.org/1999/xhtml'>" );

        String ver = Package.getPackage( "org.probatron.officeotron" )
                .getImplementationVersion();

        sb.append( "<div class='meta'>Beginning validation using "
                + "<a href='http://code.google.com/p/officeotron/'>Office-o-tron</a> " + ver
                + " at " + new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" ).format( new Date() )
                + "</div>" );
    }


    void addComment( String s )
    {
        addComment( "INFO", s );
    }


    void addComment( String klass, String s )
    {

        sb.append( "<div class='" + klass + "'>" );
        for( int i = 0; i < this.indent * 5; i++ )
        {
            sb.append( "&#160;" );
        }
        sb.append( s );
        sb.append( "</div>" );
    }


    void streamOut( HttpServletResponse resp ) throws IOException
    {
        sb.append( "</div>" );

        byte[] ba = sb.toString().getBytes( "us-ascii" ); // utf-8 compatible natch

        resp.setContentLength( ba.length );
        resp.setContentType( "application/xml" );
        resp.setHeader( "Cache-Control", "no-cache" );
        resp.setCharacterEncoding( "utf-8" );

        try
        {
            XMLReader parser = XMLReaderFactory.createXMLReader();
            parser.parse( new InputSource( new ByteArrayInputStream( ba ) ) );
        }
        catch( SAXException e )
        {
            logger.fatal( "Validation report is not conformant XML" );
        }

        ByteArrayInputStream bis = new ByteArrayInputStream( ba );
        Utils.transferBytesToEndOfStream( bis, resp.getOutputStream(), Utils.CLOSE_IN
                | Utils.CLOSE_OUT );
    }


    public void decIndent()
    {
        this.indent--;
    }


    public void incIndent()
    {
        this.indent++;
    }


    public void incErrs()
    {
        this.errCount++;
    }


    public int getErrCount()
    {
        return errCount;
    }

}