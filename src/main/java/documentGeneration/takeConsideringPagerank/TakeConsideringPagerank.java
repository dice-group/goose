package documentGeneration.takeConsideringPagerank;

import documentGeneration.takeAll.TakeAll;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.model.QueryExecutionFactoryModel;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import java.io.IOException;
import java.util.ArrayList;

/**
 * This strategy only takes triples whose subject is the entity
 * and only puts relations in the entity's document whose pagerank multiplied by
 * the thresholdFactor is at least the pagerank of the entity or that are properties
 * of the entity.
 */
public class TakeConsideringPagerank extends TakeAll {

    private QueryExecutionFactory qef;
    private double pagerank;
    private double thresholdFactor = 2;

    /**
     * Constructs a new TakeConsideringPagerank instance with the specified
     * graph to query against
     * @param databse queryable databse for pagerank of subject to create document for
     */
    public TakeConsideringPagerank(Graph databse)
    {
        super();
        qef = new QueryExecutionFactoryModel(databse);
        pagerank=0;
    }

    @Override
    public String getSPARQLQuery(String uri)
    {
        return PREFIX + "select distinct ?p ?o ?r where {{<"+uri+"> ?p ?o. optional{?o vrank:hasRank/vrank:rankValue ?r.}} " +
                "MINUS {<"+uri+"> dbo:abstract ?o}.\n" +
                "MINUS {<"+uri+"> dbo:wikiPageExternalLink ?o}.\n" +
                "MINUS {<"+uri+"> dbo:wikiPageID ?o}.\n" +
                "MINUS {<"+uri+"> dbo:wikiPageRevisionID ?o}.\n" +
                "MINUS {<"+uri+"> rdfs:comment ?o}.}";
    }

    /**
     * Generates the triple list by filtering with pagerank of subject
     * @param subject  the subject of the triple
     * @param relations  the set of relations of the subject
     * @return the list of triples representing the document
     */
    @Override
    protected ArrayList<Triple> generateTripleList(Node subject, ResultSet relations)
    {
        ArrayList<Triple> triples = new ArrayList<>();

        while (relations.hasNext()) {
            QuerySolution sol = relations.next();
            RDFNode p = sol.get("p");
            RDFNode o = sol.get("o");
            Literal pagerankLiteral = sol.getLiteral("r");
            Node predicate = rdfNodeToNode(p);
            Node object = rdfNodeToNode(o);

            //if there is no pagerank, the triple is a property -> must be included
            if(pagerankLiteral == null || pagerankLiteral.getDouble()*thresholdFactor >= pagerank)
            {
                Triple t = Triple.create(subject, predicate, object);
                triples.add(t);
            }
        }

        return triples;
    }

    /**
     * Preprocesses the pagerank for the subject and then calls TakeAll.generate()
     * @param entity  the entity to generate a document for
     * @param relations  the relations of the entity
     * @param label  the label of the entity
     * @throws IOException
     */
    @Override
    public void generate(Resource entity, ResultSet relations, String label) throws IOException
    {
        String pageRankSubjectquery = PREFIX + "select distinct ?pagerank " +
                "where {<"+entity.getURI()+"> vrank:hasRank/vrank:rankValue ?pagerank.}";

        QueryExecution exec = qef.createQueryExecution(pageRankSubjectquery);
        ResultSet result = exec.execSelect();

        if(result.hasNext())
            pagerank = result.next().getLiteral("pagerank").getDouble();
        else pagerank = 0;

        super.generate(entity, relations, label);
    }
}
