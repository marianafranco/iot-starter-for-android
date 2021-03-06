/*******************************************************************************
 * Copyright (c) 2014 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *    Mike Robertson - initial contribution
 *******************************************************************************/
package com.ibm.iot.android.iotstarter.utils;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class IoTProfile {
    private String profileName;
    private String organization;
    private String apiKey;
    private String apiToken;

    private static final String NAME_PREFIX = "name:";
    private static final String ORG_PREFIX = "org:";
    private static final String API_KEY_PREFIX = "apiKey:";
    private static final String API_TOKEN_PREFIX = "apiToken:";

    public IoTProfile(String profileName, String organization, String apiKey, String apiToken) {
        this.profileName = profileName;
        this.organization = organization;
        this.apiKey = apiKey;
        this.apiToken = apiToken;
    }

    public IoTProfile(Set<String> profileSet) {
        Iterator<String> iter = profileSet.iterator();
        while (iter.hasNext()) {
            String value = iter.next();
            if (value.contains(NAME_PREFIX)) {
                this.profileName = value.substring(NAME_PREFIX.length());
            } else if (value.contains(ORG_PREFIX)) {
                this.organization = value.substring(ORG_PREFIX.length());
            } else if (value.contains(API_KEY_PREFIX)) {
                this.apiKey = value.substring(API_KEY_PREFIX.length());
            } else if (value.contains(API_TOKEN_PREFIX)) {
                this.apiToken = value.substring(API_TOKEN_PREFIX.length());
            }
        }
    }

    public Set<String> convertToSet() {
        // Put the new profile into the store settings and remove the old stored properties.
        Set<String> profileSet = new HashSet<String>();
        profileSet.add(NAME_PREFIX + this.profileName);
        profileSet.add(ORG_PREFIX + this.organization);
        profileSet.add(API_KEY_PREFIX + this.apiKey);
        profileSet.add(API_TOKEN_PREFIX + this.apiToken);

        return profileSet;
    }

    public String getProfileName() {
        return profileName;
    }

    public String getOrganization() {
        return organization;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getApiToken() {
        return apiToken;
    }
}
