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
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

@SuppressWarnings("serial")
public class ValidatorServlet extends HttpServlet
{    
    static Logger logger = Logger.getLogger( ValidatorServlet.class );

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
    }


    @Override
    protected void doPost( HttpServletRequest req, HttpServletResponse resp )
            throws ServletException, IOException
    {

        Store.init( this ); // to get the storage layer up and running
        
        if( ! contentLengthOkay( req ) )
        {
            resp.sendError( 412, "Request body length exceeds the permitted maximum" );
            return;
        }
        
        Submission sub = new Submission( req );
        int retCode = sub.fetchFromClient();
        if( retCode != 200 )
        {
            resp.sendError( retCode, sub.getResponseErr() );
            return;
        }
        
        ValidationSession vs = Utils.autoCreateValidationSession( sub ); // determine ODF or OOXML
        if( vs == null )
        {
            resp.sendError( 412, "Submitted resource must be a recognisable Office document package" );
            return;
        }
        
        vs.validate();

        resp.setCharacterEncoding( "UTF-8" );
        resp.setContentType( "application/xml" );
        vs.getCommentary().streamOut( resp.getOutputStream() );
    }


    boolean contentLengthOkay( HttpServletRequest req ) throws IOException
    {
        int cl = req.getContentLength();
        ServletContext sc = getServletContext();
        int cmax = Integer.parseInt( sc.getInitParameter( "max-upload" ) );
        return cl <= cmax;
    }

}
