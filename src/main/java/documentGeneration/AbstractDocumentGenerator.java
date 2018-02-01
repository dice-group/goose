package documentGeneration;

import index.TripleIndexer;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.dllearner.kb.sparql.SparqlEndpoint;

import java.io.IOException;

/**
 * This class provides all necessary methods in order to create
 * a new strategy for document generation. Every document generation
 * strategy has to extend this abstract class or another strategy
 * extending this abstract class.
 */
public abstract class AbstractDocumentGenerator {
	protected TripleIndexer indexer;
	//query prefix for all extending classes
	protected static final String PREFIX = "PREFIX dbo:<http://dbpedia.org/ontology/>\n" +
			"PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n" +
			"PREFIX vrank:<http://purl.org/voc/vrank#>\n";

	/**
	 * Initializes the DocumentGenerator. Creates indexer and other things.
	 * @param indexPath
	 * @throws IOException
	 */
	public abstract void init(String indexPath) throws IOException;

	/**
	 * Initializes the DocumentGenerator. Creates indexer and other things and gives indices numbers.
	 * @param indexPath - path to index root directory
	 * @param indexNumber - name of directory of index with that indexNumber
	 * @throws IOException
	 */
	public abstract void init(String indexPath, int indexNumber) throws IOException;

	/**
	 * Constructs the SPARQL-Query used to get the relationships for the specified resource uri
	 * @param uri - the uri of the resource to query
	 * @return String representing the query
	 */
	public abstract String getSPARQLQuery(String uri);

	/**
	 * Call when document generation is finished. Closes Indexer and other things.
	 * @throws IOException
	 */
	public abstract void finish() throws IOException;

	/**
	 * Generates a document for entity with the specified information and saves it to the index.
	 * @param entity Entity ?s
	 * @param relations Triple relations ?p ?o
	 * @throws IOException
	 */
	public abstract  void generate(Resource entity, ResultSet relations, String label) throws IOException;


	/**
	 * Converts a RDFGraphNode to a Node that can be used for sem2webnl
	 * @param n
	 * @return
	 */
	protected Node rdfNodeToNode(RDFNode n){
		Node node;
		if (n.isLiteral()) {
			Literal pred = n.asLiteral();
			node = NodeFactory.createLiteral(pred.getLexicalForm(), pred.getLanguage());
		} else {
			Resource pred = n.asResource();
			node = NodeFactory.createURI(pred.toString());
		}
		return node;
	}

	/**
	 * Gives access to the indexer of this instance.
	 * @return the indexer used to store generated documents
	 */
	public TripleIndexer getIndexer(){
		return indexer;
	}
}
