import os
import joblib
import pandas as pd
from fastapi import FastAPI
from pydantic import BaseModel
from train import FEATURES, main as train

if not os.path.exists("model.joblib"):
    train()
model = joblib.load("model.joblib")
app = FastAPI(title="FoodLoop ML service")


class Req(BaseModel):
    day_of_week: int      # 0 = Monday
    month: int
    event_flag: int = 0
    prepared_qty: float


@app.get("/health")
def health():
    return {"status": "UP"}


@app.post("/predict")
def predict(r: Req):
    x = pd.DataFrame([[r.day_of_week, r.month, r.event_flag, r.prepared_qty]], columns=FEATURES)
    return {"predicted_surplus": round(max(0.0, float(model.predict(x)[0])), 2)}
