#!/usr/bin/env python
from os import listdir
from os.path import isfile, join
import csv
import json
import persistqueue
import requests
import shutil
import time
import calendar
import math
import datetime
import re
scanpath='upload_covid_monitoring'
processedPath='processed_covid_monitoring'
rows = []
csvFileNames = [f for f in listdir(scanpath) if isfile(join(scanpath, f))]
jobQueue = persistqueue.UniqueAckQ('covid-monitoring')
statesmanUrl = "http://localhost:8080"
date_fields = ['date_of_sample_collection','date_of_isolation','end_date','test_date']
phones = set()
stateWorkflows = {
    "uttarakhand": "c606cfb1-3a43-419b-a6f9-d745270dea80",
    "tamil_nadu": "b2c71733-3da3-4e29-86ee-021565ba87b4",
    "puducherry": "fd20aa74-3210-4761-9e3b-4d6ab43477fb",
    "bihar": "77ee9073-eed9-4fbb-8150-31d96af4a536",
    "maharashtra": "7735772e-523c-45f2-b64d-116489048a2e",
    "delhi": "3efd0e4b-a6cc-4e59-9f88-bb0141a66142",
    "punjab": "933bed6c-e6a6-4de4-9ea8-7a31d64a08dc','11dd4791-472b-454b-8f7a-39a589a6335c",
    "haryana": "94f340e5-76d6-416f-9e95-8b8532e3722d"
}
TEN_DAYS_IN_MS = 10 * 24 * 60 * 60 * 1000
CURRENT_DATE = datetime.date.today()
DAY_START_TIME = (int(datetime.datetime(CURRENT_DATE.year, CURRENT_DATE.month, CURRENT_DATE.day, 0, 0, 0).strftime('%s'))) * 1000
mandatoryFields = ['state','district','patient_name','mobile_number','date_of_isolation','end_date']

def now():
    return calendar.timegm(time.gmtime()) * 1000

def epoch_time(str_time):
    return (int(time.mktime(time.strptime(str_time, "%d/%m/%Y")))) * 1000

def str_date(time_in_epoch):
    return datetime.datetime.fromtimestamp(time_in_epoch / 1000).strftime('%d/%m/%Y')

def day_diff(from_epoch, till_epoch):
    if(till_epoch < from_epoch):
        return 1
    return int (math.ceil( (float)(till_epoch - from_epoch) / 86400000))

def sanitizeAge(row):
    if(row.has_key("age")):
        age = row['age'].strip().lower()
        age = age.split('.')[0].split(" ")[0].split("y")[0]
        row['age'] = age

def trigger_new_workflow(payload,mobileNumber,wfSource):
    for i in range(3):
        r = requests.post(statesmanUrl + '/callbacks/ingress/raw/'+wfSource, data=payload, headers = {'content-type': 'application/json'})
        if r.status_code == 200:
            print('successfully posted data for mobileNumber: ' + mobileNumber)
            return True
        else:
            print(str(i) + ': could not post data for mobileNumber: ' + mobileNumber + ' status:' + str(r.status_code))
    return False


def existing_workflow(phone,state):
    finalFql = """ select eventData.workflowId from statesman where eventData.workflowTemplateId in ('%s') and eventType = 'STATE_CHANGED' and eventData.newState in ('HOME_QUARANTINE','HI_ONBOARD','HOME_ISOLATION')  and eventData.data.mobile_number = '%s' limit 1  """ % (stateWorkflows[state], str(phone))
    #print(finalFql)
    r = requests.post('https://foxtrot.ps1infra.net/foxtrot/v1/fql', data=finalFql, headers = {"Accept": "application/json",'content-type': 'application/json','Authorization':'Bearer '})
    if(r.status_code == 200):
        for row in r.json()['rows']:
            return row['eventData.workflowId']
    return None

def update_or_trigger_new_workflow(w,payload,mobileNumber,wfSource):
    for i in range(3):
        r = requests.get(statesmanUrl + '/v1/housekeeping/debug/workflow/'+w, headers = {'content-type': 'application/json'})
        if(r.status_code != 200):
            print(str(i) + ': could not get data for mobileNumber: ' + mobileNumber + ' workflowId '+ w +  ' status:' + str(r.status_code))
        else:
            workflow = r.json()
            wd = workflow['dataObject']['data']
            if(wd["endTime"] >= payload['body']["endTime"]):
                print("Nothing to update for mobileNumber:" + mobileNumber + ' workflowId:'+ w )
                return True
            else:
                wd["endTime"] = payload['body']["endTime"]
                wd["end_date"] = payload['body']["end_date"]
                wd["maxDays"] = day_diff(workflow['created'],wd["endTime"])
                if(workflow['dataObject']['currentState']['name'] == "END" or workflow['dataObject']['currentState']['name'] == "FORCE_END"):
                    #workflow['dataObject']['currentState']['name'] = "HOME_ISOLATION"
                    #workflow['dataObject']['currentState']['terminal'] = False
                    return trigger_new_workflow(json.dumps(payload),mobileNumber,wfSource)
                else:
                    r = requests.put(statesmanUrl + '/v1/housekeeping/update/workflow',data = json.dumps(workflow) , headers = {'content-type': 'application/json'})
                    if(r.status_code == 200):
                        print("Updated for mobileNumber:" + mobileNumber + ' workflowId:'+ w  )
                        return True
                    else:
                        print(str(i) + ': could not update data for mobileNumber: ' + mobileNumber + ' workflowId:'+ w +  ' status:' + str(r.status_code))
    return False

def sanatizeKey(key):
    key = " ".join(key.split()).lower().strip().replace(' ', '_')
    return re.sub(r'\W+', '', key)


def sanitize_address(convrow):
    if convrow.has_key("address"):
        convrow['address'] = re.sub(r'\W+', ' ', convrow['address'])


for csvFileName in csvFileNames:
    fqFileName=scanpath + '/' + csvFileName
    fqDestFilename=processedPath + '/' + csvFileName
    try:
        with open(fqFileName) as csvfile:
            reader = csv.DictReader(csvfile)
            for row in reader:
                try:
                    convrow = dict((sanatizeKey(k), v.strip().replace('\n', "").replace('\r',"")) for k,v in row.iteritems())
                    if(convrow.has_key("")):
                        del convrow[""]
                    empty_keys = [k for k,v in convrow.iteritems() if len(str(v)) == 0 or v is None or v.lower() == 'none' ]
                    for k in empty_keys:
                        del convrow[k]
                    if(convrow.has_key("date_of_isolation") and not convrow.has_key("end_date")):
                        convrow["end_date"] = str_date(epoch_time(convrow['date_of_isolation']) + TEN_DAYS_IN_MS)
                    invalid_schema = False
                    for f in mandatoryFields:
                        if(not convrow.has_key(f)):
                            print("ERROR: missing field:"+f + " for row:"+ str(row))
                            invalid_schema = True
                            break
                    if(invalid_schema):
                        continue
                    if(convrow.has_key("district")):
                        convrow["district"] = convrow['district'].lower().strip()
                    convrow['state'] = convrow['state'].lower().strip().replace(' ', '_')
                    convrow['flow'] =  convrow['flow'].upper().strip()
                    flow = convrow['flow'].lower()
                    convrow['mobile_number'] = convrow['mobile_number'].split(",")[0].strip()
                    if(not stateWorkflows.has_key(convrow['state'])):
                        print("Error: Inavlid state mentioned skiping row:" + str(row))
                        continue
                    if(convrow['mobile_number'] in phones):
                        print("INFO: Already processed the mobile_number:" + convrow['mobile_number'])
                        continue
                    phones.add(convrow['mobile_number'])
                    for date_field in date_fields:
                        if(convrow.has_key(date_field)):
                            convrow[date_field] = convrow[date_field].replace('.',r'/').replace('-',r'/')
                            try:
                                epoch_time(convrow[date_field])
                            except:
                                print("ERROR: invalid date format for field:"+date_field + " for row:"+ str(row))
                                invalid_schema = True
                                break
                    if(invalid_schema):
                        continue
                    sanitizeAge(convrow)
                    sanitize_address(convrow)
                    convrow['wfSource'] = convrow['state'] + '_'+flow+'_monitoring_csv'
                    endTime = epoch_time(convrow['end_date'])
                    convrow['maxDays'] = day_diff(now(),endTime)
                    convrow['endTime'] = endTime
                    convrow['now'] = DAY_START_TIME
                    body = { 'id' : convrow['wfSource'] , 'body' : convrow, 'apiPath' : csvFileName }
                    print('Queuing job mobile_number: ' + convrow['mobile_number'] + " patient_name:"+convrow['patient_name'])
                    jobQueue.put(json.dumps(body))
                    #print('Queued job mobile_number: ' + convrow['mobile_number'] + " patient_name:"+convrow['patient_name'])
                except Exception as e:
                    print('Error processing row: ' + str(row))
                    print(e)
    except:
        print('Error processing file: ' + fqFileName)
    print('Items queued for file [' + csvFileName + ']: ' + str(jobQueue.size))
    shutil.move(fqFileName, fqDestFilename)
    print('Moved file ' + fqFileName + ' to ' + fqDestFilename)
print('Total queue size: ' + str(jobQueue.size))

while jobQueue.size > 0:
    payload = jobQueue.get()
    try:
        #print(payload)
        payloadDict = json.loads(payload)
        mobileNumber = str(payloadDict['body']['mobile_number'])
        state = str(payloadDict['body']['state'])
        wfSource = str(payloadDict['body']['wfSource'])
        w = existing_workflow(mobileNumber,state)
        if(w is None):
            if(trigger_new_workflow(payload,mobileNumber,wfSource)):
                jobQueue.ack(payload)
            else:
                jobQueue.ack_failed(payload)
        else:
            #print("Has workflow to update for mobile_number:"+ mobileNumber)
            if(update_or_trigger_new_workflow(w, payloadDict, mobileNumber,wfSource)):
                jobQueue.ack(payload)
            else:
                jobQueue.ack_failed(payload)
    except Exception as e:
        print('Error processing job: ' + str(payload))
        jobQueue.ack_failed(payload)


shutil.rmtree('covid-monitoring')
print('Processing complete')