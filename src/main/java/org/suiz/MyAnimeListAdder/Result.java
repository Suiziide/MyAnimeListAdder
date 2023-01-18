package org.suiz.MyAnimeListAdder;

import java.util.HashMap;

public class Result {
    private int id;
    private String title;
    private HashMap<String,String> main_picture;
    private HashMap<String,String> alternative_titles;
    private int popularity;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public HashMap<String, String> getMain_picture() {
        return main_picture;
    }

    public void setMain_picture(HashMap<String, String> main_picture) {
        this.main_picture = main_picture;
    }

    public HashMap<String, String> getAlternative_titles() {
        return alternative_titles;
    }

    public void setAlternative_titles(HashMap<String, String> alternative_titles) {
        this.alternative_titles = alternative_titles;
    }

    public int getPopularity() {
        return popularity;
    }

    public void setPopularity(int popularity) {
        this.popularity = popularity;
    }

    @Override
    public String toString() {
        return "Id: " + id + ", Title: " + title + ", Popularity: " + popularity
                + "\nPictures: " + main_picture.toString() + ", Alternative Titles: " + alternative_titles.toString();

    }
}
