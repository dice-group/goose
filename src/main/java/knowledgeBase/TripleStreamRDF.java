package knowledgeBase;

import org.apache.jena.atlas.lib.Sink;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.system.StreamRDFBase;

/**
 * This class sends the read triples to the specified Sink<Triple>
 */
public class TripleStreamRDF extends StreamRDFBase {

    Sink<Triple> sink;

    /**
     * Constructs a TripleStreamRDF object with the specified
     * {@link Sink}.
     * @param sink
     */
    public TripleStreamRDF(Sink<Triple> sink)
    {
        this.sink = sink;
    }

    /**
     * Sends a found triple to the sink.
     * @param triple - triple to be send
     */
    @Override
    public void triple(Triple triple) {
        sink.send(triple);
    }

    /**
     * Flushes the sink if stream is finished.
     */
    @Override
    public void finish() {
        sink.flush();
    }
}
