from flask import Flask, request, jsonify
from flask_cors import CORS
import requests
import json
import re

app = Flask(__name__)
CORS(app)

OLLAMA_URL = "http://localhost:11434/api/generate"
OLLAMA_MODEL = "gemma4:e2b"

def generate_ollama(prompt):
    try:
        response = requests.post(OLLAMA_URL, json={
            "model": OLLAMA_MODEL,
            "prompt": prompt,
            "stream": False
        })
        if response.status_code == 200:
            return response.json().get('response', '')
        else:
            print(f"Ollama error: {response.text}")
            return None
    except Exception as e:
        print(f"Failed to connect to Ollama: {e}")
        return None

@app.route('/place-summary', methods=['POST'])
def place_summary():
    data = request.json
    drops = data.get('drops', [])
    
    if not drops:
        return jsonify({"summary": "This place is quiet. No one has left any messages here yet."})

    texts = [drop.get('text', '') for drop in drops]
    combined_text = " | ".join(texts)

    prompt = f"You are an assistant analyzing messages left by visitors at a physical location. Summarize the overall themes, emotions, and experiences shared by visitors. Maximum 80 words. Do not use pleasantries, just provide the summary directly.\n\nMessages:\n{combined_text}\n\nSummary:"
    
    response = generate_ollama(prompt)
    
    if response:
        return jsonify({"summary": response.strip()})
    else:
        return jsonify({"summary": "I'm having trouble thinking. Make sure Ollama is running!"})


@app.route('/curator', methods=['POST'])
def curator():
    data = request.json
    drops = data.get('drops', [])
    
    if not drops:
        return jsonify({"short_intro": "There are no drops here.", "selected_drop_text": ""})

    texts_str = "\n".join([f"- {d.get('text', '')}" for d in drops])
    prompt = f"""You are a curator. From the provided list of Drops, select the single most meaningful, emotional, or interesting one for a visitor to hear. 
Provide a short 1-sentence intro, followed by the exact drop text. Do not use pleasantries.

Format your response EXACTLY like this:
INTRO: <your short intro>
DROP: <the exact text of the drop you selected>

Drops:
{texts_str}
"""
    
    response = generate_ollama(prompt)
    
    intro = "A visitor left this message here."
    drop_text = drops[0].get('text', '')
    
    if response:
        intro_match = re.search(r'INTRO:\s*(.*?)(?:\n|$)', response, re.IGNORECASE)
        drop_match = re.search(r'DROP:\s*(.*)', response, re.IGNORECASE | re.DOTALL)
        
        if intro_match:
            intro = intro_match.group(1).strip()
        if drop_match:
            drop_text = drop_match.group(1).strip()
            
    return jsonify({
        "short_intro": intro,
        "selected_drop_text": drop_text
    })

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5001, debug=False)
