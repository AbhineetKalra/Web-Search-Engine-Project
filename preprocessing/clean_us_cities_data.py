import csv
lst = set()
fw = csv.writer(open('../data/locations.txt', "wb+"),delimiter='\t')
with open('US_locations.txt', 'r') as fr:
	reader = csv.reader(fr, delimiter='\t')
	for row in reader:
		lst.add(row[2])
		lst.add(row[3])
		lst.add(row[4])
		lst.add(row[5])
lst = filter(None, lst)
for item in lst:
	fw.writerow([item])

# adding some missing values
# extend it more to include all
fw.writerow(['NYC'])
fw.writerow(['new york'])

fw2 = csv.writer(open('../web-crawlers/locations.csv', "wb+"),delimiter='\t')
geoData = []
with open('US_locations.txt', 'r') as fr:
	reader = csv.reader(fr, delimiter='\t')
	for row in reader:
		#temp = []
		#temp.append(row[2] + ", "  + row[3] + ", United States")
		#temp.append(row[9])
		#temp.append(row[10])
		key = row[2] + ", "  + row[3] + ", United States";
		fw2.writerow([key,row[9],row[10]])

#for item in geoData:
#	fw2.writerow([item])