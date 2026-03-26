package com.example.vibrationtester

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var vibrator: Vibrator
    private var currentAmplitude = 128
    private var isAlarmVibrating = false
    private var isNotificationVibrating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 获取 Vibrator
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        val seekBar = findViewById<SeekBar>(R.id.seekBarAmplitude)
        val tvAmplitude = findViewById<TextView>(R.id.tvAmplitudeValue)
        val tvWarning = findViewById<TextView>(R.id.tvAmplitudeWarning)
        val btnNotification = findViewById<Button>(R.id.btnNotification)
        val btnAlarm = findViewById<Button>(R.id.btnAlarm)

        // 检测是否支持幅度控制
        val supportsAmplitude = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.hasAmplitudeControl()
        } else {
            false
        }

        if (!supportsAmplitude) {
            tvWarning.text = "⚠️ 当前设备不支持幅度调节，Slider 无效"
            tvWarning.visibility = android.view.View.VISIBLE
        }

        // SeekBar 设置（1–255）
        seekBar.max = 254 // 0 = 1, 254 = 255
        seekBar.progress = 127 // 默认 128
        tvAmplitude.text = "128"

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                currentAmplitude = progress + 1
                tvAmplitude.text = currentAmplitude.toString()
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        // Notification 按钮：1000ms，再次点击停止
        btnNotification.setOnClickListener {
            if (isNotificationVibrating) {
                vibrator.cancel()
                isNotificationVibrating = false
                btnNotification.text = "Notification"
            } else {
                stopAll()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createOneShot(1000L, currentAmplitude)
                    vibrator.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(1000L)
                }
                isNotificationVibrating = true
                btnNotification.text = "■ 停止 Notification"
                // 1000ms 后自动恢复按钮状态
                btnNotification.postDelayed({
                    isNotificationVibrating = false
                    btnNotification.text = "Notification"
                }, 1100L)
            }
        }

        // Alarm 按钮：持续振动，再次点击停止
        btnAlarm.setOnClickListener {
            if (isAlarmVibrating) {
                vibrator.cancel()
                isAlarmVibrating = false
                btnAlarm.text = "Alarm"
            } else {
                stopAll()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // 用 createWaveform 循环实现持续振动
                    val timings = longArrayOf(0, 60000) // 单次60秒，循环
                    val amplitudes = intArrayOf(0, currentAmplitude)
                    val effect = VibrationEffect.createWaveform(timings, amplitudes, 1)
                    vibrator.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    val pattern = longArrayOf(0, 60000)
                    vibrator.vibrate(pattern, 0)
                }
                isAlarmVibrating = true
                btnAlarm.text = "■ 停止 Alarm"
            }
        }
    }

    private fun stopAll() {
        vibrator.cancel()
        isAlarmVibrating = false
        isNotificationVibrating = false
        findViewById<Button>(R.id.btnAlarm).text = "Alarm"
        findViewById<Button>(R.id.btnNotification).text = "Notification"
    }

    override fun onDestroy() {
        super.onDestroy()
        vibrator.cancel()
    }
}
