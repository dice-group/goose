package databaseCreator;

import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.model.QueryExecutionFactoryModel;
import org.apache.jena.atlas.lib.Sink;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.out.SinkTripleOutput;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.tdb.base.file.Location;
import org.apache.jena.tdb.setup.DatasetBuilderStd;
import org.apache.jena.tdb.store.DatasetGraphTDB;

import java.io.File;

public class TDBCreator {

    public static void main(String[] args)
    {
        String database = System.getProperty("user.dir")+"/../tdb";
        //select your own path to dbpedia files
        File fileDirectory = new File(System.getProperty("user.dir")+"/../dbpedia");
        DatasetGraphTDB db = DatasetBuilderStd.create(Location.create(database));

        Sink<Triple> output = new TDBSink(db);
        StreamRDF streamer = new TripleStreamRDF(output);

        //File f = new File(fileDirectory+"/labels_en.ttl");
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
