package com.progsoft.dictionary.jpinyin;

import com.progsoft.dictionary.AppContext;
import com.progsoft.dictionary.R;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 资源文件加载类
 *
 * @author stuxuhai (dczxxuhai@gmail.com)
 */
public final class PinyinResource {

    private PinyinResource() {
    }

    protected static Reader newClassPathReader(String classpath) {
        InputStream is = PinyinResource.class.getResourceAsStream(classpath);
        if (is == null)
            return null;
        try {
            return new InputStreamReader(is, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    protected static Reader newClassPathReader(int resId) {
        InputStream is = AppContext.context.getResources().openRawResource(resId);
        if (is == null)
            return null;
        try {
            return new InputStreamReader(is, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    protected static Reader newFileReader(String path) throws FileNotFoundException {
        try {
            return new InputStreamReader(new FileInputStream(path), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    protected static Map<String, String> getResource(Reader reader) {
        Map<String, String> map = new ConcurrentHashMap<String, String>();
        if (reader == null)
            return map;
        try {
            BufferedReader br = new BufferedReader(reader);
            String line = null;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.trim().split("=");
                map.put(tokens[0], tokens[1]);
            }
            br.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return map;
    }

    protected static Map<String, String> getPinyinResource() {
//        return getResource(newClassPathReader("/data/pinyin.dict"));
        return getResource(newClassPathReader(R.raw.pinyin));
    }

    protected static Map<String, String> getMutilPinyinResource() {
//        return getResource(newClassPathReader("/data/mutil_pinyin.dict"));
        return getResource(newClassPathReader(R.raw.mutil_pinyin));
    }

    protected static Map<String, String> getChineseResource() {
//        return getResource(newClassPathReader("/data/chinese.dict"));
        return getResource(newClassPathReader(R.raw.chinese));
    }
}
