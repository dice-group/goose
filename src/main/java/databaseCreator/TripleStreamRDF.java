package databaseCreator;

import org.apache.jena.atlas.lib.Sink;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.system.StreamRDFBase;

/**
 * This class sends the read triples to the specified Sink<Triple>
 */
public class TripleStreamRDF extends StreamRDFBase {

    Sink<Triple> sink;

    public TripleStreamRDF(Sink<Triple> sink)
    {
        this.sink = sink;
    }

    @Override
    public void triple(Triple triple) {
        sink.send(triple);
    }

    @Override
    public void finish() {
        sink.flush();
    }
}
