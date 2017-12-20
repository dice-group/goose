package documentGeneration.takeAll;

import documentGeneration.GeneratedDocument;
import documentGeneration.AbstractDocumentGenerator;
import index.TripleIndexer;
import org.aksw.triple2nl.TripleConverter;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * This strategy simply takes all triples corresponding to the given entity
 * and puts them as native language into the document.
 */
public class TakeAll extends AbstractDocumentGenerator {

    private TripleIndexer indexer;
    private TripleConverter converter;
    private String debugPath;

    @Override
    public void init(String indexPath) throws IOException {
        indexer = new TripleIndexer(indexPath);
        debugPath = indexPath.substring(0, indexPath.lastIndexOf(File.separator))+"/debug";
        converter = new TripleConverter();
    }

    @Override
    public void init(String indexPath, int indexNumber) throws IOException
    {
        indexer = new TripleIndexer(indexPath+"/" + indexNumber);
        debugPath = indexPath + "/debug/" + indexNumber;
        converter = new TripleConverter();
    }

    @Override
    public String getSPARQLQuery(String uri) {

        return PREFIX + "select distinct ?p ?o where {{<"+uri+"> ?p ?o.} " +
                "UNION {?o ?p <"+uri+">.}\n" +
                "MINUS {<"+uri+"> dbo:abstract ?o}.\n" +
                "MINUS {<"+uri+"> dbo:wikiPageExternalLink ?o}.\n" +
                "MINUS {<"+uri+"> dbo:wikiPageID ?o}.\n" +
                "MINUS {<"+uri+"> dbo:wikiPageRevisionID ?o}.\n" +
                "MINUS {<"+uri+"> rdfs:comment ?o}.}";
    }

    /**
     * This method generates a list of all triples to be included
     * into the generated document.
     * @param subject - the subject of the triple
     * @param relations - the set of relations of the subject
     * @return {@link ArrayList} of type {@link Triple} representing the document
     */
    protected ArrayList<Triple> generateTripleList(Node subject, ResultSet relations)
    {
        ArrayList<Triple> triples = new ArrayList<>();

        while (relations.hasNext()) {
            QuerySolution sol = relations.next();
            RDFNode p = sol.get("p");
            RDFNode o = sol.get("o");
            Node predicate = rdfNodeToNode(p);
            Node object = rdfNodeToNode(o);

            Triple t = Triple.create(subject, predicate, object);
            triples.add(t);
        }

        return triples;
    }

    @Override
    public void generate(Resource entity, ResultSet relations, String label) throws IOException {
        //?p ?o

        Node subject = NodeFactory.createURI(entity.getURI());

        //create list of triples and convert them to document
        String document = converter.convert(generateTripleList(subject, relations));

        try{

            GeneratedDocument gendoc = new GeneratedDocument(label,entity.getURI(), document);

            //debug
            if (subject.getLocalName().equals("")) return;

            if(controller.DocumentGenerator.DEBUG){
                label = label.replaceAll("/","_");
                File f = new File(debugPath + "/" + label);
                if(f.getParentFile() != null)
                    f.getParentFile().mkdirs();
                f.createNewFile();
                FileOutputStream fs = new FileOutputStream(f);
                fs.write(gendoc.getDocument().getBytes());
                fs.close();
                //debug
            }
            indexer.addDocumentToIndex(gendoc);

        } catch(IllegalArgumentException ia){
            //entity has no valid URI => don't store it into the index
            return;
        }
    }

    public void finish() throws IOException {
        indexer.closeIndex();
    }
}



