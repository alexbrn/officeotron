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

import org.apache.log4j.Logger;

public class OOXMLValidationSession extends ValidationSession
{
    static Logger logger = Logger.getLogger( OOXMLValidationSession.class );


    public OOXMLValidationSession( Submission submission )
    {
        super( submission );
    }


    public void validate()
    {
        OPCPackage opc = new OPCPackage( this.getSubmission().getCandidateUrl() );
        checkRelationships( opc );
        validateCandidates( opc );
    }


    public void checkRelationships( OPCPackage opc )
    {
        logger.trace( "Beginning package integrity test" );
        this.getCommentary().addComment( "Checking Package relationship integrity" );
        this.getCommentary().incIndent();

        logger.trace( "Collection size: " + opc.getEntryCollection().size() );

        for( int i = 0; i < opc.getEntryCollection().size(); i++ )
        {
            OOXMLTarget t = opc.getEntryCollection().get( i );
            String mt = t.getMimeType();

            logger.trace( "Testing entry of MIME type: " + mt );

            OOXMLSchemaMapping osm = OOXMLSchemaMap.getMappingForContentType( mt );

            if( osm == null )
            {
                logger.info( "No mapping found for entry" );
                continue;
            }

            if( !t.getType().equals( osm.getRelType() ) )
            {
                logger.debug( "Relationship type mismatch" );
                this.errCount++;
                this.getCommentary().addComment(
                        "ERROR",
                        "Entry with MIME type \"" + mt
                                + "\" has unrecognized relationship type \"" + t.getType()
                                + "\"" );
            }

        }

        this.getCommentary().decIndent();

    }


    void validateCandidates( OPCPackage opc )
    {
        OOXMLTargetCollection col = opc.getEntryCollection();

    }

}
