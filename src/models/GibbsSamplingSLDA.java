package models;
/**
 * UNDER MAINTENANCE
 */
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import util.FuncUtils;

/**
 * SLDA: A Java package for the SLDA topic model
 * 
 * Implementation of the Sentence-LDA (SLDA), a probabilistic generative model
 * that assumes all words in a single sentence are generated from one aspect
 * sampling, as described in:
 * 
 * Jo, Yohan, and Alice H. Oh.
 * "Aspect and sentiment unification model for online review analysis."
 * Proceedings of the fourth ACM international conference on Web search and data
 * mining. ACM, 2011.
 * 
 * @author: Sultan Alqahtani
 */

public class GibbsSamplingSLDA
{
	public double alpha; // Hyper-parameter alpha
	public double beta; // Hyper-parameter alpha
	public int numTopics; // Number of topics
	public int numIterations; // Number of Gibbs sampling iterations
	public int topWords; // Number of most probable words for each topic

	public double alphaSum; // alpha * numTopics
	public double betaSum; // beta * vocabularySize

	public List<List<List<Integer>>> corpus; // Word ID-based corpus
	public List<List<Integer>> topicAssignments; // Topics assignments for sentences in the corpus
	public int numDocuments; // Number of documents in the corpus
		// Sultan
		public int numSentences; // Number of sentences in the corpus
	public int numWordsInCorpus; // Number of words in the corpus

	public HashMap<String, Integer> word2IdVocabulary; // Vocabulary to get ID
														// given a word
	public HashMap<Integer, String> id2WordVocabulary; // Vocabulary to get word
														// given an ID
	public TreeMap<String,Integer> wordCnt;
	public int vocabularySize; // The number of word types in the corpus

	// numDocuments * numTopics matrix
	// Given a document: number of its words assigned to each topic
	public int[][] docTopicCount;
	// Number of words in every document
	public int[] sumDocTopicCount;
	// numTopics * vocabularySize matrix
	// Given a topic: number of times a word type assigned to the topic
	public int[][] topicWordCount;
	// Total number of words assigned to a topic
	public int[] sumTopicWordCount;

	// Double array used to sample a topic
	public double[] multiPros;

	// Path to the directory containing the corpus
	public String folderPath;
	// Path to the topic modeling corpus
	public String corpusPath;

	public String expName = "LDAmodel";
	public String orgExpName = "LDAmodel";
	public String tAssignsFilePath = "";
	public int savestep = 0;

	public GibbsSamplingSLDA(String pathToCorpus, int inNumTopics,
		double inAlpha, double inBeta, int inNumIterations, int inTopWords)
		throws Exception
	{
		this(pathToCorpus, inNumTopics, inAlpha, inBeta, inNumIterations,
			inTopWords, "LDAmodel");
	}

	public GibbsSamplingSLDA(String pathToCorpus, int inNumTopics,
		double inAlpha, double inBeta, int inNumIterations, int inTopWords,
		String inExpName)
		throws Exception
	{
		this(pathToCorpus, inNumTopics, inAlpha, inBeta, inNumIterations,
			inTopWords, inExpName, "", 0);
	}

	public GibbsSamplingSLDA(String pathToCorpus, int inNumTopics,
		double inAlpha, double inBeta, int inNumIterations, int inTopWords,
		String inExpName, String pathToTAfile)
		throws Exception
	{
		this(pathToCorpus, inNumTopics, inAlpha, inBeta, inNumIterations,
			inTopWords, inExpName, pathToTAfile, 0);
	}

	public GibbsSamplingSLDA(String pathToCorpus, int inNumTopics,
		double inAlpha, double inBeta, int inNumIterations, int inTopWords,
		String inExpName, int inSaveStep)
		throws Exception
	{
		this(pathToCorpus, inNumTopics, inAlpha, inBeta, inNumIterations,
			inTopWords, inExpName, "", inSaveStep);
	}

	public GibbsSamplingSLDA(String pathToCorpus, int inNumTopics,
		double inAlpha, double inBeta, int inNumIterations, int inTopWords,
		String inExpName, String pathToTAfile, int inSaveStep)
		throws Exception
	{

		alpha = inAlpha;
		beta = inBeta;
		numTopics = inNumTopics;
		numIterations = inNumIterations;
		topWords = inTopWords;
		savestep = inSaveStep;
		expName = inExpName;
		orgExpName = expName;
		corpusPath = pathToCorpus;
		folderPath = pathToCorpus.substring(
			0,
			Math.max(pathToCorpus.lastIndexOf("/"),
				pathToCorpus.lastIndexOf("\\")) + 1);
	//	folderPath = "test/";

		System.out.println("Reading topic modeling corpus: " + pathToCorpus);

		word2IdVocabulary = new HashMap<String, Integer>();
		id2WordVocabulary = new HashMap<Integer, String>();
			// Sultan added
			wordCnt = new TreeMap<String,Integer>();
		corpus = new ArrayList<List<List<Integer>>>();
		numDocuments = 0;
		numSentences = 0;
		numWordsInCorpus = 0;
	
		BufferedReader br = null;
		try {
			int indexSentence = -1;
			int indexWord = -1;
			br = new BufferedReader(new FileReader(pathToCorpus));
			for (String doc; (doc = br.readLine()) != null;) {

				if (doc.trim().length() == 0)
					continue;
				
				List<List<Integer>> document = new ArrayList<List<Integer>>();
				String [] sentenceStrs = doc.split("\t");
				for(String sentenceStr: sentenceStrs){
					ArrayList<Integer> sentence = new ArrayList<Integer>();
					indexSentence += 1;
					String[] words = sentenceStr.trim().split(" ");
					for (String word : words) {
						if (word2IdVocabulary.containsKey(word)) {
							sentence.add(word2IdVocabulary.get(word));
						}
						else {
							indexWord += 1;
							word2IdVocabulary.put(word, indexWord);
							id2WordVocabulary.put(indexWord, word);
							sentence.add(indexWord);
						}
					}
					document.add(sentence);
					numSentences++;
					numWordsInCorpus += sentence.size();
				}
				numDocuments++;
				corpus.add(document);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		vocabularySize = word2IdVocabulary.size(); // vocabularySize = indexWord
		docTopicCount = new int[numDocuments][numTopics];
		topicWordCount = new int[numTopics][vocabularySize];
		sumDocTopicCount = new int[numDocuments];
		sumTopicWordCount = new int[numTopics];

		multiPros = new double[numTopics];
		for (int i = 0; i < numTopics; i++) {
			multiPros[i] = 1.0 / numTopics;
		}

		alphaSum = numTopics * alpha;
		betaSum = vocabularySize * beta;

		System.out.println("Corpus size: " + numDocuments +"-"+corpus.size()+ " docs, "
				+ numSentences + " sentences, and " + numWordsInCorpus
				+ " words");
		System.out.println("Vocabuary size: " + vocabularySize);
		System.out.println("Number of topics: " + numTopics);
		System.out.println("alpha: " + alpha);
		System.out.println("beta: " + beta);
		System.out.println("Number of sampling iterations: " + numIterations);
		System.out.println("Number of top topical words: " + topWords);

		tAssignsFilePath = pathToTAfile;
	
			initialize();
	}
	/**
	 * Randomly initialize topic assignments
	 */
	public void initialize()
		throws IOException
	{
		System.out.println("Randomly initializing topic assignments ...");

		topicAssignments = new ArrayList<List<Integer>>();
		for (int dIndex=0; dIndex<corpus.size(); dIndex++) { 
			List<Integer> topics = new ArrayList<Integer>();
			List<List<Integer>> document = corpus.get(dIndex);
			for (int sIndex=0 ; sIndex<document.size() ; sIndex++) {
				List<Integer> sentences = document.get(sIndex);
				int topic = FuncUtils.nextDiscrete(multiPros); // Sample a topic
				docTopicCount[dIndex][topic] += 1;
				sumDocTopicCount[dIndex] += 1;
				for(int wIndex=0 ; wIndex<sentences.size() ; wIndex++){
					topicWordCount[topic][document.get(sIndex).get(wIndex)] += 1;
					sumTopicWordCount[topic] += 1;
				}
				topics.add(topic);
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
		for (int dIndex = 0; dIndex < corpus.size(); dIndex++) {
			List<List<Integer>> document = corpus.get(dIndex);
			for(int sIndex =0; sIndex < document.size(); sIndex++){
				// Get current sentence and its topic
				List<Integer> sentence = document.get(sIndex);
				int topic = topicAssignments.get(dIndex).get(sIndex);
				// Decrease counts
				docTopicCount[dIndex][topic] -= 1;
				sumDocTopicCount[dIndex] -= 1;
				for(int wIndex=0 ; wIndex<sentence.size() ; wIndex++){
					topicWordCount[topic][document.get(sIndex).get(wIndex)] -= 1;
					sumTopicWordCount[topic] -= 1;
				}
				// Sample a topic
				for (int tIndex = 0; tIndex < numTopics; tIndex++) {
					double beta0 = sumTopicWordCount[tIndex] + betaSum;
					int m0 = 0;
					double expectWT = 1;
					// getting the number of total words (or word w) in sentence i
					List<String> sentenceStat = new ArrayList<String>();
					for(int wIndex=0 ; wIndex<sentence.size() ; wIndex++){
						sentenceStat.add(id2WordVocabulary.get(document.get(sIndex).get(wIndex)));
					}
					Map<String, Integer> unique = new HashMap<String, Integer>();
					for (String s : sentenceStat) {
						Integer cnt = unique.get(s);
						if (cnt == null) {
							unique.put(s, 1);
						} else {
							unique.put(s, cnt + 1);
						}
					}
					for (Map.Entry<String, Integer> entry : unique.entrySet()) {
						String key = entry.getKey();
						int cnt = entry.getValue();
						double betaw = topicWordCount[tIndex][word2IdVocabulary
								.get(key)] + beta;
						for (int m = 0; m < cnt; m++) {
							expectWT *= (betaw + m) / (beta0 + m0);
							m0++;
						}
					}
					multiPros[tIndex] = (docTopicCount[dIndex][tIndex] + alpha) * expectWT;
				}
				topic = FuncUtils.nextDiscrete(multiPros);
				
				// Increase counts
				docTopicCount[dIndex][topic] += 1;
				sumDocTopicCount[dIndex] += 1;
				for(int wIndex=0 ; wIndex<sentence.size() ; wIndex++){
					topicWordCount[topic][document.get(sIndex).get(wIndex)] += 1;
					sumTopicWordCount[topic] += 1;
				}
				// Update topic assignments
				topicAssignments.get(dIndex).set(sIndex, topic);
			}
		}
	}

	public double computePerplexity() {
		double logliCorpus = 0.0;
		for (int dIndex = 0; dIndex < numDocuments; dIndex++) {
			int docSize = corpus.get(dIndex).size();
			double logliDoc = 0.0;
			for (int sIndex = 0; sIndex < docSize; sIndex++) {
				int sentenceSize = corpus.get(dIndex).get(sIndex).size();
				double likeSentence = 0.0;
				for (int tIndex = 0; tIndex < numTopics; tIndex++) {
					double likeWord = 0.0;
					for (int wIndex = 0; wIndex < sentenceSize; wIndex++) {
						int word = corpus.get(dIndex).get(sIndex).get(wIndex);
						likeWord += ((docTopicCount[sIndex][tIndex] + alpha) / (sumDocTopicCount[sIndex] + alphaSum))
								* ((topicWordCount[tIndex][word] + beta) / (sumTopicWordCount[tIndex] + betaSum));
					}
					likeSentence += likeWord;
				}
				logliDoc += Math.log(likeSentence);
			}
			logliCorpus += logliDoc;
		}
		double perplexity = Math.exp(-1.0 * logliCorpus / numWordsInCorpus);
		if (perplexity < 0)
			throw new RuntimeException("Illegal perplexity value: "
					+ perplexity);
		return perplexity;
	}

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
		for (int dIndex = 0; dIndex < corpus.size(); dIndex++) {
			List<List<Integer>> document = corpus.get(dIndex);
			for (int sIndex = 0; sIndex < document.size(); sIndex++) {
				writer.write(topicAssignments.get(dIndex).get(sIndex) + " ");
			}
			writer.write("\n");
		}
		writer.close();
	}

	public void writeTopTopicalWords()
		throws IOException
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
			+ expName + ".topWords"));

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
					writer.write("\n\n");
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
				double pro = (docTopicCount[i][j] + alpha)
					/ (sumDocTopicCount[i] + alphaSum);
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
				writer.write(docTopicCount[i][j] + " ");
			}
			writer.write("\n");
		}
		writer.close();
	}

	public void write()
		throws IOException
	{
		writeDictionary();
		writeTopTopicalWords();
		writeDocTopicPros();
		writeTopicAssignments();
		writeTopicWordPros();
	}

	public static void main(String args[])
		throws Exception
	{
		GibbsSamplingSLDA slda = new GibbsSamplingSLDA("data/corpus1.txt", 20, 0.1,
			0.01, 1000, 20, "testSLDA");
		slda.inference();
	}
}
