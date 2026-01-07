import json
import re
from pathlib import Path

root = Path(r"d:/Bisaya_Rescue")
target = root / "app/src/main/assets/listening_seed.json"

def tokenize(text: str) -> list[str]:
    cleaned = re.sub(r"[?!]", "", text)
    cleaned = cleaned.replace(",", " ")
    cleaned = cleaned.replace("-", " ")
    cleaned = re.sub(r"\s+", " ", cleaned).strip()
    return [token for token in cleaned.split(" ") if token]

lessons = [
    (1, [
        ("Maayong buntag", "おはよう"),
        ("Maayong adlaw", "良い一日を"),
        ("Maayong hapon", "こんにちは（午後）"),
        ("Maayong gabii", "こんばんは"),
        ("Kumusta ka", "元気ですか？"),
        ("Maayo ra ko", "私は元気です"),
        ("Nalipay ko magkita nimo", "会えて嬉しいです"),
        ("Ayo ayo", "元気でね"),
        ("Amping ha", "気をつけてね"),
        ("Salamat kaayo", "本当にありがとう")
    ]),
    (2, [
        ("Oo", "はい"),
        ("Dili", "いいえ"),
        ("Wala", "ありません"),
        ("Sige", "了解です"),
        ("Pwede", "できます"),
        ("Dili pwede", "だめです"),
        ("Maayo ra", "大丈夫です"),
        ("Wala pa", "まだです"),
        ("Naa pa", "まだあります"),
        ("Basin siguro", "たぶんそうです")
    ]),
    (3, [
        ("Ako", "私"),
        ("Ikaw", "あなた"),
        ("Siya", "彼／彼女"),
        ("Kami", "私たち（あなた抜き）"),
        ("Kita", "私たち（あなた含む）"),
        ("Kamo", "あなたたち"),
        ("Sila", "彼ら"),
        ("Akong amigo", "私の友達"),
        ("Imong pamilya", "あなたの家族"),
        ("Iyang trabaho", "彼／彼女の仕事")
    ]),
    (4, [
        ("Kaon ko karon", "今食べています"),
        ("Kaon ta ug pan", "パンを食べましょう"),
        ("Inom ko tubig", "水を飲みます"),
        ("Inom ta kape", "コーヒーを飲みましょう"),
        ("Tulog na ko", "もう寝ます"),
        ("Mata na ha", "起きてください"),
        ("Lakaw ta dayon", "すぐ出かけましょう"),
        ("Balik ta unya", "あとで戻りましょう"),
        ("Dagan ko gamay", "少し走ります"),
        ("Sulat ko og sulat", "手紙を書きます")
    ]),
    (5, [
        ("Init kaayo diri", "ここはとても暑いです"),
        ("Bugnaw ang tubig", "水が冷たいです"),
        ("Dako kaayo ang balay", "家が大きいです"),
        ("Gamay ra ang kwarto", "部屋が小さいです"),
        ("Humok ang unlan", "枕が柔らかいです"),
        ("Kusog ang hangin", "風が強いです"),
        ("Taas siya", "彼／彼女は背が高いです"),
        ("Mubo ko", "私は背が低いです"),
        ("Tambok ang iro", "犬が太っています"),
        ("Niwang ang iring", "猫が細いです")
    ]),
    (6, [
        ("Gigutom ko", "お腹が空きました"),
        ("Gikapoy ko", "疲れました"),
        ("Giuhaw ko", "喉が渇きました"),
        ("Nalipay ko", "嬉しいです"),
        ("Nasuko ko", "怒っています"),
        ("Nagool ko", "心配しています"),
        ("Nahadlok ko", "怖いです"),
        ("Naglibog ko", "混乱しています"),
        ("Masakiton ko", "体調が悪いです"),
        ("Ganahan ko mopahuway", "休みたいです")
    ]),
    (7, [
        ("Asa ka", "どこにいるの？"),
        ("Ania ko sa balay", "家にいます"),
        ("Ania ko sa opisina", "オフィスにいます"),
        ("Asa sila karon", "みんな今どこ？"),
        ("Ania ra siya sa gawas", "彼／彼女は外にいます"),
        ("Tua ko sa merkado", "市場にいます"),
        ("Anhi diri palihug", "こちらに来てください"),
        ("Didto ko ganiha", "さっきそこにいました"),
        ("Asa dapit ang terminal", "ターミナルはどの辺ですか？"),
        ("Ania ta sa klase", "今授業にいます")
    ]),
    (8, [
        ("Akoa kini", "これは私のものです"),
        ("Imoha kana", "それはあなたのものです"),
        ("Iya ni Maria", "それはマリアのものです"),
        ("Amuha ning balay", "この家は私たちのものです"),
        ("Inyong libro ni", "これはあなたたちの本です"),
        ("Ilaha tong sakyanan", "あの車は彼らのものです"),
        ("Akong pitaka gikan Japan", "私の財布は日本製です"),
        ("Imong cellphone nindot", "あなたの携帯は素敵ですね"),
        ("Akoa ang trabaho karon", "今の仕事は私の担当です"),
        ("Akoa ning plano", "この計画は私のものです")
    ]),
    (9, [
        ("Gusto ko mokaon og sinugba", "焼き魚を食べたいです"),
        ("Gusto ko moinom ug kape", "コーヒーを飲みたいです"),
        ("Gusto ko moadto sa dagat", "海に行きたいです"),
        ("Gusto ko motan aw sine", "映画を見たいです"),
        ("Gusto ko magpahuway", "休みたいです"),
        ("Gusto ko magtuon Bisaya", "ビサヤ語を学びたいです"),
        ("Gusto ko makigstorya nimo", "あなたと話したいです"),
        ("Ganahan ko mokaon og mangga", "マンゴーを食べたいです"),
        ("Ganahan ko muadto Japan", "日本に行きたいです"),
        ("Gusto ko makatulog sayo", "早く寝たいです")
    ]),
    (10, [
        ("Uli na ko", "もう帰ります"),
        ("Lakaw na ta", "そろそろ行きましょう"),
        ("Balik ko unya", "あとで戻ります"),
        ("Moadto ko sa trabaho", "仕事に行きます"),
        ("Moanha ko didto", "あそこへ行きます"),
        ("Hulat sa ko diri", "ここで待っています"),
        ("Sulod na mo", "中に入ってください"),
        ("Gawas ta gamay", "少し外に出ましょう"),
        ("Dali na og sakay", "早く乗ってください"),
        ("Naug ko sa kanto", "角で降ります")
    ]),
    (11, [
        ("Unsa kini", "これは何ですか？"),
        ("Unsa na imong dala", "それは何を持っていますか？"),
        ("Kinsa ka", "あなたは誰ですか？"),
        ("Kinsa siya", "彼／彼女は誰ですか？"),
        ("Unsa imong pangalan", "あなたの名前は？"),
        ("Unsa iyang trabaho", "彼／彼女の仕事は？"),
        ("Kinsa inyong maestro", "先生は誰ですか？"),
        ("Unsa ni nga tunog", "これはどんな音ですか？"),
        ("Kinsa pa ang mouban", "誰が一緒に行きますか？"),
        ("Unsa imong plano", "あなたの予定は？")
    ]),
    (12, [
        ("Asa dapit", "どの辺ですか？"),
        ("Asa ka moadto", "どこへ行くのですか？"),
        ("Asa ang tindahan", "店はどこですか？"),
        ("Asa dapit ang hospital", "病院はどの辺ですか？"),
        ("Kanus a ka moabot", "いつ到着しますか？"),
        ("Kanus a ta magkita", "いつ会いましょうか？"),
        ("Asa ka karon", "今どこにいますか？"),
        ("Kanus a ang flight", "フライトはいつですか？"),
        ("Asa dapit ang park", "公園はどの辺ですか？"),
        ("Kanus a ko mulakaw", "いつ出発すればいいですか？")
    ]),
    (13, [
        ("Tagpila kini", "これはいくらですか？"),
        ("Tagpila ni tanan", "全部でいくらですか？"),
        ("Tagpila ang isa", "一ついくらですか？"),
        ("Tagpila ang kilo", "1キロいくらですか？"),
        ("Pwede pa mahangyo", "値引きできますか？"),
        ("Barato ra ni", "これは安いですよ"),
        ("Mahal kaayo ni", "これは高すぎます"),
        ("Tagpila ang plete", "運賃はいくらですか？"),
        ("Tagpila imong gusto", "いくらを希望しますか？"),
        ("Last price na", "これが最終価格ですか？")
    ]),
    (14, [
        ("Bayad palihug", "支払いをお願いします"),
        ("Lugar lang", "ここで止めてください"),
        ("Naog ko sa kanto", "角で降ります"),
        ("Saka na ta", "乗りましょう"),
        ("Hapit na ko musuod", "もうすぐ乗ります"),
        ("Ayaw kalimot og sukli", "お釣りを忘れないで"),
        ("Asa ang sakayan", "乗り場はどこですか？"),
        ("Paabot ug jeep", "ジープを待ってください"),
        ("Lingkod sa likod", "後ろに座ってください"),
        ("Dali ra ang biyahe", "すぐ着きますよ")
    ]),
    (15, [
        ("Taga Japan ko", "私は日本から来ました"),
        ("Ako si Ken", "私はケンです"),
        ("Puyo ko sa Cebu", "セブに住んでいます"),
        ("Nagtrabaho ko sa IT", "ITで働いています"),
        ("Mahilig ko sa kanta", "歌うのが好きです"),
        ("Ganahan ko mag travel", "旅行が好きです"),
        ("Nagtuon ko og Bisaya", "ビサヤ語を勉強中です"),
        ("Gusto ko makaila ninyo", "皆さんと知り合いたいです"),
        ("Nalipay ko makigstorya", "お話しできて嬉しいです"),
        ("Palihug tabangi ko magpraktis", "練習を手伝ってください")
    ]),
    (16, [
        ("Lami kaayo", "とても美味しいです"),
        ("Gutom na ko", "お腹がすきました"),
        ("Kaon ta sa carinderia", "食堂で食べましょう"),
        ("Palihug og sukad sa kan on", "ご飯をよそってください"),
        ("Pakiluto pa gamay", "もう少し火を通してください"),
        ("Daghan sabaw palihug", "スープ多めでお願いします"),
        ("Walay baboy", "豚肉はありますか？"),
        ("Pwede spicy gamay", "少し辛くしてもらえますか？"),
        ("Busog na ko", "お腹いっぱいです"),
        ("Tilawi ni", "これを味見してみて")
    ]),
    (17, [
        ("Nalipay gyud ko karon", "今日は本当に嬉しいです"),
        ("Masuko ko kung malate ka", "遅れると怒ります"),
        ("Naguol ko gahapon", "昨日は落ち込んでいました"),
        ("Nahadlok ko sa kilat", "雷が怖いです"),
        ("Nalingaw ko sa salida", "映画が楽しかったです"),
        ("Nainlove ko", "恋をしました"),
        ("Naghinamhinam ko sa pagkaon", "食事が待ち遠しいです"),
        ("Naulaw ko gamay", "少し恥ずかしいです"),
        ("Naglagot ko sa trapik", "渋滞にイライラします"),
        ("Nalipay ko nga naa ka", "あなたがいて嬉しいです")
    ]),
    (18, [
        ("Init kaayo sa gawas", "外はとても暑いです"),
        ("Bugnaw ang hangin karon", "今日は風が冷たいです"),
        ("Nag uwan pag ayo", "土砂降りです"),
        ("Walay adlaw karon", "今日は日差しがありません"),
        ("Kusog ang kilat", "雷が強いです"),
        ("Kusog ang ulan", "雨が激しいです"),
        ("Ting init na", "もう夏です"),
        ("Ting ulan na pud", "また雨季になりました"),
        ("Lapok ang dalan", "道がぬかるんでいます"),
        ("Huyop ang habagat", "ハバガットが吹いています")
    ]),
    (19, [
        ("Naa kay change", "お釣りはありますか？"),
        ("Palihug og sukli", "お釣りをください"),
        ("Pila tanan", "全部でいくらですか？"),
        ("Pwede ko mobayad ug GCash", "GCashで払ってもいいですか？"),
        ("Asa ang cash register", "レジはどこですか？"),
        ("Palit ko ani duha", "これを二つ買います"),
        ("Wala moy mas barato", "もっと安いのはありますか？"),
        ("Testingan nako ni", "これを試してみます"),
        ("Salamat sa inyong serbisyo", "サービスをありがとう"),
        ("Balik ko ugma", "また明日来ます")
    ]),
    (20, [
        ("Magkita ta unya", "また後で会いましょう"),
        ("Mag amping kanunay", "いつも気をつけてね"),
        ("Hulat ko sa sunod", "次を楽しみにしています"),
        ("Kita kits", "またね"),
        ("Amping sa biyahe", "道中気をつけて"),
        ("Huwat ko sa imong chat", "メッセージを待っています"),
        ("Tawagi ko puhon", "また電話してね"),
        ("Magkita ta sunod semana", "来週また会いましょう"),
        ("Dali ra ta magbalik", "すぐ戻ってきます"),
        ("Daghang salamat ug ayo ayo", "本当にありがとう、元気でね")
    ])
]

records = []
for level, sentences in lessons:
    if len(sentences) != 10:
        raise ValueError(f"Level {level} does not have 10 sentences (found {len(sentences)})")
    for idx, (native, translation) in enumerate(sentences, start=1):
        record = {
            "id": f"LV{level:02d}_Q{idx:02d}",
            "level": level,
            "native": native,
            "translation": translation,
            "words": tokenize(native)
        }
        records.append(record)

with target.open("w", encoding="utf-8") as f:
    json.dump(records, f, ensure_ascii=False, indent=2)

print(f"Wrote {len(records)} records to {target}")
