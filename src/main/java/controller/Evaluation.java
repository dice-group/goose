package controller;

import org.aksw.qa.commons.load.Dataset;
import org.aksw.qa.commons.load.LoaderController;
import org.aksw.qa.commons.datastructure.IQuestion;
import org.aksw.qa.commons.measure.AnswerBasedEvaluation;

import java.util.List;
import java.util.Set;

public class Evaluation {
	public static void main(String[] args) {
		// Laden von QALD 7 train multilingual mittels NLIWOD/qa.commons
		// https://github.com/dice-group/NLIWOD/tree/master/qa.commons
		// https://github.com/dice-group/NLIWOD/blob/master/qa.commons/src/test/java/org/aksw/qa/commons/load/LoadTest.java
		/*List<IQuestion> questions = LoaderController.load(Dataset.QALD7_Train_Multilingual);

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
				Set<String> answers = null;// do some magic with indexer and keywords from q.getLanguageToKeywords().get("en");
				fmeasure += AnswerBasedEvaluation.fMeasure(answers, q);
				questionCounter++;
			}
		}
		
		//EVALUIEREMN
		// https://github.com/dice-group/NLIWOD/blob/master/qa.commons/src/test/java/org/aksw/qa/commons/measure/AnswerBasedEvaluationTest.java
//System.out.println(fmeasure/N);
		System.out.println("fMeasure: " + fmeasure/questionCounter);*/
	}
}
