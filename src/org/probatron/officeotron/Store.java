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
 * 
 */

package org.probatron.officeotron;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;

import org.apache.log4j.Logger;

public class Store
{
    static Logger logger = Logger.getLogger( Store.class );

    static String tmpFolder;


    public static void init( HttpServlet serv )
    {
        if( tmpFolder == null )
        {
            ServletContext sc = serv.getServletContext();
            tmpFolder = sc.getInitParameter( "temp-folder" );
        }
    }


    public static UUID put( InputStream is ) throws IOException
    {
        UUID uuid = UUID.randomUUID();
        String fn = asFilename( uuid );
        long written = Utils.streamToFile( is, fn, true );
        logger.trace( "Wrote " + written + " bytes to file" );
        return uuid;
    }


    public static InputStream getStream( UUID uuid )
    {
        String fn = asFilename( uuid );
        File f = new File( fn );
        InputStream is = null;

        try
        {
            is = new FileInputStream( f );
        }
        catch( FileNotFoundException e )
        {
            // we'll return null in this case
        }

        return is;
    }


    public static byte[] getBytes( UUID uuid )
    {
        byte[] ba = null;

        try
        {
            String fn = asFilename( uuid );
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Utils.streamFromFile( fn, baos, true );
            ba = baos.toByteArray();
        }
        catch( IOException e )
        {
            // we'll return null in this case
        }

        return ba;
    }


    public static void delete( UUID uuid )
    {
        String fn = asFilename( uuid );
        File f = new File( fn );
        f.delete();
    }


    public static String asUrlRef( UUID uuid )
    {
        return "file:" + asFilename( uuid );
    }


    public static String asFilename( UUID uuid )
    {
        return tmpFolder + uuid;
    }

}
