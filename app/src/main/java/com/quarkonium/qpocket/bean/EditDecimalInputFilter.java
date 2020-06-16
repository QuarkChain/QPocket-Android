package com.quarkonium.qpocket.bean;

import android.text.InputFilter;
import android.text.Spanned;

import com.quarkonium.qpocket.api.Constant;

public class EditDecimalInputFilter implements InputFilter {
    /**
     * source    新输入的字符串
     * start    新输入的字符串起始下标，一般为0
     * end    新输入的字符串终点下标，一般为source长度-1
     * dest    输入之前文本框内容
     * dstart    原内容起始坐标，一般为0
     * dend    原内容终点坐标，一般为dest长度-1
     */
    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        // 删除等特殊字符，直接返回
        if (source == null || "".equals(source.toString().trim())) {
            return null;
        }
        String dValue = dest.toString();
        String[] splitArray;
        if (dValue.startsWith(",")) {
            splitArray = dValue.split(",");
        } else {
            splitArray = dValue.split("\\.");
        }
        if (splitArray.length > 1) {
            String dotValue = splitArray[1];
            int diff = dotValue.length() + 1 - Constant.QKC_DECIMAL_NUMBER;//4表示输入框的小数位数
            if (diff > 0) {
                return source.subSequence(start, end - diff);
            }
        }
        return null;
    }
}
