package index;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;

import documentGeneration.GeneratedDocument;

import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;

import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

/**
 * This Indexer implements an OTF Index. It saves only the URI and the name of the Entity. Later in the
 * searching process we will search for entity uri's in this index. Get the URI, query the database and create
 * the needed documents on the fly. We will use this approach when we are running out of time. No creation of the
 * whole dbpedia index is needed for running the evaluation.
 */
public class OTFIndexer {

    private IndexWriter writer;

    /**
     * Sets up the IndexWriter with a Standard Config
     * @throws IOException
     */
    public OTFIndexer(String pathToIndex) throws IOException{
        // directory for the indexer
        FSDirectory indexDict = FSDirectory.open(Paths.get(pathToIndex));
        //create indexer with standard conifg
        IndexWriterConfig conf = new IndexWriterConfig(new SimpleAnalyzer());
        writer = new IndexWriter(indexDict, conf);
    }

    /**
     * Adds the content and the entity to the index.
     * @param labelString the label of the entity, will not be stored
     * @param uriString the URI of the entity, will be stored
     * @throws IOException
     */
    public void addDocumentToIndex(String labelString, String uriString) throws IOException {
        //create document in lucene for the document
        Document document = new Document();
        //add field for the entity to the document
        IndexableField entity = new TextField("label", labelString, Field.Store.NO);
        document.add(entity);
        IndexableField uri = new TextField("uri", uriString, Field.Store.YES);
        document.add(uri);

        //add document to index
        writer.addDocument(document);
    }

    /**
     * Closes the index.
     * @throws IOException
     */
    public void closeIndex() throws IOException {

        writer.close();

    }
}
