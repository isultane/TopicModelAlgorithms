package models;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import util.FuncUtils;

/**
 * TO BE COMPLETED ...
 * @author Sultan Alqahtani
 *
 */

public class GibbsSamplingCTLDA
{
	public double alpha; // Hyper-parameter alpha
	public double beta; // Hyper-parameter alpha
	public int numTopics; // Number of topics
	public int numIterations; // Number of Gibbs sampling iterations
	public int topWords; // Number of most probable words for each topic
	public int numConcepts; // Number of concepts

	public double alphaSum; // alpha * numTopics
	public double betaSum; // beta * vocabularySize

	public List<List<Integer>> corpus; // Word ID-based corpus
	public List<List<Integer>> topicAssignments; // Topics assignments for words
													// in the corpus
		// Sultan added
		public List<TreeSet<String>> conceptWordsList;

	public int numDocuments; // Number of documents in the corpus
	public int numWordsInCorpus; // Number of words in the corpus

	public HashMap<String, Integer> word2IdVocabulary; // Vocabulary to get ID
														// given a word
	public HashMap<Integer, String> id2WordVocabulary; // Vocabulary to get word
														// given an ID
	public int vocabularySize; // The number of word types in the corpus

	// numDocuments * numTopics matrix
	// Given a document: number of its words assigned to each topic
	public int[][] docConceptTopicCount;
	// Number of words in every document
	public int[] sumDocConceptTopicCount;
		// Sultan added
	    // numDocuments * numTopics matrix
		// Given a document: number of its words assigned to each concept
		public int[][] docConceptCount;
		// Number of words in every document
		public int[] sumDocConceptCount;
		
	// numTopics * vocabularySize matrix
	// Given a topic: number of times a word type assigned to the topic
	public int[][] topicWordCount;
	// Total number of words assigned to a topic
	public int[] sumTopicWordCount;
		// Sultan added
		// Given a topic: number of times a word type assigned to the concept
		public int[][] conceptWordCount;
		// Total number of words assigned to a concept
		public int[] sumConceptWordCount;

	// Double array used to sample a topic
	public double[] multiPros;

	// Path to the directory containing the corpus
	public String folderPath;
	// Path to the topic modeling corpus
	public String corpusPath;
		// Sultan added
		// Path to the concepts seed-words
		public String conceptFilePrefix = "seed-";

	public String expName = "LDAmodel";
	public String orgExpName = "LDAmodel";
	public String tAssignsFilePath = "";
	public int savestep = 0;


	public GibbsSamplingCTLDA(String pathToCorpus, int inNumTopics, int inNumConcepts,
		double inAlpha, double inBeta, int inNumIterations, int inTopWords,
		String inExpName)
		throws Exception
	{

		alpha = inAlpha;
		beta = inBeta;
		numTopics = inNumTopics;
		numIterations = inNumIterations;
		topWords = inTopWords;
		expName = inExpName;
		orgExpName = expName;
		corpusPath = pathToCorpus;
		folderPath = pathToCorpus.substring(
			0,
			Math.max(pathToCorpus.lastIndexOf("/"),
				pathToCorpus.lastIndexOf("\\")) + 1);
	
		numConcepts = inNumConcepts;
		System.out.println("Reading topic and concept modeling corpus: " + pathToCorpus);

		word2IdVocabulary = new HashMap<String, Integer>();
		id2WordVocabulary = new HashMap<Integer, String>();
		corpus = new ArrayList<List<Integer>>();
		numDocuments = 0;
		numWordsInCorpus = 0;

		/**
		 * preparing and reading tags seed-words from files
		 */
		ArrayList<TreeSet<String>> conceptWordsStrList = new ArrayList<TreeSet<String>>();
		for (int t = 0; t < numConcepts; t++) {
			String dicFilePath = folderPath + conceptFilePrefix+t+".txt"; 
			if (new File(dicFilePath).exists()) {
				conceptWordsStrList.add(makeSetOfWordsFromFile(dicFilePath));
			}
			
			System.out.print("ConceptWords-"+t+": ");
			for (String tagWord : conceptWordsStrList.get(conceptWordsStrList.size()-1)) {
				System.out.print(tagWord+" ");
			}
			System.out.println();
		}
		
		
		/**
		 * reading corpus data
		 * */
		BufferedReader br = null;
		try {
			int indexWord = -1;
			br = new BufferedReader(new FileReader(pathToCorpus));
			for (String doc; (doc = br.readLine()) != null;) {

				if (doc.trim().length() == 0)
					continue;

				String[] words = doc.trim().split("\\s+");
				List<Integer> document = new ArrayList<Integer>();

				for (String word : words) {
					if (word2IdVocabulary.containsKey(word)) {
						document.add(word2IdVocabulary.get(word));
					}
					else {
						indexWord += 1;
						word2IdVocabulary.put(word, indexWord);
						id2WordVocabulary.put(indexWord, word);
						document.add(indexWord);
					}
				}

				numDocuments++;
				numWordsInCorpus += document.size();
				corpus.add(document);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		/**
		 * Adding tag's seed-words into tagWordList
		 */
		conceptWordsList = new ArrayList<TreeSet<String>>(conceptWordsStrList.size());
		for (Set<String> tagWordsStr : conceptWordsStrList) {
			TreeSet<String> tagWords = new TreeSet<String>();
			for (String word : tagWordsStr)
				tagWords.add(word); 
			conceptWordsList.add(tagWords);
		}
		vocabularySize = word2IdVocabulary.size(); // vocabularySize = indexWord
		// Topics Matrix allocation
		docConceptTopicCount = new int[numDocuments][numTopics];
		topicWordCount = new int[numTopics][vocabularySize];
		sumDocConceptTopicCount = new int[numDocuments];
		sumTopicWordCount = new int[numTopics];
		// Concepts Matrix allocation
		docConceptCount = new int[numDocuments][numConcepts];
		conceptWordCount = new int[numConcepts][vocabularySize];
		sumDocConceptCount = new int[numConcepts];
		sumConceptWordCount = new int[numConcepts];

		multiPros = new double[numTopics+numConcepts];
		for (int i = 0; i < numTopics+numConcepts; i++) {
			multiPros[i] = 1.0 / numTopics;
		}

		alphaSum = numTopics * alpha;
		betaSum = vocabularySize * beta;

		System.out.println("Corpus size: " + numDocuments + " docs, "
			+ numWordsInCorpus + " words");
		System.out.println("Vocabuary size: " + vocabularySize);
		System.out.println("Number of topics: " + numTopics);
		System.out.println("alpha: " + alpha);
		System.out.println("beta: " + beta);
		System.out.println("Number of sampling iterations: " + numIterations);
		System.out.println("Number of top topical words: " + topWords);

		initialize();
	}

	/**
	 * 
	 * @param path
	 * @return words
	 * @throws Exception
	 */
	public static TreeSet<String> makeSetOfWordsFromFile(String path) throws Exception {
		TreeSet<String> words = new TreeSet<String>();
		String line;
		BufferedReader reader = new BufferedReader(new FileReader(new File(path)));
		while ((line = reader.readLine()) != null) {
			words.add(line);
		}
		return words;
	}
	/**
	 * Randomly initialize topic assignments
	 */
	public void initialize()
		throws IOException
	{
		System.out.println("Randomly initializing topic assignments ...");

		topicAssignments = new ArrayList<List<Integer>>();

		for (int i = 0; i < numDocuments; i++) {
			List<Integer> topics = new ArrayList<Integer>();
			int docSize = corpus.get(i).size();
			for (int j = 0; j < docSize; j++) {
				int wordId = corpus.get(i).get(j);
                int subtopic = FuncUtils.nextDiscrete(multiPros);
                int topic = subtopic % numTopics;
                if (topic == subtopic) {// Generated from the Dirichlet multinomial component
                	topicWordCount[topic][wordId] += 1;
                    sumTopicWordCount[topic] += 1;
                }
                else { // Generated from the latent concept component 
					for (int cIndex = 0; cIndex < numConcepts; cIndex++) {
						if (conceptWordsList.get(cIndex).contains(
								id2WordVocabulary.get(wordId))) {
							conceptWordCount[topic][wordId] += 1;
							sumConceptWordCount[topic] += 1;
						}
					}
                }
                docConceptTopicCount[i][topic] += 1;
                sumDocConceptTopicCount[i] += 1;
                topics.add(subtopic);
			}
			topicAssignments.add(topics);
		}
	}

	
	public void inference()
		throws IOException
	{
		System.out.println("Running Gibbs sampling inference: ");

		for (int iter = 1; iter <= numIterations; iter++) {

			System.out.println("\tSampling iteration: " + (iter));
//			if((iter%10) == 0)
//				System.out.println(computePerplexity());

			sampleInSingleIteration();

			if ((savestep > 0) && (iter % savestep == 0)
				&& (iter < numIterations)) {
				System.out.println("\t\tSaving the output from the " + iter
					+ "^{th} sample");
				expName = orgExpName + "-" + iter;
				write();
			}
		}
		expName = orgExpName;

		writeParameters();
		System.out.println("Writing output from the last sample ...");
		write();

		System.out.println("Sampling completed!");

	}

	public void sampleInSingleIteration()
	{
		for (int dIndex = 0; dIndex < numDocuments; dIndex++) {
			int docSize = corpus.get(dIndex).size();
			for (int wIndex = 0; wIndex < docSize; wIndex++) {
				// Get current word and its topic
				int subtopic = topicAssignments.get(dIndex).get(wIndex);
				int word = corpus.get(dIndex).get(wIndex);
				int topic = subtopic%numTopics;
				// Decrease counts
				docConceptTopicCount[dIndex][topic] -= 1;
				sumDocConceptTopicCount[dIndex] -= 1;
				if(topic == subtopic){
					topicWordCount[topic][word] -= 1;
					sumTopicWordCount[topic] -= 1;	
				}else{
					for (int cIndex = 0; cIndex < numConcepts; cIndex++) {
						if (conceptWordsList.get(cIndex).contains(
								id2WordVocabulary.get(word))) {
							conceptWordCount[topic][word] -= 1;
							sumConceptWordCount[topic] -= 1;
						}
					}	
					for (int cIndex = 0; cIndex < numConcepts; cIndex++) {
						double prob = (docConceptTopicCount[dIndex][cIndex] + alpha);
						if (conceptWordsList.get(cIndex).contains(
								id2WordVocabulary.get(word))) {
							multiPros[cIndex+numTopics] = prob *((conceptWordCount[topic][word] + beta) / (sumConceptWordCount[topic] + (beta * conceptWordsList
											.get(cIndex).size())));
						}else{
							multiPros[cIndex+numTopics] = prob * 0.0;
						}
					}
				}
				
				for (int tIndex = 0; tIndex < numTopics; tIndex++) {
					multiPros[tIndex] = (docConceptTopicCount[dIndex][tIndex] + alpha)
							* ((topicWordCount[tIndex][word] + beta) / (sumTopicWordCount[tIndex] + betaSum));
				}

				subtopic = FuncUtils.nextDiscrete(multiPros);
				topic = subtopic % numTopics;

				// Decrease counts
				docConceptTopicCount[dIndex][topic] += 1;
				sumDocConceptTopicCount[dIndex] += 1;
				if(topic == subtopic){
					topicWordCount[topic][word] += 1;
					sumTopicWordCount[topic] += 1;
				}else{
					for (int cIndex = 0; cIndex < numConcepts; cIndex++) {
						if (conceptWordsList.get(cIndex).contains(id2WordVocabulary.get(word))) {
							conceptWordCount[topic][word] += 1;
							sumConceptWordCount[topic] += 1;
						}
					}
				}
				// Update topic assignments
				topicAssignments.get(dIndex).set(wIndex, subtopic);
			}
		}
	}

//	public double computePerplexity() {
//		double logliCorpus = 0.0;
//		for (int dIndex = 0; dIndex < numDocuments; dIndex++) {
//			int docSize = corpus.get(dIndex).size();
//			double logliDoc = 0.0;
//			for (int wIndex = 0; wIndex < docSize; wIndex++) {
//				int word = corpus.get(dIndex).get(wIndex);
//				double likeWord = 0.0;
//				for (int tIndex = 0; tIndex < numTopics; tIndex++) {
//					likeWord += ((docTopicCountLDA[dIndex][tIndex] + alpha) / (sumDocTopicCountLDA[dIndex] + alphaSum))
//							* ((topicWordCountLDA[tIndex][word] + beta) / (sumTopicWordCountLDA[tIndex] + betaSum));
//				}
//				logliDoc += Math.log(likeWord);
//			}
//			logliCorpus += logliDoc;
//		}
//		double perplexity = Math.exp(-1.0 * logliCorpus / numWordsInCorpus);
//		if (perplexity < 0)
//			throw new RuntimeException("Illegal perplexity value: "
//					+ perplexity);
//		return perplexity;
//	}

	public void writeParameters()
		throws IOException
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
			+ expName + ".paras"));
		writer.write("-model" + "\t" + "LDA");
		writer.write("\n-corpus" + "\t" + corpusPath);
		writer.write("\n-ntopics" + "\t" + numTopics);
		writer.write("\n-alpha" + "\t" + alpha);
		writer.write("\n-beta" + "\t" + beta);
		writer.write("\n-niters" + "\t" + numIterations);
		writer.write("\n-twords" + "\t" + topWords);
		writer.write("\n-name" + "\t" + expName);
		if (tAssignsFilePath.length() > 0)
			writer.write("\n-initFile" + "\t" + tAssignsFilePath);
		if (savestep > 0)
			writer.write("\n-sstep" + "\t" + savestep);

		writer.close();
	}

	public void writeDictionary()
		throws IOException
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
			+ expName + ".vocabulary"));
		for (String word : word2IdVocabulary.keySet()) {
			writer.write(word + " " + word2IdVocabulary.get(word) + "\n");
		}
		writer.close();
	}

	public void writeIDbasedCorpus()
		throws IOException
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
			+ expName + ".IDcorpus"));
		for (int dIndex = 0; dIndex < numDocuments; dIndex++) {
			int docSize = corpus.get(dIndex).size();
			for (int wIndex = 0; wIndex < docSize; wIndex++) {
				writer.write(corpus.get(dIndex).get(wIndex) + " ");
			}
			writer.write("\n");
		}
		writer.close();
	}

	public void writeTopicAssignments()
		throws IOException
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
			+ expName + ".topicAssignments"));
		for (int dIndex = 0; dIndex < numDocuments; dIndex++) {
			int docSize = corpus.get(dIndex).size();
			for (int wIndex = 0; wIndex < docSize; wIndex++) {
				writer.write(topicAssignments.get(dIndex).get(wIndex) + " ");
			}
			writer.write("\n");
		}
		writer.close();
	}

	public void writeTopTopicConceptWords()
		throws IOException
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
			+ expName + ".topWords"));

		// Write top topical words 
		for (int tIndex = 0; tIndex < numTopics; tIndex++) {
			writer.write("Topic" + new Integer(tIndex) + ":");

			Map<Integer, Integer> wordCount = new TreeMap<Integer, Integer>();
			for (int wIndex = 0; wIndex < vocabularySize; wIndex++) {
				wordCount.put(wIndex, topicWordCount[tIndex][wIndex]);
			}
			wordCount = FuncUtils.sortByValueDescending(wordCount);

			Set<Integer> mostLikelyWords = wordCount.keySet();
			int count = 0;
			for (Integer index : mostLikelyWords) {
				if (count < topWords) {
					writer.write(" " + id2WordVocabulary.get(index));
					count += 1;
				}
				else {
					writer.write("\n");
					break;
				}
			}
		}
		for (int cIndex = 0; cIndex < numConcepts; cIndex++) {
			writer.write("Concept" + new Integer(cIndex) + ":");

			Map<Integer, Integer> wordCount = new TreeMap<Integer, Integer>();
			for (int wIndex = 0; wIndex < vocabularySize; wIndex++) {
				wordCount.put(wIndex, conceptWordCount[cIndex][wIndex]);
			}
			wordCount = FuncUtils.sortByValueDescending(wordCount);

			Set<Integer> mostLikelyWords = wordCount.keySet();
			int count = 0;
			for (Integer index : mostLikelyWords) {
				if (count < topWords) {
					writer.write(" " + id2WordVocabulary.get(index));
					count += 1;
				}
				else {
					writer.write("\n");
					break;
				}
			}
		}
		writer.close();
	}

	public void writeTopicWordPros()
		throws IOException
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
			+ expName + ".phi"));
		for (int i = 0; i < numTopics; i++) {
			for (int j = 0; j < vocabularySize; j++) {
				double pro = (topicWordCount[i][j] + beta)
					/ (sumTopicWordCount[i] + betaSum);
				writer.write(pro + " ");
			}
			writer.write("\n");
		}
		writer.close();
	}

	public void writeTopicWordCount()
		throws IOException
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
			+ expName + ".WTcount"));
		for (int i = 0; i < numTopics; i++) {
			for (int j = 0; j < vocabularySize; j++) {
				writer.write(topicWordCount[i][j] + " ");
			}
			writer.write("\n");
		}
		writer.close();

	}

	public void writeDocTopicPros()
		throws IOException
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
			+ expName + ".theta"));
		for (int i = 0; i < numDocuments; i++) {
			for (int j = 0; j < numTopics; j++) {
				double pro = (docConceptTopicCount[i][j] + alpha)
					/ (sumDocConceptTopicCount[i] + alphaSum);
				writer.write(pro + " ");
			}
			writer.write("\n");
		}
		writer.close();
	}

	public void writeDocTopicCount()
		throws IOException
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
			+ expName + ".DTcount"));
		for (int i = 0; i < numDocuments; i++) {
			for (int j = 0; j < numTopics; j++) {
				writer.write(docConceptTopicCount[i][j] + " ");
			}
			writer.write("\n");
		}
		writer.close();
	}

	public void write()
		throws IOException
	{
		writeDictionary();
		writeTopTopicConceptWords();
		writeDocTopicPros();
		writeTopicAssignments();
		writeTopicWordPros();
	}

	public static void main(String args[])
		throws Exception
	{
		String pathToCorpus = "data/corpus1.txt";
		double beta  = 0.01;
		double alpha = 0.1;
		int iteration = 2000;
		int topWords = 100;
		int numConcepts = 3;
		int numTopics = 20;
		GibbsSamplingCTLDA ctlda = new GibbsSamplingCTLDA(pathToCorpus, numTopics, numConcepts, alpha,
				beta, iteration, topWords, "testCTLDA");
		ctlda.inference();
	}
}