package documentGeneration;

import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Resource;

//https://github.com/SmartDataAnalytics/SemWeb2NL
//https://github.com/dice-group/AGDISTIS/tree/master/src/main/java/org/aksw/agdistis/util
public interface IDocumentGenerator {

	public void generate(Resource entity, ResultSet relations);
}
