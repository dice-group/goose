package index;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;

import org.apache.lucene.store.FSDirectory;


import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
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
            //lowercase all keywords
            keywords[i] = keywords[i].toLowerCase().trim();
            Term term = new Term("document", keywords[i]);
            builder.add(term);
            builder.setSlop(SLOPFACTOR);
        }

        TopDocs results = searcher.search(builder.build(), RESULTCOUNT);

        // just return the entities that documents match the query

        for (ScoreDoc doc : results.scoreDocs){
            Document resorce = reader.document(doc.doc);
            String entity = resorce.get("entity");
            if(subclauseContainsOneKeyword(entity, keywords)){
                //try if the answer is a literal or an entity <=> the subordinate clause that contains a keyword contains ""
                //split the document in sobordinate clauses
                String [] keysWithoutEntityName = stringMinus(keywords, entity);
                String [] subclauses = resorce.get("document").split(",");
                for(String subclause : subclauses){
                    if(subclauseContainsOneKeyword(subclause,keysWithoutEntityName)){
                        if(subclause.contains("\"")){
                            //extract literal
                            int start = StringUtils.ordinalIndexOf(subclause,"\"",1);
                            int end = StringUtils.ordinalIndexOf(subclause,"\"",2);
                            if(start == -1 || end == -1) continue;
                            answers.add(subclause.substring(start+1, end));
                        }else{
                            // extract entity
                            String sentence = subclause;
                            //delete all keywords
                            int firstUpperCaseLetter = -1;
                            char [] chars = sentence.toCharArray();
                            for(int i = 1; i < chars.length; i++){
                                if(StringUtils.isAllUpperCase(""+chars[i])){
                                    firstUpperCaseLetter = i;
                                    break;
                                }
                            }
                            answers.add(sentence.substring(firstUpperCaseLetter-1));
                        }



                    }
                }
            } else // just add the entity to the results
                answers.add(entity);
        }

        return answers;
    }

    private boolean keywordEqaulsResult(String [] keywords, String entity){
        for(String keyword : keywords){
            if(keyword.equals(entity)) return true;
        }
        return false;
    }

    private boolean subclauseContainsOneKeyword(String subclause, String [] keywords){
        for(String keyword : keywords){
            if(subclause.contains(keyword)) return true;
        }
        return false;
    }

    private String[] stringMinus(String [] keywords, String entity){
        entity = entity.toLowerCase();
        ArrayList<String> result = new ArrayList<>();
        for(String keyword : keywords){
            if(!entity.contains(keyword)){
                result.add(keyword);
            }
        }
        String [] res = new String[result.size()];
        for(int i = 0; i < result.size(); i++){
            res[i]= result.get(i);
        }
        return res;
    }

}
