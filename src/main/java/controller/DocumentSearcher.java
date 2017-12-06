package controller;

import index.Searcher;
import index.TripleSearcher;
import org.apache.jena.base.Sys;

import java.io.IOException;
import java.util.Set;

public class DocumentSearcher {
    public static void main(String[] args){
        TripleSearcher searcher;
        String indexDir = System.getProperty("user.dir")+"/index";
        try{
            searcher = new TripleSearcher(indexDir);
            Set<String> results = searcher.searchInIndex(args);
            printArray(args, "input:");
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
