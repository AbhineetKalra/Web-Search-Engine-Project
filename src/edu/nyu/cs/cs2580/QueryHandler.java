package edu.nyu.cs.cs2580;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Vector;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * Handles each incoming query, students do not need to change this class except
 * to provide more query time CGI arguments and the HTML output. N.B. This class
 * is not thread-safe.
 * 
 * @author congyu
 * @author fdiaz
 */
class QueryHandler implements HttpHandler {

  /**
   * CGI arguments provided by the user through the URL. This will determine
   * which Ranker to use and what output format to adopt. For simplicity, all
   * arguments are publicly accessible.
   */
  public static class CgiArguments {
    // The raw user query
    public String _query = "";
    // How many results to return
    private int _numResults = 10;

    String _latitude = "";
    String _longitude = "";

    // The type of the ranker we will be using.
    public enum RankerType {
      NONE, FULLSCAN, CONJUNCTIVE, FAVORITE, COSINE, PHRASE, QL, LINEAR,
    }

    public enum SortType {
      LOCATION, TIME, RATING,
    }

    public RankerType _rankerType = RankerType.NONE;

    // The output format.
    public enum OutputFormat {
      TEXT, HTML, JSON,
    }

    public OutputFormat _outputFormat = OutputFormat.TEXT;
    public SortType _sortType = SortType.LOCATION;

    public CgiArguments(String uriQuery) {
      String[] params = uriQuery.split("&");
      for (String param : params) {
        String[] keyval = param.split("=", 2);
        if (keyval.length < 2) {
          continue;
        }
        String key = keyval[0].toLowerCase();
        String val = keyval[1];
        if (key.equals("query")) {
          _query = val;
        } else if (key.equals("num")) {
          try {
            _numResults = Integer.parseInt(val);
          } catch (NumberFormatException e) {
            // Ignored, search engine should never fail upon invalid
            // user input.
          }
        } else if (key.equals("ranker")) {
          try {
            _rankerType = RankerType.valueOf(val.toUpperCase());
          } catch (IllegalArgumentException e) {
            // Ignored, search engine should never fail upon invalid
            // user input.
          }
        } else if (key.equals("format")) {
          try {
            _outputFormat = OutputFormat.valueOf(val.toUpperCase());
          } catch (IllegalArgumentException e) {
            // Ignored, search engine should never fail upon invalid
            // user input.
          }
        } else if (key.equals("lat")) {
          try {
            _latitude = val.toUpperCase();
          } catch (IllegalArgumentException e) {
            // Ignored, search engine should never fail upon invalid
            // user input.
          }
        } else if (key.equals("longitude")) {
          try {
            _longitude = val.toUpperCase();
          } catch (IllegalArgumentException e) {
            // Ignored, search engine should never fail upon invalid
            // user input.
          }
        } else if (key.equals("sort")) {
          try {
            _sortType = SortType.valueOf(val.toUpperCase());
          } catch (IllegalArgumentException e) {
            // Ignored, search engine should never fail upon invalid
            // user input.
          }
        }
      } // End of iterating over params
    }
  }

  // For accessing the underlying documents to be used by the Ranker. Since
  // we are not worried about thread-safety here, the Indexer class must take
  // care of thread-safety.
  private Indexer _indexer;

  public QueryHandler(Options options, Indexer indexer) {
    _indexer = indexer;
  }

  private void respondWithMsg(HttpExchange exchange, final String message) throws IOException {
    Headers responseHeaders = exchange.getResponseHeaders();
    responseHeaders.set("Content-Type", "text/plain");
    exchange.sendResponseHeaders(200, 0); // arbitrary number of bytes
    OutputStream responseBody = exchange.getResponseBody();
    responseBody.write(message.getBytes());
    responseBody.close();
  }

  private void constructTextOutput(final Vector<ScoredDocument> docs, StringBuffer response) {
    for (ScoredDocument doc : docs) {
      response.append(response.length() > 0 ? "\n" : "");
      response.append(doc.asTextResult());
    }
    response.append(response.length() > 0 ? "\n" : "[]");
  }

  @SuppressWarnings ("unchecked")
  private void constructJsonOutput(final Vector<ScoredDocument> docs, StringBuffer response,
      final String query, final String incorrectQuery, final String loc) {
    response.append("{");
    response.append("\"incorrectQuery\" : \"" + incorrectQuery + "\",\"correctQuery\" : \"" + query
        + "\",\"location\" : \"" + loc + "\", \"data\" : [");
    String str = "";
    for (ScoredDocument doc : docs) {
      str += doc.asJsonResult().toString() + ",";
    }
    if (str.endsWith(",")) {
      str = str.substring(0, str.length() - 1);
    }
    response.append(str);
    response.append("]}");
  }

  public void handle(HttpExchange exchange) throws IOException {
    String requestMethod = exchange.getRequestMethod();
    if (!requestMethod.equalsIgnoreCase("GET")) { // GET requests only.
      return;
    }

    // Print the user request header.
    Headers requestHeaders = exchange.getRequestHeaders();
    System.out.print("Incoming request: ");
    for (String key : requestHeaders.keySet()) {
      System.out.print(key + ":" + requestHeaders.get(key) + "; ");
    }
    System.out.println();

    // Validate the incoming request.
    String uriQuery = exchange.getRequestURI().getQuery();
    String uriPath = exchange.getRequestURI().getPath();
    if (uriPath == null || uriQuery == null) {
      respondWithMsg(exchange, "Something wrong with the URI!");
    }
    if (!uriPath.equals("/search") && !uriPath.startsWith("/autosuggest")
        && !uriPath.startsWith("/log") && !uriPath.startsWith("/wordmatch")) {
      respondWithMsg(exchange, "Only /search, /log, /wordmatch or /autosuggest are handled!");
    }
    System.out.println("Query: " + uriPath + "/" + uriQuery);

    CgiArguments cgiArgs = new CgiArguments(uriQuery);
    if (cgiArgs._query.isEmpty()) {
      respondWithMsg(exchange, "No query is given!");
    }

    // Process the CGI arguments.

    if (uriPath.equals("/search")) {

      // Create the ranker.
      Ranker ranker = Ranker.Factory.getRankerByArguments(cgiArgs, SearchEngine.OPTIONS, _indexer);
      if (ranker == null) {
        respondWithMsg(exchange, "Ranker " + cgiArgs._rankerType.toString() + " is not valid!");
      }

      // Processing the query.
      // Query processedQuery = new Query(cgiArgs._query);
      QueryLocation processedQuery = new QueryLocation(cgiArgs._query, cgiArgs._latitude,
          cgiArgs._longitude, cgiArgs._sortType.toString());
      processedQuery.processQuery();

      // Ranking.
      Vector<ScoredDocument> scoredDocs = ranker.runQuery(processedQuery, cgiArgs._numResults);
      StringBuffer response = new StringBuffer();
      switch (cgiArgs._outputFormat) {
      case TEXT :
        constructTextOutput(scoredDocs, response);
        break;
      case HTML :
        // @CS2580: Plug in your HTML output
        break;
      case JSON :
        constructJsonOutput(scoredDocs, response, processedQuery._query,
            processedQuery.incorrectQuery, processedQuery.location);
      default :
        // nothing
      }
      respondWithMsg(exchange, response.toString());
      System.out.println("Finished query: " + cgiArgs._query);
    } else if (uriPath.equals("/autosuggest")) {
      // ranker.runQuery(processedQuery, cgiArgs._numResults);
      respondWithMsg(exchange, NGram.createTable(cgiArgs._query).toString());
    } else if (uriPath.equals("/wordmatch")) {
      // ranker.runQuery(processedQuery, cgiArgs._numResults);
      respondWithMsg(exchange, NGram.wordMatch(cgiArgs._query).toString());
    } else if (uriPath.equals("/log")) {
      respondWithMsg(exchange, Logger.TrackClick(cgiArgs._query).toString());
    }
  }
}
