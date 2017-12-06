package databaseCreator;

import org.apache.jena.atlas.lib.Sink;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.TransactionalNotSupported;
import org.apache.jena.tdb.store.DatasetGraphTDB;

/**
 * This class stores the read triples into the tdb databse.
 */
public class TDBSink implements Sink<Triple> {

    private DatasetGraphTDB tdb;

    public TDBSink(DatasetGraphTDB tdb)
    {
        this.tdb = tdb;
    }

    @Override
    public void send(Triple triple) {

        tdb.getTripleTable().add(triple);
    }

    @Override
    public void flush() {
        tdb.sync();
    }

    @Override
    public void close() {

    }
}
