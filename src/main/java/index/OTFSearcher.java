package index;

import controller.DocumentGenerator;
import documentGeneration.AbstractDocumentGenerator;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.delay.core.QueryExecutionFactoryDelay;
import org.aksw.jena_sparql_api.model.QueryExecutionFactoryModel;
import org.aksw.jena_sparql_api.pagination.core.QueryExecutionFactoryPaginated;
import org.apache.jena.base.Sys;
import org.apache.jena.query.QueryExecution;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.ResourceRequiredException;
import org.apache.jena.tdb.base.file.Location;
import org.apache.jena.tdb.setup.DatasetBuilderStd;
import org.apache.jena.tdb.store.DatasetGraphTDB;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;

import org.apache.lucene.search.*;

import org.apache.lucene.store.FSDirectory;
import org.openrdf.query.algebra.Str;


import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

public class OTFSearcher {

    private final int RESULTCOUNT = 1000;
    private IndexSearcher searcher;
    private IndexReader reader;
    private DatasetGraphTDB db;
    private static final String prefix =  "PREFIX dbo:<http://dbpedia.org/ontology/>\n" +
            "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n" +
            "PREFIX res:<http://dbpedia.org/resource/>" ;
    private final String pathToOTFIndex;

    private final static boolean DEBUG = DocumentGenerator.DEBUG;

    /**
     * Sets up the Searcher.
     * @throws IOException
     */
    public OTFSearcher(String pathToIndex, String pathToOTFIndex, String pathToTDB) throws IOException {
        //open directory of the index
        FSDirectory indexDict = FSDirectory.open(Paths.get(pathToIndex));
        //create new indexsearcher for the index
        reader = DirectoryReader.open(indexDict);
        searcher = new IndexSearcher(reader);
        //load Database
        db = DatasetBuilderStd.create(Location.create(pathToTDB));
        this.pathToOTFIndex = pathToOTFIndex;
    }

    public Set<String> urisToEntityName(String [] entityNames) throws IOException {
        Set<String> answers = new TreeSet<>();

        BooleanQuery.Builder bb = new BooleanQuery.Builder();
        for(String name : entityNames){
            PhraseQuery.Builder pb = new PhraseQuery.Builder();
            for(String part : name.split(" ")){
                pb.add(new Term("label", part.toLowerCase()));
            }
            bb.add(pb.build(), BooleanClause.Occur.SHOULD);
        }
        BooleanQuery bq = bb.build();
        TopDocs rs = searcher.search(bq, RESULTCOUNT);

        for(ScoreDoc doc : rs.scoreDocs){
            answers.add(reader.document(doc.doc).get("uri"));        }

        return answers;
    }

    public Set<String> search(String [] keywords, AbstractDocumentGenerator generator) throws IOException{
        //1. get all URI associated with the question
        Set<String> uris = urisToEntityName(keywords);
        if(DEBUG){
            System.out.println("-----DEBUG-----");
            System.out.println("Keyword Entities:");
            for(String uri : uris){
                System.out.println(uri);
            }
        }

        //2. get all entities that have relations to the entities in uris and generate their documents
        Set<Resource> related = getRelated(uris);
        if(DEBUG){
            System.out.println("Related Entities:");
            for(Resource uri : related){
                System.out.println(uri.getURI());
            }
        }
        //3. generate all documents for entities in toBeGenerated and store them in the OTFIndex
        QueryExecutionFactory qef = new QueryExecutionFactoryModel(db.getDefaultGraph());
        qef = new QueryExecutionFactoryDelay(qef, 0);
        qef = new QueryExecutionFactoryPaginated(qef, 1000);

        generator.init(pathToOTFIndex);
        System.out.println("-----GENERATE-----");
        for(Resource uri : related){
            String label;
            ResultSet relations;

            //get label of the uri
            String labelQuery = prefix + " select distinct ?l where{<"+ uri +"> rdfs:label ?l}";
            QueryExecution exec = qef.createQueryExecution(labelQuery);
            ResultSet labels = exec.execSelect();
            if(!labels.hasNext()){
                continue;
            }
            label = labels.nextSolution().getLiteral("l").getLexicalForm();
            relations = qef.createQueryExecution(generator.getSPARQLQuery(uri.getURI())).execSelect();
            System.out.println(uri.getURI() + " : " + label + " : "+ relations.hasNext());

            generator.generate(uri, relations, label);

        }

        generator.finish();
        db.close();
        if(DEBUG){
            System.out.println("-----DEBUG-----");
        }
        //5. return answers from TripleSearcher
        TripleSearcher tripleSearcher = new TripleSearcher(pathToOTFIndex);

        return tripleSearcher.searchInIndex(keywords);
    }

    private Set<Resource> getRelated(Set<String> uris){
        HashSet<Resource> related = new HashSet<>();
        QueryExecutionFactory entityEx = new QueryExecutionFactoryModel(db.getDefaultGraph());
        for(String uri : uris){
            String entityQuery = prefix + "select distinct ?s where {?s ?b <"+ uri +">}";
            QueryExecution exec = entityEx.createQueryExecution(entityQuery);
            ResultSet ent = exec.execSelect();

            while(ent.hasNext()){
                QuerySolution sol = ent.nextSolution();
                Resource ruri = sol.getResource("s");
                related.add(ruri);
            }
            related.add(ResourceFactory.createProperty(uri).asResource());
        }

        return related;
    }

    /**
     * Close all resources when task is finished
     * @throws IOException
     */
    public void close() throws IOException {
        db.close();
        reader.close();
    }

}
