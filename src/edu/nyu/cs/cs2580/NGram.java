package edu.nyu.cs.cs2580;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import org.json.simple.JSONArray;

public class NGram {
  static ArrayList<String> sentences = new ArrayList<String>();
  static HashMap<String, Integer> count = new HashMap<>();
  static HashMap<String, HashMap<String, Double>> nGram = null;

  public static void calculateNgram() {
    if (nGram == null) {
      System.out.println("Calculating Ngrams");
      nGram = new HashMap<String, HashMap<String, Double>>();
      Scanner inFile = null;
      try {
        inFile = new Scanner(new File("data//cleanedTitles"));
      } catch (FileNotFoundException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      while (inFile.hasNext()) {
        sentences.add(inFile.nextLine());
      }
      inFile.close();
      if (count.size() == 0) {
        Scanner readFile = null;
        try {
          readFile = new Scanner(new File("data//cleanedTitles"));
        } catch (FileNotFoundException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        while (readFile.hasNext()) {
          String[] temp = readFile.next().split(" ");
          for (String t : temp) {
            count.put(t, 1 + (count.containsKey(t) ? count.get(t) : 0));
          }
        }
        readFile.close();
      }
      for (String s : sentences) {
        String[] words = s.split("[\\s]");
        for (int i = 0; i <= words.length - 2; i++) {
          if (i <= words.length - 3) {
            if (nGram.containsKey(words[i] + " " + words[i + 1])) {
              if (nGram.get(words[i] + " " + words[i + 1]).containsKey(words[i + 2])) {
                double v = nGram.get(words[i] + " " + words[i + 1]).get(words[i + 2]);
                v++;
                nGram.get(words[i] + " " + words[i + 1]).put(words[i + 2], v);
              } else {
                nGram.get(words[i] + " " + words[i + 1]).put(words[i + 2], 1.0);
              }
            } else {
              nGram.put(words[i] + " " + words[i + 1], createResult(words[i + 2]));
            }
          }
          if (nGram.containsKey(words[i])) {
            if (nGram.get(words[i]).containsKey(words[i + 1])) {
              double v = nGram.get(words[i]).get(words[i + 1]);
              v++;
              nGram.get(words[i]).put(words[i + 1], v);
            } else {
              nGram.get(words[i]).put(words[i + 1], 1.0);
            }
          } else {
            nGram.put(words[i], createResult(words[i + 1]));
          }
        }
      }
    }
  }

  @SuppressWarnings ("unchecked")
  public static JSONArray createTable(String query) throws FileNotFoundException {
    JSONArray suggestionArray = null;
    String[] queryWords = query.split(" ");
    if (queryWords.length > 1) {
      query = queryWords[queryWords.length - 2] + " " + queryWords[queryWords.length - 1];
    }
    List<String> list = new ArrayList<String>(nGram.get(query).keySet());
    Collections.sort(list, new Comparator<String>() {
      @Override
      public int compare(String x, String y) {
        if (count.containsKey(y) && count.containsKey(x))
          return count.get(y) - count.get(x);
        else
          return -1;
      }
    });
    suggestionArray = new JSONArray();
    for (int i = 0; i < list.size() && i < 5; i++) {
      // for (String word : list) {
      suggestionArray.add(query + " " + list.get(i));
    }
    return suggestionArray;
  }

  static final HashMap<String, Double> createResult(String s) {
    HashMap<String, Double> result = new HashMap<String, Double>();
    result.put(s, 1.0);
    return result;
  }

  @SuppressWarnings ("unchecked")
  static JSONArray wordMatch(String queries) {
    String[] queryarray = queries.split(" ");
    String query = queryarray[queryarray.length - 1];

    String initialRemaining = "";
    for (int i = 0; i < queryarray.length - 1; i++) {
      initialRemaining = initialRemaining + " " + queryarray[i];
    }
    ArrayList<String> wordsStarting = new ArrayList<String>();
    for (String word : count.keySet()) {
      if (word.startsWith(query)) {
        wordsStarting.add(word);
      }
    }
    Collections.sort(wordsStarting, new Comparator<String>() {
      @Override
      public int compare(String x, String y) {
        if (count.containsKey(y) && count.containsKey(x))
          return count.get(y) - count.get(x);
        else
          return -1;
      }
    });
    JSONArray result = new JSONArray();
    for (int i = 0; i < wordsStarting.size() && i < 5; i++) {
      // for (String word : list) {
      result.add(initialRemaining + " " + wordsStarting.get(i));
    }
    return result;
  }

  public static void main(String[] args) {
    calculateNgram();
    System.out.println(NGram.wordMatch(""));
  }
}