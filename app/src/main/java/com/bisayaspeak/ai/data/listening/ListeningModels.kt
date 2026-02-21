package com.bisayaspeak.ai.data.listening

import com.bisayaspeak.ai.data.model.DifficultyLevel
import kotlin.random.Random

enum class QuestionType {
    LISTENING,
    TRANSLATION,
    ORDERING
}

data class ListeningQuestion(
    val id: String,
    val phrase: String,
    val words: List<String>,
    val correctOrder: List<String>,
    val meaning: String,
    val pronunciation: String? = null,
    val audioUrl: String? = null,
    val type: QuestionType = QuestionType.LISTENING,
    val translations: Map<String, String> = emptyMap()
)

data class ListeningSession(
    val difficulty: DifficultyLevel,
    val questions: List<ListeningQuestion>,
    val currentQuestionIndex: Int = 0,
    val score: Int = 0,
    val mistakes: Int = 0,
    val completed: Boolean = false
)

object ListeningQuestions {
    fun getQuestions(difficulty: DifficultyLevel): List<ListeningQuestion> {
        return when (difficulty) {
            DifficultyLevel.BEGINNER -> beginnerQuestions
            DifficultyLevel.INTERMEDIATE -> intermediateQuestions
            DifficultyLevel.ADVANCED -> advancedQuestions
        }
    }
    
    fun getQuestionsByLevel(difficulty: DifficultyLevel): List<ListeningQuestion> {
        return getQuestions(difficulty)
    }

    fun getAllQuestions(): List<ListeningQuestion> {
        return beginnerQuestions + intermediateQuestions + advancedQuestions
    }

    fun getLiteSession(
        totalQuestions: Int = 6,
        hardCountRange: IntRange = 1..2
    ): List<ListeningQuestion> {
        if (totalQuestions <= 0) return emptyList()

        val beginnerPool = beginnerQuestions.shuffled().toMutableList()
        val intermediatePool = intermediateQuestions.shuffled().toMutableList()
        val advancedPool = advancedQuestions.shuffled().toMutableList()

        val hardSource = when {
            advancedPool.isNotEmpty() -> advancedPool
            intermediatePool.isNotEmpty() -> intermediatePool
            else -> beginnerPool
        }

        val requestedHardCount = if (hardCountRange.first == hardCountRange.last) {
            hardCountRange.first
        } else {
            Random.nextInt(hardCountRange.first, hardCountRange.last + 1)
        }
        val hardCount = requestedHardCount.coerceAtLeast(1).coerceAtMost(hardSource.size.coerceAtLeast(1))
        val hardQuestions = hardSource.take(hardCount)
        hardSource.removeAll(hardQuestions)

        val remainingNeeded = (totalQuestions - hardQuestions.size).coerceAtLeast(0)
        val beginnerTarget = remainingNeeded.coerceAtLeast(0).coerceAtMost(4).coerceAtMost(beginnerPool.size)
        val beginnerSelection = beginnerPool.take(beginnerTarget)
        beginnerPool.removeAll(beginnerSelection)

        val remainingAfterBeginner = remainingNeeded - beginnerSelection.size
        val intermediateSelection = intermediatePool.take(remainingAfterBeginner.coerceAtMost(intermediatePool.size))
        intermediatePool.removeAll(intermediateSelection)

        val combined = mutableListOf<ListeningQuestion>().apply {
            addAll(beginnerSelection)
            addAll(intermediateSelection)
            addAll(hardQuestions)
        }

        if (combined.size < totalQuestions) {
            val fallbackPool = (beginnerPool + intermediatePool + advancedPool).filter { it !in combined }
            combined += fallbackPool.shuffled().take(totalQuestions - combined.size)
        }

        return combined.take(totalQuestions).shuffled()
    }

    private val beginnerQuestions = listOf(
        ListeningQuestion(
            id = "b1",
            phrase = "Maayong buntag",
            words = listOf("buntag", "Maayong"),
            correctOrder = listOf("Maayong", "buntag"),
            meaning = "おはようございます"
        ),
        ListeningQuestion(
            id = "b2",
            phrase = "Kumusta ka",
            words = listOf("ka", "Kumusta"),
            correctOrder = listOf("Kumusta", "ka"),
            meaning = "元気ですか"
        ),
        ListeningQuestion(
            id = "b3",
            phrase = "Salamat kaayo",
            words = listOf("kaayo", "Salamat"),
            correctOrder = listOf("Salamat", "kaayo"),
            meaning = "ありがとうございます"
        ),
        ListeningQuestion(
            id = "b4",
            phrase = "Maayong hapon",
            words = listOf("hapon", "Maayong"),
            correctOrder = listOf("Maayong", "hapon"),
            meaning = "こんにちは"
        ),
        ListeningQuestion(
            id = "b5",
            phrase = "Maayong gabii",
            words = listOf("gabii", "Maayong"),
            correctOrder = listOf("Maayong", "gabii"),
            meaning = "こんばんは"
        ),
        ListeningQuestion(
            id = "b6",
            phrase = "Asa ka",
            words = listOf("ka", "Asa"),
            correctOrder = listOf("Asa", "ka"),
            meaning = "どこにいますか"
        ),
        ListeningQuestion(
            id = "b7",
            phrase = "Unsa imong ngalan",
            words = listOf("ngalan", "imong", "Unsa"),
            correctOrder = listOf("Unsa", "imong", "ngalan"),
            meaning = "あなたの名前は何ですか"
        ),
        ListeningQuestion(
            id = "b8",
            phrase = "Oo salamat",
            words = listOf("salamat", "Oo"),
            correctOrder = listOf("Oo", "salamat"),
            meaning = "はい、ありがとう"
        ),
        ListeningQuestion(
            id = "b9",
            phrase = "Dili salamat",
            words = listOf("salamat", "Dili"),
            correctOrder = listOf("Dili", "salamat"),
            meaning = "いいえ、結構です"
        ),
        ListeningQuestion(
            id = "b10",
            phrase = "Palihug tabang",
            words = listOf("tabang", "Palihug"),
            correctOrder = listOf("Palihug", "tabang"),
            meaning = "助けてください"
        ),
        ListeningQuestion(
            id = "b11",
            phrase = "Pila ka tuig",
            words = listOf("tuig", "ka", "Pila"),
            correctOrder = listOf("Pila", "ka", "tuig"),
            meaning = "何歳ですか"
        ),
        ListeningQuestion(
            id = "b12",
            phrase = "Maayo ra ko",
            words = listOf("ko", "ra", "Maayo"),
            correctOrder = listOf("Maayo", "ra", "ko"),
            meaning = "元気です"
        ),
        ListeningQuestion(
            id = "b13",
            phrase = "Asa ang banyo",
            words = listOf("banyo", "ang", "Asa"),
            correctOrder = listOf("Asa", "ang", "banyo"),
            meaning = "トイレはどこですか"
        ),
        ListeningQuestion(
            id = "b14",
            phrase = "Gusto ko tubig",
            words = listOf("tubig", "ko", "Gusto"),
            correctOrder = listOf("Gusto", "ko", "tubig"),
            meaning = "水が欲しいです"
        ),
        ListeningQuestion(
            id = "b15",
            phrase = "Palihug ayaw",
            words = listOf("ayaw", "Palihug"),
            correctOrder = listOf("Palihug", "ayaw"),
            meaning = "やめてください"
        ),
        ListeningQuestion(
            id = "b16",
            phrase = "Unsaon nako",
            words = listOf("nako", "Unsaon"),
            correctOrder = listOf("Unsaon", "nako"),
            meaning = "どうすればいいですか"
        ),
        ListeningQuestion(
            id = "b17",
            phrase = "Kanus-a ka mobalik",
            words = listOf("mobalik", "ka", "Kanus-a"),
            correctOrder = listOf("Kanus-a", "ka", "mobalik"),
            meaning = "いつ戻りますか"
        ),
        ListeningQuestion(
            id = "b18",
            phrase = "Lami kaayo",
            words = listOf("kaayo", "Lami"),
            correctOrder = listOf("Lami", "kaayo"),
            meaning = "とても美味しい"
        ),
        ListeningQuestion(
            id = "b19",
            phrase = "Mahal kaayo",
            words = listOf("kaayo", "Mahal"),
            correctOrder = listOf("Mahal", "kaayo"),
            meaning = "とても高い"
        ),
        ListeningQuestion(
            id = "b20",
            phrase = "Barato lang",
            words = listOf("lang", "Barato"),
            correctOrder = listOf("Barato", "lang"),
            meaning = "安いです"
        )
    )

    private val intermediateQuestions = listOf(
        ListeningQuestion(
            id = "i1",
            phrase = "Pila ang presyo",
            words = listOf("presyo", "ang", "Pila"),
            correctOrder = listOf("Pila", "ang", "presyo"),
            meaning = "いくらですか"
        ),
        ListeningQuestion(
            id = "i2",
            phrase = "Asa ang merkado",
            words = listOf("merkado", "ang", "Asa"),
            correctOrder = listOf("Asa", "ang", "merkado"),
            meaning = "市場はどこですか"
        ),
        ListeningQuestion(
            id = "i3",
            phrase = "Gusto ko mopalit ug prutas",
            words = listOf("prutas", "ug", "mopalit", "ko", "Gusto"),
            correctOrder = listOf("Gusto", "ko", "mopalit", "ug", "prutas"),
            meaning = "果物を買いたいです"
        ),
        ListeningQuestion(
            id = "i4",
            phrase = "Mahimo ba nga mohatag ug diskwento",
            words = listOf("diskwento", "ug", "mohatag", "nga", "ba", "Mahimo"),
            correctOrder = listOf("Mahimo", "ba", "nga", "mohatag", "ug", "diskwento"),
            meaning = "割引してもらえますか"
        ),
        ListeningQuestion(
            id = "i5",
            phrase = "Kanus-a ang sunod nga bus",
            words = listOf("bus", "nga", "sunod", "ang", "Kanus-a"),
            correctOrder = listOf("Kanus-a", "ang", "sunod", "nga", "bus"),
            meaning = "次のバスはいつですか"
        ),
        ListeningQuestion(
            id = "i6",
            phrase = "Pila ang pamasahe paingon sa siyudad",
            words = listOf("siyudad", "sa", "paingon", "pamasahe", "ang", "Pila"),
            correctOrder = listOf("Pila", "ang", "pamasahe", "paingon", "sa", "siyudad"),
            meaning = "市内への運賃はいくらですか"
        ),
        ListeningQuestion(
            id = "i7",
            phrase = "Mahimo ba nga mosakay ko dinhi",
            words = listOf("dinhi", "ko", "mosakay", "nga", "ba", "Mahimo"),
            correctOrder = listOf("Mahimo", "ba", "nga", "mosakay", "ko", "dinhi"),
            meaning = "ここで乗れますか"
        ),
        ListeningQuestion(
            id = "i8",
            phrase = "Palihug pahibalo nako kon moabot na",
            words = listOf("na", "moabot", "kon", "nako", "pahibalo", "Palihug"),
            correctOrder = listOf("Palihug", "pahibalo", "nako", "kon", "moabot", "na"),
            meaning = "着いたら教えてください"
        ),
        ListeningQuestion(
            id = "i9",
            phrase = "Gusto nako mokaon ug pagkaon sa Pilipinas",
            words = listOf("Pilipinas", "sa", "pagkaon", "ug", "mokaon", "nako", "Gusto"),
            correctOrder = listOf("Gusto", "nako", "mokaon", "ug", "pagkaon", "sa", "Pilipinas"),
            meaning = "フィリピン料理を食べたいです"
        ),
        ListeningQuestion(
            id = "i10",
            phrase = "Unsa ang imong rekomendar",
            words = listOf("rekomendar", "imong", "ang", "Unsa"),
            correctOrder = listOf("Unsa", "ang", "imong", "rekomendar"),
            meaning = "おすすめは何ですか"
        ),
        ListeningQuestion(
            id = "i11",
            phrase = "Mahimo ba nga dili halang",
            words = listOf("halang", "dili", "nga", "ba", "Mahimo"),
            correctOrder = listOf("Mahimo", "ba", "nga", "dili", "halang"),
            meaning = "辛くないようにできますか"
        ),
        ListeningQuestion(
            id = "i12",
            phrase = "Palihug ihatag ang bill",
            words = listOf("bill", "ang", "ihatag", "Palihug"),
            correctOrder = listOf("Palihug", "ihatag", "ang", "bill"),
            meaning = "お会計をお願いします"
        ),
        ListeningQuestion(
            id = "i13",
            phrase = "Asa ang pinakalapit nga hotel",
            words = listOf("hotel", "nga", "pinakalapit", "ang", "Asa"),
            correctOrder = listOf("Asa", "ang", "pinakalapit", "nga", "hotel"),
            meaning = "一番近いホテルはどこですか"
        ),
        ListeningQuestion(
            id = "i14",
            phrase = "Pila ang bayad para sa usa ka gabii",
            words = listOf("gabii", "ka", "usa", "sa", "para", "bayad", "ang", "Pila"),
            correctOrder = listOf("Pila", "ang", "bayad", "para", "sa", "usa", "ka", "gabii"),
            meaning = "一泊いくらですか"
        ),
        ListeningQuestion(
            id = "i15",
            phrase = "Naay ba kamoy libre nga WiFi",
            words = listOf("WiFi", "nga", "libre", "kamoy", "ba", "Naay"),
            correctOrder = listOf("Naay", "ba", "kamoy", "libre", "nga", "WiFi"),
            meaning = "無料WiFiはありますか"
        ),
        ListeningQuestion(
            id = "i16",
            phrase = "Gusto nako motan-aw sa mga tourist spot",
            words = listOf("spot", "tourist", "mga", "sa", "motan-aw", "nako", "Gusto"),
            correctOrder = listOf("Gusto", "nako", "motan-aw", "sa", "mga", "tourist", "spot"),
            meaning = "観光地を見たいです"
        ),
        ListeningQuestion(
            id = "i17",
            phrase = "Unsa ang oras karon",
            words = listOf("karon", "oras", "ang", "Unsa"),
            correctOrder = listOf("Unsa", "ang", "oras", "karon"),
            meaning = "今何時ですか"
        ),
        ListeningQuestion(
            id = "i18",
            phrase = "Mahimo ba nga mosulti ug hinay-hinay",
            words = listOf("hinay-hinay", "ug", "mosulti", "nga", "ba", "Mahimo"),
            correctOrder = listOf("Mahimo", "ba", "nga", "mosulti", "ug", "hinay-hinay"),
            meaning = "ゆっくり話してもらえますか"
        ),
        ListeningQuestion(
            id = "i19",
            phrase = "Dili ko kasabot palihug usba",
            words = listOf("usba", "palihug", "kasabot", "ko", "Dili"),
            correctOrder = listOf("Dili", "ko", "kasabot", "palihug", "usba"),
            meaning = "わかりません、もう一度お願いします"
        ),
        ListeningQuestion(
            id = "i20",
            phrase = "Salamat sa imong tabang",
            words = listOf("tabang", "imong", "sa", "Salamat"),
            correctOrder = listOf("Salamat", "sa", "imong", "tabang"),
            meaning = "助けてくれてありがとう"
        )
    )

    private val advancedQuestions = listOf(
        ListeningQuestion(
            id = "a1",
            phrase = "Gusto kong mopalit ug tinapay",
            words = listOf("tinapay", "ug", "mopalit", "kong", "Gusto"),
            correctOrder = listOf("Gusto", "kong", "mopalit", "ug", "tinapay"),
            meaning = "パンを買いたいです"
        ),
        ListeningQuestion(
            id = "a2",
            phrase = "Mahimo ba nga makigsulti nimo bahin sa kultura sa Pilipinas",
            words = listOf("Pilipinas", "sa", "kultura", "sa", "bahin", "nimo", "makigsulti", "nga", "ba", "Mahimo"),
            correctOrder = listOf("Mahimo", "ba", "nga", "makigsulti", "nimo", "bahin", "sa", "kultura", "sa", "Pilipinas"),
            meaning = "フィリピンの文化について話してもらえますか"
        ),
        ListeningQuestion(
            id = "a3",
            phrase = "Kon mahimo lang unta nga mobalik ko sa akong nasud",
            words = listOf("nasud", "akong", "sa", "ko", "mobalik", "nga", "unta", "lang", "mahimo", "Kon"),
            correctOrder = listOf("Kon", "mahimo", "lang", "unta", "nga", "mobalik", "ko", "sa", "akong", "nasud"),
            meaning = "できることなら自分の国に帰りたい"
        ),
        ListeningQuestion(
            id = "a4",
            phrase = "Nakasinati na ba ka ug mga kalisud sa imong kinabuhi",
            words = listOf("kinabuhi", "imong", "sa", "kalisud", "mga", "ug", "ka", "ba", "na", "Nakasinati"),
            correctOrder = listOf("Nakasinati", "na", "ba", "ka", "ug", "mga", "kalisud", "sa", "imong", "kinabuhi"),
            meaning = "人生で困難を経験したことがありますか"
        ),
        ListeningQuestion(
            id = "a5",
            phrase = "Ang pinakaimportante nga butang sa kinabuhi mao ang pamilya",
            words = listOf("pamilya", "ang", "mao", "kinabuhi", "sa", "butang", "nga", "pinakaimportante", "Ang"),
            correctOrder = listOf("Ang", "pinakaimportante", "nga", "butang", "sa", "kinabuhi", "mao", "ang", "pamilya"),
            meaning = "人生で最も大切なものは家族です"
        ),
        ListeningQuestion(
            id = "a6",
            phrase = "Kinahanglan natong protektahan ang atong kalikopan alang sa umaabot nga henerasyon",
            words = listOf("henerasyon", "nga", "umaabot", "sa", "alang", "kalikopan", "atong", "ang", "protektahan", "natong", "Kinahanglan"),
            correctOrder = listOf("Kinahanglan", "natong", "protektahan", "ang", "atong", "kalikopan", "alang", "sa", "umaabot", "nga", "henerasyon"),
            meaning = "未来の世代のために環境を守る必要があります"
        ),
        ListeningQuestion(
            id = "a7",
            phrase = "Dili sayon ang pagkat-on ug bag-ong pinulongan apan kinahanglan nga magpadayon",
            words = listOf("magpadayon", "nga", "kinahanglan", "apan", "pinulongan", "bag-ong", "ug", "pagkat-on", "ang", "sayon", "Dili"),
            correctOrder = listOf("Dili", "sayon", "ang", "pagkat-on", "ug", "bag-ong", "pinulongan", "apan", "kinahanglan", "nga", "magpadayon"),
            meaning = "新しい言語を学ぶのは簡単ではありませんが、続ける必要があります"
        ),
        ListeningQuestion(
            id = "a8",
            phrase = "Kon gusto nimong molambo sa imong karera kinahanglan nga magkugi",
            words = listOf("magkugi", "nga", "kinahanglan", "karera", "imong", "sa", "molambo", "nimong", "gusto", "Kon"),
            correctOrder = listOf("Kon", "gusto", "nimong", "molambo", "sa", "imong", "karera", "kinahanglan", "nga", "magkugi"),
            meaning = "キャリアで成功したいなら、努力する必要があります"
        ),
        ListeningQuestion(
            id = "a9",
            phrase = "Ang teknolohiya nagbag-o sa atong paagi sa pagkinabuhi ug pagtrabaho",
            words = listOf("pagtrabaho", "ug", "pagkinabuhi", "sa", "paagi", "atong", "sa", "nagbag-o", "teknolohiya", "Ang"),
            correctOrder = listOf("Ang", "teknolohiya", "nagbag-o", "sa", "atong", "paagi", "sa", "pagkinabuhi", "ug", "pagtrabaho"),
            meaning = "テクノロジーは私たちの生活と仕事の方法を変えています"
        ),
        ListeningQuestion(
            id = "a10",
            phrase = "Importante nga magbinuligay ug magtinabangay sa usag usa",
            words = listOf("usa", "usag", "sa", "magtinabangay", "ug", "magbinuligay", "nga", "Importante"),
            correctOrder = listOf("Importante", "nga", "magbinuligay", "ug", "magtinabangay", "sa", "usag", "usa"),
            meaning = "お互いに協力し助け合うことが重要です"
        ),
        ListeningQuestion(
            id = "a11",
            phrase = "Ang edukasyon mao ang yawe sa kalampusan ug kauswagan",
            words = listOf("kauswagan", "ug", "kalampusan", "sa", "yawe", "ang", "mao", "edukasyon", "Ang"),
            correctOrder = listOf("Ang", "edukasyon", "mao", "ang", "yawe", "sa", "kalampusan", "ug", "kauswagan"),
            meaning = "教育は成功と発展の鍵です"
        ),
        ListeningQuestion(
            id = "a12",
            phrase = "Kinahanglan natong respetuhan ang tanan nga tawo bisan unsa pa ang ilang kagikan",
            words = listOf("kagikan", "ilang", "ang", "pa", "unsa", "bisan", "tawo", "nga", "tanan", "ang", "respetuhan", "natong", "Kinahanglan"),
            correctOrder = listOf("Kinahanglan", "natong", "respetuhan", "ang", "tanan", "nga", "tawo", "bisan", "unsa", "pa", "ang", "ilang", "kagikan"),
            meaning = "出身に関係なく、すべての人を尊重する必要があります"
        ),
        ListeningQuestion(
            id = "a13",
            phrase = "Ang pagbag-o dili sayon apan kini importante alang sa pag-uswag",
            words = listOf("pag-uswag", "sa", "alang", "importante", "kini", "apan", "sayon", "dili", "pagbag-o", "Ang"),
            correctOrder = listOf("Ang", "pagbag-o", "dili", "sayon", "apan", "kini", "importante", "alang", "sa", "pag-uswag"),
            meaning = "変化は簡単ではありませんが、進歩のために重要です"
        ),
        ListeningQuestion(
            id = "a14",
            phrase = "Kon adunay problema kinahanglan natong pangitaon ang solusyon dili ang pasangil",
            words = listOf("pasangil", "ang", "dili", "solusyon", "ang", "pangitaon", "natong", "kinahanglan", "problema", "adunay", "Kon"),
            correctOrder = listOf("Kon", "adunay", "problema", "kinahanglan", "natong", "pangitaon", "ang", "solusyon", "dili", "ang", "pasangil"),
            meaning = "問題があるときは、責任を追及するのではなく解決策を探す必要があります"
        ),
        ListeningQuestion(
            id = "a15",
            phrase = "Ang kalinaw ug kahusay sa kalibutan nagsugod sa matag usa kanato",
            words = listOf("kanato", "usa", "matag", "sa", "nagsugod", "kalibutan", "sa", "kahusay", "ug", "kalinaw", "Ang"),
            correctOrder = listOf("Ang", "kalinaw", "ug", "kahusay", "sa", "kalibutan", "nagsugod", "sa", "matag", "usa", "kanato"),
            meaning = "世界の平和と幸福は私たち一人一人から始まります"
        ),
        ListeningQuestion(
            id = "a16",
            phrase = "Dili nato kinahanglan ang daghang butang aron malipay kondili ang kontento sa atong nahibaloan",
            words = listOf("nahibaloan", "atong", "sa", "kontento", "ang", "kondili", "malipay", "aron", "butang", "daghang", "ang", "kinahanglan", "nato", "Dili"),
            correctOrder = listOf("Dili", "nato", "kinahanglan", "ang", "daghang", "butang", "aron", "malipay", "kondili", "ang", "kontento", "sa", "atong", "nahibaloan"),
            meaning = "幸せになるために多くのものは必要なく、持っているものに満足することが大切です"
        ),
        ListeningQuestion(
            id = "a17",
            phrase = "Ang pagkamahinatagon ug pagkamanggihatagon maoy naghatag ug kahulugan sa atong kinabuhi",
            words = listOf("kinabuhi", "atong", "sa", "kahulugan", "ug", "naghatag", "maoy", "pagkamanggihatagon", "ug", "pagkamahinatagon", "Ang"),
            correctOrder = listOf("Ang", "pagkamahinatagon", "ug", "pagkamanggihatagon", "maoy", "naghatag", "ug", "kahulugan", "sa", "atong", "kinabuhi"),
            meaning = "思いやりと寛大さが私たちの人生に意味を与えます"
        ),
        ListeningQuestion(
            id = "a18",
            phrase = "Kinahanglan natong ampingan ang atong panglawas pinaagi sa hustong pagkaon ug ehersisyo",
            words = listOf("ehersisyo", "ug", "pagkaon", "hustong", "sa", "pinaagi", "panglawas", "atong", "ang", "ampingan", "natong", "Kinahanglan"),
            correctOrder = listOf("Kinahanglan", "natong", "ampingan", "ang", "atong", "panglawas", "pinaagi", "sa", "hustong", "pagkaon", "ug", "ehersisyo"),
            meaning = "適切な食事と運動で健康を守る必要があります"
        ),
        ListeningQuestion(
            id = "a19",
            phrase = "Ang pagpaminaw importante kaayo sa komunikasyon ug pagsabot sa uban",
            words = listOf("uban", "sa", "pagsabot", "ug", "komunikasyon", "sa", "kaayo", "importante", "pagpaminaw", "Ang"),
            correctOrder = listOf("Ang", "pagpaminaw", "importante", "kaayo", "sa", "komunikasyon", "ug", "pagsabot", "sa", "uban"),
            meaning = "傾聴はコミュニケーションと他者理解において非常に重要です"
        ),
        ListeningQuestion(
            id = "a20",
            phrase = "Kon gusto natong makab-ot ang atong mga damgo kinahanglan natong magtuo sa atong kaugalingon",
            words = listOf("kaugalingon", "atong", "sa", "magtuo", "natong", "kinahanglan", "damgo", "mga", "atong", "ang", "makab-ot", "natong", "gusto", "Kon"),
            correctOrder = listOf("Kon", "gusto", "natong", "makab-ot", "ang", "atong", "mga", "damgo", "kinahanglan", "natong", "magtuo", "sa", "atong", "kaugalingon"),
            meaning = "夢を実現したいなら、自分自身を信じる必要があります"
        )
    )
}
