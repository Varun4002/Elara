/**
 * Elara Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.elara.music.lyrics

import android.text.format.DateUtils
import com.atilika.kuromoji.ipadic.Tokenizer
import com.github.promeg.pinyinhelper.Pinyin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

val LINE_REGEX = "((\\[\\d\\d:\\d\\d\\.\\d{2,3}\\] ?)+)(.*)".toRegex()
val TIME_REGEX = "\\[(\\d\\d):(\\d\\d)\\.(\\d{2,3})\\]".toRegex()

// Regex for rich sync format: [MM:SS.mm]<MM:SS.mm> word <MM:SS.mm> word ...
private val RICH_SYNC_LINE_REGEX = "\\[(\\d{1,2}):(\\d{2})\\.(\\d{2,3})\\](.*)".toRegex()
private val RICH_SYNC_WORD_REGEX = "<(\\d{1,2}):(\\d{2})\\.(\\d{2,3})>([^<]+)".toRegex()

// Regex for Paxsenix v1/v2/bg format
// [00:00.000]v1: <00:00.000>I <00:00.154>promise...
// [bg: <02:18.078>Yeah<02:19.341>]
private val PAXSENIX_AGENT_LINE_REGEX = "\\[(\\d{1,2}):(\\d{2})\\.(\\d{2,3})\\](v\\d+):\\s*(.*)".toRegex()
private val PAXSENIX_BG_LINE_REGEX = "^\\[bg:\\s*(.*)\\]$".toRegex()

// Regex for agent and background markers (existing format)
private val AGENT_REGEX = "\\{agent:([^}]+)\\}".toRegex()
private val BACKGROUND_REGEX = "^\\{bg\\}".toRegex()

@Suppress("RegExpRedundantEscape")
object LyricsUtils {
    fun cleanTitleForSearch(title: String): String {
        return title.replace(Regex("\\s*[(\\[].*?[)\\]]"), "").trim()
    }

    fun filterLyricsCreditLines(lyrics: String): String {
        return lyrics.lines().filter { line ->
            // Strip leading bracketed/braced content, version tags, and timestamps
            // Handles [00:00.00], {agent:v1}, {bg}, [bg: ...], v1: etc.
            var textContent = line.trim()
            
            // Repeatedly strip prefixes while they match common patterns
            var stripping = true
            while (stripping) {
                val prevLength = textContent.length
                textContent = textContent
                    .replaceFirst(Regex("^\\[\\d\\d:\\d\\d\\.\\d{2,3}\\]"), "")
                    .replaceFirst(Regex("^\\{agent:[^}]+\\}"), "")
                    .replaceFirst(Regex("^\\{bg\\}"), "")
                    .replaceFirst(Regex("^\\[bg:.*\\]"), "")
                    .replaceFirst(Regex("^v\\d+:"), "")
                    .trim()
                stripping = textContent.length < prevLength
            }

            val lowerText = textContent.lowercase(Locale.getDefault())
            
            val isCredit = lowerText.startsWith("synced by") ||
                    lowerText.startsWith("lyrics by") ||
                    lowerText.startsWith("music by") ||
                    lowerText.startsWith("arranged by") ||
                    (lowerText.startsWith("[") && lowerText.endsWith("]") && lowerText.length < 40 && lowerText.contains("synced by"))
            
            !isCredit
        }.joinToString("\n")
    }

    private val KANA_ROMAJI_MAP: Map<String, String> = mapOf(
        // Digraphs (YÅon - combinations like kya, sho)
        "ã‚­ãƒ£" to "kya", "ã‚­ãƒ¥" to "kyu", "ã‚­ãƒ§" to "kyo",
        "ã‚·ãƒ£" to "sha", "ã‚·ãƒ¥" to "shu", "ã‚·ãƒ§" to "sho",
        "ãƒãƒ£" to "cha", "ãƒãƒ¥" to "chu", "ãƒãƒ§" to "cho",
        "ãƒ‹ãƒ£" to "nya", "ãƒ‹ãƒ¥" to "nyu", "ãƒ‹ãƒ§" to "nyo",
        "ãƒ’ãƒ£" to "hya", "ãƒ’ãƒ¥" to "hyu", "ãƒ’ãƒ§" to "hyo",
        "ãƒŸãƒ£" to "mya", "ãƒŸãƒ¥" to "myu", "ãƒŸãƒ§" to "myo",
        "ãƒªãƒ£" to "rya", "ãƒªãƒ¥" to "ryu", "ãƒªãƒ§" to "ryo",
        "ã‚®ãƒ£" to "gya", "ã‚®ãƒ¥" to "gyu", "ã‚®ãƒ§" to "gyo",
        "ã‚¸ãƒ£" to "ja", "ã‚¸ãƒ¥" to "ju", "ã‚¸ãƒ§" to "jo",
        "ãƒ‚ãƒ£" to "ja", "ãƒ‚ãƒ¥" to "ju", "ãƒ‚ãƒ§" to "jo",
        "ãƒ“ãƒ£" to "bya", "ãƒ“ãƒ¥" to "byu", "ãƒ“ãƒ§" to "byo",
        "ãƒ”ãƒ£" to "pya", "ãƒ”ãƒ¥" to "pyu", "ãƒ”ãƒ§" to "pyo",
        // Basic Katakana Characters
        "ã‚¢" to "a", "ã‚¤" to "i", "ã‚¦" to "u", "ã‚¨" to "e", "ã‚ª" to "o",
        "ã‚«" to "ka", "ã‚­" to "ki", "ã‚¯" to "ku", "ã‚±" to "ke", "ã‚³" to "ko",
        "ã‚µ" to "sa", "ã‚·" to "shi", "ã‚¹" to "su", "ã‚»" to "se", "ã‚½" to "so",
        "ã‚¿" to "ta", "ãƒ" to "chi", "ãƒ„" to "tsu", "ãƒ†" to "te", "ãƒˆ" to "to",
        "ãƒŠ" to "na", "ãƒ‹" to "ni", "ãƒŒ" to "nu", "ãƒ" to "ne", "ãƒŽ" to "no",
        "ãƒ" to "ha", "ãƒ’" to "hi", "ãƒ•" to "fu", "ãƒ˜" to "he", "ãƒ›" to "ho",
        "ãƒž" to "ma", "ãƒŸ" to "mi", "ãƒ " to "mu", "ãƒ¡" to "me", "ãƒ¢" to "mo",
        "ãƒ¤" to "ya", "ãƒ¦" to "yu", "ãƒ¨" to "yo",
        "ãƒ©" to "ra", "ãƒª" to "ri", "ãƒ«" to "ru", "ãƒ¬" to "re", "ãƒ­" to "ro",
        "ãƒ¯" to "wa", "ãƒ²" to "o", "ãƒ³" to "n",
        // Dakuten (voiced consonants)
        "ã‚¬" to "ga", "ã‚®" to "gi", "ã‚°" to "gu", "ã‚²" to "ge", "ã‚´" to "go",
        "ã‚¶" to "za", "ã‚¸" to "ji", "ã‚º" to "zu", "ã‚¼" to "ze", "ã‚¾" to "zo",
        "ãƒ€" to "da", "ãƒ‚" to "ji", "ãƒ…" to "zu", "ãƒ‡" to "de", "ãƒ‰" to "do",
        // Handakuten (p-sounds for 'h' group)
        "ãƒ" to "ba", "ãƒ“" to "bi", "ãƒ–" to "bu", "ãƒ™" to "be", "ãƒœ" to "bo",
        "ãƒ‘" to "pa", "ãƒ”" to "pi", "ãƒ—" to "pu", "ãƒš" to "pe", "ãƒ" to "po",
        // ChÅonpu (long vowel mark)
        "ãƒ¼" to ""
    )

    private val HANGUL_ROMAJA_MAP: Map<String, Map<String, String>> = mapOf(
        "cho" to mapOf(
            "á„€" to "g", "á„" to "kk", "á„‚" to "n", "á„ƒ" to "d",
            "á„„" to "tt", "á„…" to "r", "á„†" to "m", "á„‡" to "b",
            "á„ˆ" to "pp", "á„‰" to "s", "á„Š" to "ss", "á„‹" to "",
            "á„Œ" to "j", "á„" to "jj", "á„Ž" to "ch", "á„" to "k",
            "á„" to "t", "á„‘" to "p", "á„’" to "h"
        ),
        "jung" to mapOf(
            "á…¡" to "a", "á…¢" to "ae", "á…£" to "ya", "á…¤" to "yae",
            "á…¥" to "eo", "á…¦" to "e", "á…§" to "yeo", "á…¨" to "ye",
            "á…©" to "o", "á…ª" to "wa", "á…«" to "wae", "á…¬" to "oe",
            "á…­" to "yo", "á…®" to "u", "á…¯" to "wo", "á…°" to "we",
            "á…±" to "wi", "á…²" to "yu", "á…³" to "eu", "á…´" to "eui",
            "á…µ" to "i"
        ),
        "jong" to mapOf(
            "á†¨" to "k", "á†¨á„‹" to "g", "á†¨á„‚" to "ngn", "á†¨á„…" to "ngn", "á†¨á„†" to "ngm", "á†¨á„’" to "kh",
            "á†©" to "kk", "á†©á„‹" to "kg", "á†©á„‚" to "ngn", "á†©á„…" to "ngn", "á†©á„†" to "ngm", "á†©á„’" to "kh",
            "á†ª" to "k", "á†ªá„‹" to "ks", "á†ªá„‚" to "ngn", "á†ªá„…" to "ngn", "á†ªá„†" to "ngm", "á†ªá„’" to "kch",
            "á†«" to "n", "á†«á„…" to "ll", "á†¬" to "n", "á†¬á„‹" to "nj", "á†¬á„‚" to "nn", "á†¬á„…" to "nn",
            "á†¬á„†" to "nm", "á†¬ã…Ž" to "nch", "á†­" to "n", "á†­á„‹" to "nh", "á†­á„…" to "nn", "á†®" to "t",
            "á†®á„‹" to "d", "á†®á„‚" to "nn", "á†®á„…" to "nn", "á†®á„†" to "nm", "á†®á„’" to "th", "á†¯" to "l",
            "á†¯á„‹" to "r", "á†¯á„‚" to "ll", "á†¯á„…" to "ll", "á†°" to "k", "á†°á„‹" to "lg", "á†°á„‚" to "ngn",
            "á†°á„…" to "ngn", "á†°á„†" to "ngm", "á†°á„’" to "lkh", "á†±" to "m", "á†±á„‹" to "lm", "á†±á„‚" to "mn",
            "á†±á„…" to "mn", "á†±á„†" to "mm", "á†±á„’" to "lmh", "á†²" to "p", "á†²á„‹" to "lb", "á†²á„‚" to "mn",
            "á†²á„…" to "mn", "á†²á„†" to "mm", "á†²á„’" to "lph", "á†³" to "t", "á†³á„‹" to "ls", "á†³á„‚" to "nn",
            "á†³á„…" to "nn", "á†³á„†" to "nm", "á†³á„’" to "lsh", "á†´" to "t", "á†´á„‹" to "lt", "á†´á„‚" to "nn",
            "á†´á„…" to "nn", "á†´á„†" to "nm", "á†´á„’" to "lth", "á†µ" to "p", "á†µá„‹" to "lp", "á†µá„‚" to "mn",
            "á†µá„…" to "mn", "á†µá„†" to "mm", "á†µá„’" to "lph", "á†¶" to "l", "á†¶á„‹" to "lh", "á†¶á„‚" to "ll",
            "á†¶á„…" to "ll", "á†¶á„†" to "lm", "á†¶á„’" to "lh", "á†·" to "m", "á†·á„…" to "mn", "á†¸" to "p",
            "á†¸á„‹" to "b", "á†¸á„‚" to "mn", "á†¸á„…" to "mn", "á†¸á„†" to "mm", "á†¸á„’" to "ph", "á†¹" to "p",
            "á†¹á„‹" to "ps", "á†¹á„‚" to "mn", "á†¹á„…" to "mn", "á†¹á„†" to "mm", "á†¹á„’" to "psh", "á†º" to "t",
            "á†ºá„‹" to "s", "á†ºá„‚" to "nn", "á†ºá„…" to "nn", "á†ºá„†" to "nm", "á†ºá„’" to "sh", "á†»" to "t",
            "á†»á„‹" to "ss", "á†»á„‚" to "tn", "á†»á„…" to "tn", "á†»á„†" to "nm", "á†»á„’" to "th", "á†¼" to "ng",
            "á†½" to "t", "á†½á„‹" to "j", "á†½á„‚" to "nn", "á†½á„…" to "nn", "á†½á„†" to "nm", "á†½á„’" to "ch",
            "á†¾" to "t", "á†¾á„‹" to "ch", "á†¾á„‚" to "nn", "á†¾á„…" to "nn", "á†¾á„†" to "nm", "á†¾á„’" to "ch",
            "á†¿" to "k", "á†¿á„‹" to "k", "á†¿á„‚" to "ngn", "á†¿á„…" to "ngn", "á†¿á„†" to "ngm", "á†¿á„’" to "kh",
            "á‡€" to "t", "á‡€á„‹" to "t", "á‡€á„‚" to "nn", "á‡€á„…" to "nn", "á‡€á„†" to "nm", "á‡€á„’" to "th",
            "á‡" to "p", "á‡á„‹" to "p", "á‡á„‚" to "mn", "á‡á„…" to "mn", "á‡á„†" to "mm", "á‡á„’" to "ph",
            "á‡‚" to "t", "á‡‚á„‹" to "h", "á‡‚á„‚" to "nn", "á‡‚á„…" to "nn", "á‡‚á„†" to "mm", "á‡‚á„’" to "t",
            "á‡‚á„€" to "k"
        )
    )

    private val DEVANAGARI_ROMAJI_MAP: Map<String, String> = mapOf(
        "à¤…" to "a", "à¤†" to "aa", "à¤‡" to "i", "à¤ˆ" to "ee", "à¤‰" to "u", "à¤Š" to "oo",
        "à¤‹" to "ri", "à¤" to "e", "à¤" to "ai", "à¤“" to "o", "à¤”" to "au",
        "à¤•" to "k", "à¤–" to "kh", "à¤—" to "g", "à¤˜" to "gh", "à¤™" to "ng",
        "à¤š" to "ch", "à¤›" to "chh", "à¤œ" to "j", "à¤" to "jh", "à¤ž" to "ny",
        "à¤Ÿ" to "t", "à¤ " to "th", "à¤¡" to "d", "à¤¢" to "dh", "à¤£" to "n",
        "à¤¤" to "t", "à¤¥" to "th", "à¤¦" to "d", "à¤§" to "dh", "à¤¨" to "n",
        "à¤ª" to "p", "à¤«" to "ph", "à¤¬" to "b", "à¤­" to "bh", "à¤®" to "m",
        "à¤¯" to "y", "à¤°" to "r", "à¤²" to "l", "à¤µ" to "v",
        "à¤¶" to "sh", "à¤·" to "sh", "à¤¸" to "s", "à¤¹" to "h",
        "à¤•à¥à¤·" to "ksh", "à¤¤à¥à¤°" to "tr", "à¤œà¥à¤ž" to "gy", "à¤¶à¥à¤°" to "shr",
        "à¤¾" to "aa", "à¤¿" to "i", "à¥€" to "ee", "à¥" to "u", "à¥‚" to "oo",
        "à¥ƒ" to "ri", "à¥‡" to "e", "à¥ˆ" to "ai", "à¥‹" to "o", "à¥Œ" to "au",
        "à¤‚" to "n", "à¤ƒ" to "h", "à¤" to "n", "à¤¼" to "", "à¥" to "",
        "à¥¦" to "0", "à¥§" to "1", "à¥¨" to "2", "à¥©" to "3", "à¥ª" to "4",
        "à¥«" to "5", "à¥¬" to "6", "à¥­" to "7", "à¥®" to "8", "à¥¯" to "9",
        "à¥" to "Om", "à¤½" to "",
        "à¥˜" to "q", "à¥™" to "kh", "à¥š" to "g", "à¥›" to "z", "à¥œ" to "r", "à¥" to "rh", "à¥ž" to "f", "à¥Ÿ" to "y",
        // Decomposed characters with Nukta
        "à¤•\u093C" to "q", "à¤–\u093C" to "kh", "à¤—\u093C" to "g", "à¤œ\u093C" to "z", "à¤¡\u093C" to "r", "à¤¢\u093C" to "rh", "à¤«\u093C" to "f", "à¤¯\u093C" to "y"
    )

    private val GURMUKHI_ROMAJI_MAP: Map<String, String> = mapOf(
        "à©³" to "o", "à¨…" to "a", "à©²" to "e", "à¨¸" to "s", "à¨¹" to "h",
        "à¨•" to "k", "à¨–" to "kh", "à¨—" to "g", "à¨˜" to "gh", "à¨™" to "ng",
        "à¨š" to "ch", "à¨›" to "chh", "à¨œ" to "j", "à¨" to "jh", "à¨ž" to "ny",
        "à¨Ÿ" to "t", "à¨ " to "th", "à¨¡" to "d", "à¨¢" to "dh", "à¨£" to "n",
        "à¨¤" to "t", "à¨¥" to "th", "à¨¦" to "d", "à¨§" to "dh", "à¨¨" to "n",
        "à¨ª" to "p", "à¨«" to "ph", "à¨¬" to "b", "à¨­" to "bh", "à¨®" to "m",
        "à¨¯" to "y", "à¨°" to "r", "à¨²" to "l", "à¨µ" to "v", "à©œ" to "r",
        "à¨¶" to "sh", "à©™" to "kh", "à©š" to "g", "à©›" to "z", "à©ž" to "f", "à¨³" to "l",
        "à¨¾" to "aa", "à¨¿" to "i", "à©€" to "ee", "à©" to "u", "à©‚" to "oo",
        "à©‡" to "e", "à©ˆ" to "ai", "à©‹" to "o", "à©Œ" to "au",
        "à©°" to "n", "à¨‚" to "n", "à©±" to "", "à©" to "", "à¨¼" to "",
        "à©´" to "Ek Onkar",
        "à©¦" to "0", "à©§" to "1", "à©¨" to "2", "à©©" to "3", "à©ª" to "4",
        "à©«" to "5", "à©¬" to "6", "à©­" to "7", "à©®" to "8", "à©¯" to "9"
    )

    private val GENERAL_CYRILLIC_ROMAJI_MAP: Map<String, String> = mapOf(
        "Ð" to "A", "Ð‘" to "B", "Ð’" to "V", "Ð“" to "G", "Ò" to "G", "Ð”" to "D",
        "Ðƒ" to "GÌ", "Ð‚" to "Ä", "Ð•" to "E", "Ð" to "Yo", "Ð„" to "Ye", "Ð–" to "Zh",
        "Ð—" to "Z", "Ð…" to "Dz", "Ð˜" to "I", "Ð†" to "I", "Ð‡" to "Yi", "Ð™" to "Y",
        "Ðˆ" to "Y", "Ðš" to "K", "Ð›" to "L", "Ð‰" to "Ly", "Ðœ" to "M", "Ð" to "N",
        "ÐŠ" to "Ny", "Ðž" to "O", "ÐŸ" to "P", "Ð " to "R", "Ð¡" to "S", "Ð¢" to "T",
        "Ð‹" to "Ä†", "Ð£" to "U", "ÐŽ" to "Å¬", "Ð¤" to "F", "Ð¥" to "Kh", "Ð¦" to "Ts",
        "Ð§" to "Ch", "Ð" to "DÅ¾", "Ð¨" to "Sh", "Ð©" to "Shch", "Ðª" to "Êº", "Ð«" to "Y",
        "Ð¬" to "Ê¹", "Ð­" to "E", "Ð®" to "Yu", "Ð¯" to "Ya",
        "Ñ " to "O", "Ñ¢" to "Ya", "Ñ¤" to "Ye", "Ñ¦" to "Ya", "Ñ¨" to "Ya",
        "Ñª" to "U", "Ñ¬" to "Yu", "Ñ®" to "Ks", "Ñ°" to "Ps", "Ñ²" to "F",
        "Ñ´" to "I", "Ñ¶" to "I", "Ò’" to "Gh", "Ò”" to "G", "Ò–" to "Zh",
        "Ò˜" to "Dz", "Òš" to "Q", "Òœ" to "K", "Òž" to "K", "Ò " to "K",
        "Ò¢" to "Ng", "Ò¤" to "Ng", "Ò¦" to "P", "Ò¨" to "O", "Òª" to "S",
        "Ò¬" to "T", "Ò®" to "U", "Ò°" to "U", "Ò²" to "Kh", "Ò´" to "Ts",
        "Ò¶" to "Ch", "Ò¸" to "Ch", "Òº" to "H", "Ò¼" to "Ch", "Ò¾" to "Ch",
        "ÐŒ" to "KÌ", "Ó¨" to "Ã–",

        "Ð°" to "a", "Ð±" to "b", "Ð²" to "v", "Ð³" to "g", "Ò‘" to "g", "Ð´" to "d",
        "Ñ“" to "gÌ", "Ñ’" to "Ä‘", "Ðµ" to "e", "Ñ‘" to "yo", "Ñ”" to "ye", "Ð¶" to "zh",
        "Ð·" to "z", "Ñ•" to "dz", "Ð¸" to "i", "Ñ–" to "i", "Ñ—" to "yi", "Ð¹" to "y",
        "Ñ˜" to "y", "Ðº" to "k", "Ð»" to "l", "Ñ™" to "ly", "Ð¼" to "m", "Ð½" to "n",
        "Ñš" to "ny", "Ð¾" to "o", "Ð¿" to "p", "Ñ€" to "r", "Ñ" to "s", "Ñ‚" to "t",
        "Ñ›" to "Ä‡", "Ñƒ" to "u", "Ñž" to "Å­", "Ñ„" to "f", "Ñ…" to "kh", "Ñ†" to "ts",
        "Ñ‡" to "ch", "ÑŸ" to "dÅ¾", "Ñˆ" to "sh", "Ñ‰" to "shch", "ÑŠ" to "Êº", "Ñ‹" to "y",
        "ÑŒ" to "Ê¹", "Ñ" to "e", "ÑŽ" to "yu", "Ñ" to "ya",
        "Ñ¡" to "o", "Ñ£" to "ya", "Ñ¥" to "ye", "Ñ§" to "ya", "Ñ©" to "ya",
        "Ñ«" to "u", "Ñ­" to "yu", "Ñ¯" to "ks", "Ñ±" to "ps", "Ñ³" to "f",
        "Ñµ" to "i", "Ñ·" to "i", "Ò“" to "gh", "Ò•" to "g", "Ò—" to "zh",
        "Ò™" to "dz", "Ò›" to "q", "Ò" to "k", "ÒŸ" to "k", "Ò¡" to "k",
        "Ò£" to "ng", "Ò¥" to "ng", "Ò§" to "p", "Ò©" to "o", "Ò«" to "s",
        "Ò­" to "t", "Ò¯" to "u", "Ò±" to "u", "Ò³" to "kh", "Òµ" to "ts",
        "Ò·" to "ch", "Ò¹" to "ch", "Ò»" to "h", "Ò½" to "ch", "Ò¿" to "ch",
        "Ñœ" to "á¸±", "Ó©" to "Ã¶"
    )

    private val RUSSIAN_ROMAJI_MAP: Map<String, String> = mapOf(
        "Ð¾Ð³Ð¾" to "ovo", "ÐžÐ³Ð¾" to "Ovo", "ÐµÐ³Ð¾" to "evo", "Ð•Ð³Ð¾" to "Evo"
    )

    private val UKRAINIAN_ROMAJI_MAP: Map<String, String> = mapOf(
        "Ð“" to "H", "Ð³" to "h",
        "Ò" to "G", "Ò‘" to "g",
        "Ð„" to "Ye", "Ñ”" to "ye",
        "Ð†" to "I", "Ñ–" to "i",
        "Ð‡" to "Yi", "Ñ—" to "yi"
    )

    private val SERBIAN_ROMAJI_MAP: Map<String, String> = mapOf(
        "Ð–" to "Å½", "Ð‰" to "Lj", "ÐŠ" to "Nj", "Ð¦" to "C", "Ð§" to "ÄŒ",
        "Ð" to "DÅ¾", "Ð¨" to "Å ", "Ð¥" to "H",

        "Ð¶" to "Å¾", "Ñ™" to "lj", "Ñš" to "nj", "Ñ†" to "c", "Ñ‡" to "Ä",
        "ÑŸ" to "dÅ¾", "Ñˆ" to "Å¡", "Ñ…" to "h"
    )

    private val BULGARIAN_ROMAJI_MAP: Map<String, String> = mapOf(
        "Ð–" to "Zh", "Ð¦" to "Ts", "Ð§" to "Ch", "Ð¨" to "Sh", "Ð©" to "Sht",
        "Ðª" to "A", "Ð¬" to "Y", "Ð®" to "Yu", "Ð¯" to "Ya",

        "Ð¶" to "zh", "Ñ†" to "ts", "Ñ‡" to "ch", "Ñˆ" to "sh", "Ñ‰" to "sht",
        "ÑŠ" to "a", "ÑŒ" to "y", "ÑŽ" to "yu", "Ñ" to "ya"
    )

    private val BELARUSIAN_ROMAJI_MAP: Map<String, String> = mapOf(
        "Ð“" to "H", "Ð³" to "h", "ÐŽ" to "W", "Ñž" to "w"
    )

    private val KYRGYZ_ROMAJI_MAP: Map<String, String> = mapOf(
        "Ò®" to "Ãœ", "Ò¯" to "Ã¼", "Ð«" to "Y", "Ñ‹" to "y"
    )

    private val MACEDONIAN_ROMAJI_MAP: Map<String, String> = mapOf(
        "Ðƒ" to "Gj", "Ð…" to "Dz", "Ð˜" to "I", "Ðˆ" to "J", "Ð‰" to "Lj",
        "ÐŠ" to "Nj", "ÐŒ" to "Kj", "Ð" to "DÅ¾", "Ð§" to "ÄŒ", "Ð¨" to "Sh",
        "Ð–" to "Zh", "Ð¦" to "C", "Ð¥" to "H",

        "Ñ“" to "gj", "Ñ•" to "dz", "Ð¸" to "i", "Ñ˜" to "j", "Ñ™" to "lj",
        "Ñš" to "nj", "Ñœ" to "kj", "ÑŸ" to "dÅ¾", "Ñ‡" to "Ä", "Ñˆ" to "sh",
        "Ð¶" to "zh", "Ñ†" to "c", "Ñ…" to "h"
    )

    private val RUSSIAN_CYRILLIC_LETTERS = setOf(
        "Ð", "Ð‘", "Ð’", "Ð“", "Ð”", "Ð•", "Ð", "Ð–", "Ð—", "Ð˜", "Ð™", "Ðš", "Ð›", "Ðœ", "Ð",
        "Ðž", "ÐŸ", "Ð ", "Ð¡", "Ð¢", "Ð£", "Ð¤", "Ð¥", "Ð¦", "Ð§", "Ð¨", "Ð©", "Ðª", "Ð«", "Ð¬",
        "Ð­", "Ð®", "Ð¯",

        "Ð°", "Ð±", "Ð²", "Ð³", "Ð´", "Ðµ", "Ñ‘", "Ð¶", "Ð·", "Ð¸", "Ð¹", "Ðº", "Ð»", "Ð¼", "Ð½",
        "Ð¾", "Ð¿", "Ñ€", "Ñ", "Ñ‚", "Ñƒ", "Ñ„", "Ñ…", "Ñ†", "Ñ‡", "Ñˆ", "Ñ‰", "ÑŠ", "Ñ‹", "ÑŒ",
        "Ñ", "ÑŽ", "Ñ"
    )

    private val UKRAINIAN_CYRILLIC_LETTERS = setOf(
       "Ð", "Ð‘", "Ð’", "Ð“", "Ò", "Ð”", "Ð•", "Ð„", "Ð–", "Ð—", "Ð˜", "Ð†", "Ð‡", "Ð™",
        "Ðš", "Ð›", "Ðœ", "Ð", "Ðž", "ÐŸ", "Ð ", "Ð¡", "Ð¢", "Ð£", "Ð¤", "Ð¥", "Ð¦", "Ð§",
        "Ð¨", "Ð©", "Ð¬", "Ð®", "Ð¯",

        "Ð°", "Ð±", "Ð²", "Ð³", "Ò‘", "Ð´", "Ðµ", "Ñ”", "Ð¶", "Ð·", "Ð¸", "Ñ–", "Ñ—", "Ð¹",
        "Ðº", "Ð»", "Ð¼", "Ð½", "Ð¾", "Ð¿", "Ñ€", "Ñ", "Ñ‚", "Ñƒ", "Ñ„", "Ñ…", "Ñ†", "Ñ‡",
        "Ñˆ", "Ñ‰", "ÑŒ", "ÑŽ", "Ñ"
    )

    private val SERBIAN_CYRILLIC_LETTERS = setOf(
        "Ð", "Ð‘", "Ð’", "Ð“", "Ð”", "Ð‚", "Ð•", "Ð–", "Ð—", "Ð˜", "Ðˆ", "Ðš", "Ð›", "Ð‰", "Ðœ",
        "Ð", "ÐŠ", "Ðž", "ÐŸ", "Ð ", "Ð¡", "Ð¢", "Ð‹", "Ð£", "Ð¤", "Ð¥", "Ð¦", "Ð§", "Ð", "Ð¨",

        "Ð°", "Ð±", "Ð²", "Ð³", "Ð´", "Ñ’", "Ðµ", "Ð¶", "Ð·", "Ð¸", "Ñ˜", "Ðº", "Ð»", "Ñ™", "Ð¼",
        "Ð½", "Ñš", "Ð¾", "Ð¿", "Ñ€", "Ñ", "Ñ‚", "Ñ›", "Ñƒ", "Ñ„", "Ñ…", "Ñ†", "Ñ‡", "ÑŸ", "Ñˆ"
    )

    private val BULGARIAN_CYRILLIC_LETTERS = setOf(
        "Ð", "Ð‘", "Ð’", "Ð“", "Ð”", "Ð•", "Ð–", "Ð—", "Ð˜", "Ð™", "Ðš", "Ð›", "Ðœ",
        "Ð", "Ðž", "ÐŸ", "Ð ", "Ð¡", "Ð¢", "Ð£", "Ð¤", "Ð¥", "Ð¦", "Ð§", "Ð¨", "Ð©",
        "Ðª", "Ð¬", "Ð®", "Ð¯",

        "Ð°", "Ð±", "Ð²", "Ð³", "Ð´", "Ðµ", "Ð¶", "Ð·", "Ð¸", "Ð¹", "Ðº", "Ð»", "Ð¼",
        "Ð½", "Ð¾", "Ð¿", "Ñ€", "Ñ", "Ñ‚", "Ñƒ", "Ñ„", "Ñ…", "Ñ†", "Ñ‡", "Ñˆ", "Ñ‰",
        "ÑŠ", "ÑŒ", "ÑŽ", "Ñ"
    )

    private val BELARUSIAN_CYRILLIC_LETTERS = setOf(
        "Ð", "Ð‘", "Ð’", "Ð“", "Ð”", "Ð•", "Ð", "Ð–", "Ð—", "Ð†", "Ð™", "Ðš", "Ð›", "Ðœ", "Ð",
        "Ðž", "ÐŸ", "Ð ", "Ð¡", "Ð¢", "Ð£", "ÐŽ", "Ð¤", "Ð¥", "Ð¦", "Ð§", "Ð¨", "Ð¬", "Ð®", "Ð¯",
        "Ð«", "Ð­",

        "Ð°", "Ð±", "Ð²", "Ð³", "Ð´", "Ðµ", "Ñ‘", "Ð¶", "Ð·", "Ñ–", "Ð¹", "Ðº", "Ð»", "Ð¼", "Ð½",
        "Ð¾", "Ð¿", "Ñ€", "Ñ", "Ñ‚", "Ñƒ", "Ñž", "Ñ„", "Ñ…", "Ñ†", "Ñ‡", "Ñˆ", "ÑŒ", "ÑŽ", "Ñ",
        "Ñ‹", "Ñ"
    )

    private val KYRGYZ_CYRILLIC_LETTERS = setOf(
        "Ð", "Ð‘", "Ð’", "Ð“", "Ð”", "Ð•", "Ð", "Ð–", "Ð—", "Ð˜", "Ð™", "Ðš", "Ð›", "Ðœ", "Ð",
        "Ò¢", "Ðž", "Ó¨", "ÐŸ", "Ð ", "Ð¡", "Ð¢", "Ð£", "Ò®", "Ð¤", "Ð¥", "Ð¦", "Ð§", "Ð¨", "Ð©",
        "Ðª", "Ð«", "Ð¬", "Ð­", "Ð®", "Ð¯",

        "Ð°", "Ð±", "Ð²", "Ð³", "Ð´", "Ðµ", "Ñ‘", "Ð¶", "Ð·", "Ð¸", "Ð¹", "Ðº", "Ð»", "Ð¼", "Ð½",
        "Ò£", "Ð¾", "Ó©", "Ð¿", "Ñ€", "Ñ", "Ñ‚", "Ñƒ", "Ò¯", "Ñ„", "Ñ…", "Ñ†", "Ñ‡", "Ñˆ", "Ñ‰",
        "ÑŠ", "Ñ‹", "ÑŒ", "Ñ", "ÑŽ", "Ñ"
    )

    private val MACEDONIAN_CYRILLIC_LETTERS = setOf(
        "Ð", "Ð‘", "Ð’", "Ð“", "Ð”", "Ðƒ", "Ð•", "Ð–", "Ð—", "Ð…", "Ð˜", "Ðˆ", "Ðš", "Ð›",
        "Ð‰", "Ðœ", "Ð", "ÐŠ", "Ðž", "ÐŸ", "Ð ", "Ð¡", "Ð¢", "ÐŒ", "Ð£", "Ð¤", "Ð¥",
        "Ð¦", "Ð§", "Ð", "Ð¨",

        "Ð°", "Ð±", "Ð²", "Ð³", "Ð´", "Ñ“", "Ðµ", "Ð¶", "Ð·", "Ñ•", "Ð¸", "Ñ˜", "Ðº", "Ð»",
        "Ñ™", "Ð¼", "Ð½", "Ñš", "Ð¾", "Ð¿", "Ñ€", "Ñ", "Ñ‚", "Ñœ", "Ñƒ", "Ñ„", "Ñ…",
        "Ñ†", "Ñ‡", "ÑŸ", "Ñˆ"
    )

    private val UKRAINIAN_SPECIFIC_CYRILLIC_LETTERS = setOf(
        "Ò", "Ò‘", "Ð„", "Ñ”", "Ð†", "Ñ–", "Ð‡", "Ñ—"
    )

    private val SERBIAN_SPECIFIC_CYRILLIC_LETTERS = setOf(
        "Ð‚", "Ñ’", "Ðˆ", "Ñ˜", "Ð‰", "Ñ™", "ÐŠ", "Ñš", "Ð‹", "Ñ›", "Ð", "ÑŸ"
    )

    private val BELARUSIAN_SPECIFIC_CYRILLIC_LETTERS = setOf(
        "ÐŽ", "Ñž", "Ð†", "Ñ–"
    )

    private val KYRGYZ_SPECIFIC_CYRILLIC_LETTERS = setOf(
        "Ò¢", "Ò£", "Ó¨", "Ó©", "Ò®", "Ò¯"
    )

    private val MACEDONIAN_SPECIFIC_CYRILLIC_LETTERS = setOf(
        "Ðƒ", "Ñ“", "Ð…", "Ñ•", "ÐŒ", "Ñœ"
    )

    // Lazy initialized Tokenizer
    private val kuromojiTokenizer: Tokenizer by lazy {
        Tokenizer()
    }

    private val HEX_ENTITY_REGEX = "&#x([0-9a-fA-F]+);".toRegex()
    private val DEC_ENTITY_REGEX = "&#(\\d+);".toRegex()

    private fun decodeHtmlEntities(text: String): String {
        if (!text.contains('&')) return text
        val sb = StringBuilder(text.length)
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (c == '&') {
                val end = text.indexOf(';', i + 1)
                if (end != -1 && end - i < 12) {
                    val entity = text.substring(i, end + 1)
                    val decoded = when {
                        entity == "&apos;" -> "'"
                        entity == "&quot;" -> "\""
                        entity == "&lt;" -> "<"
                        entity == "&gt;" -> ">"
                        entity == "&nbsp;" -> " "
                        entity == "&amp;" -> "&"
                        entity.startsWith("&#x") -> {
                            entity.substring(3, entity.length - 1).toIntOrNull(16)?.let { codePoint ->
                                if (Character.isValidCodePoint(codePoint)) String(Character.toChars(codePoint)) else "\uFFFD"
                            }
                        }
                        entity.startsWith("&#") -> {
                            entity.substring(2, entity.length - 1).toIntOrNull()?.let { codePoint ->
                                if (Character.isValidCodePoint(codePoint)) String(Character.toChars(codePoint)) else "\uFFFD"
                            }
                        }
                        else -> null
                    }
                    if (decoded != null) {
                        sb.append(decoded)
                        i = end + 1
                        continue
                    }
                }
            }
            sb.append(c)
            i++
        }
        return sb.toString()
    }

    fun parseLyrics(lyrics: String): List<LyricsEntry> {
        if (lyrics.isBlank()) return emptyList()

        // Fast unescape
        val unescapedLyrics = if (lyrics.contains('\\') || lyrics.startsWith("\"")) {
            val s = lyrics.trim().removePrefix("\"").removeSuffix("\"")
            val sb = StringBuilder(s.length)
            var j = 0
            while (j < s.length) {
                val c = s[j]
                if (c == '\\' && j + 1 < s.length) {
                    when (val next = s[j + 1]) {
                        '\\' -> sb.append('\\')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        else -> sb.append(c).append(next)
                    }
                    j += 2
                } else {
                    sb.append(c)
                    j++
                }
            }
            sb.toString()
        } else lyrics

        val decodedLyrics = decodeHtmlEntities(unescapedLyrics)

        val lines = decodedLyrics.lines()
            .filter { 
                it.isNotBlank() || it.trim().startsWith("[") || it.trim().startsWith("<")
            }
            .filter { !it.trim().startsWith("[offset:") }

        // Check if this is rich sync format (contains <MM:SS.mm> patterns)
        val isRichSync = lines.any { line ->
            RICH_SYNC_LINE_REGEX.matches(line.trim()) &&
            RICH_SYNC_WORD_REGEX.containsMatchIn(line)
        }

        return if (isRichSync) {
            parseRichSyncLyrics(lines)
        } else {
            parseStandardLyrics(lines)
        }
    }

    /**
     * Parse rich sync lyrics format: [MM:SS.mm]<MM:SS.mm> word <MM:SS.mm> word ...
     * This format provides word-by-word timing for karaoke-style highlighting
     */
    private fun parseRichSyncLyrics(lines: List<String>): List<LyricsEntry> {
        val result = mutableListOf<LyricsEntry>()
        var lastNonBgAgent: String? = null

        lines.forEachIndexed { index, line ->
            val trimmedLine = line.trim()
            
            // Try Paxsenix bg format first: [bg: <02:18.078>Yeah<02:19.341>]
            val bgMatch = PAXSENIX_BG_LINE_REGEX.find(trimmedLine)
            if (bgMatch != null) {
                val content = bgMatch.groupValues[1]
                
                // Parse word-level timestamps from content
                val wordTimings = parseRichSyncWords(content, index, lines)
                    ?: run {
                        val nextLine = lines.getOrNull(index + 1)?.trim() ?: ""
                        if (nextLine.startsWith("<") && nextLine.endsWith(">")) {
                            parseWordTimestamps(nextLine.removeSurrounding("<", ">"))
                        } else null
                    }
                
                // Extract plain text (remove all <MM:SS.mm> tags)
                val plainText = content.replace(Regex("<\\d{1,2}:\\d{2}\\.\\d{2,3}>\\s*"), "").trim()
                
                val lineTimeMs = wordTimings?.firstOrNull()?.startTime?.let { (it * 1000).toLong() } ?: 0L
                result.add(LyricsEntry(lineTimeMs, plainText, wordTimings, agent = lastNonBgAgent ?: "bg", isBackground = true))
                return@forEachIndexed
            }
            
            // Try Paxsenix agent format: [00:00.000]v1: <00:00.000>I <00:00.154>promise...
            val agentMatch = PAXSENIX_AGENT_LINE_REGEX.find(trimmedLine)
            if (agentMatch != null) {
                val minutes = agentMatch.groupValues[1].toLongOrNull() ?: 0L
                val seconds = agentMatch.groupValues[2].toLongOrNull() ?: 0L
                val centiseconds = agentMatch.groupValues[3].toLongOrNull() ?: 0L
                val agent = agentMatch.groupValues[4] // v1, v2, etc.
                val content = agentMatch.groupValues[5]
                
                val millisPart = if (agentMatch.groupValues[3].length == 3) centiseconds else centiseconds * 10
                val lineTimeMs = minutes * DateUtils.MINUTE_IN_MILLIS + seconds * DateUtils.SECOND_IN_MILLIS + millisPart
                
                // Parse word-level timestamps from content
                val wordTimings = parseRichSyncWords(content, index, lines)
                    ?: run {
                        val nextLine = lines.getOrNull(index + 1)?.trim() ?: ""
                        if (nextLine.startsWith("<") && nextLine.endsWith(">")) {
                            parseWordTimestamps(nextLine.removeSurrounding("<", ">"))
                        } else null
                    }
                
                // Extract plain text (remove all <MM:SS.mm> tags)
                val plainText = content.replace(Regex("<\\d{1,2}:\\d{2}\\.\\d{2,3}>\\s*"), "").trim()
                
                if (!agent.isNullOrBlank()) {
                    lastNonBgAgent = agent
                }
                result.add(LyricsEntry(lineTimeMs, plainText, wordTimings, agent = agent, isBackground = false))
                return@forEachIndexed
            }
            
            // Try existing format: [MM:SS.mm]{agent:v1}... or [MM:SS.mm]{bg}...
            val matchResult = RICH_SYNC_LINE_REGEX.matchEntire(trimmedLine)
            if (matchResult != null) {
                val minutes = matchResult.groupValues[1].toLongOrNull() ?: 0L
                val seconds = matchResult.groupValues[2].toLongOrNull() ?: 0L
                val centiseconds = matchResult.groupValues[3].toLongOrNull() ?: 0L

                // Convert to milliseconds
                val millisPart = if (matchResult.groupValues[3].length == 3) centiseconds else centiseconds * 10
                val lineTimeMs = minutes * DateUtils.MINUTE_IN_MILLIS + seconds * DateUtils.SECOND_IN_MILLIS + millisPart

                var content = matchResult.groupValues[4].trimStart()

                // Parse agent marker {agent:v1}
                val oldAgentMatch = AGENT_REGEX.find(content)
                val agent = oldAgentMatch?.groupValues?.get(1)
                if (oldAgentMatch != null) {
                    content = content.replaceFirst(AGENT_REGEX, "")
                }

                // Parse background marker {bg}
                val isBackground = BACKGROUND_REGEX.containsMatchIn(content)
                if (isBackground) {
                    content = content.replaceFirst(BACKGROUND_REGEX, "")
                }

                // Parse word-level timestamps from content
                val wordTimings = parseRichSyncWords(content, index, lines)
                    ?: run {
                        val nextLine = lines.getOrNull(index + 1)?.trim() ?: ""
                        if (nextLine.startsWith("<") && nextLine.endsWith(">")) {
                            parseWordTimestamps(nextLine.removeSurrounding("<", ">"))
                        } else null
                    }

                // Extract plain text (remove all <MM:SS.mm> tags)
                val plainText = content.replace(Regex("<\\d{1,2}:\\d{2}\\.\\d{2,3}>\\s*"), "").trim()

                if (!isBackground && !agent.isNullOrBlank()) {
                    lastNonBgAgent = agent
                }
                result.add(LyricsEntry(lineTimeMs, plainText, wordTimings, agent = if (isBackground) lastNonBgAgent ?: "bg" else agent, isBackground = isBackground))
            }
        }

        return result.sorted()
    }

    /**
     * Parse word timestamps from rich sync content
     * Format: <MM:SS.mm> word <MM:SS.mm> word ...
     */
    private fun parseRichSyncWords(content: String, currentIndex: Int, allLines: List<String>): List<WordTimestamp>? {
        val wordMatches = RICH_SYNC_WORD_REGEX.findAll(content).toList()

        if (wordMatches.isEmpty()) return null

        // Check for a trailing end timestamp after the last word.
        // The provider uses two formats:
        //   - Angle brackets: <MM:SS.mmm> (used in v1:/v2: prefixed lines)
        //   - Square brackets: [MM:SS.xx] (used in non-prefixed lines)
        val lastMatchEnd = wordMatches.last().range.last
        val trailingContent = content.substring(lastMatchEnd + 1).trim()
        val angleTrailingMatch = "<(\\d{1,2}):(\\d{2})\\.(\\d{2,3})>".toRegex().find(trailingContent)
        val squareTrailingMatch = "\\[(\\d{1,2}):(\\d{2})\\.(\\d{2,3})\\]".toRegex().find(trailingContent)
        val trailingTimeMatch = angleTrailingMatch ?: squareTrailingMatch
        val trailingEndTime: Double? = if (trailingTimeMatch != null && trailingContent.substring(trailingTimeMatch.range.last + 1).removeSuffix("]").isBlank()) {
            val tMin = trailingTimeMatch.groupValues[1].toLongOrNull() ?: 0L
            val tSec = trailingTimeMatch.groupValues[2].toLongOrNull() ?: 0L
            val tFrac = trailingTimeMatch.groupValues[3].toLongOrNull() ?: 0L
            val tFracPart = if (trailingTimeMatch.groupValues[3].length == 3) tFrac / 1000.0 else tFrac / 100.0
            tMin * 60.0 + tSec + tFracPart
        } else null

        val wordTimings = mutableListOf<WordTimestamp>()

        wordMatches.forEachIndexed { index, match ->
            val minutes = match.groupValues[1].toLongOrNull() ?: 0L
            val seconds = match.groupValues[2].toLongOrNull() ?: 0L
            val fraction = match.groupValues[3].toLongOrNull() ?: 0L

            val fractionPart = if (match.groupValues[3].length == 3) fraction / 1000.0 else fraction / 100.0
            val startTimeSeconds = minutes * 60.0 + seconds + fractionPart

            val rawText = match.groupValues[4]
            val hasTrailingSpace = rawText.endsWith(" ")
            val words = rawText.trim().split(Regex("\\s+")).filter { it.isNotBlank() }

            // Get the next timestamp for end time calculation
            val nextTimestamp: Double
            val nextLineTime: Double?

            if (index < wordMatches.size - 1) {
                val nextMatch = wordMatches[index + 1]
                val nextMin = nextMatch.groupValues[1].toLongOrNull() ?: 0L
                val nextSec = nextMatch.groupValues[2].toLongOrNull() ?: 0L
                val nextFrac = nextMatch.groupValues[3].toLongOrNull() ?: 0L
                val nextFracPart = if (nextMatch.groupValues[3].length == 3) nextFrac / 1000.0 else nextFrac / 100.0
                nextTimestamp = nextMin * 60.0 + nextSec + nextFracPart
                nextLineTime = null
            } else {
                nextLineTime = getNextLineStartTime(currentIndex, allLines)
                nextTimestamp = trailingEndTime ?: nextLineTime ?: (startTimeSeconds + 0.5)
            }

            words.forEachIndexed { wordIndex, word ->
                val isLastWordInGroup = wordIndex == words.lastIndex
                val isLastWordOverall = index == wordMatches.lastIndex && isLastWordInGroup

                val wordStartTime = startTimeSeconds + (nextTimestamp - startTimeSeconds) * wordIndex / words.size
                val wordEndTime = if (!isLastWordInGroup) {
                    startTimeSeconds + (nextTimestamp - startTimeSeconds) * (wordIndex + 1) / words.size
                } else if (!isLastWordOverall) {
                    nextTimestamp
                } else {
                    trailingEndTime ?: nextLineTime ?: (startTimeSeconds + 0.5)
                }

                val wordHasTrailingSpace = if (!isLastWordInGroup) {
                    true
                } else if (!isLastWordOverall) {
                    hasTrailingSpace
                } else {
                    // Last word of last match - check if there's text after it (excluding our optional trailing timestamp)
                    val textAfterMatch = if (trailingTimeMatch != null) {
                        trailingContent.substring(0, trailingTimeMatch.range.first)
                    } else {
                        trailingContent
                    }
                    textAfterMatch.isNotBlank()
                }

                if (word.isNotBlank()) {
                    wordTimings.add(WordTimestamp(word, wordStartTime, wordEndTime, wordHasTrailingSpace))
                }
            }
        }

        return if (wordTimings.isNotEmpty()) wordTimings else null
    }

    /**
     * Get the start time of the next line for calculating the last word's end time
     */
    private fun getNextLineStartTime(currentIndex: Int, allLines: List<String>): Double? {
        if (currentIndex + 1 >= allLines.size) return null

        val nextLine = allLines[currentIndex + 1].trim()
        
        // Try standard rich sync line
        val matchResult = RICH_SYNC_LINE_REGEX.matchEntire(nextLine)
        if (matchResult != null) {
            val minutes = matchResult.groupValues[1].toLongOrNull() ?: return null
            val seconds = matchResult.groupValues[2].toLongOrNull() ?: return null
            val fraction = matchResult.groupValues[3].toLongOrNull() ?: 0L

            val fractionPart = if (matchResult.groupValues[3].length == 3) fraction / 1000.0 else fraction / 100.0
            return minutes * 60.0 + seconds + fractionPart
        }
        
        // Try background line
        val bgMatch = PAXSENIX_BG_LINE_REGEX.matchEntire(nextLine)
        if (bgMatch != null) {
            val content = bgMatch.groupValues[1]
            val wordMatch = RICH_SYNC_WORD_REGEX.find(content) ?: return null
            val minutes = wordMatch.groupValues[1].toLongOrNull() ?: return null
            val seconds = wordMatch.groupValues[2].toLongOrNull() ?: return null
            val fraction = wordMatch.groupValues[3].toLongOrNull() ?: 0L
            val fractionPart = if (wordMatch.groupValues[3].length == 3) fraction / 1000.0 else fraction / 100.0
            return minutes * 60.0 + seconds + fractionPart
        }

        return null
    }

    /**
     * Parse standard synced lyrics format: [MM:SS.mm] text
     */
    private fun parseStandardLyrics(lines: List<String>): List<LyricsEntry> {
        val result = mutableListOf<LyricsEntry>()

        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            if (!line.trim().startsWith("<") || !line.trim().endsWith(">")) {
                val entries = parseLine(line, null)
                if (entries != null) {
                    val wordTimestamps = if (i + 1 < lines.size) {
                        val nextLine = lines[i + 1]
                        if (nextLine.trim().startsWith("<") && nextLine.trim().endsWith(">")) {
                            parseWordTimestamps(nextLine.trim().removeSurrounding("<", ">"))
                        } else null
                    } else null

                    if (wordTimestamps != null) {
                        result.addAll(entries.map { entry ->
                            LyricsEntry(entry.time, entry.text, wordTimestamps, agent = entry.agent, isBackground = entry.isBackground)
                        })
                    } else {
                        result.addAll(entries)
                    }
                }
            }
            i++
        }
        return result.sorted()
    }

    private fun parseWordTimestamps(data: String): List<WordTimestamp>? {
        if (data.isBlank()) return null
        return try {
            data.split("|").mapNotNull { wordData ->
                val parts = wordData.split(":")
                if (parts.size >= 3) {
                    val text = parts.dropLast(2).joinToString(":")
                    val startTime = parts[parts.size - 2].toDoubleOrNull() ?: 0.0
                    val endTime = parts[parts.size - 1].toDoubleOrNull() ?: 0.0
                    val isLast = wordData == data.split("|").last()
                    WordTimestamp(
                        text = text,
                        startTime = startTime,
                        endTime = endTime,
                        hasTrailingSpace = !isLast
                    )
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseLine(line: String, words: List<WordTimestamp>? = null): List<LyricsEntry>? {
        val matchResult = LINE_REGEX.matchEntire(line.trim()) ?: return null
        val times = matchResult.groupValues[1]
        var text = matchResult.groupValues[3]
        val timeMatchResults = TIME_REGEX.findAll(times)

        // Parse agent marker {agent:v1}
        val agentMatch = AGENT_REGEX.find(text)
        val agent = agentMatch?.groupValues?.get(1)
        if (agentMatch != null) {
            text = text.replaceFirst(AGENT_REGEX, "")
        }

        // Parse background marker {bg}
        val isBackground = BACKGROUND_REGEX.containsMatchIn(text)
        if (isBackground) {
            text = text.replaceFirst(BACKGROUND_REGEX, "")
        }

        return timeMatchResults
            .map { timeMatchResult ->
                val min = timeMatchResult.groupValues[1].toLong()
                val sec = timeMatchResult.groupValues[2].toLong()
                val milString = timeMatchResult.groupValues[3]
                var mil = milString.toLong()
                if (milString.length == 2) {
                    mil *= 10
                }
                val time = min * DateUtils.MINUTE_IN_MILLIS + sec * DateUtils.SECOND_IN_MILLIS + mil
                LyricsEntry(time, text, words, agent = agent, isBackground = isBackground)
            }.toList()
    }

    fun findCurrentLineIndex(
        lines: List<LyricsEntry>,
        position: Long,
    ): Int {
        val threshold = 100L
        for (index in lines.indices) {
            if (lines[index].time >= position + threshold) {
                return index - 1
            }
        }
        return lines.lastIndex
    }

    /**
     * Returns the set of line indices that are currently active (being sung).
     * A line is active if playback position >= line.time AND position < line end time.
     * Line end time = the last word's endTime if word timings exist, otherwise the next line's start time.
     * This supports simultaneous singers whose lines overlap in time.
     */
    fun findActiveLineIndices(
        lines: List<LyricsEntry>,
        position: Long,
    ): Set<Int> {
        val active = mutableSetOf<Int>()
        val hasWordTimings = lines.any { !it.words.isNullOrEmpty() }

        for (index in lines.indices) {
            val line = lines[index]
            if (line.time > position) break // Past current position, stop early

            // Determine this line's end time
            val lineEndMs: Long = if (!line.words.isNullOrEmpty()) {
                // Use last word's endTime converted to ms
                (line.words.last().endTime * 1000).toLong()
            } else {
                // Fallback: next line's start time
                if (index + 1 < lines.size) lines[index + 1].time else Long.MAX_VALUE
            }

            if (position <= lineEndMs) {
                active.add(index)
            }
        }

        if (!hasWordTimings && active.size > 1) {
            val mainActive = active.filter { lines[it].isBackground == false }
            if (mainActive.size > 1) {
                val maxTime = mainActive.maxOf { lines[it].time }
                active.removeAll { it in mainActive && lines[it].time < maxTime }
            }
        }

        return active
    }

    // TODO: Will be useful if we let the user pick the language, useless for now
    /* enum class CyrillicLanguage {
        RUSSIAN,
        UKRAINIAN,
        SERBIAN,
        BULGARIAN,
        BELARUSIAN,
        KYRGYZ,
        MACEDONIAN
    } */

    suspend fun romanizeJapanese(text: String): String = withContext(Dispatchers.Default) {
        val tokens = kuromojiTokenizer.tokenize(text)
        val romanizedTokens = tokens.mapIndexed { index, token ->
            val currentReading = if (token.reading.isNullOrEmpty() || token.reading == "*") {
                token.surface
            } else {
                token.reading
            }
            val nextTokenReading = if (index + 1 < tokens.size) {
                tokens[index + 1].reading?.takeIf { it.isNotEmpty() && it != "*" } ?: tokens[index + 1].surface
            } else {
                null
            }
            katakanaToRomaji(currentReading, nextTokenReading)
        }
        romanizedTokens.joinToString(" ")
    }

    fun katakanaToRomaji(katakana: String?, nextKatakana: String? = null): String {
        if (katakana.isNullOrEmpty()) return ""

        val romajiBuilder = StringBuilder(katakana.length)
        var i = 0
        val n = katakana.length
        while (i < n) {
            var consumed = false
            if (i + 1 < n) {
                val twoCharCandidate = katakana.substring(i, i + 2)
                val mappedTwoChar = KANA_ROMAJI_MAP[twoCharCandidate]
                if (mappedTwoChar != null) {
                    romajiBuilder.append(mappedTwoChar)
                    i += 2
                    consumed = true
                }
            }

            if (!consumed && katakana[i] == '\u30C3') {
                val nextCharToDouble = nextKatakana?.getOrNull(0)
                if (nextCharToDouble != null) {
                    val nextCharRomaji = KANA_ROMAJI_MAP[nextCharToDouble.toString()]?.getOrNull(0)?.toString()
                        ?: nextCharToDouble.toString()
                    romajiBuilder.append(nextCharRomaji.lowercase().trim())
                }
                i += 1
                consumed = true
            }

            if (!consumed) {
                val oneCharCandidate = katakana[i].toString()
                val mappedOneChar = KANA_ROMAJI_MAP[oneCharCandidate]
                if (mappedOneChar != null) {
                    romajiBuilder.append(mappedOneChar)
                } else {
                    romajiBuilder.append(oneCharCandidate)
                }
                i += 1
            }
        }
        return romajiBuilder.toString().lowercase()
    }

    suspend fun romanizeKorean(text: String): String = withContext(Dispatchers.Default) {
        val romajaBuilder = StringBuilder()
        var prevFinal: String? = null

        for (i in text.indices) {
            val char = text[i]
            if (char in '\uAC00'..'\uD7A3') {
                val syllableIndex = char.code - 0xAC00
                val choIndex = syllableIndex / (21 * 28)
                val jungIndex = (syllableIndex % (21 * 28)) / 28
                val jongIndex = syllableIndex % 28

                val choChar = (0x1100 + choIndex).toChar().toString()
                val jungChar = (0x1161 + jungIndex).toChar().toString()
                val jongChar = if (jongIndex == 0) null else (0x11A7 + jongIndex).toChar().toString()

                if (prevFinal != null) {
                    val contextKey = prevFinal + choChar
                    val jong = HANGUL_ROMAJA_MAP["jong"]?.get(contextKey)
                        ?: HANGUL_ROMAJA_MAP["jong"]?.get(prevFinal)
                        ?: prevFinal
                    romajaBuilder.append(jong)
                }

                val cho = HANGUL_ROMAJA_MAP["cho"]?.get(choChar) ?: choChar
                val jung = HANGUL_ROMAJA_MAP["jung"]?.get(jungChar) ?: jungChar
                romajaBuilder.append(cho).append(jung)
                prevFinal = jongChar
            } else {
                if (prevFinal != null) {
                    val jong = HANGUL_ROMAJA_MAP["jong"]?.get(prevFinal) ?: prevFinal
                    romajaBuilder.append(jong)
                    prevFinal = null
                }
                romajaBuilder.append(char)
            }
        }

        if (prevFinal != null) {
            val jong = HANGUL_ROMAJA_MAP["jong"]?.get(prevFinal) ?: prevFinal
            romajaBuilder.append(jong)
        }

        romajaBuilder.toString()
    }

    suspend fun romanizeChinese(text: String): String = withContext(Dispatchers.Default) {
        if (text.isEmpty()) return@withContext ""
        val builder = StringBuilder(text.length * 2)
        for (ch in text) {
            if (ch in '\u4E00'..'\u9FFF') {
                val py = Pinyin.toPinyin(ch).lowercase(Locale.getDefault())
                builder.append(py).append(' ')
            } else {
                builder.append(ch)
            }
        }
        // Remove whitespaces before ASCII and CJK punctuations
        builder.toString()
            .replace(Regex("\\s+([,.!?;:])"), "$1")
            .replace(Regex("\\s+([ï¼Œã€‚ï¼ï¼Ÿï¼›ï¼šã€ï¼ˆï¼‰ã€Šã€‹ã€ˆã€‰ã€ã€‘ã€Žã€ã€Œã€])"), "$1")
            .trim()
    }

    suspend fun romanizeCyrillic(text: String, language: String? = null): String? = withContext(Dispatchers.Default) {
        if (text.isEmpty()) return@withContext null

        val cyrillicChars = text.filter { it in '\u0400'..'\u04FF' }

        if (cyrillicChars.isEmpty() ||
            (cyrillicChars.length == 1 && (cyrillicChars[0] == '\u0435' || cyrillicChars[0] == '\u0415'))) {
            return@withContext null
        }

        when (language) {
            "Russian" -> romanizeRussianInternal(text)
            "Ukrainian" -> romanizeUkrainianInternal(text)
            "Serbian" -> romanizeSerbianInternal(text)
            "Bulgarian" -> romanizeBulgarianInternal(text)
            "Belarusian" -> romanizeBelarusianInternal(text)
            "Kyrgyz" -> romanizeKyrgyzInternal(text)
            "Macedonian" -> romanizeMacedonianInternal(text)
            else -> when {
                isRussian(text) -> romanizeRussianInternal(text)
                isUkrainian(text) -> romanizeUkrainianInternal(text)
                isSerbian(text) -> romanizeSerbianInternal(text)
                isBulgarian(text) -> romanizeBulgarianInternal(text)
                isBelarusian(text) -> romanizeBelarusianInternal(text)
                isKyrgyz(text) -> romanizeKyrgyzInternal(text)
                isMacedonian(text) -> romanizeMacedonianInternal(text)
                else -> null
            }
        }
    }

    private fun romanizeRussianInternal(text: String): String {
        val romajiBuilder = StringBuilder(text.length)
        val words = text.split("((?<=\\s|[.,!?;])|(?=\\s|[.,!?;]))".toRegex())
            .filter { it.isNotEmpty() }

        words.forEachIndexed { _, word ->
            if (word.matches("[.,!?;]".toRegex()) || word.isBlank()) {
                romajiBuilder.append(word)
            } else {
                var charIndex = 0
                while (charIndex < word.length) {
                    var consumed = false
                    // Check for 3-character sequences
                    if (charIndex + 2 < word.length) {
                        val threeCharCandidate = word.substring(charIndex, charIndex + 3)
                        if (RUSSIAN_ROMAJI_MAP.containsKey(threeCharCandidate)) {
                            romajiBuilder.append(RUSSIAN_ROMAJI_MAP[threeCharCandidate])
                            charIndex += 3
                            consumed = true
                        }
                    }

                    if (!consumed) {
                        val charStr = word[charIndex].toString()
                        // Special case for 'Ðµ' or 'Ð•' at the start of a word
                        if ((charStr == "Ðµ" || charStr == "Ð•") && (charIndex == 0 || word[charIndex - 1].isWhitespace())) {
                            romajiBuilder.append(if (charStr == "Ðµ") "ye" else "Ye")
                        } else {
                            // Apply general Cyrillic mapping (Russian is no different so there's no need to apply a russian map)
                            val romanizedChar = GENERAL_CYRILLIC_ROMAJI_MAP[charStr] ?: charStr
                            romajiBuilder.append(romanizedChar)
                        }
                        charIndex += 1
                    }
                }
            }
        }
        return romajiBuilder.toString()
    }

    private fun romanizeUkrainianInternal(text: String): String {
        val romajiBuilder = StringBuilder(text.length)
        val words = text.split("((?<=\\s|[.,!?;])|(?=\\s|[.,!?;]))".toRegex())
            .filter { it.isNotEmpty() }

        words.forEachIndexed { _, word ->
            if (word.matches("[.,!?;]".toRegex()) || word.isBlank()) {
                romajiBuilder.append(word)
            } else {
                var charIndex = 0
                while (charIndex < word.length) {
                    val charStr = word[charIndex].toString()
                    var processed = false

                    if (charIndex > 0 && word[charIndex - 1].isLetter() && !isCyrillicVowel(word[charIndex - 1])) {
                        // Check if the current character is Ð® or Ð¯ and is preceded by a consonant
                        if (charStr == "Ð®") {
                            romajiBuilder.append("Iu")
                            processed = true
                        } else if (charStr == "ÑŽ") {
                            romajiBuilder.append("iu")
                            processed = true
                        } else if (charStr == "Ð¯") {
                            romajiBuilder.append("Ia")
                            processed = true
                        } else if (charStr == "Ñ") {
                            romajiBuilder.append("ia")
                            processed = true
                        }
                    }

                    if (!processed) {
                        romajiBuilder.append(UKRAINIAN_ROMAJI_MAP[charStr] ?: GENERAL_CYRILLIC_ROMAJI_MAP[charStr] ?: charStr)
                    }
                    charIndex++
                }
            }
        }
        return romajiBuilder.toString()
    }

    private fun romanizeSerbianInternal(text: String): String {
        val romajiBuilder = StringBuilder(text.length)
        val words = text.split("((?<=\\s|[.,!?;])|(?=\\s|[.,!?;]))".toRegex())
            .filter { it.isNotEmpty() }

        words.forEachIndexed { _, word ->
            if (word.matches("[.,!?;]".toRegex()) || word.isBlank()) {
                romajiBuilder.append(word)
            } else {
                var charIndex = 0
                while (charIndex < word.length) {
                    val charStr = word[charIndex].toString()
                    val romanizedChar = SERBIAN_ROMAJI_MAP[charStr] ?: GENERAL_CYRILLIC_ROMAJI_MAP[charStr] ?: charStr
                    romajiBuilder.append(romanizedChar)
                    charIndex++
                }
            }
        }
        return romajiBuilder.toString()
    }

    private fun romanizeBulgarianInternal(text: String): String {
        val romajiBuilder = StringBuilder(text.length)
        val words = text.split("((?<=\\s|[.,!?;])|(?=\\s|[.,!?;]))".toRegex())
            .filter { it.isNotEmpty() }

        words.forEachIndexed { _, word ->
            if (word.matches("[.,!?;]".toRegex()) || word.isBlank()) {
                romajiBuilder.append(word)
            } else {
                var charIndex = 0
                while (charIndex < word.length) {
                    val charStr = word[charIndex].toString()
                    val romanizedChar = BULGARIAN_ROMAJI_MAP[charStr] ?: GENERAL_CYRILLIC_ROMAJI_MAP[charStr] ?: charStr
                    romajiBuilder.append(romanizedChar)
                    charIndex++
                }
            }
        }
        return romajiBuilder.toString()
    }

    private fun romanizeBelarusianInternal(text: String): String {
        val romajiBuilder = StringBuilder(text.length)
        val words = text.split("((?<=\\s|[.,!?;])|(?=\\s|[.,!?;]))".toRegex())
            .filter { it.isNotEmpty() }

        words.forEach { word ->
            if (word.matches("[.,!?;]".toRegex()) || word.isBlank()) {
                romajiBuilder.append(word)
            } else {
                var charIndex = 0
                while (charIndex < word.length) {
                    val charStr = word[charIndex].toString()
                    // Special case for 'Ðµ' or 'Ð•' at the start of a word
                    if ((charStr == "Ðµ" || charStr == "Ð•") && (charIndex == 0 || word[charIndex - 1].isWhitespace())) {
                        romajiBuilder.append(if (charStr == "Ðµ") "ye" else "Ye")
                    } else {
                        // General mapping
                        val romanizedChar = BELARUSIAN_ROMAJI_MAP[charStr] ?: GENERAL_CYRILLIC_ROMAJI_MAP[charStr] ?: charStr
                        romajiBuilder.append(romanizedChar)
                    }
                    charIndex += 1
                }
            }
        }

        return romajiBuilder.toString()
    }

    private fun romanizeKyrgyzInternal(text: String): String {
        val romajiBuilder = StringBuilder(text.length)
        val words = text.split("((?<=\\s|[.,!?;])|(?=\\s|[.,!?;]))".toRegex())
            .filter { it.isNotEmpty() }

        words.forEachIndexed { _, word ->
            if (word.matches("[.,!?;]".toRegex()) || word.isBlank()) {
                romajiBuilder.append(word)
            } else {
                var charIndex = 0
                while (charIndex < word.length) {
                    val charStr = word[charIndex].toString()
                    val romanizedChar = KYRGYZ_ROMAJI_MAP[charStr] ?: GENERAL_CYRILLIC_ROMAJI_MAP[charStr] ?: charStr
                    romajiBuilder.append(romanizedChar)
                    charIndex++
                }
            }
        }
        return romajiBuilder.toString()
    }

    private fun romanizeMacedonianInternal(text: String): String {
        val romajiBuilder = StringBuilder(text.length)
        val words = text.split("((?<=\\s|[.,!?;])|(?=\\s|[.,!?;]))".toRegex())
            .filter { it.isNotEmpty() }

        words.forEachIndexed { _, word ->
            if (word.matches("[.,!?;]".toRegex()) || word.isBlank()) {
                romajiBuilder.append(word)
            } else {
                var charIndex = 0
                while (charIndex < word.length) {
                    val charStr = word[charIndex].toString()
                    val romanizedChar = MACEDONIAN_ROMAJI_MAP[charStr] ?: GENERAL_CYRILLIC_ROMAJI_MAP[charStr] ?: charStr
                    romajiBuilder.append(romanizedChar)
                    charIndex++
                }
            }
        }
        return romajiBuilder.toString()
    }

    // TODO: This function might be used later if we let the user choose the language manually
    /** private suspend fun romanizeCyrillicWithLanguage(text: String, language: CyrillicLanguage): String = withContext(Dispatchers.Default) {
        if (text.isEmpty()) return@withContext ""

        val detectedLanguage = language ?: when {
            isRussian(text) -> CyrillicLanguage.RUSSIAN
            isUkrainian(text) -> CyrillicLanguage.UKRAINIAN
            isSerbian(text) -> CyrillicLanguage.SERBIAN
            isBelarusian(text) -> CyrillicLanguage.BELARUSIAN
            isKyrgyz(text) -> CyrillicLanguage.KYRGYZ
            isMacedonian(text) -> CyrillicLanguage.MACEDONIAN
            else -> return@withContext text
        }

        val languageMap: Map<String, String> = when (detectedLanguage) {
            CyrillicLanguage.RUSSIAN -> RUSSIAN_ROMAJI_MAP
            CyrillicLanguage.UKRAINIAN -> UKRAINIAN_ROMAJI_MAP
            CyrillicLanguage.SERBIAN -> SERBIAN_ROMAJI_MAP
            CyrillicLanguage.BELARUSIAN -> BELARUSIAN_ROMAJI_MAP
            CyrillicLanguage.KYRGYZ -> KYRGYZ_ROMAJI_MAP
            CyrillicLanguage.MACEDONIAN -> MACEDONIAN_ROMAJI_MAP
            // else -> emptyMap()
        }
        val languageLetters = when (language) {
            CyrillicLanguage.RUSSIAN -> RUSSIAN_CYRILLIC_LETTERS
            CyrillicLanguage.UKRAINIAN -> UKRAINIAN_CYRILLIC_LETTERS
            CyrillicLanguage.SERBIAN -> SERBIAN_CYRILLIC_LETTERS
            CyrillicLanguage.BELARUSIAN -> BELARUSIAN_CYRILLIC_LETTERS
            CyrillicLanguage.KYRGYZ -> KYRGYZ_CYRILLIC_LETTERS
            CyrillicLanguage.MACEDONIAN -> MACEDONIAN_CYRILLIC_LETTERS
            else -> GENERAL_CYRILLIC_ROMAJI_MAP.keys
        }

        val romajiBuilder = StringBuilder(text.length)
        val words = text.split("((?<=\\s|[.,!?;])|(?=\\s|[.,!?;]))".toRegex())
            .filter { it.isNotEmpty() }

        words.forEachIndexed { _, word ->
            if (word.matches("[.,!?;]".toRegex()) || word.isBlank()) {
                // Preserve punctuation or spaces as is
                romajiBuilder.append(word)
            } else {
                // Process word
                var charIndex = 0
                while (charIndex < word.length) {
                    var consumed = false
                    // Check for 3-character sequences (language-specific, e.g., Russian)
                    if (detectedLanguage == CyrillicLanguage.RUSSIAN && charIndex + 2 < word.length) {
                        val threeCharCandidate = word.substring(charIndex, charIndex + 3)
                        if (languageLetters is Set<*> && languageLetters.containsAll(threeCharCandidate.toList().map { it.toString() })) {
                            val mappedThreeChar = languageMap[threeCharCandidate]
                            if (mappedThreeChar != null) {
                                romajiBuilder.append(mappedThreeChar)
                                charIndex += 3
                                consumed = true
                            }
                        }
                    }
                    if (!consumed) {
                        val charStr = word[charIndex].toString()
                        val isSpecificLanguageChar = languageLetters is Set<*> && languageLetters.contains(charStr)
                        val isGeneralCyrillicChar = GENERAL_CYRILLIC_ROMAJI_MAP.containsKey(charStr)

                        if (isSpecificLanguageChar || isGeneralCyrillicChar) {
                            if (detectedLanguage == CyrillicLanguage.RUSSIAN && (charStr == "Ðµ" || charStr == "Ð•") && charIndex == 0 && (charIndex == 0 || word[charIndex-1].isWhitespace())) {
                                romajiBuilder.append(if (charStr == "Ðµ") "ye" else "Ye")
                            } else {
                                val romanizedChar = languageMap[charStr] ?: GENERAL_CYRILLIC_ROMAJI_MAP[charStr]
                                if (romanizedChar != null) {
                                    romajiBuilder.append(romanizedChar)
                                } else {
                                    romajiBuilder.append(charStr)
                                }
                            }
                        } else {
                            romajiBuilder.append(charStr)
                        }
                        charIndex += 1
                    }
                }
            }
        }
        romajiBuilder.toString()
    } */

    fun isRussian(text: String): Boolean {
        return text.any { char ->
            RUSSIAN_CYRILLIC_LETTERS.contains(char.toString())
        } && text.all { char ->
            val charStr = char.toString()
            RUSSIAN_CYRILLIC_LETTERS.contains(charStr) || !charStr.matches("[\\u0400-\\u04FF]".toRegex())
        }
    }

    fun isUkrainian(text: String): Boolean {
        return text.any { char ->
            UKRAINIAN_CYRILLIC_LETTERS.contains(char.toString()) || UKRAINIAN_SPECIFIC_CYRILLIC_LETTERS.contains(char.toString())
        } && text.all { char ->
            UKRAINIAN_CYRILLIC_LETTERS.contains(char.toString()) || UKRAINIAN_SPECIFIC_CYRILLIC_LETTERS.contains(char.toString()) || !char.toString().matches("[\\u0400-\\u04FF]".toRegex())
        }
    }

    fun isSerbian(text: String): Boolean {
        return text.any { char ->
            SERBIAN_CYRILLIC_LETTERS.contains(char.toString()) || SERBIAN_SPECIFIC_CYRILLIC_LETTERS.contains(char.toString())
        } && text.all { char ->
            SERBIAN_CYRILLIC_LETTERS.contains(char.toString()) || SERBIAN_SPECIFIC_CYRILLIC_LETTERS.contains(char.toString()) || !char.toString().matches("[\\u0400-\\u04FF]".toRegex())
        }
    }

    fun isBulgarian(text: String): Boolean {
        return text.any { char ->
            BULGARIAN_CYRILLIC_LETTERS.contains(char.toString()) // Bulgarian doesn't have any language specific letters
        } && text.all { char ->
            BULGARIAN_CYRILLIC_LETTERS.contains(char.toString()) || !char.toString().matches("[\\u0400-\\u04FF]".toRegex())
        }
    }

    fun isBelarusian(text: String): Boolean {
        return text.any { char ->
            BELARUSIAN_CYRILLIC_LETTERS.contains(char.toString()) || BELARUSIAN_SPECIFIC_CYRILLIC_LETTERS.contains(char.toString())
        } && text.all { char ->
            BELARUSIAN_CYRILLIC_LETTERS.contains(char.toString()) || BELARUSIAN_SPECIFIC_CYRILLIC_LETTERS.contains(char.toString()) || !char.toString().matches("[\\u0400-\\u04FF]".toRegex())
        }
    }

    fun isKyrgyz(text: String): Boolean {
        return text.any { char ->
            KYRGYZ_CYRILLIC_LETTERS.contains(char.toString()) || KYRGYZ_SPECIFIC_CYRILLIC_LETTERS.contains(char.toString())
        } && text.all { char ->
            KYRGYZ_CYRILLIC_LETTERS.contains(char.toString()) || KYRGYZ_SPECIFIC_CYRILLIC_LETTERS.contains(char.toString()) || !char.toString().matches("[\\u0400-\\u04FF]".toRegex())
        }
    }

    fun isMacedonian(text: String): Boolean {
        return text.any { char ->
            MACEDONIAN_CYRILLIC_LETTERS.contains(char.toString()) || MACEDONIAN_SPECIFIC_CYRILLIC_LETTERS.contains(char.toString())
        } && text.all { char ->
            MACEDONIAN_CYRILLIC_LETTERS.contains(char.toString()) || MACEDONIAN_SPECIFIC_CYRILLIC_LETTERS.contains(char.toString()) || !char.toString().matches("[\\u0400-\\u04FF]".toRegex())
        }
    }

    fun isJapanese(text: String): Boolean {
        return text.any { char ->
            (char in '\u3040'..'\u309F') || // Hiragana
                    (char in '\u30A0'..'\u30FF') || // Katakana
                    (char in '\u4E00'..'\u9FFF') // CJK Unified Ideographs
        }
    }

    fun isKorean(text: String): Boolean {
        return text.any { char ->
            (char in '\uAC00'..'\uD7A3') // Hangul Syllables
        }
    }

    fun isChinese(text: String): Boolean {
        if (text.isEmpty()) return false
        val cjkCharCount = text.count { char -> char in '\u4E00'..'\u9FFF' }
        val hiraganaKatakanaCount = text.count { char -> (char in '\u3040'..'\u309F') || (char in '\u30A0'..'\u30FF') }
        return cjkCharCount > 0 && (hiraganaKatakanaCount.toDouble() / text.length.toDouble()) < 0.1
    }

    fun isHindi(text: String): Boolean {
        return text.any { char ->
            char in '\u0900'..'\u097F'
        }
    }

    suspend fun romanizeHindi(text: String): String = withContext(Dispatchers.Default) {
        val sb = StringBuilder(text.length)
        var i = 0
        while (i < text.length) {
            var consumed = false
            // Check for 2-character sequences (e.g. char + nukta)
            if (i + 1 < text.length) {
                val twoCharCandidate = text.substring(i, i + 2)
                val mappedTwoChar = DEVANAGARI_ROMAJI_MAP[twoCharCandidate]
                if (mappedTwoChar != null) {
                    sb.append(mappedTwoChar)
                    i += 2
                    consumed = true
                }
            }

            if (!consumed) {
                val charStr = text[i].toString()
                sb.append(DEVANAGARI_ROMAJI_MAP[charStr] ?: charStr)
                i += 1
            }
        }
        sb.toString()
    }

    fun isPunjabi(text: String): Boolean {
        return text.any { char ->
            char in '\u0A00'..'\u0A7F'
        }
    }

    suspend fun romanizePunjabi(text: String): String = withContext(Dispatchers.Default) {
        val sb = StringBuilder(text.length)
        var i = 0
        while (i < text.length) {
            val char = text[i]
            var consumed = false

            // Check for Adhak (Gemination)
            if (char == '\u0A71') {
                 // Double next consonant if possible
                 if (i + 1 < text.length) {
                     val nextCharStr = text[i+1].toString()
                     val nextMapped = GURMUKHI_ROMAJI_MAP[nextCharStr]
                     if (nextMapped != null && nextMapped.isNotEmpty()) {
                         sb.append(nextMapped[0])
                     }
                 }
                 i++
                 continue
            }

            // Check for 2-character sequences (e.g. char + nukta)
            if (i + 1 < text.length) {
                val twoCharCandidate = text.substring(i, i + 2)
                val mappedTwoChar = GURMUKHI_ROMAJI_MAP[twoCharCandidate]
                if (mappedTwoChar != null) {
                    sb.append(mappedTwoChar)
                    i += 2
                    consumed = true
                }
            }

            if (!consumed) {
                val str = char.toString()
                sb.append(GURMUKHI_ROMAJI_MAP[str] ?: str)
                i++
            }
        }
        sb.toString()
    }

    suspend fun romanize(
        text: String,
        line: String,
        enabledLanguages: List<String>,
        romanizeCyrillicByLine: Boolean
    ): String? {
        val detectionText = if (romanizeCyrillicByLine) line else text
        return when {
            "Japanese" in enabledLanguages && isJapanese(detectionText) && !isChinese(detectionText) -> romanizeJapanese(line)
            "Korean" in enabledLanguages && isKorean(detectionText) -> romanizeKorean(line)
            "Chinese" in enabledLanguages && isChinese(detectionText) -> romanizeChinese(line)
            "Hindi" in enabledLanguages && isHindi(detectionText) -> romanizeHindi(line)
            "Ukrainian" in enabledLanguages && isUkrainian(detectionText) -> romanizeCyrillic(line, "Ukrainian")
            "Russian" in enabledLanguages && isRussian(detectionText) -> romanizeCyrillic(line, "Russian")
            "Serbian" in enabledLanguages && isSerbian(detectionText) -> romanizeCyrillic(line, "Serbian")
            "Bulgarian" in enabledLanguages && isBulgarian(detectionText) -> romanizeCyrillic(line, "Bulgarian")
            "Belarusian" in enabledLanguages && isBelarusian(detectionText) -> romanizeCyrillic(line, "Belarusian")
            "Kyrgyz" in enabledLanguages && isKyrgyz(detectionText) -> romanizeCyrillic(line, "Kyrgyz")
            "Macedonian" in enabledLanguages && isMacedonian(detectionText) -> romanizeCyrillic(line, "Macedonian")
            else -> null
        }
    }

    private fun isCyrillicVowel(char: Char): Boolean {
        return "ÐÐ°Ð•ÐµÐ„Ñ”Ð˜Ð¸Ð†Ñ–Ð‡Ñ—ÐžÐ¾Ð£ÑƒÐ®ÑŽÐ¯ÑÐ«Ñ‹Ð­Ñ".contains(char)
    }

    fun isWordSynced(lyrics: String): Boolean {
        return (lyrics.contains("<") && lyrics.contains(">") && (lyrics.contains("|") || lyrics.contains(":"))) ||
                lyrics.contains(RICH_SYNC_WORD_REGEX)
    }

    fun isLineSynced(lyrics: String): Boolean {
        return lyrics.contains(TIME_REGEX) ||
                lyrics.contains(PAXSENIX_AGENT_LINE_REGEX) ||
                lyrics.contains(PAXSENIX_BG_LINE_REGEX)
    }

    fun getLyricsQuality(lyrics: String): Int {
        if (lyrics.isBlank() || lyrics == "Lyrics not found") return 0
        if (isWordSynced(lyrics)) return 3
        if (isLineSynced(lyrics)) return 2
        return 1
    }
}
