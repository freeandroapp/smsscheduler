/**
 * Copyright (c) 2012 Vinayak Solutions Private Limited 
 * See the file license.txt for copying permission.
 */

package com.freeandroapp.smsscheduler.models;

import java.util.ArrayList;
import android.os.Parcel;
import android.os.Parcelable;

public class Sms implements Parcelable {

    public long keyId;
    public String keyNumber;
    public String keyMessage;
    public int keyMessageParts;
    public long keyTimeMilis;
    public String keyDate;
    public int keyImageRes;
    public String keyExtraReceivers;
    public ArrayList<Recipient> keyRecipients;
    public int keyRepeatMode;
    public String keyRepeatString;

    public Sms(long keyid, String keynumber, String keymessage, int keymessageparts, long keytimemilis, String keydate, ArrayList<Recipient> keyrecipients, int keyrepeatmode, String keyrepeatstring) {
        this.keyId = keyid;
        this.keyNumber = keynumber;
        this.keyMessage = keymessage;
        this.keyMessageParts = keymessageparts;
        this.keyDate = keydate;
        this.keyTimeMilis = keytimemilis;
        this.keyRecipients = keyrecipients;
        this.keyRepeatMode = keyrepeatmode;
        this.keyRepeatString = keyrepeatstring;
    }

    public Sms() {
    }

    public static final Parcelable.Creator<Sms> CREATOR = new Parcelable.Creator<Sms>() {
        public Sms createFromParcel(Parcel in) {
            return new Sms(in);
        }

        public Sms[] newArray(int size) {
            return new Sms[size];
        }
    };

    public Sms(Parcel in) {
        this.keyId = in.readLong();
        this.keyNumber = in.readString();
        this.keyMessage = in.readString();
        this.keyMessageParts = in.readInt();
        this.keyTimeMilis = in.readLong();
        keyRecipients = new ArrayList<Recipient>();
        in.readList(keyRecipients, Recipient.class.getClassLoader());
        this.keyRepeatMode = in.readInt();
        this.keyRepeatString = in.readString();
    };

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.keyId);
        dest.writeString(this.keyNumber);
        dest.writeString(this.keyMessage);
        dest.writeInt(this.keyMessageParts);
        dest.writeLong(this.keyTimeMilis);
        dest.writeList(this.keyRecipients);
        dest.writeInt(this.keyRepeatMode);
        dest.writeString(this.keyRepeatString);
    }

    public int describeContents() {
        return 0;
    }

    public void printRecipients() {
        for (int i = 0; i < keyRecipients.size(); i++) {
            keyRecipients.get(i).print();
        }
    }
}
