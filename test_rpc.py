import requests

url = "https://qgcrfqunomlyphkookma.supabase.co/rest/v1/rpc/nearby_drops"
headers = {
    "apikey": "sb_publishable_sz-peWcUebtkreVea3D1WA_j6A89pt8",
    "Authorization": "Bearer sb_publishable_sz-peWcUebtkreVea3D1WA_j6A89pt8",
    "Content-Type": "application/json"
}
data = {
    "p_lat": 0.0,
    "p_lon": 0.0,
    "p_radius_m": 1000,
    "p_limit": 10
}
response = requests.post(url, headers=headers, json=data)
print(response.status_code)
print(response.text)
