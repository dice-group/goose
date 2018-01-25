package index;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import documentGeneration.GeneratedDocument;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
        import org.apache.lucene.document.Field;
        import org.apache.lucene.document.TextField;
        import org.apache.lucene.index.IndexWriter;
        import org.apache.lucene.index.IndexWriterConfig;
        import org.apache.lucene.index.IndexableField;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;


public class TripleIndexer {

    private IndexWriter writer;
    private Directory indexDict;

    /**
     * Sets up the IndexWriter with a Standard Config
     * @throws IOException
     */
    public TripleIndexer(String pathToIndex, boolean otfmode) throws IOException{
        // directory for the indexer
        Directory indexDict;
        if(otfmode){
            FileUtils.deleteDirectory(new File(pathToIndex));
            indexDict = new MMapDirectory(Paths.get(pathToIndex));
        }else{
           indexDict = FSDirectory.open(Paths.get(pathToIndex));
        }
        this.indexDict = indexDict;

        //create indexer with standard conifg
        IndexWriterConfig conf = new IndexWriterConfig(new SimpleAnalyzer());
        writer = new IndexWriter(indexDict, conf);
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

    /**
     * Returns the directory of the index.
     * @return
     */
    public Directory getIndexDict() {
        return indexDict;
    }
}
