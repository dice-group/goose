package index;

import documentGeneration.GeneratedDocument;
import edu.stanford.nlp.ling.tokensregex.PhraseTable;
import org.apache.commons.lang.StringUtils;
import org.apache.jena.base.Sys;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.xml.builders.BooleanQueryBuilder;
import org.apache.lucene.search.*;

import org.apache.lucene.store.FSDirectory;
import org.openrdf.query.parser.QueryParser;


import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

public class TripleSearcher {

    private final int RESULTCOUNT = 1000;
    private final int SLOPFACTOR = 10000;
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

        BooleanQuery.Builder bb = new BooleanQuery.Builder();
        // for each keyword create own term query. Connect all subqueries via "AND"

        for(String key : keywords){
            String words [] = key.split(" ");
                PhraseQuery.Builder pb = new PhraseQuery.Builder();
                for(String word : words){
                    System.out.println(word);
                    word = word.toLowerCase().trim();
                    Term t  = new Term("document", word);
                    pb.add(t);
                }
                bb.add(pb.build(), BooleanClause.Occur.MUST);
        }

        BooleanQuery bq = bb.build();
        TopDocs results = searcher.search(bq, RESULTCOUNT);
        // just return the entities that documents match the query
        System.out.println(results.totalHits);
        for (ScoreDoc doc : results.scoreDocs){
            Document resorce = reader.document(doc.doc);
            String entity = resorce.get("entity");
            if(keywordEqaulsResult(keywords, entity)){
                //try if the answer is a literal or an entity <=> the subordinate clause that contains a keyword contains ""
                //split the document in sobordinate clauses
                String [] keysWithoutEntityName = stringMinus(keywords, entity);
                String [] subclauses = resorce.get("document").split(",");
                for(String subclause : subclauses){
                    if(subclauseContainsAllKeyword(subclause, keysWithoutEntityName)){
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
                            //add URI to answers
                            if(firstUpperCaseLetter != -1) {
                                answers.add(GeneratedDocument.generateURIOutOfTriple2NLLabel(sentence.substring(firstUpperCaseLetter)));
                            }

                        }



                    }
                }
            } else { //test if one subclause contains all keywords
                String [] sub = resorce.get("document").split(",");
                for(String clause : sub){
                    if(subclauseContainsAllKeyword(clause, keywords))
                        answers.add(resorce.get("uri"));
                }

            }

        }

        return answers;
    }

    private boolean keywordEqaulsResult(String [] keywords, String entity){
        for(String keyword : keywords){
            if(entity.equals(keyword)) return true;
        }
        return false;
    }

    private boolean subclauseContainsAllKeyword(String subclause, String [] keywords){
        for(String keyword : keywords){
            if(!subclause.contains(keyword)) return false;
        }
        return true;
    }

    private String[] stringMinus(String [] keywords, String entity){
        ArrayList<String> result = new ArrayList<>();
        for(String keyword : keywords){
            if(!keyword.contains(entity)){
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
