# FixWise — AI-Based Fault Diagnosis & Decision Support Mobile Application

A responsive, installable Progressive Web App (PWA) with a Python machine-learning API and SQLite report database.

## Implemented
- Mobile-first interface for smartphones, laptops/PCs, home appliances, networks and other devices.
- Multi-symptom input, device model, onset, severity, notes and safety-risk flag.
- **ML backend:** scikit-learn TF-IDF + Logistic Regression text classifier with eight fault categories.
- **REST API:** FastAPI endpoints `/health`, `/diagnose`, and `/reports`.
- **Database:** SQLite stores report metadata, symptoms, predictions and timestamps.
- Safety override: a user-reported smoke/sparks/burning/electrical danger prioritizes an electrical-hazard warning.
- Local report history, printable reports, preventive care guide, PWA install/offline local fallback.
- Use the **Connect ML API** button in the app to enter the deployed backend URL.

## Architecture
```
Mobile / Desktop PWA (GitHub Pages)
        | HTTPS JSON
        v
FastAPI REST API (Render)
        |-- TF-IDF + Logistic Regression classifier
        |-- SQLite report database
        |-- safety warning override
```

## Deploy backend on Render
1. Sign in to Render and choose **New → Blueprint**.
2. Connect this GitHub repository and let Render read the root `render.yaml`.
3. Wait for service `fixwise-diagnostic-api` to become **Live**.
4. Copy its public URL (example: `https://fixwise-diagnostic-api.onrender.com`).
5. Open the deployed FixWise app, click **Connect ML API**, and paste that URL. The app verifies the `/health` endpoint before saving it.
6. Submit a diagnosis. The connected ML API makes the prediction and stores it in SQLite. If the backend is unavailable, the app falls back to local symptom rules.

API docs: append `/docs`; health check: `/health`.

## Deploy frontend on GitHub Pages
Enable **Repository Settings → Pages → Source → GitHub Actions**. The existing Actions workflow deploys the `FixWise/` folder. Check Actions for a successful run.
Expected URL: `https://gopichandsimats.github.io/ITA0313-MOBILE-computing-/FixWise/`

## Run backend locally
```bash
cd FixWise/backend
python -m venv .venv
# Windows: .venv\\Scripts\\activate
# macOS/Linux: source .venv/bin/activate
pip install -r requirements.txt
uvicorn main:app --reload
```
API: `http://127.0.0.1:8000`; interactive docs: `/docs`. In the app's Connect ML API prompt, enter `http://127.0.0.1:8000`.

## ML limitations — read before presenting
The classifier is an academic prototype trained on a small hand-curated demonstration dataset, **not a validated production diagnostic model**. Training phrases are not representative field data. Model scores are not reliable probabilities. Before real-world use, collect consented, technician-verified and diverse labelled examples; remove personal data; evaluate precision, recall, F1 and confusion matrices on held-out devices and time periods; and have safety advice reviewed by qualified technicians. Do not rely on this tool for life-safety, mains electrical, battery, gas, fire or other hazardous faults.

## Hosting and privacy
The Render free service may sleep when idle. The included SQLite path on a free instance may be ephemeral, so records can reset after restart/redeploy. For durable production storage, migrate to managed PostgreSQL. Do not store passwords, serial numbers, personal identifiers or sensitive data in report notes.

## Safety
FixWise is educational decision support, not a certified inspection or substitute for manufacturer support or a qualified technician. If there is smoke, sparking, burning smell, swollen battery, liquid near electrical parts or dangerous heat, stop using the device and seek professional help.
