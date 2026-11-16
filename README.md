# FoodLoop - AI-Powered Food Waste Management & Redistribution

## Run it (3 terminals)
1. MySQL: run `database/init.sql` in Workbench (once).
2. Backend (port 8080):  `cd backend && mvnw.cmd spring-boot:run`
3. ML service (port 8000, optional - backend falls back to a 12% estimate if it is off):
   `cd ml-service && pip install -r requirements.txt && uvicorn main:app --port 8000`
4. Frontend (port 5173): `cd frontend && npm install && npm run dev`

Open http://localhost:5173. Seeded admin: `admin@foodloop.com` / `Admin@123` (change via ADMIN_EMAIL / ADMIN_PASSWORD).

## Demo flow
Register a Donor, an NGO and a Volunteer (3 browsers or private windows).
Donor posts food -> NGO accepts -> Volunteer takes pickup -> Donor shows pickup QR, volunteer pastes the code
-> NGO shows delivery QR, volunteer pastes the code -> DELIVERED. Impact numbers update.

## API
POST /api/auth/register, /api/auth/login
POST/GET /api/donations, GET/PUT /api/donations/{id}
POST /api/donations/{id}/cancel | accept | assign | pickup | deliver
GET  /api/donations/{id}/qr, /api/donations/{id}/matches, /api/predict/surplus, /api/analytics

## Notes
- Lifecycle: CREATED > NGO_ACCEPTED > VOLUNTEER_ASSIGNED > PICKED_UP > DELIVERED (+ EXPIRED, CANCELLED). All transitions live in DonationService.
- NGO score = 0.40 distance + 0.25 capacity + 0.20 food type + 0.15 availability (Haversine distance).
- The ML training data is SYNTHETIC (ml-service/train.py). Replace data/daily_food_records.csv with real records and retrain.
- Set JWT_SECRET, DB_PASSWORD in the environment for any real deployment.
