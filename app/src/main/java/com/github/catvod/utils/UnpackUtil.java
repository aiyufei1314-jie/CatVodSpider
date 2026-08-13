package com.github.catvod.utils;

import com.whl.quickjs.wrapper.QuickJSContext;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UnpackUtil {
    public static String replaceReturnWithLastValue(String expression) {
        int returnIdx = expression.indexOf("return");
        if (returnIdx == -1) return expression;

        // 找到 return 对应的右大括号 }
        int braceDepth = 0;
        int start = returnIdx + 6;
        int end = -1;
        for (int i = start; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (c == '{') braceDepth++;
            else if (c == '}') {
                if (braceDepth == 0) { end = i; break; }
                braceDepth--;
            }
        }
        if (end == -1) return expression;

        String returnBody = expression.substring(start, end);

        // ----- 核心：从右往左找不在字符串里的最后一个冒号 -----
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        int lastColonPos = -1;

        for (int i = returnBody.length() - 1; i >= 0; i--) {
            char c = returnBody.charAt(i);

            // 处理转义字符（防止 \" 或 \' 被误判为字符串结束）
            if (i > 0 && returnBody.charAt(i - 1) == '\\') {
                continue; // 跳过转义字符
            }

            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (c == '\"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            } else if (c == ':' && !inSingleQuote && !inDoubleQuote) {
                lastColonPos = i;
                break;
            }
        }

        if (lastColonPos == -1) return expression; // 没找到冒号

        // 截取最后一部分（去掉可能的分号）
        String lastPart = returnBody.substring(lastColonPos + 1).trim();
        if (lastPart.endsWith(";")) {
            lastPart = lastPart.substring(0, lastPart.length() - 1);
        }

        // 替换拼接
        String newSegment = "return " + lastPart;
        return expression.substring(0, returnIdx) + newSegment + expression.substring(end);
    }

    public static String unpack(String script) {
        if (script == null) return null;
        String expression = script;

        int evalIndex = expression.indexOf("eval(function");
        if (evalIndex != -1) {
            expression = expression.substring(evalIndex + 4);
        }

        String result = evaluate(expression);
        if (result != null && !result.isEmpty()) {
            return result;
        }

        result = evaluate(replaceReturnWithLastValue(expression));
        if (result != null && !result.isEmpty()) {
            return result;
        }

        result = evaluate(replaceLegacyReturn(expression));
        if (result != null && !result.isEmpty()) {
            return result;
        }

        return null;
    }

    private static String replaceLegacyReturn(String expression) {
        Pattern p6 = Pattern.compile("\\)\\s?\\{\\s?return\\s?\\(([^}]*)\\)\\s?\\}");
        Matcher m6 = p6.matcher(expression);
        if (m6.find()) {
            String inner = m6.group(1);
            int lastColon = inner.lastIndexOf(':');
            if (lastColon != -1) {
                String lastPart = inner.substring(lastColon + 1);
                String replacement = "){return(" + lastPart + ")}";
                return m6.replaceFirst(Matcher.quoteReplacement(replacement));
            }
        }
        return expression;
    }

    private static String evaluate(String expression) {
        QuickJSContext context = null;
        try {
            context = QuickJSContext.create();
            Object result = context.evaluate(expression);
            return result == null ? null : result.toString();
        } catch (Exception e) {
            return null;
        } finally {
            if (context != null) {
                try {
                    context.destroy();
                } catch (Exception ignored) {
                }
            }
        }
    }

}