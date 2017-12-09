package controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;

import documentGeneration.AbstractDocumentGenerator;
import documentGeneration.takeAll.TakeAll;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.core.QueryExecutionFactoryDatasetGraph;
import org.aksw.jena_sparql_api.delay.core.QueryExecutionFactoryDelay;
import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp;
import org.aksw.jena_sparql_api.model.QueryExecutionFactoryModel;
import org.aksw.jena_sparql_api.pagination.core.QueryExecutionFactoryPaginated;
import org.apache.commons.io.FileUtils;
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


public class DocumentGenerator {

	public static boolean DEBUG = true;
	private static final String prefix = "PREFIX dbo:<http://dbpedia.org/ontology/>\n" +
									     "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n";
public static void main(String[] args) throws IOException {

    PropertyConfigurator.configure(System.getProperty("user.dir")+"/src/main/res/log4j.properties");

	//delete old files
	FileUtils.deleteDirectory(new File(System.getProperty("user.dir")+"/../debug"));
	FileUtils.deleteDirectory(new File(System.getProperty("user.dir")+"/../index"));
	//DBpedia Dateien einlesen via Jena (RDFMgr / RDFParser)
	//Model model = RDFDataMgr.loadModel("/home/lars/Dokumente/ProseminarSWT_Topic11/dbpedia/instance_types_en.ttl");

	//System.out.println("Loading labels");
	//load unique entities
	//Model entities = RDFDataMgr.loadModel(System.getProperty("user.dir") + "/src/main/res/labels_en1000.ttl");
	//System.out.println("Loading labels finished");
	//"http://sparql-full-text.cs.upb.de:3030/ds/sparql"
	//create query with caching to webservice
	//http://sparql-full-text.cs.upb.de:3030/ds/sparql
	//QueryExecutionFactory qef = new QueryExecutionFactoryHttp("http://dbpedia.org/sparql");

	//load tdb database
	DatasetGraphTDB db = DatasetBuilderStd.create(Location.create(System.getProperty("user.dir")+"/../tdb"));

	if(DEBUG)
		System.out.println("TDB successfully loaded!");
	QueryExecutionFactory qef = new QueryExecutionFactoryModel(db.getDefaultGraph());
	qef = new QueryExecutionFactoryDelay(qef, 0);
	qef = new QueryExecutionFactoryPaginated(qef, 1000);

	String entityQuery = prefix + " select distinct ?s ?o where {?s rdfs:label ?o}";
	QueryExecutionFactory entityEx = new QueryExecutionFactoryModel(db.getDefaultGraph());
	QueryExecution exec = entityEx.createQueryExecution(entityQuery);

	//get entities from entities model
	ResultSet entResults = exec.execSelect();

	AbstractDocumentGenerator generator = new TakeAll();
	generator.init(System.getProperty("user.dir")+"/../index");

	while(entResults.hasNext())
	{
		QuerySolution qEntity = entResults.nextSolution();
		Resource entity = qEntity.getResource("s");
		if(DEBUG)
			System.out.println(entity.getURI());
		String queryString = prefix + "select distinct ?p ?o where {{<"+entity.getURI()+"> ?p ?o.} " +
								"UNION {?o ?p <"+entity.getURI()+">.}\n" +
								"MINUS {<"+entity.getURI()+"> dbo:abstract ?o}.\n" +
								"MINUS {<"+entity.getURI()+"> dbo:wikiPageExternalLink ?o}.\n" +
								"MINUS {<"+entity.getURI()+"> dbo:wikiPageID ?o}.\n" +
								"MINUS {<"+entity.getURI()+"> dbo:wikiPageRevisionID ?o}.\n" +
								"MINUS {<"+entity.getURI()+"> rdfs:comment ?o}.}";
		//throw query at dbpedia
		QueryExecution query = qef.createQueryExecution(queryString);
		ResultSet relations = query.execSelect();

		//get label string
		String label = qEntity.get("o").asLiteral().getLexicalForm();
		if(DEBUG)
			System.err.println("Label: " + label);
		generator.generate(entity, relations, label);
		//get document from generator.generate() and throw it into lucene
	}
			
	//Speichern der Dokumente 
	//a) LUCENE/Elasticsearch 
	//b) einfach in Dateien/Hashmap
	generator.finish();
	db.close();
	}
}
