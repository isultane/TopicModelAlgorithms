package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class FuncUtils
{
    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValueDescending(Map<K, V> map)
    {
        List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(map.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<K, V>>()
        {
            @Override
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2)
            {
                int compare = (o1.getValue()).compareTo(o2.getValue());
                return -compare;
            }
        });

        Map<K, V> result = new LinkedHashMap<K, V>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValueAscending(Map<K, V> map)
    {
        List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(map.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<K, V>>()
        {
            @Override
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2)
            {
                int compare = (o1.getValue()).compareTo(o2.getValue());
                return compare;
            }
        });

        Map<K, V> result = new LinkedHashMap<K, V>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    /**
     * Sample a value from a double array
     * 
     * @param probs
     * @return
     */
    public static int nextDiscrete(double[] probs)
    {
        double sum = 0.0;
        for (int i = 0; i < probs.length; i++)
            sum += probs[i];

        double r = MTRandom.nextDouble() * sum;

        sum = 0.0;
        for (int i = 0; i < probs.length; i++) {
            sum += probs[i];
            if (sum > r)
                return i;
        }
        return probs.length - 1;
    }
    
    /**
     * Sample a value from a single integer
     * 
     * @param probs
     * @return
     */
    public static int nextRandom(int sw, int topic)
    {
        double r = MTRandom.nextDouble() * sw;
        
        return (int)r%(topic+1);
    }

    public static double mean(double[] m)
    {
        double sum = 0;
        for (int i = 0; i < m.length; i++)
            sum += m[i];
        return sum / m.length;
    }

    public static double stddev(double[] m)
    {
        double mean = mean(m);
        double s = 0;
        for (int i = 0; i < m.length; i++)
            s += (m[i] - mean) * (m[i] - mean);
        return Math.sqrt(s / m.length);
    }
    public static TreeSet<String> makeSetOfWordsFromFile(String path, boolean usingStemmer) throws Exception {
		TreeSet<String> words = new TreeSet<String>();
		String line;
		BufferedReader reader = new BufferedReader(new FileReader(new File(path)));
		while ((line = reader.readLine()) != null) {
//			if (usingStemmer == true) words.add(stemmer.stemming(line.toLowerCase()));
//			else words.add(line.toLowerCase());
			words.add(line);
		}
		return words;
	}
}
