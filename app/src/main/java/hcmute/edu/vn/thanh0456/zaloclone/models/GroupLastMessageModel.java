package hcmute.edu.vn.thanh0456.zaloclone.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;



public class GroupLastMessageModel implements Parcelable {
    public String senderId, message, type, dateTime;
    public Date dateObject;
    public GroupLastMessageModel() {
    }

    protected GroupLastMessageModel(Parcel in) {
        senderId = in.readString();
        message = in.readString();
        type = in.readString();
        dateTime = in.readString();
        dateObject = new Date(in.readLong());
    }

    public static final Creator<GroupLastMessageModel> CREATOR = new Creator<GroupLastMessageModel>() {
        @Override
        public GroupLastMessageModel createFromParcel(Parcel in) {
            return new GroupLastMessageModel(in);
        }

        @Override
        public GroupLastMessageModel[] newArray(int size) {
            return new GroupLastMessageModel[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(senderId);
        parcel.writeString(message);
        parcel.writeString(type);
        parcel.writeString(dateTime);
        parcel.writeLong(dateObject.getTime());
    }
}
