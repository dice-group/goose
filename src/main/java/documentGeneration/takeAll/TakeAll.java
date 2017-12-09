package documentGeneration.takeAll;

import documentGeneration.GeneratedDocument;
import documentGeneration.AbstractDocumentGenerator;
import index.TripleIndexer;
import org.aksw.triple2nl.TripleConverter;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.dllearner.kb.SparqlEndpointKS;
import org.dllearner.kb.sparql.SparqlEndpoint;


import javax.swing.text.Document;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class TakeAll extends AbstractDocumentGenerator {

    private TripleIndexer indexer;
    private TripleConverter converter;


    @Override
    public void init(String indexPath) throws IOException {
        indexer = new TripleIndexer(indexPath);
        converter = new TripleConverter();
    }

    @Override
    public void generate(Resource entity, ResultSet relations, String label) throws IOException {
        //?p ?o

        Node subject = NodeFactory.createURI(entity.getURI());

        //create list of triples
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
        String document = converter.convert(triples);
        GeneratedDocument gendoc = new GeneratedDocument(label, document);


        //debug
        if (subject.getLocalName().equals("")) return;

        if(controller.DocumentGenerator.DEBUG){
            label = label.replaceAll("/","_");
            File f = new File(System.getProperty("user.dir") + "/../debug/" + label);
            if(f.getParentFile() != null)
                f.getParentFile().mkdirs();
            f.createNewFile();
            FileOutputStream fs = new FileOutputStream(f);
            fs.write(gendoc.getDocument().getBytes());
            fs.close();
            //debug
        }


        indexer.addDocumentToIndex(gendoc);


    }

    public void finish() throws IOException {
        indexer.closeIndex();
    }

}



