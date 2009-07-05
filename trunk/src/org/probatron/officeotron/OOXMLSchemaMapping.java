package org.probatron.officeotron;

public class OOXMLSchemaMapping
{
    private String clause;
    private String contentType;
    private String ns;
    private String relType;
    private String schemaName;


    public OOXMLSchemaMapping( String clause, String contentType, String ns, String relType,
            String schemaName )
    {
        this.clause = clause;
        this.contentType = contentType;
        this.ns = ns;
        this.relType = relType;
        this.schemaName = schemaName;
    }


    public String getClause()
    {
        return clause;
    }


    public String getContentType()
    {
        return contentType;
    }


    public String getNs()
    {
        return ns;
    }


    public String getRelType()
    {
        return relType;
    }


    public String getSchemaName()
    {
        return schemaName;
    }

}
