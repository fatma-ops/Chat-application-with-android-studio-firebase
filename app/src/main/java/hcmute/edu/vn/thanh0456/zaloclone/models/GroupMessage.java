package hcmute.edu.vn.thanh0456.zaloclone.models;

import android.graphics.Bitmap;

import java.util.Date;


public class GroupMessage {
    public String type, message, dateTime, senderId, image;
    public Bitmap imageBitmap;
    public Date dateObject;

    public GroupMessage() {

    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
