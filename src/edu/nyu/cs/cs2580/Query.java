package edu.nyu.cs.cs2580;

import java.util.Scanner;
import java.util.Vector;
import java.io.IOException;
/**
 * Representation of a user query.
 * 
 * In HW1: instructors provide this simple implementation.
 * 
 * In HW2: students must implement {@link QueryPhrase} to handle phrases.
 * 
 * @author congyu
 * @auhtor fdiaz
 */
public class Query {
  public String _query = null;
  public Vector<String> _tokens = new Vector<String>();

  public Query(String query) {
    _query = query;
  }

  public void processQuery() throws IOException {
    if (_query == null) {
      return;
    }
    Parser parse = new Parser();
    Scanner s = new Scanner(parse.Stem(_query));
    while (s.hasNext()) {
      _tokens.add(s.next());
    }
    s.close();
    parse = null;
  }
}
