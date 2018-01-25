package search;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Set;
import java.util.TreeSet;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;

public class Searcher {

    private final int RESULTCOUNT = 1000;
    private final int SLOPFACOTR = 15;
    private IndexSearcher searcher;
    private IndexReader reader;

    /**
     * Sets up the Searcher.
     * @throws IOException
     */
    public Searcher(String pathToIndex) throws IOException {
        //open directory of the index
        FSDirectory indexDict = FSDirectory.open(Paths.get(pathToIndex));
        //create new indexsearcher for the index
        reader = DirectoryReader.open(indexDict);
        searcher = new IndexSearcher(reader);
    }

    /**
     * Searches exact matches for the words in queryString. Returns the best 10 matches.
     * @param queryString
     * @param results
     * @return
     * @throws IOException
     */
    private TopDocs search(String [] queryString, int results, int slopFactor) throws IOException {
        //use a builder to create the query
        MultiPhraseQuery.Builder builder = new MultiPhraseQuery.Builder();
        //extract words from queryString and save them to a term
        for (int i = 0; i < queryString.length; i++) {
            //TODO find all synonyms
            //add the word and synonyms to the builder
            Term term = new Term("", queryString[i]);
            builder.add(term);
            builder.setSlop(slopFactor);
        }

        //build multiphrase query
        MultiPhraseQuery query = builder.build();
        return searcher.search(query, results);
    }

    /**
     * Searches exact matches for the keywords. Returns content of all documents that matches.
     * @param keyWords
     * @return
     * @throws IOException
     */
    private String[] exactSearch(String [] keyWords) throws IOException {
        TopDocs results = search(keyWords, RESULTCOUNT,0);
        ScoreDoc[] docs = results.scoreDocs;
        String[] lines = new String[docs.length];
        for (int i = 0; i < docs.length; i++) {
            lines[i] = searcher.doc(docs[i].doc).get("");
        }
        return lines;
    }

    /**
     * Searches for matches of the keywords. Start with exact match. When there is no match the search
     * will get sloppy.
     * @param keyWords
     * @return
     * @throws IOException
     */
    private Set<String> sloppySearch(String keyWords [], int upperSlopBound) throws IOException{
        Set<String> answers = new TreeSet<>();
        for(int slop = 0; slop < upperSlopBound; slop++){
            TopDocs results = search(keyWords, RESULTCOUNT,slop);
            if(results.totalHits == 0) continue;
            ScoreDoc[] docs = results.scoreDocs;
            String[] lines = new String[docs.length];
            for (int i = 0; i < docs.length; i++) {
                answers.add(searcher.doc(docs[i].doc).get(""));
            }
        }
        return answers;
    }

    /**
     * Searches for matches of the keywords in the index. Returns the lines that are found.
     * @param keyWords
     * @return
     * @throws IOException
     */
    public Set<String> search(String [] keyWords) throws IOException {
        return sloppySearch(keyWords, SLOPFACOTR);
    }

    /**
     * Closes the IndexSearcher.
     * @throws IOException
     */
    public void closeSearcher() throws IOException {
        reader.close();
    }

}
