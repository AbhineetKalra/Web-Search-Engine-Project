import urllib.request as request
import requests
import json
import csv

#http://api.glassdoor.com/api/api.htm?t.p=110771&t.k=bdgvwfbqopS&userip=72.88.193.194&format=json&v=1&action=employers&q=fti%20consulting

baseurl = 'http://api.glassdoor.com/api/api.htm'
header = {'User-Agent": "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.81 Safari/537.36'}

fw = csv.writer(open("companies.csv", "w", newline=''))
fw.writerow(["scraped_name", "name", "website", "industry", "squareLogo", "overallRating", "cultureAndValuesRating", "seniorLeadershipRating", "compensationAndBenefitsRating", "careerOpportunitiesRating", "workLifeBalanceRating"])

fr = open('unique.txt', 'r')
for line in fr:
    #print(line)
    line = line.strip()
    if(line):
        glassdoor_params = {
            "v": "1",
            "format": "json",
            "t.p": "110771",
            "t.k": "bdgvwfbqopS",
            "userip": "72.88.193.194",
            "action": "employers",
            "q": line,
        }

        response = requests.get(baseurl, params=glassdoor_params, 
                            headers={
                               "User-Agent": "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2357.81 Safari/537.36"
                                })
        #print(response.status_code)
        #print(response.json())
        if(response.status_code == 200):
            try:
                x = response.json()["response"]["employers"][0]
                print(x["name"])
                fw.writerow([line, x["name"], x["website"], x["industry"], x["squareLogo"], x["overallRating"], x["cultureAndValuesRating"], x["seniorLeadershipRating"], x["compensationAndBenefitsRating"], x["careerOpportunitiesRating"], x["workLifeBalanceRating"]])
            except:
                #code that runs when error happens
                continue



                

#json.loads(request.urlopen("http://ip.jsontest.com/").read().decode('utf-8'))['ip']




#print(response.status_code)
#print(response.json())

#x = json.loads(x)

#print(x)



