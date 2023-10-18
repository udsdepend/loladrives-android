package de.unisaarland.loladrives.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import de.unisaarland.loladrives.MainActivity
import de.unisaarland.loladrives.R
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.fragment_simple_html_content.*

class SimpleHTMLContentFragment : Fragment {
    private var activity: MainActivity? = null

    private var _url: String = "about:blank"
    private var _title: String = ""

    var url: String
        get() = _url
        set(value) {
            _url = value
            webView?.loadUrl(value)
        }

    var title: String
        get() = _title
        set(value) {
            _title = value
            activity?.title_textview?.text = value
        }

    constructor() : super()
    constructor(url: String) : this() {
        this.url = url
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        activity = requireActivity() as MainActivity
        return inflater.inflate(R.layout.fragment_simple_html_content, container, false)
    }

    override fun onStart() {
        activity?.title_textview?.text = title
        activity?.backButton?.setImageResource(R.drawable.back_arrow_icon)

        // webView.getSettings().setJavaScriptEnabled(true)
        webView.loadUrl(url)
//        dismissButton.setOnClickListener {
//            activity?.onBackPressed()
//        }

        super.onStart()
    }
}
