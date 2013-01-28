/**
 * Copyright (c) 2012 Vinayak Solutions Private Limited 
 * See the file license.txt for copying permission.
 */

package com.freeandroapp.smsscheduler;

import java.util.ArrayList;

import android.app.Application;

import com.freeandroapp.smsscheduler.models.Contact;

public class SmsSchedulerApplication extends Application {

    public static ArrayList<Contact> contactsList = new ArrayList<Contact>();
    public static boolean isDataLoaded = false;
}
