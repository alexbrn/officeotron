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
import java.io.OutputStream;

import org.apache.log4j.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class ValidationReport
{
    static Logger logger = Logger.getLogger( ValidationReport.class );

    private StringBuffer sb = new StringBuffer();
    private boolean started;
    private int indent;
    private int errCount;


    public ValidationReport()
    {
        sb
                .append( "<?xml version='1.0'?><div id='report' xmlns='http://www.w3.org/1999/xhtml'>" );
    }


    void addComment( String s )
    {
        addComment( "INFO", s );
    }


    void addComment( String klass, String s )
    {
        if( started )
        {
            sb.append( "</div>" );
        }
        sb.append( "<div class='" + klass + "'>" );
        for( int i = 0; i < this.indent * 5; i++ )
        {
            sb.append( "&#160;" );
        }
        sb.append( s );
        started = true;
    }


    void streamOut( OutputStream sos ) throws IOException
    {
        sb.append( "</div></div>" );

        byte[] ba = sb.toString().getBytes( "UTF-8" );

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
        Utils.transferBytesToEndOfStream( bis, sos, Utils.CLOSE_IN | Utils.CLOSE_OUT );
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