/**
 * Copyright (c) 2012 Vinayak Solutions Private Limited 
 * See the file license.txt for copying permission.
 */

package com.freeandroapp.smsscheduler;

import java.util.ArrayList;
import java.util.Random;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.ContactsContract.CommonDataKinds.Phone;

import com.freeandroapp.smsscheduler.models.Recipient;
import com.freeandroapp.smsscheduler.models.Sms;
import com.freeandroapp.smsscheduler.receivers.SMSHandleReceiver;
import com.freeandroapp.smsscheduler.utils.Log;

public class DBAdapter {

    private final String DATABASE_NAME = "smsDatabase";
    private final String DATABASE_SMS_TABLE = "smsTable";
    private final String DATABASE_RECIPIENT_TABLE = "recipientTable";
    private final String DATABASE_PI_TABLE = "piTable";
    private final String DATABASE_TEMPLATE_TABLE = "templateTable";
    private final String DATABASE_GROUP_TABLE = "groupTable";
    private final String DATABASE_GROUP_CONTACT_RELATION = "groupContactRelation";
    private final String DATABASE_RECIPIENT_GROUP_REL_TABLE = "recipient_grp_rel_table";
    private final String DATABASE_RECENTS_TABLE = "recents_table";
    private final int DATABASE_VERSION = 4;

    Cursor cur;

    // ---------------------------static keys for
    // columns---------------------------------------

    // ----------------keys for SMS table--------------------------
    public static final String KEY_ID = "_id";
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_DATE = "date";
    public static final String KEY_TIME_MILLIS = "time_millis";
    public static final String KEY_MSG_PARTS = "msg_parts";
    public static final String KEY_STATUS = "status";
    public static final String KEY_REPEAT_MODE = "repeat_status";
    public static final String KEY_REPEAT_STRING = "repeat_string";

    // ------------------keys for Recipients table-----------------------
    public static final String KEY_RECIPIENT_ID = "recipient_id";
    public static final String KEY_NUMBER = "number";
    public static final String KEY_SENT = "sent";
    public static final String KEY_DELIVER = "deliver";
    public static final String KEY_OPERATED = "operated";
    public static final String KEY_S_MILLIS = "sent_milis";
    public static final String KEY_D_MILLIS = "deliver_milis";
    public static final String KEY_DISPLAY_NAME = "display_name";
    public static final String KEY_CONTACT_ID = "contact_id";
    public static final String KEY_RECIPIENT_TYPE = "recipient_type";

    // --------------keys for PI table---------------------------
    public static final String KEY_PI_ID = "_id";
    public static final String KEY_PI_NUMBER = "pi_number";
    public static final String KEY_SMS_ID = "sms_id";
    public static final String KEY_TIME = "time";

    // ----------------keys for template table---------------------
    public static final String KEY_TEMP_ID = "_id";
    public static final String KEY_TEMP_CONTENT = "content";

    // -----------------keys for group table-----------------------
    public static final String KEY_GROUP_ID = "_id";
    public static final String KEY_GROUP_NAME = "group_name";

    // ----------------keys for group contacts relation----------------
    public static final String KEY_RELATION_ID = "_id";
    public static final String KEY_GROUP_REL_ID = "group_rel_id";
    public static final String KEY_CONTACTS_ID = "contacts_id";
    public static final String KEY_CONTACTS_NUMBER = "contacts_number";

    // -------------------keys for recipient-groupId
    // relation--------------------
    public static final String KEY_RECIPIENT_GRP_REL_ID = "_id";
    public static final String KEY_RECIPIENT_GRP_REL_RECIPIENT_ID = "recipient_grp_rel_recipient_id";
    public static final String KEY_RECIPIENT_GRP_REL_GRP_ID = "recipient_grp_rel_grp_id";
    public static final String KEY_RECIPIENT_GRP_REL_GRP_TYPE = "recipient_grp_rel_grp_type";

    // ------------------keys for recents table-------------------------
    public static final String KEY_RECENT_CONTACT_ID = "_id";
    public static final String KEY_RECENT_CONTACT_CONTACT_ID = "contact_id";
    public static final String KEY_RECENT_CONTACT_NUMBER = "contact_number";

    // ------------------------------------------------------------------end of
    // static keys defs-------

    // SQL to open or create a database

    private final String DATABASE_CREATE_SMS_TABLE = "create table " + DATABASE_SMS_TABLE + " (" + KEY_ID + " integer primary key autoincrement, " + KEY_MESSAGE + " text, " + KEY_DATE + " text, " + KEY_TIME_MILLIS + " long, " + KEY_MSG_PARTS + " integer default 0, " + KEY_STATUS + " integer default 1, " + KEY_REPEAT_MODE + " integer, " + KEY_REPEAT_STRING + " text);";

    private final String DATABASE_CREATE_RECIPIENT_TABLE = "create table " + DATABASE_RECIPIENT_TABLE + " (" + KEY_RECIPIENT_ID + " integer primary key autoincrement, " + KEY_SMS_ID + " integer, " + KEY_NUMBER + " text not null, " + KEY_CONTACT_ID + " interger, " + KEY_SENT + " integer default 0, " + KEY_DELIVER + " integer default 0, " + KEY_S_MILLIS + " integer, " + KEY_D_MILLIS + " integer, " + KEY_OPERATED + " integer default 0, " + KEY_DISPLAY_NAME + " text, " + KEY_RECIPIENT_TYPE + " integer);";

    private final String DATABASE_CREATE_PI_TABLE = "create table " + DATABASE_PI_TABLE + " (" + KEY_PI_ID + " integer primary key, " + KEY_PI_NUMBER + " integer, " + KEY_SMS_ID + " integer, " + KEY_TIME + " integer);";

    private final String DATABASE_CREATE_TEMPLATE_TABLE = "create table " + DATABASE_TEMPLATE_TABLE + " (" + KEY_TEMP_ID + " integer primary key autoincrement, " + KEY_TEMP_CONTENT + " text);";

    private final String DATABASE_CREATE_GROUP_TABLE = "create table " + DATABASE_GROUP_TABLE + " (" + KEY_GROUP_ID + " integer primary key autoincrement, " + KEY_GROUP_NAME + " text);";

    private final String DATABASE_CREATE_GROUP_CONTACT_RELATION = "create table " + DATABASE_GROUP_CONTACT_RELATION + " (" + KEY_RELATION_ID + " integer primary key autoincrement, " + KEY_GROUP_REL_ID + " integer, " + KEY_CONTACTS_ID + " integer, " + KEY_CONTACTS_NUMBER + " text);";

    private final String DATABASE_CREATE_RECIPIENT_GROUP_REL_TABLE = "create table " + DATABASE_RECIPIENT_GROUP_REL_TABLE + " (" + KEY_RECIPIENT_GRP_REL_ID + " integer primary key autoincrement, " + KEY_RECIPIENT_GRP_REL_RECIPIENT_ID + " integer, " + KEY_RECIPIENT_GRP_REL_GRP_ID + " integer, " + KEY_RECIPIENT_GRP_REL_GRP_TYPE + " integer);";

    private final String DATABASE_CREATE_RECENTS_TABLE = "create table " + DATABASE_RECENTS_TABLE + " (" + KEY_RECENT_CONTACT_ID + " integer primary key autoincrement, " + KEY_RECENT_CONTACT_CONTACT_ID + " integer, " + KEY_RECENT_CONTACT_NUMBER + " text);";

    private SQLiteDatabase db;
    private final Context context;
    private MyOpenHelper myDbHelper;

    public DBAdapter(Context _context) {
        context = _context;
        myDbHelper = new MyOpenHelper(context);
    }

    public DBAdapter open() throws SQLException {
        db = myDbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        db.close();
    }

    // functions-----------------------------------------------------------------------------------------

    // ------------------------functions for SMS and Recipient
    // tables------------------------------

    /***
     * @detail gets the detail of a particular SMS that corresponds to the smsId
     *         passed
     * @param smsId
     * @return cursor containing detail of one SMS
     */
    public Cursor fetchSmsDetails(long smsId) {
        Cursor cur = db.query(DATABASE_SMS_TABLE, null, KEY_ID + "=" + smsId, null, null, null, null);
        return cur;
    }

    /***
     * @detail Tells apart if any SMS exist in database or not
     * @return Boolean: true if SMS count>0, else false
     */
    public boolean ifSmsExist() {
        Cursor cur = db.query(DATABASE_SMS_TABLE, null, null, null, null, null, null);
        boolean ifSmsExist = (cur.getCount() > 0);
        cur.close();
        return ifSmsExist;
    }

    /***
     * @detail Tells if all parts of all recipients of an SMS is sent or not
     * @param smsId
     * @return Boolean: true if all sent, else false
     */
    public boolean isSmsSent(long smsId) {
        Cursor cur = db.query(DATABASE_SMS_TABLE, new String[] { KEY_STATUS }, KEY_ID + "=" + smsId, null, null, null, null);
        cur.moveToFirst();
        boolean isSmsSent = (cur.getInt(cur.getColumnIndex(KEY_STATUS)) > Constants.SMS_STATUS_SCHEDULED);
        cur.close();
        return isSmsSent;
    }

    /***
     * @detail for a given smsId, it tells if the SMS is a draft or not
     * @param smsId
     * @return true if draft, else false;
     */
    public boolean isDraft(long smsId) {
        Cursor cur = db.query(DATABASE_SMS_TABLE, new String[] { KEY_STATUS }, KEY_ID + "=" + smsId, null, null, null, null);
        cur.moveToFirst();
        boolean isDraft = (cur.getInt(cur.getColumnIndex(KEY_STATUS)) == Constants.SMS_STATUS_DRAFT);
        cur.close();
        return isDraft;
    }

    /***
     * @detail Fetches details of all the recipients, including the detail of
     *         SMS that the recipient belongs to
     * @return Cursor containing detail of recipients
     */
    public Cursor fetchAllRecipientDetails() {
        String sql = "SELECT * FROM smsTable, recipientTable " + "WHERE smsTable._id=recipientTable.sms_id " + "ORDER BY smsTable.time_millis";

        Cursor cur = db.rawQuery(sql, null);
        return cur;
    }

    /***
     * @detail For a given recipientId, it returns the corresponding recipient's
     *         detail
     * @param recipientId
     * @return Cursor containing detail of the recipient
     */
    public Cursor fetchRecipientDetails(long recipientId) {
        String sql = "SELECT * FROM smsTable, recipientTable " + "WHERE smsTable._id=recipientTable.sms_id AND recipientTable.recipient_id =" + recipientId;

        Cursor cur = db.rawQuery(sql, null);
        return cur;
    }

    /***
     * @detail For a given smsId, returns the details of the recipients that
     *         belong to it.
     * @param smsId
     * @return cursor containing recipients detail
     */
    public Cursor fetchRecipientsForSms(long smsId) {
        String sql = "SELECT * FROM smsTable, recipientTable " + "WHERE smsTable._id=recipientTable.sms_id AND smsTable._id=" + smsId;

        Cursor cur = db.rawQuery(sql, null);
        return cur;
    }

    /***
     * @detail For a given smsId, fetches recipientIds for all the recipient
     *         that belongs to it
     * @param smsId
     * @return ArrayList<Long> recipientIds
     */
    private ArrayList<Long> fetchRecipientIdsForSms(long smsId) {
        String sql = "SELECT * FROM smsTable, recipientTable " + "WHERE smsTable._id=recipientTable.sms_id AND smsTable._id=" + smsId;

        ArrayList<Long> recipientIds = new ArrayList<Long>();

        Cursor cur = db.rawQuery(sql, null);
        if (cur.moveToFirst()) {
            do {
                recipientIds.add(cur.getLong(cur.getColumnIndex(KEY_RECIPIENT_ID)));
            } while (cur.moveToNext());
        }
        cur.close();
        return recipientIds;
    }

    /***
     * @detail Deletes a recipient from database
     * @param recipientId
     */
    public void deleteRecipient(long recipientId) {
        db.delete(DATABASE_RECIPIENT_TABLE, KEY_RECIPIENT_ID + "=" + recipientId, null);
        deleteRecipientGroupRelsForRecipient(recipientId);
    }

    /***
     * @detail Fetches details of the SMS that is to be scheduled the next.
     * @return Cursor containing the SMS detail. An empty cursor will mean
     *         "No more scheduled SMS"
     */
    public Cursor fetchNextScheduled() {
        String sql = "SELECT * FROM smsTable, recipientTable WHERE recipientTable.sms_id=smsTable._id AND recipientTable.recipient_id=" + "(SELECT recipientTable.recipient_id FROM recipientTable, smsTable " + "WHERE recipientTable.sms_id = smsTable._id AND recipientTable.operated=0 AND smsTable._id=" + "(SELECT smsTable._id FROM smsTable WHERE  smsTable.time_millis=" + "(SELECT MIN(smsTable.time_millis) FROM smsTable, recipientTable WHERE (smsTable.status=1 OR smsTable.status=3))))";

        Cursor cur = db.rawQuery(sql, null);
        return cur;
    }

    /***
     * @detail Schedules a new SMS using the details passed as parameters
     * @param message
     * @param date
     * @param parts
     * @param timeInMilis
     * @param repeatMode
     * @param repeatString
     * @return smsId for the newly scheduled SMS
     */
    public long scheduleSms(String message, String date, int parts, long timeInMilis, int repeatMode, String repeatString) {
        ContentValues addValues = new ContentValues();

        addValues.put(KEY_MESSAGE, message);
        addValues.put(KEY_DATE, date);
        addValues.put(KEY_TIME_MILLIS, timeInMilis);
        addValues.put(KEY_MSG_PARTS, parts);
        addValues.put(KEY_REPEAT_MODE, repeatMode);
        addValues.put(KEY_REPEAT_STRING, repeatString);

        return db.insert(DATABASE_SMS_TABLE, null, addValues);
    }

    /***
     * @detail Adds a recipient for the SMS corresponding to the smsId passed
     * @param smsId
     * @param number
     * @param displayName
     * @param type
     * @param contactId
     * @return recipientId of the newly created recipient
     */
    public long addRecipient(long smsId, String number, String displayName, int type, long contactId) {
        ContentValues addValues = new ContentValues();

        addValues.put(KEY_SMS_ID, smsId);
        addValues.put(KEY_NUMBER, number);
        addValues.put(KEY_DISPLAY_NAME, displayName);
        addValues.put(KEY_RECIPIENT_TYPE, type);
        addValues.put(KEY_SENT, 0);
        addValues.put(KEY_DELIVER, 0);
        addValues.put(KEY_S_MILLIS, -1);
        addValues.put(KEY_D_MILLIS, -1);
        addValues.put(KEY_CONTACT_ID, contactId);

        return db.insert(DATABASE_RECIPIENT_TABLE, null, addValues);
    }

    /***
     * @detail Sets an SMS as Draft
     * @param smsId
     */
    public void setAsDraft(long smsId) {
        setStatus(smsId, Constants.SMS_STATUS_DRAFT);
    }

    /***
     * @detail Sets a particular status for an SMS. Status is passed as
     *         parameter
     * @param smsId
     * @param status
     */
    public void setStatus(long smsId, int status) {
        ContentValues cv = new ContentValues();
        cv.put(KEY_STATUS, status);

        db.update(DATABASE_SMS_TABLE, cv, KEY_ID + "=" + smsId, null);
    }

    /***
     * @detail Set the Operated Flag for a Recipient, which denotes that the app
     *         has tried to send it.
     * @param recipientId
     */
    public void makeOperated(long recipientId) {
        ContentValues cv = new ContentValues();
        cv.put(KEY_OPERATED, Constants.RECIPIENT_OPERATED_FLAG_SET);
        db.update(DATABASE_RECIPIENT_TABLE, cv, KEY_RECIPIENT_ID + "=" + recipientId, null);
    }

    /***
     * @detail For a given smsId, removes sets repitition mode to "No Repeat"
     * @param smsId
     */
    public void removeRepitition(long smsId) {
        ContentValues cv = new ContentValues();
        cv.put(KEY_REPEAT_MODE, Constants.REPEAT_MODE_NO_REPEAT);
        cv.put(KEY_REPEAT_STRING, "");
        db.update(DATABASE_SMS_TABLE, cv, KEY_ID + "=" + smsId, null);
    }

    /***
     * @detail For a given recipientId, fetches how many parts of the message
     *         have been sent
     * @param recipientId
     * @return Number of msg parts sent
     */
    private int getSent(long recipientId) {
        Cursor cur = db.query(DATABASE_RECIPIENT_TABLE, new String[] { KEY_SENT }, KEY_RECIPIENT_ID + "=" + recipientId, null, null, null, null);
        if (cur.moveToFirst()) {
            int sent = cur.getInt(cur.getColumnIndex(KEY_SENT));
            cur.close();
            return sent;
        } else {
            cur.close();
            return 0;
        }
    }

    /***
     * @detail Corresponding to a particular pair of smsId and recipientId
     *         increases the "no. of sent parts" by one. Also checks if all the
     *         Msg parts have been sent or not, if so, then saves the sent time.
     * @param smsId
     * @param recipientId
     * @return Boolean: true if increased sent properly, else false
     */
    public boolean increaseSent(long smsId, long recipientId) {
        int sent = getSent(recipientId);
        ContentValues sentValue = new ContentValues();
        sentValue.put(KEY_SENT, ++sent);
        try {
            db.update(DATABASE_RECIPIENT_TABLE, sentValue, KEY_RECIPIENT_ID + "=" + recipientId, null);

            Cursor cur = db.query(DATABASE_SMS_TABLE, new String[] { KEY_MSG_PARTS }, KEY_ID + "=" + smsId, null, null, null, null);
            cur.moveToFirst();
            int parts = cur.getInt(cur.getColumnIndex(KEY_MSG_PARTS));
            if (sent == parts) {
                ContentValues sentTimeSaver = new ContentValues();
                sentTimeSaver.put(KEY_S_MILLIS, System.currentTimeMillis());
                db.update(DATABASE_RECIPIENT_TABLE, sentTimeSaver, KEY_RECIPIENT_ID + "=" + recipientId, null);
            }
            cur.close();
            return true;
        } catch (SQLiteException ex) {
            return false;
        }
    }

    /***
     * @detail For a given recipientId, fetches how many parts of the message
     *         have been delivered
     * @param recipientId
     * @return No. of Msg parts delivered
     */
    private int getDelivers(long recipientId) {
        Cursor cur = db.query(DATABASE_RECIPIENT_TABLE, new String[] { KEY_DELIVER }, KEY_RECIPIENT_ID + "=" + recipientId, null, null, null, null);
        if (cur.moveToFirst()) {
            int delivers = cur.getInt(cur.getColumnIndex(KEY_DELIVER));
            cur.close();
            return delivers;
        } else {
            cur.close();
            return 0;
        }
    }

    /***
     * @detail Corresponding to a particular pair of smsId and recipientId
     *         increases the "no. of delivered parts" by one. Also checks if all
     *         the Msg parts have been sent or not, if so, then saves the
     *         delivery time.
     * @param recipientId
     * @return Boolean: true if delivered parts properly increased by one, false
     *         if some issue cropped up
     */
    public boolean increaseDeliver(long recipientId) {
        int deliver = getDelivers(recipientId);
        ContentValues deliverValue = new ContentValues();
        deliverValue.put(KEY_DELIVER, deliver + 1);
        try {
            db.update(DATABASE_RECIPIENT_TABLE, deliverValue, KEY_RECIPIENT_ID + "=" + recipientId, null);
            if (checkDelivery(recipientId)) {
                ContentValues deliverTimeSaver = new ContentValues();
                deliverTimeSaver.put(KEY_D_MILLIS, System.currentTimeMillis());
                db.update(DATABASE_RECIPIENT_TABLE, deliverTimeSaver, KEY_RECIPIENT_ID + "=" + recipientId, null);
            }
            return true;
        } catch (SQLiteException ex) {
            return false;
        }
    }

    /***
     * @detail to check if all parts for a recipient have been sent and
     *         delivered or not
     * @param recipientId
     * @return true if all parts delivered, false otherwise.
     */
    public boolean checkDelivery(long recipientId) {
        Cursor cur = fetchRecipientDetails(recipientId);
        boolean bool = false;
        if (cur.moveToFirst())
            bool = ((cur.getInt(cur.getColumnIndex(KEY_DELIVER))) == (cur.getInt(cur.getColumnIndex(KEY_MSG_PARTS))));
        cur.close();
        return bool;
    }

    /***
     * @detail Deletes a particular SMS and all its recipients. Also, if a
     *         particular recipient is set as the next scheduled, this function
     *         will refresh the next Scheduled.
     * @param smsId
     * @param context
     */
    public void deleteSms(long smsId, Context context) {
        boolean isNextScheduledDeleted = false;
        ArrayList<Long> recipientIds = fetchRecipientIdsForSms(smsId);
        for (int i = 0; i < recipientIds.size(); i++) {
            if (getCurrentPiId() == recipientIds.get(i)) {
                isNextScheduledDeleted = true;
                Cursor cur = getPiDetails();
                cur.moveToFirst();

                Intent intent = new Intent(context, SMSHandleReceiver.class);
                intent.setAction(Constants.PRIVATE_SMS_ACTION);
                PendingIntent pi = PendingIntent.getBroadcast(context, cur.getInt(cur.getColumnIndex(KEY_PI_NUMBER)), intent, PendingIntent.FLAG_CANCEL_CURRENT);
                pi.cancel();
                cur.close();
            }
            deleteRecipient(recipientIds.get(i));
        }
        db.delete(DATABASE_SMS_TABLE, KEY_ID + "=" + smsId, null);

        if (isNextScheduledDeleted) {
            Cursor cur = fetchNextScheduled();
            if (cur.moveToFirst()) {
                Intent intent = new Intent(context, SMSHandleReceiver.class);
                intent.setAction(Constants.PRIVATE_SMS_ACTION);
                intent.putExtra("SMS_ID", cur.getLong(cur.getColumnIndex(KEY_ID)));
                intent.putExtra("RECIPIENT_ID", cur.getLong(cur.getColumnIndex(KEY_RECIPIENT_ID)));
                intent.putExtra("NUMBER", cur.getString(cur.getColumnIndex(KEY_NUMBER)));
                intent.putExtra("MESSAGE", cur.getString(cur.getColumnIndex(KEY_MESSAGE)));

                Random rand = new Random();
                int piNumber = rand.nextInt();
                PendingIntent pi = PendingIntent.getBroadcast(context, piNumber, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                updatePi(piNumber, cur.getLong(cur.getColumnIndex(KEY_RECIPIENT_ID)), cur.getLong(cur.getColumnIndex(KEY_TIME_MILLIS)));

                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                alarmManager.set(AlarmManager.RTC_WAKEUP, cur.getLong(cur.getColumnIndex(KEY_TIME_MILLIS)), pi);
            } else {
                updatePiForNoSmsValue();
            }
            cur.close();
        }
    }

    // --------------------------------------------------------end of functions
    // for SMS and Recipient tables---------------

    // --------------------------------------functions for Pending Intent Table
    // -----------------------------------------

    // Pending Intent table is used to store the detail of Pending Intent that
    // is used to hold the next scheduled recipient
    // that is to be operated.

    /***
     * @detail get the detail of Pending Intent (Pi).
     * @return Cursor
     */
    public Cursor getPiDetails() {
        return db.query(DATABASE_PI_TABLE, null, KEY_PI_ID + "= 1", null, null, null, null);
    }

    /***
     * @detail fetches the Id of the recipient that is set as the next scheduled
     * @return id of currently next scheduled recipient
     */
    public long getCurrentPiId() {
        Cursor cur = db.query(DATABASE_PI_TABLE, new String[] { KEY_SMS_ID }, KEY_PI_ID + "=1", null, null, null, null);
        // ********* "KEY_SMS_ID" in "Pi table" is denoting the next recipient.
        // Shall not be ***********
        // ********** confused with SMS as evident from the name
        // ***************************************
        cur.moveToFirst();
        long currentNextRecipientId = cur.getLong(cur.getColumnIndex(KEY_SMS_ID));
        cur.close();
        return currentNextRecipientId;
    }

    /***
     * @detail Sets Pending Intent data to point to another recipient as the
     *         next scheduled
     * @param pi_number
     * @param recipientId
     * @param time
     */
    public void updatePi(long pi_number, long recipientId, long time) {
        ContentValues cv = new ContentValues();

        if (getCurrentPiId() != -1) {
            Cursor cur = fetchRecipientDetails(getCurrentPiId());
            if (cur.moveToFirst()) {
                if (cur.getLong(cur.getColumnIndex(KEY_TIME_MILLIS)) > System.currentTimeMillis()) {
                    cv.put(KEY_OPERATED, 0);
                    db.update(DATABASE_RECIPIENT_TABLE, cv, KEY_RECIPIENT_ID + "=" + getCurrentPiId(), null);
                }
            }
            cur.close();
        }

        cv.clear();

        cv.put(KEY_PI_NUMBER, pi_number);
        cv.put(KEY_SMS_ID, recipientId);
        cv.put(KEY_TIME, time);

        db.update(DATABASE_PI_TABLE, cv, KEY_PI_ID + "= 1", null);
    }

    /**
     * @detail Update Pending Intent details to default, denoting that no more
     *         SMS is available to schedule.
     */
    public void updatePiForNoSmsValue() {
        updatePi(0, -1, -1);
    }

    /***
     * @detail gets the currently set next scheduled recipient's fire time
     * @return time in milliseconds
     */
    public long getCurrentPiFiretime() {
        Cursor cur = db.query(DATABASE_PI_TABLE, new String[] { KEY_TIME }, KEY_PI_ID + "= 1", null, null, null, null);
        cur.moveToFirst();
        long currentPiFireTime = cur.getLong(cur.getColumnIndex(KEY_TIME));
        cur.close();
        return currentPiFireTime;
    }

    // --------------------------------------------------------end of functions
    // for Pending Intent table---------

    // -------------------------functions for template
    // table---------------------------

    /**
     * @detail Fetches all the templates.
     * @return Cursor containing the templates
     */
    public Cursor fetchAllTemplates() {
        Cursor cur = db.query(DATABASE_TEMPLATE_TABLE, new String[] { KEY_TEMP_CONTENT, KEY_TEMP_ID }, null, null, null, null, null);
        Log.d("Size of Templates in DB : " + cur.getCount());
        return cur;
    }

    /**
     * @detail adds a new template
     * @param template
     * @return if successfully saved, then returns the id of newly created
     *         template, otherwise 0
     */
    public long addTemplate(String template) {
        ContentValues addTemplateValues = new ContentValues();
        addTemplateValues.put(KEY_TEMP_CONTENT, template);
        try {
            long newId = db.insert(DATABASE_TEMPLATE_TABLE, null, addTemplateValues);
            return newId;
        } catch (SQLException sqe) {
            return 0;
        }
    }

    /**
     * @detail edits an existing template
     * @param templateId
     * @param template
     */
    public void editTemplate(long templateId, String template) {
        ContentValues cv = new ContentValues();
        cv.put(KEY_TEMP_CONTENT, template);
        db.update(DATABASE_TEMPLATE_TABLE, cv, KEY_TEMP_ID + "=" + templateId, null);
    }

    /**
     * @detail removes a template
     * @param id
     * @return returns true if removed successfully else false;
     */
    public boolean removeTemplate(long id) {
        try {
            db.delete(DATABASE_TEMPLATE_TABLE, KEY_TEMP_ID + "=" + id, null);
            return true;
        } catch (SQLException sqe) {
            return false;
        }
    }

    // -----------------------------------------------------end of functions for
    // template table-----

    // -----------------------------------functions for group
    // table--------------------------------------
    // "Group" in this table denotes the groups that are private to Sms
    // Scheduler application, not the native ones.

    /***
     * @detail Fetch the detail of all the Groups
     * @return Cursor containing the detail
     */
    public Cursor fetchAllGroups() {
        Cursor cur = db.query(DATABASE_GROUP_TABLE, null, null, null, null, null, null);
        return cur;
    }

    /**
     * @detail For a given groupId, fetches an array of ContactIds that are
     *         there in that group
     * @param groupId
     * @return ArrayList of contactIds.
     */
    public ArrayList<Long> fetchIdsForGroups(long groupId) {
        ArrayList<Long> ids = new ArrayList<Long>();
        Cursor cur = db.query(DATABASE_GROUP_CONTACT_RELATION, new String[] { KEY_CONTACTS_ID }, KEY_GROUP_REL_ID + "=" + groupId, null, null, null, null);
        if (cur.moveToFirst()) {
            do {
                ids.add(cur.getLong(cur.getColumnIndex(KEY_CONTACTS_ID)));
            } while (cur.moveToNext());
        }
        cur.close();
        return ids;
    }

    /**
     * @detail for a give groupId, fetches all the contact numbers of all the
     *         group members
     * @param groupId
     * @return Arraylist of contact numbers
     */
    public ArrayList<String> fetchNumbersForGroup(long groupId) {
        ArrayList<String> numbers = new ArrayList<String>();
        Cursor cur = db.query(DATABASE_GROUP_CONTACT_RELATION, new String[] { KEY_CONTACTS_NUMBER }, KEY_GROUP_REL_ID + "=" + groupId, null, null, null, null);
        if (cur.moveToFirst()) {
            do {
                numbers.add(cur.getString(cur.getColumnIndex(KEY_CONTACTS_NUMBER)));
            } while (cur.moveToNext());
        }
        cur.close();
        return numbers;
    }

    /**
     * @detail creates a new Contact Group
     * @param name
     * @param contactIds
     * @param contactNumbers
     * @return groupId of the newly created group.
     */
    public long createGroup(String name, ArrayList<Long> contactIds, ArrayList<String> contactNumbers) {
        ContentValues cv = new ContentValues();
        cv.put(KEY_GROUP_NAME, name);
        long grpid = db.insert(DATABASE_GROUP_TABLE, null, cv);
        for (int i = 0; i < contactIds.size(); i++) {
            addContactToGroup(contactIds.get(i), grpid, contactNumbers.get(i));
        }
        return grpid;
    }

    /**
     * @detail adds a contact(using params: contactId and contactNumber) to a
     *         group (param: groupId)
     * @param contactId
     * @param groupId
     * @param contactNumber
     */
    public void addContactToGroup(long contactId, long groupId, String contactNumber) {
        ContentValues cv = new ContentValues();
        cv.put(KEY_GROUP_REL_ID, groupId);
        cv.put(KEY_CONTACTS_ID, contactId);
        cv.put(KEY_CONTACTS_NUMBER, contactNumber);
        db.insert(DATABASE_GROUP_CONTACT_RELATION, null, cv);
    }

    /**
     * @detail removes a contact (param: contactId) from a group (param:
     *         groupId)
     * @param contactId
     * @param groupId
     */
    public void removeContactFromGroup(long contactId, long groupId) {
        db.delete(DATABASE_GROUP_CONTACT_RELATION, KEY_GROUP_REL_ID + "=" + groupId + " AND " + KEY_CONTACTS_ID + "=" + contactId, null);
    }

    /**
     * @detail removes the group for the groupId passed as parameter
     * @param groupId
     */
    public void removeGroup(long groupId) {
        db.delete(DATABASE_GROUP_CONTACT_RELATION, KEY_GROUP_REL_ID + "=" + groupId, null);
        db.delete(DATABASE_GROUP_TABLE, KEY_GROUP_ID + "=" + groupId, null);
    }

    /**
     * @detail renames a group (param: groupId) with the string passed (param:
     *         name)
     * @param name
     * @param groupId
     */
    public void setGroupName(String name, long groupId) {
        ContentValues cv = new ContentValues();
        cv.put(KEY_GROUP_NAME, name);
        db.update(DATABASE_GROUP_TABLE, cv, KEY_GROUP_ID + "=" + groupId, null);
    }

    // ---------------------------------------------------------------end of
    // functions for group table---------------------

    // --------------------------------functions for recipient-group-relation
    // table--------------------------------
    // Table to log the relation between recipients and groups
    // It is in the context of SelectContacts Activity, for
    // seclecting/deselecting a particular contact in a native or private group.
    // A selection denotes that the recipient has been added through the group

    /**
     * @detail fetches all the recipient-ids for a contact-group
     * @param groupId
     * @param type
     * @return ArrayList of the recipientIds.
     */
    public ArrayList<Long> fetchRecipientsForGroup(long groupId, int type) {
        Cursor cur = db.query(DATABASE_RECIPIENT_GROUP_REL_TABLE, new String[] { KEY_RECIPIENT_GRP_REL_RECIPIENT_ID }, KEY_RECIPIENT_GRP_REL_GRP_ID + "=" + groupId + " AND " + KEY_RECIPIENT_GRP_REL_GRP_TYPE + "=" + type, null, null, null, null);
        ArrayList<Long> recipientIds = new ArrayList<Long>();
        if (cur.moveToFirst()) {
            do {
                recipientIds.add(cur.getLong(cur.getColumnIndex(KEY_RECIPIENT_GRP_REL_RECIPIENT_ID)));
            } while (cur.moveToNext());
        }
        cur.close();
        return recipientIds;
    }

    /**
     * @detail to add a recipient to a cotact-group, when a user selects a
     *         recipient using the Groups tab of SelectContacts Activity
     * @param recipientId
     * @param groupId
     * @param type
     */
    public void addRecipientGroupRel(long recipientId, long groupId, int type) {
        ContentValues cv = new ContentValues();
        cv.put(KEY_RECIPIENT_GRP_REL_RECIPIENT_ID, recipientId);
        cv.put(KEY_RECIPIENT_GRP_REL_GRP_ID, groupId);
        cv.put(KEY_RECIPIENT_GRP_REL_GRP_TYPE, type);

        db.insert(DATABASE_RECIPIENT_GROUP_REL_TABLE, null, cv);
    }

    /**
     * @detail deletes all recipient-group-relations associated with a
     *         particular recipientId
     * @param recipientId
     */
    public void deleteRecipientGroupRelsForRecipient(long recipientId) {
        db.delete(DATABASE_RECIPIENT_GROUP_REL_TABLE, KEY_RECIPIENT_GRP_REL_RECIPIENT_ID + "=" + recipientId, null);
    }

    /**
     * @detail deletes a particular recipient-group-relation for a group (param:
     *         groupId), recipient (param: recipientId) and type (native or
     *         sms_scheduler group)
     * @param recipientId
     * @param groupId
     * @param type
     */
    public void deleteRecipientGroupRel(long recipientId, long groupId, int type) {
        db.delete(DATABASE_RECIPIENT_GROUP_REL_TABLE, KEY_RECIPIENT_GRP_REL_RECIPIENT_ID + "=" + recipientId + " AND " + KEY_RECIPIENT_GRP_REL_GRP_ID + "=" + groupId + " AND " + KEY_RECIPIENT_GRP_REL_GRP_TYPE + "=" + type, null);
    }

    // ----------------------------------------------end of functions for
    // span-group-relation table----------------

    // ----------------------------functions for recents
    // table----------------------------------
    // This table holds 20 most recently used contacts in descending order i.e.,
    // the most recent on the top followed by contacts previously used
    // 'contactId = -1' denotes that the recent entry is an independent number

    /**
     * @detail when a contact (specific to a contactNumber, as a contact can
     *         have multiple contact numbers) is used to create an SMS, an entry
     *         is made on the top of the stack in recents table and deletes the
     *         contact at bottom (deletes only if there are 20 contacts already
     *         in recents table). Before making this entry, this function checks
     *         if this contact is already having an entry in the recents table.
     *         If present, then instead of creating a new entry and deleting the
     *         last, it simply swaps the contact to the top of the recents
     *         table.
     * 
     * @param contactId
     * @param contactNumber
     */
    public void addRecentContact(long contactId, String contactNumber) {
        contactNumber = refineNumber(contactNumber);
        Cursor cur = db.query(DATABASE_RECENTS_TABLE, new String[] { KEY_RECENT_CONTACT_ID, KEY_RECENT_CONTACT_CONTACT_ID, KEY_RECENT_CONTACT_NUMBER }, null, null, null, null, KEY_RECENT_CONTACT_ID);
        boolean contactExist = false;
        ContentValues cv = new ContentValues();
        cv.put(KEY_RECENT_CONTACT_CONTACT_ID, contactId);
        cv.put(KEY_RECENT_CONTACT_NUMBER, contactNumber);
        if (cur.moveToFirst()) {
            do {
                if (contactId != -1 && (cur.getLong(cur.getColumnIndex(KEY_RECENT_CONTACT_CONTACT_ID)) == contactId) && (cur.getString(cur.getColumnIndex(KEY_RECENT_CONTACT_NUMBER)).equals(contactNumber))) {
                    // match found for a recent entry that is not an independent
                    // numer. It is to be deleted in order to swap it to the top
                    db.delete(DATABASE_RECENTS_TABLE, KEY_RECENT_CONTACT_ID + " = " + cur.getLong(cur.getColumnIndex(KEY_RECENT_CONTACT_ID)), null);
                    contactExist = true;
                    break;
                }
                if (((cur.getLong(cur.getColumnIndex(KEY_RECENT_CONTACT_CONTACT_ID))) == -1) && (cur.getString(cur.getColumnIndex(KEY_RECENT_CONTACT_NUMBER)).equals(contactNumber))) {
                    // match found for a recent entry that is an independent
                    // number. It is to be deleted in order to swap it to the
                    // top
                    db.delete(DATABASE_RECENTS_TABLE, KEY_RECENT_CONTACT_CONTACT_ID + "=-1 AND " + KEY_RECENT_CONTACT_NUMBER + "=" + contactNumber, null);
                    contactExist = true;
                    break;
                }
            } while (cur.moveToNext());
        }
        if (!contactExist) {
            if (cur.getCount() >= 20 && cur.moveToFirst()) {
                // if contact doesn't already exist and recents table is full,
                // delete the last entry
                long idToDelete = cur.getLong(cur.getColumnIndex(KEY_RECENT_CONTACT_ID));
                db.delete(DATABASE_RECENTS_TABLE, KEY_RECENT_CONTACT_ID + "=" + idToDelete, null);
            }
        }
        cur.close();

        // make entry for the new contact
        db.insert(DATABASE_RECENTS_TABLE, null, cv);
    }

    /**
     * @detail fetches all the recent contacts
     * @return cursor containing recent contacts
     */
    public Cursor fetchAllRecents() {
        Cursor cur = db.query(DATABASE_RECENTS_TABLE, null, null, null, null, null, KEY_RECENT_CONTACT_ID + " DESC");
        return cur;
    }

    // ----------------------------------------------end of functions for
    // recents table-----------------

    /**
     * @removes all the invalid characters from the phone number (only 0-9
     *          allowed)
     * @param number
     * @return refined number
     */
    private String refineNumber(String number) {
        if (number.matches("[0-9]+")) {
            return number;
        }
        ArrayList<Character> chars = new ArrayList<Character>();
        for (int i = 0; i < number.length(); i++) {
            chars.add(number.charAt(i));
        }
        for (int i = 0; i < chars.size(); i++) {
            if (!(chars.get(i) == '0' || chars.get(i) == '1' || chars.get(i) == '2' || chars.get(i) == '3' || chars.get(i) == '4' || chars.get(i) == '5' || chars.get(i) == '6' || chars.get(i) == '7' || chars.get(i) == '8' || chars.get(i) == '9' || chars.get(i) == '+')) {
                chars.remove(i);
                i--;
            }
        }
        number = new String();
        for (int i = 0; i < chars.size(); i++) {
            number = number + chars.get(i);
        }
        return number;
    }

    // ----------------------------------------------------------end of
    // functions--------------------------------

    /**
     * @detail This class is extended from SQLiteOpenHelper class to perform DDL
     *         commands on our SQLite database implements the overridden
     *         functions: onCreate and onUpgrade
     * 
     *         onCreate is called once when the app is first installed and
     *         database is created. onUpgrade is called whenever the app is
     *         updated and a new database version is made available. This
     *         corresponds to the version changes made in the Database
     *         structure.
     */
    public class MyOpenHelper extends SQLiteOpenHelper {

        MyOpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DATABASE_CREATE_SMS_TABLE);
            db.execSQL(DATABASE_CREATE_RECIPIENT_TABLE);
            db.execSQL(DATABASE_CREATE_TEMPLATE_TABLE);
            db.execSQL(DATABASE_CREATE_PI_TABLE);
            db.execSQL(DATABASE_CREATE_GROUP_TABLE);
            db.execSQL(DATABASE_CREATE_GROUP_CONTACT_RELATION);
            db.execSQL(DATABASE_CREATE_RECIPIENT_GROUP_REL_TABLE);
            db.execSQL(DATABASE_CREATE_RECENTS_TABLE);

            // -------Setting initial content of Pending Intent-------
            ContentValues initialPi = new ContentValues();
            initialPi.put(KEY_PI_ID, 1);
            initialPi.put(KEY_PI_NUMBER, 0);
            initialPi.put(KEY_SMS_ID, -1);
            initialPi.put(KEY_TIME, -1);

            db.insert(DATABASE_PI_TABLE, null, initialPi);
            // -------------------------------------------------------

            // -------Setting default templates for the app-----------
            ContentValues initialTemplates = new ContentValues();

            initialTemplates.put(KEY_TEMP_CONTENT, "I'm in a meeting. I'll contact you later.");
            db.insert(DATABASE_TEMPLATE_TABLE, null, initialTemplates);

            initialTemplates.put(KEY_TEMP_CONTENT, "I'm driving now. I'll contact you later.");
            db.insert(DATABASE_TEMPLATE_TABLE, null, initialTemplates);

            initialTemplates.put(KEY_TEMP_CONTENT, "I'm busy. Will give you a call later.");
            db.insert(DATABASE_TEMPLATE_TABLE, null, initialTemplates);

            initialTemplates.put(KEY_TEMP_CONTENT, "Sorry, I'm going to be late.");
            db.insert(DATABASE_TEMPLATE_TABLE, null, initialTemplates);

            initialTemplates.put(KEY_TEMP_CONTENT, "Have a nice day!");
            db.insert(DATABASE_TEMPLATE_TABLE, null, initialTemplates);
            // ---------------------------------------------------------
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // All the previous database versions are to be upgraded to the
            // latest version. So the switch-case structure makes it go
            // through all the upper versions step-by-step until its upgraded to
            // the latest one.
            // Example: database at v1 will go through v2, v3, v4, ..... latest
            // version; database at v2 will go through v3, v4, v5, .... latest
            // version;

            switch (oldVersion) {
            case 1:
                From1To2(db);

            case 2:
                From2To3(db);

            case 3:
                From3To4(db);

            }
            // -----------------------------------------------------------PI
            // table updated---------------------
        }

        /**
         * @detail upgrades the database from version 1 to version 2
         * @param db
         */
        private void From1To2(SQLiteDatabase db) {
            ArrayList<Sms> SMSs = new ArrayList<Sms>();
            ArrayList<Recipient> Recipients = new ArrayList<Recipient>();

            // ------------Canceling the existing Pending Intent if one
            // exists--------------
            Cursor piCur = db.query(DATABASE_PI_TABLE, null, null, null, null, null, null);
            piCur.moveToFirst();
            Intent intent = new Intent(context, SMSHandleReceiver.class);
            intent.setAction(Constants.PRIVATE_SMS_ACTION);
            PendingIntent pi;
            if (piCur.getLong(piCur.getColumnIndex(DBAdapter.KEY_TIME)) > 0) {
                intent.putExtra("ID", piCur.getLong(piCur.getColumnIndex(DBAdapter.KEY_SMS_ID)));
                intent.putExtra("NUMBER", " ");
                intent.putExtra("MESSAGE", " ");

                pi = PendingIntent.getBroadcast(context, (int) piCur.getLong(piCur.getColumnIndex(DBAdapter.KEY_PI_NUMBER)), intent, PendingIntent.FLAG_CANCEL_CURRENT);
                pi.cancel();
            }
            piCur.close();
            // ------------------------------------------Pending Intent
            // canceled-------------

            // --------------Extracting SMS and Recipient data from existing
            // tables---------------
            // ---------------and storing them in SMS and Recipient Object
            // arrays----------------
            String sql = "SELECT DISTINCT group_id, message, date, time_millis, msg_parts " + "FROM smsTable";
            Cursor distinctSmsCursor = db.rawQuery(sql, null);
            if (distinctSmsCursor.moveToFirst()) {
                do {
                    SMSs.add(new Sms(distinctSmsCursor.getLong(distinctSmsCursor.getColumnIndex("group_id")), "", distinctSmsCursor.getString(distinctSmsCursor.getColumnIndex("message")), distinctSmsCursor.getInt(distinctSmsCursor.getColumnIndex("msg_parts")), distinctSmsCursor.getLong(distinctSmsCursor.getColumnIndex("time_millis")), distinctSmsCursor.getString(distinctSmsCursor.getColumnIndex("date")), null, 0, "no repeat"));

                    Cursor recipientsDataCur = db.query("smsTable", null, "group_id=" + distinctSmsCursor.getLong(distinctSmsCursor.getColumnIndex("group_id")), null, null, null, null);
                    if (recipientsDataCur.moveToFirst()) {
                        do {
                            ArrayList<Long> groupIds = new ArrayList<Long>();
                            ArrayList<Integer> groupTypes = new ArrayList<Integer>();

                            Cursor spansDataCur = db.query("spanTable", null, "span_sms_id=" + recipientsDataCur.getLong(recipientsDataCur.getColumnIndex("_id")), null, null, null, null);
                            if (spansDataCur.moveToFirst()) {
                                Cursor spansGroupRelCur = db.query("span_grp_rel_table", null, "span_grp_rel_span_id=" + spansDataCur.getLong(spansDataCur.getColumnIndex("_id")), null, null, null, null);
                                if (spansGroupRelCur.moveToFirst()) {
                                    do {
                                        groupIds.add(spansGroupRelCur.getLong(spansGroupRelCur.getColumnIndex("span_grp_rel_grp_id")));
                                        groupTypes.add(spansGroupRelCur.getInt(spansGroupRelCur.getColumnIndex("span_grp_rel_grp_type")));
                                    } while (spansGroupRelCur.moveToNext());
                                }
                                Recipient recipient = new Recipient();
                                recipient.smsId = distinctSmsCursor.getLong(distinctSmsCursor.getColumnIndex("group_id"));
                                recipient.number = recipientsDataCur.getString(recipientsDataCur.getColumnIndex("number"));
                                recipient.sent = recipientsDataCur.getInt(recipientsDataCur.getColumnIndex("sent"));
                                recipient.delivered = recipientsDataCur.getInt(recipientsDataCur.getColumnIndex("deliver"));
                                recipient.operated = recipientsDataCur.getInt(recipientsDataCur.getColumnIndex("operation_done"));
                                recipient.displayName = spansDataCur.getString(spansDataCur.getColumnIndex("span_display_name"));
                                recipient.type = spansDataCur.getInt(spansDataCur.getColumnIndex("span_type"));
                                recipient.contactId = spansDataCur.getLong(spansDataCur.getColumnIndex("span_entity_id"));
                                recipient.groupIds = groupIds;
                                recipient.groupTypes = groupTypes;
                                Recipients.add(recipient);
                            }
                            spansDataCur.close();
                        } while (recipientsDataCur.moveToNext());
                        recipientsDataCur.close();
                    }
                } while (distinctSmsCursor.moveToNext());
            }
            distinctSmsCursor.close();

            // ------------------------------------------------Data extracted
            // from previous tables-------------

            // -------------Dropping previous tables--------------
            sql = "DROP TABLE smsTable";
            db.execSQL(sql);
            sql = "DROP TABLE spanTable";
            db.execSQL(sql);
            sql = "DROP TABLE span_grp_rel_table";
            db.execSQL(sql);
            sql = "DROP TABLE piTable";
            db.execSQL(sql);
            // -------------------------------------------------

            // -------------Creating new tables--------------------
            db.execSQL(DATABASE_CREATE_SMS_TABLE);
            db.execSQL(DATABASE_CREATE_RECIPIENT_TABLE);
            db.execSQL(DATABASE_CREATE_RECIPIENT_GROUP_REL_TABLE);
            db.execSQL(DATABASE_CREATE_PI_TABLE);
            // ----------------------------------------------------

            // --------------Inserting SMS and Recipient Data into new tables
            // from Object arrays-------------
            ContentValues cv = new ContentValues();
            for (Sms s : SMSs) {
                cv.put(KEY_MESSAGE, s.keyMessage);
                cv.put(KEY_DATE, s.keyDate);
                cv.put(KEY_TIME_MILLIS, s.keyTimeMilis);
                cv.put(KEY_MSG_PARTS, s.keyMessageParts);

                long receivedSmsId = db.insert(DATABASE_SMS_TABLE, null, cv);

                int status = 0;
                boolean areAllOperated = true;
                for (Recipient r : Recipients) {
                    if (r.smsId == s.keyId) {
                        cv.clear();
                        cv.put(KEY_SMS_ID, receivedSmsId);
                        cv.put(KEY_NUMBER, r.number);
                        cv.put(KEY_DISPLAY_NAME, r.displayName);
                        cv.put(KEY_RECIPIENT_TYPE, r.type);
                        cv.put(KEY_SENT, r.sent);
                        cv.put(KEY_DELIVER, r.delivered);
                        cv.put(KEY_S_MILLIS, -1);
                        cv.put(KEY_D_MILLIS, -1);
                        cv.put(KEY_CONTACT_ID, r.contactId);

                        long receivedRecipientId = db.insert(DATABASE_RECIPIENT_TABLE, null, cv);

                        if (r.operated == Constants.RECIPIENT_OPERATED_FLAG_UNSET) {
                            areAllOperated = false;
                        }

                        for (int i = 0; i < r.groupIds.size(); i++) {
                            cv.clear();

                            cv.put(KEY_RECIPIENT_GRP_REL_RECIPIENT_ID, receivedRecipientId);
                            cv.put(KEY_RECIPIENT_GRP_REL_GRP_ID, r.groupIds.get(i));
                            cv.put(KEY_RECIPIENT_GRP_REL_GRP_TYPE, r.groupTypes.get(i));

                            db.insert(DATABASE_RECIPIENT_GROUP_REL_TABLE, null, cv);
                        }
                        if (areAllOperated) {
                            status = Constants.SMS_STATUS_SENT;
                        } else {
                            status = Constants.SMS_STATUS_SCHEDULED;
                        }
                    }
                }

                if (s.keyMessage.matches("(''|[' ']*)")) {
                    status = 0;
                }
                cv.clear();
                cv.put(KEY_STATUS, status);

                db.update(DATABASE_SMS_TABLE, cv, KEY_ID + "=" + receivedSmsId, null);
            }
            // -------------------------------------------data inserted into new
            // tables-------------------

            // --------------------updating the PI table for next
            // SMS-------------------------------------
            sql = "SELECT * FROM smsTable, recipientTable WHERE recipientTable.sms_id=smsTable._id AND recipientTable.recipient_id=" + "(SELECT recipientTable.recipient_id FROM recipientTable, smsTable " + "WHERE recipientTable.sms_id = smsTable._id AND recipientTable.operated=0 AND smsTable._id=" + "(SELECT smsTable._id FROM smsTable WHERE  smsTable.time_millis=" + "(SELECT MIN(smsTable.time_millis) FROM smsTable, recipientTable WHERE (smsTable.status=1 OR smsTable.status=3))))";

            Cursor cur = db.rawQuery(sql, null);

            if (cur.moveToFirst()) {
                intent = new Intent(context, SMSHandleReceiver.class);
                intent.setAction(Constants.PRIVATE_SMS_ACTION);
                intent.putExtra("SMS_ID", cur.getLong(cur.getColumnIndex(KEY_ID)));
                intent.putExtra("RECIPIENT_ID", cur.getLong(cur.getColumnIndex(KEY_RECIPIENT_ID)));
                intent.putExtra("NUMBER", cur.getString(cur.getColumnIndex(KEY_NUMBER)));
                intent.putExtra("MESSAGE", cur.getString(cur.getColumnIndex(KEY_MESSAGE)));

                Random rand = new Random();
                int piNumber = rand.nextInt();
                pi = PendingIntent.getBroadcast(context, piNumber, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                cv.clear();
                cv.put(KEY_PI_NUMBER, piNumber);
                cv.put(KEY_SMS_ID, cur.getLong(cur.getColumnIndex(KEY_RECIPIENT_ID)));
                cv.put(KEY_TIME, cur.getLong(cur.getColumnIndex(KEY_TIME_MILLIS)));

                db.insert(DATABASE_PI_TABLE, null, cv);

                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                alarmManager.set(AlarmManager.RTC_WAKEUP, cur.getLong(cur.getColumnIndex(KEY_TIME_MILLIS)), pi);
            } else {
                cv.clear();
                cv.put(KEY_PI_NUMBER, 0);
                cv.put(KEY_SMS_ID, -1);
                cv.put(KEY_TIME, -1);

                db.insert(DATABASE_PI_TABLE, null, cv);
            }
            cur.close();
        }

        /**
         * @detail upgrades the database from version 2 to version 3
         * @param db
         */
        private void From2To3(SQLiteDatabase db) {
            // ------------------fetching the Recipients in a
            // cursor--------------------------
            Cursor groupContactRelBack = db.query(DATABASE_GROUP_CONTACT_RELATION, null, null, null, null, null, null);

            ArrayList<GroupContactsBack> back = new ArrayList<GroupContactsBack>();

            if (groupContactRelBack.moveToFirst()) {
                do {
                    GroupContactsBack gcb = new GroupContactsBack();
                    gcb.contactId = groupContactRelBack.getLong(groupContactRelBack.getColumnIndex(KEY_CONTACTS_ID));
                    gcb.groupRelId = groupContactRelBack.getLong(groupContactRelBack.getColumnIndex(KEY_GROUP_REL_ID));
                    back.add(gcb);
                } while (groupContactRelBack.moveToNext());
            }

            String sql = "DROP TABLE groupContactRelation";
            db.execSQL(sql);

            db.execSQL(DATABASE_CREATE_GROUP_CONTACT_RELATION);

            ContentValues cv = new ContentValues();
            for (int m = 0; m < back.size(); m++) {
                String number = "";
                ContentResolver cr = context.getContentResolver();
                Cursor phones = cr.query(Phone.CONTENT_URI, null, Phone.CONTACT_ID + " = " + back.get(m).contactId, null, null);
                if (phones.moveToFirst())
                    number = phones.getString(phones.getColumnIndex(Phone.NUMBER));

                cv.clear();
                cv.put(KEY_GROUP_REL_ID, back.get(m).groupRelId);
                cv.put(KEY_CONTACTS_ID, back.get(m).contactId);
                cv.put(KEY_CONTACTS_NUMBER, number);
            }
        }

        /**
         * @detail upgrades the database from version 3 to version 4
         * @param db
         */
        private void From3To4(SQLiteDatabase db) {
            Cursor cur = db.query(DATABASE_SMS_TABLE, null, null, null, null, null, null);
            ArrayList<Sms> SMSs = new ArrayList<Sms>();

            if (cur.moveToFirst()) {
                do {
                    Sms sms = new Sms(cur.getLong(cur.getColumnIndex(DBAdapter.KEY_ID)), String.valueOf(cur.getInt(cur.getColumnIndex(DBAdapter.KEY_STATUS))), cur.getString(cur.getColumnIndex(DBAdapter.KEY_MESSAGE)), cur.getInt(cur.getColumnIndex(DBAdapter.KEY_MSG_PARTS)), cur.getLong(cur.getColumnIndex(DBAdapter.KEY_TIME_MILLIS)), cur.getString(cur.getColumnIndex(DBAdapter.KEY_DATE)), new ArrayList<Recipient>(), 0, "");
                    SMSs.add(sms);
                } while (cur.moveToNext());
            }

            String sql = "DROP TABLE smsTable";
            db.execSQL(sql);

            db.execSQL(DATABASE_CREATE_SMS_TABLE);

            ContentValues cv = new ContentValues();
            for (int i = 0; i < SMSs.size(); i++) {
                cv.clear();
                cv.put(KEY_ID, SMSs.get(i).keyId);
                cv.put(KEY_MESSAGE, SMSs.get(i).keyMessage);
                cv.put(KEY_DATE, SMSs.get(i).keyDate);
                cv.put(KEY_TIME_MILLIS, SMSs.get(i).keyTimeMilis);
                cv.put(KEY_MSG_PARTS, SMSs.get(i).keyMessageParts);
                cv.put(KEY_REPEAT_MODE, SMSs.get(i).keyRepeatMode);
                cv.put(KEY_REPEAT_STRING, SMSs.get(i).keyRepeatString);
                cv.put(KEY_STATUS, Integer.parseInt(SMSs.get(i).keyNumber));

                db.insert(DATABASE_SMS_TABLE, null, cv);
            }

        }

    }

    class GroupContactsBack {
        long groupRelId;
        long contactId;
    }
}
