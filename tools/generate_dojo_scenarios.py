"""Generate DOJO scenario asset JSON with counterpart roles."""
from __future__ import annotations

import json
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[1]
OUTPUT_PATHS = [
    REPO_ROOT / "app" / "src" / "main" / "assets" / "content" / "scenarios_v1.json",
    REPO_ROOT / "app" / "src" / "proDebug" / "assets" / "content" / "scenarios_v1.json",
]

DEFAULT_OPENING_JA = "Hinay nga nagginhawa si Master Tari… \"Unsa may ato?\""
DEFAULT_OPENING_EN = "(Eyes closed, breathing slowly) \"...Unsa may ato?\""

TONE_RESPECT_JA = "厳格だが公正。礼儀を欠けば即失格。"
TONE_RESPECT_EN = "Strict yet fair—any lack of respect ends the deal."
TONE_SERVICE_JA = "プロ同士として鋭いが、誠意があれば認める。"
TONE_SERVICE_EN = "Professional to professional: brisk but appreciative of sincerity."

HINTS_RESPECT = ["Maayong adlaw", "Palihug", "Salamat kaayo"]
HINTS_SERVICE = ["Sir/Ma'am", "Pasensya", "Balikon nako ha"]

GRADIENTS = [
    ["#232526", "#414345"],
    ["#1e3c72", "#2a5298"],
    ["#0f2027", "#2c5364"],
    ["#485563", "#29323c"],
]

SCENARIO_SPECS = [
    {
        "id": "dojo_1",
        "title_ja": "空港のタクシー",
        "title_en": "Airport Taxi",
        "situation_ja": "空港タクシーが高額提示。渋滞事情も理解しつつ交渉する。",
        "situation_en": "The airport taxi quotes a high flat rate; you must negotiate while respecting traffic realities.",
        "goal_ja": "メーター利用＋妥当なチップで合意し、気持ちよく乗車する。",
        "goal_en": "Secure a metered ride with a fair tip so both sides feel respected.",
        "role_ja": "タクシー運転手",
        "role_en": "Taxi driver",
    },
    {
        "id": "dojo_2",
        "title_ja": "市場の魚屋",
        "title_en": "Market Fish Vendor",
        "situation_ja": "魚の鮮度が気になる観光客として信頼を勝ち取る。",
        "situation_en": "At the wet market you want the freshest catch and must earn the vendor's trust.",
        "goal_ja": "相手の目利きを褒め、今日一番の魚を選ばせて購入する。",
        "goal_en": "Praise their expertise and let them pick today's best fish for you to buy.",
        "role_ja": "魚屋",
        "role_en": "Fish vendor",
    },
    {
        "id": "dojo_3",
        "title_ja": "サリサリストア",
        "title_en": "Sari-sari Store",
        "situation_ja": "欲しい銘柄が欠品。店主と世間話をしながら代替品を探す。",
        "situation_en": "Your preferred brand is out; you need to chat with the owner to accept a substitute.",
        "goal_ja": "世間話で距離を縮め、納得できる代替品を選んで購入する。",
        "goal_en": "Build rapport and walk away with a substitute you genuinely accept.",
        "role_ja": "サリサリストア店主",
        "role_en": "Sari-sari shop owner",
    },
    {
        "id": "dojo_4",
        "title_ja": "ジプニーの料金",
        "title_en": "Jeepney Fare",
        "situation_ja": "混雑で料金を手渡せない。周囲と協力して届ける。",
        "situation_en": "The jeepney is packed and you can't pass the fare forward.",
        "goal_ja": "乗客へ丁寧に「Palihug」と頼み、料金を確実に届けてもらう。",
        "goal_en": "Ask fellow riders politely with 'Palihug' so the driver receives your payment.",
        "role_ja": "ジプニーの乗客",
        "role_en": "Jeepney riders",
    },
    {
        "id": "dojo_5",
        "title_ja": "警察の検問",
        "title_en": "Police Checkpoint",
        "situation_ja": "書類提示を求められた旅行者。",
        "situation_en": "At a checkpoint you must present documents without panic.",
        "goal_ja": "誠実に挨拶し身分を証明、笑顔で通過させてもらう。",
        "goal_en": "Greet respectfully, show ID, and leave with a friendly send-off.",
        "role_ja": "警察官",
        "role_en": "Police officer",
    },
    {
        "id": "dojo_6",
        "title_ja": "村長への挨拶",
        "title_en": "Barangay Captain",
        "situation_ja": "祭の見学を願う余所者として門を叩く。",
        "situation_en": "You want to observe a local fiesta and must address the barangay captain.",
        "goal_ja": "謙虚な自己紹介で参加許可をもらい、礼を尽くす。",
        "goal_en": "Introduce yourself humbly and earn permission to attend.",
        "role_ja": "バランガイキャプテン",
        "role_en": "Barangay captain",
    },
    {
        "id": "dojo_7",
        "title_ja": "地主の私有地",
        "title_en": "Private Lot Owner",
        "situation_ja": "散歩中に迷い込み、土地の主と対話が必要。",
        "situation_en": "You accidentally trespassed and must calm the owner.",
        "goal_ja": "誠実に謝罪し、土地への敬意を伝え、通り抜けを許してもらう。",
        "goal_en": "Apologize sincerely, show respect, and get safe passage.",
        "role_ja": "地主",
        "role_en": "Landowner",
    },
    {
        "id": "dojo_8",
        "title_ja": "仕立て屋",
        "title_en": "Tailor Shop",
        "situation_ja": "大事な服の直しを丁寧にお願いする。",
        "situation_en": "You need precise alterations on a beloved outfit.",
        "goal_ja": "細かな要望を敬語で伝え、仕上げ期日を約束してもらう。",
        "goal_en": "Explain details respectfully and secure a reliable completion date.",
        "role_ja": "仕立て屋",
        "role_en": "Tailor",
    },
    {
        "id": "dojo_9",
        "title_ja": "図書館の係員",
        "title_en": "Library Staff",
        "situation_ja": "貸出禁止の資料を閲覧したい学生。",
        "situation_en": "You want to consult a restricted archive at the library.",
        "goal_ja": "勉強への熱意を伝え、館内閲覧の特別許可を得る。",
        "goal_en": "Show passion for study and earn supervised access.",
        "role_ja": "図書館司書",
        "role_en": "Library staff",
    },
    {
        "id": "dojo_10",
        "title_ja": "農家の庭",
        "title_en": "Mango Farmer",
        "situation_ja": "私有地のマンゴーが気になって声を掛ける。",
        "situation_en": "A private mango grove tempts you; you must speak to the farmer.",
        "goal_ja": "正しい敬称で呼びかけ、少し分けてもらう交渉を成功させる。",
        "goal_en": "Use proper honorifics and earn permission to take a few fruits.",
        "role_ja": "マンゴー農家",
        "role_en": "Mango farmer",
    },
    {
        "id": "dojo_11",
        "title_ja": "ホテルの早着",
        "title_en": "Early Hotel Check-in",
        "situation_ja": "予約より早く到着し疲れている。",
        "situation_en": "You arrived ahead of time and are exhausted.",
        "goal_ja": "状況を丁寧に伝え、空き部屋があれば入れてもらう。",
        "goal_en": "Explain politely and convince the staff to ready a room.",
        "role_ja": "ホテル受付",
        "role_en": "Hotel front desk staff",
    },
    {
        "id": "dojo_12",
        "title_ja": "教会の案内係",
        "title_en": "Church Guide",
        "situation_ja": "神聖な祭壇を撮影したい巡礼者。",
        "situation_en": "You want to take photos in a sacred church.",
        "goal_ja": "敬虔さを示し、短時間だけ撮影許可をもらう。",
        "goal_en": "Prove reverence and gain limited photo access.",
        "role_ja": "教会案内係",
        "role_en": "Church guide",
    },
    {
        "id": "dojo_13",
        "title_ja": "ボート乗り",
        "title_en": "Boatman",
        "situation_ja": "観光外の穴場へ案内してほしい。",
        "situation_en": "You seek a hidden spot only the boatman knows.",
        "goal_ja": "彼の知識をリスペクトし、特別コースへ連れて行ってもらう。",
        "goal_en": "Respect his expertise and earn a bespoke route.",
        "role_ja": "ボートマン",
        "role_en": "Boatman",
    },
    {
        "id": "dojo_14",
        "title_ja": "大家の清掃",
        "title_en": "Landlord Cleaning",
        "situation_ja": "庭を自分で掃除したい入居者。",
        "situation_en": "As a tenant you want to handle yard work yourself.",
        "goal_ja": "家への愛着を伝え、掃除を任せてもらう。",
        "goal_en": "Express affection for the home and gain permission to maintain it.",
        "role_ja": "大家",
        "role_en": "Landlord",
    },
    {
        "id": "dojo_15",
        "title_ja": "楽器店主",
        "title_en": "Music Shop Owner",
        "situation_ja": "高価な楽器を試奏したい音楽好き。",
        "situation_en": "You wish to test a pricey instrument.",
        "goal_ja": "丁寧に扱うと約束し、試奏の許可を得る。",
        "goal_en": "Promise utmost care and get permission to play.",
        "role_ja": "楽器店主",
        "role_en": "Music shop owner",
    },
    {
        "id": "dojo_16",
        "title_ja": "公園の管理人",
        "title_en": "Park Manager",
        "situation_ja": "閉園後の忘れ物を探したい。",
        "situation_en": "You need a few minutes after closing to retrieve something.",
        "goal_ja": "困っている状況を端的に伝え、数分だけ門を開けてもらう。",
        "goal_en": "Explain calmly and secure a short grace period.",
        "role_ja": "公園管理人",
        "role_en": "Park manager",
    },
    {
        "id": "dojo_17",
        "title_ja": "工事現場の班長",
        "title_en": "Construction Foreman",
        "situation_ja": "道路封鎖で通れない。",
        "situation_en": "A construction site blocks your path.",
        "goal_ja": "現場の苦労を労い、安全なタイミングで通してもらう。",
        "goal_en": "Respect the crew and arrange a safe moment to pass.",
        "role_ja": "工事現場の班長",
        "role_en": "Construction foreman",
    },
    {
        "id": "dojo_18",
        "title_ja": "郷土史家",
        "title_en": "Local Historian",
        "situation_ja": "土地の本当の歴史を知りたい旅人。",
        "situation_en": "You seek a story not written in guidebooks.",
        "goal_ja": "深い関心と敬意を示し、逸話を一つ教えてもらう。",
        "goal_en": "Show genuine curiosity to hear an authentic tale.",
        "role_ja": "郷土史家",
        "role_en": "Local historian",
    },
    {
        "id": "dojo_19",
        "title_ja": "カフェの店員",
        "title_en": "Cafe Staff",
        "situation_ja": "薬を飲むために水がほしい。",
        "situation_en": "You urgently need water to take medicine.",
        "goal_ja": "忙しい相手を気遣い、スマートに水をいただく。",
        "goal_en": "Request softly with thanks and receive water graciously.",
        "role_ja": "カフェ店員",
        "role_en": "Cafe staff",
    },
    {
        "id": "dojo_20",
        "title_ja": "バスの乗務員",
        "title_en": "Bus Conductor",
        "situation_ja": "降りる場所が不安な乗客。",
        "situation_en": "You fear missing your stop on an unfamiliar bus.",
        "goal_ja": "目的地を伝え、近くに来たら知らせてもらう約束を取り付ける。",
        "goal_en": "State your stop and get the conductor to alert you.",
        "role_ja": "バス乗務員",
        "role_en": "Bus conductor",
    },
    {
        "id": "dojo_21",
        "title_ja": "街の古老",
        "title_en": "Town Elder",
        "situation_ja": "昔の街の様子を聞きたい。",
        "situation_en": "You want a nostalgic story from a local elder.",
        "goal_ja": "敬意を持って接し、古き良き時代の話を一つ引き出す。",
        "goal_en": "Approach respectfully and draw out a cherished memory.",
        "role_ja": "街の古老",
        "role_en": "Town elder",
    },
    {
        "id": "dojo_22",
        "title_ja": "派出所の警官",
        "title_en": "Police Desk",
        "situation_ja": "落とし物を届けた善意の人。",
        "situation_en": "You are filing a lost-and-found report.",
        "goal_ja": "手続きを厭わず丁寧に説明し、受理してもらう。",
        "goal_en": "Handle the paperwork patiently and receive thanks.",
        "role_ja": "派出所の警官",
        "role_en": "Police desk officer",
    },
    {
        "id": "dojo_23",
        "title_ja": "隣人の植木",
        "title_en": "Neighbor's Tree",
        "situation_ja": "枝が越境しトラブル寸前。",
        "situation_en": "A neighbor's tree is encroaching.",
        "goal_ja": "攻撃的にならず相談ベースで剪定案をまとめる。",
        "goal_en": "Discuss calmly and agree on a trimming plan.",
        "role_ja": "隣人",
        "role_en": "Neighbor",
    },
    {
        "id": "dojo_24",
        "title_ja": "銀行の窓口",
        "title_en": "Bank Counter",
        "situation_ja": "書類の書き方がわからない。",
        "situation_en": "You need help filling out forms at the bank.",
        "goal_ja": "謙虚に助けを求め、窓口で完成させる。",
        "goal_en": "Ask humbly and complete the document together.",
        "role_ja": "銀行窓口担当",
        "role_en": "Bank clerk",
    },
    {
        "id": "dojo_25",
        "title_ja": "ポーター",
        "title_en": "Porter",
        "situation_ja": "重い荷物を運んでほしい旅人。",
        "situation_en": "You need assistance carrying heavy luggage.",
        "goal_ja": "適切な挨拶と労い、チップの約束で安全に運んでもらう。",
        "goal_en": "Greet gratefully, promise fair pay, and reach the destination.",
        "role_ja": "ポーター",
        "role_en": "Porter",
    },
    {
        "id": "dojo_26",
        "title_ja": "広場の若者",
        "title_en": "Plaza Basketball",
        "situation_ja": "バスケに混ぜてほしい。",
        "situation_en": "You want the local youths to let you join their game.",
        "goal_ja": "挨拶と謙虚さを示し、仲間として受け入れてもらう。",
        "goal_en": "Introduce yourself politely and get invited in.",
        "role_ja": "広場の若者たち",
        "role_en": "Plaza basketball players",
    },
    {
        "id": "dojo_27",
        "title_ja": "不動産管理",
        "title_en": "Property Manager",
        "situation_ja": "蛇口の修理を急ぎたい入居者。",
        "situation_en": "As a tenant you need a broken faucet fixed fast.",
        "goal_ja": "日頃の感謝を伝えつつ、迅速な対応を約束させる。",
        "goal_en": "Express gratitude yet secure a prompt repair commitment.",
        "role_ja": "不動産管理人",
        "role_en": "Property manager",
    },
    {
        "id": "dojo_28",
        "title_ja": "看護師",
        "title_en": "Nurse",
        "situation_ja": "面会時間外に友人の容態を知りたい。",
        "situation_en": "You arrive after visiting hours seeking an update.",
        "goal_ja": "忙しい相手を邪魔せず、短い報告だけお願いする。",
        "goal_en": "Respect their workload and request a concise update.",
        "role_ja": "看護師",
        "role_en": "Nurse",
    },
    {
        "id": "dojo_29",
        "title_ja": "タクシーの渋滞",
        "title_en": "Taxi Traffic",
        "situation_ja": "渋滞で急いでいる乗客。",
        "situation_en": "You are in a rush but stuck in traffic inside a taxi.",
        "goal_ja": "運転手を急かさず、プロの判断で抜け道を探してもらう。",
        "goal_en": "Trust the driver and inspire them to find the best route.",
        "role_ja": "タクシー運転手",
        "role_en": "Taxi driver",
    },
    {
        "id": "dojo_30",
        "title_ja": "バスの隣客",
        "title_en": "Bus Seatmate",
        "situation_ja": "荷物が食い込み不快。",
        "situation_en": "A neighbor's baggage invades your space on the bus.",
        "goal_ja": "相手を不快にさせず、お互いのスペースを確保する。",
        "goal_en": "Address it gently and co-create comfortable space.",
        "role_ja": "バスの隣の乗客",
        "role_en": "Bus seatmate",
    },
    {
        "id": "dojo_31",
        "title_ja": "飲食店員（ユーザー）",
        "title_en": "Restaurant Crew (User)",
        "situation_ja": "忙しいランチで迷う客を担当。",
        "situation_en": "You are the crew guiding a confused diner during rush hour.",
        "goal_ja": "Sir/Ma'amで敬意を払い、注文を提案→復唱してキッチンへ通す。",
        "goal_en": "Suggest, confirm, and relay the order flawlessly.",
        "role_ja": "迷っている客",
        "role_en": "Confused diner",
    },
    {
        "id": "dojo_32",
        "title_ja": "ホテルベルスタッフ（ユーザー）",
        "title_en": "Hotel Bell Staff (User)",
        "situation_ja": "荷物が多い家族を部屋まで案内。",
        "situation_en": "You must help a weary family settle into their room.",
        "goal_ja": "荷物数を確認し、部屋を案内して最後に「Pahuway mo og maayo」と労う。",
        "goal_en": "Count bags, escort them, and end with 'Pahuway mo og maayo'.",
        "role_ja": "荷物が多い家族",
        "role_en": "Weary family",
    },
    {
        "id": "dojo_33",
        "title_ja": "アパート管理（ユーザー）",
        "title_en": "Apartment Manager (User)",
        "situation_ja": "騒音に怒る住人と向き合う。",
        "situation_en": "You are the building manager handling a noise complaint.",
        "goal_ja": "謝罪し、今すぐ隣へ注意しに行く約束で怒りを鎮める。",
        "goal_en": "Apologize, promise immediate action, and calm them down.",
        "role_ja": "怒っている住人",
        "role_en": "Angry tenant",
    },
    {
        "id": "dojo_34",
        "title_ja": "薬局店員（ユーザー）",
        "title_en": "Pharmacist (User)",
        "situation_ja": "喉を痛めたお年寄りの相談。",
        "situation_en": "An elderly customer needs throat medicine.",
        "goal_ja": "症状と期間を聞き、薬と飲み方を丁寧に伝え「Amping kanunay」で締める。",
        "goal_en": "Ask symptoms, explain dosage, and close with 'Amping kanunay'.",
        "role_ja": "喉を痛めたお年寄り",
        "role_en": "Elderly customer",
    },
    {
        "id": "dojo_35",
        "title_ja": "ギフトショップ店員（ユーザー）",
        "title_en": "Gift Shop Staff (User)",
        "situation_ja": "恋人へのプレゼントに迷う若者。",
        "situation_en": "A young shopper is unsure about a gift for their partner.",
        "goal_ja": "相手の好みと予算を引き出し、自信を持って一品を推薦する。",
        "goal_en": "Draw out tastes and budget, then recommend one perfect item.",
        "role_ja": "恋人へのプレゼントに迷う若者",
        "role_en": "Young shopper",
    },
]


def build_entry(spec: dict) -> dict:
    number = int(spec["id"].split("_")[1])
    respect_arc = number <= 30
    title = {
        "ja": f"【道場】{number}. {spec['title_ja']}",
        "en": f"【Dojo】{number}. {spec['title_en']}",
    }
    difficulty = {
        "ja": "Lv.4 敬意と交渉" if respect_arc else "Lv.3 プロ接客",
        "en": "Lv.4 Respect" if respect_arc else "Lv.3 Service",
    }
    tone = {
        "ja": TONE_RESPECT_JA if respect_arc else TONE_SERVICE_JA,
        "en": TONE_RESPECT_EN if respect_arc else TONE_SERVICE_EN,
    }
    hints = HINTS_RESPECT if respect_arc else HINTS_SERVICE
    gradient = GRADIENTS[(number - 1) % len(GRADIENTS)]
    context = {
        "role": {"ja": spec["role_ja"], "en": spec["role_en"]},
        "situation": {"ja": spec["situation_ja"], "en": spec["situation_en"]},
        "goal": {"ja": spec["goal_ja"], "en": spec["goal_en"]},
        "hints": hints,
        "turnLimit": 8,
        "tone": tone,
        "level": "advanced" if respect_arc else "intermediate",
    }
    return {
        "id": spec["id"],
        "title": title,
        "subtitle": {"ja": "マスタータリの修行", "en": "Master Tari trial"},
        "difficultyLabel": difficulty,
        "context": context,
        "backgroundGradient": gradient,
        "openingMessage": {"ja": DEFAULT_OPENING_JA, "en": DEFAULT_OPENING_EN},
        "systemPrompt": {"ja": "", "en": ""},
        "starterOptions": build_starter_options(spec, respect_arc),
    }


def build_starter_options(spec: dict, respect_arc: bool) -> list[dict]:
    def first_sentence(text: str) -> str:
        trimmed = text.strip()
        for sep in (".", "!", "?", "。", "！", "？"):
            idx = trimmed.find(sep)
            if idx != -1:
                return trimmed[:idx].strip()
        return trimmed

    role_en = spec["role_en"].strip()
    role_ja = spec["role_ja"].strip()
    situation_en = first_sentence(spec["situation_en"])
    situation_ja = first_sentence(spec["situation_ja"])
    goal_en = first_sentence(spec["goal_en"])
    goal_ja = first_sentence(spec["goal_ja"])
    honorific = "Sir/Ma'am" if respect_arc else role_en

    candidates = [
        (
            f"Maayong adlaw, {honorific}. Palihug ko gamayng tabang.",
            f"こんにちは、{role_ja}さん。少しだけ助けてください。"
        ),
        (
            f"Pasensya ha, {situation_en}.",
            f"すみません、{situation_ja} ことについて相談させてください。"
        ),
        (
            f"Gusto ko makab-ot ni: {goal_en}.",
            f"目標は「{goal_ja}」。そのために力を貸してください。"
        ),
        (
            f"Pwede ta maghisgot saon pag-ayo ani, {role_en}?",
            f"{role_ja}さんの判断を頼りに進めてもいいですか？"
        ),
        (
            "Kalma lang ko pero kinahanglan nako imong giya karon.",
            "落ち着いて進めますので、今どう動けばいいか教えてください。"
        ),
        (
            "Hatagi ko og klaro nga lakang palihug.",
            "次に取るべき手順をはっきり教えてください。" if respect_arc else "最適なやり方を一緒に決めさせてください。"
        ),
    ]

    cleaned: list[dict] = []
    for text, translation in candidates:
        normalized_text = " ".join(text.split()).strip()
        normalized_translation = translation.strip()
        if normalized_text and normalized_translation:
            option = {
                "text": normalized_text,
                "translation": normalized_translation
            }
            if option not in cleaned:
                cleaned.append(option)
    return cleaned[:6]


def main() -> None:
    entries = [build_entry(spec) for spec in SCENARIO_SPECS]
    for path in OUTPUT_PATHS:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(entries, ensure_ascii=False, indent=2), encoding="utf-8")
        print(f"Wrote {len(entries)} scenarios to {path}")


if __name__ == "__main__":
    main()
