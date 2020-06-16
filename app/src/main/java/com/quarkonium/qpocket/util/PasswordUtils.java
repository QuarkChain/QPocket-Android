package com.quarkonium.qpocket.util;

import com.nulabinc.zxcvbn.Strength;
import com.nulabinc.zxcvbn.Zxcvbn;
import com.quarkonium.qpocket.view.PasswordLevelView;

public class PasswordUtils {

    /**
     * 获得密码强度等级，包括简单、复杂、强
     */
    public static PasswordLevelView.Level getPasswordLevel(String passWD) {
        Zxcvbn zxcvbn = new Zxcvbn();
        int code = -1;
        try {
            Strength strength = zxcvbn.measure(passWD);
            code = strength.getScore();
        } catch (Exception e) {
            return PasswordLevelView.Level.LOW;
        }
        //# Integer from 0-4 (useful for implementing a strength bar)
        //# 0 Weak        （guesses < ^ 3 10）
        //# 1 Fair        （guesses <^ 6 10）
        //# 2 Good        （guesses <^ 8 10）
        //# 3 Strong      （guesses < 10 ^ 10）
        //# 4 Very strong （guesses >= 10 ^ 10）
        switch (code) {
            case 1:
                return PasswordLevelView.Level.DANGER;
            case 2:
                return PasswordLevelView.Level.LOW;
            case 3:
                return PasswordLevelView.Level.MID;
            case 4:
                return PasswordLevelView.Level.STRONG;
        }
        return PasswordLevelView.Level.DANGER;
    }
}
