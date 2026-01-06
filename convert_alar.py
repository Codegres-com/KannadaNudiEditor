import requests
import yaml
import json
import sys
import os

def download_and_convert():
    url = "https://raw.githubusercontent.com/alar-dict/data/master/alar.yml"
    print(f"Downloading {url}...")

    try:
        response = requests.get(url)
        response.raise_for_status()
        content = response.content.decode('utf-8')
    except Exception as e:
        print(f"Failed to download: {e}")
        sys.exit(1)

    print("Parsing YAML (this may take a moment)...")
    try:
        # Load all documents if it's a multi-document stream, or just load
        data = yaml.safe_load(content)
    except Exception as e:
        print(f"Failed to parse YAML: {e}")
        sys.exit(1)

    if not isinstance(data, list):
        print("Unexpected data format: Expected a list of entries.")
        sys.exit(1)

    print(f"Processing {len(data)} entries...")

    dictionary = {}

    for item in data:
        word = item.get('entry', '').strip()
        if not word:
            continue

        definitions = []
        raw_defs = item.get('defs', [])
        if raw_defs:
            for d in raw_defs:
                meaning = d.get('entry', '').strip()
                def_type = d.get('type', '').strip()
                if meaning:
                    definitions.append({
                        "meaning": meaning,
                        "type": def_type
                    })

        if definitions:
            if word in dictionary:
                dictionary[word].extend(definitions)
            else:
                dictionary[word] = definitions

    # Output path
    output_dir = "KannadaNudiWeb/wwwroot/Resources"
    os.makedirs(output_dir, exist_ok=True)
    output_path = os.path.join(output_dir, "alar_dictionary.json")

    print(f"Writing to {output_path}...")
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(dictionary, f, ensure_ascii=False, separators=(',', ':'))

    print("Done!")

if __name__ == "__main__":
    download_and_convert()
