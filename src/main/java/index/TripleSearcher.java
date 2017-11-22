package index;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;

import org.apache.lucene.search.MultiPhraseQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;


import java.io.IOException;
import java.nio.file.Paths;
import java.util.Set;
import java.util.TreeSet;

public class TripleSearcher {

    private final int RESULTCOUNT = 1000;
    private final int SLOPFACTOR = 1000;
    private IndexSearcher searcher;
    private IndexReader reader;

    /**
     * Sets up the Searcher.
     * @throws IOException
     */
    public TripleSearcher(String pathToIndex) throws IOException {
        //open directory of the index
        FSDirectory indexDict = FSDirectory.open(Paths.get(pathToIndex));
        //create new indexsearcher for the index
        reader = DirectoryReader.open(indexDict);
        searcher = new IndexSearcher(reader);
    }

    public Set<String> searchInIndex(String [] keywords) throws IOException {
        Set<String> answers = new TreeSet<String>();

        //build query
        MultiPhraseQuery.Builder builder = new MultiPhraseQuery.Builder();
        //extract words from queryString and save them to a term
        for (int i = 0; i < keywords.length; i++) {
            //TODO find all synonyms
            //add the word and synonyms to the builder
            Term term = new Term("", keywords[i]);
            builder.add(term);
            builder.setSlop(SLOPFACTOR);
        }

        TopDocs results = searcher.search(builder.build(), RESULTCOUNT);

        // just return the entities that documents match the query

        for (ScoreDoc doc : results.scoreDocs){
            Document resorce = reader.document(doc.doc);
            answers.add(resorce.get("entity"));
        }

        return answers;
    }

}
