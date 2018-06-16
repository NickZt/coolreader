package org.coolreader.crengine;

public class StrUtils {

    public static boolean isEmptyStr(String s) {
        return s == null || s.trim().length() == 0;
    }

    public static String updateTextPre(String s, boolean bOneLine) {
        String str = s;
        if (str != null) {
            if (str.length() > 2) {
                if ((str.startsWith("\"")) && (str.endsWith("\""))) {
                    str = str.substring(1, str.length() - 1);
                }
                if ((str.startsWith("(")) && (str.endsWith(")"))) {
                    str = str.substring(1, str.length() - 1);
                }
                if ((str.startsWith("[")) && (str.endsWith("]"))) {
                    str = str.substring(1, str.length() - 1);
                }
                if ((str.startsWith("{")) && (str.endsWith("}"))) {
                    str = str.substring(1, str.length() - 1);
                }
                if ((str.startsWith("<")) && (str.endsWith(">"))) {
                    str = str.substring(1, str.length() - 1);
                }
                if ((str.startsWith("'")) && (str.endsWith("'"))) {
                    str = str.substring(1, str.length() - 1);
                }
                boolean bOneQuote = false;
                if (
                        (str.replace("\"","").length()) == (str.length() - 1)
                   )
                    bOneQuote = true;
                if (
                        (str.replace("'","").length()) == (str.length() - 1)
                   )
                    bOneQuote = true;
                if (
                        (str.startsWith(",")) ||
                                (str.startsWith(".")) ||
                                (str.startsWith(":")) ||
                                (str.startsWith(";")) ||
                                (str.startsWith("/")) ||
                                (str.startsWith("\\")) ||
                                ((str.startsWith("\""))&&(bOneQuote)) ||
                                ((str.startsWith("'"))&&(bOneQuote)) ||
                                (str.startsWith("\n")) ||
                                (str.startsWith("\r")) ||
                                (str.startsWith("-"))
                        ) {
                    str = str.substring(1, str.length());
                }
                if (
                        (str.endsWith(",")) ||
                                (str.endsWith(".")) ||
                                (str.endsWith(":")) ||
                                (str.endsWith(";")) ||
                                (str.endsWith("/")) ||
                                (str.endsWith("\\")) ||
                                ((str.endsWith("\""))&&(bOneQuote)) ||
                                ((str.endsWith("'"))&&(bOneQuote)) ||
                                (str.endsWith("!")) ||
                                (str.endsWith("?")) ||
                                (str.endsWith("\n")) ||
                                (str.endsWith("\r")) ||
                                (str.endsWith("-"))
                        ) {
                    str = str.substring(0, str.length() - 1);
                }
            }
        }
        while (str.contains("  ")) {
            str = str.replaceAll("  ", " ");
        }
        while (str.contains("\n\n")) {
            str = str.replaceAll("\n\n", "\n");
        }
        while (str.contains("\r\r")) {
            str = str.replaceAll("\r\r", "\r");
        }
        while (str.contains("\r\n\r\n")) {
            str = str.replaceAll("\r\n\r\n", "\r\n");
        }
        if (bOneLine) {
            str = str.replaceAll("\r\n","; ");
            str = str.replaceAll("\r","; ");
            str = str.replaceAll("\n","; ");
        }
        while (str.contains("; ;")) {
            str = str.replaceAll("; ;", ";");
        }
        return str.trim();
    }

    public static String textShrinkLines(String s, boolean bOneLine) {
        String str = s;
        while (str.matches("  ")) {
            str = str.replaceAll("  ", " ");
        }
        while (str.matches("\n\n")) {
            str = str.replaceAll("\n\n", "\n");
        }
        while (str.matches("\r\r")) {
            str = str.replaceAll("\r\r", "\r");
        }
        while (str.matches("\r\n\r\n")) {
            str = str.replaceAll("\r\n\r\n", "\r\n");
        }
        if (bOneLine) {
            str = str.replaceAll("\r\n","; ");
            str = str.replaceAll("\r","; ");
            str = str.replaceAll("\n","; ");
        }
        while (str.contains("; ;")) {
            str = str.replaceAll("; ;", ";");
        }
        return str.trim();
    }

    public static String updateText(String s, boolean bOneLine) {
        String str = ""; String str2 = s;
        while (!str.equals(str2)) {
            str = str2;
            str2 = updateTextPre(str2, bOneLine);
        }
        return str2.trim();
    }

    public static String dictWordCorrection(String s){
        String str = s;
        if(str != null){
            if (str.length()>2) {
                if (
                        ((str.substring(1,2).equals("'")) || (str.substring(1,2).equals("’")))
                                &&
                                (!str.toLowerCase().substring(0,1).equals("i"))
                        ) {
                    str = str.substring(2,str.length());
                }
            }
        }
        return str;
    }

    public static String replacePuncts(String str, boolean bAddSpace) {
        String s = str;
        if (bAddSpace) {
            s = s
                    .replace(",",", ")
                    .replace(".",". ")
                    .replace(";","; ")
                    .replace(":",": ")
                    .replace("-","- ")
                    .replace("/","/ ")
                    .replace("?","? ")
                    .replace("!","! ")
                    .replace("\\","\\ ");
        } else {
            s = s.replace(",","")
                    .replace(".","")
                    .replace(";","")
                    .replace(":","")
                    .replace("-","")
                    .replace("!","")
                    .replace("?","")
                    .replace("/","")
                    .replace("\\","");
        }
        return s;
    }

    public static String updateDictSelText(String str) {
        String s = replacePuncts(str, true);
        String res = "";
        String [] arrS = s.split(" ");
        for (String ss: arrS) {
            String repl = ss.trim().toLowerCase()
                    .replace("me","smb")
                    .replace("you","smb")
                    .replace("him","smb")
                    .replace("her","smb")
                    .replace("thee","smb")
                    .replace("they","smb")
                    .replace("we","smb")
                    .replace("it","smb")
                    .replace("us","smb")
                    .replace("them","smb")
                    .replace("my","smb's")
                    .replace("mine","smb's")
                    .replace("yours","smb's")
                    .replace("your","smb's")
                    .replace("thy","smb's")
                    .replace("his","smb's")
                    .replace("its","smb's")
                    .replace("our","smb's")
                    .replace("ours","smb's")
                    .replace("their","smb's")
                    .replace("theirs","smb's")
                    .replace("hers","smb's");
            if (repl.equals("smb")||repl.equals("smb's")||
                    repl.equals("smb.")||repl.equals("smb's.")||
                    repl.equals("smb!")||repl.equals("smb's!")||
                    repl.equals("smb?")||repl.equals("smb's?")||
                    repl.equals("smb,")||repl.equals("smb's,")||
                    repl.equals("smb:")||repl.equals("smb's:")||
                    repl.equals("smb;")||repl.equals("smb's;")||
                    repl.equals("smb;")||repl.equals("smb's;")||
                    repl.equals("smb/")||repl.equals("smb's/")||
                    repl.equals("smb\\")||repl.equals("smb's\\")) res=res+" "+repl; else res=res.trim()+" "+ss.trim();
        }
        return res;
    }

}