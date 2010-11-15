/*
 * This file is part of the source of
 * 
 * Office-o-tron - a web-based office document validator for Java(tm)
 * 
 * Copyright (c) 2009-2010 Griffin Brown Digital Publishing Ltd.
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

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Semaphore;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.probatron.officeotron.sessionstorage.Store;

// TODO: add a friendly info page for when n00bs try to GET this
@SuppressWarnings("serial")
public class ValidatorServlet extends HttpServlet
{
    private static Semaphore sem;
    static Logger logger = Logger.getLogger( ValidatorServlet.class );

    static
    {
        // set up log message format, etc.
        String logLvl = System.getProperty( "property://probatron.org/officeotron-log-level" );
        logLvl = ( logLvl == null ) ? "ERROR" : logLvl;

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

        ServletContext sc = getServletContext();
        synchronized( ValidatorServlet.class )
        {
            if( sem == null )
            {
                int permits = Integer.parseInt( sc.getInitParameter( "maxproc" ) );
                sem = new Semaphore( permits );
                logger.info( "maxproc=" + permits );

            }
        }

        Store.init( sc.getInitParameter( "temp-folder" ), sc
                .getInitParameter( "unzip-invocation" ), true ); // to get the storage layer up and
        // running

        if( !contentLengthOkay( req ) )
        {
            resp.sendError( 412, "Request body length exceeds the permitted maximum" );
            return;
        }

        WebSubmission sub = new WebSubmission( req );
        int retCode = sub.fetchFromClient();
        if( retCode != 200 )
        {
            resp.sendError( retCode, sub.getResponseErr() );
            return;
        }

        WebTask task = new WebTask( sub, resp );
    
        try
        {
            sem.acquire();
            task.run();
        }
        catch( InterruptedException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        finally
        {
            sem.release();
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
