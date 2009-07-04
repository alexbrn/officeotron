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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.apache.log4j.Logger;

@SuppressWarnings("serial")
public class OOXMLTargetCollection extends ArrayList<OOXMLTarget>
{
    static Logger logger = Logger.getLogger( OOXMLTargetCollection.class );

    private HashMap<String, OOXMLTarget> partNameMap = new HashMap<String, OOXMLTarget>();


    @Override
    public boolean add( OOXMLTarget t )
    {
        logger.debug( "Adding entry to target collection " + t.getQPartname() );
        partNameMap.put( t.getQPartname(), t );
        return super.add( t );
    }


    public Collection<String> getPartNamesSet()
    {
        return this.partNameMap.keySet();
    }


    public OOXMLTarget getTargetByName( String s )
    {
        return this.partNameMap.get( s );
    }

}
