/**
 * Copyright (c) 2012 Vinayak Solutions Private Limited 
 * See the file license.txt for copying permission.
 */

package com.freeandroapp.smsscheduler.models;

import java.util.ArrayList;

public class Contact {

    public long content_uri_id;
    public String name;
    public ArrayList<ContactNumber> numbers = new ArrayList<ContactNumber>();
    public ArrayList<Long> groupRowId = new ArrayList<Long>();
    public boolean checked = false;
    public int timesContacted;
}
