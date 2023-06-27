package hcmute.edu.vn.thanh0456.zaloclone.models;

import java.util.ArrayList;


public class UserStory {


    public String name, image;

    public long lastUpdated;

    public ArrayList<Story> stories;

    public UserStory(String name, String image, long lastUpdated, ArrayList<Story> stories) {
        this.name = name;
        this.image = image;
        this.lastUpdated = lastUpdated;
        this.stories = stories;
    }

    public UserStory() {

    }


}
