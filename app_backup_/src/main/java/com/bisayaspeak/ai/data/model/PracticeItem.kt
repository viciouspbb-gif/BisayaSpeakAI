package com.bisayaspeak.ai.data.model

/**
 * Practice用の統一データクラス
 */
data class PracticeItem(
    val id: String,
    val category: String,
    val bisaya: String,
    val japanese: String,
    val english: String,
    val pronunciation: String,
    val difficulty: Int,
    val isPremium: Boolean,
    val description: String? = null  // 補足説明（使用場面や注意点など）
)

/**
 * サンプルPracticeデータ
 */
object PracticeData {
    val allItems = listOf(
        // 挨拶（無料）
        PracticeItem(
            id = "greeting_1",
            category = "挨拶",
            bisaya = "Maayong buntag",
            japanese = "おはようございます",
            english = "Good morning",
            pronunciation = "マアヨン ブンタグ",
            difficulty = 1,
            isPremium = false
        ),
        PracticeItem(
            id = "greeting_2",
            category = "挨拶",
            bisaya = "Maayong hapon",
            japanese = "こんにちは",
            english = "Good afternoon",
            pronunciation = "マアヨン ハポン",
            difficulty = 1,
            isPremium = false,
            description = "午後限定の挨拶。時間帯を問わず使うならKumustaを使いましょう。"
        ),
        PracticeItem(
            id = "greeting_3",
            category = "挨拶",
            bisaya = "Maayong gabii",
            japanese = "こんばんは",
            english = "Good evening",
            pronunciation = "マアヨン ガビイ",
            difficulty = 1,
            isPremium = false
        ),
        PracticeItem(
            id = "greeting_4",
            category = "挨拶",
            bisaya = "Kumusta ka",
            japanese = "元気ですか",
            english = "How are you",
            pronunciation = "クムスタ カ",
            difficulty = 1,
            isPremium = false,
            description = "汎用挨拶。時間帯を問わずいつでも使える便利な表現です。"
        ),
        PracticeItem(
            id = "greeting_5",
            category = "挨拶",
            bisaya = "Salamat",
            japanese = "ありがとう",
            english = "Thank you",
            pronunciation = "サラマット",
            difficulty = 1,
            isPremium = false
        ),
        
        // 基本会話（無料）
        PracticeItem(
            id = "basic_1",
            category = "基本会話",
            bisaya = "Oo",
            japanese = "はい",
            english = "Yes",
            pronunciation = "オオ",
            difficulty = 1,
            isPremium = false
        ),
        PracticeItem(
            id = "basic_2",
            category = "基本会話",
            bisaya = "Dili",
            japanese = "いいえ",
            english = "No",
            pronunciation = "ディリ",
            difficulty = 1,
            isPremium = false
        ),
        PracticeItem(
            id = "basic_3",
            category = "基本会話",
            bisaya = "Palihug",
            japanese = "お願いします",
            english = "Please",
            pronunciation = "パリフグ",
            difficulty = 1,
            isPremium = false
        ),
        PracticeItem(
            id = "basic_4",
            category = "基本会話",
            bisaya = "Pasensya na",
            japanese = "ごめんなさい",
            english = "Sorry",
            pronunciation = "パセンシャ ナ",
            difficulty = 1,
            isPremium = false
        ),
        PracticeItem(
            id = "basic_5",
            category = "基本会話",
            bisaya = "Unsaon nimo",
            japanese = "どうやって",
            english = "How do you",
            pronunciation = "ウンサオン ニモ",
            difficulty = 2,
            isPremium = false
        ),
        
        // 買い物（無料版で解放）
        PracticeItem(
            id = "shopping_1",
            category = "買い物",
            bisaya = "Palit",
            japanese = "買う",
            english = "Buy",
            pronunciation = "パリット",
            difficulty = 1,
            isPremium = false
        ),
        PracticeItem(
            id = "shopping_2",
            category = "買い物",
            bisaya = "Pila",
            japanese = "いくら",
            english = "How much",
            pronunciation = "ピラ",
            difficulty = 1,
            isPremium = false
        ),
        PracticeItem(
            id = "shopping_3",
            category = "買い物",
            bisaya = "Kwarta",
            japanese = "お金",
            english = "Money",
            pronunciation = "クワルタ",
            difficulty = 1,
            isPremium = false
        ),
        PracticeItem(
            id = "shopping_4",
            category = "買い物",
            bisaya = "Barato",
            japanese = "安い",
            english = "Cheap",
            pronunciation = "バラト",
            difficulty = 1,
            isPremium = false
        ),
        PracticeItem(
            id = "shopping_5",
            category = "買い物",
            bisaya = "Mahal",
            japanese = "高い",
            english = "Expensive",
            pronunciation = "マハル",
            difficulty = 1,
            isPremium = false
        ),
        
        // 食べ物（無料版で解放）
        PracticeItem(
            id = "food_1",
            category = "食べ物",
            bisaya = "Kaon",
            japanese = "食べる",
            english = "Eat",
            pronunciation = "カオン",
            difficulty = 2,
            isPremium = false
        ),
        PracticeItem(
            id = "food_2",
            category = "食べ物",
            bisaya = "Inom",
            japanese = "飲む",
            english = "Drink",
            pronunciation = "イノム",
            difficulty = 2,
            isPremium = false
        ),
        PracticeItem(
            id = "food_3",
            category = "食べ物",
            bisaya = "Tubig",
            japanese = "水",
            english = "Water",
            pronunciation = "トゥビグ",
            difficulty = 2,
            isPremium = false
        ),
        PracticeItem(
            id = "food_4",
            category = "食べ物",
            bisaya = "Bugas",
            japanese = "米",
            english = "Rice",
            pronunciation = "ブガス",
            difficulty = 2,
            isPremium = false
        ),
        PracticeItem(
            id = "food_5",
            category = "食べ物",
            bisaya = "Isda",
            japanese = "魚",
            english = "Fish",
            pronunciation = "イスダ",
            difficulty = 2,
            isPremium = false
        ),
        
        // 場所（無料版で解放）
        PracticeItem(
            id = "place_1",
            category = "場所",
            bisaya = "Balay",
            japanese = "家",
            english = "House",
            pronunciation = "バライ",
            difficulty = 2,
            isPremium = false
        ),
        PracticeItem(
            id = "place_2",
            category = "場所",
            bisaya = "Eskwelahan",
            japanese = "学校",
            english = "School",
            pronunciation = "エスクウェラハン",
            difficulty = 2,
            isPremium = false
        ),
        PracticeItem(
            id = "place_3",
            category = "場所",
            bisaya = "Merkado",
            japanese = "市場",
            english = "Market",
            pronunciation = "メルカド",
            difficulty = 2,
            isPremium = false
        ),
        PracticeItem(
            id = "place_4",
            category = "場所",
            bisaya = "Simbahan",
            japanese = "教会",
            english = "Church",
            pronunciation = "シンバハン",
            difficulty = 2,
            isPremium = false
        ),
        PracticeItem(
            id = "place_5",
            category = "場所",
            bisaya = "Ospital",
            japanese = "病院",
            english = "Hospital",
            pronunciation = "オスピタル",
            difficulty = 2,
            isPremium = false
        )
    )
    
    // カテゴリ一覧を取得
    val categories = allItems.map { it.category }.distinct()
    
    // カテゴリ別にアイテムを取得
    fun getItemsByCategory(category: String): List<PracticeItem> {
        return allItems.filter { it.category == category && !it.isPremium }
    }
    
    // カテゴリからランダムに5問を抽出
    fun getRandomQuestions(category: String, count: Int = 5): List<PracticeItem> {
        val items = getItemsByCategory(category)
        return if (items.size <= count) {
            items.shuffled()
        } else {
            items.shuffled().take(count)
        }
    }
}
