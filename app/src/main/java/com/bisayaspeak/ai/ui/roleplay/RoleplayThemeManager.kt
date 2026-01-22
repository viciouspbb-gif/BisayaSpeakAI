package com.bisayaspeak.ai.ui.roleplay

import kotlin.random.Random

enum class RoleplayThemeFlavor { CASUAL, SCENARIO }

data class EndingCue(
    val bisaya: String,
    val translation: String,
    val explanation: String
)

data class RoleplayThemeDefinition(
    val id: String,
    val title: String,
    val description: String,
    val instruction: String,
    val attributePanels: List<String>,
    val flavor: RoleplayThemeFlavor,
    val persona: String,
    val goalStatement: String,
    val introLine: String,
    val closingCue: EndingCue,
    val minLevel: Int = 1,
    val maxLevel: Int = 30
)

class RoleplayThemeManager(
    private val random: Random = Random(System.currentTimeMillis())
) {

    private val historyLimit = 2

    private val themePools: Map<RoleplayThemeFlavor, List<RoleplayThemeDefinition>> = mapOf(
        RoleplayThemeFlavor.CASUAL to listOf(
            RoleplayThemeDefinition(
                id = "casual_walk",
                title = "朝の散歩道",
                description = "他愛もない近況や天気の話",
                instruction = "軽くおどけた調子で、散歩中の出来事に触れながら会話して。",
                attributePanels = listOf(
                    "Deep dive: 好きな景色",
                    "Scenery: 朝焼け",
                    "Joke: ねぼけ顔チェック"
                ),
                flavor = RoleplayThemeFlavor.CASUAL,
                persona = "散歩中の幼なじみ",
                goalStatement = "近況を楽しく共有してウォームアップする",
                introLine = "今日は散歩しながら色々おしゃべりしようね",
                closingCue = EndingCue(
                    "Nakalimot ko sa akong gibuhi!",
                    "あっ、やること思い出した！",
                    "雑談の途中で用事を思い出して離れるときの軽い一言"
                ),
                minLevel = 1,
                maxLevel = 8
            ),
            RoleplayThemeDefinition(
                id = "casual_cafe",
                title = "カフェ雑談",
                description = "飲み物やお気に入りの店の話",
                instruction = "ふんわり甘い雰囲気で、飲み物をシェアしながら雑談して。",
                attributePanels = listOf(
                    "Deep dive: 最近のご褒美",
                    "Scenery: カフェの音",
                    "Joke: 砂糖入れすぎ"
                ),
                flavor = RoleplayThemeFlavor.CASUAL,
                persona = "お気に入りカフェの常連",
                goalStatement = "好きな飲み物を語り合って仲を深める",
                introLine = "カフェでゆっくりしながら話そっか",
                closingCue = EndingCue(
                    "Tulog nako kay gabii na.",
                    "もう夜だから寝なきゃ。",
                    "夜更かしをやめて一緒に切り上げるときの優しい別れ方"
                ),
                minLevel = 4,
                maxLevel = 14
            ),
            RoleplayThemeDefinition(
                id = "casual_gossip",
                title = "たわいのない噂話",
                description = "友達や家族の近況",
                instruction = "タリらしく茶目っ気たっぷりに、でも優しく。",
                attributePanels = listOf(
                    "Deep dive: 最近聞いた面白い話",
                    "Scenery: 風の音",
                    "Joke: 内緒話"
                ),
                flavor = RoleplayThemeFlavor.CASUAL,
                persona = "おしゃべり好きな友達",
                goalStatement = "お互いの近況アップデートで親密さを保つ",
                introLine = "ちょっと噂話タイムにしよ？",
                closingCue = EndingCue(
                    "Oh, naa na ang akong amiga!",
                    "あ、友達が来ちゃった！",
                    "別の友達が現れて席を立つときに使う砕けた言い回し"
                ),
                minLevel = 6,
                maxLevel = 20
            )
        ),
        RoleplayThemeFlavor.SCENARIO to listOf(
            RoleplayThemeDefinition(
                id = "scene_shopping",
                title = "ローカル市場でお買い物",
                description = "タリが屋台の店員や案内役としてふるまう",
                instruction = "商品の魅力をBisayaで伝えつつ、値引き交渉やおすすめを提案して。",
                attributePanels = listOf(
                    "Deep dive: 食材の使い道",
                    "Scenery: にぎやかな市場",
                    "Joke: 試食しすぎ"
                ),
                flavor = RoleplayThemeFlavor.SCENARIO,
                persona = "市場の元気な売り子",
                goalStatement = "欲しい商品を決めて気持ちよく買い物を終える",
                introLine = "今日は私が市場案内するね！",
                closingCue = EndingCue(
                    "Balik ta sunod para sa promo!",
                    "また次のセールで会おうね！",
                    "楽しい買い物を締めて次のチャンスを約束するときのフレーズ"
                ),
                minLevel = 5,
                maxLevel = 18
            ),
            RoleplayThemeDefinition(
                id = "scene_romance",
                title = "恋愛相談",
                description = "タリが恋の先輩になってアドバイス",
                instruction = "ちょっと照れながらも、相手を励ます方向で。",
                attributePanels = listOf(
                    "Deep dive: 気持ちの整理",
                    "Scenery: 夕暮れ",
                    "Joke: 嫉妬してみる"
                ),
                flavor = RoleplayThemeFlavor.SCENARIO,
                persona = "恋の先輩",
                goalStatement = "相談を整理して次の一歩を後押しする",
                introLine = "恋バナモードに切り替えるよ",
                closingCue = EndingCue(
                    "Ay, mag-report pa ko sa love committee!",
                    "ラブ委員会に報告あるの！",
                    "恋愛相談を切り上げるときの冗談めいた別れのあいさつ"
                ),
                minLevel = 8,
                maxLevel = 24
            ),
            RoleplayThemeDefinition(
                id = "scene_hotel",
                title = "ホテルでトラブル解決",
                description = "フロント係として、部屋やサービスの相談を聞く",
                instruction = "丁寧かつフレンドリーに、問題を一緒に解決するノリで。",
                attributePanels = listOf(
                    "Deep dive: サービス案内",
                    "Scenery: ロビー",
                    "Joke: VIP扱いごっこ"
                ),
                flavor = RoleplayThemeFlavor.SCENARIO,
                persona = "ホテルのフロント係",
                goalStatement = "宿泊客の困りごとを解決し安心させる",
                introLine = "私はフロント担当タリだよ。何があったか教えて",
                closingCue = EndingCue(
                    "Mission complete, balik ta sunod!",
                    "任務完了！また来てね！",
                    "問題を解決できた時に達成感を共有しながら別れる表現"
                ),
                minLevel = 10,
                maxLevel = 30
            ),
            RoleplayThemeDefinition(
                id = "t_taxi_negotiation",
                title = "タクシー交渉",
                description = "メーター使用を拒む運転手との駆け引き",
                instruction = "落ち着いた声で、Bisayaで正当な料金と安全を主張して。",
                attributePanels = listOf(
                    "Deep dive: 行き先の確認",
                    "Scenery: 夕方の渋滞",
                    "Joke: チョコレートは渡さないよ"
                ),
                flavor = RoleplayThemeFlavor.SCENARIO,
                persona = "交渉上手な旅の相棒",
                goalStatement = "正規料金でタクシーを利用する",
                introLine = "タクシー運転手さん、メーター使ってもらおうね。私が交渉するから任せて",
                closingCue = EndingCue(
                    "Nakuha ra nato ang sakto nga presyo!",
                    "ちゃんと正規料金で乗れたね！",
                    "交渉成功を称えて別れるときの誇らしげな言い回し"
                ),
                minLevel = 5,
                maxLevel = 30
            ),
            RoleplayThemeDefinition(
                id = "t_market_bargain",
                title = "市場の値切り",
                description = "観光客価格を吹っ掛ける店主との駆け引き",
                instruction = "陽気さを保ちつつ、相場感を伝えて粘り強く交渉して。",
                attributePanels = listOf(
                    "Deep dive: 品質チェック",
                    "Scenery: にぎやかな露店",
                    "Joke: “友達価格ないの？”"
                ),
                flavor = RoleplayThemeFlavor.SCENARIO,
                persona = "値切りに強い地元案内人",
                goalStatement = "適正価格まで値下げさせる",
                introLine = "観光客価格って言われた？私が上手に値切ってあげる",
                closingCue = EndingCue(
                    "Kugi kaayo ta! Nakuha nato ang best deal!",
                    "粘った甲斐あっていい値段で買えたよ！",
                    "一緒に粘り勝ちした達成感を共有する別れの締め"
                ),
                minLevel = 5,
                maxLevel = 30
            ),
            RoleplayThemeDefinition(
                id = "t_business_greeting",
                title = "ビジネス挨拶",
                description = "フォーマルな場での自己紹介と信頼構築",
                instruction = "落ち着いた礼儀正しいトーンで、信用を意識して会話する。",
                attributePanels = listOf(
                    "Deep dive: 経験と強み",
                    "Scenery: 会議室",
                    "Joke: 時差ボケネタ"
                ),
                flavor = RoleplayThemeFlavor.SCENARIO,
                persona = "頼れるビジネスパートナー",
                goalStatement = "初対面の相手に好印象で自己紹介する",
                introLine = "今日はフォーマルな挨拶を練習しよう。私が先に見本を見せるね",
                closingCue = EndingCue(
                    "Great impression! Kita kits sa next meeting!",
                    "ばっちり印象残せたね。次の会議で会おう！",
                    "ビジネスシーンで良い印象を残したあとに再会を約束するあいさつ"
                ),
                minLevel = 7,
                maxLevel = 30
            ),
            RoleplayThemeDefinition(
                id = "t_office_consultation",
                title = "同僚への相談",
                description = "ミスをした仲間へのフォローと解決策の提案",
                instruction = "寄り添う口調で原因を整理し、建設的な案をBisayaで示して。",
                attributePanels = listOf(
                    "Deep dive: 課題の整理",
                    "Scenery: オフィスの休憩スペース",
                    "Joke: “コーヒーは私のおごりね”"
                ),
                flavor = RoleplayThemeFlavor.SCENARIO,
                persona = "頼れる同僚",
                goalStatement = "同僚を励ましながら解決策を決める",
                introLine = "落ち込んでる同僚を支えてあげよう。私が話の切り出し方をリードするね",
                closingCue = EndingCue(
                    "Teamwork solved it! Pahuway ta gamay.",
                    "チームワークで解決！少し休憩しよ。",
                    "チームで頑張った後に労い合う優しい締め言葉"
                ),
                minLevel = 8,
                maxLevel = 30
            ),
            RoleplayThemeDefinition(
                id = "t_hospital_emergency",
                title = "病院の受付",
                description = "症状を正確に伝えて迅速な対応を得る",
                instruction = "緊張感の中でもはっきりと症状・時間・既往歴を伝える。",
                attributePanels = listOf(
                    "Deep dive: 痛みの場所・度合い",
                    "Scenery: 夜間受付",
                    "Joke: “看護師さんのビサヤ語速すぎ！”"
                ),
                flavor = RoleplayThemeFlavor.SCENARIO,
                persona = "丁寧な同行サポーター",
                goalStatement = "受付に必要情報を伝え診察へ繋ぐ",
                introLine = "落ち着いて。受付で症状を順番に説明しよう。私が順序をサポートする",
                closingCue = EndingCue(
                    "Narespondehan dayon! Salamat sa imong kusog nga Bisaya.",
                    "すぐ診てもらえたよ！落ち着いて伝えられてえらい！",
                    "緊急対応がうまくいった相手を称える安心の一言"
                ),
                minLevel = 9,
                maxLevel = 30
            )
        )
    )

    private val deckState = mutableMapOf<RoleplayThemeFlavor, ArrayDeque<RoleplayThemeDefinition>>()
    private val historyState = mutableMapOf<RoleplayThemeFlavor, ArrayDeque<String>>()

    fun drawTheme(level: Int, flavor: RoleplayThemeFlavor): RoleplayThemeDefinition {
        val pool = themePools[flavor]?.filter { level in it.minLevel..it.maxLevel }.orEmpty()
        val fallbackPool = themePools[flavor].orEmpty()
        val candidates = (if (pool.isEmpty()) fallbackPool else pool).ifEmpty { fallbackPool }
        if (candidates.isEmpty()) {
            error("No themes available for flavor $flavor")
        }

        val deck = deckState.getOrPut(flavor) { ArrayDeque() }
        if (deck.isEmpty()) {
            deck.addAll(candidates.shuffled(random))
        }

        var selection = deck.removeFirst()
        val history = historyState.getOrPut(flavor) { ArrayDeque() }
        if (candidates.size > 1 && history.contains(selection.id)) {
            var attempts = 0
            while (history.contains(selection.id) && attempts < candidates.size) {
                if (deck.isEmpty()) {
                    deck.addAll(candidates.shuffled(random))
                }
                selection = deck.removeFirst()
                attempts++
            }
        }

        history.addLast(selection.id)
        if (history.size > historyLimit) {
            history.removeFirst()
        }

        return selection
    }
}
