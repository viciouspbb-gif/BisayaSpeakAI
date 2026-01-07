import json
import re
from pathlib import Path

ROOT = Path(r"d:/Bisaya_Rescue")
TARGET = ROOT / "app/src/main/assets/listening_seed.json"


def tokenize(text: str) -> list[str]:
    cleaned = re.sub(r"[?!,]", "", text)
    cleaned = cleaned.replace("-", " ")
    cleaned = cleaned.replace("'", " ")
    cleaned = re.sub(r"\s+", " ", cleaned).strip()
    return [token for token in cleaned.split(" ") if token]

lessons = {
    21: [
        ("Magluto ko ug tinola ugma", "明日はティノラを作るつもりです"),
        ("Magtuon ko sa librarya unya", "このあと図書館で勉強するつもりです"),
        ("Magtrabaho ko ug maayo karong semana", "今週はしっかり働くつもりです"),
        ("Maglimpyo ko sa among balay", "家を掃除するつもりです"),
        ("Magpraktis ko og Bisaya kada adlaw", "毎日ビサヤ語を練習するつもりです"),
        ("Magbasa ko ug bag ong libro", "新しい本を読むつもりです"),
        ("Magpahuway ko gamay human trabaho", "仕事のあと少し休むつもりです"),
        ("Magsulat ko sa akong journal", "日記を書くつもりです"),
        ("Magbisita ko sa akong lola", "祖母のところへ行くつもりです"),
        ("Magjogging ko sa buntag", "朝にジョギングするつもりです"),
    ],
    22: [
        ("Ugma puhon magkita ta sa cafe", "明日カフェで会いましょう"),
        ("Sa sunod semana mouli ko sa probinsya", "来週は故郷に帰ります"),
        ("Sa sunod bulan magsugod ang klase", "来月授業が始まります"),
        ("Unya sa hapon muadto ko sa opisina", "午後にオフィスへ行きます"),
        ("Ugma sa buntag mag exercise ko", "明日の朝は運動します"),
        ("Sa sunod tuig mag negosyo ko", "来年は起業する予定です"),
        ("Ugma sa gabii motan aw ta sine", "明日の夜に映画を観ましょう"),
        ("Sa sunod adlaw magpalit ko og telepono", "明後日電話を買います"),
        ("Ugma sa udto magkuyog mi sa mall", "明日の昼に一緒にモールへ行きます"),
        ("Sa sunod weekend mag beach trip mi", "次の週末は海へ行きます"),
    ],
    23: [
        ("Mangaon ta sa karinderya karon", "今 食堂で食べよう"),
        ("Muli na ta aron makapahuway", "帰って休もう"),
        ("Manan aw ta og sine karong gabii", "今夜映画を観に行こう"),
        ("Maglakaw ta sa baybayon", "海辺を散歩しよう"),
        ("Magduwa ta og basket sa plaza", "広場でバスケをしよう"),
        ("Magkape ta ug istorya", "コーヒーを飲みながら話そう"),
        ("Magshopping ta sa downtown", "ダウンタウンで買い物しよう"),
        ("Magpraktis ta sa kanta para sa event", "イベント用の歌を練習しよう"),
        ("Magbisita ta sa among amigo", "友達のところへ遊びに行こう"),
        ("Magluto ta ug sinugba karong weekend", "今週末はバーベキューをしよう"),
    ],
    24: [
        ("Dili ko moadto sa mall ugma", "明日はモールに行きません"),
        ("Dili ko mokaon ug tam is snacks", "甘いお菓子は食べません"),
        ("Dili siya molakaw kung ulan", "雨なら彼は出かけません"),
        ("Dili mi moorder ug mahal nga pagkaon", "高い料理は注文しません"),
        ("Dili ko moinom ug kape karong gabii", "今夜はコーヒーを飲みません"),
        ("Dili sila motan aw sa salida ugma", "彼らは明日ショーを見ません"),
        ("Dili ko mosayaw sa party", "パーティーでは踊りません"),
        ("Dili mi mag travel sa tibuok semana", "今週は旅行しません"),
        ("Dili ko mag gasto ug daghan kwarta", "お金をたくさん使いません"),
        ("Dili siya motubag sa tawag karong orasa", "今は彼は電話に出ません"),
    ],
    25: [
        ("Kanus a ka moabot", "いつ着きますか"),
        ("Kanus a ang party magsugod", "パーティーはいつ始まりますか"),
        ("Kanus a ta magkita usab", "次はいつ会いますか"),
        ("Kanus a sila mohulam sa sakyanan", "彼らはいつ車を借りますか"),
        ("Kanus a siya motawag nimo", "彼はいつあなたに電話しますか"),
        ("Kanus a nimo humanon ang report", "報告書はいつ終わりますか"),
        ("Kanus a moabut ang jeep", "ジープはいつ来ますか"),
        ("Kanus a ka mouli sa Japan", "いつ日本に帰りますか"),
        ("Kanus a mahuman ang ulan", "雨はいつ止みますか"),
        ("Kanus a mo open ang tindahan", "店はいつ開きますか"),
    ],
    26: [
        ("Nikaon na ko ug pan", "もうパンを食べました"),
        ("Niabot na siya sa terminal", "彼はすでにターミナルに着きました"),
        ("Nipalit ko ug prutas gahapon", "昨日果物を買いました"),
        ("Nimisita sila sa among balay", "彼らは家に来ました"),
        ("Nisulat ko og sulat kagabii", "昨夜手紙を書きました"),
        ("Nibasa siya ug libro ganina", "さきほど彼女は本を読みました"),
        ("Nidula mi og basketball kahapon", "昨日バスケをしました"),
        ("Nipaligo ko sa dagat niadtong Sabado", "先週土曜に海で泳ぎました"),
        ("Nitukar siya sa gitara sa party", "彼はパーティーでギターを弾きました"),
        ("Nihimo ko og kape ganina buntag", "今朝コーヒーを作りました"),
    ],
    27: [
        ("Gahapon sa buntag nag jogging ko", "昨日の朝ジョギングしました"),
        ("Gahapon sa hapon nag study ko", "昨日の午後勉強しました"),
        ("Ganina lang nahuman ang meeting", "さっき会議が終わりました"),
        ("Ganina sa udto nag lunch mi", "さきほど昼食を取りました"),
        ("Kagabii nagbantay ko sa bata", "昨夜子どもを見ていました"),
        ("Ganiha sa alas otso niabot siya", "さっき8時に彼が到着しました"),
        ("Niadtong weekend nibisita ko sa lola", "先週末おばあちゃんを訪ねました"),
        ("Sa milabay nga bulan nagbakasyon mi", "先月休暇に行きました"),
        ("Niadtong Lunes nagsugod ang klase", "月曜日に授業が始まりました"),
        ("Pagkalunes sa gabii nag movie marathon mi", "月曜の夜に映画を見続けました"),
    ],
    28: [
        ("Humana ang trabaho karon buntag", "今朝仕事が終わりました"),
        ("Humana na ang report nga akong gihimo", "作成していた報告書は終わりました"),
        ("Humana sila ug lutong sinigang", "彼らはシニガンを作り終えました"),
        ("Humana ko og laba sa sanina", "洗濯を終えました"),
        ("Wala pa nako nahuman ang libro", "その本はまだ読み終えていません"),
        ("Wala pa sila nakaabot sa terminal", "彼らはまだターミナルに着いていません"),
        ("Wala pa mi nakabayad sa kuryente", "電気代はまだ払っていません"),
        ("Wala pa ko nakaluto sa panihapon", "夕食はまだ作っていません"),
        ("Humana na ang ulan karong gabii", "今夜は雨がもう止みました"),
        ("Wala pa mahuman ang proyekto", "プロジェクトはまだ終わっていません"),
    ],
    29: [
        ("Wala ko nipalit sa mahal nga bag", "高いバッグは買いませんでした"),
        ("Wala ko nakadungog sa imong tawag", "あなたの電話に気づきませんでした"),
        ("Wala siya miadto sa klase gahapon", "彼は昨日授業に行きませんでした"),
        ("Wala mi nakadawat sa sulat", "私たちは手紙を受け取っていません"),
        ("Wala ko nakaadto sa party", "パーティーには行きませんでした"),
        ("Wala sila naligo sa dagat", "彼らは海で泳ぎませんでした"),
        ("Wala ko natulog sayo kagabii", "昨夜早く寝ませんでした"),
        ("Wala mi nakahuman sa dula", "試合を最後までできませんでした"),
        ("Wala ko nakainom ug tubig ganina", "さっき水を飲みませんでした"),
        ("Wala siya nikaon sa panihapon", "彼女は夕食を食べませんでした"),
    ],
    30: [
        ("Nakaadto na ka sa Cebu", "セブへ行ったことはありますか"),
        ("Nakaon na ka ug durian", "ドリアンを食べたことはありますか"),
        ("Nakasulay na ka og pagsakay sa zipline", "ジップラインに乗ったことはありますか"),
        ("Nakakita na ka sa Chocolate Hills", "チョコレートヒルズを見たことがありますか"),
        ("Nakasuroy na mi sa Bohol", "私たちはボホールへ行ったことがあります"),
        ("Nakaapil ko sa festival sa Sinulog", "シヌログのお祭りに参加したことがあります"),
        ("Nakaadto ko sa Camiguin sa miaging tuig", "去年カミギンへ行きました"),
        ("Nakatrabaho siya sa gawas sa nasud", "彼は海外で働いた経験があります"),
        ("Nakadula sila og professional basketball", "彼らはプロのバスケをした経験があります"),
        ("Nakatilaw ko ug kinilaw sa Davao", "ダバオでキニラウを味わったことがあります"),
    ],
}

with TARGET.open("r", encoding="utf-8") as f:
    records = json.load(f)

existing = [entry for entry in records if entry.get("level", 0) <= 20]
existing.sort(key=lambda e: (e["level"], e["id"]))

new_entries = []
for level in range(21, 31):
    sentences = lessons.get(level, [])
    if len(sentences) != 10:
        raise ValueError(f"Level {level} does not have exactly 10 sentences")
    for idx, (native, translation) in enumerate(sentences, start=1):
        new_entries.append({
            "id": f"LV{level:02d}_Q{idx:02d}",
            "level": level,
            "native": native,
            "translation": translation,
            "words": tokenize(native)
        })

output = existing + new_entries

with TARGET.open("w", encoding="utf-8") as f:
    json.dump(output, f, ensure_ascii=False, indent=2)

print(f"Rebuilt file with {len(output)} entries")
