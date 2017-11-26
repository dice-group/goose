package controller;

import java.sql.SQLException;

import documentGeneration.IDocumentGenerator;
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
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;


public class DocumentGenerator {
public static void main(String[] args) {
	//DBpedia Dateien einlesen via Jena (RDFMgr / RDFParser)
	//Model model = RDFDataMgr.loadModel("/home/lars/Dokumente/ProseminarSWT_Topic11/dbpedia/instance_types_en.ttl");

	System.out.println("Loading labels");
	//load unique entities
	Model entities = RDFDataMgr.loadModel(System.getProperty("user.dir") + "/../dbpedia/labels_en.ttl");
	System.out.println("Loading labels finished");
	//"http://sparql-full-text.cs.upb.de:3030/ds/sparql"
	//create query with caching to webservice
	QueryExecutionFactory qef = new QueryExecutionFactoryHttp("http://sparql-full-text.cs.upb.de:3030/ds/sparql");
	qef = new QueryExecutionFactoryDelay(qef, 2000);
	long timeToLive = 24l * 60l * 60l * 1000l;
	CacheBackend cacheBackend;
	try {
		cacheBackend = CacheCoreH2.create("sparql", timeToLive, true);
	} catch (ClassNotFoundException e) {
		e.printStackTrace();
		return;
	} catch (SQLException e) {
		e.printStackTrace();
		return;
	}
	CacheFrontend cacheFrontend = new CacheFrontendImpl(cacheBackend);

	qef = new QueryExecutionFactoryCacheEx(qef, cacheFrontend);
	qef = new QueryExecutionFactoryPaginated(qef, 1000);

	// FOR Enties e (SELECT DISTINCT ?s WHERE {?s ?p ?o.})  : DBpedia 
	
			// '''DOKUMENTE ERZEUGT WERDEN'''
			
			//A nehmt euch ein tripel aus der gesamt DBpedia und üerlegt ob ihr es hinzufügt 
			//B 
			//C

	System.out.println("Preparing entity query!");
	String entityQuery = "select distinct ?s where {?s ?p ?o} LIMIT 100";
	QueryExecutionFactory entityEx = new QueryExecutionFactoryModel(entities);
	QueryExecution exec = entityEx.createQueryExecution(entityQuery);
	//get entities from entities model
	ResultSet entResults = exec.execSelect();
	System.out.println("Finished query!");
	while(entResults.hasNext())
	{
		QuerySolution qEntity = entResults.nextSolution();
		Resource entity = qEntity.getResource("s");
		System.out.println(entity.getURI());
		//String entityQuery = "select distinct ?p ?o where {"+entity.getURI()+" ?p ?o }";
		//throw query at dbpedia
		ResultSet relations = null;
		IDocumentGenerator generator = null;
		//generator.generate(entity, relations);
		//get document from generator.generate() and throw it into lucene
	}
			
	//Speichern der Dokumente 
	//a) LUCENE/Elasticsearch 
	//b) einfach in Dateien/Hashmap
	}
}
