package com.lint.plus;

import java.util.Arrays;

public class EscapedText {

	/** 
	  * 替换一个字符串中的某些指定字符 
	  * @param strData String 原始字符串 
	  * @param regex String 要替换的字符串 
	  * @param replacement String 替代字符串 
	  * @return String 替换后的字符串 
	  */
	public static String replaceString(String strData, String regex, String replacement) {
		if (strData == null) {
			return null;
		}
		int index;
		index = strData.indexOf(regex);
		String strNew = "";
		if (index >= 0) {
			while (index >= 0) {
				strNew += strData.substring(0, index) + replacement;
				strData = strData.substring(index + regex.length());
				index = strData.indexOf(regex);
			}
			strNew += strData;
			return strNew;
		}
		return strData;
	}

	/** 
	 * 替换字符串中特殊字符 
	 */
	public static String encodeString(String strData)  
	 {  
	     if (strData == null)  
	     {  
	         return "";  
	     }  
	     strData = replaceString(strData, "&", "&");  
	     strData = replaceString(strData, "<", "<");  
	     strData = replaceString(strData, ">", ">");  
	     strData = replaceString(strData, "'", "'");  
	     strData = replaceString(strData, "\"", "\"");
	     return strData;  
	 }

	/** 
	 * 还原字符串中特殊字符 
	 */
	public static String decodeString(String strData)  
	 {  
	     strData = replaceString(strData, "<", "<");  
	     strData = replaceString(strData, ">", ">");  
	     strData = replaceString(strData, "'", "'");  
	     strData = replaceString(strData, "\"", "\"");  
	     strData = replaceString(strData, "&", "&");  
	     return strData;  
	 }
	
	 /**
     * 检查 text 是否 符合服务器数据格式
     *
     * @param text
     * @return
     */
    public static boolean isTextAvailable(String text) {
        if (null == text) return false;
        if (text.contains("-")) return false;
        if (text.contains("\n")) return false;
        if (text.contains("\r")) return false;
        return true;
    }

    /**
     * 特殊字符集
     */
    private static Character[] regEx = {'·', '`', '~', '!', '@', '#', '$', '%',
            '^', '&', '*', '(', ')', '+', '=', '|', '\\', '{', '}', '【',
            '】', '\'', '"', '“', '”', ':', ';', ',', '<', '‘', '’', '[',
            ']', '.', '>', '/', '?', '（', '）', '；', '。', '、', '？',
            '—', ':', '￥', '！', '，', '…'};

    /**
     * 转义 特殊字符
     *
     * @param str
     * @return
     */
    public static String translateString(String str) {
        String temp = str;
        int count = 0;
        for ( int i = 0; i < str.length(); i++ ) {
            if (Arrays.asList(regEx).contains(str.charAt(i))) {
                temp = temp.substring(0, i + count) + "\\" + temp.substring(i + count);
                count++;
            }
        }
        return temp;
    }
}
