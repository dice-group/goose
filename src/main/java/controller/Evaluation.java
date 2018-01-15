package controller;

import org.aksw.qa.commons.load.Dataset;
import org.aksw.qa.commons.load.LoaderController;
import org.aksw.qa.commons.datastructure.IQuestion;
import org.aksw.qa.commons.measure.AnswerBasedEvaluation;
import index.Searcher;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class Evaluation {

	private static final boolean OTFMode = true;

	public static void main(String[] args) {


		// Laden von QALD 7 train multilingual mittels NLIWOD/qa.commons
		// https://github.com/dice-group/NLIWOD/tree/master/qa.commons
		// https://github.com/dice-group/NLIWOD/blob/master/qa.commons/src/test/java/org/aksw/qa/commons/load/LoadTest.java
		List<IQuestion> questions = LoaderController.load(Dataset.QALD7_Train_Multilingual);
		String indexDir = System.getProperty("user.dir")+"/../index"; //path to the index directory
		Searcher searcher;
		try {
			searcher = new Searcher(indexDir);
		} catch (IOException e) {
			System.err.println("Could not open index!");
			return;
		}

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
					Set<String> answers = searcher.search((String[])q.getLanguageToKeywords().get("en").toArray());
					fmeasure += AnswerBasedEvaluation.fMeasure(answers, q);
					questionCounter++;
				}catch(IOException e) {
					System.err.println("Error searching for question: " + q.getLanguageToQuestion().get("en"));
				}
			}
		}
		
		//EVALUIEREMN
		// https://github.com/dice-group/NLIWOD/blob/master/qa.commons/src/test/java/org/aksw/qa/commons/measure/AnswerBasedEvaluationTest.java
//System.out.println(fmeasure/N);
		try {
			searcher.closeSearcher();
		} catch (IOException e) {
			System.err.println("Error closing searcher!");
		}
		System.out.println("fMeasure: " + fmeasure/questionCounter);
	}


}
