package org.coolreader.crengine;

public class UserDicEntry {

    public static int ACTION_NEW = 0;
    public static int ACTION_UPDATE_CNT = 1;
    public static int ACTION_DELETE = 2;

    public UserDicEntry()
    {
    }

    private Long id;
    private String dic_word;
    private String dic_word_translate;
    private String dic_from_book;
    private long create_time = System.currentTimeMillis(); // UTC timestamp
    private long last_access_time = System.currentTimeMillis(); // UTC timestamp
    private String language;
    private Long seen_count;
    private int is_citation;

    public int getIs_citation() {
        return is_citation;
    }

    public void setIs_citation(int is_citation) {
        this.is_citation = is_citation;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDic_word() {
        return dic_word;
    }

    public void setDic_word(String dic_word) {
        this.dic_word = dic_word;
    }

    public String getDic_word_translate() {
        return dic_word_translate;
    }

    public void setDic_word_translate(String dic_word_translate) {
        this.dic_word_translate = dic_word_translate;
    }

    public String getDic_from_book() {
        return dic_from_book;
    }

    public void setDic_from_book(String dic_from_book) {
        this.dic_from_book = dic_from_book;
    }

    public long getCreate_time() {
        return create_time;
    }

    public void setCreate_time(long create_time) {
        this.create_time = create_time;
    }

    public long getLast_access_time() {
        return last_access_time;
    }

    public void setLast_access_time(long last_access_time) {
        this.last_access_time = last_access_time;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Long getSeen_count() {
        return seen_count;
    }

    public void setSeen_count(Long seen_count) {
        this.seen_count = seen_count;
    }

}
