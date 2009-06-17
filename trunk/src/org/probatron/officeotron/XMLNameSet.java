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
