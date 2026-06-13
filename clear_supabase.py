import requests

url = "https://qgcrfqunomlyphkookma.supabase.co/rest/v1/drops"
headers = {
    "apikey": "sb_publishable_sz-peWcUebtkreVea3D1WA_j6A89pt8",
    "Authorization": "Bearer sb_publishable_sz-peWcUebtkreVea3D1WA_j6A89pt8"
}
# Delete where id is not null (i.e. everything)
response = requests.delete(url + "?id=not.is.null", headers=headers)
print(response.status_code)
print(response.text)
