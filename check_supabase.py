import requests

url = "https://qgcrfqunomlyphkookma.supabase.co/rest/v1/drops?select=*"
headers = {
    "apikey": "sb_publishable_sz-peWcUebtkreVea3D1WA_j6A89pt8",
    "Authorization": "Bearer sb_publishable_sz-peWcUebtkreVea3D1WA_j6A89pt8"
}
response = requests.get(url, headers=headers)
print(response.status_code)
data = response.json()
print("Total rows:", len(data))
if len(data) > 0:
    print("Sample:", data[0])
