package com.bisayaspeak.ai.data.repository

import android.content.Context
import com.bisayaspeak.ai.data.model.LearningContent
import com.bisayaspeak.ai.data.model.LearningLevel

class LearningContentRepository {

    fun getContentByLevel(context: Context, level: LearningLevel): List<LearningContent> {
        val loaded = try {
            ContentRepository(context.applicationContext).loadLearningContentV1()
        } catch (_: Exception) {
            emptyList()
        }

        val filtered = loaded.filter { it.level == level }
        return if (filtered.isNotEmpty()) filtered else getContentByLevel(level)
    }
    
    fun getContentByLevel(level: LearningLevel): List<LearningContent> {
        return when (level) {
            LearningLevel.BEGINNER -> beginnerContent
            LearningLevel.INTERMEDIATE -> intermediateContent
            LearningLevel.ADVANCED -> advancedContent
        }
    }
    
    fun getContentByCategory(level: LearningLevel, category: String): List<LearningContent> {
        return getContentByLevel(level).filter { it.category == category }
    }
    
    private val beginnerContent = listOf(
        // 挨拶
        LearningContent("b1", "Maayong buntag", "おはようございます", "Good morning", "greetings", LearningLevel.BEGINNER),
        LearningContent("b2", "Maayong hapon", "こんにちは", "Good afternoon", "greetings", LearningLevel.BEGINNER),
        LearningContent("b3", "Maayong gabii", "こんばんは", "Good evening", "greetings", LearningLevel.BEGINNER),
        LearningContent("b4", "Kumusta", "元気ですか", "How are you", "greetings", LearningLevel.BEGINNER),
        LearningContent("b5", "Maayo", "良い", "Good", "greetings", LearningLevel.BEGINNER),
        LearningContent("b6", "Adios", "さようなら", "Goodbye", "greetings", LearningLevel.BEGINNER),
        LearningContent("b7", "Babay", "バイバイ", "Bye", "greetings", LearningLevel.BEGINNER),
        
        // 基本単語
        LearningContent("b8", "Salamat", "ありがとう", "Thank you", "basic", LearningLevel.BEGINNER),
        LearningContent("b9", "Palihug", "お願いします", "Please", "basic", LearningLevel.BEGINNER),
        LearningContent("b10", "Oo", "はい", "Yes", "basic", LearningLevel.BEGINNER),
        LearningContent("b11", "Dili", "いいえ", "No", "basic", LearningLevel.BEGINNER),
        LearningContent("b12", "Pasensya", "すみません", "Sorry", "basic", LearningLevel.BEGINNER),
        LearningContent("b13", "Walay sapayan", "どういたしまして", "You're welcome", "basic", LearningLevel.BEGINNER),
        
        // 自己紹介
        LearningContent("b14", "Pangalan", "名前", "Name", "introduction", LearningLevel.BEGINNER),
        LearningContent("b15", "Ako si", "私は〜です", "I am", "introduction", LearningLevel.BEGINNER),
        LearningContent("b16", "Unsa imong pangalan", "あなたの名前は", "What is your name", "introduction", LearningLevel.BEGINNER),
        LearningContent("b17", "Nalipay ko nga makaila nimo", "お会いできて嬉しいです", "Nice to meet you", "introduction", LearningLevel.BEGINNER),
        
        // 数字
        LearningContent("b18", "Usa", "1", "One", "numbers", LearningLevel.BEGINNER),
        LearningContent("b19", "Duha", "2", "Two", "numbers", LearningLevel.BEGINNER),
        LearningContent("b20", "Tulo", "3", "Three", "numbers", LearningLevel.BEGINNER),
        LearningContent("b21", "Upat", "4", "Four", "numbers", LearningLevel.BEGINNER),
        LearningContent("b22", "Lima", "5", "Five", "numbers", LearningLevel.BEGINNER),
        
        // 家族
        LearningContent("b23", "Pamilya", "家族", "Family", "family", LearningLevel.BEGINNER),
        LearningContent("b24", "Mama", "お母さん", "Mother", "family", LearningLevel.BEGINNER),
        LearningContent("b25", "Papa", "お父さん", "Father", "family", LearningLevel.BEGINNER),
        LearningContent("b26", "Igsoon", "兄弟姉妹", "Sibling", "family", LearningLevel.BEGINNER),
        
        // 質問
        LearningContent("b27", "Asa ka gikan", "どこから来ましたか", "Where are you from", "questions", LearningLevel.BEGINNER),
        LearningContent("b28", "Pila ka tuig", "何歳ですか", "How old are you", "questions", LearningLevel.BEGINNER),
        LearningContent("b29", "Unsa ni", "これは何ですか", "What is this", "questions", LearningLevel.BEGINNER),
        LearningContent("b30", "Asa ang banyo", "トイレはどこですか", "Where is the bathroom", "questions", LearningLevel.BEGINNER)
    )
    
    private val intermediateContent = listOf(
        // 市場
        LearningContent("i1", "Pila ni", "いくらですか", "How much is this", "shopping", LearningLevel.INTERMEDIATE),
        LearningContent("i2", "Mahal kaayo", "高すぎます", "Too expensive", "shopping", LearningLevel.INTERMEDIATE),
        LearningContent("i3", "Pwede ba nga discount", "割引できますか", "Can you give a discount", "shopping", LearningLevel.INTERMEDIATE),
        LearningContent("i4", "Barato lang", "安いです", "It's cheap", "shopping", LearningLevel.INTERMEDIATE),
        LearningContent("i5", "Kuhaon nako ni", "これを買います", "I'll take this", "shopping", LearningLevel.INTERMEDIATE),
        
        // 交通
        LearningContent("i6", "Asa ang jeepney", "ジープニーはどこ", "Where is the jeepney", "transportation", LearningLevel.INTERMEDIATE),
        LearningContent("i7", "Pila ang pamasahe", "運賃はいくら", "How much is the fare", "transportation", LearningLevel.INTERMEDIATE),
        LearningContent("i8", "Lugar lang", "ここで降ります", "Stop here please", "transportation", LearningLevel.INTERMEDIATE),
        LearningContent("i9", "Adto ko sa Ayala", "アヤラに行きます", "I'm going to Ayala", "transportation", LearningLevel.INTERMEDIATE),
        LearningContent("i10", "Unsa ang sakyan paingon sa", "〜への乗り物は", "What vehicle goes to", "transportation", LearningLevel.INTERMEDIATE),
        
        // レストラン
        LearningContent("i11", "Unsa ang imong gusto", "何が欲しいですか", "What do you want", "restaurant", LearningLevel.INTERMEDIATE),
        LearningContent("i12", "Gusto ko og tubig", "水が欲しい", "I want water", "restaurant", LearningLevel.INTERMEDIATE),
        LearningContent("i13", "Lami kaayo", "とても美味しい", "Very delicious", "restaurant", LearningLevel.INTERMEDIATE),
        LearningContent("i14", "Pila ang bayad", "いくらですか", "How much is the bill", "restaurant", LearningLevel.INTERMEDIATE),
        LearningContent("i15", "Busog na ko", "お腹いっぱいです", "I'm full", "restaurant", LearningLevel.INTERMEDIATE),
        
        // ホテル
        LearningContent("i16", "May bakante ba mo", "空室はありますか", "Do you have vacancy", "hotel", LearningLevel.INTERMEDIATE),
        LearningContent("i17", "Pila ang usa ka gabii", "一泊いくらですか", "How much per night", "hotel", LearningLevel.INTERMEDIATE),
        LearningContent("i18", "Asa ang akong kwarto", "私の部屋はどこですか", "Where is my room", "hotel", LearningLevel.INTERMEDIATE),
        
        // 観光
        LearningContent("i19", "Asa ang beach", "ビーチはどこですか", "Where is the beach", "tourism", LearningLevel.INTERMEDIATE),
        LearningContent("i20", "Nindot kaayo diri", "ここはとても素敵です", "It's very nice here", "tourism", LearningLevel.INTERMEDIATE),
        LearningContent("i21", "Pwede ba ko magkuha og picture", "写真を撮ってもいいですか", "Can I take a picture", "tourism", LearningLevel.INTERMEDIATE),
        
        // 日常会話
        LearningContent("i22", "Tabang", "助けて", "Help", "emergency", LearningLevel.INTERMEDIATE),
        LearningContent("i23", "Nawala akong bag", "バッグをなくしました", "I lost my bag", "emergency", LearningLevel.INTERMEDIATE),
        LearningContent("i24", "Masakiton ko", "具合が悪いです", "I'm sick", "emergency", LearningLevel.INTERMEDIATE),
        LearningContent("i25", "Asa ang hospital", "病院はどこですか", "Where is the hospital", "emergency", LearningLevel.INTERMEDIATE)
    )
    
    private val advancedContent = listOf(
        // 日常会話
        LearningContent("a1", "Unsaon nako pag-adto sa Ayala", "アヤラへの行き方は", "How do I get to Ayala", "conversation", LearningLevel.ADVANCED),
        LearningContent("a2", "Pwede ba nga tabangan nimo ko", "手伝ってもらえますか", "Can you help me", "conversation", LearningLevel.ADVANCED),
        LearningContent("a3", "Wala koy kasabot sa imong gisulti", "理解できません", "I don't understand what you said", "conversation", LearningLevel.ADVANCED),
        LearningContent("a4", "Mahimo ba nimo nga hinayhinay ang pagsulti", "ゆっくり話してもらえますか", "Can you speak slowly", "conversation", LearningLevel.ADVANCED),
        
        // ビジネス
        LearningContent("a5", "Gusto nako nga makigsabot sa imong manager", "マネージャーと話したいです", "I want to speak with your manager", "business", LearningLevel.ADVANCED),
        LearningContent("a6", "Kanus-a ang sunod nga miting", "次の会議はいつですか", "When is the next meeting", "business", LearningLevel.ADVANCED),
        
        // 緊急時
        LearningContent("a7", "Kinahanglan nako og doktor dayon", "すぐに医者が必要です", "I need a doctor immediately", "emergency", LearningLevel.ADVANCED),
        LearningContent("a8", "Tawagi ang pulis", "警察を呼んでください", "Call the police", "emergency", LearningLevel.ADVANCED),
        LearningContent("a9", "Gikawatan ko sa akong wallet", "財布を盗まれました", "My wallet was stolen", "emergency", LearningLevel.ADVANCED),
        
        // 感情表現
        LearningContent("a10", "Nalipay kaayo ko nga naa ka dinhi", "ここにいてくれて嬉しいです", "I'm very happy you're here", "emotions", LearningLevel.ADVANCED),
        LearningContent("a11", "Nasuko ko tungod sa imong gibuhat", "あなたの行動に怒っています", "I'm angry because of what you did", "emotions", LearningLevel.ADVANCED),
        
        // 依頼
        LearningContent("a12", "Pwede ba nimo nga ipahibalo nako kung human na", "終わったら教えてください", "Please let me know when it's done", "requests", LearningLevel.ADVANCED),
        LearningContent("a13", "Palihug ayuha ang akong booking", "予約を修正してください", "Please fix my booking", "requests", LearningLevel.ADVANCED),
        
        // 意見
        LearningContent("a14", "Sa akong hunahuna, mas maayo kung", "私の考えでは〜の方が良い", "In my opinion, it's better if", "opinions", LearningLevel.ADVANCED),
        LearningContent("a15", "Dili ko mouyon sa imong plano", "あなたの計画に同意しません", "I don't agree with your plan", "opinions", LearningLevel.ADVANCED)
    )
}
