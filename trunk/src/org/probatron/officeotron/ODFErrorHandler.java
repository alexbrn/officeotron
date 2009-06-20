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

import org.apache.log4j.Logger;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class ODFErrorHandler implements ErrorHandler
{
    private ValidationReport commentary;
    static Logger logger = Logger.getLogger( ODFErrorHandler.class );
    private int instanceErrCount;
    public final static int THRESHOLD = 10;


    public ODFErrorHandler( ValidationReport commentary )
    {
        this.commentary = commentary;
    }


    public void error( SAXParseException e ) throws SAXException
    {
        logger.trace( "Error: " + e.getMessage() );
        if( instanceErrCount < THRESHOLD )
        {
            commentary.addComment( "ERROR", "(line " + e.getLineNumber() + ") "
                    + e.getMessage() );
        }
        commentary.incErrs();
        instanceErrCount++;
    }


    public void fatalError( SAXParseException e ) throws SAXException
    {
        logger.debug( "Fatal Error: " + e.getMessage() );

        commentary.addComment( "ERROR", e.getMessage() );
    }


    public void warning( SAXParseException e ) throws SAXException
    {
        logger.debug( "Warning: " + e.getMessage() );
        commentary.addComment( "WARN", e.getMessage() );
    }


    public int getInstanceErrCount()
    {
        return instanceErrCount;
    }

}
