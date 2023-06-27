package hcmute.edu.vn.thanh0456.zaloclone.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Date;


public class Group implements Parcelable {
    public String id, ownerId, ownerName, image, name, dateTime;
    public Date dateObject;
    public ArrayList<User> members;
    public GroupLastMessageModel lastMessageModel;

    public Group() {

    }

    protected Group(Parcel in) {
        id = in.readString();
        ownerId = in.readString();
        ownerName = in.readString();
        image = in.readString();
        name = in.readString();
        dateTime = in.readString();
        dateObject = new Date(in.readLong());
        members = in.createTypedArrayList(User.CREATOR);
        lastMessageModel = in.readParcelable(GroupLastMessageModel.class.getClassLoader());
    }

    public static final Creator<Group> CREATOR = new Creator<Group>() {
        @Override
        public Group createFromParcel(Parcel in) {
            return new Group(in);
        }

        @Override
        public Group[] newArray(int size) {
            return new Group[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(id);
        parcel.writeString(ownerId);
        parcel.writeString(ownerName);
        parcel.writeString(image);
        parcel.writeString(name);
        parcel.writeString(dateTime);
        parcel.writeLong(dateObject.getTime());
        parcel.writeTypedList(members);
        parcel.writeParcelable(lastMessageModel, i);
    }
}
