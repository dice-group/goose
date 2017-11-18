package controller;

import java.io.File;
import java.io.FileFilter;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.tdb.TDBFactory;

public class DocumentGenerator {
public static void main(String[] args) {
	//DBpedia Dateien einlesen via Jena (RDFMgr / RDFParser)
	//Model model = RDFDataMgr.loadModel("/home/lars/Dokumente/ProseminarSWT_Topic11/dbpedia/instance_types_en.ttl");
	String queryString = "PREFIX dbr: <http://dbpedia.org/resource/>\n select distinct ?s where {dbr: ?s.}";

	//use your local path
	Dataset dataset = TDBFactory.createDataset(System.getProperty("user.dir")+"/../db");
	Model model = dataset.getDefaultModel();
	//use your local path
	File dir = new File(System.getProperty("user.dir")+"/../dbpedia");
	FileFilter ff = new TTLFilter();
	
	//read all .ttl files into model
	for(File f : dir.listFiles(ff))
	{
		System.out.println("Loading " + f.getAbsolutePath());
		RDFDataMgr.read(model, f.getAbsolutePath());
	}
	
	//TODO: correct ontology model creation with dbpedia_2016-10.owl
	OntModel ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM, model);
	//use your local path
	ontModel.getImportedModel(System.getProperty("user.dir")+"/../dbpedia/dbpedia_2016-10.owl");
	
	Query q = QueryFactory.create(queryString);
	QueryExecution exec = QueryExecutionFactory.create(queryString, model);
	ResultSet results = exec.execSelect();
	
	while(results.hasNext())
		System.out.println(results.nextSolution().toString());
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
