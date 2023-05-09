package com.progsoft.dictionary

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.progsoft.dictionary.jpinyin.PinyinFormat
import com.progsoft.dictionary.jpinyin.PinyinHelper
import com.progsoft.dictionary.util.FontStrokeUtil
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private var autoDraw = false;
    private var showMedian = false;
    private var s = "松鼠乖巧歆"
    private var loop = 0;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView.text = "PC-VER:" + BuildConfig.VERSION_NAME + " " + BuildConfig.BUILD_TYPE + " (" + BuildConfig.APPLICATION_ID + ")"

        btn_character.setOnClickListener {
            if (!et_character.text.toString().equals("")) {
                search(et_character.text.toString())
            } else {
                loop = (loop + 1) % s.length
                search(s.get(loop).toString())
            }
        }

        btn_character2.setOnClickListener {
            loop = (loop + s.length - 1) % s.length
            search(s.get(loop).toString())
        }

        btn_auto.setOnClickListener {
            autoDraw = !autoDraw
            mChineseCharacterView.setAutoDraw(autoDraw)
            mChineseCharacterView.redraw(false)
            btn_auto.text = "自动绘制$autoDraw"
        }
        btn_median.setOnClickListener {
            showMedian = !showMedian
            mChineseCharacterView.setShowMedian(showMedian)
            mChineseCharacterView.redraw(false)
            btn_median.text = "显示中线$showMedian"
        }
        btn_auto.text = "自动绘制$autoDraw"
        btn_median.text = "显示中线$showMedian"
    }

    private fun search(text: String) {
        var searchchar = text;
        if (text.length > 1) {
            //Toast.makeText(this, "只能查询单个字", Toast.LENGTH_SHORT).show()
            loop = loop % text.length
            searchchar = text.get(loop).toString()
            loop++
        }
        try {
            val bean = FontStrokeUtil.getInstance().query(searchchar)
            PinyinHelper.addPinyinDict("/sdcard/user.dict")        //添加拥护自定义字典
            PinyinHelper.addMutilPinyinDict("/sdcard/m_user.dict") //添加拥护自定义字典
            val pinyin = PinyinHelper.convertToPinyinString(searchchar, ",",  PinyinFormat.WITH_TONE_MARK)
            mChineseCharacterView.setStrokeInfo(bean.strokes).setMedianPaths(bean.medians)
            mChineseCharacterView.setPinyin(pinyin)
            mChineseCharacterView.redraw(true)
        } catch (e: java.lang.Exception) {
            Toast.makeText(this, "不支持($searchchar)", Toast.LENGTH_SHORT).show()
        }
    }
}
