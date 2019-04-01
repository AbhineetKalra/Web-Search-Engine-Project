package edu.nyu.cs.cs2580;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.json.simple.JSONObject;

/**
 * Document with score.
 * 
 * @author fdiaz
 * @author congyu
 */
class ScoredDocument implements Comparable<ScoredDocument> {
    private Job _doc;
    public double _qlike;
    public double _locDist;
    public double _timeDiff;
    public double _rating;
    public double _clickLog;
    public double _totalScore;

    DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

    public ScoredDocument(Job doc, double ql, double dist
    	, double time, double rat, double log) {
		_doc = doc;
		_qlike = ql;
		_locDist = dist;
		_timeDiff = time;
		_rating = rat;
		_clickLog = log;
    }

    public ScoredDocument(Job doc, double ql, double dist
    	, double time, double rat, double log, double score) {
		_doc = doc;
		_qlike = ql;
		_locDist = dist;
		_timeDiff = time;
		_rating = rat;
		_clickLog = log;
		_totalScore = score;
    }

    public ScoredDocument(Job doc, double score) {
		_doc = doc;
		//_qlike = ql;
		//_locDist = dist;
		//_timeDiff = time;
		//_rating = rat;
		//_clickLog = log;
		_totalScore = score;
    }

    public double getScore() {
		return _totalScore;
    }

    public Job getDocument() {
		return _doc;
    }

    public void SetTotalScore(double d){
    	this._totalScore = d;
    }

    public String asTextResult() {
		StringBuffer buf = new StringBuffer();
		buf.append(_doc._jobid).append("\t");
		buf.append(_doc.getTitle()).append("\t");
		buf.append(_qlike);
		return buf.toString();
    }

    @SuppressWarnings("unchecked")
    public JSONObject asJsonResult() {
		JSONObject obj = new JSONObject();
		obj.put("job_id", _doc._jobid);
		obj.put("job_title", _doc._title.replace("\"", "\'"));
		obj.put("job_url", _doc._url);
		obj.put("job_description", _doc._desc.replace("\"", "\'"));
		obj.put("job_company_name", _doc._companyName);
		obj.put("job_company_image", _doc._companyLogo);
		obj.put("job_country", _doc._country);
		obj.put("job_city", _doc._city);
		obj.put("job_state", _doc._state);
		obj.put("job_latitude", _doc._latitude);
		obj.put("job_longitude", _doc._longitude);
		obj.put("job_posted_date", df.format(_doc._postedDate));
		obj.put("job_company_rating_overall", _doc._overallRating);
		obj.put("job_company_rating_opportunity", _doc._careerOpportunitiesRating);
		obj.put("job_company_rating_benefits", _doc._compensationBenefitsRating);
		obj.put("job_company_rating_worklife", _doc._workLifeBalanceRating);
		obj.put("job_score", _totalScore);
		return obj;
    }

    /**
     * @CS2580: Student should implement {@code asHtmlResult} for final project.
     */
    public String asHtmlResult() {
		return "";
    }

    @Override
    public int compareTo(ScoredDocument o) {
		if (this._totalScore == o._totalScore) {
		    return 0;
		}
		return (this._totalScore > o._totalScore) ? 1 : -1;
    }
}
