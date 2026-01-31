import json
from pathlib import Path

MAPPING_PATH = Path(r"d:/Bisaya_Rescue/seed_translation_map.txt")
SEED_FILES = [
    Path(r"d:/Bisaya_Rescue/app/src/main/assets/listening_seed.json"),
    Path(r"d:/Bisaya_Rescue/app/src/main/assets/content/listening_seed_v2.json")
]


def load_mapping():
    mapping = {}
    for line in MAPPING_PATH.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        if "=" not in line:
            raise ValueError(f"Invalid mapping line: {line}")
        ja, en = line.split("=", 1)
        mapping[ja.strip()] = en.strip()
    return mapping


def process_file(path: Path, mapping):
    data = json.loads(path.read_text(encoding="utf-8"))
    missing = []
    for entry in data:
        ja = entry.get("translation", "").strip()
        if not ja:
            missing.append(f"ID {entry.get('id')}: missing Japanese translation")
            continue
        en = mapping.get(ja)
        if not en:
            missing.append(f"ID {entry.get('id')}: '{ja}' not found in mapping")
            continue
        entry["translations"] = {
            "ja": {"meaning": ja},
            "en": {"meaning": en}
        }
        entry.pop("translation", None)
    if missing:
        raise ValueError("Missing translations:\n" + "\n".join(missing))
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Updated {path}")


def main():
    mapping = load_mapping()
    for path in SEED_FILES:
        process_file(path, mapping)


if __name__ == "__main__":
    main()
