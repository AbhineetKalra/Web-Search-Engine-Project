package edu.nyu.cs.cs2580;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

class Job implements Serializable {
    private static final long serialVersionUID = -539495106357836976L;

    // id title url datePosted hiringOrganization_organizationName
    // jobLocation_address_countryName jobLocation_address_region
    // jobLocation_geo_latitude jobLocation_geo_longitude jobDescription

    // scraped_name name website industry squareLogo overallRating
    // cultureAndValuesRating seniorLeadershipRating
    // compensationAndBenefitsRating careerOpportunitiesRating
    // workLifeBalanceRating

    int _jobid;

    // Basic information for display
    String _title = "";
    String _url = "";
    String _desc = "";
    String _companyName = "";
    String _city = "";
    String _state = "";
    String _country = "";
    Date _postedDate;
    String _latitude = "";
    String _longitude = "";
    DateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

    String _companyIndustry;
    String _companyLogo;
    double _overallRating;
    double _cultureValuesRating;
    double _seniorLeadershipRating;
    double _compensationBenefitsRating;
    double _careerOpportunitiesRating;
    double _workLifeBalanceRating;
    double _totalScore;

    public Job(int docid) {
	   _jobid = docid;
    }

    public Job(int docid, String title, String desc, String companyName, String postedDate,String country, String state,
	    String city, String url, String lat, String longi) {
	_jobid = docid;
	_title = title;
	_desc = desc;
	_companyName = companyName;
	_city = city;
	_state = state;
	_country = country;
	_latitude = lat;
	_longitude = longi;
	_url = url;
	try {
	    _postedDate = format.parse(postedDate);
	} catch (ParseException e) {
	    _postedDate = null;
	}
    }

    public String getTitle() {
	return _title;
    }

    public void setTitle(String title) {
	this._title = title;
    }

    public String getUrl() {
	return _url;
    }

    public void setUrl(String url) {
	this._url = url;
    }

    public int getJobID() {
	return _jobid;
    }

}
