package edu.nyu.cs.cs2580;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

public class Tagger {

    public Vector<String> nerTagger(String text) throws IOException {
	Vector<String> location_ner = new Vector<String>();

	// creates a StanfordCoreNLP object, with lemmatization,
	// NER, parsing, and coreference resolution
	Properties props = new Properties();
	props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
	StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

	// read some text from the file..
	// String text = " Java Software Engineer in Santa Clara, California";

	// create an empty Annotation just with the given text
	Annotation document = new Annotation(text);

	// run all Annotators on this text
	pipeline.annotate(document);

	// these are all the sentences in this document
	// a CoreMap is essentially a Map that uses class objects as keys and
	// has values with custom types
	List<CoreMap> sentences = (List<CoreMap>) document.get(SentencesAnnotation.class);

	for (CoreMap sentence : sentences) {
	    // traversing the words in the current sentence
	    // a CoreLabel is a CoreMap with additional token-specific methods
	    for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
		// this is the text of the token
		String word = token.get(TextAnnotation.class);
		// this is the POS tag of the token
		String pos = token.get(PartOfSpeechAnnotation.class);
		// this is the NER label of the token
		String ne = token.get(NamedEntityTagAnnotation.class);

		if (ne.equalsIgnoreCase("location")) {
		    location_ner.add(word);

		}

		// System.out.println("word: " + word + " pos: " + pos + " ne:"
		// + ne);
	    }
	}
	return (location_ner);
    }

    public static void main(String[] args) throws IOException {
	Tagger tagger = new Tagger();
	System.out.println(tagger.nerTagger("java software developer in San Francisco California"));
    }

}
