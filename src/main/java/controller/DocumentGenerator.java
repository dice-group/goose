package controller;

import java.io.IOException;
import java.sql.SQLException;

import documentGeneration.AbstractDocumentGenerator;
import documentGeneration.takeAll.TakeAll;
import org.aksw.jena_sparql_api.cache.core.QueryExecutionFactoryCacheEx;
import org.aksw.jena_sparql_api.cache.extra.CacheBackend;
import org.aksw.jena_sparql_api.cache.extra.CacheFrontend;
import org.aksw.jena_sparql_api.cache.extra.CacheFrontendImpl;
import org.aksw.jena_sparql_api.cache.h2.CacheCoreH2;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.delay.core.QueryExecutionFactoryDelay;
import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp;
import org.aksw.jena_sparql_api.model.QueryExecutionFactoryModel;
import org.aksw.jena_sparql_api.pagination.core.QueryExecutionFactoryPaginated;
import org.apache.commons.logging.impl.Log4JLogger;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.log4j.PropertyConfigurator;


public class DocumentGenerator {
public static void main(String[] args) throws IOException {


	//DBpedia Dateien einlesen via Jena (RDFMgr / RDFParser)
	//Model model = RDFDataMgr.loadModel("/home/lars/Dokumente/ProseminarSWT_Topic11/dbpedia/instance_types_en.ttl");

	System.out.println("Loading labels");
	//load unique entities
	Model entities = RDFDataMgr.loadModel(System.getProperty("user.dir") + "/src/main/res/labels_en1000.ttl");
	System.out.println("Loading labels finished");
	//"http://sparql-full-text.cs.upb.de:3030/ds/sparql"
	//create query with caching to webservice
	//http://sparql-full-text.cs.upb.de:3030/ds/sparql
	QueryExecutionFactory qef = new QueryExecutionFactoryHttp("http://dbpedia.org/sparql");
	qef = new QueryExecutionFactoryDelay(qef, 2000);
	long timeToLive = 24l * 60l * 60l * 1000l;
	/*CacheBackend cacheBackend;
	try {
		cacheBackend = CacheCoreH2.create("~/sparql", timeToLive, true);
	} catch (ClassNotFoundException e) {
		e.printStackTrace();
		return;
	} catch (SQLException e) {
		e.printStackTrace();
		return;
	}
	CacheFrontend cacheFrontend = new CacheFrontendImpl(cacheBackend);

	qef = new QueryExecutionFactoryCacheEx(qef, cacheFrontend);*/
	qef = new QueryExecutionFactoryPaginated(qef, 1000);

	// FOR Enties e (SELECT DISTINCT ?s WHERE {?s ?p ?o.})  : DBpedia 
	
			// '''DOKUMENTE ERZEUGT WERDEN'''
			
			//A nehmt euch ein tripel aus der gesamt DBpedia und üerlegt ob ihr es hinzufügt 
			//B 
			//C

	System.out.println("Preparing entity query!");
	String entityQuery = "select distinct ?s ?o where {?s ?p ?o}";
	QueryExecutionFactory entityEx = new QueryExecutionFactoryModel(entities);
	QueryExecution exec = entityEx.createQueryExecution(entityQuery);
	//get entities from entities model
	ResultSet entResults = exec.execSelect();
	System.out.println("Finished query!");


	AbstractDocumentGenerator generator = new TakeAll();
	generator.init(System.getProperty("user.dir")+"/../index");
	while(entResults.hasNext())
	{
		QuerySolution qEntity = entResults.nextSolution();
		Resource entity = qEntity.getResource("s");
		System.out.println(entity.getURI());
		String queryString = "select distinct ?p ?o where {<"+entity.getURI()+"> ?p ?o }";
		//throw query at dbpedia
		QueryExecution query = qef.createQueryExecution(queryString);
		ResultSet relations = query.execSelect();
		System.out.println("Queried successfully");

		//get label string
		String label = qEntity.get("o").asLiteral().getLexicalForm();
		generator.generate(entity, relations, label);
		//get document from generator.generate() and throw it into lucene
	}
			
	//Speichern der Dokumente 
	//a) LUCENE/Elasticsearch 
	//b) einfach in Dateien/Hashmap
	generator.finish();
	}
}
