package documentGeneration;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.dllearner.kb.sparql.SparqlEndpoint;

import java.io.IOException;

//https://github.com/SmartDataAnalytics/SemWeb2NL
//https://github.com/dice-group/AGDISTIS/tree/master/src/main/java/org/aksw/agdistis/util
public abstract class AbstractDocumentGenerator {

	/**
	 * Initializes the DocumentGenerator. Creates indexer and other things.
	 * @param indexPath
	 * @throws IOException
	 */
	public abstract void init(String indexPath) throws IOException;

	/**
	 * Call when document generation is finished. Closes Indexer in other things.
	 * @throws IOException
	 */
	public abstract void finish() throws IOException;

	/**
	 * Generates a document to entity with the specified informations and saves it to the index.
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

}
