package edu.nyu.cs.cs2580;

import java.util.Collections;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * Instructors' code for illustration purpose. Non-tested code.
 * 
 * @author congyu
 */
public class RankerConjunctive extends Ranker {

  public RankerConjunctive(Options options,
      CgiArguments arguments, Indexer indexer) {
    super(options, arguments, indexer);
    System.out.println("Using Ranker: " + this.getClass().getSimpleName());
  }

  @Override
  public Vector<ScoredDocument> runQuery(Query query, int numResults) {
    long startTime = System.currentTimeMillis();
    Vector<ScoredDocument> rankVector = new Vector<ScoredDocument>();
    Job doc = null;
    int docid = -1;
    try {
      while ((doc = _indexer.nextDoc(query, docid)) != null) {
        //System.out.println("Sent : " + docid + " | Doc retrieved : " +  doc._docid);
        rankVector.add(new ScoredDocument(doc, 1.0));
        if (rankVector.size() == numResults) {
          break;
        }
        docid = doc._jobid;
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    long stopTime = System.currentTimeMillis();
    long elapsedTime = stopTime - startTime;
    System.out.println("Processed query in " +
        String.format("%d min, %d sec", 
        TimeUnit.MILLISECONDS.toMinutes(elapsedTime),
        TimeUnit.MILLISECONDS.toSeconds(elapsedTime) - 
        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(elapsedTime))));
    return rankVector;
  }
}
