package com.alibaba.tesla.tkgone.server.common;

import java.util.Stack;

/**
 * @author yangjinghua
 */
public class ExpressionToLong {

    private static boolean isNumber(char ch) {
        return ch == '.' || (ch >= '0' && ch <= '9');
    }

    private static void hasAddOrSub(Stack<Character> s, char opration) {
        StringBuilder number = new StringBuilder();
        while (true) {
            char cur = s.peek();
            if (cur == opration) { break; }

            number.insert(0, cur);
            s.pop();
        }

        s.pop();
        Long right = Long.parseLong(number.toString());
        number.replace(0, number.length(), "");

        while (!s.isEmpty()) {
            char cur = s.peek();
            number.insert(0, cur);
            s.pop();
        }

        Long left = Long.parseLong(number.toString());
        number.replace(0, number.length(), "");

        Long res;
        if (opration == '+') {
            res = left + right;
        } else {
            res = left - right;
        }
        String str = res + "";
        for (char chr : str.toCharArray()) {
            s.push(chr);
        }
    }

    private static void hasMulOrDiv(Stack<Character> s, char opration) {
        StringBuilder number = new StringBuilder();
        while (true) {
            char cur = s.peek();
            if (cur == opration) { break; }

            number.insert(0, cur);
            s.pop();
        }

        s.pop();
        Long right = Long.parseLong(number.toString());
        number.replace(0, number.length(), "");

        while (!s.isEmpty()) {
            char cur = s.peek();
            if (cur == '+' || cur == '-') {
                break;
            }
            number.insert(0, cur);
            s.pop();
        }

        Long left = Long.parseLong(number.toString());
        number.replace(0, number.length(), "");
        Long res;
        if (opration == '*') {
            res = left * right;
        } else {
            res = left / right;
        }

        String str = res + "";
        for (char chr : str.toCharArray()) {
            s.push(chr);
        }
    }

    private static boolean checkInfo(char[] datas) {
        for (int i = 0; i < datas.length - 1; i++) {
            if (!isNumber(datas[i]) && !isNumber(datas[i + 1])) {
                return false;
            }
            if (datas[i] == '.' && datas[i + 1] == '.') {
                return false;
            }
        }
        return true;
    }

    public static String stringToRes(String info) {
        if (!info.endsWith("=")) {
            info += "=";
        }
        Stack<Character> s = new Stack<>();
        char[] datas = info.toCharArray();
        if (!checkInfo(datas)) {
            return "";
        }
        boolean hasAdd = false;
        boolean hasSub = false;
        boolean hasMul = false;
        boolean hasDiv = false;

        for (char ch : datas) {
            if (isNumber(ch)) {
                s.push(ch);
            } else {
                switch (ch) {
                    case '+':
                        if (hasMul) {
                            hasMulOrDiv(s, '*');
                            hasMul = false;
                        }

                        if (hasDiv) {
                            hasMulOrDiv(s, '/');
                            hasDiv = false;
                        }

                        if (hasAdd) {
                            hasAddOrSub(s, '+');
                            hasAdd = false;
                        }

                        if (hasSub) {
                            hasAddOrSub(s, '-');
                            hasSub = false;
                        }

                        s.push(ch);
                        hasAdd = true;
                        break;
                    case '-':
                        if (hasMul) {
                            hasMulOrDiv(s, '*');
                            hasMul = false;
                        }

                        if (hasDiv) {
                            hasMulOrDiv(s, '/');
                            hasDiv = false;
                        }

                        if (hasAdd) {
                            hasAddOrSub(s, '+');
                            hasAdd = false;
                        }

                        if (hasSub) {
                            hasAddOrSub(s, '-');
                            hasSub = false;
                        }

                        s.push(ch);
                        hasSub = true;
                        break;
                    case '*':
                        if (hasMul) {
                            hasMulOrDiv(s, '*');
                            hasMul = false;
                        }

                        if (hasDiv) {
                            hasMulOrDiv(s, '/');
                            hasDiv = false;
                        }
                        s.push(ch);
                        hasMul = true;
                        break;
                    case '/':
                        if (hasMul) {
                            hasMulOrDiv(s, '*');
                            hasMul = false;
                        }

                        if (hasDiv) {
                            hasMulOrDiv(s, '/');
                            hasDiv = false;
                        }
                        s.push(ch);
                        hasDiv = true;
                        break;
                    case '=':
                        if (hasMul) {
                            hasMulOrDiv(s, '*');
                            hasMul = false;
                        }

                        if (hasDiv) {
                            hasMulOrDiv(s, '/');
                            hasDiv = false;
                        }

                        if (hasAdd) {
                            hasAddOrSub(s, '+');
                            hasAdd = false;
                        }

                        if (hasSub) {
                            hasAddOrSub(s, '-');
                            hasSub = false;
                        }

                        StringBuilder number = new StringBuilder();
                        while (!s.isEmpty()) {
                            char cur = s.peek();
                            number.insert(0, cur);
                            s.pop();
                        }
                        return number.toString();
                    default:
                        break;
                }
            }
        }
        return "";
    }
}
