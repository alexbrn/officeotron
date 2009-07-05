package org.probatron.officeotron;

import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;
import org.junit.Test;

import junit.framework.TestCase;

public class OOXMLValidationSessionTest extends TestCase
{
    OPCPackage  opc;
    OOXMLValidationSession ovs;

    static
    {
        // set up log message format, etc.
        String logLvl = System.getProperty( "property://probatron.org/officeotron-log-level" );
        logLvl = ( logLvl == null ) ? "TRACE" : logLvl;

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
        ovs = new OOXMLValidationSession( null );       
    }
    
    @Test
    public void test_packageIntegrity()
    {
        opc.process();
        ovs.checkRelationships( opc );        
        assertTrue( ovs.getErrCount() == 0 );
    }
    

}
