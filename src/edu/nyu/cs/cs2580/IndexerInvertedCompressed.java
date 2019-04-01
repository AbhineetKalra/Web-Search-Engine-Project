package edu.nyu.cs.cs2580;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2.
 */
public class IndexerInvertedCompressed extends Indexer implements Serializable {
  private static final long serialVersionUID = -113983643337683565L;

  // Maps each term to their integer representation
  private Map<String, Integer> _dictionary = new HashMap<String, Integer>();
  private static HashSet<String> _stopwords = new HashSet<String>();
  // Map to save the inverted index - Words and the sorted list of Job it
  // appears in
  private TreeMap<Integer, ArrayList<Byte>> _invertedList = new TreeMap<Integer, ArrayList<Byte>>();

  private TreeMap<Integer, ArrayList<Integer>> _invertedListDecoded = new TreeMap<Integer, ArrayList<Integer>>();

  // Map to save the skiplist
  private Map<Integer, ArrayList<Byte>> _skipPointers = new HashMap<Integer, ArrayList<Byte>>();

  private Map<Integer, ArrayList<Integer>> _skipPointersDecoded = new HashMap<Integer, ArrayList<Integer>>();

  // Term frequency, key is the integer representation of the term and value
  // is
  // the number of times the term appears in the corpus.
  private Map<Integer, Integer> _termCorpusFrequency = new HashMap<Integer, Integer>();

  // Total words per Jobs
  private Map<Integer, Integer> _totalTermsByDocFrequency = new HashMap<Integer, Integer>();

  // Stores all Job in memory.
  private Vector<Job> _jobs = new Vector<Job>();
  private Integer wordOffset = 0;
  private Map<String, Boolean> isDocFirstToken = new HashMap<String, Boolean>();
  private Parser parse = new Parser();
  private ArrayList<Integer> splitArr = new ArrayList<Integer>(Arrays.asList(0, 5, 10, 15, 20, 25,
      30, 50, 70, 100, 120, 140, 160, 180, 200, 225, 250, 275, 300, 325, 350, 400, 450, 500, 600,
      700, 825, 1000, 1100, 1200, 1300, 1400, 1500, 1600, 1800, 2000, 2250, 2500, 2750, 3000, 4000,
      5000, 6000, 6500, 7000, 8000, 9000, 10500, 12000, 15000, 20000, 30000, 35000, 40000, 45000,
      50000, 55000, 60000, 70000, 80000, 90000, 100000, 170000));

  public IndexerInvertedCompressed(Options options) {
    super(options);
    System.out.println("Using Indexer: " + this.getClass().getSimpleName());
  }

  @Override
  public void constructIndex() throws IOException {
    long startTime = System.currentTimeMillis();
    if (_options._corpusPrefix.contains("scraped")) {
      // Read Stop Words
      // GetStopWords(_options._stopWordsFile);
      System.out.println("Construct index from: Scraped Content");
      File _folder = new File(_options._corpusPrefix + "/");
      File[] allFiles = _folder.listFiles();
      int index = 1;
      for (File _file : allFiles) {

        if (_file.isFile() && !_file.isHidden()) {

          // Read all lines in the files
          String filename = _file.getAbsolutePath();
          System.out.println("Parsing : " + filename);
          File f = new File(filename);
          try {

            Scanner sc = new Scanner(f);
            while (sc.hasNextLine()) {
              processJob(sc.nextLine());
            }
            sc.close();
          } catch (Exception e) {

          }

        }

        index++;
        _file = null;
      }

      _folder = null;
      allFiles = null;
    } else {
      System.out.println("No Construct Index Specified. Exiting ...");
      return;
    }
    // _terms.clear();
    // _terms = null;
    parse = null;
    _stopwords.clear();
    _stopwords = null;
    isDocFirstToken.clear();
    isDocFirstToken = null;

    // Delete all old index files
    File indexDirectory = new File(_options._indexPrefix);
    File[] files = indexDirectory.listFiles();
    for (File f : files) {
      f.delete();
    }
    int dictLength = _invertedListDecoded.size();
    int fileNum = 0;
    if (splitArr.get(splitArr.size() - 1) > dictLength) {
      splitArr = new ArrayList<Integer>();
      splitArr.add(0);
      splitArr.add(dictLength / 2);
    }
    splitArr.add(dictLength + 1);
    System.out.println("Saved " + dictLength + " terms !");
    System.out.println("Store InvertedList in : " + _options._indexPrefix);

    /*
     * ObjectOutputStream writer1 = new ObjectOutputStream(( new
     * BufferedOutputStream(new FileOutputStream(_options._indexPrefix +
     * "/temp-posting-"+String.format("%02d", fileNum)+".gz"))));
     * writer1.writeObject(_invertedListDecoded); //writer.reset();
     * writer1.close();
     */

    // Peforming delta encoding and byte aligned encoding
    for (Map.Entry<Integer, ArrayList<Integer>> entry : _invertedListDecoded.entrySet()) {
      int termCode = entry.getKey();
      ArrayList<Integer> posting = entry.getValue();
      ArrayList<Integer> skipList = _skipPointersDecoded.get(termCode);
      ArrayList<Integer> temp = new ArrayList<Integer>();

      for (int i = 0; i < skipList.size() - 1; i++) {
        int offset = skipList.get(i);
        temp.add(posting.get(offset));
        int len = posting.get(offset + 1);
        temp.add(len);
        if (len == 1) {
          temp.add(posting.get(offset + 2));
        } else {
          ArrayList<Integer> indexes = new ArrayList<Integer>(
              posting.subList(offset + 2, offset + 2 + len));

          for (int j = indexes.size() - 1; j > 0; j--) {
            indexes.set(j, indexes.get(j) - indexes.get(j - 1));
          }

          temp.addAll(indexes);
        }

      }
      _invertedList.put(termCode, encode(temp));
    }

    _invertedListDecoded.clear();
    _invertedListDecoded = null;

    /*
     * // Print Inverted Index for (Integer key :
     * _invertedListDecoded.keySet()) { String s =
     * (String)getKeyFromValue(_dictionary, key); ArrayList<Integer> value =
     * _invertedListDecoded.get(key); System.out.println(s + " -> " +
     * value); }
     * // Print Skip List for (Integer key : _skipPointersDecoded.keySet())
     * { String s = (String)getKeyFromValue(_dictionary, key);
     * ArrayList<Integer> value = _skipPointersDecoded.get(key);
     * System.out.print(s + " : "); for(Integer i : value){
     * System.out.print(i + " "); } System.out.println(""); }
     */

    for (int i = 0; i < splitArr.size() - 1; i++) {
      TreeMap<Integer, ArrayList<Byte>> splittedMap = new TreeMap<Integer, ArrayList<Byte>>(
          _invertedList.subMap(splitArr.get(i), splitArr.get(i + 1)));
      ObjectOutputStream writer = new ObjectOutputStream(
          new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(
              _options._indexPrefix + "/posting-" + String.format("%02d", fileNum) + ".gz"))));
      writer.writeObject(splittedMap);
      // writer.reset();
      writer.close();

      fileNum++;

    }

    this._invertedList.clear();
    this._invertedList = null;

    for (Map.Entry<Integer, ArrayList<Integer>> entry : _skipPointersDecoded.entrySet()) {
      _skipPointers.put(entry.getKey(), encode(entry.getValue()));
    }
    _skipPointersDecoded.clear();
    _skipPointersDecoded = null;
    String indexFile = _options._indexPrefix + "/corpus.idx";
    System.out.println("Store index to: " + indexFile);
    ObjectOutputStream writer = new ObjectOutputStream(
        new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(indexFile))));

    writer.writeObject(this);
    writer.close();

    long stopTime = System.currentTimeMillis();
    long elapsedTime = stopTime - startTime;
    System.out.println("Indexed " + Integer.toString(_numDocs) + " jobs with "
        + Long.toString(_totalTermFrequency) + " terms in "
        + String.format("%d min, %d sec", TimeUnit.MILLISECONDS.toMinutes(elapsedTime),
            TimeUnit.MILLISECONDS.toSeconds(elapsedTime)
                - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(elapsedTime))));

    _dictionary.clear();
    _dictionary = null;
    _termCorpusFrequency.clear();
    _termCorpusFrequency = null;
    _jobs = null;
    _skipPointers.clear();
    _skipPointers = null;
    _totalTermsByDocFrequency.clear();
    _totalTermsByDocFrequency = null;
  }

  public static void GetStopWords(String stopWordFile) throws IOException {
    Parser parse = new Parser();
    BufferedReader br = new BufferedReader(new FileReader(stopWordFile));
    String line;
    while ((line = br.readLine()) != null)
      _stopwords.add(parse.Stem(line.trim()));
    br.close();
  }

  public static Object getKeyFromValue(Map hm, Object value) {
    for (Object o : hm.keySet()) {
      if (hm.get(o).equals(value)) {
        return o;
      }
    }
    return null;
  }

  /**
   * Process the raw content (i.e., one line in corpus.tsv) corresponding to a
   * Job, and constructs the token vectors for both title and body.
   * 
   * @param content
   */
  private void processJob(String content) throws IOException {
    try {
      wordOffset = 0;
      Scanner s = new Scanner(content).useDelimiter("\t");
      String jobID = s.next();
      String title = s.next();
      Vector<Integer> titleTokens = new Vector<Integer>();
      readTermVector(parse.Stem(title), titleTokens);
      String jobURL = s.next();
      String postedDate = s.next();
      Vector<Integer> companyTokens = new Vector<Integer>();
      String companyName = s.next();
      readTermVector(parse.Stem(companyName), companyTokens);
      String country = s.next();
      String state = s.next();
      String city = s.next();
      String latitude = s.next();
      String longitude = s.next();
      Vector<Integer> bodyTokens = new Vector<Integer>();
      String body = s.next();
      readTermVector(parse.Stem(body), bodyTokens);
      s.close();
      body = body.length() > 540 ? body.substring(0, 540) + "..." : body;
      Job j = new Job(_jobs.size(), title, body, companyName, postedDate, country, state, city,
          jobURL, latitude, longitude);
      _jobs.add(j);
      _totalTermsByDocFrequency.put(_numDocs, titleTokens.size() + bodyTokens.size() + companyTokens.size());
      Set<Integer> uniqueTerms = new HashSet<Integer>();
      updateStatistics(titleTokens, uniqueTerms);
      updateStatistics(bodyTokens, uniqueTerms);
      updateStatistics(companyTokens, uniqueTerms);

      j = null;
      uniqueTerms = null;
      titleTokens = null;
      bodyTokens = null;
      companyTokens = null;
      ++_numDocs;
    } catch (Exception ex) {
      ex.printStackTrace();
      System.out.println(ex);
      System.out.println("Error in :" + content);
    }
  }

  /**
   * Tokenize {@code content} into terms, translate terms into their integer
   * representation, store the integers in {@code tokens}.
   * 
   * @param content
   * @param tokens
   */
  private void readTermVector(String content, Vector<Integer> tokens)
      throws IOException, UnsupportedEncodingException {
    Scanner s = new Scanner(content); // Uses white space by default.
    while (s.hasNext()) {
      String token = s.next();
      int idx = -1;

      if (_dictionary.containsKey(token)) {
        idx = _dictionary.get(token);
      } else {
        idx = _dictionary.size();
        // _terms.add(token);
        _dictionary.put(token, idx);
        _termCorpusFrequency.put(idx, 0);
      }

      if (!_invertedListDecoded.containsKey(idx)) {
        _invertedListDecoded.put(idx, new ArrayList<Integer>());
        _skipPointersDecoded.put(idx, new ArrayList<Integer>());
        _skipPointersDecoded.get(idx).add(0);
      }
      tokens.add(idx);
    }
    s.close();
    return;
  }

  /**
   * Update the corpus statistics with {@code tokens}. Using {@code uniques}
   * to bridge between different token vectors.
   * 
   * @param tokens
   * @param uniques
   */
  private void updateStatistics(Vector<Integer> tokens, Set<Integer> uniques) {
    for (int idx : tokens) {
      uniques.add(idx);
      _termCorpusFrequency.put(idx, _termCorpusFrequency.get(idx) + 1);
      ++_totalTermFrequency;
      String key = Integer.toString(idx) + "-" + Integer.toString(_numDocs);
      Boolean alreadyAdded = true;
      if (!isDocFirstToken.containsKey(key)) {
        _invertedListDecoded.get(idx).add(_numDocs);
        isDocFirstToken.put(key, false);
        alreadyAdded = false;
      }
      ArrayList<Integer> data = _skipPointersDecoded.get(idx);
      // if already contains the word in the Job
      // update skipList
      if (alreadyAdded) {
        int currentValue = data.get(data.size() - 1);
        data.set(data.size() - 1, currentValue + 1);
        _skipPointersDecoded.put(idx, data);

        _invertedListDecoded.get(idx).add(wordOffset);
        // Correct invertedList word count
        int previousValue = data.size() > 1 ? data.get(data.size() - 2) : 0;
        int count = currentValue + 1 - previousValue - 2;
        _invertedListDecoded.get(idx).set(previousValue + 1, count);
      } else {
        // Add the word and also create entry in skiplist
        // Start with 3 because first 2 for doc ID and number of items.
        int lastValue = data.size() > 0 ? data.get(data.size() - 1) : 0;
        data.add(lastValue + 3);
        _skipPointersDecoded.put(idx, data);
        _invertedListDecoded.get(idx).add(1);
        _invertedListDecoded.get(idx).add(wordOffset);
      }
      wordOffset++;

    }
  }

  @Override
  @SuppressWarnings ("unchecked")
  public void loadIndex() throws IOException, ClassNotFoundException {

    String indexFile = _options._indexPrefix + "/corpus.idx";
    System.out.println("Load index from: " + indexFile);
    NGram.calculateNgram();
    System.out.println("Finished NGram Construction");

    ObjectInputStream readerObj = new ObjectInputStream(
        new GZIPInputStream(new BufferedInputStream(new FileInputStream(indexFile))));

    IndexerInvertedCompressed loaded = (IndexerInvertedCompressed) readerObj.readObject();
    this._jobs = loaded._jobs;
    // Compute numDocs and totalTermFrequency b/c Indexer is not
    // serializable.
    this._numDocs = _jobs.size();
    for (Integer freq : loaded._termCorpusFrequency.values()) {
      this._totalTermFrequency += freq;
    }
    this._dictionary = loaded._dictionary;
    this.splitArr = loaded.splitArr;
    // this._terms = loaded._terms;
    this._termCorpusFrequency = loaded._termCorpusFrequency;
    this._invertedList = new TreeMap<Integer, ArrayList<Byte>>();
    this._invertedListDecoded = new TreeMap<Integer, ArrayList<Integer>>();
    this._skipPointersDecoded = new HashMap<Integer, ArrayList<Integer>>();
    this._totalTermsByDocFrequency = loaded._totalTermsByDocFrequency;
    this._skipPointers = loaded._skipPointers;
    readerObj.close();
    loaded = null;

    System.out.println(Integer.toString(_numDocs) + " Jobs loaded " + "with "
        + Long.toString(_totalTermFrequency) + " terms!");
  }

  private int GetPostingChunkFileName(int termCode) {
    int idx = 0;
    for (int num : splitArr) {
      if (num > termCode) {
        return idx == 0 ? 0 : idx - 1;
      }
      idx++;
    }
    return 0;
  }

  @SuppressWarnings ("unchecked")
  private TreeMap<Integer, ArrayList<Byte>> loadPostingChunk(int termCode)
      throws IOException, ClassNotFoundException {
    String fileName = _options._indexPrefix + "/posting-"
        + String.format("%02d", GetPostingChunkFileName(termCode)) + ".gz";
    ObjectInputStream reader = new ObjectInputStream(
        new GZIPInputStream(new BufferedInputStream(new FileInputStream(fileName))));
    TreeMap<Integer, ArrayList<Byte>> obj = (TreeMap<Integer, ArrayList<Byte>>) reader.readObject();
    reader.close();
    return obj;
  }

  @Override
  public Job getDoc(int jobid) {
    return (jobid >= _jobs.size() || jobid < 0) ? null : _jobs.get(jobid);
  }

  /**
   * In HW2, you should be using {@link JobIndexed}
   */
  @Override
  public Job nextDoc(Query query, int jobid) {
    try {
      QueryLocation lQuery = (QueryLocation) query;
      ArrayList<Integer> _indexes = new ArrayList<Integer>();
      for (String queryToken : lQuery._tokens) {
        int index = Next(queryToken, jobid);
        // System.out.println(jobid + " <- Sent | Recieved -> " +
        // index);
        _indexes.add(index);
      }

      // Check if any word not found
      if (_indexes.contains(-1)) {
        return null;
      }
      // if Job found
      Set<Integer> uniqueDocs = new HashSet<Integer>(_indexes);
      if (uniqueDocs.size() == 1) {
        // Now check for phrases
        Boolean phrasePass = true;
        if (lQuery.phrase_tokens != null) {
          for (String phraseToken : lQuery.phrase_tokens) {
            String[] phraseParts = phraseToken.split(" ");
            if (!phrasePass) {
              // Since previous phrase was not found, skip finding
              // the next phrase
              break;
            }
            if (phraseParts.length > 0) {

              ArrayList<ArrayList<Integer>> allIndex = new ArrayList<ArrayList<Integer>>();
              for (String token : phraseParts) {
                // get the indexes for all tokens
                ArrayList<Integer> wordIndex = GetWordIndexes(token, _indexes.get(0));
                // System.out.println("jobid " + _indexes.get(0)
                // + " : " + wordIndex);
                allIndex.add(wordIndex);

              }

              // Now check if indexes are near
              ArrayList<Integer> firstWordIndex = allIndex.get(0);
              int tokenOffset = 1;
              for (Integer idx : firstWordIndex) {
                tokenOffset = 1;
                for (int j = 1; j < allIndex.size(); j++) {
                  if (!allIndex.get(j).contains(idx + tokenOffset)) {
                    phrasePass = false;
                    // System.out.println("Not together in "
                    // + _indexes.get(0));
                    break;
                  }
                  phrasePass = true;
                  tokenOffset++;
                }

                // already found in the Job
                if (phrasePass) {
                  // System.out.println("Already together ..
                  // skipping");
                  break;
                }
              }
            }
          }
        }

        if (phrasePass) {
          return getDoc(_indexes.get(0));
        } else {
          // if phrase was not found in the cur doc, try the next
          int max = _indexes.get(0);
          return nextDoc(query, max);
        }
      }

      // find the max Doc ID
      int max = Collections.max(_indexes);
      return nextDoc(query, max - 1);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return null;

  }

  private ArrayList<Integer> GetWordIndexes(String cleanedToken, int jobid)
      throws UnsupportedEncodingException {
    Integer termCode = _dictionary.get(cleanedToken);

    ArrayList<Integer> skipPointerList = _skipPointersDecoded.get(termCode);
    ArrayList<Integer> postingList = decodeList(_invertedList.get(termCode));
    int offset = 0;
    int skipPointerListSize = skipPointerList.size();
    if (skipPointerListSize > 1 && jobid > 0) {
      for (int i = 0; i < skipPointerListSize - 1; i++) {
        // for(Integer data : skipPointerList){
        if (postingList.get(skipPointerList.get(i)) == jobid) {
          offset = skipPointerList.get(i);
          break;
        }
      }
    }

    int lenOffset = offset + 1;

    int len = postingList.get(lenOffset);
    ArrayList<Integer> arr = new ArrayList<Integer>(
        postingList.subList(offset + 2, offset + 2 + len));
    for (int i = 1; i < arr.size(); i++) {
      arr.set(i, arr.get(i) + arr.get(i - 1));
    }
    return arr;

  }

  private int Next(String t, int current)
      throws IOException, ClassNotFoundException, UnsupportedEncodingException {
    if (!_dictionary.containsKey(t)) {
      return -1;
    }

    Integer termCode = _dictionary.get(t);

    if (!_invertedListDecoded.containsKey(termCode)) {

      long startTime = System.currentTimeMillis();
      _invertedList.putAll(loadPostingChunk(termCode));

      long stopTime = System.currentTimeMillis();
      long elapsedTime = stopTime - startTime;
      System.out.println(t + " not in loaded InvertList.  Loaded new list in "
          + String.format("%d min, %d sec", TimeUnit.MILLISECONDS.toMinutes(elapsedTime),
              TimeUnit.MILLISECONDS.toSeconds(elapsedTime)
                  - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(elapsedTime))));
      _invertedListDecoded.put(termCode, decodeList(_invertedList.get(termCode)));
      _skipPointersDecoded.put(termCode, decodeList(_skipPointers.get(termCode)));
    }

    ArrayList<Integer> skipPointerList = _skipPointersDecoded.get(termCode);
    ArrayList<Integer> postingList = _invertedListDecoded.get(termCode);

    int skipListSize = skipPointerList.size() - 1;
    if ((skipListSize == 0) || postingList.get(skipPointerList.get(skipListSize - 1)) <= current) {
      return -1;
    }

    int firstValue = postingList.get(skipPointerList.get(0));
    if (firstValue > current) {
      return firstValue;
    }

    return postingList
        .get(skipPointerList.get(BinarySearch(termCode, 0, skipListSize - 1, current)));
  }

  private int BinarySearch(Integer t, int low, int high, int current)
      throws UnsupportedEncodingException {
    ArrayList<Integer> skipPointerList = _skipPointersDecoded.get(t);
    ArrayList<Integer> postingList = _invertedListDecoded.get(t);
    while (high - low > 1) {
      int mid = (high + low) / 2;
      if (postingList.get(skipPointerList.get(mid)) <= current) {
        low = mid;
      } else {
        high = mid;
      }
    }
    return high;
  }

  @Override
  public int corpusDocFrequencyByTerm(String term) {
    return 0;
  }

  public int getTotalTermsByDoc(int jobid) {
    return _totalTermsByDocFrequency.containsKey(jobid) ? _totalTermsByDocFrequency.get(jobid) : 0;
  }

  @Override
  public int corpusTermFrequency(String term) {
    return _dictionary.containsKey(term) ? _termCorpusFrequency.get(_dictionary.get(term)) : 0;
  }

  public static ArrayList<Byte> encode(ArrayList<Integer> nums) throws IOException {

    ArrayList<Byte> b = new ArrayList<>();
    for (Integer num : nums) {
      String finalHexStr = "";
      String hexStr = null;
      if (num >= 0) {
        StringBuilder binaryRep = new StringBuilder(Integer.toBinaryString(num));
        if (num <= 127) {
          while (binaryRep.length() < 7) {
            binaryRep.insert(0, "0");
          }
          binaryRep.insert(0, "1");
          int decimal = Integer.parseInt(binaryRep.toString(), 2);
          hexStr = Integer.toString(decimal, 16);
          finalHexStr = hexStr;
          // byteResult.write(hexStr.toUpperCase().getBytes(Charset.forName("UTF-8")));
        } else {
          ArrayList<String> numbers = new ArrayList<>();
          String temp = binaryRep.toString();
          // System.out.println(temp);
          int numberOfTerms = temp.length() / 7 + 1;
          int now = temp.length();
          while (numberOfTerms > 0) {
            if (now - 7 >= 0) {
              numbers.add(temp.substring(now - 7, now));
              // System.out.println(temp.substring(now - 7, now) +
              // "last");
              now -= 7;
            } else if (now > 0) {
              numbers.add(temp.substring(0, now));
              // System.out.println(temp.substring(0, now));
            }
            numberOfTerms--;
          }
          // System.out.println(numbers);
          numbers.set(0, "1" + numbers.get(0));
          for (int i = 1; i < numbers.size(); i++) {
            {
              if (numbers.get(i).length() == 7) {
                numbers.set(i, "0" + numbers.get(i));
              } else if (numbers.get(i).length() < 7) {
                String n = numbers.get(i);
                while (n.length() < 8) {
                  n = "0" + n;
                }
                numbers.set(i, n);
              }
            }
          }
          Collections.reverse(numbers);
          // System.out.println(numbers);
          for (String item : numbers) {
            int decimal = Integer.parseInt(item.toString(), 2);
            hexStr = Integer.toString(decimal, 16);
            if (hexStr.length() < 2) {
              hexStr = "0" + hexStr;
            }
            finalHexStr = finalHexStr + hexStr;
          }
          // System.out.println(finalHexStr);
        }
      }
      int len = finalHexStr.length();
      byte[] data = new byte[len / 2];
      for (int i = 0; i < len; i += 2) {
        data[i / 2] = (byte) ((Character.digit(finalHexStr.charAt(i), 16) << 4)
            + Character.digit(finalHexStr.charAt(i + 1), 16));
      }
      for (byte item : data) {
        b.add(item);
      }
    }
    return b;
  }

  public static ArrayList<Integer> decodeList(ArrayList<Byte> input) throws NumberFormatException {

    String Binary = "";
    ArrayList<Integer> Numbers = new ArrayList<>();
    for (byte b : input) {
      String binaryByte = (String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ',
          '0'));
      if (binaryByte.charAt(0) == '0') {
        Binary = Binary + binaryByte.substring(1);
      } else if (binaryByte.charAt(0) == '1') {
        Binary = Binary + binaryByte.substring(1);
        Numbers.add(Integer.parseInt(Binary, 2));
        Binary = "";
      }
    }
    return Numbers;
  }

  /**
   * @CS2580: Implement this to work with your RankerFavorite.
   */
  @Override
  public int documentTermFrequency(String term, int jobid) {
    int len = 0;
    if (_dictionary.containsKey(term)) {
      Integer termCode = _dictionary.get(term);
      ArrayList<Integer> skipPointerList = _skipPointersDecoded.get(termCode);
      ArrayList<Integer> postingList = _invertedListDecoded.get(termCode);
      int offset = 0;
      int skipPointerListSize = skipPointerList.size();
      if (skipPointerListSize > 1 && jobid > 0) {
        int level = -1;
        for (int i = 0; i < skipPointerListSize - 1; i++) {
          // for(Integer data : skipPointerList){
          if (postingList.get(skipPointerList.get(i)) == jobid) {
            offset = skipPointerList.get(i);
            break;
          }
          level++;
        }
      }

      int lenOffset = offset + 1;
      len = postingList.get(lenOffset);
    }
    return len;
  }
}
