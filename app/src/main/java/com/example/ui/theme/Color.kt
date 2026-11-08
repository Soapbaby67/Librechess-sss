package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Move Classification Semantic Colors (Chess.com & Lichess Game Review)
val BrilliantCyan = Color(0xFF1BACA6)
val GreatMoveBlue = Color(0xFF5C8BB0)
val BestMoveGreen = Color(0xFF81B64C)
val GoodMoveOlive = Color(0xFF96BC4B)
val BookMoveBrown = Color(0xFFA88865)
val InaccuracyYellow = Color(0xFFF7C631)
val MistakeOrange = Color(0xFFFFA459)
val BlunderRed = Color(0xFFFA412D)

enum class AppUiTheme(
    val id: String,
    val title: String,
    val subtitle: String,
    val badgeText: String,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val tertiary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val cardBorder: Color,
    val defaultBoardSkinId: String,
    val defaultPieceStyleId: String
) {
    TRUE_OLED(
        id = "TRUE_OLED",
        title = "True OLED Pitch Black",
        subtitle = "Абсолютный #000000 для AMOLED-экранов, высокий контраст и экономия батареи",
        badgeText = "OLED #000000",
        background = Color(0xFF000000),
        surface = Color(0xFF070709),
        surfaceVariant = Color(0xFF101216),
        primary = Color(0xFF00E676),
        onPrimary = Color(0xFF000000),
        secondary = Color(0xFF00E5FF),
        tertiary = Color(0xFFFFD54F),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFF9EA7B3),
        cardBorder = Color(0xFF1F242D),
        defaultBoardSkinId = "OLED_PITCH",
        defaultPieceStyleId = "OLED_WIREFRAME"
    ),
    CHESS_COM(
        id = "CHESS_COM",
        title = "Chess.com Classic Style",
        subtitle = "Теплый древесный графит #312E2B, фирменный зеленый #81B64C и бейджи Game Review",
        badgeText = "Chess.com Style",
        background = Color(0xFF262421),
        surface = Color(0xFF312E2B),
        surfaceVariant = Color(0xFF3D3935),
        primary = Color(0xFF81B64C),
        onPrimary = Color(0xFFFFFFFF),
        secondary = Color(0xFF1BACA6),
        tertiary = Color(0xFFF7C631),
        textPrimary = Color(0xFFF3F3F2),
        textSecondary = Color(0xFFB3B0AC),
        cardBorder = Color(0xFF47433E),
        defaultBoardSkinId = "CHESS_COM_GREEN",
        defaultPieceStyleId = "NEO_CLASSIC"
    ),
    OBSIDIAN_GOLD(
        id = "OBSIDIAN_GOLD",
        title = "Grandmaster Obsidian & Gold",
        subtitle = "Авторская гроссмейстерская тема: глубокий обсидиан и королевское золото",
        badgeText = "Авторская",
        background = Color(0xFF0D1015),
        surface = Color(0xFF151A22),
        surfaceVariant = Color(0xFF1E2530),
        primary = Color(0xFFE5A93C),
        onPrimary = Color(0xFF0D1015),
        secondary = Color(0xFF2ECC71),
        tertiary = Color(0xFF38BDF8),
        textPrimary = Color(0xFFF8FAFC),
        textSecondary = Color(0xFF94A3B8),
        cardBorder = Color(0xFF293241),
        defaultBoardSkinId = "OBSIDIAN_GOLD",
        defaultPieceStyleId = "IMPERIAL_GOLD"
    ),
    CYBERPUNK_NEON(
        id = "CYBERPUNK_NEON",
        title = "Cyberpunk Neon Matrix",
        subtitle = "Авторская кибер-тема: полуночный индиго, неоновый циан и пурпур",
        badgeText = "Авторская",
        background = Color(0xFF090414),
        surface = Color(0xFF130926),
        surfaceVariant = Color(0xFF1E103A),
        primary = Color(0xFF00F0FF),
        onPrimary = Color(0xFF090414),
        secondary = Color(0xFFFF2A85),
        tertiary = Color(0xFFB829FF),
        textPrimary = Color(0xFFF0F9FF),
        textSecondary = Color(0xFFA78BFA),
        cardBorder = Color(0xFF351B63),
        defaultBoardSkinId = "CYBERPUNK_GRID",
        defaultPieceStyleId = "CYBER_MINIMAL"
    ),
    SAKURA_INK(
        id = "SAKURA_INK",
        title = "Sakura & Imperial Ink",
        subtitle = "Авторская восточная тема: японская тушь, лепестки сакуры и теплый бамбук",
        badgeText = "Авторская",
        background = Color(0xFF141115),
        surface = Color(0xFF1F1A22),
        surfaceVariant = Color(0xFF2B242F),
        primary = Color(0xFFFF8FA3),
        onPrimary = Color(0xFF141115),
        secondary = Color(0xFFE9C46A),
        tertiary = Color(0xFF72EFDD),
        textPrimary = Color(0xFFFFF5F7),
        textSecondary = Color(0xFFCDB4DB),
        cardBorder = Color(0xFF3C3142),
        defaultBoardSkinId = "SAKURA_WOOD",
        defaultPieceStyleId = "FANTASY_CROWN"
    ),
    NORDIC_FROST(
        id = "NORDIC_FROST",
        title = "Nordic Emerald Frost",
        subtitle = "Авторская скандинавская тема: арктический сланец и северное сияние",
        badgeText = "Авторская",
        background = Color(0xFF0B1319),
        surface = Color(0xFF121F28),
        surfaceVariant = Color(0xFF1A2C38),
        primary = Color(0xFF2DD4BF),
        onPrimary = Color(0xFF0B1319),
        secondary = Color(0xFF38BDF8),
        tertiary = Color(0xFFA7F3D0),
        textPrimary = Color(0xFFF0FDFA),
        textSecondary = Color(0xFF99F6E4),
        cardBorder = Color(0xFF233B4B),
        defaultBoardSkinId = "ARCTIC_ICE",
        defaultPieceStyleId = "BAUHAUS_GEO"
    );

    companion object {
        fun fromId(id: String): AppUiTheme = values().find { it.id == id } ?: OBSIDIAN_GOLD
    }
}

enum class BoardSkin(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String,
    val lightSquare: Color,
    val darkSquare: Color,
    val borderColor: Color,
    val lastMoveHighlight: Color,
    val selectedHighlight: Color,
    val coordLightColor: Color,
    val coordDarkColor: Color,
    val hasWoodGrain: Boolean = false,
    val hasNeonGrid: Boolean = false,
    val hasMarbleVeins: Boolean = false,
    val isRetroPixel: Boolean = false
) {
    CHESS_COM_GREEN(
        id = "CHESS_COM_GREEN",
        title = "Chess.com Tournament Green",
        subtitle = "Фирменная турнирная зелено-кремовая доска (#769656 / #EEEED2)",
        category = "Chess.com",
        lightSquare = Color(0xFFEEEED2),
        darkSquare = Color(0xFF769656),
        borderColor = Color(0xFF262421),
        lastMoveHighlight = Color(0xAAF7F769),
        selectedHighlight = Color(0xBBBBCB2B),
        coordLightColor = Color(0xFF769656),
        coordDarkColor = Color(0xFFEEEED2)
    ),
    OLED_PITCH(
        id = "OLED_PITCH",
        title = "True OLED Pitch Black",
        subtitle = "Абсолютно черные поля #000000 с глубоким ониксом и неоновым контуром",
        category = "OLED",
        lightSquare = Color(0xFF181B20),
        darkSquare = Color(0xFF000000),
        borderColor = Color(0xFF00E676),
        lastMoveHighlight = Color(0x7700E676),
        selectedHighlight = Color(0x9900E5FF),
        coordLightColor = Color(0xFF64748B),
        coordDarkColor = Color(0xFF00E676),
        hasNeonGrid = true
    ),
    LICHESS_BROWN(
        id = "LICHESS_BROWN",
        title = "Lichess Classic Maple",
        subtitle = "Культовая древесная доска оригинального Lichess (#F0D9B5 / #B58863)",
        category = "Lichess",
        lightSquare = Color(0xFFF0D9B5),
        darkSquare = Color(0xFFB58863),
        borderColor = Color(0xFF5C4028),
        lastMoveHighlight = Color(0xAA9BC700),
        selectedHighlight = Color(0xBB14551E),
        coordLightColor = Color(0xFFB58863),
        coordDarkColor = Color(0xFFF0D9B5),
        hasWoodGrain = true
    ),
    OBSIDIAN_GOLD(
        id = "OBSIDIAN_GOLD",
        title = "Obsidian & Royal Gold",
        subtitle = "Авторская доска из полированного обсидиана с золотой инкрустацией",
        category = "Авторская",
        lightSquare = Color(0xFF333B48),
        darkSquare = Color(0xFF181D24),
        borderColor = Color(0xFFE5A93C),
        lastMoveHighlight = Color(0x88E5A93C),
        selectedHighlight = Color(0xAAF59E0B),
        coordLightColor = Color(0xFF94A3B8),
        coordDarkColor = Color(0xFFE5A93C)
    ),
    CYBERPUNK_GRID(
        id = "CYBERPUNK_GRID",
        title = "Cyberpunk Matrix Grid",
        subtitle = "Авторская голографическая сетка с циановым и пурпурным свечением",
        category = "Авторская",
        lightSquare = Color(0xFF28164B),
        darkSquare = Color(0xFF110626),
        borderColor = Color(0xFF00F0FF),
        lastMoveHighlight = Color(0x8800F0FF),
        selectedHighlight = Color(0x99FF2A85),
        coordLightColor = Color(0xFF00F0FF),
        coordDarkColor = Color(0xFFFF2A85),
        hasNeonGrid = true
    ),
    CARRARA_MARBLE(
        id = "CARRARA_MARBLE",
        title = "Royal Carrara Marble",
        subtitle = "Авторский итальянский мрамор с естественными каменными прожилками",
        category = "Авторская",
        lightSquare = Color(0xFFE5E9F0),
        darkSquare = Color(0xFF5E6C7A),
        borderColor = Color(0xFF2E3440),
        lastMoveHighlight = Color(0x88EBCB8B),
        selectedHighlight = Color(0x9988C0D0),
        coordLightColor = Color(0xFF4C566A),
        coordDarkColor = Color(0xFFECEFF4),
        hasMarbleVeins = true
    ),
    SAKURA_WOOD(
        id = "SAKURA_WOOD",
        title = "Sakura Rosewood",
        subtitle = "Авторское японское розовое дерево и нежный перламутр сакуры",
        category = "Авторская",
        lightSquare = Color(0xFFFCE7F3),
        darkSquare = Color(0xFF833857),
        borderColor = Color(0xFF4A1D31),
        lastMoveHighlight = Color(0x99F472B6),
        selectedHighlight = Color(0xAAFB7185),
        coordLightColor = Color(0xFF833857),
        coordDarkColor = Color(0xFFFCE7F3),
        hasWoodGrain = true
    ),
    ARCTIC_ICE(
        id = "ARCTIC_ICE",
        title = "Arctic Frosted Quartz",
        subtitle = "Авторский ледяной кварц и глубокий северный океан",
        category = "Авторская",
        lightSquare = Color(0xFFD8ECF8),
        darkSquare = Color(0xFF3E6B89),
        borderColor = Color(0xFF1B3A4B),
        lastMoveHighlight = Color(0x8838BDF8),
        selectedHighlight = Color(0xAA2DD4BF),
        coordLightColor = Color(0xFF2C5364),
        coordDarkColor = Color(0xFFE0F2FE)
    ),
    SYNTHWAVE_SUNSET(
        id = "SYNTHWAVE_SUNSET",
        title = "Synthwave Outrun 1984",
        subtitle = "Авторский ретровейв с закатным градиентом и лазерной сеткой",
        category = "Авторская",
        lightSquare = Color(0xFF4A1E47),
        darkSquare = Color(0xFF1F0C29),
        borderColor = Color(0xFFFF6B6B),
        lastMoveHighlight = Color(0x88FFE66D),
        selectedHighlight = Color(0xAAFF6B6B),
        coordLightColor = Color(0xFFFFE66D),
        coordDarkColor = Color(0xFF4ECDC4),
        hasNeonGrid = true
    ),
    RETRO_PIXEL(
        id = "RETRO_PIXEL",
        title = "Retro 8-Bit Arcade",
        subtitle = "Авторская пиксельная доска в стиле классических 8-битных автоматов",
        category = "Авторская",
        lightSquare = Color(0xFFCAD2C5),
        darkSquare = Color(0xFF52796F),
        borderColor = Color(0xFF2F3E46),
        lastMoveHighlight = Color(0x99F4A261),
        selectedHighlight = Color(0xAAE76F51),
        coordLightColor = Color(0xFF354F52),
        coordDarkColor = Color(0xFFCAD2C5),
        isRetroPixel = true
    );

    companion object {
        fun fromId(id: String): BoardSkin = values().find { it.id == id } ?: CHESS_COM_GREEN
    }
}

enum class PieceStyle(
    val id: String,
    val title: String,
    val subtitle: String,
    val badge: String,
    val whiteFill: Color,
    val whiteStroke: Color,
    val blackFill: Color,
    val blackStroke: Color,
    val accentGlow: Color
) {
    NEO_CLASSIC(
        id = "NEO_CLASSIC",
        title = "Neo-Classic (Chess.com Style)",
        subtitle = "Объемные скругленные силуэты с мягкими бликами и четким контуром",
        badge = "Chess.com",
        whiteFill = Color(0xFFFDFBF7),
        whiteStroke = Color(0xFF2B2927),
        blackFill = Color(0xFF312E2B),
        blackStroke = Color(0xFF141311),
        accentGlow = Color(0xFF81B64C)
    ),
    CBURNETT_LICHESS(
        id = "CBURNETT_LICHESS",
        title = "Cburnett (Lichess Standard)",
        subtitle = "Эталонная турнирная классика оригинального Lichess",
        badge = "Lichess",
        whiteFill = Color(0xFFFFFFFF),
        whiteStroke = Color(0xFF1E1E1E),
        blackFill = Color(0xFF222222),
        blackStroke = Color(0xFFE2E8F0),
        accentGlow = Color(0xFFD97706)
    ),
    OLED_WIREFRAME(
        id = "OLED_WIREFRAME",
        title = "OLED Neon Wireframe",
        subtitle = "Светящиеся неоновые векторные контуры на глубоком черном ядре",
        badge = "OLED",
        whiteFill = Color(0xFF0A191E),
        whiteStroke = Color(0xFF00F0FF),
        blackFill = Color(0xFF1A091E),
        blackStroke = Color(0xFFFF2A85),
        accentGlow = Color(0xFF00E676)
    ),
    IMPERIAL_GOLD(
        id = "IMPERIAL_GOLD",
        title = "Imperial Gold & Onyx",
        subtitle = "Авторский граненый набор из белого золота и черного оникса",
        badge = "Авторский",
        whiteFill = Color(0xFFFFF3C4),
        whiteStroke = Color(0xFF78350F),
        blackFill = Color(0xFF111827),
        blackStroke = Color(0xFFE5A93C),
        accentGlow = Color(0xFFF59E0B)
    ),
    BAUHAUS_GEO(
        id = "BAUHAUS_GEO",
        title = "Bauhaus Geometric",
        subtitle = "Авторский авангардный геометрический минимализм 1920-х",
        badge = "Авторский",
        whiteFill = Color(0xFFF1F5F9),
        whiteStroke = Color(0xFF0F172A),
        blackFill = Color(0xFF1E293B),
        blackStroke = Color(0xFF38BDF8),
        accentGlow = Color(0xFF38BDF8)
    ),
    FANTASY_CROWN(
        id = "FANTASY_CROWN",
        title = "Royal Medieval Fantasy",
        subtitle = "Авторские королевские силуэты с острыми коронами и щитами",
        badge = "Авторский",
        whiteFill = Color(0xFFFFF1F2),
        whiteStroke = Color(0xFF4C0519),
        blackFill = Color(0xFF2E1065),
        blackStroke = Color(0xFFF472B6),
        accentGlow = Color(0xFFC084FC)
    );

    companion object {
        fun fromId(id: String): PieceStyle = values().find { it.id == id } ?: NEO_CLASSIC
    }
}
