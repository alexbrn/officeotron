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
