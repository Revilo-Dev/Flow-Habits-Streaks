package org.isoron.uhabits.activities

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import org.isoron.uhabits.utils.FlowAccent

abstract class FlowActivity : AppCompatActivity() {
    private var accentAtCreation: FlowAccent.State? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        accentAtCreation = FlowAccent.snapshot(this)
    }

    override fun onResume() {
        super.onResume()
        if (accentAtCreation != FlowAccent.snapshot(this)) recreate()
    }
}
