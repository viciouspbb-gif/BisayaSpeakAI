package com.bisayaspeak.ai.data.repository

import com.bisayaspeak.ai.data.remote.FirebaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * リスニング練習データリポジトリ
 * Firebaseとの通信を管理
 */
@Singleton
class ListeningDataRepository @Inject constructor(
    private val firebaseManager: FirebaseManager
) {
    
    /**
     * LV31のリスニングデータをFirebaseに追加
     */
    suspend fun addLevel31Data(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val level31Data = listOf(
                    mapOf(
                        "id" to 301L,
                        "level" to 31,
                        "native" to "Gusto ko makakat-on og dugang bahin nimo.",
                        "words" to listOf("Gusto", "ko", "makakat-on", "og", "dugang", "bahin", "nimo"),
                        "translations" to mapOf(
                            "ja" to mapOf("meaning" to "あなたのことをもっと知りたいです"),
                            "en" to mapOf("meaning" to "I want to learn more about you")
                        )
                    ),
                    mapOf(
                        "id" to 302L,
                        "level" to 31,
                        "native" to "Ganahan ko moadto sa Bohol sa sunod tuig.",
                        "words" to listOf("Ganahan", "ko", "moadto", "sa", "Bohol", "sa", "sunod", "tuig"),
                        "translations" to mapOf(
                            "ja" to mapOf("meaning" to "来年ボホールに行きたいです"),
                            "en" to mapOf("meaning" to "I want to go to Bohol next year")
                        )
                    ),
                    mapOf(
                        "id" to 303L,
                        "level" to 31,
                        "native" to "Nagplano ko nga magpuyo sa Sugbo.",
                        "words" to listOf("Nagplano", "ko", "nga", "magpuyo", "sa", "Sugbo"),
                        "translations" to mapOf(
                            "ja" to mapOf("meaning" to "セブに住む計画を立てています"),
                            "en" to mapOf("meaning" to "I am planning to live in Cebu")
                        )
                    ),
                    mapOf(
                        "id" to 304L,
                        "level" to 31,
                        "native" to "Hinaut nga magkita ta pag-usab.",
                        "words" to listOf("Hinaut", "nga", "magkita", "ta", "pag-usab"),
                        "translations" to mapOf(
                            "ja" to mapOf("meaning" to "またお会いできることを願っています"),
                            "en" to mapOf("meaning" to "I hope to see you again")
                        )
                    ),
                    mapOf(
                        "id" to 305L,
                        "level" to 31,
                        "native" to "Gusto nako mapalambo ang akong Bisaya.",
                        "words" to listOf("Gusto", "nako", "mapalambo", "ang", "akong", "Bisaya"),
                        "translations" to mapOf(
                            "ja" to mapOf("meaning" to "私のビサヤ語を向上させたいです"),
                            "en" to mapOf("meaning" to "I want to improve my Bisaya")
                        )
                    ),
                    mapOf(
                        "id" to 306L,
                        "level" to 31,
                        "native" to "Ganahan ko mokaon og lami nga pagkaon uban nimo.",
                        "words" to listOf("Ganahan", "ko", "mokaon", "og", "lami", "nga", "pagkaon", "uban", "nimo"),
                        "translations" to mapOf(
                            "ja" to mapOf("meaning" to "あなたと一緒に美味しいものを食べたいです"),
                            "en" to mapOf("meaning" to "I want to eat delicious food with you")
                        )
                    ),
                    mapOf(
                        "id" to 307L,
                        "level" to 31,
                        "native" to "Naghinam-hinam ko sa atong sunod nga panagkita.",
                        "words" to listOf("Naghinam-hinam", "ko", "sa", "atong", "sunod", "nga", "panagkita"),
                        "translations" to mapOf(
                            "ja" to mapOf("meaning" to "次に会えるのを楽しみにしています"),
                            "en" to mapOf("meaning" to "I am excited for our next meeting")
                        )
                    ),
                    mapOf(
                        "id" to 308L,
                        "level" to 31,
                        "native" to "Gusto ko makahibalo sa imong paborito nga kanta.",
                        "words" to listOf("Gusto", "ko", "makahibalo", "sa", "imong", "paborito", "nga", "kanta"),
                        "translations" to mapOf(
                            "ja" to mapOf("meaning" to "あなたの好きな歌を知りたいです"),
                            "en" to mapOf("meaning" to "I want to know your favorite song")
                        )
                    ),
                    mapOf(
                        "id" to 309L,
                        "level" to 31,
                        "native" to "Ganahan ko maglakaw-lakaw sa baybayon.",
                        "words" to listOf("Ganahan", "ko", "maglakaw-lakaw", "sa", "baybayon"),
                        "translations" to mapOf(
                            "ja" to mapOf("meaning" to "ビーチを散歩したいです"),
                            "en" to mapOf("meaning" to "I want to walk along the beach")
                        )
                    ),
                    mapOf(
                        "id" to 310L,
                        "level" to 31,
                        "native" to "Hinaut nga malingaw ka sa imong adlaw.",
                        "words" to listOf("Hinaut", "nga", "malingaw", "ka", "sa", "imong", "adlaw"),
                        "translations" to mapOf(
                            "ja" to mapOf("meaning" to "あなたが一日を楽しめるよう願っています"),
                            "en" to mapOf("meaning" to "I hope you enjoy your day")
                        )
                    )
                )
                
                firebaseManager.addListeningPracticeData(level31Data)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * LV32-35のリスニングデータをFirebaseに一括追加
     */
    suspend fun addLevel32To35Data(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val allData = mutableListOf<Map<String, Any>>()
                
                // LV32: 丁寧な依頼編（ビジネス・接客）
                val level32Data = listOf(
                    mapOf(
                        "id" to 311L,
                        "level" to 32,
                        "native" to "Mahimo ba nako mangayo og tabang sa imo?",
                        "words" to listOf("Mahimo", "ba", "nako", "mangayo", "og", "tabang", "sa", "imo"),
                        "translations" to mapOf(
                            "ja" to mapOf("meaning" to "あなたに助けを求めてもよろしいですか？"),
                            "en" to mapOf("meaning" to "May I ask for your help?")
                        )
                    ),
                    mapOf(
                        "id" to 312L,
                        "level" to 32,
                        "native" to "Palihug ipakita ang paagi sa merkado.",
                        "words" to listOf("Palihug", "ipakita", "ang", "paagi", "sa", "merkado"),
                        "translations" to mapOf(
                            "ja" to mapOf("meaning" to "すみません、市場への道を教えてください"),
                            "en" to mapOf("meaning" to "Please show me the way to the market")
                        )
                    ),
                    mapOf(
                        "id" to 313L,
                        "level" to 32,
                        "native" to "Unsa ang presyo niini?",
                        "words" to listOf("Unsa", "ang", "presyo", "niini"),
                        "translations" to mapOf(
                            "ja" to mapOf("meaning" to "これはいくらですか？"),
                            "en" to mapOf("meaning" to "How much is this?")
                        )
                    ),
                    mapOf(
                        "id" to 314L,
                        "level" to 32,
                        "native" to "Pwede ba makig-uban sa imong oras?",
                        "words" to listOf("Pwede", "ba", "makig-uban", "sa", "imong", "oras"),
                        "translations" to mapOf(
                            "ja" to mapOf("meaning" to "少しお時間をいただいてもよろしいですか？"),
                            "en" to mapOf("meaning" to "May I have some of your time?")
                        )
                    ),
                    mapOf(
                        "id" to 315L,
                        "level" to 32,
                        "native" to "Dili ko kahibalo kung asa ang istasyon.",
                        "words" to listOf("Dili", "ko", "kahibalo", "kung", "asa", "ang", "istasyon"),
                        "translations" to mapOf(
                            "ja" to mapOf("meaning" to "駅がどこかわかりません"),
                            "en" to mapOf("meaning" to "I don't know where the station is")
                        )
                    ),
                    mapOf(
                        "id" to 316L,
                        "level" to 32,
                        "native" to "Palihug sulbad ang akong problema.",
                        "words" to listOf("Palihug", "sulbad", "ang", "akong", "problema"),
                        "translations" to mapOf(
                            "ja" to mapOf("meaning" to "私の問題を解決してください"),
                            "en" to mapOf("meaning" to "Please solve my problem")
                        )
                    ),
                    mapOf(
                        "id" to 317L,
                        "level" to 32,
                        "native" to "Unsaon nako pag-order sa pagkaon?",
                        "words" to listOf("Unsaon", "nako", "pag-order", "sa", "pagkaon"),
                        "translations" to mapOf(
                            "ja" to mapOf("meaning" to "どうやって食事を注文すればよいですか？"),
                            "en" to mapOf("meaning" to "How do I order food?")
                        )
                    ),
                    mapOf(
                        "id" to 318L,
                        "level" to 32,
                        "native" to "Nasayran ko ba sa imong ngalan?",
                        "words" to listOf("Nasayran", "ko", "ba", "sa", "imong", "ngalan"),
                        "translations" to mapOf(
                            "ja" to mapOf("meaning" to "あなたの名前を知ってもよろしいですか？"),
                            "en" to mapOf("meaning" to "May I know your name?")
                        )
                    ),
                    mapOf(
                        "id" to 319L,
                        "level" to 32,
                        "native" to "Aron unsa ang imong gipangita?",
                        "words" to listOf("Aron", "unsa", "ang", "imong", "gipangita"),
                        "translations" to mapOf(
                            "ja" to mapOf("meaning" to "何をお探しですか？"),
                            "en" to mapOf("meaning" to "What are you looking for?")
                        )
                    ),
                    mapOf(
                        "id" to 320L,
                        "level" to 32,
                        "native" to "Pwede ba nako kuhaon ang imong numero?",
                        "words" to listOf("Pwede", "ba", "nako", "kuhaon", "ang", "imong", "numero"),
                        "translations" to mapOf(
                            "ja" to mapOf("meaning" to "あなたの番号をいただいてもよろしいですか？"),
                            "en" to mapOf("meaning" to "May I have your number?")
                        )
                    )
                )
                
                // LV33: 丁寧な挨拶・自己紹介編
                val level33Data = listOf(
                    mapOf(
                        "id" to 321L,
                        "level" to 33,
                        "native" to "Maayong adlaw sa imong tanan.",
                        "words" to listOf("Maayong", "adlaw", "sa", "imong", "tanan"),
                        "translations" to mapOf(
                            "ja" to mapOf("meaning" to "皆さん、良い一日を"),
                            "en" to mapOf("meaning" to "Good day to all of you")
                        )
                    ),
                    mapOf(
                        "id" to 322L,
                        "level" to 33,
                        "native" to "Akong ngalan ay [Name] gikan sa Japan.",
                        "words" to listOf("Akong", "ngalan", "ay", "gikan", "sa", "Japan"),
                        "translations" to mapOf(
                            "ja" to mapOf("meaning" to "私の名前は[Name]、日本から来ました"),
                            "en" to mapOf("meaning" to "My name is [Name] from Japan")
                        )
                    ),
                    mapOf(
                        "id" to 323L,
                        "level" to 33,
                        "native" to "Nalipay ko nga makaila sa imo.",
                        "words" to listOf("Nalipay", "ko", "nga", "makaila", "sa", "imo"),
                        "translations" to mapOf(
                            "ja" to mapOf("meaning" to "お会いできて嬉しいです"),
                            "en" to mapOf("meaning" to "I'm pleased to meet you")
                        )
                    ),
                    mapOf(
                        "id" to 324L,
                        "level" to 33,
                        "native" to "Asa ka gikan?",
                        "words" to listOf("Asa", "ka", "gikan"),
                        "translations" to mapOf(
                            "ja" to mapOf("meaning" to "どちらからいらっしゃいましたか？"),
                            "en" to mapOf("meaning" to "Where are you from?")
                        )
                    ),
                    mapOf(
                        "id" to 325L,
                        "level" to 33,
                        "native" to "Taga-dinhi ko sa Sugbo.",
                        "words" to listOf("Taga-dinhi", "ko", "sa", "Sugbo"),
                        "translations" to mapOf(
                            "ja" to mapOf("meaning" to "私はセブ出身です"),
                            "en" to mapOf("meaning" to "I'm from Cebu")
                        )
                    ),
                    mapOf(
                        "id" to 326L,
                        "level" to 33,
                        "native" to "Unsa ang imong trabaho?",
                        "words" to listOf("Unsa", "ang", "imong", "trabaho"),
                        "translations" to mapOf(
                            "ja" to mapOf("meaning" to "お仕事は何ですか？"),
                            "en" to mapOf("meaning" to "What is your job?")
                        )
                    ),
                    mapOf(
                        "id" to 327L,
                        "level" to 33,
                        "native" to "Magtutudlo ko sa eskwelahan.",
                        "words" to listOf("Magtutudlo", "ko", "sa", "eskwelahan"),
                        "translations" to mapOf(
                            "ja" to mapOf("meaning" to "私は学校で教師をしています"),
                            "en" to mapOf("meaning" to "I'm a teacher at the school")
                        )
                    ),
                    mapOf(
                        "id" to 328L,
                        "level" to 33,
                        "native" to "Pila ka nagsugod og pag-learn sa Bisaya?",
                        "words" to listOf("Pila", "ka", "nagsugod", "og", "pag-learn", "sa", "Bisaya"),
                        "translations" to mapOf(
                            "ja" to mapOf("meaning" to "ビサヤ語の学習を始めてどのくらいですか？"),
                            "en" to mapOf("meaning" to "How long have you been learning Bisaya?")
                        )
                    ),
                    mapOf(
                        "id" to 329L,
                        "level" to 33,
                        "native" to "Tulo ka bulan na ko.",
                        "words" to listOf("Tulo", "ka", "bulan", "na", "ko"),
                        "translations" to mapOf(
                            "ja" to mapOf("meaning" to "3ヶ月になります"),
                            "en" to mapOf("meaning" to "It's been three months")
                        )
                    ),
                    mapOf(
                        "id" to 330L,
                        "level" to 33,
                        "native" to "Maayo kaayo ang imong Binisaya.",
                        "words" to listOf("Maayo", "kaayo", "ang", "imong", "Binisaya"),
                        "translations" to mapOf(
                            "ja" to mapOf("meaning" to "あなたのビサヤ語はとても上手です"),
                            "en" to mapOf("meaning" to "Your Bisaya is very good")
                        )
                    )
                )
                
                // LV34: トラブル・サバイバル編（価格交渉・移動）
                val level34Data = listOf(
                    mapOf(
                        "id" to 331L,
                        "level" to 34,
                        "native" to "Mahimo ba nga paubos ang presyo?",
                        "words" to listOf("Mahimo", "ba", "nga", "paubos", "ang", "presyo"),
                        "translations" to mapOf(
                            "ja" to mapOf("meaning" to "値下げしてもらえませんか？"),
                            "en" to mapOf("meaning" to "Can you lower the price?")
                        )
                    ),
                    mapOf(
                        "id" to 332L,
                        "level" to 34,
                        "native" to "Mas mahal kini sa akong budget.",
                        "words" to listOf("Mas", "mahal", "kini", "sa", "akong", "budget"),
                        "translations" to mapOf(
                            "ja" to mapOf("meaning" to "これは私の予算より高いです"),
                            "en" to mapOf("meaning" to "This is more than my budget")
                        )
                    ),
                    mapOf(
                        "id" to 333L,
                        "level" to 34,
                        "native" to "Pila kung pila ang last price?",
                        "words" to listOf("Pila", "kung", "pila", "ang", "last", "price"),
                        "translations" to mapOf(
                            "ja" to mapOf("meaning" to "最終価格はいくらですか？"),
                            "en" to mapOf("meaning" to "What's the final price?")
                        )
                    ),
                    mapOf(
                        "id" to 334L,
                        "level" to 34,
                        "native" to "Nawala ang akong cellphone.",
                        "words" to listOf("Nawala", "ang", "akong", "cellphone"),
                        "translations" to mapOf(
                            "ja" to mapOf("meaning" to "携帯をなくしました"),
                            "en" to mapOf("meaning" to "I lost my cellphone")
                        )
                    ),
                    mapOf(
                        "id" to 335L,
                        "level" to 34,
                        "native" to "Palihug tawag ang pulis.",
                        "words" to listOf("Palihug", "tawag", "ang", "pulis"),
                        "translations" to mapOf(
                            "ja" to mapOf("meaning" to "警察を呼んでください"),
                            "en" to mapOf("meaning" to "Please call the police")
                        )
                    ),
                    mapOf(
                        "id" to 336L,
                        "level" to 34,
                        "native" to "Nasakit ko ang tiyan.",
                        "words" to listOf("Nasakit", "ko", "ang", "tiyan"),
                        "translations" to mapOf(
                            "ja" to mapOf("meaning" to "お腹が痛いです"),
                            "en" to mapOf("meaning" to "My stomach hurts")
                        )
                    ),
                    mapOf(
                        "id" to 337L,
                        "level" to 34,
                        "native" to "Asa ang pinakasentro nga ospital?",
                        "words" to listOf("Asa", "ang", "pinakasentro", "nga", "ospital"),
                        "translations" to mapOf(
                            "ja" to mapOf("meaning" to "中心病院はどこですか？"),
                            "en" to mapOf("meaning" to "Where is the central hospital?")
                        )
                    ),
                    mapOf(
                        "id" to 338L,
                        "level" to 34,
                        "native" to "Kinahanglan ko og tambal.",
                        "words" to listOf("Kinahanglan", "ko", "og", "tambal"),
                        "translations" to mapOf(
                            "ja" to mapOf("meaning" to "薬が必要です"),
                            "en" to mapOf("meaning" to "I need medicine")
                        )
                    ),
                    mapOf(
                        "id" to 339L,
                        "level" to 34,
                        "native" to "Na-delay ang akong flight.",
                        "words" to listOf("Na-delay", "ang", "akong", "flight"),
                        "translations" to mapOf(
                            "ja" to mapOf("meaning" to "フライトが遅れました"),
                            "en" to mapOf("meaning" to "My flight was delayed")
                        )
                    ),
                    mapOf(
                        "id" to 340L,
                        "level" to 34,
                        "native" to "Unsaon nako pagpuyo sa hotel?",
                        "words" to listOf("Unsaon", "nako", "pagpuyo", "sa", "hotel"),
                        "translations" to mapOf(
                            "ja" to mapOf("meaning" to "どうやってホテルに泊まればよいですか？"),
                            "en" to mapOf("meaning" to "How do I check into the hotel?")
                        )
                    )
                )
                
                // LV35: タリとの深い会話編（感情・好意）
                val level35Data = listOf(
                    mapOf(
                        "id" to 341L,
                        "level" to 35,
                        "native" to "Gihigugma kaayo tika.",
                        "words" to listOf("Gihigugma", "kaayo", "tika"),
                        "translations" to mapOf(
                            "ja" to mapOf("meaning" to "私はあなたを深く愛しています"),
                            "en" to mapOf("meaning" to "I love you so deeply")
                        )
                    ),
                    mapOf(
                        "id" to 342L,
                        "level" to 35,
                        "native" to "Ikaw ang akong kalipay.",
                        "words" to listOf("Ikaw", "ang", "akong", "kalipay"),
                        "translations" to mapOf(
                            "ja" to mapOf("meaning" to "あなたは私の幸せです"),
                            "en" to mapOf("meaning" to "You are my happiness")
                        )
                    ),
                    mapOf(
                        "id" to 343L,
                        "level" to 35,
                        "native" to "Dili ko makabaya bisan wala ka.",
                        "words" to listOf("Dili", "ko", "makabaya", "bisan", "wala", "ka"),
                        "translations" to mapOf(
                            "ja" to mapOf("meaning" to "あなたがいなくても生きていけません"),
                            "en" to mapOf("meaning" to "I can't live without you")
                        )
                    ),
                    mapOf(
                        "id" to 344L,
                        "level" to 35,
                        "native" to "Gusto ko nga makig-uban sa imo hangtod sa katapusan.",
                        "words" to listOf("Gusto", "ko", "nga", "makig-uban", "sa", "imo", "hangtod", "sa", "katapusan"),
                        "translations" to mapOf(
                            "ja" to mapOf("meaning" to "最後まであなたと一緒にいたいです"),
                            "en" to mapOf("meaning" to "I want to be with you until the end")
                        )
                    ),
                    mapOf(
                        "id" to 345L,
                        "level" to 35,
                        "native" to "Ang imong ngiting makapahinumdum kanako sa adlaw.",
                        "words" to listOf("Ang", "imong", "ngiting", "makapahinumdum", "kanako", "sa", "adlaw"),
                        "translations" to mapOf(
                            "ja" to mapOf("meaning" to "あなたの笑顔が一日中私を元気にします"),
                            "en" to mapOf("meaning" to "Your smile reminds me throughout the day")
                        )
                    ),
                    mapOf(
                        "id" to 346L,
                        "level" to 35,
                        "native" to "Sa tanang tawo, ikaw ang pinakapili ko.",
                        "words" to listOf("Sa", "tanang", "tawo", "ikaw", "ang", "pinakapili", "ko"),
                        "translations" to mapOf(
                            "ja" to mapOf("meaning" to "すべての人の中で、あなたが一番選びたい人です"),
                            "en" to mapOf("meaning" to "Among all people, you are my chosen one")
                        )
                    ),
                    mapOf(
                        "id" to 347L,
                        "level" to 35,
                        "native" to "Gikan sa atong pagkita, nahigugma ako nimo.",
                        "words" to listOf("Gikan", "sa", "atong", "pagkita", "nahigugma", "ako", "nimo"),
                        "translations" to mapOf(
                            "ja" to mapOf("meaning" to "会った瞬間から、あなたに恋をしました"),
                            "en" to mapOf("meaning" to "From the moment we met, I fell in love with you")
                        )
                    ),
                    mapOf(
                        "id" to 348L,
                        "level" to 35,
                        "native" to "Ang akong kasingkasing milukad sa imong ngalan.",
                        "words" to listOf("Ang", "akong", "kasingkasing", "milukad", "sa", "imong", "ngalan"),
                        "translations" to mapOf(
                            "ja" to mapOf("meaning" to "私の心臓があなたの名前を呼んでいます"),
                            "en" to mapOf("meaning" to "My heart calls your name")
                        )
                    ),
                    mapOf(
                        "id" to 349L,
                        "level" to 35,
                        "native" to "Hinaut nga ang atong gugma magpabilin hangtod sa katapusan.",
                        "words" to listOf("Hinaut", "nga", "ang", "atong", "gugma", "magpabilin", "hangtod", "sa", "katapusan"),
                        "translations" to mapOf(
                            "ja" to mapOf("meaning" to "私たちの愛が永遠に続くことを願っています"),
                            "en" to mapOf("meaning" to "I hope our love lasts forever")
                        )
                    ),
                    mapOf(
                        "id" to 350L,
                        "level" to 35,
                        "native" to "Ikaw ra ang akong kinabuhi ug kasingkasing.",
                        "words" to listOf("Ikaw", "ra", "ang", "akong", "kinabuhi", "ug", "kasingkasing"),
                        "translations" to mapOf(
                            "ja" to mapOf("meaning" to "あなたは私の人生と心のすべてです"),
                            "en" to mapOf("meaning" to "You are my life and my heart")
                        )
                    )
                )
                
                allData.addAll(level32Data)
                allData.addAll(level33Data)
                allData.addAll(level34Data)
                allData.addAll(level35Data)
                
                firebaseManager.addListeningPracticeData(allData)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * すべてのリスニングデータを取得
     */
    suspend fun getAllListeningData(): Result<List<Map<String, Any>>> {
        return withContext(Dispatchers.IO) {
            firebaseManager.getListeningPracticeData()
        }
    }
    
    /**
     * 特定IDのリスニングデータを取得
     */
    suspend fun getListeningDataById(id: Long): Result<Map<String, Any>?> {
        return withContext(Dispatchers.IO) {
            firebaseManager.getListeningPracticeById(id)
        }
    }
}
