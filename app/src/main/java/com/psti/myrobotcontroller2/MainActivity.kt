package com.psti.myrobotcontroller2

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private var isConnected = false
    private var someData: String? = null
    private var isVacuumOn = false
    private var raspberryPiIp: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Restore state if available
        if (savedInstanceState != null) {
            isConnected = savedInstanceState.getBoolean("isConnected")
            someData = savedInstanceState.getString("someData")
            isVacuumOn = savedInstanceState.getBoolean("isVacuumOn")
            raspberryPiIp = savedInstanceState.getString("raspberryPiIp")
            Log.d("MainActivity", "Restored data: isConnected=$isConnected, someData=$someData, isVacuumOn=$isVacuumOn, raspberryPiIp=$raspberryPiIp")
        }

        // Check if raspberryPiIp is not null, set up connection state accordingly
        if (!raspberryPiIp.isNullOrEmpty()) {
            isConnected = true // Assume the connection is still valid if IP is available
        }

        val btnStart: Button? = findViewById(R.id.start)
        btnStart?.setOnClickListener {
            Log.d("MainActivity", "Start button clicked")
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }

        val connect: Button? = findViewById(R.id.connect)
        connect?.setOnClickListener {
            Log.d("MainActivity", "Connect button clicked")
            val ipInput = findViewById<EditText>(R.id.ipAddress)
            raspberryPiIp = ipInput?.text.toString()
            connectToRaspberryPi()
        }

        // Initialize buttons and their listeners after setting up the IP address
        setupButtons()
    }

    private fun setupButtons() {
        // Movement buttons
        setupMovementButton(R.id.btnUp, "front")
        setupMovementButton(R.id.btnDown, "back")
        setupMovementButton(R.id.btnLeft, "left")
        setupMovementButton(R.id.btnRight, "right")

        // DOF1 buttons
        setupDOFButton(R.id.btndof1kiri, "s1:2", "s1:0")
        setupDOFButton(R.id.btndof1kanan, "s1:1", "s1:0")

        // DOF2 buttons
        setupDOFButton(R.id.btndof2atas, "s2:2", "s2:0")
        setupDOFButton(R.id.btndof2bawah, "s2:1", "s2:0")

        // DOF3 buttons
        setupDOFButton(R.id.btndof3atas, "s3:1", "s3:0")
        setupDOFButton(R.id.btndof3bawah, "s3:2", "s3:0")

        // DOF4 buttons
        setupDOFButton(R.id.btndof4atas, "s4:1", "s4:0")
        setupDOFButton(R.id.btndof4bawah, "s4:2", "s4:0")

        // Vacuum button
        val btnVacuum: Button? = findViewById(R.id.btnVacuum)
        btnVacuum?.setOnClickListener {
            isVacuumOn = !isVacuumOn
            val command = if (isVacuumOn) "vacuum:on" else "vacuum:off"
            Log.d("MainActivity", "Vacuum button clicked, sending command: $command")
            sendCommand(command)
        }

        val btnShutdown: Button? = findViewById(R.id.btnShutdown)
        btnShutdown?.setOnClickListener {
            Log.d("MainActivity", "Shutdown button clicked")
            sendCommand("shutdown")
        }

    }

    private fun setupMovementButton(buttonId: Int, command: String) {
        val button: Button? = findViewById(buttonId)
        button?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    Log.d("MainActivity", "$command button pressed")
                    sendCommand("${command}1")
                }
                MotionEvent.ACTION_UP -> {
                    Log.d("MainActivity", "$command button released, sending '${command}0'")
                    sendCommand("${command}0")
                }
            }
            false
        }
    }

    private fun setupDOFButton(buttonId: Int, pressCommand: String, releaseCommand: String) {
        val button: Button? = findViewById(buttonId)
        button?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    Log.d("MainActivity", "DOF button pressed, sending command: $pressCommand")
                    sendCommand(pressCommand)
                }
                MotionEvent.ACTION_UP -> {
                    Log.d("MainActivity", "DOF button released, sending command: $releaseCommand")
                    sendCommand(releaseCommand)
                }
            }
            false
        }
    }

    private fun connectToRaspberryPi() {
        if (raspberryPiIp.isNullOrEmpty()) {
            Log.d("MainActivity", "IP address not provided")
            return
        }

        Thread {
            try {
                val url = URL("http://$raspberryPiIp:5000/connect")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    isConnected = true
                    val inputStream = connection.inputStream
                    val response = inputStream.bufferedReader().use { it.readText() }
                    Log.d("MainActivity", "Connected: $response")
                } else {
                    Log.d("MainActivity", "Connection failed: $responseCode")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Connection error", e)
            }
        }.start()
    }

    private fun sendCommand(command: String) {
        if (raspberryPiIp.isNullOrEmpty()) {
            Log.d("MainActivity", "IP address not provided, cannot send command: $command")
            return
        }

        Log.d("MainActivity", "Attempting to send command: $command, raspberryPiIp=$raspberryPiIp")

        Thread {
            try {
                val url = URL("http://$raspberryPiIp:5000/$command")
                Log.d("MainActivity", "Sending request to URL: $url")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val response = inputStream.bufferedReader().use { it.readText() }
                    Log.d("MainActivity", "ESP32 Response: $response")
                } else {
                    Log.d("MainActivity", "GET request to ESP32 failed: $responseCode")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "ESP32 Command error", e)
            }
        }.start()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("isConnected", isConnected)
        outState.putString("someData", someData)
        outState.putBoolean("isVacuumOn", isVacuumOn)
        outState.putString("raspberryPiIp", raspberryPiIp)
        Log.d("MainActivity", "State saved: isConnected=$isConnected, someData=$someData, isVacuumOn=$isVacuumOn, raspberryPiIp=$raspberryPiIp")
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        isConnected = savedInstanceState.getBoolean("isConnected")
        someData = savedInstanceState.getString("someData")
        isVacuumOn = savedInstanceState.getBoolean("isVacuumOn")
        raspberryPiIp = savedInstanceState.getString("raspberryPiIp")
        Log.d("MainActivity", "State restored: isConnected=$isConnected, someData=$someData, isVacuumOn=$isVacuumOn, raspberryPiIp=$raspberryPiIp")
    }
}
