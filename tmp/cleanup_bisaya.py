import json
from pathlib import Path

TARGET = Path(r"d:/Bisaya_Rescue/app/src/main/assets/listening_seed.json")

def remove_bisaya(word: str) -> str:
    tokens = word.split()
    filtered = [token for token in tokens if token.lower() != "bisaya"]
    return " ".join(filtered)

def main():
    data = json.loads(TARGET.read_text(encoding="utf-8"))
    changed = False
    for entry in data:
        native = entry.get("native", "")
        if "bisaya" in native.lower():
            new_native = remove_bisaya(native)
            if new_native != native:
                entry["native"] = new_native
                changed = True
        words = entry.get("words", [])
        new_words = [w for w in words if w.lower() != "bisaya"]
        if new_words != words:
            entry["words"] = new_words
            changed = True
    if changed:
        TARGET.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
        print("Removed stray 'bisaya' tokens.")
    else:
        print("No 'bisaya' tokens found.")

if __name__ == "__main__":
    main()
