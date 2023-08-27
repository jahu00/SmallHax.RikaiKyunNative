package smallhax.rikaikyunnative

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private val TAG: String = "MainActivity"
    private lateinit var openButton: Button
    private lateinit var continueButton: Button

    private lateinit var resultLauncher: ActivityResultLauncher<Intent>
    private lateinit var application: ExtendedApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        application = applicationContext as ExtendedApplication

        resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result -> activityResultHandler(result)
        }

        openButton = findViewById(R.id.openButton)
        openButton.setOnClickListener {
            //Log.e(TAG, "button clicked")
            var chooseFile = Intent(Intent.ACTION_OPEN_DOCUMENT)
            chooseFile.type = "*/*"
            chooseFile = Intent.createChooser(chooseFile, "Choose a file")
            resultLauncher.launch(chooseFile)
        }

        continueButton = findViewById(R.id.continueButton)
        continueButton.setOnClickListener {
            openReader()
        }
    }

    private fun openReader(){
        var switchActivityIntent = Intent(this@MainActivity, ReaderActivity::class.java)
        //switchActivityIntent.putExtra("path", uri.toString())
        startActivity(switchActivityIntent)
    }

    private fun activityResultHandler (result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            // There are no request codes
            val intent: Intent? = result.data
            var uri: Uri? = intent!!.data
            //var path = uri?.path
            //Log.e(TAG, uri.toString())

            application.openDocument(uri!!)
            openReader()
        }
    }
}