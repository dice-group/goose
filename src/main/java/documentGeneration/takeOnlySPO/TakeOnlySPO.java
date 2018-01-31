package documentGeneration.takeOnlySPO;

import documentGeneration.takeAll.TakeAll;

/**
 * This strategy only takes triples whose subject is the entity
 * and puts them into the document by calling the same generation as in TakeAll
 */
public class TakeOnlySPO extends TakeAll {

    @Override
    public String getSPARQLQuery(String uri) {

        return PREFIX + "select distinct ?p ?o where {{<"+uri+"> ?p ?o.} " +
                "MINUS {<"+uri+"> dbo:abstract ?o}.\n" +
                "MINUS {<"+uri+"> dbo:wikiPageExternalLink ?o}.\n" +
                "MINUS {<"+uri+"> dbo:wikiPageID ?o}.\n" +
                "MINUS {<"+uri+"> dbo:wikiPageRevisionID ?o}.\n" +
                "MINUS {<"+uri+"> rdfs:comment ?o}.}";
    }
}
