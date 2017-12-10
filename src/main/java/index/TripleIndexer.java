package index;

import java.io.*;
        import java.nio.file.Paths;

import documentGeneration.GeneratedDocument;
import org.apache.lucene.document.Document;
        import org.apache.lucene.document.Field;
        import org.apache.lucene.document.TextField;
        import org.apache.lucene.index.IndexWriter;
        import org.apache.lucene.index.IndexWriterConfig;
        import org.apache.lucene.index.IndexableField;

        import org.apache.lucene.store.FSDirectory;


public class TripleIndexer {

    private IndexWriter writer;

    /**
     * Sets up the IndexWriter with a Standard Config
     * @throws IOException
     */
    public TripleIndexer(String pathToIndex) throws IOException{
        // directory for the indexer
        FSDirectory indexDict = FSDirectory.open(Paths.get(pathToIndex));
        //create indexer with standard conifg
        writer = new IndexWriter(indexDict, new IndexWriterConfig());
    }

    /**
     * Adds the content and the entity to the index.
     * @param gendocument
     * @throws IOException
     */
    public void addDocumentToIndex(GeneratedDocument gendocument) throws IOException {
        //create document in lucene for the document
        Document document = new Document();
        //add field for the entity to the document
        IndexableField entity = new TextField("entity", gendocument.getEntity(), Field.Store.YES);
        document.add(entity);
        IndexableField uri = new TextField("uri", gendocument.getUri(), Field.Store.YES);
        document.add(uri);
        IndexableField content = new TextField("document",gendocument.getDocument(), Field.Store.YES);
        document.add(content);
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
