package documentGeneration;

public class GeneratedDocument {

    private String entity;
    private String uri;
    private String document;

    /**
     * Creates a new document. Takes the URI (<http://dbpedia.org/resource/entity>) provided as entity as key. And the document as value.
     * @param entity URI of the entity of the document
     * @param document generated document text for the entity
     */
    public GeneratedDocument(String entity, String uri, String document) throws IllegalArgumentException{
        //test wether entity is an URI
            this.uri = uri;
            this.entity = entity;
            this.document = document;
    }

    /**
     * Returns the name of the entity
     * @return URI string
     */
    public String getEntity(){
        return entity;
    }

    /**
     * Returns the uri of the entity
     * @return
     */
    public String getUri(){
        return uri;
    }

    /**
     * Returns the document of the entity
     * @return document string
     */
    public String getDocument(){
        return document;
    }

    /**
     * Takes a label for an entity generated by Triple2NL and creates URI out of it.
     * @param label label from Triple2NL
     * @return URI of the entity
     */
    public static String generateURIOutOfTriple2NLLabel(String label){
        label =  label.replaceAll(" ", "_");
        return "http://dbpedia.org/resource/" + label;
    }

}
