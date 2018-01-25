package search;

import knowledgeBase.DocumentGenerator;
import documentGeneration.AbstractDocumentGenerator;
import documentGeneration.takeConsideringPagerank.TakeConsideringPagerank;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.delay.core.QueryExecutionFactoryDelay;
import org.aksw.jena_sparql_api.model.QueryExecutionFactoryModel;
import org.aksw.jena_sparql_api.pagination.core.QueryExecutionFactoryPaginated;
import org.apache.commons.io.FileUtils;
import org.apache.jena.query.QueryExecution;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.tdb.base.file.Location;
import org.apache.jena.tdb.setup.DatasetBuilderStd;
import org.apache.jena.tdb.store.DatasetGraphTDB;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;

import org.apache.lucene.search.*;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;


import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

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

    private AbstractDocumentGenerator generator;

    /**
     * Sets up the OTFSearcher.
     * @param pathToIndex Path to index in that the index with labels and uris is stored.
     * @param pathToOTFIndex Path to index that will be created during the search.
     * @param pathToTDB Path to the tdb that holds all the data.
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
        //remove old otfindex
        this.pathToOTFIndex = pathToOTFIndex;
        FileUtils.deleteDirectory(new File(pathToOTFIndex));
        this.generator = new TakeConsideringPagerank(db.getDefaultGraph());
    }

    /**
     * Queries the index (containing entitylabel and uri) and searches for the given entitynames.
     * @param entityNames labels of entities
     * @return Set of all found uris corresponding to the entities out of entityNames
     * @throws IOException
     */
    private Set<String> urisToEntityName(String [] entityNames) throws IOException {
        Set<String> answers = new TreeSet<>();

        BooleanQuery.Builder bb = new BooleanQuery.Builder();
        for(String name : entityNames){
            if(Character.isUpperCase(name.trim().charAt(0))){
                PhraseQuery.Builder pb = new PhraseQuery.Builder();
                for(String part : name.split(" ")){
                    pb.add(new Term("label", part.toLowerCase()));
                }
                bb.add(pb.build(), BooleanClause.Occur.SHOULD);
            }
        }
        BooleanQuery bq = bb.build();
        TopDocs rs = searcher.search(bq, RESULTCOUNT);

        for(ScoreDoc doc : rs.scoreDocs){
            //workaround
            answers.add(reader.document(doc.doc).get("uri"));        }

        return answers;
    }

    /**
     * Searches for the keywords by generating a subset of DBpedia corresponding to the entity mentioned in keywords.
     * @param keywords Keywords which will be searched for.
     * @return Set of Strings as answers.
     * @throws IOException
     */
    public Set<String> search(String [] keywords) throws IOException{
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
        Directory otframdir = generateDocuments(related, new TreeSet<String>());
        if(DEBUG){
            System.out.println("-----DEBUG-----");
        }
        //5. return answers from TripleSearcher
        return executeSearch(otframdir, keywords);
    }

    /**
     * This method generates all documents and adds them to the index
     * @param related - the uris to create documents for
     * @param old - already contained uris
     * @return path to otf RAM dir
     * @throws IOException
     */
    public Directory generateDocuments(Set<Resource> related, Set<String> old) throws IOException
    {
        QueryExecutionFactory qef = new QueryExecutionFactoryModel(db.getDefaultGraph());
        qef = new QueryExecutionFactoryDelay(qef, 0);
        qef = new QueryExecutionFactoryPaginated(qef, 1000);

        System.out.println(related.size());
        //if(related.size()> 1500) return new TreeSet<>();


        generator.init(pathToOTFIndex);
        if(DEBUG)
            System.out.println("-----GENERATE-----");

        for(Resource uri : related){
            if(old.contains(uri.toString()))
                continue;
            String label;
            ResultSet relations;
            //get label of the uri
            String labelQuery = prefix + " select distinct ?l where{<"+ uri +"> rdfs:label ?l}";
            QueryExecution exec;
            try {
                exec = qef.createQueryExecution(labelQuery);
            }catch(Exception e)
            {
                continue;
            }
            ResultSet labels = exec.execSelect();
            if(!labels.hasNext()){
                continue;
            }
            label = labels.nextSolution().getLiteral("l").getLexicalForm();
            relations = qef.createQueryExecution(generator.getSPARQLQuery(uri.getURI())).execSelect();
            if(DEBUG)
                System.out.println(uri.getURI() + " : " + label);
            try{
                generator.generate(uri, relations, label);
            } catch (Exception e){
                System.err.println(label);
            }
        }
        Directory otframdir = generator.getIndexer().getIndexDict();

        generator.finish();

        return otframdir;
    }

    /**
     * This method calls the searcher with two keywords recursively until it used all keywords.
     * @param otframdir - path to otf index in memory
     * @param keywords - the keywords to be searched for
     * @return - a set of results
     */
    private Set<String> executeSearch(Directory otframdir, String[] keywords) throws IOException
    {
        if(keywords.length < 2)
            return new TreeSet<>();

        TripleSearcher t = new TripleSearcher(pathToOTFIndex, otframdir);
        Map<String, String> results = null;

        //generate first results only if first two keywords contain entity
        if(keywords[0].charAt(0) >= 'A' && keywords[0].charAt(0) <= 'Z'
                || keywords[1].charAt(0) >= 'A' && keywords[1].charAt(0) <= 'Z')
        {
            results = t.searchWith2Keywords(keywords[0], keywords[1]);
            if(2 <= keywords.length-1)
                t = new TripleSearcher(pathToOTFIndex, generateFurtherDocuments(results.keySet(), new TreeSet<String>()));

            //generate results for already got result and one new keyword
            for (int i = 2; i < keywords.length; i++) {
                Map<String, String> tmp = new HashMap<>();

                for (String result : results.keySet()) {
                    tmp.putAll(t.searchWith2Keywords(results.get(result), keywords[i]));
                }

                if (!tmp.keySet().isEmpty()) {
                    if (i < keywords.length - 1)
                        t = new TripleSearcher(pathToOTFIndex, generateFurtherDocuments(tmp.keySet(), results.keySet()));
                    results = tmp;
                }
            }

            if (!results.keySet().isEmpty())
                return results.keySet();
        }

        results = t.searchWith2Keywords(keywords[keywords.length-1], keywords[keywords.length-2]);
        if(0 <= keywords.length-3)
            t = new TripleSearcher(pathToOTFIndex, generateFurtherDocuments(results.keySet(), new TreeSet<String>()));

        //reverse if nothing found
        for(int i=keywords.length-3; i>= 0; i--)
        {
            Map<String, String> tmp = new HashMap<>();

            for (String result : results.keySet())
            {
                tmp.putAll(t.searchWith2Keywords(results.get(result), keywords[i]));
            }

            if(!tmp.keySet().isEmpty())
            {
                if(i > 0)
                    t=new TripleSearcher(pathToOTFIndex, generateFurtherDocuments(tmp.keySet(), results.keySet()));
                results = tmp;
            }
        }

        return results.keySet();
    }

    /**
     * Adds new found uris to the index.
     * @param uris - set containing the uris
     * @return directory of index
     */
    private Directory generateFurtherDocuments(Set<String> uris, Set<String> old) throws IOException
    {
        Set<Resource> related = getRelated(uris);

       return generateDocuments(related, old);
    }

    /**
     * Queries the database to get all related entities for uris out of uris
     * @param uris Set of uris
     * @return Set of Resources related to the uris out of uris
     */
    private Set<Resource> getRelated(Set<String> uris){
        HashSet<Resource> related = new HashSet<>();
        QueryExecutionFactory entityEx = new QueryExecutionFactoryModel(db.getDefaultGraph());
        for(String uri : uris){
            if(uri.toString().contains("\"")) continue;
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
     * Closes index and databases. Call after search is finished.
     * @throws IOException
     */
    public void close() throws IOException {
        if(db!= null)
        db.close();
        if(reader != null)
        reader.close();
    }

}
