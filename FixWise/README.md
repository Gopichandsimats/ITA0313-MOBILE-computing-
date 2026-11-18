# FixWise — AI-Based Fault Diagnosis & Decision Support Mobile Application

FixWise is a responsive, installable Progressive Web App (PWA) prototype for preliminary device fault diagnosis. It is designed mobile-first and also works on desktop.

## Features
- Device categories: smartphone, laptop/PC, home appliance, network/Wi-Fi, and other.
- Multi-select symptom intake, model, duration, impact, and additional notes.
- Transparent symptom-to-cause rules engine with ranked possible causes, match indicators, safe troubleshooting steps, and escalation advice.
- High-risk safety alert for smoke, sparks, burning smell, liquid/electricity, or dangerous overheating.
- Local diagnosis history (browser localStorage), report view, and print/save-to-PDF.
- Device care guide, responsive layout, PWA install manifest, and service-worker offline caching.
- No login, server, or external API required for this front-end prototype.

## Run locally
Open `index.html` in a browser, or serve this folder with a local static server. For PWA install/offline support, use HTTPS or localhost.

## Deploy with GitHub Pages
1. Open repository **Settings → Pages**.
2. Under **Build and deployment**, select **Deploy from a branch**.
3. Select branch **main** and folder **/(root)** only if deploying the repository root. This project is in `FixWise/`, so the included workflow deploys that folder.
4. Go to **Actions** and run/inspect **Deploy FixWise to GitHub Pages**. If Pages is not enabled for GitHub Actions, enable it in Settings → Pages → Source → GitHub Actions.
5. After a successful workflow, open the Pages URL shown in the deployment summary. The app URL will end in `/FixWise/`.

## Important project scope
The current version is a functional client-side prototype using explicit rules, not a trained machine-learning model or a cloud LLM. The displayed match score is a rule-match indicator, not a validated probability. Do not describe it as clinically/technically validated AI. A production AI version should connect a separately deployed, authenticated backend model, use verified device-specific datasets, add technician-reviewed labels, and evaluate precision/recall and safety before release.

## Safety and privacy
Reports stay in the current browser's local storage and are not synchronized across devices. Avoid entering passwords, serial numbers, personal identifiers, or other sensitive information. FixWise is educational decision support, not a replacement for manufacturer support or a qualified technician. High-voltage, battery, gas, fire, and electrical hazards must be escalated to professionals.
