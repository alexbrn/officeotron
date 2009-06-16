package org.probatron.officeotron;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class ODFSniffer extends XMLSniffer
{
    private String generator = "";
    private boolean harvestGenerator;
    final static String ODF_DOC_NS = "urn:oasis:names:tc:opendocument:xmlns:office:1.0";
    final static String ODF_META_NS = "urn:oasis:names:tc:opendocument:xmlns:meta:1.0";


    @Override
    public void startElement( String uri, String localName, String name, Attributes atts )
            throws SAXException
    {
        super.startElement( uri, localName, name, atts );

        this.harvestGenerator = ( uri.equals( ODF_META_NS ) && localName.equals( "generator" ) );
    }


    @Override
    public void characters( char[] ch, int start, int length ) throws SAXException
    {
        super.characters( ch, start, length );

        if( this.harvestGenerator )
        {
            this.generator += new String( ch, start, length );
        }
    }


    public String getGenerator()
    {
        return generator;
    }

}
