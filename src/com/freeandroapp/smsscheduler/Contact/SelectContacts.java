/**
 * Copyright (c) 2012 Vinayak Solutions Private Limited 
 * See the file license.txt for copying permission.
 */

package com.freeandroapp.smsscheduler.Contact;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SectionIndexer;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;

import com.freeandroapp.smsscheduler.Constants;
import com.freeandroapp.smsscheduler.DBAdapter;
import com.freeandroapp.smsscheduler.SmsSchedulerApplication;
import com.freeandroapp.smsscheduler.activities.ScheduleSms.AbstractScheduleSms;
import com.freeandroapp.smsscheduler.activities.ScheduleSms.EditScheduledSms;
import com.freeandroapp.smsscheduler.activities.ScheduleSms.ScheduleNewSms;
import com.freeandroapp.smsscheduler.models.Contact;
import com.freeandroapp.smsscheduler.models.ContactNumber;
import com.freeandroapp.smsscheduler.models.Recipient;
import com.freeandroapp.smsscheduler.utils.DisplayImage;
import com.freeandroapp.smsscheduler.utils.Utils;
import com.freeandroapp.smsscheduler.R;

public class SelectContacts extends Activity {

    private TabHost mtabHost;
    private DBAdapter mdba = new DBAdapter(this);
    private Cursor cur;
    private LinearLayout listLayout;
    private LinearLayout blankLayout;
    private Button blankListAddButton;
    private LinearLayout recentsListLayout;
    private LinearLayout recentsBlankLayout;

    private DisplayImage displayImage = new DisplayImage();

    // ---------------- Variables relating to Contacts tab
    // -----------------------
    private ListView nativeContactsList;
    private EditText filterField;
    private ImageView clearFilterButton;
    private Button doneButton;
    private Button cancelButton;

    private ContactsAdapter contactsAdapter;
    private String origin;
    private ArrayList<Contact> sortedContacts = new ArrayList<Contact>(); // This
                                                                          // is
                                                                          // the
                                                                          // actual
                                                                          // Contact
                                                                          // ArrayList
                                                                          // used
                                                                          // to
                                                                          // display
                                                                          // contacts
                                                                          // in
                                                                          // ContactsTab.

    private ArrayList<Recipient> RecipientsTemp = new ArrayList<Recipient>();
    // ---------------------------------------------------------------------------

    // ----------- Variables related to Groups
    // Tab-------------------------------
    private ExpandableListView nativeGroupExplList;
    private ExpandableListView privateGroupExplList;
    private ArrayList<ArrayList<HashMap<String, Object>>> nativeChildDataTemp = new ArrayList<ArrayList<HashMap<String, Object>>>();
    private ArrayList<HashMap<String, Object>> nativeGroupDataTemp = new ArrayList<HashMap<String, Object>>();

    private ArrayList<ArrayList<HashMap<String, Object>>> privateChildDataTemp = new ArrayList<ArrayList<HashMap<String, Object>>>();
    private ArrayList<HashMap<String, Object>> privateGroupDataTemp = new ArrayList<HashMap<String, Object>>();
    private ArrayList<ArrayList<HashMap<String, Object>>> groupedPrivateChildDataTemp = new ArrayList<ArrayList<HashMap<String, Object>>>();

    private ArrayList<ArrayList<ArrayList<ContactNumber>>> nativeExtraNumbers = new ArrayList<ArrayList<ArrayList<ContactNumber>>>();

    private SimpleExpandableListAdapter nativeGroupAdapter;
    private SimpleExpandableListAdapter privateGroupAdapter;

    private boolean hasToRefresh;
    // -----------------------------------------------------------------------------

    // ----------------------Variables relating to Recents
    // Tab-----------------------
    private ArrayList<Long> recentIds = new ArrayList<Long>();
    private ArrayList<Long> recentContactIds = new ArrayList<Long>();
    private ArrayList<String> recentContactNumbers = new ArrayList<String>();
    private ListView recentsList;
    private RecentsAdapter recentsAdapter;

    // ------------------------------------------------------------------------------

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_contacts);

        for (int i = 0; i < SmsSchedulerApplication.contactsList.size(); i++) {
            sortedContacts.add(SmsSchedulerApplication.contactsList.get(i));
        }

        filterField = (EditText) findViewById(R.id.filter_text);
        clearFilterButton = (ImageView) findViewById(R.id.clear_filter_button);
        recentsList = (ListView) findViewById(R.id.contacts_tabs_recents_list);

        // ----------------------Setting up the
        // Tabs--------------------------------
        final TabHost tabHost = (TabHost) findViewById(R.id.tabHost);
        tabHost.setup();

        tabHost.getTabWidget().setDividerDrawable(R.drawable.vertical_seprator);

        TabSpec spec1 = tabHost.newTabSpec("Tab 1");
        spec1.setIndicator("Contacts", getResources().getDrawable(R.drawable.tab_icon_contacts));
        spec1.setContent(R.id.contacts_tab);

        TabSpec spec2 = tabHost.newTabSpec("Tab 2");
        spec2.setIndicator("Groups", getResources().getDrawable(R.drawable.tab_icon_group));
        spec2.setContent(R.id.group_tabs);

        TabSpec spec3 = tabHost.newTabSpec("Tab 3");
        spec3.setIndicator("Recents", getResources().getDrawable(R.drawable.tab_icon_recents));
        spec3.setContent(R.id.contacts_tabs_recents_layout);

        tabHost.addTab(spec1);
        tabHost.addTab(spec2);
        tabHost.addTab(spec3);

        for (int i = 0; i < tabHost.getTabWidget().getChildCount(); i++) {
            tabHost.getTabWidget().getChildAt(i).setBackgroundDrawable(getResources().getDrawable(R.drawable.tab_bg_selector));
        }
        // ----------------------------------------------------end of Tabs
        // Setup-----------

        /**
         * @details watches the text in the Filter field. Loops through the
         *          Contacts ArrayList, finds the matches for the text entered
         *          in Filter field, populates the 'sortedContacts' ArrayList
         *          with these matches and then notifies the Contacts list.
         */
        filterField.addTextChangedListener(new TextWatcher() {

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void afterTextChanged(Editable s) {
                if (s.equals("")) {
                    sortedContacts.clear();
                    for (int i = 0; i < SmsSchedulerApplication.contactsList.size(); i++) {
                        sortedContacts.add(SmsSchedulerApplication.contactsList.get(i));
                    }
                    contactsAdapter.notifyDataSetChanged();
                } else {
                    sortedContacts.clear();

                    String text1 = s.toString();
                    String text2 = "";
                    for (int i = 0; i < text1.length(); i++) {
                        if (!(text1.charAt(i) == '.' || text1.charAt(i) == '+' || text1.charAt(i) == '*' || text1.charAt(i) == '(' || text1.charAt(i) == ')' || text1.charAt(i) == '{' || text1.charAt(i) == '}' || text1.charAt(i) == '[' || text1.charAt(i) == ']' || text1.charAt(i) == '\\')) {
                            text2 = text2 + text1.charAt(i);
                        }
                    }

                    for (int i = 0; i < SmsSchedulerApplication.contactsList.size(); i++) {
                        Pattern p = Pattern.compile(text2, Pattern.CASE_INSENSITIVE);

                        Matcher m = p.matcher(SmsSchedulerApplication.contactsList.get(i).name);
                        if (m.find()) {
                            sortedContacts.add(SmsSchedulerApplication.contactsList.get(i));
                        } else {
                            for (int j = 0; j < SmsSchedulerApplication.contactsList.get(i).numbers.size(); j++) {
                                SmsSchedulerApplication.contactsList.get(i).numbers.get(j).number = Utils.refineNumber(SmsSchedulerApplication.contactsList.get(i).numbers.get(j).number);
                                m = p.matcher(SmsSchedulerApplication.contactsList.get(i).numbers.get(j).number);
                                if (m.find()) {
                                    sortedContacts.add(SmsSchedulerApplication.contactsList.get(i));
                                    break;
                                }
                            }
                        }
                    }
                    contactsAdapter.notifyDataSetChanged();
                }
            }
        });

        /**
         * @details clears the filter field and refreshes the Contacts list
         *          showing all the Contacts unfiltered.
         */
        clearFilterButton.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                filterField.setText("");
                filterField.setHint("Filter");
                sortedContacts.clear();
                for (int i = 0; i < SmsSchedulerApplication.contactsList.size(); i++) {
                    sortedContacts.add(SmsSchedulerApplication.contactsList.get(i));
                }
                contactsAdapter.notifyDataSetChanged();
            }
        });

        Intent intent = getIntent();
        origin = intent.getStringExtra("ORIGIN");

        // Loading the Native Groups data into data-structures in order to show
        // them in expandable list in Groups Tab.
        for (int i = 0; i < AbstractScheduleSms.Recipients.size(); i++) {
            RecipientsTemp.add(AbstractScheduleSms.Recipients.get(i));
        }
        for (int groupCount = 0; groupCount < AbstractScheduleSms.nativeGroupData.size(); groupCount++) {
            boolean hasAChild = false;
            HashMap<String, Object> group = new HashMap<String, Object>();
            group.put(Constants.GROUP_ID, AbstractScheduleSms.nativeGroupData.get(groupCount).get(Constants.GROUP_ID));
            group.put(Constants.GROUP_NAME, AbstractScheduleSms.nativeGroupData.get(groupCount).get(Constants.GROUP_NAME));
            group.put(Constants.GROUP_IMAGE, AbstractScheduleSms.nativeGroupData.get(groupCount).get(Constants.GROUP_IMAGE));
            group.put(Constants.GROUP_TYPE, AbstractScheduleSms.nativeGroupData.get(groupCount).get(Constants.GROUP_TYPE));
            group.put(Constants.GROUP_CHECK, false);

            ArrayList<HashMap<String, Object>> child = new ArrayList<HashMap<String, Object>>();
            for (int childCount = 0; childCount < AbstractScheduleSms.nativeChildData.get(groupCount).size(); childCount++) {

                HashMap<String, Object> childParams = new HashMap<String, Object>();
                childParams.put(Constants.CHILD_CONTACT_ID, AbstractScheduleSms.nativeChildData.get(groupCount).get(childCount).get(Constants.CHILD_CONTACT_ID));
                childParams.put(Constants.CHILD_NAME, AbstractScheduleSms.nativeChildData.get(groupCount).get(childCount).get(Constants.CHILD_NAME));
                childParams.put(Constants.CHILD_NUMBER, AbstractScheduleSms.nativeChildData.get(groupCount).get(childCount).get(Constants.CHILD_NUMBER));
                childParams.put(Constants.CHILD_IMAGE, AbstractScheduleSms.nativeChildData.get(groupCount).get(childCount).get(Constants.CHILD_IMAGE));
                childParams.put(Constants.CHILD_CHECK, AbstractScheduleSms.nativeChildData.get(groupCount).get(childCount).get(Constants.CHILD_CHECK));
                child.add(childParams);
                if ((Boolean) AbstractScheduleSms.nativeChildData.get(groupCount).get(childCount).get(Constants.CHILD_CHECK)) {
                    hasAChild = true;
                }
            }
            if (hasAChild) {
                group.put(Constants.GROUP_CHECK, true);
            }
            nativeGroupDataTemp.add(group);
            nativeChildDataTemp.add(child);
        }

        // Loading the Private Groups data into data-structures in order to show
        // them in expandable list in Groups Tab.
        for (int groupCount = 0; groupCount < AbstractScheduleSms.privateGroupData.size(); groupCount++) {
            boolean hasAChild = false;
            HashMap<String, Object> group = new HashMap<String, Object>();
            group.put(Constants.GROUP_ID, AbstractScheduleSms.privateGroupData.get(groupCount).get(Constants.GROUP_ID));
            group.put(Constants.GROUP_NAME, AbstractScheduleSms.privateGroupData.get(groupCount).get(Constants.GROUP_NAME));
            group.put(Constants.GROUP_IMAGE, AbstractScheduleSms.privateGroupData.get(groupCount).get(Constants.GROUP_IMAGE));
            group.put(Constants.GROUP_TYPE, AbstractScheduleSms.privateGroupData.get(groupCount).get(Constants.GROUP_TYPE));
            group.put(Constants.GROUP_CHECK, false);

            ArrayList<HashMap<String, Object>> child = new ArrayList<HashMap<String, Object>>();
            for (int childCount = 0; childCount < AbstractScheduleSms.privateChildData.get(groupCount).size(); childCount++) {

                HashMap<String, Object> childParams = new HashMap<String, Object>();
                childParams.put(Constants.CHILD_CONTACT_ID, AbstractScheduleSms.privateChildData.get(groupCount).get(childCount).get(Constants.CHILD_CONTACT_ID));
                childParams.put(Constants.CHILD_NAME, AbstractScheduleSms.privateChildData.get(groupCount).get(childCount).get(Constants.CHILD_NAME));
                childParams.put(Constants.CHILD_NUMBER, AbstractScheduleSms.privateChildData.get(groupCount).get(childCount).get(Constants.CHILD_NUMBER));
                childParams.put(Constants.CHILD_IMAGE, AbstractScheduleSms.privateChildData.get(groupCount).get(childCount).get(Constants.CHILD_IMAGE));
                childParams.put(Constants.CHILD_CHECK, AbstractScheduleSms.privateChildData.get(groupCount).get(childCount).get(Constants.CHILD_CHECK));
                child.add(childParams);
                if ((Boolean) AbstractScheduleSms.privateChildData.get(groupCount).get(childCount).get(Constants.CHILD_CHECK)) {
                    hasAChild = true;
                }
            }

            if (hasAChild) {
                group.put(Constants.GROUP_CHECK, true);
            }
            privateGroupDataTemp.add(group);
            privateChildDataTemp.add(child);
        }

        doneButton = (Button) findViewById(R.id.contacts_tab_done_button);
        cancelButton = (Button) findViewById(R.id.contacts_tab_cancel_button);

        doneButton.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                Intent intent = new Intent();
                AbstractScheduleSms.nativeGroupData.clear();
                AbstractScheduleSms.nativeChildData.clear();

                AbstractScheduleSms.nativeGroupData = nativeGroupDataTemp;
                AbstractScheduleSms.nativeChildData = nativeChildDataTemp;

                AbstractScheduleSms.privateGroupData.clear();
                AbstractScheduleSms.privateChildData.clear();

                AbstractScheduleSms.privateGroupData = privateGroupDataTemp;
                AbstractScheduleSms.privateChildData = privateChildDataTemp;

                AbstractScheduleSms.Recipients.clear();
                for (int i = 0; i < RecipientsTemp.size(); i++) {
                    AbstractScheduleSms.Recipients.add(RecipientsTemp.get(i));
                }

                setResult(2, intent);
                SelectContacts.this.finish();
            }
        });

        cancelButton.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                Intent intent = new Intent();
                setResult(2, intent);
                SelectContacts.this.finish();
            }
        });

        // ------------------Setting up the Contacts Tab
        // ---------------------------------------------
        nativeContactsList = (ListView) findViewById(R.id.contacts_tabs_native_contacts_list);
        contactsAdapter = new ContactsAdapter(this, sortedContacts);
        nativeContactsList.setAdapter(contactsAdapter);
        // ------------------------------------------------------------end of
        // setting up Contacts Tab--------

        listLayout = (LinearLayout) findViewById(R.id.list_layout);
        blankLayout = (LinearLayout) findViewById(R.id.blank_layout);
        blankListAddButton = (Button) findViewById(R.id.blank_list_add_button);

        blankListAddButton.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                Intent intent = new Intent(SelectContacts.this, ContactsList.class);
                intent.putExtra("ORIGINATOR", "Group Add Activity");
                startActivity(intent);
            }
        });

        mdba.open();
        cur = mdba.fetchAllGroups();
        if (cur.getCount() == 0) {
            listLayout.setVisibility(LinearLayout.GONE);
            blankLayout.setVisibility(LinearLayout.VISIBLE);
        } else {
            listLayout.setVisibility(LinearLayout.VISIBLE);
            blankLayout.setVisibility(LinearLayout.GONE);
        }
        cur.close();
        mdba.close();

        LinearLayout groupTabs = (LinearLayout) findViewById(R.id.group_tabs);
        mtabHost = (TabHost) groupTabs.findViewById(android.R.id.tabhost);

        mtabHost.setup();
        mtabHost.getTabWidget().setDividerDrawable(R.drawable.vertical_seprator);

        setupTab(new TextView(this), "Phone Groups");
        setupTab(new TextView(this), "My Groups");

        privateGroupExplList = (ExpandableListView) findViewById(R.id.private_list);
        nativeGroupExplList = (ExpandableListView) groupTabs.findViewById(R.id.native_list);

        groupedPrivateChildDataTemp = organizeChildData(privateGroupDataTemp, privateChildDataTemp);

        nativeGroupsAdapterSetup();
        privateGroupsAdapterSetup();

        nativeGroupExplList.setAdapter(nativeGroupAdapter);
        privateGroupExplList.setAdapter(privateGroupAdapter);
        // ----------------------------------------------------end of Groups Tab
        // setup-------------------

        // --------------------setting up Recents
        // Tab--------------------------------
        recentsListLayout = (LinearLayout) findViewById(R.id.contacts_tabs_recents_list_layout);
        recentsBlankLayout = (LinearLayout) findViewById(R.id.contacts_tabs_recents_blank_layout);

        mdba.open();
        Cursor cur = mdba.fetchAllRecents();
        startManagingCursor(cur);
        if (cur.getCount() == 0) {
            recentsListLayout.setVisibility(LinearLayout.GONE);
            recentsBlankLayout.setVisibility(LinearLayout.VISIBLE);
        } else {
            recentsBlankLayout.setVisibility(LinearLayout.GONE);
            recentsListLayout.setVisibility(LinearLayout.VISIBLE);
            recentIds.clear();
            recentContactIds.clear();
            recentContactNumbers.clear();

            if (cur.moveToFirst()) {
                do {
                    recentIds.add(cur.getLong(cur.getColumnIndex(DBAdapter.KEY_RECENT_CONTACT_ID)));
                    recentContactIds.add(cur.getLong(cur.getColumnIndex(DBAdapter.KEY_RECENT_CONTACT_CONTACT_ID)));
                    recentContactNumbers.add(cur.getString(cur.getColumnIndex(DBAdapter.KEY_RECENT_CONTACT_NUMBER)));
                } while (cur.moveToNext());
            }
            recentsAdapter = new RecentsAdapter();
            recentsList.setAdapter(recentsAdapter);
        }
        mdba.close();
        // ---------------------------------------------------------------------------
    }

    /**
     * @details Organizes the Group Data into a convenient structure that fits
     *          the Expandable List which has extra views for each number.
     * @param privateGroupData
     * @param privateChildData
     * @return new DataStructure.
     */
    public ArrayList<ArrayList<HashMap<String, Object>>> organizeChildData(ArrayList<HashMap<String, Object>> privateGroupData, ArrayList<ArrayList<HashMap<String, Object>>> privateChildData) {
        ArrayList<ArrayList<HashMap<String, Object>>> data = new ArrayList<ArrayList<HashMap<String, Object>>>();

        for (int groupCount = 0; groupCount < privateGroupData.size(); groupCount++) {
            ArrayList<HashMap<String, Object>> groupMembers = new ArrayList<HashMap<String, Object>>();
            for (int childCount = 0; childCount < privateChildData.get(groupCount).size(); childCount++) {
                boolean isPresent = false;
                for (int i = 0; i < groupMembers.size(); i++) {
                    if (groupMembers.get(i).get(Constants.CHILD_CONTACT_ID).equals(privateChildData.get(groupCount).get(childCount).get(Constants.CHILD_CONTACT_ID))) {
                        isPresent = true;
                    }
                }
                if (!isPresent) {
                    HashMap<String, Object> child = new HashMap<String, Object>();
                    child.put(Constants.CHILD_NAME, privateChildData.get(groupCount).get(childCount).get(Constants.CHILD_NAME));
                    child.put(Constants.CHILD_CONTACT_ID, privateChildData.get(groupCount).get(childCount).get(Constants.CHILD_CONTACT_ID));
                    ArrayList<ContactNumber> numbers = new ArrayList<ContactNumber>();
                    ContactNumber number = new ContactNumber((Long) child.get(Constants.CHILD_CONTACT_ID), (String) privateChildData.get(groupCount).get(childCount).get(Constants.CHILD_NUMBER), getType((Long) child.get(Constants.CHILD_CONTACT_ID), (String) privateChildData.get(groupCount).get(childCount).get(Constants.CHILD_NUMBER)));
                    numbers.add(number);
                    for (int childCountExt = childCount + 1; childCountExt < privateChildData.get(groupCount).size(); childCountExt++) {

                        if (((Long) privateChildData.get(groupCount).get(childCount).get(Constants.CHILD_CONTACT_ID)).equals((Long) privateChildData.get(groupCount).get(childCount).get(Constants.CHILD_CONTACT_ID))) {
                            number = new ContactNumber((Long) child.get(Constants.CHILD_CONTACT_ID), (String) privateChildData.get(groupCount).get(childCountExt).get(Constants.CHILD_NUMBER), getType((Long) child.get(Constants.CHILD_CONTACT_ID), (String) privateChildData.get(groupCount).get(childCountExt).get(Constants.CHILD_NUMBER)));
                            if (number.type != null)
                                numbers.add(number);
                        }
                    }
                    child.put(Constants.CHILD_NUMBER, numbers);
                    groupMembers.add(child);
                }
            }
            data.add(groupMembers);
        }

        return data;
    }

    /**
     * @details finds the phone-number type of a particular number of a
     *          particular contact
     * @param id
     * @param number
     * @return phone-number type string.
     */
    private String getType(long id, String number) {
        for (int i = 0; i < SmsSchedulerApplication.contactsList.size(); i++) {
            if (SmsSchedulerApplication.contactsList.get(i).content_uri_id == id) {
                for (int j = 0; j < SmsSchedulerApplication.contactsList.get(i).numbers.size(); j++) {
                    if (number.equals(SmsSchedulerApplication.contactsList.get(i).numbers.get(j).number)) {
                        return SmsSchedulerApplication.contactsList.get(i).numbers.get(j).type;
                    }
                }
            }
        }
        return null;
    }

    /**
     * @details creates and sets up tabs for the nested tab-widget (Phone Groups
     *          | My Groups)
     * @param view
     * @param tag
     */
    private void setupTab(final View view, final String tag) {
        View tabview = createTabView(mtabHost.getContext(), tag);
        TabSpec setContent = null;
        if (tag.equals("Phone Groups")) {
            setContent = mtabHost.newTabSpec(tag).setIndicator(tabview).setContent(R.id.native_list);
        } else if (tag.equals("My Groups")) {
            setContent = mtabHost.newTabSpec(tag).setIndicator(tabview).setContent(R.id.private_list_parent_layout);
        }
        mtabHost.addTab(setContent);
    }

    /**
     * @details creates a Tab View using a custom layout.
     * @param context
     * @param text
     * @return Tab View.
     */
    private View createTabView(final Context context, final String text) {
        View view = LayoutInflater.from(context).inflate(R.layout.tabs_bg, null);
        TextView tv = (TextView) view.findViewById(R.id.tabsText);
        tv.setText(text);
        return view;
    }

    protected void onPause() {
        super.onPause();
        // case: when there's no Private Group and screen is navigated to Create
        // New Group module, a bit has to be set in order to
        // refresh the "My Groups" body when the screen returns back.
        if (privateGroupDataTemp.size() == 0) {
            hasToRefresh = true;
        } else {
            hasToRefresh = false;
        }
    }

    protected void onResume() {
        super.onResume();
        if (hasToRefresh) {
            // case: when the screen needs a refresh after addition of a private
            // group.
            mdba.open();
            cur = mdba.fetchAllGroups();
            if (cur.getCount() == 0) {
                listLayout.setVisibility(LinearLayout.GONE);
                blankLayout.setVisibility(LinearLayout.VISIBLE);
            } else {
                listLayout.setVisibility(LinearLayout.VISIBLE);
                blankLayout.setVisibility(LinearLayout.GONE);
            }
            mdba.close();
            reloadPrivateGroupData();
            privateGroupAdapter.notifyDataSetChanged();
            hasToRefresh = false;
        }
    }

    public void onBackPressed() {
        super.onBackPressed();

        Intent intent = new Intent();
        setResult(2, intent);

        SelectContacts.this.finish();
    }

    // ************************* Adapter for the list
    // *****************************************
    // **************************** in Contacts Tab
    // ********************************************

    private class ContactsAdapter extends ArrayAdapter<Contact> implements
            SectionIndexer {

        HashMap<String, Integer> alphaIndexer;
        String[] sections;
        ArrayList<Contact> contacts;

        ContactsAdapter(Context context, ArrayList<Contact> _contacts) {
            super(SelectContacts.this, R.layout.contacts_list_row, _contacts);

            // For the implementation of SectionIndexer.
            contacts = _contacts;

            alphaIndexer = new HashMap<String, Integer>();
            int size = contacts.size();

            for (int x = 0; x < size; x++) {
                Contact c = contacts.get(x);
                String ch = c.name.substring(0, 1);
                ch = ch.toUpperCase();
                alphaIndexer.put(ch, x);
            }

            Set<String> sectionLetters = alphaIndexer.keySet();
            ArrayList<String> sectionList = new ArrayList<String>(sectionLetters);

            Collections.sort(sectionList);

            sections = new String[sectionList.size()];

            sectionList.toArray(sections);
            // -------------------------------------------
        }

        public View getView(final int position, View convertView, ViewGroup parent) {
            final ContactsListHolder holder;
            if (convertView == null) {
                LayoutInflater inflater = getLayoutInflater();
                convertView = inflater.inflate(R.layout.contacts_list_row, parent, false);
                holder = new ContactsListHolder();
                holder.contactImage = (ImageView) convertView.findViewById(R.id.contact_list_row_contact_pic);
                holder.nameText = (TextView) convertView.findViewById(R.id.contact_list_row_contact_name);
                holder.numberText = (TextView) convertView.findViewById(R.id.contact_list_row_contact_number);
                holder.contactCheck = (CheckBox) convertView.findViewById(R.id.contact_list_row_contact_check);
                holder.primaryContactLayout = (RelativeLayout) convertView.findViewById(R.id.contact_list_primary_contact_space);
            } else {
                holder = (ContactsListHolder) convertView.getTag();
            }

            displayImage.submitImage(holder.contactImage, contacts.get(position).content_uri_id, SelectContacts.this);

            holder.nameText.setText(contacts.get(position).name);
            holder.numberText.setText(contacts.get(position).numbers.get(0).type + ": " + contacts.get(position).numbers.get(0).number);

            holder.extraContactsLayout = (LinearLayout) convertView.findViewById(R.id.extra_numbers_layout);
            holder.extraContactsViews = new ArrayList<View>();

            if (contacts.get(position).numbers.size() > 1) {
                // case: when a contact has multiple numbers, an extra view is
                // created for each extra number and is shown below the
                // Contacts view in the list.
                holder.extraContactsLayout.setVisibility(View.VISIBLE);
                holder.extraContactsLayout.removeAllViews();
                holder.extraContactsViews.clear();
                ArrayList<ContactNumber> extraNumbers = new ArrayList<ContactNumber>();

                // Load extra numbers into a data structure.
                for (int i = 1; i < contacts.get(position).numbers.size(); i++) {
                    extraNumbers.add(contacts.get(position).numbers.get(i));
                }

                // For each extra number, create a View. Store these views in a
                // data structure.
                for (int i = 0; i < extraNumbers.size(); i++) {
                    View view = createView(extraNumbers.get(i), contacts.get(position), getLayoutInflater());
                    holder.extraContactsViews.add(view);
                }

                // Add all the extra views to the extraContactsLayout for that
                // Contact.
                for (int i = 0; i < holder.extraContactsViews.size(); i++) {
                    holder.extraContactsLayout.addView(holder.extraContactsViews.get(i));
                }
            } else {
                holder.extraContactsLayout.setVisibility(View.GONE);
            }

            convertView.setTag(holder);

            for (int i = 0; i < RecipientsTemp.size(); i++) {
                if (contacts.get(position).content_uri_id == RecipientsTemp.get(i).contactId && contacts.get(position).numbers.get(0).number.equals(RecipientsTemp.get(i).number)) {
                    holder.contactCheck.setChecked(true);
                    break;
                } else {
                    holder.contactCheck.setChecked(false);
                }
            }

            holder.contactCheck.setOnClickListener(new OnClickListener() {

                public void onClick(View v) {
                    if (holder.contactCheck.isChecked()) {
                        boolean isPresent = false;
                        for (int i = 0; i < RecipientsTemp.size(); i++) {
                            if (RecipientsTemp.get(i).contactId == contacts.get(position).content_uri_id && contacts.get(position).numbers.get(0).number.equals(RecipientsTemp.get(i).number)) {
                                isPresent = true;
                                break;
                            }
                        }
                        if (!isPresent) {
                            HashMap<String, String> params = new HashMap<String, String>();
                            params.put("From", "Contacts List");
                            params.put("Is Primary Number", "yes");

                            Recipient recipient = new Recipient(-1, 2, contacts.get(position).name, contacts.get(position).content_uri_id, -1, -1, -1, contacts.get(position).numbers.get(0).number);
                            recipient.groupIds.add((long) -1);
                            recipient.groupTypes.add(-1);
                            RecipientsTemp.add(recipient);
                            recentsList.setAdapter(new RecentsAdapter());
                        }
                    } else {
                        for (int i = 0; i < RecipientsTemp.size(); i++) {
                            if (contacts.get(position).content_uri_id == RecipientsTemp.get(i).contactId && contacts.get(position).numbers.get(0).number.equals(RecipientsTemp.get(i).number)) {
                                for (int j = 0; j < nativeGroupDataTemp.size(); j++) {
                                    int noOfChecks = 0;
                                    for (int k = 0; k < nativeChildDataTemp.get(j).size(); k++) {
                                        if ((Long) nativeChildDataTemp.get(j).get(k).get(Constants.CHILD_CONTACT_ID) == RecipientsTemp.get(i).contactId) {
                                            nativeChildDataTemp.get(j).get(k).put(Constants.CHILD_CHECK, false);
                                        }
                                        if ((Boolean) nativeChildDataTemp.get(j).get(k).get(Constants.CHILD_CHECK)) {
                                            noOfChecks = 1;
                                        }
                                    }
                                    if (noOfChecks > 0) {
                                        nativeGroupDataTemp.get(j).put(Constants.GROUP_CHECK, true);
                                    } else {
                                        nativeGroupDataTemp.get(j).put(Constants.GROUP_CHECK, false);
                                    }
                                }
                                for (int j = 0; j < privateGroupDataTemp.size(); j++) {
                                    int noOfChecks = 0;
                                    for (int k = 0; k < privateChildDataTemp.get(j).size(); k++) {
                                        if ((Long) privateChildDataTemp.get(j).get(k).get(Constants.CHILD_CONTACT_ID) == RecipientsTemp.get(i).contactId) {
                                            privateChildDataTemp.get(j).get(k).put(Constants.CHILD_CHECK, false);
                                        }
                                        if ((Boolean) privateChildDataTemp.get(j).get(k).get(Constants.CHILD_CHECK)) {
                                            noOfChecks = 1;
                                        }
                                    }
                                    if (noOfChecks > 0) {
                                        privateGroupDataTemp.get(j).put(Constants.GROUP_CHECK, true);
                                    } else {
                                        privateGroupDataTemp.get(j).put(Constants.GROUP_CHECK, false);
                                    }
                                }
                                HashMap<String, String> params = new HashMap<String, String>();
                                params.put("From", "Contacts List");

                                RecipientsTemp.remove(i);

                                nativeGroupAdapter.notifyDataSetChanged();
                                privateGroupAdapter.notifyDataSetChanged();
                                recentsList.setAdapter(new RecentsAdapter());
                            }
                        }
                    }
                }
            });

            holder.primaryContactLayout.setOnClickListener(new OnClickListener() {

                public void onClick(View v) {
                    if (!holder.contactCheck.isChecked()) {
                        holder.contactCheck.setChecked(true);
                        boolean isPresent = false;
                        for (int i = 0; i < RecipientsTemp.size(); i++) {
                            if (RecipientsTemp.get(i).contactId == contacts.get(position).content_uri_id && contacts.get(position).numbers.get(0).number.equals(RecipientsTemp.get(i).number)) {
                                isPresent = true;
                                break;
                            }
                        }
                        if (!isPresent) {
                            HashMap<String, String> params = new HashMap<String, String>();
                            params.put("From", "Contacts List");
                            params.put("Is Primary Number", "yes");

                            Recipient recipient = new Recipient(-1, 2, contacts.get(position).name, contacts.get(position).content_uri_id, -1, -1, -1, contacts.get(position).numbers.get(0).number);
                            recipient.groupIds.add((long) -1);
                            recipient.groupTypes.add(-1);
                            RecipientsTemp.add(recipient);
                            recentsList.setAdapter(new RecentsAdapter());
                        }
                    } else {
                        holder.contactCheck.setChecked(false);
                        for (int i = 0; i < RecipientsTemp.size(); i++) {
                            if (contacts.get(position).content_uri_id == RecipientsTemp.get(i).contactId && contacts.get(position).numbers.get(0).number.equals(RecipientsTemp.get(i).number)) {
                                for (int j = 0; j < nativeGroupDataTemp.size(); j++) {
                                    int noOfChecks = 0;
                                    for (int k = 0; k < nativeChildDataTemp.get(j).size(); k++) {
                                        if ((Long) nativeChildDataTemp.get(j).get(k).get(Constants.CHILD_CONTACT_ID) == RecipientsTemp.get(i).contactId) {
                                            nativeChildDataTemp.get(j).get(k).put(Constants.CHILD_CHECK, false);
                                        }
                                        if ((Boolean) nativeChildDataTemp.get(j).get(k).get(Constants.CHILD_CHECK)) {
                                            noOfChecks = 1;
                                        }
                                    }
                                    if (noOfChecks > 0) {
                                        nativeGroupDataTemp.get(j).put(Constants.GROUP_CHECK, true);
                                    } else {
                                        nativeGroupDataTemp.get(j).put(Constants.GROUP_CHECK, false);
                                    }
                                }
                                for (int j = 0; j < privateGroupDataTemp.size(); j++) {
                                    int noOfChecks = 0;
                                    for (int k = 0; k < privateChildDataTemp.get(j).size(); k++) {
                                        if ((Long) privateChildDataTemp.get(j).get(k).get(Constants.CHILD_CONTACT_ID) == RecipientsTemp.get(i).contactId) {
                                            privateChildDataTemp.get(j).get(k).put(Constants.CHILD_CHECK, false);
                                        }
                                        if ((Boolean) privateChildDataTemp.get(j).get(k).get(Constants.CHILD_CHECK)) {
                                            noOfChecks = 1;
                                        }
                                    }
                                    if (noOfChecks > 0) {
                                        privateGroupDataTemp.get(j).put(Constants.GROUP_CHECK, true);
                                    } else {
                                        privateGroupDataTemp.get(j).put(Constants.GROUP_CHECK, false);
                                    }
                                }
                                HashMap<String, String> params = new HashMap<String, String>();
                                params.put("From", "Contacts List");

                                RecipientsTemp.remove(i);

                                nativeGroupAdapter.notifyDataSetChanged();
                                privateGroupAdapter.notifyDataSetChanged();
                                recentsList.setAdapter(new RecentsAdapter());
                            }
                        }
                    }
                    if (recentsAdapter != null)
                        recentsAdapter.notifyDataSetChanged();
                }
            });

            return convertView;
        }

        public int getPositionForSection(int section) {
            return alphaIndexer.get(sections[section]);
        }

        public int getSectionForPosition(int position) {
            return 1;
        }

        public Object[] getSections() {
            return sections;
        }
    }

    // ************************************************************** end of
    // ContactsAdapter******************

    /**
     * @details Creates an Extra Number View based on the contactNumber and
     *          Contact. Also sets the click listeners for the Views.
     * @param contactNumber
     * @param contact
     * @param inflater
     * @return Fully functional View that shows a contact number, number-type
     *         and a checkBox.
     */
    public View createView(final ContactNumber contactNumber, final Contact contact, LayoutInflater inflater) {

        View view = inflater.inflate(R.layout.extra_numbers_list_row, null);

        TextView tv = (TextView) view.findViewById(R.id.extra_number);
        final CheckBox cb = (CheckBox) view.findViewById(R.id.extra_number_checkbox);

        tv.setText(contactNumber.type + ": " + contactNumber.number);

        for (int i = 0; i < RecipientsTemp.size(); i++) {
            if (contactNumber.contactId == RecipientsTemp.get(i).contactId && contactNumber.number.equals(RecipientsTemp.get(i).number)) {
                cb.setChecked(true);
                break;
            } else {
                cb.setChecked(false);
            }
        }

        view.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                if (!cb.isChecked()) {
                    cb.setChecked(true);
                    boolean isPresent = false;
                    for (int i = 0; i < RecipientsTemp.size(); i++) {
                        if (RecipientsTemp.get(i).contactId == contactNumber.contactId && RecipientsTemp.get(i).number.equals(contactNumber.number)) {
                            isPresent = true;
                            break;
                        }
                    }
                    if (!isPresent) {
                        int k;
                        for (k = 0; k < SmsSchedulerApplication.contactsList.size(); k++) {
                            if (SmsSchedulerApplication.contactsList.get(k).content_uri_id == contactNumber.contactId) {
                                break;
                            }
                        }

                        HashMap<String, String> params = new HashMap<String, String>();
                        params.put("From", "Contacts List");
                        params.put("Is Primary Number", "no");

                        Recipient recipient = new Recipient(-1, 2, SmsSchedulerApplication.contactsList.get(k).name, contactNumber.contactId, -1, -1, -1, contactNumber.number);
                        recipient.groupIds.add((long) -1);
                        recipient.groupTypes.add(-1);
                        RecipientsTemp.add(recipient);
                        recentsList.setAdapter(new RecentsAdapter());
                    }
                } else {
                    HashMap<String, String> params = new HashMap<String, String>();
                    params.put("From", "Contacts List");

                    cb.setChecked(false);
                    for (int i = 0; i < RecipientsTemp.size(); i++) {
                        if (contactNumber.contactId == RecipientsTemp.get(i).contactId && contactNumber.number.equals(RecipientsTemp.get(i).number)) {
                            RecipientsTemp.remove(i);
                            recentsList.setAdapter(new RecentsAdapter());
                        }
                    }
                }
            }
        });

        cb.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {

                if (cb.isChecked()) {
                    boolean isPresent = false;
                    for (int i = 0; i < RecipientsTemp.size(); i++) {
                        if (RecipientsTemp.get(i).contactId == contactNumber.contactId && RecipientsTemp.get(i).number.equals(contactNumber.number)) {
                            isPresent = true;
                            break;
                        }
                    }
                    if (!isPresent) {
                        int k;
                        for (k = 0; k < SmsSchedulerApplication.contactsList.size(); k++) {
                            if (SmsSchedulerApplication.contactsList.get(k).content_uri_id == contactNumber.contactId) {
                                break;
                            }
                        }

                        HashMap<String, String> params = new HashMap<String, String>();
                        params.put("From", "Contacts List");
                        params.put("Is Primary Number", "no");

                        Recipient recipient = new Recipient(-1, 2, SmsSchedulerApplication.contactsList.get(k).name, contactNumber.contactId, -1, -1, -1, contactNumber.number);
                        recipient.groupIds.add((long) -1);
                        recipient.groupTypes.add(-1);
                        RecipientsTemp.add(recipient);
                        recentsList.setAdapter(new RecentsAdapter());
                    }
                } else {
                    HashMap<String, String> params = new HashMap<String, String>();
                    params.put("From", "Contacts List");

                    for (int i = 0; i < RecipientsTemp.size(); i++) {
                        if (contactNumber.contactId == RecipientsTemp.get(i).contactId && contactNumber.number.equals(RecipientsTemp.get(i).number)) {
                            RecipientsTemp.remove(i);
                            recentsList.setAdapter(new RecentsAdapter());
                        }
                    }
                }
            }
        });

        return view;
    }

    /**
     * @details Adapter for List of Native Groups.
     */
    private void nativeGroupsAdapterSetup() {

        final LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        nativeGroupAdapter = new SimpleExpandableListAdapter(this, nativeGroupDataTemp, android.R.layout.simple_expandable_list_item_1, new String[] { Constants.GROUP_NAME }, new int[] { android.R.id.text1 }, nativeChildDataTemp, 0, null, new int[] {}) {

            public Object getChild(int groupPosition, int childPosition) {
                return nativeChildDataTemp.get(groupPosition).get(childPosition);
            }

            public long getChildId(int groupPosition, int childPosition) {
                long id = childPosition;
                for (int i = 0; i < groupPosition; i++) {
                    id += nativeChildDataTemp.get(groupPosition).size();
                }
                return id;
            }

            public int getChildrenCount(int groupPosition) {
                return nativeChildDataTemp.get(groupPosition).size();
            }

            public Object getGroup(int groupPosition) {
                return nativeChildDataTemp.get(groupPosition);
            }

            public int getGroupCount() {
                return nativeGroupDataTemp.size();
            }

            public long getGroupId(int groupPosition) {
                return groupPosition;
            }

            public View getGroupView(final int groupPosition, final boolean isExpanded, View convertView, ViewGroup parent) {
                final GroupListHolder holder;
                if (convertView == null) {
                    LayoutInflater li = getLayoutInflater();
                    convertView = li.inflate(R.layout.select_contacts_expandable_list_group, null);
                    holder = new GroupListHolder();
                    holder.groupHeading = (TextView) convertView.findViewById(R.id.group_expl_list_group_row_group_name);
                    holder.groupCheck = (CheckBox) convertView.findViewById(R.id.group_expl_list_group_row_group_check);
                    convertView.setTag(holder);
                } else {
                    holder = (GroupListHolder) convertView.getTag();
                }
                holder.groupHeading.setText((String) nativeGroupDataTemp.get(groupPosition).get(Constants.GROUP_NAME));
                holder.groupCheck.setChecked((Boolean) nativeGroupDataTemp.get(groupPosition).get(Constants.GROUP_CHECK));

                nativeExtraNumbers.add(new ArrayList<ArrayList<ContactNumber>>());

                holder.groupCheck.setOnClickListener(new OnClickListener() {

                    public void onClick(View v) {
                        if (holder.groupCheck.isChecked()) {
                            nativeGroupDataTemp.get(groupPosition).put(Constants.GROUP_CHECK, true);
                            for (int i = 0; i < nativeChildDataTemp.get(groupPosition).size(); i++) {
                                if (!((Boolean) nativeChildDataTemp.get(groupPosition).get(i).get(Constants.CHILD_CHECK))) {
                                    addCheck(groupPosition, i, nativeChildDataTemp, nativeGroupDataTemp);
                                }
                            }
                            nativeGroupAdapter.notifyDataSetChanged();
                            contactsAdapter = new ContactsAdapter(SelectContacts.this, sortedContacts);
                            nativeContactsList.setAdapter(contactsAdapter);
                        } else {
                            nativeGroupDataTemp.get(groupPosition).put(Constants.GROUP_CHECK, false);
                            for (int i = 0; i < nativeChildDataTemp.get(groupPosition).size(); i++) {
                                if ((Boolean) nativeChildDataTemp.get(groupPosition).get(i).get(Constants.CHILD_CHECK)) {
                                    HashMap<String, String> params = new HashMap<String, String>();
                                    params.put("From", "Native Group");

                                    removeCheck(groupPosition, i, nativeChildDataTemp, nativeGroupDataTemp);
                                }
                            }
                            nativeGroupAdapter.notifyDataSetChanged();
                            contactsAdapter = new ContactsAdapter(SelectContacts.this, sortedContacts);
                            nativeContactsList.setAdapter(contactsAdapter);
                        }
                    }
                });

                convertView.setOnClickListener(new OnClickListener() {

                    public void onClick(View v) {
                        if (isExpanded) {
                            nativeGroupExplList.collapseGroup(groupPosition);
                        } else {
                            nativeGroupExplList.expandGroup(groupPosition);
                        }
                    }
                });
                return convertView;
            }

            public android.view.View getChildView(final int groupPosition, final int childPosition, boolean isLastChild, android.view.View convertView, android.view.ViewGroup parent) {

                final ChildListHolder holder;
                if (convertView == null) {
                    convertView = layoutInflater.inflate(R.layout.contacts_list_row, null, false);
                    holder = new ChildListHolder();
                    holder.childNameText = (TextView) convertView.findViewById(R.id.contact_list_row_contact_name);
                    holder.childContactImage = (ImageView) convertView.findViewById(R.id.contact_list_row_contact_pic);
                    holder.childNumberText = (TextView) convertView.findViewById(R.id.contact_list_row_contact_number);
                    holder.childCheck = (CheckBox) convertView.findViewById(R.id.contact_list_row_contact_check);
                    holder.primaryNumberLayout = (RelativeLayout) convertView.findViewById(R.id.contact_list_primary_contact_space);

                    convertView.setTag(holder);
                } else {
                    holder = (ChildListHolder) convertView.getTag();
                }

                long contactId = (Long) nativeChildDataTemp.get(groupPosition).get(childPosition).get(Constants.CHILD_CONTACT_ID);

                Contact contact = null;
                for (int i = 0; i < SmsSchedulerApplication.contactsList.size(); i++) {
                    if (SmsSchedulerApplication.contactsList.get(i).content_uri_id == contactId) {
                        contact = SmsSchedulerApplication.contactsList.get(i);
                    }
                }

                holder.childNameText.setText((String) nativeChildDataTemp.get(groupPosition).get(childPosition).get(Constants.CHILD_NAME));
                holder.childNumberText.setText(contact.numbers.get(0).type + ": " + (String) nativeChildDataTemp.get(groupPosition).get(childPosition).get(Constants.CHILD_NUMBER));

                holder.childContactImage.setImageBitmap((Bitmap) nativeChildDataTemp.get(groupPosition).get(childPosition).get(Constants.CHILD_IMAGE));
                holder.childCheck.setChecked((Boolean) nativeChildDataTemp.get(groupPosition).get(childPosition).get(Constants.CHILD_CHECK));

                holder.extraNumbersLayout = (LinearLayout) convertView.findViewById(R.id.extra_numbers_layout);

                nativeExtraNumbers.get(groupPosition).add(new ArrayList<ContactNumber>());

                ArrayList<ContactNumber> prunedList = new ArrayList<ContactNumber>(); // to
                                                                                      // store
                                                                                      // the
                                                                                      // numbers
                                                                                      // from
                                                                                      // 1
                                                                                      // to
                                                                                      // last.
                for (int i = 1; i < contact.numbers.size(); i++) {
                    prunedList.add(contact.numbers.get(i));
                }
                if (prunedList.size() > 0) {
                    holder.extraNumbersLayout.setVisibility(View.VISIBLE);
                    holder.extraNumbersLayout.removeAllViews();

                    // Create a view for each extra number and add it to the
                    // extraNumbersLayout.
                    for (int i = 0; i < prunedList.size(); i++) {
                        View view = createNativeExtraNumberView(groupPosition, childPosition, prunedList.get(i), contact, getLayoutInflater(), (Long) nativeGroupDataTemp.get(groupPosition).get(Constants.GROUP_ID));
                        holder.extraNumbersLayout.addView(view);
                    }
                } else {
                    holder.extraNumbersLayout.setVisibility(View.GONE);
                }

                holder.childCheck.setOnClickListener(new OnClickListener() {

                    public void onClick(View v) {
                        if (holder.childCheck.isChecked()) {
                            HashMap<String, String> params = new HashMap<String, String>();
                            params.put("From", "Native Group");
                            params.put("Is Primary Number", "yes");

                            addCheck(groupPosition, childPosition, nativeChildDataTemp, nativeGroupDataTemp);
                            contactsAdapter = new ContactsAdapter(SelectContacts.this, sortedContacts);
                            nativeContactsList.setAdapter(contactsAdapter);
                            boolean areAllSelected = true;
                            for (int i = 0; i < nativeChildDataTemp.get(groupPosition).size(); i++) {
                                if (!((Boolean) nativeChildDataTemp.get(groupPosition).get(i).get(Constants.CHILD_CHECK))) {
                                    areAllSelected = false;
                                    break;
                                }
                            }
                            if (areAllSelected) {
                                nativeGroupDataTemp.get(groupPosition).put(Constants.GROUP_CHECK, true);
                                nativeGroupAdapter.notifyDataSetChanged();
                            }
                        } else {
                            HashMap<String, String> params = new HashMap<String, String>();
                            params.put("From", "Native Group");

                            removeCheck(groupPosition, childPosition, nativeChildDataTemp, nativeGroupDataTemp);
                            contactsAdapter = new ContactsAdapter(SelectContacts.this, sortedContacts);
                            nativeContactsList.setAdapter(contactsAdapter);
                            boolean areAllDeselected = true;
                            for (int i = 0; i < nativeChildDataTemp.get(groupPosition).size(); i++) {
                                if ((Boolean) nativeChildDataTemp.get(groupPosition).get(i).get(Constants.CHILD_CHECK)) {
                                    areAllDeselected = false;
                                    break;
                                }
                            }
                            if (areAllDeselected) {
                                nativeGroupDataTemp.get(groupPosition).put(Constants.GROUP_CHECK, false);
                                nativeGroupAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                });

                holder.primaryNumberLayout.setOnClickListener(new OnClickListener() {

                    public void onClick(View v) {
                        if (holder.childCheck.isChecked()) {
                            holder.childCheck.setChecked(false);

                            HashMap<String, String> params = new HashMap<String, String>();
                            params.put("From", "Native Group");

                            removeCheck(groupPosition, childPosition, nativeChildDataTemp, nativeGroupDataTemp);
                            contactsAdapter = new ContactsAdapter(SelectContacts.this, sortedContacts);
                            nativeContactsList.setAdapter(contactsAdapter);
                            boolean areAllDeselected = true;
                            for (int i = 0; i < nativeChildDataTemp.get(groupPosition).size(); i++) {
                                if ((Boolean) nativeChildDataTemp.get(groupPosition).get(i).get(Constants.CHILD_CHECK)) {
                                    areAllDeselected = false;
                                    break;
                                }
                            }
                            if (areAllDeselected) {
                                nativeGroupDataTemp.get(groupPosition).put(Constants.GROUP_CHECK, false);
                                nativeGroupAdapter.notifyDataSetChanged();
                            }
                        } else {
                            holder.childCheck.setChecked(true);

                            HashMap<String, String> params = new HashMap<String, String>();
                            params.put("From", "Native Group");
                            params.put("Is Primary Number", "yes");

                            addCheck(groupPosition, childPosition, nativeChildDataTemp, nativeGroupDataTemp);
                            contactsAdapter = new ContactsAdapter(SelectContacts.this, sortedContacts);
                            nativeContactsList.setAdapter(contactsAdapter);
                            boolean areAllSelected = true;
                            for (int i = 0; i < nativeChildDataTemp.get(groupPosition).size(); i++) {
                                if (!((Boolean) nativeChildDataTemp.get(groupPosition).get(i).get(Constants.CHILD_CHECK))) {
                                    areAllSelected = false;
                                    break;
                                }
                            }
                            if (areAllSelected) {
                                nativeGroupDataTemp.get(groupPosition).put(Constants.GROUP_CHECK, true);
                                nativeGroupAdapter.notifyDataSetChanged();
                            }
                        }
                    }
                });

                return convertView;
            }

            public boolean areAllItemsEnabled() {
                return true;
            }

            public boolean hasStableIds() {
                return false;
            }

            public boolean isChildSelectable(int groupPosition, int childPosition) {
                return true;
            }
        };
    }

    /**
     * @details creates an ExtraNumberView for a Number based on the details of
     *          the Contact in the Group's Data.
     * @param groupPosition
     * @param childPosition
     * @param contactNumber
     * @param contact
     * @param inflater
     * @param groupId
     * @return fully functional View with Number, Number-Type and a CheckBox.
     */
    public View createNativeExtraNumberView(final int groupPosition, final int childPosition, final ContactNumber contactNumber, final Contact contact, LayoutInflater inflater, final long groupId) {

        View view = inflater.inflate(R.layout.extra_numbers_list_row, null);

        TextView tv = (TextView) view.findViewById(R.id.extra_number);
        final CheckBox cb = (CheckBox) view.findViewById(R.id.extra_number_checkbox);

        tv.setText(contactNumber.type + ": " + contactNumber.number);

        boolean gotChecked = false;
        for (int i = 0; i < RecipientsTemp.size() && !gotChecked; i++) {

            if (contactNumber.contactId == RecipientsTemp.get(i).contactId && contactNumber.number.equals(RecipientsTemp.get(i).number)) {
                for (int j = 0; j < RecipientsTemp.get(i).groupIds.size(); j++) {
                    if (RecipientsTemp.get(i).groupIds.get(j) == groupId && RecipientsTemp.get(i).groupTypes.get(j) == 2) {
                        cb.setChecked(true);
                        gotChecked = true;
                        break;
                    }
                }
            }
        }

        view.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                if (!cb.isChecked()) {
                    HashMap<String, String> params = new HashMap<String, String>();
                    params.put("From", "Native Group");
                    params.put("Is Primary Number", "no");

                    cb.setChecked(true);
                    addExtraCheck(groupPosition, childPosition, cb, contact.name, contact.content_uri_id, contactNumber, groupId);
                    contactsAdapter = new ContactsAdapter(SelectContacts.this, sortedContacts);
                    nativeContactsList.setAdapter(contactsAdapter);
                } else {
                    cb.setChecked(false);

                    HashMap<String, String> params = new HashMap<String, String>();
                    params.put("From", "Native Group");

                    removeExtraCheck(groupPosition, childPosition, cb, contactNumber, groupId);
                    contactsAdapter = new ContactsAdapter(SelectContacts.this, sortedContacts);
                    nativeContactsList.setAdapter(contactsAdapter);
                }
            }
        });

        cb.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                if (cb.isChecked()) {
                    HashMap<String, String> params = new HashMap<String, String>();
                    params.put("From", "Native Group");
                    params.put("Is Primary Number", "no");

                    addExtraCheck(groupPosition, childPosition, cb, contact.name, contact.content_uri_id, contactNumber, groupId);
                    contactsAdapter = new ContactsAdapter(SelectContacts.this, sortedContacts);
                    nativeContactsList.setAdapter(contactsAdapter);
                } else {
                    HashMap<String, String> params = new HashMap<String, String>();
                    params.put("From", "Native Group");

                    removeExtraCheck(groupPosition, childPosition, cb, contactNumber, groupId);
                    contactsAdapter = new ContactsAdapter(SelectContacts.this, sortedContacts);
                    nativeContactsList.setAdapter(contactsAdapter);
                }
            }
        });

        return view;
    }

    private void privateGroupsAdapterSetup() {

        final LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        privateGroupAdapter = new SimpleExpandableListAdapter(this, privateGroupDataTemp, android.R.layout.simple_expandable_list_item_1, new String[] { Constants.GROUP_NAME }, new int[] { android.R.id.text1 }, groupedPrivateChildDataTemp, 0, null, new int[] {}) {

            public Object getChild(int groupPosition, int childPosition) {
                return groupedPrivateChildDataTemp.get(groupPosition).get(childPosition);
            }

            public long getChildId(int groupPosition, int childPosition) {
                long id = childPosition;
                for (int i = 0; i < groupPosition; i++) {
                    id += groupedPrivateChildDataTemp.get(groupPosition).size();
                }
                return id;
            }

            public int getChildrenCount(int groupPosition) {
                return groupedPrivateChildDataTemp.get(groupPosition).size();
            }

            public Object getGroup(int groupPosition) {
                return privateGroupDataTemp.get(groupPosition);
            }

            public int getGroupCount() {
                return privateGroupDataTemp.size();
            }

            public long getGroupId(int groupPosition) {
                return groupPosition;
            }

            public View getGroupView(final int groupPosition, final boolean isExpanded, View convertView, ViewGroup parent) {
                final GroupListHolder holder;
                if (convertView == null) {
                    LayoutInflater layoutInflater = getLayoutInflater();
                    convertView = layoutInflater.inflate(R.layout.select_contacts_expandable_list_group, null);
                    holder = new GroupListHolder();
                    holder.groupHeading = (TextView) convertView.findViewById(R.id.group_expl_list_group_row_group_name);
                    holder.groupCheck = (CheckBox) convertView.findViewById(R.id.group_expl_list_group_row_group_check);
                    convertView.setTag(holder);
                } else {
                    holder = (GroupListHolder) convertView.getTag();
                }
                holder.groupHeading.setText((String) privateGroupDataTemp.get(groupPosition).get(Constants.GROUP_NAME));
                holder.groupCheck.setChecked((Boolean) privateGroupDataTemp.get(groupPosition).get(Constants.GROUP_CHECK));

                holder.groupCheck.setOnClickListener(new OnClickListener() {

                    public void onClick(View v) {
                        if (holder.groupCheck.isChecked()) {
                            privateGroupDataTemp.get(groupPosition).put(Constants.GROUP_CHECK, true);
                            for (int i = 0; i < privateChildDataTemp.get(groupPosition).size(); i++) {
                                if (!((Boolean) privateChildDataTemp.get(groupPosition).get(i).get(Constants.CHILD_CHECK))) {
                                    addCheck(groupPosition, i, privateChildDataTemp, privateGroupDataTemp);
                                }
                            }
                            privateGroupAdapter.notifyDataSetChanged();
                        } else {
                            privateGroupDataTemp.get(groupPosition).put(Constants.GROUP_CHECK, false);
                            for (int i = 0; i < privateChildDataTemp.get(groupPosition).size(); i++) {
                                if ((Boolean) privateChildDataTemp.get(groupPosition).get(i).get(Constants.CHILD_CHECK)) {
                                    HashMap<String, String> params = new HashMap<String, String>();
                                    params.put("From", "Private Group");

                                    removeCheck(groupPosition, i, privateChildDataTemp, privateGroupDataTemp);
                                }
                            }
                            privateGroupAdapter.notifyDataSetChanged();
                        }
                    }
                });

                convertView.setOnClickListener(new OnClickListener() {

                    public void onClick(View v) {
                        if (isExpanded) {
                            privateGroupExplList.collapseGroup(groupPosition);
                        } else {
                            privateGroupExplList.expandGroup(groupPosition);
                        }
                    }
                });

                return convertView;
            }

            public android.view.View getChildView(final int groupPosition, final int childPosition, boolean isLastChild, android.view.View convertView, android.view.ViewGroup parent) {

                privateChildDataTemp.get(groupPosition).get(childPosition);
                final ChildListHolder holder;
                if (convertView == null) {
                    convertView = layoutInflater.inflate(R.layout.contacts_list_row, null, false);
                    holder = new ChildListHolder();
                    holder.childNameText = (TextView) convertView.findViewById(R.id.contact_list_row_contact_name);
                    holder.childContactImage = (ImageView) convertView.findViewById(R.id.contact_list_row_contact_pic);
                    holder.childNumberText = (TextView) convertView.findViewById(R.id.contact_list_row_contact_number);
                    holder.childCheck = (CheckBox) convertView.findViewById(R.id.contact_list_row_contact_check);
                    holder.primaryNumberLayout = (RelativeLayout) convertView.findViewById(R.id.contact_list_primary_contact_space);

                    convertView.setTag(holder);
                } else {
                    holder = (ChildListHolder) convertView.getTag();
                }

                final Long contactId = (Long) groupedPrivateChildDataTemp.get(groupPosition).get(childPosition).get(Constants.CHILD_CONTACT_ID);

                String contactName = (String) groupedPrivateChildDataTemp.get(groupPosition).get(childPosition).get(Constants.CHILD_NAME);
                holder.childNameText.setText(contactName);
                @SuppressWarnings("unchecked")
                final ArrayList<ContactNumber> numbers = (ArrayList<ContactNumber>) groupedPrivateChildDataTemp.get(groupPosition).get(childPosition).get(Constants.CHILD_NUMBER);

                holder.childContactImage.setImageBitmap((Bitmap) privateChildDataTemp.get(groupPosition).get(childPosition).get(Constants.CHILD_IMAGE));
                holder.childNumberText.setText(numbers.get(0).type + ": " + numbers.get(0).number);

                int i = 0;

                for (i = 0; i < privateChildDataTemp.get(groupPosition).size(); i++) {
                    if (contactId == (Long) privateChildDataTemp.get(groupPosition).get(i).get(Constants.CHILD_CONTACT_ID) && ((String) privateChildDataTemp.get(groupPosition).get(i).get(Constants.CHILD_NUMBER)).equals(numbers.get(0).number)) {
                        break;
                    }
                }

                final int reqChildPosition = i;

                if ((Boolean) privateChildDataTemp.get(groupPosition).get(reqChildPosition).get(Constants.CHILD_CHECK)) {
                    holder.childCheck.setChecked(true);
                } else {
                    holder.childCheck.setChecked(false);
                }

                holder.extraNumbersLayout = (LinearLayout) convertView.findViewById(R.id.extra_numbers_layout);

                nativeExtraNumbers.get(groupPosition).add(new ArrayList<ContactNumber>());

                ArrayList<ContactNumber> prunedList = new ArrayList<ContactNumber>();

                // Store the extra numbers (2nd to last) in prunedList.
                for (i = 1; i < numbers.size(); i++) {
                    prunedList.add(numbers.get(i));
                }

                // if prunedList has any element, then create an Extra Number
                // View for each element and add it into the Extra Numbers
                // Layout.
                if (prunedList.size() > 0) {
                    holder.extraNumbersLayout.setVisibility(View.VISIBLE);
                    holder.extraNumbersLayout.removeAllViews();
                    for (i = 0; i < prunedList.size(); i++) {
                        View view = createPrivateExtraNumberView(groupPosition, childPosition, prunedList.get(i), contactName, contactId, getLayoutInflater(), Long.parseLong((String) privateGroupDataTemp.get(groupPosition).get(Constants.GROUP_ID)));
                        holder.extraNumbersLayout.addView(view);
                    }
                } else {
                    holder.extraNumbersLayout.setVisibility(View.GONE);
                }

                holder.childCheck.setOnClickListener(new OnClickListener() {

                    public void onClick(View v) {
                        if (holder.childCheck.isChecked()) {
                            HashMap<String, String> params = new HashMap<String, String>();
                            params.put("From", "Private Group");
                            params.put("Is Primary Number", "yes");

                            // check the contact in contact list if not checked
                            // and create span, add group in group ids of the
                            // span if contact already checked////
                            addCheck(groupPosition, reqChildPosition, privateChildDataTemp, privateGroupDataTemp);

                            boolean areAllSelected = true;
                            for (int j = 0; j < privateChildDataTemp.get(groupPosition).size(); j++) {
                                if (!(Boolean) privateChildDataTemp.get(groupPosition).get(j).get(Constants.CHILD_CHECK)) {
                                    areAllSelected = false;
                                    break;
                                }
                            }

                            if (areAllSelected) {
                                privateGroupDataTemp.get(groupPosition).put(Constants.GROUP_CHECK, true);
                                privateGroupAdapter.notifyDataSetChanged();
                            }

                        } else {
                            removeCheck(groupPosition, reqChildPosition, privateChildDataTemp, privateGroupDataTemp);

                            HashMap<String, String> params = new HashMap<String, String>();
                            params.put("From", "Private Group");

                            boolean areAllDeselected = true;
                            for (int j = 0; j < privateChildDataTemp.get(groupPosition).size(); j++) {
                                if ((Boolean) privateChildDataTemp.get(groupPosition).get(j).get(Constants.CHILD_CHECK)) {
                                    areAllDeselected = false;
                                    break;
                                }
                            }

                            if (areAllDeselected) {
                                privateGroupDataTemp.get(groupPosition).put(Constants.GROUP_CHECK, false);
                                privateGroupAdapter.notifyDataSetChanged();
                            }
                        }
                        privateGroupAdapter.notifyDataSetChanged();
                    }
                });

                holder.primaryNumberLayout.setOnClickListener(new OnClickListener() {

                    public void onClick(View v) {
                        if (!holder.childCheck.isChecked()) {
                            HashMap<String, String> params = new HashMap<String, String>();
                            params.put("From", "Private Group");
                            params.put("Is Primary Number", "yes");

                            holder.childCheck.setChecked(true);
                            // check the contact in contact list if not checked
                            // and create span, add group in group ids of the
                            // span if contact already checked////
                            addCheck(groupPosition, reqChildPosition, privateChildDataTemp, privateGroupDataTemp);

                            boolean areAllSelected = true;
                            for (int j = 0; j < privateChildDataTemp.get(groupPosition).size(); j++) {
                                if (!(Boolean) privateChildDataTemp.get(groupPosition).get(j).get(Constants.CHILD_CHECK)) {
                                    areAllSelected = false;
                                    break;
                                }
                            }

                            if (areAllSelected) {
                                privateGroupDataTemp.get(groupPosition).put(Constants.GROUP_CHECK, true);
                                privateGroupAdapter.notifyDataSetChanged();
                            }
                        } else {
                            holder.childCheck.setChecked(false);

                            HashMap<String, String> params = new HashMap<String, String>();
                            params.put("From", "Private Group");

                            removeCheck(groupPosition, reqChildPosition, privateChildDataTemp, privateGroupDataTemp);

                            boolean areAllDeselected = true;
                            for (int j = 0; j < privateChildDataTemp.get(groupPosition).size(); j++) {
                                if ((Boolean) privateChildDataTemp.get(groupPosition).get(j).get(Constants.CHILD_CHECK)) {
                                    areAllDeselected = false;
                                    break;
                                }
                            }

                            if (areAllDeselected) {
                                privateGroupDataTemp.get(groupPosition).put(Constants.GROUP_CHECK, false);
                                privateGroupAdapter.notifyDataSetChanged();
                            }
                        }
                        privateGroupAdapter.notifyDataSetChanged();
                    }
                });

                return convertView;
            }

            /**
             * @detail creates an Extra Number's View for a ContactNumber object
             *         using the details like groupPosition, childPosition, etc
             * @param groupPosition
             * @param childPosition
             * @param contactNumber
             * @param contactName
             * @param contactId
             * @param inflater
             * @param groupId
             * @return Fully functional Extra Number's View with Listeners.
             */
            private View createPrivateExtraNumberView(final int groupPosition, final int childPosition, final ContactNumber contactNumber, final String contactName, final long contactId, LayoutInflater inflater, final Long groupId) {
                View view = inflater.inflate(R.layout.extra_numbers_list_row, null);

                TextView tv = (TextView) view.findViewById(R.id.extra_number);
                final CheckBox cb = (CheckBox) view.findViewById(R.id.extra_number_checkbox);

                tv.setText(contactNumber.type + ": " + contactNumber.number);

                int i = 0;

                for (i = 0; i < privateChildDataTemp.get(groupPosition).size(); i++) {
                    if (contactNumber.contactId == (Long) privateChildDataTemp.get(groupPosition).get(i).get(Constants.CHILD_CONTACT_ID) && ((String) privateChildDataTemp.get(groupPosition).get(i).get(Constants.CHILD_NUMBER)).equals(contactNumber.number)) {
                        break;
                    }
                }

                final int reqChildPosition = i;

                cb.setChecked((Boolean) privateChildDataTemp.get(groupPosition).get(reqChildPosition).get(Constants.CHILD_CHECK));

                view.setOnClickListener(new OnClickListener() {

                    public void onClick(View v) {
                        if (!cb.isChecked()) {
                            HashMap<String, String> params = new HashMap<String, String>();
                            params.put("From", "Priavte Group");
                            params.put("Is Primary Number", "no");

                            cb.setChecked(true);

                            privateChildDataTemp.get(groupPosition).get(reqChildPosition).put(Constants.CHILD_CHECK, true);

                            addExtraCheck(groupPosition, reqChildPosition, cb, contactName, contactId, contactNumber, groupId);

                            boolean areAllSelected = true;
                            for (int j = 0; j < privateChildDataTemp.get(groupPosition).size(); j++) {
                                if (!(Boolean) privateChildDataTemp.get(groupPosition).get(j).get(Constants.CHILD_CHECK)) {
                                    areAllSelected = false;
                                    break;
                                }
                            }

                            if (areAllSelected) {
                                privateGroupDataTemp.get(groupPosition).put(Constants.GROUP_CHECK, true);
                                privateGroupAdapter.notifyDataSetChanged();
                            }

                        } else {
                            cb.setChecked(false);

                            HashMap<String, String> params = new HashMap<String, String>();
                            params.put("From", "Priavte Group");

                            privateChildDataTemp.get(groupPosition).get(reqChildPosition).put(Constants.CHILD_CHECK, false);

                            removeExtraCheck(groupPosition, reqChildPosition, cb, contactNumber, groupId);

                            boolean areAllDeselected = true;
                            for (int j = 0; j < privateChildDataTemp.get(groupPosition).size(); j++) {
                                if ((Boolean) privateChildDataTemp.get(groupPosition).get(j).get(Constants.CHILD_CHECK)) {
                                    areAllDeselected = false;
                                    break;
                                }
                            }

                            if (areAllDeselected) {
                                privateGroupDataTemp.get(groupPosition).put(Constants.GROUP_CHECK, false);
                                privateGroupAdapter.notifyDataSetChanged();
                            }

                        }

                        privateGroupAdapter.notifyDataSetChanged();
                        contactsAdapter = new ContactsAdapter(SelectContacts.this, sortedContacts);
                        nativeContactsList.setAdapter(contactsAdapter);
                    }
                });

                cb.setOnClickListener(new OnClickListener() {

                    public void onClick(View v) {
                        if (cb.isChecked()) {
                            HashMap<String, String> params = new HashMap<String, String>();
                            params.put("From", "Private Group");
                            params.put("Is Primary Number", "no");

                            privateChildDataTemp.get(groupPosition).get(reqChildPosition).put(Constants.CHILD_CHECK, true);

                            addExtraCheck(groupPosition, reqChildPosition, cb, contactName, contactId, contactNumber, groupId);

                            boolean areAllSelected = true;
                            for (int j = 0; j < privateChildDataTemp.get(groupPosition).size(); j++) {
                                if (!(Boolean) privateChildDataTemp.get(groupPosition).get(j).get(Constants.CHILD_CHECK)) {
                                    areAllSelected = false;
                                    break;
                                }
                            }

                            if (areAllSelected) {
                                privateGroupDataTemp.get(groupPosition).put(Constants.GROUP_CHECK, true);
                                privateGroupAdapter.notifyDataSetChanged();
                            }

                        } else {
                            HashMap<String, String> params = new HashMap<String, String>();
                            params.put("From", "Private Group");

                            privateChildDataTemp.get(groupPosition).get(reqChildPosition).put(Constants.CHILD_CHECK, true);

                            removeExtraCheck(groupPosition, reqChildPosition, cb, contactNumber, groupId);

                            boolean areAllDeselected = true;
                            for (int j = 0; j < privateChildDataTemp.get(groupPosition).size(); j++) {
                                if ((Boolean) privateChildDataTemp.get(groupPosition).get(j).get(Constants.CHILD_CHECK)) {
                                    areAllDeselected = false;
                                    break;
                                }
                            }

                            if (areAllDeselected) {
                                privateGroupDataTemp.get(groupPosition).put(Constants.GROUP_CHECK, false);
                                privateGroupAdapter.notifyDataSetChanged();
                            }
                        }
                        privateGroupAdapter.notifyDataSetChanged();
                        contactsAdapter = new ContactsAdapter(SelectContacts.this, sortedContacts);
                        nativeContactsList.setAdapter(contactsAdapter);
                    }
                });

                return view;
            }

            public boolean areAllItemsEnabled() {
                return true;
            }

            public boolean hasStableIds() {
                return false;
            }

            public boolean isChildSelectable(int groupPosition, int childPosition) {
                return true;
            }
        };
    }

    /**
     * @detail Checks an ExtraNumber's Checkbox. First it checks if the
     *         recipient already exists. If so, it just checks the checkbox,
     *         Otherwise it also creates a new Recipient.
     * @param groupPosition
     * @param childPosition
     * @param cb
     * @param contactName
     * @param contactId
     * @param contactNumber
     * @param groupId
     */
    private void addExtraCheck(int groupPosition, int childPosition, CheckBox cb, String contactName, long contactId, ContactNumber contactNumber, long groupId) {
        cb.setChecked(true);
        boolean recipientExist = false;
        for (int i = 0; i < RecipientsTemp.size(); i++) {
            if (RecipientsTemp.get(i).contactId == contactNumber.contactId && RecipientsTemp.get(i).number.equals(contactNumber.number)) {
                recipientExist = true;
                RecipientsTemp.get(i).groupIds.add(groupId);
                RecipientsTemp.get(i).groupTypes.add(2);
                break;
            }
        }
        if (!recipientExist) {
            Recipient recipient = new Recipient(-1, 2, contactName, contactId, -1, -1, -1, contactNumber.number);
            recipient.groupIds.add(groupId);
            recipient.groupTypes.add(2);
            RecipientsTemp.add(recipient);
            contactsAdapter.notifyDataSetChanged();
        }
    }

    /**
     * @details checks the Primary Number's checkbox. Also, if the recipient
     *          doesn't exist, creates it.
     * @param groupPosition
     * @param childPosition
     * @param ChildDataTemp
     * @param GroupDataTemp
     */
    private void addCheck(int groupPosition, int childPosition, ArrayList<ArrayList<HashMap<String, Object>>> ChildDataTemp, ArrayList<HashMap<String, Object>> GroupDataTemp) {
        ChildDataTemp.get(groupPosition).get(childPosition).put(Constants.CHILD_CHECK, true);
        boolean spanExist = false;
        for (int i = 0; i < RecipientsTemp.size(); i++) {
            if (RecipientsTemp.get(i).contactId == (Long) ChildDataTemp.get(groupPosition).get(childPosition).get(Constants.CHILD_CONTACT_ID) && RecipientsTemp.get(i).number.equals((String) ChildDataTemp.get(groupPosition).get(childPosition).get(Constants.CHILD_NUMBER))) {
                spanExist = true;
                try {
                    RecipientsTemp.get(i).groupIds.add(Long.parseLong((String) GroupDataTemp.get(groupPosition).get(Constants.GROUP_ID)));
                } catch (ClassCastException e) {
                    RecipientsTemp.get(i).groupIds.add(((Long) GroupDataTemp.get(groupPosition).get(Constants.GROUP_ID)));
                }
                RecipientsTemp.get(i).groupTypes.add(2);
                break;
            }
        }
        if (!spanExist) {
            Recipient recipient = new Recipient(-1, 2, (String) ChildDataTemp.get(groupPosition).get(childPosition).get(Constants.CHILD_NAME), (Long) ChildDataTemp.get(groupPosition).get(childPosition).get(Constants.CHILD_CONTACT_ID), -1, -1, -1, (String) ChildDataTemp.get(groupPosition).get(childPosition).get(Constants.CHILD_NUMBER));
            try {
                recipient.groupIds.add(((Long) GroupDataTemp.get(groupPosition).get(Constants.GROUP_ID)));
            } catch (ClassCastException e) {
                recipient.groupIds.add(Long.parseLong((String) GroupDataTemp.get(groupPosition).get(Constants.GROUP_ID)));
            }
            recipient.groupTypes.add(2);
            RecipientsTemp.add(recipient);
            contactsAdapter.notifyDataSetChanged();
        }
    }

    /**
     * @details removes check from an Extra Number's checkbox. If the concerned
     *          recipient isn't there in any other group, removes it from the
     *          Recipients ArrayList.
     * @param groupPosition
     * @param childPosition
     * @param cb
     * @param contactNumber
     * @param groupId
     */
    private void removeExtraCheck(int groupPosition, int childPosition, CheckBox cb, ContactNumber contactNumber, long groupId) {
        cb.setChecked(false);
        for (int i = 0; i < RecipientsTemp.size(); i++) {
            if (contactNumber.contactId == RecipientsTemp.get(i).contactId && contactNumber.number.equals(RecipientsTemp.get(i).number)) {
                Long groupIdToRemove = groupId;
                int groupTypeToRemove = 2;
                for (int j = 0; j < RecipientsTemp.get(i).groupIds.size(); j++) {
                    if (RecipientsTemp.get(i).groupIds.get(j) == groupIdToRemove && RecipientsTemp.get(i).groupTypes.get(j) == groupTypeToRemove) {
                        RecipientsTemp.get(i).groupIds.remove(j);
                        if (RecipientsTemp.get(i).groupIds.size() == 0) {
                            RecipientsTemp.remove(i);
                            contactsAdapter.notifyDataSetChanged();
                        }
                        break;
                    }
                }
            }
        }
    }

    /**
     * @details Unchecks a Primany Number's checkbox. If the concerned recipient
     *          doesn't exist in any other selected group, it is removed from
     *          the Recipient's ArrayList.
     * @param groupPosition
     * @param childPosition
     * @param ChildDataTemp
     * @param GroupDataTemp
     */
    private void removeCheck(int groupPosition, int childPosition, ArrayList<ArrayList<HashMap<String, Object>>> ChildDataTemp, ArrayList<HashMap<String, Object>> GroupDataTemp) {
        ChildDataTemp.get(groupPosition).get(childPosition).put(Constants.CHILD_CHECK, false);
        for (int i = 0; i < RecipientsTemp.size(); i++) {
            if (RecipientsTemp.get(i).contactId == (Long) ChildDataTemp.get(groupPosition).get(childPosition).get(Constants.CHILD_CONTACT_ID) && RecipientsTemp.get(i).number.equals((String) ChildDataTemp.get(groupPosition).get(childPosition).get(Constants.CHILD_NUMBER))) {
                for (int j = 0; j < RecipientsTemp.get(i).groupIds.size(); j++) {
                    Long groupIdToRemove;
                    int groupTypeToRemove;
                    try {
                        groupIdToRemove = Long.parseLong((String) GroupDataTemp.get(groupPosition).get(Constants.GROUP_ID));
                    } catch (ClassCastException e) {
                        groupIdToRemove = (Long) GroupDataTemp.get(groupPosition).get(Constants.GROUP_ID);
                    }
                    groupTypeToRemove = 2;
                    if (RecipientsTemp.get(i).groupIds.get(j) == groupIdToRemove && RecipientsTemp.get(i).groupTypes.get(j) == groupTypeToRemove) {
                        RecipientsTemp.get(i).groupIds.remove(j);
                        if (RecipientsTemp.get(i).groupIds.size() == 0) {
                            RecipientsTemp.remove(i);
                            contactsAdapter.notifyDataSetChanged();
                        }
                        break;
                    }
                }
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private class RecentsAdapter extends ArrayAdapter {

        @SuppressWarnings("unchecked")
        RecentsAdapter() {
            super(SelectContacts.this, R.layout.contacts_list_row, recentIds);
        }

        public View getView(final int position, View convertView, ViewGroup parent) {
            final RecentsListHolder holder;
            final int _position = position;
            if (convertView == null) {
                LayoutInflater inflater = getLayoutInflater();
                convertView = inflater.inflate(R.layout.contacts_list_row, parent, false);
                holder = new RecentsListHolder();
                holder.contactImage = (ImageView) convertView.findViewById(R.id.contact_list_row_contact_pic);
                holder.nameText = (TextView) convertView.findViewById(R.id.contact_list_row_contact_name);
                holder.numberText = (TextView) convertView.findViewById(R.id.contact_list_row_contact_number);
                holder.contactCheck = (CheckBox) convertView.findViewById(R.id.contact_list_row_contact_check);
                holder.primaryNumberLayout = (RelativeLayout) convertView.findViewById(R.id.contact_list_primary_contact_space);
                convertView.setTag(holder);
            } else {
                holder = (RecentsListHolder) convertView.getTag();
            }

            int i = 0;

            if (recentContactIds.get(position) > -1) {
                for (i = 0; i < SmsSchedulerApplication.contactsList.size(); i++) {
                    if (SmsSchedulerApplication.contactsList.get(i).content_uri_id == recentContactIds.get(position)) {

                        displayImage.submitImage(holder.contactImage, SmsSchedulerApplication.contactsList.get(i).content_uri_id, SelectContacts.this);
                        holder.nameText.setText(SmsSchedulerApplication.contactsList.get(i).name);
                        holder.numberText.setText(recentContactNumbers.get(position));

                        break;
                    }
                }

                for (int j = 0; j < RecipientsTemp.size(); j++) {
                    if ((recentContactIds.get(position) == RecipientsTemp.get(j).contactId) && (recentContactNumbers.get(position).equals(RecipientsTemp.get(j).number))) {// TODO
                        holder.contactCheck.setChecked(true);
                        break;
                    } else {
                        holder.contactCheck.setChecked(false);
                    }
                }

            } else if (recentContactIds.get(position) == -1) {
                holder.contactImage.setImageBitmap(BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.no_image_thumbnail));
                holder.nameText.setText(recentContactNumbers.get(position));
                holder.numberText.setText("");
                for (int j = 0; j < RecipientsTemp.size(); j++) {
                    if (RecipientsTemp.get(j).displayName.equals(recentContactNumbers.get(position))) {
                        holder.contactCheck.setChecked(true);
                        break;
                    } else {
                        holder.contactCheck.setChecked(false);
                    }
                }
            }
            holder.primaryNumberLayout.setOnClickListener(new OnClickListener() {

                public void onClick(View v) {
                    if (!holder.contactCheck.isChecked()) {
                        holder.contactCheck.setChecked(true);
                        Recipient recipient = new Recipient();
                        if (recentContactIds.get(_position) > -1) {
                            for (int k = 0; k < SmsSchedulerApplication.contactsList.size(); k++) {
                                if (SmsSchedulerApplication.contactsList.get(k).content_uri_id == recentContactIds.get(_position)) {
                                    recipient = new Recipient(-1, 2, SmsSchedulerApplication.contactsList.get(k).name, SmsSchedulerApplication.contactsList.get(k).content_uri_id, -1, -1, -1, recentContactNumbers.get(position));// TODO
                                    break;
                                }
                            }
                        } else {
                            HashMap<String, String> params = new HashMap<String, String>();
                            params.put("From", "Recents");
                            params.put("Is Primary Number", "no");

                            recipient = new Recipient(Constants.GENERIC_DEFAULT_INT_VALUE, Constants.RECIPIENT_TYPE_NUMBER, recentContactNumbers.get(_position), Constants.GENERIC_DEFAULT_INT_VALUE, Constants.GENERIC_DEFAULT_INT_VALUE, Constants.GENERIC_DEFAULT_INT_VALUE, Constants.GENERIC_DEFAULT_INT_VALUE, null); // TODO
                        }
                        recipient.groupIds.add((long) Constants.GENERIC_DEFAULT_INT_VALUE);
                        recipient.groupTypes.add(Constants.GENERIC_DEFAULT_INT_VALUE);
                        RecipientsTemp.add(recipient);
                        nativeContactsList.setAdapter(new ContactsAdapter(SelectContacts.this, sortedContacts));
                    } else {
                        HashMap<String, String> params = new HashMap<String, String>();
                        params.put("From", "Recents");

                        holder.contactCheck.setChecked(false);
                        for (int i = 0; i < RecipientsTemp.size(); i++) {
                            if (recentContactIds.get(_position) > -1) {
                                if (recentContactIds.get(_position) == RecipientsTemp.get(i).contactId && recentContactNumbers.get(position).equals(RecipientsTemp.get(i).number)) {
                                    RecipientsTemp.remove(i);
                                    nativeContactsList.setAdapter(new ContactsAdapter(SelectContacts.this, sortedContacts));
                                    break;
                                }
                            } else {
                                if (RecipientsTemp.get(i).displayName.equals(recentContactNumbers.get(_position))) {
                                    RecipientsTemp.remove(i);
                                    nativeContactsList.setAdapter(new ContactsAdapter(SelectContacts.this, sortedContacts));
                                    break;
                                }
                            }
                        }
                    }
                }
            });

            holder.contactCheck.setOnClickListener(new OnClickListener() {

                public void onClick(View v) {
                    if (holder.contactCheck.isChecked()) {
                        Recipient recipient = new Recipient();
                        if (recentContactIds.get(_position) > -1) {
                            for (int k = 0; k < SmsSchedulerApplication.contactsList.size(); k++) {
                                if (SmsSchedulerApplication.contactsList.get(k).content_uri_id == recentContactIds.get(_position)) {
                                    recipient = new Recipient(Constants.GENERIC_DEFAULT_INT_VALUE, Constants.RECIPIENT_TYPE_CONTACT, SmsSchedulerApplication.contactsList.get(k).name, SmsSchedulerApplication.contactsList.get(k).content_uri_id, -1, -1, -1, recentContactNumbers.get(_position));
                                    break;
                                }
                            }
                        } else {
                            recipient = new Recipient(Constants.GENERIC_DEFAULT_INT_VALUE, Constants.RECIPIENT_TYPE_NUMBER, recentContactNumbers.get(_position), Constants.GENERIC_DEFAULT_INT_VALUE, Constants.GENERIC_DEFAULT_INT_VALUE, Constants.GENERIC_DEFAULT_INT_VALUE, Constants.GENERIC_DEFAULT_INT_VALUE, null);
                        }
                        recipient.groupIds.add((long) Constants.GENERIC_DEFAULT_INT_VALUE);
                        recipient.groupTypes.add(Constants.GENERIC_DEFAULT_INT_VALUE);

                        HashMap<String, String> params = new HashMap<String, String>();
                        params.put("From", "Recents");
                        params.put("Is Primary Number", "no");

                        RecipientsTemp.add(recipient);

                        nativeContactsList.setAdapter(new ContactsAdapter(SelectContacts.this, sortedContacts));
                    } else {
                        HashMap<String, String> params = new HashMap<String, String>();
                        params.put("From", "Recents");

                        for (int i = 0; i < RecipientsTemp.size(); i++) {
                            if (recentContactIds.get(_position) > -1) {
                                if (recentContactIds.get(_position) == RecipientsTemp.get(i).contactId && recentContactNumbers.get(position).equals(RecipientsTemp.get(i).number)) {
                                    RecipientsTemp.remove(i);
                                    nativeContactsList.setAdapter(new ContactsAdapter(SelectContacts.this, sortedContacts));
                                    break;
                                }
                            } else {
                                if (RecipientsTemp.get(i).displayName.equals(recentContactNumbers.get(_position))) {
                                    RecipientsTemp.remove(i);
                                    nativeContactsList.setAdapter(new ContactsAdapter(SelectContacts.this, sortedContacts));
                                    break;
                                }
                            }
                        }
                    }
                }
            });

            return convertView;
        }
    }

    private class ContactsListHolder {
        ImageView contactImage;
        TextView nameText;
        TextView numberText;
        CheckBox contactCheck;
        LinearLayout extraContactsLayout;
        RelativeLayout primaryContactLayout;
        ArrayList<View> extraContactsViews;
    }

    private class GroupListHolder {
        TextView groupHeading;
        CheckBox groupCheck;
    }

    private class ChildListHolder {
        TextView childNameText;
        ImageView childContactImage;
        TextView childNumberText;
        CheckBox childCheck;
        LinearLayout extraNumbersLayout;
        RelativeLayout primaryNumberLayout;
    }

    private class RecentsListHolder {
        ImageView contactImage;
        TextView nameText;
        TextView numberText;
        CheckBox contactCheck;
        RelativeLayout primaryNumberLayout;
    }

    @SuppressWarnings("static-access")
    /**
     * @details Reloads the data of private groups into the data structure. This is called from the onResume() method when the screen
     * 			returns to this Activity after going to the Add New Group module when the Private Groups list would have been empty.
     */
    private void reloadPrivateGroupData() {
        mdba.open();

        privateChildDataTemp.clear();
        privateGroupDataTemp.clear();

        if (origin.equals("new")) {
            ScheduleNewSms.privateGroupData.clear();
            ScheduleNewSms.privateChildData.clear();
        } else {
            EditScheduledSms.privateGroupData.clear();
            EditScheduledSms.privateChildData.clear();
        }
        Cursor groupsCursor = mdba.fetchAllGroups();
        if (groupsCursor.moveToFirst()) {
            do {
                HashMap<String, Object> group = new HashMap<String, Object>();
                ArrayList<Long> spanIdsForGroup = mdba.fetchRecipientsForGroup(groupsCursor.getLong(groupsCursor.getColumnIndex(DBAdapter.KEY_GROUP_ID)), 2);
                group.put(Constants.GROUP_NAME, groupsCursor.getString(groupsCursor.getColumnIndex(DBAdapter.KEY_GROUP_NAME)));
                group.put(Constants.GROUP_IMAGE, new BitmapFactory().decodeResource(getResources(), R.drawable.expander_ic_maximized));
                if (spanIdsForGroup.size() == 0) {
                    group.put(Constants.GROUP_CHECK, false);
                } else {
                    for (int i = 0; i < RecipientsTemp.size(); i++) {
                        for (int j = 0; j < spanIdsForGroup.size(); j++) {
                            if (spanIdsForGroup.get(j) == RecipientsTemp.get(i).recipientId) {
                                group.put(Constants.GROUP_CHECK, true);
                                break;
                            }
                        }
                    }
                }
                group.put(Constants.GROUP_TYPE, 2);
                group.put(Constants.GROUP_ID, groupsCursor.getString(groupsCursor.getColumnIndex(DBAdapter.KEY_GROUP_ID)));
                ArrayList<String> contactNumbers = mdba.fetchNumbersForGroup(groupsCursor.getLong(groupsCursor.getColumnIndex(DBAdapter.KEY_GROUP_ID)));

                AbstractScheduleSms.privateGroupData.add(group);

                ArrayList<HashMap<String, Object>> child = new ArrayList<HashMap<String, Object>>();
                ArrayList<Long> contactIds = mdba.fetchIdsForGroups(groupsCursor.getLong(groupsCursor.getColumnIndex(DBAdapter.KEY_GROUP_ID)));

                for (int i = 0; i < contactIds.size(); i++) {
                    for (int j = 0; j < SmsSchedulerApplication.contactsList.size(); j++) {
                        if (contactIds.get(i) == SmsSchedulerApplication.contactsList.get(j).content_uri_id) {
                            HashMap<String, Object> childParameters = new HashMap<String, Object>();
                            childParameters.put(Constants.CHILD_NAME, SmsSchedulerApplication.contactsList.get(j).name);
                            ArrayList<ContactNumber> numbers = SmsSchedulerApplication.contactsList.get(j).numbers;
                            String number = "";
                            for (int m = 0; m < numbers.size(); m++) {
                                if (numbers.get(m).number.equals(contactNumbers.get(i))) {
                                    number = numbers.get(m).number;
                                    break;
                                }
                            }
                            childParameters.put(Constants.CHILD_NUMBER, number);// TODO
                            childParameters.put(Constants.CHILD_CONTACT_ID, SmsSchedulerApplication.contactsList.get(j).content_uri_id);
                            displayImage.storeImage(SmsSchedulerApplication.contactsList.get(j).content_uri_id, childParameters, SelectContacts.this);
                            childParameters.put(Constants.CHILD_CHECK, false);
                            for (int k = 0; k < spanIdsForGroup.size(); k++) {
                                for (int m = 0; m < RecipientsTemp.size(); m++) {
                                    if (RecipientsTemp.get(m).recipientId == spanIdsForGroup.get(k) && RecipientsTemp.get(m).recipientId == contactIds.get(i)) {
                                        childParameters.put(Constants.CHILD_CHECK, true);
                                    }
                                }
                            }

                            child.add(childParameters);
                        }
                    }
                }
                AbstractScheduleSms.privateChildData.add(child);
            } while (groupsCursor.moveToNext());
        }

        mdba.close();

        if (origin.equals("new")) {
            for (int groupCount = 0; groupCount < ScheduleNewSms.privateGroupData.size(); groupCount++) {
                HashMap<String, Object> group = new HashMap<String, Object>();
                group.put(Constants.GROUP_ID, ScheduleNewSms.privateGroupData.get(groupCount).get(Constants.GROUP_ID));
                group.put(Constants.GROUP_NAME, ScheduleNewSms.privateGroupData.get(groupCount).get(Constants.GROUP_NAME));
                group.put(Constants.GROUP_IMAGE, ScheduleNewSms.privateGroupData.get(groupCount).get(Constants.GROUP_IMAGE));
                group.put(Constants.GROUP_TYPE, ScheduleNewSms.privateGroupData.get(groupCount).get(Constants.GROUP_TYPE));
                group.put(Constants.GROUP_CHECK, ScheduleNewSms.privateGroupData.get(groupCount).get(Constants.GROUP_CHECK));
                for (int i = 0; i < RecipientsTemp.size(); i++) {
                    for (int j = 0; j < RecipientsTemp.get(i).groupIds.size(); j++) {
                        if ((RecipientsTemp.get(i).groupIds.get(j) == group.get(Constants.GROUP_ID)) && RecipientsTemp.get(i).groupTypes.get(j) == 2) {
                            group.put(Constants.GROUP_CHECK, true);
                            break;
                        }
                    }
                }

                privateGroupDataTemp.add(group);
                ArrayList<HashMap<String, Object>> child = new ArrayList<HashMap<String, Object>>();
                for (int childCount = 0; childCount < ScheduleNewSms.privateChildData.get(groupCount).size(); childCount++) {
                    HashMap<String, Object> childParams = new HashMap<String, Object>();
                    childParams.put(Constants.CHILD_CONTACT_ID, ScheduleNewSms.privateChildData.get(groupCount).get(childCount).get(Constants.CHILD_CONTACT_ID));
                    childParams.put(Constants.CHILD_NAME, ScheduleNewSms.privateChildData.get(groupCount).get(childCount).get(Constants.CHILD_NAME));
                    childParams.put(Constants.CHILD_NUMBER, ScheduleNewSms.privateChildData.get(groupCount).get(childCount).get(Constants.CHILD_NUMBER));
                    displayImage.storeImage((Long) childParams.get(Constants.CHILD_CONTACT_ID), childParams, SelectContacts.this);
                    childParams.put(Constants.CHILD_CHECK, ScheduleNewSms.privateChildData.get(groupCount).get(childCount).get(Constants.CHILD_CHECK));

                    child.add(childParams);
                }
                privateChildDataTemp.add(child);
            }
        } else {
            for (int groupCount = 0; groupCount < EditScheduledSms.privateGroupData.size(); groupCount++) {
                HashMap<String, Object> group = new HashMap<String, Object>();
                group.put(Constants.GROUP_ID, EditScheduledSms.privateGroupData.get(groupCount).get(Constants.GROUP_ID));
                group.put(Constants.GROUP_NAME, EditScheduledSms.privateGroupData.get(groupCount).get(Constants.GROUP_NAME));
                group.put(Constants.GROUP_IMAGE, EditScheduledSms.privateGroupData.get(groupCount).get(Constants.GROUP_IMAGE));
                group.put(Constants.GROUP_TYPE, EditScheduledSms.privateGroupData.get(groupCount).get(Constants.GROUP_TYPE));
                group.put(Constants.GROUP_CHECK, EditScheduledSms.privateGroupData.get(groupCount).get(Constants.GROUP_CHECK));

                privateGroupDataTemp.add(group);
                ArrayList<HashMap<String, Object>> child = new ArrayList<HashMap<String, Object>>();
                for (int childCount = 0; childCount < EditScheduledSms.privateChildData.get(groupCount).size(); childCount++) {
                    HashMap<String, Object> childParams = new HashMap<String, Object>();
                    childParams.put(Constants.CHILD_CONTACT_ID, EditScheduledSms.privateChildData.get(groupCount).get(childCount).get(Constants.CHILD_CONTACT_ID));
                    childParams.put(Constants.CHILD_NAME, EditScheduledSms.privateChildData.get(groupCount).get(childCount).get(Constants.CHILD_NAME));
                    childParams.put(Constants.CHILD_NUMBER, EditScheduledSms.privateChildData.get(groupCount).get(childCount).get(Constants.CHILD_NUMBER));
                    displayImage.storeImage((Long) childParams.get(Constants.CHILD_CONTACT_ID), childParams, SelectContacts.this);
                    childParams.put(Constants.CHILD_CHECK, EditScheduledSms.privateChildData.get(groupCount).get(childCount).get(Constants.CHILD_CHECK));
                    child.add(childParams);
                }
                privateChildDataTemp.add(child);
            }
        }

        groupedPrivateChildDataTemp = organizeChildData(privateGroupDataTemp, privateChildDataTemp);
    }
}
