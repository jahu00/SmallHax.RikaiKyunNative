package smallhax.rikaikyunnative

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_UP
import android.view.View
import android.widget.FrameLayout
import android.widget.ScrollView
import android.widget.TextView
import com.google.android.material.color.MaterialColors
import kotlin.system.measureTimeMillis

class ReaderActivity : AppCompatActivity() {
    private var TAG: String = "ReaderActivity"
    private var text: String = ""
    private lateinit var textView: TextView
    private lateinit var dictionaryTextView: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var dictionaryPopup: FrameLayout
    private lateinit var dictionaryScrollView: ScrollView
    private lateinit var application: ExtendedApplication
    private lateinit var spannable: Spannable
    private var foregroundSelectionSpan: ForegroundColorSpan? = null
    private var backgroundSelectionSpan: BackgroundColorSpan? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader)

        application = applicationContext as ExtendedApplication
        /*val path = intent.getStringExtra("path")
        val uri = Uri.parse(path)
        val inputStream = contentResolver.openInputStream(uri)*/
        textView = findViewById(R.id.textView)
        text = application.document
        spannable = SpannableString(text)
        textView.text = spannable
        textView.setOnTouchListener(View.OnTouchListener {
                view, motionEvent -> onTextViewTouch(view, motionEvent)
        })
        scrollView = findViewById(R.id.scrollView)

        dictionaryPopup = findViewById(R.id.dictionaryPupup)
        dictionaryTextView = findViewById(R.id.dictionaryTextView)
        dictionaryScrollView = findViewById(R.id.dictionaryScrollView)
    }

    private fun onTextViewTouch(view: View, event: MotionEvent): Boolean {
        //Log.e(TAG, event.action.toString())
        if (event.action != ACTION_UP){
            return true
        }

        dictionaryPopup.visibility = View.VISIBLE

        var textViewLocation = IntArray(2)
        textView.getLocationOnScreen(textViewLocation)
        val x = event.getRawX() - textViewLocation[0] - textView.textSize / 2
        val y = event.getRawY() - textViewLocation[1] //scrollView!!.scrollY - textView!!.y
        val layout = textView.layout
        val lineId = layout.getLineForVertical(y.toInt())
        var i = layout.getOffsetForHorizontal(lineId, x)// - 1
        if (i < 0){
            //i = 0
            unselect()
            dictionaryTextView.setText("")
            return true
        }
        //val bounds = layout.getLineBounds(lineId)
        //Log.e(TAG, lineId.toString())
        //Log.e(TAG, i.toString())
        val words = getLookups(i)
        val dictionary = application.edict

        var lookups: List<Lookup>
        val deinflectElapsedTime = measureTimeMillis {
            lookups = dictionary.prepareLookups(words)
        }
        Log.e(TAG, "Deinflect time (${words.count()}): ${deinflectElapsedTime}")
        var result: List<SearchResult>
        val searchElapsedTime = measureTimeMillis {
            result = dictionary.search(lookups)
        }
        Log.e(TAG, "Search time (${lookups.count()}): ${searchElapsedTime}")
        if (result.count() > 0) {
            val firstResult = result[0]
            //Log.e(TAG, firstResult.lookup)
            //Log.e(TAG, firstResult.entry)
            val entries = result.joinToString("\n\n") { x -> "${x.entry.word} ${if(x.entry.reading == null) "" else "[${x.entry.reading}]"} - ${x.entry.definition}" }
            dictionaryTextView.setText(entries)
            dictionaryScrollView.scrollY = 0
            select(i, i + firstResult.lookup.word.length)
        }
        else
        {
            select(i, i + 1)
            dictionaryTextView.setText("")
        }

        return true;
    }

    fun select(startPosition: Int, endPosition: Int){
        val elapsedTime = measureTimeMillis {
            unselect(false)
            foregroundSelectionSpan = ForegroundColorSpan(
                MaterialColors.getColor(
                    dictionaryTextView,
                    com.google.android.material.R.attr.background
                )
            )
            backgroundSelectionSpan = BackgroundColorSpan(textView.currentTextColor)
            spannable.setSpan(
                backgroundSelectionSpan,
                startPosition,
                endPosition,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannable.setSpan(
                foregroundSelectionSpan,
                startPosition,
                endPosition,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            updateTextView()
        }

        Log.e(TAG, "Select time: ${elapsedTime}")
    }

    fun unselect(shouldUpdateTextView: Boolean = true){
        if (foregroundSelectionSpan != null){
            spannable.removeSpan(foregroundSelectionSpan)
            foregroundSelectionSpan = null
        }
        if (backgroundSelectionSpan != null){
            spannable.removeSpan(backgroundSelectionSpan)
            backgroundSelectionSpan = null
        }
        if (shouldUpdateTextView){
            updateTextView()
        }
    }

    fun updateTextView(){
        textView.setText(spannable)
    }

    fun getLookups(startPosition: Int, maxLength: Int = 13): List<String>{
        var endPosition = startPosition + maxLength
        if (endPosition > text.length){
            endPosition = text.length
        }
        val result = mutableListOf<String>()
        while (endPosition > startPosition){
            result.add(text.substring(startPosition, endPosition))
            endPosition--
        }
        return result
    }
}