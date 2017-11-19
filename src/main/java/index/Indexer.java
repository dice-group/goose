package index;


import java.io.*;
import java.nio.file.Paths;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;

import org.apache.lucene.store.FSDirectory;


public class Indexer {

	private final String TITLEFIELD = "title";
	private final String CONTENTFIELD = "content";
	private IndexWriter writer;
	
	/**
	 * Sets up the IndexWriter with a Standard Config
	 * @throws IOException
	 */
	public Indexer(String pathToIndex) throws IOException{
		// directory for the indexer
		FSDirectory indexDict = FSDirectory.open(Paths.get(pathToIndex));
		//create indexer with standard conifg
		writer = new IndexWriter(indexDict, new IndexWriterConfig());
	}
	
	/**
	 * Adds the content to the index.
	 * @param f
	 * @throws IOException
	 */
	public void addDocumentToIndex(File f) throws IOException {
		//create document in lucene for the document
		Document document = new Document();
		//add content and title of thr entity to the document
		IndexableField content = new TextField("", new FileReader(f));
		document.add(content);
		System.out.println(document.get(""));
		//add document to index
		writer.addDocument(document);
	}

	/**
	 * Adds the document to the index. Uses FileInpuStream instead of FileReader.
	 * @param f
	 * @throws IOException
	 */
	public void addDocumentToIndexAsStream(File f) throws IOException {
		//read in file
		FileInputStream fis = new FileInputStream(f);
		String documentContent ="";
		int content;
		while((content = fis.read())!= -1){
			documentContent += (char) content;
		}
		//
		Document doc = new Document();
		doc.add(new TextField("", documentContent, Field.Store.YES));
		writer.addDocument(doc);
	}
	
	/**
	 * Closes the index.
	 * @throws IOException
	 */
	public void closeIndex() throws IOException {

		writer.close();
		
	}
}
;