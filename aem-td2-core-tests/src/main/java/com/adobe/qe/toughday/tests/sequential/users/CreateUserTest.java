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
package com.adobe.qe.toughday.tests.sequential.users;

import com.adobe.qe.toughday.api.core.AbstractTest;
import com.adobe.qe.toughday.api.annotations.Before;
import com.adobe.qe.toughday.api.annotations.Description;
import com.adobe.qe.toughday.api.annotations.Tag;
import com.adobe.qe.toughday.api.annotations.ConfigArgGet;
import com.adobe.qe.toughday.api.annotations.ConfigArgSet;
import com.adobe.qe.toughday.tests.sequential.AEMTestBase;
import com.adobe.qe.toughday.tests.utils.Constants;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.sling.testing.clients.util.FormEntityBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Tag(tags = { "author" })
@Description(desc = "Creates users similar to the user editor console (/libs/granite/security/content/userEditor.html)")
public class CreateUserTest extends AEMTestBase {

    public static final String DEFAULT_PASSWORD = "toughday";
    public static final String DEFAULT_EMAIL_ADDRESS = "toughday@adobe.com";
    public static final String DEFAULT_PHONE_NUMBER = "098765654";
    public static final String DEFAULT_FIRST_NAME = "Tough";
    public static final String DEFAULT_LAST_NAME = "Day";
    public static final String DEFAULT_JOB_TITLE = "Performance Tester";
    public static final String DEFAULT_STREET = "151 South Almaden Boulevard";
    public static final String DEFAULT_CITY = "San Jose";
    public static final String DEFAULT_MOBILE = "0987654";
    public static final String DEFAULT_POSTAL_CODE = "123456";
    public static final String DEFAULT_COUNTRY = "United States";
    public static final String DEFAULT_GENDER = "male";
    public static final String DEFAULT_STATE = "California";
    public static final String DEFAULT_ABOUT_ME = "Stress testing and performance benchmarking.";
    public static final String DEFAULT_PATH_TO_ADD_USERS = "/home/users";

    private String id;
    private String title;
    private String password = DEFAULT_PASSWORD;
    private String emailAddress = DEFAULT_EMAIL_ADDRESS;
    private String phoneNumber = DEFAULT_PHONE_NUMBER;
    private String firstName = DEFAULT_FIRST_NAME;
    private String lastName = DEFAULT_LAST_NAME;
    private String jobTitle = DEFAULT_JOB_TITLE;
    private String street = DEFAULT_STREET;
    private String city = DEFAULT_CITY;
    private String mobile = DEFAULT_MOBILE;
    private String postalCode = DEFAULT_POSTAL_CODE;
    private String country = DEFAULT_COUNTRY;
    private String state = DEFAULT_STATE;
    private String gender = DEFAULT_GENDER;
    private String aboutMe = DEFAULT_ABOUT_ME;
    private String pathToAddUsers = DEFAULT_PATH_TO_ADD_USERS;
    private List<String> groups = new ArrayList<>();
    private AtomicInteger increment;
    private boolean incrementGender = true;
    private boolean incrementCountry = true;

    public CreateUserTest() {
        increment = new AtomicInteger(0);
    }

    @Before
    private void before() {
        title = id = RandomStringUtils.randomAlphanumeric(20);
    }

    @Override
    public void test() throws Throwable {
        String firstName    = this.firstName;
        String lastName     = this.lastName;
        String phoneNumber  = this.phoneNumber;
        String mobile       = this.mobile;
        String jobTitle     = this.jobTitle;
        String country      = this.country;
        String gender       = this.gender;
        String aboutMe      = this.aboutMe;
        String emailAddress = this.emailAddress;

        if(increment != null) {
            int incrementValue = increment.getAndIncrement();
            firstName += incrementValue;
            lastName += incrementValue;
            phoneNumber += incrementValue;
            mobile += incrementValue;
            jobTitle += incrementValue;
            aboutMe += incrementValue;
            String[] tmp = emailAddress.split("@");
            gender = incrementGender ? Constants.GENDERS[incrementValue % 2] : gender;
            country = incrementCountry ? Constants.COUNTRIES[incrementValue % Constants.COUNTRIES.length] : country;
            emailAddress = tmp[0] + incrementValue + "@" + tmp[1];
        }

        String currentPathToAddUsers = StringUtils.stripEnd(getCommunication("parentPath", pathToAddUsers), "/");

        //Create user
        FormEntityBuilder entityBuilder = FormEntityBuilder.create()
            .addParameter("authorizableId", id)
            .addParameter("./jcr:title", title)
            .addParameter("./profile/email", emailAddress)
            .addParameter("rep:password", password)
            .addParameter("./profile/givenName", firstName)
            .addParameter("./profile/familyName", lastName)
            .addParameter("./profile/phoneNumber", phoneNumber)
            .addParameter("./profile/jobTitle", jobTitle)
            .addParameter("./profile/street", street)
            .addParameter("./profile/mobile", mobile)
            .addParameter("./profile/city", city)
            .addParameter("./profile/postalCode", postalCode)
            .addParameter("./profile/country", country)
            .addParameter("./profile/state", state)
            .addParameter("./profile/gender", gender)
            .addParameter("./profile/aboutMe", aboutMe)
            .addParameter("_charset_", "utf-8")
            .addParameter("intermediatePath",currentPathToAddUsers)
            .addParameter("createUser", "1");

        try {
            logger().debug("{}: Trying to create user={}, with id={}", Thread.currentThread().getName(), firstName + " " + lastName, id);
            benchmark().measure(this, "CreateUser", getDefaultClient()).doPost("/libs/granite/security/post/authorizables.html", entityBuilder.build(), HttpStatus.SC_CREATED);
        } catch (Throwable e) {
            logger().warn("{}: Failed to create user={}, with id={}", Thread.currentThread().getName(), firstName + " " + lastName, id);
            logger().debug(Thread.currentThread().getName() + "ERROR: ", e);

            throw e;
        }

        logger().debug("{}: Successfully created user={}, with id={}", Thread.currentThread().getName(), firstName + " " + lastName, id);

        //Add user to the groups
        for(String group : groups) {
            addUserToGroup(group, id);
        }

        List<String> communicatedGroups = getCommunication("groups", new ArrayList<String>());
        communicatedGroups.remove(groups);
        for(String group : communicatedGroups) {
            addUserToGroup(group, id);
        }
    }

    private void addUserToGroup(String group, String user) throws Throwable {
        String groupServlet = group + ".rw.userprops.html";

        FormEntityBuilder entityBuilder = FormEntityBuilder.create()
                .addParameter("addMembers", id)
                .addParameter("_charset_", "utf-8");

        try {
            logger().debug("{}: Trying to add user={}, to group ={}", Thread.currentThread().getName(), user, group);

            getDefaultClient().doPost(groupServlet, entityBuilder.build(), HttpStatus.SC_OK);
        } catch (Throwable e) {
            logger().warn("{}: Failed to add user={}, to group ={}", Thread.currentThread().getName(), user, group);
            logger().debug(Thread.currentThread().getName() + "ERROR: ", e);

            throw e;
        }

        logger().debug("{}: Successfully added user={}, to group ={}", Thread.currentThread().getName(), user, group);
    }

    @ConfigArgSet(required = false, desc = "Path where the users are created.", defaultValue = DEFAULT_PATH_TO_ADD_USERS)
    public CreateUserTest setPathToAddUsers(String pathToAddUsers) {
        this.pathToAddUsers = pathToAddUsers;
        return this;
    }

    @ConfigArgGet
    public String getPathToAddUsers() {
        return pathToAddUsers;
    }

    @ConfigArgSet(required = false, desc = "Email address for created users.", defaultValue = DEFAULT_EMAIL_ADDRESS)
    public CreateUserTest setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
        return this;
    }

    @ConfigArgGet
    public String getEmailAddress() {
        return this.emailAddress;
    }

    @ConfigArgSet(required = false, desc = "Password for the created users.", defaultValue = DEFAULT_PASSWORD)
    public CreateUserTest setPassword(String password) {
        this.password = password;
        return this;
    }

    @ConfigArgGet
    public String getPassword() {
        return this.password;
    }

    @ConfigArgSet(required = false, desc = "Telephone for the created users.", defaultValue = DEFAULT_PHONE_NUMBER)
    public CreateUserTest setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        return this;
    }

    @ConfigArgGet
    public String getPhoneNumber() {
        return this.phoneNumber;
    }

    @ConfigArgSet(required = false, desc = "First name for the created users", defaultValue = DEFAULT_FIRST_NAME)
    public CreateUserTest setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    @ConfigArgGet
    public String getFirstName() {
        return this.firstName;
    }

    @ConfigArgSet(required = false, desc = "Last name for the created users", defaultValue = DEFAULT_LAST_NAME)
    public CreateUserTest setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    @ConfigArgGet
    public String getLastName() {
        return this.lastName;
    }

    @ConfigArgSet(required = false, desc = "Job title for the created users", defaultValue = DEFAULT_JOB_TITLE)
    public CreateUserTest setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
        return this;
    }

    @ConfigArgGet
    public String getJobTitle() {
        return this.jobTitle;
    }

    @ConfigArgSet(required = false, desc = "Street address for the created users", defaultValue = DEFAULT_STREET)
    public CreateUserTest setStreet(String street) {
        this.street = street;
        return this;
    }

    @ConfigArgGet
    public String getStreet() {
        return this.street;
    }

    @ConfigArgSet(required = false, desc = "City address for the created users", defaultValue = DEFAULT_CITY)
    public CreateUserTest setCity(String city) {
        this.city = city;
        return this;
    }

    @ConfigArgGet
    public String getCity() {
        return this.city;
    }

    @ConfigArgSet(required = false, desc = "Mobile number for the created users", defaultValue = DEFAULT_MOBILE)
    public CreateUserTest setMobile(String mobile) {
        this.mobile = mobile;
        return this;
    }

    @ConfigArgGet
    public String getMobile() {
        return this.mobile;
    }

    @ConfigArgSet(required = false, desc = "Postal code for the created users", defaultValue = DEFAULT_POSTAL_CODE)
    public CreateUserTest setPostalCode(String postalCode) {
        this.postalCode = postalCode;
        return this;
    }

    @ConfigArgGet
    public String getPostalCode() {
        return postalCode;
    }

    @ConfigArgSet(required = false, desc = "Country for the created users", defaultValue = DEFAULT_COUNTRY)
    public CreateUserTest setCountry(String country) {
        this.country = country;
        incrementCountry = false;
        return this;
    }

    @ConfigArgGet
    public String getCountry() {
        return this.country;
    }

    @ConfigArgSet(required = false, desc = "State for the created users", defaultValue = DEFAULT_STATE)
    public CreateUserTest setState(String state) {
        this.state = state;
        return this;
    }

    @ConfigArgGet
    public String getState() {
        return this.state;
    }

    @ConfigArgSet(required = false, desc = "Gender for the created users.", defaultValue = DEFAULT_GENDER)
    public CreateUserTest setGender(String gender) {
        this.gender = gender;
        incrementGender = false;
        return this;
    }

    @ConfigArgGet
    public String getGender() {
        return this.gender;
    }

    @ConfigArgSet(required = false, desc = "User description", defaultValue = DEFAULT_ABOUT_ME)
    public CreateUserTest setAboutMe(String aboutMe) {
        this.aboutMe = aboutMe;
        return this;
    }

    @ConfigArgGet
    public String getAboutMe() {
        return this.aboutMe;
    }

    @ConfigArgSet(required = false, desc = "If this is \"true\" then some of user properties will be either incremented or randomised", defaultValue = "true")
    public CreateUserTest setIncrement(String value) {
        if (!Boolean.valueOf(value))
            this.increment = null;
        return this;
    }

    @ConfigArgGet
    public boolean getIncrement() {
        return this.increment != null;
    }

    public CreateUserTest setIncrement(AtomicInteger increment) {
        this.increment = increment;
        return this;
    }

    @ConfigArgSet(required = false, desc = "Comma separated group paths. Newly created users will be added in these groups.")
    public CreateUserTest setGroups(String values) {
        groups.addAll(Arrays.asList(values.split(",")));
        return this;
    }

    public CreateUserTest setGroups(List<String> groups) {
        this.groups = groups;
        return this;
    }

    private CreateUserTest setIncrementCountry(boolean incrementCountry) {
        this.incrementCountry = incrementCountry;
        return this;
    }

    private CreateUserTest setIncrementGender(boolean incrementGender) {
        this.incrementGender = incrementGender;
        return this;
    }

    @Override
    public AbstractTest newInstance() {
        return new CreateUserTest()
                .setGroups(groups)
                .setIncrement(increment)
                .setFirstName(firstName)
                .setLastName(lastName)
                .setPassword(password)
                .setStreet(street)
                .setCity(city)
                .setState(state)
                .setCountry(country)
                .setIncrementCountry(incrementCountry)
                .setPostalCode(postalCode)
                .setPhoneNumber(phoneNumber)
                .setMobile(mobile)
                .setJobTitle(jobTitle)
                .setEmailAddress(emailAddress)
                .setAboutMe(aboutMe)
                .setGender(gender)
                .setPathToAddUsers(pathToAddUsers)
                .setIncrementGender(incrementGender);
    }

}
