package controller;

import documentGeneration.AbstractDocumentGenerator;
import documentGeneration.takeOnlySPO.TakeOnlySPO;
import index.OTFSearcher;
import org.aksw.qa.commons.load.Dataset;
import org.aksw.qa.commons.load.LoaderController;
import org.aksw.qa.commons.datastructure.IQuestion;
import org.aksw.qa.commons.measure.AnswerBasedEvaluation;
import index.Searcher;
import org.apache.jena.base.Sys;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class OTFEvaluation {


    public static void main(String[] args) {


        // Laden von QALD 7 train multilingual mittels NLIWOD/qa.commons
        // https://github.com/dice-group/NLIWOD/tree/master/qa.commons
        // https://github.com/dice-group/NLIWOD/blob/master/qa.commons/src/test/java/org/aksw/qa/commons/load/LoadTest.java
        List<IQuestion> questions = LoaderController.load(Dataset.QALD7_Train_Multilingual);
        String indexDir = System.getProperty("user.dir")+"/../index";
        String otfDir = System.getProperty("user.dir")+"/../otfindex";
        String tdbDir = System.getProperty("user.dir")+"/../tdb";


        //douzble fmeasure= 0
        //for alle Fragen q
        //if(q . get Answertrype = true, q.getaggregate = false, q.hybrid =false)
        // Set<string> answer =  im Index eine Suche mit String []= q.getKeywords();
        // fmeasure += fmeasure (answer, q.goldanswer)
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
                        searcher = new OTFSearcher(indexDir, otfDir, tdbDir,  new TakeOnlySPO());
                    } catch (IOException e) {
                        System.err.println("Could not open index!");
                        return;
                    }
                    List<String> keywords = q.getLanguageToKeywords().get("en");
                    System.out.println("Question: "+keywords);

                    Set<String> answers = searcher.search((String [])keywords.toArray());
                    System.out.println("Answer: "+answers);
                    fmeasure += AnswerBasedEvaluation.fMeasure(answers, q);
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

        //EVALUIEREMN
        // https://github.com/dice-group/NLIWOD/blob/master/qa.commons/src/test/java/org/aksw/qa/commons/measure/AnswerBasedEvaluationTest.java
//System.out.println(fmeasure/N);

        System.out.println("fMeasure: " + fmeasure/questionCounter);
    }
}
