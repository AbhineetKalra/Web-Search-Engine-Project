import bs4
import functools
import json
import multiprocessing as mp
import pathlib as pl
import re, time, sys, copy
import requests
import random, datetime
from contextlib import closing
import csv
from geopy.geocoders import Nominatim

geolocator = Nominatim()

locationDict = {}

BASE_URL = 'https://www.my.jobs'
USER_AGENT = {'user-agent': 'Googlebot'}
filename = "data/myjob-"+datetime.datetime.now().strftime("%Y-%m-%d-%H-%M")+".csv"
fw = csv.writer(open(filename, "wb+"),delimiter='\t')

locFilename = "locations.csv"
fwLoc = csv.writer(open(locFilename, "a"),delimiter='\t')

with open(locFilename, 'rb') as csvfile:
	spamreader = csv.reader(csvfile, delimiter='\t')
	for row in spamreader:
		if row:
			locData = {"lat":row[1],"lng":row[2]}
		  	locationDict[row[0].lower()] = locData

dump_json = functools.partial(json.dumps,
                              indent=4,
                              ensure_ascii=False,
                              sort_keys=True)


def LoadUserAgents(uafile):
    """
    uafile : string
        path to text file of user agents, one per line
    """
    uas = []
    with open(uafile, 'rb') as uaf:
        for ua in uaf.readlines():
            if ua:
                uas.append(ua.strip()[1:-1-1])
    random.shuffle(uas)
    return uas

# load the user agents, in random order
user_agents = LoadUserAgents(uafile="user_agents.txt")

from http import cookiejar  # Python 2: import cookielib as cookiejar
class BlockAll(cookiejar.CookiePolicy):
	return_ok = set_ok = domain_return_ok = path_return_ok = lambda self, *args, **kwargs: False
	netscape = True
	rfc2965 = hide_cookie2 = False

s = requests.Session()
s.cookies.set_policy(BlockAll())

def make_absolute(url):
    return "{}{}".format(BASE_URL, url)

def create_request(url):
	#proxy={'http': 'http://137.135.166.225:8147'}
	ua = random.choice(user_agents)
	headers = {"Connection" : "keep-alive", "User-Agent" : ua, "Accept" : "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
	"Accept-Encoding":"gzip, deflate, sdch, br","Accept-Language":"en-US,en;q=0.8", "X-Requested-With":"XMLHttpRequest"}
	#proxies=proxy, 
	r = requests.get(url,headers=headers)
	sleepTime = random.randint(1,5)
	time.sleep(sleepTime)
	s = requests.session()
	s.cookies.clear()
	return r

def get_jobs_links(url, isSecondAttempt = False):
	try:
		r = create_request(url)
		#r.status_code, r.text = create_request2(url)
		if r.status_code != 200:
			#raise requests.exceptions.RequestException("Request error: main('{}')".format(url))
			print 'Error Fetching Information', r.status_code
			return
			if not isSecondAttempt:
				links = get_jobs_links(url,True)
				return links
			else:
				print 'Error fetching from url', url
				return None
		soup = bs4.BeautifulSoup(r.text,"lxml")
		main_section = soup.find('ul', {'class': 'default_jobListing'})
		anchors =  main_section.findAll('a')
		links = [x['href'] for x in anchors]
		return links
	except:
		return None

def scrapeJobPage(url, isSecondAttempt = False):
	out = {}
	try:
		r = create_request(make_absolute(url))
		if r.status_code != 200:
			print 'Error fetching Info :', r.status_code
			'''if not isSecondAttempt:
				out = scrapeJobPage(url,True)
				return out
			else:
				print 'Error fetching from url', url'''

		soup = bs4.BeautifulSoup(r.text,"lxml")
		main_section = soup.find('div', {'id': 'direct_innerContainer'})
		jobTitle = main_section.find('span', {'itemprop': 'title'}).text
	  	companyName = main_section.find('span', {'itemprop': 'name'}).text
	  	address = main_section.find('span', {'itemprop': 'address'})
	  	city = address.find('span', {'itemprop': 'addressLocality'})
	  	if city is not None:
	  		city = city.text.encode("ascii","replace")
	  	else:
	  		city = ''
	  	state = address.find('span', {'itemprop': 'addressRegion'})
	  	if state is not None:
	  		state = state.text.encode("ascii","replace")
	  	else:
	  		state = ''

	  	country = address.find('span', {'itemprop': 'addressCountry'})
	  	if country is not None and not country.text:
	  		country = country.text.encode("ascii","replace")
	  	else:
	  		country = 'United States'
	  	
	  	addedDate = main_section.find('meta', {'itemprop': 'datePosted'})
	  	if addedDate is not None:
	  		addedDate = addedDate['content'].encode("ascii","replace")
	  		addedDate = addedDate.split('T')[0]
	  	else:
	  		addedDate = ''
	  	applySection = main_section.find('div',{'id':'direct_applyButtonBottom'})
	  	jobURL = applySection.find('a')['href']
	  	jobDescription = main_section.find('div',{'id':'direct_jobDescriptionText'})
	  	desc = '. '.join(p.text for p in jobDescription.select('p'))
	  	desc = desc.replace('\n','')
	  	
	  	#Location not in dictionary, find from API
	  	locationKey = (city + ", " +state + ", " + country).lower()
	  	if locationKey not in locationDict:
	  		# looking for location
	  		location = geolocator.geocode(locationKey)
	  		if location is not None:
		  		fwLoc.writerow([locationKey,location.latitude,location.longitude]);
		  		locData = {"lat":location.latitude,"lng":location.longitude}
		  		locationDict[locationKey] = locData
		  	else:
		  		locData = None
	  	else:
	  		#print 'Loc found in master data'
	  		locData = locationDict[locationKey]
	  	
	  	lat = 0
	  	lng = 0
	  	if locData is not None:
	  		lat = locData['lat']
	  		lng = locData['lng']
	  	return [0,jobTitle.encode("ascii","replace"),jobURL.encode("ascii","replace"),
	  	addedDate.encode("ascii","replace"),
	  	companyName.encode("ascii","replace"),country, state,
	  	city,lat,lng,desc.encode("ascii","replace")]
	except:
		print 'exception'
		return []

def getCategoryJobLinks(pagenum):
	print 'Scraping page number for :', (pagenum + 1)
	jobLinks = []
	totalPages = 2
	for pagenum in xrange(1,totalPages):
		categoryURL = 'https://www.my.jobs/jobs/ajax/joblisting/?num_items=250&offset='+str(25*(pagenum+1))
		jobs = get_jobs_links(categoryURL);
		if jobs is not None:
			jobLinks.extend(jobs)
	print 'Scraping jobs..'
	i = 1
	jobs = []
	for jobURL in jobLinks:
		job = scrapeJobPage(jobURL)
		if len(job) > 0:
			fw.writerow(job)
		sys.stdout.write("\rScrapped : {0} / {1}".format(i,len(jobLinks)))
		sys.stdout.flush()
		time.sleep(3)
		i += 1
	print 'Wrote page :', (pagenum + 1)
		


def main():
	for i in xrange(0,100):
		getCategoryJobLinks(i)
	print 
	print 'Scrape Completed !'
	
if __name__ == "__main__":
    main()

