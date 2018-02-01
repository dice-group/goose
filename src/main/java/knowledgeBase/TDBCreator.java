package knowledgeBase;

import org.apache.jena.atlas.lib.Sink;
import org.apache.jena.base.Sys;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.tdb.base.file.Location;
import org.apache.jena.tdb.setup.DatasetBuilderStd;
import org.apache.jena.tdb.store.DatasetGraphTDB;

import java.io.File;

/**
 * This class creates a database from all triples provided as .ttl files
 * in the given directory.
 */
public class TDBCreator {

    public static void main(String[] args)
    {
        String database;
        String files;
        if(args.length < 2){
            System.out.println("Usage:");
            System.out.println("1st arg: path where database will be created");
            System.out.println("2nd arg: path to DBpedia files");
            return;
        } else{
            database = args[0];
            files = args[1];
        }
        //select your own path to dbpedia files in triple store (ttl)
        File fileDirectory = new File(files);
        DatasetGraphTDB db = DatasetBuilderStd.create(Location.create(database));

        Sink<Triple> output = new TDBSink(db);
        StreamRDF streamer = new TripleStreamRDF(output);

        for(File f : fileDirectory.listFiles(new TTLFilter()))
        {
            System.out.println("Parsing " + f.getName());
            RDFParser.source(f.getPath()).parse(streamer);
            streamer.finish();
        }

        //test code
        /*System.out.println("Preparing entity query!");
        String entityQuery = "select distinct ?s where {?s ?p ?o}";
        QueryExecutionFactory entityEx = new QueryExecutionFactoryModel(db.getDefaultGraph());
        QueryExecution exec = entityEx.createQueryExecution(entityQuery);
        //get entities from entities model
        ResultSet entResults = exec.execSelect();
        System.out.println("Finished query!");

        while(entResults.hasNext())
        {
            QuerySolution q = entResults.next();
            System.out.println(q.get("s").asResource().getLocalName());
        }*/

        db.close();
    }
}
