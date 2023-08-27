package smallhax.rikaikyunnative

import android.app.Application
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader


class ExtendedApplication : Application() {
    private var cleanFuriganaRegex: Regex = Regex("《(?<furigana>[^》]+)》")

    private var _edict : EdictDictionary? = null
    var edict: EdictDictionary
        get() {
            if (_edict == null){
                _edict = EdictDictionary(baseContext, "dictionaries/japanese/edict_utf8", "dictionaries/japanese/index_string", "dictionaries/japanese/deinflect")
            }
            return _edict!!
        }
        set(value) {
            _edict = value
        }

    var document: String = ""
    var position: Float = 0f

    fun openDocument(path: String){
        val uri = Uri.parse(path)
        openDocument(uri)
    }

    fun openDocument(uri: Uri){
        val inputStream = contentResolver.openInputStream(uri)
        document = inputStream!!.bufferedReader().use {it.readText()}
        document = cleanFuriganaRegex.replace(document, "")
        position = 0f
    }
}