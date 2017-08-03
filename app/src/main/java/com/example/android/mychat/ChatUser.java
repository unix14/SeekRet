package com.example.android.mychat;

import com.google.firebase.auth.FirebaseUser;

/**
 * Created by eyal on 31-Jul-17.
 */

public class ChatUser {
    private FirebaseUser firebaseUser;
    private String userName;
    private String profilePhotoUrl;
    private boolean isAdmin;


    public ChatUser(FirebaseUser firebaseUser, String profilePhotoUrl) {
        this.firebaseUser = firebaseUser;
        if(firebaseUser.getDisplayName() !=null){
            userName = firebaseUser.getDisplayName();
        }else
            userName = firebaseUser.getEmail();
        this.profilePhotoUrl = profilePhotoUrl;
        isAdmin = false;
    }

    public ChatUser(FirebaseUser firebaseUser) {
        this.firebaseUser = firebaseUser;
        if(firebaseUser.getDisplayName() !=null){
            userName = firebaseUser.getDisplayName();
        }else
            userName = firebaseUser.getEmail();
        isAdmin = false;
    }


    public ChatUser(String profilePhotoUrl) {
        this.profilePhotoUrl = profilePhotoUrl;
        isAdmin = false;
    }

    public FirebaseUser getFirebaseUser() {
        return firebaseUser;
    }

    public void setFirebaseUser(FirebaseUser firebaseUser) {
        this.firebaseUser = firebaseUser;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getProfilePhotoUrl() {
        return profilePhotoUrl;
    }

    public void setProfilePhotoUrl(String profilePhotoUrl) {
        this.profilePhotoUrl = profilePhotoUrl;
    }
}
