package camp.visual.android.kotlin.sample.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import camp.visual.android.kotlin.sample.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.gazeTrackerInit.setOnTouchListener { v, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    var isHidden = binding.optionContainer.visibility == View.GONE
                    binding.optionContainer.visibility = if (isHidden) View.VISIBLE else View.GONE
                }
                else -> {

                }
            }
            true
        }
        binding.courses.check(binding.onePoint.id)
        binding.switchUserOption.setOnCheckedChangeListener { _, isChecked ->

        }
    }

    override fun onStart() {
        super.onStart()
    }

}