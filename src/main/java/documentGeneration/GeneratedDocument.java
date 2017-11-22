package documentGeneration;

import java.util.ArrayList;

public class GeneratedDocument {

    private String entity;
    private String document;

    public GeneratedDocument(String entity, String document){
        this.entity = entity;
        this.document = document;
    }


    public String getEntity(){
        return entity;
    }

    public String getDocument(){
        return document;
    }
}
