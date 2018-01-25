package documentGeneration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;

import documentGeneration.AbstractDocumentGenerator;
import documentGeneration.takeAll.TakeAll;
import documentGeneration.takeConsideringPagerank.TakeConsideringPagerank;
import documentGeneration.takeOnlySPO.TakeOnlySPO;
import index.OTFIndexer;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.core.QueryExecutionFactoryDatasetGraph;
import org.aksw.jena_sparql_api.delay.core.QueryExecutionFactoryDelay;
import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp;
import org.aksw.jena_sparql_api.model.QueryExecutionFactoryModel;
import org.aksw.jena_sparql_api.pagination.core.QueryExecutionFactoryPaginated;
import org.apache.commons.io.FileUtils;
import org.apache.jena.base.Sys;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.tdb.base.file.Location;
import org.apache.jena.tdb.setup.DatasetBuilderStd;
import org.apache.jena.tdb.store.DatasetGraphTDB;
import org.apache.log4j.PropertyConfigurator;
import org.openrdf.query.algebra.Str;


public class DocumentGenerator {

	public static final boolean DEBUG = false;
	public static final boolean OTFMODE = true;
	private static final String prefix = "PREFIX dbo:<http://dbpedia.org/ontology/>\n" +
									     "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n";
	public static void main(String[] args) throws IOException
	{
		PropertyConfigurator.configure(System.getProperty("user.dir") + "/src/main/res/log4j.properties");

		if (args.length < 2) {
			System.err.println("Must be called like: " +
					"java -jar GOOSE.jar <path to database> <directory to store index in>");
			System.exit(-1);
		}

		File dbDir = new File(args[0]);
		File indexDir = new File(args[1]);

		if (!dbDir.exists() || !dbDir.isDirectory()) {
			System.err.println("Database file path must exist and be a directory!");
			System.exit(-1);
		}
		if (indexDir.exists()) {
			System.err.println("Index file path mustn't exist!");
			System.exit(-1);
		}

		//http://sparql-full-text.cs.upb.de:3030/ds/sparql
		//QueryExecutionFactory qef = new QueryExecutionFactoryHttp("http://dbpedia.org/sparql");

		//load tdb database
		DatasetGraphTDB db = DatasetBuilderStd.create(Location.create(dbDir.getPath()));

		if (DEBUG)
			System.out.println("TDB successfully loaded!");

		QueryExecutionFactory qef = new QueryExecutionFactoryModel(db.getDefaultGraph());
		qef = new QueryExecutionFactoryDelay(qef, 0);
		qef = new QueryExecutionFactoryPaginated(qef, 1000);


		//generating documents for two strategies or OTFMODE
		for (int i = 0; i < (OTFMODE ? 1 : 2); i++) {
			String entityQuery = prefix + " select distinct ?s ?o where {?s rdfs:label ?o}";
			QueryExecutionFactory entityEx = new QueryExecutionFactoryModel(db.getDefaultGraph());
			QueryExecution exec = entityEx.createQueryExecution(entityQuery);

			//get entities from entities model
			ResultSet entResults = exec.execSelect();
			AbstractDocumentGenerator generator = null;

			if(OTFMODE){
				OTFIndexer otf = new OTFIndexer(indexDir.getPath());
				QuerySolution qEntity = null;
				for(int cnt = 1; entResults.hasNext(); cnt++){
					qEntity = entResults.next();
					String uri = qEntity.getResource("s").getURI();
					String label = qEntity.get("o").asLiteral().getLexicalForm();
					otf.addDocumentToIndex(label, uri);
					if(cnt % 10000 == 0){
						System.out.println(cnt);
					}
				}
				otf.closeIndex();
				break;
			}

			if(i==0)
				generator = new TakeConsideringPagerank(db.getDefaultGraph());
			else
				generator = new TakeOnlySPO();

			generator.init(indexDir.getAbsolutePath(), i);


			while (entResults.hasNext()) {
				QuerySolution qEntity = entResults.nextSolution();
				Resource entity = qEntity.getResource("s");
				if (DEBUG)
					System.out.println(entity.getURI());

				//throw query at dbpedia
				QueryExecution query = qef.createQueryExecution(generator.getSPARQLQuery(entity.getURI()));
				ResultSet relations = query.execSelect();

				//get label string
				String label = qEntity.get("o").asLiteral().getLexicalForm();
				if (DEBUG)
					System.err.println("Label: " + label);
				
				try {
					generator.generate(entity, relations, label);
				}catch(Exception e)
				{
					e.printStackTrace();
					System.out.println("Continuing..");
				}
			}

			generator.finish();
		}
		db.close();
	}
}
