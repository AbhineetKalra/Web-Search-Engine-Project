import csv
from geopy.geocoders import Nominatim

geolocator = Nominatim()
#location = geolocator.geocode("175 5th Avenue NYC")
#print(location.address)
#print((location.latitude, location.longitude))
#print(location.raw)
tsv_rows = []
with open('geolocations.tsv', 'r') as fr:
	reader = csv.reader(fr, delimiter='\t')
	for row in reader:
		temp = []
		temp.append(row[1])
		temp.append(row[2])
		tsv_rows.append(temp)
#print(tsv_rows)


  
#fields=['address','latitude','longitude']
fields = [None] * 3

with open('geolocations.tsv', 'a') as fa:
	fr = open('addresses.txt', 'r')
	for line in fr:
		line = line.strip()
		print(line)

		location = geolocator.geocode(line)
		print location.latitude , location.longitude
		fields[0] = line
		fields[1] = location.latitude
		fields[2] = location.longitude

		flag = 0
		for temp in tsv_rows:
			if temp[0].__eq__(fields[1]) and temp[1].__eq__(fields[2]):
				flag = 1

		if flag == 0:	
			writer = csv.writer(fa, delimiter='\t')
			writer.writerow(fields)


		
		