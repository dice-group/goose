package controller;

import index.Searcher;
import index.TripleSearcher;
import org.apache.commons.lang.StringUtils;
import org.apache.jena.base.Sys;

import java.io.IOException;
import java.util.Set;

public class DocumentSearcher {
    public static void main(String[] args){
        TripleSearcher searcher;
        String indexDir = System.getProperty("user.dir")+"/../index";
        try{
            searcher = new TripleSearcher(indexDir);
            String input = "";
            for(String s : args){
                input += s +" ";
            }
            String keywords[] = input.split(",");
            for(int i = 0; i < keywords.length;i++){
                keywords[i] = keywords[i].trim();
            }
            Set<String> results = searcher.searchInIndex(keywords);
            printArray(keywords, "input:");
            printSet(results, "output:");
        } catch (IOException e) {
            System.err.println("Could not open index!");
            return;
        }
    }

    public static void printArray(String [] array, String title){
        System.out.println(title);
        for(String word :array){
            System.out.println(word);
        }
    }
    public static void printSet(Set<String> set, String title){
        System.out.println(title);
        for(String word : set){
            System.out.println(word);
        }
    }
}
