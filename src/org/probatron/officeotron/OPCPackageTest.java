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

import java.util.Properties;

import junit.framework.TestCase;

import org.apache.log4j.PropertyConfigurator;
import org.junit.Test;

public class OPCPackageTest extends TestCase
{
    OPCPackage opc;

    static
    {
        // set up log message format, etc.
        String logLvl = System.getProperty( "property://probatron.org/officeotron-log-level" );
        logLvl = ( logLvl == null ) ? "DEBUG" : logLvl;

        Properties p = new Properties();
        p.setProperty( "log4j.rootCategory", logLvl + ", A1" );
        p.setProperty( "log4j.appender.A1", "org.apache.log4j.ConsoleAppender" );
        p.setProperty( "log4j.appender.A1.target", "System.err" );
        p.setProperty( "log4j.appender.A1.layout", "org.apache.log4j.PatternLayout" );
        p.setProperty( "log4j.appender.A1.layout.ConversionPattern", "%c %p - %m%n" );
        PropertyConfigurator.configure( p );

    }


    @Override
    protected void setUp() throws Exception
    {
        opc = new OPCPackage( "file:etc/test-data/maria.xlsx" );
        opc.process();
        super.setUp();
    }


    @Test
    public void test_targetCount()
    {
        assertTrue( opc.getEntryCollection().size() == 10 );
    }


    @Test
    public void test_entrySizeMatch()
    {
        assertTrue( opc.getEntryCollection().size() == opc.getEntryCollection().getPartNamesSet().size() );
    }


    @Test
    public void test_anEntry()
    {
        OOXMLTarget t = opc.getEntryCollection().getTargetByName( "/xl/worksheets/sheet1.xml" );
        assertTrue( t != null );
    }


    @Test
    public void test_entryType()
    {
        OOXMLTarget t = opc.getEntryCollection().getTargetByName( "/xl/worksheets/sheet1.xml" );
        assertTrue( t
                .getType()
                .equals(
                        "http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" ) );
    }
    
    @Test
    public void test_mimeType()
    {
        OOXMLTarget t = opc.getEntryCollection().getTargetByName( "/xl/worksheets/sheet1.xml" );
        assertTrue( t
                .getMimeType()
                .equals(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml" ) );
    }

}
