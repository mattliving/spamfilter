import java.util.*;
import java.io.*;

public class filter {

	private static class training {
		protected static HashMap<String, Double[]> vocab = new HashMap<String, Double[]>();
		protected static HashMap<String, Integer> ham    = new HashMap<String, Integer>();
		protected static HashMap<String, Integer> spam   = new HashMap<String, Integer>();
		protected static double priorHam  = 0;
		protected static double priorSpam = 0;
		protected static double hamCount  = 0;
		protected static double spamCount = 0;
		protected static double totalHamWordCount  = 0;
		protected static double totalSpamWordCount = 0; 

		static void setPriorHam(double prior)  { priorHam  = prior; }
		static void setPriorSpam(double prior) { priorSpam = prior; }
		static void incHamCount()  { hamCount++; }
		static void incSpamCount() { spamCount++; }
		static void incHamWordCount(int words) { 
			totalHamWordCount += words; 
		}
		static void incSpamWordCount(int words) { 
			totalSpamWordCount += words; 
		}
		static double getPriorHam() 	 { return priorHam; }
		static double getPriorSpam() 	 { return priorSpam; }
		static double getHamCount() 	 { return hamCount; }
		static double getSpamCount()	 { return spamCount; }
		static double getHamWordCount()  { return totalHamWordCount; }
		static double getSpamWordCount() { return totalSpamWordCount; }
	}

	void printHashMap(HashMap hashMap) {
		Iterator iter = hashMap.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry pairs = (Map.Entry)iter.next();
			System.out.println(pairs.getKey() + " = " + pairs.getValue());
			iter.remove();
		}
		System.out.println();
	}

	void insert(String word, HashMap<String, Integer> hashMap) {
		if (!hashMap.containsKey(word)) {
			hashMap.put(word, 1);
		}
		else {
			hashMap.put(word, hashMap.get(word) + 1);
		}
	}

	int readFile(String type, File trainingFile) {
		int wordCount = 0;
		Scanner s = null;

		try {
			s = new Scanner(new BufferedReader(new FileReader(trainingFile)));
			
			String word;
			while (s.hasNext()) {
				word = s.next();
				wordCount++;
				if (!training.vocab.containsKey(word)) {
					training.vocab.put(word, new Double[2]);
				}
				if (type.matches("ham")) {
					insert(word, training.ham);
				}
				else if (type.matches("spam")) {
					insert(word, training.spam);
				}
			}
		}
		catch (IOException e) {
			System.err.println("Error reading training file.");
			System.exit(1);
		}
		finally {
			if (s != null) {
				s.close();
			}
		}

		return wordCount;
	}

	String readFile(File testfile) {
		double hamSum = 0.0, spamSum = 0.0;
		Scanner s = null;

		try {
			s = new Scanner(new BufferedReader(new FileReader(testfile)));
			
			String word;
			Double[] value;
			while (s.hasNext()) {
				word  = s.next();
				value = training.vocab.get(word);
				if (value != null) {
					hamSum  += Math.log(value[0]);
					spamSum += Math.log(value[1]);
				}
			}
		}
		catch (IOException e) {
			System.err.println("Error reading testfile.");
			System.exit(1);
		}
		finally {
			if (s != null) {
				s.close();
			}
		}

		double result = (Math.log(training.getPriorHam()) + hamSum) - (Math.log(training.getPriorSpam()) + spamSum);
		// System.out.println(result);
		if (result > 0) {
			return "ham";
		}
		else {
			return "spam";
		}
	}

	void calculateProbabilities() {
		int sizeOfVocabulary = training.vocab.size();

		double totalNumberOfDocuments = training.getHamCount() + training.getSpamCount();
		training.setPriorHam(training.getHamCount() / totalNumberOfDocuments);
		training.setPriorSpam(training.getSpamCount() / totalNumberOfDocuments);

		for (Map.Entry<String, Double[]> entry : training.vocab.entrySet()) {
			String key     = entry.getKey();
			Double[] value = entry.getValue();

			// System.out.println("key: " + key + " value: " + value[0] + ", " + value[1]);

			Integer occurrencesInHam, occurrencesInSpam;
			if ((occurrencesInHam = training.ham.get(key)) == null) { 
				occurrencesInHam = 0;
			}
			if ((occurrencesInSpam = training.spam.get(key)) == null) { 
				occurrencesInSpam = 0;
			}

			value[0] = (occurrencesInHam + 1) / (training.getHamWordCount() + sizeOfVocabulary);
			value[1] = (occurrencesInSpam + 1) / (training.getSpamWordCount() + sizeOfVocabulary);

			// System.out.println(value[0] + " " + value[1]);
			training.vocab.put(key, value);
		}
	}

	void train(String traindir) {
		File folder          = new File(traindir);
		File[] trainingFiles = folder.listFiles();

		for (int i = 0; i < trainingFiles.length; i++) {
			String fileName = trainingFiles[i].getName();
			if (trainingFiles[i].isFile() && fileName.endsWith(".txt")) {
				if (fileName.matches("^ham.*txt$")) {
					training.incHamWordCount(readFile("ham", trainingFiles[i]));
					training.incHamCount();
				}
				else if (fileName.matches("^spam.*txt$")) {
					training.incSpamWordCount(readFile("spam", trainingFiles[i]));
					training.incSpamCount();
				}
			}
		}
	}

	void classify(String testfile) {
		File file = new File(testfile);
		String result = readFile(file);
		System.out.println(result);
	}

	void run(String traindir, String testfile) {
		train(traindir);
		calculateProbabilities();
		classify(testfile);
		try {
			BufferedReader in = new BufferedReader(new FileReader(testfile));
		}
		catch (IOException e) {
			
		}
	}
	
	public static void main(String[] args) {
	 	filter classifier = new filter();
	 	if (args.length != 2) {
	 		System.err.println("Error: Please use the format 'java filter traindir testfile'.");
	 	}
	 	else {
	 		classifier.run(args[0], args[1]);
	 	}
	}

}