package controller;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;

public class DocumentGenerator {
public static void main(String[] args) {
	//DBpedia Dateien einlesen via Jena (RDFMgr / RDFParser)
	Model model = RDFDataMgr.loadModel("albert_einstein.ttl");
	
	String queryString = "@prefix dbr: <http://dbpedia.org/resource/>. select ?a ?p where {dbr:resource ?p ?a. }";
	
	Query q = QueryFactory.create(queryString);
	QueryExecution exec = QueryExecutionFactory.create(queryString, model);
	
	ResultSet results = exec.execSelect();
	
	while(results.hasNext())
		System.out.println(results.next().toString());
	// FOR Enties e (SELECT DISTINCT ?s WHERE {?s ?p ?o.})  : DBpedia 
	
			// '''DOKUMENTE ERZEUGT WERDEN'''
			
			//A nehmt euch ein tripel aus der gesamt DBpedia und üerlegt ob ihr es hinzufügt 
			//B 
			//C
			
	//Speichern der Dokumente 
	//a) LUCENE/Elasticsearch 
	//b) einfach in Dateien/Hashmap
}
}
