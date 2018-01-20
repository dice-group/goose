package index;

import documentGeneration.GeneratedDocument;
import edu.stanford.nlp.ling.tokensregex.PhraseTable;
import org.aksw.triple2nl.nlp.stemming.PlingStemmer;
import org.apache.commons.lang.StringUtils;
import org.apache.jena.base.Sys;
import org.apache.lucene.analysis.core.SimpleAnalyzer;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.xml.builders.BooleanQueryBuilder;
import org.apache.lucene.search.*;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.openrdf.query.parser.QueryParser;


import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

public class TripleSearcher {

    private final int RESULTCOUNT = 1000;
    private final int SLOPFACTOR = 10000;
    private final HashMap<String, ArrayList<String>> synonymTable;
    private IndexSearcher searcher;
    private IndexReader reader;

    /**
     * Sets up the Searcher.
     * @throws IOException
     */
    public TripleSearcher(String pathToIndex, Directory indexDict) throws IOException {

        synonymTable = new HashMap<>();
        createSynonymTable();

        //open directory of the index
        if(indexDict == null){
            indexDict = FSDirectory.open(Paths.get(pathToIndex));
        }

        //create new indexsearcher for the index
        reader = DirectoryReader.open(indexDict);

        searcher = new IndexSearcher(reader);
    }



    public Set<String> searchInIndex(String [] keywords) throws IOException {
        //keyword search with one keyword doesn't make sense at all
        if(keywords.length == 1) return new TreeSet<>();

        replaceProblematicKeywords(keywords);

        Set<String> answers = new TreeSet<String>();

        BooleanQuery.Builder bb = new BooleanQuery.Builder();
        // for each keyword create own term query. Connect all subqueries via "AND"

        for(String key : keywords){
            String words [] = key.split(" ");
                PhraseQuery.Builder pb = new PhraseQuery.Builder();
                for(String word : words){
                    word = word.toLowerCase().trim();
                    Term t  = new Term("document", word);
                    pb.add(t);
                }
                bb.add(pb.build(), BooleanClause.Occur.MUST);
        }

        BooleanQuery bq = bb.build();
        System.out.println(bq.toString());
        TopDocs results = searcher.search(bq, RESULTCOUNT);
        // just return the entities that documents match the query
        System.out.println(results.totalHits);
        for (ScoreDoc doc : results.scoreDocs){
            Document resource = reader.document(doc.doc);
            String entity = resource.get("entity");

            postprocessing(doc, keywords, answers, resource);
        }

        if(answers.size() == 0){

            // try search with a subset therefore delete, one keyword

            for(int i = 0; i < keywords.length; i++){
                //skip excluding entities
                if(Character.isUpperCase(keywords[i].charAt(0)))
                    continue;

                ArrayList<String> newKeywords = new ArrayList<>();
                //copy all except i to  newKeywords
                for(int j = 0; j < keywords.length; j++){
                    if(j!= i)
                        newKeywords.add(keywords[j]);
                }
                String[] keys = new String[newKeywords.size()];
                for(int j = 0; j < keys.length; j++){
                    keys[j] = newKeywords.get(j);
                }
                Set<String> ans = searchInIndex(keys);
                if(ans.size() > 0){
                    return  ans;
                }
            }

        }

        return answers;
    }

    private void replaceProblematicKeywords(String[] keywords) {
        for(int i = 0; i < keywords.length; i++){
            if(keywords[i].equals("birthplace")){
                keywords[i] = "birth place";
            }
        }
    }

    private boolean resultContainsOneKeyWord(String[] keywords, String entity) {
        for(String word : keywords){
            if(entity.contains(word))
                return true;
        }
        return false;
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
            if(!keyword.contains(entity) && !entity.contains(keyword)){
                result.add(keyword);
            }
        }
        String [] res = new String[result.size()];
        for(int i = 0; i < result.size(); i++){
            res[i]= result.get(i);
        }
        return res;
    }


    /**
     * Postprocess the found document.
     * @param doc
     * @param keywords
     * @return
     */
    private void postprocessing(ScoreDoc doc, String[] keywords, Set<String> answers, Document resource) throws IOException {
        ArrayList<String> entities = new ArrayList<>();
        //extract all entites from keywords --> entities start with upper case letter
        for(String s : keywords){
            if(Character.isUpperCase(s.charAt(0)))
                entities.add(s);
        }

        //does document name contains an entity?
        String entity = reader.document(doc.doc).get("entity");

        if(documentContainsOneEntity(entity, entities)){
            //try if the answer is a literal or an entity <=> the subordinate clause that contains a keyword contains ""
            //split the document in sobordinate clauses
            String [] keysWithoutEntityName = stringMinus(keywords, entity);
            String [] subclauses = resource.get("document").split(",");
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
        }
        else { //test if one subclause contains all keywords
            String[] sub = resource.get("document").split(",");
            for (String clause : sub) {
                if (subclauseContainsAllKeyword(clause, keywords))
                    answers.add(resource.get("uri"));
            }
        }


    }

    private boolean documentContainsOneEntity(String name, ArrayList<String> entites){
        for(String e : entites){
            if(name.contains(e)){
                return true;
            }
        }
        return false;
    }

    public String [] getSynomyms(String word){
        ArrayList<String> tmp = synonymTable.get(word);
        if(tmp == null){
            String [] res = new String[1];
            res[0] = word;
            return res;
        }
        if(!tmp.contains(word))
            tmp.add(word);
        String [] synonyms = tmp.toArray(new String[tmp.size()]);
        return synonyms;
    }

    public Map<String, String> searchWith2Keywords(String keyword1, String keyword2){
        HashMap<String, String> results = new HashMap<>();

        keyword1 = keyword1.trim();
        keyword2 = keyword2.trim();

        String entityFromKeywords = Character.isUpperCase(keyword1.charAt(0)) ?  keyword1 : keyword2;
        String nonEntityFromKeywords = Character.isUpperCase(keyword1.charAt(0)) ?  keyword2 : keyword1;

        try{
            nonEntityFromKeywords = PlingStemmer.stem(nonEntityFromKeywords);
            String[] synonyms = getSynomyms(nonEntityFromKeywords);

            //build query
            BooleanQuery.Builder bqBuilder= new BooleanQuery.Builder();
            PhraseQuery.Builder pqBuilder1 = new PhraseQuery.Builder();
            BooleanQuery.Builder bqSubBuilder = new BooleanQuery.Builder();

            //add synonyms to the query
            for(String entityChunk : entityFromKeywords.split(" ")){
                Term t = new Term("document", entityChunk.toLowerCase());
                pqBuilder1.add(t);
            }

            Term[] synonymTerms = new Term[synonyms.length];
            for(int i = 0; i < synonymTerms.length; i++){
                PhraseQuery.Builder pqbuilder2 = new PhraseQuery.Builder();
                for (String nonEntityChunk : synonyms[i].split(" ")){
                    pqbuilder2.add(new Term("document", nonEntityChunk.trim().toLowerCase()));
                }
                bqSubBuilder.add(pqbuilder2.build(), BooleanClause.Occur.SHOULD);
            }

            bqBuilder.add(pqBuilder1.build(), BooleanClause.Occur.MUST);
            bqBuilder.add(bqSubBuilder.build(), BooleanClause.Occur.MUST);

            BooleanQuery query = bqBuilder.build();

            System.out.println(query.toString());

            // query index

            TopDocs matchingDocs = searcher.search(query, RESULTCOUNT);
            System.out.println(matchingDocs.totalHits);

            // post process answers

            for (ScoreDoc doc : matchingDocs.scoreDocs){
                String entityFromDocument = reader.document(doc.doc).get("entity");
                String uriFromDocument = reader.document(doc.doc).get("uri");
                String documentFromDocument = reader.document(doc.doc).get("document");

                //replace last and by ,
                int indexOfLastAnd = documentFromDocument.lastIndexOf(" and ");
                if(indexOfLastAnd>=0)
                    documentFromDocument = documentFromDocument.substring(0,indexOfLastAnd)+", "+
                        documentFromDocument.substring(indexOfLastAnd+5, documentFromDocument.length());


                //if entity name is corresponding to the found document try to extract teh righthandside of
                // a relation
                if(entityFromDocument.contains(entityFromKeywords) ||(entityFromKeywords.contains(entityFromDocument))){
                    // split document into chunks and search if the non entity keyword occurs in a chunk
                    String [] chunks = documentFromDocument.split(",|\\.");
                    for(String chunk : chunks){


                        if(chunkContainsNonEntityFromKeywords(chunk, synonyms)){
                            int firstUpperCaseLetter = -1;
                            char [] chars = chunk.toCharArray();
                            for(int i = 1; i < chars.length; i++){
                                if(StringUtils.isAllUpperCase(""+chars[i])){
                                    firstUpperCaseLetter = i;
                                    break;
                                }
                            }
                            //add URI to answers
                            if(firstUpperCaseLetter != -1) {
                                String extractedEntity =  chunk.substring(firstUpperCaseLetter);
                                results.put(GeneratedDocument.generateURIOutOfTriple2NLLabel(extractedEntity),extractedEntity);
                            }

                        }
                    }
                }
                //entity is not in the document title return
                else{
                    String [] chunks = documentFromDocument.split(",|\\.");
                    for(String chunk : chunks){
                        if(chunkContainsNonEntityFromKeywords(chunk, synonyms) && chunk.contains(entityFromKeywords))
                            results.put(uriFromDocument, entityFromDocument);
                    }
                }
            }
        } catch (IOException io){
            //if searcher throws io exception return an empty set
            return new HashMap<>();
        }
        System.out.println(results);

        return results;
    }

    private boolean chunkContainsNonEntityFromKeywords(String chunk, String[] synonyms) {
        for(String syn : synonyms) {
            if(chunk.contains(syn)) return true;
        }
        return false;
    }

    private void createSynonymTable() {
        ArrayList<String> tmp = new ArrayList<>();
        tmp.add("part of");
        synonymTable.put("borough", tmp);

        tmp = new ArrayList<>();
        tmp.add("spouse");
        synonymTable.put("wife", tmp);

        tmp = new ArrayList<>();
        tmp.add("spouse");
        synonymTable.put("husband", tmp);

        tmp = new ArrayList<>();
        tmp.add("spouse");
        synonymTable.put("married to", tmp);

        tmp = new ArrayList<>();
        tmp.add("spouse");
        synonymTable.put("married", tmp);

        tmp = new ArrayList<>();
        tmp.add("located");
        synonymTable.put("location", tmp);

        tmp = new ArrayList<>();
        tmp.add("parent");
        synonymTable.put("parents", tmp);

        tmp = new ArrayList<>();
        tmp.add("parent");
        synonymTable.put("father", tmp);

        tmp = new ArrayList<>();
        tmp.add("parent");
        synonymTable.put("mother", tmp);

        tmp = new ArrayList<>();
        tmp.add("doctoral advisor");
        synonymTable.put("doctoral supervisor", tmp);

        tmp = new ArrayList<>();
        tmp.add("architect");
        synonymTable.put("design", tmp);

        tmp = new ArrayList<>();
        tmp.add("battle");
        synonymTable.put("military conflict", tmp);

        tmp = new ArrayList<>();
        tmp.add("leader");
        synonymTable.put("king", tmp);

        tmp = new ArrayList<>();
        tmp.add("developer");
        synonymTable.put("devolopers", tmp);

        tmp = new ArrayList<>();
        tmp.add("musical");
        synonymTable.put("musicals", tmp);

        tmp = new ArrayList<>();
        tmp.add("host");
        synonymTable.put("presenter", tmp);

        tmp = new ArrayList<>();
        tmp.add("death cause");
        synonymTable.put("die", tmp);

        tmp = new ArrayList<>();
        tmp.add("child");
        synonymTable.put("daughter", tmp);

        tmp = new ArrayList<>();
        tmp.add("child");
        synonymTable.put("son", tmp);

        tmp = new ArrayList<>();
        tmp.add("birth place");
        synonymTable.put("born", tmp);

        tmp = new ArrayList<>();
        tmp.add("subdivision name");
        synonymTable.put("country", tmp);

        tmp = new ArrayList<>();
        tmp.add("product");
        synonymTable.put("produce", tmp);

        tmp = new ArrayList<>();
        tmp.add("hovercraft");
        synonymTable.put("hovercrafts", tmp);

        tmp = new ArrayList<>();
        tmp.add("source country");
        synonymTable.put("start", tmp);

        tmp = new ArrayList<>();
        tmp.add("director");
        synonymTable.put("direct", tmp);

        tmp = new ArrayList<>();
        tmp.add("leader");
        synonymTable.put("president", tmp);

        tmp = new ArrayList<>();
        tmp.add("author");
        synonymTable.put("created", tmp);

        tmp = new ArrayList<>();
        tmp.add("target airport");
        tmp.add("source airport");
        synonymTable.put("airports", tmp);

        tmp = new ArrayList<>();
        tmp.add("movie");
        tmp.add("film");
        tmp.add("television");
        synonymTable.put("movies", tmp);

        tmp = new ArrayList<>();
        tmp.add("nick");
        tmp.add("nickname");
        synonymTable.put("called", tmp);

        tmp = new ArrayList<>();
        tmp.add("publisher");
        synonymTable.put("publish", tmp);

        tmp = new ArrayList<>();
        tmp.add("founded place");
        synonymTable.put("founded", tmp);

        tmp = new ArrayList<>();
        tmp.add("portrayer");
        tmp.add("starring");
        tmp.add("played");
        synonymTable.put("play", tmp);

        tmp = new ArrayList<>();
        tmp.add("abbreviation");
        synonymTable.put("stand for", tmp);

        tmp = new ArrayList<>();
        tmp.add("headquarter");
        synonymTable.put("headquarters", tmp);

        tmp = new ArrayList<>();
        tmp.add("location");
        synonymTable.put("city", tmp);

        tmp = new ArrayList<>();
        tmp.add("ingedient");
        synonymTable.put("ingedients", tmp);

        tmp = new ArrayList<>();
        tmp.add("writer");
        synonymTable.put("write", tmp);

        tmp = new ArrayList<>();
        tmp.add("creator");
        synonymTable.put("create", tmp);

        tmp = new ArrayList<>();
        tmp.add("crosses");
        synonymTable.put("cross", tmp);

        tmp = new ArrayList<>();
        tmp.add("author");
        synonymTable.put("wrote", tmp);

        tmp = new ArrayList<>();
        tmp.add("painter");
        tmp.add("author");
        synonymTable.put("paint", tmp);

        tmp = new ArrayList<>();
        tmp.add("route end");
        synonymTable.put("ends", tmp);

        tmp = new ArrayList<>();
        tmp.add("presenter");
        synonymTable.put("host", tmp);

        tmp = new ArrayList<>();
        tmp.add("birth place");
        synonymTable.put("birthplace", tmp);
    }
}

