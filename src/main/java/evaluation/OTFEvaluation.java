package evaluation;

import search.OTFSearcher;
import org.aksw.qa.commons.load.Dataset;
import org.aksw.qa.commons.load.LoaderController;
import org.aksw.qa.commons.datastructure.IQuestion;
import org.aksw.qa.commons.measure.AnswerBasedEvaluation;

import java.io.*;
import java.util.List;
import java.util.Set;

public class OTFEvaluation {

    private static BufferedWriter writer;

    /**
     * This class starts the evaluation with QALD7. Uses OTF approach of document generation.
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

        String indexDir;
        String otfDir;
        String tdbDir;
        boolean onlySPO;
        String eva;
        if(args.length > 2){
            indexDir = args[0];
            otfDir = args[1];
            tdbDir = args[2];
            onlySPO = Integer.parseInt(args[3]) == 0 ? true : false;
            eva = args[4];

        }
        else{
            System.out.println("Usage:");
            System.out.println("1st argument: path to index\n" +
                    "2nd argument: path where the temporary index will be created\n" +
                    "3rd argument: path to tdb\n"+
                    "4th argument: 0 for Take only S P O, 1 for Take considering Pagerank");
            return;
        }


        //loading QALD7 train multilingual using NLIWOD qa commons
        List<IQuestion> questions = LoaderController.load(Dataset.QALD7_Train_Multilingual);
        //String indexDir = System.getProperty("user.dir")+"/../index";
        //String otfDir = System.getProperty("user.dir")+"/../otfindex";
        //String tdbDir = System.getProperty("user.dir") + "/../tdb";

        FileOutputStream out = new FileOutputStream(new File(eva));
        writer = new BufferedWriter(new OutputStreamWriter(out));


        //benchmark
        double fmeasure = 0;
        int questionCounter = 0;
        for(IQuestion q : questions)
        {
            //only use questions for resources for benchmarking
            if(q.getAnswerType().equals("resource") && !q.getAggregation() && !q.getHybrid())
            {
                // do some magic with indexer
                try {
                    OTFSearcher searcher;
                    try {
                        searcher = new OTFSearcher(indexDir, otfDir, tdbDir, onlySPO);
                    } catch (IOException e) {
                        System.err.println("Could not open index!");
                        return;
                    }
                    //keywords of question
                    List<String> keywords = q.getLanguageToKeywords().get("en");
                    System.out.println("Question: "+keywords);

                    //answers to that question
                    Set<String> answers = searcher.search(keywords.toArray(new String[keywords.size()]));
                    System.out.println("Answer: "+answers);
                    out(keywords, q.getGoldenAnswers(), answers);

                    //score answers
                    double afmeasure = AnswerBasedEvaluation.fMeasure(answers, q);
                    writer.write("afmeasure: " + afmeasure + "\n");
                    fmeasure += afmeasure;
                    questionCounter++;
                    try {
                        searcher.close();
                    } catch (IOException e) {
                        System.err.println("Error closing searcher!");
                    }
                }catch(IOException e) {
                    System.err.println("Error searching for question: " + q.getLanguageToQuestion().get("en"));
                }
            }
        }



        //calculate mean fmeasure and print it
        System.out.println("fMeasure: " + fmeasure/questionCounter);
        writer.write("fMeasure: " + fmeasure/questionCounter);
        writer.close();
    }

    /**
     * Print progress to file.
     * @param keywords
     * @param expected
     * @param got
     * @throws IOException
     */
    public static void out(List<String> keywords, Set<String> expected, Set<String> got) throws IOException {
        writer.write("--------------------\n");
        writer.write("Keywords:\n");
        for(String s : keywords){
            writer.write("\t"+s+"\n");
        }
        writer.write("Expected:\n");
        for(String s : expected){
            writer.write("\t"+s+"\n");
        }
        writer.write("Answers:\n");
        for(String s : got){
            writer.write("\t"+s+"\n");
        }
    }
}
