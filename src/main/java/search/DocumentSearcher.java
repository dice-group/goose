package search;

import search.OTFSearcher;
import search.TripleSearcher;

import java.io.IOException;
import java.util.Set;

/**
 * This class lets you search for keywords by hand. Uses OTF approach.
 */
public class DocumentSearcher {

    //flag to activate otf mode
    private static final boolean OTFMode = true;

    /**
     * Main method to search. With otf mode.
     * @param args
     */
    public static void main(String[] args){
        boolean onlySPO = true;
        String indexDir = "";
        String otfDir = "";
        String tdbDir = "";
        if(OTFMode){
            if(args.length > 2){
                indexDir = args[0];
                otfDir = args[1];
                tdbDir = args[2];
                onlySPO = Integer.parseInt(args[3]) == 0 ? true : false;
                }
            else{
                System.out.println("Usage:");
                System.out.println("1st argument: path to index\n" +
                        "2nd argument: path where the temporary index will be created\n" +
                        "3rd argument: path to tdb\n" +
                        "4th argument:  0 for Take only S P O, 1 for Take considering Pagerank"+
                        "other arguments: keywords of questions seperated by ,");
                return;
            }
            //String indexDir = System.getProperty("user.dir")+"/../index";
            //String otfDir = System.getProperty("user.dir")+"/../otfindex";
            //String tdbDir = System.getProperty("user.dir")+"/../tdb";
            try {
                OTFSearcher searcher = new OTFSearcher(indexDir, otfDir, tdbDir, onlySPO);
                String input = "";
                for(int i = 4; i < args.length; i++){
                    input += args[i] +" ";
                }
                String keywords[] = input.split(",");
                for(int i = 0; i < keywords.length;i++){
                    keywords[i] = keywords[i].trim();
                }
                Set<String> results = searcher.search(keywords);
                printArray(keywords, "input:");
                printSet(results, "output:");
            } catch (IOException e) {
                System.err.println("Could not open index!");
                return;
            }
        } else {
            // SEARCH ONLY POSSIBLE IN OTFMODE
            /**
            TripleSearcher searcher;
            String indexDir = System.getProperty("user.dir")+"/../index";
            try{
                searcher = new TripleSearcher(indexDir,null);
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
         **/
        }


    }

    /**
     * Prints array to standard out.
     * @param array
     * @param title
     */
    public static void printArray(String [] array, String title){
        System.out.println(title);
        for(String word :array){
            System.out.println(word);
        }
    }

    /**
     * Prints set to standard out.
     * @param set
     * @param title
     */
    public static void printSet(Set<String> set, String title){
        System.out.println(title);
        for(String word : set){
            System.out.println(word);
        }
    }
}
