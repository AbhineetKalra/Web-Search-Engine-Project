import bs4
import functools
import json
import multiprocessing as mp
import pathlib as pl
import re, time, sys, copy
import requests
import random
from contextlib import closing
import urllib2

BASE_URL = 'http://www.linkup.com/'
USER_AGENT = {'user-agent': 'Googlebot'}

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
	"Accept-Encoding":"gzip, deflate, sdch, br","Accept-Language":"en-US,en;q=0.8"
	, "Host":"www.linkup.com"}
	#proxies=proxy, 
	r = requests.get(url,headers=headers)
	sleepTime = random.randint(2,30)
	#time.sleep(sleepTime)
	s = requests.session()
	s.cookies.clear()
	return r

def create_request2(url):
	url = 'https://www.google.com/search?q=python'
	headers = {}
	headers['User-Agent'] = "Mozilla/5.0 (X11; Linux i686) AppleWebKit/537.17 (KHTML, like Gecko) Chrome/24.0.1312.27 Safari/537.17"
	req = urllib.request.Request(url, headers = headers)
	response = urllib.request.urlopen(req)
	print response
	return response.getcode(), response.read()



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
		main_section = soup.find('section', {'class': 'organic-content'})
		links = [x['href'] for x in main_section.findAll('a')]
	except:
		return None

def scrapeJobPage(url, isSecondAttempt = False):
	out = {}
	try:
		r = create_request(url)
		#r.status_code, r.text = create_request2(url)
		if r.status_code != 200:
			print 'Error fetching Info'
			'''if not isSecondAttempt:
				out = scrapeJobPage(url,True)
				return out
			else:
				print 'Error fetching from url', url'''

		soup = bs4.BeautifulSoup(r.text,"lxml")
		main_section = soup.find('section', {'class': 'main-content'})
		jobTitle = main_section.find('h1', {'itemprop': 'title'}).text
		side_bar = soup.find('section', {'id': 'sidebar-left'})
		company = side_bar.find('a', {'id': 'company-name'})
	  	companyName = company.text
	 	companyURL = company['href']
	  	address = side_bar.find('div', {'itemprop': 'address'})
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
	  	zipCode = address.find('span', {'itemprop': 'postalCode'})
	  	if zipCode is not None:
	  		zipCode = zipCode.text.encode("ascii","replace")
	  	else:
	  		zipCode = ''
	  	jobCategory = side_bar.find('div', {'itemprop': 'occupationalCategory'})
	  	if jobCategory is not None:
	  		jobCategory = jobCategory.find('a').text.encode("ascii","replace")
	  	else:
	  		jobCategory = ''
	  	addedDate = side_bar.find('div', {'itemprop': 'datePosted'}).text
	  	jobURL = main_section.find('a',{'id':'apply-button-top'})['href']
	  	jobDescription = main_section.find('section',{'id':'job-description-text'})
	  	desc = '. '.join(p.text for p in jobDescription.select('p'))
	  	out['title'] = jobTitle.encode("ascii","replace")
	  	out['url'] = jobURL.encode("ascii","replace")
	  	out['companyName'] = companyName.encode("ascii","replace")
	  	out['companyURL'] = companyURL.encode("ascii","replace")
	  	out['city'] = city
	  	out['state'] = state
	  	out['zipCode'] = zipCode
	  	out['category'] = jobCategory.encode("ascii","replace")
	  	out['date'] = addedDate.encode("ascii","replace")
	  	out['desc'] = desc.encode("ascii","replace")
	  	#print jobTitle,jobURL, companyName, companyURL, city, state, zipCode, jobCategory, addedDate, desc.replace(',',' ')
	  	return out
	except:
		return {}

def getCategoryJobLinks(category):
	category = category.replace('\n','')
	print 'Scraping jobs for :', category
	jobLinks = []
	totalPages = 2
	for pagenum in xrange(3,totalPages):
		categoryURL = 'http://www.linkup.com/b/'+category.lower().replace('&','AND').replace(' ','-').replace('\n','')+'-jobs.html?page='+str(pagenum)+'&sort=d'
		print categoryURL
		jobs = get_jobs_links(categoryURL);
		print jobs
		if jobs is not None:
			jobLinks.extend(jobs)
	print len(jobLinks), ' jobs found in ', category
	print 'Scraping jobs..'
	i = 1
	jobs = []
	for jobURL in jobLinks:
		job = scrapeJobPage(jobURL)
		if len(job) > 0:
			jobs.append(copy.deepcopy(job))
		sys.stdout.write("\rScrapped : {0} / {1}".format(i,len(jobLinks)))
		sys.stdout.flush()
		time.sleep(3)
		i += 1
	
	file_path = pl.Path('linkup') / category
	with file_path.open('w', encoding="utf-8") as f:
		f.write(unicode(dump_json(jobs,ensure_ascii=False)))



def main():
	with open('categories.txt') as f:
		categories = f.readlines()
	
	'''with closing(mp.Pool(processes=1)) as pool:
		pool.map(getCategoryJobLinks, categories)
		pool.terminate()'''

	for category in categories[:1]:
		getCategoryJobLinks(category)


	print 'Scrape Completed !'
	
if __name__ == "__main__":
    main()

