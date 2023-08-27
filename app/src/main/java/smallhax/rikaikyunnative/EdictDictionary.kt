package smallhax.rikaikyunnative

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.InputStream

class Lookup {
    var word: String
    var deinflectedWord: String
    var requiredTags: List<String>
    var inflections: List<String>

    constructor(word: String, deinflectedWord: String, requiredTags: List<String>, inflections: List<String>){
        this.word = word
        this.deinflectedWord = deinflectedWord
        this.requiredTags = requiredTags
        this.inflections = inflections
    }
}

class SearchResult {
    var lookup: Lookup
    var entry: DictionaryEntry

    constructor(lookup: Lookup, entry: DictionaryEntry){
        this.lookup = lookup
        this.entry = entry
    }
}

class DictionaryEntry {
    var word: String
    var reading: String?
    var definition: String
    var tags: List<String>

    constructor(word: String, reading: String?, definition: String, tags: List<String>){
        this.word = word
        this.reading = reading
        this.definition = definition
        this.tags = tags
    }
}

enum class DeinflectType{
    Prefix,
    Suffix
}

class Deinflect {
    var id: String
    var name: String
    var tag: String

    constructor(id: String, name: String, tag: String){
        this.id = id
        this.name = name
        this.tag = tag
    }
}

class DeinflectRule {
    var deinflectId: String
    var type: DeinflectType
    var text: String
    var replace: String
    var tags: List<String>

    constructor(deinflectId: String, type:DeinflectType, text: String, replace: String, tags: List<String>){
        this.deinflectId = deinflectId
        this.type = type
        this.text = text
        this.replace = replace
        this.tags = tags
    }
}

class EdictDictionary {
    private val TAG: String = "EdictDictionary"
    private val rowParseRegex: Regex = Regex("^(?<Kanji>.*?)\\s(?:\\[(?<Kana>.*?)\\]\\s)?(?:\\/(?<Gloss>.*?))?\\/\$")
    private val tagParseRegex: Regex = Regex("\\((?<Tags>[^() ]+)\\)")
    private val newLineSequence: String = "\n"
    private var data: String
    private var index: Map<String,Array<Int>>

    private lateinit var validTags: List<String>
    private lateinit var deinflects: Map<String,Deinflect>
    private lateinit var deinflectRules: List<DeinflectRule>

    constructor(context: Context, dictionaryPath:String, indexPath: String, deinflectPath: String? = null) {
        val assets = context.getAssets()
        val inputStream = assets.open(dictionaryPath)
        data = inputStream.bufferedReader().use {it.readText()}
        val indexInputStream = assets.open(indexPath)
        index = getIndex(indexInputStream)
        if (deinflectPath == null){
            validTags = listOf()
            deinflects = mapOf()
            deinflectRules = listOf()
        }
        else
        {
            val deinflectInputStream = assets.open(deinflectPath!!)
            loadDeinflects(deinflectInputStream)
        }
    }

    private fun loadDeinflects(inputStream: InputStream) {
        inputStream.bufferedReader().use {
            reader ->
            validTags = reader.readLine().split(",").map { x -> x.trim() }.filter { x -> x.length > 0 }
            deinflects = readDeinflects(reader)
            deinflectRules = readDeinflectRules(reader)
        }
    }

    private fun readDeinflectRules(reader: BufferedReader): List<DeinflectRule> {
        val result = mutableListOf<DeinflectRule>()
        do{
            val line = reader.readLine()
            if (line == "" || line == null)
            {
                break;
            }
            val split = line.split('/');
            var rule = DeinflectRule (
                deinflectId = split[0],
                type = if (split[1] == "S") DeinflectType.Suffix else DeinflectType.Prefix,
                text = split[2],
                replace = split[3],
                tags = split[4].split(',').filter{x -> x != ""}.map{x -> x.trim()}
            )
            result.add(rule);
        } while(true)
        return result;
    }

    private fun readDeinflects(reader: BufferedReader): Map<String, Deinflect> {
        val result = mutableListOf<Deinflect>()
        do{
            val line = reader.readLine()
            if (line == "" || line == null)
            {
                break;
            }
            val split = line.split('/');
            val deinflect = Deinflect(
               id = split[0],
               tag =  split[1],
               name = split[2]
            );
            result.add(deinflect);
        } while(true)
        return result.associateBy({it.id}, {it})
    }

    fun prepareLookups(words: List<String>, watchdog : Int = 255): List<Lookup>{
        return words.flatMap { word -> deinflect(word, watchdog = watchdog) }
    }

    fun deinflect(startingWord: String, word: String? = null, inflections: List<String>? = null, tags: List<String>? = null, watchdog: Int = 255): List<Lookup>{
        val _watchdog = watchdog - 1;
        if (_watchdog == 0)
        {
            throw Exception("Watchdog triggered");
        }
        var result = mutableListOf<Lookup>()
        val _inflections = inflections ?: listOf()
        val _tags = tags ?: listOf();
        val _word = word ?: startingWord
        if (_tags.count() == 0 || validTags.count() == 0 || _tags.any{tag -> validTags.contains(tag) }) {
            val lookup = Lookup(
                deinflectedWord = _word,
                inflections = _inflections,
                requiredTags = _tags,
                word = startingWord,
            )
            result.add(lookup)
        }
        for (rule in deinflectRules) {
            val deinflect = deinflects[rule.deinflectId]
            if (_tags.count() > 0 && !_tags.contains(deinflect!!.tag))
            {
                continue;
            }
            var deinflectedWord: String
            if (rule.type == DeinflectType.Suffix)
            {
                if (!_word.endsWith(rule.text))
                {
                    continue;
                }
                deinflectedWord = _word.substring(0, _word.length - rule.text.length) + rule.replace;
            }
            else if (rule.type == DeinflectType.Prefix)
            {
                if (!_word.startsWith(rule.text))
                {
                    continue;
                }
                deinflectedWord = _word.substring(0, _word.length - rule.text.length) + rule.replace;
            }
            else
            {
                throw Exception("Unsupported deinflect type ${rule.type}");
            }
            val derivativeInflections = mutableListOf(deinflect!!.name)
            derivativeInflections.addAll(_inflections);
            var derivatives = deinflect(
                startingWord = startingWord,
                word = deinflectedWord,
                inflections = derivativeInflections,
                tags = rule.tags,
                watchdog = _watchdog
            );
            result.addAll(derivatives);
        }
        return result;
    }

    fun getIndex(inputStream: InputStream) : Map<String,Array<Int>> {
        val data = inputStream.bufferedReader().use {it.readText()}
        val split = data.split(newLineSequence)
        return split.associate {
            row -> parseIndexRow(row)
        }
    }

    fun parseIndexRow(row: String): Pair<String, Array<Int>>{
        val rowSplit = row.split("/")
        val key = rowSplit[0]
        val positions = rowSplit.drop(1).map { positionString -> positionString.toInt() }.toTypedArray()
        return Pair( key,  positions )
    }

    fun searchIndex(word: String): Array<Int>?{
        return index.get(word)
    }

    fun search(lookups: List<Lookup>): List<SearchResult>{
        val result = mutableListOf<SearchResult>()
        // TODO: use flatMap
        lookups.forEach(){
            lookup -> result.addAll(search(lookup))
        }
        return result
    }

    fun search(lookup: Lookup): List<SearchResult>{
        val positions = searchIndex(lookup.deinflectedWord)
        if (positions == null){
            return listOf()
        }
        return positions!!.map{
            position -> getSearchResult(lookup, position)
        }
    }

    fun getSearchResult(lookup: Lookup, startIndex: Int): SearchResult{
        var line = getLine(startIndex)
        var entry = parseLine(line)
        return SearchResult(lookup, entry)
    }

    fun getLine(startIndex: Int): String{
        val endIndex = data.indexOf(newLineSequence, startIndex)
        val line = data.substring(startIndex, endIndex)
        return line
    }

    fun parseLine(line: String): DictionaryEntry{
        var rowMatch = rowParseRegex.matchEntire(line);
        if (rowMatch == null)
        {
            throw Exception("Unable to parse row \"${line}\"");
        }

        var gloss = rowMatch.groups["Gloss"]!!.value.trim();

        var tagMatchs = tagParseRegex.findAll(gloss);
        var tags = tagMatchs.flatMap { match -> match.groups["Tags"]!!.value.split(',') }.map{x -> x.trim()}.distinct().toList()

        var entry = DictionaryEntry(
            rowMatch.groups["Kanji"]!!.value.trim(),
            rowMatch.groups["Kana"]?.value?.trim(),
            gloss,
            tags
        )
        return entry;
    }
}