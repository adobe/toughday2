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


import com.adobe.qe.toughday.api.annotations.Description;
import com.adobe.qe.toughday.api.annotations.Tag;
import com.adobe.qe.toughday.api.annotations.ConfigArgGet;
import com.adobe.qe.toughday.api.annotations.ConfigArgSet;
import com.adobe.qe.toughday.api.core.AbstractTest;
import com.adobe.qe.toughday.api.core.CompositeTest;
import com.adobe.qe.toughday.tests.sequential.CreateFolderTreeTest;
import com.adobe.qe.toughday.tests.sequential.users.CreateUserTest;
import com.adobe.qe.toughday.tests.utils.TreePhaser;

@Tag(tags = { "author" })
@Description(desc = "This test creates folders hierarchically. In addition, it creates users in each folder.")
public class CreateUserTreeTest extends CompositeTest {
    private CreateFolderTreeTest createFolderTreeTest;
    private CreateUserTest createUserTest;
    private static final String AUTHORIZABLE_FOLDER_RESOURCE_TYPE = "rep:AuthorizableFolder";
    private static final String DEFAULT_FOLDER_PATH = "/home/users";

    public CreateUserTreeTest() {
        this(true);
    }

    public CreateUserTreeTest(boolean createChild) {
        if (createChild) {
            createFolderTreeTest = new CreateFolderTreeTest();
            createUserTest = new CreateUserTest();

            createFolderTreeTest.setGlobalArgs(this.getGlobalArgs());
            createUserTest.setGlobalArgs(this.getGlobalArgs());

            this.addChild(createFolderTreeTest);
            this.addChild(createUserTest);
        }
    }

    @Override
    public AbstractTest newInstance() {
        return new CreateUserTreeTest(false);
    }

    @ConfigArgGet
    public String getAboutMe() {
        return this.createUserTest.getAboutMe();
    }

    @ConfigArgSet(required = false, desc = "User description", defaultValue = CreateUserTest.DEFAULT_ABOUT_ME)
    public CreateUserTreeTest setAboutMe(String aboutMe) {
        this.createUserTest.setAboutMe(aboutMe);
        return this;
    }

    @ConfigArgGet
    public String getGender() {
        return this.createUserTest.getGender();
    }

    @ConfigArgSet(required = false, desc = "Gender for created users.", defaultValue = CreateUserTest.DEFAULT_GENDER)
    public CreateUserTreeTest setGender(String gender) {
        this.createUserTest.setGender(gender);
        return this;
    }

    @ConfigArgGet
    public String getState() {
        return this.createUserTest.getState();
    }

    @ConfigArgSet(required = false, desc = "State for created users.", defaultValue = CreateUserTest.DEFAULT_STATE)
    public CreateUserTreeTest setState(String state) {
        this.createUserTest.setState(state);
        return this;
    }

    @ConfigArgGet
    public String getPostalCode() {
        return this.createUserTest.getPostalCode();
    }

    @ConfigArgSet(required = false, desc = "Postal code for created users.", defaultValue = CreateUserTest.DEFAULT_POSTAL_CODE)
    public CreateUserTreeTest setPostalCode(String postalCode) {
        this.createUserTest.setPostalCode(postalCode);
        return this;
    }

    @ConfigArgGet
    public String getPhoneNumber() {
        return this.createUserTest.getPhoneNumber();
    }

    @ConfigArgSet(required = false, desc = "Phone number for created users.", defaultValue = CreateUserTest.DEFAULT_PHONE_NUMBER)
    public CreateUserTreeTest setPhoneNumber(String phoneNumber) {
        this.createUserTest.setPhoneNumber(phoneNumber);
        return this;
    }

    @ConfigArgGet
    public String getMobile() {
        return this.createUserTest.getMobile();
    }

    @ConfigArgSet(required = false, desc = "Mobile for created users.", defaultValue = CreateUserTest.DEFAULT_MOBILE)
    public CreateUserTreeTest setMobile(String mobile) {
        this.createUserTest.setMobile(mobile);
        return this;
    }

    @ConfigArgGet
    public String getJobTitle() {
        return this.createUserTest.getJobTitle();
    }

    @ConfigArgSet(required = false, desc = "Job title for created users.", defaultValue = CreateUserTest.DEFAULT_JOB_TITLE)
    public CreateUserTreeTest setJobTitle(String jobTitle) {
        this.createUserTest.setJobTitle(jobTitle);
        return this;
    }

    @ConfigArgGet
    public String getCountry() {
        return this.createUserTest.getCountry();
    }

    @ConfigArgSet(required = false, desc = "Country for created users.", defaultValue = CreateUserTest.DEFAULT_COUNTRY)
    public CreateUserTreeTest setCountry(String country) {
        this.createUserTest.setCountry(country);
        return this;
    }

    @ConfigArgGet
    public String getCity() {
        return this.createUserTest.getCity();
    }

    @ConfigArgSet(required = false, desc = "City for created users.", defaultValue = CreateUserTest.DEFAULT_CITY)
    public CreateUserTreeTest setCity(String city) {
        this.createUserTest.setCity(city);
        return this;
    }

    @ConfigArgGet
    public String getStreet() {
        return this.createUserTest.getStreet();
    }

    @ConfigArgSet(required = false, desc = "Street for created users.", defaultValue = CreateUserTest.DEFAULT_STREET)
    public CreateUserTreeTest setStreet(String street) {
        this.createUserTest.setStreet(street);
        return this;
    }

    @ConfigArgGet
    public String getLastName() {
        return this.createUserTest.getLastName();
    }

    @ConfigArgSet(required = false, desc = "Last name for created users.", defaultValue = CreateUserTest.DEFAULT_LAST_NAME)
    public CreateUserTreeTest setLastName(String lastName) {
        this.createUserTest.setLastName(lastName);
        return this;
    }

    @ConfigArgGet
    public String getFirstName() {
        return this.createUserTest.getFirstName();
    }

    @ConfigArgSet(required = false, desc = "First name for created users.", defaultValue = CreateUserTest.DEFAULT_FIRST_NAME)
    public CreateUserTreeTest setFirstName(String firstName) {
        this.createUserTest.setFirstName(firstName);
        return this;
    }

    @ConfigArgGet
    public String getPassword() {
        return this.createUserTest.getPassword();
    }

    @ConfigArgSet(required = false, desc = "Password for created users.", defaultValue = CreateUserTest.DEFAULT_PASSWORD)
    public CreateUserTreeTest setPassword(String password) {
        this.createUserTest.setPassword(password);
        return this;
    }

    @ConfigArgGet
    public String getEmailAddress() {
        return this.createUserTest.getEmailAddress();
    }

    @ConfigArgSet(required = false, desc = "Email address for created users.", defaultValue = CreateUserTest.DEFAULT_EMAIL_ADDRESS)
    public CreateUserTreeTest setEmailAddress(String emailAddress) {
        this.createUserTest.setEmailAddress(emailAddress);
        return this;
    }

    @ConfigArgSet(required = false,  defaultValue = "true",
            desc = "If this is true then some of user properties will be either incremented or randomised.")
    public void setIncrement(String value) {
        createUserTest.setIncrement(value);
    }

    @ConfigArgGet
    public boolean getIncrement() {
        return this.createUserTest.getIncrement();
    }

    @ConfigArgSet(required = false, desc = "Path where the users are created.", defaultValue = CreateUserTest.DEFAULT_PATH_TO_ADD_USERS)
    public CreateUserTreeTest setPathToAddUsers(String pathToAddUsers) {
        this.createUserTest.setPathToAddUsers(pathToAddUsers);
        return this;
    }

    @ConfigArgGet
    public String getPathToAddUsers() {
        return this.createUserTest.getPathToAddUsers();
    }

    @ConfigArgGet
    public String getFolderTitle() {
        return this.createFolderTreeTest.getTitle();
    }

    @ConfigArgSet(required = false, defaultValue = CreateFolderTreeTest.DEFAULT_TITLE, desc = "The title of the folders. Internally, this is incremented")
    public CreateUserTreeTest setFolderTitle(String title) {
        this.createFolderTreeTest.setTitle(title);
        return this;
    }

    @ConfigArgGet
    public String getParentPath() {
        return this.createFolderTreeTest.getParentPath();
    }

    @ConfigArgSet(required = false, defaultValue = DEFAULT_FOLDER_PATH, desc = "The path prefix for all folders.")
    public CreateUserTreeTest setFolderParentPath(String parentPath) {
        this.createFolderTreeTest.setParentPath(parentPath);
        return this;
    }

    @ConfigArgGet
    public int getBase() {
        return this.createFolderTreeTest.getBase();
    }

    @ConfigArgSet(required = false, desc = "How many direct child folders will a folder have.", defaultValue = TreePhaser.DEFAULT_BASE)
    public CreateUserTreeTest setBase(String base) {
        this.createFolderTreeTest.setBase(base);
        return this;
    }

    @ConfigArgSet(required = false, desc = "Resource type for folders.", defaultValue = AUTHORIZABLE_FOLDER_RESOURCE_TYPE)
    public CreateUserTreeTest setResourceType(String resourceType) {
        this.createFolderTreeTest.setResourceType(resourceType);
        return this;
    }

    @ConfigArgGet
    public String getResourceType() {
        return this.createFolderTreeTest.getResourceType();
    }

}
