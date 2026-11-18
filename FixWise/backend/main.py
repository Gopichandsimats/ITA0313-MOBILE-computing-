import os, sqlite3, json, re
from datetime import datetime, timezone
from pathlib import Path
from typing import List, Optional
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import Pipeline

DB_PATH=os.getenv("FIXWISE_DB_PATH",str(Path(__file__).parent/"fixwise.db"))
app=FastAPI(title="FixWise Diagnostic API",version="1.0.0",description="Academic prototype ML fault classifier and SQLite report history")
app.add_middleware(CORSMiddleware,allow_origins=[x.strip() for x in os.getenv("CORS_ORIGINS","*").split(",")],allow_credentials=False,allow_methods=["GET","POST"],allow_headers=["*"])

# Hand-curated demonstration phrases only; not real-world field data or a validated diagnostic dataset.
DATA={
"battery_or_charging":["phone battery drains fast loses charge quickly","mobile not charging cable adapter port issue","laptop battery drains quickly unplugged","charger connected battery percentage not increasing","phone charges intermittently loose connector","battery percentage drops suddenly device switches off","slow charging damaged cable adapter","battery swollen hot while charging"],
"thermal_or_cooling":["device overheating hot fan loud vents blocked","computer gets hot shuts down cooling fan issue","phone becomes hot during normal use","appliance overheating poor ventilation","fan noise and system temperature rises","burning smell and overheating electrical device","laptop thermal throttling high temperature","device heats while idle cooling fault"],
"software_or_performance":["phone screen frozen apps crash slow performance","computer very slow operating system hangs","applications close unexpectedly after update","blue screen random restart driver problem","device stuck during startup operating system error","system lag low storage background apps","software update failed device keeps restarting","computer application not responding"],
"network_or_connectivity":["wifi not connecting no internet frequent disconnections","mobile network no signal weak coverage","internet slow high latency router disconnects","bluetooth cannot pair device connection problem","one device cannot connect wireless network","router restarts repeatedly weak wifi signal","network connected but no internet access","laptop wifi adapter disconnects"],
"power_or_startup":["device will not turn on no power startup failure","laptop power button does nothing screen black","computer does not boot power supply problem","appliance not starting after power connection","device switches off immediately after turning on","no lights no startup response adapter issue","system stuck before boot screen"],
"display_or_screen":["screen blank no display but power light on","monitor no signal display cable graphics issue","phone display flickers lines touch screen issue","laptop screen black after wake","display brightness issue screen goes dark","external monitor not detected"],
"mechanical_or_leak":["washing machine water leaking bottom hose seal","appliance unusual vibration loud mechanical noise","device grinding rattling noise while running","refrigerator not cooling airflow issue","air conditioner poor cooling airflow filter","water leaking around appliance connection"],
"electrical_hazard":["sparks from plug power trips breaker electrical hazard","burning smell smoke from appliance electrical fault","exposed wire electric shock damaged cable","water near electrical socket sparking","repeated breaker trips appliance switched on","visible smoke from battery electronic device"]}
ADVICE={
"battery_or_charging":("Battery or charging-system issue","Battery wear, charging accessories, connector damage or power-management faults may be involved.",["Stop charging if swollen, wet, unusually hot or damaged.","Check visible cable/adapter damage; use approved accessories.","Review battery health and power usage in settings.","Arrange authorized service if the problem persists."]),
"thermal_or_cooling":("Thermal or cooling-system issue","High workload, blocked airflow, ambient heat or a cooling component fault may cause excess temperature.",["Stop heavy workloads and move to a cool ventilated area.","Keep external vents unobstructed; do not open sealed equipment.","Check for unusually high app or CPU activity.","Seek service if overheating returns or occurs while idle."]),
"software_or_performance":("Software or performance issue","App conflicts, low storage, outdated software or system-resource pressure may explain the symptoms.",["Restart normally and note displayed error codes.","Check storage and back up important files.","Install stable updates from official sources.","Avoid factory reset until a verified backup exists."]),
"network_or_connectivity":("Network or connectivity issue","Signal strength, router configuration, interference or provider outage may be responsible.",["Check whether other devices have the same issue.","Test closer to the router or in a better signal area.","Check provider outage notices and official network settings.","Do not share router passwords or admin credentials."]),
"power_or_startup":("Power or startup issue","A depleted battery, damaged adapter, power connection or internal startup fault may be involved.",["Check the outlet and compatible adapter only if safe.","Disconnect non-essential accessories.","Follow the manufacturer's normal restart guidance.","If there is no response, seek a qualified technician."]),
"display_or_screen":("Display or screen issue","Display settings, cable connections, graphics drivers or panel hardware may be involved.",["Check brightness and wake the device normally.","For external displays, verify input selection and visible cable condition.","Restart normally and record startup indicators.","Seek service if the display remains blank."]),
"mechanical_or_leak":("Mechanical, airflow or leak issue","A loose external component, blocked filter, hose, seal or mechanical wear may reduce performance.",["Stop operation if leaking, vibrating severely or making a sudden loud noise.","Keep away from water near electrical outlets.","Check only user-accessible filters or hoses in the manual.","Use an authorized technician for internal repairs."]),
"electrical_hazard":("Potential electrical hazard","Symptoms may indicate arcing, damaged wiring, overheating or another dangerous electrical fault.",["Stop using the device immediately.","Keep people away from the device and exposed wiring.","Do not reconnect it or repeatedly reset the breaker.","Contact a licensed electrician or authorized service provider."])}
texts=[]; labels=[]
for label,phrases in DATA.items():
 for phrase in phrases:
  texts.extend([phrase,phrase+" device fault symptoms"]); labels.extend([label,label])
model=Pipeline([("tfidf",TfidfVectorizer(ngram_range=(1,2),sublinear_tf=True)),("classifier",LogisticRegression(max_iter=1200,class_weight="balanced"))])
model.fit(texts,labels)

def connect():
 c=sqlite3.connect(DB_PATH); c.row_factory=sqlite3.Row; return c
with connect() as c:
 c.execute("""CREATE TABLE IF NOT EXISTS reports(id INTEGER PRIMARY KEY AUTOINCREMENT,created_at TEXT,device TEXT,model_name TEXT,symptoms TEXT,details TEXT,duration TEXT,severity TEXT,safety_flag INTEGER,prediction TEXT,score REAL)""")
class Diagnosis(BaseModel):
 device:str=Field(min_length=2,max_length=80)
 model:Optional[str]=""
 symptoms:List[str]=Field(min_length=1,max_length=20)
 details:Optional[str]=""
 duration:Optional[str]=""
 severity:Optional[str]="medium"
 dangerous:bool=False
def clean(s): return re.sub(r"\s+"," ",s or "").strip()
@app.get("/")
def root(): return {"service":"FixWise Diagnostic API","docs":"/docs","health":"/health"}
@app.get("/health")
def health(): return {"status":"ok","model":"TF-IDF + LogisticRegression","training_examples":sum(map(len,DATA.values())),"note":"Small curated demo dataset; not validated for real-world diagnosis."}
@app.post("/diagnose")
def diagnose(p:Diagnosis):
 symptoms=[clean(x) for x in p.symptoms if clean(x)]
 if not symptoms: raise HTTPException(422,"Select at least one symptom")
 phrase=" ".join([p.device,p.model or ""," ".join(symptoms),p.details or ""])
 probs=model.predict_proba([phrase])[0]; classes=model.named_steps["classifier"].classes_
 ranked=sorted(zip(classes,probs),key=lambda x:x[1],reverse=True)[:3]
 results=[{"fault_code":k,"fault":ADVICE[k][0],"description":ADVICE[k][1],"steps":ADVICE[k][2],"model_score":round(float(v),3)} for k,v in ranked]
 if p.dangerous:
  k="electrical_hazard"; hazard={"fault_code":k,"fault":ADVICE[k][0],"description":ADVICE[k][1],"steps":ADVICE[k][2],"model_score":1.0}
  results=[hazard]+[x for x in results if x["fault_code"]!=k][:2]
 created=datetime.now(timezone.utc).isoformat()
 with connect() as c:
  cur=c.execute("INSERT INTO reports(created_at,device,model_name,symptoms,details,duration,severity,safety_flag,prediction,score) VALUES(?,?,?,?,?,?,?,?,?,?)",(created,clean(p.device),clean(p.model or ""),json.dumps(symptoms),clean(p.details or ""),p.duration,p.severity,int(p.dangerous),results[0]["fault_code"],results[0]["model_score"]))
  report_id=cur.lastrowid
 return {"report_id":report_id,"created_at":created,"device":p.device,"model":p.model,"symptoms":symptoms,"dangerous":p.dangerous,"source":"demo_ml_model","predictions":results,"notice":"Prototype classifier trained on a small curated demo dataset. Scores are not validated probabilities or a safety certification."}
@app.get("/reports")
def reports(limit:int=25):
 limit=max(1,min(limit,100))
 with connect() as c: rows=c.execute("SELECT * FROM reports ORDER BY id DESC LIMIT ?",(limit,)).fetchall()
 return {"reports":[{"id":r["id"],"created_at":r["created_at"],"device":r["device"],"model":r["model_name"],"symptoms":json.loads(r["symptoms"]),"details":r["details"],"dangerous":bool(r["safety_flag"]),"prediction":r["prediction"],"model_score":r["score"]} for r in rows]}
