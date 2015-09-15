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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.IntStream;

import util.FuncUtils;

/**
 * SW: A Java package for the SW topic model
 * 
 * Implementation of the Special Word  (SW) topic model, using
 * collapsed Gibbs sampling, as described in:
 * 
 * Chemudugunta, C., & Steyvers, P. S. M. (2007). Modelling General and Specific Aspects of 
 * Documents with a Probabilistic Topic Model. In Advances in Neural Information Processing
 * Systems 19: Proceedings of the 2006 Conference (Vol. 19, p. 241). MIT Press.
 * 
 * @author: Sultan Alqahtani
 */

public class GibbsSamplingSW
{
	public double alpha; // Hyper-parameter alpha
	public double[] betas; // Hyper-parameter betas
		// Sultan added
		public double gamma; // Hyper-parameter gamma
	public int numTopics; // Number of topics
	public int numIterations; // Number of Gibbs sampling iterations
	public int topWords; // Number of most probable words for each topic

	public double alphaSum; // alpha * numTopics
	public double[] betaSum; // beta[i] * vocabularySize
		// Sultan added
		public double gammaSum; // gamma * 3 -> three means words categories (topic, special-words, or background word)

	public List<List<Integer>> corpus; // Word ID-based corpus
	public List<List<Integer>> topicAssignments; // Topics assignments for words in the corpus
		//Sultan added
		public List<List<Integer>> wordTypeAssignments; // x assignments for words in the corpus
	
	public int numDocuments; // Number of documents in the corpus
	public int numWordsInCorpus; // Number of words in the corpus

	public HashMap<String, Integer> word2IdVocabulary; // Vocabulary to get ID
														// given a word
	public HashMap<Integer, String> id2WordVocabulary; // Vocabulary to get word
														// given an ID
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
		// Sultan added
		// vocabularySize * numDocuments matrix
		// Given a document: number of times word w is assigned to o the special-words distribution of document d 
		public int[][] docWordCount;
		// Total number of special-words  in every document
		public int[] sumDocWordCount;
		
		// Sultan added
		// Number of words in document d 
		public int Nd[];
		// Number of words in document d assigned to the latent topics -> similar to sumDocTopicCount
		public int Nd0[];
		// Number of words in document d assigned to special words
		public int Nd1[];
	
		
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


	public GibbsSamplingSW(String pathToCorpus, int inNumTopics,
		double inAlpha, double[] inBetas,double gamma, int inNumIterations, int inTopWords,
		String inExpName)
		throws Exception
	{
		this(pathToCorpus, inNumTopics, inAlpha, inBetas,gamma, inNumIterations,
			inTopWords, inExpName, "");
	}

	public GibbsSamplingSW(String pathToCorpus, int inNumTopics,
		double inAlpha, double[] inBeta,double gamma, int inNumIterations, int inTopWords,
		String inExpName, String pathToTAfile)
		throws Exception
	{

		alpha = inAlpha;
		this.betas = inBeta;
			// Sultan added
			this.gamma = gamma;
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

		System.out.println("Reading topic modeling (SWB) corpus: " + pathToCorpus);

		word2IdVocabulary = new HashMap<String, Integer>();
		id2WordVocabulary = new HashMap<Integer, String>();
		corpus = new ArrayList<List<Integer>>();
		numDocuments = 0;
		numWordsInCorpus = 0;

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

		vocabularySize = word2IdVocabulary.size(); // vocabularySize = indexWord
		docTopicCount = new int[numDocuments][numTopics];
		topicWordCount = new int[numTopics][vocabularySize];
		sumDocTopicCount = new int[numDocuments];
		sumTopicWordCount = new int[numTopics];
		
			// Sultan added
			docWordCount = new int[numDocuments][vocabularySize];
			sumDocWordCount = new int[numDocuments];
	
			//Sultan added
			Nd = new int[numDocuments];
			Nd0 = new int[numDocuments];
			Nd1 = new int[numDocuments];
		
			
		multiPros = new double[numTopics * 2];
		for (int i = 0; i < numTopics * 2; i++) {
			multiPros[i] = 1.0 / numTopics;
		}

		alphaSum = numTopics * alpha;
		this.betaSum = new double[2];
		for(int i = 0; i<2 ; i++)
			betaSum[i] = betas[i] * vocabularySize;
			// Sultan added
			gammaSum = gamma * 2;

		System.out.println("Corpus size: " + numDocuments + " docs, "
			+ numWordsInCorpus + " words");
		System.out.println("Vocabuary size: " + vocabularySize);
		System.out.println("Number of topics: " + numTopics);
		System.out.println("alpha: " + alpha);
		for(int i=0 ; i<2 ; i++) System.out.println("beta: " + betas[i]);
		System.out.println("Number of sampling iterations: " + numIterations);
		System.out.println("Number of top topical words: " + topWords);

		tAssignsFilePath = pathToTAfile;
//		if (tAssignsFilePath.length() > 0)
//			initialize(tAssignsFilePath);
//		else
			initialize();
	}

	/**
	 * Randomly initialize topic assignments
	 */
	public void initialize() throws IOException {
		System.out.println("Randomly initializing topic assignments ...");

		topicAssignments = new ArrayList<List<Integer>>();

		for (int i = 0; i < numDocuments; i++) {
			List<Integer> topics = new ArrayList<Integer>();
			int docSize = corpus.get(i).size();
			// Sultan added
			for (int j = 0; j < docSize; j++) {
				int subtopic = FuncUtils.nextDiscrete(multiPros); // Sample a topic
				int wordId = corpus.get(i).get(j);
				
				int topic = subtopic % numTopics;
				// Increase counts
				docTopicCount[i][topic] += 1;
				sumDocTopicCount[i] += 1;
				
				if(subtopic == topic){ // latent-topic distribution
					// Increase counts
					Nd0[i] += 1;
					topicWordCount[topic][wordId] += 1;
					sumTopicWordCount[topic] += 1;
				} else  { // special-word distribution
					// Increase counts
					Nd1[i] += 1;
					docWordCount[i][wordId] += 1;
					sumDocWordCount[i] += 1;
				} 
				
				topics.add(subtopic);
			}
			Nd[i] += Nd0[i] + Nd1[i] ;
//			System.out.println("doc size=> LT:["+Nd0[i]+"] SW["+Nd1[i]+"] BK["+Nd2[i]+"] = ["+Nd[i]+"]");

			topicAssignments.add(topics);

		}
	}

	public void inference()
		throws IOException
	{
		System.out.println("Running Gibbs sampling inference: ");

		for (int iter = 1; iter <= numIterations; iter++) {

//			System.out.println("\tSampling iteration: " + (iter));
			if((iter%10) == 0)
				System.out.println(computePerplexity());

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

	public void sampleInSingleIteration() {
		for (int dIndex = 0; dIndex < numDocuments; dIndex++) {
			int docSize = corpus.get(dIndex).size();
			for (int wIndex = 0; wIndex < docSize; wIndex++) {
				// Get current word and its topic
				int word = corpus.get(dIndex).get(wIndex);
				int subtopic = topicAssignments.get(dIndex).get(wIndex);
				int topic = subtopic % numTopics;
				
				// Decrease counts
				docTopicCount[dIndex][topic] -= 1;
				sumDocTopicCount[dIndex] -= 1;
				Nd[dIndex] -= 1;
				
				if (topic == subtopic) {
					Nd0[dIndex] -= 1;
					topicWordCount[topic][word] -= 1;
					sumTopicWordCount[topic] -= 1;	
				}else {
					// Decrease counts
					Nd1[dIndex] -= 1;
					docWordCount[dIndex][word] -= 1;
					sumDocWordCount[dIndex] -= 1;
				}
				// Sample a topic and binary indicator variable x
				for (int tIndex = 0; tIndex < numTopics; tIndex++) {
					multiPros[tIndex] = ((Nd0[dIndex] + gamma) / (Nd[dIndex] + gammaSum))
							* ((docTopicCount[dIndex][tIndex] + alpha) / (sumDocTopicCount[dIndex] + alphaSum))
							* ((topicWordCount[tIndex][word] + betas[0]) / (sumTopicWordCount[tIndex] + betaSum[0]));
					
					multiPros[tIndex+numTopics] = ((Nd1[dIndex] + gamma) / (Nd[dIndex] + gammaSum))
							* ((docWordCount[dIndex][word] + betas[1]) / (sumDocWordCount[dIndex] + betaSum[1]));
				}
				
				subtopic = FuncUtils.nextDiscrete(multiPros);
				topic = subtopic % numTopics;
			
				// Increase counts
				docTopicCount[dIndex][topic] -= 1;
				sumDocTopicCount[dIndex] -= 1;
				Nd[dIndex] += 1;
				if(topic == subtopic){
					Nd0[dIndex] += 1;
					topicWordCount[topic][word] += 1;
					sumTopicWordCount[topic] += 1;
				}else {
					Nd1[dIndex] += 1;
					docWordCount[dIndex][word] += 1;
					sumDocWordCount[dIndex] += 1;
				}
				// Update topic assignments
				topicAssignments.get(dIndex).set(wIndex, subtopic);
			}
		}
	}

	public double computePerplexity() {
		double logliCorpus = 0.0;
		for (int dIndex = 0; dIndex < numDocuments; dIndex++) {
			int docSize = corpus.get(dIndex).size();
			double logliDoc = 0.0;
			for (int wIndex = 0; wIndex < docSize; wIndex++) {
				int word = corpus.get(dIndex).get(wIndex);
				double likeWord = 0.0;
				for (int tIndex = 0; tIndex < numTopics; tIndex++) {
					likeWord += ((Nd0[dIndex] + gamma) / (Nd[dIndex] + gammaSum))
							* ((docTopicCount[dIndex][tIndex] + alpha) / (sumDocTopicCount[dIndex] + alphaSum))
							* ((topicWordCount[tIndex][word] + betas[0]) / (sumTopicWordCount[tIndex] + betaSum[0]));
				}
				likeWord += ((Nd1[dIndex] + gamma) / (Nd[dIndex] + gammaSum))
						* ((docWordCount[dIndex][word] + betas[1]) / (sumDocWordCount[dIndex] + betaSum[1]));
				
				logliDoc += Math.log(likeWord);
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
		writer.write("\n-beta" + "\t" + "[" + betas[0] + "," + betas[1] + "]");
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

	public void writeTopTopicalWords() throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
				+ expName + ".topWords"));

		for (int tIndex = 0; tIndex < numTopics; tIndex++) {
			writer.write("Topic" + new Integer(tIndex) + ":");

			Map<Integer, Double> wordCount = new TreeMap<Integer, Double>();
			for (int wIndex = 0; wIndex < vocabularySize; wIndex++) {
				/*
				 * double prob = ((Nd0[dIndex] + gamma) / (Nd[dIndex] + 3 *
				 * gamma)) ((docTopicCount[dIndex][tIndex] + alpha) /
				 * (sumDocTopicCount[dIndex] + alphaSum))
				 * ((topicWordCount[tIndex][wIndex] + betas[0]) /
				 * (sumTopicWordCount[tIndex] + betaSum[0]));
				 * 
				 * prob += ((Nd1[dIndex] + gamma) / (Nd[dIndex] + 3 * gamma))
				 * ((docWordCount[dIndex][wIndex] + betas[1]) /
				 * (sumDocWordCount[dIndex] + betaSum[1]));
				 * 
				 * prob += ((Nd2[dIndex] + gamma) / (Nd[dIndex] + 3 * gamma))
				 * ((wordCount[dIndex][wIndex] + betas[2]) /
				 * (sumDocWordCount[dIndex] + betaSum[2]));
				 */

				double prob = (topicWordCount[tIndex][wIndex] + betas[0])
						/ (sumTopicWordCount[tIndex] + betaSum[0]);
				wordCount.put(wIndex, prob);
			}
			wordCount = FuncUtils.sortByValueDescending(wordCount);

			Set<Integer> mostLikelyWords = wordCount.keySet();
			int count = 0;
			for (Integer index : mostLikelyWords) {
				if (count < topWords) {
					writer.write(" " + id2WordVocabulary.get(index));
					count += 1;
				} else {
					writer.write("\n\n");
					break;
				}
			}
		}

		writer.close();
	}

	public void writeSpecialWordPros() throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
				+ expName + ".psi.csv"));
		for (int i = 0; i < numDocuments; i++) {
			for (int j = 0; j < vocabularySize; j++) {
				double pro = (docWordCount[i][j] + betas[1])
						/ (sumDocWordCount[i] + betaSum[1]);
				writer.write(pro + ",");
			}
			writer.write("\n");
		}
		writer.close();
	}

	public void writeTopicWordPros()
		throws IOException
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
			+ expName + ".phi.csv"));
		for (int i = 0; i < numTopics; i++) {
			for (int j = 0; j < vocabularySize; j++) {
				double pro = (topicWordCount[i][j] + betas[0])
					/ (sumTopicWordCount[i] + betaSum[0]);
				writer.write(pro + ",");
			}
			writer.write("\n");
		}
		writer.close();
	}

	public void writeDocTopicPros()
			throws IOException
		{
			BufferedWriter writer = new BufferedWriter(new FileWriter(folderPath
				+ expName + ".theta.csv"));
			for (int i = 0; i < numDocuments; i++) {
				for (int j = 0; j < numTopics; j++) {
					double pro = (docTopicCount[i][j] + alpha)
						/ (sumDocTopicCount[i] + alphaSum);
					writer.write(pro + ",");
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
		writeTopTopicalWords();
		writeDocTopicPros();
		writeTopicAssignments();
		writeTopicWordPros();
		writeSpecialWordPros();
	}

	public static void main(String args[])
		throws Exception
	{
		String pathToCorpus = "data/corpus.txt";
		double[] betas = {0.01, 0.0001};
		double alpha = 0.1;
		double gamma = 0.5;
		int iteration = 500;
		int topWords = 100;
		int numTopics = 20;
		
		GibbsSamplingSW sw = new GibbsSamplingSW(pathToCorpus, numTopics, alpha,
				betas,gamma, iteration, topWords, "testSW");
		sw.inference();

	}
}
