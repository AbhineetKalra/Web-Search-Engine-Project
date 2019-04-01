package edu.nyu.cs.cs2580;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.text.DateFormat;
import java.util.Locale;
import java.util.HashMap;

import edu.nyu.cs.cs2580.QueryHandler.CgiArguments;
import edu.nyu.cs.cs2580.SearchEngine.Options;

/**
 * @CS2580: Implement this class for HW2 based on a refactoring of your favorite
 *          Ranker (except RankerPhrase) from HW1. The new Ranker should no
 *          longer rely on the instructors' {@link IndexerFullScan}, instead it
 *          should use one of your more efficient implementations.
 */
public class RankerFavorite extends Ranker {

    public RankerFavorite(Options options, CgiArguments arguments, Indexer indexer) {
	super(options, arguments, indexer);
		System.out.println("Using Ranker: " + this.getClass().getSimpleName());
    }

    @Override
    public Vector<ScoredDocument> runQuery(Query query, int numResults) {

		QueryLocation _processedQuery = (QueryLocation) query;
		System.out.println("Location " + _processedQuery.location_tokens);
		System.out.println("Query " + _processedQuery._query);
		long startTime = System.currentTimeMillis();
		Vector<ScoredDocument> rankVector = new Vector<ScoredDocument>();
		int N = _indexer.numJobs();
		Job job = null;
		ReadLogs(query._query);
		int jobid = -1;
		try {
		    while ((job = _indexer.nextDoc(query, jobid)) != null) {
				// System.out.println("Sent : " + docid + " -> " + doc._docid);
				ScoredDocument doc = scoreDocument(_processedQuery, job._jobid);
				if(doc != null){
					rankVector.add(doc);
				}
				jobid = job._jobid;
		    }
		} catch (Exception ex) {
		    ex.printStackTrace();
		}

		Vector<ScoredDocument> results = new Vector<ScoredDocument>();
		Vector<ScoredDocument> finalResults = new Vector<ScoredDocument>();
		if (rankVector.size() > 0) {
		    N = rankVector.size();

		    double weight_clickLog = 0.15, weight_distance = 0.1, 
		    	weight_time = 0.1, weight_ranking = 0.1 , 
		    	weight_relevance = 0.15;
		    switch (_processedQuery._sortType.toUpperCase())
    		{
    			case "DISTANCE":
    				weight_distance = 0.9;
    				weight_clickLog = 0.0025;
    				weight_time = 0.0025;
    				//weight_ranking = 0.025;
    				weight_relevance = 0.050;
    				break;

    			case "TIME":
    				weight_distance = 0.15;
    				weight_clickLog = 0.0;
    				weight_time = 0.8;
    				//weight_ranking = 0.05;
    				weight_relevance = 0.05;
    				break;

    			case "RANKING":
    				weight_ranking = 0.7;
    				weight_clickLog = 0.1;
    				weight_distance = 0.0;
    				weight_time = 0.05;
    				weight_relevance = 0.05;
    				break;
    		}

		    // Data to normalize the scores
		    ScoredDocument scoredDoc = null;
		    double qlMax = Double.MIN_VALUE;		    
		    double locMax = Double.MIN_VALUE;
		    double timeMax = Double.MIN_VALUE;
		    double clickMax = Double.MIN_VALUE;
		    double ratingMax = Double.MIN_VALUE;
		    double qlMin = Double.MAX_VALUE;
		    double locMin = Double.MAX_VALUE;
		    double timeMin = Double.MAX_VALUE;
		    double clickMin = Double.MAX_VALUE;
		    double ratingMin = Double.MAX_VALUE;
		    
		    for (int i = 0; i < N; ++i) {
				scoredDoc = rankVector.get(i);
				qlMax = (scoredDoc._qlike > qlMax) ? scoredDoc._qlike : qlMax;
				qlMin = (scoredDoc._qlike < qlMin) ? scoredDoc._qlike : qlMin;
				locMax = (scoredDoc._locDist > locMax) ? scoredDoc._locDist : locMax;
				locMin = (scoredDoc._locDist < locMin) ? scoredDoc._locDist : locMin;
				timeMax = (scoredDoc._timeDiff > timeMax) ? scoredDoc._timeDiff : timeMax;
				timeMin = (scoredDoc._timeDiff < timeMin) ? scoredDoc._timeDiff : timeMin;
				clickMax = (scoredDoc._clickLog > clickMax) ? scoredDoc._clickLog : clickMax;
				clickMin = (scoredDoc._clickLog < clickMin) ? scoredDoc._clickLog : clickMin;
				ratingMax = (scoredDoc._rating > ratingMax) ? scoredDoc._rating : ratingMax;
				ratingMin = (scoredDoc._rating < ratingMin) ? scoredDoc._rating : ratingMin;
		    }
		    
		    double qlDiff = qlMax - qlMin;
		    double qlScore = 0;
		    double locDiff = locMax - locMin;
		    double locScore = 0;
		    double timeDiff = timeMax - timeMin;
		    double timeScore = 0;
		    double clickDiff = clickMax - clickMin;
		    double clickScore = 0;
		    double ratingDiff = ratingMax - ratingMin;
		    double ratingScore = 0;
		    
		    for (int i = 0; i < N; ++i) {
				scoredDoc = rankVector.get(i);		    
		    	// Normalize the score
				qlScore = qlDiff == 0 ? 1 : (scoredDoc._qlike - qlMin) / qlDiff;
				locScore = locDiff == 0 ? 1 : (scoredDoc._locDist - locMin) / locDiff;
				timeScore = timeDiff == 0 ? 1 : (scoredDoc._timeDiff - timeMin) / timeDiff;
				clickScore = clickDiff == 0 ? 1 : (scoredDoc._clickLog - clickMin) / clickDiff;
				ratingScore = ratingDiff == 0 ? 1 : (scoredDoc._rating - ratingMin) / ratingDiff;
				
				// near location and less time are better
				locScore = 1.01 - locScore;
				timeScore = 1.01 - timeScore;

				double totalScore = (weight_relevance * qlScore) 
				 + (weight_distance *locScore)
				 + (weight_time *timeScore)
				 + (weight_ranking * ratingScore)
				 + (weight_clickLog * clickScore);
				
				results.add(new ScoredDocument(scoredDoc.getDocument(),
					(weight_relevance * qlScore),
					(weight_distance *locScore),
					(weight_time *timeScore),
					(weight_ranking * ratingScore),
					(weight_clickLog * clickScore),
				 	totalScore));

				results.add(new ScoredDocument(scoredDoc.getDocument(), totalScore));
			}

			// Sort 
			Collections.sort(results, Collections.reverseOrder());

			for (int i = 0; i < results.size() && i < numResults; ++i) {
				scoredDoc = results.get(i);
				//System.out.println("Title :" + scoredDoc.getDocument().getTitle());
				//System.out.println("Relevance Score :" + scoredDoc._qlike);
				//System.out.println("Location Score :" + scoredDoc._locDist);
				//System.out.println("Time Score :" + scoredDoc._timeDiff);
				//System.out.println("Click Score :" + scoredDoc._clickLog);
				//System.out.println("Rating Score :" + scoredDoc._rating);
				//System.out.println("Total Score :" + scoredDoc._totalScore);
				//System.out.println("----------------------------");
				finalResults.add(scoredDoc);
	    	}

		}
		long stopTime = System.currentTimeMillis();
		long elapsedTime = stopTime - startTime;
		System.out.println("Processed query in " + String.format("%d min, %d sec",
			TimeUnit.MILLISECONDS.toMinutes(elapsedTime), TimeUnit.MILLISECONDS.toSeconds(elapsedTime)
				- TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(elapsedTime))));
		return finalResults;
    }

    private ScoredDocument scoreDocument(QueryLocation query, int did) {

		Job doc = _indexer.getDoc(did);
		Job _job = mapCompanyInfo(doc);
		double scoreMaxLikelihood = 1.0;
		double locDistance = 0;
		double dateDiff = 0;
		double logScore = 0;
		try{
			int documentTotalTermFrequency = ((IndexerInvertedCompressed) _indexer).getTotalTermsByDoc(did);

			long corpusTotalTermFrequency = _indexer.totalTermFrequency();
			// System.out.println(did + " " + documentTotalTermFrequency);
			
			for (String queryToken : query._tokens) {

			    double _lambda = 0.5;
			    int corpusQueryTermFrequency = _indexer.corpusTermFrequency(queryToken);
			    int documentQueryTermFrequency = _indexer.documentTermFrequency(queryToken, did);

			    double documentProbability = ((double) documentQueryTermFrequency) / ((double) documentTotalTermFrequency);
			    double corpusProbability = ((double) corpusQueryTermFrequency) / ((double) corpusTotalTermFrequency);

			    double smoothingProbability = ((1 - _lambda) * documentProbability) + (_lambda * corpusProbability);
			    scoreMaxLikelihood += Math.log10(smoothingProbability);
			}
			//System.out.println("ql:" + scoreMaxLikelihood);


			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

			Date currentDate = new Date();
			dateDiff = getDayCount(doc._postedDate, currentDate);
			//System.out.println("date diff:" + dateDiff);
			
			//System.out.println("rating:" + _job._overallRating);
			//System.out.println("latitude:" + _job._latitude);
			//System.out.println("longitude:" + _job._longitude);
			//System.out.println("query latitude:" + query.locLatitude);
			//System.out.println("query longitude:" + query.locLongitude);
			try {
				double _lat = Double.parseDouble(_job._latitude);
				double _lng = Double.parseDouble(_job._longitude);
				locDistance = distance(query.locLatitude, query.locLongitude, _lat, _lng);
			} catch (Exception ex) {
				//TODO
			}

			// If distance is more than 200, return null
			if (locDistance > 200) {
				return null;
			}

			//System.out.println(_job.getTitle()  + " distance:" + locDistance);

			if(_clickLogs.containsKey(_job._url.toLowerCase())){ 
				logScore = _clickLogs.get(_job._url.toLowerCase());
			}

			//System.out.println("Log Score:" + logScore);
			//System.out.println("-----------------------------");
			
		}
		catch(Exception ex){
			//ex.printStackTrace();
		}
		return new ScoredDocument(_job, scoreMaxLikelihood, locDistance, dateDiff, _job._overallRating, logScore);

    }


    private HashMap<String,Double> _clickLogs = new HashMap<String,Double>();
    private void ReadLogs(String query){
    	String csvFile = "data/clickLog.csv";
    	String line = "";
		BufferedReader br = null;
		try {

		    br = new BufferedReader(new FileReader(csvFile));
		    while ((line = br.readLine()) != null) {
				//String[] company = line.split(csvSplitBy);
				String[] logs = line.toLowerCase().split("\t");
				String logQuery = logs[0];
				//System.out.println(logQuery + " " + query);
				if(logQuery.equalsIgnoreCase(query)){
					String _url = logs[1];
					if(_clickLogs.containsKey(_url)){
						_clickLogs.put(_url, _clickLogs.get(_url) + 1);
					}else{
						_clickLogs.put(_url,1.0);
					}
					//System.out.println(line);
				}
			}
		} catch (IOException e) {
		    //e.printStackTrace();
		} finally {
		    try {
				br.close();
		    } catch (IOException e) {
				//e.printStackTrace();
		    }
		}
    }
    private double distance(double lat1, double lon1, double lat2, double lon2) {
		double theta = lon1 - lon2;
		double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2))
			+ Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
		dist = Math.acos(dist);
		dist = rad2deg(dist);
		dist = dist * 60 * 1.1515;
		return (dist);
    }

    private double deg2rad(double deg) {
		return (deg * Math.PI / 180.0);
    }

    private double rad2deg(double rad) {
		return (rad * 180.0 / Math.PI);
    }

    private static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    public static long getDayCount(Date dateStart, Date dateEnd) {
		long diff = -1;
		try {
		    diff = Math.round((dateEnd.getTime() - dateStart.getTime()) / (double) 86400000);
		} catch (Exception e) {
		    System.out.println("date exception");
		}
		return diff;
    }

    public Job mapCompanyInfo(Job job) {
		String csvFile = "preprocessing/companies.csv";
		BufferedReader br = null;
		String line = "";
		String csvSplitBy = ",";

		try {

		    br = new BufferedReader(new FileReader(csvFile));
		    int flag = 0;
		    while ((line = br.readLine()) != null) {
				//String[] company = line.split(csvSplitBy);
				String[] company = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
				// System.out.println(company[0].replace("\"", ""));
				String scraped_name = company[0].replace("\"", "");
				if (scraped_name.equalsIgnoreCase(job._companyName)) {
				    job._companyName = company[1];

				    job._companyLogo = company[4];
				    if(job._companyLogo.isEmpty()){
				    	job._companyLogo = "../preprocessing/empty_organization.png";
				    }
				    try{
				    	job._overallRating = Double.parseDouble(company[5]);
				    }
				    catch(Exception e){
				    	job._overallRating = 0;
				    }

				    try{
				    	job._careerOpportunitiesRating = Double.parseDouble(company[8]);
				    }
				    catch(Exception e){
				    	job._careerOpportunitiesRating = 0;
				    }

				    try{
				    	job._compensationBenefitsRating = Double.parseDouble(company[9]);
				    }
				    catch(Exception e){
				    	job._compensationBenefitsRating = 0;
				    }

				    try{
				    	job._workLifeBalanceRating = Double.parseDouble(company[10]);
				    }
				    catch(Exception e){
				    	job._workLifeBalanceRating = 0;
				    }
				    
				    
				    
				    flag = 1;
				    break;
				}
		    }
		    if (flag != 1) {
				job._companyLogo = "../preprocessing/empty_organization.png";
				job._overallRating = 0;
				job._careerOpportunitiesRating = 0;
				job._compensationBenefitsRating = 0;
				job._workLifeBalanceRating = 0;
		    }
		    //System.out.println(job._companyName + "\t" + job._overallRating + "\t" + job._companyLogo);
		} catch (IOException e) {
		    e.printStackTrace();
		} finally {
		    try {
			br.close();
		    } catch (IOException e) {
			e.printStackTrace();
		    }
		}
		return job;
    }

}
