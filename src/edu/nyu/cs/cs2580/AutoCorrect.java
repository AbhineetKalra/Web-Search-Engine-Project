package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class AutoCorrect {

    private HashMap<String, Integer> correctWordFreq;

    public AutoCorrect() throws IOException {
	correctWordFreq = new HashMap<String, Integer>();
	String english = "english_words.txt";
	BufferedReader br = new BufferedReader(new FileReader(english));
	Pattern p = Pattern.compile("\\w+");
	for (String line = ""; line != null; line = br.readLine()) {
	    Matcher m = p.matcher(line.toLowerCase());
	    while (m.find()) {
		correctWordFreq.put((line = m.group()),
			correctWordFreq.containsKey(line) ? correctWordFreq.get(line) + 1 : 1);
	    }
	}
	br.close();
    }

    public String correctString(String str) {
	if (correctWordFreq.containsKey(str)) {
	    return str;
	}
	ArrayList<String> strList = editDistance1(str);
	HashMap<Integer, String> correctStrings = new HashMap<Integer, String>();
	for (String s : strList) {
	    if (correctWordFreq.containsKey(s)) {
		correctStrings.put(correctWordFreq.get(s), s);
	    }
	}
	if (correctStrings.size() > 0) {
	    return correctStrings.get(Collections.max(correctStrings.keySet()));
	}
	for (String s1 : strList) {
	    for (String s2 : editDistance1(s1)) {
		if (correctWordFreq.containsKey(s2)) {
		    correctStrings.put(correctWordFreq.get(s2), s2);
		}
	    }
	}
	if (correctStrings.size() > 0) {
	    return (correctStrings.get(Collections.max(correctStrings.keySet())));
	} else {
	    return ("?" + str);
	}
    }

    private ArrayList<String> editDistance1(String str) {
	ArrayList<String> strList = new ArrayList<String>();
	for (int i = 0; i < str.length(); ++i) {
	    strList.add(str.substring(0, i) + str.substring(i + 1));
	}
	for (int i = 0; i < str.length() - 1; ++i) {
	    strList.add(
		    str.substring(0, i) + str.substring(i + 1, i + 2) + str.substring(i, i + 1) + str.substring(i + 2));
	}
	for (int i = 0; i < str.length(); ++i) {
	    for (char ch = 'a'; ch <= 'z'; ++ch) {
		strList.add(str.substring(0, i) + String.valueOf(ch) + str.substring(i + 1));
	    }
	}
	for (int i = 0; i <= str.length(); ++i) {
	    for (char ch = 'a'; ch <= 'z'; ++ch) {
		strList.add(str.substring(0, i) + String.valueOf(ch) + str.substring(i));
	    }
	}
	return strList;
    }
}