package org.probatron.officeotron;

import java.util.ArrayList;
import java.util.Collections;

public class XMLNameSet
{
    private ArrayList<String> lst = new ArrayList<String>();


    private boolean contains( String synth )
    {
        return Collections.binarySearch( this.lst, synth ) >= 0;
    }


    void put( String uri, String local )
    {
        String synth = "{" + uri + "}" + local;
        if( ! contains( uri, local ) )
        {
            this.lst.add( synth );
            Collections.sort( this.lst );
        }

    }


    boolean contains( String uri, String local )
    {
        String synth = "{" + uri + "}" + local;
        return contains( synth );

    }

}
