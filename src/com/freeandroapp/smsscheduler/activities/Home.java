/**
 * Copyright (c) 2012 Vinayak Solutions Private Limited
 * See the file license.txt for copying permission.
 */

package com.freeandroapp.smsscheduler.activities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Groups;
import android.text.method.ScrollingMovementMethod;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.ExpandableListContextMenuInfo;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.airpush.android.Airpush;
import com.freeandroapp.smsscheduler.Constants;
import com.freeandroapp.smsscheduler.DBAdapter;
import com.freeandroapp.smsscheduler.R;
import com.freeandroapp.smsscheduler.SmsSchedulerApplication;
import com.freeandroapp.smsscheduler.activities.Group.ManageGroups;
import com.freeandroapp.smsscheduler.activities.ScheduleSms.EditScheduledSms;
import com.freeandroapp.smsscheduler.activities.ScheduleSms.ScheduleNewSms;
import com.freeandroapp.smsscheduler.activities.Template.ManageTemplates;
import com.freeandroapp.smsscheduler.constants.SmsSchedulerConstants;
import com.freeandroapp.smsscheduler.models.Contact;
import com.freeandroapp.smsscheduler.models.ContactNumber;
import com.freeandroapp.smsscheduler.models.Recipient;
import com.freeandroapp.smsscheduler.models.Sms;
import com.freeandroapp.smsscheduler.utils.MyGson;
import com.freeandroapp.smsscheduler.utils.Utils;

public class Home extends Activity {
	public static final int DIALOG_HELP = 1;
    private class ChildRowHolder {
        TextView messageTextView;
        com.freeandroapp.smsscheduler.utils.ExtendedImageView statusImageView;
        TextView dateTextView;
        TextView receiverTextView;
        TextView extraReceiversTextView;
        ImageView repeatModeIcon;
    }
    /**
     * @details makes loading of contacts, deserialization and serialization
     *          happen in the background on a separate thread. In Post Execute,
     *          fires up a broadcast intent that closes the "loading Contacts"
     *          wait dialog.
     */
    private class ContactsAsync extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            String data = contactData.getString("Data", "default");
            SmsSchedulerApplication.contactsList.clear();

            if (!data.equals("default")) {
                SmsSchedulerApplication.contactsList = myGson.deserializer(data);
            } else {
                loadContactsByPhone();
                String jsonString = myGson.serializer(SmsSchedulerApplication.contactsList);
                SharedPreferences.Editor editor = contactData.edit();
                editor.putString("Data", jsonString);
                editor.commit();
            }
            SharedPreferences.Editor editor = contactData.edit();
            editor.putString("isChanged", "0");
            editor.commit();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            SmsSchedulerApplication.isDataLoaded = true;
            Intent mIntent = new Intent();

            mIntent.setAction(Constants.DIALOG_CONTROL_ACTION);

            sendBroadcast(mIntent);
        }
    }
    private class GroupListHolder {
        TextView groupHeading;
    }

    // -----------functions for new Contacts load--------------------
    /**
     * @details Observes if any change is made to the native Contacts database.
     *          Upon occurance, the 'onChange' method of this class reloads the
     *          Contacts.
     */
    private class MyContentObserver extends ContentObserver {
        Context _context;

        public MyContentObserver(Handler h, Context context) {
            super(h);
            _context = context;
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            reloadSharedPreference(_context);
        }
    }
    private class SentDialogListHolder {
        TextView numberLabel;
        ImageView statusImage;
    }
    // ********* Adapter for the list of recipients and msg status, in the show
    // dialog of sent msgs ***********************
    @SuppressWarnings("rawtypes")
    private class SentDialogNumberListAdapter extends ArrayAdapter {

        @SuppressWarnings({ "unchecked" })
        SentDialogNumberListAdapter() {
            super(Home.this, R.layout.sent_sms_recepients_list_row, numbersForSentDialog);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            SentDialogListHolder holder;
            if (convertView == null) {
                LayoutInflater inflater = getLayoutInflater();
                convertView = inflater.inflate(R.layout.sent_sms_recepients_list_row, parent, false);
                holder = new SentDialogListHolder();
                holder.numberLabel = (TextView) convertView.findViewById(R.id.sent_details_number_list_number_text);
                holder.statusImage = (ImageView) convertView.findViewById(R.id.sent_details_number_list_status_image);
                convertView.setTag(holder);
            } else {
                holder = (SentDialogListHolder) convertView.getTag();
            }

            holder.numberLabel.setText(numbersForSentDialog[position]);

            mdba.open();
            if (sentSMSs.get(smsPositionForSentDialog).keyRecipients.get(position).sent > 0 && sentSMSs.get(smsPositionForSentDialog).keyRecipients.get(position).delivered != sentSMSs.get(smsPositionForSentDialog).keyMessageParts) {
                holder.statusImage.setImageResource(R.drawable.sending_sms_icon);
            } else if (sentSMSs.get(smsPositionForSentDialog).keyRecipients.get(position).delivered == sentSMSs.get(smsPositionForSentDialog).keyMessageParts) {
                holder.statusImage.setImageResource(R.drawable.sent_success_icon);
            }else{
                holder.statusImage.setImageResource(R.drawable.sent_failure_icon);
            }
            mdba.close();
            return convertView;
        }
    }

    private final ArrayList<Sms> scheduledSMSs = new ArrayList<Sms>();
    private final ArrayList<Sms> sentSMSs = new ArrayList<Sms>();

    private final ArrayList<Sms> drafts = new ArrayList<Sms>();
    private ExpandableListView explList;
    private ImageView newSmsButton;

    private ImageView optionsImageButton;
    // For the UI when there's no item in the list
    private LinearLayout blankListLayout;

    private Button blankListAddButton;
    // -------------------------------------------

    private SimpleExpandableListAdapter mAdapter;
    private ArrayList<HashMap<String, String>> headerData;
    private final ArrayList<ArrayList<HashMap<String, Object>>> childData = new ArrayList<ArrayList<HashMap<String, Object>>>();

    private String[] numbersForSentDialog = new String[] {};
    private int smsPositionForSentDialog;
    private final DBAdapter mdba = new DBAdapter(Home.this);

    private final int MENU_DELETE = R.id.home_options_delete;

    private final int MENU_RESCHEDULE = R.id.home_options_reschedule;
    private final int MENU_ADD_TO_TEMPLATE = R.id.home_options_add_to_template;

    private final int GROUP_DRAFT = 0;

    private final int GROUP_SCHEDULED = 1;

    private final int GROUP_SENT = 2;
    private Dialog sentInfoDialog;

    private Dialog dataLoadWaitDialog;

    private int toOpen = 0;

    Long selectedSms;
    private Cursor groupCursor;
    private IntentFilter mIntentFilter;
    private IntentFilter dataloadIntentFilter;

    private boolean showMessage;

    SharedPreferences contactData;

    // --------new contactload implementation vars---------//
    public static String PREFS_NAME = "MyPrefsFile";

    public Handler handler = new Handler();

    MyContentObserver contentObserver = new MyContentObserver(handler, Home.this);

    MyGson myGson = new MyGson();
    // -------------------------------------------------------

    /**
     * @details This broadcast fires up at any change in status of a recipient
     *          (one single message).
     */
    private final BroadcastReceiver mUpdateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            loadData();
            mAdapter.notifyDataSetChanged();
        }
    };

    /**
     * @details This broadcast fires up when The data to be displayed in the
     *          expandable list completes loading up.
     */
    private final BroadcastReceiver mDataLoadedReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (dataLoadWaitDialog.isShowing()) {
                dataLoadWaitDialog.cancel();
                if (toOpen == 1) {
                    toOpen = 0;
                    intent = new Intent(Home.this, ManageGroups.class);
                    startActivity(intent);
                }
            }
        }
    };

    /**
     * @details deletes a selected SMS, reloads the data and notifies the
     *          Expandable list to refresh.
     */
    private void deleteSms() {
        mdba.open();
        mdba.deleteSms(selectedSms, Home.this);
        Toast.makeText(Home.this,getResources().getString(R.string.message_deleted), Toast.LENGTH_SHORT).show();
        loadData();
        mAdapter.notifyDataSetChanged();
        doScreenUpdate();
    }

    /**
     * @details after an SMS is deleted, this function checks if any other SMS
     *          exist or not. If exists then, ExpandableList is visible and
     *          blankListLayout is invisible. Otherwise, vice-versa.
     */
    private void doScreenUpdate() {
        mdba.open();
        boolean ifSmsExist = mdba.ifSmsExist();
        mdba.close();
        if (ifSmsExist) {
            explList.setVisibility(LinearLayout.VISIBLE);
            blankListLayout.setVisibility(LinearLayout.GONE);
        } else {
            explList.setVisibility(LinearLayout.GONE);
            blankListLayout.setVisibility(LinearLayout.VISIBLE);
        }
    }

    /**
     * @details after pruning the number string upto certain number of
     *          recipients depending upon the length, this function calculates
     *          how many recipients have been excluded from the string. A string
     *          is made up like "+2" in case 2 recipients hadn't been included
     *          in the number's string.
     * @param number
     *            : String to calculate extra recipients string.
     * @return extraRecipientsString. "", "+2", "+6", etc
     */
    private String extraReceiversCal(String number) {
        if (number.length() <= 30) {
            return "";
        }
        int delimiterCount = 0;
        int validDelimiterCount = 0;
        for (int i = 0; i < number.length(); i++) {
            if (number.charAt(i) == ' ' && number.charAt(i - 1) == ',') {
                delimiterCount++;
                if (i <= 30) {
                    validDelimiterCount++;
                }
            }
        }

        return "+" + (delimiterCount - validDelimiterCount + 1);
    }

    // ------------------------Contacts Data Load
    // functions---------------------------------------------
    /**
     * @details loads contacts into data structures based upon the Phone numbers
     *          (ContactsContract.CommonDataKinds.Phone.CONTENT_URI) using
     *          ContactsContract database so that only those contacts which have
     *          at least one phone number is stored in data structure. By
     *          iterating over the Phone Numbers, we save the time consumed in
     *          looking through contacts without phone numbers.
     */
    public void loadContactsByPhone() {

        if (SmsSchedulerApplication.contactsList.size() == 0) {
            ContentResolver cr = getContentResolver();

            ArrayList<String> contactIds = new ArrayList<String>();
            ArrayList<Long> groups = new ArrayList<Long>();

            String[] projection = new String[] { Groups._ID, };
            Uri groupsUri = ContactsContract.Groups.CONTENT_URI;
            groupCursor = cr.query(groupsUri, projection, null, null, null);
            while (groupCursor.moveToNext()) {
                groups.add(groupCursor.getLong(groupCursor.getColumnIndex(Groups._ID)));
            }

            Cursor phones = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);

            while (phones.moveToNext()) {
                boolean isContactPresent = false;
                String contactId = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));
                ContactNumber cn = new ContactNumber(Long.parseLong(phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID))), Utils.refineNumber(phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))), resolveType(phones.getInt(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE))));

                if (phones.getInt(phones.getColumnIndex(Phone.IS_PRIMARY)) != 0) {
                    cn.isPrimary = true;
                }

                for (int i = 0; i < SmsSchedulerApplication.contactsList.size(); i++) {
                    if (Long.parseLong(contactId) == SmsSchedulerApplication.contactsList.get(i).content_uri_id) {
                        isContactPresent = true;
                        SmsSchedulerApplication.contactsList.get(i).numbers.add(cn);
                        break;
                    }
                }
                if (!isContactPresent) {
                    contactIds.add(contactId);
                    Contact contact = new Contact();
                    contact.content_uri_id = Long.parseLong(contactId);
                    contact.name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    contact.numbers.add(cn);

                    SmsSchedulerApplication.contactsList.add(contact);
                }
            }
            phones.close();

            String[] contactIdsArray = new String[contactIds.size()];
            for (int i = 0; i < contactIds.size(); i++) {
                contactIdsArray[i] = contactIds.get(i);
            }

            Cursor cur = cr.query(ContactsContract.Data.CONTENT_URI, new String[] { ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID, ContactsContract.CommonDataKinds.GroupMembership.CONTACT_ID }, null, null, null);

            if (cur.moveToFirst()) {
                do {
                    Long groupId = cur.getLong(cur.getColumnIndex(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID));
                    Long contactIdOfGroup = cur.getLong(cur.getColumnIndex(ContactsContract.CommonDataKinds.GroupMembership.CONTACT_ID));
                    boolean isValid = false;
                    for (int m = 0; m < groups.size(); m++) {
                        if (cur.getLong(cur.getColumnIndex(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID)) == groups.get(m)) {
                            isValid = true;
                            break;
                        }
                    }
                    if (!(groupId == 0) && isValid) {
                        for (int i = 0; i < SmsSchedulerApplication.contactsList.size(); i++) {
                            if (contactIdOfGroup == SmsSchedulerApplication.contactsList.get(i).content_uri_id) {
                                SmsSchedulerApplication.contactsList.get(i).groupRowId.add(groupId);
                            }
                        }
                    }
                } while (cur.moveToNext());
            }

            // To set primary number for contacts...
            for (int i = 0; i < SmsSchedulerApplication.contactsList.size(); i++) {
                boolean primaryPresent = false;
                for (int j = 0; j < SmsSchedulerApplication.contactsList.get(i).numbers.size(); j++) {
                    if (SmsSchedulerApplication.contactsList.get(i).numbers.get(j).isPrimary) {
                        SmsSchedulerApplication.contactsList.get(i).numbers.add(0, SmsSchedulerApplication.contactsList.get(i).numbers.remove(j));
                        primaryPresent = true;
                    }
                }
                if (!primaryPresent) {
                    // first number to be Primary
                    SmsSchedulerApplication.contactsList.get(i).numbers.get(0).isPrimary = true;
                }
            }

            for (int i = 0; i < SmsSchedulerApplication.contactsList.size() - 1; i++) {
                for (int j = i + 1; j < SmsSchedulerApplication.contactsList.size(); j++) {
                    if (SmsSchedulerApplication.contactsList.get(i).name.toUpperCase().compareTo(SmsSchedulerApplication.contactsList.get(j).name.toUpperCase()) > 0) {
                        SmsSchedulerApplication.contactsList.set(j, SmsSchedulerApplication.contactsList.set(i, SmsSchedulerApplication.contactsList.get(j)));
                    }
                }
            }
        }
    }

    /**
     * @details this function loads contacts by looping through Contacts array
     *          rather than through PhoneNumbers. This is not being used
     *          currently in the app but may find some use in updates to come.
     */
    public void loadContactsData() {
        if (SmsSchedulerApplication.contactsList.size() == 0) {
            System.currentTimeMillis();

            String[] projection = new String[] { Groups._ID };
            Uri groupsUri = ContactsContract.Groups.CONTENT_URI;

            ContentResolver cr = getContentResolver();
            groupCursor = cr.query(groupsUri, projection, null, null, null);
            Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
            if (cursor.moveToFirst()) {
                do {
                    if (!cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)).equals("0")) {
                        String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                        Cursor phones = cr.query(Phone.CONTENT_URI, null, Phone.CONTACT_ID + " = " + id, null, null);
                        if (phones.moveToFirst()) {
                            Contact contact = new Contact();
                            contact.content_uri_id = Long.parseLong(cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID)));
                            contact.name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                            do {
                                contact.numbers.add(new ContactNumber(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID), phones.getString(phones.getColumnIndex(Phone.NUMBER)), resolveType(Integer.parseInt(phones.getString(phones.getColumnIndex(Phone.TYPE))))));
                            } while (phones.moveToNext());
                            Cursor cur = cr.query(ContactsContract.Data.CONTENT_URI, new String[] { ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID }, ContactsContract.CommonDataKinds.GroupMembership.CONTACT_ID + "=" + contact.content_uri_id, null, null);
                            if (cur.moveToFirst()) {
                                do {
                                    // SAZWQA: Should we add a rule that if
                                    // GROUP_ROW_ID == 0 or it's equal to phone
                                    // no. don't ADD it?
                                    boolean equalsNumber = false;
                                    for (int i = 0; i < contact.numbers.size(); i++) {
                                        if (String.valueOf(cur.getLong(cur.getColumnIndex(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID))).equals(contact.numbers.get(i))) {
                                            equalsNumber = true;
                                            break;
                                        }
                                    }
                                    if (!equalsNumber && cur.getLong(cur.getColumnIndex(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID)) != 0) {
                                        boolean isValid = false;
                                        if (groupCursor.moveToFirst()) {
                                            do {
                                                if (!cur.isClosed() && !groupCursor.isClosed() && cur.getLong(cur.getColumnIndex(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID)) == groupCursor.getLong(groupCursor.getColumnIndex(Groups._ID))) {
                                                    isValid = true;
                                                    break;
                                                }
                                            } while (groupCursor.moveToNext());
                                        }
                                        if (isValid) {
                                            contact.groupRowId.add(cur.getLong(cur.getColumnIndex(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID)));
                                        }
                                    }
                                } while (cur.moveToNext());
                            }
                            cur.close();
                            SmsSchedulerApplication.contactsList.add(contact);
                        }
                        phones.close();
                    }
                } while (cursor.moveToNext());
            }
            cursor.close();
            groupCursor.close();

            for (int i = 0; i < SmsSchedulerApplication.contactsList.size() - 1; i++) {
                for (int j = i + 1; j < SmsSchedulerApplication.contactsList.size(); j++) {
                    if (SmsSchedulerApplication.contactsList.get(i).name.toUpperCase().compareTo(SmsSchedulerApplication.contactsList.get(j).name.toUpperCase()) > 0) {
                        SmsSchedulerApplication.contactsList.set(j, SmsSchedulerApplication.contactsList.set(i, SmsSchedulerApplication.contactsList.get(j)));
                    }
                }
            }
        }
    }

    /**
     * @define loads actual data from database to the data structures in order
     *         to feed into the ExpandableList.
     */
    private void loadData() {

        childData.clear();
        mdba.open();
        drafts.clear();
        scheduledSMSs.clear();
        sentSMSs.clear();

        // -----------------------Putting group headers for Expandable
        // list----------------------------
        headerData = new ArrayList<HashMap<String, String>>();

        // if(draftCur.getCount()>0){
        HashMap<String, String> group3 = new HashMap<String, String>();
        group3.put(SmsSchedulerConstants.NAME, "Drafts");
        headerData.add(group3);
        // }

        // if(schCur.getCount()>0){
        HashMap<String, String> group1 = new HashMap<String, String>();
        group1.put(SmsSchedulerConstants.NAME, "Scheduled");
        headerData.add(group1);
        // }

        // if(sentCur.getCount()>0){
        HashMap<String, String> group2 = new HashMap<String, String>();
        group2.put(SmsSchedulerConstants.NAME, "Sent");
        headerData.add(group2);
        // }
        // ---------------------------------------------------------------------------------------------

        // --------------Extracting Sent, Draft and Scheduled messages from
        // Database------------------------
        Cursor SMSsCur = mdba.fetchAllRecipientDetails();

        long previousSmsId = -1;
        int previousSmsType = -1;

        Sms SMS = new Sms();
        if (SMSsCur.moveToFirst()) {
            do {
                if (previousSmsId != SMSsCur.getLong(SMSsCur.getColumnIndex(DBAdapter.KEY_ID))) {

                    if (previousSmsType == 0) {
                        drafts.add(SMS);
                    } else if (previousSmsType == 1 || previousSmsType == 3) {
                        scheduledSMSs.add(SMS);
                    } else if (previousSmsType == 2) {
                        sentSMSs.add(SMS);
                    }

                    previousSmsId = SMSsCur.getLong(SMSsCur.getColumnIndex(DBAdapter.KEY_ID));
                    previousSmsType = SMSsCur.getInt(SMSsCur.getColumnIndex(DBAdapter.KEY_STATUS));

                    String displayName = "";
                    ArrayList<Recipient> tempRecipients = new ArrayList<Recipient>();

                    SMS = new Sms(SMSsCur.getLong(SMSsCur.getColumnIndex(DBAdapter.KEY_ID)),
                            displayName,
                            SMSsCur.getString(SMSsCur.getColumnIndex(DBAdapter.KEY_MESSAGE)),
                            SMSsCur.getInt(SMSsCur.getColumnIndex(DBAdapter.KEY_MSG_PARTS)),
                            SMSsCur.getLong(SMSsCur.getColumnIndex(DBAdapter.KEY_TIME_MILLIS)),
                            SMSsCur.getString(SMSsCur.getColumnIndex(DBAdapter.KEY_DATE)),
                            tempRecipients,
                            SMSsCur.getInt(SMSsCur.getColumnIndex(DBAdapter.KEY_REPEAT_MODE)),
                            SMSsCur.getString(SMSsCur.getColumnIndex(DBAdapter.KEY_REPEAT_STRING)));
                }
                Recipient recipient = new Recipient(SMSsCur.getLong(SMSsCur.getColumnIndex(DBAdapter.KEY_RECIPIENT_ID)),
                        SMSsCur.getInt(SMSsCur.getColumnIndex(DBAdapter.KEY_RECIPIENT_TYPE)),
                        SMSsCur.getString(SMSsCur.getColumnIndex(DBAdapter.KEY_DISPLAY_NAME)),
                        SMSsCur.getLong(SMSsCur.getColumnIndex(DBAdapter.KEY_CONTACT_ID)),
                        SMSsCur.getLong(SMSsCur.getColumnIndex(DBAdapter.KEY_ID)),
                        SMSsCur.getInt(SMSsCur.getColumnIndex(DBAdapter.KEY_SENT)),
                        SMSsCur.getInt(SMSsCur.getColumnIndex(DBAdapter.KEY_DELIVER)),
                        SMSsCur.getString(SMSsCur.getColumnIndex(DBAdapter.KEY_NUMBER)));
                if (SMS.keyNumber.equals("")) {
                    SMS.keyNumber = SMSsCur.getString(SMSsCur.getColumnIndex(DBAdapter.KEY_DISPLAY_NAME));
                } else {
                    SMS.keyNumber = SMS.keyNumber + ", " + SMSsCur.getString(SMSsCur.getColumnIndex(DBAdapter.KEY_DISPLAY_NAME));
                }

                SMS.keyRecipients.add(recipient);

            } while (SMSsCur.moveToNext());

            if (previousSmsType == 0) {
                drafts.add(SMS);
            } else if (previousSmsType == 1 || previousSmsType == 3) {
                scheduledSMSs.add(SMS);
            } else if (previousSmsType == 2) {
                sentSMSs.add(SMS);
            }
        }
        SMSsCur.close();
        // ------------------------------------------------Messages Extracted
        // From Database---------------------

        // ------------------------Loading scheduled
        // msgs----------------------------------------------------
        ArrayList<HashMap<String, Object>> groupChildSch = new ArrayList<HashMap<String, Object>>();

        for (int i = 0; i < scheduledSMSs.size(); i++) {
            HashMap<String, Object> child = new HashMap<String, Object>();
            child.put(SmsSchedulerConstants.NAME, scheduledSMSs.get(i).keyMessage);
            scheduledSMSs.get(i).keyImageRes = R.drawable.delete_icon_states;
            child.put(SmsSchedulerConstants.IMAGE, this.getResources().getDrawable(R.drawable.icon));
            child.put(SmsSchedulerConstants.DATE, scheduledSMSs.get(i).keyDate);
            child.put(SmsSchedulerConstants.RECEIVER, scheduledSMSs.get(i).keyNumber);
            child.put(SmsSchedulerConstants.EXTRA_RECEIVERS, extraReceiversCal(scheduledSMSs.get(i).keyNumber));
            child.put(SmsSchedulerConstants.REPEAT_MODE, scheduledSMSs.get(i).keyRepeatMode);
            groupChildSch.add(child);
        }
        // -------------------------------------------------------------------------end
        // of scheduled msgs load--------

        // --------------------------loading sent
        // messages------------------------------------------
        ArrayList<HashMap<String, Object>> groupChildSent = new ArrayList<HashMap<String, Object>>();

        for (int i = sentSMSs.size() - 1; i > -1; i--) {
            HashMap<String, Object> child = new HashMap<String, Object>();
            child.put(SmsSchedulerConstants.NAME, sentSMSs.get(i).keyMessage);
            int condition = 1;

            for (int k = 0; k < sentSMSs.get(i).keyRecipients.size(); k++) {
                if (sentSMSs.get(i).keyRecipients.get(k).sent == 0) {
                    condition = 1;
                    sentSMSs.get(i).keyImageRes = R.drawable.sent_failure_icon;
                    break;
                }
                if (sentSMSs.get(i).keyRecipients.get(k).sent > 0 && !mdba.checkDelivery(sentSMSs.get(i).keyRecipients.get(k).recipientId)) {
                    condition = 2;
                    sentSMSs.get(i).keyImageRes = R.drawable.sending_sms_icon;
                    break;
                }
                if (sentSMSs.get(i).keyRecipients.get(k).sent == sentSMSs.get(i).keyRecipients.get(k).delivered) {
                    condition = 3;
                }
            }

            if (condition == 3) {
                sentSMSs.get(i).keyImageRes = R.drawable.sent_success_icon;
            }

            child.put(SmsSchedulerConstants.IMAGE, this.getResources().getDrawable(R.drawable.icon));
            child.put(SmsSchedulerConstants.DATE, sentSMSs.get(i).keyDate);
            child.put(SmsSchedulerConstants.REPEAT_MODE, sentSMSs.get(i).keyRepeatMode);

            child.put(SmsSchedulerConstants.RECEIVER, Utils.numbersLengthRectify(sentSMSs.get(i).keyNumber));
            child.put(SmsSchedulerConstants.EXTRA_RECEIVERS, extraReceiversCal(sentSMSs.get(i).keyNumber));
            groupChildSent.add(child);
        }
        // --------------------------------------------------------------------------end
        // of sent msgs load-----------

        // ------------------------Loading
        // Drafts----------------------------------------------------
        ArrayList<HashMap<String, Object>> groupChildDraft = new ArrayList<HashMap<String, Object>>();

        for (int i = 0; i < drafts.size(); i++) {
            HashMap<String, Object> child = new HashMap<String, Object>();
            child.put(SmsSchedulerConstants.NAME, drafts.get(i).keyMessage);
            drafts.get(i).keyImageRes = R.drawable.delete_icon_states;

            child.put(SmsSchedulerConstants.IMAGE, this.getResources().getDrawable(R.drawable.icon));
            child.put(SmsSchedulerConstants.DATE, drafts.get(i).keyDate);
            child.put(SmsSchedulerConstants.RECEIVER, Utils.numbersLengthRectify(drafts.get(i).keyNumber));
            child.put(SmsSchedulerConstants.REPEAT_MODE, drafts.get(i).keyRepeatMode);
            try {
                child.put(SmsSchedulerConstants.EXTRA_RECEIVERS, extraReceiversCal(sentSMSs.get(i).keyNumber));
            } catch (IndexOutOfBoundsException e) {
                child.put(SmsSchedulerConstants.EXTRA_RECEIVERS, "");
            }

            groupChildDraft.add(child);
        }

        childData.add(groupChildDraft);
        childData.add(groupChildSch);
        childData.add(groupChildSent);
        // -------------------------------------------------------------------------end
        // of drafts load--------

        mdba.close();
    }

    /**
     * @details Overridden method of Activity class. Fires up when any item from
     *          the context menu for child elements of expandable list is
     *          selected.
     * @param item
     *            : Packages information abou the selected item, like group
     *            postion, child position, etc.
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ExpandableListContextMenuInfo info = (ExpandableListContextMenuInfo) item.getMenuInfo();
        int groupPos = 0, childPos = 0;
        int type = ExpandableListView.getPackedPositionType(info.packedPosition);
        if (type == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
            groupPos = ExpandableListView.getPackedPositionGroup(info.packedPosition);
            childPos = ExpandableListView.getPackedPositionChild(info.packedPosition);

            switch (item.getItemId()) {
            case MENU_DELETE:
                mdba.open();
                HashMap<String, String> deletedGroup = new HashMap<String, String>();
                if (groupPos == GROUP_SCHEDULED) {
                    selectedSms = scheduledSMSs.get(childPos).keyId;
                    deletedGroup.put("deleted SMS", "Scheduled SMS");

                } else if (groupPos == GROUP_SENT) {
                    selectedSms = sentSMSs.get(childPos).keyId;
                    deletedGroup.put("deleted SMS", "Edited SMS");

                } else if (groupPos == GROUP_DRAFT) {
                    selectedSms = drafts.get(childPos).keyId;
                    deletedGroup.put("deleted SMS", "Draft");

                }
                deleteSms();
                break;

            case MENU_RESCHEDULE:
                Intent intent = new Intent(Home.this, EditScheduledSms.class);
                intent.putExtra("SMS DATA", sentSMSs.get(childPos));
                startActivity(intent);
                break;

            case MENU_ADD_TO_TEMPLATE:
                showAddToTemplateDialog(sentSMSs.get(childPos).keyMessage);
                break;
            }
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		new Airpush(this,"100248","1346166928111406435",false,true,true);

        setContentView(R.layout.home);

        // if the native Contact list has been modified, the "isChanged" field
        // has a value "1" and the data in Contacts data structure reloads
        // -------
        contactData = getSharedPreferences(PREFS_NAME, 0);
        String isChanged = contactData.getString("isChanged", "1");

        this.getApplicationContext().getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI, true, contentObserver);

        if (!SmsSchedulerApplication.isDataLoaded || SmsSchedulerApplication.contactsList.size() == 0 || isChanged.equals("1")) {
            ContactsAsync contactsAsync = new ContactsAsync();
            contactsAsync.execute();
        }
        // ---------------------------------------------------------------------------------------------------------------------------------------

        newSmsButton = (ImageView) findViewById(R.id.main_new_sms_imgbutton);//the new SMS Button//TODO
        explList = (ExpandableListView) findViewById(R.id.main_expandable_list);
        optionsImageButton = (ImageView) findViewById(R.id.main_options_menu_imgbutton);
        blankListLayout = (LinearLayout) findViewById(R.id.blank_list_layout);
        blankListAddButton = (Button) findViewById(R.id.blank_list_add_button);

        registerForContextMenu(explList);

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        showMessage = settings.getBoolean("SHOW_NEW_FEATURES", true);

        showMessagePreference();

        dataLoadWaitDialog = new Dialog(Home.this);
        dataLoadWaitDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        newSmsButton.setOnClickListener(new OnClickListener() {

            public void onClick(View arg0) {
                Intent intent = new Intent(Home.this, ScheduleNewSms.class);
                startActivity(intent);
            }
        });

        blankListAddButton.setOnClickListener(new OnClickListener() {

            public void onClick(View arg0) {
                Intent intent = new Intent(Home.this, ScheduleNewSms.class);
                startActivity(intent);
            }
        });

        /**
         * @details Overridden method of ExpandableList for OnClick behaviour.
         *          It first checks the child of which group is clicked.
         *          groupPosition 0 (Draft section) : opens up EditScheduledSms
         *          activity with data of the draft. groupPosition 1 (Scheduled
         *          Section) : opens up EditScheduledSms activity with data of
         *          the Scheduled SMS. groupPosition 2 (Send Section) : opens up
         *          Sent Message Details Dialog.
         */
        explList.setOnChildClickListener(new OnChildClickListener() {

            public boolean onChildClick(ExpandableListView arg0, View view, int groupPosition, int childPosition, long id) {
                if (groupPosition == GROUP_SCHEDULED) {
                    Intent intent = new Intent(Home.this, EditScheduledSms.class);
                    intent.putExtra("SMS DATA", scheduledSMSs.get(childPosition));
                    startActivity(intent);
                } else if (groupPosition == GROUP_SENT) {
                    openContextMenu(view);
                } else if (groupPosition == GROUP_DRAFT) {
                    Intent intent = new Intent(Home.this, EditScheduledSms.class);
                    intent.putExtra("SMS DATA", drafts.get(childPosition));
                    startActivity(intent);
                }
                return false;
            }
        });

        // Manually implemented Options Menu (Button on the left of "New SMS"
        // button).
        optionsImageButton.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                openOptionsMenu();
            }
        });

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(getResources().getString(R.string.update_action));

        dataloadIntentFilter = new IntentFilter();
        dataloadIntentFilter.addAction(Constants.DIALOG_CONTROL_ACTION);

        setExplData();

        explList.setAdapter(mAdapter);
        registerForContextMenu(explList);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        ExpandableListView.ExpandableListContextMenuInfo info = (ExpandableListView.ExpandableListContextMenuInfo) menuInfo;
        int group = ExpandableListView.getPackedPositionGroup(info.packedPosition);
        ExpandableListView.getPackedPositionChild(info.packedPosition);

        CharSequence menu_title;

        if (group == GROUP_SENT) {
            // If selected SMS is from Sent group, its context menu will have
            // three options - 'Reschedule', 'Add to Templates' and 'Delete'.
            final String MENU_TITLE_RESCHEDULE = "Reschedule";
            menu_title = MENU_TITLE_RESCHEDULE.subSequence(0, MENU_TITLE_RESCHEDULE.length());
            menu.add(0, MENU_RESCHEDULE, 1, menu_title);

            final String MENU_TITLE_ADD_TO_TEMPLATES = "Add to Templates";
            menu_title = MENU_TITLE_ADD_TO_TEMPLATES.subSequence(0, MENU_TITLE_ADD_TO_TEMPLATES.length());
            menu.add(0, MENU_ADD_TO_TEMPLATE, 2, menu_title);

            final String MENU_TITLE_DELETE = "Delete";
            menu_title = MENU_TITLE_DELETE.subSequence(0, MENU_TITLE_DELETE.length());
            menu.add(0, MENU_DELETE, 3, menu_title);
        } else {
            // For Drafts and Scheduled SMSs, there will be only one option -
            // 'Delete'.
            final String MENU_TITLE_DELETE = "Delete";
            menu_title = MENU_TITLE_DELETE.subSequence(0, MENU_TITLE_DELETE.length());
            menu.add(0, MENU_DELETE, 1, menu_title);
        }
    }

    // ----------------------------------------------------------------------------------------

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
        case R.id.template_opt_menu:
            intent = new Intent(Home.this, ManageTemplates.class);
            startActivity(intent);
            break;
        case R.id.group_opt_menu:
            if (SmsSchedulerApplication.isDataLoaded) {
                intent = new Intent(Home.this, ManageGroups.class);
                startActivity(intent);
            } else {
                dataLoadWaitDialog.setContentView(R.layout.wait_dialog);
                toOpen = 1;
                dataLoadWaitDialog.show();
            }
            break;
        case R.id.about_opt_menu:
        	showDialog(DIALOG_HELP);
            break;
        }
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mUpdateReceiver);
        unregisterReceiver(mDataLoadedReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();

        doScreenUpdate();
        setExplData();
        explList.setAdapter(mAdapter);
        explList.expandGroup(GROUP_DRAFT);
        explList.expandGroup(GROUP_SCHEDULED);
        explList.expandGroup(GROUP_SENT);

        registerReceiver(mUpdateReceiver, mIntentFilter);
        registerReceiver(mDataLoadedReceiver, dataloadIntentFilter);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    /**
     * @details this function clears the current serialized string for Contacts.
     *          Reloads the Contacts, serializes it and saves it in
     *          SharedPreferences.
     * @param context
     */
    private void reloadSharedPreference(Context context) {
        SharedPreferences contactData = getSharedPreferences(PREFS_NAME, 0);
        System.currentTimeMillis();
        SmsSchedulerApplication.contactsList.clear();
        loadContactsByPhone();
        String jsonString = myGson.serializer(SmsSchedulerApplication.contactsList);
        SharedPreferences.Editor editor = contactData.edit();
        editor.putString("Data", jsonString);
        editor.putString("isChanged", "1");
        editor.commit();
    }

    /**
     * @details Resolves phone number type. The integer parameter 'type' is
     *          resolved into the string it is meant for.
     * @param type
     * @return corresponding phone-number type String.
     */
    public String resolveType(int type) {
        switch (type) {
        case Phone.TYPE_ASSISTANT:
            return "Assistant";
        case Phone.TYPE_CALLBACK:
            return "Callback";
        case Phone.TYPE_CAR:
            return "Car";
        case Phone.TYPE_COMPANY_MAIN:
            return "Company Main";
        case Phone.TYPE_FAX_HOME:
            return "Fax Home";
        case Phone.TYPE_FAX_WORK:
            return "Fax Work";
        case Phone.TYPE_HOME:
            return "Home";
        case Phone.TYPE_ISDN:
            return "ISDN";
        case Phone.TYPE_MAIN:
            return "Main";
        case Phone.TYPE_MMS:
            return "MMS";
        case Phone.TYPE_MOBILE:
            return "Mobile";
        case Phone.TYPE_OTHER:
            return "Other";
        case Phone.TYPE_OTHER_FAX:
            return "Other Fax";
        case Phone.TYPE_PAGER:
            return "Pager";
        case Phone.TYPE_RADIO:
            return "Radio";
        case Phone.TYPE_TELEX:
            return "Telex";
        case Phone.TYPE_TTY_TDD:
            return "TTY TDD";
        case Phone.TYPE_WORK:
            return "Work";
        case Phone.TYPE_WORK_MOBILE:
            return "Work Mobile";
        case Phone.TYPE_WORK_PAGER:
            return "Work Pager";
        case Phone.TYPE_CUSTOM:
            return "Custom";
        default:
            return "Other";
        }
    }

    /**
     * @details Sets the data into the Expandable List.
     */
    private void setExplData() {
        loadData();

        final LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mAdapter = new SimpleExpandableListAdapter(this, headerData, android.R.layout.simple_expandable_list_item_1, new String[] { SmsSchedulerConstants.NAME }, new int[] { android.R.id.text1 }, childData, 0, null, new int[] {}) {

            @Override
            public android.view.View getChildView(int groupPosition, final int childPosition, boolean isLastChild, android.view.View convertView, android.view.ViewGroup parent) {
                ChildRowHolder holder;
                if (convertView == null) {
                    convertView = layoutInflater.inflate(R.layout.home_expandable_list_child, null, false);
                    holder = new ChildRowHolder();
                    holder.messageTextView = (TextView) convertView.findViewById(R.id.main_row_message_area);
                    holder.statusImageView = (com.freeandroapp.smsscheduler.utils.ExtendedImageView) convertView.findViewById(R.id.main_row_image_area);
                    holder.dateTextView = (TextView) convertView.findViewById(R.id.main_row_date_area);
                    holder.receiverTextView = (TextView) convertView.findViewById(R.id.main_row_recepient_area);
                    holder.extraReceiversTextView = (TextView) convertView.findViewById(R.id.main_row_extra_recepient_area);
                    holder.repeatModeIcon = (ImageView) convertView.findViewById(R.id.repeat_icon);
                    convertView.setTag(holder);
                } else {
                    holder = (ChildRowHolder) convertView.getTag();
                }

                if (groupPosition == GROUP_SCHEDULED) {
                    // case: child is a scheduled message.
                    holder.messageTextView.setText(scheduledSMSs.get(childPosition).keyMessage);
                    holder.statusImageView.setImageResource(scheduledSMSs.get(childPosition).keyImageRes);
                    holder.dateTextView.setText(scheduledSMSs.get(childPosition).keyDate);
                    holder.receiverTextView.setText(Utils.numbersLengthRectify(scheduledSMSs.get(childPosition).keyNumber));
                    holder.extraReceiversTextView.setText(extraReceiversCal(scheduledSMSs.get(childPosition).keyNumber));
                    holder.messageTextView.setTextColor(getResources().getColor(R.color.black));
                    holder.receiverTextView.setTextColor(getResources().getColor(R.color.black));
                    if (scheduledSMSs.get(childPosition).keyRepeatMode == Constants.REPEAT_MODE_NO_REPEAT) {
                        holder.repeatModeIcon.setVisibility(View.GONE);
                    } else {
                        holder.repeatModeIcon.setVisibility(View.VISIBLE);
                    }
                } else if (groupPosition == GROUP_SENT) {
                    // case: child is a sent message.
                    holder.messageTextView.setText(sentSMSs.get(childPosition).keyMessage);
                    holder.statusImageView.setImageResource(sentSMSs.get(childPosition).keyImageRes);
                    holder.dateTextView.setText(sentSMSs.get(childPosition).keyDate);
                    holder.receiverTextView.setText(Utils.numbersLengthRectify(sentSMSs.get(childPosition).keyNumber));
                    holder.extraReceiversTextView.setText(extraReceiversCal(sentSMSs.get(childPosition).keyNumber));
                    holder.messageTextView.setTextColor(getResources().getColor(R.color.black));
                    holder.receiverTextView.setTextColor(getResources().getColor(R.color.black));
                    holder.repeatModeIcon.setVisibility(View.GONE);
                } else if (groupPosition == GROUP_DRAFT) {
                    // case: child is a draft.
                    if (!drafts.get(childPosition).keyMessage.matches(Constants.BLANK_OR_ONLY_SPACES_PATTERN)) {
                        holder.messageTextView.setText(drafts.get(childPosition).keyMessage);
                    } else {
                        holder.messageTextView.setText("[No Message Written]");
                        holder.messageTextView.setTextColor(getResources().getColor(R.color.grey));
                    }
                    holder.statusImageView.setImageResource(drafts.get(childPosition).keyImageRes);
                    holder.dateTextView.setText(drafts.get(childPosition).keyDate);
                    if (!drafts.get(childPosition).keyNumber.matches(Constants.BLANK_OR_ONLY_SPACES_PATTERN)) {
                        holder.receiverTextView.setText(Utils.numbersLengthRectify(drafts.get(childPosition).keyNumber));
                        holder.extraReceiversTextView.setText(extraReceiversCal(drafts.get(childPosition).keyNumber));
                    } else {
                        holder.receiverTextView.setText("[No Recepients Added]");
                        holder.receiverTextView.setTextColor(getResources().getColor(R.color.grey));
                        holder.extraReceiversTextView.setText("");
                    }
                    if (drafts.get(childPosition).keyRepeatMode == Constants.REPEAT_MODE_NO_REPEAT) {
                        holder.repeatModeIcon.setVisibility(View.GONE);
                    } else {
                        holder.repeatModeIcon.setVisibility(View.VISIBLE);
                    }
                }

                final HashMap<String, String> deletedGroup = new HashMap<String, String>();

                if (groupPosition == GROUP_SCHEDULED) {
                    holder.statusImageView.setOnClickListener(new OnClickListener() {

                        public void onClick(View v) {
                            showDeleteDialog(scheduledSMSs, childPosition, "Delete this Scheduled Message?");
                            deletedGroup.put("deleted SMS", "Scheduled SMS");
                        }
                    });

                } else if (groupPosition == GROUP_DRAFT) {
                    holder.statusImageView.setOnClickListener(new OnClickListener() {

                        public void onClick(View v) {
                            showDeleteDialog(drafts, childPosition, "Delete this Draft?");
                            deletedGroup.put("deleted SMS", "Draft");
                        }
                    });
                } else if (groupPosition == GROUP_SENT) {
                    holder.statusImageView.setOnClickListener(new OnClickListener() {

                        public void onClick(View v) {
                            showSentInfoDialog(childPosition);
                        }
                    });
                }

                return convertView;
            }

            @Override
            public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
                GroupListHolder holder;
                if (convertView == null) {
                    LayoutInflater li = getLayoutInflater();
                    convertView = li.inflate(R.layout.home_expandable_list_group, null);
                    holder = new GroupListHolder();
                    holder.groupHeading = (TextView) convertView.findViewById(R.id.group_heading);
                    convertView.setTag(holder);
                } else {
                    holder = (GroupListHolder) convertView.getTag();
                }

                holder.groupHeading.setText(headerData.get(groupPosition).get(SmsSchedulerConstants.NAME));

                return convertView;
            }
        };
    }

    /**
     * @details shows up a dialog to make an entry into the Templates table. The
     *          "message" from the SMS is provided in an EditText, to be edited
     *          and saved.
     * @param keyMessage
     *            message from the SMS.
     */
    private void showAddToTemplateDialog(String keyMessage) {
        final Dialog dialog = new Dialog(Home.this);
        dialog.setContentView(R.layout.add_to_templates_dialog);
        dialog.setTitle("Add to Templates");

        final EditText templateText = (EditText) dialog.findViewById(R.id.new_template_dialog_input_edit_text);
        Button addTemplateButton = (Button) dialog.findViewById(R.id.new_template_dialog_add_button);
        Button cancelTemplateButton = (Button) dialog.findViewById(R.id.new_template_dialog_cancel_button);

        templateText.setText(keyMessage);
        addTemplateButton.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                if (templateText.getText().toString().equals("")) {
                    Toast.makeText(Home.this, getResources().getString(R.string.blank_template_error), Toast.LENGTH_SHORT).show();
                } else {
                    mdba.open();
                    Cursor cur = mdba.fetchAllTemplates();
                    mdba.close();
                    boolean templateExists = false;
                    if (cur.moveToFirst()) {
                        do {
                            if (cur.getString(cur.getColumnIndex(DBAdapter.KEY_TEMP_CONTENT)).equals(templateText.getText().toString())) {
                                templateExists = true;
                                break;
                            }
                        } while (cur.moveToNext());
                    }
                    if (templateExists) {
                        Toast.makeText(Home.this,getResources().getString(R.string.template_already_exist), Toast.LENGTH_SHORT).show();
                    } else {
                        mdba.open();
                        mdba.addTemplate(templateText.getText().toString());
                        mdba.close();
                        Toast.makeText(Home.this, getResources().getString(R.string.template_added), Toast.LENGTH_SHORT).show();
                        dialog.cancel();
                    }
                }
            }
        });

        cancelTemplateButton.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                dialog.cancel();
            }
        });

        dialog.show();
    }

    /**
     * @details Confirmation dialog for deleting an SMS.
     * @param SMSList
     * @param childPosition
     * @param questionString
     */
    public void showDeleteDialog(final ArrayList<Sms> SMSList, final int childPosition, String questionString) {
        final Dialog d = new Dialog(Home.this);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        d.setContentView(R.layout.confirmation_dialog);
        TextView questionText = (TextView) d.findViewById(R.id.confirmation_dialog_text);
        Button yesButton = (Button) d.findViewById(R.id.confirmation_dialog_yes_button);
        Button noButton = (Button) d.findViewById(R.id.confirmation_dialog_no_button);

        questionText.setText(questionString);

        yesButton.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                selectedSms = SMSList.get(childPosition).keyId;
                deleteSms();
                d.cancel();
            }
        });

        noButton.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                d.cancel();
            }
        });
        d.show();
    }

    /**
     * @details shows a Dialog that lists the newly added features, when the
     *          application starts up. A shared pref value toggles the
     *          show/not-show of this dialog. This value can be set to false by
     *          user only once by checking the "Don't show this message again"
     *          option in the Dialog.
     */
    private void showMessagePreference() {
        if (showMessage) {
            final Dialog d = new Dialog(Home.this);
            d.requestWindowFeature(Window.FEATURE_NO_TITLE);
            d.setCancelable(false);
            d.setContentView(R.layout.show_message_dialog);

            final CheckBox checkBox = (CheckBox) d.findViewById(R.id.show_again_check);

            //((TextView) d.findViewById(R.id.header_text)).setText("New Features");
            //((TextView) d.findViewById(R.id.message_text)).setText(getString(R.string.new_feature_message));

            ((TextView) d.findViewById(R.id.dont_show_msg_text)).setOnClickListener(new OnClickListener() {

                public void onClick(View v) {
                    if (checkBox.isChecked()) {
                        checkBox.setChecked(false);
                    } else {
                        checkBox.setChecked(true);
                    }
                }
            });

            ((Button) d.findViewById(R.id.ok_button)).setOnClickListener(new OnClickListener() {

                public void onClick(View v) {
                    if (checkBox.isChecked()) {
                        // case: user has checked
                        // "Don't show this message again" checkbox.
                        // "SHOW_NEW_FEATURES" pref is set to false.
                        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putBoolean("SHOW_NEW_FEATURES", false);
                        editor.commit();
                    }
                    d.cancel();
                }
            });

            d.show();
        }
    }

    // --------------------------------------------------------------

    /**
     * @details opens up a dialog to show the details of a sent SMS.
     * @param childPos
     *            : to get the child to show the detail of.
     */
    private void showSentInfoDialog(int childPos) {
        sentInfoDialog = new Dialog(Home.this);
        sentInfoDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        sentInfoDialog.setContentView(R.layout.sent_sms_details);
        ListView numbersList = (ListView) sentInfoDialog.findViewById(R.id.sent_details_dialog_number_list);
        TextView timeLabel = (TextView) sentInfoDialog.findViewById(R.id.sent_details_dialog_time_label);
        TextView messageSpace = (TextView) sentInfoDialog.findViewById(R.id.sent_details_dialog_message_space);
        mdba.open();
        numbersForSentDialog = sentSMSs.get(childPos).keyNumber.split(", ");
        smsPositionForSentDialog = childPos;
        timeLabel.setText(sentSMSs.get(childPos).keyDate);
        messageSpace.setText(sentSMSs.get(childPos).keyMessage);
        messageSpace.setMovementMethod(new ScrollingMovementMethod());
        SentDialogNumberListAdapter sentDialogAdapter = new SentDialogNumberListAdapter();
        numbersList.setAdapter(sentDialogAdapter);
        mdba.close();
        sentInfoDialog.show();
    }
    
    @Override
	protected Dialog onCreateDialog(int id)
	{
		Dialog dlg = null;
		switch (id) 
		{
			case DIALOG_HELP:
	            AlertDialog.Builder builder = new AlertDialog.Builder(this);
	            String title = getString(R.string.sms_scheduler);
	            WebView wv = new WebView(this);
	            builder.setView(wv);
	            String data = "";
	            try {
	                InputStream is = getResources().openRawResource(R.raw.about);
	                InputStreamReader isr = new InputStreamReader(is, "UTF-8");
	                BufferedReader br = new BufferedReader(isr);
	                StringBuilder sb = new StringBuilder();
	                String line;
	                while ((line = br.readLine()) != null) {
	                    sb.append(line);
	                    sb.append('\n');
	                }
	                br.close();
	                data = sb.toString();
	            } catch (UnsupportedEncodingException e1) {
	            } catch (IOException e) {
	            }
	            /*System.out.printf("%.3f DroidFish.onCreateDialog(): data:%s\n",
	                    System.currentTimeMillis() * 1e-3, data);*/
	            wv.loadDataWithBaseURL(null, data, "text/html", "utf-8", null);
	            try {
	                PackageInfo pi = getPackageManager().getPackageInfo("com.freeandroapp.ultimateguns", 0);
	                title += " " + pi.versionName;
	            } catch (NameNotFoundException e) {
	            }
	    
	            builder.setIcon(R.drawable.icon);
	            builder.setTitle(title);
	            AlertDialog alert = builder.create();
	            return alert;
		}
		
		return dlg;
	}
}
