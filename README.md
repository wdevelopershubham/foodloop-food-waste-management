# FoodLoop: AI-Powered Food Waste Management & Redistribution

FoodLoop connects restaurants, hotels, canteens and event organizers that have surplus food with NGOs, food banks and volunteers, so good food reaches people before it expires.

## Features

- Role-based login for donors, NGOs, volunteers and admins (JWT)
- Donation lifecycle tracking: `CREATED → NGO_ACCEPTED → VOLUNTEER_ASSIGNED → PICKED_UP → DELIVERED` (plus `EXPIRED` and `CANCELLED`)
- Expiry monitoring: urgent flag under 2 hours, automatic expiry of overdue donations
- Smart NGO matching by distance, capacity, food type and availability
- QR verification at pickup and at delivery
- AI surplus prediction (scikit-learn model served by FastAPI)
- Impact dashboard: kg rescued, meals served, completed deliveries, top donors

## Tech stack

| Layer    | Technology                                               |
| -------- | -------------------------------------------------------- |
| Frontend | React, Vite, CSS                                         |
| Backend  | Java 17, Spring Boot 3, Spring Security, Spring Data JPA |
| Database | MySQL 8                                                  |
| AI/ML    | Python, Pandas, scikit-learn, FastAPI                    |
| Tools    | VS Code, MySQL Workbench, Git, GitHub                    |

## Project structure

```
foodloop/
├── backend/      Spring Boot REST API (port 8080)
├── frontend/     React app (port 5173)
├── ml-service/   FastAPI surplus prediction (port 8000)
└── database/     init.sql
```

## Getting started

**Prerequisites:** JDK 17 or newer, Node.js 20+, MySQL 8, Python 3.10+

1. **Database.** Run `database/init.sql` once in MySQL Workbench.

2. **Backend** (port 8080)

   ```
   cd backend
   .\mvnw.cmd spring-boot:run
   ```

   On macOS or Linux use `./mvnw spring-boot:run`.

3. **Frontend** (port 5173)

   ```
   cd frontend
   npm install
   npm run dev
   ```

4. **ML service** (port 8000, optional)
   ```
   cd ml-service
   pip install -r requirements.txt
   uvicorn main:app --port 8000
   ```
   If it is not running, the backend falls back to a simple 12% estimate.

Open http://localhost:5173.

**Default admin:** `admin@foodloop.com` / `Admin@123`. Change it with the `ADMIN_EMAIL` and `ADMIN_PASSWORD` environment variables.

## Demo flow

Register a Donor, an NGO and a Volunteer (use private windows).

1. Donor posts surplus food
2. NGO accepts it
3. Volunteer takes the pickup
4. Donor shows the pickup QR, and the volunteer enters the code
5. NGO shows the delivery QR, and the volunteer enters the code
6. Status becomes **Delivered** and the impact numbers update

## API overview

| Method    | Endpoint                                | Description                                     |
| --------- | --------------------------------------- | ----------------------------------------------- |
| POST      | `/api/auth/register`, `/api/auth/login` | Sign up and sign in                             |
| POST, GET | `/api/donations`                        | Create a donation, list donations for your role |
| GET, PUT  | `/api/donations/{id}`                   | View or edit a donation                         |
| POST      | `/api/donations/{id}/cancel`            | Donor cancels                                   |
| POST      | `/api/donations/{id}/accept`            | NGO accepts                                     |
| POST      | `/api/donations/{id}/assign`            | Volunteer takes the pickup                      |
| POST      | `/api/donations/{id}/pickup`            | Volunteer confirms pickup with a code           |
| POST      | `/api/donations/{id}/deliver`           | Volunteer confirms delivery with a code         |
| GET       | `/api/donations/{id}/qr`                | Get the QR for your role                        |
| GET       | `/api/donations/{id}/matches`           | Top 5 matching NGOs                             |
| GET       | `/api/predict/surplus`                  | Predicted surplus in kg                         |
| GET       | `/api/analytics`                        | Impact statistics                               |

## How it works

- **NGO matching:** `score = 0.40 × distance + 0.25 × capacity + 0.20 × food type + 0.15 × availability`, with distance from the Haversine formula.
- **Lifecycle rules:** every status change goes through `DonationService`, so a donation cannot skip steps.
- **Surplus model:** gradient boosting trained on day of week, month, event flag and prepared quantity.

## Notes

- The ML training data is **synthetic** (`ml-service/train.py`). Replace `data/daily_food_records.csv` with real records and retrain for real use.
- For any real deployment, set `JWT_SECRET` and `DB_PASSWORD` as environment variables.

## Future work

Map view, camera-based QR scanning, admin user management, Docker Compose, automated tests.
