import json
import re
from pathlib import Path

repo_root = Path(r"c:\\Bisaya_Final")
source = repo_root / "app" / "src" / "main" / "java" / "com" / "bisayaspeak" / "ai" / "data" / "repository" / "ScenarioRepository.kt"
text = source.read_text(encoding="utf-8")
pattern = re.compile(r'DojoContent\(\s*"([^"]+)"\s*,\s*"([^"]+)"\s*,\s*"([^"]+)"\s*,\s*"([^"]+)"\s*,\s*"([^"]+)"\s*,\s*"([^"]+)"\s*\)', re.MULTILINE)
items = []
for idx, match in enumerate(pattern.finditer(text), start=1):
    items.append({
        "index": idx,
        "title_ja": match.group(1),
        "title_en": match.group(2),
        "situation_ja": match.group(3),
        "situation_en": match.group(4),
        "goal_ja": match.group(5),
        "goal_en": match.group(6)
    })
output = repo_root / "tmp_dojo_contents.json"
output.write_text(json.dumps(items, ensure_ascii=False, indent=2), encoding="utf-8")
print(f"Extracted {len(items)} dojo contents -> {output}")
