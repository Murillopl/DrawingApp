package com.example.drawingapp

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.app.admin.PackagePolicy
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.Image
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.registerForActivityResult
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import yuku.ambilwarna.AmbilWarnaDialog
import java.io.File
import java.io.FileOutputStream
import java.util.Random

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var drawingView: DrawingView
    private lateinit var brushButton: ImageButton
    private lateinit var purpleButton: ImageButton
    private lateinit var redButton: ImageButton
    private lateinit var orangeButton: ImageButton
    private lateinit var greenButton: ImageButton
    private lateinit var blueButton: ImageButton
    private lateinit var undoButton: ImageButton
    private lateinit var colorPickerButton: ImageButton
    private lateinit var galleryButton: ImageButton
    private lateinit var saveButton: ImageButton

    private val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            findViewById<ImageView>(R.id.gallery_image).setImageURI(result.data?.data)
        }

    val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->

            val granted = permissions.values.any { it }

            if (granted) {
                openGallery()
            } else {
                Toast.makeText(
                    this,
                    "Permission denied. Enable in settings.",
                    Toast.LENGTH_LONG
                ).show()

                openAppSettings()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        brushButton = findViewById(R.id.brush_button)

        purpleButton = findViewById(R.id.purple_button)
        orangeButton = findViewById(R.id.orange_button)
        redButton = findViewById(R.id.red_button)
        greenButton = findViewById(R.id.green_button)
        blueButton = findViewById(R.id.blue_button)

        undoButton = findViewById(R.id.undo_button)
        colorPickerButton = findViewById(R.id.color_picker_button)
        galleryButton = findViewById(R.id.gallery_button)
        saveButton = findViewById(R.id.save_button)

        drawingView = findViewById(R.id.drawing_view)
        drawingView.changeBrushSize(23.toFloat())


        brushButton.setOnClickListener {
            showBrushChooserDialog()
        }

        purpleButton.setOnClickListener(this)
        greenButton.setOnClickListener(this)
        redButton.setOnClickListener(this)
        orangeButton.setOnClickListener(this)
        blueButton.setOnClickListener(this)

        undoButton.setOnClickListener(this)
        colorPickerButton.setOnClickListener(this)
        galleryButton.setOnClickListener(this)
        saveButton.setOnClickListener(this)


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun showBrushChooserDialog() {
        val brushDialog = Dialog(this@MainActivity)
        brushDialog.setContentView(R.layout.dialog_brush)
        val seekBarProgress = brushDialog.findViewById<SeekBar>(R.id.dialog_seek_bar)
        val showProgressTv = brushDialog.findViewById<TextView>(R.id.dialog_text_view_progress)

        seekBarProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(
                seekBar: SeekBar,
                p1: Int,
                p2: Boolean
            ) {

                drawingView.changeBrushSize(seekBar.progress.toFloat())
                showProgressTv.text = seekBar.progress.toString()
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {

            }

            override fun onStopTrackingTouch(p0: SeekBar?) {

            }
        })
        brushDialog.show()
    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.purple_button -> {
                drawingView.setColor("#D14EF6")
            }

            R.id.red_button -> {
                drawingView.setColor("#FA5B68")
            }

            R.id.orange_button -> {
                drawingView.setColor("#EFB041")
            }

            R.id.green_button -> {
                drawingView.setColor("#2DC408")
            }

            R.id.blue_button -> {
                drawingView.setColor("#2F6FF1")
            }

            R.id.undo_button -> {
                drawingView.undoPath()
            }

            R.id.color_picker_button -> {
                showColorPickerDialog()
            }

            R.id.gallery_button -> {
                checkGalleryPermission()
            }

            R.id.save_button -> {
                val layout = findViewById<RelativeLayout>(R.id.main)

                layout.post {
                    val bitmap = getBitmapFromView(layout)
                    CoroutineScope(IO).launch {
                        saveImage(bitmap)
                    }
                }
            }
        }
    }

    private fun showColorPickerDialog() {
        val dialog =
            AmbilWarnaDialog(
                this,
                Color.GREEN,
                object : AmbilWarnaDialog.OnAmbilWarnaListener {
                    override fun onCancel(dialog: AmbilWarnaDialog?) {

                    }

                    override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                        drawingView.setColor(color)
                    }

                })
        dialog.show()

    }


    private fun showRationaleDialog(permission: String) {
        AlertDialog.Builder(this)
            .setTitle("Storage Permission")
            .setMessage(getString(R.string.we_need_this_permission_in_order_to_access_the_internal_storage))
            .setPositiveButton(R.string.yes) { _, _ ->
                requestPermission.launch(
                    arrayOf(permission, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                )
            }
            .setNegativeButton("Cancell", null)
            .show()
    }


    private fun openGallery() {
        val pickIntent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        openGalleryLauncher.launch(pickIntent)
    }

    private fun openAppSettings() {
        val intent = Intent(
            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        )
        intent.data = android.net.Uri.fromParts("package", packageName, null)
        startActivity(intent)
    }

    private fun checkGalleryPermission() {

        val permission =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                Manifest.permission.READ_MEDIA_IMAGES
            else
                Manifest.permission.READ_EXTERNAL_STORAGE

        when {
            ActivityCompat.checkSelfPermission(this, permission)
                    == PackageManager.PERMISSION_GRANTED -> {
                openGallery()
            }

            ActivityCompat.shouldShowRequestPermissionRationale(this, permission) -> {
                showRationaleDialog(permission)
            }

            else -> {
                requestPermission.launch(
                    arrayOf(
                        permission,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                )
            }
        }
    }

    private fun getBitmapFromView(view: View)
            : Bitmap {
        val bitmap = Bitmap.createBitmap(
            view.width, view.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    private suspend fun saveImage(bitmap: Bitmap) {
        withContext(IO) {
            val filename = "Drawing_${System.currentTimeMillis()}.jpg"

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/DrawingApp")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }

            val resolver = contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

            uri?.let {
                resolver.openOutputStream(it)?.use { output ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
                }

                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(it, values, null, null)

                withContext(Main) {
                    Toast.makeText(this@MainActivity, "\n" +
                            getString(R.string.image_saved_in_gallery), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}