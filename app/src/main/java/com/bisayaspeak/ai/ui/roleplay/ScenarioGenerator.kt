package com.bisayaspeak.ai.ui.roleplay

import kotlin.random.Random

/**
 * Provides dynamic everyday situations for Tari free-talk sessions.
 */
data class LocalizedText(val ja: String, val en: String) {
    fun resolve(isJapanese: Boolean): String = if (isJapanese) ja else en
}

data class DynamicScenarioTemplate(
    val id: String,
    val label: LocalizedText,
    val description: LocalizedText,
    val tariRole: LocalizedText,
    val userRole: LocalizedText,
    val guidance: LocalizedText,
    val keywords: List<String>
) {
    fun matches(input: String): Boolean {
        val normalized = input.lowercase()
        return keywords.any { keyword ->
            val token = keyword.lowercase()
            normalized.contains(token)
        }
    }
}

class ScenarioGenerator(private val random: Random) {

    private val templates: List<DynamicScenarioTemplate> = listOf(
        DynamicScenarioTemplate(
            id = "cafe_order",
            label = LocalizedText("カフェの注文", "Café Order"),
            description = LocalizedText(
                "セブの人気カフェ。初めて来店した客におすすめを提案する。",
                "A popular Cebu café where a first-time guest asks for recommendations."
            ),
            tariRole = LocalizedText("タリ=店員/バリスタ", "Tari = barista"),
            userRole = LocalizedText("ユーザー=初来店の客", "User = first-time guest"),
            guidance = LocalizedText(
                "メニュー説明や味の特徴を交えつつ、丁寧語とビサヤ語を織り交ぜて案内する。",
                "Explain menu highlights, flavor notes, and upsell kindly using Bisaya phrases."
            ),
            keywords = listOf("cafe", "coffee", "latte", "カフェ", "コーヒー", "注文")
        ),
        DynamicScenarioTemplate(
            id = "hospital_reception",
            label = LocalizedText("病院の受付", "Hospital Reception"),
            description = LocalizedText(
                "総合病院の受付。体調不良の患者を落ち着かせながら情報を確認する。",
                "A hospital front desk calming an unwell patient while gathering details."
            ),
            tariRole = LocalizedText("タリ=受付スタッフ", "Tari = receptionist"),
            userRole = LocalizedText("ユーザー=体調不良で訪れた患者", "User = patient seeking care"),
            guidance = LocalizedText(
                "丁寧な敬語と医療関連のビサヤ語ボキャブラリーで症状や保険情報を確認する。",
                "Use polite Bisaya medical vocabulary to ask about symptoms and paperwork."
            ),
            keywords = listOf("hospital", "clinic", "doctor", "ill", "sick", "病院", "受付", "体調")
        ),
        DynamicScenarioTemplate(
            id = "hotel_checkin",
            label = LocalizedText("ホテルのチェックイン", "Hotel Check-in"),
            description = LocalizedText(
                "ビーチサイドホテルのロビー。予約名や宿泊プランを確認する。",
                "Beach hotel lobby confirming reservation names and stay details."
            ),
            tariRole = LocalizedText("タリ=フロントクラーク", "Tari = front-desk agent"),
            userRole = LocalizedText("ユーザー=宿泊客", "User = arriving guest"),
            guidance = LocalizedText(
                "丁寧な敬語と旅行/宿泊に関するビサヤ語を交え、アップグレード提案も行う。",
                "Blend Bisaya hospitality phrases with polite service language and upgrade offers."
            ),
            keywords = listOf("hotel", "check-in", "room", "reservation", "宿泊", "ホテル", "チェックイン")
        ),
        DynamicScenarioTemplate(
            id = "taxi_ride",
            label = LocalizedText("タクシー移動", "Taxi Ride"),
            description = LocalizedText(
                "空港前で乗車したばかりのタクシー。行き先と料金を相談する。",
                "A taxi ride right after leaving the airport, discussing route and fare."
            ),
            tariRole = LocalizedText("タリ=運転手", "Tari = driver"),
            userRole = LocalizedText("ユーザー=乗客", "User = passenger"),
            guidance = LocalizedText(
                "渋滞状況やルートを提案しつつ、軽い世間話とビサヤ語の交通用語を使う。",
                "Offer route suggestions, traffic notes, and light banter with Bisaya transport terms."
            ),
            keywords = listOf("taxi", "cab", "grab", "ride", "タクシー", "配車", "移動")
        ),
        DynamicScenarioTemplate(
            id = "business_pitch",
            label = LocalizedText("ビジネス交渉", "Business Negotiation"),
            description = LocalizedText(
                "シェアオフィスの会議室。新商品の提案をする営業と取引先の打ち合わせ。",
                "Co-working meeting room where a salesperson pitches a new product to a partner."
            ),
            tariRole = LocalizedText("タリ=取引先担当者", "Tari = partner representative"),
            userRole = LocalizedText("ユーザー=営業担当", "User = salesperson"),
            guidance = LocalizedText(
                "敬語とビジネス用語を織り交ぜ、条件交渉やフィードバックを論理的に返す。",
                "Use business Bisaya terms, negotiate politely, and provide structured feedback."
            ),
            keywords = listOf("business", "deal", "pitch", "proposal", "sales", "交渉", "営業", "取引")
        ),
        DynamicScenarioTemplate(
            id = "directions",
            label = LocalizedText("道案内", "Giving Directions"),
            description = LocalizedText(
                "繁華街の交差点。観光客が目的地を探してタリに道を尋ねる。",
                "Busy downtown intersection where a tourist asks for directions."
            ),
            tariRole = LocalizedText("タリ=地元の案内人", "Tari = helpful local"),
            userRole = LocalizedText("ユーザー=観光客", "User = tourist"),
            guidance = LocalizedText(
                "目印や徒歩/ジプニーの行き方をビサヤ語で説明し、安心させる表現を添える。",
                "Describe landmarks, jeepney routes, and reassure the tourist using Bisaya phrases."
            ),
            keywords = listOf("direction", "way", "lost", "tourist", "guide", "道", "案内", "迷っ")
        ),
        DynamicScenarioTemplate(
            id = "market_bargain_dynamic",
            label = LocalizedText("市場（パレンケ）での買い物", "Market Bargaining"),
            description = LocalizedText(
                "カルボン市場の活気ある通り。値切り交渉で盛り上がる。",
                "Bustling Carbon market lane filled with spirited haggling."
            ),
            tariRole = LocalizedText("タリ=威勢のいい店主", "Tari = lively vendor"),
            userRole = LocalizedText("ユーザー=値切り交渉をする客", "User = bargaining shopper"),
            guidance = LocalizedText(
                "勢いのあるトーンで価格交渉を展開しつつ、冗談やサービスを織り交ぜる。",
                "Use energetic Bisaya, playful teasing, and creative bundling tactics during haggling."
            ),
            keywords = listOf("market", "palengke", "bargain", "haggle", "price", "市場", "パレンケ", "値切")
        ),
        DynamicScenarioTemplate(
            id = "jeepney_commute",
            label = LocalizedText("ジプニー移動", "Jeepney Commute"),
            description = LocalizedText(
                "込み合ったジプニーで隣に座ったタリに合図と支払いを伝える。",
                "Inside a crowded jeepney, coordinating fare and stop requests with Tari."
            ),
            tariRole = LocalizedText("タリ=車掌/ベテラン乗客", "Tari = fare collector / seasoned commuter"),
            userRole = LocalizedText("ユーザー=乗客", "User = rider"),
            guidance = LocalizedText(
                "停車合図や乗り換え情報を短いビサヤ語で伝え、ローカルの雰囲気を描写する。",
                "Use brisk Bisaya commands (bayad, lugar, para) and share quick commute tips."
            ),
            keywords = listOf("lugar", "bayad", "stop", "ride", "jeepney", "jeep")
        ),
        DynamicScenarioTemplate(
            id = "sari_sari_store",
            label = LocalizedText("サリサリストア", "Sari-sari Store"),
            description = LocalizedText(
                "近所のサリサリで駄菓子や生活品をまとめ買いする。",
                "Neighborhood sari-sari store stop for candies and daily needs."
            ),
            tariRole = LocalizedText("タリ=店番", "Tari = shopkeeper"),
            userRole = LocalizedText("ユーザー=常連客", "User = regular shopper"),
            guidance = LocalizedText(
                "小銭計算やおまけ交渉をビサヤ語で行い、親しみのある口調で対応する。",
                "Handle small change, freebies, and playful banter in Bisaya."
            ),
            keywords = listOf("buy", "candy", "change", "palit", "sari", "store")
        ),
        DynamicScenarioTemplate(
            id = "friend_house_visit",
            label = LocalizedText("友人宅の訪問", "Friend's House"),
            description = LocalizedText(
                "友人の家に招かれ、食事や雑談を楽しむ。",
                "Visiting a friend’s home for food, stories, and bonding."
            ),
            tariRole = LocalizedText("タリ=友人", "Tari = close friend"),
            userRole = LocalizedText("ユーザー=招待客", "User = guest"),
            guidance = LocalizedText(
                "食事の誘い (kaon) や近況報告をビサヤ語で交わし、家庭的なムードを演出する。",
                "Use cozy Bisaya phrases about food (kaon ta), updates, and invitations."
            ),
            keywords = listOf("visit", "eat", "friend", "invite", "kaon", "house")
        ),
        DynamicScenarioTemplate(
            id = "airport_troubleshooter",
            label = LocalizedText("空港サポート", "Airport Assistance"),
            description = LocalizedText(
                "空港で荷物や搭乗手続きに困る旅行者をサポート。",
                "Helping at the airport with luggage, tickets, and check-in issues."
            ),
            tariRole = LocalizedText("タリ=空港スタッフ", "Tari = airport staff"),
            userRole = LocalizedText("ユーザー=旅行客", "User = traveler"),
            guidance = LocalizedText(
                "フライト情報や紛失案内を落ち着いたビサヤ語で伝え、安心させる。",
                "Provide calm Bisaya guidance about flights, lost luggage, and check-in steps."
            ),
            keywords = listOf("lost", "luggage", "flight", "ticket", "check-in", "airport")
        ),
        DynamicScenarioTemplate(
            id = "salon_session",
            label = LocalizedText("理容室・サロン", "Barbershop / Salon"),
            description = LocalizedText(
                "ローカルサロンでヘアカットやスタイル相談をする。",
                "Local salon visit discussing haircut styles and trimming details."
            ),
            tariRole = LocalizedText("タリ=スタイリスト", "Tari = stylist"),
            userRole = LocalizedText("ユーザー=お客", "User = customer"),
            guidance = LocalizedText(
                "長さやスタイルの希望、シェービング指示をビサヤ語で確認する。",
                "Clarify preferred length, style, and shave details using stylish Bisaya expressions."
            ),
            keywords = listOf("cut", "hair", "short", "style", "shave", "salon", "barber")
        )
    )

    private val templateMap: Map<String, DynamicScenarioTemplate> = templates.associateBy { it.id }
    private val priorityScenarioIds = listOf("hospital_reception", "cafe_order", "hotel_checkin", "taxi_ride")
    private val priorityScenarios: List<DynamicScenarioTemplate> = priorityScenarioIds.mapNotNull { templateMap[it] }

    fun selectScenario(userMessage: String?, current: DynamicScenarioTemplate?): DynamicScenarioTemplate {
        val sanitized = userMessage?.trim().orEmpty()
        val matched = sanitized
            .takeIf { it.isNotEmpty() }
            ?.let { message -> templates.firstOrNull { it.matches(message) } }
        if (matched != null) {
            return matched
        }

        val previousId = current?.id
        val sanitizedBlank = sanitized.isBlank()
        val prioritizedPool = if (sanitizedBlank) {
            priorityScenarios.filter { it.id != previousId }
        } else emptyList()

        val pool = prioritizedPool.takeIf { it.isNotEmpty() }
            ?: templates.filter { it.id != previousId }

        if (pool.isEmpty()) {
            return templates.random(random)
        }

        return pool[random.nextInt(pool.size)]
    }

    fun shuffledTemplates(): List<DynamicScenarioTemplate> {
        return templates.shuffled(random)
    }
}
