/*
Copyright 2015 Adobe. All rights reserved.
This file is licensed to you under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License. You may obtain a copy
of the License at http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under
the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
OF ANY KIND, either express or implied. See the License for the specific language
governing permissions and limitations under the License.
*/
package com.adobe.qe.toughday.tests.composite;

import com.adobe.qe.toughday.api.core.AbstractTest;
import com.adobe.qe.toughday.api.core.CompositeTest;
import com.adobe.qe.toughday.api.annotations.Description;
import com.adobe.qe.toughday.api.annotations.ConfigArgGet;
import com.adobe.qe.toughday.api.annotations.ConfigArgSet;
import com.adobe.qe.toughday.tests.sequential.users.CreateUserGroupTest;
import com.adobe.qe.toughday.tests.sequential.users.CreateUserTest;

import java.util.concurrent.atomic.AtomicInteger;

@Description(desc = "At every execution creates one group, five users and adds the users to that group. " +
                "Additionally, it creates one extra group at the beginning and all the users are added into that group as well")
public class CreateGroupWithUsers extends CompositeTest {
    private CreateFiveUsersTest createFiveUsersTest;
    private CreateUserGroupTest createUserGroupTest;

    public CreateGroupWithUsers() { this(true); }

    public CreateGroupWithUsers(boolean createChildren) {
        if (createChildren) {
            this.createUserGroupTest = new CreateUserGroupTest();
            this.createFiveUsersTest = new CreateFiveUsersTest();

            createUserGroupTest.setGlobalArgs(this.getGlobalArgs());
            createFiveUsersTest.setGlobalArgs(this.getGlobalArgs());

            addChild(createUserGroupTest);
            addChild(createFiveUsersTest);
        }
    }

    @Override
    public AbstractTest newInstance() {
        return new CreateGroupWithUsers(false);
    }

    @ConfigArgSet(required = false, desc = "Email address for created users.", defaultValue = CreateUserTest.DEFAULT_EMAIL_ADDRESS)
    public void setUserEmailAddress(String emailAddress) {
        this.createFiveUsersTest.setEmailAddress(emailAddress);
    }

    @ConfigArgGet
    public String getUserEmailAddress() {
        return this.createFiveUsersTest.getEmailAddress();
    }

    @ConfigArgSet(required = false, desc = "Password for the created users.", defaultValue = CreateUserTest.DEFAULT_PASSWORD)
    public void setUserPassword(String password) {
        this.createFiveUsersTest.setPassword(password);
    }

    @ConfigArgGet
    public String getUserPassword() {
        return this.createFiveUsersTest.getPassword();
    }

    @ConfigArgSet(required = false, desc = "Telephone for the created users.", defaultValue = CreateUserTest.DEFAULT_PHONE_NUMBER)
    public void setUserPhoneNumber(String phoneNumber) {
        this.createFiveUsersTest.setPhoneNumber(phoneNumber);
    }

    @ConfigArgGet
    public String getUserPhoneNumber() {
        return this.createFiveUsersTest.getPhoneNumber();
    }

    @ConfigArgSet(required = false, desc = "First name for the created users", defaultValue = CreateUserTest.DEFAULT_FIRST_NAME)
    public void setUserFirstName(String firstName) {
        this.createFiveUsersTest.setFirstName(firstName);
    }

    @ConfigArgGet
    public String getUserFirstName() {
        return this.createFiveUsersTest.getFirstName();
    }

    @ConfigArgSet(required = false, desc = "Last name for the created users", defaultValue = CreateUserTest.DEFAULT_LAST_NAME)
    public void setUserLastName(String lastName) {
        this.createFiveUsersTest.setLastName(lastName);
    }

    @ConfigArgGet
    public String getUserLastName() {
        return this.createFiveUsersTest.getLastName();
    }

    @ConfigArgSet(required = false, desc = "Job title for the created users", defaultValue = CreateUserTest.DEFAULT_JOB_TITLE)
    public void setUserJobTitle(String jobTitle) {
        this.createFiveUsersTest.setJobTitle(jobTitle);
    }

    @ConfigArgGet
    public String getUserJobTitle() {
        return this.createFiveUsersTest.getJobTitle();
    }

    @ConfigArgSet(required = false, desc = "Street address for the created users", defaultValue = CreateUserTest.DEFAULT_STREET)
    public void setUserStreet(String street) {
        this.createFiveUsersTest.setStreet(street);
    }

    @ConfigArgGet
    public String getUserStreet() {
        return this.createFiveUsersTest.getStreet();
    }

    @ConfigArgSet(required = false, desc = "City address for the created users", defaultValue = CreateUserTest.DEFAULT_CITY)
    public void setUserCity(String city) {
        this.createFiveUsersTest.setCity(city);
    }

    @ConfigArgGet
    public String getUserCity() {
        return this.createFiveUsersTest.getCity();
    }

    @ConfigArgSet(required = false, desc = "Mobile number for the created users", defaultValue = CreateUserTest.DEFAULT_MOBILE)
    public void setUserMobile(String mobile) {
        this.createFiveUsersTest.setMobile(mobile);
    }

    @ConfigArgGet
    public String setUserMobile() {
        return this.createFiveUsersTest.getMobile();
    }

    @ConfigArgSet(required = false, desc = "Postal code for the created users", defaultValue = CreateUserTest.DEFAULT_POSTAL_CODE)
    public void setUserPostalCode(String postalCode) {
        this.createFiveUsersTest.setPostalCode(postalCode);
    }

    @ConfigArgGet
    public String setUserPostalCode() {
        return this.createFiveUsersTest.getPostalCode();
    }

    @ConfigArgSet(required = false, desc = "Country for the created users", defaultValue = CreateUserTest.DEFAULT_COUNTRY)
    public void setUserCountry(String country) {
        this.createFiveUsersTest.setCountry(country);
    }

    @ConfigArgGet
    public String getUserCountry() {
        return this.createFiveUsersTest.getCountry();
    }

    @ConfigArgSet(required = false, desc = "State for the created users", defaultValue = CreateUserTest.DEFAULT_STATE)
    public void setUserState(String state) {
        this.createFiveUsersTest.setState(state);
    }

    @ConfigArgGet
    public String getUserState() {
        return this.createFiveUsersTest.getState();
    }

    @ConfigArgSet(required = false, desc = "Gender for the created users.", defaultValue = CreateUserTest.DEFAULT_GENDER)
    public void setUserGender(String gender) {
        this.createFiveUsersTest.setGender(gender);
    }

    @ConfigArgGet
    public String getUserGender() {
        return this.createFiveUsersTest.getGender();
    }

    @ConfigArgSet(required = false, desc = "User description", defaultValue = CreateUserTest.DEFAULT_ABOUT_ME)
    public void setUserAboutMe(String aboutMe) {
        this.createFiveUsersTest.setAboutMe(aboutMe);
    }

    @ConfigArgGet
    public String getUserAboutMe() {
        return this.createFiveUsersTest.getAboutMe();
    }

    @ConfigArgSet(required = false, desc = "Group Name", defaultValue = CreateUserGroupTest.DEFAULT_GROUP_NAME)
    public void setGroupName(String groupName) {
        this.createUserGroupTest.setGroupName(groupName);
    }

    @ConfigArgGet
    public String getGroupName() {
        return this.createUserGroupTest.getGroupName();
    }

    @ConfigArgSet(required = false, desc = "Group Description", defaultValue = CreateUserGroupTest.DEFAULT_GROUP_DESCRIPTION)
    public void setGroupDescription(String description) {
        this.createUserGroupTest.setDescription(description);
    }

    @ConfigArgGet
    public String getGroupDescription() {
        return this.createUserGroupTest.getDescription();
    }

    @ConfigArgSet(required = false, desc = "If this is true then some of user properties will be either incremented or randomised", defaultValue = "true")
    public void setIncrement(String value) {
        createUserGroupTest.setIncrement(value);
        createFiveUsersTest.setIncrement(value);
    }

    @ConfigArgGet
    public boolean getIncrement() {
        return createUserGroupTest.getIncrement();
    }

    private static class CreateFiveUsersTest extends CompositeTest {

        public CreateFiveUsersTest() { this(true); }

        public CreateFiveUsersTest(boolean createChildren) {
            if (createChildren) {
                AtomicInteger increment = new AtomicInteger(0);
                for(int i = 0; i < 5; i++) {
                    addChild(new CreateUserTest()
                            .setIncrement(increment)
                            .setGlobalArgs(this.getGlobalArgs()));
                }
            }
        }


        @Override
        public AbstractTest newInstance() {
            return new CreateFiveUsersTest(false);
        }

        @Override
        public boolean includeChildren() {
            return false;
        }

        public void setEmailAddress(final String emailAddress) {
            for(AbstractTest userTest : this.getChildren()) {
                ((CreateUserTest)userTest).setEmailAddress(emailAddress);
            }
        }

        public String getEmailAddress() {
            return ((CreateUserTest)this.getChildren().get(0)).getEmailAddress();
        }

        public void setPassword(String password) {
            for(AbstractTest userTest : this.getChildren()) {
                ((CreateUserTest)userTest).setPassword(password);
            }
        }

        public String getPassword() {
           return ((CreateUserTest)this.getChildren().get(0)).getPassword();
        }

        public void setPhoneNumber(String phoneNumber) {
            for(AbstractTest userTest : this.getChildren()) {
                ((CreateUserTest)userTest).setPhoneNumber(phoneNumber);
            }
        }

        public String getPhoneNumber() {
            return ((CreateUserTest)this.getChildren().get(0)).getPhoneNumber();
        }

        public void setFirstName(String firstName) {
            for(AbstractTest userTest : this.getChildren()) {
                ((CreateUserTest)userTest).setFirstName(firstName);
            }
        }

        public String getFirstName() {
           return ((CreateUserTest)this.getChildren().get(0)).getFirstName();
        }

        public void setLastName(String lastName) {
            for(AbstractTest userTest : this.getChildren()) {
                ((CreateUserTest)userTest).setLastName(lastName);
            }
        }

        public String getLastName() {
            return ((CreateUserTest)this.getChildren().get(0)).getLastName();
        }

        public void setJobTitle(String jobTitle) {
            for(AbstractTest userTest : this.getChildren()) {
                ((CreateUserTest)userTest).setJobTitle(jobTitle);
            }
        }

        public String getJobTitle() {
            return ((CreateUserTest)this.getChildren().get(0)).getJobTitle();
        }

        public void setStreet(String street) {
            for(AbstractTest userTest : this.getChildren()) {
                ((CreateUserTest)userTest).setStreet(street);
            }
        }

        public String getStreet() {
            return ((CreateUserTest)this.getChildren().get(0)).getStreet();
        }

        public void setCity(String city) {
            for(AbstractTest userTest : this.getChildren()) {
                ((CreateUserTest)userTest).setCity(city);
            }
        }

        public String getCity() {
            return ((CreateUserTest)this.getChildren().get(0)).getCity();
        }

        public void setMobile(String mobile) {
            for(AbstractTest userTest : this.getChildren()) {
                ((CreateUserTest)userTest).setMobile(mobile);
            }
        }

        public String getMobile() {
            return ((CreateUserTest)this.getChildren().get(0)).getMobile();
        }

        public void setPostalCode(String postalCode) {
            for(AbstractTest userTest : this.getChildren()) {
                ((CreateUserTest)userTest).setPostalCode(postalCode);
            }
        }

        public String getPostalCode() {
            return ((CreateUserTest)this.getChildren().get(0)).getPostalCode();
        }

        public void setCountry(String country) {
            for(AbstractTest userTest : this.getChildren()) {
                ((CreateUserTest)userTest).setCountry(country);
            }
        }

        public String getCountry() {
            return ((CreateUserTest)this.getChildren().get(0)).getCountry();
        }

        public void setState(String state) {
            for(AbstractTest userTest : this.getChildren()) {
                ((CreateUserTest)userTest).setState(state);
            }
        }

        public String getState() {
            return ((CreateUserTest)this.getChildren().get(0)).getState();
        }

        public void setGender(String gender) {
            for(AbstractTest userTest : this.getChildren()) {
                ((CreateUserTest)userTest).setGender(gender);
            }
        }

        public String getGender() {
            return ((CreateUserTest)this.getChildren().get(0)).getGender();
        }

        public void setIncrement(String increment) {
            for(AbstractTest userTest : this.getChildren()) {
                ((CreateUserTest)userTest).setIncrement(increment);
            }
        }

        public boolean getIncrement() {
            return ((CreateUserTest)this.getChildren().get(0)).getIncrement();
        }

        public void setAboutMe(String aboutMe) {
            for(AbstractTest userTest : this.getChildren()) {
                ((CreateUserTest)userTest).setAboutMe(aboutMe);
            }
        }

        public String getAboutMe() {
            return ((CreateUserTest)this.getChildren().get(0)).getAboutMe();
        }

    }
}
